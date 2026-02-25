package com.newwave.student_management.domains.notification.controller;

import com.newwave.student_management.domains.notification.dto.FcmTokenRequest;
import com.newwave.student_management.domains.notification.dto.NotificationRequest;
import com.newwave.student_management.domains.notification.service.NotificationInternalService;
import com.newwave.student_management.infrastructure.security.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationInternalService notificationService;
    private final JwtService jwtService;

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<java.util.Map<String, Object>> getStats() {
        return ResponseEntity.ok(notificationService.getStats());
    }

    @PostMapping("/tokens")
    public ResponseEntity<Void> registerToken(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid FcmTokenRequest request) {

        String userIdStr = jwt.getClaim("userId").toString();
        notificationService.registerToken(UUID.fromString(userIdStr), request.getToken(),
                request.getDeviceType());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/broadcast")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> broadcast(@RequestBody @Valid NotificationRequest request) {
        notificationService.broadcast(request.getTitle(), request.getBody(), request.getActionUrl());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/send-targeted")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sendTargeted(
            @RequestBody @Valid com.newwave.student_management.domains.notification.dto.TargetedNotificationRequest request) {
        notificationService.sendTargeted(
                request.getTitle(),
                request.getBody(),
                request.getActionUrl(),
                request.getRole(),
                request.getDepartmentId(),
                request.getClassCode());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/send-personal")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sendPersonal(
            @RequestBody @Valid com.newwave.student_management.domains.notification.dto.TargetedNotificationRequest request) {
        notificationService.sendPersonal(
                request.getTitle(),
                request.getBody(),
                request.getActionUrl(),
                request.getRecipientId()); // Reusing DTO, using recipientId for identifier
        return ResponseEntity.ok().build();
    }

    @GetMapping("/search-recipients")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<java.util.List<com.newwave.student_management.domains.notification.dto.RecipientSearchResponse>> searchRecipients(
            @RequestParam String query) {
        return ResponseEntity.ok(notificationService.searchRecipients(query));
    }

    @GetMapping("/history")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<org.springframework.data.domain.Page<com.newwave.student_management.domains.notification.entity.SentNotification>> getHistory(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime endDate,
            @org.springframework.data.web.PageableDefault(size = 10, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) org.springframework.data.domain.Pageable pageable) {
        return ResponseEntity.ok(notificationService.getSentHistory(search, type, startDate, endDate, pageable));
    }

    @DeleteMapping("/history/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteHistory(@PathVariable("id") UUID id) {
        notificationService.deleteSentNotification(id);
        return ResponseEntity.noContent().build();
    }
}
