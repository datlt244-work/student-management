package com.newwave.student_management.domains.notification.service;

import com.newwave.student_management.domains.auth.entity.User;
import com.newwave.student_management.domains.notification.entity.FcmToken;
import com.newwave.student_management.domains.notification.entity.Notification;
import com.newwave.student_management.domains.notification.entity.SentNotification;
import com.newwave.student_management.domains.notification.repository.FcmTokenRepository;
import com.newwave.student_management.domains.notification.repository.NotificationRepository;
import com.newwave.student_management.domains.notification.repository.SentNotificationRepository;
import com.newwave.student_management.infrastructure.fcm.FcmService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationInternalService {

    private final FcmService fcmService;
    private final FcmTokenRepository fcmTokenRepository;
    private final NotificationRepository notificationRepository;
    private final SentNotificationRepository sentNotificationRepository;

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
    public void registerToken(User user, String token, String deviceType) {
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
            fcmService.sendNotification(fcmToken.getToken(), title, body);
        }
    }

    @Transactional
    public void broadcast(String title, String body, String actionUrl) {
        // 1. Lấy toàn bộ token
        List<FcmToken> allTokens = fcmTokenRepository.findAll();

        // 2. Lưu vào lịch sử Admin
        SentNotification history = SentNotification.builder()
                .title(title)
                .body(body)
                .actionUrl(actionUrl)
                .notificationType("BROADCAST")
                .recipientCount(allTokens.size())
                .targetGroup("All Users")
                .build();
        sentNotificationRepository.save(history);

        // 3. Gửi FCM cho từng thiết bị
        // Trong thực tế nên dùng multicast hoặc batching nếu số lượng lớn
        for (FcmToken fcmToken : allTokens) {
            try {
                fcmService.sendNotification(fcmToken.getToken(), title, body);
            } catch (Exception e) {
                // Log lỗi cho từng token nhưng không chặn quá trình gửi tiếp
                System.err.println("Failed to send FCM to token: " + fcmToken.getToken() + " - " + e.getMessage());
            }
        }

        // 4. (Tùy chọn) Lưu vao bảng Notification cho từng User để xem lại trong app
        // Lưu ý: Nếu số lượng User cực lớn thì bước này nên xử lý bất đồng bộ
    }
}
