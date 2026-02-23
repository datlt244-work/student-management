package com.newwave.student_management.domains.enrollment.controller;

import com.newwave.student_management.common.dto.ApiResponse;
import com.newwave.student_management.domains.enrollment.dto.request.AdminCreateClassRequest;
import com.newwave.student_management.domains.enrollment.dto.response.AdminClassListItemResponse;
import com.newwave.student_management.domains.enrollment.dto.response.AdminClassListResponse;
import com.newwave.student_management.domains.enrollment.entity.ScheduledClassStatus;
import com.newwave.student_management.domains.enrollment.service.IAdminClassService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/classes")
@RequiredArgsConstructor
@Tag(name = "Admin - Classes", description = "API quản lý lớp học (Admin) - UC-14")
public class AdminClassController {

    private final IAdminClassService adminClassService;

    @Operation(summary = "UC-14.1 - Danh sách Lớp học (Admin)", description = "Trả về danh sách lớp học với phân trang, filter theo status, semesterId và search theo Course Name/Code hoặc Teacher Name.")
    @GetMapping
    public ApiResponse<AdminClassListResponse> getClasses(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ScheduledClassStatus status,
            @RequestParam(required = false) Integer semesterId,
            @PageableDefault(size = 10, sort = "createdAt,desc") Pageable pageable) {
        return ApiResponse.success(adminClassService.getAdminClasses(search, status, semesterId, pageable));
    }

    @Operation(summary = "UC-14.2 - Tạo Lớp học mới (Admin)", description = "Tạo một lớp học mới với course, teacher, semester, room, schedule và maxStudents.")
    @PostMapping
    public ApiResponse<AdminClassListItemResponse> createClass(@RequestBody @Valid AdminCreateClassRequest request) {
        return ApiResponse.success(adminClassService.createClass(request));
    }
}
