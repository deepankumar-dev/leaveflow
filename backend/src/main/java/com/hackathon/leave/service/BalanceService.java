package com.hackathon.leave.service;

import com.hackathon.leave.model.LeaveBalance;
import com.hackathon.leave.model.LeaveType;
import com.hackathon.leave.model.User;
import com.hackathon.leave.repository.LeaveBalanceRepository;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

/** Owns balance rows and their four movements (RULES §4). Unpaid/untracked types never touch a balance. */
@Service
public class BalanceService {

    private final LeaveBalanceRepository balances;
    private final RuleEngine rules;

    public BalanceService(LeaveBalanceRepository balances, RuleEngine rules) {
        this.balances = balances;
        this.rules = rules;
    }

    public LeaveBalance getOrCreate(User user, LeaveType type, int year) {
        return balances.findByUserAndLeaveTypeAndYear(user, type, year).orElseGet(() -> {
            LeaveBalance b = new LeaveBalance();
            b.setUser(user);
            b.setLeaveType(type);
            b.setYear(year);
            b.setEntitled(rules.entitlement(user, type, year));
            return balances.save(b);
        });
    }

    public BigDecimal available(User user, LeaveType type, int year) {
        LeaveBalance b = getOrCreate(user, type, year);
        return b.getEntitled().subtract(b.getUsed()).subtract(b.getPending());
    }

    /** Apply: pending += days. */
    public void reserve(User user, LeaveType type, int year, BigDecimal days) {
        if (RuleEngine.isTracked(type)) {
            LeaveBalance b = getOrCreate(user, type, year);
            b.setPending(b.getPending().add(days));
        }
    }

    /** Final approval: pending -= days, used += days. */
    public void commit(User user, LeaveType type, int year, BigDecimal days) {
        if (RuleEngine.isTracked(type)) {
            LeaveBalance b = getOrCreate(user, type, year);
            b.setPending(b.getPending().subtract(days).max(BigDecimal.ZERO));
            b.setUsed(b.getUsed().add(days));
        }
    }

    /** Reject / cancel while pending: pending -= days. */
    public void release(User user, LeaveType type, int year, BigDecimal days) {
        if (RuleEngine.isTracked(type)) {
            LeaveBalance b = getOrCreate(user, type, year);
            b.setPending(b.getPending().subtract(days).max(BigDecimal.ZERO));
        }
    }

    /** Cancel after approval: used -= days. */
    public void refund(User user, LeaveType type, int year, BigDecimal days) {
        if (RuleEngine.isTracked(type)) {
            LeaveBalance b = getOrCreate(user, type, year);
            b.setUsed(b.getUsed().subtract(days).max(BigDecimal.ZERO));
        }
    }
}
