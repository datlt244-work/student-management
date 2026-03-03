package com.newwave.student_management.domains.enrollment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentEnrolledClassResponse {
    private Integer enrollmentId;
    private Integer classId;
    private String courseCode;
    private String courseName;
    private Integer credits;
    private String teacherName;
    private java.util.List<ClassSessionResponse> sessions;
    private LocalDate enrollmentDate;
}
