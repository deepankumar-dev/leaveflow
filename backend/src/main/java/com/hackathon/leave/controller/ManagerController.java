package com.hackathon.leave.controller;

import com.hackathon.leave.dto.LeaveRequestDto;
import com.hackathon.leave.dto.PageResponse;
import com.hackathon.leave.model.LeaveStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Manager")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/manager")
public class ManagerController {

    @Operation(summary = "Requests in the caller's approval scope (own team plus delegated), filterable and paged")
    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping("/requests")
    public ResponseEntity<PageResponse<LeaveRequestDto>> requests(
            @RequestParam(required = false) LeaveStatus status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Stubs.notImplemented();
    }
}
