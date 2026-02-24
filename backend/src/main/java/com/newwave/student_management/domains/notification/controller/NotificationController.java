package com.newwave.student_management.domains.notification.controller;

import com.newwave.student_management.domains.auth.entity.User;
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

    @PostMapping("/tokens")
    public ResponseEntity<Void> registerToken(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid FcmTokenRequest request) {
        
        User user = new User();
        user.setUserId(UUID.fromString(jwt.getSubject()));
        
        notificationService.registerToken(user, request.getToken(), request.getDeviceType());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/broadcast")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> broadcast(@RequestBody @Valid NotificationRequest request) {
        notificationService.broadcast(request.getTitle(), request.getBody(), request.getActionUrl());
        return ResponseEntity.ok().build();
    }
}
