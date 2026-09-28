package com.hackathon.leave.repository;

import com.hackathon.leave.model.ApprovalStage;
import com.hackathon.leave.model.ApprovalStep;
import com.hackathon.leave.model.Decision;
import com.hackathon.leave.model.LeaveRequest;
import com.hackathon.leave.model.User;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalStepRepository extends JpaRepository<ApprovalStep, Long> {
    List<ApprovalStep> findByRequestOrderByIdAsc(LeaveRequest request);
    List<ApprovalStep> findByAssigneeAndDecision(User assignee, Decision decision);
    List<ApprovalStep> findByStageAndDecisionAndDueAtBefore(ApprovalStage stage, Decision decision, Instant cutoff);
}
