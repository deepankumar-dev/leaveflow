package com.hackathon.leave.controller;

import com.hackathon.leave.dto.NotificationDto;
import com.hackathon.leave.security.AuthUser;
import com.hackathon.leave.service.NotificationQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Notifications")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationQueryService notifications;

    public NotificationController(NotificationQueryService notifications) {
        this.notifications = notifications;
    }

    @Operation(summary = "The caller's in-app notifications, newest first")
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<List<NotificationDto>> list(@AuthenticationPrincipal AuthUser me) {
        return ResponseEntity.ok(notifications.list(me.id()));
    }

    @Operation(summary = "Mark one of the caller's notifications as read")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{id}/read")
    public ResponseEntity<NotificationDto> markRead(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return ResponseEntity.ok(notifications.markRead(me.id(), id));
    }
}
