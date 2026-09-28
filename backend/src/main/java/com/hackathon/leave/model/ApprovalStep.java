package com.hackathon.leave.model;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "approval_steps")
@Getter
@Setter
@NoArgsConstructor
public class ApprovalStep {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id")
    private LeaveRequest request;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalStage stage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    /** Set when a delegation redirected this step; the original approver. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delegated_from_id")
    private User delegatedFrom;

    /** Escalation deadline for MANAGER steps. */
    private Instant dueAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Decision decision = Decision.PENDING;

    private Instant decidedAt;

    @Column(name = "comment_text", length = 1000)
    private String comment;
}
