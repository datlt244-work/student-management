package com.newwave.student_management.infrastructure.security;

import com.newwave.student_management.domains.auth.entity.User;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Service
@Slf4j
public class JwtService {

    @Value("${spring.security.jwt.signer-key}")
    private String signerKey;

    @Value("${spring.security.jwt.expiration-seconds}")
    private long expiration;

    public String generateToken(User user, long tokenVersion) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS256);

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(user.getEmail())
                .issuer("student.management")
                .issueTime(new Date())
                .expirationTime(new Date(Instant.now().plus(expiration, ChronoUnit.SECONDS).toEpochMilli()))
                .jwtID(UUID.randomUUID().toString())
                .claim("userId", user.getUserId())
                .claim("role", user.getRole() != null ? user.getRole().getRoleName() : null)
                .claim("tv", tokenVersion)
                .build();

        Payload payload = new Payload(jwtClaimsSet.toJSONObject());
        JWSObject jweObject = new JWSObject(header, payload);

        try {
            jweObject.sign(new MACSigner(signerKey.getBytes()));
            return jweObject.serialize();
        }catch (JOSEException e){
            throw new RuntimeException("Error generate token: ", e);
        }
    }

    public String generateRefreshToken() {
        return UUID.randomUUID().toString();
    }

}
