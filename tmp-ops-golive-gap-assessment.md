# TEMP — Go-Live Gap Assessment: No Ops Actions Exist Yet

**Status: TEMPORARY WORKING DOC — answers a team question; NOT folded
into the baseline docs. Where it disagrees with `requirment-v4.md`,
that document wins.**
**Assumption (given):** the current code base implements ONLY the
read-only card (§12). None of the ops actions — no procedures, no
endpoints, no console — exist.
**Sources:** requirment-v4.md §3/§9.3/§18/§20, ops-console-proposal.md
rev 2026-07-10 (catalog + phasing), failure-recovery-walkthrough.md
(scenario IDs), tmp-ops-operations-implementation.md (op details).
**Date:** 2026-07-11.

------

## 1. Impact / risk if we go live with nothing but the card

The system is deliberately **fail-blocked**: when it cannot prove an
outcome it parks state and HOLDS the reservation. That philosophy is
only safe if the parked states have an exit. With zero ops surface,
every T3 scenario in the walkthrough becomes **fail-forever**:

| # | Risk | Mechanism | Walkthrough scenarios | Severity |
|---|------|-----------|----------------------|----------|
| R1 | **Permanently wedged MAYBE rows** — money reserved forever, scope can never complete | repost_permitted permanently false (stale amount / passed cutoff / divergent payload) + key aged past the engine's query lookback → NOT_FOUND unfalsifiable → no automated exit exists BY DESIGN; the §9.3 procedure is the designed exit | P-2 tail, P-6, R-3, U-12 | **CRITICAL — this is exactly why §18 BLOCKING item 3 exists; going live without it violates a stated go-live gate** |
| R2 | **Blocked payments never leave the queue** — retry-exhausted, cutoff-expired, unmapped-code rows accumulate; I6 blocks any successor while the active row holds its reservation | O1/O2/O3 (retry / reject / supersede) have no executable form; §3 explicitly makes supersede/close a REQUIRED capability | E-2, E-4, P-7, P-11, M-5, M-6, B-6 | HIGH — every blocked payment is a missed/late payment with no remediation path |
| R3 | **Alert surface pages humans with no lever** | §9.3 escalation + tier-2 re-page assume the queue is workable; ops can look (card) but not act | all BLOCKED/ESCALATED alerts | HIGH — alert fatigue, SLA breach, and the inevitable workaround: raw SQL |
| R4 | **Raw-SQL workaround under incident pressure** | §10.3 CHECKs/triggers will refuse UNSAFE writes (good), but safe-looking writes without reason/ticket/4-eyes violate §20-7/8; after a DB restore the ticket trail is the ONLY surviving audit record | H-1, H-2, H-7 | HIGH — audit/compliance exposure, and drift between DB truth and any record of why |
| R5 | **Tie conflicts stall whole trades** | AMENDMENT_TIE_CONFLICT requires manual application (§6.7/§20-10); no operation → the amendment is never applied; a resend ties forever | U-9 | MEDIUM (rare by construction; disappears with upstream ask 1) |
| R6 | **Repeat provider-reject scopes stay frozen** | from the 2nd reject the marker is ops-only-clearable (§2.1); with no ops clear, the scope waits indefinitely | P-10, M-8, B-4 | MEDIUM (O11 is FUTURE/PO-7 anyway — the freeze is partly by design) |
| R7 | Overpay latches unacknowledged | annotation is display-only | M-2..M-4 | LOW |

What does NOT get worse without an ops surface (already covered by
runtime automation or existing levers): all T0/T1/T2 scenarios —
durable retries, the resolver sweep, the §9.2 auto-downgrade, breaker
recovery, redelivery convergence, the Hazelcast freeze toggle (role
control exists today, §16.1), and Kafka DLT tooling (platform-owned).

**Net:** the design's money-safety does NOT degrade (nothing pays
twice; the DB backstops still refuse unsafe writes). What breaks is
LIVENESS and AUDITABILITY: payments that hit any dead-end state stay
dead, reservations stay held, and the §4.1 completion predicate keeps
those scopes incomplete forever.

## 2. Must-have BEFORE go-live

The PO decision (§20) already defines the bar: MVP ships WITHOUT a
console, but dead-end states are exited via **controlled manual
database procedures**. "Only the card exists" does not meet that bar.
The must-have list, in order:

```text
M1  apply-platform-verified-outcome STORED PROCEDURE + ops drill
    — §18 BLOCKING item 3; §16.6 artifact 8 is the spec. This is a
    named GO-LIVE GATE (GO-04/Q28 in the playbook): procedure exists,
    dual control enforced inside it, drill executed. The only
    de-scope condition: TL-10 AND TL-5-lookback both affirmed in
    writing + PO re-confirmation. Closes R1.

M2  Guarded manual procedures for the release family + retry
    (O1 retry / O2 reject / O3 supersede equivalents), with the
    §10.1/§10.3-guarded CAS shape, mandatory operator-id + reason +
    ticket parameters, and the §14 log line. §3 makes supersede/close
    a REQUIRED feature; §20 names these as the interim model.
    Closes R2, and (with M5) most of R3.

M3  ops_annotation write procedure (one UPDATE) — trivial, closes R7
    and gives the overpay queue an acknowledgement path (§20-4).

M4  Tie-application procedure (O12 equivalent, §20-10) — AND the
    runtime prerequisite that CANNOT be retrofitted: the
    AMENDMENT_TIE_CONFLICT record (alert + §14 log line) must carry
    the canonicalized snapshot payload FROM DAY ONE (§6.7
    executability requirement). If day-one scope must shrink, ship
    the payload preservation at go-live and the procedure in the
    first patch — without the preserved payload there is nothing any
    later tool can apply. Closes R5.

M5  Ops READ access to the queues — the card is user-facing
    (business_id lookup only, §12) and cannot answer "what needs
    working, oldest first". Minimum: the four read-only queue VIEWS
    (BLOCKED-by-reason with ESCALATED first, stuck reservations,
    aged-MAYBE by maybe_since + cutoff proximity, overpay latches)
    + the §15 alerts wired with runbook links pointing at them.
    Without this, M1/M2 exist but nobody can find their targets.

M6  Process guardrail for §20-7/8 until a UI exists: procedures
    REQUIRE (not merely record) operator id, reason, ticket ref, and
    a second approver id where the catalog says 4-eyes/dual —
    enforcement in the procedure signature, not in a wiki page.
```

Explicitly NOT needed for go-live (deliberate, with rationale):

```text
- O6 resolve-now        the §9.5 sweep already queries every MAYBE/
                        aged-SUBMITTED row automatically; on-demand
                        is ergonomics (§20-3 open question)
- O7 ops downgrade      the §9.2 AUTO-downgrade is the runtime
                        self-heal; the ops-triggered variant adds
                        nothing at MVP
- O8 stale re-POST      wait-then-decide + M1 (verified outcome)
                        cover the parked-MAYBE exits; O8 only
                        shortens toil
- O9 TL-10 ask          an email/ticket to the platform needs no
                        endpoint; capture-in-console is P1 polish
- O11 marker clear      FUTURE by definition (pending §18 PO-7)
- The console UI itself PO-accepted as post-MVP (§20); P0–P2 phasing
                        in ops-console-proposal.md stands
```

## 3. Minimal surface to build (given: only the card exists today)

Two build lanes — the MVP lane is procedures-first (matches the PO
decision and is the smallest thing that meets the bar); the endpoint
column is what those same operations become when console P1/P2 wraps
them (no rework: the procedures stay the execution layer).

| Priority | Operation | MVP form (go-live) | Later console endpoint (P1/P2) | Closes |
|---|---|---|---|---|
| 1 | Apply platform-verified outcome (O10) | `apply_platform_verified_outcome(request_id, outcome, ticket_ref, evidence_ref, approver1, approver2)` — dual control INSIDE the procedure; §15 every-use alert | `POST /requests/{id}/platform-verified-outcome` (UI wrapper only) | R1 |
| 2 | Retry blocked (O1) | `ops_retry_blocked(request_id, expected_version, operator, approver, reason, ticket)` | `POST /requests/{id}/actions {type:RETRY}` | R2 |
| 2 | Reject blocked (O2) | `ops_reject_blocked(...)` — releases reservation + provider_rejected marker | same, `{type:REJECT}` | R2 |
| 2 | Supersede/close (O3) | `ops_supersede(...)` — the §3 required op | same, `{type:SUPERSEDE}` | R2 |
| 3 | Queue visibility | 4 read-only DB views (+ card stays as-is); §15 alerts link to them | `GET /queues/blocked|stuck|maybe-aged|overpay` + S1/S2 read-only screens (proposal P0) | R3 |
| 4 | Tie application (O12) | payload preservation in the tie-conflict record at go-live (runtime, non-negotiable) + `ops_apply_tied_amendment(business_id, tie_record_ref, operator, approver, reason, ticket)` | `POST /trades/{businessId}/apply-tied-amendment` | R5 |
| 5 | Overpay annotate | `ops_annotate(obligation_id, text, operator, ticket)` | `POST /obligations/{scopeKey}/annotation` | R7 |

Sizing note: rows 1–5 are six stored procedures (one of which, O10, is
already mandated by §18-3 regardless of this assessment), four SQL
views, and one runtime change (tie-record payload). No schema changes;
every WHERE clause and money movement is already specified
(tmp-ops-operations-implementation.md §2). The §10.3 CHECKs/triggers
and the §14 log line are assumed present — they are runtime
requirements of the baseline, not ops-surface extras; if THEY are also
missing, they precede everything in this list.

Recommended sequencing to go-live: **M5 views → M2 procedures → M1
procedure + drill → M4 payload preservation (+ procedure) → M3/M6** —
read surface first so every subsequent procedure can be exercised and
drilled against real queues.

## 4. One-line answer

Going live with only the card is not an option the baseline permits:
§18 BLOCKING item 3 (the O10 procedure + drill) is a hard gate, and §20's
accepted interim model presumes the guarded procedures exist. The
minimal build is six procedures + four queue views + the tie-record
payload preservation; the console itself stays post-MVP as decided.
