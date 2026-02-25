package com.newwave.student_management.domains.notification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TargetedNotificationRequest {
    @NotBlank
    private String title;

    @NotBlank
    private String body;

    private String actionUrl;

    // Filters
    private String role; // STUDENT, TEACHER or "All Roles"
    private Long departmentId;
    private String classCode;
    private String recipientId; // For personal notifications
}
