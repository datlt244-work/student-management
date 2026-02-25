package com.newwave.student_management.domains.notification.service;

import com.newwave.student_management.domains.notification.entity.SentNotification;
import com.newwave.student_management.domains.notification.repository.SentNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationScheduler {

    private final SentNotificationRepository repository;
    private final NotificationInternalService notificationService;

    /**
     * Check for scheduled notifications every 1 minute.
     */
    @Scheduled(cron = "0 * * * * *")
    public void processScheduledNotifications() {
        LocalDateTime now = LocalDateTime.now();
        List<SentNotification> pending = repository.findByStatusAndScheduledAtBefore("PENDING", now);

        if (!pending.isEmpty()) {
            log.info("Found {} pending scheduled notifications to process", pending.size());
            for (SentNotification notif : pending) {
                try {
                    notificationService.processScheduledNotification(notif);
                } catch (Exception e) {
                    log.error("Failed to process scheduled notification {}: {}", notif.getSentId(), e.getMessage());
                }
            }
        }
    }
}
