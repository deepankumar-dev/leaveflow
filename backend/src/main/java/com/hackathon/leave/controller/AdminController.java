package com.hackathon.leave.controller;

import com.hackathon.leave.dto.AdminUserRequest;
import com.hackathon.leave.dto.DirectoryDto;
import com.hackathon.leave.dto.ResetPasswordRequest;
import com.hackathon.leave.dto.TeamRequest;
import com.hackathon.leave.dto.UserDto;
import com.hackathon.leave.security.AuthUser;
import com.hackathon.leave.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('HR')")
public class AdminController {

    private final AdminService admin;

    public AdminController(AdminService admin) {
        this.admin = admin;
    }

    @Operation(summary = "All people, including deactivated ones")
    @GetMapping("/users")
    public ResponseEntity<List<UserDto>> users() {
        return ResponseEntity.ok(admin.list());
    }

    @Operation(summary = "Add a person with a temporary password they must change at first sign-in")
    @PostMapping("/users")
    public ResponseEntity<UserDto> create(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody AdminUserRequest request) {
        return ResponseEntity.ok(admin.create(request, me.id()));
    }

    @Operation(summary = "Edit a person, or deactivate them (people are never deleted)")
    @PutMapping("/users/{id}")
    public ResponseEntity<UserDto> update(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                                          @Valid @RequestBody AdminUserRequest request) {
        return ResponseEntity.ok(admin.update(id, request, me.id()));
    }

    @Operation(summary = "Set a new temporary password; the person must change it at next sign-in")
    @PostMapping("/users/{id}/reset-password")
    public ResponseEntity<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordRequest request) {
        admin.resetPassword(id, request.password());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Add a team")
    @PostMapping("/teams")
    public ResponseEntity<DirectoryDto.TeamInfo> createTeam(@Valid @RequestBody TeamRequest request) {
        return ResponseEntity.ok(admin.createTeam(request.name()));
    }
}
