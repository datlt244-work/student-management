package com.newwave.student_management.domains.enrollment.service;

import com.newwave.student_management.domains.enrollment.dto.request.AdminCreateClassRequest;
import com.newwave.student_management.domains.enrollment.dto.response.AdminClassListItemResponse;
import com.newwave.student_management.domains.enrollment.dto.response.AdminClassListResponse;
import com.newwave.student_management.domains.enrollment.entity.ScheduledClassStatus;
import org.springframework.data.domain.Pageable;

public interface IAdminClassService {
    AdminClassListResponse getAdminClasses(
            String search,
            ScheduledClassStatus status,
            Integer semesterId,
            Pageable pageable);

    AdminClassListItemResponse createClass(AdminCreateClassRequest request);
}
