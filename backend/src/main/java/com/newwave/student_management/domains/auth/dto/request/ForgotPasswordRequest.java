package com.newwave.student_management.domains.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ForgotPasswordRequest {

    @NotBlank(message = "EMAIL_REQUIRED")
    @Email(message = "EMAIL_INVALID")
    @Pattern(
            regexp = "^[A-Za-z0-9._%+-]+@fpt\\.edu\\.vn$",
            message = "EMAIL_INVALID"
    )
    private String email;
}

