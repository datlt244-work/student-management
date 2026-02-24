package com.newwave.student_management.domains.enrollment.service.impl;

import com.newwave.student_management.common.exception.AppException;
import com.newwave.student_management.common.exception.ErrorCode;
import com.newwave.student_management.domains.enrollment.dto.response.StudentAvailableClassResponse;
import com.newwave.student_management.domains.enrollment.dto.response.StudentEnrolledClassResponse;
import com.newwave.student_management.domains.enrollment.entity.Enrollment;
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

    @Override
    @Transactional(readOnly = true)
    public List<StudentEnrolledClassResponse> getEnrolledClasses(UUID userId) {
        Student student = studentRepository.findByUserIdWithDepartment(userId)
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_PROFILE_NOT_FOUND));

        Semester currentSemester = semesterRepository.findByIsCurrentTrue()
                .orElseThrow(() -> new AppException(ErrorCode.NO_CURRENT_SEMESTER));

        List<Enrollment> enrollments = enrollmentRepository
                .findByStudentStudentIdAndScheduledClassSemesterSemesterIdAndScheduledClassDeletedAtIsNull(
                        student.getStudentId(),
                        currentSemester.getSemesterId());

        return enrollments.stream()
                .map(this::mapToEnrolledClassResponse)
                .collect(Collectors.toList());
    }

    private StudentEnrolledClassResponse mapToEnrolledClassResponse(Enrollment enrollment) {
        ScheduledClass scheduledClass = enrollment.getScheduledClass();
        String teacherName = "N/A";
        if (scheduledClass.getTeacher() != null) {
            teacherName = (scheduledClass.getTeacher().getFirstName() + " " + scheduledClass.getTeacher().getLastName())
                    .trim();
        }

        return StudentEnrolledClassResponse.builder()
                .enrollmentId(enrollment.getEnrollmentId())
                .classId(scheduledClass.getClassId())
                .courseCode(scheduledClass.getCourse().getCode())
                .courseName(scheduledClass.getCourse().getName())
                .credits(scheduledClass.getCourse().getCredits())
                .teacherName(teacherName)
                .schedule(formatSchedule(scheduledClass))
                .roomNumber(scheduledClass.getRoomNumber())
                .enrollmentDate(enrollment.getEnrollmentDate())
                .build();
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

    @Override
    @Transactional
    public void enroll(UUID userId, Integer classId) {
        Student student = studentRepository.findByUserIdWithDepartment(userId)
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_PROFILE_NOT_FOUND));

        ScheduledClass scheduledClass = scheduledClassRepository.findById(classId)
                .orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_FOUND));

        // 1. Check if class is OPEN
        if (scheduledClass
                .getStatus() != com.newwave.student_management.domains.enrollment.entity.ScheduledClassStatus.OPEN) {
            throw new AppException(ErrorCode.CLASS_HAS_ENROLLED_STUDENTS); // Or a more specific code if available
        }

        // 2. Check if already enrolled in THIS class
        if (enrollmentRepository.existsByScheduledClassClassIdAndStudentStudentId(classId, student.getStudentId())) {
            throw new AppException(ErrorCode.STUDENT_ALREADY_ENROLLED);
        }

        // 3. Check if already enrolled in another class of the SAME COURSE in the SAME
        // SEMESTER
        if (enrollmentRepository
                .existsByStudentStudentIdAndScheduledClassCourseCourseIdAndScheduledClassSemesterSemesterId(
                        student.getStudentId(), scheduledClass.getCourse().getCourseId(),
                        scheduledClass.getSemester().getSemesterId())) {
            throw new AppException(ErrorCode.COURSE_ALREADY_ENROLLED);
        }

        // 4. Check capacity
        long currentEnrolled = enrollmentRepository.countByScheduledClassClassId(classId);
        if (currentEnrolled >= scheduledClass.getMaxStudents()) {
            throw new AppException(ErrorCode.CLASS_FULL);
        }

        // 5. Check schedule conflicts
        long conflicts = enrollmentRepository.countStudentConflicts(
                student.getStudentId(),
                scheduledClass.getSemester().getSemesterId(),
                scheduledClass.getDayOfWeek(),
                scheduledClass.getStartTime(),
                scheduledClass.getEndTime());

        if (conflicts > 0) {
            throw new AppException(ErrorCode.STUDENT_SCHEDULE_CONFLICT);
        }

        // 6. Create enrollment
        com.newwave.student_management.domains.enrollment.entity.Enrollment enrollment = new com.newwave.student_management.domains.enrollment.entity.Enrollment();
        enrollment.setStudent(student);
        enrollment.setScheduledClass(scheduledClass);
        enrollment.setEnrollmentDate(java.time.LocalDate.now());

        enrollmentRepository.save(enrollment);
    }

    @Override
    @Transactional
    public void unenroll(UUID userId, Integer classId) {
        Student student = studentRepository.findByUserIdWithDepartment(userId)
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_PROFILE_NOT_FOUND));

        // Find the enrollment record
        Enrollment enrollment = enrollmentRepository
                .findByScheduledClassClassIdAndStudentStudentId(classId, student.getStudentId())
                .orElseThrow(() -> new AppException(ErrorCode.ENROLLMENT_NOT_FOUND));

        // Verify the class belongs to the current semester
        Semester currentSemester = semesterRepository.findByIsCurrentTrue()
                .orElseThrow(() -> new AppException(ErrorCode.NO_CURRENT_SEMESTER));

        if (!enrollment.getScheduledClass().getSemester().getSemesterId()
                .equals(currentSemester.getSemesterId())) {
            throw new AppException(ErrorCode.SEMESTER_NOT_CURRENT);
        }

        enrollmentRepository.delete(enrollment);
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
