package com.newwave.student_management.domains.notification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FcmTokenRequest {
    @NotBlank
    private String token;
    private String deviceType;
}
