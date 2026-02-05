package com.newwave.student_management.domains.auth.controller;

import com.newwave.student_management.common.dto.ApiResponse;
import com.newwave.student_management.domains.auth.dto.request.LoginRequest;
import com.newwave.student_management.domains.auth.dto.response.LoginResponse;
import com.newwave.student_management.domains.auth.dto.request.LogoutRequest;
import com.newwave.student_management.domains.auth.dto.request.RefreshTokenRequest;
import com.newwave.student_management.domains.auth.dto.request.ForgotPasswordRequest;
import com.newwave.student_management.domains.auth.dto.request.ResetPasswordRequest;
import com.newwave.student_management.domains.auth.service.IAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "API xác thực người dùng (login, token, ...)")
public class AuthController {

    private final IAuthService authService;

    @PostMapping("/login")
    @Operation(
            summary = "Đăng nhập hệ thống",
            description = "Xác thực người dùng bằng email và mật khẩu, áp dụng rate limiting theo email, "
                    + "cập nhật thông tin đăng nhập và trả về access token + refresh token."
    )
    public ApiResponse<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpServletRequest
    ) {
        String clientIp = extractClientIp(httpServletRequest);
        return ApiResponse.success(authService.authenticate(request, clientIp));
    }

    @PostMapping("/refresh-token")
    @Operation(
            summary = "Làm mới access token",
            description = "Nhận refresh token, validate trong Redis, lấy lại thông tin user và trả về cặp token mới."
    )
    public ApiResponse<LoginResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        return ApiResponse.success(authService.refreshToken(request));
    }

    @PostMapping("/logout")
    @Operation(
            summary = "Đăng xuất",
            description = "Public endpoint. Xóa refresh token trong Redis và (tùy chọn) blacklist access token. "
                    + "Cho phép gọi khi token hết hạn/không có để client xóa cookie mà không nhận 401."
    )
    public ApiResponse<String> logout(
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader,
            @RequestBody(required = false) LogoutRequest request
    ) {
        String accessToken = null;
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            accessToken = authorizationHeader.substring(7);
        }
        String refreshToken = (request != null && request.getRefreshToken() != null) ? request.getRefreshToken().trim() : null;
        if (refreshToken != null && refreshToken.isEmpty()) refreshToken = null;
        authService.logout(accessToken, refreshToken);
        return ApiResponse.success("Logged out successfully");
    }

    @PostMapping("/forgot-password")
    @Operation(
            summary = "Quên mật khẩu",
            description = "Gửi email reset mật khẩu với rate limit 3 lần / 15 phút, không tiết lộ email tồn tại hay không."
    )
    public ApiResponse<Object> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request
    ) {
        authService.forgotPassword(request);
        return ApiResponse.success(
                java.util.Map.of(
                        "message", "If an account exists with this email, a password reset link has been sent.",
                        "cooldownMinutes", 15
                )
        );
    }

    @PostMapping("/reset-password")
    @Operation(
            summary = "Đặt lại mật khẩu",
            description = "Dùng token từ email reset để đặt mật khẩu mới. Sau khi đổi, tất cả phiên đăng nhập bị hủy."
    )
    public ApiResponse<Object> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        authService.resetPassword(request);
        return ApiResponse.success(
                java.util.Map.of(
                        "message",
                        "Password reset successfully. All sessions have been logged out. Please login again."
                )
        );
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
