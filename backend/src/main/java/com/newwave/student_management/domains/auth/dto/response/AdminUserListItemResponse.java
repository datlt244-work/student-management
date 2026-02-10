package com.newwave.student_management.domains.auth.dto.response;

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
public class AdminUserListItemResponse {

    private UUID userId;
    private String email;
    private RoleSummary role;
    private String status;
    private boolean emailVerified;
    private String profilePictureUrl;
    private LocalDateTime lastLoginAt;
    private int loginCount;
    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoleSummary {
        private int roleId;
        private String roleName;
    }
}


