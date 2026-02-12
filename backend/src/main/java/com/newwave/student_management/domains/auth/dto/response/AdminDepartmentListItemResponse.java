package com.newwave.student_management.domains.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDepartmentListItemResponse {

    private Integer departmentId;
    private String name;
    private String officeLocation;
    private LocalDateTime createdAt;
}

