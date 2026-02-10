package com.newwave.student_management.domains.profile.service;

import com.newwave.student_management.domains.profile.dto.request.UpdateProfileRequest;
import com.newwave.student_management.domains.profile.dto.response.AvatarUploadResponse;
import com.newwave.student_management.domains.profile.dto.response.CombinedProfileResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface IProfileService {

    /**
     * Lấy thông tin profile đầy đủ: User + Student/Teacher profile
     * dựa theo role của user.
     */
    CombinedProfileResponse getMyProfile(UUID userId, String role);

    /**
     * Cập nhật profile: Student (phone, address) hoặc Teacher (phone).
     * Trả về combined profile đã cập nhật.
     */
    CombinedProfileResponse updateMyProfile(UUID userId, String role, UpdateProfileRequest request);

    /**
     * Upload ảnh đại diện:
     * 1. Validate (size, format, magic bytes)
     * 2. Resize (max 500x500)
     * 3. Upload lên MinIO (avatar/{userId}/{uuid}.jpg)
     * 4. Update users.profile_picture_url
     * 5. Delete ảnh cũ trên MinIO (nếu có)
     *
     * @param userId UUID user từ JWT
     * @param file   ảnh upload
     * @return AvatarUploadResponse chứa URL mới
     */
    AvatarUploadResponse uploadAvatar(UUID userId, MultipartFile file);
}
