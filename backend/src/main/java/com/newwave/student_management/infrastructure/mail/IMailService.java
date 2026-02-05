package com.newwave.student_management.infrastructure.mail;

public interface IMailService {

    void sendPasswordResetEmail(String toEmail, String fullName, String token);

    void sendEmail(String toEmail, String subject, String htmlContent);
}
