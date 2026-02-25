package com.newwave.student_management.domains.notification.repository;

import com.newwave.student_management.domains.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    Page<Notification> findByUser_UserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    long countByUser_UserIdAndReadFalse(java.util.UUID userId);

    @org.springframework.data.jpa.repository.Query("SELECT n FROM Notification n WHERE n.user.userId = :userId AND n.read = false")
    java.util.List<Notification> findAllByUserIdAndIsReadFalse(
            @org.springframework.data.repository.query.Param("userId") java.util.UUID userId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE Notification n SET n.read = true WHERE n.user.userId = :userId AND n.notificationId = :notificationId")
    void markAsRead(
            @org.springframework.data.repository.query.Param("userId") java.util.UUID userId,
            @org.springframework.data.repository.query.Param("notificationId") java.util.UUID notificationId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE Notification n SET n.read = true WHERE n.user.userId = :userId AND n.read = false")
    void markAllAsRead(
            @org.springframework.data.repository.query.Param("userId") java.util.UUID userId);
}
