package com.newwave.student_management.domains.auth.service;

import com.newwave.student_management.domains.auth.dto.request.ChangePasswordRequest;
import com.newwave.student_management.domains.auth.dto.request.ForgotPasswordRequest;
import com.newwave.student_management.domains.auth.dto.request.LoginRequest;
import com.newwave.student_management.domains.auth.dto.request.RefreshTokenRequest;
import com.newwave.student_management.domains.auth.dto.request.ResetPasswordRequest;
import com.newwave.student_management.domains.auth.dto.response.ChangePasswordResponse;
import com.newwave.student_management.domains.auth.dto.response.LoginResponse;

import java.util.UUID;

public interface IAuthService {
    LoginResponse authenticate(LoginRequest loginRequest, String clientIp);

    LoginResponse refreshToken(RefreshTokenRequest request);

    void logout(String accessToken, String refreshToken);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);

    ChangePasswordResponse changePassword(UUID userId, ChangePasswordRequest request);
}
