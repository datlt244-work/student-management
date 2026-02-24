package com.newwave.student_management.domains.enrollment.controller;

import com.newwave.student_management.common.dto.ApiResponse;
import com.newwave.student_management.domains.enrollment.dto.request.AdminCreateClassRequest;
import com.newwave.student_management.domains.enrollment.dto.request.AdminUpdateClassRequest;
import com.newwave.student_management.domains.enrollment.dto.response.AdminClassDetailResponse;
import com.newwave.student_management.domains.enrollment.dto.response.AdminClassListItemResponse;
import com.newwave.student_management.domains.enrollment.dto.response.AdminClassListResponse;
import com.newwave.student_management.domains.enrollment.entity.ScheduledClassStatus;
import com.newwave.student_management.domains.enrollment.service.IAdminClassService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/classes")
@RequiredArgsConstructor
@Tag(name = "Admin - Classes", description = "API quản lý lớp học (Admin) - UC-14")
public class AdminClassController {

    private final IAdminClassService adminClassService;

    @Operation(summary = "UC-14.1 - Danh sách Lớp học (Admin)", description = "Trả về danh sách lớp học với phân trang, filter theo status, semesterId và search theo Course Name/Code hoặc Teacher Name.")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminClassListResponse> getClasses(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ScheduledClassStatus status,
            @RequestParam(required = false) Integer semesterId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(adminClassService.getAdminClasses(search, status, semesterId, pageable));
    }

    @Operation(summary = "UC-14.2 - Tạo Lớp học mới (Admin)", description = "Tạo một lớp học mới với course, teacher, semester, room, schedule và maxStudents.")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminClassListItemResponse> createClass(@RequestBody @Valid AdminCreateClassRequest request) {
        return ApiResponse.success(adminClassService.createClass(request));
    }

    @Operation(summary = "UC-14.4 - Chi tiết Lớp học (Admin)", description = "Trả về thông tin chi tiết lớp học và danh sách sinh viên.")
    @GetMapping("/{classId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminClassDetailResponse> getClassDetail(@PathVariable Integer classId) {
        return ApiResponse.success(adminClassService.getClassDetail(classId));
    }

    @Operation(summary = "UC-14.3 - Chỉnh sửa Lớp học (Admin)", description = "Cập nhật thông tin lớp học hiện có.")
    @PutMapping("/{classId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminClassListItemResponse> updateClass(
            @PathVariable Integer classId,
            @RequestBody @Valid AdminUpdateClassRequest request) {
        return ApiResponse.success(adminClassService.updateClass(classId, request));
    }

    @Operation(summary = "UC-14.6 - Xóa Lớp học (Admin)", description = "Xóa mềm lớp học nếu không có sinh viên.")
    @DeleteMapping("/{classId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteClass(@PathVariable Integer classId) {
        adminClassService.deleteClass(classId);
        return ApiResponse.success(null);
    }
}
