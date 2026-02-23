package com.newwave.student_management.domains.enrollment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminClassStudentResponse {
    private Integer enrollmentId;
    private String studentId;
    private String studentCode;
    private String fullName;
    private String email;
    private LocalDate enrollmentDate;
}
