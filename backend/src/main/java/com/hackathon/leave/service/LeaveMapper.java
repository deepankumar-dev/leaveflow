package com.hackathon.leave.service;

import com.hackathon.leave.dto.*;
import com.hackathon.leave.model.*;
import com.hackathon.leave.repository.ApprovalStepRepository;
import java.util.List;
import org.springframework.stereotype.Component;

/** Entity → DTO conversion (call inside a transaction: relations are lazy). */
@Component
public class LeaveMapper {

    private final ApprovalStepRepository steps;
    private final WorkflowService workflow;

    public LeaveMapper(ApprovalStepRepository steps, WorkflowService workflow) {
        this.steps = steps;
        this.workflow = workflow;
    }

    public LeaveRequestDto toDto(LeaveRequest r, User viewer) {
        List<ApprovalStepDto> stepDtos = steps.findByRequestOrderByIdAsc(r).stream().map(this::toDto).toList();
        User e = r.getEmployee();
        return new LeaveRequestDto(r.getId(), e.getId(), e.getName(), e.getTeam() == null ? null : e.getTeam().getName(),
                r.getLeaveType().getCode(), r.getLeaveType().getName(), r.getFromDate(), r.getToDate(), r.getDays(),
                r.getReason(), r.getStatus(), r.isFlagged(), r.getFlagReason(), r.getCreatedAt(), r.getLastActionAt(),
                stepDtos, workflow.canDecide(r, viewer), workflow.canCancel(r, viewer));
    }

    public ApprovalStepDto toDto(ApprovalStep s) {
        return new ApprovalStepDto(s.getId(), s.getStage(), s.getAssignee() == null ? null : s.getAssignee().getId(),
                s.getAssignee() == null ? null : s.getAssignee().getName(),
                s.getDelegatedFrom() == null ? null : s.getDelegatedFrom().getName(), s.getDueAt(), s.getDecision(),
                s.getDecidedAt(), s.getComment());
    }

    public TimelineEventDto toDto(AuditEvent e) {
        return new TimelineEventDto(e.getId(), e.getAction(), e.getActor() == null ? "SYSTEM" : e.getActor().getName(),
                e.getFromStatus(), e.getToStatus(), e.getComment(), e.getAt());
    }

    public UserDto toDto(User u) {
        return new UserDto(u.getId(), u.getName(), u.getEmail(), u.getRole(),
                u.getTeam() == null ? null : u.getTeam().getId(), u.getTeam() == null ? null : u.getTeam().getName(),
                u.getManager() == null ? null : u.getManager().getId(),
                u.getManager() == null ? null : u.getManager().getName(), u.getJoinDate());
    }
}
