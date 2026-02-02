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
        String key = REFRESH_KEY_PREFIX + userId;
        stringRedisTemplate.opsForValue().set(key, refreshToken, Duration.ofSeconds(refreshExpirationSeconds));
        return refreshToken;
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
