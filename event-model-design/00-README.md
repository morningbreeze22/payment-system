# Event-Model Design (Alternative B) — Exploration Docs

> **Status: PROPOSAL — design exploration, NOT the adopted baseline.**
> The authoritative design remains `requirment-v4.md` + the portable
> implementation playbook. This folder carries the REFACTORED (v2)
> event-table variant far enough that the two designs can be compared
> and decided on their merits. Nothing here changes the reviewed
> baseline; no card, test, or gate in the playbook refers to this
> folder.
>
> Origin: the runnable three-model comparison app in this repository
> (`tl-proposal-proof`, "Model 2") proved that an append-only core CAN
> satisfy the money-safety scenarios provided it carries the full guard
> set. A first draft (v1) scaled that up and was reviewed to a ranked
> limits list; the v2 refactor then closed every item v1 ranked
> CRITICAL or HIGH by a mechanism. The v1 draft and its companion
> analyses (scenario walkthroughs, known-limits sheet, five-issue
> digest) are superseded and removed — they remain in git history.

## Contents

| File | What it is |
|---|---|
| `event-model-v2.md` | The refactor rationale: what changed from v1 and WHY — the v1-problem → v2-resolution map, the write-protocol redesign (pessimistic head lock, fence demoted to backstop), money-facts-as-events, fold governance, the contradiction exit, and the honesty box of what remains accepted |
| `01-event-table-schema.md` | The normative schema reference: full DDL for all four structures, the complete 19-type shape matrix (with the CHECK derivation rule), identity (request-ordinal, restore-safe), the event vocabulary with fold effects, the per-event write protocol, backstops, and read surfaces |

Where the two disagree, `event-model-v2.md` wins — `01` is its
derived reference and the mismatch is a defect in `01`.

## The design rules everything else follows from

1. **The event stream is the only truth.** Current state is never
   stored authoritatively; it is computed by THE fold (one shared,
   versioned, golden-vector-tested implementation). `PAYMENT_HEAD` is
   a transaction-fresh cache and witness: it can VETO a write (the
   write-path witness check, the CAS set, the triggers), it never
   authorizes one, and no money decision reads it as an input.
2. **One write path.** Every append happens under the head-row lock
   (the baseline's own lock-then-write idiom), events applied one at a
   time against the backstops; the fence `UNIQUE(payment_key, version)`
   remains as the loud backstop against anything that bypasses the
   lock. There is no privileged writer — not scanners, not ops, not
   humans.
3. **Money facts are events.** Every money-bearing fact carries its own
   amount and classification, recorded at decision time; the fold may
   only AGGREGATE them. Fold deploys are gated by re-fold-and-compare
   against the head witness.

## Decision checklist before this could replace the baseline

(The v1 checklist items closed by the v2 refactor are gone; this is
what actually remains. This system is not live — no migration design
is needed in either direction.)

1. **Adversarial review rounds.** Fifteen external adversarial
   rounds folded (2026-07-21/22: round 1 = 3C/4H/1M; round 2 = 3C/3H/1M;
   round 3 = 2C/4H/1M; round 4 = 0C/5H; round 5 fresh-eyes sweep =
   2C/2H/2M; round 6 = 3C/3H; round 7 = 2C/4H/1M, which killed the
   archival mechanism outright; round 8 = 3C/4H/1L; round 9 =
   2C/2H, which retracted round 8's event-level UETR claim for the
   head-level one; round 10 = 2C/4H (injective encoding,
   UETR-association binding, feed ACCEPTED, RESOLVED inbox status);
   round 11 = 1C/1H/1L (UETR required on feed evidence; audited
   two-exit inbox resolution); round 12 = 1C/3H (verified resolution
   provenance, RESOLVED_AGREED, approval-CAS-bound disposition,
   R-both-ways UETR conjunct); round 13 = 1C/4H/1L and round 14 =
   1C/4H/1L, both concentrated in the inbox-resolution
   correspondence machinery, which round 14 therefore RETRACTED as a
   class (write-path gates = sole legality authority; audit-pointer
   RESOLVED_HANDLED; audited RECONCILED_BY_KEY disposal;
   FEED_RESULT_RECORDED joined the open-ordinal trigger); round 15 =
   3C/1M (delivery-fidelity backstop restored narrowly — the
   retraction rationale honestly corrected; RECONCILED_BY_KEY
   state-gated; opening amount bound to the shortfall — a
   14-round-old gap; global UTC) — all closed by mechanism or
   removal, see `event-model-v2.md` §0; no clean round yet after
   fifteen). The baseline's mechanisms have
   survived MANY more such rounds; additional rounds are required
   before the two are comparable. Still the single largest open
   item.
2. **Fold specification + golden vectors.** The fold must implement
   the baseline §4/§6/§7/§9/§10 semantics verbatim; the vector set and
   the `fold --explain` MVP deliverable do not exist yet.
3. **Fold-change governance in practice.** The §4.2 deploy gate and
   drift scan are designed; the operational procedure (who runs the
   re-fold-compare, where results are filed) is not written.
4. **Upstream fact in writing.** "Strictly increasing per business_id
   sequence" must be a written contract fact (the BA-3 extension) —
   the deleted tie machinery stays deleted only if this holds.
5. **Trigger/CAS implementation evidence on real Oracle.** §6.2/§6.3
   of `01`: mutation-style tests proving each backstop actually
   rejects (per-event apply order, amount equality, key echo, version
   continuity, the closed-ordinal door admitting ONLY the dual-control
   pair), fence-collision head rebuild, contention behavior — plus the
   generated 19-constraint shape set with its matrix parity self-test
   (comments and Markdown enforce nothing; only the generated set
   does), the COMPOUND-trigger enforcement point (single-row-insert
   guard; mutating-table behavior proven on real Oracle, not assumed),
   and the guard trigger's TX_ID/CREATED_AT stamping.
6. **Data classification + the PII vault** — event rows are PERMANENT
   by design (`event-model-v2.md` §9: archival removed; partitioning
   is the only tiering), so ALL compliance-deletion pressure lands on
   the vault: data classification (which fields are personal data,
   which records carry mandatory retention) and the
   no-erasable-PII-in-events rule (vault + opaque references) must
   precede the first production event.
7. **Full §-by-§ parity review against `requirment-v4.md`**, then
   external re-review — the final comparison is on TOTAL complexity
   (build + assurance + operations), not conceptual elegance.

## How to read this folder against the baseline

The honest comparison is no longer schema properties — v2 closes the
v1 draft's self-reported blockers by mechanism. What differentiates
the two designs today: **review maturity** (the baseline's rules
embody the full adversarial review history; v2's equivalents have not
yet earned that) versus **write-path properties** (immutability, one
interpretation point, built-in per-payment history, set-once
timestamps, pure-function testability). Both stand equally on the §18
engine contract facts.
