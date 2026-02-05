package com.newwave.student_management.infrastructure.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnProperty(name = "spring.mail.username", havingValue = "false", matchIfMissing = true)
public class NoOpMailService implements IMailService {

    @Override
    @Async
    public void sendPasswordResetEmail(String toEmail, String fullName, String token) {
        log.debug("[No-op] Password reset email would be sent to: {} (token omitted)", toEmail);
    }

    @Override
    @Async
    public void sendEmail(String toEmail, String subject, String htmlContent) {
        log.debug("[No-op] Email would be sent to: {} subject: {}", toEmail, subject);
    }
}
