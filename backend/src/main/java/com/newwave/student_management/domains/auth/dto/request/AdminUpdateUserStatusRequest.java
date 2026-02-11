package com.newwave.student_management.domains.auth.dto.request;

import com.newwave.student_management.domains.auth.entity.UserStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * UC-11.5: Admin thay đổi trạng thái User (Block/Unblock).
 */
@Data
public class AdminUpdateUserStatusRequest {

    @NotNull(message = "VALIDATION_ERROR")
    private UserStatus status;

    @Size(max = 255, message = "Ban reason must be at most 255 characters")
    private String banReason;
}

