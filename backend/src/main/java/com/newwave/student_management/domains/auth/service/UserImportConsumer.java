package com.newwave.student_management.domains.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.newwave.student_management.common.exception.AppException;
import com.newwave.student_management.domains.auth.entity.ImportJobError;
import com.newwave.student_management.domains.auth.event.UserImportEvent;
import com.newwave.student_management.domains.auth.repository.ImportJobErrorRepository;
import com.newwave.student_management.domains.auth.repository.ImportJobRepository;
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
            }
        });
    }
}
