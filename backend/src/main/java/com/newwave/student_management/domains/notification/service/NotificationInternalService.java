package com.newwave.student_management.domains.notification.service;

import com.newwave.student_management.domains.auth.entity.User;
import com.newwave.student_management.domains.notification.entity.FcmToken;
import com.newwave.student_management.domains.notification.entity.Notification;
import com.newwave.student_management.domains.notification.entity.SentNotification;
import com.newwave.student_management.domains.notification.repository.FcmTokenRepository;
import com.newwave.student_management.domains.notification.repository.NotificationRepository;
import com.newwave.student_management.domains.notification.repository.SentNotificationRepository;
import com.newwave.student_management.domains.auth.repository.UserRepository;
import com.newwave.student_management.domains.profile.repository.StudentRepository;
import com.newwave.student_management.domains.profile.repository.TeacherRepository;
import com.newwave.student_management.infrastructure.fcm.FcmService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.newwave.student_management.domains.notification.dto.RecipientSearchResponse;
import org.springframework.data.domain.PageRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationInternalService {

    private final FcmService fcmService;
    private final FcmTokenRepository fcmTokenRepository;
    private final NotificationRepository notificationRepository;
    private final SentNotificationRepository sentNotificationRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;

    @Transactional(readOnly = true)
    public Page<SentNotification> getSentHistory(
            String search,
            String type,
            java.time.LocalDateTime startDate,
            java.time.LocalDateTime endDate,
            Pageable pageable) {
        String searchParam = (search == null || search.isBlank()) ? null : search;
        String typeParam = (type == null || type.isBlank()) ? null : type.toUpperCase();

        return sentNotificationRepository.findWithFilters(searchParam, typeParam, startDate, endDate, pageable);
    }

    @Transactional
    public void registerToken(java.util.UUID userId, String token, String deviceType) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        fcmTokenRepository.findByToken(token).ifPresentOrElse(
                t -> {
                    t.setUser(user);
                    t.setDeviceType(deviceType);
                    fcmTokenRepository.save(t);
                },
                () -> {
                    FcmToken fcmToken = FcmToken.builder()
                            .user(user)
                            .token(token)
                            .deviceType(deviceType)
                            .build();
                    fcmTokenRepository.save(fcmToken);
                });
    }

    @Transactional
    public void sendToUser(User user, String title, String body, String actionUrl) {
        // 1. Lưu vào DB cho người dùng xem
        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .body(body)
                .actionUrl(actionUrl)
                .isRead(false)
                .notificationType("SYSTEM")
                .build();
        notificationRepository.save(notification);

        // 2. Gửi qua FCM
        List<FcmToken> tokens = fcmTokenRepository.findByUser_UserId(user.getUserId());
        for (FcmToken fcmToken : tokens) {
            fcmService.sendNotification(fcmToken.getToken(), title, body, actionUrl);
        }
    }

    @Transactional
    public void broadcast(String title, String body, String actionUrl, java.time.LocalDateTime scheduledAt) {
        if (scheduledAt != null && scheduledAt.isAfter(java.time.LocalDateTime.now())) {
            SentNotification history = SentNotification.builder()
                    .title(title)
                    .body(body)
                    .actionUrl(actionUrl)
                    .notificationType("BROADCAST")
                    .status("PENDING")
                    .scheduledAt(scheduledAt)
                    .targetGroup("All Users (Scheduled)")
                    .build();
            sentNotificationRepository.save(history);
            return;
        }

        SentNotification history = SentNotification.builder()
                .title(title)
                .body(body)
                .actionUrl(actionUrl)
                .notificationType("BROADCAST")
                .targetGroup("All Users")
                .status("SENT")
                .sentAt(java.time.LocalDateTime.now())
                .build();

        sendBroadcastImmediate(history);
        sentNotificationRepository.save(history);
    }

    private void sendBroadcastImmediate(SentNotification notif) {
        List<FcmToken> allTokens = fcmTokenRepository.findAllByRoleNot("ADMIN");
        notif.setRecipientCount(allTokens.size());
        for (FcmToken fcmToken : allTokens) {
            try {
                fcmService.sendNotification(fcmToken.getToken(), notif.getTitle(), notif.getBody(),
                        notif.getActionUrl());
            } catch (Exception e) {
                System.err.println("Failed to send FCM: " + e.getMessage());
            }
        }
    }

    @Transactional
    public void sendTargeted(String title, String body, String actionUrl, String role, Long departmentId,
            String classCode, java.time.LocalDateTime scheduledAt) {

        StringBuilder targetGroup = new StringBuilder();
        if (role != null)
            targetGroup.append("Role: ").append(role).append("; ");
        if (departmentId != null)
            targetGroup.append("Dept ID: ").append(departmentId).append("; ");
        if (classCode != null)
            targetGroup.append("Class: ").append(classCode);
        if (targetGroup.length() == 0)
            targetGroup.append("Specific Groups");

        if (scheduledAt != null && scheduledAt.isAfter(java.time.LocalDateTime.now())) {
            SentNotification history = SentNotification.builder()
                    .title(title)
                    .body(body)
                    .actionUrl(actionUrl)
                    .notificationType("TARGETED")
                    .status("PENDING")
                    .scheduledAt(scheduledAt)
                    .targetGroup(targetGroup.toString().trim() + " (Scheduled)")
                    .targetRole(role)
                    .targetDepartmentId(departmentId)
                    .targetClassCode(classCode)
                    .build();
            sentNotificationRepository.save(history);
            return;
        }

        SentNotification history = SentNotification.builder()
                .title(title)
                .body(body)
                .actionUrl(actionUrl)
                .notificationType("TARGETED")
                .targetGroup(targetGroup.toString().trim())
                .targetRole(role)
                .targetDepartmentId(departmentId)
                .targetClassCode(classCode)
                .status("SENT")
                .sentAt(java.time.LocalDateTime.now())
                .build();

        sendTargetedImmediate(history);
        sentNotificationRepository.save(history);
    }

    private void sendTargetedImmediate(SentNotification notif) {
        String roleParam = (notif.getTargetRole() == null || notif.getTargetRole().equalsIgnoreCase("All Roles")) ? null
                : notif.getTargetRole().toUpperCase();
        List<FcmToken> tokens = fcmTokenRepository.findTokensByCriteria(roleParam, notif.getTargetDepartmentId(),
                notif.getTargetClassCode());
        notif.setRecipientCount(tokens.size());
        for (FcmToken fcmToken : tokens) {
            try {
                fcmService.sendNotification(fcmToken.getToken(), notif.getTitle(), notif.getBody(),
                        notif.getActionUrl());
            } catch (Exception e) {
                System.err.println("Failed to send FCM: " + e.getMessage());
            }
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getStats() {
        long activeTokens = fcmTokenRepository.count();
        long sentLast30Days = sentNotificationRepository
                .countByCreatedAtAfter(java.time.LocalDateTime.now().minusDays(30));

        Map<String, Object> stats = new HashMap<>();
        stats.put("activeTokens", activeTokens);
        stats.put("sentLast30Days", sentLast30Days);
        return stats;
    }

    @Transactional(readOnly = true)
    public List<RecipientSearchResponse> searchRecipients(String query) {
        if (query == null || query.isBlank())
            return new ArrayList<>();

        org.springframework.data.domain.Pageable limit = PageRequest.of(0, 10);

        List<RecipientSearchResponse> results = new ArrayList<>();

        // 1. Search Students
        results.addAll(studentRepository.searchRecipients(query, limit).stream()
                .map(s -> RecipientSearchResponse.builder()
                        .name(s.getFirstName() + " " + s.getLastName())
                        .identifier(s.getStudentCode() != null ? s.getStudentCode() : s.getEmail())
                        .role("STUDENT")
                        .build())
                .collect(Collectors.toList()));

        // 2. Search Teachers
        results.addAll(teacherRepository.searchRecipients(query, limit).stream()
                .map(t -> RecipientSearchResponse.builder()
                        .name(t.getFirstName() + " " + t.getLastName())
                        .identifier(t.getTeacherCode() != null ? t.getTeacherCode() : t.getEmail())
                        .role("TEACHER")
                        .build())
                .collect(Collectors.toList()));

        return results;
    }

    @Transactional
    public void sendPersonal(String title, String body, String actionUrl, String identifier,
            java.time.LocalDateTime scheduledAt) {
        if (scheduledAt != null && scheduledAt.isAfter(java.time.LocalDateTime.now())) {
            SentNotification history = SentNotification.builder()
                    .title(title)
                    .body(body)
                    .actionUrl(actionUrl)
                    .notificationType("PERSONAL")
                    .status("PENDING")
                    .scheduledAt(scheduledAt)
                    .targetGroup("User: " + identifier + " (Scheduled)")
                    .targetRecipientId(identifier)
                    .build();
            sentNotificationRepository.save(history);
            return;
        }

        SentNotification history = SentNotification.builder()
                .title(title)
                .body(body)
                .actionUrl(actionUrl)
                .notificationType("PERSONAL")
                .targetGroup("User: " + identifier)
                .targetRecipientId(identifier)
                .recipientCount(1)
                .status("SENT")
                .sentAt(java.time.LocalDateTime.now())
                .build();

        sendPersonalImmediate(history);
        sentNotificationRepository.save(history);
    }

    private void sendPersonalImmediate(SentNotification notif) {
        String identifier = notif.getTargetRecipientId();
        java.util.Optional<com.newwave.student_management.domains.auth.entity.User> userOpt = userRepository
                .findByEmail(identifier);
        if (userOpt.isEmpty()) {
            userOpt = studentRepository.findByStudentCodeAndDeletedAtIsNull(identifier)
                    .map(com.newwave.student_management.domains.profile.entity.Student::getUser);
        }
        if (userOpt.isEmpty()) {
            userOpt = teacherRepository.findByTeacherCodeAndDeletedAtIsNull(identifier)
                    .map(com.newwave.student_management.domains.profile.entity.Teacher::getUser);
        }

        if (userOpt.isPresent()) {
            sendToUser(userOpt.get(), notif.getTitle(), notif.getBody(), notif.getActionUrl());
        } else {
            throw new RuntimeException("Recipient not found: " + identifier);
        }
    }

    @Transactional
    public void deleteSentNotification(java.util.UUID sentId) {
        sentNotificationRepository.findById(sentId).ifPresent(notif -> {
            if ("PENDING".equals(notif.getStatus())) {
                notif.setStatus("CANCELLED");
            }
            notif.setDeletedAt(java.time.LocalDateTime.now());
            sentNotificationRepository.save(notif);
        });
    }

    @Transactional
    public void processScheduledNotification(SentNotification notif) {
        try {
            if ("BROADCAST".equals(notif.getNotificationType())) {
                sendBroadcastImmediate(notif);
            } else if ("TARGETED".equals(notif.getNotificationType())) {
                sendTargetedImmediate(notif);
            } else if ("PERSONAL".equals(notif.getNotificationType())) {
                sendPersonalImmediate(notif);
            }

            notif.setStatus("SENT");
            notif.setSentAt(java.time.LocalDateTime.now());
            sentNotificationRepository.save(notif);
        } catch (Exception e) {
            notif.setStatus("FAILED");
            sentNotificationRepository.save(notif);
        }
    }
}
