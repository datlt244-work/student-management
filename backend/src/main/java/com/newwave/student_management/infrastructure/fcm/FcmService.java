package com.newwave.student_management.infrastructure.fcm;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class FcmService {

    public void sendNotification(String token, String title, String body, String actionUrl) {
        try {
            com.google.firebase.messaging.Message.Builder builder = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build());

            if (actionUrl != null && !actionUrl.isBlank()) {
                builder.putData("url", actionUrl);
                builder.setWebpushConfig(com.google.firebase.messaging.WebpushConfig.builder()
                        .setFcmOptions(com.google.firebase.messaging.WebpushFcmOptions.withLink(actionUrl))
                        .build());
            }

            Message message = builder.build();
            String response = FirebaseMessaging.getInstance().send(message);
            log.info("Successfully sent message: " + response);
        } catch (Exception e) {
            log.error("Error sending FCM message", e);
        }
    }
}
