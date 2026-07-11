# TEMP — Ops Operations: Implementation Summary (endpoints, queries, locks)

**Status: TEMPORARY WORKING DOC — answers a team question; NOT folded into
`requirment-v4.md` / `ops-console-proposal.md`. If any statement here
disagrees with those documents, they win.**
**Sources:** requirment-v4.md (§3, §4, §6.4/§6.7/§6.8, §7.0, §9.2/§9.3,
§10.1/§10.3, §11, §14, §20), ops-console-proposal.md rev 2026-07-10
(catalog O1–O12 + §3.1 coverage matrix), failure-recovery-walkthrough.md.
**Date:** 2026-07-11.

------

## 1. The shared transaction skeleton (every mutating operation)

Every ops mutation is the SAME two-tier flow the orchestrator uses
(§11) — the console/procedures never get their own mutation semantics
(proposal principle 1):

```sql
BEGIN;

-- Tier 1: obligation lock (serializes all activity on the scope)
SELECT id, committed_amount, confirmed_amount, required_amount,
       upstream_ordering, overpay_blocked, next_request_seq
  FROM payment_obligation
 WHERE id = :obligation_id
   FOR UPDATE;

-- Tier 2: the request CAS — preconditions ARE the WHERE clause,
-- expressed on DIMENSION columns only (never labels/blocked_reason, §10.1)
UPDATE payment_request
   SET <target dimensions>, version = version + 1,
       state_changed_at = SYS_EXTRACT_UTC(SYSTIMESTAMP)
 WHERE id = :request_id
   AND version = :expected_version          -- optimistic If-Match
   AND <operation-specific dimension preconditions>;

-- row count 0  → ROLLBACK; surface "state moved under you" (HTTP 409);
--                NO money movement, NO derivation
-- row count 1  → continue:

--   money movement (only for terminal-negative / confirm outcomes, §3)
UPDATE payment_obligation
   SET committed_amount = committed_amount - :request_amount   -- release
 WHERE id = :obligation_id;                                    -- (or += confirmed)

--   §6.8 standing shortfall re-evaluation (single creation point):
--   may INSERT the successor payment_request (next_request_seq++,
--   deterministic key §5.1, +committed) iff shortfall>0, I6 free,
--   no live marker, latch off, successor policy permits

--   §4 re-derivation under the SAME lock: ui_step_status,
--   active_exception_* recomputed (never accumulated)

--   §14 structured log line: request_id, idempotency_key, request_seq,
--   correlation_id, (dimensions before→after), display label,
--   trigger_source = MANUAL_OPS:<operator-id>, ticket reference

COMMIT;
```

Standing rules that apply to ALL of the below:

- **Lock order:** obligation FIRST, then request CAS. Multi-obligation
  operations (O12) lock in deterministic scope-tuple order (§6.1).
- **Backstops fire regardless of the caller:** §10.3 CHECK constraints
  (legality matrix L2–L8, L1 shape) and the L1-freeze + §10.1
  release-guard TRIGGERS refuse a violating write even from raw SQL.
- **Approval flow (4-eyes/dual):** `POST` creates a console-local
  approval row (initiator, reason, ticket) → approver confirms →
  execution runs the CAS then. The CAS re-check at execution time is
  the final arbiter; approvals expire after 24 h. Idempotency key for
  the endpoint = the approval-row id; `If-Match` carries the request
  `version`.
- **Audit:** no local journal (§14) — the log line + the mandatory
  external ticket reference ARE the audit trail (§20-8).

## 2. Operation-by-operation

### O1 — Retry a BLOCKED request

- **Endpoint:** `POST /requests/{id}/actions` `{type: RETRY, reason, ticketRef}` — 4-eyes.
- **CAS:**
```sql
UPDATE payment_request
   SET stage_state = 'RETRY_WAIT', blocked_reason = NULL,
       next_retry_at = :per_ops_retry_policy,      -- L7: explicit write
       version = version + 1
 WHERE id = :id AND version = :v
   AND outcome IS NULL
   AND stage_state = 'BLOCKED'
   AND submission_state = 'NOT_SUBMITTED'
   AND divergent_payload_at IS NULL;               -- repost_permitted stored term
```
- **Code-side preconditions (POST-stage rows only):** remaining
  `repost_permitted` terms (§7.0) — cutoff not passed, freeze off;
  the amount-staleness term is re-validated by the §6.4 retry-guard at
  claim time anyway (defense in depth: the posting claim re-checks
  everything).
- **Details:** stage is NOT changed — an ENRICH-blocked row re-enriches
  (§10.5); no money movement; attempt counters per the ops-retry policy
  class (config §16.6). Covers walkthrough E-2, E-4, P-7, P-11, B-6.

### O2 — Reject a BLOCKED request

- **Endpoint:** same `POST /requests/{id}/actions` `{type: REJECT, ...}` — 4-eyes (releases money).
- **CAS:**
```sql
UPDATE payment_request
   SET outcome = 'REJECTED', version = version + 1
 WHERE id = :id AND version = :v
   AND outcome IS NULL
   AND stage_state = 'BLOCKED'
   AND submission_state = 'NOT_SUBMITTED';         -- §10.1 release guard
```
- **Same transaction (row count 1):** `committed_amount -= amount`;
  set the `provider_rejected` marker on the obligation (L9 marker
  totality — every REJECTED outcome sets exactly one marker; ops is the
  "(or ops)" branch of §4.2), ordering-tagged with `creating_ordering`,
  `provider_reject_count + 1`; §6.8 re-evaluation (the live marker
  correctly BLOCKS an automatic successor — recovery is a newer
  message or the future O11 clear); re-derive; L1 freeze applies.
- Covers P-11 (give-up), M-6.

### O3 — Supersede/close a stalled request (the §3 REQUIRED operation)

- **Endpoint:** same `{type: SUPERSEDE, ...}` — 4-eyes (releases money).
- **CAS:**
```sql
UPDATE payment_request
   SET outcome = 'SUPERSEDED', version = version + 1
 WHERE id = :id AND version = :v
   AND outcome IS NULL
   AND submission_state = 'NOT_SUBMITTED'          -- §10.1 release guard
   AND stage_state <> 'CLAIMED';                   -- live claim owns its row (§11)
```
- **Same transaction:** release reservation; NO marker (totality is for
  REJECTED); §6.8 may create a right-sized successor immediately
  (shortfall reopened); re-derive.
- Covers M-5, M-6. Note: not restricted to BLOCKED — a stalled
  ENRICH·RETRY_WAIT row qualifies.

### O6 — Resolve now (ops-triggered status query)

- **Endpoint:** `POST /requests/{id}/resolve-now` `{reason, ticketRef}` — single operator.
- **DB:** NO direct payment-table mutation by the console. The
  implementation hands the request's key set to the resolver's
  **ops-triggered mode** (§9.5 — the same mode §5.2 step 5 uses, which
  queries an explicit key set regardless of state). The resolver
  applies whatever the engine answers through the normal
  evidence-guarded CAS (§9.1/§4.4) under the obligation lock.
- **Details:** consumes the shared §9.5 query budget (TL-13 rate
  limit); §20-3 open question governs whether it is free or
  rate-limited per operator.

### O7 — Ops-triggered §9.2 downgrade (same-key re-POST)

- **Endpoint:** `POST /requests/{id}/downgrade-repost` `{reason, ticketRef}` — 4-eyes.
- **CAS:** (the ONE sanctioned backward stage move)
```sql
UPDATE payment_request
   SET stage = 'POST', stage_state = 'RETRY_WAIT', blocked_reason = NULL,
       next_retry_at = SYS_EXTRACT_UTC(SYSTIMESTAMP),  -- downgrade class: now
       attempt_count = 0, version = version + 1
 WHERE id = :id AND version = :v
   AND outcome IS NULL
   AND submission_state = 'MAYBE_SUBMITTED'         -- stays MAYBE
   AND stage_state <> 'CLAIMED'
   AND divergent_payload_at IS NULL;
```
- **Code-side preconditions under the obligation lock:** past trust-age
  (`last_post_attempt_at` vs `NOT_FOUND_TRUST_AGE`); full
  `repost_permitted` — including amount vs current shortfall (the
  staleness term needs the locked obligation row) and cutoff/freeze.
  A row is never un-parked for an action the next gate would forbid.
- **Details:** same idempotency key; instruction re-assembled fresh at
  claim time (§7.0); safety rides on the §1 assumed collision contract.

### O8 — Dual-control stale-amount re-POST (§7.0 override)

- **Endpoint:** `POST /requests/{id}/stale-amount-repost` `{reason, ticketRef, secondApprover}` — strict dual control.
- **CAS:** identical target write to O7. The DIFFERENCE is the
  precondition check in code: `repost_permitted` must fail **solely**
  on the amount-staleness term (verified under the obligation lock:
  request amount ≠ current shortfall) — cutoff, divergent payload,
  freeze, terminal are NEVER overridable.
- **Details:** knowingly re-POSTs the request's own IMMUTABLE amount;
  if the original also executed, the overpay latch (§13) is the
  designed catch. Covers U-12.

### O9 — Request platform-side formal rejection (TL-10 ask)

- **Endpoint:** `POST /requests/{id}/platform-reject-ask` `{reason, ticketRef}` — single operator.
- **DB:** none on payment tables. Records the ask (console audit row +
  §14 log line) and raises the external request to the platform by
  UETR — or by idempotency key / end_to_end_id for rows that never
  received a UETR. If granted, the negative arrives through the normal
  feed/query evidence path (§9.4) — the clean exit; nothing local to do.

### O10 — Apply platform-verified outcome (THE MVP procedure)

- **Endpoint:** `POST /requests/{id}/platform-verified-outcome`
  `{outcome: EXECUTED|REJECTED, evidenceRef, ticketRef, approvers[2]}`.
- **DB:** `CALL apply_platform_verified_outcome(:request_id, :outcome,
  :ticket_ref, :evidence_ref, :approver1, :approver2)` — the console
  ONLY collects inputs; it never reimplements (§16.6 artifact 8 is the
  spec). Inside the procedure, per §9.3:
  1. obligation lock;
  2. dual control enforced (two distinct authenticated identities) +
     mandatory ticket — by the procedure, not by convention;
  3. sets the §10.3 evidence session flag (the release-guard trigger is
     passed LEGITIMATELY, never disabled);
  4. evidence-guarded CAS — refuses CLAIMED rows, terminal rows (L1),
     and amount mismatch:
     - EXECUTED → `outcome='EXECUTED'`, `submission_state='SUBMITTED'`,
       `confirmed_amount += amount` (amount equality enforced);
     - REJECTED → `outcome='REJECTED'`, provider_rejected marker (L9),
       `committed_amount -= amount`;
  5. §6.8 re-evaluation + §4 re-derivation;
  6. §14 log line `trigger_source = OPS_PLATFORM_VERIFIED` + §15
     every-use alert.
- This is the single sanctioned §9.4 exception and the guaranteed
  MAYBE-row terminal exit (§18 BLOCKING item 3).

### O11 — Retry-after-provider-reject (FUTURE — §18 PO-7)

- **Endpoint (when approved):** `POST /obligations/{scopeKey}/clear-provider-reject` `{reason, ticketRef, secondApprover}` — 4-eyes.
- **DB (obligation-level, under the lock):** clear
  `provider_rejected*` fields, reset `provider_reject_count`; then §6.8
  creates a FRESH successor (next_request_seq++, NEW deterministic
  key — correct: the engine definitively rejected the old key's
  payment). Records operator/reason/ticket per §19.3.

### O12 — Apply tied amendment (NEW — §20-10)

- **Endpoint:** `POST /trades/{businessId}/apply-tied-amendment`
  `{tieRecordRef, reason, ticketRef}` — 4-eyes. The snapshot payload
  comes from the tie-conflict record (§6.7 executability requirement:
  the alert + §14 log line carry the canonicalized §6.0 equality
  subset) — NEVER from the request body.
- **DB (TRADE-level):** for each payment block of the recorded
  snapshot, **sorted by scope tuple** (deterministic lock order, §6.1):
```sql
SELECT ... FROM payment_obligation
 WHERE business_id = :biz AND payment_type = :pt
   AND debit_account = :acct AND currency = :ccy
   FOR UPDATE;

UPDATE payment_obligation
   SET required_amount   = :tied_amount,
       upstream_ordering = :tied_ordering          -- idempotent re-apply
 WHERE id = :obligation_id
   AND (upstream_ordering IS NULL
        OR upstream_ordering <= :tied_ordering);   -- ≥ RELAXATION applies
                                                   -- ONLY to this recorded
                                                   -- tied ordering value
```
  then per block: §6.3/§6.4 amendment consequences (auto-cancel /
  amendment-park exactly as a normal message would), §6.8
  re-evaluation, §4 re-derivation. Blocks whose payload already
  matches are no-ops.
- **Details:** the operation adjudicates ORDERING only; every money
  guard (retry-guard, latch, markers, I6) applies unchanged. A later
  strictly-newer snapshot still supersedes normally.

### Annotation — overpay acknowledge (no O-number)

- **Endpoint:** `POST /obligations/{scopeKey}/annotation` `{text, ticketRef}` — single operator.
- **DB:** `UPDATE payment_obligation SET ops_annotation = :text WHERE id = :id;`
  Display-only read-model field (§2.1): no state change, no derivation
  input, latch never cleared (§13).

## 3. Read-side queries (S1 queues / S2 detail)

All queue scans key on dimension columns and episode anchors, and ride
the ACTIVE-row-bounded function indexes (§16.6 artifact 4 — expressions
NULL for terminal rows, so plans are independent of terminal-row count):

```sql
-- S1 · NEEDS_REVIEW queue (ESCALATED class ranked first)
SELECT r.id, o.business_id, o.payment_type, o.debit_account, o.currency,
       r.amount, r.blocked_reason, r.last_error_code, r.state_changed_at
  FROM payment_request r JOIN payment_obligation o ON o.id = r.payment_obligation_id
 WHERE r.outcome IS NULL AND r.stage_state = 'BLOCKED'
 ORDER BY CASE WHEN r.blocked_reason = 'ESCALATED' THEN 0 ELSE 1 END,
          r.state_changed_at;            -- non-churning state: safe clock (§15)

-- S1 · Aged MAYBE (episode anchor, never state_changed_at)
 WHERE r.outcome IS NULL AND r.submission_state = 'MAYBE_SUBMITTED'
   AND r.maybe_since < :now - :maybe_age_threshold
 ORDER BY r.maybe_since;                 -- + cutoff proximity from the calendar

-- S1 · Stuck reservations
 WHERE r.outcome IS NULL
   AND r.state_changed_at < :now - :max_age(stage, stage_state);

-- S1 · Overpay
SELECT ... FROM payment_obligation WHERE overpay_blocked = 'Y';

-- S2 · Detail (multi-payment: business_id returns ALL obligations, §12)
SELECT ... FROM payment_obligation WHERE business_id = :biz;   -- indexed
SELECT ... FROM payment_request WHERE payment_obligation_id IN (...)
 ORDER BY created_at;
-- Transition timeline: from the LOG STORE by request_id (§14) —
-- the console keeps no journal.
```

Reads take NO locks (plain MVCC reads); the S3 effect preview computes
its numbers in a read-only transaction that DOES take the obligation
lock briefly (proposal S3) so the preview matches what execution would
see.
