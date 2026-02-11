package com.newwave.student_management.infrastructure.mail;

public interface IMailService {

    void sendPasswordResetEmail(String toEmail, String fullName, String token);

    /**
     * Welcome email for newly created user (Admin). Contains email, temporary password and activation link.
     */
    void sendWelcomeEmail(String toEmail, String firstName, String lastName, String plainPassword, String activationToken);

    void sendEmail(String toEmail, String subject, String htmlContent);
}
