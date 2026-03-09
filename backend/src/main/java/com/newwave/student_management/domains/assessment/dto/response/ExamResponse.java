package com.newwave.student_management.domains.assessment.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamResponse {
    private UUID examId;
    private Integer classId;
    private String courseCode;
    private String courseName;
    private String roomNumber;
    private LocalDate examDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String examType;
    private String note;

    // Semester dates for UI validation
    private LocalDate semesterStartDate;
    private LocalDate semesterEndDate;

    // Eligibility status
    @JsonProperty("isEligible")
    private boolean isEligible;
    private Integer totalAbsent;
    private Double absentPercentage;
    private String reason; // If not eligible
}
