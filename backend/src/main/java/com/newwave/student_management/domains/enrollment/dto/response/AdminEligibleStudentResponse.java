package com.newwave.student_management.domains.enrollment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminEligibleStudentResponse {
    private String userId;
    private String studentCode;
    private String fullName;
    private String email;
}
