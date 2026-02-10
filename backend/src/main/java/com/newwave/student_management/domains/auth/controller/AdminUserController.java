package com.newwave.student_management.domains.auth.controller;

import com.newwave.student_management.common.dto.ApiResponse;
import com.newwave.student_management.domains.auth.dto.response.AdminUserListResponse;
import com.newwave.student_management.domains.auth.entity.UserStatus;
import com.newwave.student_management.domains.auth.service.IAdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin - Users", description = "API quản lý tài khoản (Admin) - UC-11.1 List users")
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
}


