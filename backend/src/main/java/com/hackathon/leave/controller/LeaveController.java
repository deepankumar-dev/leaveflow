package com.hackathon.leave.controller;

import com.hackathon.leave.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.hackathon.leave.security.AuthUser;
import com.hackathon.leave.service.LeaveService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Leaves")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/leaves")
public class LeaveController {

    private final LeaveService leaves;

    public LeaveController(LeaveService leaves) {
        this.leaves = leaves;
    }

    @Operation(summary = "Dry-run an application: working days, balance, conflict flag; never persists")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/preview")
    public ResponseEntity<LeavePreviewResponse> preview(@AuthenticationPrincipal AuthUser me,
                                                         @Valid @RequestBody LeavePreviewRequest request) {
        return ResponseEntity.ok(leaves.preview(me.id(), request));
    }

    @Operation(summary = "Apply for leave; reserves balance as pending and starts the approval chain")
    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public ResponseEntity<LeaveRequestDto> apply(@AuthenticationPrincipal AuthUser me,
                                               @Valid @RequestBody LeaveApplyRequest request) {
        return ResponseEntity.ok(leaves.apply(me.id(), request));
    }

    @Operation(summary = "The caller's own leave requests, newest first")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/mine")
    public ResponseEntity<List<LeaveRequestDto>> mine(@AuthenticationPrincipal AuthUser me) {
        return ResponseEntity.ok(leaves.mine(me.id()));
    }

    @Operation(summary = "One request (owner, their manager, or HR)")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public ResponseEntity<LeaveRequestDto> get(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return ResponseEntity.ok(leaves.get(me.id(), id));
    }

    @Operation(summary = "Audit timeline of a request, oldest first")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}/timeline")
    public ResponseEntity<List<TimelineEventDto>> timeline(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return ResponseEntity.ok(leaves.timeline(me.id(), id));
    }

    @Operation(summary = "Cancel own request (pending or approved); releases the reserved/used balance")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{id}/cancel")
    public ResponseEntity<LeaveRequestDto> cancel(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                                                  @RequestBody(required = false) DecisionRequest body) {
        return ResponseEntity.ok(leaves.cancel(me.id(), id, comment(body)));
    }

    @Operation(summary = "Approve the caller's current step (manager or HR); nobody decides their own request")
    @PreAuthorize("hasAnyRole('MANAGER','HR')")
    @PostMapping("/{id}/approve")
    public ResponseEntity<LeaveRequestDto> approve(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                                                   @RequestBody(required = false) DecisionRequest body) {
        return ResponseEntity.ok(leaves.approve(me.id(), id, comment(body)));
    }

    @Operation(summary = "Reject the caller's current step; a comment is required")
    @PreAuthorize("hasAnyRole('MANAGER','HR')")
    @PostMapping("/{id}/reject")
    public ResponseEntity<LeaveRequestDto> reject(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                                                  @Valid @RequestBody DecisionRequest body) {
        return ResponseEntity.ok(leaves.reject(me.id(), id, comment(body)));
    }

    private static String comment(DecisionRequest body) {
        return body == null ? null : body.comment();
    }
}
