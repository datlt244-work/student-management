package com.newwave.student_management.domains.auth.controller;

import com.newwave.student_management.common.dto.ApiResponse;
import com.newwave.student_management.domains.auth.dto.request.ChangePasswordRequest;
import com.newwave.student_management.domains.auth.dto.response.ChangePasswordResponse;
import com.newwave.student_management.domains.auth.service.IAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "API quản lý tài khoản người dùng (change password, ...)")
public class UserController {

    private final IAuthService authService;

    @PostMapping("/me/change-password")
    @Operation(
            summary = "Đổi mật khẩu",
            description = "Cho phép user đổi mật khẩu bằng current password. "
                    + "Có tùy chọn đăng xuất tất cả thiết bị khác và cấp token mới cho thiết bị hiện tại."
    )
    public ApiResponse<ChangePasswordResponse> changePassword(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        UUID userId = UUID.fromString(jwt.getClaim("userId"));
        ChangePasswordResponse response = authService.changePassword(userId, request);
        return ApiResponse.success(response);
    }
}

