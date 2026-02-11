package com.newwave.student_management.domains.admin.controller;

import com.newwave.student_management.common.dto.ApiResponse;
import com.newwave.student_management.domains.admin.dto.ComponentStatusDto;
import com.newwave.student_management.domains.admin.dto.SystemHealthResponse;
import com.newwave.student_management.domains.admin.service.SystemStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "Admin - Legacy Health", description = "Adapter để giữ compatible với FE /admin/health")
@RestController
@RequestMapping("/admin/health")
@RequiredArgsConstructor
@Slf4j
public class AdminHealthController {

    private final SystemStatusService systemStatusService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Legacy admin health endpoint",
            description = "Trả về health cho FE hiện tại với các field: backend, database, redis, minio, nginx, frontend."
    )
    public ApiResponse<Map<String, Object>> health() {
        SystemHealthResponse overall = systemStatusService.overall();

        Map<String, Object> result = new HashMap<>();
        result.put("overallStatus", overall.getOverallStatus());
        result.put("timestamp", overall.getTimestamp());

        result.put("backend", toHealthComponent(overall.getApi()));
        result.put("database", toHealthComponent(overall.getDatabase()));
        result.put("redis", toHealthComponent(overall.getRedis()));
        result.put("minio", toHealthComponent(overall.getMinio()));
        result.put("nginx", toHealthComponent(overall.getNginx()));
        result.put("frontend", toHealthComponent(overall.getFrontend()));

        return ApiResponse.success(result);
    }

    private Map<String, Object> toHealthComponent(ComponentStatusDto component) {
        if (component == null) {
            return Map.of(
                    "status", "UNKNOWN",
                    "details", "No data",
                    "latencyMs", null
            );
        }
        Map<String, Object> map = new HashMap<>();
        map.put("status", component.getStatus());
        // FE dùng field 'details' nên map message -> details
        map.put("details", component.getMessage());
        map.put("latencyMs", component.getLatencyMs());
        return map;
    }
}


