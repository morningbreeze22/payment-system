> **Purpose:** Minimal context packets RG-01..RG-10 — paste-alone briefs for a small-context local agent (original Section I, phase P7).
> **When to use this file:** Paired with the matching task-card file 08-task-cards/07-reservation-and-release-guards.md — one packet per task, used as the working brief.
> **Depends on:** 09-minimal-context-packets/README.md; the matching task card; the requirement sections each packet cites; 07-placeholder-glossary.md.
> **Used by:** The local coding agent executing phase P7.
> **Safe to transfer:** yes
> **Contains local code names:** no

# Minimal Context Packets — Phase P7

```text
[RG-01] +committed at creation
Read: §3 (increment) §6.8 §11. Invariant: reservation increments in the SAME transaction as the insert; NOTHING moves at POST time.
Placeholders: [Payment Request Creation Component] [Reservation Repository] [Obligation Repository]. Mappings: creation sites; D-03 semantics memo (legacy counters untouched).
Objective: committed_amount += amount with the insert, under the lock.
Tests: atomicity; I1; concurrent-create (I6 + single increment). Stop: merged.
```

```text
[RG-02] −committed on terminal-negative
Read: §3 (decrement) §10.2. Invariant: decrement iff the terminal-negative CAS affected exactly one row, same transaction; EXECUTED never decrements committed.
Placeholders: [Reservation Repository] [Request Status Persistence Layer]. Mappings: ST-06 helper.
Objective: extend the outcome path: REJECTED/CANCELLED/SUPERSEDED + rowCount 1 → decrement.
Tests: redelivery-safe (0 rows → no move); I1 across transitions. Stop: merged.
```

```text
[RG-03] +confirmed with amount equality
Read: §3 §8 (mismatch) §16.4 §10.5. Invariant: compareTo equality only; mismatch → BLOCKED(AMOUNT_MISMATCH)+SUB=SUBMITTED+CRITICAL, NO money movement.
Placeholders: [Reservation Repository] [Request Status Persistence Layer]. Mappings: none new.
Objective: settlement helper: equal → EXECUTED CAS (sets SUBMITTED, L4) + confirmed += amount; unequal → park path.
Tests: scale-variant equality; mismatch park; I2/I3; JPY/BHD round-trip. Stop: merged.
```

```text
[RG-04] Overpay latch
Read: §13 (latch + rationale + race) §3 (I4). Invariant: one-way; set when confirmed > required; NEVER auto-cleared by anything.
Placeholders: [Reservation Repository] [Metrics / Alerting Layer]. Mappings: RG-03 helper.
Objective: post-change check under lock → set latch (idempotent) + alert hook.
Tests: sets on overpay; survives required-amount rise (§13 trace); I4. Stop: merged.
```

```text
[RG-05] Release guard + supersede/close
Read: §10.1 §9.4 §3 (required feature + FORBIDDEN clause) §20. Invariant: terminal-negative only on NOT_SUBMITTED, or authoritative engine negative, or the §9.3 operation; a query answer never releases.
Placeholders: [Request Status Persistence Layer] [Operator Admin Procedure Area] [Stored Procedure / Trigger Area]. Mappings: S-06 flag mechanics.
Objective: shared guard before every terminal-negative CAS; guarded supersede/close procedure (restricted role, ticket + identity logged) refusing MAYBE/SUBMITTED.
Tests: deny/allow at code AND trigger layers; supersede releases on legal rows only. Stop: merged.
```

```text
[RG-06] Standing shortfall re-evaluation
Read: §6.8 (whole) §3 (I5) §6.2. Invariant: exactly ONE creation point; triggers T1–T4; successor policy gates REJECTED successors (ordering-newer ∧ count<2 ∧ no live marker).
Placeholders: [Payment Request Creation Component] [Obligation Repository]. Mappings: ALL legacy creation sites (unroutable → STOP).
Objective: evaluate() under lock per §6.8's condition list; invoke from T1–T4; route every legacy site through it. The creation INSERT stamps required_total_at_creation := the locked row's required_amount (§2.2 — set-once display stamp; never UPDATEd, never read by money logic; NULL only on pre-migration rows).
Tests: each trigger, each gate, each successor row; deferred amendment; zero-shortfall no-op; stamp cases — top-up stamps 100 then 120; reject-retry stamps 100 not 200; unchanged on downgrade/re-POST. Stop: merged.
```

```text
[RG-07] Auto-cancel + retry-guard
Read: §6.4 (whole) §7.0 (staleness term) §10.5. Invariant: cancellable set is strictly NOT_SUBMITTED (not POST·CLAIMED, not BLOCKED); row-count-0 branches on submission_state FIRST; MAYBE → AMENDMENT_PARKED (deferred under live claim).
Placeholders: [Payment Request Creation Component] [Request Status Persistence Layer] [Retry Resolver Job]. Mappings: amendment path.
Objective: the §6.4 CAS verbatim + branch handling + retry-guard (stale+NOT → cancel; stale+MAYBE → park).
Tests: every §10.5 cancel/park row; ENRICH·CLAIMED cancellable; POST·CLAIMED untouched; deferred park lands. Stop: merged.
```

```text
[RG-08] Step-status predicate
Read: §4.1 (predicate + BOTH branches + bullets) §4 §2.1 (liveness incl. anchor clause) §12. Invariant: completion derived only; anchors can't complete; active request blocks completion; feed never writes ui_step_status; round 11 — required = 0 (writable only by the §6.1 absence path) with 0/0/0 + no active request + validation_failed not LIVE derives CANCELLED (provider_rejected is NOT a predicate term — round 13), a TERMINAL branch displayed CANCELLED never COMPLETED, reopenable by a strictly newer positive block; provider_rejected never blocks the CANCELLED branch (round 12 — markers stay stored, resurface on reappearance).
Placeholders: [Obligation Repository] [Request Status Persistence Layer]. Mappings: ST-02 re-derive hook.
Objective: implement BOTH branches exactly (COMPLETED incl. required NOT NULL ∧ >0 and confirmed>=required terms; CANCELLED per round 11); output IN_PROGRESS/COMPLETED/CANCELLED; wire into every re-derivation; remove event-copy writers.
Tests: each term isolated; recovered anchor completes; zeroed row → CANCELLED never COMPLETED; zeroed + live provider_rejected (count 2) → still CANCELLED (round 13); zeroed with confirmed>0 → IN_PROGRESS + latch + OVERPAY_DETECTED (no request mutation — round 12); reappearance → IN_PROGRESS (T-37). Stop: merged.
```

```text
[RG-09] Exception + next-actor derivation
Read: §4.2 (ranks + round-12 suppression) §4.3 §4.5 §13. Invariant: derived, never accumulated; rank-1 (MAYBE, OVERPAY) never masked; actor never stored; active requests only; required = 0 suppresses ONLY historical PROVIDER_REJECTED (ordering < upstream_ordering, live solely via count >= 2); a LIVE validation_failed is ALWAYS visible (malformed-reappearance signature — round 13); markers stay stored, resurface on reappearance; active-request conditions + latch derive normally.
Placeholders: [Obligation Repository] [Request Status Persistence Layer]. Mappings: RG-08 hook.
Objective: precedence evaluation → active_exception_* writes (codes: PAYMENT_OUTCOME_UNKNOWN, OVERPAY_DETECTED, DATA_VALIDATION_FAILED, PROVIDER_REJECTED, BLOCKED-derived, INSUFFICIENT_ACCOUNT_BALANCE, SYSTEM_UNAVAILABLE; content per §12 rules); §4.5 actor as a pure function.
Tests: precedence; construction-clearing (corrected message); dual-actor rows; PAYMENT_OUTCOME_UNKNOWN never shows as SYSTEM_UNAVAILABLE. Stop: merged.
```

```text
[RG-10] Reopening + latch guard
Read: §6.5 (both terminal states) §6.3 §2.1 (reopened_at). Invariant: reopening = standing re-evaluation from COMPLETED or CANCELLED alike (round 12); ALL §6.8 gates apply on reappearance (live provider_rejected blocks; count >= 2 = ops-only clear); latched scope applies amounts but creates NOTHING (AMENDMENT_ON_LATCHED_SCOPE).
Placeholders: [Obligation Repository] [Payment Request Creation Component]. Mappings: amendment path.
Objective: required-increase on COMPLETED, or positive-again on CANCELLED → recalc + RG-06 (gates apply) + reopened_at + IN_PROGRESS + overpay re-eval; latch branch alerts instead.
Tests: reopening trace both terminal states; count-1 vs count-2 reappearance (T-37 F/G); latched branch; reopened_at set. Stop: merged.
```

