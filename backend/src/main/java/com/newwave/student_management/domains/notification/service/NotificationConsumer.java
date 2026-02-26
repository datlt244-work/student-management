package com.newwave.student_management.domains.notification.service;

import com.newwave.student_management.domains.notification.dto.NotificationEvent;
import com.newwave.student_management.domains.notification.entity.SentNotification;
import com.newwave.student_management.domains.notification.repository.SentNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationInternalService notificationService;
    private final SentNotificationRepository sentNotificationRepository;

    @KafkaListener(topics = "notification-topic", groupId = "${spring.application.name}-group")
    public void listenNotification(NotificationEvent event) {
        log.info("Received notification event from Kafka: {}", event);

        sentNotificationRepository.findById(event.getSentNotificationId()).ifPresent((SentNotification notif) -> {
            // Chỉ xử lý nếu status là PENDING hoặc PROCESSING (vừa được gửi từ producer)
            // Hoặc có thể tạo thêm status PROCESSING để tránh tranh chấp
            notificationService.processScheduledNotification(notif);
        });
    }
}
