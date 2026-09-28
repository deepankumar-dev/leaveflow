package com.hackathon.leave.controller;

import com.hackathon.leave.dto.LeaveRequestDto;
import com.hackathon.leave.dto.ResetSeedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Demo")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/demo")
public class DemoController {

    @Operation(summary = "Force the manager step of a pending request to time out now, so escalation can be shown live")
    @PreAuthorize("hasAnyRole('MANAGER','HR')")
    @PostMapping("/simulate-timeout/{id}")
    public ResponseEntity<LeaveRequestDto> simulateTimeout(@PathVariable Long id) {
        return Stubs.notImplemented();
    }

    @Operation(summary = "Wipe and reload the fixed seed scenarios (HR only)")
    @PreAuthorize("hasRole('HR')")
    @PostMapping("/reset-seed")
    public ResponseEntity<ResetSeedResponse> resetSeed() {
        return Stubs.notImplemented();
    }
}
