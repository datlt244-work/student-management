package com.newwave.student_management.domains.enrollment.service;

import com.newwave.student_management.domains.enrollment.dto.response.StudentScheduleResponse;

import java.util.List;
import java.util.UUID;

public interface IScheduleService {
    List<StudentScheduleResponse> getStudentSchedule(UUID userId, Integer semesterId);
}
