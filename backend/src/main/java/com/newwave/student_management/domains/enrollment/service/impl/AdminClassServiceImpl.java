package com.newwave.student_management.domains.enrollment.service.impl;

import com.newwave.student_management.common.util.PaginationUtil;
import com.newwave.student_management.common.exception.AppException;
import com.newwave.student_management.common.exception.ErrorCode;
import com.newwave.student_management.domains.curriculum.entity.Course;
import com.newwave.student_management.domains.curriculum.repository.CourseRepository;
import com.newwave.student_management.domains.enrollment.dto.request.AdminCreateClassRequest;
import com.newwave.student_management.domains.enrollment.dto.request.AdminEnrollStudentRequest;
import com.newwave.student_management.domains.enrollment.dto.request.AdminUpdateClassRequest;
import com.newwave.student_management.domains.enrollment.dto.response.AdminClassDetailResponse;
import com.newwave.student_management.domains.enrollment.dto.response.AdminClassListItemResponse;
import com.newwave.student_management.domains.enrollment.dto.response.AdminClassListResponse;
import com.newwave.student_management.domains.enrollment.dto.response.AdminClassStudentResponse;
import com.newwave.student_management.domains.enrollment.dto.response.AdminEligibleStudentResponse;
import com.newwave.student_management.domains.enrollment.entity.Enrollment;
import com.newwave.student_management.domains.enrollment.entity.ScheduledClass;
import com.newwave.student_management.domains.enrollment.entity.ScheduledClassStatus;
import com.newwave.student_management.domains.profile.entity.Student;
import com.newwave.student_management.domains.enrollment.repository.EnrollmentRepository;
import com.newwave.student_management.domains.enrollment.repository.ScheduledClassRepository;
import com.newwave.student_management.domains.enrollment.service.IAdminClassService;
import com.newwave.student_management.domains.profile.entity.Semester;
import com.newwave.student_management.domains.profile.entity.Teacher;
import com.newwave.student_management.domains.enrollment.dto.request.ClassSessionRequest;
import com.newwave.student_management.domains.enrollment.dto.response.ClassSessionResponse;
import com.newwave.student_management.domains.enrollment.entity.ClassSession;
import com.newwave.student_management.domains.enrollment.repository.ClassSessionRepository;
import com.newwave.student_management.domains.facility.entity.Room;
import com.newwave.student_management.domains.facility.repository.RoomRepository;
import com.newwave.student_management.domains.profile.repository.SemesterRepository;
import com.newwave.student_management.domains.profile.repository.StudentRepository;
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
    private final StudentRepository studentRepository;
    private final SemesterRepository semesterRepository;
    private final RoomRepository roomRepository;
    private final ClassSessionRepository classSessionRepository;

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
        Course course = courseRepository.findByCourseIdAndDeletedAtIsNull(request.getCourseId())
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_FOUND));

        // Always use CURRENT semester for new classes
        Semester currentSemester = semesterRepository.findByIsCurrentTrue()
                .orElseThrow(() -> new AppException(ErrorCode.NO_CURRENT_SEMESTER));

        // Conflict Check (Mandatory Teacher Check)
        if (request.getTeacherId() == null || request.getTeacherId().isBlank()) {
            throw new AppException(ErrorCode.VALIDATION_ERROR);
        }

        Teacher teacher = teacherRepository.findByTeacherIdAndDeletedAtIsNull(UUID.fromString(request.getTeacherId()))
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

        ScheduledClass scheduledClass = new ScheduledClass();
        scheduledClass.setCourse(course);
        scheduledClass.setTeacher(teacher);
        scheduledClass.setSemester(currentSemester);
        scheduledClass.setMaxStudents(request.getMaxStudents() != null ? request.getMaxStudents() : 40);
        scheduledClass.setStatus(ScheduledClassStatus.OPEN);

        List<ClassSession> classSessions = new java.util.ArrayList<>();
        if (request.getSessions() != null) {
            for (ClassSessionRequest sessionRequest : request.getSessions()) {
                Room room = roomRepository.findById(sessionRequest.getRoomId())
                        .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));

                // Check room capacity
                if (scheduledClass.getMaxStudents() > room.getCapacity()) {
                    throw new AppException(ErrorCode.CLASS_SLOT_EXCEEDS_ROOM_CAPACITY);
                }

                // Check teacher conflict
                long teacherConflicts = scheduledClassRepository.countOverlappingClasses(
                        teacher.getTeacherId(),
                        currentSemester.getSemesterId(),
                        sessionRequest.getDayOfWeek(),
                        sessionRequest.getStartTime(),
                        sessionRequest.getEndTime(),
                        null);
                if (teacherConflicts > 0) {
                    throw new AppException(ErrorCode.TEACHER_SCHEDULE_CONFLICT);
                }

                // Check room conflict
                long roomConflicts = classSessionRepository.countRoomConflicts(
                        room.getRoomId(),
                        sessionRequest.getDayOfWeek(),
                        sessionRequest.getStartTime(),
                        sessionRequest.getEndTime(),
                        currentSemester.getSemesterId(),
                        null);
                if (roomConflicts > 0) {
                    throw new AppException(ErrorCode.ROOM_SCHEDULE_CONFLICT);
                }

                ClassSession classSession = new ClassSession();
                classSession.setScheduledClass(scheduledClass);
                classSession.setRoom(room);
                classSession.setDayOfWeek(sessionRequest.getDayOfWeek());
                classSession.setStartTime(sessionRequest.getStartTime());
                classSession.setEndTime(sessionRequest.getEndTime());
                classSessions.add(classSession);
            }
        }
        scheduledClass.setSessions(classSessions);

        scheduledClass = scheduledClassRepository.save(scheduledClass);

        return mapToListItemResponse(scheduledClass);
    }

    @Override
    @Transactional
    public AdminClassListItemResponse updateClass(Integer classId, AdminUpdateClassRequest request) {
        ScheduledClass scheduledClass = scheduledClassRepository.findByClassIdAndDeletedAtIsNull(classId)
                .orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_FOUND));

        // Constraint: Cannot change Course or Semester
        // We ignore courseId and semesterId from request and keep existing ones

        Teacher teacher = teacherRepository.findByTeacherIdAndDeletedAtIsNull(UUID.fromString(request.getTeacherId()))
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

        scheduledClass.setTeacher(teacher);
        scheduledClass.setMaxStudents(
                request.getMaxStudents() != null ? request.getMaxStudents() : scheduledClass.getMaxStudents());
        scheduledClass.setStatus(request.getStatus());

        if (scheduledClass.getSessions() != null) {
            scheduledClass.getSessions().clear();
        } else {
            scheduledClass.setSessions(new java.util.ArrayList<>());
        }

        if (request.getSessions() != null) {
            for (ClassSessionRequest sessionRequest : request.getSessions()) {
                Room room = roomRepository.findById(sessionRequest.getRoomId())
                        .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));

                // Check room capacity
                if (scheduledClass.getMaxStudents() > room.getCapacity()) {
                    throw new AppException(ErrorCode.CLASS_SLOT_EXCEEDS_ROOM_CAPACITY);
                }

                // Check teacher conflict
                long teacherConflicts = scheduledClassRepository.countOverlappingClasses(
                        teacher.getTeacherId(),
                        scheduledClass.getSemester().getSemesterId(),
                        sessionRequest.getDayOfWeek(),
                        sessionRequest.getStartTime(),
                        sessionRequest.getEndTime(),
                        classId);
                if (teacherConflicts > 0) {
                    throw new AppException(ErrorCode.TEACHER_SCHEDULE_CONFLICT);
                }

                // Check room conflict
                long roomConflicts = classSessionRepository.countRoomConflicts(
                        room.getRoomId(),
                        sessionRequest.getDayOfWeek(),
                        sessionRequest.getStartTime(),
                        sessionRequest.getEndTime(),
                        scheduledClass.getSemester().getSemesterId(),
                        classId);
                if (roomConflicts > 0) {
                    throw new AppException(ErrorCode.ROOM_SCHEDULE_CONFLICT);
                }

                ClassSession classSession = new ClassSession();
                classSession.setScheduledClass(scheduledClass);
                classSession.setRoom(room);
                classSession.setDayOfWeek(sessionRequest.getDayOfWeek());
                classSession.setStartTime(sessionRequest.getStartTime());
                classSession.setEndTime(sessionRequest.getEndTime());
                scheduledClass.getSessions().add(classSession);
            }
        }

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
                .sessions(scheduledClass.getSessions() != null ? scheduledClass.getSessions().stream()
                        .map(s -> ClassSessionResponse.builder()
                                .sessionId(s.getSessionId())
                                .roomId(s.getRoom() != null ? s.getRoom().getRoomId() : null)
                                .roomName(s.getRoom() != null ? s.getRoom().getName() : null)
                                .dayOfWeek(s.getDayOfWeek())
                                .startTime(s.getStartTime())
                                .endTime(s.getEndTime())
                                .build())
                        .collect(Collectors.toList()) : new java.util.ArrayList<>())
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

    @Override
    @Transactional
    public void enrollStudent(AdminEnrollStudentRequest request) {
        ScheduledClass scheduledClass = scheduledClassRepository.findByClassIdAndDeletedAtIsNull(request.getClassId())
                .orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_FOUND));

        Student student = studentRepository.findByUser_UserIdAndDeletedAtIsNull(UUID.fromString(request.getStudentId()))
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_PROFILE_NOT_FOUND));

        // 1. Check if already enrolled
        if (enrollmentRepository.existsByScheduledClassClassIdAndStudentStudentId(scheduledClass.getClassId(),
                student.getStudentId())) {
            throw new AppException(ErrorCode.STUDENT_ALREADY_ENROLLED);
        }

        // 1.1 Check Department Mismatch
        if (scheduledClass.getCourse().getDepartment() != null && student.getDepartment() != null) {
            if (!scheduledClass.getCourse().getDepartment().getDepartmentId()
                    .equals(student.getDepartment().getDepartmentId())) {
                throw new AppException(ErrorCode.STUDENT_DEPARTMENT_MISMATCH);
            }
        }

        // 2. Check Capacity
        long currentEnrolled = enrollmentRepository.countByScheduledClassClassId(scheduledClass.getClassId());
        if (currentEnrolled >= scheduledClass.getMaxStudents()) {
            throw new AppException(ErrorCode.CLASS_FULL);
        }

        // 3. Check Student Schedule Conflict
        if (scheduledClass.getSessions() != null) {
            for (ClassSession session : scheduledClass.getSessions()) {
                long conflicts = enrollmentRepository.countStudentConflicts(
                        student.getStudentId(),
                        scheduledClass.getSemester().getSemesterId(),
                        session.getDayOfWeek(),
                        session.getStartTime(),
                        session.getEndTime());
                if (conflicts > 0) {
                    throw new AppException(ErrorCode.STUDENT_SCHEDULE_CONFLICT);
                }
            }
        }

        // 4. Create Enrollment
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setScheduledClass(scheduledClass);
        enrollment.setEnrollmentDate(java.time.LocalDate.now());

        enrollmentRepository.save(enrollment);
    }

    @Override
    @Transactional
    public void unenrollStudent(Integer classId, UUID studentId) {
        Enrollment enrollment = enrollmentRepository
                .findByScheduledClassClassIdAndStudentStudentId(classId, studentId)
                .orElseThrow(() -> new AppException(ErrorCode.ENROLLMENT_NOT_FOUND));

        enrollmentRepository.delete(enrollment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminEligibleStudentResponse> getEligibleStudents(Integer classId) {
        ScheduledClass scheduledClass = scheduledClassRepository.findByClassIdAndDeletedAtIsNull(classId)
                .orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_FOUND));

        // Get department from course
        Integer departmentId = (scheduledClass.getCourse().getDepartment() != null)
                ? scheduledClass.getCourse().getDepartment().getDepartmentId()
                : null;

        if (departmentId == null) {
            return new ArrayList<>();
        }

        // Find students in the same department
        List<Student> departmentStudents = studentRepository
                .findByDepartment_DepartmentIdAndDeletedAtIsNull(departmentId);

        return departmentStudents.stream()
                .filter(student -> {
                    // Check if already enrolled in this SPECIFIC class
                    if (enrollmentRepository.existsByScheduledClassClassIdAndStudentStudentId(classId,
                            student.getStudentId())) {
                        return false;
                    }
                    // Check schedule conflicts in the SAME semester
                    if (scheduledClass.getSessions() != null) {
                        for (ClassSession session : scheduledClass.getSessions()) {
                            long conflicts = enrollmentRepository.countStudentConflicts(
                                    student.getStudentId(),
                                    scheduledClass.getSemester().getSemesterId(),
                                    session.getDayOfWeek(),
                                    session.getStartTime(),
                                    session.getEndTime());
                            if (conflicts > 0) {
                                return false;
                            }
                        }
                    }
                    return true;
                })
                .map(student -> AdminEligibleStudentResponse.builder()
                        .userId(student.getUser().getUserId().toString())
                        .studentCode(student.getStudentCode())
                        .fullName((student.getFirstName() + " " + student.getLastName()).trim())
                        .email(student.getEmail())
                        .build())
                .collect(Collectors.toList());
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
                .sessions(scheduledClass.getSessions() != null ? scheduledClass.getSessions().stream()
                        .map(s -> ClassSessionResponse.builder()
                                .sessionId(s.getSessionId())
                                .roomId(s.getRoom() != null ? s.getRoom().getRoomId() : null)
                                .roomName(s.getRoom() != null ? s.getRoom().getName() : null)
                                .dayOfWeek(s.getDayOfWeek())
                                .startTime(s.getStartTime())
                                .endTime(s.getEndTime())
                                .build())
                        .collect(Collectors.toList()) : new java.util.ArrayList<>())
                .status(scheduledClass.getStatus())
                .maxStudents(scheduledClass.getMaxStudents())
                .studentCount(studentCount)
                .build();
    }
}
