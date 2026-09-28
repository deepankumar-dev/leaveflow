package com.hackathon.leave.service;

import com.hackathon.leave.dto.*;
import com.hackathon.leave.exception.ApiException;
import com.hackathon.leave.model.*;
import com.hackathon.leave.repository.*;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Stream;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Use-cases around leave requests: preview, apply, read models and the approve/reject/cancel entry points. */
@Service
@Transactional
public class LeaveService {

    /** A rule violation; apply throws the first one, preview reports them all. */
    private record Problem(HttpStatus status, String code, String message) {
        ApiException toException() {
            return new ApiException(status, code, message);
        }
    }

    private record Checked(LeaveType type, BigDecimal days, List<Problem> problems, BigDecimal available) {}

    private final LeaveRequestRepository requests;
    private final LeaveTypeRepository types;
    private final UserRepository users;
    private final ApprovalStepRepository steps;
    private final AuditEventRepository auditEvents;
    private final RuleEngine rules;
    private final BalanceService balances;
    private final WorkflowService workflow;
    private final LeaveMapper mapper;
    private final Clock clock;

    public LeaveService(LeaveRequestRepository requests, LeaveTypeRepository types, UserRepository users,
                        ApprovalStepRepository steps, AuditEventRepository auditEvents, RuleEngine rules,
                        BalanceService balances, WorkflowService workflow, LeaveMapper mapper, Clock clock) {
        this.requests = requests;
        this.types = types;
        this.users = users;
        this.steps = steps;
        this.auditEvents = auditEvents;
        this.rules = rules;
        this.balances = balances;
        this.workflow = workflow;
        this.mapper = mapper;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ preview / apply

    @Transactional(readOnly = true)
    public LeavePreviewResponse preview(Long userId, LeavePreviewRequest req) {
        User user = user(userId);
        Checked c = check(user, req.leaveTypeCode(), req.fromDate(), req.toDate());
        RuleEngine.ConflictResult conflict = c.type() == null || req.fromDate().isAfter(req.toDate())
                ? new RuleEngine.ConflictResult(false, null, List.of(), 0)
                : rules.conflict(user, req.fromDate(), req.toDate());
        BigDecimal available = c.available();
        BigDecimal after = available == null || c.days() == null ? null : available.subtract(c.days());
        String explanation = c.type() == null ? null
                : rules.explanation(user, c.type(), req.fromDate().getYear());
        return new LeavePreviewResponse(c.problems().isEmpty(), c.problems().stream().map(Problem::message).toList(),
                c.days(), available, after, explanation, conflict.flagged(), conflict.reason(),
                conflict.overlappingTeammates(), conflict.teamSize());
    }

    public LeaveRequestDto apply(Long userId, LeaveApplyRequest req) {
        User user = user(userId);
        Checked c = check(user, req.leaveTypeCode(), req.fromDate(), req.toDate());
        if (!c.problems().isEmpty()) {
            throw c.problems().get(0).toException();
        }
        RuleEngine.ConflictResult conflict = rules.conflict(user, req.fromDate(), req.toDate());

        LeaveRequest r = new LeaveRequest();
        r.setEmployee(user);
        r.setLeaveType(c.type());
        r.setFromDate(req.fromDate());
        r.setToDate(req.toDate());
        r.setDays(c.days());
        r.setReason(req.reason());
        r.setFlagged(conflict.flagged());
        r.setFlagReason(conflict.reason());
        r = workflow.transition(r, WorkflowService.Action.SUBMIT, user, "Submitted");
        return mapper.toDto(r, user);
    }

    private Checked check(User user, String typeCode, LocalDate from, LocalDate to) {
        List<Problem> problems = new ArrayList<>();
        if (from.isAfter(to)) {
            problems.add(new Problem(HttpStatus.BAD_REQUEST, "INVALID_RANGE", "The end date cannot be before the start date"));
            return new Checked(null, null, problems, null);
        }
        LeaveType type = types.findByCode(typeCode.toUpperCase()).orElse(null);
        if (type == null) {
            problems.add(new Problem(HttpStatus.BAD_REQUEST, "UNKNOWN_LEAVE_TYPE", "Unknown leave type " + typeCode));
            return new Checked(null, null, problems, null);
        }
        if (from.getYear() != to.getYear()) {
            problems.add(new Problem(HttpStatus.BAD_REQUEST, "SPANS_YEARS",
                    "Please apply separately for each calendar year"));
            return new Checked(type, null, problems, null);
        }
        BigDecimal days = BigDecimal.valueOf(rules.workingDays(from, to).size()).setScale(1);
        if (days.signum() == 0) {
            problems.add(new Problem(HttpStatus.BAD_REQUEST, "NO_WORKING_DAYS",
                    "That range has no working days (weekends and holidays only)"));
        }
        if (rules.overlapsOwnLeave(user, from, to)) {
            problems.add(new Problem(HttpStatus.CONFLICT, "OVERLAPS_OWN_LEAVE",
                    "You already have leave covering some of those dates"));
        }
        BigDecimal available = null;
        if (RuleEngine.isTracked(type)) {
            available = balances.available(user, type, from.getYear());
            if (days.compareTo(available) > 0) {
                problems.add(new Problem(HttpStatus.UNPROCESSABLE_ENTITY, "INSUFFICIENT_BALANCE",
                        "Requested " + days.stripTrailingZeros().toPlainString() + " day(s) but only "
                                + available.stripTrailingZeros().toPlainString() + " available"));
            }
        }
        return new Checked(type, days, problems, available);
    }

    // ------------------------------------------------------------------ decisions

    public LeaveRequestDto approve(Long userId, Long id, String comment) {
        User actor = user(userId);
        LeaveRequest r = request(id);
        WorkflowService.Action action = r.getStatus() == LeaveStatus.PENDING_MANAGER
                ? WorkflowService.Action.MANAGER_APPROVE : WorkflowService.Action.HR_APPROVE;
        return mapper.toDto(workflow.transition(r, action, actor, comment), actor);
    }

    public LeaveRequestDto reject(Long userId, Long id, String comment) {
        User actor = user(userId);
        return mapper.toDto(workflow.transition(request(id), WorkflowService.Action.REJECT, actor, comment), actor);
    }

    public LeaveRequestDto cancel(Long userId, Long id, String comment) {
        User actor = user(userId);
        LeaveRequest r = request(id);
        assertCanView(actor, r);
        String c = comment == null || comment.isBlank() ? "Cancelled by " + actor.getName() : comment;
        return mapper.toDto(workflow.transition(r, WorkflowService.Action.CANCEL, actor, c), actor);
    }

    // ------------------------------------------------------------------ reads

    @Transactional(readOnly = true)
    public List<LeaveRequestDto> mine(Long userId) {
        User u = user(userId);
        return requests.findByEmployeeOrderByCreatedAtDesc(u).stream().map(r -> mapper.toDto(r, u)).toList();
    }

    @Transactional(readOnly = true)
    public LeaveRequestDto get(Long userId, Long id) {
        User u = user(userId);
        LeaveRequest r = request(id);
        assertCanView(u, r);
        return mapper.toDto(r, u);
    }

    @Transactional(readOnly = true)
    public List<TimelineEventDto> timeline(Long userId, Long id) {
        User u = user(userId);
        LeaveRequest r = request(id);
        assertCanView(u, r);
        return auditEvents.findByRequestOrderByAtAscIdAsc(r).stream().map(mapper::toDto).toList();
    }

    /** Filter, sort and page requests. Managers see their reports' requests plus any step assigned to them. */
    @Transactional(readOnly = true)
    public PageResponse<LeaveRequestDto> list(Long userId, boolean hrScope, LeaveStatus status, String type,
                                              LocalDate from, LocalDate to, String q, String sort, int page, int size) {
        User me = user(userId);
        Stream<LeaveRequest> stream = requests.findAll().stream();
        if (!hrScope) {
            stream = stream.filter(r -> inManagerScope(me, r));
        }
        if (status != null) {
            stream = stream.filter(r -> r.getStatus() == status);
        }
        if (type != null && !type.isBlank()) {
            stream = stream.filter(r -> r.getLeaveType().getCode().equalsIgnoreCase(type));
        }
        if (from != null) {
            stream = stream.filter(r -> !r.getToDate().isBefore(from));
        }
        if (to != null) {
            stream = stream.filter(r -> !r.getFromDate().isAfter(to));
        }
        if (q != null && !q.isBlank()) {
            String needle = q.toLowerCase();
            stream = stream.filter(r -> r.getEmployee().getName().toLowerCase().contains(needle)
                    || (r.getReason() != null && r.getReason().toLowerCase().contains(needle)));
        }
        List<LeaveRequest> all = stream.sorted(comparator(sort)).toList();
        int safeSize = Math.max(1, Math.min(size, 100));
        int safePage = Math.max(0, page);
        int fromIdx = Math.min(safePage * safeSize, all.size());
        List<LeaveRequestDto> content = all.subList(fromIdx, Math.min(fromIdx + safeSize, all.size())).stream()
                .map(r -> mapper.toDto(r, me)).toList();
        return new PageResponse<>(content, safePage, safeSize, all.size(), (int) Math.ceil(all.size() / (double) safeSize));
    }

    private boolean inManagerScope(User me, LeaveRequest r) {
        User boss = r.getEmployee().getManager();
        if (boss != null && boss.getId().equals(me.getId())) {
            return true;
        }
        return steps.findByRequestOrderByIdAsc(r).stream().anyMatch(s ->
                (s.getAssignee() != null && s.getAssignee().getId().equals(me.getId()))
                        || (s.getDelegatedFrom() != null && s.getDelegatedFrom().getId().equals(me.getId())));
    }

    private static Comparator<LeaveRequest> comparator(String sort) {
        String[] parts = (sort == null || sort.isBlank() ? "createdAt,desc" : sort).split(",");
        Comparator<LeaveRequest> c = switch (parts[0]) {
            case "fromDate" -> Comparator.comparing(LeaveRequest::getFromDate);
            case "days" -> Comparator.comparing(LeaveRequest::getDays);
            case "status" -> Comparator.comparing(r -> r.getStatus().name());
            case "employee" -> Comparator.comparing(r -> r.getEmployee().getName());
            default -> Comparator.comparing(LeaveRequest::getCreatedAt, Comparator.nullsFirst(Comparator.naturalOrder()));
        };
        c = c.thenComparing(LeaveRequest::getId);
        return parts.length > 1 && parts[1].equalsIgnoreCase("desc") ? c.reversed() : c;
    }

    // ------------------------------------------------------------------ access

    private void assertCanView(User viewer, LeaveRequest r) {
        boolean ok = viewer.getId().equals(r.getEmployee().getId()) || viewer.getRole() == Role.HR
                || (viewer.getRole() == Role.MANAGER && inManagerScope(viewer, r));
        if (!ok) {
            throw ApiException.forbidden("You cannot view this request");
        }
    }

    private LeaveRequest request(Long id) {
        return requests.findById(id).orElseThrow(() -> ApiException.notFound("Leave request " + id + " not found"));
    }

    private User user(Long id) {
        return users.findById(id).orElseThrow(() -> ApiException.notFound("User not found"));
    }
}
