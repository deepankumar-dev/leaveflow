package com.hackathon.leave.dto;

import java.time.LocalDate;

public record DelegationDto(Long id, Long delegatorId, String delegatorName, Long delegateId, String delegateName,
                            LocalDate fromDate, LocalDate toDate, boolean active) {}
