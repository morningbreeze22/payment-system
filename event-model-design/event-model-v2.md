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

Second round (2026-07-21, same harness, targeting the round-1 fixes):
3 CRITICAL / 3 HIGH / 1 MEDIUM, all closed — correction-door amount
equality re-anchored to the ordinal's OWN opening amount + the
excess-direction park-persistence rule (§5.3, §6), query results now
carry and echo the QUERIED key (§2.2, §5.3), the head's open key/amount
copied FROM the opening row instead of program variables (§5.2), the
`TX_ID` + typed `APPROVAL_REF` pair binding for the correction door
(§2, §5.3), verified-NOT-executed latches the inherited
`provider_rejected` marker — the same-transaction-successor promise
RETRACTED (§6), the two-watermark `TRADE_HEAD` so invalid-snapshot
markers fan out under the fence (§1, §7), and amount equality extended
to every outcome code (§5.3).

Third round (2026-07-21, same harness, targeting the round-2 fixes):
2 CRITICAL / 4 HIGH / 1 MEDIUM, all closed — the pair gate extended to
EVERY verified outcome, open or closed (§5.3); full-current-state
fan-out: the seen-fence owner catches up the stored ACCEPTED truth
before marking invalid, so a fenced-out cancellation can never starve
(§7); `TRADE_HEAD` carries digest + pointer PER WATERMARK and a
nullable accepted pair for first-delivery-invalid trades (§1, 01 §7);
`APPROVAL_REF` required on the money-enabling single ops actions
(§2.2); the witness check covers required_amount and gained the
WITNESS_DIVERGED quarantine + rebuild path (§5.1); the correction
delta pinned to the decide-time prior (§6); the request view selects
the authoritative outcome (01 §9).

Fourth round (2026-07-21, targeting the round-3 fixes): 0 CRITICAL /
5 HIGH, all closed — first-contact `REQUIRED_AMOUNT` = NULL with a
NULL-safe witness comparison (§5.1); the dual-control door runs in
RECORDING mode under persistent divergence (fold-authoritative, head
rebuilt in the same transaction) so the prescribed exit is reachable
(§5.1); the backstop trigger set specified as a COMPOUND trigger +
single-row-insert guard, with the mutating-table behavior itself
checklist-5 evidence (§5.3); WITNESS_DIVERGED added to the pre-wire
skip set (§9); `CREATED_AT` guard-trigger-stamped like `TX_ID` — an
immutable episode anchor may never be writer-supplied (§2).

Fifth round (2026-07-21, fresh-eyes sweep of untouched sections):
2 CRITICAL / 2 HIGH / 2 MEDIUM, all closed — the v4 release-guard
transplanted as trigger checks (`CANCELLED_NOT_SUBMITTED` /
`REJECTED_VALIDATION` / `SUPERSEDED_OPS` require zero `POST_STARTED`;
`REJECTED_PROVIDER` requires first-party negative evidence — §5.3);
archival finality rules: PERMANENT heads, event-only archival behind
terminality + reprocessing + key-retention windows, rehydration before
any write (§9, §10.4 reworded: design pre-production, execution
far-future); pre-open definitive enrichment failure = payment-level
marker with NULL ordinal, no impossible outcome pair (§2.2);
`ESCALATION_MARKED` given its real inherited fold effect — the episode
transitions to ops-owned BLOCKED once per episode (01 §4);
`REQUIRED_AT_OPEN` required + head-equality-checked at opening (§2.2);
feed multiplicity counted in DISTINCT payments (§8).

Sixth round (2026-07-21, targeting the round-5 fixes): 3 CRITICAL /
3 HIGH, all closed — feed candidates = UNION of head and event-index
matches, both always consulted, unmatched terminal evidence paged
never silently acked (§8); `REJECTED_PROVIDER` evidence must be
terminal-class AND post-date the latest `POST_STARTED` —
`BUSINESS_REJECT` never qualifies (§5.3); archival eligibility also
waits out every evidence channel's lateness bound and the archive
stays fold-readable, with the deploy gate covering archived streams
(§4.2, §9); pre-open `ENRICH_FAILED` carries the seq it enriched for
(marker provenance + strictly-newer unlatch) and ordinal-bearing
`ENRICH_FAILED` joined the open-ordinal trigger (§2.2, §5.3);
`ESCALATION_MARKED` reverted to §9.3-verbatim semantics — escalated
MAYBE stays resolver-owned, the resolver keeps querying, no separate
unblock exists (01 §4).

Seventh round (2026-07-21): 2 CRITICAL / 4 HIGH / 1 MEDIUM. Four of
seven findings attacked the archival mechanism itself — the honest
remediation was REMOVAL, not more machinery: event rows are now
PERMANENT alongside the heads (§9, §10.4; partitioning-only tiering;
the fold-readable-archive / rehydration / lateness-bound clauses of
rounds 5–6 are superseded). The rest: feed-candidate union re-checked
INSIDE the booking transaction + the honest same-instant residual
(§8); `FEED_RESULT_RECORDED(REJECTED)` added as the 19th type so an
authoritative feed rejection is recordable and release-whitelisted
(§2.1, §2.2, §5.3); unmatched terminal evidence = durable
`MATCH_STATUS` on the inbox row with LEVEL-TRIGGERED paging (§8);
`PAYMENT_KEY` canonical encoding = byte-exact spec + golden vectors +
one shared encoder (§3).

Eighth round (2026-07-22, targeting the round-7 fixes): 3 CRITICAL /
4 HIGH / 1 LOW, all closed — `PE_UETR_UQ` on a generated acceptance
claim column now backs the §8 post-recheck residual with a schema
constraint instead of a promise (§2, §8); `PAYMENT_HEAD.BUSINESS_ID`
bound to the key (decoder-derived + CHECK) so a mis-bound head cannot
drop out of its trade's fan-out worklist (§5); `DOWNGRADED_FOR_REPOST`
trigger-gated on post-attempt NOT_FOUND evidence + the
attempt-attribution question answered by the §18 key-dedup contract,
stated (§5.3); `FEED_REJECTS_OUTCOME` contradiction code added
(§2.2); unmatched inbox rows carry the evidence CONTENT, resolve
atomically, and are purge-exempt (§7); partition maintenance must be
index-preserving with a usability gate before writes resume (§9);
`CREATED_AT` = SYS_EXTRACT_UTC per the inherited single-UTC rule
(§2); two stale cross-references fixed (01).

Ninth round (2026-07-22, targeting the round-8 fixes): 2 CRITICAL /
2 HIGH, all closed — the event-level UETR claim RETRACTED (it missed
the query/feed association channels and refused the legal
re-recording of a CT-dedup-returned acceptance) and replaced by the
HEAD-level claim `PH_UETR_UQ` (one head per UETR, every channel
funnels through the head effect; historical overlaps stay covered by
the UNION rule) (§2, §5, §8); `PH_BIZ_BIND_CK` now IN the DDL,
consuming the newly frozen canonical-form fact (leading business_id +
`|` delimiter) (§3, §5); the downgrade gate gained the
no-intervening-acceptance conjunct — recency alone proved recording
order, not the absence of acceptance (§5.3); the unmatched inbox
evidence content is SHAPE-BOUND per status (§7).

Tenth round (2026-07-22, targeting the round-9 fixes): 2 CRITICAL /
4 HIGH, all closed — the canonical form made INJECTIVE by rule (no
`|` in components, encoder fails closed, intake classifies violations
INVALID; head checks exactly three delimiters) (§3, §5); the
UETR-association trigger — write-once per ordinal + global
first-claim probe on the permanent `PE_UETR_IX` — closes both the
intra-payment wrong-ordinal association and the abandoned-historical-
UETR claim, with `PH_UETR_UQ` kept as the simultaneous-first-claim
belt (§5.3); `FEED_RESULT_RECORDED` gained code ACCEPTED so
intermediate feed acceptance is recordable and visible to the
downgrade gate's acceptance list (§2.1, §2.2, §5.3); inbox resolution
= third status RESOLVED with evidence retained (the two-state flip
was schema-illegal as specified) and the EV class vocabulary is
CHECK-closed (§7).

Eleventh round (2026-07-22, targeting the round-10 fixes):
1 CRITICAL / 1 HIGH / 1 LOW, all closed — UETR REQUIRED on every
feed-matched evidence event and on sync acceptance (a NULL-UETR feed
event was the one door around the §5.3 association gate; recording a
UETR-matched delivery without its UETR discards the identity it was
matched by) (§2.2); inbox resolution split into RESOLVED_MATCHED
(append-bound: records payment key + version of the append it rode
with) and RESOLVED_DISPOSED (ops exit: actor + four-eyes approval +
reason, shape-CHECKed) (§7); the README round-count header fixed
(LOW).

## 1. Physical structures — four, same count as v4

| Structure | Kind | Role |
|---|---|---|
| `PAYMENT_EVENT` | append-only, THE authority | everything that ever happened to a payment; per-payment total order |
| `PAYMENT_HEAD` | ONE mutable row per payment | write serialization lock, money WITNESS, open-request backstop, scanner/UI index — updated in the append transaction, rebuildable from the stream, NEVER read by a money decision |
| `TRADE_HEAD` | one mutable row per trade | TWO snapshot watermarks, EACH with its own digest + storage pointer — accepted (`LAST_ACCEPTED_SEQ`, newest VALID snapshot = trade truth; NULL until the first valid one) and seen (`LAST_SEEN_SEQ`, newest processed at all, valid or invalid; `≥` accepted). The fan-out equality fence (§7) checks SEEN, so invalid-snapshot markers CAN fan out without admitting invalid content as truth; the seen digest/pointer make an invalid redelivery deduplicable and its fan-out resumable (a single accepted-only digest would misread an identical invalid redelivery as an upstream defect). With a contractual sequence number there is no tie to adjudicate — an equal-seq redelivery (equal digest, per the SEEN pair) is admitted-without-update; equal seq with different content is an upstream DEFECT (refuse + CRITICAL alert), not a workflow |
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
  APPROVAL_REF       VARCHAR2(64),              -- dual-control approval id, TYPED (fold-visible);
                                                --   R on the §6 pair, N elsewhere — never DETAIL
  ACTOR              VARCHAR2(64)   NOT NULL,
  DETAIL             VARCHAR2(1000),            -- human text; the fold NEVER reads it
  TX_ID              VARCHAR2(64),              -- stamped BY THE GUARD TRIGGER with the local
                                                --   transaction id; writers cannot supply it
  CREATED_AT         TIMESTAMP      NOT NULL,   -- ALSO guard-trigger-stamped, UTC:
                                                --   SYS_EXTRACT_UTC(SYSTIMESTAMP) per the
                                                --   inherited v4 §16.4 single-UTC rule (a
                                                --   local-clock stamp crosses DST jumps and
                                                --   corrupts trust-age arithmetic forever);
                                                --   no writer may supply it

  -- identity is claimed exactly once, in the schema:
  IDEM_CLAIM         VARCHAR2(128)  GENERATED ALWAYS AS
                       (CASE WHEN EVENT_TYPE = 'REQUEST_OPENED' THEN IDEMPOTENCY_KEY END),
  -- and so is the ordinal (one opening per (payment, ordinal), ever):
  ORDINAL_CLAIM      VARCHAR2(220)  GENERATED ALWAYS AS
                       (CASE WHEN EVENT_TYPE = 'REQUEST_OPENED'
                             THEN PAYMENT_KEY || '#' || TO_CHAR(REQUEST_ORDINAL) END),
  -- (UETR uniqueness is enforced at the HEAD, not here: an event-level
  --  acceptance claim missed the query/feed association channels AND
  --  collided with the legal re-recording of a CT-dedup-returned
  --  original acceptance — see PH_UETR_UQ, §5)

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

### 2.1 Event vocabulary (19 types)

Unchanged in meaning from v1/v4 except where noted: `REQUIRED_AMOUNT_SET`,
`SNAPSHOT_INVALID_MARKED`, `REQUEST_OPENED`, `ENRICH_FAILED`,
`POST_STARTED`, `POST_RESULT_RECORDED`, `QUERY_RESULT_RECORDED`,
`DOWNGRADED_FOR_REPOST`, `OUTCOME_RECORDED`, `SETTLED`,
`SETTLEMENT_MISMATCH_RECORDED`, `EVIDENCE_CONTRADICTION_RECORDED`,
`ESCALATION_MARKED`, `OPS_VERIFIED_OUTCOME_APPLIED`, `OPS_RETRY_REARMED`,
`OPS_BLOCKED`, `OPS_ANNOTATED`, **new** `OPS_MARKER_CLEARED`
(the v4 §19.3 ops clear of a live reject marker — the v1 draft had no
way to express it; without it a twice-rejected payment could never be
re-armed except by a newer upstream message), and **new**
`FEED_RESULT_RECORDED` (codes `ACCEPTED` / `REJECTED`: the feed
channel's evidence about an active request — REJECTED is its terminal
rejection (without it an authoritative feed reject was UNRECORDABLE:
`SETTLED` means money moved, contradiction events require an existing
terminal, and the §5.3 release whitelist demands a recorded evidence
row); ACCEPTED is the intermediate status-feed acceptance that
tightens submission knowledge per the inherited §4.4 precedence —
without it, feed acceptance was silently droppable and the §5.3
downgrade gate could not see it. Feed-executed remains `SETTLED`).

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
| ENRICH_FAILED | R: TRANSIENT, DEFINITIVE | O — NULL when enrichment fails BEFORE any opening (no request exists to cite; fold effect = latch `validation_failed`, block new opens, NO outcome pair — an outcome would need an open ordinal and a payload hash that was never assembled); R when re-enrichment fails for an OPEN request (then open-ordinal trigger-checked like attempt events) | R when ORDINAL is NULL — the seq of the required-amount truth being enriched, so the marker has replayable provenance and unlatches on strictly NEWER truth (without it a delayed stale failure could outlive the correction that superseded it); N when ORDINAL is R | N | N | N | SYSTEM |
| POST_STARTED | N | R | N | N | R | R | SYSTEM |
| POST_RESULT_RECORDED | R: ACCEPTED, BUSINESS_REJECT, DEFINITIVE_REJECT, AMBIGUOUS, COLLISION, UNMAPPED | R | N | N | R | N | SYNC_RESPONSE |
| *(UETR rule for evidence events)* | *UETR is REQUIRED — never optional — on every feed-matched evidence event (`SETTLED`, `FEED_RESULT_RECORDED` both codes, `SETTLEMENT_MISMATCH_RECORDED`, feed-sourced contradictions) and on `POST_RESULT_RECORDED(ACCEPTED)`: the delivery was MATCHED BY its UETR, and recording it without the UETR discards the identity the §5.3 association gate keys on — a NULL-UETR feed event was the one door around that gate* | | | | | | |
| QUERY_RESULT_RECORDED | R: EXECUTED, REJECTED, ACCEPTED, NOT_FOUND, LOOKBACK_EXPIRED | R | N | N | R (the key that was QUERIED — echo-checked, §5.3) | N | QUERY |
| DOWNGRADED_FOR_REPOST | N | R | N | N | N | N | SYSTEM/OPS |
| OUTCOME_RECORDED | R: EXECUTED, REJECTED_VALIDATION, REJECTED_PROVIDER, CANCELLED_NOT_SUBMITTED, SUPERSEDED_OPS, PLATFORM_VERIFIED_EXECUTED, PLATFORM_VERIFIED_NOT_EXECUTED | R | N | R (the request amount, restated — §4) | N | N | R |
| SETTLED | N | R | N | R | N | N | FEED |
| FEED_RESULT_RECORDED | R: ACCEPTED, REJECTED | R | N | N | N | N | FEED |
| SETTLEMENT_MISMATCH_RECORDED | N | R | N | R (the wrong amount) | N | N | FEED |
| EVIDENCE_CONTRADICTION_RECORDED | R: SETTLED_AFTER_TERMINAL, MISMATCH_AFTER_TERMINAL, QUERY_CONTRADICTS_OUTCOME, FEED_REJECTS_OUTCOME | R | N | O | N | N | R |
| ESCALATION_MARKED | N | R | N | N | N | N | SYSTEM |
| OPS_VERIFIED_OUTCOME_APPLIED | N | R | N | N | N | N | OPS |
| OPS_RETRY_REARMED / OPS_BLOCKED / OPS_MARKER_CLEARED / OPS_ANNOTATED | N | O | N | N | N | N | OPS |

(`REQUIRED_AT_OPEN` is REQUIRED on `REQUEST_OPENED` and must-be-NULL
elsewhere — aligned with the derived schema, which wins nothing: this
matrix is authoritative — and the opening backstop additionally checks
`REQUIRED_AT_OPEN = REQUIRED_AMOUNT` on the transaction-fresh head, so
the immutable UI amount-series stamp cannot be born wrong even though
it is never load-bearing. `UETR` only on acceptance-class and feed
events, per the v4 §5 UETR-persistence rule, inherited verbatim:
reject/collision UETRs are never recorded.)

Ops events carry the approval reference in the TYPED `APPROVAL_REF`
column — R on `OPS_VERIFIED_OUTCOME_APPLIED` and its paired
`OUTCOME_RECORDED(PLATFORM_VERIFIED_*)` (equal on both), and R on the
two MONEY-ENABLING single ops actions, `OPS_MARKER_CLEARED` and
`OPS_RETRY_REARMED` (the v4 §19.3-class clears carry four-eyes
authorization; a nullable approval on the event that re-opens the
road to fresh payment would make that authorization unrepresentable).
N on `OPS_BLOCKED`/`OPS_ANNOTATED` (restrictive/neutral actions) and
every non-ops type — free text in DETAIL binds nothing. The verified
pair is appended in the SAME transaction — dual-control protocol
inherited from v4 §9.3 unchanged, and the same-transaction fact is
DB-checkable via `TX_ID` (§5.3), not merely promised by the
application.

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
- **The `PAYMENT_KEY` encoding is itself identity input** and gets the
  SAME discipline: a byte-exact canonical encoding specification,
  golden vectors, and ONE shared encoder — two "reasonable" encodings
  of the same scope tuple would create two heads for one payment and
  re-derive the same idempotency keys under a fresh stream. Every
  uniqueness guarantee in this design keys on the canonical form
  existing exactly once. The NORMATIVE canonical form (consumed by
  the `PH_BIZ_BIND_CK` binding check, §5):
  `business_id || '|' || payment_type || '|' || debit_account || '|'
  || currency` — and the form is INJECTIVE by rule, not by luck: no
  component may contain `|` (the shared encoder FAILS CLOSED on one;
  intake whole-document validation classifies such a snapshot
  INVALID — without this rule, `(B, T|X, D)` and `(B, T, X|D)`
  encode identically and two real payments merge into one head). The
  head additionally CHECKs exactly three delimiters in the key. The
  leading business_id component and the `|` delimiter are FROZEN
  facts of the form.

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
   live, re-fold EVERY payment stream with a head row — open AND
   terminal, no sampling, no exclusions (events are permanent, §9,
   so the full population is always foldable) — and compare against
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
  BUSINESS_ID          VARCHAR2(64)  NOT NULL,     -- card lookup (indexed); BOUND to the key:
                                                   --   derived FROM PAYMENT_KEY by the one
                                                   --   canonical decoder at first contact, and
                                                   --   CHECK-enforced against the key's leading
                                                   --   component (an unbound copy would drop the
                                                   --   payment from its trade's fan-out worklist
                                                   --   and skip an absence-cancellation)
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
CREATE UNIQUE INDEX PH_UETR_UQ ON PAYMENT_HEAD (UETR);
-- plus, in the CREATE TABLE: the key/business binding constraint
--   CONSTRAINT PH_BIZ_BIND_CK CHECK
--     (BUSINESS_ID = SUBSTR(PAYMENT_KEY, 1, INSTR(PAYMENT_KEY,'|') - 1)
--      AND LENGTH(PAYMENT_KEY)
--          - LENGTH(REPLACE(PAYMENT_KEY,'|','')) = 3)   -- injectivity assist
-- which depends on the NORMATIVE canonical form (§3):
--   PAYMENT_KEY = business_id || '|' || payment_type || '|' ||
--                 debit_account || '|' || currency  (no '|' in components)
```

**The UETR claim lives on the head:** every UETR-bearing evidence
recording sets `PAYMENT_HEAD.UETR` in its per-event head effect, and
`PH_UETR_UQ` makes the CURRENT association unique across ALL payments
— two heads can never claim the same UETR, whichever channel (sync
acceptance, query, feed) recorded it, and the competing claim dies
loudly at ITS commit. Historical associations (a head moved on to a
successor's UETR) are covered by the §8 UNION multiplicity rule. An
event-level acceptance claim was tried and RETRACTED: it missed the
query/feed channels entirely, and it refused the LEGAL re-recording
of a CT-dedup-returned original acceptance (same payment, same UETR)
after a downgrade re-post — the head claim is a no-op update in that
case, exactly right.

The head is a CACHE with respect to truth (rebuildable from the
stream at any time) but a CONTRACT with respect to freshness: it is
updated **in the append transaction**, so there is no async projector
to fall behind and no stale-false-negative class (v1's L5 is closed by
construction, not by a reconciling sweep).

### 5.1 Write protocol — the only write path

```
1. SELECT ... FOR UPDATE on PAYMENT_HEAD (insert-on-first-contact:
   LAST_VERSION = 0, NEXT_REQUEST_ORDINAL = 1, PAID_TOTAL = 0,
   RESERVED = 0, REQUIRED_AMOUNT = NULL — NULL is the inherited
   "no valid data ever applied" value and matches the empty stream's
   fold, where 0 would fail the first witness check ever run;
   explicit initial values, never NULL+1 arithmetic; PK-race retry —
   the same idiom as v4's obligation row)
2. fold(stream)                       -- read PAYMENT_EVENT by (key, version)
3. WITNESS CHECK (fail closed): compare the fold's money outputs —
   required_amount, paid_total, reserved, AND open ordinal — against
   the locked head row (the COMPLETE money witness; omitting
   required_amount would let a mis-witnessed requirement drive an
   oversized opening). Comparison is NULL-safe: fold-NULL equals
   head-NULL (the first-contact state). ANY mismatch: abort with NO
   decision, page, and QUARANTINE: set the head's PHASE to
   WITNESS_DIVERGED through the same narrow head-only exemption as
   scheduling updates (a display/candidate-selection mutation, never
   a derivation input) — scanners skip diverged payments, so the
   quarantine needs no append and cannot itself be blocked by the
   check it serves. Repair = the head REBUILD runbook (§5.1), which
   now triggers on fence collision OR witness divergence: rebuild the
   head from the stream under the lock; if the divergence clears, the
   head was wrong — resume. If it persists, the stream itself is under
   dispute and only the §6 dual-control door may resolve it — and so
   that exit is REACHABLE, the door transaction alone runs under
   divergence with the witness check in RECORDING mode: it proceeds on
   the step-2 fold (the stream is the authority; the head is a cache),
   appends the gated pair, then REBUILDS the head from the post-append
   stream in the same transaction and clears the quarantine. Nothing
   else may write while diverged. The check is a VETO, never an
   authorization
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
UPDATE PAYMENT_HEAD h
   SET h.OPEN_REQUEST_ORDINAL = :ordinal,
       (h.OPEN_IDEMPOTENCY_KEY, h.RESERVED) =
         (SELECT e.IDEMPOTENCY_KEY, e.AMOUNT     -- copied FROM THE OPENING
            FROM PAYMENT_EVENT e                 -- ROW, never from a program
           WHERE e.ORDINAL_CLAIM =               -- variable (binding rule)
                 :key || '#' || TO_CHAR(:ordinal)),
       h.NEXT_REQUEST_ORDINAL = h.NEXT_REQUEST_ORDINAL + 1
 WHERE h.PAYMENT_KEY = :key
   AND h.OPEN_REQUEST_ORDINAL IS NULL
   AND h.NEXT_REQUEST_ORDINAL = :ordinal    -- the counter IS the ordinal
```

The head's open-request key and amount are COPIES OF THE OPENING EVENT
ROW (read back through `PE_ORDINAL_UQ` inside the same transaction) —
a program variable can therefore never put one key into the schema
claim and a different one onto the head that the wire echo checks
against; the wire key is transitively bound to the claimed key.

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
  `OUTCOME_RECORDED` / `SETTLED` — and `ENRICH_FAILED` when it names
  an ordinal — require `OPEN_REQUEST_ORDINAL = :new.REQUEST_ORDINAL`
  (except the terminal-evidence contradiction path, which must
  instead append `EVIDENCE_CONTRADICTION_RECORDED` — the trigger
  enforces that routing); `REQUEST_OPENED` requires it NULL (§5.2).
- **The dual-control pair gate — on EVERY verified outcome, open or
  closed**: `OUTCOME_RECORDED(PLATFORM_VERIFIED_*)` — for ANY ordinal
  state — is admitted only when the row at `VERSION − 1` is
  `OPS_VERIFIED_OUTCOME_APPLIED` for the SAME ordinal with the SAME
  `TX_ID` (same transaction, DB-checked — a dangling approval
  committed earlier can never be paired later) and equal
  `APPROVAL_REF`. Gating only the closed-ordinal case would let a solo
  verified outcome close the OPEN ordinal and book money with no
  approval at all. The closed-ordinal case is additionally the ONLY
  write admitted for a closed ordinal (§6). A solo
  `OPS_VERIFIED_OUTCOME_APPLIED` has no fold effect and is surfaced by
  the drift scan as an anomaly.
- **Amount equality (every outcome)**: `OUTCOME_RECORDED` of ANY code
  and `SETTLED` for the OPEN ordinal require `:new.AMOUNT = RESERVED`
  ("the request amount, restated" is an enforced equality, not a
  convention — a reject recording amount 999 for a 100 request is
  false history and is refused). Through the closed-ordinal door the
  comparison is instead against THAT ordinal's OPENING amount (one
  indexed read via `PE_ORDINAL_UQ` — NEVER against the payment-wide
  `RESERVED`, which may belong to a LATER open request). A differing
  feed amount is only insertable as `SETTLEMENT_MISMATCH_RECORDED`
  (routing enforced).
- **Key echo**: `POST_STARTED` / `POST_RESULT_RECORDED` /
  `QUERY_RESULT_RECORDED` require `:new.IDEMPOTENCY_KEY =
  OPEN_IDEMPOTENCY_KEY` — an attempt event can never cite a key other
  than its opening's, and a query result must name the key that was
  actually queried, so stale evidence for an EARLIER request's key
  cannot be recorded against the current one.
- **UETR-association binding (write-once per ordinal, first-claim
  global)**: an event carrying a UETR must either MATCH its ordinal's
  existing association (any prior UETR-bearing event of the same
  ordinal — so a resolver cannot re-attach ordinal 1's UETR to open
  ordinal 2 and let ordinal 1's stale feed reject release ordinal 2's
  reservation), or be the FIRST association of that UETR anywhere:
  no event row with this UETR exists on any OTHER payment (one probe
  on `PE_UETR_IX` — events are permanent, so the index IS the full
  history and a claim of an ABANDONED historical UETR dies at ITS
  commit, not as a later permanent matching ambiguity) and on no
  other ordinal of this payment. `PH_UETR_UQ` (§5) stays as the belt
  for two SIMULTANEOUS first claims, which committed-data probes
  cannot see.
- **Release rights (the v4 release-guard trigger, transplanted)** —
  the release predicate is a CHECK, not a convention:
  `CANCELLED_NOT_SUBMITTED` requires ZERO `POST_STARTED` rows for the
  ordinal (one indexed existence check — "provably never sent" is
  exactly this predicate, so enforce it); `REJECTED_VALIDATION`
  likewise requires no `POST_STARTED` (validation rejects are
  pre-wire) plus its same-transaction `ENRICH_FAILED(DEFINITIVE)`;
  `SUPERSEDED_OPS` requires no `POST_STARTED` — a posted claim may
  only close on evidence or through the verified door;
  `REJECTED_PROVIDER` requires a same-ordinal first-party negative
  evidence row that POST-DATES THE LATEST ATTEMPT — evidence
  `VERSION >` the version of the ordinal's latest `POST_STARTED` —
  and only terminal-class evidence qualifies:
  `POST_RESULT_RECORDED(DEFINITIVE_REJECT)`,
  `QUERY_RESULT_RECORDED(REJECTED)`, or
  `FEED_RESULT_RECORDED(REJECTED)`; `BUSINESS_REJECT` NEVER
  qualifies (it is retry-class — its negative fact expires the
  moment a later attempt starts, and consuming it would let a lost
  response on attempt 2 be closed by attempt 1's stale reject).
  Without this set, one wrong "it was never sent" / "it was
  rejected" decision after a crash releases an executed request's
  reservation and the successor pays a second time.
- **Downgrade gate**: `DOWNGRADED_FOR_REPOST` requires a same-ordinal
  `QUERY_RESULT_RECORDED(NOT_FOUND)` row post-dating the latest
  `POST_STARTED` AND the absence of ANY acceptance-class row
  (`POST_RESULT_RECORDED(ACCEPTED)`,
  `QUERY_RESULT_RECORDED(ACCEPTED | EXECUTED)`, or
  `FEED_RESULT_RECORDED(ACCEPTED)`) post-dating that same
  `POST_STARTED` — NOT_FOUND recency alone proves recording order,
  not that no acceptance intervened; without the second conjunct a
  wrong decision downgrades an ACCEPTED request, which is the
  backward transition v4 forbids (the trust-age arithmetic stays
  fold policy; EVIDENCE EXISTENCE/ABSENCE is the trigger's part). Attempt attribution
  note (contract-backed): evidence is keyed, and the engine's
  collision/dedup contract (§18 keystone) means ONE key = ONE
  engine-side instruction — key-scoped evidence is therefore
  attempt-agnostic BY CONTRACT; there is no "stale attempt's
  evidence" class distinct from false provider evidence, which every
  design fails on equally.
- **Version continuity**: every insert requires `:new.VERSION =
  LAST_VERSION + 1`, and the per-event head effect sets
  `LAST_VERSION = :new.VERSION` — closing the skipped-slot gap the
  fence alone cannot see (a unique constraint rejects duplicates, not
  holes); the drift scan additionally asserts density
  (`COUNT(*) = MAX(VERSION) = LAST_VERSION`).

**Enforcement point (Oracle-real):** the `:new`-style predicates above
are the SPECIFICATION; the implementation is a COMPOUND trigger that
validates each inserted row at after-statement time against
`PAYMENT_EVENT` and `PAYMENT_HEAD` (raising aborts the transaction —
backstop semantics preserved). The per-event apply order means every
event arrives by its own single-row `INSERT ... VALUES` — where
Oracle's mutating-table restriction does not bite — and a statement
guard FORBIDS multi-row inserts into `PAYMENT_EVENT` so no path can
reintroduce it. Proving this behavior on real Oracle is checklist
item 5 evidence, not an assumption.

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
resolution: money books per the recorded outcome and the standing rule
resumes UNDER THE INHERITED GATES (markers, park consistency — see
correction mechanics below). Nothing else unparks a contradicted
payment.

**Correction mechanics (the closed-ordinal door).** The dual-control
pair is the ONLY write admitted for a closed ordinal (§5.3 exception —
without it, the design's sole unpark path would be rejected by its own
trigger). Fold rule: an ordinal's authoritative outcome is the LATEST
outcome-class event in stream order (§4), so the verified outcome
supersedes the wrong recorded one while history keeps both. Head
effect of a superseding verified outcome: `PAID_TOTAL` adjusted by the
signed difference (verified NOT_EXECUTED over a booked EXECUTED
subtracts the amount; verified EXECUTED over a booked reject adds it).
**The delta is pinned at decide time**: the prior side of the
difference is the authoritative outcome over events with `VERSION <`
the correction's version — i.e., the step-2 fold the decision was made
from — never re-derived after the insert (a post-insert "latest
outcome" lookup would see the correction itself and apply a zero
delta, silently splitting the bookkeepers). `RESERVED` untouched — the
ordinal stays closed. The same transaction then re-evaluates the
payment on the corrected numbers, in BOTH directions and under the
INHERITED gates:

- **Shortfall direction — markers gate it (v4 §9.3 inherited
  verbatim):** a verified NOT_EXECUTED latches the `provider_rejected`
  marker exactly as the baseline's platform-verified rejection does,
  so NO successor opens automatically in the correction transaction —
  re-payment requires the marker to unlatch (strictly newer upstream
  truth) or the explicit ops re-arm/clear path. An earlier draft of
  this section promised a same-transaction successor; that contradicted
  the inherited marker semantics and is retracted.
- **Excess direction — the correction may strand an open successor:**
  if the corrected numbers show `paid_total + reserved > required`
  and a request is open, the open request is now excess commitment.
  If it is provably unsent (no `POST_STARTED`), the same transaction
  closes it `CANCELLED_NOT_SUBMITTED`. If a `POST_STARTED` exists,
  **the park does NOT lift**: the payment stays parked until the
  in-flight claim resolves through the ask path, and the §9 pre-wire
  recheck (which sees the persisting park) blocks any not-yet-sent
  wire call. The unpark is then the resolution of that claim — never
  a state in which a corrected history and a live excess request run
  concurrently.

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
  atomically; a crash before commit leaves neither. An UNMATCHED
  terminal delivery's inbox row additionally carries the EVIDENCE
  CONTENT (UETR, result class, amount, payload reference) — a status
  flag without the payload would be unresolvable by the sweep, so the
  content is SHAPE-BOUND, not conventional: a CHECK requires the
  evidence columns NOT NULL on UNMATCHED_TERMINAL rows (amount
  optional only for reject-class), the class drawn from the CLOSED
  vocabulary (`SETTLED` / `REJECTED` / `MISMATCH` — a misspelled
  class would be a permanently unroutable purge-exempt row), and NULL
  on plain PROCESSED rows. Resolution is TWO schema-distinguished
  exits, evidence content RETAINED on both (a bare "resolved" flag
  would let one wrong ops click bury live terminal evidence
  unrecoverably and unauditably): `RESOLVED_MATCHED` — the sweep's
  exit, in ONE transaction with the resulting append under the
  payment's head lock, the row recording WHICH append (payment key +
  version), so the flag is bound to an event, never free-standing;
  and `RESOLVED_DISPOSED` — the ops exit for genuinely foreign
  evidence, requiring actor + four-eyes approval reference + reason
  on the row, shape-CHECKed like every other money-adjacent ops
  action. Both age out on the purge chain; inbox purge NEVER removes
  an UNMATCHED_TERMINAL row.
- **Snapshot deliveries (multi-payment):** NO inbox row at all, and an
  EXPLICIT transaction boundary. The ADMISSION transaction updates
  `LAST_SEEN_SEQ` always, and additionally `LAST_ACCEPTED_SEQ` +
  digest + XML pointer only when whole-document validation PASSES —
  an invalid snapshot advances what the trade has SEEN without ever
  becoming accepted truth. FAN-OUT then runs as separate per-payment
  transactions, each of which (1) locks `TRADE_HEAD` (the v4 lock
  order), (2) verifies its carried snapshot seq still EQUALS
  `LAST_SEEN_SEQ` — the **equality fence**: if a newer arrival owns
  the trade, abort — then (3) locks the payment head and appends
  seq-guarded. **The current owner fans out the FULL current trade
  state, not just its own document** (this is what makes "the newer
  fan-out covers everything" true): per payment, FIRST the accepted
  truth's `REQUIRED_AMOUNT_SET` from the STORED accepted snapshot
  (seq-guarded — catching up any accepted admission whose own fan-out
  was fenced out, including cancels-to-zero), THEN, if the seen
  snapshot is invalid (`LAST_SEEN_SEQ > LAST_ACCEPTED_SEQ`),
  `SNAPSHOT_INVALID_MARKED(LAST_SEEN_SEQ)`. An invalid-only fan-out
  that skipped the catch-up would starve an already-accepted
  cancellation behind the fence and let a cancelled payment post.
  Worklist = payments named in the stored ACCEPTED snapshot ∪ existing
  head rows of the trade (an invalid document's own payment list is
  never trusted). Resume after a crash re-derives the worklist from
  the CURRENT watermarks' stored state, never from an in-memory
  snapshot, so a stale resumed worker can neither create nor touch a
  payment from superseded trade truth. Kafka ack only after fan-out
  completes; redelivery re-runs and converges. Side effects (metrics,
  alerts) key on state CHANGES (an append that actually happened), so
  re-runs do not re-fire them.
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
- **Feed matching multiplicity (explicit, counted in PAYMENTS not
  rows, over the UNION of both sources):** the candidate set is
  DISTINCT `PAYMENT_KEY`s from head matches ∪ event-index matches —
  BOTH always consulted (a head-first shortcut would return one head
  match and never see that the event index names a DIFFERENT payment
  for the same UETR, silently booking the wrong one; two indexed
  queries are trivial at this volume). One payment carrying the same
  UETR on several events resolves as ONE candidate. 0 payments →
  unmatched path: the inbox row is written with
  `MATCH_STATUS = UNMATCHED_TERMINAL` — a DURABLE fact, so paging is
  LEVEL-TRIGGERED from data (a sweep re-matches and pages while any
  such row exists; a crash between commit and page loses nothing,
  and a later-created head is found by the re-match), never an
  edge-triggered side effect that a crash can drop; 1 → lock that
  payment, then RE-RUN the union query INSIDE the transaction before
  deciding (the initial lookup is unserialized check-then-act — a
  concurrent acceptance claiming the same UETR on another payment
  commits between lookup and lock, and booking on the stale set
  would seal the wrong payment under the inbox); recheck still 1 →
  fold + append; recheck 2+ → abort, CRITICAL, nothing committed,
  redelivery retries. 2+ DISTINCT payments at any point → CRITICAL
  anomaly, no state change. The post-recheck race is caught BY THE
  SCHEMA, not by a promise: `PH_UETR_UQ` (§5 — one HEAD per UETR,
  every association channel funnels through the head effect) makes
  the competing same-UETR claim die loudly at ITS commit with a
  constraint failure + CRITICAL page. UETR uniqueness is a platform
  contract fact (§18 class); its violation is now a loud event,
  never a silent double-booking.

## 9. Operational inheritances (unchanged from v4, restated as binding)

Posting freeze in Hazelcast (outside the DB; absent = frozen);
write-ahead rule (identity + payload hash durable before the wire —
here structural: `POST_STARTED` IS the durable claim, and "no
POST_STARTED = provably never sent" is the release predicate) — with
one mandatory addition: between the COMMIT of `POST_STARTED` and the
wire call the worker re-reads the head (no lock) and SKIPS the send if
the payment is parked/blocked, in WITNESS_DIVERGED quarantine (a
diverged payment must not reach the wire on a claim decided from
disputed numbers), or the ordinal is no longer open; the
committed claim then resolves through the standard §9.1-style ask path
under the park (it is NOT provably unsent, so it is never released —
only asked about). This narrows the irreducible commit-to-wire window
to the recheck-to-send gap (honesty box item 7);
evidence precedence and release rights (§9.4/§10.1 semantics live in
the fold, golden-vector tested); trust-age / downgrade / escalation
clocks (event timestamps are the episode anchors — set-once by
immutability, the v4 clock-discipline problem disappears); engine
collision contract as the keystone, proven by the §18 item-1 sandbox
test before go-live.

**Permanence (the archival decision — REMOVAL, not machinery):**
`PAYMENT_HEAD`, `TRADE_HEAD`, AND `PAYMENT_EVENT` rows are all
PERMANENT for the system's operational lifetime. An earlier revision
designed event-row archival with finality windows and rehydration;
two review rounds then showed the mechanism itself breeds
CRITICAL-class defects faster than it can be fenced (archived
`IDEM_CLAIM` rows leave the unique index and un-forget burned keys;
rehydration cannot pass the version-continuity trigger it must use;
the required evidence-lateness eligibility bound is not an obtainable
contract fact; archived requests vanish from the view). The honest
remediation is removal: at ~3,000 trades/day the event table grows by
well under a million rows a year — decades of headroom for Oracle
with `PE_KEY_IX` — so nothing forces rows out of the table.
Storage tiering, if ever wanted, is PARTITIONING that never removes
rows from the table or its global unique indexes (the claims, fence,
and view keep working unchanged) — with the maintenance rule stated,
because Oracle partition operations mark global indexes UNUSABLE by
default: every partition DDL MUST use index-maintaining form
(`UPDATE INDEXES` / online operations), and writes may resume only
after verifying every `PE_*` unique index reports VALID — an
unusable claims index is a global liveness outage, checklist-5
evidence territory. Compliance-deletion pressure lands
on the PII VAULT (00-README item 6: no erasable PII in events, ever),
never on event rows. The heads never forget, the claims never leave
the indexes, and the entire restore/matching story needs no
archive-aware branch.

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
4. **Event-table growth.** Rows are PERMANENT by design (§9 — the
   archival mechanism was removed after review proved it a defect
   factory; partitioning that never leaves the indexes is the only
   sanctioned tiering). Under a million rows a year at this volume:
   decades of headroom. The accepted residue is that this design
   answer is volume-dependent — a future 100× volume would reopen
   the question, and would have to solve the archived-claims /
   rehydration problems this design deliberately refused to carry.
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
