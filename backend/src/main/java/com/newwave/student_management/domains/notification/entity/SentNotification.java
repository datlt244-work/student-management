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
}
