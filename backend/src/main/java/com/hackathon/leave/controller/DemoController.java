package com.hackathon.leave.controller;

import org.springframework.context.annotation.Profile;
import com.hackathon.leave.dto.LeaveRequestDto;
import com.hackathon.leave.dto.ResetSeedResponse;
import com.hackathon.leave.security.AuthUser;
import com.hackathon.leave.service.DemoService;
import com.hackathon.leave.service.SeedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Profile("demo")
@Tag(name = "Demo")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/demo")
public class DemoController {

    private final DemoService demo;
    private final SeedService seed;

    public DemoController(DemoService demo, SeedService seed) {
        this.demo = demo;
        this.seed = seed;
    }

    @Operation(summary = "Force the manager step of a pending request to time out now, so escalation can be shown live")
    @PreAuthorize("hasAnyRole('MANAGER','HR')")
    @PostMapping("/simulate-timeout/{id}")
    public ResponseEntity<LeaveRequestDto> simulateTimeout(@AuthenticationPrincipal AuthUser me,
                                                           @PathVariable Long id) {
        return ResponseEntity.ok(demo.simulateTimeout(me.id(), id));
    }

    @Operation(summary = "Wipe and reload the fixed seed scenarios (HR only)")
    @PreAuthorize("hasRole('HR')")
    @PostMapping("/reset-seed")
    public ResponseEntity<ResetSeedResponse> resetSeed() {
        return ResponseEntity.ok(seed.reset());
    }
}
