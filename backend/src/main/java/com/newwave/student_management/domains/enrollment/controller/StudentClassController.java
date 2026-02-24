package com.newwave.student_management.domains.enrollment.controller;

import com.newwave.student_management.common.dto.ApiResponse;
import com.newwave.student_management.domains.enrollment.dto.response.StudentAvailableClassResponse;
import com.newwave.student_management.domains.enrollment.service.IStudentClassService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/student/classes")
@RequiredArgsConstructor
@Tag(name = "Student Class", description = "API cho sinh viên đăng ký học phần")
public class StudentClassController {

    private final IStudentClassService studentClassService;

    @GetMapping("/available")
    @Operation(summary = "Lấy danh sách lớp có thể đăng ký", description = "Trả về danh sách lớp học thuộc khoa của sinh viên, trong học kỳ hiện tại, "
            +
            "có trạng thái OPEN và sinh viên chưa đăng ký.")
    public ApiResponse<List<StudentAvailableClassResponse>> getAvailableClasses(
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getClaim("userId"));
        return ApiResponse.success(studentClassService.getAvailableClasses(userId));
    }
}
