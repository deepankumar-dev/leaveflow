package com.hackathon.leave.controller;

import com.hackathon.leave.dto.AnalyticsSummaryDto;
import com.hackathon.leave.dto.ForecastDto;
import com.hackathon.leave.service.AnalyticsService;
import com.hackathon.leave.service.ForecastService;
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
    private final ForecastService forecast;

    public AnalyticsController(AnalyticsService analytics, ForecastService forecast) {
        this.analytics = analytics;
        this.forecast = forecast;
    }

    @Operation(summary = "Organisation-wide leave analytics summary")
    @PreAuthorize("hasRole('HR')")
    @GetMapping("/summary")
    public ResponseEntity<AnalyticsSummaryDto> summary() {
        return ResponseEntity.ok(analytics.summary());
    }

    @Operation(summary = "Team capacity forecast: approved + pending leave per day for the next N days")
    @PreAuthorize("hasRole('HR')")
    @GetMapping("/forecast")
    public ResponseEntity<ForecastDto> forecast(@RequestParam(required = false) Long teamId,
                                                @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(forecast.forecast(teamId, days));
    }
}
