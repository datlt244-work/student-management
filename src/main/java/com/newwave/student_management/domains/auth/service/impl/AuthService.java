package com.newwave.student_management.domains.auth.service.impl;

import com.newwave.student_management.common.exception.AppException;
import com.newwave.student_management.common.exception.ErrorCode;
import com.newwave.student_management.domains.auth.dto.request.LoginRequest;
import com.newwave.student_management.domains.auth.dto.response.LoginResponse;
import com.newwave.student_management.domains.auth.entity.User;
import com.newwave.student_management.domains.auth.entity.UserStatus;
import com.newwave.student_management.domains.auth.repository.UserRepository;
import com.newwave.student_management.domains.auth.service.IAuthService;
import com.newwave.student_management.domains.auth.service.ITokenRedisService;
import com.newwave.student_management.infrastructure.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService implements IAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ITokenRedisService tokenRedisService;

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

        // 9. Generate tokens
        String accessToken = jwtService.generateToken(user);
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
}
