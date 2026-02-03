package com.newwave.student_management.domains.auth.service;

import java.util.UUID;

public interface ITokenRedisService {

    boolean isRateLimited(String email);

    int recordFailedAttempt(String email);

    void resetFailedAttempts(String email);

    String createAndStoreRefreshToken(UUID userId);

    UUID getUserIdByRefreshToken(String refreshToken);

    void deleteRefreshToken(UUID userId, String refreshToken);

    void blacklistAccessToken(String jti, long expiresInSeconds);

    boolean isAccessTokenBlacklisted(String jti);

    int incrementResetPasswordCount(String email, long windowSeconds);

    void deleteResetPasswordToken(UUID userId);

    String createResetPasswordToken(UUID userId, long ttlSeconds);

}
