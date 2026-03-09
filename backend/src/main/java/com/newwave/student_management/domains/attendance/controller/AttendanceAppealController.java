package com.newwave.student_management.domains.attendance.controller;

import com.newwave.student_management.common.dto.ApiResponse;
import com.newwave.student_management.domains.attendance.dto.request.AttendanceAppealRequest;
import com.newwave.student_management.domains.attendance.dto.response.AttendanceAppealResponse;
import com.newwave.student_management.domains.attendance.dto.response.EvidenceUploadResponse;
import com.newwave.student_management.domains.attendance.service.IAttendanceAppealService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/student/attendance-appeals")
@RequiredArgsConstructor
public class AttendanceAppealController {

    private final IAttendanceAppealService appealService;

    @PostMapping
    public ApiResponse<AttendanceAppealResponse> submitAppeal(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AttendanceAppealRequest request) {
        UUID userId = UUID.fromString(jwt.getClaim("userId"));
        return ApiResponse.success(appealService.submitAppeal(userId, request));
    }

    @GetMapping
    public ApiResponse<List<AttendanceAppealResponse>> getStudentAppeals(
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getClaim("userId"));
        return ApiResponse.success(appealService.getStudentAppeals(userId));
    }

    @PostMapping(value = "/upload-evidence", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<EvidenceUploadResponse> uploadEvidence(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("file") MultipartFile file) {
        UUID userId = UUID.fromString(jwt.getClaim("userId"));
        return ApiResponse.success(appealService.uploadEvidence(userId, file));
    }
}
