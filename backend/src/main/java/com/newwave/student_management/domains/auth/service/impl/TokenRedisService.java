package com.newwave.student_management.domains.auth.service.impl;

import com.newwave.student_management.domains.auth.service.ITokenRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;
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
    private static final String RESET_TOKEN_TO_USER_PREFIX = "auth:reset:token-to-user:";
    private static final String TOKEN_VERSION_PREFIX = "auth:token-version:";

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

        Duration ttl = Duration.ofSeconds(refreshExpirationSeconds);
        // User -> ZSET (score = expiry timestamp) tránh "rò rỉ bộ nhớ" khi Token Key hết TTL trước Set
        long expiryTime = System.currentTimeMillis() + ttl.toMillis();
        stringRedisTemplate.opsForZSet().add(userKey, refreshToken, expiryTime);
        // Token -> userId (để validate nhanh)
        stringRedisTemplate.opsForValue().set(tokenKey, userId.toString(), ttl);
        cleanupExpiredTokensFromUserZSet(userKey);
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
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }

        // Luôn xóa "con đường" token -> userId trước
        String tokenKey = REFRESH_TOKEN_TO_USER_PREFIX + refreshToken;
        String resolvedUserId = userId != null ? userId.toString() : stringRedisTemplate.opsForValue().get(tokenKey);
        stringRedisTemplate.delete(tokenKey);

        // Xóa token khỏi ZSET user -> tokens (con đường ngược)
        if (resolvedUserId != null && !resolvedUserId.isBlank()) {
            String userKey = REFRESH_KEY_PREFIX + resolvedUserId;
            stringRedisTemplate.opsForZSet().remove(userKey, refreshToken);
            cleanupExpiredTokensFromUserZSet(userKey);
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
        String userKey = RESET_TOKEN_PREFIX + userId;
        String token = stringRedisTemplate.opsForValue().get(userKey);
        stringRedisTemplate.delete(userKey);
        if (token != null && !token.isBlank()) {
            stringRedisTemplate.delete(RESET_TOKEN_TO_USER_PREFIX + token);
        }
    }

    @Override
    public String createResetPasswordToken(UUID userId, long ttlSeconds) {
        if (userId == null) {
            return null;
        }
        String token = UUID.randomUUID().toString();
        Duration ttl = Duration.ofSeconds(ttlSeconds);
        String userKey = RESET_TOKEN_PREFIX + userId;
        String tokenToUserKey = RESET_TOKEN_TO_USER_PREFIX + token;
        stringRedisTemplate.opsForValue().set(userKey, token, ttl);
        stringRedisTemplate.opsForValue().set(tokenToUserKey, userId.toString(), ttl);
        return token;
    }

    @Override
    public UUID getUserIdByResetToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String key = RESET_TOKEN_TO_USER_PREFIX + token;
        String userIdStr = stringRedisTemplate.opsForValue().get(key);
        if (userIdStr == null || userIdStr.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(userIdStr);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid UUID for reset token, value={}", userIdStr);
            return null;
        }
    }

    @Override
    public int deleteAllRefreshTokensForUser(UUID userId) {
        if (userId == null) {
            return 0;
        }
        String userKey = REFRESH_KEY_PREFIX + userId;
        Set<String> tokens = stringRedisTemplate.opsForZSet().range(userKey, 0, -1);
        int deletedCount = 0;
        if (tokens != null && !tokens.isEmpty()) {
            deletedCount = tokens.size();
            for (String token : tokens) {
                stringRedisTemplate.delete(REFRESH_TOKEN_TO_USER_PREFIX + token);
            }
        }
        stringRedisTemplate.delete(userKey);
        return deletedCount;
    }

    @Override
    public long getTokenVersion(UUID userId) {
        if (userId == null) return 0L;
        String key = TOKEN_VERSION_PREFIX + userId;
        String val = stringRedisTemplate.opsForValue().get(key);
        if (val == null || val.isBlank()) return 0L;
        try {
            return Long.parseLong(val);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    @Override
    public void incrementTokenVersion(UUID userId) {
        if (userId == null) return;
        String key = TOKEN_VERSION_PREFIX + userId;
        stringRedisTemplate.opsForValue().increment(key);
    }

    /**
     * Tự dọn các token đã hết hạn khỏi ZSET (score &lt; now).
     * Giải quyết lệch pha TTL: Token Key mất trước khi có thể xóa khỏi Set → rò rỉ bộ nhớ.
     */
    private void cleanupExpiredTokensFromUserZSet(String userKey) {
        if (userKey == null || userKey.isBlank()) {
            return;
        }
        long now = System.currentTimeMillis();
        stringRedisTemplate.opsForZSet().removeRangeByScore(userKey, 0, now);
        Long size = stringRedisTemplate.opsForZSet().zCard(userKey);
        if (size != null && size == 0) {
            stringRedisTemplate.delete(userKey);
        }
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
