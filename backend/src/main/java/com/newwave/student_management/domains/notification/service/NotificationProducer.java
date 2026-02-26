package com.newwave.student_management.domains.notification.service;

import com.newwave.student_management.domains.notification.dto.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC = "notification-topic";

    public void sendNotificationEvent(NotificationEvent event) {
        log.info("Sending notification event to Kafka: {}", event);
        kafkaTemplate.send(TOPIC, event);
    }
}
