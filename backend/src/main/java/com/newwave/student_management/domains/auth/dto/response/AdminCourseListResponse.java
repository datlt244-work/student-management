package com.newwave.student_management.domains.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminCourseListResponse {
    private List<AdminCourseListItemResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
