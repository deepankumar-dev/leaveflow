package com.hackathon.leave.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "delegations")
@Getter
@Setter
@NoArgsConstructor
public class Delegation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delegator_id")
    private User delegator;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delegate_id")
    private User delegate;

    @Column(nullable = false)
    private LocalDate fromDate;

    @Column(nullable = false)
    private LocalDate toDate;

    private boolean active = true;
}
