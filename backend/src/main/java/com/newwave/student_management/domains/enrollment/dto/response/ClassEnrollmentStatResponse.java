package com.newwave.student_management.domains.enrollment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassEnrollmentStatResponse {
    private Integer classId;
    private String courseCode;
    private String courseName;
    private String teacherName;
    private int maxSlot;
    private int currentSlot;
    private String fillRate;
    private String status;
}
