package com.newwave.student_management.domains.profile.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvatarUploadResponse {

    /**
     * Relative path stored in DB (vd: "avatar/userId/uuid.jpg")
     */
    private String profilePictureUrl;

    /**
     * Full public URL to access the image
     */
    private String fullUrl;

    private String message;
}
