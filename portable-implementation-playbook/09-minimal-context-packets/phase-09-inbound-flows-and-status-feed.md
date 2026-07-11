> **Purpose:** Minimal context packets IN-01..IN-09 — paste-alone briefs for a small-context local agent (original Section I, phase P9).
> **When to use this file:** Paired with the matching task-card file 08-task-cards/09-inbound-flows-and-status-feed.md — one packet per task, used as the working brief.
> **Depends on:** 09-minimal-context-packets/README.md; the matching task card; the requirement sections each packet cites; 07-placeholder-glossary.md.
> **Used by:** The local coding agent executing phase P9.
> **Safe to transfer:** yes
> **Contains local code names:** no

# Minimal Context Packets — Phase P9

```text
[IN-01] Message validation + contract
Read: §6.0 (fields+equality+emission fact) §16.4 (scale) §16.5 §6.6. Invariant: payload equality = canonicalized business-field subset, never raw bytes; scale violations reject, never round.
Placeholders: upstream consumer, [Contract Test Suite]. Mappings: consumer.
Objective: field validation (business_id, scope, positive scale-valid amount, ordering, trade ref, ui ids, correlation); equality function; §6.6 failure routing; build-time schema enforcement.
Tests: validation cases; scale cases; equality (envelope excluded). Stop: merged.
```

```text
[IN-02] Snapshot admission + upsert + ordering guard
Read: §6.1 (ADMISSION first — round 5) §2.4 §6.7 (whole) §6.9 (required row) §6.0. Invariant: NO per-block work before the trade-level admission transaction (upsert-lock trade_snapshot_state; newer → admit+update; equal+digest-equal → admit; equal+digest-differs → tie alert, stop; older → refuse WHOLE — a refused document NEVER creates a scope); lock order trade row → obligations in tuple order; required_amount mutates only on strictly-newer ordering; comparator is ONE pluggable point shared by admission and blocks.
Placeholders: [Obligation Repository]. Mappings: upsert path; B-01 answered; S-10 applied.
Objective: admission gate; locked upsert (ORA-00001 retry); guard; stale counted; T1 → RG-06 even without amount change.
Tests: §6.7 regression trace; ties at admission; T1; T-35 admission set (never-seen-scope refused; disjoint first snapshots serialize; failed-validation advances neither watermark). Stop: merged.
```

```text
[IN-03] Validation anchors + DLT
Read: §6.6 (normal path only — tiers 2-3 are TL-7 FUTURE) §2.1 (marker fields) §4.1. Invariant: anchors never advance upstream_ordering; §4.1 cannot complete them.
Placeholders: [Obligation Repository], DLT. Mappings: consumer + DLT.
Objective: extractable-scope failures → anchor (required NULL, IN_PROGRESS, validation_failed marker with failing ordering, first_at, count); unidentifiable → DLT + alert.
Tests: anchor lifecycle incl. recovery by valid message; DLT. Stop: merged.
```

```text
[IN-04] Monotonic markers + counters
Read: §2.1 (marker blocks) §6.9 (write+read rules). Invariant: overwrite only on strictly-newer ordering; stale writes dropped+counted; provider_rejected live while count>=2 regardless of newer messages; first_at never refreshed by re-tags.
Placeholders: [Obligation Repository]. Mappings: none new.
Objective: one write helper per marker + liveness predicates (incl. anchor clause) + counters with the spec'd resets.
Tests: monotonicity; liveness truth table; first_at; counter resets. Stop: merged.
```

```text
[IN-05] Feed transaction order
Read: §8 (transaction + layering) §16.2 (ack) §4.4. Invariant: inbox insert FIRST (no locks); ack strictly after commit; unmatched = log+count+ack+drop.
Placeholders: [Payment Status Feed Consumer] [Inbox / Processed Event Repository] [Request Status Persistence Layer] [Reservation Repository]. Mappings: consumer + inbox.
Objective: rebuild consumption: inbox → resolve (UETR primary) → lock → evidence CAS → amounts on rowCount 1 → re-derive → commit → ack.
Tests: duplicate short-circuit; concurrent in-flight duplicate (row-lock then dup-key); ack-after-commit crash test. Stop: merged.
```

```text
[IN-06] Fallback + unmatched policy
Read: §8 (fallback + rationale) §16.6 (recency config). Invariant: provider_reference fallback iff exactly ONE ACTIVE match ∧ amount equal ∧ within recency; zero/multiple → unmatched; NO parked-event storage.
Placeholders: [Payment Status Feed Consumer]. Mappings: IN-05 skeleton.
Objective: resolution chain + fail-closed fallback + unmatched path.
Tests: single/multi/amount/recency cases; unmatched logged+counted+acked. Stop: merged.
```

```text
[IN-07] Evidence application
Read: §4.4 §10.1 (terminal + mirror) §8 (totality, negatives) §9.4. Invariant: terminal → ANY active row; intermediate → non-CLAIMED only, BLOCKED preserved; stale → zero rows; reject sets its marker in the SAME transaction.
Placeholders: [Payment Status Feed Consumer] [Request Status Persistence Layer]. Mappings: CA-2 ranks.
Objective: ONE shared evidence helper (feed + resolver): settlement → RG-03; reject → REJECTED + flag + marker + release; acceptance → SUBMITTED/CONFIRM/READY on non-CLAIMED; anomaly + return-event rules.
Tests: each rule; race both orders; terminal-evidence CRITICAL. Stop: merged.
```

```text
[IN-08] Amount-mismatch park
Read: §8 (mismatch) §16.4 §13. Invariant: mismatch = defect evidence; BLOCKED(AMOUNT_MISMATCH) + SUB=SUBMITTED + CRITICAL; confirmed does not move; resolution external.
Placeholders: [Payment Status Feed Consumer] [Metrics / Alerting Layer]. Mappings: IN-07.
Objective: verify the park path end to end from feed evidence incl. later corrected event completing normally.
Tests: park; corrected-event completion; redelivery. Stop: merged.
```

```text
[IN-09] Kafka hardening
Read: §16.2 (whole). Invariant: manual ack after commit; earliest; ErrorHandlingDeserializer; DLT for poison only; no retry topics on money events.
Placeholders: both consumers, [Metrics / Alerting Layer]. Mappings: D-07 gap checklist.
Objective: close every checklist gap; retention-chain scheduled check; keying verified (feed by UETR, upstream by business_id) else concurrency 1.
Tests: poison→DLT; transient→in-place; offset-after-commit. Stop: checklist compliant.
```

