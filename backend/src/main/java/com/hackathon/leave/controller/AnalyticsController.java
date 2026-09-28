package com.hackathon.leave.controller;

import com.hackathon.leave.dto.AnalyticsSummaryDto;
import com.hackathon.leave.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Analytics")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analytics;

    public AnalyticsController(AnalyticsService analytics) {
        this.analytics = analytics;
    }

    @Operation(summary = "Organisation-wide leave analytics summary")
    @PreAuthorize("hasRole('HR')")
    @GetMapping("/summary")
    public ResponseEntity<AnalyticsSummaryDto> summary() {
        return ResponseEntity.ok(analytics.summary());
    }
}
