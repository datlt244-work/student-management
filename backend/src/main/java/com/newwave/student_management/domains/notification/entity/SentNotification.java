package com.newwave.student_management.domains.notification.entity;

import com.newwave.student_management.common.entity.JpaBaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "sent_notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SentNotification extends JpaBaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "sent_id")
    private UUID sentId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String body;

    @Column(name = "action_url")
    private String actionUrl;

    @Column(name = "notification_type")
    private String notificationType; // BROADCAST, TARGETED, PERSONAL

    @Column(name = "recipient_count")
    private Integer recipientCount;

    @Column(name = "target_group")
    private String targetGroup; // "All", "Department: CS", etc.

    @Column(name = "status")
    private String status; // PENDING, SENT, FAILED, CANCELLED

    @Column(name = "scheduled_at")
    private java.time.LocalDateTime scheduledAt;

    @Column(name = "sent_at")
    private java.time.LocalDateTime sentAt;

    // Original criteria for re-sending if scheduled
    @Column(name = "target_role")
    private String targetRole;

    @Column(name = "target_department_id")
    private Long targetDepartmentId;

    @Column(name = "target_class_code")
    private String targetClassCode;

    @Column(name = "target_recipient_id")
    private String targetRecipientId;
}
