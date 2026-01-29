package com.newwave.student_management.domains.auth.service;

import java.util.UUID;

public interface ITokenRedisService {

    boolean isRateLimited(String email);

    int recordFailedAttempt(String email);

    void resetFailedAttempts(String email);

    String createAndStoreRefreshToken(UUID userId);

}
