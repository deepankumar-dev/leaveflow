package com.hackathon.leave.controller;

import com.hackathon.leave.dto.BalanceDto;
import com.hackathon.leave.security.AuthUser;
import com.hackathon.leave.service.BalanceQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Balances")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/balances")
public class BalanceController {

    private final BalanceQueryService balances;

    public BalanceController(BalanceQueryService balances) {
        this.balances = balances;
    }

    @Operation(summary = "The caller's balances for the current year, with pro-rata explanation")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<List<BalanceDto>> mine(@AuthenticationPrincipal AuthUser me) {
        return ResponseEntity.ok(balances.mine(me.id()));
    }
}
