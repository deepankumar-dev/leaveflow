package com.hackathon.leave.service;

import com.hackathon.leave.model.AuditEvent;
import com.hackathon.leave.model.LeaveRequest;
import com.hackathon.leave.model.LeaveStatus;
import com.hackathon.leave.model.User;
import com.hackathon.leave.repository.AuditEventRepository;
import java.time.Clock;
import org.springframework.stereotype.Service;

/** Append-only audit trail; a null actor is the SYSTEM. */
@Service
public class AuditService {

    private final AuditEventRepository events;
    private final Clock clock;

    public AuditService(AuditEventRepository events, Clock clock) {
        this.events = events;
        this.clock = clock;
    }

    public AuditEvent record(LeaveRequest request, User actor, String action, LeaveStatus from, LeaveStatus to,
                             String comment) {
        AuditEvent e = new AuditEvent();
        e.setRequest(request);
        e.setActor(actor);
        e.setAction(action);
        e.setFromStatus(from);
        e.setToStatus(to);
        e.setComment(comment);
        e.setAt(clock.instant());
        return events.save(e);
    }
}
