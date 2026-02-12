package com.newwave.student_management.domains.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for UC-13.2: Create Department
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDepartmentDetailResponse {

    private Integer departmentId;
    private String name;
    private String officeLocation;
    private LocalDateTime createdAt;
}

