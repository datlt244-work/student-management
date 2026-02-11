package com.newwave.student_management.domains.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthCheckResponse {

    private String overallStatus;
    private LocalDateTime timestamp;

    private ComponentHealth backend;
    private ComponentHealth database;
    private ComponentHealth redis;
    private ComponentHealth minio;
    private ComponentHealth nginx;
    private ComponentHealth frontend;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComponentHealth {
        private String status; // UP / DOWN / UNKNOWN
        private String details;
        private Long latencyMs;
    }
}

