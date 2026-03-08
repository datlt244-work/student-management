package com.newwave.student_management.domains.assessment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentGradeResponse {
    private Integer enrollmentId;
    private String semesterName;
    private Integer semesterYear;
    private String courseCode;
    private String courseName;
    private String classCode;
    private LocalDate fromDate;
    private LocalDate toDate;
    private Integer credits;
    private BigDecimal grade;
    private BigDecimal grade4;
    private String status; // PASSED / FAILED
    private String feedback;
    private List<StudentAssessmentScoreResponse> assessmentScores;
}
