package com.newwave.student_management.domains.auth.service;

import com.newwave.student_management.domains.auth.dto.request.AdminCreateSemesterRequest;
import com.newwave.student_management.domains.auth.dto.request.AdminUpdateSemesterRequest;
import com.newwave.student_management.domains.auth.dto.response.AdminSemesterListResponse;
import com.newwave.student_management.domains.auth.dto.response.AdminSemesterResponse;
import org.springframework.data.domain.Pageable;

public interface AdminSemesterService {
    AdminSemesterListResponse getSemesters(Integer year, String name, Boolean isCurrent, Pageable pageable);
    AdminSemesterResponse createSemester(AdminCreateSemesterRequest request);
    AdminSemesterResponse updateSemester(Integer id, AdminUpdateSemesterRequest request);
    AdminSemesterResponse setCurrentSemester(Integer id);
    void deleteSemester(Integer id);
}
