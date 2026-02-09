package com.newwave.student_management.domains.profile.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CombinedProfileResponse {

    // ========== FROM USERS TABLE ==========
    private UUID userId;
    private String email;
    private String profilePictureUrl;
    private String role;
    private String status;
    private boolean emailVerified;
    private LocalDateTime lastLoginAt;
    private int loginCount;
    private LocalDateTime createdAt;

    // ========== FROM STUDENTS TABLE (null if not STUDENT) ==========
    private StudentProfileResponse studentProfile;

    // ========== FROM TEACHERS TABLE (null if not TEACHER) ==========
    private TeacherProfileResponse teacherProfile;
}
