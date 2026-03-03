package com.newwave.student_management.domains.enrollment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentStatsResponse {
    private int totalClasses;
    private int totalSlots;
    private int filledSlots;
    private String fillRate;
    private boolean cacheActive;
    private List<ClassEnrollmentStatResponse> classes;
}
