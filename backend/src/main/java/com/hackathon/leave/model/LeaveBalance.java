package com.hackathon.leave.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "leave_balances",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "leave_type_id", "balance_year"}))
@Getter
@Setter
@NoArgsConstructor
public class LeaveBalance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leave_type_id")
    private LeaveType leaveType;

    @Column(name = "balance_year", nullable = false)
    private int year;

    @Column(precision = 5, scale = 1, nullable = false)
    private BigDecimal entitled = BigDecimal.ZERO;

    @Column(precision = 5, scale = 1, nullable = false)
    private BigDecimal used = BigDecimal.ZERO;

    @Column(precision = 5, scale = 1, nullable = false)
    private BigDecimal pending = BigDecimal.ZERO;
}
