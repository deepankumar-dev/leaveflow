package com.hackathon.leave.controller;

import com.hackathon.leave.dto.DelegationDto;
import com.hackathon.leave.dto.DelegationRequest;
import com.hackathon.leave.security.AuthUser;
import com.hackathon.leave.service.DelegationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Delegations")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/delegations")
public class DelegationController {

    private final DelegationService delegations;

    public DelegationController(DelegationService delegations) {
        this.delegations = delegations;
    }

    @Operation(summary = "Delegate the caller's manager approvals to another manager for a date range")
    @PreAuthorize("hasRole('MANAGER')")
    @PostMapping
    public ResponseEntity<DelegationDto> create(@AuthenticationPrincipal AuthUser me,
                                                @Valid @RequestBody DelegationRequest request) {
        return ResponseEntity.ok(delegations.create(me.id(), request));
    }

    @Operation(summary = "The caller's active delegations (given by them)")
    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping
    public ResponseEntity<List<DelegationDto>> list(@AuthenticationPrincipal AuthUser me) {
        return ResponseEntity.ok(delegations.list(me.id()));
    }

    @Operation(summary = "Revoke a delegation")
    @PreAuthorize("hasRole('MANAGER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        delegations.revoke(me.id(), id);
        return ResponseEntity.ok().build();
    }
}
