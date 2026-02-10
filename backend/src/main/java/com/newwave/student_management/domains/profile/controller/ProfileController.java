package com.newwave.student_management.domains.profile.controller;

import com.newwave.student_management.common.dto.ApiResponse;
import com.newwave.student_management.domains.profile.dto.request.UpdateProfileRequest;
import com.newwave.student_management.domains.profile.dto.response.AvatarUploadResponse;
import com.newwave.student_management.domains.profile.dto.response.CombinedProfileResponse;
import com.newwave.student_management.domains.profile.service.IProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
@Tag(name = "Profile", description = "API quản lý thông tin profile người dùng")
public class ProfileController {

    private final IProfileService profileService;

    @GetMapping("/me")
    @Operation(
            summary = "Lấy thông tin profile đầy đủ",
            description = "Trả về thông tin User + Student/Teacher profile trong 1 API call, "
                    + "dựa theo role của user đang đăng nhập."
    )
    public ApiResponse<CombinedProfileResponse> getMyProfile(
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getClaim("userId"));
        String role = jwt.getClaim("role");
        return ApiResponse.success(profileService.getMyProfile(userId, role));
    }

    @PutMapping("/me")
    @Operation(
            summary = "Cập nhật profile",
            description = "Cập nhật thông tin Student (phone, address) hoặc Teacher (phone). "
                    + "Chỉ update các field được gửi (partial update)."
    )
    public ApiResponse<CombinedProfileResponse> updateMyProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        UUID userId = UUID.fromString(jwt.getClaim("userId"));
        String role = jwt.getClaim("role");
        return ApiResponse.success(profileService.updateMyProfile(userId, role, request));
    }

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload ảnh đại diện",
            description = "Upload hoặc thay thế ảnh đại diện. "
                    + "Hỗ trợ JPG, PNG, WebP. Tối đa 5MB. "
                    + "Ảnh sẽ được resize (max 500x500) và lưu lên MinIO."
    )
    public ApiResponse<AvatarUploadResponse> uploadAvatar(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("file") MultipartFile file
    ) {
        UUID userId = UUID.fromString(jwt.getClaim("userId"));
        return ApiResponse.success(profileService.uploadAvatar(userId, file));
    }
}
