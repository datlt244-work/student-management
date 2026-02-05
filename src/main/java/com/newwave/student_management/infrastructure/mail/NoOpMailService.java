package com.newwave.student_management.infrastructure.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Kế hoạch dự phòng khi SmtpMailService không được tạo (mail chưa cấu hình).
 * Tránh xung đột điều kiện với @ConditionalOnProperty khi spring.mail.username="false".
 */
@Slf4j
@Service
@ConditionalOnMissingBean(SmtpMailService.class)
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
