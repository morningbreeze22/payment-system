# Event-Model Schema v2 (Refactored)

> Status: PROPOSAL. This refactor keeps the one fixed constraint — the
> append-only `PAYMENT_EVENT` table is the single authority for what
> happened to a payment — and redesigns everything else until, in the
> author's judgment, no MAJOR issue remains. Every L-item from the v1
> draft's known-limits list is either FIXED here or explicitly accepted
> with its mitigation. Business semantics (amount rules, evidence
> precedence, release rights, trust-age, escalation, absence-as-zero)
> are inherited from requirement v4 unchanged; this document defines
> the storage model and the mechanisms that make it safe.
>
> New input honored throughout: upstream's ORDERING is a confirmed
> per-trade SEQUENCE NUMBER carried in the message. The entire tie
> class (payload-equality tie detection, AMENDMENT_TIE_CONFLICT, the
> reprocess ≥ relaxation) is deleted, not inherited.

## 0. What changed from the v1 draft, in one view

| v1 problem (its own ranking) | v2 resolution |
|---|---|
| L6 (HIGH, money blocker): identity derived from version slots; restore re-deals slots → key reuse | **FIXED** — identity now derives from a request ordinal, not a version slot (§3). Restore behaves exactly like baseline v4: replay regenerates the same keys. |
| L2 (CRITICAL): no independent money witness; fold changes reinterpret history | **FIXED in its money-bearing part** — money facts are explicit, amount-carrying events the fold may only aggregate (§4); a transactionally-maintained witness row is cross-checked by a drift scan (§5); fold changes are gated by a deploy-time re-fold-compare (§4.2). Control-state reinterpretation remains, accepted (§10). |
| L1 (CRITICAL): no DB backstop for cross-row legality (e.g. two open requests) | **LARGELY FIXED** — the head row makes "at most one open request" a row-count-checked CAS independent of the fold, plus a trigger backstop (§5.2); a small set of cheap cross-row trigger checks is added (§5.3). Full temporal legality stays code-enforced, accepted (§10). |
| L4: per-type EVENT_CODE / column shape matrix incomplete | **FIXED** — complete matrix, including must-be-NULL rules under Oracle three-valued logic (§2.2). |
| L5: projection staleness can strand a payment invisibly | **FIXED by construction** — the projection is merged into the head row and updated in the append transaction; there is no async projector to fall behind (§5). |
| L7: inbox "seen ≠ processed" for multi-payment deliveries | **FIXED** — the inbox covers feed deliveries only, inserted in the same transaction as the append it causes; snapshot deliveries need no inbox because admission + per-payment sequence guards make redelivery converge (§7). |
| L8: request-granular UI undecided | **FIXED** — a read-only SQL view over the event table itself; opening events ARE the request rows; no second projection (§8). |
| L3: contradiction park designed, unpark not | **FIXED** — single exit defined: dual-control verified-outcome events (§6). |
| (new, found in this refactor) no ops event to clear a reject marker; no query REJECTED/ACCEPTED codes | **FIXED** — `OPS_MARKER_CLEARED` added; query result codes completed (§2.2). |
| Fence-retry write loop (optimistic, unfamiliar idiom) | **REPLACED** — pessimistic head-row lock as the write protocol; the fence unique constraint demoted to backstop (§5.1). The write path becomes the same lock-then-write idiom as v4. |

The v1 draft's companion documents (scenario walkthroughs, the
known-limits sheet, the five-issue digest) analyzed the v1 schema and
are removed from this folder — they and the review history that
produced this refactor remain in git history. The runnable
`tl-proposal-proof` suites remain valid where they target mechanisms
this refactor keeps (fence backstop, write-once identity, write-ahead,
idempotent fold) and are superseded where they target v1-specific
choices (slot-derived identity, projection staleness).

First external adversarial review of v2 (2026-07-21): 3 CRITICAL /
4 HIGH / 1 MEDIUM, every finding closed here by mechanism — terminal
amount binding (§4, §5.2, §5.3), full-population deploy gate + the
synchronous write-path witness check (§4.2, §5.1 step 3), pre-wire
recheck + the honest commit-to-wire window (§9, §10.7), the
closed-ordinal correction door with outcome supersession (§4, §5.3,
§6), ordinal claim + counter-equality CAS + key echo (§2, §3, §5.2,
§5.3), the snapshot fan-out equality fence (§7), and version
continuity (§5.3).

## 1. Physical structures — four, same count as v4

| Structure | Kind | Role |
|---|---|---|
| `PAYMENT_EVENT` | append-only, THE authority | everything that ever happened to a payment; per-payment total order |
| `PAYMENT_HEAD` | ONE mutable row per payment | write serialization lock, money WITNESS, open-request backstop, scanner/UI index — updated in the append transaction, rebuildable from the stream, NEVER read by a money decision |
| `TRADE_HEAD` | one mutable row per trade | snapshot admission watermark (`last_accepted_seq`) + XML storage pointer. Simplified from v4's trade_snapshot_state: with a contractual sequence number there is no tie to adjudicate — an equal-seq redelivery is admitted-without-update; equal seq with different content is an upstream DEFECT (refuse + CRITICAL alert), not a workflow |
| `INBOUND_EVENT_INBOX` | `UNIQUE(source, event_id)` | dedup of FEED deliveries only, atomic with processing (§7) |

Everything else — required amount, paid, reserved, phase, markers,
retry state, MAYBE, parked, escalation — is derived by the fold.

## 2. `PAYMENT_EVENT`

```sql
CREATE TABLE PAYMENT_EVENT (
  ID                 NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  PAYMENT_KEY        VARCHAR2(200)  NOT NULL,   -- canonical scope tuple
  VERSION            NUMBER(10)     NOT NULL,   -- fence slot; total order
  EVENT_TYPE         VARCHAR2(40)   NOT NULL,
  EVENT_CODE         VARCHAR2(40),              -- typed classification, per-type CHECK-bound
  REQUEST_ORDINAL    NUMBER(10),                -- WHICH request this event concerns (identity input, §3)
  UPSTREAM_SEQ       NUMBER(19),                -- confirmed upstream sequence (snapshot-derived events)
  AMOUNT             NUMBER(18,3),
  REQUIRED_AT_OPEN   NUMBER(18,3),              -- display stamp on REQUEST_OPENED only (UI amount series); never load-bearing
  IDEMPOTENCY_KEY    VARCHAR2(128),
  PAYLOAD_HASH       VARCHAR2(64),
  UETR               VARCHAR2(64),
  PROVIDER_REFERENCE VARCHAR2(128),
  PROVIDER_CODE      VARCHAR2(64),
  EVIDENCE_SOURCE    VARCHAR2(16),              -- SYNC_RESPONSE / QUERY / FEED / OPS / SYSTEM
  ACTOR              VARCHAR2(64)   NOT NULL,
  DETAIL             VARCHAR2(1000),            -- human text; the fold NEVER reads it
  CREATED_AT         TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,

  -- identity is claimed exactly once, in the schema:
  IDEM_CLAIM         VARCHAR2(128)  GENERATED ALWAYS AS
                       (CASE WHEN EVENT_TYPE = 'REQUEST_OPENED' THEN IDEMPOTENCY_KEY END),
  -- and so is the ordinal (one opening per (payment, ordinal), ever):
  ORDINAL_CLAIM      VARCHAR2(220)  GENERATED ALWAYS AS
                       (CASE WHEN EVENT_TYPE = 'REQUEST_OPENED'
                             THEN PAYMENT_KEY || '#' || TO_CHAR(REQUEST_ORDINAL) END),

  CONSTRAINT PE_FENCE_UQ   UNIQUE (PAYMENT_KEY, VERSION),  -- backstop fence (§5.1)
  CONSTRAINT PE_IDEM_UQ    UNIQUE (IDEM_CLAIM),            -- identity write-once
  CONSTRAINT PE_ORDINAL_UQ UNIQUE (ORDINAL_CLAIM)          -- ordinal write-once
  -- plus the full §2.2 shape-check set
);
CREATE INDEX PE_KEY_IX  ON PAYMENT_EVENT (PAYMENT_KEY, VERSION);
CREATE INDEX PE_UETR_IX ON PAYMENT_EVENT (UETR);
```

Grants: application role has INSERT and SELECT only. A guard trigger
raises on any UPDATE or DELETE — history is immutable against every
writer including humans in an incident.

### 2.1 Event vocabulary (18 types)

Unchanged in meaning from v1/v4 except where noted: `REQUIRED_AMOUNT_SET`,
`SNAPSHOT_INVALID_MARKED`, `REQUEST_OPENED`, `ENRICH_FAILED`,
`POST_STARTED`, `POST_RESULT_RECORDED`, `QUERY_RESULT_RECORDED`,
`DOWNGRADED_FOR_REPOST`, `OUTCOME_RECORDED`, `SETTLED`,
`SETTLEMENT_MISMATCH_RECORDED`, `EVIDENCE_CONTRADICTION_RECORDED`,
`ESCALATION_MARKED`, `OPS_VERIFIED_OUTCOME_APPLIED`, `OPS_RETRY_REARMED`,
`OPS_BLOCKED`, `OPS_ANNOTATED`, and **new** `OPS_MARKER_CLEARED`
(the v4 §19.3 ops clear of a live reject marker — the v1 draft had no
way to express it; without it a twice-rejected payment could never be
re-armed except by a newer upstream message).

### 2.2 Complete shape matrix (closes L4)

R = required (IS NOT NULL enforced), N = must be NULL, O = optional.
Every classified type's EVENT_CODE check carries an explicit
`IS NOT NULL` (a bare IN-list CHECK evaluates UNKNOWN on NULL and
silently passes); every N cell is a real CHECK, not a convention.

| EVENT_TYPE | EVENT_CODE | ORDINAL | UPSTREAM_SEQ | AMOUNT | IDEM_KEY | PAYLOAD_HASH | EVIDENCE_SOURCE |
|---|---|---|---|---|---|---|---|
| REQUIRED_AMOUNT_SET | N | N | R | R (≥0; 0 = removal) | N | N | SYSTEM |
| SNAPSHOT_INVALID_MARKED | N | N | R | N | N | N | SYSTEM |
| REQUEST_OPENED | N | R | N | R (>0) | R | R | SYSTEM/OPS |
| ENRICH_FAILED | R: TRANSIENT, DEFINITIVE | R | N | N | N | N | SYSTEM |
| POST_STARTED | N | R | N | N | R | R | SYSTEM |
| POST_RESULT_RECORDED | R: ACCEPTED, BUSINESS_REJECT, DEFINITIVE_REJECT, AMBIGUOUS, COLLISION, UNMAPPED | R | N | N | R | N | SYNC_RESPONSE |
| QUERY_RESULT_RECORDED | R: EXECUTED, REJECTED, ACCEPTED, NOT_FOUND, LOOKBACK_EXPIRED | R | N | N | N | N | QUERY |
| DOWNGRADED_FOR_REPOST | N | R | N | N | N | N | SYSTEM/OPS |
| OUTCOME_RECORDED | R: EXECUTED, REJECTED_VALIDATION, REJECTED_PROVIDER, CANCELLED_NOT_SUBMITTED, SUPERSEDED_OPS, PLATFORM_VERIFIED_EXECUTED, PLATFORM_VERIFIED_NOT_EXECUTED | R | N | R (the request amount, restated — §4) | N | N | R |
| SETTLED | N | R | N | R | N | N | FEED |
| SETTLEMENT_MISMATCH_RECORDED | N | R | N | R (the wrong amount) | N | N | FEED |
| EVIDENCE_CONTRADICTION_RECORDED | R: SETTLED_AFTER_TERMINAL, MISMATCH_AFTER_TERMINAL, QUERY_CONTRADICTS_OUTCOME | R | N | O | N | N | R |
| ESCALATION_MARKED | N | R | N | N | N | N | SYSTEM |
| OPS_VERIFIED_OUTCOME_APPLIED | N | R | N | N | N | N | OPS |
| OPS_RETRY_REARMED / OPS_BLOCKED / OPS_MARKER_CLEARED / OPS_ANNOTATED | N | O | N | N | N | N | OPS |

(`REQUIRED_AT_OPEN` is permitted only on `REQUEST_OPENED`; `UETR` only
on acceptance-class and feed events, per the v4 §5 UETR-persistence
rule, which is inherited verbatim: reject/collision UETRs are never
recorded.)

Ops events carry the approval reference in DETAIL and, for
`OPS_VERIFIED_OUTCOME_APPLIED`, are appended in the SAME transaction
as their `OUTCOME_RECORDED(PLATFORM_VERIFIED_*)` — dual-control
protocol inherited from v4 §9.3 unchanged.

## 3. Identity — the L6 fix

```
request_ordinal  = 1 + (count of prior REQUEST_OPENED events in this stream)
                   — maintained as PAYMENT_HEAD.NEXT_REQUEST_ORDINAL,
                     consumed and stamped onto the opening event in the
                     opening transaction (write-once, shape-checked)
idempotency_key  = hash(business_id | payment_type | debit_account |
                        currency | request_ordinal)
                   — byte-exact spec + golden vectors, identical
                     discipline to v4 §5.1
```

Version slots no longer participate in identity. Consequences:

- A database restore + Kafka replay regenerates the SAME ordinals from
  business history — recreated requests carry the SAME keys, the
  engine rejects the collision, and the v4 §5.2 restore runbook
  applies essentially unchanged (posting freeze lives in Hazelcast,
  outside the database; the 5b enumeration sweep enumerates
  hash(scope | 1..N+K) exactly as v4 does).
- `IDEM_CLAIM` remains the schema-level write-once guarantee: two
  opening events can never carry the same key, whatever produced them.
- **The ordinal itself is schema-bound**: `PE_ORDINAL_UQ` makes ordinal
  reuse collide loudly, and the §5.2 opening CAS carries
  `NEXT_REQUEST_ORDINAL = :ordinal`, so a writer supplying a stale or
  skipped ordinal aborts on row count 0 — counter, ordinal, and claim
  cannot drift apart. What remains code discipline (stated honestly):
  the KEY DERIVATION content — that the hash was computed over THIS
  ordinal — which lives under the same golden-vector regime as v4 §5.1.
- No epoch/generation machinery is needed for the main hazard. (An
  epoch component remains a cheap optional hardening; not required.)

## 4. Money facts are events; the fold only aggregates them — the L2 fix (money half)

**Rule (normative):** every money-bearing fact is an explicit event
carrying its own amount and its own classification, recorded at
decision time. The fold may AGGREGATE money events; it may never
re-classify them, re-derive them from raw provider codes, or infer an
amount not present on an event.

```
required_amount = AMOUNT of the REQUIRED_AMOUNT_SET with the highest UPSTREAM_SEQ
authoritative_outcome(ordinal)
                = the LATEST outcome-class event (OUTCOME_RECORDED or
                  SETTLED) for that ordinal in stream order — a
                  PLATFORM_VERIFIED_* outcome appended through the §6
                  dual-control door SUPERSEDES the outcome it corrects
paid_total      = Σ AMOUNT over request ordinals whose authoritative
                  outcome is executed-class (EXECUTED |
                  PLATFORM_VERIFIED_EXECUTED | SETTLED)  (idempotent by ordinal)
reserved        = AMOUNT of the open request (opening event exists,
                  no outcome event for its ordinal), else 0
shortfall       = required − paid_total − reserved
```

Amount binding (normative): a money-terminal event for an ordinal must
carry AMOUNT equal to that ordinal's OPENING amount — the all-or-nothing
engine contract makes any other number defect evidence, recorded as
`SETTLEMENT_MISMATCH_RECORDED` (or a contradiction), never as
`OUTCOME_RECORDED(EXECUTED)`/`SETTLED`. §5.2/§5.3 enforce this equality
in the database, so a wrong terminal number cannot silently enter
either bookkeeper and reopen a phantom shortfall.

Because the inputs are literal recorded numbers, deploying a changed
fold CANNOT retroactively change a settled payment's money totals
unless the aggregation itself changes — which is exactly what §4.2
gates.

### 4.2 Fold governance

One shared fold artifact, semantically versioned, golden-vector
frozen; no consumer (UI, scanner, resolver, ops surface) may
re-implement any part of it. Two standing controls:

1. **Deploy gate:** before a release containing a fold change goes
   live, re-fold EVERY payment stream still within operational
   retention — open AND terminal, no sampling — and compare against
   the `PAYMENT_HEAD` witness (§5). Any money-field difference is a
   page and blocks the deploy. Sampling is forbidden: a sampled gate
   provably lets a terminal-stream fold defect deploy silently and
   later reopen a phantom shortfall when new upstream traffic touches
   the unsampled stream. (At ~3,000 trades/day the full population is
   minutes of work.)
2. **Drift scan (continuous):** a scheduled job re-folds open payments
   and compares fold output to the head witness; mismatch pages. This
   is the same two-bookkeepers control as v4's I1/I2 scan: the head is
   maintained by simple mechanical increments (§5), the fold by rule
   evaluation — two independent computations of the same money.

History is never edited. A wrong recorded decision is corrected by a
dual-control ops event (§6), which is itself a new recorded fact.

## 5. `PAYMENT_HEAD` — lock, witness, backstop, index (one row per payment)

```sql
CREATE TABLE PAYMENT_HEAD (
  PAYMENT_KEY          VARCHAR2(200) PRIMARY KEY,
  BUSINESS_ID          VARCHAR2(64)  NOT NULL,     -- card lookup (indexed)
  LAST_VERSION         NUMBER(10)    NOT NULL,     -- must equal stream max
  NEXT_REQUEST_ORDINAL NUMBER(10)    NOT NULL,     -- identity counter (§3)
  OPEN_REQUEST_ORDINAL NUMBER(10),                 -- NULL = no open request
  OPEN_IDEMPOTENCY_KEY VARCHAR2(128),              -- the open request's key (echo check, §5.3)
  -- money WITNESS (mechanical increments; never read by decisions):
  REQUIRED_AMOUNT      NUMBER(18,3),
  PAID_TOTAL           NUMBER(18,3) DEFAULT 0 NOT NULL,
  RESERVED             NUMBER(18,3) DEFAULT 0 NOT NULL,
  -- scanner / UI index (display + candidate selection only):
  PHASE                VARCHAR2(24),
  NEXT_ACTION_AT       TIMESTAMP,
  UETR                 VARCHAR2(64),
  UI_STEP_STATUS       VARCHAR2(16),
  ESCALATED            CHAR(1) DEFAULT 'N',
  UPDATED_AT           TIMESTAMP
);
CREATE INDEX PH_DUE_IX  ON PAYMENT_HEAD (PHASE, NEXT_ACTION_AT);
CREATE INDEX PH_BIZ_IX  ON PAYMENT_HEAD (BUSINESS_ID);
CREATE INDEX PH_UETR_IX ON PAYMENT_HEAD (UETR);
```

The head is a CACHE with respect to truth (rebuildable from the
stream at any time) but a CONTRACT with respect to freshness: it is
updated **in the append transaction**, so there is no async projector
to fall behind and no stale-false-negative class (v1's L5 is closed by
construction, not by a reconciling sweep).

### 5.1 Write protocol — the only write path

```
1. SELECT ... FOR UPDATE on PAYMENT_HEAD (insert-on-first-contact:
   LAST_VERSION = 0, NEXT_REQUEST_ORDINAL = 1, witness columns 0 —
   explicit initial values, never NULL+1 arithmetic; PK-race retry —
   the same idiom as v4's obligation row)
2. fold(stream)                       -- read PAYMENT_EVENT by (key, version)
3. WITNESS CHECK (fail closed): compare the fold's money outputs
   (paid_total, reserved, open ordinal) against the locked head row.
   ANY mismatch: abort with NO decision, page, park the payment.
   The two bookkeepers must agree BEFORE money logic runs — this is
   the §4.2 drift scan made synchronous at the only moment it matters,
   and it is a VETO, never an authorization
4. decide                             -- pure function: fold state -> events
5. FOR EACH decided event, IN ORDER:
     INSERT it at the next version slot,
     THEN apply ITS head effect (§5.2 CAS, witness change, phase)
6. COMMIT   (multi-event decisions remain ONE transaction — atomic)
```

Step 5 is **per event, not batched**: the §5.2/§5.3 backstops check each
insert against the head state its predecessors IN THIS TRANSACTION left
behind. The atomic outcome+successor decision is legal only in this
interleaving — outcome insert (its ordinal still open) → close CAS
(column goes NULL) → successor `REQUEST_OPENED` insert (trigger sees
NULL) → open CAS. Batching all inserts before one head update would
make the backstops reject the design's own legal writes.

The head lock serializes writers per payment — no optimistic retry
loop, no lock-order subtleties beyond TRADE_HEAD → PAYMENT_HEAD
(sorted) during snapshot fan-out, which is v4's exact lock order.
`PE_FENCE_UQ` remains as the backstop: any writer that bypasses the
lock discipline collides on the unique constraint and fails loudly
instead of forking history. A fence collision seen by a writer that
HOLDS the head lock means the head has lost sync with the stream (a
bypass write happened): page, stop this stream's writes, rebuild the
head row from the stream, then resume — the head is a cache and this
is its rebuild path, exercised deliberately in tests.

Narrow exemption (mirrors v4's claim-field rule): pure scheduling
updates on the head (`NEXT_ACTION_AT` backoff after an INDETERMINATE
query) may be written without an append — they are not derivation
inputs and not money.

### 5.2 The one-open-request backstop — the L1 fix (critical part)

Opening a request executes, in the opening transaction:

```
UPDATE PAYMENT_HEAD
   SET OPEN_REQUEST_ORDINAL = :ordinal,
       OPEN_IDEMPOTENCY_KEY = :idem_key,
       NEXT_REQUEST_ORDINAL = NEXT_REQUEST_ORDINAL + 1,
       RESERVED = :amount
 WHERE PAYMENT_KEY = :key
   AND OPEN_REQUEST_ORDINAL IS NULL
   AND NEXT_REQUEST_ORDINAL = :ordinal      -- the counter IS the ordinal
```

Row count 0 aborts the append. This is a second bookkeeper for the
single most dangerous invariant — it does not trust the fold. Closing
(outcome) clears the ordinal and key columns the same way
(`WHERE OPEN_REQUEST_ORDINAL = :ordinal`), zeroes `RESERVED`, and adds
the amount to `PAID_TOTAL` only when the outcome is money-terminal
(`EXECUTED` / `PLATFORM_VERIFIED_EXECUTED` / `SETTLED`) — and the
money-terminal close additionally carries `AND RESERVED = :amount`:
the witness binds not just WHICH ordinal closes but AT WHAT NUMBER it
closes (the §4 amount-binding rule made mechanical; a differing feed
amount can only enter as `SETTLEMENT_MISMATCH_RECORDED`). A trigger on
`PAYMENT_EVENT` additionally rejects a `REQUEST_OPENED` insert when
the head's column is non-NULL — belt and braces, both independent of
fold correctness.

**Witness-exactly-once (consequence, worth stating):** because §5.3
admits money-bearing events for an ordinal only WHILE that ordinal is
the open one, and the close CAS fires exactly once per ordinal (row
count 0 otherwise), `PAID_TOTAL` increments exactly once per request
ordinal — the mechanical witness cannot double-count the same request
even if late confirming evidence arrives (that lands as a no-op or a
contradiction event, §6, never as a second increment).

### 5.3 Cheap cross-row trigger backstops (the rest of L1, partial)

Because the head is transaction-fresh, a set of cross-row legality
checks becomes DB-enforceable as BEFORE-INSERT triggers on
`PAYMENT_EVENT` (each a single indexed head read under the lock
already held):

- **Open-ordinal**: `POST_STARTED` / `POST_RESULT_RECORDED` /
  `OUTCOME_RECORDED` / `SETTLED` require `OPEN_REQUEST_ORDINAL =
  :new.REQUEST_ORDINAL` (except the terminal-evidence contradiction
  path, which must instead append `EVIDENCE_CONTRADICTION_RECORDED` —
  the trigger enforces that routing); `REQUEST_OPENED` requires it
  NULL (§5.2).
- **One closed-ordinal exception** (the §6 correction door, and
  nothing else): `OUTCOME_RECORDED(PLATFORM_VERIFIED_*)` appended in
  the same transaction as `OPS_VERIFIED_OUTCOME_APPLIED` is admitted
  for a CLOSED ordinal.
- **Amount equality**: `OUTCOME_RECORDED` with an executed-class code
  and `SETTLED` require `:new.AMOUNT = RESERVED` (the opened amount);
  a differing feed amount is only insertable as
  `SETTLEMENT_MISMATCH_RECORDED` (routing enforced).
- **Key echo**: `POST_STARTED` / `POST_RESULT_RECORDED` require
  `:new.IDEMPOTENCY_KEY = OPEN_IDEMPOTENCY_KEY` — an attempt event can
  never cite a key other than its opening's.
- **Version continuity**: every insert requires `:new.VERSION =
  LAST_VERSION + 1`, and the per-event head effect sets
  `LAST_VERSION = :new.VERSION` — closing the skipped-slot gap the
  fence alone cannot see (a unique constraint rejects duplicates, not
  holes); the drift scan additionally asserts density
  (`COUNT(*) = MAX(VERSION) = LAST_VERSION`).

Full temporal legality beyond this set stays code-enforced (honesty
box, §10).

## 6. Contradictions and the unpark path — the L3 fix

`EVIDENCE_CONTRADICTION_RECORDED` has a FIXED fold effect: book
nothing, PARK the payment (no posting, no new opens), CRITICAL alert.
The ONLY exit is the dual-control verified-outcome operation (v4 §9.3
protocol verbatim — two-step approval, approval_id as the sole
execution input), which appends `OPS_VERIFIED_OUTCOME_APPLIED` +
`OUTCOME_RECORDED(PLATFORM_VERIFIED_*)` in one transaction. The fold
treats a verified outcome AFTER a contradiction as the authoritative
resolution: the park lifts, money books per the recorded outcome, and
the standing rule resumes (a remaining shortfall opens a successor in
the same transaction). Nothing else unparks a contradicted payment.

**Correction mechanics (the closed-ordinal door).** The dual-control
pair is the ONLY write admitted for a closed ordinal (§5.3 exception —
without it, the design's sole unpark path would be rejected by its own
trigger). Fold rule: an ordinal's authoritative outcome is the LATEST
outcome-class event in stream order (§4), so the verified outcome
supersedes the wrong recorded one while history keeps both. Head
effect of a superseding verified outcome: `PAID_TOTAL` adjusted by the
signed difference (verified NOT_EXECUTED over a booked EXECUTED
subtracts the amount; verified EXECUTED over a booked reject adds it);
`RESERVED` untouched — the ordinal stays closed. The same transaction
re-evaluates the standing rule on the corrected numbers.

**Scope rule — contradiction means CONFLICT, not repetition.** Evidence
that AGREES with an already-recorded terminal outcome (the routine
case: feed settlement arriving for an ordinal already closed
`EXECUTED`-class with an equal amount, or a query re-confirming an
executed outcome) is a benign no-op delivery — inbox-deduped, nothing
appended, nothing parked. `EVIDENCE_CONTRADICTION_RECORDED` is
reserved for evidence that CONFLICTS: settlement after a
rejected-class terminal, an amount that differs, a query verdict
contradicting the recorded outcome. Without this rule the happy path
(executed-by-query, then the feed catches up) would CRITICAL-park
healthy payments.

## 7. Inbox semantics — the L7 fix

- **Feed deliveries (single-payment):** the inbox INSERT rides the
  SAME transaction as the resulting append (or the same transaction
  that decides "no-op": stale evidence). "Seen" and "processed" commit
  atomically; a crash before commit leaves neither.
- **Snapshot deliveries (multi-payment):** NO inbox row at all, and an
  EXPLICIT transaction boundary: the ADMISSION transaction updates
  only the watermark/digest/XML pointer; FAN-OUT then runs as separate
  per-payment transactions, each of which (1) locks `TRADE_HEAD` (the
  v4 lock order), (2) verifies its carried snapshot seq still EQUALS
  `LAST_ACCEPTED_SEQ` — the **equality fence**: if a newer admission
  owns the trade, abort; the newer fan-out covers every payment
  including absences — then (3) locks the payment head and appends
  seq-guarded. Resume after a crash re-derives the worklist from the
  CURRENT watermark's stored XML, never from an in-memory snapshot, so
  a stale resumed worker can neither create nor touch a payment from
  superseded trade truth. Kafka ack only after fan-out completes;
  redelivery re-runs and converges. Side effects (metrics, alerts) key
  on state CHANGES (an append that actually happened), so re-runs do
  not re-fire them.
- Retention: inbox purge > Kafka retention ≥ replay window (owner
  rule inherited from v4 §16.2).

## 8. Read surfaces — the L8 fix

- **Step card (obligation-granular):** reads `PAYMENT_HEAD`
  (`UI_STEP_STATUS`, exception summary via phase) — display-only, and
  the head is transaction-fresh. Lookup by BUSINESS_ID returns all of
  a trade's payments; row absence = NOT_STARTED.
- **All-payments table (request-granular):** a read-only SQL VIEW over
  `PAYMENT_EVENT` — one row per `REQUEST_OPENED` (request identity =
  payment_key + ordinal), outcome joined by ordinal, obligation-only
  placeholders from head rows with `NEXT_REQUEST_ORDINAL = 1`. The
  amount series is free: `REQUIRED_AT_OPEN` is stamped on the opening
  event (immutable by nature — no set-once discipline needed, no F0
  capture-boundary machinery). Keyset pagination on
  (payment_key, version). No projection, no maintenance job.
- **Scanners/resolver:** select CANDIDATES from the head
  (`PHASE, NEXT_ACTION_AT`); every ACTION folds the stream under the
  head lock. A stale candidate costs a wasted fold, never a wrong
  payment — and the head cannot be stale by more than an uncommitted
  transaction (§5).
- **Feed matching multiplicity (explicit):** match UETR against head
  first, event index second; 0 → unmatched path (ack; §9-style query
  sweep recovers), 1 → fold + append, 2+ → CRITICAL anomaly, no state
  change.

## 9. Operational inheritances (unchanged from v4, restated as binding)

Posting freeze in Hazelcast (outside the DB; absent = frozen);
write-ahead rule (identity + payload hash durable before the wire —
here structural: `POST_STARTED` IS the durable claim, and "no
POST_STARTED = provably never sent" is the release predicate) — with
one mandatory addition: between the COMMIT of `POST_STARTED` and the
wire call the worker re-reads the head (no lock) and SKIPS the send if
the payment is parked/blocked or the ordinal is no longer open; the
committed claim then resolves through the standard §9.1-style ask path
under the park (it is NOT provably unsent, so it is never released —
only asked about). This narrows the irreducible commit-to-wire window
to the recheck-to-send gap (honesty box item 7);
evidence precedence and release rights (§9.4/§10.1 semantics live in
the fold, golden-vector tested); trust-age / downgrade / escalation
clocks (event timestamps are the episode anchors — set-once by
immutability, the v4 clock-discipline problem disappears); engine
collision contract as the keystone, proven by the §18 item-1 sandbox
test before go-live; retention: terminal payments archive as whole
streams together with their head row and trade.

## 10. Honesty box v2 — what remains accepted, with mitigations

1. **Full temporal legality is still code-enforced** beyond the §5.2/
   §5.3 backstops. Mitigation: the backstopped subset covers the
   money-moving transitions; the rest is fold logic under golden
   vectors. Residual risk: a fold bug producing a legal-shaped but
   semantically wrong event sequence. Comparable in kind to a wrong
   CAS in v4; caught by the same class of tests.
2. **Control-state reinterpretation on fold change** (marker liveness,
   retry pacing — not money, per §4). A fold deploy can change how
   existing streams BEHAVE next. This is the same risk as any code
   deploy in v4; the §4.2 deploy gate bounds the money side to zero.
3. **The fold is a single point of interpretation.** Deliberate — it
   is also the single place to test. The witness (§5), the deploy gate
   (§4.2), and the DB backstops (§5.2/5.3) are the independent checks.
4. **Event-table growth.** Trivial at 3k trades/day for years;
   stream-granular archival (§9) must exist before it matters. Not a
   go-live item.
5. **Everything still stands on the engine collision contract** —
   exactly as v4 does. §18 BLOCKING item 1 gates go-live for this
   design identically.
6. **Ops learning curve:** incident diagnosis reads the head for
   "where is it" and the stream for "why" — the stream IS the
   transition history v4 lacks locally, but the team must learn to
   read it. A `fold --explain <payment_key>` debug tool (prints the
   stream and the fold's state after each event) should ship with the
   MVP; it is the event model's answer to "SELECT the state" and it
   must be treated as a deliverable, not a nice-to-have.
7. **The commit-to-wire window.** A park or contradiction committed
   after `POST_STARTED` commits but before the wire call cannot
   retract the claim — identical in kind to v4's write-ahead window.
   The mandatory pre-wire head recheck (§9) narrows it to the
   recheck-to-send gap; the residual is an executed request whose
   outcome is still truthfully recorded and then reconciled through
   the §6 dual-control flow. No write-ahead design closes this window
   from the database alone — claiming "park = no posting, absolutely"
   would be dishonest, so the claim here is "park = no NEW posting
   decisions + in-flight sends re-checked at the last possible
   moment."

## 11. Why this version is presentable

Every item the v1 draft itself ranked CRITICAL or HIGH is closed by a
mechanism, not by a promise: identity survives restore because it no
longer depends on stream position; money cannot be silently
reinterpreted because money is recorded, witnessed twice, and
deploy-gated; the killer invariant has a fold-independent CAS + trigger;
freshness is transactional; the inbox is atomic; the UI needs no
second projection. What remains in §10 is the honest residue every
design carries — and for each item, the v4 baseline has a directly
comparable counterpart risk. The comparison between the two designs
is now a fair fight between representations, not between a finished
design and a draft.
