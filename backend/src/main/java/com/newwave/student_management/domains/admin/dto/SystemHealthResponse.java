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
public class SystemHealthResponse {

    private String overallStatus;
    private LocalDateTime timestamp;

    private ComponentStatusDto api;
    private ComponentStatusDto database;
    private ComponentStatusDto redis;
    private ComponentStatusDto mail;
    private ComponentStatusDto minio;
    private ComponentStatusDto nginx;
    private ComponentStatusDto frontend;
}


