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
public class AdminCourseListItemResponse {
    private Integer courseId;
    private String code;
    private String name;
    private Integer credits;
    private Integer departmentId;
    private String departmentName;
    private String status;
    private LocalDateTime createdAt;
}
