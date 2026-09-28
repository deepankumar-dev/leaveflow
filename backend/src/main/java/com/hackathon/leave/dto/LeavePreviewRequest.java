package com.hackathon.leave.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/** Body of POST /api/leaves/preview: what the apply form sends on every change. */
public record LeavePreviewRequest(@NotBlank String leaveTypeCode, @NotNull LocalDate fromDate,
                                  @NotNull LocalDate toDate) {}
