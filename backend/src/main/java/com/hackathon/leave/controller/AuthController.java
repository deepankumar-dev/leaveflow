package com.hackathon.leave.controller;

import com.hackathon.leave.dto.LoginRequest;
import com.hackathon.leave.dto.LoginResponse;
import com.hackathon.leave.dto.UserDto;
import com.hackathon.leave.security.AuthUser;
import com.hackathon.leave.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @Operation(summary = "Log in with email and password; returns a JWT (public endpoint)")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(auth.login(request));
    }

    @Operation(summary = "The currently authenticated user", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<UserDto> me(@AuthenticationPrincipal AuthUser me) {
        return ResponseEntity.ok(auth.me(me.id()));
    }
}
