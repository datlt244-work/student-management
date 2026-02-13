package com.newwave.student_management.domains.auth.controller;

import com.newwave.student_management.common.dto.ApiResponse;
import com.newwave.student_management.domains.auth.dto.request.AdminCreateDepartmentRequest;
import com.newwave.student_management.domains.auth.dto.request.AdminUpdateDepartmentRequest;
import com.newwave.student_management.domains.auth.dto.response.AdminDepartmentDetailResponse;
import com.newwave.student_management.domains.auth.dto.response.AdminDepartmentListResponse;
import com.newwave.student_management.domains.auth.service.IAdminDepartmentService;
import com.newwave.student_management.domains.profile.dto.response.DepartmentResponse;
import com.newwave.student_management.domains.profile.entity.Department;
import com.newwave.student_management.domains.profile.repository.DepartmentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

import com.newwave.student_management.domains.profile.entity.DepartmentStatus;

import java.util.List;
import java.util.Map;

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

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "UC-13.2 - Tạo Khoa (Admin)",
            description = "Tạo khoa mới. Validation: name required (max 100, unique case-insensitive), officeLocation optional (max 100)."
    )
    public ApiResponse<AdminDepartmentDetailResponse> createDepartment(
            @Valid @RequestBody AdminCreateDepartmentRequest request
    ) {
        AdminDepartmentDetailResponse response = adminDepartmentService.createDepartment(request);
        return ApiResponse.success(response);
    }

    @PutMapping("/{departmentId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "UC-13.3 - Cập Nhật Khoa (Admin)",
            description = "Cập nhật thông tin khoa. Validation: name optional (max 100, unique case-insensitive), officeLocation optional (max 100)."
    )
    public ApiResponse<AdminDepartmentDetailResponse> updateDepartment(
            @PathVariable Integer departmentId,
            @Valid @RequestBody AdminUpdateDepartmentRequest request
    ) {
        AdminDepartmentDetailResponse response = adminDepartmentService.updateDepartment(departmentId, request);
        return ApiResponse.success(response);
    }

    @PatchMapping("/{departmentId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update Department Status (Active/Inactive)")
    public ApiResponse<AdminDepartmentDetailResponse> updateDepartmentStatus(
            @PathVariable Integer departmentId,
            @RequestBody Map<String, String> request
    ) {
        String statusStr = request.get("status");
        if (statusStr == null) {
            throw new com.newwave.student_management.common.exception.AppException(com.newwave.student_management.common.exception.ErrorCode.VALIDATION_ERROR);
        }
        DepartmentStatus status;
        try {
            status = DepartmentStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new com.newwave.student_management.common.exception.AppException(com.newwave.student_management.common.exception.ErrorCode.VALIDATION_ERROR);
        }

        AdminDepartmentDetailResponse response = adminDepartmentService.updateDepartmentStatus(departmentId, status);
        return ApiResponse.success(response);
    }

    @DeleteMapping("/{departmentId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "UC-13.4 - Xóa Khoa (Soft Delete)",
            description = "Xóa mềm khoa. Không cho phép xóa nếu còn teacher/student active thuộc khoa này."
    )
    public ResponseEntity<ApiResponse<Void>> deleteDepartment(
            @PathVariable Integer departmentId
    ) {
        adminDepartmentService.deleteDepartment(departmentId);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(null));
    }

    @GetMapping("/simple")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Danh sách Departments (Simple)",
            description = "Trả về danh sách khoa đơn giản (chưa xóa) để dùng cho select khi tạo/sửa user. " +
                    "Endpoint này giữ lại để tương thích với frontend hiện tại."
    )
    public ApiResponse<List<DepartmentResponse>> getDepartmentsSimple() {
        List<Department> all = departmentRepository.findAllByStatusAndDeletedAtIsNull(DepartmentStatus.ACTIVE);
        List<DepartmentResponse> result = all.stream()
                .map(DepartmentResponse::fromEntity)
                .toList();
        return ApiResponse.success(result);
    }
}
