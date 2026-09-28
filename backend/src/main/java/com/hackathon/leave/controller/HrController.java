package com.hackathon.leave.controller;

import com.hackathon.leave.dto.LeaveRequestDto;
import com.hackathon.leave.dto.PageResponse;
import com.hackathon.leave.model.LeaveStatus;
import com.hackathon.leave.security.AuthUser;
import com.hackathon.leave.service.LeaveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "HR")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/hr")
public class HrController {

    private final LeaveService leaves;

    public HrController(LeaveService leaves) {
        this.leaves = leaves;
    }

    @Operation(summary = "All requests organisation-wide, filterable and paged")
    @PreAuthorize("hasRole('HR')")
    @GetMapping("/requests")
    public ResponseEntity<PageResponse<LeaveRequestDto>> requests(
            @AuthenticationPrincipal AuthUser me,
            @RequestParam(required = false) LeaveStatus status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(leaves.list(me.id(), true, status, type, from, to, q, sort, page, size));
    }
}
