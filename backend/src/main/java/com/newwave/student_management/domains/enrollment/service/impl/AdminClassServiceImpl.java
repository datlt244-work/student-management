package com.newwave.student_management.domains.enrollment.service.impl;

import com.newwave.student_management.common.util.PaginationUtil;
import com.newwave.student_management.domains.curriculum.entity.Course;
import com.newwave.student_management.domains.enrollment.dto.response.AdminClassListItemResponse;
import com.newwave.student_management.domains.enrollment.dto.response.AdminClassListResponse;
import com.newwave.student_management.domains.enrollment.entity.ScheduledClass;
import com.newwave.student_management.domains.enrollment.entity.ScheduledClassStatus;
import com.newwave.student_management.domains.enrollment.repository.EnrollmentRepository;
import com.newwave.student_management.domains.enrollment.repository.ScheduledClassRepository;
import com.newwave.student_management.domains.enrollment.service.IAdminClassService;
import com.newwave.student_management.domains.profile.entity.Teacher;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminClassServiceImpl implements IAdminClassService {

    private final ScheduledClassRepository scheduledClassRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Override
    public AdminClassListResponse getAdminClasses(
            String search,
            ScheduledClassStatus status,
            Integer semesterId,
            Pageable pageable) {
        Specification<ScheduledClass> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.isBlank()) {
                String searchLower = "%" + search.toLowerCase() + "%";

                // Join with Course
                Join<ScheduledClass, Course> courseJoin = root.join("course", JoinType.INNER);
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
                .schedule(sc.getSchedule())
                .status(sc.getStatus())
                .maxStudents(sc.getMaxStudents())
                .studentCount(studentCount)
                .build();
    }
}
