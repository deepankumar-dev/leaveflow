package com.hackathon.leave.dto;

import com.hackathon.leave.model.LeaveStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record LeaveRequestDto(Long id, Long employeeId, String employeeName, String teamName, String leaveTypeCode,
                              String leaveTypeName, LocalDate fromDate, LocalDate toDate, BigDecimal days,
                              String reason, LeaveStatus status, boolean flagged, String flagReason,
                              Instant createdAt, Instant lastActionAt, List<ApprovalStepDto> steps,
                              boolean canApprove, boolean canCancel) {}
