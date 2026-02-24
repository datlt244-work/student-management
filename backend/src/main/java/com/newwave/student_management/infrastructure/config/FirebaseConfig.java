package com.newwave.student_management.infrastructure.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @Bean
    public FirebaseApp firebaseApp() {
        try {
            InputStream serviceAccount;
            
            // 1. Thử đọc từ biến môi trường (Hữu ích cho CI/CD hoặc Docker)
            String jsonConfig = System.getenv("FIREBASE_CONFIG_JSON");
            if (jsonConfig != null && !jsonConfig.isEmpty()) {
                serviceAccount = new java.io.ByteArrayInputStream(jsonConfig.getBytes());
            } else {
                // 2. Nếu không có biến môi trường, thử đọc từ file trong resources
                serviceAccount = getClass().getClassLoader().getResourceAsStream("firebase-service-account.json");
            }
            
            if (serviceAccount == null) {
                // 3. Fallback cuối cùng: dùng Google Application Default Credentials
                // Nếu chạy trên GCP (Google Cloud) thì nó sẽ tự động nhận diện
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.getApplicationDefault())
                        .build();
                
                if (FirebaseApp.getApps().isEmpty()) {
                    return FirebaseApp.initializeApp(options);
                }
                return FirebaseApp.getInstance();
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                return FirebaseApp.initializeApp(options);
            }
            return FirebaseApp.getInstance();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khởi tạo Firebase: " + e.getMessage());
        }
    }
}
