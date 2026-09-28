package com.hackathon.leave.repository;

import com.hackathon.leave.model.LeaveBalance;
import com.hackathon.leave.model.LeaveType;
import com.hackathon.leave.model.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {
    Optional<LeaveBalance> findByUserAndLeaveTypeAndYear(User user, LeaveType leaveType, int year);
    List<LeaveBalance> findByUserAndYear(User user, int year);
}
