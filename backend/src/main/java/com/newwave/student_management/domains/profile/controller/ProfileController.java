package com.newwave.student_management.domains.profile.controller;

import com.newwave.student_management.common.dto.ApiResponse;
import com.newwave.student_management.domains.profile.dto.response.CombinedProfileResponse;
import com.newwave.student_management.domains.profile.service.IProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
                    + "dựa theo role của user đang đăng nhập. "
                    + "Student sẽ có studentProfile, Teacher sẽ có teacherProfile, Admin chỉ có thông tin user."
    )
    public ApiResponse<CombinedProfileResponse> getMyProfile(
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getClaim("userId"));
        String role = jwt.getClaim("role");

        CombinedProfileResponse profile = profileService.getMyProfile(userId, role);
        return ApiResponse.success(profile);
    }
}
