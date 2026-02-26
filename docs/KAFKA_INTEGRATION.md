# Tài liệu Hướng dẫn Tích hợp Kafka cho Hệ thống Quản lý Sinh viên

Tài liệu này hướng dẫn cách cấu hình và sử dụng Apache Kafka để xử lý bất đồng bộ (Asynchronous processing) trong hệ thống, giúp cải thiện hiệu năng và khả năng mở rộng.

---

## 1. Tổng quan các Use Case ứng dụng Kafka
- **Gửi thông báo (Notifications)**: Đẩy thông báo FCM hoặc Email vào queue để không làm chậm luồng xử lý chính.
- **Xử lý điểm số/Kết quả học tập**: Tính toán điểm trung bình hoặc xếp loại khi giảng viên nhập điểm cho số lượng lớn sinh viên.
- **Ghi log hệ thống (Audit Logs)**: Ghi lại các thao tác quan trọng vào Kafka để xử lý phân tích dữ liệu sau này.

---

## 2. Giao diện quản lý (Kafka UI)
Hệ thống tích hợp **Kafka UI** để bạn có thể theo dõi Topic, tin nhắn và Consumer thông qua trình duyệt.

- **URL**: `http://localhost:8080`
- **Tính năng**: 
  - Xem danh sách và nội dung các tin nhắn trong Topic.
  - Quản lý Consumer Group và Offset.
  - Tạo/Xóa Topic trực tiếp từ giao diện.

---

## 3. Cấu hình cơ sở hạ tầng (Infrastructure)

Để chạy Kafka cục bộ, cách đơn giản nhất là sử dụng **Docker Compose**.

### Tạo file `docker-compose-kafka.yml` tại thư mục gốc:
```yaml
version: '3.8'
services:
  zookeeper:
    image: confluentinc/cp-zookeeper:latest
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000

  kafka:
    image: confluentinc/cp-kafka:latest
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092,PLAINTEXT_INTERNAL://kafka:29092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_INTERNAL:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT_INTERNAL
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
```

---

## 3. Cấu hình Backend (Spring Boot)

### 3.1. Thêm Dependency vào `pom.xml`:
```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

### 3.2. Cấu hình đa môi trường (Profiles)

#### application.yaml (Cấu hình chung)
```yaml
spring:
  kafka:
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    consumer:
      group-id: ${spring.application.name}-group
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "*"
```

#### application-dev.yaml (Môi trường Dev - Local)
```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
```

#### application-prod.yaml (Môi trường Prod - Docker/Cloud)
```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS}
```

---

## 4. Triển khai CI/CD và Docker Compose

Hệ thống sử dụng **Docker Compose** để quản lý các service. Kafka đã được tích hợp vào `backend/docker-compose.yml`.

### Cấu hình Docker Compose:
```yaml
  kafka:
    image: confluentinc/cp-kafka:7.5.0
    depends_on:
      - zookeeper
    environment:
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092,PLAINTEXT_INTERNAL://kafka:29092
      # ...
```

### GitHub Actions (CI/CD):
Khi deploy thông qua `deploy.yml`, biến môi trường `KAFKA_BOOTSTRAP_SERVERS` sẽ được truyền tự động thông qua Docker Compose (mặc định là `kafka:29092`). Nếu bạn chạy test trong CI, hãy cân nhắc sử dụng **Testcontainers** để khởi tạo Kafka giả lập.

---

## 5. Triển khai Code mẫu (Tiếp theo)

### 4.1. Tạo Topic Configuration
```java
@Configuration
public class KafkaTopicConfig {
    @Bean
    public NewTopic notificationTopic() {
        return TopicBuilder.name("notification-topic")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
```

### 4.2. Viết Producer (Người gửi tin)
```java
@Service
@RequiredArgsConstructor
public class NotificationProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendNotificationEvent(NotificationRequest request) {
        kafkaTemplate.send("notification-topic", request);
    }
}
```

### 4.3. Viết Consumer (Người nhận và xử lý)
```java
@Service
@Slf4j
public class NotificationConsumer {

    @KafkaListener(topics = "notification-topic", groupId = "student-mgmt-group")
    public void listenNotification(NotificationRequest request) {
        log.info("Received notification request from Kafka: {}", request.getTitle());
        // Gọi NotificationInternalService để gửi FCM hoặc lưu DB
    }
}
```

---

## 5. Các lưu ý quan trọng (Best Practices)

1. **Acknowledge (ACKs)**: Trong sản xuất, nên cấu hình `acks: all` để đảm bảo dữ liệu không bị mất.
2. **Error Handling**: Sử dụng `Dead Letter Topic (DLT)` để hứng những tin nhắn bị lỗi xử lý, tránh làm treo Consumer.
3. **Serialization**: Sử dụng JSON là cách phổ biến nhất để truyền tải Object giữa các service.
4. **Idempotency**: Đảm bảo Consumer xử lý tin nhắn là Idempotent (xử lý nhiều lần cùng 1 tin nhắn không gây sai lệch dữ liệu).

---

## 6. Lệnh kiểm tra Kafka (CLI)
- **Liệt kê topic**: `kafka-topics --list --bootstrap-server localhost:9092`
- **Xem tin nhắn**: `kafka-console-consumer --topic notification-topic --from-beginning --bootstrap-server localhost:9092`
