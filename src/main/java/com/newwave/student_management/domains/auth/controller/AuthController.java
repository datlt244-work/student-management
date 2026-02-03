package com.newwave.student_management.domains.auth.controller;

import com.newwave.student_management.common.dto.ApiResponse;
import com.newwave.student_management.domains.auth.dto.request.LoginRequest;
import com.newwave.student_management.domains.auth.dto.response.LoginResponse;
import com.newwave.student_management.domains.auth.dto.request.RefreshTokenRequest;
import com.newwave.student_management.domains.auth.service.IAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
