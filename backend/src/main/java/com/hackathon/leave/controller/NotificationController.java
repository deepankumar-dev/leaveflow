package com.hackathon.leave.controller;

import com.hackathon.leave.dto.NotificationDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Notifications")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Operation(summary = "The caller's in-app notifications, newest first")
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<List<NotificationDto>> list() {
        return Stubs.notImplemented();
    }

    @Operation(summary = "Mark one of the caller's notifications as read")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{id}/read")
    public ResponseEntity<NotificationDto> markRead(@PathVariable Long id) {
        return Stubs.notImplemented();
    }
}
