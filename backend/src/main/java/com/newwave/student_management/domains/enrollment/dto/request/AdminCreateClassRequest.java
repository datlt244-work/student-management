package com.newwave.student_management.domains.enrollment.dto.request;

import jakarta.validation.constraints.Min;
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
public class AdminCreateClassRequest {

    @NotNull(message = "Course is required")
    private Integer courseId;

    @NotBlank(message = "Teacher is required")
    private String teacherId; // UUID as String

    @NotNull(message = "Semester is required")
    private Integer semesterId;

    @NotNull(message = "Sessions are required")
    private java.util.List<ClassSessionRequest> sessions;

    @NotNull(message = "Max students is required")
    @Min(value = 1, message = "Max students must be at least 1")
    private Integer maxStudents;
}
