package com.newwave.student_management.domains.profile.controller;

import com.newwave.student_management.common.dto.ApiResponse;
import com.newwave.student_management.domains.profile.dto.response.SemesterResponse;
import com.newwave.student_management.domains.profile.repository.SemesterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/semesters")
@RequiredArgsConstructor
public class SemesterController {

    private final SemesterRepository semesterRepository;

    @GetMapping
    public ApiResponse<List<SemesterResponse>> getAllSemesters() {
        return ApiResponse.<List<SemesterResponse>>builder()
                .result(semesterRepository.findAll(Sort.by(Sort.Direction.DESC, "year", "semesterId")).stream()
                        .map(SemesterResponse::fromEntity)
                        .collect(Collectors.toList()))
                .build();
    }

    @GetMapping("/current")
    public ApiResponse<SemesterResponse> getCurrentSemester() {
        return ApiResponse.<SemesterResponse>builder()
                .result(semesterRepository.findByIsCurrentTrue()
                        .map(SemesterResponse::fromEntity)
                        .orElse(null))
                .build();
    }
}
