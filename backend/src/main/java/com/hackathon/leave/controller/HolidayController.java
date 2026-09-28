package com.hackathon.leave.controller;

import com.hackathon.leave.dto.HolidayDto;
import com.hackathon.leave.dto.HolidayRequest;
import com.hackathon.leave.service.HolidayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Holidays")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/holidays")
public class HolidayController {

    private final HolidayService holidays;

    public HolidayController(HolidayService holidays) {
        this.holidays = holidays;
    }

    @Operation(summary = "All holidays, by date")
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<List<HolidayDto>> list() {
        return ResponseEntity.ok(holidays.list());
    }

    @Operation(summary = "Add a holiday (HR only)")
    @PreAuthorize("hasRole('HR')")
    @PostMapping
    public ResponseEntity<HolidayDto> create(@Valid @RequestBody HolidayRequest request) {
        return ResponseEntity.ok(holidays.create(request));
    }
}
