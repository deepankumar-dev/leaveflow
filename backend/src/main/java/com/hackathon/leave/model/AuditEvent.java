package com.hackathon.leave.model;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Append-only audit trail. A null actor means the SYSTEM (scheduler, auto-routing). */
@Entity
@Table(name = "audit_events")
@Getter
@Setter
@NoArgsConstructor
public class AuditEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id")
    private LeaveRequest request;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id", nullable = true)
    private User actor;

    /** SUBMITTED, FLAGGED, DELEGATED, MANAGER_APPROVED, HR_APPROVED, REJECTED, ESCALATED, CANCELLED. */
    @Column(nullable = false)
    private String action;

    @Enumerated(EnumType.STRING)
    private LeaveStatus fromStatus;

    @Enumerated(EnumType.STRING)
    private LeaveStatus toStatus;

    @Column(name = "comment_text", length = 1000)
    private String comment;

    @Column(name = "occurred_at", nullable = false)
    private Instant at;
}
