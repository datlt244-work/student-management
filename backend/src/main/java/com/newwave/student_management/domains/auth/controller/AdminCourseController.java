package com.newwave.student_management.domains.auth.controller;

import com.newwave.student_management.common.dto.ApiResponse;
import com.newwave.student_management.domains.auth.dto.request.AdminCreateCourseRequest;
import com.newwave.student_management.domains.auth.dto.request.AdminUpdateCourseRequest;
import com.newwave.student_management.domains.auth.dto.response.AdminCourseListResponse;
import com.newwave.student_management.domains.auth.dto.response.AdminCourseDetailResponse;
import com.newwave.student_management.domains.auth.dto.response.AdminCourseListItemResponse;
import com.newwave.student_management.domains.auth.service.IAdminCourseService;
import com.newwave.student_management.domains.curriculum.entity.CourseStatus;
import com.newwave.student_management.common.exception.AppException;
import com.newwave.student_management.common.exception.ErrorCode;
import java.util.Map;
import jakarta.validation.Valid;
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

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new course")
    public ApiResponse<AdminCourseDetailResponse> createCourse(
            @RequestBody @Valid AdminCreateCourseRequest request
    ) {
        return ApiResponse.success(adminCourseService.createCourse(request));
    }

    @PatchMapping("/{courseId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update course status")
    public ApiResponse<AdminCourseListItemResponse> updateCourseStatus(
            @PathVariable Integer courseId,
            @RequestBody Map<String, String> request
    ) {
        String statusStr = request.get("status");
        if (statusStr == null) {
            throw new AppException(ErrorCode.VALIDATION_ERROR);
        }
        CourseStatus status;
        try {
            status = CourseStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
             throw new AppException(ErrorCode.VALIDATION_ERROR);
        }

        return ApiResponse.success(adminCourseService.updateCourseStatus(courseId, status));
    }

    @GetMapping("/{courseId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get course details")
    public ApiResponse<com.newwave.student_management.domains.auth.dto.response.AdminCourseDetailResponse> getCourseDetail(@PathVariable Integer courseId) {
        return ApiResponse.success(adminCourseService.getCourseDetail(courseId));
    }

    @PutMapping("/{courseId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update course details")
    public ApiResponse<AdminCourseDetailResponse> updateCourse(
            @PathVariable Integer courseId,
            @RequestBody @Valid AdminUpdateCourseRequest request
    ) {
        return ApiResponse.success(adminCourseService.updateCourse(courseId, request));
    }
    @DeleteMapping("/{courseId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete course (soft delete)")
    public ApiResponse<Void> deleteCourse(@PathVariable Integer courseId) {
        adminCourseService.deleteCourse(courseId);
        return ApiResponse.success(null);
    }
}
