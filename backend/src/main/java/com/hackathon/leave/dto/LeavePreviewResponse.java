package com.hackathon.leave.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Live answer for the apply form. {@code valid} is false when the request would be rejected on submit
 * (all-holiday range, insufficient balance, overlapping own leave); {@code errors} says why.
 * A conflict flag never makes a request invalid.
 */
public record LeavePreviewResponse(boolean valid, List<String> errors, BigDecimal workingDays,
                                   BigDecimal balanceAvailable, BigDecimal balanceAfter, String balanceExplanation,
                                   boolean conflictFlagged, String conflictReason,
                                   List<String> overlappingTeammates, int teamSize) {}
