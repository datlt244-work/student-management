package com.newwave.student_management.domains.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * UC: Admin update user profile (Teacher/Student).
 * Admin can update profile fields only (NOT: email, password, status, role).
 *
 * All fields are optional; when provided, they must satisfy validation constraints.
 * Role-specific applicability is enforced in service layer based on user's role.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminUpdateUserProfileRequest {

    // Common fields (Teacher/Student)
    @Size(max = 50, message = "First name must be at most 50 characters")
    private String firstName;

    @Size(max = 50, message = "Last name must be at most 50 characters")
    private String lastName;

    @Size(max = 20, message = "Phone must be at most 20 characters")
    @Pattern(regexp = "^(|\\d{1,20})$", message = "Phone must contain digits only")
    private String phone;

    @Positive(message = "Department ID must be positive")
    private Integer departmentId;

    // Teacher-only fields
    @Size(max = 10, message = "Teacher code must be at most 10 characters")
    @Pattern(regexp = "^(|[Hh][Jj][0-9]{6})$", message = "Teacher code must match format HJxxxxxx (6 digits)")
    private String teacherCode;

    @Size(max = 100, message = "Specialization must be at most 100 characters")
    private String specialization;

    @Size(max = 50, message = "Academic rank must be at most 50 characters")
    private String academicRank;

    @Size(max = 50, message = "Office room must be at most 50 characters")
    private String officeRoom;

    private String degreesQualification; // TEXT

    // Student-only fields
    @Size(max = 10, message = "Student code must be at most 10 characters")
    @Pattern(regexp = "^(|[Hh][Ee][0-9]{6})$", message = "Student code must match format HExxxxxx (6 digits)")
    private String studentCode;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dob;

    @Pattern(regexp = "^(MALE|FEMALE|OTHER)$", message = "Gender must be MALE, FEMALE or OTHER")
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

