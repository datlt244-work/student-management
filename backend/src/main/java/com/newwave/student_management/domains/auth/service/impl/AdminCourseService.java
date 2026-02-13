package com.newwave.student_management.domains.auth.service.impl;

import com.newwave.student_management.common.util.PaginationUtil;
import com.newwave.student_management.domains.auth.dto.response.AdminCourseListItemResponse;
import com.newwave.student_management.domains.auth.dto.response.AdminCourseListResponse;
import com.newwave.student_management.domains.auth.service.IAdminCourseService;
import com.newwave.student_management.domains.curriculum.entity.Course;
import com.newwave.student_management.domains.curriculum.entity.CourseStatus;
import com.newwave.student_management.domains.curriculum.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminCourseService implements IAdminCourseService {

    private final CourseRepository courseRepository;

    @Override
    public AdminCourseListResponse getCourses(String search, CourseStatus status, Integer departmentId, Pageable pageable) {
        String normalizedSearch = PaginationUtil.normalizeSearch(search);
        
        Page<Course> page = courseRepository.searchAdminCourses(normalizedSearch, status, departmentId, pageable);
        
        List<AdminCourseListItemResponse> items = page.getContent().stream()
                .map(this::toListItem)
                .toList();

        PaginationUtil.PaginationMetadata meta = PaginationUtil.extractMetadata(page);

        return AdminCourseListResponse.builder()
                .content(items)
                .page(meta.page)
                .size(meta.size)
                .totalElements(meta.totalElements)
                .totalPages(meta.totalPages)
                .build();
    }

    private AdminCourseListItemResponse toListItem(Course c) {
        return AdminCourseListItemResponse.builder()
                .courseId(c.getCourseId())
                .code(c.getCode())
                .name(c.getName())
                .credits(c.getCredits())
                .departmentId(c.getDepartment() != null ? c.getDepartment().getDepartmentId() : null)
                .departmentName(c.getDepartment() != null ? c.getDepartment().getName() : null)
                .status(c.getStatus().name())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
