package com.newwave.student_management.domains.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request body for POST /admin/courses — Create course.
 */
@Data
public class AdminCreateCourseRequest {

    @NotBlank(message = "Course name is required")
    @Size(max = 200, message = "Course name must be at most 200 characters")
    private String name;

    @NotBlank(message = "Course code is required")
    @Size(max = 20, message = "Course code must be at most 20 characters")
    private String code;

    @NotNull(message = "Credits is required")
    @Positive(message = "Credits must be positive")
    private Integer credits;

    @NotNull(message = "Department ID is required")
    private Integer departmentId;

    private String description;
}
