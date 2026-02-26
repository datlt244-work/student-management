package com.newwave.student_management.domains.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {
    private UUID sentNotificationId;
    private String type; // BROADCAST, TARGETED, PERSONAL
}
