package com.hackathon.leave.service;

import com.hackathon.leave.config.LeaveProperties;
import com.hackathon.leave.model.LeaveType;
import com.hackathon.leave.model.Role;
import com.hackathon.leave.model.Team;
import com.hackathon.leave.model.User;
import com.hackathon.leave.repository.LeaveTypeRepository;
import com.hackathon.leave.repository.TeamRepository;
import com.hackathon.leave.repository.UserRepository;
import com.hackathon.leave.security.PasswordPolicy;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * First-run setup for a real deployment: makes sure the leave types exist and, on an empty database, creates the
 * first HR administrator (from ADMIN_EMAIL / ADMIN_PASSWORD) who must change the password at first sign-in.
 * Everything else (teams, people, holidays) is then managed in the app.
 */
@Slf4j
@Component
@Profile("!demo")
public class BootstrapRunner implements ApplicationRunner {

    private final LeaveTypeRepository types;
    private final UserRepository users;
    private final TeamRepository teams;
    private final PasswordEncoder encoder;
    private final LeaveProperties props;
    private final Clock clock;

    public BootstrapRunner(LeaveTypeRepository types, UserRepository users, TeamRepository teams,
                           PasswordEncoder encoder, LeaveProperties props, Clock clock) {
        this.types = types;
        this.users = users;
        this.teams = teams;
        this.encoder = encoder;
        this.props = props;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        ensureLeaveTypes();
        if (users.count() == 0) {
            createFirstAdmin();
        }
    }

    private void ensureLeaveTypes() {
        ensure("ANNUAL", "Annual Leave", BigDecimal.valueOf(props.annualQuotaDefault()), true, true, true);
        ensure("SICK", "Sick Leave", BigDecimal.valueOf(12), false, false, true);
        ensure("CASUAL", "Casual Leave", BigDecimal.valueOf(8), false, false, true);
        ensure("UNPAID", "Unpaid Leave", null, false, false, false);
    }

    private void ensure(String code, String name, BigDecimal quota, boolean proRata, boolean requiresHr, boolean paid) {
        if (types.findByCode(code).isPresent()) {
            return;
        }
        LeaveType t = new LeaveType();
        t.setCode(code);
        t.setName(name);
        t.setAnnualQuota(quota);
        t.setProRata(proRata);
        t.setRequiresHr(requiresHr);
        t.setPaid(paid);
        types.save(t);
        log.info("Created leave type {}", code);
    }

    private void createFirstAdmin() {
        LeaveProperties.Bootstrap b = props.bootstrap();
        if (b == null || b.adminEmail() == null || b.adminEmail().isBlank()) {
            log.warn("The database has no users and ADMIN_EMAIL is not set: nobody can sign in. "
                    + "Set ADMIN_EMAIL (and optionally ADMIN_PASSWORD) and restart.");
            return;
        }
        String password = b.adminPassword();
        boolean generated = password == null || password.isBlank();
        if (generated) {
            password = randomPassword();
        } else {
            PasswordPolicy.validate(password);
        }
        Team hrTeam = teams.findAll().stream().filter(t -> t.getName().equals("Administration")).findFirst()
                .orElseGet(() -> {
                    Team t = new Team();
                    t.setName("Administration");
                    return teams.save(t);
                });
        User u = new User();
        u.setName(b.adminName() == null || b.adminName().isBlank() ? "Administrator" : b.adminName());
        u.setEmail(b.adminEmail().trim().toLowerCase());
        u.setPasswordHash(encoder.encode(password));
        u.setRole(Role.HR);
        u.setTeam(hrTeam);
        u.setJoinDate(LocalDate.now(clock));
        u.setMustChangePassword(true);
        users.save(u);
        if (generated) {
            log.warn("Created the first HR administrator {} with the ONE-TIME password: {}  (you must change it at "
                    + "first sign-in)", u.getEmail(), password);
        } else {
            log.info("Created the first HR administrator {} (must change the password at first sign-in)", u.getEmail());
        }
    }

    private static String randomPassword() {
        String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
        SecureRandom r = new SecureRandom();
        StringBuilder sb = new StringBuilder("A1");
        for (int i = 0; i < 14; i++) {
            sb.append(alphabet.charAt(r.nextInt(alphabet.length())));
        }
        return sb.toString();
    }
}
