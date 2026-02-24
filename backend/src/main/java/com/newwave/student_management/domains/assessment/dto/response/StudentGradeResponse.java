package com.newwave.student_management.domains.assessment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentGradeResponse {
    private String courseCode;
    private String courseName;
    private Integer credits;
    private BigDecimal grade;
    private BigDecimal grade4;
    private String feedback;
}
