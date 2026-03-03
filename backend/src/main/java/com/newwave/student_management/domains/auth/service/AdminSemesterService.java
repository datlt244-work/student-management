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

    /**
     * Publish semester: sync tất cả lớp OPEN lên Redis, đổi status thành PUBLISHED.
     */
    AdminSemesterResponse publishSemester(Integer id);

    /**
     * Đóng đăng ký: xóa cache Redis, đổi status thành CLOSED.
     */
    AdminSemesterResponse closeSemesterEnrollment(Integer id);
}
