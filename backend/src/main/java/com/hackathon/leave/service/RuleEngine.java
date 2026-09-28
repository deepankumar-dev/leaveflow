package com.hackathon.leave.service;

import com.hackathon.leave.config.LeaveProperties;
import com.hackathon.leave.model.*;
import com.hackathon.leave.repository.HolidayRepository;
import com.hackathon.leave.repository.LeaveRequestRepository;
import com.hackathon.leave.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import org.springframework.stereotype.Component;

/**
 * The frozen business rules (docs/RULES.md): working days, holidays, pro-rata, balance and team-conflict checks.
 * It reads data but never changes it.
 */
@Component
public class RuleEngine {

    /** Statuses that count as "away" for conflicts and for overlap-with-own-leave. */
    public static final Set<LeaveStatus> ACTIVE_STATUSES =
            EnumSet.of(LeaveStatus.PENDING_MANAGER, LeaveStatus.ESCALATED, LeaveStatus.PENDING_HR, LeaveStatus.APPROVED);

    public record ConflictResult(boolean flagged, String reason, List<String> overlappingTeammates, int teamSize) {
        static ConflictResult none(int teamSize) {
            return new ConflictResult(false, null, List.of(), teamSize);
        }
    }

    private final HolidayRepository holidays;
    private final LeaveRequestRepository requests;
    private final UserRepository users;
    private final LeaveProperties props;

    public RuleEngine(HolidayRepository holidays, LeaveRequestRepository requests, UserRepository users,
                      LeaveProperties props) {
        this.holidays = holidays;
        this.requests = requests;
        this.users = users;
        this.props = props;
    }

    // ------------------------------------------------------------------ working days

    public static boolean isWeekday(LocalDate d) {
        DayOfWeek w = d.getDayOfWeek();
        return w != DayOfWeek.SATURDAY && w != DayOfWeek.SUNDAY;
    }

    /** Monday-Friday minus holidays, both ends inclusive. */
    public List<LocalDate> workingDays(LocalDate from, LocalDate to) {
        Set<LocalDate> hol = new HashSet<>();
        holidays.findByDateBetweenOrderByDateAsc(from, to).forEach(h -> hol.add(h.getDate()));
        return workingDays(from, to, hol);
    }

    /** Pure version, used directly by unit tests. */
    public static List<LocalDate> workingDays(LocalDate from, LocalDate to, Set<LocalDate> holidaySet) {
        List<LocalDate> days = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            if (isWeekday(d) && !holidaySet.contains(d)) {
                days.add(d);
            }
        }
        return days;
    }

    // ------------------------------------------------------------------ pro-rata

    /** Joined before the year: 12. Join month counts fully: joined in month m → 12 − m + 1. After the year: 0. */
    public static int monthsRemaining(LocalDate joinDate, int year) {
        if (joinDate.getYear() < year) {
            return 12;
        }
        if (joinDate.getYear() > year) {
            return 0;
        }
        return 12 - joinDate.getMonthValue() + 1;
    }

    /** quota × monthsRemaining / 12, rounded to the nearest 0.5 (halves round up). */
    public static BigDecimal proRata(BigDecimal quota, LocalDate joinDate, int year) {
        BigDecimal raw = quota.multiply(BigDecimal.valueOf(monthsRemaining(joinDate, year)))
                .divide(BigDecimal.valueOf(12), 6, RoundingMode.HALF_UP);
        return raw.multiply(BigDecimal.valueOf(2)).setScale(0, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(2), 1, RoundingMode.UNNECESSARY);
    }

    public static boolean isTracked(LeaveType type) {
        return type.getAnnualQuota() != null && type.getAnnualQuota().signum() > 0;
    }

    public BigDecimal entitlement(User user, LeaveType type, int year) {
        if (!isTracked(type)) {
            return BigDecimal.ZERO.setScale(1);
        }
        return type.isProRata() ? proRata(type.getAnnualQuota(), user.getJoinDate(), year)
                : type.getAnnualQuota().setScale(1, RoundingMode.HALF_UP);
    }

    public String explanation(User user, LeaveType type, int year) {
        if (!isTracked(type)) {
            return "No balance: unpaid leave is not limited";
        }
        if (!type.isProRata()) {
            return "Fixed yearly quota of " + type.getAnnualQuota().stripTrailingZeros().toPlainString() + " days";
        }
        int months = monthsRemaining(user.getJoinDate(), year);
        return type.getAnnualQuota().stripTrailingZeros().toPlainString() + " × " + months + "/12 = "
                + proRata(type.getAnnualQuota(), user.getJoinDate(), year).toPlainString()
                + (months == 12 ? " (full year)" : " (joined " + user.getJoinDate() + ")");
    }

    // ------------------------------------------------------------------ own overlap

    public boolean overlapsOwnLeave(User employee, LocalDate from, LocalDate to) {
        return !requests.findOverlapping(List.of(employee), ACTIVE_STATUSES, from, to).isEmpty();
    }

    // ------------------------------------------------------------------ conflict

    /** Flags when (overlapping teammates + 1) / teamSize exceeds the threshold on any working day. Never rejects. */
    public ConflictResult conflict(User employee, LocalDate from, LocalDate to) {
        if (employee.getTeam() == null) {
            return ConflictResult.none(1);
        }
        List<User> members = users.findByTeam(employee.getTeam()).stream().filter(User::isActive).toList();
        int teamSize = members.size();
        if (teamSize == 0) {
            return ConflictResult.none(0);
        }
        List<LeaveRequest> overlapping = requests.findOverlapping(members, ACTIVE_STATUSES, from, to);
        int worst = 0;
        LocalDate worstDay = null;
        List<String> worstNames = List.of();
        for (LocalDate day : workingDays(from, to)) {
            Map<Long, String> away = new LinkedHashMap<>();
            for (LeaveRequest r : overlapping) {
                if (!r.getEmployee().getId().equals(employee.getId()) && !day.isBefore(r.getFromDate())
                        && !day.isAfter(r.getToDate())) {
                    away.put(r.getEmployee().getId(), r.getEmployee().getName());
                }
            }
            if (away.size() > worst) {
                worst = away.size();
                worstDay = day;
                worstNames = new ArrayList<>(away.values());
            }
        }
        boolean flagged = worst > 0 && (worst + 1.0) / teamSize > props.conflictThreshold();
        if (!flagged) {
            return new ConflictResult(false, null, worstNames, teamSize);
        }
        long pct = Math.round((worst + 1) * 100.0 / teamSize);
        String reason = (worst + 1) + " of " + teamSize + " team members (" + pct + "%) away on " + worstDay + ": "
                + String.join(", ", worstNames);
        return new ConflictResult(true, reason, worstNames, teamSize);
    }
}
