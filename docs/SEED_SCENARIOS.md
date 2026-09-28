# SEED SCENARIOS (fixed data)

All dates are hard-coded, never relative to "today". The seed's reference "now" is **2026-10-12 10:00 IST (Monday)**; every
`createdAt` / event timestamp below is on or before it. Password for every user: `Password@123`. Emails end in `@leave.demo`.

Calendar check (2026): 1 Oct = Thu, 2 Oct = Fri, 12 Oct = Mon, 2 Nov = Mon, 30 Nov = Mon.

## Holidays

| Date | Day | Name |
|---|---|---|
| 2026-10-02 | Fri | Gandhi Jayanti |
| 2026-10-20 | Tue | Dussehra |
| 2026-11-09 | Mon | Diwali (Govardhan Puja) |
| 2026-11-24 | Tue | Guru Nanak Jayanti |
| 2026-12-25 | Fri | Christmas Day |

## Teams & people (15 users)

| Team | Size | Members |
|---|---|---|
| Engineering | 7 | Priya Sharma (MANAGER) + 6 employees |
| Product | 7 | Arjun Mehta (MANAGER) + 6 employees |
| HR | 1 | Meena Iyer (HR) |

| Name | Email | Role | Team | Manager | Joined | Notes |
|---|---|---|---|---|---|---|
| Meena Iyer | meena@leave.demo | HR | HR | – | 2020-01-06 | |
| Priya Sharma | priya@leave.demo | MANAGER | Engineering | – | 2020-08-03 | own leave → HR |
| Arjun Mehta | arjun@leave.demo | MANAGER | Product | – | 2020-05-04 | delegates to Priya 2026-10-08..10-23 |
| Asha Rao | asha@leave.demo | EMPLOYEE | Engineering | Priya | 2022-06-01 | |
| **Ravi Kumar** | ravi@leave.demo | EMPLOYEE | Engineering | Priya | **2026-07-20** | **mid-year joiner** (pro-rata 12.0) |
| Kiran Patel | kiran@leave.demo | EMPLOYEE | Engineering | Priya | 2021-02-15 | |
| Divya Nair | divya@leave.demo | EMPLOYEE | Engineering | Priya | 2023-01-09 | |
| Sam Thomas | sam@leave.demo | EMPLOYEE | Engineering | Priya | 2022-11-21 | |
| Tarun Iyer | tarun@leave.demo | EMPLOYEE | Engineering | Priya | 2022-02-07 | |
| Neha Kapoor | neha@leave.demo | EMPLOYEE | Product | Arjun | 2021-09-01 | |
| Rohan Das | rohan@leave.demo | EMPLOYEE | Product | Arjun | 2022-03-14 | |
| Isha Gupta | isha@leave.demo | EMPLOYEE | Product | Arjun | 2023-04-03 | |
| Karan Joshi | karan@leave.demo | EMPLOYEE | Product | Arjun | 2024-01-15 | **balance edge** |
| Meera Pillai | meera@leave.demo | EMPLOYEE | Product | Arjun | 2023-08-21 | |
| Vikas Singh | vikas@leave.demo | EMPLOYEE | Product | Arjun | 2022-09-12 | |

That is 12 employees, 2 managers, 1 HR. With team size 7, the conflict rule `(overlap+1)/7 > 0.30` means **2 or more overlapping teammates flags**; 1 does not (2/7 = 0.286).

## Delegation

| Delegator | Delegate | From | To | Active |
|---|---|---|---|---|
| Arjun Mehta | Priya Sharma | 2026-10-08 | 2026-10-23 | yes |

## Requests (18)

`days` = working days after weekends and holidays. Times are IST on the given date. Steps: M = MANAGER stage, H = HR stage.
Seeded pending steps use `dueAt = 2099-01-01` so the scheduler never fires by itself; use `POST /api/demo/simulate-timeout/{id}`.

| # | Employee | Type | From → To | Days | Status | Flag | Scenario |
|---|---|---|---|---|---|---|---|
| R01 | Asha Rao | ANNUAL | 2026-10-26 → 10-28 | 3 | APPROVED | – | fully approved |
| R02 | Kiran Patel | ANNUAL | 2026-11-02 → 11-04 | 3 | APPROVED | – | fully approved (creates overlap) |
| R03 | Divya Nair | ANNUAL | 2026-11-03 → 11-06 | 4 | APPROVED | – | fully approved; 1 overlap (Kiran) = 2/7, not flagged |
| R04 | Sam Thomas | ANNUAL | 2026-11-04 → 11-05 | 2 | PENDING_MANAGER | **yes** | conflict-flagged: Kiran + Divya overlap on 4 Nov → 3/7 = 43% |
| R05 | Ravi Kumar | ANNUAL | 2026-11-16 → 11-20 | 5 | PENDING_MANAGER | – | mid-year joiner; **best candidate for simulate-timeout** |
| R06 | Neha Kapoor | ANNUAL | 2026-10-14 → 10-16 | 3 | PENDING_HR | – | manager-approved, awaiting HR |
| R07 | Rohan Das | ANNUAL | 2026-10-21 → 10-23 | 3 | ESCALATED | – | Arjun did not act; timed out, HR step pending |
| R08 | Isha Gupta | ANNUAL | 2026-10-19 → 10-21 | 2 | PENDING_MANAGER | **yes** | **delegated** (M assignee Priya, delegatedFrom Arjun); 20 Oct holiday excluded; Arjun (R09) + Rohan (R07) overlap on 21 Oct → 3/7 |
| R09 | Arjun Mehta | ANNUAL | 2026-10-19 → 10-23 | 4 | APPROVED | – | manager's own leave: HR-only step, approved by Meena |
| R10 | Meera Pillai | SICK | 2026-10-08 → 10-09 | 2 | APPROVED | – | type without HR step: approved by Arjun only |
| R11 | Vikas Singh | CASUAL | 2026-10-15 → 10-15 | 1 | REJECTED | – | rejected by Arjun, comment "Release deadline, please pick another day" |
| R12 | Tarun Iyer | ANNUAL | 2026-11-10 → 11-13 | 4 | CANCELLED | – | approved, then cancelled by owner (balance released) |
| R13 | Karan Joshi | ANNUAL | 2026-11-23 → 11-27 | 4 | PENDING_MANAGER | – | delegated to Priya; **insufficient-balance edge**: consumes exactly the last 4 days (available → 0); 24 Nov holiday excluded |
| R14 | Priya Sharma | ANNUAL | 2026-11-30 → 12-02 | 3 | PENDING_HR | – | manager's own leave → HR only |
| R15 | Asha Rao | CASUAL | 2026-10-30 → 10-30 | 1 | APPROVED | – | approved by Priya only (no HR step) |
| R16 | Ravi Kumar | CASUAL | 2026-10-09 → 10-09 | 1 | APPROVED | – | approved by Priya only |
| R17 | Neha Kapoor | UNPAID | 2026-12-28 → 12-31 | 4 | PENDING_MANAGER | – | unpaid: no balance touched |
| R18 | Asha Rao | ANNUAL | 2026-11-02 → 11-03 | 2 | PENDING_MANAGER | **yes** | conflict-flagged: Kiran + Divya overlap on 3 Nov → 3/7 |

Documented negative cases (not stored; used in tests/demo):
- Karan applies ANNUAL 2026-12-21 → 12-22 → **422 insufficient balance** (available 0).
- Anyone applies 2026-11-09 → 11-09 (Mon, holiday) → **rejected, no working days**.
- Ravi applies ANNUAL 2026-11-16 → 11-17 (overlaps R05) → **409 overlapping own leave**.
- Priya tries to approve R14 (her own leave) → **403 own request**.

## Balances (year 2026)

Annual entitled is 24, except Ravi 12.0 (24 × 6/12; joined July). "Baseline used" is leave taken earlier in 2026 with no request rows.

### ANNUAL

| User | Entitled | Baseline used | Seeded used | Used total | Pending | Available |
|---|---|---|---|---|---|---|
| Meena Iyer | 24 | 2 | 0 | 2 | 0 | 22 |
| Priya Sharma | 24 | 6 | 0 | 6 | 3 (R14) | 15 |
| Arjun Mehta | 24 | 2 | 4 (R09) | 6 | 0 | 18 |
| Asha Rao | 24 | 5 | 3 (R01) | 8 | 2 (R18) | 14 |
| Ravi Kumar | 12.0 | 0 | 0 | 0 | 5 (R05) | 7 |
| Kiran Patel | 24 | 4 | 3 (R02) | 7 | 0 | 17 |
| Divya Nair | 24 | 3 | 4 (R03) | 7 | 0 | 17 |
| Sam Thomas | 24 | 6 | 0 | 6 | 2 (R04) | 16 |
| Tarun Iyer | 24 | 8 | 0 (R12 cancelled) | 8 | 0 | 16 |
| Neha Kapoor | 24 | 5 | 0 | 5 | 3 (R06) | 16 |
| Rohan Das | 24 | 7 | 0 | 7 | 3 (R07) | 14 |
| Isha Gupta | 24 | 4 | 0 | 4 | 2 (R08) | 18 |
| Karan Joshi | 24 | 20 | 0 | 20 | 4 (R13) | **0** |
| Meera Pillai | 24 | 3 | 0 | 3 | 0 | 21 |
| Vikas Singh | 24 | 9 | 0 | 9 | 0 | 15 |

### SICK (entitled 12, not pro-rata)

Meera Pillai: used 2 (R10), available 10. Everyone else: used 0, pending 0, available 12.

### CASUAL (entitled 8, not pro-rata)

Asha Rao: used 1 (R15), available 7. Ravi Kumar: used 1 (R16), available 7. Vikas Singh: used 0 (R11 rejected, released), available 8. Everyone else: used 0, available 8.

### UNPAID

No balance rows.

## Approval steps & audit events

Actor "SYSTEM" = null actor. Every request starts with `SUBMITTED`. `→` shows from/to status.

| # | Steps | Audit events (timestamp, action, actor, from→to, comment) |
|---|---|---|
| R01 | M: Priya APPROVED 10-01 16:00; H: Meena APPROVED 10-05 11:00 | 10-01 09:00 SUBMITTED Asha →PENDING_MANAGER · 10-01 16:00 MANAGER_APPROVED Priya →PENDING_HR · 10-05 11:00 HR_APPROVED Meena →APPROVED |
| R02 | M: Priya APPROVED 10-01 15:00; H: Meena APPROVED 10-05 11:30 | 10-01 10:00 SUBMITTED Kiran · 10-01 15:00 MANAGER_APPROVED Priya · 10-05 11:30 HR_APPROVED Meena |
| R03 | M: Priya APPROVED 10-02 12:00; H: Meena APPROVED 10-05 12:00 | 10-02 10:00 SUBMITTED Divya · 10-02 12:00 MANAGER_APPROVED Priya · 10-05 12:00 HR_APPROVED Meena |
| R04 | M: Priya PENDING (due 2099-01-01) | 10-09 10:00 SUBMITTED Sam →PENDING_MANAGER · 10-09 10:00 FLAGGED SYSTEM "3 of 7 team members (43%) away on 2026-11-04: Kiran Patel, Divya Nair" |
| R05 | M: Priya PENDING (due 2099-01-01) | 10-10 09:30 SUBMITTED Ravi →PENDING_MANAGER |
| R06 | M: Arjun APPROVED 10-08 09:00; H: Meena PENDING | 10-07 14:00 SUBMITTED Neha · 10-08 09:00 MANAGER_APPROVED Arjun →PENDING_HR |
| R07 | M: Arjun ESCALATED 10-06 09:01; H: Meena PENDING | 10-06 09:00 SUBMITTED Rohan →PENDING_MANAGER · 10-06 09:01 ESCALATED SYSTEM →ESCALATED "Manager step timed out after 1 minute" |
| R08 | M: Priya PENDING, delegatedFrom Arjun (due 2099-01-01) | 10-09 11:00 SUBMITTED Isha →PENDING_MANAGER · 10-09 11:00 DELEGATED SYSTEM "Routed to Priya Sharma (delegated by Arjun Mehta)" · 10-09 11:00 FLAGGED SYSTEM "3 of 7 team members (43%) away on 2026-10-21: Arjun Mehta, Rohan Das" |
| R09 | H: Meena APPROVED 10-03 10:00 | 10-01 11:00 SUBMITTED Arjun →PENDING_HR · 10-03 10:00 HR_APPROVED Meena →APPROVED |
| R10 | M: Arjun APPROVED 10-08 08:45 | 10-08 08:30 SUBMITTED Meera →PENDING_MANAGER · 10-08 08:45 MANAGER_APPROVED Arjun →APPROVED "Get well soon" |
| R11 | M: Arjun REJECTED 10-06 10:00 "Release deadline, please pick another day" | 10-05 15:00 SUBMITTED Vikas · 10-06 10:00 REJECTED Arjun →REJECTED "Release deadline, please pick another day" |
| R12 | M: Priya APPROVED 10-03 09:00; H: Meena APPROVED 10-05 10:00 | 10-02 15:00 SUBMITTED Tarun · 10-03 09:00 MANAGER_APPROVED Priya · 10-05 10:00 HR_APPROVED Meena →APPROVED · 10-08 17:00 CANCELLED Tarun →CANCELLED "Plans changed" |
| R13 | M: Priya PENDING, delegatedFrom Arjun (due 2099-01-01) | 10-11 12:00 SUBMITTED Karan →PENDING_MANAGER · 10-11 12:00 DELEGATED SYSTEM "Routed to Priya Sharma (delegated by Arjun Mehta)" |
| R14 | H: Meena PENDING | 10-08 10:00 SUBMITTED Priya →PENDING_HR |
| R15 | M: Priya APPROVED 10-08 13:00 | 10-08 11:00 SUBMITTED Asha · 10-08 13:00 MANAGER_APPROVED Priya →APPROVED |
| R16 | M: Priya APPROVED 10-08 14:00 | 10-08 12:00 SUBMITTED Ravi · 10-08 14:00 MANAGER_APPROVED Priya →APPROVED |
| R17 | M: Priya PENDING, delegatedFrom Arjun (due 2099-01-01) | 10-11 16:00 SUBMITTED Neha →PENDING_MANAGER · 10-11 16:00 DELEGATED SYSTEM "Routed to Priya Sharma (delegated by Arjun Mehta)" |
| R18 | M: Priya PENDING (due 2099-01-01) | 10-11 09:00 SUBMITTED Asha →PENDING_MANAGER · 10-11 09:00 FLAGGED SYSTEM "3 of 7 team members (43%) away on 2026-11-03: Kiran Patel, Divya Nair" |

Delegation note: Arjun's delegation is effective from 2026-10-08. Steps created **before** that date (R06, R07) stay with Arjun.
Steps created inside the window are assigned to Priya with `delegatedFrom = Arjun`: R08, R13 and R17. (R10, R11 belong to
Arjun's own approvals on 10-06/10-08 09:00 and were decided before/at the start of the window; R10 was created 10-08 08:30, before the delegation was created at 10-08 12:00.)

## Notifications & email

For each seeded transition the seeder writes the same notifications the live system would (e.g. R06 → one to Meena "ready for HR approval",
R07 → one to Meena "escalated", R04/R08/R18 → flagged notice to the approver). Unread by default except those older than 2026-10-08. One `EmailOutbox` row per notification, `sent = false`.

## Quick demo paths

- **Conflict flag**: log in as Sam, preview 2026-11-04 → 11-05 → flagged (R04 exists).
- **Escalation**: log in as Priya, `POST /api/demo/simulate-timeout/{R05 id}` → R05 becomes ESCALATED, Meena sees it.
- **Delegation**: Priya's inbox shows R08 with "delegated from Arjun".
- **Pro-rata**: Ravi's balance shows `24 × 6/12 = 12.0`.
- **Insufficient balance**: Karan applies 12-21 → 12-22 → rejected.
