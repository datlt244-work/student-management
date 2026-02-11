package com.newwave.student_management.domains.auth.controller;

import com.newwave.student_management.common.dto.ApiResponse;
import com.newwave.student_management.domains.profile.dto.response.DepartmentResponse;
import com.newwave.student_management.domains.profile.entity.Department;
import com.newwave.student_management.domains.profile.repository.DepartmentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/departments")
@RequiredArgsConstructor
@Tag(name = "Admin - Departments", description = "API danh sách khoa (cho dropdown Admin)")
public class AdminDepartmentController {

    private final DepartmentRepository departmentRepository;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Danh sách Departments", description = "Trả về danh sách khoa (chưa xóa) để dùng cho select khi tạo/sửa user.")
    public ApiResponse<List<DepartmentResponse>> getDepartments() {
        List<Department> all = departmentRepository.findAll().stream()
                .filter(d -> d.getDeletedAt() == null)
                .toList();
        List<DepartmentResponse> result = all.stream()
                .map(DepartmentResponse::fromEntity)
                .toList();
        return ApiResponse.success(result);
    }
}
