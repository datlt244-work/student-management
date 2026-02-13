package com.newwave.student_management.domains.auth.controller;

import com.newwave.student_management.common.dto.ApiResponse;
import com.newwave.student_management.domains.auth.dto.response.AdminCourseListResponse;
import com.newwave.student_management.domains.auth.service.IAdminCourseService;
import com.newwave.student_management.domains.curriculum.entity.CourseStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/courses")
@RequiredArgsConstructor
@Tag(name = "Admin - Courses", description = "APIs for managing courses (Admin)")
public class AdminCourseController {

    private final IAdminCourseService adminCourseService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get list of courses with filtering and pagination")
    public ApiResponse<AdminCourseListResponse> getCourses(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) CourseStatus status,
            @RequestParam(required = false) Integer departmentId,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ApiResponse.success(adminCourseService.getCourses(search, status, departmentId, pageable));
    }
}
