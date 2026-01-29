package com.newwave.student_management.domains.auth.service;

import com.newwave.student_management.domains.auth.dto.request.LoginRequest;
import com.newwave.student_management.domains.auth.dto.response.LoginResponse;

public interface IAuthService {
    LoginResponse authenticate(LoginRequest loginRequest, String clientIp);
}
