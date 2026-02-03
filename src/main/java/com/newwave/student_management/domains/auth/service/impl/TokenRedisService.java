package com.newwave.student_management.domains.auth.service.impl;

import com.newwave.student_management.domains.auth.service.ITokenRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class TokenRedisService implements ITokenRedisService {

    private static final String FAIL_KEY_PREFIX = "auth:login:fail:";
    private static final String LOCK_KEY_PREFIX = "auth:login:lock:";
    private static final String REFRESH_KEY_PREFIX = "auth:refresh:";
    private static final String REFRESH_TOKEN_TO_USER_PREFIX = "auth:refresh-token:";
    private static final String ACCESS_BLACKLIST_PREFIX = "auth:access:blacklist:";
    private static final String RESET_COUNT_PREFIX = "auth:reset:count:";
    private static final String RESET_TOKEN_PREFIX = "auth:reset:token:";

    private final StringRedisTemplate stringRedisTemplate;

    @Value("${auth.login.max-failed-attempts:5}")
    private int maxFailedAttempts;

    @Value("${auth.login.fail-window-seconds:900}")
    private long failWindowSeconds;

    @Value("${auth.login.lock-seconds:900}") 
    private long lockSeconds;

    @Value("${spring.security.jwt.refresh-expiration-seconds:604800}") 
    private long refreshExpirationSeconds;

    @Override
    public boolean isRateLimited(String email) {
        String lockKey = LOCK_KEY_PREFIX + normalize(email);
        Boolean exists = stringRedisTemplate.hasKey(lockKey);
        return Boolean.TRUE.equals(exists);
    }

    @Override
    public int recordFailedAttempt(String email) {
        String normalized = normalize(email);
        String failKey = FAIL_KEY_PREFIX + normalized;
        Long count = stringRedisTemplate.opsForValue().increment(failKey);
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(failKey, Duration.ofSeconds(failWindowSeconds));
        }

        long safeCount = count == null ? 0 : count;
        int remaining = Math.max(0, maxFailedAttempts - (int) safeCount);

        if (safeCount >= maxFailedAttempts) {
            String lockKey = LOCK_KEY_PREFIX + normalized;
            stringRedisTemplate.opsForValue().set(lockKey, "1", Duration.ofSeconds(lockSeconds));
            stringRedisTemplate.delete(failKey);
            remaining = 0;
        }

        return remaining;
    }

    @Override
    public void resetFailedAttempts(String email) {
        String normalized = normalize(email);
        stringRedisTemplate.delete(FAIL_KEY_PREFIX + normalized);
        stringRedisTemplate.delete(LOCK_KEY_PREFIX + normalized);
    }

    @Override
    public String createAndStoreRefreshToken(UUID userId) {
        Objects.requireNonNull(userId, "userId");
        String refreshToken = UUID.randomUUID().toString();
        String userKey = REFRESH_KEY_PREFIX + userId;
        String tokenKey = REFRESH_TOKEN_TO_USER_PREFIX + refreshToken;

        // Lưu 2 chiều: userId -> token và token -> userId để validate nhanh
        stringRedisTemplate.opsForValue().set(userKey, refreshToken, Duration.ofSeconds(refreshExpirationSeconds));
        stringRedisTemplate.opsForValue().set(tokenKey, userId.toString(), Duration.ofSeconds(refreshExpirationSeconds));
        return refreshToken;
    }

    @Override
    public UUID getUserIdByRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return null;
        }
        String tokenKey = REFRESH_TOKEN_TO_USER_PREFIX + refreshToken;
        String userIdStr = stringRedisTemplate.opsForValue().get(tokenKey);
        if (userIdStr == null || userIdStr.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(userIdStr);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid UUID stored for refresh token={}, value={}", refreshToken, userIdStr);
            return null;
        }
    }

    @Override
    public void deleteRefreshToken(UUID userId, String refreshToken) {
        if (userId == null && (refreshToken == null || refreshToken.isBlank())) {
            return;
        }

        if (userId != null) {
            String userKey = REFRESH_KEY_PREFIX + userId;
            stringRedisTemplate.delete(userKey);
        }

        if (refreshToken != null && !refreshToken.isBlank()) {
            String tokenKey = REFRESH_TOKEN_TO_USER_PREFIX + refreshToken;
            stringRedisTemplate.delete(tokenKey);
        }
    }

    @Override
    public void blacklistAccessToken(String jti, long expiresInSeconds) {
        if (jti == null || jti.isBlank() || expiresInSeconds <= 0) {
            return;
        }
        String key = ACCESS_BLACKLIST_PREFIX + jti;
        stringRedisTemplate.opsForValue().set(key, "1", Duration.ofSeconds(expiresInSeconds));
    }

    @Override
    public boolean isAccessTokenBlacklisted(String jti) {
        if (jti == null || jti.isBlank()) {
            return false;
        }
        String key = ACCESS_BLACKLIST_PREFIX + jti;
        Boolean exists = stringRedisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    }

    @Override
    public int incrementResetPasswordCount(String email, long windowSeconds) {
        String normalized = normalize(email);
        String key = RESET_COUNT_PREFIX + normalized;
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
        }
        return count == null ? 0 : count.intValue();
    }

    @Override
    public void deleteResetPasswordToken(UUID userId) {
        if (userId == null) {
            return;
        }
        String key = RESET_TOKEN_PREFIX + userId;
        stringRedisTemplate.delete(key);
    }

    @Override
    public String createResetPasswordToken(UUID userId, long ttlSeconds) {
        if (userId == null) {
            return null;
        }
        String token = UUID.randomUUID().toString();
        String key = RESET_TOKEN_PREFIX + userId;
        stringRedisTemplate.opsForValue().set(key, token, Duration.ofSeconds(ttlSeconds));
        return token;
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
