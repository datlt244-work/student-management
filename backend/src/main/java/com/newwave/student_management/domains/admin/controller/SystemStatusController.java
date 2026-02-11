package com.newwave.student_management.domains.admin.controller;

import com.newwave.student_management.common.dto.ApiResponse;
import com.newwave.student_management.domains.admin.dto.ComponentStatusDto;
import com.newwave.student_management.domains.admin.dto.SystemHealthResponse;
import com.newwave.student_management.domains.admin.service.SystemStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "System Health", description = "Các API kiểm tra trạng thái hệ thống")
@RestController
@RequestMapping("/admin/system/health")
@RequiredArgsConstructor
@Slf4j
public class SystemStatusController {

    private final SystemStatusService systemStatusService;

    @Operation(
            summary = "Tổng quan hệ thống",
            description = "Trả về trạng thái API, Database, Redis, Mail, MinIO, Nginx và Frontend."
    )
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/overall")
    public ResponseEntity<ApiResponse<SystemHealthResponse>> overall() {
        return ResponseEntity.ok(ApiResponse.success(systemStatusService.overall()));
    }

    @Operation(summary = "Kiểm tra API", description = "Kiểm tra tình trạng phản hồi của API server.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api")
    public ResponseEntity<ApiResponse<ComponentStatusDto>> api() {
        return ResponseEntity.ok(ApiResponse.success(systemStatusService.checkApi()));
    }

    @Operation(summary = "Kiểm tra Database", description = "Kiểm tra kết nối cơ sở dữ liệu (SELECT 1).")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/db")
    public ResponseEntity<ApiResponse<ComponentStatusDto>> db() {
        return ResponseEntity.ok(ApiResponse.success(systemStatusService.checkDatabase()));
    }

    @Operation(summary = "Kiểm tra Redis", description = "Kiểm tra kết nối Redis.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/redis")
    public ResponseEntity<ApiResponse<ComponentStatusDto>> redis() {
        return ResponseEntity.ok(ApiResponse.success(systemStatusService.checkRedis()));
    }

    @Operation(summary = "Kiểm tra Mail", description = "Kiểm tra cấu hình SMTP (nếu khả dụng).")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/mail")
    public ResponseEntity<ApiResponse<ComponentStatusDto>> mail() {
        return ResponseEntity.ok(ApiResponse.success(systemStatusService.checkMail()));
    }

    @Operation(summary = "Kiểm tra MinIO", description = "Kiểm tra trạng thái MinIO (S3 storage).")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/minio")
    public ResponseEntity<ApiResponse<ComponentStatusDto>> minio() {
        return ResponseEntity.ok(ApiResponse.success(systemStatusService.checkMinIO()));
    }

    @Operation(summary = "Kiểm tra Nginx", description = "Kiểm tra trạng thái Nginx (reverse proxy).")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/nginx")
    public ResponseEntity<ApiResponse<ComponentStatusDto>> nginx() {
        return ResponseEntity.ok(ApiResponse.success(systemStatusService.checkNginx()));
    }

    @Operation(summary = "Kiểm tra Frontend", description = "Kiểm tra trạng thái Frontend (SPA admin).")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/frontend")
    public ResponseEntity<ApiResponse<ComponentStatusDto>> frontend() {
        return ResponseEntity.ok(ApiResponse.success(systemStatusService.checkFrontend()));
    }
}


