package com.hackathon.leave.service;

import static com.hackathon.leave.model.LeaveStatus.*;

import com.hackathon.leave.config.LeaveProperties;
import com.hackathon.leave.exception.ApiException;
import com.hackathon.leave.model.*;
import com.hackathon.leave.repository.ApprovalStepRepository;
import com.hackathon.leave.repository.LeaveRequestRepository;
import com.hackathon.leave.repository.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The ONLY place a leave request's status changes. {@link #transition} validates the move against an explicit
 * transition map, checks who may perform it, applies the balance effect, manages approval steps, writes the audit
 * event and sends notifications.
 */
@Slf4j
@Service
public class WorkflowService {

    public enum Action { SUBMIT, MANAGER_APPROVE, HR_APPROVE, REJECT, ESCALATE, CANCEL }

    /** Allowed targets for a brand-new request (there is no "from" status yet). */
    private static final Map<Action, Set<LeaveStatus>> INITIAL = Map.of(
            Action.SUBMIT, EnumSet.of(PENDING_MANAGER, PENDING_HR));

    /** The state machine: from status → action → allowed target statuses. REJECTED and CANCELLED are terminal. */
    private static final Map<LeaveStatus, Map<Action, Set<LeaveStatus>>> TRANSITIONS = new EnumMap<>(LeaveStatus.class);

    static {
        TRANSITIONS.put(PENDING_MANAGER, Map.of(
                Action.MANAGER_APPROVE, EnumSet.of(PENDING_HR, APPROVED),
                Action.REJECT, EnumSet.of(REJECTED),
                Action.ESCALATE, EnumSet.of(ESCALATED),
                Action.CANCEL, EnumSet.of(CANCELLED)));
        TRANSITIONS.put(ESCALATED, Map.of(
                Action.HR_APPROVE, EnumSet.of(APPROVED),
                Action.REJECT, EnumSet.of(REJECTED),
                Action.CANCEL, EnumSet.of(CANCELLED)));
        TRANSITIONS.put(PENDING_HR, Map.of(
                Action.HR_APPROVE, EnumSet.of(APPROVED),
                Action.REJECT, EnumSet.of(REJECTED),
                Action.CANCEL, EnumSet.of(CANCELLED)));
        TRANSITIONS.put(APPROVED, Map.of(Action.CANCEL, EnumSet.of(CANCELLED)));
        TRANSITIONS.put(REJECTED, Map.of());
        TRANSITIONS.put(CANCELLED, Map.of());
    }

    /** {@code from == null} means a new request. */
    public static boolean isAllowed(LeaveStatus from, Action action, LeaveStatus to) {
        Map<Action, Set<LeaveStatus>> map = from == null ? INITIAL : TRANSITIONS.get(from);
        return map != null && map.getOrDefault(action, Set.of()).contains(to);
    }

    public static Map<LeaveStatus, Map<Action, Set<LeaveStatus>>> transitionMap() {
        return Collections.unmodifiableMap(TRANSITIONS);
    }

    private final LeaveRequestRepository requests;
    private final ApprovalStepRepository steps;
    private final UserRepository users;
    private final BalanceService balances;
    private final DelegationResolver delegation;
    private final AuditService audit;
    private final NotificationService notifications;
    private final LeaveProperties props;
    private final Clock clock;

    public WorkflowService(LeaveRequestRepository requests, ApprovalStepRepository steps, UserRepository users,
                           BalanceService balances, DelegationResolver delegation, AuditService audit,
                           NotificationService notifications, LeaveProperties props, Clock clock) {
        this.requests = requests;
        this.steps = steps;
        this.users = users;
        this.balances = balances;
        this.delegation = delegation;
        this.audit = audit;
        this.notifications = notifications;
        this.props = props;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ the one entry point

    /**
     * @param request a persisted request, or (for SUBMIT) a new unsaved one with {@code status == null}
     * @param actor   the acting user; null means the SYSTEM (scheduler)
     */
    @Transactional
    public LeaveRequest transition(LeaveRequest request, Action action, User actor, String comment) {
        LeaveStatus from = request.getStatus();
        LeaveStatus to = targetFor(request, action);
        if (!isAllowed(from, action, to)) {
            throw ApiException.conflict("INVALID_TRANSITION", "Action " + action + " is not allowed while the request is "
                    + (from == null ? "new" : from));
        }
        authorize(request, action, actor, from);
        if (action == Action.REJECT && (comment == null || comment.isBlank())) {
            throw ApiException.badRequest("COMMENT_REQUIRED", "A comment is required to reject a request");
        }

        Instant now = clock.instant();
        int year = request.getFromDate().getYear();
        request.setStatus(to);
        request.setLastActionAt(now);
        if (from == null) {
            request.setCreatedAt(now);
        }
        request = requests.save(request);

        switch (action) {
            case SUBMIT -> onSubmit(request, actor, to, comment);
            case MANAGER_APPROVE -> {
                decideStep(request, ApprovalStage.MANAGER, Decision.APPROVED, now, comment);
                if (to == PENDING_HR) {
                    openHrStep(request);
                } else {
                    balances.commit(request.getEmployee(), request.getLeaveType(), year, request.getDays());
                }
                audit.record(request, actor, "MANAGER_APPROVED", from, to, comment);
            }
            case HR_APPROVE -> {
                decideStep(request, ApprovalStage.HR, Decision.APPROVED, now, comment);
                balances.commit(request.getEmployee(), request.getLeaveType(), year, request.getDays());
                audit.record(request, actor, "HR_APPROVED", from, to, comment);
            }
            case REJECT -> {
                decideStep(request, from == PENDING_MANAGER ? ApprovalStage.MANAGER : ApprovalStage.HR,
                        Decision.REJECTED, now, comment);
                balances.release(request.getEmployee(), request.getLeaveType(), year, request.getDays());
                audit.record(request, actor, "REJECTED", from, to, comment);
            }
            case ESCALATE -> {
                decideStep(request, ApprovalStage.MANAGER, Decision.ESCALATED, now,
                        "Manager step timed out after " + humanTimeout());
                openHrStep(request);
                audit.record(request, null, "ESCALATED", from, to,
                        "Manager step timed out after " + humanTimeout());
            }
            case CANCEL -> {
                if (from == APPROVED) {
                    balances.refund(request.getEmployee(), request.getLeaveType(), year, request.getDays());
                } else {
                    balances.release(request.getEmployee(), request.getLeaveType(), year, request.getDays());
                }
                audit.record(request, actor, "CANCELLED", from, to, comment);
            }
        }
        notifyAfter(request, action, actor, from, to, comment);
        log.info("Request {} {} -> {} via {} by {}", request.getId(), from, to, action,
                actor == null ? "SYSTEM" : actor.getEmail());
        return request;
    }

    // ------------------------------------------------------------------ permissions (read side)

    /** Whether {@code viewer} may approve/reject the request in its current state. */
    @Transactional(readOnly = true)
    public boolean canDecide(LeaveRequest r, User viewer) {
        if (viewer.getId().equals(r.getEmployee().getId())) {
            return false;
        }
        return switch (r.getStatus()) {
            case PENDING_MANAGER -> pendingStep(r, ApprovalStage.MANAGER)
                    .map(s -> s.getAssignee() != null && s.getAssignee().getId().equals(viewer.getId())).orElse(false);
            case ESCALATED, PENDING_HR -> viewer.getRole() == Role.HR;
            default -> false;
        };
    }

    public boolean canCancel(LeaveRequest r, User viewer) {
        if (r.getStatus() == null || r.getStatus() == REJECTED || r.getStatus() == CANCELLED) {
            return false;
        }
        return viewer.getId().equals(r.getEmployee().getId())
                || (viewer.getRole() == Role.HR && r.getStatus() == APPROVED);
    }

    // ------------------------------------------------------------------ internals

    private LeaveStatus targetFor(LeaveRequest r, Action action) {
        return switch (action) {
            case SUBMIT -> {
                User e = r.getEmployee();
                yield e.getRole() == Role.EMPLOYEE && e.getManager() != null ? PENDING_MANAGER : PENDING_HR;
            }
            case MANAGER_APPROVE -> r.getLeaveType().isRequiresHr() ? PENDING_HR : APPROVED;
            case HR_APPROVE -> APPROVED;
            case REJECT -> REJECTED;
            case ESCALATE -> ESCALATED;
            case CANCEL -> CANCELLED;
        };
    }

    private void authorize(LeaveRequest r, Action action, User actor, LeaveStatus from) {
        switch (action) {
            case SUBMIT -> {
                if (actor == null || !actor.getId().equals(r.getEmployee().getId())) {
                    throw ApiException.forbidden("You can only submit your own leave");
                }
            }
            case ESCALATE -> {
                if (actor != null) {
                    throw ApiException.forbidden("Only the system can escalate");
                }
            }
            case CANCEL -> {
                if (actor == null || !canCancel(r, actor)) {
                    throw ApiException.forbidden("You cannot cancel this request");
                }
            }
            case MANAGER_APPROVE, HR_APPROVE, REJECT -> {
                if (actor == null) {
                    throw ApiException.forbidden("Only a person can decide a request");
                }
                if (actor.getId().equals(r.getEmployee().getId())) {
                    throw ApiException.forbidden("You cannot decide your own request");
                }
                if (!canDecide(r, actor)) {
                    throw ApiException.forbidden("This request is not waiting for your decision");
                }
            }
        }
    }

    private void onSubmit(LeaveRequest r, User actor, LeaveStatus to, String comment) {
        balances.reserve(r.getEmployee(), r.getLeaveType(), r.getFromDate().getYear(), r.getDays());
        audit.record(r, actor, "SUBMITTED", null, to, comment);
        if (r.isFlagged()) {
            audit.record(r, null, "FLAGGED", null, to, r.getFlagReason());
        }
        if (to == PENDING_MANAGER) {
            User manager = r.getEmployee().getManager();
            DelegationResolver.Assignment a = delegation.resolve(manager, r.getEmployee(), LocalDate.now(clock));
            ApprovalStep step = newStep(r, ApprovalStage.MANAGER, a.assignee(), a.delegatedFrom());
            step.setDueAt(clock.instant().plus(Duration.ofMinutes(props.escalationTimeoutMinutes())));
            steps.save(step);
            if (a.delegatedFrom() != null) {
                audit.record(r, null, "DELEGATED", null, to, "Routed to " + a.assignee().getName()
                        + " (delegated by " + a.delegatedFrom().getName() + ")");
            }
        } else {
            openHrStep(r);
        }
    }

    private void openHrStep(LeaveRequest r) {
        List<User> hr = users.findByRole(Role.HR).stream().filter(User::isActive)
                .filter(u -> !u.getId().equals(r.getEmployee().getId())).toList();
        steps.save(newStep(r, ApprovalStage.HR, hr.isEmpty() ? null : hr.get(0), null));
    }

    private ApprovalStep newStep(LeaveRequest r, ApprovalStage stage, User assignee, User delegatedFrom) {
        ApprovalStep s = new ApprovalStep();
        s.setRequest(r);
        s.setStage(stage);
        s.setAssignee(assignee);
        s.setDelegatedFrom(delegatedFrom);
        s.setDecision(Decision.PENDING);
        return s;
    }

    private Optional<ApprovalStep> pendingStep(LeaveRequest r, ApprovalStage stage) {
        return steps.findByRequestOrderByIdAsc(r).stream()
                .filter(s -> s.getStage() == stage && s.getDecision() == Decision.PENDING).reduce((a, b) -> b);
    }

    private void decideStep(LeaveRequest r, ApprovalStage stage, Decision decision, Instant at, String comment) {
        pendingStep(r, stage).ifPresent(s -> {
            s.setDecision(decision);
            s.setDecidedAt(at);
            s.setComment(comment);
            steps.save(s);
        });
    }

    private void notifyAfter(LeaveRequest r, Action action, User actor, LeaveStatus from, LeaveStatus to,
                             String comment) {
        User owner = r.getEmployee();
        String what = r.getLeaveType().getName() + " " + r.getFromDate() + " → " + r.getToDate();
        switch (action) {
            case SUBMIT -> {
                String flag = r.isFlagged() ? " (conflict flagged)" : "";
                if (to == PENDING_MANAGER) {
                    pendingStep(r, ApprovalStage.MANAGER).ifPresent(s -> notifications.notify(s.getAssignee(),
                            "New leave request from " + owner.getName() + ": " + what + flag, r));
                } else {
                    hrUsers(owner).forEach(h -> notifications.notify(h,
                            "New leave request from " + owner.getName() + ": " + what + flag, r));
                }
            }
            case MANAGER_APPROVE -> {
                if (to == PENDING_HR) {
                    notifications.notify(owner, "Your manager approved your leave (" + what + "); waiting for HR", r);
                    hrUsers(owner).forEach(h -> notifications.notify(h,
                            owner.getName() + "'s leave (" + what + ") is ready for HR approval", r));
                } else {
                    notifications.notify(owner, "Your leave (" + what + ") is approved", r);
                }
            }
            case HR_APPROVE -> notifications.notify(owner, "Your leave (" + what + ") is approved", r);
            case REJECT -> notifications.notify(owner, "Your leave (" + what + ") was rejected: " + comment, r);
            case ESCALATE -> {
                notifications.notify(owner, "Your leave (" + what + ") was escalated to HR: the manager did not respond", r);
                hrUsers(owner).forEach(h -> notifications.notify(h,
                        "Escalated: " + owner.getName() + "'s leave (" + what + ") needs an HR decision", r));
            }
            case CANCEL -> {
                if (actor != null && !actor.getId().equals(owner.getId())) {
                    notifications.notify(owner, "Your leave (" + what + ") was cancelled by HR", r);
                } else if (from == PENDING_MANAGER) {
                    steps.findByRequestOrderByIdAsc(r).stream().filter(s -> s.getStage() == ApprovalStage.MANAGER)
                            .reduce((a, b) -> b).ifPresent(s -> notifications.notify(s.getAssignee(),
                                    owner.getName() + " withdrew the request (" + what + ")", r));
                } else if (from == PENDING_HR || from == ESCALATED) {
                    hrUsers(owner).forEach(h -> notifications.notify(h,
                            owner.getName() + " withdrew the request (" + what + ")", r));
                }
            }
        }
    }

    private String humanTimeout() {
        int m = props.escalationTimeoutMinutes();
        return m >= 60 && m % 60 == 0 ? m / 60 + " hour(s)" : m + " minute(s)";
    }

    private List<User> hrUsers(User exclude) {
        return users.findByRole(Role.HR).stream().filter(User::isActive)
                .filter(u -> !u.getId().equals(exclude.getId())).toList();
    }
}
