package com.newwave.student_management.domains.profile.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateStudentProfileRequest {

    @Size(max = 20, message = "Phone must be at most 20 characters")
    private String phone;

    @Size(max = 255, message = "Address must be at most 255 characters")
    private String address;
}
