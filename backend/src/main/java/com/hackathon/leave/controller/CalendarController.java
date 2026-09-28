package com.hackathon.leave.controller;

import com.hackathon.leave.dto.TeamCalendarDto;
import com.hackathon.leave.security.AuthUser;
import com.hackathon.leave.service.CalendarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Calendar")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/calendar")
public class CalendarController {

    private final CalendarService calendar;

    public CalendarController(CalendarService calendar) {
        this.calendar = calendar;
    }

    @Operation(summary = "Team leave calendar for a month")
    @PreAuthorize("hasAnyRole('MANAGER','HR')")
    @GetMapping("/team")
    public ResponseEntity<TeamCalendarDto> team(
            @AuthenticationPrincipal AuthUser me,
            @Parameter(description = "yyyy-MM", example = "2026-11") @RequestParam String month,
            @Parameter(description = "HR only: which team; others always get their own")
            @RequestParam(required = false) Long teamId) {
        return ResponseEntity.ok(calendar.team(me.id(), month, teamId));
    }
}
