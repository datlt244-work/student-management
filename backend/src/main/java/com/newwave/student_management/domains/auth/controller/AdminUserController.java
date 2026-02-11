package com.newwave.student_management.domains.auth.controller;

import com.newwave.student_management.common.dto.ApiResponse;
import com.newwave.student_management.domains.auth.dto.request.AdminCreateUserRequest;
import com.newwave.student_management.domains.auth.dto.request.AdminUpdateUserProfileRequest;
import com.newwave.student_management.domains.auth.dto.request.AdminUpdateUserStatusRequest;
import com.newwave.student_management.domains.auth.dto.response.AdminUserDetailResponse;
import com.newwave.student_management.domains.auth.dto.response.AdminUserListResponse;
import com.newwave.student_management.domains.auth.entity.UserStatus;
import com.newwave.student_management.domains.auth.service.IAdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin - Users", description = "API quản lý tài khoản (Admin) - UC-11")
public class AdminUserController {

    private final IAdminUserService adminUserService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "UC-11.1 - Danh sách Users (Admin)",
            description = "Trả về danh sách user với phân trang, filter theo status, roleId và search theo email. " +
                    "Pattern: ?page=0&size=20&sort=createdAt,desc&search=keyword&status=ACTIVE&roleId=2"
    )
    public ApiResponse<AdminUserListResponse> getUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) Integer roleId,
            @ParameterObject Pageable pageable
    ) {
        AdminUserListResponse response = adminUserService.getUsers(search, status, roleId, pageable);
        return ApiResponse.success(response);
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "UC-11.2 - Xem chi tiết User (Admin)",
            description = "Trả về thông tin user và profile tương ứng (teacherProfile hoặc studentProfile theo role). " +
                    "404 nếu user không tồn tại hoặc đã bị xóa."
    )
    public ApiResponse<AdminUserDetailResponse> getUserById(@PathVariable UUID userId) {
        AdminUserDetailResponse response = adminUserService.getById(userId);
        return ApiResponse.success(response);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "UC-11.3a - Tạo User (Teacher hoặc Student)",
            description = "Tạo tài khoản mới với role TEACHER hoặc STUDENT. Password tự sinh, gửi welcome email với link kích hoạt (72h). " +
                    "User tạo xong có status PENDING_VERIFICATION. Không tạo tài khoản ADMIN."
    )
    public ResponseEntity<ApiResponse<AdminUserDetailResponse>> createUser(@Valid @RequestBody AdminCreateUserRequest request) {
        AdminUserDetailResponse response = adminUserService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PutMapping("/{userId}/profile")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "UC - Cập nhật profile User (Admin)",
            description = "Admin cập nhật các field của teacher/student profile. Không cho phép cập nhật: email, password, status, role."
    )
    public ApiResponse<AdminUserDetailResponse> updateUserProfile(
            @PathVariable UUID userId,
            @Valid @RequestBody AdminUpdateUserProfileRequest request
    ) {
        AdminUserDetailResponse response = adminUserService.updateUserProfile(userId, request);
        return ApiResponse.success(response);
    }

    @PatchMapping("/{userId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "UC-11.5 - Thay đổi trạng thái User (Block/Unblock)",
            description = "Admin thay đổi trạng thái user giữa ACTIVE / INACTIVE / BLOCKED, kèm banReason khi BLOCKED."
    )
    public ApiResponse<AdminUserDetailResponse> updateUserStatus(
            @PathVariable UUID userId,
            @Valid @RequestBody AdminUpdateUserStatusRequest request
    ) {
        AdminUserDetailResponse response = adminUserService.updateUserStatus(userId, request);
        return ApiResponse.success(response);
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "UC-11.7 - Soft delete User",
            description = "Admin soft delete user (set deleted_at) cùng với profile, force logout. Không được xóa chính mình."
    )
    public ApiResponse<Void> softDeleteUser(
            @PathVariable UUID userId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID actingAdminId = UUID.fromString(jwt.getClaim("userId"));
        adminUserService.softDeleteUser(userId, actingAdminId);
        return ApiResponse.success(null);
    }
}


