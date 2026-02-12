package com.newwave.student_management.domains.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request body for UC-13.2: POST /admin/departments — Create department.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminCreateDepartmentRequest {

    @NotBlank(message = "Department name is required")
    @Size(max = 100, message = "Department name must be at most 100 characters")
    private String name;

    @Size(max = 100, message = "Office location must be at most 100 characters")
    private String officeLocation;
}

