package com.hackathon.leave.service;

import com.hackathon.leave.exception.ApiException;
import com.hackathon.leave.model.*;
import com.hackathon.leave.repository.ApprovalStepRepository;
import com.hackathon.leave.repository.LeaveRequestRepository;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Escalation (RULES section 7): a manager step past its deadline moves the request to HR. The system never approves
 * or rejects on anyone's behalf. Each request escalates in its own transaction, so one failure or a manager approving
 * at the same moment (optimistic lock) cannot affect the others.
 */
@Service
public class EscalationService {

    private final ApprovalStepRepository steps;
    private final LeaveRequestRepository requests;
    private final WorkflowService workflow;
    private final Clock clock;

    public EscalationService(ApprovalStepRepository steps, LeaveRequestRepository requests, WorkflowService workflow,
                             Clock clock) {
        this.steps = steps;
        this.requests = requests;
        this.workflow = workflow;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Long> dueRequestIds() {
        return steps.findByStageAndDecisionAndDueAtBefore(ApprovalStage.MANAGER, Decision.PENDING, clock.instant())
                .stream().map(s -> s.getRequest().getId()).distinct().toList();
    }

    /** Escalates one request; returns false when it is no longer waiting on its manager (already decided). */
    @Transactional
    public boolean escalate(Long requestId) {
        LeaveRequest r = requests.findById(requestId).orElseThrow(() -> ApiException.notFound("Request not found"));
        if (r.getStatus() != LeaveStatus.PENDING_MANAGER) {
            return false;
        }
        workflow.transition(r, WorkflowService.Action.ESCALATE, null, null);
        return true;
    }
}
