package com.newwave.student_management.domains.auth.dto.request;

import lombok.Data;

@Data
public class LogoutRequest {

    private String refreshToken;
}
