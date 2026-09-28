package com.hackathon.leave.service;

import com.hackathon.leave.dto.AdminUserRequest;
import com.hackathon.leave.dto.DirectoryDto;
import com.hackathon.leave.dto.UserDto;
import com.hackathon.leave.exception.ApiException;
import com.hackathon.leave.model.Role;
import com.hackathon.leave.model.Team;
import com.hackathon.leave.model.User;
import com.hackathon.leave.repository.TeamRepository;
import com.hackathon.leave.repository.UserRepository;
import com.hackathon.leave.security.PasswordPolicy;
import java.util.Comparator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * HR-only management of people and teams. People are never deleted (their leave history must stay intact); they are
 * deactivated. Guards keep the organisation consistent: employees always have an active manager in their own team, a
 * manager with reports cannot be removed or moved, and the last active HR user cannot be lost.
 */
@Slf4j
@Service
@Transactional
public class AdminService {

    private final UserRepository users;
    private final TeamRepository teams;
    private final PasswordEncoder encoder;
    private final LeaveMapper mapper;

    public AdminService(UserRepository users, TeamRepository teams, PasswordEncoder encoder, LeaveMapper mapper) {
        this.users = users;
        this.teams = teams;
        this.encoder = encoder;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<UserDto> list() {
        return users.findAll().stream().sorted(Comparator.comparing(User::getName)).map(mapper::toDto).toList();
    }

    public UserDto create(AdminUserRequest req, Long actorId) {
        String email = req.email().trim().toLowerCase();
        if (users.findByEmail(email).isPresent()) {
            throw ApiException.conflict("EMAIL_TAKEN", "Someone already uses " + email);
        }
        PasswordPolicy.validate(req.password());
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(req.password()));
        u.setMustChangePassword(true);
        apply(u, req);
        log.info("User {} created by {}", email, actorId);
        return mapper.toDto(users.save(u));
    }

    public UserDto update(Long id, AdminUserRequest req, Long actorId) {
        User u = users.findById(id).orElseThrow(() -> ApiException.notFound("User not found"));
        String email = req.email().trim().toLowerCase();
        users.findByEmail(email).filter(other -> !other.getId().equals(id)).ifPresent(x -> {
            throw ApiException.conflict("EMAIL_TAKEN", "Someone already uses " + email);
        });
        boolean self = id.equals(actorId);
        boolean active = req.active() == null || req.active();
        if (self && (!active || req.role() != u.getRole())) {
            throw ApiException.conflict("SELF_CHANGE", "You cannot deactivate yourself or change your own role");
        }
        boolean losesHr = u.getRole() == Role.HR && u.isActive() && (!active || req.role() != Role.HR);
        if (losesHr && users.findByRole(Role.HR).stream().filter(User::isActive).count() <= 1) {
            throw ApiException.conflict("LAST_HR", "At least one active HR user must remain");
        }
        boolean leavesManagerPost = u.getRole() == Role.MANAGER && u.isActive()
                && (!active || req.role() != Role.MANAGER || !sameTeam(u, req.teamId()));
        if (leavesManagerPost && !activeReports(u).isEmpty()) {
            throw ApiException.conflict("HAS_REPORTS", "This manager still has active reports (" + activeReports(u).size()
                    + "). Reassign them first.");
        }
        u.setEmail(email);
        apply(u, req);
        u.setActive(active);
        log.info("User {} updated by {}", email, actorId);
        return mapper.toDto(u);
    }

    public void resetPassword(Long id, String password) {
        PasswordPolicy.validate(password);
        User u = users.findById(id).orElseThrow(() -> ApiException.notFound("User not found"));
        u.setPasswordHash(encoder.encode(password));
        u.setMustChangePassword(true);
        log.info("Password reset for {}", u.getEmail());
    }

    public DirectoryDto.TeamInfo createTeam(String name) {
        String n = name.trim();
        if (teams.findAll().stream().anyMatch(t -> t.getName().equalsIgnoreCase(n))) {
            throw ApiException.conflict("TEAM_EXISTS", "A team called " + n + " already exists");
        }
        Team t = new Team();
        t.setName(n);
        t = teams.save(t);
        return new DirectoryDto.TeamInfo(t.getId(), t.getName(), 0);
    }

    // ------------------------------------------------------------------ rules

    /** Copies the fields and validates the organisation rules for the role. */
    private void apply(User u, AdminUserRequest req) {
        Team team = req.teamId() == null ? null
                : teams.findById(req.teamId()).orElseThrow(() -> ApiException.notFound("Team not found"));
        User manager = null;
        switch (req.role()) {
            case HR -> {
                if (req.managerId() != null) {
                    throw ApiException.badRequest("INVALID_MANAGER", "HR users do not have a manager");
                }
            }
            case MANAGER -> {
                if (team == null) {
                    throw ApiException.badRequest("TEAM_REQUIRED", "A manager needs a team");
                }
                if (req.managerId() != null) {
                    throw ApiException.badRequest("INVALID_MANAGER",
                            "Managers do not have a manager: their own leave goes straight to HR");
                }
            }
            case EMPLOYEE -> {
                if (team == null) {
                    throw ApiException.badRequest("TEAM_REQUIRED", "An employee needs a team");
                }
                if (req.managerId() == null) {
                    throw ApiException.badRequest("MANAGER_REQUIRED", "An employee needs a manager");
                }
                manager = users.findById(req.managerId()).orElseThrow(() -> ApiException.notFound("Manager not found"));
                if (manager.getRole() != Role.MANAGER || !manager.isActive()) {
                    throw ApiException.badRequest("INVALID_MANAGER", "The manager must be an active manager");
                }
                if (manager.getTeam() == null || !manager.getTeam().getId().equals(team.getId())) {
                    throw ApiException.badRequest("INVALID_MANAGER", "The manager must be in the same team");
                }
                if (u.getId() != null && manager.getId().equals(u.getId())) {
                    throw ApiException.badRequest("INVALID_MANAGER", "Nobody can be their own manager");
                }
            }
        }
        u.setName(req.name().trim());
        u.setRole(req.role());
        u.setTeam(team);
        u.setManager(manager);
        u.setJoinDate(req.joinDate());
    }

    private boolean sameTeam(User u, Long teamId) {
        return u.getTeam() != null && u.getTeam().getId().equals(teamId);
    }

    private List<User> activeReports(User manager) {
        return users.findAll().stream().filter(User::isActive)
                .filter(x -> x.getManager() != null && x.getManager().getId().equals(manager.getId())).toList();
    }
}
