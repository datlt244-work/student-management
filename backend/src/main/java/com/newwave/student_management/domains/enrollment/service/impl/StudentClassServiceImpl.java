package com.newwave.student_management.domains.enrollment.service.impl;

import com.newwave.student_management.common.exception.AppException;
import com.newwave.student_management.common.exception.ErrorCode;
import com.newwave.student_management.domains.enrollment.dto.response.StudentAvailableClassResponse;
import com.newwave.student_management.domains.enrollment.entity.ScheduledClass;
import com.newwave.student_management.domains.enrollment.repository.EnrollmentRepository;
import com.newwave.student_management.domains.enrollment.repository.ScheduledClassRepository;
import com.newwave.student_management.domains.enrollment.service.IStudentClassService;
import com.newwave.student_management.domains.profile.entity.Semester;
import com.newwave.student_management.domains.profile.entity.Student;
import com.newwave.student_management.domains.profile.repository.SemesterRepository;
import com.newwave.student_management.domains.profile.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentClassServiceImpl implements IStudentClassService {

    private final ScheduledClassRepository scheduledClassRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final SemesterRepository semesterRepository;

    @Override
    @Transactional(readOnly = true)
    public List<StudentAvailableClassResponse> getAvailableClasses(UUID userId) {
        Student student = studentRepository.findByUserIdWithDepartment(userId)
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_PROFILE_NOT_FOUND));

        Semester currentSemester = semesterRepository.findByIsCurrentTrue()
                .orElseThrow(() -> new AppException(ErrorCode.NO_CURRENT_SEMESTER));

        Integer departmentId = student.getDepartment() != null ? student.getDepartment().getDepartmentId() : null;
        if (departmentId == null) {
            return List.of();
        }

        List<ScheduledClass> availableClasses = scheduledClassRepository.findAvailableClassesForStudent(
                departmentId,
                currentSemester.getSemesterId(),
                student.getStudentId());

        return availableClasses.stream()
                .map(this::mapToAvailableClassResponse)
                .collect(Collectors.toList());
    }

    private StudentAvailableClassResponse mapToAvailableClassResponse(ScheduledClass scheduledClass) {
        String teacherName = "N/A";
        if (scheduledClass.getTeacher() != null) {
            teacherName = (scheduledClass.getTeacher().getFirstName() + " " + scheduledClass.getTeacher().getLastName())
                    .trim();
        }

        long currentStudents = enrollmentRepository.countByScheduledClassClassId(scheduledClass.getClassId());

        return StudentAvailableClassResponse.builder()
                .classId(scheduledClass.getClassId())
                .courseCode(scheduledClass.getCourse().getCode())
                .courseName(scheduledClass.getCourse().getName())
                .credits(scheduledClass.getCourse().getCredits())
                .teacherName(teacherName)
                .schedule(formatSchedule(scheduledClass))
                .roomNumber(scheduledClass.getRoomNumber())
                .maxStudents(scheduledClass.getMaxStudents())
                .currentStudents((int) currentStudents)
                .status(scheduledClass.getStatus())
                .build();
    }

    private String formatSchedule(ScheduledClass scheduledClass) {
        if (scheduledClass.getDayOfWeek() == null || scheduledClass.getStartTime() == null
                || scheduledClass.getEndTime() == null) {
            return "N/A";
        }
        String day = switch (scheduledClass.getDayOfWeek()) {
            case 1 -> "Mon";
            case 2 -> "Tue";
            case 3 -> "Wed";
            case 4 -> "Thu";
            case 5 -> "Fri";
            case 6 -> "Sat";
            case 7 -> "Sun";
            default -> "N/A";
        };
        return String.format("%s %s-%s", day, scheduledClass.getStartTime(), scheduledClass.getEndTime());
    }
}
