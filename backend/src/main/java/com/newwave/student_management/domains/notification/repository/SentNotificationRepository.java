package com.newwave.student_management.domains.notification.repository;

import com.newwave.student_management.domains.notification.entity.SentNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.UUID;

@Repository
public interface SentNotificationRepository extends JpaRepository<SentNotification, UUID> {
        @Query("SELECT sn FROM SentNotification sn WHERE " +
                        "sn.deletedAt IS NULL AND " +
                        "(CAST(:search AS string) IS NULL OR LOWER(sn.title) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))) AND "
                        +
                        "(CAST(:type AS string) IS NULL OR sn.notificationType = CAST(:type AS string)) AND " +
                        "(CAST(:startDate AS timestamp) IS NULL OR sn.createdAt >= :startDate) AND " +
                        "(CAST(:endDate AS timestamp) IS NULL OR sn.createdAt <= :endDate)")
        Page<SentNotification> findWithFilters(
                        @Param("search") String search,
                        @Param("type") String type,
                        @Param("startDate") java.time.LocalDateTime startDate,
                        @Param("endDate") java.time.LocalDateTime endDate,
                        Pageable pageable);

        long countByCreatedAtAfter(java.time.LocalDateTime date);

        java.util.List<SentNotification> findByStatusAndScheduledAtBefore(String status, java.time.LocalDateTime now);
}
