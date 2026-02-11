package com.newwave.student_management.domains.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

/**
 * Request body for UC-11.3a: POST /admin/users — Create user (Teacher or Student).
 * Validation aligned with project (email @fpt.edu.vn, code formats HJ/HE, sizes, etc.).
 * Role-conditioned required fields (teacherCode/studentCode) are enforced in service.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminCreateUserRequest {

    @NotBlank(message = "Role is required")
    @Pattern(regexp = "TEACHER|STUDENT", message = "Role must be TEACHER or STUDENT")
    private String role;

    @NotBlank(message = "EMAIL_REQUIRED")
    @Email(message = "EMAIL_INVALID")
    @Pattern(regexp = "^[A-Za-z0-9._%+-]+@fpt\\.edu\\.vn$", message = "EMAIL_INVALID")
    @Size(max = 100, message = "Email must be at most 100 characters")
    private String email;

    @NotNull(message = "Department is required")
    @Positive(message = "Department ID must be positive")
    private Integer departmentId;

    @NotBlank(message = "First name is required")
    @Size(max = 50, message = "First name must be at most 50 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 50, message = "Last name must be at most 50 characters")
    private String lastName;

    @Size(max = 20, message = "Phone must be at most 20 characters")
    private String phone;

    // ——— Teacher-only (when role = TEACHER): teacherCode required in service ———
    @Size(max = 10, message = "Teacher code must be at most 10 characters")
    @Pattern(regexp = "^(|[Hh][Jj][0-9]{6})$", message = "Teacher code must match format HJxxxxxx (6 digits)")
    private String teacherCode;
    @Size(max = 100, message = "Specialization must be at most 100 characters")
    private String specialization;
    @Size(max = 50, message = "Academic rank must be at most 50 characters")
    private String academicRank;
    @Size(max = 50, message = "Office room must be at most 50 characters")
    private String officeRoom;
    private String degreesQualification; // TEXT, no max in spec

    // ——— Student-only (when role = STUDENT): studentCode required in service ———
    @Size(max = 10, message = "Student code must be at most 10 characters")
    @Pattern(regexp = "^(|[Hh][Ee][0-9]{6})$", message = "Student code must match format HExxxxxx (6 digits)")
    private String studentCode;
    @Past(message = "Date of birth must be in the past")
    private LocalDate dob;
    @Pattern(regexp = "^(|MALE|FEMALE|OTHER)$", message = "Gender must be MALE, FEMALE or OTHER")
    private String gender;
    @Size(max = 100, message = "Major must be at most 100 characters")
    private String major;
    @Size(max = 255, message = "Address must be at most 255 characters")
    private String address;
    @Min(value = 1, message = "Year must be between 1 and 4")
    @Max(value = 4, message = "Year must be between 1 and 4")
    private Integer year;
    @Size(max = 50, message = "Manage class must be at most 50 characters")
    private String manageClass;
}
