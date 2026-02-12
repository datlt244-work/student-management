package com.newwave.student_management.domains.auth.service.impl;

import com.newwave.student_management.common.util.PaginationUtil;
import com.newwave.student_management.domains.auth.dto.response.AdminDepartmentListResponse;
import com.newwave.student_management.domains.auth.dto.response.AdminDepartmentListItemResponse;
import com.newwave.student_management.domains.auth.service.IAdminDepartmentService;
import com.newwave.student_management.domains.profile.entity.Department;
import com.newwave.student_management.domains.profile.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDepartmentService implements IAdminDepartmentService {

    private final DepartmentRepository departmentRepository;

    @Override
    public AdminDepartmentListResponse getDepartments(String search, Pageable pageable) {
        String normalizedSearch = PaginationUtil.normalizeSearch(search);

        Page<Department> pageResult = departmentRepository.searchAdminDepartments(
                normalizedSearch,
                pageable
        );

        List<AdminDepartmentListItemResponse> items = pageResult.getContent().stream()
                .map(department -> AdminDepartmentListItemResponse.builder()
                        .departmentId(department.getDepartmentId())
                        .name(department.getName())
                        .officeLocation(department.getOfficeLocation())
                        .createdAt(department.getCreatedAt())
                        .build())
                .toList();

        PaginationUtil.PaginationMetadata metadata = PaginationUtil.extractMetadata(pageResult);

        return AdminDepartmentListResponse.builder()
                .content(items)
                .page(metadata.page)
                .size(metadata.size)
                .totalElements(metadata.totalElements)
                .totalPages(metadata.totalPages)
                .build();
    }
}

