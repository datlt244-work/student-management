package com.newwave.student_management.domains.admin.service.impl;

import com.newwave.student_management.domains.admin.dto.ComponentStatusDto;
import com.newwave.student_management.domains.admin.dto.SystemHealthResponse;
import com.newwave.student_management.domains.admin.service.SystemStatusService;
import com.newwave.student_management.domains.auth.service.ITokenRedisService;
import com.newwave.student_management.infrastructure.mail.IMailService;
import com.newwave.student_management.infrastructure.storage.IStorageService;
import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemStatusServiceImpl implements SystemStatusService {

    private final DataSource dataSource;
    private final ITokenRedisService tokenRedisService;
    private final IStorageService storageService;
    private final IMailService mailService;
    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String minioBucket;

    @Override
    public SystemHealthResponse overall() {
        ComponentStatusDto api = checkApi();
        ComponentStatusDto db = checkDatabase();
        ComponentStatusDto redis = checkRedis();
        ComponentStatusDto mail = checkMail();
        ComponentStatusDto minio = checkMinIO();
        ComponentStatusDto nginx = checkNginx();
        ComponentStatusDto frontend = checkFrontend();

        String overallStatus = "UP";
        if (!isUp(api) || !isUp(db) || !isUp(redis) || !isUp(mail) || !isUp(minio)) {
            overallStatus = "DEGRADED";
        }

        return SystemHealthResponse.builder()
                .overallStatus(overallStatus)
                .timestamp(LocalDateTime.now())
                .api(api)
                .database(db)
                .redis(redis)
                .mail(mail)
                .minio(minio)
                .nginx(nginx)
                .frontend(frontend)
                .build();
    }

    private boolean isUp(ComponentStatusDto component) {
        return component != null && "UP".equalsIgnoreCase(component.getStatus());
    }

    @Override
    public ComponentStatusDto checkApi() {
        // Nếu controller được gọi được tới đây nghĩa là API đang chạy
        return ComponentStatusDto.builder()
                .status("UP")
                .message("Backend API is running")
                .latencyMs(0L)
                .build();
    }

    @Override
    public ComponentStatusDto checkDatabase() {
        Instant start = Instant.now();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT 1")) {
            ps.execute();
            long ms = Duration.between(start, Instant.now()).toMillis();
            return ComponentStatusDto.builder()
                    .status("UP")
                    .message("Database reachable")
                    .latencyMs(ms)
                    .build();
        } catch (Exception e) {
            log.warn("Database health check failed: {}", e.getMessage());
            return ComponentStatusDto.builder()
                    .status("DOWN")
                    .message(e.getMessage())
                    .latencyMs(null)
                    .build();
        }
    }

    @Override
    public ComponentStatusDto checkRedis() {
        Instant start = Instant.now();
        try {
            // simple read/write using tokenRedisService via rate-limit key; nếu có vấn đề Redis sẽ ném exception
            tokenRedisService.isRateLimited("__healthcheck@example.com");
            long ms = Duration.between(start, Instant.now()).toMillis();
            return ComponentStatusDto.builder()
                    .status("UP")
                    .message("Redis reachable")
                    .latencyMs(ms)
                    .build();
        } catch (Exception e) {
            log.warn("Redis health check failed: {}", e.getMessage());
            return ComponentStatusDto.builder()
                    .status("DOWN")
                    .message(e.getMessage())
                    .latencyMs(null)
                    .build();
        }
    }

    @Override
    public ComponentStatusDto checkMail() {
        Instant start = Instant.now();
        try {
            String impl = mailService.getClass().getSimpleName();
            long ms = Duration.between(start, Instant.now()).toMillis();
            return ComponentStatusDto.builder()
                    .status("UP")
                    .message("Mail service bean: " + impl)
                    .latencyMs(ms)
                    .build();
        } catch (Exception e) {
            log.warn("Mail health check failed: {}", e.getMessage());
            return ComponentStatusDto.builder()
                    .status("DOWN")
                    .message(e.getMessage())
                    .latencyMs(null)
                    .build();
        }
    }

    @Override
    public ComponentStatusDto checkMinIO() {
        Instant start = Instant.now();
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(minioBucket)
                            .build()
            );
            long ms = Duration.between(start, Instant.now()).toMillis();

            if (exists) {
                return ComponentStatusDto.builder()
                        .status("UP")
                        .message("MinIO bucket '" + minioBucket + "' is reachable")
                        .latencyMs(ms)
                        .build();
            } else {
                return ComponentStatusDto.builder()
                        .status("DOWN")
                        .message("MinIO bucket '" + minioBucket + "' does not exist or is not reachable")
                        .latencyMs(ms)
                        .build();
            }
        } catch (Exception e) {
            log.warn("MinIO health check failed: {}", e.getMessage());
            return ComponentStatusDto.builder()
                    .status("DOWN")
                    .message(e.getMessage())
                    .latencyMs(null)
                    .build();
        }
    }

    @Override
    public ComponentStatusDto checkNginx() {
        return ComponentStatusDto.builder()
                .status("UP")
                .message("Nginx is routing requests to backend successfully (this endpoint is reachable)")
                .latencyMs(null)
                .build();
    }

    @Override
    public ComponentStatusDto checkFrontend() {
        return ComponentStatusDto.builder()
                .status("UP")
                .message("Frontend SPA can reach backend health endpoints (assumed when called via browser)")
                .latencyMs(null)
                .build();
    }
}


