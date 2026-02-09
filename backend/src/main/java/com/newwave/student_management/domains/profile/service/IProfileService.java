package com.newwave.student_management.domains.profile.service;

import com.newwave.student_management.domains.profile.dto.request.UpdateProfileRequest;
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

    /**
     * Cập nhật profile: Student (phone, address) hoặc Teacher (phone).
     * Trả về combined profile đã cập nhật (cùng format với getMyProfile).
     *
     * @param userId  UUID của user từ JWT
     * @param role    role name từ JWT
     * @param request chứa studentProfile hoặc teacherProfile cần update
     * @return CombinedProfileResponse đã cập nhật
     */
    CombinedProfileResponse updateMyProfile(UUID userId, String role, UpdateProfileRequest request);
}
