package com.newwave.student_management.domains.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.newwave.student_management.common.exception.AppException;
import com.newwave.student_management.domains.auth.entity.ImportJobError;
import com.newwave.student_management.domains.auth.event.UserImportEvent;
import com.newwave.student_management.domains.auth.repository.ImportJobErrorRepository;
import com.newwave.student_management.domains.auth.repository.ImportJobRepository;
import com.newwave.student_management.domains.notification.dto.NotificationEvent;
import com.newwave.student_management.domains.notification.entity.SentNotification;
import com.newwave.student_management.domains.notification.repository.SentNotificationRepository;
import com.newwave.student_management.domains.notification.service.NotificationProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserImportConsumer {

    private final IAdminUserService adminUserService;
    private final ImportJobRepository importJobRepository;
    private final ImportJobErrorRepository importJobErrorRepository;
    private final SentNotificationRepository sentNotificationRepository;
    private final NotificationProducer notificationProducer;

    // Manual ObjectMapper to avoid missing beans and handle JSR310 dates
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @KafkaListener(topics = "user-import-topic", groupId = "${spring.application.name}-import-group")
    public void consumeImportEvent(String message) {
        log.info("Received UserImportEvent: {}", message);
        UserImportEvent event = null;
        try {
            event = objectMapper.readValue(message, UserImportEvent.class);

            // Re-use logic to create user, validate payload, insert db, send welcome email
            adminUserService.createUser(event.getRequestPayload());

            // If everything reaches here, it succeeded for this row
            importJobRepository.incrementSuccessCount(event.getJobId());
            checkAndCompleteJob(event);

        } catch (AppException e) {
            log.error("Validation error processing row in UserImportEvent", e);
            if (event != null) {
                String errMsg = e.getErrorCode().getMessage();
                if (e.getMessage() != null && !e.getMessage().equals(errMsg)) {
                    errMsg += ": " + e.getMessage();
                }
                saveErrorAndIncrementFailure(event, errMsg);
            }
        } catch (Exception e) {
            log.error("System error processing UserImportEvent", e);
            if (event != null) {
                saveErrorAndIncrementFailure(event, e.getMessage());
            }
        }
    }

    private void saveErrorAndIncrementFailure(UserImportEvent event, String errorMessage) {
        ImportJobError error = ImportJobError.builder()
                .importJob(importJobRepository.findById(event.getJobId()).orElse(null))
                .rowNumber(event.getRowNumber())
                .errorMessage(errorMessage)
                .build();

        try {
            if (event.getRequestPayload() != null) {
                error.setRawData(objectMapper.writeValueAsString(event.getRequestPayload()));
            } else if (event.getRawDataJson() != null) {
                error.setRawData(event.getRawDataJson());
            }
        } catch (Exception ignored) {
            log.warn("Could not serialize rawData to JSON string");
        }

        importJobErrorRepository.save(error);

        importJobRepository.incrementFailureCount(event.getJobId());
        checkAndCompleteJob(event);
    }

    private void checkAndCompleteJob(UserImportEvent event) {
        importJobRepository.findById(event.getJobId()).ifPresent(job -> {
            int currentProcessed = job.getSuccessCount() + job.getFailureCount();
            if (currentProcessed == event.getTotalRows()) {
                job.setStatus(job.getFailureCount() == 0 ? "COMPLETED" : "COMPLETED_WITH_ERRORS");
                importJobRepository.save(job);
                log.info("ImportJob {} has finished. Status: {}, Total: {}, Success: {}, Failure: {}",
                        job.getId(), job.getStatus(), job.getTotalRows(), job.getSuccessCount(), job.getFailureCount());

                // UC-12.4: Notify Admin when Import Job is finished
                try {
                    String title = "Import " + job.getRole() + " Completed";
                    String body = String.format(
                            "Tiến trình import %s từ Excel đã hoàn tất. Thành công: %d, Thất bại: %d.",
                            job.getRole(), job.getSuccessCount(), job.getFailureCount());

                    SentNotification sn = SentNotification.builder()
                            .title(title)
                            .body(body)
                            .actionUrl("http://localhost:8080/ui/clusters/local/all-topics/user-import-topic") // Redirect
                                                                                                               // to
                                                                                                               // Kafka
                                                                                                               // Topic
                                                                                                               // UI
                            .notificationType("PERSONAL")
                            .targetGroup("User: " + job.getCreatedBy().getUserId())
                            .status("PENDING")
                            .targetRecipientId(job.getCreatedBy().getUserId().toString())
                            .recipientCount(1)
                            .build();

                    SentNotification savedSn = sentNotificationRepository.save(sn);

                    NotificationEvent notifyEvent = NotificationEvent.builder()
                            .sentNotificationId(savedSn.getSentId())
                            .type("PERSONAL")
                            .build();

                    notificationProducer.sendNotificationEvent(notifyEvent);
                } catch (Exception e) {
                    log.error("Failed to push UC-12.4 Notification to Admin for Job {}", job.getId(), e);
                }
            }
        });
    }
}
