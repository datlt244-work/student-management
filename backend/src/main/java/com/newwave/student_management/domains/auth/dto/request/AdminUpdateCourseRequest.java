package com.newwave.student_management.domains.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class AdminUpdateCourseRequest {
    @NotBlank(message = "Course name is required")
    private String name;

    @NotBlank(message = "Course code is required")
    private String code;

    @NotNull(message = "Credits is required")
    @Positive(message = "Credits must be positive")
    private Integer credits;

    @NotNull(message = "Department ID is required")
    private Integer departmentId;

    private String description;
}
