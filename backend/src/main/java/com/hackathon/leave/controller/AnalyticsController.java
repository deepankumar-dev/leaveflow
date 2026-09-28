package com.hackathon.leave.controller;

import com.hackathon.leave.dto.AnalyticsSummaryDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Analytics")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    @Operation(summary = "Organisation-wide leave analytics summary")
    @PreAuthorize("hasRole('HR')")
    @GetMapping("/summary")
    public ResponseEntity<AnalyticsSummaryDto> summary() {
        return Stubs.notImplemented();
    }
}
