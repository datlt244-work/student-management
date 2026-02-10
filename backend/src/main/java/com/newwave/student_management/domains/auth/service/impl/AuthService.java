package com.newwave.student_management.domains.auth.service.impl;

import com.newwave.student_management.common.exception.AppException;
import com.newwave.student_management.common.exception.ErrorCode;
import com.newwave.student_management.domains.auth.dto.request.ChangePasswordRequest;
import com.newwave.student_management.domains.auth.dto.request.ForgotPasswordRequest;
import com.newwave.student_management.domains.auth.dto.request.LoginRequest;
import com.newwave.student_management.domains.auth.dto.request.RefreshTokenRequest;
import com.newwave.student_management.domains.auth.dto.request.ResetPasswordRequest;
import com.newwave.student_management.domains.auth.dto.response.ChangePasswordResponse;
import com.newwave.student_management.domains.auth.dto.response.LoginResponse;
import com.newwave.student_management.domains.auth.entity.User;
import com.newwave.student_management.domains.auth.entity.UserStatus;
import com.newwave.student_management.domains.auth.repository.UserRepository;
import com.newwave.student_management.domains.auth.service.IAuthService;
import com.newwave.student_management.domains.auth.service.ITokenRedisService;
import com.newwave.student_management.infrastructure.mail.IMailService;
import com.newwave.student_management.infrastructure.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService implements IAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ITokenRedisService tokenRedisService;
    private final IMailService mailService;

    @Value("${spring.security.jwt.expiration-seconds:3600}")
    private long accessTokenExpiresIn;

    @Override
    public LoginResponse authenticate(LoginRequest loginRequest, String clientIp) {
        String email = loginRequest.getEmail() != null ? loginRequest.getEmail().trim().toLowerCase() : null;

        // 2. Rate limit by email
        if (tokenRedisService.isRateLimited(email)) {
            throw new AppException(ErrorCode.RATE_LIMITED);
        }

        // 3. Find user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    tokenRedisService.recordFailedAttempt(email);
                    return new AppException(ErrorCode.UNAUTHENTICATED);
                });

        // 4. Status checks
        UserStatus status = user.getStatus();
        if (status == UserStatus.PENDING_VERIFICATION || !user.isEmailVerified()) {
            throw new AppException(ErrorCode.EMAIL_NOT_VERIFIED);
        }
        if (status == UserStatus.BLOCKED) {
            throw new AppException(ErrorCode.ACCOUNT_BLOCKED);
        }
        if (status == UserStatus.INACTIVE) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_ACTIVE);
        }

        // 5. Verify password
        boolean ok = passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash());
        if (!ok) {
            int remaining = tokenRedisService.recordFailedAttempt(email);
            log.warn("Login failed for email={}, remainingAttempts={}, ip={}", email, remaining, clientIp);
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        // 6. Reset failed attempts
        tokenRedisService.resetFailedAttempts(email);

        // 7. Update login tracking
        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(clientIp);
        user.setLoginCount(user.getLoginCount() + 1);
        userRepository.save(user);

        // 9. Generate tokens (with current token version so change-password can invalidate all)
        long tokenVersion = tokenRedisService.getTokenVersion(user.getUserId());
        String accessToken = jwtService.generateToken(user, tokenVersion);
        String refreshToken = tokenRedisService.createAndStoreRefreshToken(user.getUserId());

        // 10. Return response
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(accessTokenExpiresIn)
                .userId(user.getUserId())
                .email(user.getEmail())
                .profilePictureUrl(user.getProfilePictureUrl())
                .role(user.getRole() != null ? user.getRole().getRoleName() : null)
                .authenticated(true)
                .build();
    }

    @Override
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new AppException(ErrorCode.TOKEN_REQUIRED);
        }

        // 2. Validate refresh token in Redis and get userId
        var userId = tokenRedisService.getUserIdByRefreshToken(refreshToken);
        if (userId == null) {
            throw new AppException(ErrorCode.TOKEN_INVALID);
        }

        // 3. Load user info from database
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // 4. Status checks
        UserStatus status = user.getStatus();
        if (status == UserStatus.PENDING_VERIFICATION || !user.isEmailVerified()) {
            throw new AppException(ErrorCode.EMAIL_NOT_VERIFIED);
        }
        if (status == UserStatus.BLOCKED) {
            throw new AppException(ErrorCode.ACCOUNT_BLOCKED);
        }
        if (status == UserStatus.INACTIVE) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_ACTIVE);
        }

        // 5. Xóa refresh token cũ
        tokenRedisService.deleteRefreshToken(userId, refreshToken);

        // 6. Generate new tokens (with current token version)
        long tokenVersion = tokenRedisService.getTokenVersion(user.getUserId());
        String newAccessToken = jwtService.generateToken(user, tokenVersion);
        String newRefreshToken = tokenRedisService.createAndStoreRefreshToken(user.getUserId());

        // 7. Build response
        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .expiresIn(accessTokenExpiresIn)
                .userId(user.getUserId())
                .email(user.getEmail())
                .profilePictureUrl(user.getProfilePictureUrl())
                .role(user.getRole() != null ? user.getRole().getRoleName() : null)
                .authenticated(true)
                .build();
    }

    @Override
    public void logout(String accessToken, String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            var userId = tokenRedisService.getUserIdByRefreshToken(refreshToken);
            tokenRedisService.deleteRefreshToken(userId, refreshToken);
        }

        if (accessToken == null || accessToken.isBlank()) {
            return;
        }

        try {
            var claims = com.nimbusds.jwt.SignedJWT.parse(accessToken).getJWTClaimsSet();
            String jti = claims.getJWTID();
            var exp = claims.getExpirationTime();
            if (jti != null && exp != null) {
                long nowMillis = System.currentTimeMillis();
                long remainingSeconds = Math.max(0, (exp.getTime() - nowMillis) / 1000);
                tokenRedisService.blacklistAccessToken(jti, remainingSeconds);
            }
        } catch (ParseException e) {
            log.warn("Failed to parse access token on logout", e);
        }
    }

    @Override
    public void forgotPassword(ForgotPasswordRequest request) {
        String rawEmail = request.getEmail();
        String email = rawEmail != null ? rawEmail.trim().toLowerCase() : null;

        // Step 1: rate limit trước
        int count = tokenRedisService.incrementResetPasswordCount(email, 15 * 60);
        if (count > 3) {
            throw new AppException(ErrorCode.PASSWORD_RESET_COOLDOWN);
        }

        // Step 2: tìm user
        var optionalUser = userRepository.findByEmail(email);
        if (optionalUser.isEmpty()) {
            // Case A: user không tồn tại → vẫn trả success, không gửi email
            return;
        }

        var user = optionalUser.get();

        // Step 3: xóa token cũ + tạo token mới với TTL 15 phút
        tokenRedisService.deleteResetPasswordToken(user.getUserId());
        String resetToken = tokenRedisService.createResetPasswordToken(user.getUserId(), 15 * 60);

        // Step 4: gửi email reset (HTML email)
        String fullName = user.getEmail(); // Có thể thay bằng user.getName() nếu có field name
        mailService.sendPasswordResetEmail(email, fullName, resetToken);
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        // 1. Validate passwords match
        if (!java.util.Objects.equals(request.getNewPassword(), request.getConfirmPassword())) {
            throw new AppException(ErrorCode.PASSWORD_MISMATCH);
        }

        // 2. Validate reset token and get userId
        String token = request.getToken() != null ? request.getToken().trim() : null;
        if (token == null || token.isBlank()) {
            throw new AppException(ErrorCode.TOKEN_REQUIRED);
        }
        UUID userId = tokenRedisService.getUserIdByResetToken(token);
        if (userId == null) {
            throw new AppException(ErrorCode.INVALID_RESET_TOKEN);
        }

        // 3. Load user and update password
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // 4. Delete reset token (one-time use)
        tokenRedisService.deleteResetPasswordToken(userId);

        // 5. Logout all devices: delete all refresh tokens for this user
        tokenRedisService.deleteAllRefreshTokensForUser(userId);
    }

    @Override
    public ChangePasswordResponse changePassword(UUID userId, ChangePasswordRequest request) {
        if (userId == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        // 1. Validate new password confirmation
        if (!Objects.equals(request.getNewPassword(), request.getConfirmPassword())) {
            throw new AppException(ErrorCode.PASSWORD_MISMATCH);
        }

        // 2. Load current user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // 3. Verify current password
        boolean currentOk = passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash());
        if (!currentOk) {
            throw new AppException(ErrorCode.INCORRECT_PASSWORD);
        }

        // 4. Ensure new password is different
        boolean samePassword = passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash());
        if (samePassword) {
            throw new AppException(ErrorCode.SAME_PASSWORD);
        }

        // 5. Update password
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // 6. Invalidate all access tokens (current + other devices/tabs): increment token version
        tokenRedisService.incrementTokenVersion(userId);

        // 7. Handle logoutOtherDevices option: delete all refresh tokens
        boolean logoutOtherDevices = request.getLogoutOtherDevices() == null || request.getLogoutOtherDevices();

        int loggedOutDevices = 0;
        if (logoutOtherDevices) {
            loggedOutDevices = tokenRedisService.deleteAllRefreshTokensForUser(userId);
        }

        return ChangePasswordResponse.builder()
                .message("Password changed successfully. Please login again.")
                .loggedOutDevices(loggedOutDevices)
                .build();
    }
}
