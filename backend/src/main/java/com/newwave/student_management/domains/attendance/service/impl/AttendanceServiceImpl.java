package com.newwave.student_management.domains.attendance.service.impl;

import com.newwave.student_management.common.exception.AppException;
import com.newwave.student_management.common.exception.ErrorCode;
import com.newwave.student_management.domains.attendance.dto.response.AttendanceRecordResponse;
import com.newwave.student_management.domains.attendance.repository.AttendanceRepository;
import com.newwave.student_management.domains.attendance.service.IAttendanceService;
import com.newwave.student_management.domains.profile.entity.Student;
import com.newwave.student_management.domains.profile.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements IAttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final StudentRepository studentRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceRecordResponse> getStudentAttendances(UUID userId, Integer classId) {
        Student student = studentRepository.findByUser_UserIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_PROFILE_NOT_FOUND));

        return attendanceRepository.findByStudentStudentIdAndScheduledClassClassId(student.getStudentId(), classId)
                .stream()
                .map(att -> AttendanceRecordResponse.builder()
                        .date(att.getDate())
                        .status(att.getStatus())
                        .recordTime(att.getRecordTime())
                        .build())
                .collect(Collectors.toList());
    }
}
