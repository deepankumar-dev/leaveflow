package com.hackathon.leave.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record LeaveApplyRequest(@NotBlank String leaveTypeCode, @NotNull LocalDate fromDate,
                                @NotNull LocalDate toDate, @Size(max = 1000) String reason) {}
