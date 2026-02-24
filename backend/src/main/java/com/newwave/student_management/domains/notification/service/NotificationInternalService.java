package com.newwave.student_management.domains.notification.service;

import com.newwave.student_management.domains.auth.entity.User;
import com.newwave.student_management.domains.notification.entity.FcmToken;
import com.newwave.student_management.domains.notification.entity.Notification;
import com.newwave.student_management.domains.notification.repository.FcmTokenRepository;
import com.newwave.student_management.domains.notification.repository.NotificationRepository;
import com.newwave.student_management.infrastructure.fcm.FcmService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationInternalService {

    private final FcmService fcmService;
    private final FcmTokenRepository fcmTokenRepository;
    private final NotificationRepository notificationRepository;

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
                }
        );
    }

    @Transactional
    public void sendToUser(User user, String title, String body, String actionUrl) {
        // 1. Lưu vào DB
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
        // Thông thường broadcast sẽ gửi cho toàn bộ user đang có token hoặc theo topic
        // Để đơn giản, ta lấy toàn bộ token trong DB gửi đi
        List<FcmToken> allTokens = fcmTokenRepository.findAll();
        for (FcmToken fcmToken : allTokens) {
            fcmService.sendNotification(fcmToken.getToken(), title, body);
        }
        
        // Lưu lịch sử thông báo có thể cần logic phức tạp hơn (VD: Notification hỉển thị chung)
        // Ở đây ta chỉ gửi FCM.
    }
}
