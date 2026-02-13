package com.newwave.student_management.domains.auth.service;

import com.newwave.student_management.domains.auth.dto.request.AdminCreateDepartmentRequest;
import com.newwave.student_management.domains.auth.dto.request.AdminUpdateDepartmentRequest;
import com.newwave.student_management.domains.auth.dto.response.AdminDepartmentDetailResponse;
import com.newwave.student_management.domains.auth.dto.response.AdminDepartmentListResponse;
import org.springframework.data.domain.Pageable;

public interface IAdminDepartmentService {

    AdminDepartmentListResponse getDepartments(String search, com.newwave.student_management.domains.profile.entity.DepartmentStatus status, Pageable pageable);

    AdminDepartmentDetailResponse createDepartment(AdminCreateDepartmentRequest request);

    AdminDepartmentDetailResponse updateDepartment(Integer departmentId, AdminUpdateDepartmentRequest request);

    AdminDepartmentDetailResponse updateDepartmentStatus(Integer departmentId, com.newwave.student_management.domains.profile.entity.DepartmentStatus status);

    void deleteDepartment(Integer departmentId);
}

