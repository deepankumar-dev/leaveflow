package com.hackathon.leave.service;

import com.hackathon.leave.dto.BalanceDto;
import com.hackathon.leave.exception.ApiException;
import com.hackathon.leave.model.LeaveBalance;
import com.hackathon.leave.model.LeaveType;
import com.hackathon.leave.model.User;
import com.hackathon.leave.repository.LeaveTypeRepository;
import com.hackathon.leave.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read model for "my balances", including the pro-rata explanation shown in the UI. */
@Service
@Transactional
public class BalanceQueryService {

    private final UserRepository users;
    private final LeaveTypeRepository types;
    private final BalanceService balances;
    private final RuleEngine rules;
    private final Clock clock;

    public BalanceQueryService(UserRepository users, LeaveTypeRepository types, BalanceService balances,
                               RuleEngine rules, Clock clock) {
        this.users = users;
        this.types = types;
        this.balances = balances;
        this.rules = rules;
        this.clock = clock;
    }

    public List<BalanceDto> mine(Long userId) {
        User u = users.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
        int year = LocalDate.now(clock).getYear();
        return types.findAll().stream().sorted(Comparator.comparing(LeaveType::getId)).map(t -> {
            if (!RuleEngine.isTracked(t)) {
                return new BalanceDto(t.getCode(), t.getName(), year, null, null, null, null, false,
                        rules.explanation(u, t, year));
            }
            LeaveBalance b = balances.getOrCreate(u, t, year);
            BigDecimal available = b.getEntitled().subtract(b.getUsed()).subtract(b.getPending());
            return new BalanceDto(t.getCode(), t.getName(), year, b.getEntitled(), b.getUsed(), b.getPending(),
                    available, true, rules.explanation(u, t, year));
        }).toList();
    }
}
