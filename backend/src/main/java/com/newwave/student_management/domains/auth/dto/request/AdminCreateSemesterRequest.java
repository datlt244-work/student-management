package com.newwave.student_management.domains.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AdminCreateSemesterRequest {
    @NotBlank(message = "Semester name is required")
    private String name;

    @NotNull(message = "Year is required")
    private Integer year;

    private LocalDate startDate;
    private LocalDate endDate;
}
