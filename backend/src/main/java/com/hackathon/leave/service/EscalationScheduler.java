package com.hackathon.leave.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs the escalation check every {@code leave.escalation-check-seconds}. It loops here (not inside the service) so
 * each request is escalated through the service's transactional proxy, in its own transaction.
 */
@Slf4j
@Component
public class EscalationScheduler {

    private final EscalationService escalation;

    public EscalationScheduler(EscalationService escalation) {
        this.escalation = escalation;
    }

    @Scheduled(fixedDelayString = "#{${leave.escalation-check-seconds} * 1000}",
            initialDelayString = "#{${leave.escalation-check-seconds} * 1000}")
    public void scheduledRun() {
        int moved = run();
        if (moved > 0) {
            log.info("Escalated {} overdue request(s) to HR", moved);
        }
    }

    /** Escalates every overdue request; returns how many moved. Safe to call repeatedly (idempotent). */
    public int run() {
        int moved = 0;
        for (Long id : escalation.dueRequestIds()) {
            try {
                if (escalation.escalate(id)) {
                    moved++;
                }
            } catch (RuntimeException e) {
                // e.g. the manager decided at the same moment: the next run sees the settled state.
                log.warn("Escalation of request {} skipped: {}", id, e.getMessage());
            }
        }
        return moved;
    }
}
