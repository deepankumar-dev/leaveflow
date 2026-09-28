package com.hackathon.leave.dto;

import com.hackathon.leave.model.LeaveStatus;
import java.time.Instant;

/** One audit row. actorName is "SYSTEM" when the actor is null. */
public record TimelineEventDto(Long id, String action, String actorName, LeaveStatus fromStatus,
                               LeaveStatus toStatus, String comment, Instant at) {}
