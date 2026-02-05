package com.newwave.student_management.infrastructure.security;

import com.newwave.student_management.domains.auth.service.ITokenRedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Reject JWTs whose jti has been blacklisted in Redis.
 *
 * This makes logout/token invalidation take effect immediately (instead of waiting for exp).
 */
@Component
@RequiredArgsConstructor
public class JwtBlacklistValidator implements OAuth2TokenValidator<Jwt> {

    private final ITokenRedisService tokenRedisService;

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        String jti = token.getId();
        if (jti != null && tokenRedisService.isAccessTokenBlacklisted(jti)) {
            OAuth2Error error = new OAuth2Error(
                    "token_blacklisted",
                    "Token has been revoked",
                    null
            );
            return OAuth2TokenValidatorResult.failure(error);
        }
        return OAuth2TokenValidatorResult.success();
    }
}

