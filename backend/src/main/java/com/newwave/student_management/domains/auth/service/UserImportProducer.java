package com.newwave.student_management.domains.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newwave.student_management.domains.auth.event.UserImportEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserImportProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    private static final String TOPIC = "user-import-topic";

    public void sendImportEvent(UserImportEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(TOPIC, event.getJobId().toString(), payload);
            log.info("Sent UserImportEvent to Kafka for job {}, row {}", event.getJobId(), event.getRowNumber());
        } catch (JsonProcessingException e) {
            log.error("Error serializing UserImportEvent", e);
            throw new RuntimeException("Error processing import event", e);
        }
    }
}
