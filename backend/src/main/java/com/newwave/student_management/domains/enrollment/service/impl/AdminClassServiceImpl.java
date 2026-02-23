package com.newwave.student_management.domains.enrollment.service.impl;

import com.newwave.student_management.common.util.PaginationUtil;
import com.newwave.student_management.common.exception.AppException;
import com.newwave.student_management.common.exception.ErrorCode;
import com.newwave.student_management.domains.curriculum.entity.Course;
import com.newwave.student_management.domains.curriculum.repository.CourseRepository;
import com.newwave.student_management.domains.enrollment.dto.request.AdminCreateClassRequest;
import com.newwave.student_management.domains.enrollment.dto.response.AdminClassListItemResponse;
import com.newwave.student_management.domains.enrollment.dto.response.AdminClassListResponse;
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
            predicates.add(cb.equal(courseJoin.get("status"), com.newwave.student_management.domains.curriculum.entity.CourseStatus.ACTIVE));

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

        // Requirement: Kỳ học để là kỳ hiện tại không thể thay đổi
        Semester currentSemester = semesterRepository.findByIsCurrentTrue()
                .orElseThrow(() -> new AppException(ErrorCode.NO_CURRENT_SEMESTER));

        // Requirement: Giáo viên phải có (Mandatory Teacher)
        if (request.getTeacherId() == null || request.getTeacherId().isBlank()) {
            throw new AppException(ErrorCode.VALIDATION_ERROR); // Or specialized missing teacher error
        }

        Teacher teacher = teacherRepository.findById(UUID.fromString(request.getTeacherId()))
                .orElseThrow(() -> new AppException(ErrorCode.TEACHER_PROFILE_NOT_FOUND));

        // Validation: Teacher must be in the same department as the course
        if (course.getDepartment() != null && teacher.getDepartment() != null) {
            if (!course.getDepartment().getDepartmentId().equals(teacher.getDepartment().getDepartmentId())) {
                throw new AppException(ErrorCode.TEACHER_DEPARTMENT_MISMATCH);
            }
        }

        // Requirement: Max student default là 40 có thể giảm (Max limit 40)
        if (request.getMaxStudents() != null && request.getMaxStudents() > 40) {
            throw new AppException(ErrorCode.INVALID_MAX_STUDENTS);
        }

        // Requirement: Khi đăng kỳ phải check có bị duplicate lịch của giáo viên không
        // (Conflict Check)
        long conflicts = scheduledClassRepository.countOverlappingClasses(
                teacher.getTeacherId(),
                currentSemester.getSemesterId(),
                request.getDayOfWeek(),
                request.getStartTime(),
                request.getEndTime());

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

    private AdminClassListItemResponse mapToListItemResponse(ScheduledClass sc) {
        String teacherName = "N/A";
        if (sc.getTeacher() != null) {
            teacherName = (sc.getTeacher().getFirstName() + " " + sc.getTeacher().getLastName()).trim();
        }

        long studentCount = enrollmentRepository.countByScheduledClassClassId(sc.getClassId());

        return AdminClassListItemResponse.builder()
                .classId(sc.getClassId())
                .courseName(sc.getCourse().getName())
                .courseCode(sc.getCourse().getCode())
                .teacherName(teacherName)
                .semesterName(sc.getSemester() != null ? sc.getSemester().getDisplayName() : "N/A")
                .roomNumber(sc.getRoomNumber())
                .schedule(formatSchedule(sc))
                .dayOfWeek(sc.getDayOfWeek())
                .startTime(sc.getStartTime() != null ? sc.getStartTime().toString() : null)
                .endTime(sc.getEndTime() != null ? sc.getEndTime().toString() : null)
                .status(sc.getStatus())
                .maxStudents(sc.getMaxStudents())
                .studentCount(studentCount)
                .build();
    }

    private String formatSchedule(ScheduledClass sc) {
        if (sc.getDayOfWeek() == null || sc.getStartTime() == null || sc.getEndTime() == null) {
            return "N/A";
        }
        String day = switch (sc.getDayOfWeek()) {
            case 1 -> "Mon";
            case 2 -> "Tue";
            case 3 -> "Wed";
            case 4 -> "Thu";
            case 5 -> "Fri";
            case 6 -> "Sat";
            case 7 -> "Sun";
            default -> "N/A";
        };
        return String.format("%s %s-%s", day, sc.getStartTime(), sc.getEndTime());
    }
}
