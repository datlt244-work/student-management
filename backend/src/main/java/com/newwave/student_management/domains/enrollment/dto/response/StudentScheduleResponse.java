package com.newwave.student_management.domains.enrollment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentScheduleResponse {
    private String courseCode;
    private String courseName;
    private String teacherName;
    private String roomNumber;
    private Integer dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private String classStatus;
}
