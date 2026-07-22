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
| `TRADE_HEAD` | one mutable row per trade | TWO watermarks, EACH with digest + storage pointer — accepted (= trade truth; NULL until the first valid snapshot) and seen (newest processed, valid or invalid); the fan-out fence checks SEEN so invalid markers can fan out, and the owner catches up ACCEPTED truth first (§7) |
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
  APPROVAL_REF       VARCHAR2(64),              -- dual-control approval id, TYPED; the NORMATIVE
                                                --   requirement set lives in §2.2 (correction pair,
                                                --   OPS_MARKER_CLEARED, OPS_RETRY_REARMED, and
                                                --   OUTCOME_RECORDED(SUPERSEDED_OPS)) — §2.2
                                                --   governs, not this comment
  ACTOR              VARCHAR2(64)   NOT NULL,   -- SYSTEM / SCANNER / RESOLVER / OPS:<user>
  DETAIL             VARCHAR2(1000),            -- human text; the fold NEVER reads it
  TX_ID              VARCHAR2(64),              -- stamped BY THE GUARD TRIGGER with the local
                                                --   transaction id; writers cannot supply it
  CREATED_AT         TIMESTAMP      NOT NULL,   -- ALSO guard-trigger-stamped, UTC:
                                                --   SYS_EXTRACT_UTC(SYSTIMESTAMP) per the inherited
                                                --   v4 §16.4 single-UTC rule (local-clock stamps
                                                --   cross DST jumps and corrupt trust-age forever);
                                                --   never writer-supplied

  -- identity is claimed exactly once, in the schema:
  IDEM_CLAIM         VARCHAR2(128)  GENERATED ALWAYS AS
                       (CASE WHEN EVENT_TYPE = 'REQUEST_OPENED' THEN IDEMPOTENCY_KEY END),
  -- and so is the ordinal (one opening per (payment, ordinal), ever):
  ORDINAL_CLAIM      VARCHAR2(220)  GENERATED ALWAYS AS
                       (CASE WHEN EVENT_TYPE = 'REQUEST_OPENED'
                             THEN PAYMENT_KEY || '#' || TO_CHAR(REQUEST_ORDINAL) END),
  -- (UETR uniqueness lives on the HEAD — PH_UETR_UQ, §6: an
  --  event-level acceptance claim missed the query/feed channels and
  --  refused the legal re-recording of a CT-dedup-returned original
  --  acceptance after a downgrade re-post)

  CONSTRAINT PE_FENCE_UQ   UNIQUE (PAYMENT_KEY, VERSION), -- backstop fence (§6.1)
  CONSTRAINT PE_IDEM_UQ    UNIQUE (IDEM_CLAIM),           -- identity write-once
  CONSTRAINT PE_ORDINAL_UQ UNIQUE (ORDINAL_CLAIM),        -- ordinal write-once
  CONSTRAINT PE_SOURCE_CK  CHECK (EVIDENCE_SOURCE IS NULL OR EVIDENCE_SOURCE IN
      ('SYNC_RESPONSE','QUERY','FEED','OPS','SYSTEM')),
  CONSTRAINT PE_TYPE_CK  CHECK (EVENT_TYPE IN (
      'REQUIRED_AMOUNT_SET','SNAPSHOT_INVALID_MARKED',
      'REQUEST_OPENED','ENRICH_FAILED','POST_STARTED','POST_RESULT_RECORDED',
      'QUERY_RESULT_RECORDED','DOWNGRADED_FOR_REPOST',
      'OUTCOME_RECORDED','SETTLED','FEED_RESULT_RECORDED',
      'SETTLEMENT_MISMATCH_RECORDED',
      'EVIDENCE_CONTRADICTION_RECORDED','ESCALATION_MARKED',
      'OPS_VERIFIED_OUTCOME_APPLIED','OPS_RETRY_REARMED',
      'OPS_BLOCKED','OPS_MARKER_CLEARED','OPS_ANNOTATED'))
  -- plus ONE shape CHECK per type, derived mechanically from the §2.2
  -- matrix. THE COMPLETE 19-CONSTRAINT SET IS A BUILD DELIVERABLE:
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
          AND ((EVENT_CODE = 'ACCEPTED' AND UETR IS NOT NULL)
            OR (EVENT_CODE != 'ACCEPTED' AND UETR IS NULL))))
          -- R-when-ACCEPTED both ways: the earlier one-sided form
          -- (code='ACCEPTED' OR UETR IS NULL) accepted a NULL-UETR
          -- acceptance, silently bypassing the association gate
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
every writer, including humans in an incident, with NO exemptions:
event rows are permanent (§10 — the archival mechanism was removed,
so no maintenance path ever needs to delete from this table; the only
sanctioned tiering is partitioning, which moves storage, not rows out
of the table or its indexes).

### 2.1 Why each column exists

| Column | Why |
|---|---|
| `PAYMENT_KEY` | the §1 scope tuple, canonically encoded — one stream per payment. Trade membership is derivable from the key. The ENCODING is identity input: byte-exact canonical spec + golden vectors + ONE shared encoder (same regime as key derivation) — two "reasonable" encodings of one tuple would create two heads and re-derive the same idempotency keys under a fresh stream. NORMATIVE frozen form (consumed by `PH_BIZ_BIND_CK`): `business_id \|\| '\|' \|\| payment_type \|\| '\|' \|\| debit_account \|\| '\|' \|\| currency`, INJECTIVE by rule — no component may contain the delimiter (encoder fails closed; intake classifies violations INVALID; the head checks exactly three delimiters). |
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
| ENRICH_FAILED | R: TRANSIENT, DEFINITIVE | O (NULL = failure BEFORE any opening; R when re-enrichment fails for an open request — then open-ordinal trigger-checked) | R when ORDINAL NULL (the seq of the truth being enriched — marker provenance; unlatches on strictly newer truth); N when ORDINAL R | N | N | N | N | N | SYSTEM |
| POST_STARTED | N | R | N | N | R | R | N | N | SYSTEM |
| POST_RESULT_RECORDED | R: ACCEPTED, BUSINESS_REJECT, DEFINITIVE_REJECT, AMBIGUOUS, COLLISION, UNMAPPED | R | N | N | R | N | R when ACCEPTED, N otherwise — R depends on the §18 upstream ask "acceptance response always carries its UETR" (fail-closed default: R; if the confirmed answer is "may be absent", this cell relaxes to O and the association gate keys on presence) | N | SYNC_RESPONSE |
| QUERY_RESULT_RECORDED | R: EXECUTED, REJECTED, ACCEPTED, NOT_FOUND, LOOKBACK_EXPIRED | R | N | N | R (the key QUERIED — echo-checked §6.3) | N | O (EXECUTED/ACCEPTED only) | N | QUERY |
| DOWNGRADED_FOR_REPOST | N | R | N | N | N | N | N | N | SYSTEM/OPS |
| OUTCOME_RECORDED | R: EXECUTED, REJECTED_VALIDATION, REJECTED_PROVIDER, CANCELLED_NOT_SUBMITTED, SUPERSEDED_OPS, PLATFORM_VERIFIED_EXECUTED, PLATFORM_VERIFIED_NOT_EXECUTED | R | N | R (the request amount, restated) | N | N | O (executed-class only) | N | R (any) |
| SETTLED | N | R | N | R | N | N | R (the delivery was matched BY it) | N | FEED |
| FEED_RESULT_RECORDED | R: ACCEPTED, REJECTED | R | N | N | N | N | R | N | FEED |
| SETTLEMENT_MISMATCH_RECORDED | N | R | N | R (the wrong amount) | N | N | R | N | FEED |
| EVIDENCE_CONTRADICTION_RECORDED | R: SETTLED_AFTER_TERMINAL, MISMATCH_AFTER_TERMINAL, QUERY_CONTRADICTS_OUTCOME, FEED_REJECTS_OUTCOME | R | N | O | N | N | R when EVIDENCE_SOURCE = FEED, O otherwise | N | R (any) |
| ESCALATION_MARKED | N | R | N | N | N | N | N | N | SYSTEM |
| OPS_VERIFIED_OUTCOME_APPLIED | N | R | N | N | N | N | N | N | OPS |
| OPS_RETRY_REARMED / OPS_BLOCKED / OPS_MARKER_CLEARED / OPS_ANNOTATED | N | O | N | N | N | N | N | N | OPS |

(The "only" qualifiers on O cells — e.g. UETR only when the code is
acceptance-class — are conditional conjuncts inside the same per-type
CHECK.)

Two columns are governed globally rather than per row: `APPROVAL_REF`
is R on `OPS_VERIFIED_OUTCOME_APPLIED` and on
`OUTCOME_RECORDED(PLATFORM_VERIFIED_*)` (equal across the §6.3 pair)
AND on the two money-enabling single ops actions `OPS_MARKER_CLEARED`
and `OPS_RETRY_REARMED` (their inherited §19.3-class four-eyes
authorization must be representable on the very event that re-opens
the road to fresh payment) AND on
`OUTCOME_RECORDED(SUPERSEDED_OPS)` — the manual close releases a
reservation and re-arms the standing rule, which the inherited
approval workflow gates; every approval consumes APPROVED→CONSUMED
by CAS in its transaction; N on `OPS_BLOCKED` / `OPS_ANNOTATED`
(restrictive/neutral) and every other type/code. `TX_ID` is stamped by
the guard trigger on EVERY row and can never be writer-supplied (it is
how the pair gate proves same-transaction membership in the database).

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
| `SNAPSHOT_INVALID_MARKED` | whole-snapshot validation failed — appended INSIDE the seen-admission transaction (atomic with the watermark), to exactly the set knowable then: existing heads ∪ the invalid document's canonically extractable keys, locked in sorted payment_key order. A payment first introduced by later-admitted valid truth NEVER inherits it (§7) | blocks NEW request opening; never touches in-flight work; unlatched by a newer valid `REQUIRED_AMOUNT_SET` |
| `REQUEST_OPENED` | the standing rule decides to pay a shortfall: claims ordinal + identity + amount + payload hash (write-ahead part 1) | an OPEN request exists; reservation = its amount; at most one open request is DB-backstopped (§6.2), not merely a fold invariant |
| `ENRICH_FAILED` | enrichment failed; code says transient vs definitive. Ordinal NULL when it fails BEFORE any opening (no request exists; no payload hash was ever assembled, so no opening — and therefore no outcome — is even representable); pre-open events carry the UPSTREAM_SEQ of the truth they were enriching | transient: retry timing derives from this event's timestamp + policy. Definitive PRE-open: latches the `validation_failed` marker AT its carried seq — unlatched by any strictly NEWER `REQUIRED_AMOUNT_SET`, so a delayed stale failure cannot outlive the correction that superseded it; NO outcome pair. Definitive on an OPEN request (re-enrichment): appended with `OUTCOME_RECORDED(REJECTED_VALIDATION)` in one tx (release-guarded: zero `POST_STARTED`, §6.3) |
| `POST_STARTED` | immediately BEFORE the wire call (write-ahead part 2). Its existence is the durable fact "the wire MAY have been reached". **Mandatory pre-wire recheck:** between COMMIT and the wire call the worker re-reads the head (no lock) and SKIPS the send if the payment is parked/blocked, in WITNESS_DIVERGED quarantine, or the ordinal is no longer open — the claim then resolves via the ask path under the park | request is posting/ambiguous until a result follows; the safe-release predicate is the UNIFIED **provably NOT SUBMITTED** (§5 full definition, cited without restating) |
| `POST_RESULT_RECORDED` | the synchronous response, classified per CA-1 | ACCEPTED → awaiting settlement; BUSINESS_REJECT → retry per policy (same key); DEFINITIVE_REJECT → outcome same tx, code per the CA-1 classification of the response: invalid-data → `REJECTED_VALIDATION` (latches `validation_failed`, newer-truth-recoverable, §6.3 post-wire form), otherwise `REJECTED_PROVIDER`; AMBIGUOUS → MAYBE; COLLISION → expected/unexpected via hash comparison; UNMAPPED → MAYBE + alert |
| `QUERY_RESULT_RECORDED` | the resolver asked by OUR key — the event CARRIES that key, and the §6.3 echo refuses a key other than the open request's, so stale evidence for an earlier request's key cannot be recorded against the current one | EXECUTED → outcome same tx; REJECTED → `OUTCOME_RECORDED(REJECTED_PROVIDER)` same tx; ACCEPTED → still in flight, keep waiting (submission knowledge tightens); NOT_FOUND young → no change (trust age); NOT_FOUND past trust age → enables the one sanctioned downgrade; LOOKBACK_EXPIRED → stays MAYBE (ops path) |
| `DOWNGRADED_FOR_REPOST` | the §9.2-equivalent move: NOT_FOUND past trust age, same key will be re-sent. Trigger-gated (§6.3): requires a same-ordinal `QUERY_RESULT_RECORDED(NOT_FOUND)` post-dating the latest `POST_STARTED` — a wrong decision cannot downgrade an ACCEPTED request | re-posting becomes legal for the SAME key; audit of the only backward transition. Key-scoped evidence stays attempt-agnostic BY the §18 engine dedup contract (one key = one engine-side instruction) |
| `OUTCOME_RECORDED` | terminal for a request (codes per §2.2, with `EVIDENCE_SOURCE`); AMOUNT of EVERY code must equal the OPENED amount (§6.3 — "restated" is an enforced equality; any other number is defect evidence, not an outcome). `PLATFORM_VERIFIED_*` may additionally supersede a CLOSED ordinal's outcome through the §6.3 dual-control door (amount checked against THAT ordinal's opening amount) | closes the open request (head CAS, §6.2); books or releases its reservation; latches the corresponding marker; the same transaction re-evaluates the standing rule UNDER THE INHERITED GATES — a shortfall opens a successor only if no live marker forbids it (a verified NOT_EXECUTED latches `provider_rejected` per the baseline §9.3: NO automatic successor); a corrected EXCESS (`paid + reserved > required`) cancels a provably-NOT-SUBMITTED open request (the §5 unified predicate, cited without restating) in the same tx, or keeps the payment PARKED until a claim that MAY have executed resolves. A superseding verified outcome adjusts `PAID_TOTAL` by the signed difference; the fold takes the LATEST outcome-class event per ordinal as authoritative (§5) |
| `SETTLED` | the feed confirms full-amount settlement AND the fold shows a non-terminal request for the ordinal | books confirmed money (idempotent by ordinal); closes/freezes that request. Feed evidence AGREEING with an already-EXECUTED terminal (equal amount) is a benign no-op delivery — NOT appended, NOT a contradiction |
| `FEED_RESULT_RECORDED` | the feed channel's evidence for an active request: `REJECTED` = terminal rejection; `ACCEPTED` = intermediate status-feed acceptance (feed-executed is `SETTLED`, feed-vs-terminal conflict is a contradiction event) | REJECTED: qualifying terminal-class negative evidence for `OUTCOME_RECORDED(REJECTED_PROVIDER)` in the same tx, under the §6.3 recency rule. ACCEPTED: tightens submission knowledge per inherited §4.4 and blocks downgrade via the §6.3 acceptance list. Without this type, feed rejects were unrecordable and feed acceptance silently droppable |
| `SETTLEMENT_MISMATCH_RECORDED` | feed amount ≠ instructed amount (all-or-nothing engine ⇒ defect evidence) | books NOTHING; parks loudly; submission knowledge still tightens |
| `EVIDENCE_CONTRADICTION_RECORDED` | evidence CONFLICTS with a terminal decision (codes per §2.2, incl. `FEED_REJECTS_OUTCOME` — a feed rejection against a booked EXECUTED must be representable or the delivery loops forever) — conflict, never mere repetition | FIXED effect: book nothing, PARK the payment, CRITICAL alert; sole exit is the dual-control verified outcome (`event-model-v2.md` §6) |
| `ESCALATION_MARKED` | the MAYBE got old (once per episode — the fold derives "already escalated" from this event's presence within the episode, so repeated scans cannot re-append or re-page) | AUTHORITATIVE escalation fact, inherited baseline §9.3 semantics VERBATIM: the episode is marked escalated (paged, ops attention required) while remaining RESOLVER-OWNED — **the resolver keeps querying**; no posting was possible during MAYBE anyway, and the episode exits exactly as any MAYBE does (evidence resolves it, or the dual-control verified outcome). No unblock event exists because nothing is blocked beyond what MAYBE already blocks. (Two earlier drafts erred in opposite directions — "derived state unchanged" made escalation unimplementable, "cadence stops" contradicted §9.3's resolver ownership; both retracted) |
| `OPS_VERIFIED_OUTCOME_APPLIED` | the dual-control audited operation (typed `APPROVAL_REF`, equal on both pair members) | appended with its `OUTCOME_RECORDED(PLATFORM_VERIFIED_*)` in one tx — the only manual door for possibly-moved money, the only unpark of a contradicted payment, and (as a DB-verified pair: version-adjacent, same ordinal, same `TX_ID`, equal `APPROVAL_REF`) the ONLY write admitted for a CLOSED ordinal (§6.3). A solo verified-applied event has NO fold effect and pages as an anomaly |
| `OPS_RETRY_REARMED` / `OPS_BLOCKED` / `OPS_MARKER_CLEARED` / `OPS_ANNOTATED` | human actions through the ops surface (`OPS_MARKER_CLEARED` = the §19.3-equivalent clear of a live reject marker). The two MONEY-ENABLING actions — marker clear and retry re-arm — carry the typed four-eyes `APPROVAL_REF` (R per §2.2); blocking and annotating do not | budget reset / hard block on new opens / marker cleared / display note. All arrive through the SAME write path — there is no privileged one |

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
                  ages, provably_not_submitted — the UNIFIED release
                  predicate (FULL definition, here and v2 §5.3; all
                  other sites cite it): open request with no
                  POST_STARTED, OR whose latest POST_STARTED is closed
                  by a synchronous BUSINESS_REJECT / DEFINITIVE_REJECT
                  with no later attempt AND no acceptance-class row
                  (POST_RESULT ACCEPTED / QUERY ACCEPTED|EXECUTED /
                  FEED ACCEPTED) post-dating that latest POST_STARTED
                  — the acceptance exclusion mirrors the downgrade
                  gate's: a delayed key-scoped acceptance proves
                  SUBMITTED and revokes releasability (inherited §9.4)
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
cancels a provably-NOT-SUBMITTED open request (the ONE unified
predicate above, cited without restating) in the same transaction,
or keeps the payment PARKED until a claim that MAY have executed
resolves (the §4 pre-wire recheck sees the persisting park and
blocks the send).

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
  BUSINESS_ID          VARCHAR2(64)  NOT NULL,     -- card lookup (indexed); derived FROM
                                                   --   PAYMENT_KEY by the one canonical decoder
                                                   --   and CHECK-bound to the key's leading
                                                   --   component (PH_BIZ_BIND_CK) — an unbound
                                                   --   copy drops the payment from its trade's
                                                   --   fan-out worklist and skips cancellations
  LAST_VERSION         NUMBER(10)    NOT NULL,     -- must equal stream max
  NEXT_REQUEST_ORDINAL NUMBER(10)    NOT NULL,     -- identity counter (§3)
  OPEN_REQUEST_ORDINAL NUMBER(10),                 -- NULL = no open request
  OPEN_IDEMPOTENCY_KEY VARCHAR2(128),              -- the open request's key (echo check, §6.3)
  -- money WITNESS (mechanical increments; can VETO, never authorize):
  REQUIRED_AMOUNT      NUMBER(18,3),               -- NULL = no valid data ever applied
                                                   --   (first-contact value; matches empty-stream fold)
  PAID_TOTAL           NUMBER(18,3) DEFAULT 0 NOT NULL,
  RESERVED             NUMBER(18,3) DEFAULT 0 NOT NULL,
  -- scanner / UI index (display + candidate selection only):
  PHASE                VARCHAR2(24),
  NEXT_ACTION_AT       TIMESTAMP,
  UETR                 VARCHAR2(64),
  UI_STEP_STATUS       VARCHAR2(16),
  ESCALATED            CHAR(1) DEFAULT 'N',
  UPDATED_AT           TIMESTAMP,
  -- the key/business binding (consumes the §3 frozen canonical form —
  -- an unbound copy drops the payment from its trade's worklist):
  CONSTRAINT PH_BIZ_BIND_CK CHECK
    (BUSINESS_ID = SUBSTR(PAYMENT_KEY, 1, INSTR(PAYMENT_KEY,'|') - 1)
     AND LENGTH(PAYMENT_KEY)
         - LENGTH(REPLACE(PAYMENT_KEY,'|','')) = 3)  -- injectivity assist
);
CREATE INDEX PH_DUE_IX  ON PAYMENT_HEAD (PHASE, NEXT_ACTION_AT);
CREATE INDEX PH_BIZ_IX  ON PAYMENT_HEAD (BUSINESS_ID);
CREATE UNIQUE INDEX PH_UETR_UQ ON PAYMENT_HEAD (UETR);
```

**The UETR claim lives here:** every UETR-bearing evidence recording
(sync acceptance, query, feed) sets `UETR` in its per-event head
effect, and `PH_UETR_UQ` makes the CURRENT association unique across
all payments — a competing same-UETR claim on another head dies
loudly at its commit. Historical associations are covered by the §9
UNION multiplicity rule. Re-recording a CT-dedup-returned original
acceptance (same payment, same UETR) is a no-op update — legal.

A CACHE with respect to truth (rebuildable from the stream at any
time) but a CONTRACT with respect to freshness: updated in the append
transaction — no async projector, no stale-false-negative class. It
can VETO a write (§6.1 step 3 witness check, §6.2 CAS, §6.3 triggers);
it never AUTHORIZES one — authorization always derives from the fold.

### 6.1 Write protocol — the only write path

```
1. SELECT ... FOR UPDATE on PAYMENT_HEAD (insert-on-first-contact:
   LAST_VERSION = 0, NEXT_REQUEST_ORDINAL = 1, PAID_TOTAL = 0,
   RESERVED = 0, REQUIRED_AMOUNT = NULL — the inherited "no valid
   data ever applied" value, matching the empty stream's fold;
   PK-race retry — the baseline's obligation-row idiom)
2. fold(stream)                      -- read PAYMENT_EVENT by (key, version)
3. WITNESS CHECK (fail closed, NULL-safe: fold-NULL equals head-NULL,
   the first-contact state): fold money outputs — required_amount,
   paid_total, reserved, AND open ordinal (the COMPLETE witness) —
   must equal the locked head row. ANY mismatch: abort with NO
   decision, page, and QUARANTINE via the head-only exemption
   (PHASE = WITNESS_DIVERGED — a candidate-selection mutation, never
   a derivation input; scanners skip diverged payments, so the
   quarantine needs no append). Repair = the head rebuild runbook
   (§6.1 below), triggered by fence collision OR witness divergence;
   rebuild input = the stream PLUS the payment's permanent
   `RECONCILED_BY_KEY` rows (§8 — the one sanctioned non-stream
   input: the reconciled UETR exists nowhere in the stream, and a
   stream-only rebuild would drop it out of the `PH_UETR_UQ` fence).
   If rebuild does not clear it, only the dual-control door resolves —
   reachable because that door ALONE runs under divergence in
   RECORDING mode: proceeds on the step-2 fold (the stream is the
   authority), appends the gated pair, rebuilds the head from the
   post-append stream in the same transaction, clears the quarantine.
   A veto, never an authorization
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
lock, or a step-3 witness divergence, means the head lost sync with
the stream: page, stop the stream's writes, rebuild the head from the
stream under the lock (the cache's rebuild path, exercised in tests).
If a rebuilt head still diverges from the fold, the stream itself is
in dispute and only the §6.3 dual-control door resolves it — resume
only after the divergence clears.

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
itself an appended, audited fact. The delta's prior side is PINNED at
decide time: the authoritative outcome over events with `VERSION <`
the correction's version (the step-2 fold the decision came from),
never re-derived after the insert — a post-insert "latest outcome"
lookup would see the correction itself and apply a zero delta.

### 6.3 Cross-row trigger backstops

BEFORE-INSERT triggers on `PAYMENT_EVENT` (each a single indexed head
read under the lock already held):

- **Open-ordinal**: `POST_STARTED` / `POST_RESULT_RECORDED` /
  `QUERY_RESULT_RECORDED` / `OUTCOME_RECORDED` / `SETTLED` /
  `FEED_RESULT_RECORDED` / `SETTLEMENT_MISMATCH_RECORDED` — and
  `ENRICH_FAILED` when it names an ordinal — require
  `OPEN_REQUEST_ORDINAL = :new.REQUEST_ORDINAL` —
  except the contradiction path, which must instead append
  `EVIDENCE_CONTRADICTION_RECORDED` (routing enforced; a feed
  rejection against a CLOSED executed ordinal must park, a query
  result must not attach a first UETR claim to a never-opened
  ordinal, and a mismatch against a CLOSED ordinal must become
  `MISMATCH_AFTER_TERMINAL` — not a free-standing park with no §6
  exit); `REQUEST_OPENED` requires the column NULL (§6.2).
- **The dual-control pair gate — on EVERY verified outcome, open or
  closed**: `OUTCOME_RECORDED(PLATFORM_VERIFIED_*)` for ANY ordinal
  state is admitted only when the row at `VERSION − 1` is
  `OPS_VERIFIED_OUTCOME_APPLIED` for the SAME ordinal with the SAME
  `TX_ID` (same-transaction membership, DB-checked — a dangling
  approval committed earlier can never be paired later) and equal
  `APPROVAL_REF`. Gating only closed ordinals would let a solo
  verified outcome close the OPEN ordinal and book money with no
  approval. The closed-ordinal case is additionally the ONLY write
  admitted for a closed ordinal — the dual-control unpark/correction
  path, nothing else. A solo verified-applied event has no fold
  effect; the drift scan pages it.
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
- **Release rights (the baseline release-guard, transplanted; the
  predicate is "provably NOT SUBMITTED", wider than "never sent")**:
  `CANCELLED_NOT_SUBMITTED` and `SUPERSEDED_OPS` require the UNIFIED
  §5 predicate (cited without restating — the trigger enforces all
  of its conjuncts, including the no-post-dating-acceptance
  exclusion: a delayed key-scoped acceptance revokes releasability,
  inherited §9.4); a claim that MAY have executed closes only on
  evidence or through the verified door.
  `REJECTED_VALIDATION` is admissible in TWO forms — pre-wire: zero
  `POST_STARTED` paired with its same-transaction
  `ENRICH_FAILED(DEFINITIVE)`; or post-wire synchronous: the unified
  §5 predicate's second arm with `DEFINITIVE_REJECT` specifically
  (incl. its no-post-dating-acceptance conjunct), where CA-1
  classifies the definitive response as invalid-data
  (inherited §7.2: it releases and latches `validation_failed`,
  recoverable by strictly newer truth — the pre-wire-only rule
  forced it into `REJECTED_PROVIDER` and the wrong marker); `REJECTED_PROVIDER` requires a
  same-ordinal TERMINAL-class negative evidence row —
  `POST_RESULT_RECORDED(DEFINITIVE_REJECT)`,
  `QUERY_RESULT_RECORDED(REJECTED)`, or
  `FEED_RESULT_RECORDED(REJECTED)` only, `BUSINESS_REJECT` never
  qualifies (retry-class; its negative fact expires when a later
  attempt starts) — whose `VERSION >` the version of the ordinal's
  LATEST `POST_STARTED` (evidence must post-date the last attempt, or
  attempt 1's stale reject could terminally close attempt 2's
  lost-response execution).
- **Downgrade gate**: `DOWNGRADED_FOR_REPOST` requires a same-ordinal
  `QUERY_RESULT_RECORDED(NOT_FOUND)` post-dating the latest
  `POST_STARTED` AND NO acceptance-class row
  (`POST_RESULT_RECORDED(ACCEPTED)` /
  `QUERY_RESULT_RECORDED(ACCEPTED | EXECUTED)` /
  `FEED_RESULT_RECORDED(ACCEPTED)`) post-dating that same
  `POST_STARTED` — recency alone proves recording order, not the
  absence of an intervening acceptance; an ACCEPTED request must
  never be downgraded (evidence existence/absence in the trigger;
  trust-age arithmetic stays fold policy).
- **UETR-association binding**: THE ASSOCIATION RELATION is defined
  once — UETR-bearing `PAYMENT_EVENT` rows ∪ permanent
  `RECONCILED_BY_KEY` rows (§8) — and every consumer resolves
  UETR→ordinal through it (this binding, the no-op witness's ordinal
  resolution, contradiction routing; a veto-only reconciled row left
  the lost-UETR ordinal positively unresolvable). An event carrying a
  UETR must either MATCH its ordinal's existing association, or be
  the FIRST association of that UETR anywhere — no event row with
  this UETR on any OTHER payment
  (one probe on the permanent `PE_UETR_IX` — a claim of an abandoned
  historical UETR dies at ITS commit, not as a later permanent
  matching ambiguity), no RECONCILED inbox association naming it (§8
  — the lost-UETR association lives ONLY there, which is why those
  rows are permanent), and none on another ordinal of this payment
  (a resolver cannot re-attach ordinal 1's UETR to open ordinal 2
  and let ordinal 1's stale feed reject release ordinal 2's
  reservation). `PH_UETR_UQ` (§6) remains the belt for two
  SIMULTANEOUS first claims, invisible to committed-data probes.
- **Opening stamp**: `REQUEST_OPENED` requires `:new.REQUIRED_AT_OPEN
  = REQUIRED_AMOUNT` on the transaction-fresh head — the immutable UI
  amount-series stamp cannot be born wrong (display-only, but
  unfixable if wrong).
- **Opening amount = shortfall**: `REQUEST_OPENED` requires
  `:new.AMOUNT = REQUIRED_AMOUNT − PAID_TOTAL` on the
  transaction-fresh head (the standing rule pays the FULL remaining
  shortfall, baseline §6.8; both fields just witness-checked, and
  `RESERVED` is 0 at opening). Without it, an oversized opening rides
  every downstream gate — reservation copy, key echo, terminal
  equality, close CAS — faithfully to the wire and books paid 180
  against required 100.
- **Version continuity**: every insert requires `:new.VERSION =
  LAST_VERSION + 1`; the per-event head effect sets `LAST_VERSION =
  :new.VERSION`. The fence rejects duplicates; this closes holes. The
  drift scan asserts density (`COUNT(*) = MAX(VERSION) = LAST_VERSION`).

**Enforcement point (Oracle-real):** the `:new`-style predicates are
the SPECIFICATION; the implementation is a COMPOUND trigger validating
each inserted row at after-statement time against `PAYMENT_EVENT` and
`PAYMENT_HEAD` (raise = transaction aborts — backstop semantics
preserved). Per-event apply order means every event is its own
single-row `INSERT ... VALUES` (no mutating-table bite), and a
statement guard FORBIDS multi-row inserts into `PAYMENT_EVENT`.
Real-Oracle proof of this behavior is 00-README checklist item 5
evidence, not an assumption.

All backstops are independent of fold correctness. Full temporal
legality beyond this set stays code-enforced — accepted, with
mitigations, in `event-model-v2.md` §10.

## 7. `TRADE_HEAD` — snapshot admission

```sql
CREATE TABLE TRADE_HEAD (
  BUSINESS_ID        VARCHAR2(64) PRIMARY KEY,
  -- accepted pair: newest VALID snapshot = trade truth.
  -- NULL until the trade's FIRST valid snapshot (a trade whose first
  -- delivery is invalid must be representable without inventing truth):
  LAST_ACCEPTED_SEQ  NUMBER(19),
  ACCEPTED_DIGEST    VARCHAR2(64),
  ACCEPTED_XML_REF   VARCHAR2(200),
  -- seen pair: newest snapshot processed AT ALL (valid or invalid;
  -- >= accepted when both present). Digest/pointer make an invalid
  -- redelivery deduplicable and its fan-out resumable:
  LAST_SEEN_SEQ      NUMBER(19)   NOT NULL,
  SEEN_DIGEST        VARCHAR2(64) NOT NULL,
  SEEN_XML_REF       VARCHAR2(200),
  UPDATED_AT         TIMESTAMP
);
```

Admission (this table's only writer) and fan-out have an EXPLICIT
transaction boundary:

- **Admission tx**: validity is judged against the RIGHT watermark —
  a VALID snapshot strictly newer than `LAST_ACCEPTED_SEQ` is
  ADMITTED even when older than `LAST_SEEN_SEQ` (inherited §6.6:
  valid 150 after invalid 200 over accepted 100 must land; judging
  arrivals against SEEN discarded cancellations); the SEEN pair
  advances monotonically on any newer-than-seen arrival, valid or
  invalid (first-contact insert with an invalid document leaves the
  accepted pair NULL). ANY watermark change re-triggers the
  full-current-state fan-out below, fenced by the `LAST_SEEN_SEQ`
  current at worklist derivation. Equal seq + equal
  SEEN_DIGEST → identical redelivery, admit-without-update (comparing
  an invalid redelivery against an accepted-only digest would misread
  it as a defect). Equal seq + DIFFERENT digest → upstream DEFECT:
  refuse + CRITICAL alert (no tie workflow exists in this design).
  Older than BOTH watermarks → ignore.
- **Fan-out**: separate per-payment transactions in sorted payment_key
  order, each of which (1) locks `TRADE_HEAD` (the baseline lock
  order), (2) verifies its carried snapshot seq still EQUALS
  `LAST_SEEN_SEQ` — the **equality fence**: abort if a newer arrival
  owns the trade — then (3) locks the payment head and appends
  seq-guarded. **The owner fans out the FULL current trade state**:
  per payment, FIRST the accepted truth's `REQUIRED_AMOUNT_SET` from
  the STORED accepted snapshot (seq-guarded — catching up any
  accepted admission whose own fan-out was fenced out, including
  cancels-to-zero; skipped only when the accepted pair is NULL),
  and NOTHING else — invalid markers are NOT this fan-out's job:
  `SNAPSHOT_INVALID_MARKED(seq)` appends happen INSIDE the
  seen-admission transaction, atomically with the watermark, over
  exactly the set knowable then (existing heads ∪ the invalid
  document's canonically extractable keys, heads locked in sorted
  key order). The stream rows ARE the durable record — no sidecar
  set, no resume rule; a crash rolls the admission back and
  redelivery re-runs. A payment first introduced by later-admitted
  valid truth never inherits the marker (inherited §6.6). An
  accepted-catch-up skip would starve an already-accepted
  cancellation behind the fence and let a cancelled payment post. Worklist =
  payments named in the stored ACCEPTED snapshot ∪ existing head rows
  of the trade (the extractable-key branch moved INTO the
  seen-admission transaction with the markers: distrust bars an
  invalid document from MONEY truth, not from anchoring visibility —
  a first-delivery-invalid trade gets its head + marker at
  admission, required stays NULL, nothing can open). Already-applied streams no-op, so partial fan-out is
  safely resumable; resume re-derives the worklist from the CURRENT
  watermarks' stored state, never from an in-memory snapshot — a
  stale resumed worker can neither create nor touch a payment from
  superseded trade truth.
- Kafka ack only after fan-out completes; redelivery re-runs and
  converges.

## 8. `INBOUND_EVENT_INBOX` — delivery identity

```sql
CREATE TABLE INBOUND_EVENT_INBOX (
  SOURCE       VARCHAR2(32)  NOT NULL,
  EVENT_ID     VARCHAR2(128) NOT NULL,
  RECEIVED_AT  TIMESTAMP     NOT NULL,  -- guard-stamped UTC (SYS_EXTRACT_UTC):
                                        --   a retention anchor; the single-UTC
                                        --   rule is global to all four structures
  MATCH_STATUS VARCHAR2(20)  DEFAULT 'PROCESSED' NOT NULL,
  -- evidence CONTENT, populated on UNMATCHED_TERMINAL/RESOLVED rows —
  -- a status flag without the payload would be unresolvable:
  EV_UETR      VARCHAR2(64),
  EV_CLASS     VARCHAR2(20),
  EV_AMOUNT    NUMBER(18,3),
  EV_PAYLOAD_REF VARCHAR2(200),
  -- resolution provenance (which exit closed an unmatched row):
  RES_PAYMENT_KEY  VARCHAR2(200),             -- RESOLVED_HANDLED / RECONCILED_BY_KEY:
                                              --   the payment it was handled against
  RES_REQUEST_ORDINAL NUMBER(10),             -- RECONCILED_BY_KEY: WHICH request the
                                              --   human's association claim names — the
                                              --   locked agreement check runs at THIS
                                              --   granularity, and the approval binds to it
  RES_AT_VERSION   NUMBER(10),                -- RESOLVED_HANDLED: head LAST_VERSION at
                                              --   handling time (audit pointer, NEVER
                                              --   load-bearing)
  DISPOSED_BY      VARCHAR2(64),              -- RESOLVED_DISPOSED: audited ops exit
  DISPOSED_CATEGORY VARCHAR2(20),             --   FOREIGN | RECONCILED_BY_KEY
  DISPOSED_APPROVAL VARCHAR2(64),
  DISPOSED_REASON  VARCHAR2(400),
  CONSTRAINT INB_UQ UNIQUE (SOURCE, EVENT_ID),
  CONSTRAINT INB_STATUS_CK CHECK (MATCH_STATUS IN
      ('PROCESSED','UNMATCHED_TERMINAL','RESOLVED_HANDLED',
       'RESOLVED_DISPOSED')),
  -- every column bound in EVERY arm (an arm that ignores a column
  -- leaks free-standing provenance); closed vocabularies carry
  -- explicit IS NOT NULL against Oracle 3VL
  CONSTRAINT INB_SHAPE_CK CHECK (
      (MATCH_STATUS = 'PROCESSED'
        -- non-terminal delivery: no evidence content; TERMINAL
        -- delivery handled at arrival: evidence content RETAINED so
        -- the universal fidelity rules can see it (a NULLed matched
        -- path was the hole that let SETTLED record as a rejection)
        AND ((EV_UETR IS NULL AND EV_CLASS IS NULL
              AND EV_AMOUNT IS NULL AND EV_PAYLOAD_REF IS NULL)
          OR (EV_UETR IS NOT NULL
              AND EV_CLASS IS NOT NULL
              AND EV_CLASS IN ('SETTLED','REJECTED','MISMATCH')
              AND EV_PAYLOAD_REF IS NOT NULL
              AND (EV_CLASS = 'REJECTED' OR EV_AMOUNT IS NOT NULL)))
        AND RES_PAYMENT_KEY IS NULL AND RES_REQUEST_ORDINAL IS NULL
        AND RES_AT_VERSION IS NULL
        AND DISPOSED_BY IS NULL AND DISPOSED_CATEGORY IS NULL
        AND DISPOSED_APPROVAL IS NULL AND DISPOSED_REASON IS NULL)
   OR (MATCH_STATUS IN ('UNMATCHED_TERMINAL','RESOLVED_HANDLED',
                        'RESOLVED_DISPOSED')
        AND EV_UETR IS NOT NULL
        AND EV_CLASS IS NOT NULL
        AND EV_CLASS IN ('SETTLED','REJECTED','MISMATCH')
        AND EV_PAYLOAD_REF IS NOT NULL
        AND (EV_CLASS = 'REJECTED' OR EV_AMOUNT IS NOT NULL)
        AND (MATCH_STATUS != 'UNMATCHED_TERMINAL'
             OR (RES_PAYMENT_KEY IS NULL AND RES_REQUEST_ORDINAL IS NULL
                 AND RES_AT_VERSION IS NULL
                 AND DISPOSED_BY IS NULL AND DISPOSED_CATEGORY IS NULL
                 AND DISPOSED_APPROVAL IS NULL AND DISPOSED_REASON IS NULL))
        AND (MATCH_STATUS != 'RESOLVED_HANDLED'
             OR (RES_PAYMENT_KEY IS NOT NULL AND RES_AT_VERSION IS NOT NULL
                 AND RES_REQUEST_ORDINAL IS NULL
                 AND DISPOSED_BY IS NULL AND DISPOSED_CATEGORY IS NULL
                 AND DISPOSED_APPROVAL IS NULL AND DISPOSED_REASON IS NULL))
        AND (MATCH_STATUS != 'RESOLVED_DISPOSED'
             OR (DISPOSED_BY IS NOT NULL
                 AND DISPOSED_CATEGORY IS NOT NULL
                 AND DISPOSED_CATEGORY IN ('FOREIGN','RECONCILED_BY_KEY')
                 AND DISPOSED_APPROVAL IS NOT NULL
                 AND DISPOSED_REASON IS NOT NULL
                 AND RES_AT_VERSION IS NULL
                 AND ((DISPOSED_CATEGORY = 'RECONCILED_BY_KEY'
                       AND RES_PAYMENT_KEY IS NOT NULL
                       AND RES_REQUEST_ORDINAL IS NOT NULL)
                   OR (DISPOSED_CATEGORY = 'FOREIGN'
                       AND RES_PAYMENT_KEY IS NULL
                       AND RES_REQUEST_ORDINAL IS NULL))))))
);
```

`MATCH_STATUS` makes the unmatched-terminal case a DURABLE data fact:
paging is LEVEL-TRIGGERED — a sweep re-runs matching (from the stored
evidence columns) and pages while any `UNMATCHED_TERMINAL` row exists
(re-match finds a head created later; a crash between commit and page
loses nothing, and redelivery hitting the inbox dedup cannot silently
bury an evidence fact that was never resolved). **Resolution
(SIMPLIFIED — the correspondence-verification relation of two earlier
revisions is RETRACTED):** three review rounds proved that
schema-verifying inbox evidence against specific stream events is a
second implementation of write-path legality that diverges from the
real one (it refused legal parks, agreements, and rejections, and
could not see fold state without re-implementing the fold in a
trigger). The rule now: whatever the re-match decides — append,
contradiction park, or benign no-op — is governed SOLELY by the
normal write-path gates under the head lock (§6.3), in the same
transaction as the flip to `RESOLVED_HANDLED`, whose
(`RES_PAYMENT_KEY`, `RES_AT_VERSION` = head `LAST_VERSION` at
handling) is an AUDIT POINTER. Every feed-delivery transaction — matched at
arrival or unmatched flip — carries the two-rule DELIVERY-FIDELITY
BACKSTOP (fold-state-free; restored after review proved class
fidelity is invisible to the write-path gates, then made UNIVERSAL
after the matched path proved equally exposed; enforcement = one
compound trigger on the inbox write reading its own transaction's
appends): (1) no class inversion — EV SETTLED forbids
rejected-class evidence/outcomes for the associated ordinal;
EV REJECTED forbids executed-class/`SETTLED` bookings; EV MISMATCH
forbids BOTH (its legal appends: `SETTLEMENT_MISMATCH_RECORDED` or a
contradiction); contradiction events always admissible; (2) no
witnessless no-op — a terminal delivery handled with NO append
requires that the ASSOCIATED ORDINAL (bound to the delivery's UETR
via THE UNIFIED ASSOCIATION RELATION of §6.3 — UETR-bearing events ∪
permanent `RECONCILED_BY_KEY` rows; an acceptance row suffices, and
an events-only lookup would make the reconciled ordinal
unresolvable) has an AUTHORITATIVE outcome (latest outcome-class event, §5 supersession
— never a superseded historical one) agreeing in CLASS and, for
settled/mismatch classes, in AMOUNT with the evidence — with ONE
class-specific form: EV MISMATCH's witness is an existing
same-ordinal `SETTLEMENT_MISMATCH_RECORDED` row of equal UETR and
amount (mismatch rows are deliberately NOT outcomes — the request
stays open and the payment parks — so an authoritative-outcome
requirement would make the legal repeated-mismatch no-op
unsatisfiable and loop the redelivery forever). Transcription
fidelity (delivery → EV columns) is the inherited CA-1
recorded-at-the-time code class, one shared golden-vector-tested
transcriber.
`RESOLVED_DISPOSED` covers evidence that CANNOT go
through a payment's write path, in two audited categories: `FOREIGN`
(not ours) and `RECONCILED_BY_KEY` (the lost-UETR settlement: the
response timed out, the key-query recovery carried no UETR, no
stream will ever contain this delivery's UETR — a human reconciles
it to the payment via the §9.1 query trail and records that payment
key). `RECONCILED_BY_KEY` is STATE-GATED with a real
enforcement point at REQUEST granularity: the reconciliation names
payment key AND request ordinal (the §9.1 trail identifies the
request — a payment-level check let one ordinal's hidden settlement
be acknowledged against another's equal-sized execution); the
disposal transaction locks the NAMED payment's head
(`SELECT FOR UPDATE`), and under that lock the inbox trigger verifies
THE NAMED ORDINAL's authoritative outcome agrees with the evidence
(class and amount) AND the head is not parked/quarantined, serialized
against concurrent corrections; the four-eyes approval binds to the
stated (payment, ordinal) claim. If the stream disagrees, disposal
FAILS — the truth enters through the dual-control verified-outcome
door FIRST; reconciliation acknowledges recorded truth, it never
substitutes for it. Both categories require actor + reason + an approval through
the INHERITED §9.3 protocol (bound to exactly this (source, event
id) + action, consumed APPROVED→CONSUMED by CAS in the same
transaction — columns are the echo, the approval-store CAS is the
enforcement). The reconciled association is DURABLE and
GUARD-VISIBLE: `RECONCILED_BY_KEY` rows are PERMANENT (they carry a
UETR→(payment, ordinal) association recorded NOWHERE else — purging
one would un-forget the UETR), and both the §9 matching UNION and
the §6.3 first-claim probe include them as a source, so a later
platform reuse of a reconciled UETR dies loudly at its commit. The
disposal additionally applies a HEAD EFFECT under the lock it
already holds: the named head's `UETR` is set to the reconciled UETR
when the slot is NULL, putting the association inside the
`PH_UETR_UQ` fence for simultaneous first claims (a successor-owned
slot leaves a bounded residual that resolves FAIL-CLOSED as a parked
three-source multiplicity anomaly under dual control). Inbox purge
NEVER removes an `UNMATCHED_TERMINAL` or `RECONCILED_BY_KEY` row;
the other statuses age out on the retention chain.

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
  payment_key + ordinal), joined to the AUTHORITATIVE outcome: the
  outcome-class event with the MAX version for that ordinal (a
  `ROW_NUMBER`/max-version selection — the same storage-contract rule
  §5 defines, stated once, NOT a fold re-implementation; a plain
  join-by-ordinal would show two rows and the superseded verdict after
  a legal correction), obligation-only placeholders from head rows
  with `NEXT_REQUEST_ORDINAL = 1`. The
  amount series is free: `REQUIRED_AT_OPEN` sits on the opening event.
  Keyset pagination on (payment_key, version). No projection, no
  maintenance job.
- **Scanners/resolver:** select CANDIDATES from the head
  (`PHASE, NEXT_ACTION_AT`); every ACTION folds the stream under the
  head lock. A stale candidate costs a wasted fold, never a wrong
  payment.
- **Feed matching (fail-closed multiplicity, counted in PAYMENTS not
  rows, over the UNION of all THREE sources):** candidates = DISTINCT
  `PAYMENT_KEY`s from head matches ∪ event-index matches ∪ RECONCILED
  inbox associations (§8 — the only place a lost-UETR association
  exists), ALL always consulted — a head-first shortcut returning one match would never
  see the event index naming a DIFFERENT payment for the same UETR
  and would book the wrong one (one payment's several UETR-bearing
  events still resolve as ONE candidate). 0 payments → unmatched
  path: inbox row written with `MATCH_STATUS = UNMATCHED_TERMINAL`
  (§8 — durable, level-triggered paging; never a silent ack). 1 →
  lock that payment, then RE-RUN the union INSIDE the transaction
  before deciding (the initial lookup is unserialized check-then-act;
  a concurrent same-UETR claim on another payment must abort the
  booking, not be sealed under the inbox); recheck 1 → fold + append;
  recheck 2+ → abort, CRITICAL, nothing committed. 2+ DISTINCT
  payments at any point → CRITICAL anomaly, NO state change. The
  post-recheck race is caught BY THE SCHEMA: `PH_UETR_UQ` (§6 — one
  HEAD per UETR; every association channel funnels through the head
  effect) makes the competing same-UETR claim die loudly at ITS
  commit — UETR uniqueness is a platform contract fact whose
  violation is a constraint failure + CRITICAL page, never a silent
  double-booking.

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
- **No event archival — rows are PERMANENT** (`event-model-v2.md` §9:
  the archival mechanism was REMOVED after two review rounds showed
  it breeds critical-class defects — archived claims leave the unique
  indexes and un-forget burned keys; rehydration cannot pass the
  continuity trigger; the lateness eligibility bound is not an
  obtainable contract fact). Heads and events are both permanent;
  storage tiering only via partitioning that never removes rows from
  the table or its global unique indexes — and every partition DDL
  must be index-maintaining (`UPDATE INDEXES` / online), with writes
  resuming only after all `PE_*` unique indexes verify VALID (Oracle
  marks global indexes UNUSABLE by default on partition maintenance;
  an unusable claims index is a global liveness outage);
  compliance-deletion pressure lands on the PII vault, never on
  events.
- **What is deliberately ACCEPTED** (with mitigations): see the
  honesty box in `event-model-v2.md` §10 — code-enforced temporal
  legality beyond the §6.2/6.3 backstops, control-state
  reinterpretation on fold deploys (money side gated to zero by the
  §4.2 deploy gate), the single interpretation point, event-table
  growth, the engine collision contract as keystone (§18 item 1 gates
  go-live identically), and the ops learning curve (`fold --explain`
  ships with the MVP as a deliverable).
