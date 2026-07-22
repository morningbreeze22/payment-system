# Event-Model Schema Reference (v2)

> Status: PROPOSAL (see `00-README.md`). This is the normative schema
> reference for the refactored event model; `event-model-v2.md` is its
> companion — the what-changed-and-why document that records how every
> CRITICAL/HIGH item from the v1 draft was closed. Business semantics
> (amount rules, evidence precedence, release rights, trust-age,
> escalation, absence-as-zero) are inherited unchanged from
> `requirment-v4.md`; this document defines HOW facts are stored and
> which mechanisms make the storage safe. Where the two files disagree,
> this one is wrong — fix it here.
>
> Design input honored throughout: upstream ordering is a confirmed
> per-trade SEQUENCE NUMBER carried in the message. There is no tie
> class in this design (equal seq = identical redelivery; equal seq
> with different content = upstream defect, refuse + CRITICAL).

## 1. Physical structures — four

| Structure | Kind | Role |
|---|---|---|
| `PAYMENT_EVENT` | append-only, THE authority | everything that ever happened to a payment; per-payment total order |
| `PAYMENT_HEAD` | ONE mutable row per payment | write serialization lock, money WITNESS, open-request backstop, scanner/UI index — updated in the append transaction; rebuildable from the stream; can VETO a write, never authorize one |
| `TRADE_HEAD` | one mutable row per trade | TWO watermarks — `LAST_ACCEPTED_SEQ` (accepted truth) and `LAST_SEEN_SEQ` (newest processed, valid or invalid) — + payload digest (defect detection, not tie adjudication) + XML storage pointer; the fan-out fence checks SEEN so invalid markers can fan out (§7) |
| `INBOUND_EVENT_INBOX` | `UNIQUE(SOURCE, EVENT_ID)` | dedup of FEED deliveries only, atomic with processing (§8) |

Everything else — required amount, paid, reserved, phase, markers,
retry state, MAYBE, parked, escalation — is derived by the fold (§5).

## 2. `PAYMENT_EVENT`

```sql
CREATE TABLE PAYMENT_EVENT (
  ID                 NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  PAYMENT_KEY        VARCHAR2(200)  NOT NULL,   -- canonical scope tuple
  VERSION            NUMBER(10)     NOT NULL,   -- fence slot; the stream's total order
  EVENT_TYPE         VARCHAR2(40)   NOT NULL,
  EVENT_CODE         VARCHAR2(40),              -- typed classification, per-type CHECK-bound
  REQUEST_ORDINAL    NUMBER(10),                -- WHICH request this event concerns (identity input, §3)
  UPSTREAM_SEQ       NUMBER(19),                -- confirmed upstream sequence (snapshot-derived events only)
  AMOUNT             NUMBER(18,3),              -- same domain/scale rules as the baseline amount columns
  REQUIRED_AT_OPEN   NUMBER(18,3),              -- display stamp on REQUEST_OPENED only; never load-bearing
  IDEMPOTENCY_KEY    VARCHAR2(128),
  PAYLOAD_HASH       VARCHAR2(64),
  UETR               VARCHAR2(64),
  PROVIDER_REFERENCE VARCHAR2(128),
  PROVIDER_CODE      VARCHAR2(64),
  EVIDENCE_SOURCE    VARCHAR2(16),              -- SYNC_RESPONSE / QUERY / FEED / OPS / SYSTEM
  APPROVAL_REF       VARCHAR2(64),              -- dual-control approval id, TYPED: R on the §6.3
                                                --   correction pair (equal on both), N elsewhere
  ACTOR              VARCHAR2(64)   NOT NULL,   -- SYSTEM / SCANNER / RESOLVER / OPS:<user>
  DETAIL             VARCHAR2(1000),            -- human text; the fold NEVER reads it
  TX_ID              VARCHAR2(64),              -- stamped BY THE GUARD TRIGGER with the local
                                                --   transaction id; writers cannot supply it
  CREATED_AT         TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,

  -- identity is claimed exactly once, in the schema:
  IDEM_CLAIM         VARCHAR2(128)  GENERATED ALWAYS AS
                       (CASE WHEN EVENT_TYPE = 'REQUEST_OPENED' THEN IDEMPOTENCY_KEY END),
  -- and so is the ordinal (one opening per (payment, ordinal), ever):
  ORDINAL_CLAIM      VARCHAR2(220)  GENERATED ALWAYS AS
                       (CASE WHEN EVENT_TYPE = 'REQUEST_OPENED'
                             THEN PAYMENT_KEY || '#' || TO_CHAR(REQUEST_ORDINAL) END),

  CONSTRAINT PE_FENCE_UQ   UNIQUE (PAYMENT_KEY, VERSION), -- backstop fence (§6.1)
  CONSTRAINT PE_IDEM_UQ    UNIQUE (IDEM_CLAIM),           -- identity write-once
  CONSTRAINT PE_ORDINAL_UQ UNIQUE (ORDINAL_CLAIM),        -- ordinal write-once
  CONSTRAINT PE_SOURCE_CK  CHECK (EVIDENCE_SOURCE IS NULL OR EVIDENCE_SOURCE IN
      ('SYNC_RESPONSE','QUERY','FEED','OPS','SYSTEM')),
  CONSTRAINT PE_TYPE_CK  CHECK (EVENT_TYPE IN (
      'REQUIRED_AMOUNT_SET','SNAPSHOT_INVALID_MARKED',
      'REQUEST_OPENED','ENRICH_FAILED','POST_STARTED','POST_RESULT_RECORDED',
      'QUERY_RESULT_RECORDED','DOWNGRADED_FOR_REPOST',
      'OUTCOME_RECORDED','SETTLED','SETTLEMENT_MISMATCH_RECORDED',
      'EVIDENCE_CONTRADICTION_RECORDED','ESCALATION_MARKED',
      'OPS_VERIFIED_OUTCOME_APPLIED','OPS_RETRY_REARMED',
      'OPS_BLOCKED','OPS_MARKER_CLEARED','OPS_ANNOTATED'))
  -- plus ONE shape CHECK per type, derived mechanically from the §2.2
  -- matrix. THE COMPLETE 18-CONSTRAINT SET IS A BUILD DELIVERABLE:
  -- generated by a small tool from the matrix and proven equal to it
  -- by a parity self-test (every R/N/O cell exercised by an
  -- insert-must-fail / insert-must-pass pair) — comments and Markdown
  -- enforce nothing; only the generated set does. Representative
  -- examples of what the generator emits (each covers EVERY governed
  -- column of its row, including EVIDENCE_SOURCE):
  , CONSTRAINT PE_SHAPE_RESULT_CK CHECK (EVENT_TYPE != 'POST_RESULT_RECORDED'
      OR (EVENT_CODE IS NOT NULL
          AND EVENT_CODE IN ('ACCEPTED','BUSINESS_REJECT','DEFINITIVE_REJECT',
                             'AMBIGUOUS','COLLISION','UNMAPPED')
          AND REQUEST_ORDINAL IS NOT NULL AND IDEMPOTENCY_KEY IS NOT NULL
          AND EVIDENCE_SOURCE = 'SYNC_RESPONSE'
          AND UPSTREAM_SEQ IS NULL AND AMOUNT IS NULL
          AND PAYLOAD_HASH IS NULL AND REQUIRED_AT_OPEN IS NULL
          AND (EVENT_CODE = 'ACCEPTED' OR UETR IS NULL)))  -- O-only-when rule
  , CONSTRAINT PE_SHAPE_OPEN_CK CHECK (EVENT_TYPE != 'REQUEST_OPENED'
      OR (EVENT_CODE IS NULL AND REQUEST_ORDINAL IS NOT NULL
          AND AMOUNT IS NOT NULL AND AMOUNT > 0
          AND IDEMPOTENCY_KEY IS NOT NULL AND PAYLOAD_HASH IS NOT NULL
          AND REQUIRED_AT_OPEN IS NOT NULL
          AND EVIDENCE_SOURCE IN ('SYSTEM','OPS')
          AND UPSTREAM_SEQ IS NULL AND UETR IS NULL))
  , CONSTRAINT PE_SHAPE_REQ_CK CHECK (EVENT_TYPE != 'REQUIRED_AMOUNT_SET'
      OR (EVENT_CODE IS NULL AND UPSTREAM_SEQ IS NOT NULL
          AND AMOUNT IS NOT NULL AND AMOUNT >= 0        -- 0 = removal (BA-2)
          AND EVIDENCE_SOURCE = 'SYSTEM'
          AND REQUEST_ORDINAL IS NULL AND IDEMPOTENCY_KEY IS NULL
          AND PAYLOAD_HASH IS NULL AND UETR IS NULL AND REQUIRED_AT_OPEN IS NULL))
);
CREATE INDEX PE_KEY_IX  ON PAYMENT_EVENT (PAYMENT_KEY, VERSION);
CREATE INDEX PE_UETR_IX ON PAYMENT_EVENT (UETR);            -- feed-matching assist
```

Grants: the application role has INSERT and SELECT only. A guard
trigger raises on any UPDATE or DELETE — history is immutable against
every writer, including humans in an incident. (Stream-granular
archival, §10, runs under a separate maintenance role in a controlled
window with the guard's documented archive exemption — never the
application role.)

### 2.1 Why each column exists

| Column | Why |
|---|---|
| `PAYMENT_KEY` | the §1 scope tuple, canonically encoded — one stream per payment. Trade membership is derivable from the key. |
| `VERSION` | the fence slot and the stream's total order. Dense, starts at 1; allocated as `PAYMENT_HEAD.LAST_VERSION + 1` under the head lock (§6.1). No reliance on timestamps or identity columns for ordering. |
| `EVENT_TYPE` | closed vocabulary, CHECK-bound. Adding a type is a design change, not a code convenience. |
| `EVENT_CODE` | the STRUCTURED classification the fold branches on — a decision RECORDED AT THE TIME (e.g. the CA-1 mapping of a raw provider code), never re-derived later from `PROVIDER_CODE`: re-mapping raw codes during a fold would be load-bearing replay of a mutable mapping, forbidden exactly as in the baseline. |
| `REQUEST_ORDINAL` | which request the event concerns. The identity input (§3) and the join key for the request-granular view (§9). NOT the version slot — that is the L6 fix. |
| `UPSTREAM_SEQ` | carried only on snapshot-derived events; the strictly-newer guard and marker unlatching compare against it. |
| `AMOUNT` | the event's own amount where meaningful (required amount; request amount; outcome amount restated; settled amount; mismatched amount). Money facts carry their numbers — the fold aggregates, never infers (§5). |
| `REQUIRED_AT_OPEN` | the UI amount-series stamp on the opening event. Immutable by nature — no set-once discipline, no capture-boundary machinery needed. Never load-bearing. |
| `IDEMPOTENCY_KEY` | present on `REQUEST_OPENED` (the allocation, §3) and echoed on `POST_STARTED` / `POST_RESULT_RECORDED` so the stream is self-describing about what went to the wire. |
| `PAYLOAD_HASH` | §5.1-style write-ahead instruction hash on `REQUEST_OPENED` and every `POST_STARTED`. A re-POST whose hash differs from the previous `POST_STARTED` is expected-divergent (re-enrichment happened); collision classification builds on this being durable BEFORE the wire call. |
| `UETR` / `PROVIDER_REFERENCE` / `PROVIDER_CODE` | provider evidence. UETR persisted only from acceptance-class responses and feed events (platform-SDK rule inherited verbatim; reject/collision UETRs never recorded). |
| `EVIDENCE_SOURCE` | which channel produced the event — evidence-precedence rules key on it. |
| `APPROVAL_REF` | the dual-control approval id, TYPED so the §6.3 door can bind the correction pair — free text in `DETAIL` binds nothing. |
| `TX_ID` | guard-trigger-stamped local transaction id: the DB-checkable fact "these rows were written together," consumed only by the §6.3 correction-door check. Writers cannot supply it. |
| `ACTOR` | audit. |
| `DETAIL` | display/audit text only. THE FOLD NEVER READS IT — a design rule, enforced by the fold's golden vectors. |
| `IDEM_CLAIM` (generated) | identity write-once IN THE SCHEMA: two opening events can never carry the same key, whatever writer, path, or bug produced them. |
| `ORDINAL_CLAIM` (generated) | ordinal write-once IN THE SCHEMA: one `REQUEST_OPENED` per (payment, ordinal), ever — ordinal reuse collides loudly instead of splitting the fold's per-ordinal aggregation. Single-column unique on a generated key-qualified string, so non-opening rows (NULL) are simply not indexed. |

### 2.2 Complete shape matrix (normative)

R = required (`IS NOT NULL`, plus the listed domain), N = must be NULL
(a real CHECK conjunct, not a convention), O = optional.

**Derivation rule for the DDL:** one CHECK constraint per type,
generated mechanically from this table — every R cell becomes an
`IS NOT NULL` conjunct (with its IN-list or range), every N cell an
`IS NULL` conjunct. Every classified type's EVENT_CODE check carries
the explicit `IS NOT NULL`: under Oracle three-valued logic a bare
IN-list CHECK evaluates UNKNOWN on NULL and silently PASSES. The
matrix is the normative artifact; the complete generated constraint
set (§2 DDL note) must be proven equal to it by the parity self-test —
a matrix/DDL mismatch is a defect.

| EVENT_TYPE | EVENT_CODE | ORDINAL | UPSTREAM_SEQ | AMOUNT | IDEM_KEY | PAYLOAD_HASH | UETR | REQUIRED_AT_OPEN | EVIDENCE_SOURCE |
|---|---|---|---|---|---|---|---|---|---|
| REQUIRED_AMOUNT_SET | N | N | R | R (≥0; 0 = removal) | N | N | N | N | SYSTEM |
| SNAPSHOT_INVALID_MARKED | N | N | R | N | N | N | N | N | SYSTEM |
| REQUEST_OPENED | N | R | N | R (>0) | R | R | N | R | SYSTEM/OPS |
| ENRICH_FAILED | R: TRANSIENT, DEFINITIVE | R | N | N | N | N | N | N | SYSTEM |
| POST_STARTED | N | R | N | N | R | R | N | N | SYSTEM |
| POST_RESULT_RECORDED | R: ACCEPTED, BUSINESS_REJECT, DEFINITIVE_REJECT, AMBIGUOUS, COLLISION, UNMAPPED | R | N | N | R | N | O (ACCEPTED only) | N | SYNC_RESPONSE |
| QUERY_RESULT_RECORDED | R: EXECUTED, REJECTED, ACCEPTED, NOT_FOUND, LOOKBACK_EXPIRED | R | N | N | R (the key QUERIED — echo-checked §6.3) | N | O (EXECUTED/ACCEPTED only) | N | QUERY |
| DOWNGRADED_FOR_REPOST | N | R | N | N | N | N | N | N | SYSTEM/OPS |
| OUTCOME_RECORDED | R: EXECUTED, REJECTED_VALIDATION, REJECTED_PROVIDER, CANCELLED_NOT_SUBMITTED, SUPERSEDED_OPS, PLATFORM_VERIFIED_EXECUTED, PLATFORM_VERIFIED_NOT_EXECUTED | R | N | R (the request amount, restated) | N | N | O (executed-class only) | N | R (any) |
| SETTLED | N | R | N | R | N | N | O | N | FEED |
| SETTLEMENT_MISMATCH_RECORDED | N | R | N | R (the wrong amount) | N | N | O | N | FEED |
| EVIDENCE_CONTRADICTION_RECORDED | R: SETTLED_AFTER_TERMINAL, MISMATCH_AFTER_TERMINAL, QUERY_CONTRADICTS_OUTCOME | R | N | O | N | N | O | N | R (any) |
| ESCALATION_MARKED | N | R | N | N | N | N | N | N | SYSTEM |
| OPS_VERIFIED_OUTCOME_APPLIED | N | R | N | N | N | N | N | N | OPS |
| OPS_RETRY_REARMED / OPS_BLOCKED / OPS_MARKER_CLEARED / OPS_ANNOTATED | N | O | N | N | N | N | N | N | OPS |

(The "only" qualifiers on O cells — e.g. UETR only when the code is
acceptance-class — are conditional conjuncts inside the same per-type
CHECK.)

Two columns are governed globally rather than per row: `APPROVAL_REF`
is R on `OPS_VERIFIED_OUTCOME_APPLIED` and on
`OUTCOME_RECORDED(PLATFORM_VERIFIED_*)` — equal across the §6.3 pair —
and N on every other type/code; `TX_ID` is stamped by the guard
trigger on EVERY row and can never be writer-supplied (it is how the
correction door proves same-transaction membership in the database).

## 3. Identity — request ordinal, not stream position

```
request_ordinal  = 1 + (count of prior REQUEST_OPENED events in this stream)
                   — maintained as PAYMENT_HEAD.NEXT_REQUEST_ORDINAL
                     (initialized to 1 at insert-on-first-contact — an
                     explicit value, never NULL+1 arithmetic), consumed
                     and stamped onto the opening event in the opening
                     transaction; write-once by the §2.2 shape check
idempotency_key  = hash(business_id | payment_type | debit_account |
                        currency | request_ordinal)
                   — byte-exact spec + golden vectors, identical
                     discipline to the baseline §5.1
```

Version slots do NOT participate in identity. Consequences:

- A database restore + Kafka replay regenerates the SAME ordinals from
  business history — recreated requests carry the SAME keys, the
  engine rejects the collision, and the baseline §5.2 restore runbook
  applies essentially unchanged: posting freeze lives in Hazelcast
  (outside the database), and the burned-key sweep enumerates
  `hash(scope | 1..N+K)` exactly as the baseline does.
- `PE_IDEM_UQ` on the generated `IDEM_CLAIM` is the schema-level
  write-once guarantee, independent of any writer's correctness.
- The ordinal itself is schema-bound: `PE_ORDINAL_UQ` makes ordinal
  reuse collide loudly, and the §6.2 opening CAS carries
  `NEXT_REQUEST_ORDINAL = :ordinal`, so a stale or skipped ordinal
  aborts on row count 0 — counter, ordinal, and claim cannot drift
  apart. What remains code discipline (stated honestly): the KEY
  DERIVATION content — that the hash was computed over THIS ordinal —
  under the same golden-vector regime as the baseline §5.1.
- No epoch/generation machinery is required for the main hazard (an
  epoch component remains a cheap optional hardening).

## 4. Event vocabulary — when appended, what it means to the fold

| Event | Appended when | Fold effect |
|---|---|---|
| `REQUIRED_AMOUNT_SET` | an ADMITTED snapshot names this payment (incl. amount 0 = cancel-to-zero; absence-means-cancel per BA-2 produces an explicit 0) | `required := amount` if `UPSTREAM_SEQ` strictly newer; markers with older seq unlatch |
| `SNAPSHOT_INVALID_MARKED` | whole-snapshot validation failed at admission — appended to EVERY payment of the trade, in payment_key order | blocks NEW request opening; never touches in-flight work; unlatched by a newer valid `REQUIRED_AMOUNT_SET` |
| `REQUEST_OPENED` | the standing rule decides to pay a shortfall: claims ordinal + identity + amount + payload hash (write-ahead part 1) | an OPEN request exists; reservation = its amount; at most one open request is DB-backstopped (§6.2), not merely a fold invariant |
| `ENRICH_FAILED` | enrichment failed; code says transient vs definitive | transient: retry timing derives from this event's timestamp + policy; definitive: appended with `OUTCOME_RECORDED(REJECTED_VALIDATION)` in one tx |
| `POST_STARTED` | immediately BEFORE the wire call (write-ahead part 2). Its existence is the durable fact "the wire MAY have been reached". **Mandatory pre-wire recheck:** between COMMIT and the wire call the worker re-reads the head (no lock) and SKIPS the send if the payment is parked/blocked or the ordinal is no longer open — the claim then resolves via the ask path under the park | request is posting/ambiguous until a result follows; **no `POST_STARTED` = provably never sent** — the safe-release predicate |
| `POST_RESULT_RECORDED` | the synchronous response, classified per CA-1 | ACCEPTED → awaiting settlement; BUSINESS_REJECT → retry per policy (same key); DEFINITIVE_REJECT → outcome same tx; AMBIGUOUS → MAYBE; COLLISION → expected/unexpected via hash comparison; UNMAPPED → MAYBE + alert |
| `QUERY_RESULT_RECORDED` | the resolver asked by OUR key — the event CARRIES that key, and the §6.3 echo refuses a key other than the open request's, so stale evidence for an earlier request's key cannot be recorded against the current one | EXECUTED → outcome same tx; REJECTED → `OUTCOME_RECORDED(REJECTED_PROVIDER)` same tx; ACCEPTED → still in flight, keep waiting (submission knowledge tightens); NOT_FOUND young → no change (trust age); NOT_FOUND past trust age → enables the one sanctioned downgrade; LOOKBACK_EXPIRED → stays MAYBE (ops path) |
| `DOWNGRADED_FOR_REPOST` | the §9.2-equivalent move: NOT_FOUND past trust age, same key will be re-sent | re-posting becomes legal for the SAME key; audit of the only backward transition |
| `OUTCOME_RECORDED` | terminal for a request (codes per §2.2, with `EVIDENCE_SOURCE`); AMOUNT of EVERY code must equal the OPENED amount (§6.3 — "restated" is an enforced equality; any other number is defect evidence, not an outcome). `PLATFORM_VERIFIED_*` may additionally supersede a CLOSED ordinal's outcome through the §6.3 dual-control door (amount checked against THAT ordinal's opening amount) | closes the open request (head CAS, §6.2); books or releases its reservation; latches the corresponding marker; the same transaction re-evaluates the standing rule UNDER THE INHERITED GATES — a shortfall opens a successor only if no live marker forbids it (a verified NOT_EXECUTED latches `provider_rejected` per the baseline §9.3: NO automatic successor); a corrected EXCESS (`paid + reserved > required`) cancels a provably-unsent open request in the same tx, or keeps the payment PARKED until an in-flight claim resolves. A superseding verified outcome adjusts `PAID_TOTAL` by the signed difference; the fold takes the LATEST outcome-class event per ordinal as authoritative (§5) |
| `SETTLED` | the feed confirms full-amount settlement AND the fold shows a non-terminal request for the ordinal | books confirmed money (idempotent by ordinal); closes/freezes that request. Feed evidence AGREEING with an already-EXECUTED terminal (equal amount) is a benign no-op delivery — NOT appended, NOT a contradiction |
| `SETTLEMENT_MISMATCH_RECORDED` | feed amount ≠ instructed amount (all-or-nothing engine ⇒ defect evidence) | books NOTHING; parks loudly; submission knowledge still tightens |
| `EVIDENCE_CONTRADICTION_RECORDED` | evidence CONFLICTS with a terminal decision (codes per §2.2) — conflict, never mere repetition | FIXED effect: book nothing, PARK the payment, CRITICAL alert; sole exit is §7 of `event-model-v2.md` (§6 there): the dual-control verified outcome |
| `ESCALATION_MARKED` | the MAYBE got old (once per episode) | audit of paging; derived state unchanged |
| `OPS_VERIFIED_OUTCOME_APPLIED` | the dual-control audited operation (typed `APPROVAL_REF`, equal on both pair members) | appended with its `OUTCOME_RECORDED(PLATFORM_VERIFIED_*)` in one tx — the only manual door for possibly-moved money, the only unpark of a contradicted payment, and (as a DB-verified pair: version-adjacent, same ordinal, same `TX_ID`, equal `APPROVAL_REF`) the ONLY write admitted for a CLOSED ordinal (§6.3). A solo verified-applied event has NO fold effect and pages as an anomaly |
| `OPS_RETRY_REARMED` / `OPS_BLOCKED` / `OPS_MARKER_CLEARED` / `OPS_ANNOTATED` | human actions through the ops surface (`OPS_MARKER_CLEARED` = the §19.3-equivalent clear of a live reject marker) | budget reset / hard block on new opens / marker cleared / display note. All arrive through the SAME write path — there is no privileged one |

## 5. The canonical fold — aggregation, never inference

**Rule (normative):** every money-bearing fact is an explicit event
carrying its own amount and its own classification, recorded at
decision time. The fold may AGGREGATE money events; it may never
re-classify them, re-derive them from raw provider codes, or infer an
amount not present on an event.

```
required_amount = AMOUNT of the REQUIRED_AMOUNT_SET with highest UPSTREAM_SEQ
authoritative_outcome(ordinal)
                = the LATEST outcome-class event (OUTCOME_RECORDED or
                  SETTLED) for the ordinal in stream order —
                  PLATFORM_VERIFIED_* appended through the dual-control
                  door SUPERSEDES the outcome it corrects
paid_total      = Σ AMOUNT over request ordinals whose authoritative
                  outcome is executed-class (EXECUTED |
                  PLATFORM_VERIFIED_EXECUTED | SETTLED) (idempotent by ordinal)
reserved        = AMOUNT of the open request (opening event exists,
                  no outcome/settled event for its ordinal), else 0
shortfall       = required − paid_total − reserved    (standing-rule input)
plus derived:     markers (+ their seqs), retry state, MAYBE/park/escalation
                  ages, provably_unsent (open request with no POST_STARTED)
```

Amount binding (normative): EVERY outcome-class event carries AMOUNT
equal to its ordinal's OPENING amount — the all-or-nothing contract
makes any other number defect evidence (`SETTLEMENT_MISMATCH_RECORDED`
or a contradiction), and §6.2/§6.3 enforce the equality in the
database, so a wrong terminal number cannot silently enter either
bookkeeper, reopen a phantom shortfall, or plant false immutable
history.

Correction gates (normative): a superseding verified outcome
re-evaluates the payment in BOTH directions under the INHERITED rules —
verified NOT_EXECUTED latches `provider_rejected` (baseline §9.3), so
NO automatic successor; corrected excess (`paid + reserved > required`)
cancels a provably-unsent open request in the same transaction or
keeps the payment PARKED until the in-flight claim resolves (the §4
pre-wire recheck sees the persisting park and blocks the send).

One shared fold artifact — semantically versioned, golden-vector
frozen; no consumer (UI, scanner, resolver, ops surface) re-implements
any part of it. Fold rules replicate the baseline §4/§6/§7/§9/§10
semantics verbatim; those sections remain the specification. Fold
changes are governed by the deploy gate + continuous drift scan of
`event-model-v2.md` §4.2 — the head witness (§6) is the independent
second bookkeeper both compare against.

## 6. `PAYMENT_HEAD` — lock, witness, backstop, index

```sql
CREATE TABLE PAYMENT_HEAD (
  PAYMENT_KEY          VARCHAR2(200) PRIMARY KEY,
  BUSINESS_ID          VARCHAR2(64)  NOT NULL,     -- card lookup (indexed)
  LAST_VERSION         NUMBER(10)    NOT NULL,     -- must equal stream max
  NEXT_REQUEST_ORDINAL NUMBER(10)    NOT NULL,     -- identity counter (§3)
  OPEN_REQUEST_ORDINAL NUMBER(10),                 -- NULL = no open request
  OPEN_IDEMPOTENCY_KEY VARCHAR2(128),              -- the open request's key (echo check, §6.3)
  -- money WITNESS (mechanical increments; can VETO, never authorize):
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

A CACHE with respect to truth (rebuildable from the stream at any
time) but a CONTRACT with respect to freshness: updated in the append
transaction — no async projector, no stale-false-negative class. It
can VETO a write (§6.1 step 3 witness check, §6.2 CAS, §6.3 triggers);
it never AUTHORIZES one — authorization always derives from the fold.

### 6.1 Write protocol — the only write path

```
1. SELECT ... FOR UPDATE on PAYMENT_HEAD (insert-on-first-contact:
   LAST_VERSION = 0, NEXT_REQUEST_ORDINAL = 1, witness columns 0;
   PK-race retry — the baseline's obligation-row idiom)
2. fold(stream)                      -- read PAYMENT_EVENT by (key, version)
3. WITNESS CHECK (fail closed): fold money outputs (paid_total,
   reserved, open ordinal) must equal the locked head row.
   ANY mismatch: abort with NO decision, page, park the payment —
   the §4.2 drift scan made synchronous, a veto never an authorization
4. decide                            -- pure function: fold state -> events
5. FOR EACH decided event, IN ORDER:
     INSERT it at LAST_VERSION+1 (+2, ...),
     THEN apply ITS head effect (§6.2 CAS / witness change / phase)
6. COMMIT                            -- multi-event decisions are ONE tx
```

Step 5 is **per event, not batched** — each backstop check runs
against the head state its predecessors in the same transaction left
behind. The atomic outcome+successor decision is legal only in this
interleaving: outcome insert (its ordinal still open) → close CAS →
successor `REQUEST_OPENED` insert (column now NULL) → open CAS.

The head lock serializes writers per payment; the only lock-order rule
is TRADE_HEAD → PAYMENT_HEAD (sorted) during snapshot fan-out — the
baseline's exact order. `PE_FENCE_UQ` stays as the backstop against
lock-bypassing writers. A fence collision seen by a writer HOLDING the
lock means the head lost sync with the stream: page, stop the stream's
writes, rebuild the head from the stream (the cache's rebuild path,
exercised in tests), resume.

Narrow exemption (mirrors the baseline's claim-field rule): pure
scheduling updates (`NEXT_ACTION_AT` backoff after an INDETERMINATE
query) may be written without an append — not derivation inputs, not
money.

### 6.2 The one-open-request backstop (fold-independent)

Opening, in the opening transaction:

```sql
UPDATE PAYMENT_HEAD h
   SET h.OPEN_REQUEST_ORDINAL = :ordinal,
       (h.OPEN_IDEMPOTENCY_KEY, h.RESERVED) =
         (SELECT e.IDEMPOTENCY_KEY, e.AMOUNT    -- copied FROM THE OPENING ROW
            FROM PAYMENT_EVENT e                -- (via PE_ORDINAL_UQ, same tx),
           WHERE e.ORDINAL_CLAIM =              -- never from a program variable
                 :key || '#' || TO_CHAR(:ordinal)),
       h.NEXT_REQUEST_ORDINAL = h.NEXT_REQUEST_ORDINAL + 1
 WHERE h.PAYMENT_KEY = :key
   AND h.OPEN_REQUEST_ORDINAL IS NULL
   AND h.NEXT_REQUEST_ORDINAL = :ordinal      -- the counter IS the ordinal
```

The binding rule matters: the head's open key/amount are copies of the
OPENING EVENT ROW, so the wire echo (§6.3) transitively checks the
wire key against the SCHEMA-CLAIMED key — a program variable can never
put one key into `IDEM_CLAIM` and send a different one, which would
otherwise burn an unenumerable key and defeat the §3 restore story.

Row count 0 aborts the append. Closing (outcome/settled) clears the
ordinal and key columns the same way (`WHERE OPEN_REQUEST_ORDINAL =
:ordinal`), zeroes `RESERVED`, and adds the amount to `PAID_TOTAL`
only for money-terminal codes (`EXECUTED` / `PLATFORM_VERIFIED_EXECUTED`
/ `SETTLED`) — with the money-terminal close additionally carrying
`AND RESERVED = :amount`: the witness binds not just WHICH ordinal
closes but AT WHAT NUMBER (§5 amount binding made mechanical).

**Witness-exactly-once:** §6.3 admits money-bearing events for an
ordinal only WHILE that ordinal is the open one, and the close CAS
fires exactly once per ordinal — so `PAID_TOTAL` increments exactly
once per request, whatever late confirming evidence arrives later
(that is a no-op or a contradiction event, never a second increment).
The sole exception is deliberate: a §6.3 dual-control superseding
outcome adjusts `PAID_TOTAL` by the signed difference — a correction,
itself an appended, audited fact.

### 6.3 Cross-row trigger backstops

BEFORE-INSERT triggers on `PAYMENT_EVENT` (each a single indexed head
read under the lock already held):

- **Open-ordinal**: `POST_STARTED` / `POST_RESULT_RECORDED` /
  `OUTCOME_RECORDED` / `SETTLED` require `OPEN_REQUEST_ORDINAL =
  :new.REQUEST_ORDINAL` — except the contradiction path, which must
  instead append `EVIDENCE_CONTRADICTION_RECORDED` (routing enforced);
  `REQUEST_OPENED` requires the column NULL (§6.2).
- **The one closed-ordinal exception** (the correction door):
  `OUTCOME_RECORDED(PLATFORM_VERIFIED_*)` for a closed ordinal is
  admitted only when the row at `VERSION − 1` is
  `OPS_VERIFIED_OUTCOME_APPLIED` for the SAME ordinal with the SAME
  `TX_ID` (same-transaction membership, DB-checked — a dangling
  approval committed earlier can never be paired later) and equal
  `APPROVAL_REF` — the dual-control unpark/correction path, nothing
  else. A solo verified-applied event has no fold effect; the drift
  scan pages it.
- **Amount equality (every outcome)**: `OUTCOME_RECORDED` of ANY code
  and `SETTLED` on the OPEN ordinal require `:new.AMOUNT = RESERVED`;
  through the closed-ordinal door the comparison is against THAT
  ordinal's OPENING amount (indexed read via `PE_ORDINAL_UQ`), never
  the payment-wide `RESERVED`, which may belong to a LATER open
  request. A differing feed amount is only insertable as
  `SETTLEMENT_MISMATCH_RECORDED`.
- **Key echo**: `POST_STARTED` / `POST_RESULT_RECORDED` /
  `QUERY_RESULT_RECORDED` require `:new.IDEMPOTENCY_KEY =
  OPEN_IDEMPOTENCY_KEY` — stale evidence for an earlier request's key
  dies at insert.
- **Version continuity**: every insert requires `:new.VERSION =
  LAST_VERSION + 1`; the per-event head effect sets `LAST_VERSION =
  :new.VERSION`. The fence rejects duplicates; this closes holes. The
  drift scan asserts density (`COUNT(*) = MAX(VERSION) = LAST_VERSION`).

All backstops are independent of fold correctness. Full temporal
legality beyond this set stays code-enforced — accepted, with
mitigations, in `event-model-v2.md` §10.

## 7. `TRADE_HEAD` — snapshot admission

```sql
CREATE TABLE TRADE_HEAD (
  BUSINESS_ID       VARCHAR2(64) PRIMARY KEY,
  LAST_ACCEPTED_SEQ NUMBER(19)   NOT NULL,   -- newest VALID snapshot (accepted truth)
  LAST_SEEN_SEQ     NUMBER(19)   NOT NULL,   -- newest snapshot processed AT ALL
                                             --   (valid or invalid; >= accepted)
  PAYLOAD_DIGEST    VARCHAR2(64) NOT NULL,   -- defect detection, not ties
  SNAPSHOT_XML_REF  VARCHAR2(200),
  UPDATED_AT        TIMESTAMP
);
```

Admission (this table's only writer) and fan-out have an EXPLICIT
transaction boundary:

- **Admission tx**: strictly newer seq → update `LAST_SEEN_SEQ`
  always; update `LAST_ACCEPTED_SEQ` + digest + XML pointer ONLY when
  whole-document validation PASSES — an invalid snapshot advances
  what the trade has SEEN without ever becoming accepted truth. Equal
  seq + equal digest → identical redelivery, admit-without-update.
  Equal seq + DIFFERENT digest → upstream DEFECT: refuse + CRITICAL
  alert (no tie workflow exists in this design). Older seq → ignore.
- **Fan-out**: separate per-payment transactions in sorted payment_key
  order, each of which (1) locks `TRADE_HEAD` (the baseline lock
  order), (2) verifies its carried snapshot seq still EQUALS
  `LAST_SEEN_SEQ` — the **equality fence**: abort if a newer arrival
  owns the trade (the newer fan-out covers every payment, including
  absences) — then (3) locks the payment head and appends seq-guarded:
  `REQUIRED_AMOUNT_SET` only from the snapshot that is ALSO accepted
  (`= LAST_ACCEPTED_SEQ`); `SNAPSHOT_INVALID_MARKED` from an invalid
  one. Already-applied streams no-op, so partial fan-out is safely
  resumable; resume re-derives the worklist from the CURRENT
  watermarks' stored state, never from an in-memory snapshot — a
  stale resumed worker can neither create nor touch a payment from
  superseded trade truth.
- Kafka ack only after fan-out completes; redelivery re-runs and
  converges.

## 8. `INBOUND_EVENT_INBOX` — delivery identity

```sql
CREATE TABLE INBOUND_EVENT_INBOX (
  SOURCE      VARCHAR2(32)  NOT NULL,
  EVENT_ID    VARCHAR2(128) NOT NULL,
  RECEIVED_AT TIMESTAMP     DEFAULT SYSTIMESTAMP NOT NULL,
  CONSTRAINT INB_UQ UNIQUE (SOURCE, EVENT_ID)
);
```

- **Feed deliveries (single-payment):** the inbox INSERT rides the
  SAME transaction as the resulting append (or the same transaction
  that decides "no-op": stale or agreeing evidence). "Seen" and
  "processed" commit atomically; a crash before commit leaves neither.
- **Snapshot deliveries (multi-payment): NO inbox row.** Dedup and
  convergence come from the §7 watermark + seq-guarded per-payment
  appends; redelivery re-runs and converges. Side effects (metrics,
  alerts) key on appends that actually happened, so re-runs do not
  re-fire them.
- Retention: inbox purge > Kafka retention ≥ replay window (owner rule
  inherited from the baseline §16.2).

## 9. Read surfaces

- **Step card (obligation-granular):** reads `PAYMENT_HEAD`
  (`UI_STEP_STATUS`, exception summary via phase) — display-only, and
  the head is transaction-fresh. Lookup by `BUSINESS_ID` returns all
  of a trade's payments; row absence = NOT_STARTED.
- **All-payments table (request-granular):** a read-only SQL VIEW over
  `PAYMENT_EVENT` — one row per `REQUEST_OPENED` (request identity =
  payment_key + ordinal), outcome joined by ordinal, obligation-only
  placeholders from head rows with `NEXT_REQUEST_ORDINAL = 1`. The
  amount series is free: `REQUIRED_AT_OPEN` sits on the opening event.
  Keyset pagination on (payment_key, version). No projection, no
  maintenance job.
- **Scanners/resolver:** select CANDIDATES from the head
  (`PHASE, NEXT_ACTION_AT`); every ACTION folds the stream under the
  head lock. A stale candidate costs a wasted fold, never a wrong
  payment.
- **Feed matching (fail-closed multiplicity):** match UETR against the
  head first, the event index second; 0 → unmatched path (ack; query
  sweep recovers by key later); 1 → fold + append under the lock;
  2+ → CRITICAL anomaly, NO state change on any stream.

## 10. What deliberately does NOT exist

- **No stored authoritative state machine** — phase/markers/retry
  state are fold-derived; the head's copies are display/candidate
  index only.
- **No mutable request row, no claim/lease columns** — the posting
  claim is the `POST_STARTED` event; writer exclusivity is the head
  lock, backstopped by the fence.
- **No projection table** — merged into the head, updated in the
  append transaction (the v1 draft's staleness class is closed by
  construction).
- **No tie machinery** — deleted with the confirmed upstream sequence
  number, not inherited.
- **No transition-history journal** — the stream IS the history; the
  external §14 log contract is unchanged.
- **What is deliberately ACCEPTED** (with mitigations): see the
  honesty box in `event-model-v2.md` §10 — code-enforced temporal
  legality beyond the §6.2/6.3 backstops, control-state
  reinterpretation on fold deploys (money side gated to zero by the
  §4.2 deploy gate), the single interpretation point, event-table
  growth, the engine collision contract as keystone (§18 item 1 gates
  go-live identically), and the ops learning curve (`fold --explain`
  ships with the MVP as a deliverable).
