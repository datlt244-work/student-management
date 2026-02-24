package com.newwave.student_management.domains.enrollment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminEnrollStudentRequest {

    @NotNull(message = "Class ID is required")
    private Integer classId;

    @NotBlank(message = "Student ID is required")
    private String studentId;
}
