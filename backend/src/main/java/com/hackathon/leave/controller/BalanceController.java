package com.hackathon.leave.controller;

import com.hackathon.leave.dto.BalanceDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Balances")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/balances")
public class BalanceController {

    @Operation(summary = "The caller's balances for the current year, with pro-rata explanation")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<List<BalanceDto>> mine() {
        return Stubs.notImplemented();
    }
}
