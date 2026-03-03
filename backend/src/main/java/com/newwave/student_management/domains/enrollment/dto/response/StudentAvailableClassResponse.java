package com.newwave.student_management.domains.enrollment.dto.response;

import com.newwave.student_management.domains.enrollment.entity.ScheduledClassStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentAvailableClassResponse {
    private Integer classId;
    private String courseCode;
    private String courseName;
    private Integer credits;
    private String teacherName;
    private java.util.List<ClassSessionResponse> sessions;
    private Integer maxStudents;
    private Integer currentStudents;
    private ScheduledClassStatus status;
}
