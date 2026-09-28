package com.hackathon.leave.repository;

import com.hackathon.leave.model.LeaveType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaveTypeRepository extends JpaRepository<LeaveType, Long> {
    Optional<LeaveType> findByCode(String code);
}
