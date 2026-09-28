package com.hackathon.leave.service;

import com.hackathon.leave.config.LeaveProperties;
import com.hackathon.leave.dto.ForecastDto;
import com.hackathon.leave.exception.ApiException;
import com.hackathon.leave.model.LeaveRequest;
import com.hackathon.leave.model.LeaveStatus;
import com.hackathon.leave.model.Role;
import com.hackathon.leave.model.Team;
import com.hackathon.leave.model.User;
import com.hackathon.leave.repository.LeaveRequestRepository;
import com.hackathon.leave.repository.TeamRepository;
import com.hackathon.leave.repository.UserRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Explainable capacity forecast: known leave (approved + pending) for the next days, plus a trailing average. */
@Service
@Transactional(readOnly = true)
public class ForecastService {

    private static final int HISTORY_DAYS = 90;

    private final UserRepository users;
    private final TeamRepository teams;
    private final LeaveRequestRepository requests;
    private final RuleEngine rules;
    private final LeaveProperties props;
    private final Clock clock;

    public ForecastService(UserRepository users, TeamRepository teams, LeaveRequestRepository requests,
                           RuleEngine rules, LeaveProperties props, Clock clock) {
        this.users = users;
        this.teams = teams;
        this.requests = requests;
        this.rules = rules;
        this.props = props;
        this.clock = clock;
    }

    /** @param teamId null = everyone except HR staff */
    public ForecastDto forecast(Long teamId, int days) {
        int n = Math.max(1, Math.min(days, 90));
        List<User> members;
        String scope;
        if (teamId == null) {
            members = users.findAll().stream().filter(User::isActive).filter(u -> u.getRole() != Role.HR).toList();
            scope = "All teams";
        } else {
            Team t = teams.findById(teamId).orElseThrow(() -> ApiException.notFound("Team not found"));
            members = users.findByTeam(t).stream().filter(User::isActive).toList();
            scope = t.getName();
        }
        LocalDate today = LocalDate.now(clock);
        LocalDate end = today.plusDays(n - 1L);
        List<LeaveRequest> ahead = requests.findOverlapping(members, RuleEngine.ACTIVE_STATUSES, today, end);

        List<ForecastDto.Day> out = new ArrayList<>();
        Set<LocalDate> working = new HashSet<>(rules.workingDays(today, end));
        for (LocalDate d = today; !d.isAfter(end); d = d.plusDays(1)) {
            Set<Long> approved = new HashSet<>();
            Set<Long> pending = new HashSet<>();
            for (LeaveRequest r : ahead) {
                if (!d.isBefore(r.getFromDate()) && !d.isAfter(r.getToDate())) {
                    (r.getStatus() == LeaveStatus.APPROVED ? approved : pending).add(r.getEmployee().getId());
                }
            }
            boolean isWorking = working.contains(d);
            int a = isWorking ? approved.size() : 0;
            int p = isWorking ? (int) pending.stream().filter(id -> !approved.contains(id)).count() : 0;
            double pct = members.isEmpty() ? 0 : Math.round((a + p) * 1000.0 / members.size()) / 10.0;
            out.add(new ForecastDto.Day(d, isWorking, a, p, pct));
        }
        return new ForecastDto(scope, members.size(), props.conflictThreshold() * 100, history(members, today), out);
    }

    /** Mean people away per working day over the previous 90 days (approved leave only). */
    private double history(List<User> members, LocalDate today) {
        LocalDate from = today.minusDays(HISTORY_DAYS);
        LocalDate to = today.minusDays(1);
        List<LocalDate> workingDays = rules.workingDays(from, to);
        if (workingDays.isEmpty() || members.isEmpty()) {
            return 0;
        }
        List<LeaveRequest> past = requests.findOverlapping(members, EnumSet.of(LeaveStatus.APPROVED), from, to);
        long personDays = 0;
        for (LocalDate d : workingDays) {
            Set<Long> away = new HashSet<>();
            for (LeaveRequest r : past) {
                if (!d.isBefore(r.getFromDate()) && !d.isAfter(r.getToDate())) {
                    away.add(r.getEmployee().getId());
                }
            }
            personDays += away.size();
        }
        return Math.round(personDays * 100.0 / workingDays.size()) / 100.0;
    }
}
