# FROZEN BUSINESS RULES

These rules are frozen. Implementations and tests must match them exactly; change only with explicit permission.

## 1. Leave types

| Code | Name | Quota | Pro-rata | Requires HR | Paid | Balance tracked |
|---|---|---|---|---|---|---|
| ANNUAL | Annual Leave | 24 (`leave.annual-quota-default`) | yes | yes | yes | yes |
| SICK | Sick Leave | 12 | no | no | yes | yes |
| CASUAL | Casual Leave | 8 | no | no | yes | yes |
| UNPAID | Unpaid Leave | none | no | no | no | **no** (no balance check, no reservation) |

`requiresHr = false` means the chain ends at the manager's approval (status goes straight to APPROVED).

## 2. Pro-rata

`entitled = quota * monthsRemaining / 12`, rounded to the nearest 0.5.

- `monthsRemaining` counts the join month **fully**: joined any day in July → July..December = 6 months.
- Employees who joined before the balance year get the full quota.
- Example: quota 24, joined 2026-07-20 → 24 × 6/12 = **12.0**.
- Only leave types with `proRata = true` (ANNUAL) are pro-rated.

## 3. Working days

- Working days = Monday–Friday **minus** holidays in the `holidays` table.
- `days` on a request = count of working days in `[fromDate, toDate]`, both inclusive.
- A range that contains **no** working days (all weekend/holiday) is **rejected**.
- `fromDate <= toDate` is required.

## 4. Balance accounting

`available = entitled - used - pending`

- **Apply**: `pending += days`. Rejected with 422 if `days > available` (tracked types only).
- **Final approval**: `pending -= days; used += days`.
- **Reject / cancel while pending**: `pending -= days`.
- **Cancel after approval**: `used -= days`.

## 5. Conflict flag (never rejects)

A request is **flagged** when, on any day of the range,
`(overlappingTeammates + 1) / teamSize > 0.30` (`leave.conflict-threshold`).

- `teamSize` counts every active member of the requester's team, manager included.
- `overlappingTeammates` = other members of the same team with a request in status PENDING_MANAGER, ESCALATED, PENDING_HR or APPROVED covering that day.
- The flag and a human-readable `flagReason` are stored and shown; the request is **never** rejected because of it.

## 6. Approval chain

Employee → Manager → HR.

- An employee's request first goes to their manager (`PENDING_MANAGER`), then to HR (`PENDING_HR`), then `APPROVED`.
- A **manager's own leave goes straight to HR** (no manager step; initial status `PENDING_HR`).
- **Nobody decides their own request**, including HR (403).
- Reject at any step → `REJECTED`, comment **required**.
- Types with `requiresHr = false` skip the HR step.

## 7. Escalation

A `PENDING_MANAGER` step older than `leave.escalation-timeout-minutes` (checked every `leave.escalation-check-seconds`) →
status `ESCALATED`, the manager step is marked `ESCALATED`, an HR step is created, and **HR decides**.
The system never approves or rejects on anyone's behalf.

## 8. Delegation

- A manager delegates to another manager for `[fromDate, toDate]`.
- When a manager step is created while a delegation is effective, the step is **reassigned to the delegate** and `delegatedFrom` records the original manager.
- A delegate never decides on a request they own, nor on their own leave.

## 9. Overlapping own leave

A new request that overlaps the requester's own PENDING_MANAGER / ESCALATED / PENDING_HR / APPROVED request is **rejected** (409).

## 10. Cancellation

- The owner may cancel while pending, or after approval; the balance is released per §4.
- HR may also cancel an approved request.
- Terminal states: APPROVED (until cancelled), REJECTED, CANCELLED.

## 11. Status transitions (only via `WorkflowService.transition()`)

| From | To | Trigger |
|---|---|---|
| (new) | PENDING_MANAGER | employee applies |
| (new) | PENDING_HR | manager applies (own leave) |
| PENDING_MANAGER | PENDING_HR | manager (or delegate) approves, type requires HR |
| PENDING_MANAGER | APPROVED | manager approves, type does not require HR |
| PENDING_MANAGER | ESCALATED | timeout (system) |
| PENDING_MANAGER / ESCALATED / PENDING_HR | REJECTED | current approver rejects (comment required) |
| ESCALATED | APPROVED | HR approves |
| PENDING_HR | APPROVED | HR approves |
| PENDING_MANAGER / ESCALATED / PENDING_HR / APPROVED | CANCELLED | owner cancels (HR may cancel APPROVED) |

## 12. Audit & notifications

Every transition writes one `AuditEvent` (actor null = SYSTEM). Every transition also creates in-app `Notification` rows and a simulated `EmailOutbox` row (never actually sent).
