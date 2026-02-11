package com.newwave.student_management.infrastructure.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Year;


@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${spring.mail.username:}') && !'${spring.mail.username:}'.equalsIgnoreCase('false')")
public class SmtpMailService implements IMailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from}")
    private String fromEmail;

    @Value("${app.name:Student Management}")
    private String appName;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${app.backend-url:http://localhost:6868/api/v1}")
    private String backendUrl;

    @Override
    @Async
    public void sendPasswordResetEmail(String toEmail, String fullName, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(appName + " - Password Reset");

            String resetLink = frontendUrl + "/reset-password?token=" + token;
            String htmlContent = buildPasswordResetEmailHtml(fullName, resetLink);

            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Password reset email sent to: {}", toEmail);

        } catch (MessagingException e) {
            log.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }

    @Override
    @Async
    public void sendWelcomeEmail(String toEmail, String firstName, String lastName, String plainPassword, String activationToken) {
        try {
            String fullName = (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "").trim();
            if (fullName.isBlank()) fullName = toEmail;
            String subject = "[" + appName + "] Tài khoản của bạn đã được tạo";
            String activationLink = frontendUrl + "/activate?token=" + (activationToken != null ? activationToken : "");
            String htmlContent = buildWelcomeEmailHtml(fullName, toEmail, plainPassword != null ? plainPassword : "", activationLink);
            sendEmail(toEmail, subject, htmlContent);
            log.info("Welcome email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send welcome email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send welcome email", e);
        }
    }

    private String buildWelcomeEmailHtml(String fullName, String email, String plainPassword, String activationLink) {
        String currentYear = Year.now().toString();
        return """
            <!DOCTYPE html>
            <html lang="vi">
            <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
            body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 0; background-color: #f8f7f5; color: #1c160d; }
            .container { max-width: 560px; margin: 30px auto; background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 2px 12px rgba(0,0,0,0.06); border: 1px solid #e8ddce; }
            .header { background-color: #f49d25; padding: 28px 30px; text-align: center; }
            .header h1 { margin: 0; font-size: 22px; color: #ffffff; font-weight: 700; }
            .content { padding: 32px 30px; }
            .content h2 { color: #1c160d; font-size: 20px; margin: 0 0 16px 0; font-weight: 600; }
            .content p { color: #5c4d3c; font-size: 15px; line-height: 1.7; margin: 0 0 14px 0; }
            .info { background: #f8f7f5; padding: 16px; border-radius: 8px; margin: 16px 0; font-family: monospace; font-size: 14px; }
            .btn-wrap { text-align: center; margin: 28px 0; }
            .button { display: inline-block; padding: 14px 36px; background-color: #f49d25; color: #ffffff !important; text-decoration: none; border-radius: 8px; font-weight: 700; font-size: 15px; }
            .footer { background-color: #f8f7f5; padding: 20px 30px; text-align: center; border-top: 1px solid #e8ddce; }
            .footer p { color: #9c7a49; font-size: 12px; margin: 0; }
            </style></head>
            <body>
            <div class="container">
            <div class="header"><h1>%s</h1></div>
            <div class="content">
            <h2>Tài khoản của bạn đã được tạo</h2>
            <p>Xin chào <strong>%s</strong>,</p>
            <p>Tài khoản của bạn đã được tạo trong hệ thống Student Management.</p>
            <div class="info">
            <p style="margin:0 0 8px 0;">Email: %s</p>
            <p style="margin:0;">Mật khẩu: %s</p>
            </div>
            <p>Vui lòng click link dưới đây để kích hoạt tài khoản:</p>
            <div class="btn-wrap"><a href="%s" class="button">Kích hoạt tài khoản</a></div>
            <p style="font-size:13px;color:#8a6d3b;">Link kích hoạt có hiệu lực trong 72 giờ. Sau khi kích hoạt, khuyến khích đổi mật khẩu ngay.</p>
            </div>
            <div class="footer"><p>&copy; %s %s. This is an automated message.</p></div>
            </div>
            </body>
            </html>
            """
            .formatted(appName, fullName, email, plainPassword, activationLink, currentYear, appName);
    }

    @Override
    @Async
    public void sendEmail(String toEmail, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email sent to: {} with subject: {}", toEmail, subject);

        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send email", e);
        }
    }

    private String buildPasswordResetEmailHtml(String fullName, String resetLink) {
        String currentYear = Year.now().toString();
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <style>
                        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 0; background-color: #f8f7f5; color: #1c160d; }
                        .container { max-width: 560px; margin: 30px auto; background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 2px 12px rgba(0,0,0,0.06); border: 1px solid #e8ddce; }
                        .header { background-color: #f49d25; padding: 28px 30px; text-align: center; }
                        .header h1 { margin: 0; font-size: 22px; color: #ffffff; font-weight: 700; letter-spacing: -0.3px; }
                        .content { padding: 32px 30px; }
                        .content h2 { color: #1c160d; font-size: 20px; margin: 0 0 16px 0; font-weight: 600; }
                        .content p { color: #5c4d3c; font-size: 15px; line-height: 1.7; margin: 0 0 14px 0; }
                        .btn-wrap { text-align: center; margin: 28px 0; }
                        .button { display: inline-block; padding: 14px 36px; background-color: #f49d25; color: #ffffff !important; text-decoration: none; border-radius: 8px; font-weight: 700; font-size: 15px; letter-spacing: 0.02em; }
                        .link-fallback { margin-top: 20px; padding: 16px; background-color: #f8f7f5; border-radius: 8px; border: 1px solid #e8ddce; }
                        .link-fallback p { color: #9c7a49; font-size: 13px; margin: 0 0 6px 0; }
                        .link-fallback a { color: #f49d25; font-size: 13px; word-break: break-all; }
                        .warning { margin-top: 20px; padding: 14px 16px; background-color: #fff8ee; border-left: 4px solid #f49d25; border-radius: 0 8px 8px 0; }
                        .warning p { color: #8a6d3b; font-size: 14px; margin: 0; }
                        .footer { background-color: #f8f7f5; padding: 20px 30px; text-align: center; border-top: 1px solid #e8ddce; }
                        .footer p { color: #9c7a49; font-size: 12px; margin: 0 0 4px 0; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>%s</h1>
                        </div>
                        <div class="content">
                            <h2>Password Reset Request</h2>
                            <p>Hi <strong>%s</strong>,</p>
                            <p>We received a request to reset your password. Click the button below to set a new password:</p>
                            <div class="btn-wrap">
                                <a href="%s" class="button">Reset Password</a>
                            </div>
                            <div class="link-fallback">
                                <p>If the button doesn't work, copy and paste this link into your browser:</p>
                                <a href="%s">%s</a>
                            </div>
                            <div class="warning">
                                <p><strong>This link will expire in 15 minutes.</strong> If you did not request a password reset, you can safely ignore this email.</p>
                            </div>
                        </div>
                        <div class="footer">
                            <p>&copy; %s %s. All rights reserved.</p>
                            <p>This is an automated message — please do not reply.</p>
                        </div>
                    </div>
                </body>
                </html>
                """
                .formatted(appName, fullName, resetLink, resetLink, resetLink, currentYear, appName);
    }
}
