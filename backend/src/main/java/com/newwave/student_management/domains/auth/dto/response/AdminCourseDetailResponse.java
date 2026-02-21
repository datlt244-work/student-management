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
public class AdminCourseDetailResponse {
    private Integer courseId;
    private String code;
    private String name;
    private Integer credits;
    private String description;
    private Integer departmentId;
    private String departmentName;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String currentSemester;
}
