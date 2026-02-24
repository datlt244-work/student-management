package com.newwave.student_management.domains.notification.repository;

import com.newwave.student_management.domains.notification.entity.FcmToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FcmTokenRepository extends JpaRepository<FcmToken, UUID> {
    Optional<FcmToken> findByToken(String token);
    List<FcmToken> findByUser_UserId(UUID userId);
    void deleteByToken(String token);
}
