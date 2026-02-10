package com.newwave.student_management.infrastructure.security;

import com.newwave.student_management.domains.auth.service.ITokenRedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Reject JWTs whose token version ("tv") is older than the current version in Redis.
 * When user changes password we increment the version, so all existing tokens (all devices/tabs) become invalid.
 */
@Component
@RequiredArgsConstructor
public class JwtTokenVersionValidator implements OAuth2TokenValidator<Jwt> {

    private final ITokenRedisService tokenRedisService;

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        String userIdStr = token.getClaimAsString("userId");
        if (userIdStr == null || userIdStr.isBlank()) {
            return OAuth2TokenValidatorResult.success();
        }
        UUID userId;
        try {
            userId = UUID.fromString(userIdStr);
        } catch (IllegalArgumentException e) {
            return OAuth2TokenValidatorResult.success();
        }

        long tokenVersion = 0L;
        Object tvClaim = token.getClaim("tv");
        if (tvClaim instanceof Number) {
            tokenVersion = ((Number) tvClaim).longValue();
        }

        long currentVersion = tokenRedisService.getTokenVersion(userId);
        if (tokenVersion < currentVersion) {
            OAuth2Error error = new OAuth2Error(
                    "token_version_invalid",
                    "Token has been invalidated (e.g. after password change). Please login again.",
                    null
            );
            return OAuth2TokenValidatorResult.failure(error);
        }
        return OAuth2TokenValidatorResult.success();
    }
}
