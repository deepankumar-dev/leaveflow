package com.hackathon.leave.controller;

import com.hackathon.leave.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Leaves")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/leaves")
public class LeaveController {

    @Operation(summary = "Dry-run an application: working days, balance, conflict flag; never persists")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/preview")
    public ResponseEntity<LeavePreviewResponse> preview(@Valid @RequestBody LeavePreviewRequest request) {
        return Stubs.notImplemented();
    }

    @Operation(summary = "Apply for leave; reserves balance as pending and starts the approval chain")
    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public ResponseEntity<LeaveRequestDto> apply(@Valid @RequestBody LeaveApplyRequest request) {
        return Stubs.notImplemented();
    }

    @Operation(summary = "The caller's own leave requests, newest first")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/mine")
    public ResponseEntity<List<LeaveRequestDto>> mine() {
        return Stubs.notImplemented();
    }

    @Operation(summary = "One request (owner, their manager, or HR)")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public ResponseEntity<LeaveRequestDto> get(@PathVariable Long id) {
        return Stubs.notImplemented();
    }

    @Operation(summary = "Audit timeline of a request, oldest first")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}/timeline")
    public ResponseEntity<List<TimelineEventDto>> timeline(@PathVariable Long id) {
        return Stubs.notImplemented();
    }

    @Operation(summary = "Cancel own request (pending or approved); releases the reserved/used balance")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{id}/cancel")
    public ResponseEntity<LeaveRequestDto> cancel(@PathVariable Long id,
                                                  @RequestBody(required = false) DecisionRequest body) {
        return Stubs.notImplemented();
    }

    @Operation(summary = "Approve the caller's current step (manager or HR); nobody decides their own request")
    @PreAuthorize("hasAnyRole('MANAGER','HR')")
    @PostMapping("/{id}/approve")
    public ResponseEntity<LeaveRequestDto> approve(@PathVariable Long id,
                                                   @RequestBody(required = false) DecisionRequest body) {
        return Stubs.notImplemented();
    }

    @Operation(summary = "Reject the caller's current step; a comment is required")
    @PreAuthorize("hasAnyRole('MANAGER','HR')")
    @PostMapping("/{id}/reject")
    public ResponseEntity<LeaveRequestDto> reject(@PathVariable Long id, @Valid @RequestBody DecisionRequest body) {
        return Stubs.notImplemented();
    }
}
