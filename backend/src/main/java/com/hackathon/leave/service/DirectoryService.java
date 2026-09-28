package com.hackathon.leave.service;

import com.hackathon.leave.dto.DirectoryDto;
import com.hackathon.leave.model.Role;
import com.hackathon.leave.model.Team;
import com.hackathon.leave.model.User;
import com.hackathon.leave.repository.TeamRepository;
import com.hackathon.leave.repository.UserRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DirectoryService {

    private final TeamRepository teams;
    private final UserRepository users;

    public DirectoryService(TeamRepository teams, UserRepository users) {
        this.teams = teams;
        this.users = users;
    }

    public DirectoryDto directory() {
        List<DirectoryDto.TeamInfo> teamInfos = teams.findAll().stream().sorted(Comparator.comparing(Team::getName))
                .map(t -> new DirectoryDto.TeamInfo(t.getId(), t.getName(),
                        (int) users.findByTeam(t).stream().filter(User::isActive).count()))
                .toList();
        List<DirectoryDto.ManagerInfo> managers = users.findByRole(Role.MANAGER).stream().filter(User::isActive)
                .sorted(Comparator.comparing(User::getName))
                .map(u -> new DirectoryDto.ManagerInfo(u.getId(), u.getName(),
                        u.getTeam() == null ? null : u.getTeam().getName()))
                .toList();
        return new DirectoryDto(teamInfos, managers);
    }
}
