package com.hackathon.leave.controller;

import com.hackathon.leave.dto.PublicConfigDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Public")
@RestController
@RequestMapping("/api/public")
public class PublicConfigController {

    private final boolean demo;

    public PublicConfigController(Environment env) {
        this.demo = env.acceptsProfiles(Profiles.of("demo"));
    }

    @Operation(summary = "Settings needed before sign-in (public endpoint)")
    @GetMapping("/config")
    public ResponseEntity<PublicConfigDto> config() {
        return ResponseEntity.ok(new PublicConfigDto(demo, "LeaveFlow"));
    }
}
