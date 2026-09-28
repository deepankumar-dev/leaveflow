package com.hackathon.leave.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "leave_types")
@Getter
@Setter
@NoArgsConstructor
public class LeaveType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ANNUAL, SICK, CASUAL, UNPAID. */
    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    /** Null or zero for UNPAID, which has no balance. */
    @Column(precision = 5, scale = 1)
    private BigDecimal annualQuota;

    private boolean proRata;
    private boolean requiresHr;
    private boolean paid = true;
}
