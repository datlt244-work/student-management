package com.newwave.student_management.domains.enrollment.service.impl;

import com.newwave.student_management.common.exception.AppException;
import com.newwave.student_management.common.exception.ErrorCode;
import com.newwave.student_management.domains.enrollment.dto.response.StudentScheduleResponse;
import com.newwave.student_management.domains.enrollment.entity.Enrollment;
import com.newwave.student_management.domains.enrollment.repository.EnrollmentRepository;
import com.newwave.student_management.domains.enrollment.service.IScheduleService;
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
public class ScheduleServiceImpl implements IScheduleService {

        private final EnrollmentRepository enrollmentRepository;
        private final StudentRepository studentRepository;

        @Override
        @Transactional(readOnly = true)
        public List<StudentScheduleResponse> getStudentSchedule(UUID userId, Integer semesterId) {
                Student student = studentRepository.findByUser_UserIdAndDeletedAtIsNull(userId)
                                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_PROFILE_NOT_FOUND));

                List<Enrollment> enrollments = enrollmentRepository
                                .findByStudentStudentIdAndScheduledClassSemesterSemesterIdAndScheduledClassDeletedAtIsNull(
                                                student.getStudentId(), semesterId);

                return enrollments.stream()
                                .map(enrollment -> StudentScheduleResponse.builder()
                                                .courseCode(enrollment.getScheduledClass().getCourse().getCode())
                                                .courseName(enrollment.getScheduledClass().getCourse().getName())
                                                .teacherName(enrollment.getScheduledClass().getTeacher() != null
                                                                ? enrollment.getScheduledClass().getTeacher()
                                                                                .getFirstName() + " "
                                                                                + enrollment.getScheduledClass()
                                                                                                .getTeacher()
                                                                                                .getLastName()
                                                                : "TBA")
                                                .roomNumber(enrollment.getScheduledClass().getRoomNumber())
                                                .dayOfWeek(enrollment.getScheduledClass().getDayOfWeek())
                                                .startTime(enrollment.getScheduledClass().getStartTime())
                                                .endTime(enrollment.getScheduledClass().getEndTime())
                                                .classStatus(enrollment.getScheduledClass().getStatus().name())
                                                .build())
                                .collect(Collectors.toList());
        }
}
