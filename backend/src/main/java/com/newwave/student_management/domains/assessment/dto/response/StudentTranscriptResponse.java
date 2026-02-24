package com.newwave.student_management.domains.assessment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentTranscriptResponse {
    private List<StudentGradeResponse> grades;
    private BigDecimal semesterGpa; // GPA của học kỳ đang xem (nếu có)
    private BigDecimal cumulativeGpa; // GPA tích lũy
    private Integer totalCredits;
}
