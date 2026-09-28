package com.hackathon.leave.service;

import com.hackathon.leave.dto.CalendarEntryDto;
import com.hackathon.leave.dto.HolidayDto;
import com.hackathon.leave.dto.TeamCalendarDto;
import com.hackathon.leave.exception.ApiException;
import com.hackathon.leave.model.LeaveRequest;
import com.hackathon.leave.model.Role;
import com.hackathon.leave.model.Team;
import com.hackathon.leave.model.User;
import com.hackathon.leave.repository.HolidayRepository;
import com.hackathon.leave.repository.LeaveRequestRepository;
import com.hackathon.leave.repository.TeamRepository;
import com.hackathon.leave.repository.UserRepository;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CalendarService {

    private final UserRepository users;
    private final TeamRepository teams;
    private final LeaveRequestRepository requests;
    private final HolidayRepository holidays;

    public CalendarService(UserRepository users, TeamRepository teams, LeaveRequestRepository requests,
                           HolidayRepository holidays) {
        this.users = users;
        this.teams = teams;
        this.requests = requests;
        this.holidays = holidays;
    }

    /** Managers always see their own team; HR may pick a team (defaults to their own). */
    public TeamCalendarDto team(Long userId, String month, Long teamId) {
        User me = users.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
        YearMonth ym;
        try {
            ym = YearMonth.parse(month);
        } catch (DateTimeParseException e) {
            throw ApiException.badRequest("INVALID_MONTH", "Month must look like 2026-11");
        }
        Team team = me.getTeam();
        if (me.getRole() == Role.HR && teamId != null) {
            team = teams.findById(teamId).orElseThrow(() -> ApiException.notFound("Team not found"));
        }
        if (team == null) {
            throw ApiException.badRequest("NO_TEAM", "You do not belong to a team");
        }
        List<User> members = users.findByTeam(team).stream().filter(User::isActive).toList();
        List<CalendarEntryDto> entries = requests
                .findOverlapping(members, RuleEngine.ACTIVE_STATUSES, ym.atDay(1), ym.atEndOfMonth()).stream()
                .sorted(Comparator.comparing(LeaveRequest::getFromDate).thenComparing(LeaveRequest::getId))
                .map(r -> new CalendarEntryDto(r.getId(), r.getEmployee().getId(), r.getEmployee().getName(),
                        r.getLeaveType().getCode(), r.getFromDate(), r.getToDate(), r.getStatus(), r.isFlagged()))
                .toList();
        List<HolidayDto> hol = holidays.findByDateBetweenOrderByDateAsc(ym.atDay(1), ym.atEndOfMonth()).stream()
                .map(HolidayService::toDto).toList();
        return new TeamCalendarDto(ym.toString(), team.getId(), team.getName(), members.size(), entries, hol);
    }
}
