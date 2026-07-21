# Event-Model Design (Alternative B) — Exploration Docs

> **Status: DRAFT — design exploration, NOT the adopted baseline.**
> The authoritative design remains `requirment-v4.md` + the portable
> implementation playbook. This folder finalizes the EVENT-TABLE variant
> far enough that the two can be compared and decided on their merits.
> Nothing here changes the reviewed baseline; no card, test, or gate in
> the playbook refers to this folder.
>
> Origin: the runnable three-model comparison app in this repository
> (`proof.minimal`, "Model 2") proved that an append-only, single-table
> core CAN satisfy the money-safety scenarios — provided it carries the
> full guard set (version fence, write-once fenced identity, write-ahead,
> idempotent fold, ask-before-retry, strictly-newer admission). These
> documents scale that prototype up to the real requirement.

## Contents

| File | What it is |
|---|---|
| `01-event-table-schema.md` | The full schema: the event table, its constraints, the event vocabulary, the canonical fold, projections, and access paths — with the reasoning for every column |
| `02-scenario-walkthroughs.md` | Every scenario from `payment-state-playground.html`, replayed step by step in the event model, with the guard that makes each one safe |
| `scenario-walkthroughs.html` | The same walkthroughs as an interactive explainer (open in any browser): step through each scenario watching full PAYMENT_EVENT rows claim fenced slots while the fold panel is LITERALLY `fold(rows)` recomputed live — including the dual-stream trade scenarios |
| `03-known-limits.md` | **The honest cost sheet**: every known limitation of this model (consolidated from the adversarial self-review + the external assessment of it), each with a real-world example and fully simulated database rows — read this BEFORE forming an opinion from the scenario docs, which show only what works |

## The two design rules everything else follows from

1. **The event stream is the only truth.** Current state is never stored
   authoritatively; it is computed by THE fold (one shared, versioned,
   golden-vector-tested implementation — never re-implemented per reader).
   Anything materialized (projections) is a rebuildable cache and is never
   read by a money decision.
2. **Every write is fenced.** An append must claim the next
   `(payment_key, version)` slot; the database grants each slot exactly
   once. Winning the slot proves the writer's fold was current; losing it
   forces a re-fold. There is no other write path — not for scanners, not
   for operations, not for humans.

## Decision checklist before this could replace the baseline

(Adapted from the external assessment's recommended process; the
original migration item is STRUCK — this system is not live, there is
no legacy population, so no migration design is needed. Symmetrically,
the greenfield fact also makes a slice of the baseline's own migration
machinery cheap.)

1. ~~Structured event codes + per-event shape constraints~~ — DONE in
   `01` (EVENT_CODE + PE_SHAPE_* checks).
2. Specify the sole append API: raw-write privileges revoked, strict
   prefix validation in the append library, poison-stream behavior on
   semantically invalid history (mitigates, never closes, limit L1).
3. ~~Durable projection catch-up + scanner false-negative recovery~~ —
   contract DONE in `01` §6 (same-tx update + monitored full sweep);
   test set still to be written.
4. ~~UETR multiplicity rule + source-event dedup~~ — DONE in `01`
   (fail-closed rule + INBOUND_EVENT_INBOX restored); UETR claim
   uniqueness design still open.
5. Rewrite the restore/DR identity procedure (limit L6 — money-safety
   blocker; epoch component + burned-key reconciliation).
6. Request-granular UI and pagination parity (limit L8).
7. Event retention, archival, and compliance-deletion rules (limit L9).
8. Real-Oracle evidence: fence contention, multi-event atomic appends,
   malformed-event rejection, partial fan-out recovery, high-history
   fold performance.
9. Full §-by-§ parity review against `requirment-v4.md`, then external
   re-review — and the final comparison is on TOTAL complexity (build +
   assurance + operations), not conceptual elegance.

Severity ranking (per the PO's rubric — CRITICAL only if genuinely NOT
achievable inside a one-event-table design; HIGH = significant redesign
needed; MEDIUM = bounded additive change; LOW = policy/resolved):
**L1 and L2 are the two CRITICALs** — they are not defects to fix but
the PRICE of the model (temporal invariants enforced by disciplined
code instead of declarative schema; a derived, self-witnessed,
retroactively reinterpretable ledger) — adopt means accept. **L6 is the
one HIGH** (restore-time identity reuse — designable, undesigned).
Everything else is MEDIUM/LOW bounded work. See `03-known-limits.md`,
each with simulated rows.
