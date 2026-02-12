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
public class AdminDepartmentListResponse {

    private List<AdminDepartmentListItemResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}

