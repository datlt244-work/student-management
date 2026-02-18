package com.newwave.student_management.domains.auth.service;

import com.newwave.student_management.domains.auth.dto.response.AdminCourseDetailResponse;
import com.newwave.student_management.domains.auth.dto.response.AdminCourseListItemResponse;
import com.newwave.student_management.domains.auth.dto.response.AdminCourseListResponse;
import com.newwave.student_management.domains.curriculum.entity.CourseStatus;
import org.springframework.data.domain.Pageable;

public interface IAdminCourseService {
    AdminCourseListResponse getCourses(String search, CourseStatus status, Integer departmentId, Pageable pageable);
    
    AdminCourseListItemResponse updateCourseStatus(Integer courseId, CourseStatus status);

   AdminCourseDetailResponse getCourseDetail(Integer courseId);
}
