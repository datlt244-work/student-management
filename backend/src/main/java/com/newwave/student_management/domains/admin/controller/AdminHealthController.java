package com.newwave.student_management.domains.admin.controller;

import com.newwave.student_management.common.dto.ApiResponse;
import com.newwave.student_management.domains.admin.dto.HealthCheckResponse;
import com.newwave.student_management.infrastructure.storage.IStorageService;
import com.newwave.student_management.domains.auth.service.ITokenRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/admin/health")
@RequiredArgsConstructor
@Slf4j
public class AdminHealthController {

    private final DataSource dataSource;
    private final ITokenRedisService tokenRedisService;
    private final IStorageService storageService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<HealthCheckResponse>> health() {
        HealthCheckResponse.HealthCheckResponseBuilder builder = HealthCheckResponse.builder()
                .timestamp(LocalDateTime.now());

        HealthCheckResponse.ComponentHealth be = HealthCheckResponse.ComponentHealth.builder()
                .status("UP")
                .details("Backend is running")
                .latencyMs(0L)
                .build();

        builder.backend(be);

        // Database health
        HealthCheckResponse.ComponentHealth dbHealth = checkDatabase();
        builder.database(dbHealth);

        // Redis health
        HealthCheckResponse.ComponentHealth redisHealth = checkRedis();
        builder.redis(redisHealth);

        // MinIO health
        HealthCheckResponse.ComponentHealth minioHealth = checkMinio();
        builder.minio(minioHealth);

        // Nginx & Frontend are inferred from this endpoint being reachable through Nginx + SPA
        builder.nginx(HealthCheckResponse.ComponentHealth.builder()
                .status("UP")
                .details("Nginx is routing /admin/health successfully")
                .latencyMs(null)
                .build());

        builder.frontend(HealthCheckResponse.ComponentHealth.builder()
                .status("UP")
                .details("Frontend can reach backend /admin/health")
                .latencyMs(null)
                .build());

        String overall = "UP";
        if (!"UP".equalsIgnoreCase(dbHealth.getStatus())
                || !"UP".equalsIgnoreCase(redisHealth.getStatus())
                || !"UP".equalsIgnoreCase(minioHealth.getStatus())) {
            overall = "DEGRADED";
        }

        builder.overallStatus(overall);

        return ResponseEntity.ok(ApiResponse.success(builder.build()));
    }

    private HealthCheckResponse.ComponentHealth checkDatabase() {
        Instant start = Instant.now();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT 1")) {
            ps.execute();
            long ms = Duration.between(start, Instant.now()).toMillis();
            return HealthCheckResponse.ComponentHealth.builder()
                    .status("UP")
                    .details("Database reachable")
                    .latencyMs(ms)
                    .build();
        } catch (Exception e) {
            log.warn("Database health check failed: {}", e.getMessage());
            return HealthCheckResponse.ComponentHealth.builder()
                    .status("DOWN")
                    .details(e.getMessage())
                    .latencyMs(null)
                    .build();
        }
    }

    private HealthCheckResponse.ComponentHealth checkRedis() {
        Instant start = Instant.now();
        try {
            // simple read/write using tokenRedisService via rate-limit key; if any Redis issue, it will throw/log
            boolean limited = tokenRedisService.isRateLimited("__healthcheck@example.com");
            long ms = Duration.between(start, Instant.now()).toMillis();
            return HealthCheckResponse.ComponentHealth.builder()
                    .status("UP")
                    .details("Redis reachable" + (limited ? "" : ""))
                    .latencyMs(ms)
                    .build();
        } catch (Exception e) {
            log.warn("Redis health check failed: {}", e.getMessage());
            return HealthCheckResponse.ComponentHealth.builder()
                    .status("DOWN")
                    .details(e.getMessage())
                    .latencyMs(null)
                    .build();
        }
    }

    private HealthCheckResponse.ComponentHealth checkMinio() {
        Instant start = Instant.now();
        try {
            // We can't do a cheap ping, but we can at least check URL generation logic is configured
            String url = storageService.getPublicUrl("healthcheck-object");
            long ms = Duration.between(start, Instant.now()).toMillis();
            return HealthCheckResponse.ComponentHealth.builder()
                    .status("UP")
                    .details(url != null ? "MinIO public URL configured" : "MinIO public URL base configured")
                    .latencyMs(ms)
                    .build();
        } catch (Exception e) {
            log.warn("MinIO health check failed: {}", e.getMessage());
            return HealthCheckResponse.ComponentHealth.builder()
                    .status("DOWN")
                    .details(e.getMessage())
                    .latencyMs(null)
                    .build();
        }
    }
}

