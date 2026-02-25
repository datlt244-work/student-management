package com.newwave.student_management.domains.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipientSearchResponse {
    private String name;
    private String identifier; // email or student/teacher code
    private String role;
}
