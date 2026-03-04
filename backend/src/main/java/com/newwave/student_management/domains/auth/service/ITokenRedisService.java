package com.newwave.student_management.domains.auth.service;

import java.util.UUID;

public interface ITokenRedisService {

    boolean isRateLimited(String email);

    int recordFailedAttempt(String email);

    void resetFailedAttempts(String email);

    // ── IP-based rate limiting (chống Password Spraying) ──────────────────

    /**
     * Kiểm tra IP có đang bị block do quá nhiều login fail không.
     * Key: auth:login:ip:lock:{ip}
     */
    boolean isIpRateLimited(String ip);

    /**
     * Ghi nhận 1 login fail từ IP này.
     * Trả về số attempt còn lại trước khi bị block.
     * Key: auth:login:ip:fail:{ip} TTL = IP_FAIL_WINDOW_SECONDS
     */
    int recordIpFailedAttempt(String ip);

    /**
     * Reset fail counter của IP khi login thành công.
     */
    void resetIpFailedAttempts(String ip);

    String createAndStoreRefreshToken(UUID userId);

    UUID getUserIdByRefreshToken(String refreshToken);

    void deleteRefreshToken(UUID userId, String refreshToken);

    void blacklistAccessToken(String jti, long expiresInSeconds);

    boolean isAccessTokenBlacklisted(String jti);

    int incrementResetPasswordCount(String email, long windowSeconds);

    void deleteResetPasswordToken(UUID userId);

    String createResetPasswordToken(UUID userId, long ttlSeconds);

    UUID getUserIdByResetToken(String token);

    /** Activation token for new users (TTL e.g. 72h). */
    String createActivationToken(UUID userId, long ttlSeconds);

    UUID getUserIdByActivationToken(String token);

    void deleteActivationToken(UUID userId);

    int deleteAllRefreshTokensForUser(UUID userId);

    /**
     * Token version for user. Incremented on password change so all existing JWTs
     * are invalidated.
     */
    long getTokenVersion(UUID userId);

    void incrementTokenVersion(UUID userId);

}
