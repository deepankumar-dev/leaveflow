package com.hackathon.leave.repository;

import com.hackathon.leave.model.AuditEvent;
import com.hackathon.leave.model.LeaveRequest;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
    List<AuditEvent> findByRequestOrderByAtAscIdAsc(LeaveRequest request);
}
