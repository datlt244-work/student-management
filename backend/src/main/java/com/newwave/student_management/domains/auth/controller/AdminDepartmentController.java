package com.newwave.student_management.domains.auth.controller;

import com.newwave.student_management.common.dto.ApiResponse;
import com.newwave.student_management.domains.auth.dto.response.AdminDepartmentListResponse;
import com.newwave.student_management.domains.auth.service.IAdminDepartmentService;
import com.newwave.student_management.domains.profile.dto.response.DepartmentResponse;
import com.newwave.student_management.domains.profile.entity.Department;
import com.newwave.student_management.domains.profile.repository.DepartmentRepository;
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

import java.util.List;

@RestController
@RequestMapping("/admin/departments")
@RequiredArgsConstructor
@Tag(name = "Admin - Departments", description = "API quản lý khoa (Admin) - UC-13")
public class AdminDepartmentController {

    private final IAdminDepartmentService adminDepartmentService;
    private final DepartmentRepository departmentRepository;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "UC-13.1 - Danh sách Khoa (Admin)",
            description = "Trả về danh sách khoa với phân trang, search theo tên. " +
                    "Pattern: ?page=0&size=20&sort=createdAt,desc&search=Computer"
    )
    public ApiResponse<AdminDepartmentListResponse> getDepartments(
            @RequestParam(required = false) String search,
            @ParameterObject Pageable pageable
    ) {
        AdminDepartmentListResponse response = adminDepartmentService.getDepartments(search, pageable);
        return ApiResponse.success(response);
    }

    @GetMapping("/simple")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Danh sách Departments (Simple)",
            description = "Trả về danh sách khoa đơn giản (chưa xóa) để dùng cho select khi tạo/sửa user. " +
                    "Endpoint này giữ lại để tương thích với frontend hiện tại."
    )
    public ApiResponse<List<DepartmentResponse>> getDepartmentsSimple() {
        List<Department> all = departmentRepository.findAll().stream()
                .filter(d -> d.getDeletedAt() == null)
                .toList();
        List<DepartmentResponse> result = all.stream()
                .map(DepartmentResponse::fromEntity)
                .toList();
        return ApiResponse.success(result);
    }
}
