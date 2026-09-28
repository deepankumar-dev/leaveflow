package com.hackathon.leave.service;

import org.springframework.context.annotation.Profile;
import static com.hackathon.leave.model.LeaveStatus.*;

import com.hackathon.leave.config.ClockConfig;
import com.hackathon.leave.config.LeaveProperties;
import com.hackathon.leave.dto.ResetSeedResponse;
import com.hackathon.leave.model.*;
import com.hackathon.leave.repository.*;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads the fixed demo data described in docs/SEED_SCENARIOS.md. Every date is hard-coded (never relative to today);
 * the reference "now" of the seed is 2026-10-12 10:00 IST. Rows are written directly (not through the workflow) so
 * timestamps, steps and audit events match the document exactly.
 */
@Profile("demo")
@Slf4j
@Service
public class SeedService {

    private static final String PASSWORD = "Password@123";
    private static final Instant READ_CUTOFF = at(10, 8, 0, 0);

    private final UserRepository users;
    private final TeamRepository teams;
    private final LeaveTypeRepository types;
    private final HolidayRepository holidays;
    private final LeaveRequestRepository requests;
    private final ApprovalStepRepository steps;
    private final AuditEventRepository audit;
    private final DelegationRepository delegations;
    private final LeaveBalanceRepository balanceRows;
    private final NotificationRepository notifications;
    private final EmailOutboxRepository outbox;
    private final BalanceService balances;
    private final RuleEngine rules;
    private final PasswordEncoder encoder;
    private final LeaveProperties props;
    private final EntityManager em;

    public SeedService(UserRepository users, TeamRepository teams, LeaveTypeRepository types,
                       HolidayRepository holidays, LeaveRequestRepository requests, ApprovalStepRepository steps,
                       AuditEventRepository audit, DelegationRepository delegations,
                       LeaveBalanceRepository balanceRows, NotificationRepository notifications,
                       EmailOutboxRepository outbox, BalanceService balances, RuleEngine rules,
                       PasswordEncoder encoder, LeaveProperties props, EntityManager em) {
        this.users = users;
        this.teams = teams;
        this.types = types;
        this.holidays = holidays;
        this.requests = requests;
        this.steps = steps;
        this.audit = audit;
        this.delegations = delegations;
        this.balanceRows = balanceRows;
        this.notifications = notifications;
        this.outbox = outbox;
        this.balances = balances;
        this.rules = rules;
        this.encoder = encoder;
        this.props = props;
        this.em = em;
    }

    // ------------------------------------------------------------------ entry points

    @Transactional
    public boolean seedIfEmpty() {
        if (users.count() > 0) {
            return false;
        }
        reset();
        return true;
    }

    @Transactional
    public ResetSeedResponse reset() {
        wipe();
        managerAssignee.clear();
        LeaveType annual = type("ANNUAL", "Annual Leave", BigDecimal.valueOf(props.annualQuotaDefault()), true, true, true);
        LeaveType sick = type("SICK", "Sick Leave", BigDecimal.valueOf(12), false, false, true);
        LeaveType casual = type("CASUAL", "Casual Leave", BigDecimal.valueOf(8), false, false, true);
        LeaveType unpaid = type("UNPAID", "Unpaid Leave", null, false, false, false);

        holiday(LocalDate.of(2026, 10, 2), "Gandhi Jayanti");
        holiday(LocalDate.of(2026, 10, 20), "Dussehra");
        holiday(LocalDate.of(2026, 11, 9), "Diwali (Govardhan Puja)");
        holiday(LocalDate.of(2026, 11, 24), "Guru Nanak Jayanti");
        holiday(LocalDate.of(2026, 12, 25), "Christmas Day");

        Team eng = team("Engineering");
        Team prod = team("Product");
        Team hrTeam = team("HR");
        String hash = encoder.encode(PASSWORD);

        User meena = user("Meena Iyer", "meena", Role.HR, hrTeam, null, LocalDate.of(2020, 1, 6), hash);
        User priya = user("Priya Sharma", "priya", Role.MANAGER, eng, null, LocalDate.of(2020, 8, 3), hash);
        User arjun = user("Arjun Mehta", "arjun", Role.MANAGER, prod, null, LocalDate.of(2020, 5, 4), hash);
        User asha = user("Asha Rao", "asha", Role.EMPLOYEE, eng, priya, LocalDate.of(2022, 6, 1), hash);
        User ravi = user("Ravi Kumar", "ravi", Role.EMPLOYEE, eng, priya, LocalDate.of(2026, 7, 20), hash);
        User kiran = user("Kiran Patel", "kiran", Role.EMPLOYEE, eng, priya, LocalDate.of(2021, 2, 15), hash);
        User divya = user("Divya Nair", "divya", Role.EMPLOYEE, eng, priya, LocalDate.of(2023, 1, 9), hash);
        User sam = user("Sam Thomas", "sam", Role.EMPLOYEE, eng, priya, LocalDate.of(2022, 11, 21), hash);
        User tarun = user("Tarun Iyer", "tarun", Role.EMPLOYEE, eng, priya, LocalDate.of(2022, 2, 7), hash);
        User neha = user("Neha Kapoor", "neha", Role.EMPLOYEE, prod, arjun, LocalDate.of(2021, 9, 1), hash);
        User rohan = user("Rohan Das", "rohan", Role.EMPLOYEE, prod, arjun, LocalDate.of(2022, 3, 14), hash);
        User isha = user("Isha Gupta", "isha", Role.EMPLOYEE, prod, arjun, LocalDate.of(2023, 4, 3), hash);
        User karan = user("Karan Joshi", "karan", Role.EMPLOYEE, prod, arjun, LocalDate.of(2024, 1, 15), hash);
        User meera = user("Meera Pillai", "meera", Role.EMPLOYEE, prod, arjun, LocalDate.of(2023, 8, 21), hash);
        User vikas = user("Vikas Singh", "vikas", Role.EMPLOYEE, prod, arjun, LocalDate.of(2022, 9, 12), hash);

        Delegation d = new Delegation();
        d.setDelegator(arjun);
        d.setDelegate(priya);
        d.setFromDate(LocalDate.of(2026, 10, 8));
        d.setToDate(LocalDate.of(2026, 10, 23));
        delegations.save(d);

        // ---- balances: every user gets a row per tracked type, then baseline usage, then the requests' effect
        List<User> all = users.findAll();
        for (User u : all) {
            for (LeaveType t : List.of(annual, sick, casual)) {
                balances.getOrCreate(u, t, 2026);
            }
        }
        baseline(annual, 2026, Map.of(meena, 2, priya, 6, arjun, 2, asha, 5, kiran, 4, divya, 3, sam, 6, tarun, 8,
                neha, 5, rohan, 7));
        baseline(annual, 2026, Map.of(isha, 4, karan, 20, meera, 3, vikas, 9));

        // ---- requests R01..R18 (days are checked against SEED_SCENARIOS.md)
        Instant far = ZonedDateTime.of(2099, 1, 1, 0, 0, 0, 0, ClockConfig.ZONE).toInstant();

        LeaveRequest r01 = req(asha, annual, "2026-10-26", "2026-10-28", 3, APPROVED, "Family trip", null);
        step(r01, ApprovalStage.MANAGER, priya, Decision.APPROVED, at(10, 1, 16, 0), null, null, null);
        step(r01, ApprovalStage.HR, meena, Decision.APPROVED, at(10, 5, 11, 0), null, null, null);
        ev(r01, at(10, 1, 9, 0), "SUBMITTED", asha, null, PENDING_MANAGER, null);
        ev(r01, at(10, 1, 16, 0), "MANAGER_APPROVED", priya, PENDING_MANAGER, PENDING_HR, null);
        ev(r01, at(10, 5, 11, 0), "HR_APPROVED", meena, PENDING_HR, APPROVED, null);

        LeaveRequest r02 = req(kiran, annual, "2026-11-02", "2026-11-04", 3, APPROVED, "Wedding", null);
        step(r02, ApprovalStage.MANAGER, priya, Decision.APPROVED, at(10, 1, 15, 0), null, null, null);
        step(r02, ApprovalStage.HR, meena, Decision.APPROVED, at(10, 5, 11, 30), null, null, null);
        ev(r02, at(10, 1, 10, 0), "SUBMITTED", kiran, null, PENDING_MANAGER, null);
        ev(r02, at(10, 1, 15, 0), "MANAGER_APPROVED", priya, PENDING_MANAGER, PENDING_HR, null);
        ev(r02, at(10, 5, 11, 30), "HR_APPROVED", meena, PENDING_HR, APPROVED, null);

        LeaveRequest r03 = req(divya, annual, "2026-11-03", "2026-11-06", 4, APPROVED, "Short break", null);
        step(r03, ApprovalStage.MANAGER, priya, Decision.APPROVED, at(10, 2, 12, 0), null, null, null);
        step(r03, ApprovalStage.HR, meena, Decision.APPROVED, at(10, 5, 12, 0), null, null, null);
        ev(r03, at(10, 2, 10, 0), "SUBMITTED", divya, null, PENDING_MANAGER, null);
        ev(r03, at(10, 2, 12, 0), "MANAGER_APPROVED", priya, PENDING_MANAGER, PENDING_HR, null);
        ev(r03, at(10, 5, 12, 0), "HR_APPROVED", meena, PENDING_HR, APPROVED, null);

        String r04Flag = "3 of 7 team members (43%) away on 2026-11-04: Kiran Patel, Divya Nair";
        LeaveRequest r04 = req(sam, annual, "2026-11-04", "2026-11-05", 2, PENDING_MANAGER, "Personal errand", r04Flag);
        step(r04, ApprovalStage.MANAGER, priya, Decision.PENDING, null, null, null, far);
        ev(r04, at(10, 9, 10, 0), "SUBMITTED", sam, null, PENDING_MANAGER, null);
        ev(r04, at(10, 9, 10, 0), "FLAGGED", null, null, PENDING_MANAGER, r04Flag);

        LeaveRequest r05 = req(ravi, annual, "2026-11-16", "2026-11-20", 5, PENDING_MANAGER, "Visiting family", null);
        step(r05, ApprovalStage.MANAGER, priya, Decision.PENDING, null, null, null, far);
        ev(r05, at(10, 10, 9, 30), "SUBMITTED", ravi, null, PENDING_MANAGER, null);

        LeaveRequest r06 = req(neha, annual, "2026-10-14", "2026-10-16", 3, PENDING_HR, "Rest", null);
        step(r06, ApprovalStage.MANAGER, arjun, Decision.APPROVED, at(10, 8, 9, 0), null, null, null);
        step(r06, ApprovalStage.HR, meena, Decision.PENDING, null, null, null, null);
        ev(r06, at(10, 7, 14, 0), "SUBMITTED", neha, null, PENDING_MANAGER, null);
        ev(r06, at(10, 8, 9, 0), "MANAGER_APPROVED", arjun, PENDING_MANAGER, PENDING_HR, null);

        LeaveRequest r07 = req(rohan, annual, "2026-10-21", "2026-10-23", 3, ESCALATED, "Home shifting", null);
        String timeout = "Manager step timed out after 1 minute";
        step(r07, ApprovalStage.MANAGER, arjun, Decision.ESCALATED, at(10, 6, 9, 1), timeout, null, null);
        step(r07, ApprovalStage.HR, meena, Decision.PENDING, null, null, null, null);
        ev(r07, at(10, 6, 9, 0), "SUBMITTED", rohan, null, PENDING_MANAGER, null);
        ev(r07, at(10, 6, 9, 1), "ESCALATED", null, PENDING_MANAGER, ESCALATED, timeout);

        String r08Flag = "3 of 7 team members (43%) away on 2026-10-21: Arjun Mehta, Rohan Das";
        LeaveRequest r08 = req(isha, annual, "2026-10-19", "2026-10-21", 2, PENDING_MANAGER, "Festival", r08Flag);
        step(r08, ApprovalStage.MANAGER, priya, Decision.PENDING, null, null, arjun, far);
        ev(r08, at(10, 9, 11, 0), "SUBMITTED", isha, null, PENDING_MANAGER, null);
        ev(r08, at(10, 9, 11, 0), "DELEGATED", null, null, PENDING_MANAGER,
                "Routed to Priya Sharma (delegated by Arjun Mehta)");
        ev(r08, at(10, 9, 11, 0), "FLAGGED", null, null, PENDING_MANAGER, r08Flag);

        LeaveRequest r09 = req(arjun, annual, "2026-10-19", "2026-10-23", 4, APPROVED, "Vacation", null);
        step(r09, ApprovalStage.HR, meena, Decision.APPROVED, at(10, 3, 10, 0), null, null, null);
        ev(r09, at(10, 1, 11, 0), "SUBMITTED", arjun, null, PENDING_HR, null);
        ev(r09, at(10, 3, 10, 0), "HR_APPROVED", meena, PENDING_HR, APPROVED, null);

        LeaveRequest r10 = req(meera, sick, "2026-10-08", "2026-10-09", 2, APPROVED, "Fever", null);
        step(r10, ApprovalStage.MANAGER, arjun, Decision.APPROVED, at(10, 8, 8, 45), "Get well soon", null, null);
        ev(r10, at(10, 8, 8, 30), "SUBMITTED", meera, null, PENDING_MANAGER, null);
        ev(r10, at(10, 8, 8, 45), "MANAGER_APPROVED", arjun, PENDING_MANAGER, APPROVED, "Get well soon");

        String r11Comment = "Release deadline, please pick another day";
        LeaveRequest r11 = req(vikas, casual, "2026-10-15", "2026-10-15", 1, REJECTED, "Errand", null);
        step(r11, ApprovalStage.MANAGER, arjun, Decision.REJECTED, at(10, 6, 10, 0), r11Comment, null, null);
        ev(r11, at(10, 5, 15, 0), "SUBMITTED", vikas, null, PENDING_MANAGER, null);
        ev(r11, at(10, 6, 10, 0), "REJECTED", arjun, PENDING_MANAGER, REJECTED, r11Comment);

        LeaveRequest r12 = req(tarun, annual, "2026-11-10", "2026-11-13", 4, CANCELLED, "Trip", null);
        step(r12, ApprovalStage.MANAGER, priya, Decision.APPROVED, at(10, 3, 9, 0), null, null, null);
        step(r12, ApprovalStage.HR, meena, Decision.APPROVED, at(10, 5, 10, 0), null, null, null);
        ev(r12, at(10, 2, 15, 0), "SUBMITTED", tarun, null, PENDING_MANAGER, null);
        ev(r12, at(10, 3, 9, 0), "MANAGER_APPROVED", priya, PENDING_MANAGER, PENDING_HR, null);
        ev(r12, at(10, 5, 10, 0), "HR_APPROVED", meena, PENDING_HR, APPROVED, null);
        ev(r12, at(10, 8, 17, 0), "CANCELLED", tarun, APPROVED, CANCELLED, "Plans changed");

        LeaveRequest r13 = req(karan, annual, "2026-11-23", "2026-11-27", 4, PENDING_MANAGER, "Family function", null);
        step(r13, ApprovalStage.MANAGER, priya, Decision.PENDING, null, null, arjun, far);
        ev(r13, at(10, 11, 12, 0), "SUBMITTED", karan, null, PENDING_MANAGER, null);
        ev(r13, at(10, 11, 12, 0), "DELEGATED", null, null, PENDING_MANAGER,
                "Routed to Priya Sharma (delegated by Arjun Mehta)");

        LeaveRequest r14 = req(priya, annual, "2026-11-30", "2026-12-02", 3, PENDING_HR, "Personal", null);
        step(r14, ApprovalStage.HR, meena, Decision.PENDING, null, null, null, null);
        ev(r14, at(10, 8, 10, 0), "SUBMITTED", priya, null, PENDING_HR, null);

        LeaveRequest r15 = req(asha, casual, "2026-10-30", "2026-10-30", 1, APPROVED, "Errand", null);
        step(r15, ApprovalStage.MANAGER, priya, Decision.APPROVED, at(10, 8, 13, 0), null, null, null);
        ev(r15, at(10, 8, 11, 0), "SUBMITTED", asha, null, PENDING_MANAGER, null);
        ev(r15, at(10, 8, 13, 0), "MANAGER_APPROVED", priya, PENDING_MANAGER, APPROVED, null);

        LeaveRequest r16 = req(ravi, casual, "2026-10-09", "2026-10-09", 1, APPROVED, "Errand", null);
        step(r16, ApprovalStage.MANAGER, priya, Decision.APPROVED, at(10, 8, 14, 0), null, null, null);
        ev(r16, at(10, 8, 12, 0), "SUBMITTED", ravi, null, PENDING_MANAGER, null);
        ev(r16, at(10, 8, 14, 0), "MANAGER_APPROVED", priya, PENDING_MANAGER, APPROVED, null);

        LeaveRequest r17 = req(neha, unpaid, "2026-12-28", "2026-12-31", 4, PENDING_MANAGER, "Extended break", null);
        step(r17, ApprovalStage.MANAGER, priya, Decision.PENDING, null, null, arjun, far);
        ev(r17, at(10, 11, 16, 0), "SUBMITTED", neha, null, PENDING_MANAGER, null);
        ev(r17, at(10, 11, 16, 0), "DELEGATED", null, null, PENDING_MANAGER,
                "Routed to Priya Sharma (delegated by Arjun Mehta)");

        String r18Flag = "3 of 7 team members (43%) away on 2026-11-03: Kiran Patel, Divya Nair";
        LeaveRequest r18 = req(asha, annual, "2026-11-02", "2026-11-03", 2, PENDING_MANAGER, "Long weekend", r18Flag);
        step(r18, ApprovalStage.MANAGER, priya, Decision.PENDING, null, null, null, far);
        ev(r18, at(10, 11, 9, 0), "SUBMITTED", asha, null, PENDING_MANAGER, null);
        ev(r18, at(10, 11, 9, 0), "FLAGGED", null, null, PENDING_MANAGER, r18Flag);

        // ---- request effect on balances (approved -> used, pending statuses -> pending; the rest nets to zero)
        for (LeaveRequest r : requests.findAll()) {
            if (r.getStatus() == APPROVED) {
                balances.reserve(r.getEmployee(), r.getLeaveType(), 2026, r.getDays());
                balances.commit(r.getEmployee(), r.getLeaveType(), 2026, r.getDays());
            } else if (RuleEngine.ACTIVE_STATUSES.contains(r.getStatus())) {
                balances.reserve(r.getEmployee(), r.getLeaveType(), 2026, r.getDays());
            }
        }

        log.info("Seeded {} users and {} requests", users.count(), requests.count());
        return new ResetSeedResponse("Seed data reloaded", (int) users.count(), (int) requests.count());
    }

    // ------------------------------------------------------------------ helpers

    private void wipe() {
        notifications.deleteAllInBatch();
        outbox.deleteAllInBatch();
        audit.deleteAllInBatch();
        steps.deleteAllInBatch();
        requests.deleteAllInBatch();
        balanceRows.deleteAllInBatch();
        delegations.deleteAllInBatch();
        holidays.deleteAllInBatch();
        users.findAll().forEach(u -> u.setManager(null));
        users.flush();
        users.deleteAllInBatch();
        teams.deleteAllInBatch();
        types.deleteAllInBatch();
        em.clear();
    }

    private static Instant at(int month, int day, int hour, int minute) {
        return ZonedDateTime.of(2026, month, day, hour, minute, 0, 0, ClockConfig.ZONE).toInstant();
    }

    private LeaveType type(String code, String name, BigDecimal quota, boolean proRata, boolean requiresHr, boolean paid) {
        LeaveType t = new LeaveType();
        t.setCode(code);
        t.setName(name);
        t.setAnnualQuota(quota);
        t.setProRata(proRata);
        t.setRequiresHr(requiresHr);
        t.setPaid(paid);
        return types.save(t);
    }

    private void holiday(LocalDate date, String name) {
        Holiday h = new Holiday();
        h.setDate(date);
        h.setName(name);
        holidays.save(h);
    }

    private Team team(String name) {
        Team t = new Team();
        t.setName(name);
        return teams.save(t);
    }

    private User user(String name, String emailLocal, Role role, Team team, User manager, LocalDate joined,
                      String hash) {
        User u = new User();
        u.setName(name);
        u.setEmail(emailLocal + "@leave.demo");
        u.setPasswordHash(hash);
        u.setRole(role);
        u.setTeam(team);
        u.setManager(manager);
        u.setJoinDate(joined);
        return users.save(u);
    }

    private void baseline(LeaveType type, int year, Map<User, Integer> usedDays) {
        usedDays.forEach((u, days) -> {
            LeaveBalance b = balances.getOrCreate(u, type, year);
            b.setUsed(BigDecimal.valueOf(days).setScale(1));
        });
    }

    private LeaveRequest req(User employee, LeaveType type, String from, String to, int expectedDays,
                             LeaveStatus status, String reason, String flagReason) {
        LocalDate f = LocalDate.parse(from);
        LocalDate t = LocalDate.parse(to);
        int days = rules.workingDays(f, t).size();
        if (days != expectedDays) {
            throw new IllegalStateException("Seed mismatch for " + employee.getName() + " " + from + ".." + to
                    + ": expected " + expectedDays + " working days but got " + days);
        }
        LeaveRequest r = new LeaveRequest();
        r.setEmployee(employee);
        r.setLeaveType(type);
        r.setFromDate(f);
        r.setToDate(t);
        r.setDays(BigDecimal.valueOf(days).setScale(1));
        r.setReason(reason);
        r.setStatus(status);
        r.setFlagged(flagReason != null);
        r.setFlagReason(flagReason);
        return requests.save(r);
    }

    private final Map<Long, User> managerAssignee = new HashMap<>();

    private void step(LeaveRequest r, ApprovalStage stage, User assignee, Decision decision, Instant decidedAt,
                      String comment, User delegatedFrom, Instant dueAt) {
        ApprovalStep s = new ApprovalStep();
        s.setRequest(r);
        s.setStage(stage);
        s.setAssignee(assignee);
        s.setDelegatedFrom(delegatedFrom);
        s.setDecision(decision);
        s.setDecidedAt(decidedAt);
        s.setComment(comment);
        s.setDueAt(dueAt);
        steps.save(s);
        if (stage == ApprovalStage.MANAGER) {
            managerAssignee.put(r.getId(), assignee);
        }
    }

    /** Writes one audit event and the notifications the live system would send for it. */
    private void ev(LeaveRequest r, Instant at, String action, User actor, LeaveStatus from, LeaveStatus to,
                    String comment) {
        AuditEvent e = new AuditEvent();
        e.setRequest(r);
        e.setActor(actor);
        e.setAction(action);
        e.setFromStatus(from);
        e.setToStatus(to);
        e.setComment(comment);
        e.setAt(at);
        audit.save(e);
        if (r.getCreatedAt() == null) {
            r.setCreatedAt(at);
        }
        r.setLastActionAt(at);

        User owner = r.getEmployee();
        String what = r.getLeaveType().getName() + " " + r.getFromDate() + " → " + r.getToDate();
        List<User> hr = users.findByRole(Role.HR).stream().filter(u -> !u.getId().equals(owner.getId())).toList();
        switch (action) {
            case "SUBMITTED" -> {
                String flag = r.isFlagged() ? " (conflict flagged)" : "";
                String msg = "New leave request from " + owner.getName() + ": " + what + flag;
                if (to == PENDING_MANAGER) {
                    notify(managerAssignee.get(r.getId()), msg, r, at);
                } else {
                    hr.forEach(h -> notify(h, msg, r, at));
                }
            }
            case "MANAGER_APPROVED" -> {
                if (to == PENDING_HR) {
                    notify(owner, "Your manager approved your leave (" + what + "); waiting for HR", r, at);
                    hr.forEach(h -> notify(h, owner.getName() + "'s leave (" + what + ") is ready for HR approval",
                            r, at));
                } else {
                    notify(owner, "Your leave (" + what + ") is approved", r, at);
                }
            }
            case "HR_APPROVED" -> notify(owner, "Your leave (" + what + ") is approved", r, at);
            case "REJECTED" -> notify(owner, "Your leave (" + what + ") was rejected: " + comment, r, at);
            case "ESCALATED" -> {
                notify(owner, "Your leave (" + what + ") was escalated to HR: the manager did not respond", r, at);
                hr.forEach(h -> notify(h, "Escalated: " + owner.getName() + "'s leave (" + what
                        + ") needs an HR decision", r, at));
            }
            default -> { }
        }
    }

    private void notify(User recipient, String message, LeaveRequest r, Instant at) {
        if (recipient == null) {
            return;
        }
        Notification n = new Notification();
        n.setUser(recipient);
        n.setMessage(message);
        n.setCreatedAt(at);
        n.setRelatedRequest(r);
        n.setRead(at.isBefore(READ_CUTOFF));
        notifications.save(n);
        EmailOutbox mail = new EmailOutbox();
        mail.setToEmail(recipient.getEmail());
        mail.setSubject("Leave update");
        mail.setBody(message);
        mail.setCreatedAt(at);
        mail.setSent(false);
        outbox.save(mail);
    }
}
