package com.newwave.student_management.domains.assessment.controller;

import com.newwave.student_management.common.dto.ApiResponse;
import com.newwave.student_management.domains.assessment.dto.response.ExamResponse;
import com.newwave.student_management.domains.assessment.service.IExamService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/student/exams")
@RequiredArgsConstructor
public class StudentExamController {

    private final IExamService examService;

    @GetMapping
    public ApiResponse<List<ExamResponse>> getMyExamSchedule(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(name = "semesterId") Integer semesterId) {
        UUID userId = UUID.fromString(jwt.getClaim("userId"));
        return ApiResponse.success(examService.getStudentExamSchedule(userId, semesterId));
    }
}
