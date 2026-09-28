package com.hackathon.leave.service;

import com.hackathon.leave.dto.AnalyticsSummaryDto;
import com.hackathon.leave.dto.AnalyticsSummaryDto.MonthlyPoint;
import com.hackathon.leave.dto.AnalyticsSummaryDto.TeamLoad;
import com.hackathon.leave.model.AuditEvent;
import com.hackathon.leave.model.LeaveRequest;
import com.hackathon.leave.model.LeaveStatus;
import com.hackathon.leave.model.Team;
import com.hackathon.leave.repository.AuditEventRepository;
import com.hackathon.leave.repository.LeaveRequestRepository;
import com.hackathon.leave.repository.TeamRepository;
import java.time.Duration;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Organisation-wide numbers for the HR dashboard. Everything is derived from requests and the audit trail. */
@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private final LeaveRequestRepository requests;
    private final AuditEventRepository audit;
    private final TeamRepository teams;

    public AnalyticsService(LeaveRequestRepository requests, AuditEventRepository audit, TeamRepository teams) {
        this.requests = requests;
        this.audit = audit;
        this.teams = teams;
    }

    public AnalyticsSummaryDto summary() {
        List<LeaveRequest> all = requests.findAll();
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (LeaveStatus s : LeaveStatus.values()) {
            byStatus.put(s.name(), all.stream().filter(r -> r.getStatus() == s).count());
        }
        Map<String, Long> byType = all.stream().collect(
                Collectors.groupingBy(r -> r.getLeaveType().getCode(), TreeMap::new, Collectors.counting()));

        List<AuditEvent> events = audit.findAll();
        long escalated = events.stream().filter(e -> "ESCALATED".equals(e.getAction())).count();

        // Time from submission to the final decision (approved or rejected), in hours.
        Map<Long, AuditEvent> submitted = new HashMap<>();
        events.stream().filter(e -> "SUBMITTED".equals(e.getAction()))
                .forEach(e -> submitted.put(e.getRequest().getId(), e));
        double avgHours = events.stream()
                .filter(e -> e.getToStatus() == LeaveStatus.APPROVED || e.getToStatus() == LeaveStatus.REJECTED)
                .filter(e -> e.getFromStatus() != LeaveStatus.APPROVED && submitted.containsKey(e.getRequest().getId()))
                .mapToDouble(e -> Duration.between(submitted.get(e.getRequest().getId()).getAt(), e.getAt())
                        .toMinutes() / 60.0)
                .average().orElse(0);

        TreeMap<YearMonth, double[]> trend = new TreeMap<>();
        all.stream().filter(r -> r.getStatus() == LeaveStatus.APPROVED).forEach(r -> {
            double[] cell = trend.computeIfAbsent(YearMonth.from(r.getFromDate()), k -> new double[2]);
            cell[0] += r.getDays().doubleValue();
            cell[1] += 1;
        });
        List<MonthlyPoint> monthly = trend.entrySet().stream()
                .map(e -> new MonthlyPoint(e.getKey().toString(), e.getValue()[0], (long) e.getValue()[1])).toList();

        List<TeamLoad> load = new ArrayList<>();
        for (Team t : teams.findAll()) {
            List<LeaveRequest> mine = all.stream()
                    .filter(r -> r.getEmployee().getTeam() != null && r.getEmployee().getTeam().getId().equals(t.getId()))
                    .toList();
            if (mine.isEmpty()) {
                continue;
            }
            load.add(new TeamLoad(t.getName(),
                    mine.stream().filter(r -> r.getStatus() == LeaveStatus.PENDING_MANAGER
                            || r.getStatus() == LeaveStatus.ESCALATED || r.getStatus() == LeaveStatus.PENDING_HR).count(),
                    mine.stream().filter(r -> r.getStatus() == LeaveStatus.APPROVED).count(),
                    mine.stream().filter(LeaveRequest::isFlagged).count()));
        }
        return new AnalyticsSummaryDto(all.size(), byStatus, byType, all.stream().filter(LeaveRequest::isFlagged).count(),
                escalated, Math.round(avgHours * 10) / 10.0, monthly, load);
    }
}
