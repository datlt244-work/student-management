package com.newwave.student_management.domains.auth.service.impl;

import com.newwave.student_management.common.exception.AppException;
import com.newwave.student_management.common.exception.ErrorCode;
import com.newwave.student_management.domains.auth.dto.request.AdminCreateSemesterRequest;
import com.newwave.student_management.domains.auth.dto.request.AdminUpdateSemesterRequest;
import com.newwave.student_management.domains.auth.dto.response.AdminSemesterListResponse;
import com.newwave.student_management.domains.auth.dto.response.AdminSemesterResponse;
import com.newwave.student_management.domains.auth.service.AdminSemesterService;
import com.newwave.student_management.domains.enrollment.repository.ScheduledClassRepository;
import com.newwave.student_management.domains.profile.entity.Semester;
import com.newwave.student_management.domains.profile.repository.SemesterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminSemesterServiceImpl implements AdminSemesterService {

    private final SemesterRepository semesterRepository;
    private final ScheduledClassRepository scheduledClassRepository;

    @Override
    public AdminSemesterListResponse getSemesters(Integer year, String name, Boolean isCurrent, Pageable pageable) {
        org.springframework.data.jpa.domain.Specification<Semester> spec = (root, query, cb) -> {
            java.util.List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();

            if (year != null) {
                predicates.add(cb.equal(root.get("year"), year));
            }
            if (name != null && !name.isBlank()) {
                predicates.add(cb.equal(root.get("name"), name.toUpperCase()));
            }
            if (isCurrent != null) {
                predicates.add(cb.equal(root.get("isCurrent"), isCurrent));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<Semester> semesterPage = semesterRepository.findAll(spec, pageable);
        List<AdminSemesterResponse> content = semesterPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return AdminSemesterListResponse.builder()
                .content(content)
                .page(semesterPage.getNumber())
                .size(semesterPage.getSize())
                .totalElements(semesterPage.getTotalElements())
                .totalPages(semesterPage.getTotalPages())
                .build();
    }

    @Override
    @Transactional
    public AdminSemesterResponse createSemester(AdminCreateSemesterRequest request) {
        if (semesterRepository.existsByNameAndYear(request.getName().toUpperCase(), request.getYear())) {
            throw new AppException(ErrorCode.SEMESTER_EXISTED);
        }

        validateSemesterDatesOverlap(null, request.getStartDate(), request.getEndDate());

        Semester semester = new Semester();
        semester.setName(request.getName().toUpperCase());
        semester.setYear(request.getYear());
        semester.setStartDate(request.getStartDate());
        semester.setEndDate(request.getEndDate());
        semester.setCurrent(false);

        semester = semesterRepository.save(semester);
        return mapToResponse(semester);
    }

    @Override
    @Transactional
    public AdminSemesterResponse updateSemester(Integer id, AdminUpdateSemesterRequest request) {
        Semester semester = semesterRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SEMESTER_NOT_FOUND));

        java.time.LocalDate newStart = request.getStartDate() != null ? request.getStartDate() : semester.getStartDate();
        java.time.LocalDate newEnd = request.getEndDate() != null ? request.getEndDate() : semester.getEndDate();

        validateSemesterDatesOverlap(id, newStart, newEnd);

        if (request.getStartDate() != null) semester.setStartDate(request.getStartDate());
        if (request.getEndDate() != null) semester.setEndDate(request.getEndDate());

        semester = semesterRepository.save(semester);
        return mapToResponse(semester);
    }

    private void validateSemesterDatesOverlap(Integer id, java.time.LocalDate start, java.time.LocalDate end) {
        if (start == null || end == null) return;
        if (start.isAfter(end)) {
            throw new AppException(ErrorCode.VALIDATION_ERROR);
        }
        
        var overlaps = semesterRepository.findOverlappingSemesters(id != null ? id : -1, start, end);
        if (!overlaps.isEmpty()) {
            throw new AppException(ErrorCode.SEMESTER_DATE_OVERLAP);
        }
    }

    @Override
    @Transactional
    public AdminSemesterResponse setCurrentSemester(Integer id) {
        Semester target = semesterRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SEMESTER_NOT_FOUND));

        // Unset previous current
        semesterRepository.findByIsCurrentTrue().ifPresent(s -> {
            s.setCurrent(false);
            semesterRepository.save(s);
        });

        target.setCurrent(true);
        target = semesterRepository.save(target);
        return mapToResponse(target);
    }

    @Override
    @Transactional
    public void deleteSemester(Integer id) {
        Semester semester = semesterRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SEMESTER_NOT_FOUND));

        if (scheduledClassRepository.countBySemesterSemesterId(id) > 0) {
            throw new AppException(ErrorCode.SEMESTER_HAS_CLASSES);
        }

        if (semester.isCurrent()) {
            throw new AppException(ErrorCode.SEMESTER_IS_CURRENT);
        }

        semesterRepository.delete(semester);
    }

    private AdminSemesterResponse mapToResponse(Semester semester) {
        return AdminSemesterResponse.builder()
                .semesterId(semester.getSemesterId())
                .name(semester.getName())
                .year(semester.getYear())
                .displayName(semester.getDisplayName())
                .startDate(semester.getStartDate())
                .endDate(semester.getEndDate())
                .isCurrent(semester.isCurrent())
                .createdAt(semester.getCreatedAt())
                .build();
    }
}
