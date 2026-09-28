package com.hackathon.leave.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record DelegationRequest(@NotNull Long delegateId, @NotNull LocalDate fromDate, @NotNull LocalDate toDate) {}
