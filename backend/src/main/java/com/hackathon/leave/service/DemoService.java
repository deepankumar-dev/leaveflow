package com.hackathon.leave.service;

import org.springframework.context.annotation.Profile;
import com.hackathon.leave.dto.LeaveRequestDto;
import com.hackathon.leave.exception.ApiException;
import com.hackathon.leave.model.LeaveRequest;
import com.hackathon.leave.model.LeaveStatus;
import com.hackathon.leave.repository.LeaveRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Demo-only helpers: force a timeout so escalation can be shown without waiting. */
@Profile("demo")
@Service
public class DemoService {

    private final LeaveRequestRepository requests;
    private final EscalationService escalation;
    private final LeaveService leaves;

    public DemoService(LeaveRequestRepository requests, EscalationService escalation, LeaveService leaves) {
        this.requests = requests;
        this.escalation = escalation;
        this.leaves = leaves;
    }

    /** Not transactional itself: escalate() commits first, then the fresh state is read back. */
    public LeaveRequestDto simulateTimeout(Long userId, Long requestId) {
        LeaveRequest r = requests.findById(requestId)
                .orElseThrow(() -> ApiException.notFound("Leave request " + requestId + " not found"));
        if (r.getStatus() != LeaveStatus.PENDING_MANAGER) {
            throw ApiException.conflict("INVALID_TRANSITION",
                    "Only a request waiting for its manager can time out; this one is " + r.getStatus());
        }
        escalation.escalate(requestId);
        return leaves.get(userId, requestId);
    }
}
