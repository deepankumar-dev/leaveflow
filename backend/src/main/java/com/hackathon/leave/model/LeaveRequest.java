package com.hackathon.leave.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "leave_requests")
@Getter
@Setter
@NoArgsConstructor
public class LeaveRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id")
    private User employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leave_type_id")
    private LeaveType leaveType;

    @Column(nullable = false)
    private LocalDate fromDate;

    @Column(nullable = false)
    private LocalDate toDate;

    /** Working days (Mon-Fri minus holidays), in steps of 0.5. */
    @Column(precision = 5, scale = 1, nullable = false)
    private BigDecimal days;

    @Column(length = 1000)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeaveStatus status;

    /** Team-coverage conflict flag: informational only, never blocks a request. */
    private boolean flagged;

    @Column(length = 500)
    private String flagReason;

    private Instant createdAt;
    private Instant lastActionAt;

    /** Optimistic lock: two people deciding the same request at once, the second gets 409. */
    @Version
    private Long version;

    @OneToMany(mappedBy = "request", fetch = FetchType.LAZY)
    @OrderBy("id ASC")
    private List<ApprovalStep> steps = new ArrayList<>();

    @OneToMany(mappedBy = "request", fetch = FetchType.LAZY)
    @OrderBy("at ASC, id ASC")
    private List<AuditEvent> auditEvents = new ArrayList<>();
}
