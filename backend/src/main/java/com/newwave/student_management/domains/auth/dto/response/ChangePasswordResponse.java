package com.newwave.student_management.domains.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordResponse {

    private String message;

    /**
     * Number of other devices/sessions that were logged out
     * (based on deleted refresh tokens).
     */
    private int loggedOutDevices;
}

