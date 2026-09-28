package com.hackathon.leave.dto;

import com.hackathon.leave.model.ApprovalStage;
import com.hackathon.leave.model.Decision;
import java.time.Instant;

public record ApprovalStepDto(Long id, ApprovalStage stage, Long assigneeId, String assigneeName,
                              String delegatedFromName, Instant dueAt, Decision decision, Instant decidedAt,
                              String comment) {}
