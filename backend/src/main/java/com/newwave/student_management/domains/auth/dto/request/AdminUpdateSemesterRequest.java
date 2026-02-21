package com.newwave.student_management.domains.auth.dto.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class AdminUpdateSemesterRequest {
    private LocalDate startDate;
    private LocalDate endDate;
}
