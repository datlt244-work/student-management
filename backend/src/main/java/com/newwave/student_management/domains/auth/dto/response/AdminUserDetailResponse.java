package com.newwave.student_management.domains.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response for UC-11.2: GET /admin/users/{userId} — User detail + teacher/student profile.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDetailResponse {

    private UUID userId;
    private String email;
    private RoleSummary role;
    private String status;
    private boolean emailVerified;
    private String banReason;
    private LocalDateTime lastLoginAt;
    private int loginCount;
    private LocalDateTime createdAt;

    /** Populated when role is STUDENT; null otherwise. */
    private StudentProfilePart studentProfile;
    /** Populated when role is TEACHER; null otherwise. */
    private TeacherProfilePart teacherProfile;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoleSummary {
        private int roleId;
        private String roleName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DepartmentSummary {
        private Integer departmentId;
        private String name;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentProfilePart {
        private UUID studentId;
        private String studentCode;
        private String firstName;
        private String lastName;
        private LocalDate dob;
        private String gender;
        private String major;
        private String email;
        private String phone;
        private String address;
        private BigDecimal gpa;
        /** Year of study (e.g. 1, 2, 3, 4). */
        private Integer year;
        /** Manage class / class code (e.g. SE1701, IT-01). */
        private String manageClass;
        private DepartmentSummary department;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeacherProfilePart {
        private UUID teacherId;
        private String teacherCode;
        private String firstName;
        private String lastName;
        private String email;
        private String phone;
        private String specialization;
        private String academicRank;
        private String officeRoom;
        /** Degrees / qualifications (free text). */
        private String degreesQualification;
        private DepartmentSummary department;
    }
}
