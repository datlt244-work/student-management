package com.newwave.student_management.domains.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

/**
 * Request body for UC-11.3a: POST /admin/users — Create user (Teacher or Student).
 * Common fields required; role-specific fields validated in service when role is set.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminCreateUserRequest {

    @NotBlank(message = "Role is required")
    @Pattern(regexp = "TEACHER|STUDENT", message = "Role must be TEACHER or STUDENT")
    private String role;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 100)
    private String email;

    @NotNull(message = "Department is required")
    private Integer departmentId;

    @NotBlank(message = "First name is required")
    @Size(max = 50)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 50)
    private String lastName;

    @Size(max = 20)
    private String phone;

    // ——— Teacher-only (when role = TEACHER) ———
    @Size(max = 10)
    private String teacherCode;
    @Size(max = 100)
    private String specialization;
    @Size(max = 50)
    private String academicRank;
    @Size(max = 50)
    private String officeRoom;
    private String degreesQualification; // TEXT

    // ——— Student-only (when role = STUDENT) ———
    @Size(max = 10)
    private String studentCode;
    private LocalDate dob;
    @Pattern(regexp = "MALE|FEMALE|OTHER", message = "Gender must be MALE, FEMALE or OTHER")
    private String gender;
    @Size(max = 100)
    private String major;
    @Size(max = 255)
    private String address;
    @Min(1)
    @Max(4)
    private Integer year;
    @Size(max = 50)
    private String manageClass;
}
