package com.newwave.student_management.domains.enrollment.controller;

import com.newwave.student_management.common.dto.ApiResponse;
import com.newwave.student_management.domains.enrollment.dto.response.StudentScheduleResponse;
import com.newwave.student_management.domains.enrollment.service.IScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/student/schedule")
@RequiredArgsConstructor
@Tag(name = "Student Schedule", description = "API cho sinh viên xem thời khóa biểu")
@PreAuthorize("hasRole('STUDENT')")
public class StudentScheduleController {

    private final IScheduleService scheduleService;

    @GetMapping
    @Operation(summary = "Xem thời khóa biểu", description = "Trả về danh sách các tiết học của sinh viên trong một học kỳ.")
    public ApiResponse<List<StudentScheduleResponse>> getMySchedule(
            @RequestParam Integer semesterId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getClaim("userId"));
        return ApiResponse.success(scheduleService.getStudentSchedule(userId, semesterId));
    }
}
