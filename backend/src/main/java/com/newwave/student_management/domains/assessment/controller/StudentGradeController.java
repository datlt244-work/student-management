package com.newwave.student_management.domains.assessment.controller;

import com.newwave.student_management.common.dto.ApiResponse;
import com.newwave.student_management.domains.assessment.dto.response.StudentGradeResponse;
import com.newwave.student_management.domains.assessment.dto.response.StudentTranscriptResponse;
import com.newwave.student_management.domains.assessment.service.IGradeService;
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
@RequestMapping("/student/grades")
@RequiredArgsConstructor
@Tag(name = "Student Grades", description = "API cho sinh viên xem điểm và kết quả học tập")
@PreAuthorize("hasRole('STUDENT')")
public class StudentGradeController {

    private final IGradeService gradeService;

    @GetMapping
    @Operation(summary = "Xem điểm theo học kỳ", description = "Trả về danh sách điểm các môn học trong một học kỳ cụ thể.")
    public ApiResponse<List<StudentGradeResponse>> getGradesBySemester(
            @RequestParam Integer semesterId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getClaim("userId"));
        return ApiResponse.success(gradeService.getGradesBySemester(userId, semesterId));
    }

    @GetMapping("/transcript")
    @Operation(summary = "Xem bảng điểm tổng hợp", description = "Trả về toàn bộ điểm số tích lũy, GPA tổng kết và tổng số tín chỉ.")
    public ApiResponse<StudentTranscriptResponse> getTranscript(
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getClaim("userId"));
        return ApiResponse.success(gradeService.getTranscript(userId));
    }
}
