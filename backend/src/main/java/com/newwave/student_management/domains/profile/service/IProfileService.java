package com.newwave.student_management.domains.profile.service;

import com.newwave.student_management.domains.profile.dto.response.CombinedProfileResponse;

import java.util.UUID;

public interface IProfileService {

    /**
     * Lấy thông tin profile đầy đủ: User + Student/Teacher profile
     * dựa theo role của user.
     *
     * @param userId UUID của user từ JWT
     * @param role   role name từ JWT (student, teacher, admin)
     * @return CombinedProfileResponse chứa tất cả thông tin
     */
    CombinedProfileResponse getMyProfile(UUID userId, String role);
}
