package com.newwave.student_management.domains.attendance.service;

import com.newwave.student_management.domains.attendance.dto.response.AttendanceRecordResponse;

import java.util.List;
import java.util.UUID;

public interface IAttendanceService {
    List<AttendanceRecordResponse> getStudentAttendances(UUID studentId, Integer classId);

    // Optionally a method for teacher to record attendance, to be used later
    // void recordAttendance(Integer classId, LocalDate date, UUID studentId,
    // AttendanceStatus status);
}
