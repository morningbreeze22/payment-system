# Ops Console — Proposal

**Status:** PROPOSAL — FUTURE implementation, pending PO discussion (`requirment-v4.md` §20).
**PO DECISION (recorded in §20):** the MVP ships WITHOUT an ops execution surface. Interim: dead-end states are exited via controlled manual database procedures under the §10.1 release guard and §10.3 trigger backstops. Exactly ONE procedure is REQUIRED at MVP — the §9.3 **apply-platform-verified-outcome** audited stored procedure (§18 BLOCKING item 3). It exists independently of this console; the console would only ever be a nicer, audited surface over the same machinery.
**Baseline:** `requirment-v4.md` (v4, factored state model). This revision supersedes the v2-era draft: O4/O5 (replay/discard parked event) are **OBSOLETE** — no parked-event store exists (§2.3); unmatched feed events are log/metric/ack and outcomes recover via the §9 status-query sweep. All state displays use the §10.4 labels; all action preconditions are expressed on the dimension columns (stage, stage_state, submission_state, outcome) — never on labels and never on blocked_reason (§10.1).
**Updated:** 2026-07-10 — revised against `failure-recovery-walkthrough.md`: added **O12 apply-tied-amendment** (the walkthrough's U-9 found §6.7's "manual application" had no operation anywhere), added the §3.1 coverage matrix proving the catalog covers every T3 (ops-action) scenario, and recorded which recoveries deliberately live OUTSIDE this console. **Context:** Payment Orchestration System. Stack: Java (Spring Boot), Oracle, Kafka, Hazelcast.

---

## 1. Why this exists

The orchestrator is deliberately fail-blocked: when it cannot prove an
outcome it parks state and holds money reserved rather than guessing.
Every parked state **requires a database mutation to leave**, and today
only guarded manual procedures can execute one:

| Dead-end state (v4 terms) | Produced by | What must eventually happen |
|---|---|---|
| `stage_state = BLOCKED` (label NEEDS_REVIEW; reasons RETRY_EXHAUSTED, UNMAPPED_CODE, AMOUNT_MISMATCH, CUTOFF_EXPIRED, ENGINE_INCONSISTENCY, AMENDMENT_PARKED, OPS_PARKED, ESCALATED — §13) | retry exhaustion, unmapped codes, mismatch defects, amendment parks, §9.3 escalation | retry / reject / supersede — a CAS transition under the §10.1 release guard; reject/supersede release the reservation |
| Stuck reservation | active request not progressing (§3 / §15 stuck-reservation alert) | supersede/close — §3 makes this a *required* capability (guarded procedure at MVP, §20-2) |
| Aged `MAYBE_SUBMITTED` (label UNKNOWN; escalates once per episode to BLOCKED(ESCALATED) on the maybe_since clock, §9.3) | ambiguous POST outcomes, lease expiry, DUPLICATE_REQUEST answers | the §9.3 action set (see O6–O10 below) — **never** a plain release: terminal-negatives on MAYBE/SUBMITTED rows are forbidden unless evidence-driven (§10.1/§9.4) |
| Overpay latch (`overpay_blocked`, §13) | confirmed > required | today: scope ignored forever (§13 one-way door); ops annotates via the `ops_annotation` field (§2.1, §20-4) — **no state change, no clearing** (clearing is FUTURE, tied to §19.2) |
| Amendment tie (AMENDMENT_TIE_CONFLICT, §6.7) | two snapshots share an ordering value with DIFFERING payloads — the guard cannot pick a winner, and an upstream resend ties again forever | manual application of the chosen snapshot (O12, NEW). Unlike the rows above this is **not a DB state**: the incoming snapshot was acked and dropped, so the work item is the tie-conflict record itself (alert + §14 log line carrying the canonicalized payload — §6.7 executability requirement / §20-10) |

~~Parked feed event~~ — **removed**: no parked-event store exists (§2.3); recovery is query-based (§9).

The interim model — ops reads the card, verifies in other systems, and
executes guarded DB procedures — is the accepted MVP (§20). This
console proposal is about replacing raw-procedure ergonomics with an
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
   apply-platform-verified-outcome procedure (dual-control, evidence
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
| O10 | Apply platform-verified outcome | active ∧ MAYBE/SUBMITTED; refuses CLAIMED and terminal rows and amount mismatch | invokes the **existing MVP audited stored procedure** (§9.3, §16.6-8): verified EXECUTED (+confirmed, SUB=SUBMITTED, amount equality) or REJECTED (marker + release); evidence flag set legitimately; every use alerts (§15) | dual-control ENFORCED BY THE PROCEDURE; ticket mandatory |
| O11 | Retry-after-provider-reject (clear marker) | provider_rejected marker live (≥2 = ops-only clear) | records decision, clears the marker, §6.8 creates a FRESH successor (new key) | **FUTURE** — pending PO approval (§19.3, §18 PO-7) |
| O12 | Apply tied amendment (**NEW** — closes walkthrough U-9) | a recorded AMENDMENT_TIE_CONFLICT for the trade (§6.7); input = the tie-conflict record's preserved canonicalized snapshot payload, NEVER free-typed amounts | **TRADE-level** (a tie is a whole-snapshot verdict — §6.7 snapshot note: all blocks resolve together): re-runs the §6.1 fan-out for the chosen snapshot with the strictly-newer ordering check relaxed to ≥ for exactly the recorded tied ordering value; per block, under that obligation's lock: apply amounts → set upstream_ordering to the tied value (idempotent) → §6.8 re-evaluation. Blocks whose payload already matches no-op; §6.4/§6.8 guards (retry-guard, latch, markers, I6) apply unchanged — this op adjudicates ORDERING only, it bypasses no money guard | 4-eyes (can initiate money movement via §6.8) |
| — | Overpay acknowledge/annotate | latch set | writes `ops_annotation` (§2.1) — display only, **no state change**, latch never cleared (§13) | operator |
| — | Returned-funds adjustment | — | **FUTURE** — blocked on §19.2 | — |
| — | Posting-freeze flip (kill switch) | — | role-controlled Hazelcast toggle EXISTS today (§16.1); a dedicated audited surface is §20-6 — capability is not blocked on this console | out of console scope |

O1–O3, O7, O8, O12 change money-relevant state or initiate wire
attempts → 4-eyes. O6/O9/annotation push events through existing
idempotent paths → single operator. O10's dual control lives in the
procedure itself — the console only collects the two authenticated
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
O12 tied amendment    U-9 — was uncovered before this revision
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
                               │  - approval store │    procedure    │        │
                               └──────────────────┘                 └────────┘
```

- **Separate deployable**, same database, same transaction primitives —
  ideally a shared transition library so console and orchestrator can
  never disagree about a WHERE clause; O10 calls the §9.3 stored
  procedure (never reimplements it).
- **AuthN:** corporate SSO (OAuth2/OIDC). **AuthZ roles:** `viewer`,
  `operator` (O6, O9, annotations), `approver` (second pair of eyes
  for O1–O3/O7/O8; cannot initiate what they approve). O10 requires
  two distinct authenticated identities passed to the procedure.
- **Approval flow:** initiator submits action + reason + ticket ref →
  pending row → approver confirms → execution. Pending approvals
  expire (24h); the CAS re-check at execution time is the final
  arbiter; row-count-0 is surfaced to both parties.
- **Audit:** every submit/approve/execute/reject emits the §14
  structured log line, `trigger_source = MANUAL_OPS:<operator-id>`
  (O10's procedure emits `trigger_source = OPS_PLATFORM_VERIFIED`,
  §9.3), plus the ticket reference. Console DB keeps only approval
  workflow rows (console state, not payment state).

## 5. UI surface (three screens)

**S1 — Queues dashboard.** One table per dead-end class, keyed on
dimension columns; blocked_reason used for grouping/display only:
BLOCKED by reason with the **ESCALATED (money-critical) class ranked
first** (§2.2); stuck reservations by age; MAYBE_SUBMITTED by
maybe_since age (with cutoff proximity); overpay latches. Each row:
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
POST /requests/{id}/stale-amount-repost      {reason, ticketRef, secondApprover}  (O8; staleness override only)
POST /requests/{id}/platform-verified-outcome {outcome: EXECUTED|REJECTED, evidenceRef, ticketRef, approvers[2]} (O10 → §9.3 procedure)
POST /trades/{businessId}/apply-tied-amendment {tieRecordRef, reason, ticketRef}     (O12; trade-level — payload comes
                                                                                     from the tie-conflict record, never
                                                                                     from the request body)
POST /approvals/{id}/approve | /reject
```

All mutating endpoints: idempotency via the approval-row id;
optimistic `If-Match` on the request `version`; every response
surfaces the CAS row count. (Parked-event endpoints removed.)

## 7. Phasing

| Phase | Scope | Value |
|---|---|---|
| — | **Already at MVP, outside this console:** the §9.3 apply-platform-verified-outcome stored procedure + drill (§18 BLOCKING item 3); guarded manual procedures for O1–O3-equivalents; role-controlled posting-freeze toggle (§16.1) | the guaranteed MAYBE-row terminal exit exists before any console ships |
| P0 | S1 + S2 read-only (queues, detail, log timeline) | kills "where do I even look"; no approval machinery; can ship first |
| P1 | O6 resolve-now, O9 TL-10 ask capture, overpay annotation (`ops_annotation`) | non-monetary, single-operator |
| P2 | O1–O3 with 4-eyes; O7/O8 downgrade lane; O10 as a UI wrapper over the existing procedure; O12 tied-amendment application | the money-touching operations; requires the PO decisions below |
| P3 | O11 retry-after-provider-reject (needs §18 PO-7 approval); returned-funds adjustment (blocked on §19.2) | future money-policy operations |

## 8. Open questions for the PO discussion (aligned to §20)

1. Who are the ops users (team, count, roles) and what SLA do the
   queues carry — especially the BLOCKED(ESCALATED) money-critical
   class, which §9.3's tier-2 re-page assumes is worked promptly?
2. Is 4-eyes required by policy for O1–O3/O7, or is single-operator
   with mandatory ticket acceptable? (O8 and O10 are dual-control by
   design — not negotiable here; O10's dual control is enforced by the
   procedure, §9.3.)
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
   the console ships, is the controlled manual procedure acceptable
   for what §6.7 expects to be a rare event (ties disappear entirely
   once upstream ask 1's explicit sequence field arrives)? And who
   adjudicates WHICH tied snapshot is the business truth — ops alone,
   or ops + upstream confirmation?
