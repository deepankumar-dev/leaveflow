package com.hackathon.leave.model;

/** Outcome of one approval step. ESCALATED means the step timed out and the request moved on to HR. */
public enum Decision {
    PENDING, APPROVED, REJECTED, ESCALATED
}
