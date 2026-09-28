package com.hackathon.leave.controller;

import com.hackathon.leave.dto.DirectoryDto;
import com.hackathon.leave.service.DirectoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Directory")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/directory")
public class DirectoryController {

    private final DirectoryService directory;

    public DirectoryController(DirectoryService directory) {
        this.directory = directory;
    }

    @Operation(summary = "Teams and managers, for pickers (calendar team, delegation target)")
    @PreAuthorize("hasAnyRole('MANAGER','HR')")
    @GetMapping
    public ResponseEntity<DirectoryDto> get() {
        return ResponseEntity.ok(directory.directory());
    }
}
