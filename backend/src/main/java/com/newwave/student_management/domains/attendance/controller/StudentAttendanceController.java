package com.newwave.student_management.domains.attendance.controller;

import com.newwave.student_management.common.dto.ApiResponse;
import com.newwave.student_management.domains.attendance.dto.response.AttendanceRecordResponse;
import com.newwave.student_management.domains.attendance.service.IAttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/student/classes")
@RequiredArgsConstructor
@Tag(name = "Student Attendance", description = "Xem thông tin điểm danh của sinh viên")
@PreAuthorize("hasRole('STUDENT')")
public class StudentAttendanceController {

    private final IAttendanceService attendanceService;

    @GetMapping("/{classId}/attendances")
    @Operation(summary = "Lấy điểm danh của lớp", description = "Danh sách điểm danh của sinh viên cho 1 lớp học.")
    public ApiResponse<List<AttendanceRecordResponse>> getAttendances(
            @PathVariable Integer classId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getClaim("userId"));
        return ApiResponse.success(attendanceService.getStudentAttendances(userId, classId));
    }
}
