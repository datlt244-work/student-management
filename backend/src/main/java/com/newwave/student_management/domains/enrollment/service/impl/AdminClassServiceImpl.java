package com.newwave.student_management.domains.enrollment.service.impl;

import com.newwave.student_management.common.util.PaginationUtil;
import com.newwave.student_management.common.exception.AppException;
import com.newwave.student_management.common.exception.ErrorCode;
import com.newwave.student_management.domains.curriculum.entity.Course;
import com.newwave.student_management.domains.curriculum.repository.CourseRepository;
import com.newwave.student_management.domains.enrollment.dto.request.AdminCreateClassRequest;
import com.newwave.student_management.domains.enrollment.dto.request.AdminUpdateClassRequest;
import com.newwave.student_management.domains.enrollment.dto.response.AdminClassDetailResponse;
import com.newwave.student_management.domains.enrollment.dto.response.AdminClassListItemResponse;
import com.newwave.student_management.domains.enrollment.dto.response.AdminClassListResponse;
import com.newwave.student_management.domains.enrollment.dto.response.AdminClassStudentResponse;
import com.newwave.student_management.domains.enrollment.entity.Enrollment;
import com.newwave.student_management.domains.enrollment.entity.ScheduledClass;
import com.newwave.student_management.domains.enrollment.entity.ScheduledClassStatus;
import com.newwave.student_management.domains.enrollment.repository.EnrollmentRepository;
import com.newwave.student_management.domains.enrollment.repository.ScheduledClassRepository;
import com.newwave.student_management.domains.enrollment.service.IAdminClassService;
import com.newwave.student_management.domains.profile.entity.Semester;
import com.newwave.student_management.domains.profile.entity.Teacher;
import com.newwave.student_management.domains.profile.repository.SemesterRepository;
import com.newwave.student_management.domains.profile.repository.TeacherRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminClassServiceImpl implements IAdminClassService {

    private final ScheduledClassRepository scheduledClassRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final TeacherRepository teacherRepository;
    private final SemesterRepository semesterRepository;

    @Override
    public AdminClassListResponse getAdminClasses(
            String search,
            ScheduledClassStatus status,
            Integer semesterId,
            Pageable pageable) {
        Specification<ScheduledClass> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            Join<ScheduledClass, Course> courseJoin = root.join("course", JoinType.INNER);

            if (search != null && !search.isBlank()) {
                String searchLower = "%" + search.toLowerCase() + "%";

                Predicate courseName = cb.like(cb.lower(courseJoin.get("name")), searchLower);
                Predicate courseCode = cb.like(cb.lower(courseJoin.get("code")), searchLower);

                // Join with Teacher (Optional)
                Join<ScheduledClass, Teacher> teacherJoin = root.join("teacher", JoinType.LEFT);
                Predicate teacherFirstName = cb.like(cb.lower(teacherJoin.get("firstName")), searchLower);
                Predicate teacherLastName = cb.like(cb.lower(teacherJoin.get("lastName")), searchLower);

                predicates.add(cb.or(courseName, courseCode, teacherFirstName, teacherLastName));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (semesterId != null) {
                predicates.add(cb.equal(root.get("semester").get("semesterId"), semesterId));
            }

            // Always filter out deleted classes
            predicates.add(cb.isNull(root.get("deletedAt")));

            // Filter classes with ACTIVE courses only
            predicates.add(cb.equal(courseJoin.get("status"),
                    com.newwave.student_management.domains.curriculum.entity.CourseStatus.ACTIVE));

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<ScheduledClass> pageResult = scheduledClassRepository.findAll(spec, pageable);

        List<AdminClassListItemResponse> content = pageResult.getContent().stream()
                .map(this::mapToListItemResponse)
                .toList();

        PaginationUtil.PaginationMetadata metadata = PaginationUtil.extractMetadata(pageResult);

        return AdminClassListResponse.builder()
                .content(content)
                .page(metadata.page)
                .size(metadata.size)
                .totalElements(metadata.totalElements)
                .totalPages(metadata.totalPages)
                .build();
    }

    @Override
    @Transactional
    public AdminClassListItemResponse createClass(AdminCreateClassRequest request) {
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_FOUND));

        // Always use CURRENT semester for new classes
        Semester currentSemester = semesterRepository.findByIsCurrentTrue()
                .orElseThrow(() -> new AppException(ErrorCode.NO_CURRENT_SEMESTER));

        // Conflict Check (Mandatory Teacher Check)
        if (request.getTeacherId() == null || request.getTeacherId().isBlank()) {
            throw new AppException(ErrorCode.VALIDATION_ERROR);
        }

        Teacher teacher = teacherRepository.findById(UUID.fromString(request.getTeacherId()))
                .orElseThrow(() -> new AppException(ErrorCode.TEACHER_PROFILE_NOT_FOUND));

        // Validation: Teacher must be in the same department as the course
        if (course.getDepartment() != null && teacher.getDepartment() != null) {
            if (!course.getDepartment().getDepartmentId().equals(teacher.getDepartment().getDepartmentId())) {
                throw new AppException(ErrorCode.TEACHER_DEPARTMENT_MISMATCH);
            }
        }

        // Requirement: Max student limit 40
        if (request.getMaxStudents() != null && request.getMaxStudents() > 40) {
            throw new AppException(ErrorCode.INVALID_MAX_STUDENTS);
        }

        long conflicts = scheduledClassRepository.countOverlappingClasses(
                teacher.getTeacherId(),
                currentSemester.getSemesterId(),
                request.getDayOfWeek(),
                request.getStartTime(),
                request.getEndTime(),
                null);

        if (conflicts > 0) {
            throw new AppException(ErrorCode.TEACHER_SCHEDULE_CONFLICT);
        }

        ScheduledClass sc = new ScheduledClass();
        sc.setCourse(course);
        sc.setTeacher(teacher);
        sc.setSemester(currentSemester);
        sc.setRoomNumber(request.getRoomNumber());
        sc.setDayOfWeek(request.getDayOfWeek());
        sc.setStartTime(request.getStartTime());
        sc.setEndTime(request.getEndTime());
        sc.setMaxStudents(request.getMaxStudents() != null ? request.getMaxStudents() : 40);
        sc.setStatus(ScheduledClassStatus.OPEN);

        sc = scheduledClassRepository.save(sc);

        return mapToListItemResponse(sc);
    }

    @Override
    @Transactional
    public AdminClassListItemResponse updateClass(Integer classId, AdminUpdateClassRequest request) {
        ScheduledClass scheduledClass = scheduledClassRepository.findByClassIdAndDeletedAtIsNull(classId)
                .orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_FOUND));

        // Constraint: Cannot change Course or Semester
        // We ignore courseId and semesterId from request and keep existing ones

        Teacher teacher = teacherRepository.findById(UUID.fromString(request.getTeacherId()))
                .orElseThrow(() -> new AppException(ErrorCode.TEACHER_PROFILE_NOT_FOUND));

        // Validation: Teacher must be in same department as CURRENT course
        if (scheduledClass.getCourse().getDepartment() != null && teacher.getDepartment() != null) {
            if (!scheduledClass.getCourse().getDepartment().getDepartmentId()
                    .equals(teacher.getDepartment().getDepartmentId())) {
                throw new AppException(ErrorCode.TEACHER_DEPARTMENT_MISMATCH);
            }
        }

        // Constraint: Cannot cancel if class has students
        if (request.getStatus() == ScheduledClassStatus.CANCELLED) {
            long studentCount = enrollmentRepository.countByScheduledClassClassId(classId);
            if (studentCount > 0) {
                throw new AppException(ErrorCode.CLASS_HAS_ENROLLED_STUDENTS);
            }
        }

        // Requirement: Max student limit 40
        if (request.getMaxStudents() != null && request.getMaxStudents() > 40) {
            throw new AppException(ErrorCode.INVALID_MAX_STUDENTS);
        }

        // Conflict Check (exclude current class)
        long conflicts = scheduledClassRepository.countOverlappingClasses(
                teacher.getTeacherId(),
                scheduledClass.getSemester().getSemesterId(),
                request.getDayOfWeek(),
                request.getStartTime(),
                request.getEndTime(),
                classId);

        if (conflicts > 0) {
            throw new AppException(ErrorCode.TEACHER_SCHEDULE_CONFLICT);
        }

        scheduledClass.setTeacher(teacher);
        scheduledClass.setRoomNumber(request.getRoomNumber());
        scheduledClass.setDayOfWeek(request.getDayOfWeek());
        scheduledClass.setStartTime(request.getStartTime());
        scheduledClass.setEndTime(request.getEndTime());
        scheduledClass.setMaxStudents(request.getMaxStudents());
        scheduledClass.setStatus(request.getStatus());

        scheduledClass = scheduledClassRepository.save(scheduledClass);
        return mapToListItemResponse(scheduledClass);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminClassDetailResponse getClassDetail(Integer classId) {
        ScheduledClass scheduledClass = scheduledClassRepository.findByClassIdAndDeletedAtIsNull(classId)
                .orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_FOUND));

        List<Enrollment> enrollments = enrollmentRepository.findByScheduledClassClassId(classId);

        List<AdminClassStudentResponse> studentResponses = enrollments.stream()
                .map(e -> AdminClassStudentResponse.builder()
                        .enrollmentId(e.getEnrollmentId())
                        .studentId(e.getStudent().getStudentId().toString())
                        .studentCode(e.getStudent().getStudentCode())
                        .fullName((e.getStudent().getFirstName() + " " + e.getStudent().getLastName()).trim())
                        .email(e.getStudent().getEmail())
                        .enrollmentDate(e.getEnrollmentDate())
                        .build())
                .collect(Collectors.toList());

        String teacherName = "N/A";
        if (scheduledClass.getTeacher() != null) {
            teacherName = (scheduledClass.getTeacher().getFirstName() + " " + scheduledClass.getTeacher().getLastName())
                    .trim();
        }

        return AdminClassDetailResponse.builder()
                .classId(scheduledClass.getClassId())
                .courseName(scheduledClass.getCourse().getName())
                .courseCode(scheduledClass.getCourse().getCode())
                .teacherName(teacherName)
                .teacherId(scheduledClass.getTeacher() != null ? scheduledClass.getTeacher().getTeacherId().toString()
                        : null)
                .semesterName(
                        scheduledClass.getSemester() != null ? scheduledClass.getSemester().getDisplayName() : "N/A")
                .roomNumber(scheduledClass.getRoomNumber())
                .schedule(formatSchedule(scheduledClass))
                .dayOfWeek(scheduledClass.getDayOfWeek())
                .startTime(scheduledClass.getStartTime() != null ? scheduledClass.getStartTime().toString() : null)
                .endTime(scheduledClass.getEndTime() != null ? scheduledClass.getEndTime().toString() : null)
                .status(scheduledClass.getStatus())
                .maxStudents(scheduledClass.getMaxStudents())
                .studentCount(enrollments.size())
                .students(studentResponses)
                .build();
    }

    @Override
    @Transactional
    public void deleteClass(Integer classId) {
        ScheduledClass scheduledClass = scheduledClassRepository.findByClassIdAndDeletedAtIsNull(classId)
                .orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_FOUND));

        long studentCount = enrollmentRepository.countByScheduledClassClassId(classId);
        if (studentCount > 0) {
            throw new AppException(ErrorCode.CLASS_HAS_ENROLLED_STUDENTS);
        }

        scheduledClass.setDeletedAt(java.time.LocalDateTime.now());
        scheduledClassRepository.save(scheduledClass);
    }

    private AdminClassListItemResponse mapToListItemResponse(ScheduledClass scheduledClass) {
        String teacherName = "N/A";
        if (scheduledClass.getTeacher() != null) {
            teacherName = (scheduledClass.getTeacher().getFirstName() + " " + scheduledClass.getTeacher().getLastName())
                    .trim();
        }

        long studentCount = enrollmentRepository.countByScheduledClassClassId(scheduledClass.getClassId());

        return AdminClassListItemResponse.builder()
                .classId(scheduledClass.getClassId())
                .courseName(scheduledClass.getCourse().getName())
                .courseCode(scheduledClass.getCourse().getCode())
                .teacherName(teacherName)
                .teacherId(scheduledClass.getTeacher() != null ? scheduledClass.getTeacher().getTeacherId().toString()
                        : null)
                .semesterName(
                        scheduledClass.getSemester() != null ? scheduledClass.getSemester().getDisplayName() : "N/A")
                .roomNumber(scheduledClass.getRoomNumber())
                .schedule(formatSchedule(scheduledClass))
                .dayOfWeek(scheduledClass.getDayOfWeek())
                .startTime(scheduledClass.getStartTime() != null ? scheduledClass.getStartTime().toString() : null)
                .endTime(scheduledClass.getEndTime() != null ? scheduledClass.getEndTime().toString() : null)
                .status(scheduledClass.getStatus())
                .maxStudents(scheduledClass.getMaxStudents())
                .studentCount(studentCount)
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
