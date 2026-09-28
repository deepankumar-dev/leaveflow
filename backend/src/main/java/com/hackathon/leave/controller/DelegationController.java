package com.hackathon.leave.controller;

import com.hackathon.leave.dto.DelegationDto;
import com.hackathon.leave.dto.DelegationRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Delegations")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/delegations")
public class DelegationController {

    @Operation(summary = "Delegate the caller's manager approvals to another manager for a date range")
    @PreAuthorize("hasRole('MANAGER')")
    @PostMapping
    public ResponseEntity<DelegationDto> create(@Valid @RequestBody DelegationRequest request) {
        return Stubs.notImplemented();
    }

    @Operation(summary = "The caller's active delegations (given by them)")
    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping
    public ResponseEntity<List<DelegationDto>> list() {
        return Stubs.notImplemented();
    }

    @Operation(summary = "Revoke a delegation")
    @PreAuthorize("hasRole('MANAGER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return Stubs.notImplemented();
    }
}
