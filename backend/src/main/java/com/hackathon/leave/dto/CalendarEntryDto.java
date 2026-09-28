package com.hackathon.leave.dto;

import com.hackathon.leave.model.LeaveStatus;
import java.time.LocalDate;

public record CalendarEntryDto(Long requestId, Long employeeId, String employeeName, String leaveTypeCode,
                               LocalDate fromDate, LocalDate toDate, LeaveStatus status, boolean flagged) {}
