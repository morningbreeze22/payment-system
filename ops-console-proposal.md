# Ops Console — Proposal

**Status:** PROPOSAL — FUTURE implementation, pending PO discussion (`requirment-v4.md` §20).
**PO DECISION (recorded in §20; execution boundary decided 2026-07-11):** the MVP ships WITHOUT an ops console. Interim: dead-end states are exited via controlled, AUTHORIZED ADMIN OPERATIONS — enterprise-authenticated endpoints of the payment application invoking the same shared transition service (never PL/SQL reimplementations), under the §10.1 release guard with the §10.3 trigger backstops. THREE operations are NON-WAIVABLE at go-live (the §20 minimal exit set, round 4): the §9.3 **apply-platform-verified-outcome** audited operation (additionally the §18 BLOCKING item-3 gate), **supersede/close**, and **reprocess-snapshot**; retry/reject/annotation/views are Q29-waivable ergonomics. All exist independently of this console; the console would only ever be a UI over these same endpoints.
**Baseline:** `requirment-v4.md` (v4, factored state model). This revision supersedes the v2-era draft: O4/O5 (replay/discard parked event) are **OBSOLETE** — no parked-event store exists (§2.3); unmatched feed events are log/metric/ack and outcomes recover via the §9 status-query sweep. All state displays use the §10.4 labels; all action preconditions are expressed on the dimension columns (stage, stage_state, submission_state, outcome) — never on labels and never on blocked_reason (§10.1).
**Updated:** 2026-07-11 — second-external-review response: **O12 revised to REPROCESS-SNAPSHOT** (the snapshot XML lives durably in the upstream-populated store, Kafka carries only the storage id — §6.0 transport note; the earlier payload-in-tie-record design conflicted with §16.3 masking) and the **execution boundary decided**: every mutation here is an authorized, enterprise-authenticated endpoint of the payment application calling the shared transition service — never a PL/SQL reimplementation; §10.3 triggers stay as backstops. (2026-07-10 revision had added O12 + the §3.1 coverage matrix over the failure-recovery walkthrough.) **Context:** Payment Orchestration System. Stack: Java (Spring Boot), Oracle, Kafka, Hazelcast.

---

## 1. Why this exists

The orchestrator is deliberately fail-blocked: when it cannot prove an
outcome it parks state and holds money reserved rather than guessing.
Every parked state **requires a database mutation to leave**, and today
only guarded admin operations (authorized application endpoints, §20) can execute one:

| Dead-end state (v4 terms) | Produced by | What must eventually happen |
|---|---|---|
| `stage_state = BLOCKED` (label NEEDS_REVIEW; reasons RETRY_EXHAUSTED, UNMAPPED_CODE, AMOUNT_MISMATCH, CUTOFF_EXPIRED, ENGINE_INCONSISTENCY, AMENDMENT_PARKED, OPS_PARKED, ESCALATED — §13) | retry exhaustion, unmapped codes, mismatch defects, amendment parks, §9.3 escalation | retry / reject / supersede — a CAS transition under the §10.1 release guard; reject/supersede release the reservation |
| Stuck reservation | active request not progressing (§3 / §15 stuck-reservation alert) | supersede/close — §3 makes this a *required* capability, NON-WAIVABLE per the §20 minimal exit set (guarded admin operation at MVP, §20-2) |
| Aged `MAYBE_SUBMITTED` (label UNKNOWN; escalates once per episode to BLOCKED(ESCALATED) on the maybe_since clock, §9.3) | ambiguous POST outcomes, lease expiry, DUPLICATE_REQUEST answers | the §9.3 action set (see O6–O10 below) — **never** a plain release: terminal-negatives on MAYBE/SUBMITTED rows are forbidden unless evidence-driven (§10.1/§9.4) |
| Overpay latch (`overpay_blocked`, §13) | confirmed > required | today: scope ignored forever (§13 one-way door); ops annotates via the `ops_annotation` field (§2.1, §20-4) — **no state change, no clearing** (clearing is FUTURE, tied to §19.2) |
| Amendment tie (AMENDMENT_TIE_CONFLICT, §6.7) | two snapshots share an ordering value with DIFFERING payloads — the guard cannot pick a winner, and a verbatim upstream resend ties again forever | REPROCESS-SNAPSHOT (O12, REVISED 2026-07-11): the payload lives durably in the upstream-populated XML STORE (§6.0 transport note — Kafka carries only the storage id); the tie-conflict record holds identifiers + a masked diff, and O12 re-fetches the adjudicated snapshot BY ID and re-runs the normal §6.1 fan-out with the ordering check relaxed to ≥ for exactly the recorded tied value |

~~Parked feed event~~ — **removed**: no parked-event store exists (§2.3); recovery is query-based (§9).

The interim model — ops reads the card, verifies in other systems, and
executes guarded admin operations — is the accepted MVP (§20). This
console proposal is about replacing raw-endpoint ergonomics with an
audited UI, not about adding capability.

## 2. Design principles

1. **No new mutation semantics.** Every console action executes the
   *same* conditional CAS UPDATE + obligation-lock flow the
   orchestrator uses (§3, §4, §10, §11). Row count 0 = "state moved
   under you" = action rejected, re-fetch.
2. **The release guard is absolute (§10.1).** A terminal-negative
   outcome (REJECT / SUPERSEDE / CANCEL) is permitted only when
   `submission_state = NOT_SUBMITTED`, or driven by an authoritative
   engine negative, or executed via the §9.3
   apply-platform-verified-outcome operation (dual-control, evidence
   flag). The console disables release actions for MAYBE/SUBMITTED
   rows (§9.3 rule) — and the §10.3 trigger backstop would refuse them
   anyway. The console never disables a trigger or guard.
3. **repost_permitted gates every re-POST path (§7.0).** Retry,
   un-park, and downgrade actions fire only where the gate passes
   (divergent_payload_at NULL, pre-cutoff, amount not stale on a MAYBE
   row, freeze off, active row). DECIDED (closes the old open question
   5): the payment cutoff always wins; the ONLY overridable term is
   amount staleness, via the dual-control override (O8). blocked_reason
   is display/queue-routing only — no action keys on it (§10.1).
4. **Fail-safe bias preserved.** Nothing in the console creates or
   submits a payment. The only paths that lead to money moving again
   are the §6.4-retry-guarded retry (O1), the §9.2 downgrade re-POST
   lane (O7/O8, same-key, fresh assembly §7.0), and §6.8's standing
   re-evaluation after a legal release.
5. **Every action survives DR.** No local journal exists (§14); each
   action emits the §14 structured log line AND requires an external
   ticket reference — the ticket trail is the record that survives a
   restore (§20-8).
6. **Read wide, write narrow.** The read surface can show everything;
   the write surface is a closed catalog.

## 3. Operation catalog (preconditions on dimension columns)

| # | Operation | Precondition (CAS WHERE, v4 dimensions) | Effect | Guardrail |
|---|---|---|---|---|
| O1 | Retry a BLOCKED request | outcome IS NULL ∧ stage_state = BLOCKED ∧ submission_state = NOT_SUBMITTED; POST-stage exits additionally pass repost_permitted (§7.0) | → SAME-stage `RETRY_WAIT`, next_retry_at set per policy (L7); an ENRICH-blocked row re-enriches, never skips to POST (§10.5) | 4-eyes |
| O2 | Reject a BLOCKED request | as O1 (release guard: NOT_SUBMITTED only) | → outcome = REJECTED; exactly one marker set (L9); reservation released (§3); re-derived (§4) | 4-eyes (releases money) |
| O3 | Supersede/close a stalled request | outcome IS NULL ∧ submission_state = NOT_SUBMITTED (the §3 required operation; FORBIDDEN on MAYBE/SUBMITTED unless evidence-driven) | → outcome = SUPERSEDED; reservation released; §6.8 may create a right-sized successor | 4-eyes (releases money) |
| ~~O4~~ | ~~Replay parked feed event~~ | — | **OBSOLETE** (no parked-event store, §2.3) | — |
| ~~O5~~ | ~~Discard parked feed event~~ | — | **OBSOLETE** | — |
| O6 | Trigger the resolver now | outcome IS NULL ∧ submission_state IN (MAYBE_SUBMITTED, SUBMITTED) | one §9 status-query cycle via the §9.5 ops-triggered mode; outcome applied through the normal evidence path; no direct state mutation | operator (§20-3 open question) |
| O7 | Ops-triggered §9.2 downgrade (same-key re-POST) | MAYBE_SUBMITTED ∧ past trust-age ∧ repost_permitted passes | → POST · RETRY_WAIT (SUB stays MAYBE); next_retry_at = now, attempt reset (§7.4 downgrade class) | 4-eyes (initiates a wire attempt) |
| O8 | Dual-control stale-amount re-POST | MAYBE_SUBMITTED ∧ repost_permitted fails ONLY on the amount-staleness term (§7.0 override — the single overridable term) | → POST · RETRY_WAIT · MAYBE; knowing re-POST of the request's own immutable amount | strict dual-control |
| O9 | Request platform-side formal rejection (TL-10) | BLOCKED/aged MAYBE row | external ask to the platform; the negative flows back as authoritative feed/query evidence — the CLEAN exit | operator (records the ask) |
| O10 | Apply platform-verified outcome | active ∧ MAYBE/SUBMITTED; refuses CLAIMED and terminal rows and amount mismatch | invokes the **existing MVP audited operation** (authorized application endpoint — §9.3, §16.6-8): verified EXECUTED (+confirmed, SUB=SUBMITTED, amount equality) or REJECTED (marker + release); evidence flag set legitimately; every use alerts (§15) | dual-control ENFORCED BY THE OPERATION (§9.3 two-step approval workflow); ticket mandatory |
| O11 | Retry-after-provider-reject (clear marker) | provider_rejected marker live (≥2 = ops-only clear) | records decision, clears the marker, §6.8 creates a FRESH successor (new key) | **FUTURE** — pending PO approval (§19.3, §18 PO-7) |
| O12 | Reprocess stored snapshot (**rounds 3–5: SERVER-VERIFIED, DIGEST-BOUND, CONSUME-AT-START** — closes walkthrough U-9 with no payload storage and no caller-supplied ordering) | input = XML storage id ONLY at APPROVAL time (from a recorded AMENDMENT_TIE_CONFLICT, or a corrected DLT document — always a NEW immutable id/version per ask 8); EXECUTION input = the §9.3 approval_id | **TRADE-level**: approval fetches + validates the snapshot and binds its canonical digest (§9.3); execution re-fetches by id (§6.0 transport note — the payload is NEVER a parameter, a log field, or a new store), recomputes the digest and HARD-REFUSES on mismatch BEFORE consumption or locks, consumes the approval AT START (round 5 — a crash mid-fan-out is remedied by a NEW approval of the same document; convergence applies only the remainder), verifies document.business_id, then enters the §6.1 ADMISSION gate (round 5): ≥ relaxation AT ADMISSION iff the FETCHED document's ordering equals the trade watermark AND its digest differs (§6.7 — fabrication impossible; a non-tying document gets the ordinary strictly-newer guard; OLDER than the trade watermark → refused whole, even approved); admission updates trade_snapshot_state (for a trade-reference-only tie that update IS the application), then per block, under that obligation's lock: §20-10 rules → apply → set upstream_ordering (idempotent) → §6.8 re-evaluation. Re-run after apply is digest-equal at admission and no-ops (single-use by construction); §6.4/§6.5/§6.8 guards and I6 apply unchanged. A purged xml id → clean refusal (ask 8 retention). Rounds 6–7: every block tx passes the trade-snapshot FENCE (overtaken by newer live intake → remaining blocks abandoned per §6.1 block-level supersession — stated on the approval screen; re-approval of the stale document refused — correct); completed_at + per-block summary stamped on the approval record; §15 alerts on consumed-without-completion | 4-eyes ALWAYS (can initiate money movement via §6.8) |
| — | Overpay acknowledge/annotate | latch set | writes `ops_annotation` (§2.1) — display only, **no state change**, latch never cleared (§13) | operator |
| — | Returned-funds adjustment | — | **FUTURE** — blocked on §19.2 | — |
| — | Posting-freeze flip (kill switch) | — | role-controlled Hazelcast toggle EXISTS today (§16.1); a dedicated audited surface is §20-6 — capability is not blocked on this console | out of console scope |

O1–O3, O7, O8, O12 change money-relevant state or initiate wire
attempts → 4-eyes. O6/O9/annotation push events through existing
idempotent paths → single operator. O10's dual control lives in the
operation itself (§9.3 approval workflow) — the console only fronts the two authenticated
approvals.

### 3.1 Coverage matrix — every ops-action scenario in the failure walkthrough

Derived from `failure-recovery-walkthrough.md` (2026-07-10): every
scenario whose recovery reaches T3 (ops action) maps to a catalog
operation or a deliberately-external surface. This is the check that
the catalog is COMPLETE — re-run it whenever the walkthrough changes.

```text
O1  retry             E-2, E-4 (re-enrich); P-7, P-11, B-6 (re-POST,
                      repost_permitted-gated, next window)
O2  reject            P-11 (give-up branch), M-6 (scope abandoned)
O3  supersede/close   M-5 stuck reservation, M-6 (the §3 required op)
O6  resolve-now       R-1, R-3 triage, U-12, B-1 investigation, §5.2
                      ops-triggered key-set mode (post-MVP DR)
O7  downgrade re-POST P-2 tail, R-2 (after trust-age, gate passes)
O8  stale re-POST     U-12 amendment-parked MAYBE (the one override)
O9  TL-10 ask         R-3, U-12 — the clean external negative
O10 verified outcome  R-3, P-6, H-2/H-3 — the guaranteed un-wedge
                      (§18 BLOCKING item 3; exists at MVP regardless)
O11 clear reject      P-10, M-8, B-4 — FUTURE, pending PO-7
O12 reprocess snapshot U-9 (tie adjudication, xml-id based) + the
                      §6.6 DLT re-trigger (corrected document = NEW immutable id/version, ask 8)
ann annotation        M-2, M-3, M-4 overpay acknowledgement

OUTSIDE this console, by design:
freeze flip           P-15, H-6 — role-controlled Hazelcast toggle
                      exists today (§16.1); §20-6 is only a nicer surface
DLT replay            U-5, U-18 — Kafka platform tooling (§16.2),
                      keys preserved
platform recon        C-6, C-7, M-1, B-5 — money-truth divergence
                      reconciles in the payment platform (§19.2 policy);
                      the future manual-adjustment op is the eventual
                      endpoint, deliberately absent at MVP
corrected message     U-3, U-6, E-3, P-9, M-7 — upstream is the fix;
                      no console op can or should substitute for it
```

## 4. Architecture

```text
┌─────────────┐   OAuth2/SSO   ┌──────────────────┐   same schema   ┌────────┐
│  Ops UI     │ ─────────────► │  ops-console-api  │ ──────────────► │ Oracle │
│  (SPA)      │                │  (Spring Boot)    │  CAS + oblig.   │        │
└─────────────┘                │                   │  lock, read-only│        │
                               │  - queue queries  │  views for lists│        │
                               │  - action executor│  + §9.3 stored  │        │
                               │  - approval store │    operation    │        │
                               └──────────────────┘                 └────────┘
```

- **Separate deployable**, same database, same transaction primitives —
  ideally a shared transition library so console and orchestrator can
  never disagree about a WHERE clause; O10 calls the §9.3 stored
  operation (never reimplements it).
- **AuthN:** corporate SSO (OAuth2/OIDC). **AuthZ roles:** `viewer`,
  `operator` (O6, O9, annotations), `approver` (second pair of eyes
  for O1–O3/O7/O8; cannot initiate what they approve). O10 requires
  two distinct authenticated identities verified by the operation (§9.3 approval workflow).
- **Approval flow:** initiator submits action + reason + ticket ref →
  pending row → approver confirms → execution. Pending approvals
  expire (24h); the CAS re-check at execution time is the final
  arbiter; row-count-0 is surfaced to both parties.
- **Audit:** every submit/approve/execute/reject emits the §14
  structured log line, `trigger_source = MANUAL_OPS:<operator-id>`
  (O10's operation emits `trigger_source = OPS_PLATFORM_VERIFIED`,
  §9.3), plus the ticket reference. Console DB keeps only approval
  workflow rows (console state, not payment state).

## 5. UI surface (three screens)

**S1 — Queues dashboard.** One table per dead-end class, keyed on
dimension columns; blocked_reason used for grouping/display only:
BLOCKED by reason with the **ESCALATED (money-critical) class ranked
first** (§2.2); stuck reservations by age; MAYBE_SUBMITTED by
maybe_since age; overpay latches (round 10: no cutoff proximity —
the engine owns the calendar). Each row:
scope key, business_id, amount, age (episode-anchor clocks, §15),
last error, §10.4 label chip, deep-link to S2. This is the screen the
§15 alerts link to. (Parked-events queue removed.) Tie-conflict note
(O12): ties have NO queue table — there is no DB state to derive one
from (the snapshot was acked and dropped); the AMENDMENT_TIE_CONFLICT
alert deep-links straight to the S3 flow for O12, carrying the
tie-conflict record reference. Multi-payment note
(§1 contract facts): a business_id can map to SEVERAL obligations —
queues stay keyed per obligation, and S1/S2 offer a business_id
filter/grouping so an operator can see a whole trade's payments
together (§12).

**S2 — Payment detail.** The full story of one obligation: header
(scope key, required/committed/confirmed, derived step status, active
exception per §4.2 ranks), request list showing the **dimension tuple
(stage · stage_state · submission_state · outcome) plus the §10.4
display label**, retry/anchor state, idempotency key, UETR,
provider_reference; transition history read *from the log store* (§14
lines by request_id — the console keeps no journal). Action buttons
appear only where the operation catalog's dimension preconditions hold
and only for the caller's role; for MAYBE/SUBMITTED rows the release
actions are absent and the §9.3 action set (O6–O10) is offered
instead.

**S3 — Action flow.** Modal per action: reason (mandatory), ticket
reference (mandatory, validated), a **preview of effect** computed
read-only under the obligation lock ("SUPERSEDE releases 20.00 EUR;
shortfall becomes 20.00; step remains IN_PROGRESS"), then submit →
pending approval. O10 additionally captures the platform-records
evidence reference and the second approver. O12's preview is
per-block ("block (T1, ACC-1, EUR): required 100 → 120, shortfall
+20, successor WILL be created; block (T2, ACC-2, EUR): identical —
no-op") so the approver sees exactly which payments the tied snapshot
would move.

## 6. API sketch

```text
GET  /queues/blocked | /stuck | /maybe-aged | /overpay
GET  /obligations/{scopeKey}                 → S2 payload (dimensions + labels)
GET  /obligations/{scopeKey}/log-timeline    → from log store (§14 lines)
POST /requests/{id}/actions                  {type: RETRY|REJECT|SUPERSEDE, reason, ticketRef}
POST /requests/{id}/resolve-now              {reason, ticketRef}                  (O6)
POST /requests/{id}/downgrade-repost         {reason, ticketRef}                  (O7; repost_permitted-gated)
POST /requests/{id}/stale-amount-repost      {reason, ticketRef}                  (O8; staleness override only; 4-eyes
                                                                                     via the §9.3 approval workflow —
                                                                                     round 5: NEVER an approver identity
                                                                                     in the body)
POST /requests/{id}/platform-verified-outcome {outcome: EXECUTED|REJECTED, evidenceRef, ticketRef} (O10 → §9.3 operation; approvals via the §9.3 two-step workflow, not inline)
POST /trades/{businessId}/reprocess-snapshot {xmlStorageId, reason, ticketRef}       (O12 rounds 3–5; trade-level —
                                                                                     INITIATES the §9.3 approval: fetch +
                                                                                     validate + bind digest; payload
                                                                                     fetched from the XML store by id,
                                                                                     never from the body; EXECUTION runs
                                                                                     by approval_id after the second
                                                                                     approval — consume-at-start, §6.1
                                                                                     admission entry, tie recomputed
                                                                                     SERVER-side, no ordering parameter)
POST /approvals/{id}/approve | /reject
POST /approvals/{id}/execute                 (round 5: the ONE execution entry —
                                              input is the approval_id only)
```

All mutating endpoints: idempotency via the approval-row id;
optimistic `If-Match` on the request `version`; every response
surfaces the CAS row count. (Parked-event endpoints removed.)

### 6.1 Execution semantics per operation

Every mutation is the §11 two-tier shape — obligation lock FIRST,
then a conditional CAS whose WHERE carries the dimension
preconditions; row count 0 = state moved (surface 409, zero side
effects); row count 1 → money movement per §3 + §6.8 re-evaluation +
§4 re-derivation + the §14 log line, all in ONE transaction. The
canonical skeleton, claim mechanics, and trap list live in the
playbook's `24-implementation-mechanics.md` (M1–M7); this section
pins each operation's specifics:

```text
O1  SET stage_state=RETRY_WAIT, blocked_reason=NULL, next_retry_at
    per the ops-retry policy class (L7)
    WHERE outcome IS NULL ∧ stage_state='BLOCKED'
      ∧ submission_state='NOT_SUBMITTED' ∧ divergent_payload_at IS NULL.
    POST-stage rows: remaining repost_permitted terms (§7.0 —
    cutoff, freeze) checked in code; stage NEVER changes (an
    ENRICH-blocked row re-enriches, §10.5). No money movement.
O2  SET outcome='REJECTED' WHERE outcome IS NULL ∧ stage_state=
    'BLOCKED' ∧ submission_state='NOT_SUBMITTED' (§10.1). Same
    transaction: −committed_amount; provider_rejected marker +
    count (L9 totality); §6.8 (the live marker correctly blocks an
    automatic successor).
O3  SET outcome='SUPERSEDED' WHERE outcome IS NULL ∧
    submission_state='NOT_SUBMITTED' ∧ stage_state<>'CLAIMED'.
    −committed_amount; §6.8 may create the right-sized successor.
O6  No payment-table mutation: hands the key set to the resolver's
    §9.5 ops-triggered mode; answers apply via the normal
    evidence-guarded CAS. Shares the TL-13 query budget.
O7  SET stage='POST', stage_state='RETRY_WAIT', blocked_reason=NULL,
    next_retry_at=now, attempt_count=0 (the §9.2 downgrade class)
    WHERE outcome IS NULL ∧ submission_state='MAYBE_SUBMITTED'
      ∧ stage_state<>'CLAIMED' ∧ divergent_payload_at IS NULL.
    Code, under the obligation lock: past trust-age + FULL
    repost_permitted incl. amount-vs-shortfall.
O8  Same target write as O7; permitted iff repost_permitted fails
    SOLELY on the amount-staleness term (verified under the lock);
    strict dual control. Cutoff/divergence/terminal never override.
O9  No payment-table mutation: records the TL-10 ask (console audit
    row + §14 line); the negative, if granted, arrives as normal
    feed/query evidence.
O10 Invoke the §9.3 audited operation — the authorized application
    endpoint (spec = §16.6 artifact 8 / CA-9; 2026-07-11 boundary:
    Java service, never a PL/SQL reimplementation). The console
    collects inputs only — it NEVER reimplements.
O11 (FUTURE, PO-7) Obligation-level under the lock: clear the
    provider_rejected fields + reset the count; §6.8 then creates a
    FRESH successor (new deterministic key — correct after a
    definitive reject).
O12 (round 3 — SERVER-VERIFIED) Fetch the snapshot XML from the
    store by id (§6.0 transport note), verify document.business_id
    matches, then per payment block in sorted tuple order:
    obligation lock → apply amounts with the tie condition
    RECOMPUTED server-side (≥ applies iff the FETCHED document's
    ordering equals upstream_ordering AND its payload differs —
    §6.7; NO caller-supplied ordering exists) → set
    upstream_ordering (idempotent) → normal §6.4/§6.5/§6.8
    consequences. Payload always from the STORE, never a request
    parameter or log field; re-run no-ops; purged id → clean
    refusal.
ann Single UPDATE of payment_obligation.ops_annotation (read-model
    field; no derivation input, no state change).
```

Reads (queues/detail) are lock-free MVCC queries; the canonical
queue-view SQL ships with playbook cards OP-04a–e (the §20 interim
surface — these views exist BEFORE this console). The S3 effect
preview runs read-only but takes the obligation lock briefly so its
numbers match what execution would compute.

## 7. Phasing

| Phase | Scope | Value |
|---|---|---|
| — | **Already at MVP, outside this console:** the NON-WAIVABLE §20 minimal exit set — the §9.3 verified-outcome operation + drill (also §18-3), supersede/close, reprocess-snapshot — plus the Q29-waivable ergonomics (ops retry, ops reject, annotation, four queue views) as authorized application endpoints (playbook RG-05 + OP-04a-e); role-controlled posting-freeze toggle (§16.1) | the three covered dead-end classes (MAYBE/SUBMITTED rows, provably-unsent active requests, ties) have audited exits before any console ships; marker-only and latched scopes are documented STOP STATES (§20 round-4 exit honesty) |
| P0 | S1 + S2 read-only (queues, detail, log timeline) | kills "where do I even look"; no approval machinery; can ship first |
| P1 | O6 resolve-now, O9 TL-10 ask capture, overpay annotation (`ops_annotation`) | non-monetary, single-operator |
| P2 | O1–O3 with 4-eyes; O7/O8 downgrade lane; O10 as a UI wrapper over the existing audited endpoint; O12 reprocess-snapshot | the money-touching operations; requires the PO decisions below |
| P3 | O11 retry-after-provider-reject (needs §18 PO-7 approval); returned-funds adjustment (blocked on §19.2) | future money-policy operations |

## 8. Open questions for the PO discussion (aligned to §20)

1. Who are the ops users (team, count, roles) and what SLA do the
   queues carry — especially the BLOCKED(ESCALATED) money-critical
   class, which §9.3's tier-2 re-page assumes is worked promptly?
2. Is 4-eyes required by policy for O1–O3/O7, or is single-operator
   with mandatory ticket acceptable? (O8 and O10 are dual-control by
   design — not negotiable here; O10's dual control is enforced by the
   operation, §9.3.)
3. Which ticket system anchors the audit trail (§20-8), and is the
   reference validated against its API or free-text?
4. Retention for console approval/action records.
5. ~~May retry bypass the payment cutoff?~~ **ANSWERED in v4:** the
   cutoff always wins (§7.0 term); nothing overrides it. The ONLY
   overridable term is amount staleness (O8, dual-control).
6. Does the overpay acknowledgement need to be visible on the card?
   (`ops_annotation` is a §2.1 read-model field, so surfacing it is a
   card-contract question — TL-2 / §20-4.)
7. §20-3: may ops trigger the resolver ahead of schedule (O6) freely,
   or rate-limited (the §9.5 sweep budget shares the engine's
   query-API quota, TL-13)?
8. O12 (tie application, §20-10): confirm the interim form — until
   the console ships, is the controlled admin operation acceptable
   for what §6.7 expects to be a rare event (ties disappear entirely
   once upstream ask 1's explicit sequence field arrives)? And who
   adjudicates WHICH tied snapshot is the business truth — ops alone,
   or ops + upstream confirmation?
