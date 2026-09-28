# Demo script (5 minutes)

Start the demo profile (see README section 1) and open http://localhost:5173. All users use `Password@123`. If you
have already clicked around, HR can press **Reset demo data** in the sidebar first.

## 1. Pro-rated balance (0:00-0:45), as Ravi
Click **New joiner (Ravi)** → **My balances**. Annual Leave shows 12 days entitled, with "24 × 6/12 = 12.0 (joined
2026-07-20)". Every number explains itself. 5 days are pending (his request for 16-20 Nov).

## 2. Conflict flag that never blocks (0:45-1:45), as Ravi
**Apply for leave** → Annual, 4 Nov → 5 Nov 2026. The live check shows 2 working days, balance 7 → 5, and an amber
"Team coverage conflict: 4 of 7 team members away". The Submit button stays enabled, and "Try these dates instead"
offers quieter windows. Submit anyway. **My requests** → click the new row: the audit timeline shows *Submitted* and the
conflict flag.

## 3. Approval chain, and balance only changes at the end (1:45-3:00)
Sign out → **Manager (Priya)**. The inbox lists Ravi's request (and Arjun's team's, delegated to her). Open Ravi's new
request → **Approve**: it becomes *Pending HR*. Ravi's balance is unchanged (still pending, nothing used). Sign out →
**HR (Meena)** → **HR queue** → open it → **Approve**: *Approved*. Now Ravi's used days go up by 2; that is the only
moment they change.

## 4. Escalation without auto-approval (3:00-4:00)
As Priya, open Ravi's 16-20 Nov request and click **Simulate timeout** (a real timeout is 48 h; the demo uses 1 minute).
It becomes *Escalated to HR*, and the timeline shows an *Escalated* event by SYSTEM. As Meena, **Escalations** shows it;
Priya can no longer approve it. Meena approves. The system never decided anything itself.

## 5. Guardrails and HR view (4:00-5:00)
- Open http://localhost:8080/swagger-ui.html, authorize with a token (POST `/api/auth/login`), and call approve on the
  already approved request: **409** with `{code, message, details}` and no stack trace. An employee token gets **403**.
- As Meena: **Analytics** (status mix, escalation rate, 30-day capacity forecast with the coverage threshold),
  **People & teams**, **Change password**.
- Finish with **Reset demo data**.
