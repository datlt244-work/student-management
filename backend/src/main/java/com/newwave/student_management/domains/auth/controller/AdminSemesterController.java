package com.newwave.student_management.domains.auth.controller;

import com.newwave.student_management.common.dto.ApiResponse;
import com.newwave.student_management.domains.auth.dto.request.AdminCreateSemesterRequest;
import com.newwave.student_management.domains.auth.dto.request.AdminUpdateSemesterRequest;
import com.newwave.student_management.domains.auth.dto.response.AdminSemesterListResponse;
import com.newwave.student_management.domains.auth.dto.response.AdminSemesterResponse;
import com.newwave.student_management.domains.auth.service.AdminSemesterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/semesters")
@RequiredArgsConstructor
@Slf4j
public class AdminSemesterController {

    private final AdminSemesterService adminSemesterService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminSemesterListResponse> getSemesters(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Boolean isCurrent,
            @PageableDefault(sort = {"year", "semesterId"}, direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.<AdminSemesterListResponse>builder()
                .result(adminSemesterService.getSemesters(year, name, isCurrent, pageable))
                .build();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminSemesterResponse> createSemester(@RequestBody @Valid AdminCreateSemesterRequest request) {
        return ApiResponse.<AdminSemesterResponse>builder()
                .result(adminSemesterService.createSemester(request))
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminSemesterResponse> updateSemester(
            @PathVariable Integer id,
            @RequestBody @Valid AdminUpdateSemesterRequest request) {
        return ApiResponse.<AdminSemesterResponse>builder()
                .result(adminSemesterService.updateSemester(id, request))
                .build();
    }

    @PatchMapping("/{id}/set-current")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminSemesterResponse> setCurrentSemester(@PathVariable Integer id) {
        return ApiResponse.<AdminSemesterResponse>builder()
                .result(adminSemesterService.setCurrentSemester(id))
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteSemester(@PathVariable Integer id) {
        adminSemesterService.deleteSemester(id);
        return ApiResponse.<Void>builder().build();
    }
}
