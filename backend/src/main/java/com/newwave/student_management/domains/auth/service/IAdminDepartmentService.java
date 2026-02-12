package com.newwave.student_management.domains.auth.service;

import com.newwave.student_management.domains.auth.dto.response.AdminDepartmentListResponse;
import org.springframework.data.domain.Pageable;

public interface IAdminDepartmentService {

    AdminDepartmentListResponse getDepartments(String search, Pageable pageable);
}

