package com.newwave.student_management.domains.enrollment.dto.response;

import com.newwave.student_management.domains.enrollment.entity.ScheduledClassStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminClassDetailResponse {
    private Integer classId;
    private String courseName;
    private String courseCode;
    private String teacherName;
    private String teacherId;
    private String semesterName;
    private String roomNumber;
    private String schedule;
    private Integer dayOfWeek;
    private String startTime;
    private String endTime;
    private ScheduledClassStatus status;
    private Integer maxStudents;
    private long studentCount;
    private List<AdminClassStudentResponse> students;
}
