# Event-Model Schema Design

> Status: DRAFT (see `00-README.md`). Amount semantics, identity
> derivation, and contract facts are inherited unchanged from
> `requirment-v4.md` (§1 scope tuple, §5.1 identity, §18-1 provider
> facts). This document defines HOW they are stored, not WHAT they mean.

## 1. Overview — three physical structures

| Structure | Kind | Role |
|---|---|---|
| `PAYMENT_EVENT` | THE table (append-only) | the only authoritative store: everything that ever happened to a payment, fenced per payment |
| `TRADE_SNAPSHOT_STATE` | small mutable table (unchanged from the baseline design) | trade-level snapshot admission: newest-wins ordering anchor + payload digest, ONE row per trade |
| `PAYMENT_STATUS_PROJECTION` | derived cache (rebuildable) | how scanners, the feed matcher, and the UI FIND payments; never read by a money decision |

Everything else — required amount, paid amount, reservation, markers,
retry state, MAYBE, blocked/parked, escalation — is **derived by the
fold** (§4 below), never stored authoritatively.

## 2. `PAYMENT_EVENT` — the table

```sql
CREATE TABLE PAYMENT_EVENT (
  ID                 NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  PAYMENT_KEY        VARCHAR2(200)  NOT NULL,
  VERSION            NUMBER(10)     NOT NULL,
  EVENT_TYPE         VARCHAR2(40)   NOT NULL,
  UPSTREAM_ORDERING  NUMBER(19),
  AMOUNT             NUMBER(18,3),          -- same domain/scale rules as the baseline amount columns
  IDEMPOTENCY_KEY    VARCHAR2(128),
  PAYLOAD_HASH       VARCHAR2(64),
  UETR               VARCHAR2(64),
  PROVIDER_REFERENCE VARCHAR2(128),
  PROVIDER_CODE      VARCHAR2(64),
  EVIDENCE_SOURCE    VARCHAR2(16),          -- SYNC_RESPONSE / QUERY / FEED / OPS / SYSTEM
  ACTOR              VARCHAR2(64)   NOT NULL,  -- SYSTEM / SCANNER / RESOLVER / OPS:<user> / MIGRATION
  DETAIL             VARCHAR2(1000),        -- human-readable context; NEVER read by the fold
  CREATED_AT         TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,

  -- identity is claimed exactly once: only the opening event carries it
  -- into this generated column, and no writer can supply the column
  IDEM_CLAIM         VARCHAR2(128)  GENERATED ALWAYS AS
                       (CASE WHEN EVENT_TYPE = 'REQUEST_OPENED' THEN IDEMPOTENCY_KEY END),

  CONSTRAINT PE_FENCE_UQ  UNIQUE (PAYMENT_KEY, VERSION),   -- THE fence
  CONSTRAINT PE_IDEM_UQ   UNIQUE (IDEM_CLAIM),             -- identity is write-once
  CONSTRAINT PE_TYPE_CK   CHECK (EVENT_TYPE IN (
      'REQUIRED_AMOUNT_SET','SNAPSHOT_INVALID_MARKED',
      'REQUEST_OPENED','ENRICH_FAILED','POST_STARTED','POST_RESULT_RECORDED',
      'QUERY_RESULT_RECORDED','DOWNGRADED_FOR_REPOST',
      'OUTCOME_RECORDED','SETTLED','SETTLEMENT_MISMATCH_RECORDED',
      'ESCALATION_MARKED','OPS_VERIFIED_OUTCOME_APPLIED','OPS_RETRY_REARMED',
      'OPS_BLOCKED','OPS_ANNOTATED'))
);
CREATE INDEX PE_KEY_IX  ON PAYMENT_EVENT (PAYMENT_KEY, VERSION);
CREATE INDEX PE_UETR_IX ON PAYMENT_EVENT (UETR);            -- feed-matching assist
```

### Why each column exists

| Column | Why |
|---|---|
| `PAYMENT_KEY` | the §1 scope tuple, canonically encoded — one stream per payment. Trade membership is derivable from the key (fan-out and lock-free ordering rules sort by it). |
| `VERSION` | the fence slot. Dense, starts at 1, no gaps by construction (an insert claims exactly `max+1`; a lost race is retried after re-folding). Doubles as the total order of the stream — no reliance on timestamps or identity columns for ordering. |
| `EVENT_TYPE` | closed vocabulary, CHECK-bound (§3). Adding a type is a design change, not a code convenience. |
| `UPSTREAM_ORDERING` | carried only on snapshot-derived events (`REQUIRED_AMOUNT_SET`, `SNAPSHOT_INVALID_MARKED`); the strictly-newer guard and marker unlatching compare against it. |
| `AMOUNT` | the event's amount where meaningful (required amount; request amount; settled amount; mismatched amount). Immutable like every event field — an in-flight request's amount can never change because nothing can rewrite its opening event. |
| `IDEMPOTENCY_KEY` | present on `REQUEST_OPENED` (allocation — see identity rule below) and echoed on the attempt/outcome events that concern that request, so the stream is self-describing. |
| `PAYLOAD_HASH` | §5.1 write-ahead instruction hash, on `REQUEST_OPENED` and on every `POST_STARTED`. Divergence expectation is DERIVED: a re-POST whose hash differs from the previous `POST_STARTED` hash is expected-divergent (re-enrichment happened); the collision handling in the scenarios builds on this being in the durable record BEFORE the wire call. |
| `UETR` / `PROVIDER_REFERENCE` / `PROVIDER_CODE` | provider evidence, recorded on acceptance/result/feed events exactly as the baseline records them (UETR only from acceptance-class responses, per the platform-SDK rule). |
| `EVIDENCE_SOURCE` | which §-channel produced the event — the fold's evidence-precedence rules key on it. |
| `ACTOR` | audit: which component or human appended. `OPS:*` events additionally carry the approval reference in `DETAIL`. |
| `DETAIL` | display/audit text only. THE FOLD NEVER READS IT — this line is a design rule, enforced by the fold's golden vectors. |
| `IDEM_CLAIM` (generated) | makes identity write-once IN THE SCHEMA: two `REQUEST_OPENED` events can never carry the same key, no matter which writer, path, or bug produced them. |

### Identity rule

`IDEMPOTENCY_KEY = derive(payment_key, opening_version)` using the SAME
§5.1 derivation as the baseline, with `request_seq := the version slot
that the REQUEST_OPENED event won`. Because the slot is granted by the
fence exactly once, identity is allocated exactly once — never computed
from `MAX(history)+1`, never reused after crash or restore of anything
except the database itself (a DB restore rewinds versions AND the events
together, so key and payload stay consistent; the §5.2 recovery
discussion applies unchanged).

### Append discipline (the only write path)

```
loop (bounded):
  state  = fold(payment_key)                -- read the stream
  decide                                     -- pure function of state
  try INSERT event(s) at VERSION = state.maxVersion + 1 (, +2 …)
  on PE_FENCE_UQ violation: re-fold and re-decide (the world moved)
```

- Multi-event decisions (e.g. "record outcome AND open the successor")
  append consecutive versions **in one transaction** — atomicity without
  any lock: either both slots are won or the transaction retries.
- The table is INSERT-only by grant (application role has no
  UPDATE/DELETE) **and** by a guard trigger that raises on UPDATE/DELETE
  — the equivalent of the baseline's freeze trigger, protecting history
  against any writer including humans in an incident.

## 3. Event vocabulary — when appended, what it means to the fold

| Event | Appended when | Fold effect |
|---|---|---|
| `REQUIRED_AMOUNT_SET` | an ADMITTED snapshot names this payment (incl. amount 0 = cancel-to-zero; absence-means-cancel per BA-2 produces an explicit 0 here) | `required := amount` if `upstream_ordering` is strictly newer; markers with older ordering unlatch |
| `SNAPSHOT_INVALID_MARKED` | whole-snapshot validation failed at admission — appended to EVERY payment of the trade, in payment_key order | blocks NEW request opening; never touches in-flight work; unlatched by a newer valid `REQUIRED_AMOUNT_SET` |
| `REQUEST_OPENED` | the standing rule decides to pay a shortfall: claims identity + amount + payload hash (write-ahead part 1) | an OPEN request exists; reservation = its amount (derived); at most one open request per payment is a FOLD INVARIANT (and the fence makes racing opens impossible) |
| `ENRICH_FAILED` | enrichment failed; `DETAIL`/`PROVIDER_CODE` say transient vs definitive-invalid | transient: retry timing derives from this event's timestamp + policy; definitive: appended together with `OUTCOME_RECORDED(REJECTED_VALIDATION)` in one tx |
| `POST_STARTED` | immediately BEFORE the wire call (write-ahead part 2 — the posting claim). Its very existence is the durable fact "the wire MAY have been reached" | the request is posting/ambiguous until a result event follows; **no `POST_STARTED` = provably never sent** (this is what makes auto-cancel and safe release provable) |
| `POST_RESULT_RECORDED` | the synchronous response, classified per CA-1: `ACCEPTED` (uetr), `BUSINESS_REJECT`, `DEFINITIVE_REJECT`, `AMBIGUOUS`, `COLLISION`, `UNMAPPED` | drives the §7 classes: accepted→awaiting settlement; business reject→retry per policy (same key); definitive→outcome in same tx; ambiguous→MAYBE; collision→expected/unexpected via hash comparison |
| `QUERY_RESULT_RECORDED` | the resolver asked by OUR key (§9.1): `EXECUTED` / `NOT_FOUND` (+ store age) / `LOOKBACK_EXPIRED` | EXECUTED→outcome same tx; NOT_FOUND young→no change (trust age); NOT_FOUND past trust age→enables the one sanctioned downgrade; LOOKBACK_EXPIRED→stays MAYBE (ops path) |
| `DOWNGRADED_FOR_REPOST` | the §9.2 move: NOT_FOUND past trust age, same key will be re-sent | re-posting becomes legal for the SAME key; audit trail of the only backward transition |
| `OUTCOME_RECORDED` | terminal for a request: `EXECUTED` / `REJECTED_VALIDATION` / `REJECTED_PROVIDER` / `CANCELLED_NOT_SUBMITTED` / `PLATFORM_VERIFIED_*` — with `EVIDENCE_SOURCE` | closes the open request; releases or books its reservation; latches the corresponding marker WITH the ordering current at that time; **the same transaction re-evaluates the standing rule** (successor opens atomically if shortfall remains) |
| `SETTLED` | the feed confirms settlement of the full amount | books confirmed money (idempotent by request key); freezes that request in the fold |
| `SETTLEMENT_MISMATCH_RECORDED` | feed amount ≠ instructed amount (all-or-nothing engine ⇒ defect evidence) | books NOTHING; derived state parks loudly; submission knowledge still tightens (even wrong evidence proves it was sent) |
| `ESCALATION_MARKED` | the MAYBE got old (once per episode) | audit of paging; derived state unchanged (escalation alerts, never mutates) |
| `OPS_VERIFIED_OUTCOME_APPLIED` | the dual-control audited operation applies a platform-verified outcome (§9.3 equivalent; approval reference in DETAIL) | appended together with the matching `OUTCOME_RECORDED(PLATFORM_VERIFIED_*)` in one tx — the only manual door for possibly-moved money |
| `OPS_RETRY_REARMED` / `OPS_BLOCKED` / `OPS_ANNOTATED` | human actions through the ops surface | budget reset / hard block on new opens / display note. All arrive through the SAME fence — there is no privileged write path |

## 4. The canonical fold

One shared implementation (one artifact, semantic-versioned, golden-vector
tested exactly like the baseline's identity derivation). Inputs: the
payment's events ordered by `VERSION`. Outputs (all derived):

```
required_amount        latest REQUIRED_AMOUNT_SET by strictly-newer ordering
snapshot_invalid       latched/unlatched by ordering comparison
markers                validation_failed / provider_rejected (+ their orderings, counts)
open_request           (key, amount, phase: OPEN/POSTING/AWAITING/MAYBE/RETRY_WAIT/PARKED)
                       — from REQUEST_OPENED … outcome-less tail of the stream
paid_total             Σ amounts of DISTINCT request keys with EXECUTED/SETTLED   (idempotent)
reserved               open_request.amount (0 if none)
shortfall              required − paid − reserved   (the standing rule's input)
retry state            attempts used, next due time (event timestamps + policy)
escalation/park state  derived ages + mismatch/exhaustion evidence
provably_unsent        open request with NO POST_STARTED event — the safe-release predicate
```

Fold rules replicate the baseline §4/§6/§7/§9/§10 semantics verbatim —
those sections remain the specification; the fold is their event-sourced
implementation. **Rule: no consumer may re-implement any part of this.**
UI, scanners, resolver, drift checks, and the ops surface all call the
one fold.

## 5. `TRADE_SNAPSHOT_STATE` — unchanged

Exactly the baseline table (one row per trade, overwritten; last accepted
ordering + payload digest; the §6.1 admission transaction is its only
writer). Snapshot admission, whole-document validation, and newest-wins
stay OUTSIDE the event streams; an admitted snapshot then fans out
`REQUIRED_AMOUNT_SET` (or `SNAPSHOT_INVALID_MARKED`) to each payment
stream in sorted payment_key order — each under its own fence, so
redeliveries can never deadlock and a partial fan-out is safely re-run
(appends are ordering-guarded, hence idempotent).

## 6. `PAYMENT_STATUS_PROJECTION` — the find-things cache

```sql
CREATE TABLE PAYMENT_STATUS_PROJECTION (
  PAYMENT_KEY     VARCHAR2(200) PRIMARY KEY,
  FOLD_VERSION    NUMBER(10)  NOT NULL,     -- stream version this row reflects
  PHASE           VARCHAR2(24),             -- derived phase for scanner scoping
  NEXT_ACTION_AT  TIMESTAMP,                -- retry due / query due / escalation due
  UETR            VARCHAR2(64),
  UI_STEP_STATUS  VARCHAR2(16),
  ESCALATED       CHAR(1),
  UPDATED_AT      TIMESTAMP
);
CREATE INDEX PSP_DUE_IX  ON PAYMENT_STATUS_PROJECTION (PHASE, NEXT_ACTION_AT);
CREATE INDEX PSP_UETR_IX ON PAYMENT_STATUS_PROJECTION (UETR);
```

Rules that keep it honest:

1. **Never load-bearing.** A money decision NEVER reads it. Scanners use
   it only to pick CANDIDATES; the action itself folds the stream and
   goes through the fence — a stale projection therefore costs a wasted
   fold, never a wrong payment.
2. Updated after each successful append (same service call, after
   commit); rebuilt at any time by replaying streams. `FOLD_VERSION`
   makes staleness visible and testable.
3. Feed matching: UETR → projection (or the `PE_UETR_IX` fallback) →
   payment_key → fold → fenced append. An unmatched feed event is acked
   and recovered by key later — same contract as the baseline.

## 7. What deliberately does NOT exist

- **No stored required/committed/confirmed counters** — the fold is the
  ledger. Consequence accepted: there is no independent arithmetic anchor
  for an I1/I2-style drift scan; the replacement control is fold
  determinism (golden vectors + a periodic re-fold-and-compare job
  between the projection and a fresh fold).
- **No mutable request row, no claim/lease columns** — the posting claim
  is the `POST_STARTED` event; worker exclusivity is the fence.
- **No parked-event table, no local cutoff machinery, no
  transition-history journal** — same exclusions as the baseline (the
  stream itself IS the full history, which also subsumes the §14.1
  attempt journal's purpose; the §14 external log contract is unchanged).
```
