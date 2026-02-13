package com.newwave.student_management.domains.auth.service.impl;

import com.newwave.student_management.common.exception.AppException;
import com.newwave.student_management.common.exception.ErrorCode;
import com.newwave.student_management.common.util.PaginationUtil;
import com.newwave.student_management.domains.auth.dto.request.AdminCreateDepartmentRequest;
import com.newwave.student_management.domains.auth.dto.request.AdminUpdateDepartmentRequest;
import com.newwave.student_management.domains.auth.dto.response.AdminDepartmentDetailResponse;
import com.newwave.student_management.domains.auth.dto.response.AdminDepartmentListResponse;
import com.newwave.student_management.domains.auth.dto.response.AdminDepartmentListItemResponse;
import com.newwave.student_management.domains.auth.service.IAdminDepartmentService;
import com.newwave.student_management.domains.curriculum.repository.CourseRepository;
import com.newwave.student_management.domains.profile.entity.Department;
import com.newwave.student_management.domains.profile.repository.DepartmentRepository;
import com.newwave.student_management.domains.profile.repository.StudentRepository;
import com.newwave.student_management.domains.profile.repository.StudentRepository;
import com.newwave.student_management.domains.profile.repository.TeacherRepository;
import com.newwave.student_management.domains.profile.entity.DepartmentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDepartmentService implements IAdminDepartmentService {

    private final DepartmentRepository departmentRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;

    @Override
    public AdminDepartmentListResponse getDepartments(String search, DepartmentStatus status, Pageable pageable) {
        String normalizedSearch = PaginationUtil.normalizeSearch(search);

        Page<Department> pageResult = departmentRepository.searchAdminDepartments(
                normalizedSearch,
                status,
                pageable
        );

        List<Integer> departmentIds = pageResult.getContent().stream()
                .map(Department::getDepartmentId)
                .toList();

        Map<Integer, Long> courseCountByDepartmentId = new HashMap<>();
        if (!departmentIds.isEmpty()) {
            for (Object[] row : courseRepository.countActiveCoursesByDepartmentIds(departmentIds)) {
                Integer departmentId = (Integer) row[0];
                Long count = (Long) row[1];
                courseCountByDepartmentId.put(departmentId, count);
            }
        }

        List<AdminDepartmentListItemResponse> items = pageResult.getContent().stream()
                .map(department -> AdminDepartmentListItemResponse.builder()
                        .departmentId(department.getDepartmentId())
                        .name(department.getName())
                        .officeLocation(department.getOfficeLocation())
                        .courseCount(courseCountByDepartmentId.getOrDefault(department.getDepartmentId(), 0L))
                        .courseCount(courseCountByDepartmentId.getOrDefault(department.getDepartmentId(), 0L))
                        .createdAt(department.getCreatedAt())
                        .status(department.getStatus())
                        .build())
                .toList();

        PaginationUtil.PaginationMetadata metadata = PaginationUtil.extractMetadata(pageResult);

        return AdminDepartmentListResponse.builder()
                .content(items)
                .page(metadata.page)
                .size(metadata.size)
                .totalElements(metadata.totalElements)
                .totalPages(metadata.totalPages)
                .totalCourses(courseRepository.countByDeletedAtIsNull())
                .build();
    }

    @Override
    @Transactional
    public AdminDepartmentDetailResponse createDepartment(AdminCreateDepartmentRequest request) {
        String name = request.getName() != null ? request.getName().trim() : "";
        if (name.isBlank()) {
            throw new AppException(ErrorCode.DEPARTMENT_NAME_REQUIRED);
        }

        // Check if department name already exists (case-insensitive)
        if (departmentRepository.existsByNameIgnoreCaseAndDeletedAtIsNull(name)) {
            throw new AppException(ErrorCode.DEPARTMENT_NAME_EXISTED);
        }

        String officeLocation = request.getOfficeLocation() != null ? request.getOfficeLocation().trim() : null;
        if (officeLocation != null && officeLocation.isBlank()) {
            officeLocation = null;
        }

        Department department = new Department();
        department.setName(name);
        department.setOfficeLocation(officeLocation);
        department.setStatus(DepartmentStatus.ACTIVE);

        Department saved = departmentRepository.save(department);

        return AdminDepartmentDetailResponse.builder()
                .departmentId(saved.getDepartmentId())
                .name(saved.getName())
                .officeLocation(saved.getOfficeLocation())
                .createdAt(saved.getCreatedAt())
                .status(saved.getStatus())
                .build();
    }

    @Override
    @Transactional
    public AdminDepartmentDetailResponse updateDepartment(Integer departmentId, AdminUpdateDepartmentRequest request) {
        Department department = departmentRepository.findByDepartmentIdAndDeletedAtIsNull(departmentId)
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND));

        // Update name if provided
        if (request.getName() != null) {
            String name = request.getName().trim();
            if (name.isBlank()) {
                throw new AppException(ErrorCode.DEPARTMENT_NAME_REQUIRED);
            }

            // Check if new name conflicts with existing department (excluding current one)
            if (!name.equalsIgnoreCase(department.getName()) &&
                    departmentRepository.existsByNameIgnoreCaseAndDeletedAtIsNull(name)) {
                throw new AppException(ErrorCode.DEPARTMENT_NAME_EXISTED);
            }

            department.setName(name);
        }

        // Update officeLocation if provided
        if (request.getOfficeLocation() != null) {
            String officeLocation = request.getOfficeLocation().trim();
            department.setOfficeLocation(officeLocation.isBlank() ? null : officeLocation);
        }

        Department saved = departmentRepository.save(department);

        return AdminDepartmentDetailResponse.builder()
                .departmentId(saved.getDepartmentId())
                .name(saved.getName())
                .officeLocation(saved.getOfficeLocation())
                .createdAt(saved.getCreatedAt())
                .status(saved.getStatus())
                .build();
    }

    @Override
    @Transactional
    public AdminDepartmentDetailResponse updateDepartmentStatus(Integer departmentId, DepartmentStatus status) {
        Department department = departmentRepository.findByDepartmentIdAndDeletedAtIsNull(departmentId)
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND));

        if (status == DepartmentStatus.INACTIVE) {
            long courseCount = courseRepository.countByDepartment_DepartmentIdAndStatusAndDeletedAtIsNull(
                    departmentId,
                    com.newwave.student_management.domains.curriculum.entity.CourseStatus.ACTIVE
            );
            if (courseCount > 0) {
                throw new AppException(ErrorCode.DEPARTMENT_HAS_ACTIVE_COURSES);
            }
        }

        department.setStatus(status);
        Department saved = departmentRepository.save(department);

        return AdminDepartmentDetailResponse.builder()
                .departmentId(saved.getDepartmentId())
                .name(saved.getName())
                .officeLocation(saved.getOfficeLocation())
                .createdAt(saved.getCreatedAt())
                .status(saved.getStatus())
                .build();
    }

    @Override
    @Transactional
    public void deleteDepartment(Integer departmentId) {
        Department department = departmentRepository.findByDepartmentIdAndDeletedAtIsNull(departmentId)
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND));

        // Check if department has active teachers or students
        long teacherCount = teacherRepository.countByDepartment_DepartmentIdAndDeletedAtIsNull(departmentId);
        long studentCount = studentRepository.countByDepartment_DepartmentIdAndDeletedAtIsNull(departmentId);
        long courseCount = courseRepository.countByDepartment_DepartmentIdAndDeletedAtIsNull(departmentId);

        if (teacherCount > 0 || studentCount > 0) {
            throw new AppException(ErrorCode.DEPARTMENT_HAS_ACTIVE_MEMBERS);
        }
        if (courseCount > 0) {
            throw new AppException(ErrorCode.DEPARTMENT_HAS_ACTIVE_COURSES);
        }

        // Soft delete department
        department.setDeletedAt(LocalDateTime.now());
        departmentRepository.save(department);
    }
}

