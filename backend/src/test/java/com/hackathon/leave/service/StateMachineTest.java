package com.hackathon.leave.service;

import static com.hackathon.leave.model.LeaveStatus.*;
import static com.hackathon.leave.service.WorkflowService.Action.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.hackathon.leave.model.LeaveStatus;
import com.hackathon.leave.service.WorkflowService.Action;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Checks the transition table against docs/RULES.md section 11, for every (from, action, to) combination. */
class StateMachineTest {

    private record Move(LeaveStatus from, Action action, LeaveStatus to) {}

    /** RULES section 11, written out independently of the implementation. */
    private static final Set<Move> ALLOWED = Set.of(
            new Move(null, SUBMIT, PENDING_MANAGER),
            new Move(null, SUBMIT, PENDING_HR),
            new Move(PENDING_MANAGER, MANAGER_APPROVE, PENDING_HR),
            new Move(PENDING_MANAGER, MANAGER_APPROVE, APPROVED),
            new Move(PENDING_MANAGER, ESCALATE, ESCALATED),
            new Move(PENDING_MANAGER, REJECT, REJECTED),
            new Move(PENDING_MANAGER, CANCEL, CANCELLED),
            new Move(ESCALATED, HR_APPROVE, APPROVED),
            new Move(ESCALATED, REJECT, REJECTED),
            new Move(ESCALATED, CANCEL, CANCELLED),
            new Move(PENDING_HR, HR_APPROVE, APPROVED),
            new Move(PENDING_HR, REJECT, REJECTED),
            new Move(PENDING_HR, CANCEL, CANCELLED),
            new Move(APPROVED, CANCEL, CANCELLED));

    @Test
    @DisplayName("exactly the moves in RULES section 11 are allowed, and nothing else")
    void everyCombination() {
        Set<Move> actuallyAllowed = new HashSet<>();
        Set<LeaveStatus> froms = new HashSet<>(Set.of(LeaveStatus.values()));
        froms.add(null);
        for (LeaveStatus from : froms) {
            for (Action action : Action.values()) {
                for (LeaveStatus to : LeaveStatus.values()) {
                    if (WorkflowService.isAllowed(from, action, to)) {
                        actuallyAllowed.add(new Move(from, action, to));
                    }
                }
            }
        }
        assertThat(actuallyAllowed).isEqualTo(ALLOWED);
    }

    @Test
    @DisplayName("REJECTED and CANCELLED are terminal")
    void terminalStates() {
        for (LeaveStatus terminal : new LeaveStatus[] {REJECTED, CANCELLED}) {
            for (Action action : Action.values()) {
                for (LeaveStatus to : LeaveStatus.values()) {
                    assertThat(WorkflowService.isAllowed(terminal, action, to)).isFalse();
                }
            }
        }
    }

    @Test
    @DisplayName("an approved request can only be cancelled")
    void approvedOnlyCancels() {
        for (Action action : Action.values()) {
            for (LeaveStatus to : LeaveStatus.values()) {
                boolean expected = action == CANCEL && to == CANCELLED;
                assertThat(WorkflowService.isAllowed(APPROVED, action, to)).isEqualTo(expected);
            }
        }
    }

    @Test
    @DisplayName("a manager cannot approve after the request escalated; HR must decide")
    void escalatedNeedsHr() {
        assertThat(WorkflowService.isAllowed(ESCALATED, MANAGER_APPROVE, APPROVED)).isFalse();
        assertThat(WorkflowService.isAllowed(ESCALATED, MANAGER_APPROVE, PENDING_HR)).isFalse();
        assertThat(WorkflowService.isAllowed(ESCALATED, HR_APPROVE, APPROVED)).isTrue();
    }
}
