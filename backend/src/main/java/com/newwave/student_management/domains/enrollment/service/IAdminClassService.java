package com.newwave.student_management.domains.enrollment.service;

import com.newwave.student_management.domains.enrollment.dto.request.AdminCreateClassRequest;
import com.newwave.student_management.domains.enrollment.dto.request.AdminEnrollStudentRequest;
import com.newwave.student_management.domains.enrollment.dto.request.AdminUpdateClassRequest;
import com.newwave.student_management.domains.enrollment.dto.response.AdminClassDetailResponse;
import com.newwave.student_management.domains.enrollment.dto.response.AdminClassListItemResponse;
import com.newwave.student_management.domains.enrollment.dto.response.AdminClassListResponse;
import com.newwave.student_management.domains.enrollment.dto.response.AdminEligibleStudentResponse;
import com.newwave.student_management.domains.enrollment.entity.ScheduledClassStatus;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface IAdminClassService {
    AdminClassListResponse getAdminClasses(
            String search,
            ScheduledClassStatus status,
            Integer semesterId,
            Pageable pageable);

    AdminClassListItemResponse createClass(AdminCreateClassRequest request);

    AdminClassListItemResponse updateClass(Integer classId, AdminUpdateClassRequest request);

    AdminClassDetailResponse getClassDetail(Integer classId);

    void deleteClass(Integer classId);

    void enrollStudent(AdminEnrollStudentRequest request);

    void unenrollStudent(Integer classId, UUID studentId);

    List<AdminEligibleStudentResponse> getEligibleStudents(Integer classId);
}
