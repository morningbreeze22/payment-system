# Payment Orchestration System — Requirements v4 (Factored State Model)

**Supersedes:** `requirment-v3.md`. Same rules; new state representation.
**Date:** 2026-07-05
**Stack:** Java (Spring Boot), Oracle DB, Spring Kafka, Hazelcast.
**Services:** `PaymentOrchestrationService`, `PaymentEnrichmentService`, `PaymentExecutionService`, `PaymentNotificationConsumerService`.
**Status:** BASELINE — hardened through fourteen review rounds (internal + external adversarial); every finding resolved in place or tracked as an open item. Implementation is gated on the §18 BLOCKING items (0–3). Settled ground a reviewer must not re-challenge: the §1 contract facts (confirmed and assumed) and the §1.1 Basic Agreements. Full review provenance: `requirment-v4-annotated.md` and the design-review-v1…v14 series (maintained locally by the design owner; intentionally excluded from the shared repository — available on request).

What changed from v3: the 13-value request `status` enum entangled
four orthogonal facts — lifecycle position, money truth, who acts
next, and why blocked. Four review rounds independently found bugs
whose root cause was a rule keyed on the compound status when it
meant one dimension. v4 factors the enum into independent columns so
that keying on the wrong dimension is unrepresentable. No rule from v3 was removed.

Design principles:

```text
P1  Money derives status — never the other way around.        (§4)
P2  Derive, never accumulate — step status, active exception,
    the next actor, and request creation are standing
    consequences of state.                                     (§4, §6.8)
P3  One staleness guard per mutable input — reads AND writes.  (§6.9)
P4  Release rights move only on first-party evidence.          (§9.4, §10.1)
P5  Fail blocked with an alarm, never pay twice,
    never fail silently.                                       (everywhere)
P6  One column per fact: no field encodes two dimensions.      (§2, §10)
```

------

## 1. System Context and Integration Model

This system orchestrates payments. It is a payment **orchestrator**,
not a payment platform: it stores only the state it needs for its own
processing; the authoritative audit trail for the whole trade and
payment lifecycle lives in the payment platform.

The system **sends no notifications**. It is a consumer of exactly two
inbound flows:

```text
1. Upstream notifications identifying the trade's snapshot — the
   trigger for payment processing. The Kafka message carries the
   snapshot's STORAGE ID; the full snapshot XML is fetched from the
   upstream-populated store and payment details are taken from it
   (§6.0 transport note). Message contract: §6.0.
2. Payment platform status notifications keyed by UETR — the
   authoritative payment-status feed.
```

UI visibility is provided by a **read-only card**, implemented by this
team, that displays payment details by reading current state from this
system (§12). All UI-visible state is a read model derived from the
core tables — never a delivered event. There is no outbox, no
notification retry machinery, no event ordering or idempotency-key
scheme toward the UI.

Sources of truth:

```text
Local database + payment platform status feed = payment execution truth
The card / process UI = display only, never authoritative
```

Terminology: "the provider" and "the payment engine" are the same
party — the payment platform's execution API (POST), status feed, and
status-query API.

Confirmed contract facts this design relies on:

```text
- The payment engine settles ALL-OR-NOTHING: partial settlement and
  fee-deducted amounts are impossible by contract (§8, §16.4).
- The engine offers a status-query API by idempotency key / UETR (§9).
- Trade-payment cardinality: one trade (business_id) can carry
  MULTIPLE payments. Each upstream message is a FULL-TRADE SNAPSHOT —
  the complete current truth of the trade, carrying ALL of its
  payments; a newer snapshot OVERWRITES the older one in full (a trade
  has exactly one current XML at a time).
- Role derivation (PO fact, 2026-07-12): the snapshot carries the
  trade's full SETTLEMENT set — who pays whom, what amount. This
  system derives ITS OWN payment set from that list by our role in
  the trade (payer/receiver). The DERIVED set is therefore 0..N: a
  valid, non-empty snapshot can legitimately contain ZERO payments
  for this system, and such a snapshot is admissible — it means
  "the current truth requires no payment from us" and drives the
  §6.1 absence fan-out over any existing obligations.
- Within one snapshot,
  (payment_type + debit_account + currency) is UNIQUE per payment
  block — validated at intake (§6.0); across snapshots, an equal
  tuple MEANS the same payment (that identity IS the contract — it is
  how amendments are recognized). Written upstream confirmation of
  the snapshot schema and the uniqueness guarantee is tracked as §18
  upstream ask 5. Both consequences are now ANSWERED: absence
  semantics (PO-9, 2026-07-11 — absence = amendment to zero, BA-2
  amended §1.1) and the snapshot ordering-watermark rule (TL-16,
  round 5 — trade-level admission §6.1/§2.4).
```

Assumed contract facts — the design ASSUMES these are true and
carries NO runtime gating, flag, or defensive machinery for them;
the assumption is not free-floating: proving them is §18 BLOCKING
item 1 (sandbox test), and go-live does not happen without the proof:

```text
- A re-POST of a known idempotency key NEVER executes a new payment:
  identical payload → deduped/acked; different payload → rejected
  with a code distinguishable from plain DUPLICATE_REQUEST (§7.0,
  §7.2, TL-4).
- The engine's key retention covers the maximum request lifetime
  (including the ops-queue SLA) — keys do not age out of the dedup
  store while a request can still re-POST.
- Duplicate detection keys on the CALLER-SUPPLIED idempotency key,
  not the SDK-minted UETR.
```

### 1.1 Basic Agreements (settled with PO and users — not re-challengeable)

Entries here are decided business facts, agreed outside this document.
Design reviews shall treat them as ground truth: they are not findings,
not assumptions to stress, and not open items. Changing one requires
the PO, not a reviewer.

```text
BA-1 Scope-key mutability is agreed business behavior, not a
     duplicate-payment risk. payment_type / debit_account / currency
     arrive in mutable upstream messages; if one changes, the message
     computes a NEW scope (§2.1) and the system pays under the new
     info as a NEW obligation. This is the intended outcome — agreed
     with the PO and users — and is NOT a duplicate, even if the old
     scope's request already executed. No scope-migration mechanism,
     no upstream immutability ask, and no cross-scope detector is
     required.
BA-2 (AMENDED 2026-07-11 by the PO's PO-9 answer) Upstream's ONE
     cancellation signal is ABSENCE FROM A NEWER SNAPSHOT: a
     payment absent from a newer admitted snapshot NO LONGER
     EXISTS — treated as an amendment to ZERO (§6.1/§6.3), which
     the existing machinery resolves with no new rules:
     provably-unsent active request → §6.4 auto-cancel +
     reservation release; in-flight/MAYBE → wait-then-decide (§6.4
     — posted requests are never auto-amended) until the outcome
     is known; payment already CONFIRMED → confirmed > required(0)
     → the §6.5/§13 OVERPAY LATCH fires naturally — WE STOP (PO:
     "same as overpay"): trade ignored, alert, annotation only, no
     clawback; recovery for an executed payment remains a
     platform-side recall (§19.2 family). No OTHER cancellation
     signal exists or is planned: intake keeps rejecting
     zero/absent BLOCK amounts (a zero can only be written by the
     absence path — §4.1). A cleanly unwound removed payment is
     TERMINAL: the §4.1 CANCELLED branch (round 11), displayed
     CANCELLED. Removal of a trade's ONLY payment is representable:
     the derived payment set is 0..N (§1 role derivation — a valid
     snapshot can carry zero payments for us), so the cancelling
     snapshot simply has an empty derived set. PO-5 (§18) remains
     purely a DISPLAY question.
BA-3 Message-ordering correctness is upstream's
     responsibility. The system trusts the ordering value as
     delivered (§6.7): a genuinely-newer amendment carrying an
     older ordering value is dropped as stale BY DESIGN — that is
     an upstream data defect, not a defect of this system, and no
     compensating machinery (acknowledgments, tie-rate metrics
     beyond the existing §6.7 alert) is required.
```

------

## 2. Data Model

Four tables (three payment tables + the §2.4 trade-level admission
row — added 2026-07-11 round 5, the one correctness-driven schema
change since the model froze: per-obligation watermarks cannot stop
a stale snapshot from CREATING a never-seen scope).

### 2.1 payment_obligation

One row per payment scope. Scope key:

```text
business_id + payment_type + debit_account + currency   (UNIQUE)
```

A trade (business_id) carrying multiple payments (§1 contract facts)
therefore owns MULTIPLE obligation rows — one per payment — and that
is the normal case, not an anomaly. The scope key needs no
discriminator: within one snapshot the tuple is unique by contract
(validated at intake, §6.0), and across snapshots an equal tuple
means the same payment.

Financial fields:

```text
required_amount     — what the scope must pay (mutable via upstream
                      messages only, §6.7 guard); NULL on anchor rows
                      created from failed-validation messages (§6.6)
                      until the first valid message arrives; a valid
                      message must carry a positive amount (intake
                      validation)
committed_amount    — reservation counter (§3)
confirmed_amount    — authoritatively confirmed money (§3)
overpay_blocked     — latch, set when confirmed_amount > required_amount
```

Ordering / derivation-input fields:

```text
next_request_seq    — per-obligation counter, incremented under the
                      obligation lock in the same transaction that
                      inserts a request; input to the deterministic
                      identity derivation (§5); deterministic across
                      a database restore by construction
upstream_ordering   — last-APPLIED message ordering value (§6.7);
                      never set by a failed-validation message
correlation_id      — from the upstream message (§6.0); persisted for
                      cross-system tracing; included in every §14 log
                      line
validation_failed   — ordering-tagged marker, stored as
                      (validation_failed_at, validation_failed_ordering
                      = ordering of the message whose data failed):
                      set by message validation failure OR enrichment
                      definitive invalid-data OR a synchronous
                      invalid-data engine reject; LIVE only while
                      validation_failed_ordering >= upstream_ordering
                      OR upstream_ordering IS NULL (a §6.6
                      anchor scope has never applied a valid message —
                      its marker is LIVE by definition, otherwise the
                      liveness predicate is undefined exactly on the
                      row §6.6 requires to display the exception);
                      WRITES ARE MONOTONIC — overwritten only by a
                      strictly newer ordering value, stale writes
                      dropped + counted (§6.9)
provider_rejected   — ordering-tagged marker, stored as
                      (provider_rejected_at, reject code,
                      provider_rejected_ordering = ordering of the
                      message that created the rejected request): set
                      by EVERY engine or ops negative that is not
                      invalid-data (§7, §8 — marker totality);
                      monotonic writes (§6.9). LIVE while
                      provider_rejected_ordering >= upstream_ordering
                      OR provider_reject_count >= 2 — from the second
                      reject the marker stays live regardless of
                      newer messages, so the exception, the §15
                      alert, and the §6.8 block all persist until ops
                      clears it (§19.3); a newer message alone must
                      not silence a repeat-reject scope
provider_reject_count — per-scope counter, incremented each time
                      provider_rejected is set; reset by the §19.3
                      ops clear. From the SECOND reject onward the
                      marker is clearable only by ops, never by a
                      newer message — prevents an upstream-paced
                      reject/re-pay loop; alert on increment ≥ 2 (§15)
validation_failed_first_at — set when validation_failed transitions
                      not-live → live; NOT touched by the monotonic
                      re-tags; cleared when the marker goes not-live.
                      The §15 marker-age alert keys on it — the
                      re-tag timestamp (validation_failed_at) is
                      refreshed by every newer failing message and
                      can therefore never age
validation_reject_count — per-scope counter, incremented each time
                      validation_failed is set; reset when the marker
                      clears (corrected message or ops). ALERT ONLY
                      at ≥ 3 (§15), deliberately NO gate — unlike
                      provider_rejected, a validation cycle moves no
                      money and a newer corrected message IS the
                      designed recovery, so the counter observes a
                      repeat-reject loop without ever blocking the
                      correction
reopened_at         — set on step reopening (§6.5); derivation input
                      so the card can indicate reopening
```

Read-model fields (consumed by the card, derived only — §4, §12):

```text
ui_step_status                  — IN_PROGRESS / COMPLETED / CANCELLED
                                  as stored values (CANCELLED = the
                                  §4.1 zero-required terminal branch,
                                  round 11; the Java enum, the DB
                                  CHECK, and the serialization/API
                                  contract ALL carry the three values
                                  — CA-4/S-02/S-05 (the CHECK
                                  is S-05's — round 13), rollout-safe per
                                  §16.5); NOT_STARTED is virtual —
                                  represented by row absence (§12)
active_exception_category
active_exception_code
active_exception_message
active_exception_retryable
active_exception_severity       — WARNING / ERROR / CRITICAL (§13)
active_exception_manual_action  — manual_action_required flag (§7, §13)
active_exception_at
ops_annotation                  — free-text ops note, e.g. overpay
                                  acknowledgement (§12, §20)
ui_process_instance_id          — display/reference field supplied by
                                  upstream (NOT a lookup key — the
                                  card looks up by business_id, §12;
                                  see §18 BLOCKING item 0)
ui_step_instance_id             — opaque step string supplied by
                                  upstream; display/reference only
```

Constraints: scope-key uniqueness; `CHECK` amounts `>= 0`; index on
business_id backing the card lookup (§12).

### 2.2 payment_request — the factored state model

One row per logical payment attempt. The request's state is FOUR
independent columns, one per fact (P6):

```text
stage             — WHERE in the pipeline. ENRICH → POST → CONFIRM.
                    Monotonic, with exactly ONE sanctioned exception:
                    CONFIRM → POST via the trust-age downgrade (§9.2,
                    same-key re-attempt).
stage_state       — HOW the request sits at its stage:
                    READY       claimable by the stage's worker now
                    CLAIMED     in flight under a lease (§11)
                    RETRY_WAIT  failed transiently; claimable when
                                next_retry_at is due
                    BLOCKED     requires a human decision or an
                                external resolution; blocked_reason
                                set
submission_state  — MONEY TRUTH (§7.1):
                    NOT_SUBMITTED / MAYBE_SUBMITTED / SUBMITTED
outcome           — FINAL RESOLUTION, write-once:
                    NULL (active) / EXECUTED / REJECTED / CANCELLED
                    / SUPERSEDED
```

Nothing stores "who acts next" — it is derived (§4.5). Nothing stores
the old 13-value status — it survives only as a derived DISPLAY LABEL
(§10.4) for dashboards, logs, the card, and ops; no rule may key on
it.

Supporting fields:

```text
payment_obligation_id
amount              — IMMUTABLE after creation (§6.3)
blocked_reason      — set iff stage_state = BLOCKED (§13 codes:
                      RETRY_EXHAUSTED, UNMAPPED_CODE, AMOUNT_MISMATCH,
                      CUTOFF_EXPIRED (RESERVED round 10 — never
                      produced; the engine owns the calendar, §7.4;
                      kept to avoid CHECK churn),
                      ENGINE_INCONSISTENCY,
                      AMENDMENT_PARKED, OPS_PARKED, ESCALATED —
                      ESCALATED is the §9.3 max-age escalation of a
                      MAYBE_SUBMITTED row, kept distinct from
                      OPS_PARKED so the §15 BLOCKED queue can rank the
                      money-critical class first)
idempotency_key /
end_to_end_id       — generated and persisted before POST (UNIQUE)
uetr                — SDK/engine-assigned, persisted from the
                      POST response (UNIQUE, NULL until assigned),
                      see §5
version             — CAS counter
claim fields        — claimed_by, claim_expires_at
post_attempt_seq    — monotonic posting-claim counter (added
                      2026-07-16, §14.1): +1 in EVERY posting-claim
                      CAS, NEVER reset — the §9.2 downgrade resets
                      attempt_count (a retry BUDGET) but not this
                      column (an IDENTITY): it pairs the §14.1
                      journal's STARTED/RESOLVED events, and a
                      resettable counter would collide on the
                      post-downgrade re-POST
retry fields        — attempt_count, next_retry_at, last_error_code
                      (retry_deadline_at exists but is RESERVED/
                      unused — the 2026-07-11 bounds decision, §7.4:
                      MAX ATTEMPTS is the retry limit; the engine
                      owns the cutoff calendar)
resolver fields     — next_query_at (per-row query backoff, §9.5). NO consecutive-answer counter exists
                      (a column whose only job is answer
                      validation was rejected as over-design — the
                      §1 assumed collision contract makes acting on
                      a single post-trust-age answer safe)
created_at          — insert timestamp; the §5 runbook's replay-window
                      query keys on it
state_changed_at    — updated on every dimension-changing CAS (single
                      timestamp, no history); the LAST-WRITE clock
                      only: serves non-churning ages (BLOCKED
                      queue age) and is-anything-moving checks. Age
                      RULES key on the episode anchors below — churn
                      resets this clock and would silently re-arm
                      them (§15 clock discipline)
creating_ordering   — the upstream_ordering value at creation time;
                      input to the §6.8 successor policy and the
                      marker ordering tags
provider_reference  — engine-assigned reference, if any, persisted
                      from the POST response; secondary feed-matching
                      key (§8); a distinct field from the uetr,
                      never merged with it
last_sent_hash /
divergence_expected — persisted in the claim transaction BEFORE the
                      HTTP call, every attempt: the hash of
                      the canonically-serialized instruction being
                      sent, plus a per-attempt flag computed AT CLAIM
                      TIME, before the overwrite —
                      divergence_expected := (previous last_sent_hash
                      IS NOT NULL AND differs from the new hash).
                      The flag exists because the comparison is
                      impossible at collision-response time: the
                      overwrite has already destroyed the prior
                      attempt's hash, the §7.2 branch discriminator. If the worker dies mid-call these are
                      the only record of what may be executing; the
                      send instant is last_post_attempt_at (below) —
                      one clock, not two. The instruction CONTENT is
                      never persisted ON THE PAYMENT TABLES — details
                      are re-resolved fresh per attempt (§7.0). The
                      engine's copy is authoritative but (recorded
                      2026-07-16, PO) NOT VISIBLE to this team —
                      status is queryable, content is not — so the
                      local content record lives in the §14.1
                      attempt journal, written in this same claim
                      transaction. A DR-replay-recreated row has no prior
                      hash → divergence_expected false → a collision
                      correctly classifies ANOMALOUS (§5.1/§7.2).
                      (Considered and REJECTED: replacing these two columns
                      with an append-only attempt-history table;
                      REJECTED at PO review — the flag is audited
                      and correct, a schema change is not justified
                      by elegance, and the forensics gap is closed
                      by the sent hash on the posting-claim log
                      line, §14. That NARROW rejection stands
                      unchanged: the §14.1 journal replaces neither
                      column, is never read at runtime, and exists
                      for the 2026-07-16 content-visibility driver,
                      not for elegance.)
divergent_payload_at — write-once; set when the engine reports a
                      known-key-different-payload collision (§7.2 —
                      expected or anomalous). Meaning: the engine
                      already HOLDS this key, so further re-POSTs are
                      futile. While set, repost_permitted (§7.0) is
                      false FOREVER — resolution is status query or
                      ops, never another POST
maybe_since         — set ONCE when submission_state first becomes
                      MAYBE_SUBMITTED; cleared when it leaves AND by
                      the outcome-setting normalization (§10.2 —
                      a terminal-negative row keeps submission_state
                      = MAYBE forever, and without the clear the
                      MAYBE-age alerts and escalation scanner would
                      misfire on frozen rows; their scopes also carry
                      outcome IS NULL, belt and braces). The §9.3
                      escalation clock and the §15 MAYBE-age alerts
                      key on it — state_changed_at churns on every
                      dimension hop and silently re-arms any age
                      alert keyed on it
escalated_at        — set ONCE when the §9.3 escalation STATE WRITE
                      first fires for the current MAYBE episode;
                      cleared with maybe_since. §9.3's
                      BLOCKED(ESCALATED) write is gated on
                      escalated_at IS NULL — escalation fires once
                      per episode, so a §9.2 downgrade that un-parks
                      an ESCALATED row (maybe_since already past the
                      threshold) cannot enter a downgrade ⇄ escalate
                      cycle
submitted_at        — set when submission_state becomes SUBMITTED;
                      the §9.2 SUBMITTED-branch trust-age and the
                      §9.5 confirmation age key on it
last_post_attempt_at — stamped in the posting-claim transaction,
                      BEFORE the HTTP call, alongside last_sent_hash
                      (stamping on response processing would
                      leave the anchor unset/stale exactly in the
                      crash and lease-expiry cases that produce
                      MAYBE, making the trust-age unevaluable or
                      prematurely elapsed); the §9.2 MAYBE-branch
                      trust-age keys on it — each new attempt
                      restarts the trust-age clock, which maybe_since
                      (a first-episode anchor) must not do
```

Timestamp discipline: a timestamp column exists iff a NAMED rule
or alert keys on it. `state_changed_at` is the single last-write
clock ("is anything moving"); every AGE rule keys on a set-once
episode anchor (`maybe_since`, `escalated_at`, `submitted_at`,
`last_post_attempt_at`, `validation_failed_first_at` §2.1 — and
`divergent_payload_at`, consumed as IS-NULL by repost_permitted §7.0
rather than as an age) because the last-write clock churns. No per-dimension `*_changed_at` columns exist: transition
history lives in the §14 log line, not in columns (the
no-TRANSITION-HISTORY-journal decision; the §14.1 attempt-content
journal is a separate audit sink, not transition history). Terminal time needs no column — the frozen-row convention
(L1) freezes `state_changed_at` at the outcome transition, so for
terminal rows `state_changed_at` IS the outcome time (the future
terminal-row retention/archival design — §18 tech-lead item —
relies on that convention).

Constraints (the DB is the backstop for every invariant the code
enforces): per-column enum CHECKs; the §10.3 legality matrix as CHECK
constraints (L2–L8 and L1's terminal-row shape; L1's freeze and the
release guard are trigger backstops — §10.3); and:

```text
- UNIQUE (idempotency_key)
- UNIQUE (uetr)                — NULL until assigned; Oracle ignores
                                 NULLs in the index
- I6: at most ONE ACTIVE request per obligation, enforced with a
  function-based unique index ON THIS TABLE
  (CASE WHEN outcome IS NULL THEN payment_obligation_id END)
```

Vocabulary: a request is ACTIVE iff `outcome IS NULL`; TERMINAL
otherwise; TERMINAL-NEGATIVE iff outcome IN (REJECTED, CANCELLED,
SUPERSEDED).

### 2.3 processed_inbound_event (inbox)

```sql
CREATE TABLE processed_inbound_event (
    event_id     VARCHAR2(64)  NOT NULL,
    source       VARCHAR2(30)  NOT NULL,   -- e.g. PAYMENT_STATUS_FEED
    processed_at TIMESTAMP(6)  DEFAULT SYS_EXTRACT_UTC(SYSTIMESTAMP) NOT NULL,
    CONSTRAINT pk_inbound_event PRIMARY KEY (source, event_id)
);
```

Dedup of identical feed redeliveries (§8). Requires a purge policy
whose retention exceeds the maximum plausible replay window (at least
the Kafka topic retention; ownership §16.2).

There is deliberately NO parked-event table: unmatched feed events are
logged, counted, and acked (§8); recovery of any real missed outcome
is query-based (§9), keyed by identity this system already persists.

### 2.4 trade_snapshot_state (snapshot admission — added 2026-07-11 round 5)

One row per trade (business_id). It owns the two trade-wide facts
that per-obligation rows structurally cannot: which snapshot ordering
this trade has ACCEPTED, and which stored document that was.

```text
business_id              — PRIMARY KEY (one row per trade)
last_accepted_ordering   — ordering value of the last ADMITTED
                           snapshot; same representation and pluggable
                           comparator as upstream_ordering (§6.7 —
                           business timestamp today, explicit sequence
                           later, no logic change on cutover)
last_xml_storage_id      — the immutable storage id (+ version) of
                           that snapshot in the upstream-populated
                           store (§6.0 transport, upstream ask 8);
                           THE durable pointer to "the most recent
                           snapshot" — §7.0 instruction assembly and
                           §20-10 reprocessing read it
last_payload_digest      — the canonical business-payload digest of
                           that snapshot (SAME algorithm as the §9.3
                           approval digest); what makes §6.0/§6.7 tie
                           equality — including the trade reference —
                           EVALUABLE against applied state (round 5)
updated_at               — audit timestamp (DB time, §16.4)
```

Why it exists (money safety, not audit — round-5 review H-1): a
payment block whose scope has no obligation row has no watermark to
be checked against, so without a trade-level watermark a DELAYED
OLDER full snapshot could CREATE and pay a payment that the newer
authoritative snapshot says does not exist. The row is the admission
gate's lock and memory (§6.1): it is locked (SELECT ... FOR UPDATE;
insert-on-first-contact with PK-race retry, the §6.1 pattern) as the
FIRST lock of snapshot processing — lock order: trade row, then
obligations in scope-tuple order, so the existing deadlock-freedom
argument is preserved. Two concurrent first snapshots for one trade
serialize HERE (previously nothing existed to lock). A
failed-validation message NEVER advances this row (§6.6 — the same
rule as upstream_ordering). This table is payment data (§16.5 schema
contract) — it is NOT the §9.3 ops-schema approval store, which
remains a separate, sanctioned, non-payment store.

GREENFIELD FACT (PO, 2026-07-11 — supersedes the round-6..9
bootstrap/pointer machinery): this flow is a NEW FEATURE — no
pre-existing trades or obligations exist at cutover, and any
received message is by definition a new trade or an amendment
to one this flow itself created. Consequences:

```text
- trade_snapshot_state legitimately starts EMPTY; every row is
  created by its trade's FIRST ADMITTED message WITH storage id
  and digest populated. A NULL digest/pointer row CANNOT exist.
- Considered and REMOVED (drivers gone with the greenfield
  fact; retained in git history at commit 9a53c75 — restore
  ONLY if a future deployment inherits pre-existing trades):
  the S-11 bootstrap job (retired) + digest-NULL semantics (retired),
  the transitional legacy-assembly flag (retired), the
  pointer-presence claim-gate term (retired), the pointer-coverage
  gate + pointer-residue alert/view (both retired), and upstream
  ask 9 (WITHDRAWN — nothing to export).
- Mixed-version note (Section M): no prior application version
  consumes these snapshots, so admission enforcement is ON
  from day one — there is no drain step and no second point
  of no return.
- Archival (TL-14): the trade row archives WITH its trade.
- §5.2 restore note (HONEST bounds, round 11 — post-MVP scope):
  a database restore can REGRESS this row or remove it entirely
  (trade admitted after the restore point). Kafka replay within
  retention re-creates/converges it. OUTSIDE the replay window:
  the conservative ORDERING is re-derivable from the obligations
  (max upstream_ordering), but last_xml_storage_id and the digest
  are NOT — they exist only here. Such a trade needs an upstream
  store lookup or manual reconciliation before tie adjudication /
  reprocess can run for it; this is an explicit step for the §5.2
  runbook (post-MVP), not a solved property.
```

------

## 3. Money: Reservation Semantics and Invariants

`committed_amount` uses reservation semantics — "money spoken for,"
not "money at the engine":

```text
INCREMENT  in the same database transaction that inserts the
           payment_request row (request creation, §6.8).

DECREMENT  in the same database transaction as the CAS that sets
           outcome to a terminal-negative value (REJECTED, CANCELLED,
           SUPERSEDED). Executes only when the CAS affects exactly
           one row — once per transition, redelivery-safe.

NO MOVEMENT at POST time, on the POST response, on retry of the same
           logical request, or on confirmation (confirmation moves
           confirmed_amount only).
```

Considered and REJECTED (PO review — recorded so it is not
relitigated): deriving committed_amount instead of storing it
(confirmed + the single active row's amount, per I6). The stored
counter is kept DELIBERATELY: the redundancy is a tripwire — the
drift scanner's I1 cross-check compares the counter against actual
row state and detects row-corruption and money-math bugs that a
by-definition derivation could never surface; and the counter
choreography has survived ten audited review rounds, so replacing it
buys marginal simplification at the cost of re-reviewing the entire
money model.

Consequences:

- No counter mutation is ever adjacent to an external call; a crash or
  timeout around the POST cannot desynchronize the counters. A
  MAYBE_SUBMITTED request keeps its reservation — the safe default.
- Shortfall always sees in-flight money; the request-creation
  double-pay window does not exist.
- The failure mode is fail-blocked, not pay-twice: a stalled request
  holds its reservation until explicitly released.

Invariants (enforced in code, verified by the drift scanner):

```text
I1  committed_amount = Σ amount over requests where
      outcome IS NULL OR outcome = EXECUTED
I2  confirmed_amount = Σ amount over requests where outcome = EXECUTED
      (holds until terminal-row archival introduces a rollup — §18)
I3  confirmed_amount <= committed_amount
I4  overpay_blocked = 'Y' ⇐ confirmed_amount > required_amount
I5  shortfall := required_amount - committed_amount
      (computed ONLY under the obligation lock)
I6  at most one ACTIVE request per obligation (DB-enforced, §2.2)
```

Drift detection: a scheduled job recomputes I1/I2 from the request
table and pages (not logs) on any mismatch. The scanner reads a
consistent snapshot (SCN/flashback query) and re-checks any mismatch
under the obligation lock before paging — routine read skew between a
counter update and a request insert in another session's uncommitted
transaction must never page. The drift job also verifies the
non-CHECK-able legality/totality rules (§10.3 L9).

Required supporting features:

- An explicit supersede/close operation that sets outcome =
  SUPERSEDED/CANCELLED on a stalled active request (e.g. BLOCKED, or
  stuck at ENRICH) and releases its reservation. FORBIDDEN while
  `submission_state` is `MAYBE_SUBMITTED` or `SUBMITTED` unless
  driven by an authoritative engine negative (§9.4, §10.1) — releasing
  a reservation whose money may have moved is the one remaining
  double-payment path.
- An age-based alert on requests holding a reservation without
  progressing (stuck-reservation monitor, §15).
- If reporting needs "money accepted by the engine", derive it as
  Σ amount where submission_state = SUBMITTED AND (outcome IS NULL OR
  outcome = EXECUTED) — accepted-then-rejected money is excluded, and
  it is never a second maintained counter.

------

## 4. Derived State

Status is derived from money and stored markers, never the other way
around, and never accumulated:

```text
1. An authoritative event changes the request's dimension columns via
   a conditional CAS UPDATE guarded by the legality matrix (§10.3)
   and the evidence rules (§4.4, §10.1).
2. Only when that UPDATE affects exactly one row do the obligation
   amounts move (§3).
3. Step status, the active exception, AND the next actor are then
   re-derived under the obligation lock, in the same transaction
   (§11).
```

### 4.1 Step-status predicate

```text
step complete ⇔
    required_amount IS NOT NULL AND required_amount > 0
    AND confirmed_amount >= required_amount
    AND committed_amount = confirmed_amount
    AND overpay_blocked = false
    AND validation_failed is not LIVE
        (LIVE = marker set AND its ordering >= upstream_ordering,
         §2.1/§6.9 — a stored, ordering-gated marker, NOT the derived
         exception itself, to avoid circular derivation)

step cancelled ⇔                      (round 11 — the zero-required
    required_amount = 0                terminal branch; PO-9 absence
    AND committed_amount = 0           = amendment to zero, §6.1)
    AND confirmed_amount = 0
    AND no ACTIVE request exists
    AND overpay_blocked = false
    AND validation_failed is not LIVE
```

The CANCELLED branch is TERMINAL like COMPLETED (displayed as
CANCELLED, never COMPLETED — §12): a payment removed by upstream
truth, fully unwound (reservation released, nothing confirmed).
It is REOPENABLE exactly like COMPLETED is (§6.5): a strictly newer
snapshot carrying the block again applies normally
(required_amount := the new positive value) and the derivation
returns the step to IN_PROGRESS. If money already moved
(confirmed > 0), the zeroing write instead trips the §6.5/§13
overpay latch — the OBLIGATION derives IN_PROGRESS with
overpay_blocked = true and active exception OVERPAY_DETECTED,
never CANCELLED. NO request-state mutation is implied (round 12):
an executed request is terminal/frozen; BLOCKED is a
payment_request stage_state, and no active request need exist on
a zeroed scope. Ops disposition per §10/§13 governs (the PO's
"same as overpay: we stop").

- The first and last terms guard against vacuous completion: an anchor
  obligation created from a failed-validation message (§6.6) has no
  valid required_amount, and without them the predicate
  `0 >= 0 AND 0 = 0` would derive COMPLETED for a trade whose payment
  data was rejected. A later valid message makes `validation_failed`
  not-live in the same transaction that applies it, so a recovered
  scope can complete.
- `required_amount = 0` vs `NULL` is load-bearing (round 11): inbound
  blocks are strictly POSITIVE (§6.0), so 0 can only be written by the
  §6.1 absence path — it is the removal tombstone, and it needs no new
  column or state. NULL remains "no valid data ever applied" (anchor);
  an anchor can reach the CANCELLED branch only via the §6.1
  ordering-aware anchor retirement (which zeroes it AND advances the
  watermark past the failure marker in one write).
- `provider_rejected` is DELIBERATELY absent from the CANCELLED branch
  (round 12): removal does not launder reject history, and reject
  history does not resurrect a removed payment. At count 1 the zeroing
  write's watermark advance makes the marker not-live naturally; at
  count >= 2 the marker stays LIVE (§2.1 — ops-only clearing) yet the
  scope STILL derives CANCELLED — the payment no longer exists, so
  nothing is being refused. While required_amount = 0, §4.2 suppresses
  ONLY the historical provider-reject exception (round 13 — markers
  stay STORED, never cleared by removal; a LIVE validation marker
  still surfaces: it is the malformed-reappearance signature). If the
  payment REAPPEARS (§6.5), required becomes positive,
  the exception resurfaces, and request creation remains subject to
  ALL §6.8 gates — a live provider_rejected marker still blocks the
  automatic successor, and count >= 2 still requires the ops-only
  clear. Reappearance NEVER auto-pays through a live marker.
- `committed_amount = confirmed_amount` alone is insufficient: after a
  terminal-negative decrement both can be zero while `required_amount`
  is unpaid. The `confirmed_amount >= required_amount` term is
  mandatory.
- Because every ACTIVE request holds a reservation (§3), the predicate
  automatically encodes "no unresolved request exists": any active
  request keeps `committed_amount > confirmed_amount` and blocks
  completion.
- `ui_step_status` is always the output of this derivation. It shall
  never be copied from an inbound feed event, even when matched by
  UETR — a UETR identifies one request; step completion is a
  cumulative, obligation-level property.

### 4.2 Active-exception derivation

The active exception is **derived, never accumulated**: after every
applied mutation it is recomputed under the obligation lock from live
conditions. There are no set/clear rules to get wrong — a retry
success, a corrected upstream message, or a completion each changes
the underlying condition, and the next derivation reflects it. In
particular, a corrected message clears a DATA_VALIDATION_FAILED
exception by construction (it makes the validation_failed marker
not-live), which also unblocks the §4.1 completion predicate.

Zero-required suppression (round 12; NARROWED round 13 — the
broad "skip all marker ranks" rule silently hid a malformed
reappearance): a scope with required_amount = 0 (removed by
upstream truth, §6.1) suppresses EXACTLY ONE condition:
PROVIDER_REJECTED, and only while it is HISTORICAL —
provider_rejected_ordering < upstream_ordering, live solely
because provider_reject_count >= 2 — a payment that no longer
exists is not being refused. DATA_VALIDATION_FAILED is NEVER
suppressed: a LIVE validation marker on a zeroed scope is
precisely the malformed-reappearance signature (§6.6 recorded a
failing ordering NEWER than the removal — someone is trying to
bring the payment back and failing); the step is IN_PROGRESS
(the §4.1 CANCELLED branch requires the marker not-live) and the
exception plus the §15 marker-age alert MUST surface it (P5). The
provider marker stays STORED (monotonic writes untouched) and its
exception resurfaces when the payment reappears (required > 0,
§6.5). Active-REQUEST conditions and the latch derive NORMALLY on
a zeroed scope: an in-flight MAYBE still shows
PAYMENT_OUTCOME_UNKNOWN (money may be moving), and
OVERPAY_DETECTED still shows when the latch is set.

Derivation — first live condition wins (precedence order; all request
conditions consider ACTIVE requests only):

```text
Rank 1:  PAYMENT_OUTCOME_UNKNOWN  if the active request has
                                  submission_state = MAYBE_SUBMITTED
                                  (any stage / stage_state — money may
                                  be moving; must never surface as a
                                  generic retryable error, §9.3)
         OVERPAY_DETECTED         if overpay_blocked latch is set
                                  (money-in-limbo outranks
                                  money-overpaid: the former is still
                                  changeable decision)
Rank 2:  DATA_VALIDATION_FAILED   if validation_failed marker is LIVE
                                  (§6.9)
         PROVIDER_REJECTED        if provider_rejected marker is LIVE
         (manual_action_required) (§6.9) — the data we hold is fine;
                                  the engine (or ops) refused the
                                  payment
         BLOCKED-derived          if the active request is BLOCKED
         (manual_action_required) (blocked_reason gives the code, §13)
Rank 3:  INSUFFICIENT_ACCOUNT_BALANCE (retryable per policy, §13)
                                  from the active request's
                                  last_error_code
Rank 4:  SYSTEM_UNAVAILABLE (retryable) from the active request's
                                  last_error_code
none     otherwise
```

Rationale for the precedence: the two rank-1 conditions are exactly
the states where a human must act; a transient SYSTEM_UNAVAILABLE
blip must not mask them on the card.

### 4.3 Stored derivation inputs

Set by events, read by derivation — stored because they are not
computable from amounts alone (staleness guards in §6.9):

```text
overpay_blocked      — the latch (§13)
validation_failed    — invalid-data marker (§2.1)
provider_rejected    — negative-outcome marker (§2.1, §7, §8)
reopened_at          — step reopening marker (§6.5); lets the card
                       indicate "reopened"
submission_state     — on the request (§2.2)
blocked_reason       — on the request (§2.2)
ops annotations      — e.g. overpay acknowledgement (§20)
```

### 4.4 Evidence rules (feed-status precedence)

Inbound engine statuses are ranked (full vocabulary and rank table:
companion artifact, §16.6) and evidence application is monotonic:

```text
- Terminal evidence (settlement / authoritative reject) applies to
  any ACTIVE request per §10.1's terminal-evidence rule.
- Intermediate evidence (acceptance) applies only to rows that are
  NOT CLAIMED (a live claim's own response or lease expiry owns the
  row; yanking dimensions from under an in-flight worker is
  forbidden — the evidence is a no-op there and the outcome arrives
  another way). On a non-CLAIMED row it sets submission_state =
  SUBMITTED, stage = CONFIRM, and stage_state = READY (clearing
  next_retry_at) — unless the row is BLOCKED, in which case BLOCKED
  and its reason are preserved (CONFIRM·BLOCKED is legal, L5). It
  never regresses any dimension.
- Stale or duplicate evidence affects zero rows (legality matrix +
  outcome write-once + submission monotonicity) → ignored.
```

### 4.5 Next-actor derivation (never stored)

```text
actor := outcome set                          → nobody (terminal)
         submission = MAYBE_SUBMITTED         → resolver (§9.5 —
                                                 ALWAYS, regardless of
                                                 stage or stage_state)
         SUBMITTED, active, older than
           confirmation age                   → resolver sweep (§9.5)
         stage_state = BLOCKED                → ops (§20)
         stage_state = RETRY_WAIT, due       → retry scanner (§7.4)
         stage_state = READY                  → the stage's worker
         stage_state = CLAIMED                → nobody (lease rules §11)
```

The resolver branches do not exclude the others: a row can have two
actors — BLOCKED + MAYBE → ops AND resolver (§9.3); RETRY_WAIT +
MAYBE (the §9.2 downgraded row) → retry scanner AND resolver, because
the scanner's same-key re-POST is exactly the action that settles the
question. Correct and intended.

------

## 5. Request Identity and Disaster Recovery

The `payment_request` row shall exist durably before any POST:

```text
No POST shall be made under a CALLER-SUPPLIED identity
(idempotency key / end_to_end_id) that is not already durably
persisted on the payment_request row. (The SDK-minted UETR is
outside this rule by construction — it does not exist before the
call; see the identity chain below.)
```

Identity chain, in order of availability:

```text
payment_request.id      — always present
end_to_end_id /
idempotency_key         — generated and persisted before POST
uetr                    — assigned by the PLATFORM SDK (from the PO): the SDK's validate-and-POST
                          method generates the UETR internally; this
                          system neither generates nor validates it.
                          Persisted from the POST response, when one
                          arrives
```

Rules:

- Retries of the same logical request reuse the same IDEMPOTENCY
  KEY — that is the identity money safety rides on. The UETR is the
  SDK's affair: a re-POST of the same key may carry a fresh
  SDK-minted UETR, which is why nothing money-safe may key on the
  UETR (engine dedup MUST key on the caller-supplied idempotency
  key — §18 tech-lead ask 11).
- The UETR is persisted ONLY from ACCEPTANCE-class responses:
  engine accepted, or an original-response replay (artifact 1
  class). Rejection and collision responses — DUPLICATE_REQUEST,
  known-key-different-payload, sync rejects — NEVER write or
  overwrite `uetr`: the UETR they carry identifies a submission
  under which NOTHING EXECUTES, and persisting it would orphan the
  real payment's feed events (which arrive under the EXECUTED
  attempt's UETR) and could let a feed reject emitted under the
  dead UETR match the row as an authoritative negative — releasing
  the reservation of a payment that actually executed. Any OTHER
  engine-assigned reference is persisted as `provider_reference`
  (§2.2). Reconciliation of an ambiguous outcome falls back to the
  pre-persisted `end_to_end_id`/idempotency key.
- A crash/timeout BEFORE the response leaves the attempt with no
  recorded UETR: its feed events go unmatched (§8 — logged, counted,
  acked) and the §9 sweep recovers the outcome by idempotency key.
  Existing machinery; no new rule.
- Requests that fail before POST never receive a UETR; they are
  identified by `payment_request_id`.

### 5.1 Deterministic derivation (the DR keystone)

```text
idempotency_key = hash(business_id | payment_type | debit_account |
                       currency | request_seq)   ← THE DR keystone
request_seq     = payment_obligation.next_request_seq (§2.1),
                  incremented under the obligation lock in the same
                  transaction that inserts the request
```

The UETR is deliberately NOT part of this derivation: UETR
generation, format compliance, and validation belong to the payment
platform's SDK (§5 identity chain) — generating one ourselves would
be unnecessary work and additional risk. Dedup and DR ride on the
idempotency key alone: a post-restore recreated request regenerates
the SAME key, so the engine rejects DUPLICATE_REQUEST regardless of
whatever fresh UETR the SDK mints; feed events bearing a lost UETR
go unmatched and are recovered by the §9 sweep, which queries by
key.

The idempotency key shall be derived from business state, never
generated randomly.
Rationale: write-ahead persistence protects against a crash (RPO = 0)
but not against a database RESTORE to an earlier point (RPO > 0). In
a restore, a payment that was persisted, posted, and confirmed can
lose its row; the orchestrator then sees an unpaid shortfall and
re-creates "the same" request. With random keys, the re-creation
carries a fresh key → the engine executes a brand-new payment →
duplicate. With deterministic keys, the re-creation regenerates the
SAME key → the engine rejects with DUPLICATE_REQUEST → §7 routes it
to ambiguous-outcome handling → the status query recovers the lost
outcome. One derivation rule converts a restore from "money at risk"
into a recoverable reconciliation problem — fully operationalized
only by the §5.2 runbook (post-MVP); until that exists, a restore
is handled as a major incident with manual engine-side
reconciliation (§5.2 PO decision).

The amount is deliberately NOT part of the key: hashing it in would
regenerate a fresh key whenever a post-restore replay recreates a
request with a diverged amount — silently reintroducing the duplicate
the deterministic key exists to prevent. A known-key-different-payload
submission is instead rejected by the engine and routed per §7.2 —
a replay divergence is the ANOMALOUS branch (a replay-recreated row
has no prior last_sent_hash, so divergence_expected is false):
BLOCKED(ENGINE_INCONSISTENCY) + status query.

Exactness requirement: the derivation shall be specified to the
byte — hash algorithm, field serialization order, delimiter,
canonicalization (case, trimming, encoding, account-number
normalization) — versioned, and
frozen by golden-vector tests (§16.6). Byte-identical reproducibility
across releases and a database restore IS the DR property.

Instruction hash: the claim transaction that persists identity
also persists the hash of the instruction actually sent
(last_sent_hash, §2.2, before the HTTP call). There is deliberately
NO payload freeze: details are re-resolved fresh per
attempt (§7.0) — the DR property is the deterministic KEY plus the
engine's known-key-different-payload rejection (§7.2), exactly as
this section always stated for the amount. The stored hash lets the
§7.2 handler distinguish expected divergence (newer details
knowingly carried) from replay anomalies.

### 5.2 Post-restore runbook (post-MVP — good to have)

MVP scope (PO decision): DR RECOVERY IS NOT AN MVP DELIVERABLE.
The deterministic-key design (§5.1) stays regardless — it is free at
design time and keeps a restore recoverable in principle — but this
runbook, its tooling (the step-5 query modes below), and the ops
drill are built when DR recovery is scheduled, not for go-live.
Until then, a database restore is handled as a major incident with
manual engine-side reconciliation.

```text
1. Freeze posting — Hazelcast toggle (§16.1), deliberately OUTSIDE
   the database being restored, so the restore cannot un-freeze it;
   absence of the toggle also reads as frozen.
2. Restore the database.
3. Rewind Kafka consumer groups to at or before the restore point
   (replay is safe: inbox + evidence rules + §6.7/§6.9 guards).
   While the replay window is being consumed, UNMATCHED feed events
   are a CRITICAL alert, not log-and-drop (§8 exception):
   during replay they are exactly the signature of a payment that
   executed pre-restore and whose row was never re-created.
4. Resolver sweep over every ACTIVE request with submission_state
   MAYBE_SUBMITTED or stage POST/CONFIRM.
5. Status-query EVERY request re-created during the replay window by
   its deterministic key, INCLUDING requests the replay left
   terminal. Executor: the resolver's ops-triggered mode, which
   queries an explicit key set regardless of state; the window is
   selected on created_at (§2.2).
5b. Sequence-divergence guard: for EVERY obligation touched in
   the replay window, ENUMERATE the deterministic key space
   hash(scope | seq) and status-query each key, up to a PROVEN
   upper bound — the maximum request_seq observed in the §14 logs
   for that obligation across the replay window (the log platform
   is outside the restored database and its retention floor covers
   the replay window by definition; the posting-claim log lines
   even identify exactly which keys hit the wire). Only if the log
   platform is unavailable, fall back to the heuristic stop of K
   consecutive NOT_FOUNDs past the current next_request_seq
   (config §16.6) — recorded as HEURISTIC (reviewer
   follow-up): enrichment-only requests never reach the engine, so
   a NOT_FOUND run below a later executed sequence can be
   arbitrarily long, and K can stop short of it. Rationale:
   a replay can consume sequence numbers DIFFERENTLY than the lost
   history — e.g. a request that originally died at enrichment now
   enriches successfully (fresh assembly against the CURRENT trade
   store, which is not restored with our database), consumes the
   shortfall, and the corrected-message successor that actually
   EXECUTED pre-restore is never re-created. Its key then belongs to
   no row: step 5's row-keyed sweep never asks about it, and after
   unfreeze the replayed row POSTs a genuinely-unused key — a
   duplicate. Enumeration closes this because the key space per
   obligation is derivable (§5.1) — no external key store or extra
   correlation id is needed; the TL-5 lookback covers the replay
   window by construction. Any EXECUTED/pending answer for a key
   with NO matching row → the §8 evidence-for-terminal CRITICAL
   path; ops resolves before unfreeze.
6. Drift check (§3).
7. Unfreeze.
```

Retention constraint, written down (owner: §16.2):
`inbox_retention > kafka_retention ≥ replay_window`.

Step 5 exists because a replay can diverge from the original history
where a decision depended on a race outcome recorded only in the lost
rows. Example: originally an auto-cancel (§6.4) LOST the race to a
posting claim and the payment executed; under the replay's posting
freeze the auto-cancel WINS, the request ends CANCELLED, and a
successor request is created for the new shortfall. The database is
internally consistent and totally wrong: the cancelled request's key
already hit the wire. The pre-unfreeze query finds the executed
payment, hits the evidence-for-terminal-request CRITICAL path (§8),
and ops resolves before the successor ever posts.

------

## 6. Inbound Flow 1: Upstream Trade Messages

### 6.0 Message contract (normative)

The message is a FULL-TRADE SNAPSHOT (§1 contract facts): it carries
the complete current truth of the trade, including ALL of its
payments; a newer snapshot supersedes the older one in full. The
upstream Kafka message shall carry:

```text
Trade level (once per message):
business_id             — ALSO the Kafka message key (upstream ask 2,
                          §18): partition routing AND §6.6 key-only
                          anchoring depend on it
ordering value          — the business timestamp TODAY; an explicit
                          sequence in the FUTURE (§6.7); exact field
                          name: upstream contract (§18); ONE value
                          per snapshot, shared by all payment blocks
trade reference         — the id used to look up payment details
ui_process_instance_id, ui_step_instance_id
                        — display/reference (§2.1)
correlation_id          — cross-system tracing (§2.1, §14)

Payment block (one per payment RELEVANT TO THIS SYSTEM; 0..N):
payment_type, debit_account, currency
                        — scope-key fields (§2.1); with business_id
                          they identify the payment
required_amount         — positive; currency-scale validated (§16.4)

Derived-set fact (PO, 2026-07-12 — §1 role derivation): the raw
message carries the trade's full SETTLEMENT set; the payment
blocks above are what THIS SYSTEM derives from it by role
(payer/receiver). An EMPTY derived set is VALID — the snapshot
still passes admission (§6.1) and its emptiness is meaningful:
every existing non-anchor obligation of the trade is absent from
it → amendment to zero (§6.1). It creates no obligations — but it
DOES create/advance the §2.4 trade admission row like any admitted
snapshot (otherwise a DELAYED OLDER snapshot carrying a payment
this newer truth removed would find no watermark and pay it — the
round-5 hole). Display: a trade with only zeroed/no obligations
renders per §12; it is never an error.
```

Intake validation — within-snapshot uniqueness: the tuple
(payment_type + debit_account + currency) shall be unique across the
payment blocks of one snapshot. A snapshot violating this is a
VALIDATION FAILURE for the whole message (fail closed: §6.6 handling,
alert) — it is the one runtime-checkable edge of the §1 cardinality
contract: applying such a snapshot would silently merge two payments
into one obligation, one payment's amount REPLACING the other's.

Payload equality (used by §6.7 tie handling) is defined over the
CANONICALIZED BUSINESS-FIELD SUBSET — the (possibly empty) SET of payment blocks
(sorted by scope tuple; each block: scope-key fields +
required_amount) + trade reference — never over raw bytes or envelope
fields (message ids, emission timestamps), which would turn every
redelivery into a false tie-conflict.

Contract fact (stated by upstream; formal confirmation tracked as
§18 upstream ask 4): upstream emits a new message ONLY when a
business field actually changed — there are no blind re-emissions of
identical snapshots. The §6.8 recovery model ("a newer message is
worth a new attempt") and the boundedness of the validation
reject cycle (§2.1 validation_reject_count) both lean on this fact.

Transport (contract fact, recorded 2026-07-11 from the PO/team):
the Kafka notification carries the SNAPSHOT STORAGE ID only — the
full snapshot XML is written to the upstream-populated store (a
database this service can read) BEFORE the notification is emitted;
this service fetches the XML by id, parses it, and processes exactly
as above (the field list describes the XML content; business_id
remains the Kafka MESSAGE KEY — ask 2). Consequences:

```text
- The XML store is an intake dependency: per-dependency timeout +
  breaker (§16.1); a fetch failure is a TRANSIENT intake failure —
  retry in place / pause the container, never ack-and-drop (§16.2).
- A notification whose XML row is MISSING after bounded retries is
  an upstream defect: DLT + page (a pointer without its payload).
- The stored XML is durable and RE-FETCHABLE by id: replays and
  ops-triggered reprocessing (§6.7 tie resolution, §20-10) re-read
  the SAME document — no payload content ever needs to travel in
  messages, logs, or alerts. The store contract (id stability,
  fetch-by-id sanctioned, retention ≥ the ops-queue/tie SLA) is
  upstream ask 8 (§18).
```

This schema is one of the three build-time-enforced contracts
(§16.5).

### 6.1 Normal processing

ADMISSION (trade-level, normative — added 2026-07-11 round 5; runs
BEFORE any per-block work, in its own transaction):

```text
1. Upsert-lock the trade_snapshot_state row (§2.4) for the
   document's business_id: INSERT on first contact (PK race →
   retry + re-read, the same pattern as obligation creation), then
   SELECT ... FOR UPDATE.
2. Compare the document's ordering value (pluggable comparator,
   §6.7):
   doc.ordering >  last_accepted_ordering
     → ADMIT: update the row (ordering, xml storage id, canonical
       payload digest), COMMIT, then fan out per block below.
   doc.ordering == last_accepted_ordering
     → digest EQUAL   → ADMIT WITHOUT UPDATE: a true redelivery or
       a crash re-run; fan out — per-block convergence no-ops the
       applied blocks and applies the remainder.
     → digest DIFFERS → AMENDMENT_TIE_CONFLICT (§6.7): NO block is
       applied, NO scope is created; resolution is §20-10, which
       passes THIS gate with the ≥ relaxation under an approved
       digest.
   doc.ordering <  last_accepted_ordering
     → REFUSE the whole document as stale: counted (stale metric),
       no block applied, and — the round-5 rule — NO NEW SCOPE IS
       EVER CREATED from a refused document.
3. Only an ADMITTED document may create or mutate obligations —
   and admission is a POINT-IN-TIME fact (round 6), so every
   BLOCK transaction passes the TRADE-SNAPSHOT FENCE (renamed
   round 7 — never "currency check": currency is a scope-key
   field in this system): it locks the trade row FIRST
   (SELECT ... FOR UPDATE — the global lock order is trade row →
   obligation → request), confirms that (last_accepted_ordering,
   last_payload_digest) still equal the values THIS worker
   admitted, and only then applies the block. On mismatch the
   fan-out STOPS: a newer snapshot owns the trade and the
   remaining blocks are ABANDONED — each abandoned block is
   LOGGED with its scope identifiers and counted (metric,
   round 7). Consequences: block transactions for one trade
   SERIALIZE on the trade row (this is what makes "no
   interleaved block application" literally true), and a stale
   worker can never create a scope — creation commits in the
   same transaction that proved the snapshot still current (no
   check-then-act window).
4. Kafka ack: the record is acknowledged ONLY after its fan-out
   completes (M6). Crash before that → redelivery; the re-run
   admits (== ordering, equal digest) and the per-block guards
   no-op whatever already applied. Partition ordering
   (business_id is the message key) means a redelivered snapshot
   ALWAYS re-runs before any newer snapshot of its trade is
   consumed — normal intake cannot overtake itself; the fence
   exists for the paths that ARE concurrent (§20-10 reprocess vs
   live intake; rebalance-zombie consumers).
5. BLOCK-LEVEL SUPERSESSION — the explicit business rule
   (round 7, replacing an INCORRECT round-6 sequential-
   equivalence claim; ratified by the design owner 2026-07-11;
   PO RATIFIED with the PO-9 answer, same day): a strictly newer ADMITTED
   snapshot supersedes the UNAPPLIED remainder of an older
   fan-out; obligations the older fan-out already created remain
   and are governed by BA-2 and the PO-9 absence semantics. This
   is NOT full-snapshot sequential convergence — sequential
   S1-then-S2 under absence-no-op would keep ALL of S1's blocks,
   supersession keeps only the applied prefix — and the outcome
   is therefore timing-dependent BY RULE, not by accident. The
   only production path that can experience it is a §20-10
   reprocess racing a newer live amendment (normal intake is
   partition-serialized); there, newer live truth beating an
   older adjudication is the intended answer — the same
   newest-wins stance as refusing to re-approve a stale
   document. Reprocess approvers are TOLD this at approval time
   (§9.3 display). Considered and REJECTED (round 6, upheld
   round 7): (a) an APPLYING → COMPLETE application state
   machine on the trade row (generation, owner, lease, fencing);
   (b) one atomic whole-snapshot transaction — both purchase
   full-snapshot completion for a path where supersession is the
   wanted semantics, at the cost of machinery or trade-wide
   locks.
```

A snapshot FANS OUT to one application per payment block. Message
validation (schema, amounts, within-snapshot uniqueness §6.0) runs
once for the whole snapshot; then, per payment block, in a
deterministic order (sorted by scope tuple — a fixed lock order so
concurrent redeliveries cannot deadlock), each block is applied to
its own obligation:

Under that obligation's lock: upsert obligation → apply
amounts/ordering (§6.7) → run the standing shortfall re-evaluation
(§6.8, the single request-creation point) → re-derive (§4).
`ui_step_status = IN_PROGRESS` from the first request.

Everything downstream of this point (§6.2–§6.9, §7–§13) operates per
payment scope, exactly as before — the snapshot model changes HOW
obligations receive updates, not what an obligation is.

Per-block transactions + at-least-once redelivery CONVERGE by the
§6.7 ordering guard: if the consumer dies mid-fan-out, the
redelivered snapshot re-applies — blocks already applied drop as
stale (ordering not newer for that obligation), unapplied blocks
apply normally. No cross-obligation transaction is required.

A payment block whose scope tuple has no existing obligation creates
one (the normal first-message path) — safe ONLY because the document
passed admission: a stale document never reaches this path (round-5
H-1; a new scope has no per-obligation watermark of its own). Two
concurrent first messages can race the obligation insert; the
scope-key unique constraint is the backstop — on `ORA-00001`, retry
the transaction and re-read.

RESOLVED — absence semantics (PO-9, ANSWERED by the PO 2026-07-11;
BA-2 amended accordingly, §1.1; lifecycle completed round 11): a
payment that exists as an obligation but is ABSENT from a newer
ADMITTED snapshot no longer exists — ABSENCE = AMENDMENT TO ZERO.
After the per-block fan-out, the same worker enumerates the trade's
obligations NOT carried by the document and, per obligation (own
transaction, trade-snapshot fence + obligation lock, ordinary
strictly-newer guard against doc.ordering), sets
required_amount := 0 AND advances upstream_ordering := doc.ordering
— the zeroing IS an application of the document to that obligation,
so it advances the watermark like any applied block (round 11: this
supersedes the older round-5 "no write for absent obligations"
wording — absence was a no-op then; it is a WRITE now).
Everything downstream is EXISTING machinery: unsent active request
→ §6.4 auto-cancel + release; in-flight → wait-then-decide (§6.4);
confirmed > 0 → the §6.5/§13 overpay latch — STOP (the PO's "same
as overpay"). A cleanly unwound row (0/0/0, no active request)
derives the §4.1 CANCELLED terminal branch — displayed CANCELLED,
never COMPLETED (§12).

Reappearance (round 11): removal is not a tombstone forever — a
STRICTLY NEWER snapshot carrying the block again applies normally
(required_amount := the new positive value; the §6.8 trigger
inventory fires) and the step returns to IN_PROGRESS. Round 12:
request creation on a reappeared scope remains subject to ALL
§6.8 gates — a live provider_rejected marker (count >= 2 =
ops-only clear, §2.1) still blocks the automatic successor;
removal never launders reject history and reappearance never
auto-pays through a live marker. Redelivery
of the zeroing document itself converges: the zeroed obligation
compares equal (same ordering, same absence) and no-ops.

Anchor retirement (round 11 — replaces the round-10 blanket
exclusion): a §6.6 anchor scope may be retired by absence ONLY when
doc.ordering > validation_failed_ordering — a truth STRICTLY NEWER
than the failure that omits the scope means the payment genuinely
no longer exists; the same zeroing write applies (required := 0,
upstream_ordering := doc.ordering), which makes the marker not-LIVE
and the row derives CANCELLED. A document whose ordering is NOT
strictly newer than the failure marker still cannot touch the
anchor (the round-10 protection this rule keeps: a failed snapshot
never advanced the trade watermark, so a later-ADMITTED document
can still be older than the failure — a malformed trade's scopes
must not be cancelled by a valid snapshot that predates their fix).

Zero-payment documents (round 11, PO role-derivation fact §1/§6.0):
a valid snapshot whose DERIVED payment set is empty is admitted
normally (it creates/advances the §2.4 trade row) and its fan-out
is PURE absence — every non-anchor obligation of the trade is
zeroed as above; anchors follow the ordering-aware rule. This is
how "the trade no longer requires any payment from us" — including
removal of the trade's ONLY payment — is represented.

RESOLVED — snapshot ordering-watermark rule (TL-16, §18; ANSWERED
2026-07-11 round 5; superseded detail round 11): the trade-level
ADMISSION gate above is the answer. Round-11 correction: the
round-5 clause "per-obligation watermarks are not advanced for
absent obligations" described the PRE-PO-9 no-op world and is
RETIRED — under absence = amendment to zero, the zeroing write
advances the per-obligation watermark (see the RESOLVED block
above); per-block watermarks otherwise remain the per-block
convergence/re-run guard. Both TL-16 failure traces still close at
admission: a delayed older snapshot is refused WHOLE, so it can
neither apply stale amounts to an existing absent-from-newer
obligation NOR create a never-seen scope (the sharper round-5
trace). The absence answer terminalizes absent obligations via the
§4.1 CANCELLED branch.

### 6.2 Zero shortfall

If the scope is already fully covered when the message arrives
(`shortfall = 0` and the §4.1 predicate holds), the derived step
status goes directly to `COMPLETED`. No request is created, no other
processing occurs.

### 6.3 Amount immutability

The amount of a payment_request is fixed at creation. Enrichment
resolves account numbers and party information only; it never modifies
amounts. A changed amount can only originate from a new upstream
message:

```text
required_amount increases → shortfall re-evaluated (§6.8) → new
  request immediately if no request is active; otherwise DEFERRED —
  created by §6.8 when the in-flight request resolves. Never lost.
  (Step reopening per §6.5 if already COMPLETED or CANCELLED
   (round 12); never if overpay-latched — §6.5 latch guard.)
required_amount decreases → attempt auto-cancel of un-posted requests
  (§6.4); posted/in-flight requests are never auto-amended
```

### 6.4 Auto-cancellation of un-posted requests

Cancellation is an ordinary CAS reusing the standard rules (row-count
gating §4, reservation release §3, stale-worker fencing):

```text
UPDATE payment_request
   SET outcome = 'CANCELLED', ...
 WHERE id = ?
   AND outcome IS NULL
   AND stage IN ('ENRICH', 'POST')
   AND NOT (stage = 'POST' AND stage_state = 'CLAIMED')
   AND stage_state <> 'BLOCKED'
   AND submission_state = 'NOT_SUBMITTED'
```

Set semantics (normative): ENRICH·CLAIMED IS cancellable —
enrichment is read-only lookups, no external effect exists, and the
worker's later CAS hits row count 0 (stale-worker fencing). Only
POST·CLAIMED is untouchable (a POST may be mid-flight). BLOCKED rows
are NOT auto-cancellable — a row parked for human review exits only
via an ops decision (§20), never via an automated amendment.

- Row count 1 → cancelled; reservation released; re-derived (§4);
  §6.8 then creates the successor for the new amount if a positive
  shortfall remains (all-or-nothing per request — amounts are
  immutable).
- Row count 0 → the request is not releasable by automation. Branch
  on submission_state FIRST, stage second (the operative fact is
  money truth, not pipeline position):
    - MAYBE_SUBMITTED (ANY stage, any stage_state EXCEPT CLAIMED —
      including CONFIRM·READY and §9.2-downgraded rows): set
      stage_state = BLOCKED (AMENDMENT_PARKED) + alert instead of
      releasing a reservation whose money may have moved (§10.1).
      A CLAIMED row is never parked mid-flight: a live claim
      owns its row (§4.4/§11 — writing BLOCKED would NULL the claim
      fields per L6 and destroy the worker's fence, silently
      dropping its response). The park DEFERS to claim resolution;
      the stale-amount term of repost_permitted (§7.0) protects the
      row meanwhile, and the retry-guard or the resolver (§9.2)
      applies the park at the next non-CLAIMED evaluation. The parked row
      follows WAIT-THEN-DECIDE: the resolver keeps querying
      (§9.5), feed/query evidence settles it, and §9.3 escalation
      (maybe_since clock) brings ops in. The §9.2 downgrade does NOT
      auto-fire for it — repost_permitted (§7.0) is false while the
      amount is stale — so no park ⇄ un-park cycle exists.
    - SUBMITTED: money genuinely engaged — leave alone; overpay
      evaluation on confirmation handles the outcome; recalling an
      in-flight/settled payment is a payment-platform operation
      outside this system (§19.2 family).
    - NOT_SUBMITTED and yet unreleasable: it is POST·CLAIMED (a POST
      may be mid-flight; the retry-guard below re-checks after the
      claim resolves) or BLOCKED (ops owns it; the amendment simply
      leaves the new required_amount applied).

The cancellable set is strictly NOT_SUBMITTED (§7.1: provably no
executable payment at the engine — which is why a POST · RETRY_WAIT
row from a synchronous business reject qualifies despite the POST
having been attempted).

**Retry-guard rule (mandatory):** before re-POSTing a RETRY_WAIT
request at stage POST, the retry worker shall re-validate the request
amount against the current shortfall under the obligation lock — this
is the amount-staleness term of repost_permitted (§7.0). If stale
(e.g. required_amount dropped while the cancel lost the race to the
posting claim), branch on submission_state:

- NOT_SUBMITTED → cancel instead of retrying (release is legal;
  §6.8 creates the right-sized successor).
- MAYBE_SUBMITTED → park BLOCKED (AMENDMENT_PARKED): cancel is
  forbidden (§10.1) and re-POSTing a stale amount is a knowing
  overpay. Wait-then-decide applies; the §9.2 downgrade will not
  un-park it (repost_permitted is false), so the park is stable, not
  a cycle.

This prevents re-posting a stale amount after an amendment.

### 6.5 Step reopening

If `required_amount` increases via a newer upstream message after the
step reached `COMPLETED` — or becomes positive again after the step
reached `CANCELLED` (round 12: a reappeared removed payment reopens
IDENTICALLY; §4.1's two terminal branches share one reopening rule):

```text
1. The obligation re-activates: shortfall recalculated under the
   obligation lock; §6.8 may create new requests — MAY: every §6.8
   gate applies, incl. live markers (round 12: a reappeared
   payment with provider_reject_count >= 2 gets NO automatic
   successor until the ops-only clear; removal never laundered
   the reject history).
2. Derived ui_step_status returns to IN_PROGRESS; reopened_at is set
   (derivation input, §4.3 — the card can indicate reopening).
3. Overpay evaluation re-runs against the updated amounts.
```

Latch guard (decided — latch wins): if `overpay_blocked` is latched,
an amendment still updates `required_amount` (ordering guard §6.7
permitting) but NO new request is created and automated payment never
resumes — alert `AMENDMENT_ON_LATCHED_SCOPE` for manual handling.
Rationale: a refund/return for the overpaid amount may already have
been requested and processed outside this system's visibility
(§19.2); any amount this system computed now could be wrong. §13's
ignore-forward rule takes precedence over reopening.

### 6.6 Validation failure before any request exists

If an upstream message fails validation before request creation and
the payment scope key + `ui_process_instance_id` are extractable, the
obligation row SHALL still be created as the durable anchor readers
query: `required_amount = NULL` (populated by the first valid
message), `ui_step_status = IN_PROGRESS`, active exception
`DATA_VALIDATION_FAILED, retryable = false`. The failing message's
ordering value is recorded as `validation_failed_ordering` (subject
to monotonic writes, §6.9) but does NOT advance `upstream_ordering`
(§6.7/§6.9). The completion predicate (§4.1) cannot complete such an
anchor. A later corrected message creates the first request against
this same obligation.

Snapshot note (§6.0): validation failure is a WHOLE-SNAPSHOT verdict
(including within-snapshot uniqueness violations). The anchor rule
above then applies PER extractable payment block — one anchor per
extractable scope tuple; blocks colliding on the same tuple
necessarily share one anchor row, which is fine: the anchor exists
to surface the problem, not to resolve it.

Trade-wide blast radius (deliberate, fail closed): because the
snapshot is ONE document, a whole-snapshot validation failure sets
the validation_failed marker (monotonic writes, §6.9; failing
ordering recorded; upstream_ordering NOT advanced) on ALL of the
trade's extractable scopes — existing obligations and anchors alike,
including payments whose own blocks were fine. Consequences, scoped
precisely: in-flight requests are UNTOUCHED (the marker is not a
state-machine input); terminal evidence still applies (§10.1);
retries of existing requests continue (the marker gates creation,
not retry); what stops is NEW request creation (§6.8's
validation-marker condition) on every scope KNOWN OR EXTRACTABLE
when the invalid document is processed (the ratified scope — see
the consistency semantics below; NOT literally "the whole trade":
scopes introduced later by VALID documents may proceed), until a
newer valid snapshot clears the markers by ordering. One corrupt
trade-mate delays its known siblings' NEW work — never their
in-flight work. Partial application of a document that violated
its own contract would be worse.

Consistency semantics (clarified 2026-07-11 after external review):
this is EVENTUAL TRADE-WIDE VALIDATION VISIBILITY, not an atomic
fail-closed gate — markers are applied per obligation in per-block
transactions (§6.1), and a crash mid-fan-out leaves siblings
unmarked until redelivery re-applies. This window is ACCEPTED and
harmless by construction: a request created on a not-yet-marked
sibling is created from the LAST VALID APPLIED state — exactly what
would have happened had the corrupt snapshot simply arrived later —
and the corrupt snapshot itself carries no applicable truth
(validation failure advances nothing). The SAME rationale covers
two further windows (made explicit 2026-07-17, review 4d5cb83 H1 —
ratified as the DEFINED behavior, not a gap):
(a) OUT-OF-ORDER NEW SCOPE: valid ordering 100 carries only A; an
    invalid ordering-200 document marks A (and every then-known
    scope); a VALID out-of-order ordering-150 document then
    introduces NEW scope B. B admits (150 > accepted 100), carries
    no marker, and MAY create a request — from valid-150's applied
    state, a VALID document. Had the corrupt 200 arrived after
    150 (a legal ordering), B's request would exist anyway; the
    invalid document says nothing about B, so blocking B protects
    nothing money-real. A itself stays marker-blocked until a
    valid document newer than 200 clears it BY ORDERING.
(b) ENUMERATION RACE — SCHEDULE-DEPENDENT BY DECISION (corrected
    2026-07-17, review 928341a H1: the earlier "converges
    identically" claim was FALSE — the invalid path holds no
    trade-row lock across its per-scope marking pass, so no
    deterministic order exists): a concurrent valid fan-out may
    create scope B before OR after the invalid path enumerates
    existing scopes. BOTH outcomes are RATIFIED as correct:
    - B created AFTER enumeration → B carries NO marker (window
      (a) exactly); its requests and successors proceed normally;
    - B created BEFORE enumeration → B IS in the enumerated set
      and receives the LIVE marker: its already-created request is
      UNTOUCHED (markers never alter in-flight work), but
      SUCCESSOR creation on B is marker-blocked until a valid
      document newer than the failure ordering clears it — and the
      UI shows DATA_VALIDATION_FAILED on B meanwhile.
    Neither outcome moves money incorrectly; the difference is
    successor gating and display until the next valid message.
    Tests assert BOTH as allowed end states, never convergence.
    Observability (revised 2026-07-17, review 2b697fb M1 — a
    CANDIDATE report, deliberately NOT a classifier): OB-01's
    reconciliation emits
    VALID_SCOPE_CREATED_BELOW_KNOWN_VALIDATION_FAILURE_ORDERING
    candidates via an explicit join — for each obligation M with a
    LIVE validation_failed marker, flag sibling payment_request
    rows r (same business_id, different scope) where
    r.creating_ordering < M.validation_failed_ordering AND
    r.created_at > M.validation_failed_first_at (§2.1 — the
    chronology term excludes ordinary requests that predate the
    failure). Candidates require MANUAL review: persisted state
    CANNOT distinguish this intentional window from a
    missed-marker crash, and the marker fields carry no source
    discriminator (whole-snapshot vs enrichment vs engine
    invalid-data share them) — none is added, BY DECISION (schema
    churn for an observability aid). Not detectable online without
    a trade-level watermark; documented, accepted, monitored.
PRECISE SCOPE OF THE STOP, restated: what stops is new request
creation on scopes KNOWN OR EXTRACTABLE when the invalid document
is processed; scopes introduced afterwards by VALID documents
create from valid state. Monotonic ordering-tagged
marker writes (§6.9) make re-application and interleaving with a
newer valid snapshot converge. No trade-level lock, gate, or
trade-level validation fence exists, BY DECISION (re-affirmed
2026-07-17): the atomicity it would buy protects nothing
money-real, and a fence would DEFER VALID PAYMENTS (B above)
to enforce a promise about a document that carries no truth.

If the message is too malformed to identify the scope, route it to the
dead-letter/ops-alert path. This blind spot is accepted and monitored.

Recommended improvement (worth implementing, NOT a hard requirement —
tracked as a §18 tech-lead item): key-only anchoring. The upstream
topic is keyed by business_id (§6.0, §16.2), and the Kafka key
deserializes independently of the body — a poison-pill body usually
arrives with a readable key. Tiered handling:

```text
1. Body parseable → normal processing / normal anchor (above).
2. Body unparseable, key readable:
   - No obligation exists for the business_id → create the anchor
     from the KEY alone (business_id set; other scope fields NULL),
     IN_PROGRESS + DATA_VALIDATION_FAILED. The card then shows a
     problem instead of a healthy NOT_STARTED, and the DLT alert
     carries the business_id so the platform and business
     investigations meet. Multi-payment note: a key-only anchor is
     a TRADE-LEVEL display row, never a payment scope — under the
     snapshot model (§1) it cannot be "populated into" any one
     payment. When the first valid snapshot for that business_id
     applies (creating the real per-payment obligations), the
     key-only anchor row is DELETED in the same processing — it is
     a pure placeholder (no money, no requests, no applied
     ordering; the failure record lives in the DLT and logs), and
     it existed only to surface the problem, which is now over.
     This is the single sanctioned obligation-row deletion in the
     system, possible only because the row can never have held
     value.
   - An obligation already exists → touch NO markers (an unparseable
     body has no readable ordering value and cannot be proven
     non-stale, §6.9); DLT + alert carrying the business_id.
3. Key also unreadable → the accepted DLT blind spot, now shrunk to
   key corruption only.
```

### 6.7 Message ordering and staleness guard

Upstream delivery is at-least-once and may be out of order. Duplicate
identical messages are naturally absorbed (the second sees shortfall
0). Stale messages are not: a redelivered older message must never
regress `required_amount`.

```text
Failure prevented: amendment raises required 100 → 120 and a request
for the extra 20 is created; the ORIGINAL message (required = 100)
arrives late → required regresses → negative shortfall → auto-cancel
(§6.4) silently kills the legitimate 20-request → underpayment.
```

Guard:

```text
- Ordering source: the message business timestamp TODAY; upstream
  will add an explicit sequence/version to the Kafka message in the
  FUTURE (§18 upstream ask 1). The comparison is a single pluggable
  point in code so the cutover requires no logic change.
- The obligation persists the last-applied ordering value
  (upstream_ordering); the TRADE persists the last-ADMITTED ordering,
  storage id, and canonical digest (trade_snapshot_state, §2.4 —
  round 5). The §6.1 admission gate consults the trade watermark
  FIRST; per-obligation watermarks remain the per-block
  convergence/re-run guard.
- A message mutates required_amount only if its ordering value is
  strictly newer than the stored one. Otherwise it is ignored and
  counted (stale-message metric; alert on unusual volume).
- Timestamp caveat (accepted until the version exists): clock skew or
  equal timestamps can misorder. Tie handling is payload-aware
  (payload equality per §6.0):
    tie + IDENTICAL payload  → silent drop (a true redelivery).
    tie + DIFFERING payload  → AMENDMENT_TIE_CONFLICT alert for
                               manual application — NEVER a silent
                               drop. Two genuine amendments can share
                               a timestamp, and "upstream resends" is
                               not a recovery for ties: the resend
                               carries the identical timestamp and
                               would be rejected forever.
    Snapshot note: equality is evaluated over the WHOLE snapshot
    (§6.0), concretely as digest-vs-stored-digest at the §6.1
    admission gate (trade_snapshot_state.last_payload_digest, §2.4
    — round 5: this is what makes equality INCLUDING the trade
    reference evaluable against applied state). Two tying snapshots
    differing in only one payment block therefore raise the
    tie-conflict for the snapshot as a whole — deliberately
    conservative; manual application resolves all blocks together.
    Executability requirement (REVISED again 2026-07-11 round 3 —
    the alert-recorded ordering is now advisory, never an input):
    the tie-conflict record — the alert and its §14 log line —
    carries IDENTIFIERS ONLY: business_id, the tied ordering value,
    the incoming snapshot's XML STORAGE ID (§6.0 transport note),
    and a per-block diff summary with MASKED accounts (enough for a
    human to adjudicate, never enough to execute). The payload
    stays where it already durably lives: the XML store. Resolution
    is the §20-10 REPROCESS-SNAPSHOT operation, and its safety is
    SERVER-VERIFIED, not attested: the operation takes the XML
    STORAGE ID ALONE (no caller-supplied ordering), fetches the
    document, and RECOMPUTES the tie condition itself at execution
    time — at the §6.1 ADMISSION gate (round 5): the ≥ relaxation
    applies iff the FETCHED document's own ordering value equals
    trade_snapshot_state.last_accepted_ordering AND its canonical
    digest differs from the stored one (which IS the definition of
    the tie); per block, the §20-10 rules then decide application
    against each obligation's own watermark. A fabricated or
    non-tying input therefore cannot invoke the relaxation; a
    re-run after apply finds digest equality at admission and
    no-ops (single use by construction); every money guard (§6.4
    retry-guard, §6.5 latch, §6.8 marker conditions, I6) applies
    unchanged. No
    payload is ever a parameter, a log field, or a new store, and
    no durable conflict record is needed — the store + the
    obligation row ARE the evidence.
- Regressing required_amount remains the non-recoverable direction;
  everything above fails toward alerts and manual application.
```

### 6.8 Standing shortfall re-evaluation (single request-creation point)

Request creation is a standing consequence of state, never a one-shot
side effect of whichever flow happened to be executing. There is
exactly ONE creation point in the system: under the obligation lock,
after every triggering mutation (trigger inventory below),
re-evaluate:

```text
if   shortfall > 0                        (I5)
AND  no active request exists             (I6 free)
AND  overpay_blocked = false              (§6.5 latch guard)
AND  validation_failed not live           (§4.1)
AND  provider_rejected not live           (§6.9; cleared by a newer
                                           valid message on the first
                                           reject, ops-only from the
                                           second — §2.1, §19.3)
AND  the successor policy permits         (below)
then create the next request (reservation +committed, §3;
     next_request_seq++, deterministic identity §5.1;
     creating_ordering := upstream_ordering)
```

Trigger inventory (normative):

```text
T1  an upstream message is APPLIED (upstream_ordering advanced —
    whether or not required_amount changed: an ordering advance can
    flip a marker not-live, which is exactly when a blocked scope's
    successor becomes creatable)
T2  the active request becomes terminal (outcome set)
T3  a marker's liveness changes for any other reason
    (monotonic marker write, §6.9)
T4  an ops marker clear (§19.3) or ops retry decision (§20)
```

Successor policy — how the DEPARTING request's outcome gates
auto-creation (ordering-aware, not unconditional):

```text
CANCELLED / SUPERSEDED  → auto-create PERMITTED. No engine attempt
                          failed; this is the deferred-amendment and
                          released-reservation case.
EXECUTED                → auto-create PERMITTED if a shortfall
                          remains (required_amount increased while
                          the request was in flight).
REJECTED                → auto-create PERMITTED iff
                          upstream_ordering is STRICTLY NEWER than
                          the rejected request's creating_ordering
                          (the corrected upstream message §7.3 names
                          has already arrived — it raced ahead)
                          AND provider_reject_count < 2
                          AND no marker is live (conditions above).
                          Otherwise NO auto-create: the live marker
                          carries the memory (PROVIDER_REJECTED /
                          DATA_VALIDATION_FAILED exception + §15
                          alert) until a newer message or ops (§19.3)
                          clears it. Blind re-pay of a rejected
                          payment is bounded to one ordering-newer
                          attempt before ops must intervene.
```

Consequences:

- An amendment that increases `required_amount` while a request is in
  flight is DEFERRED, not lost: message processing simply applies the
  new amount, and this re-evaluation creates the successor when the
  in-flight request resolves. The I6 conflict cannot occur by
  construction.
- The correction-races-enrichment-reject case recovers by
  construction: the reject's marker is tagged with the OLD
  creating_ordering, the applied correction is newer, so the marker
  is not live and the REJECTED policy's ordering test passes.
- Upstream message processing (§6.1) is just: validate → apply
  amounts/ordering → run this re-evaluation. There is no second
  request-creation code path to keep consistent.
- Deferral latency: the amendment delta is paid after the in-flight
  request resolves (typically minutes). PO sign-off tracked in §18.

### 6.9 Ordering-guard inventory

Every mutable input has exactly one staleness guard — covering READS
and WRITES; this table is normative and reviewed whenever a new
stored input is added:

```text
required_amount     — mutates only if message ordering is strictly
                      newer than upstream_ordering (§6.7)
validation_failed   — WRITE: monotonic — overwritten only by a
                      strictly newer ordering value; stale writes
                      dropped + counted (a stale replay must not
                      overwrite a LIVE marker and flip a blocked
                      scope to COMPLETED).
                      READ: LIVE only while marker ordering >=
                      upstream_ordering; a failed message never
                      advances upstream_ordering, so a stale replay
                      that fails validation cannot poison a completed
                      scope, and a correction that raced ahead of an
                      enrichment reject makes the late-set marker
                      not-live (recovered by §6.8)
provider_rejected   — same WRITE and READ rules; additionally
                      clearable only by ops from the second reject
                      onward (provider_reject_count)
request dimensions  — stage monotonic (one sanctioned exception,
                      §9.2); submission_state moves only on §9.4
                      evidence; outcome write-once; evidence rules
                      §4.4 + legality matrix §10.3: the state-side
                      equivalent of the ordering guard
committed/confirmed — move only on row-count-1 of the above (§3)
overpay_blocked     — DELIBERATELY un-gated: a latch, never
                      auto-cleared (§13)
provider_reject_count — monotonic counter; no guard required
blocked_reason      — set/cleared only together with stage_state
                      BLOCKED transitions (L8, §10.3)
ops_annotation      — free-text, operator-owned, last-write-wins;
                      display-only (never a derivation condition for
                      money or completion), so no ordering guard
reopened_at, next_request_seq, creating_ordering, provider_reference
                    — monotonic or set-once; no guard required
```

------

## 7. POST Execution and Failure Classification

### 7.0 Instruction assembly (fresh per attempt) and repost_permitted

Where the POST payload comes from — normative (this was previously
unspecified):

- The request row stores ground truth only for what it OWNS: the
  amount (immutable, §6.3), the scope, and the deterministic identity
  (§5.1). Party and account data (agent bank, account numbers) are
  NOT request columns.
- The instruction is assembled FRESH on EVERY attempt — first POST,
  retry, §9.2 downgrade re-POST, DR replay — from enrichment lookups
  (trade reference → trade store + reference data). The newest
  upstream data is ALWAYS the ground truth: a party-information
  update between attempts is picked up automatically. There is no
  payload freeze. SOURCE of the trade reference (made normative
  2026-07-11 round 5, PO-confirmed): every attempt re-reads the
  trade's MOST RECENT admitted snapshot — fetched by
  trade_snapshot_state.last_xml_storage_id (§2.4) — and re-does ALL
  enrichment steps against it (account mappings and party addresses
  change; nothing about the instruction is cached on payment rows).
  The trade reference is deliberately NOT an obligation column.
  GREENFIELD (round 10 — PO fact, §2.4): this flow starts with no
  pre-existing trades, so every obligation's trade row was created
  by an admitted message with the pointer populated — a NULL
  pointer is unreachable and the former transitional/claim-gate
  ladder is REMOVED (git history, 9a53c75).
- The claim transaction persists the identity (§5.1, first claim) and
  the hash of the assembled instruction (last_sent_hash, §2.2,
  every claim) BEFORE the HTTP call.
- Safety comes from the KEY, not from payload immutability (this is
  the original §5.1 stance): the engine's contract — elevated to a
  LOAD-BEARING contract fact, §18 tech-lead 4 — is that a known key
  with a DIFFERENT payload is NEVER executed (rejected or deduped,
  never treated as a new payment). Consequences of a re-assembled
  same-key re-POST:
    · original never arrived → executes with UP-TO-DATE details and
      the original amount — the desired outcome;
    · original arrived → the engine rejects the collision, which
      ANSWERS the MAYBE question (the payment exists at the engine):
      set divergent_payload_at (no further re-POSTs — futile), stay
      MAYBE, the resolver query recovers the outcome. EXPECTED
      divergence (the attempt's divergence_expected flag, recorded
      at claim time §2.2 — the fresh assembly differed from the
      prior attempt's hash: we knowingly carried newer details) is
      evidence, not an anomaly: no park, no CRITICAL. ANOMALOUS
      divergence (divergence_expected false — a DR replay mismatch,
      or the engine disagrees with an unchanged payload) → BLOCKED
      (ENGINE_INCONSISTENCY) + CRITICAL, as §7.2 always specified.
- A details update therefore reaches the engine iff the payment has
  not actually executed; an executed payment paid the details current
  at its execution — correcting THAT is a platform-side recall
  (§19.2 family), like any executed payment.

repost_permitted — the single normative re-POST gate:

```text
repost_permitted(request) =
      divergent_payload_at IS NULL      (stored, write-once — §7.2)
  AND NOT (request amount stale against the current shortfall
           AND submission_state = MAYBE_SUBMITTED)
                                        (derived — §6.4 retry-guard /
                                         wait-then-decide)
  AND the posting freeze is OFF         (Hazelcast — §16.1)
  AND outcome IS NULL                   (terminal rows: never)

(The former cutoff term was RETIRED 2026-07-11 — PO calendar
answer, §7.4: the engine owns its calendar, initiation is valid at
any time; a late submission is the engine's ordinary business
response, classified per CA-1. The §18-1(c) TTL contingency may
ADD a key-age term here — recorded as a named design decision by
the design owner BEFORE RC-03 implements it, never by a card.)
```

Checked at BOTH ends (defense in depth):

- by every writer that routes an existing key toward POST·READY or
  POST·RETRY_WAIT — the §9.2 downgrade and the ops re-POST action
  fire ONLY if the re-POST they enable is permitted. A row is never
  un-parked for an action the next gate would forbid — this kills
  the park ⇄ un-park livelock class structurally, not case by case.
- by the posting claim itself (§11): the claim CAS carries
  `divergent_payload_at IS NULL` and the claim logic re-checks the
  derived terms before launch. A laundered blocked_reason or a
  future writer bug hits row count 0, never the wire.

Ops override: a dual-control ops action may override ONLY the
amount-staleness term (the conscious stale-amount re-POST, §9.3).
NOTHING overrides divergent_payload_at or a terminal outcome.

The safety of every same-key re-POST rests on the §1 ASSUMED
contract facts: the design assumes the collision contract
works and carries no runtime gate for it — the §18 item-1 sandbox
test is the proof, and it blocks go-live, not runtime behavior.

blocked_reason plays no part in this predicate — §10.1: no rule may
key on blocked_reason.

The classifier output is a closed taxonomy keyed on cause, mapping to
`(exception_category, exception_code, retryable, severity,
submission_state, target dimensions)`. The actual engine
error-code-by-code table is a required companion artifact with a
named owner (§16.6).

### 7.1 Submission-state definitions

Normative — §10.1 release rights hang on these:

```text
NOT_SUBMITTED   := provably NO payment exists at the engine that
                   could execute. True for connect-level failures
                   (the request never arrived) AND for synchronous
                   definitive/business rejections (it arrived and was
                   provably not executed). "Never left the system" is
                   NOT the criterion — "cannot execute" is.
MAYBE_SUBMITTED := the engine may hold an executable payment.
SUBMITTED       := the engine acknowledged acceptance.

Working assumption (pending payment-team confirmation, §18): after a
synchronous business rejection, a same-key re-POST RE-EXECUTES — the
engine does not replay the cached rejection. The retry design
depends on this.
```

### 7.2 POST-failure classification

Target states in factored terms (request is at POST · CLAIMED when
the POST is made):

```text
Connect timeout / connection refused
  → request never left the system
  → NOT_SUBMITTED → POST · RETRY_WAIT (safe to resubmit)

Read timeout / connection reset after write / crash mid-call
  → engine may have received and executed
  → MAYBE_SUBMITTED → CONFIRM · READY (never resubmit blindly;
    resolver owns it, §9)

HTTP 200 with error payload in body
  → classify from the body, never the status line alone

DUPLICATE_REQUEST rejection on the first known attempt
  → a lost earlier attempt got through
  → MAYBE_SUBMITTED → CONFIRM · READY + status query; never resubmit
    with a fresh key; the response's UETR is NOT persisted (§5 —
    it names a submission under which nothing executes)

Known idempotency key with a DIFFERENT payload rejected
  → the engine already HOLDS this key; its content is the truth.
    The response's UETR is NOT persisted (§5).
    divergent_payload_at is set in the SAME transaction (write-once,
    §2.2) → repost_permitted (§7.0) is false forever, independent of
    whatever later happens to blocked_reason. Then branch on
    the attempt's divergence_expected flag (§2.2 — recorded in the
    claim transaction, BEFORE the hash overwrite: comparing at
    response time is impossible, the prior attempt's hash is
    already gone):
    · divergence_expected = TRUE (we knowingly re-assembled newer
      details — §7.0 fresh-assembly rule): this is EVIDENCE the
      original arrived, not an anomaly → MAYBE_SUBMITTED →
      CONFIRM · READY + status query; the resolver recovers the
      outcome. No park, no CRITICAL.
    · divergence_expected = FALSE (post-restore replay divergence
      §5.1 — a replay-recreated row has no prior hash — or the
      engine disagrees with an unchanged payload)
      → MAYBE_SUBMITTED → BLOCKED (ENGINE_INCONSISTENCY) + status
      query + CRITICAL; never overwrite, never retry

Unmapped / unclassifiable mid-call failure
  → fail closed: MAYBE_SUBMITTED → CONFIRM · READY

Unmapped engine error code
  → fail closed: MAYBE_SUBMITTED → BLOCKED (UNMAPPED_CODE) + alert
    (never "assume retryable")
```

Engine business rejections (synchronous — NOT_SUBMITTED per §7.1):

```text
Insufficient balance → POST · RETRY_WAIT (policy-driven retry)
                       or BLOCKED (OPS_PARKED) if manual handling
                       is required
Invalid data        → outcome = REJECTED; reservation released;
                       validation_failed marker set (ordering-tagged,
                       monotonic write, §6.9); recovery via a
                       corrected upstream message (§6.8)
Other definitive
synchronous reject  → outcome = REJECTED; reservation released;
                       provider_rejected marker set (marker totality —
                       EVERY REJECTED outcome sets exactly one
                       marker)
```

### 7.3 Enrichment outcome classification

```text
Transient failure (timeout, 5xx, connect)  → ENRICH · RETRY_WAIT
Definitive invalid-data result             → outcome = REJECTED
    (reservation released; validation_failed marker set, tagged with
     creating_ordering (§6.9); recovers via a corrected upstream
     message — §6.8 successor policy)
Unmapped / unclassifiable result           → ENRICH · BLOCKED
    (UNMAPPED_CODE, fail closed) + alert
Retry exhaustion                           → ENRICH · BLOCKED
    (RETRY_EXHAUSTED)

All enrichment-stage requests are NOT_SUBMITTED — no money has moved,
so terminal-negative outcomes are unrestricted here (§10.1).
```

### 7.4 Retry policy

Retry exhaustion (bounds decided 2026-07-11; REVISED same day by
the PO's calendar answer — the PAYMENT ENGINE owns its own cutoff
calendar, this system may initiate at ANY time, so the local cutoff
ceased to be a rule input: MAX ATTEMPTS is THE retry limit; the
independent wall-clock retry deadline was already REMOVED — its
§16.1 suspension requirement had no durable implementation): when a
retryable failure hits its max attempts, the request goes
stage_state = BLOCKED (RETRY_EXHAUSTED); the derived exception
becomes non-retryable with `manual_action_required = true`.
Suspension is STRUCTURAL: a gated or frozen scanner makes zero
attempts, so the attempt budget cannot burn during an outage. A
submission the engine deems late under ITS calendar comes back as
an ordinary engine response and is classified per CA-1 — no local
calendar, no local cutoff gate, no special handling. CUTOFF_EXPIRED
remains in the blocked_reason enum as RESERVED/never produced (the
RESERVED-column retry_deadline_at precedent — no schema churn); the retired local
calendar machinery is retained only in git history.

Retry policy is explicit per error class (base interval, multiplier,
max attempts), lives in externalized config (§16.6); retry
*state* (`attempt_count`, `next_retry_at`, `last_error_code`) lives
on the request row (`retry_deadline_at` remains as a RESERVED,
unused column — kept to avoid schema churn after the 2026-07-11
bounds decision). Exactly one retry owner
per (operation, error class) — the DB scanner; no stacked in-process
retries on the payment POST (§16.1).

The trust-age downgrade (§9.2) has its OWN policy class:
`next_retry_at = now` (trust-age already provided the waiting);
`attempt_count` RESETS on downgrade — a downgrade starts a new
episode under a new error class, the old counter belonged to the
original failure class (`post_attempt_seq` is NOT reset — it is
monotonic journal identity, §2.2/§14.1, never a retry budget);
small max attempts (suggest 2–3, config
§16.6) — MAX ATTEMPTS is the ONLY bound (round 10: no cutoff
pre-check exists, the engine owns its calendar; no wall-clock
deadline exists). Exhaustion
→ BLOCKED (RETRY_EXHAUSTED) with submission_state still
MAYBE_SUBMITTED: the row stays in resolver scope (§9.5) and the
maybe_since escalation clock (§9.3) keeps running, so exhaustion
cannot strand it. Every attempt stamps `last_post_attempt_at` (§2.2),
which restarts the §9.2 trust-age clock per attempt.

------

## 8. Inbound Flow 2: Payment Platform Status Feed

Consumption transaction, in order:

```text
1. INSERT INTO processed_inbound_event
   → duplicate key: already fully processed, return (no locks taken)
2. Resolve request (UETR primary; provider_reference fallback under
   the fail-closed rule below)
   → no match: log (event_id, UETR, status) + count, ack, drop
     (recovery is §9)
3. Obligation lock → evidence-guarded CAS (§4.4, §10.1, §10.3) →
   amounts on row-count 1 (§3) → re-derive (§4)
4. Commit, then ack Kafka
```

Layering — complementary, never alternatives:

```text
Inbox table     → stops IDENTICAL redeliveries (same event_id) cheaply,
                  before any lock. Performance/operational layer.
Evidence rules  → stop SEMANTICALLY STALE events (different event_id,
                  older meaning; re-keyed duplicates). Correctness
                  layer. The money is protected here and only here.
```

Rules:

- Unmatched UETR: log (event_id, UETR, status) + metric + ack; no
  durable record, no replay (decided). EXCEPTION: while a §5.2
  replay window is being consumed, an unmatched event is a CRITICAL
  alert — see §5.2 step 3. If the event was real (the
  routine race where the feed beats the executor's commit), the
  request simply remains unconfirmed and the §9 scheduled sweep
  recovers the outcome from the engine's API. Accepted trade-off:
  confirmation of a missed event arrives at sweep latency, not
  immediately. Foreign or unexplained events are investigated in the
  payment platform, which holds the authoritative record.
- provider_reference fallback — fail closed: the reference's
  uniqueness scope and lifetime are UNCONFIRMED (§18 tech-lead ask);
  engine references are often unique only per day, batch, or rail,
  and are reused. Until confirmed globally unique, the fallback
  applies ONLY when exactly ONE ACTIVE request matches, additionally
  guarded by amount equality and a recency window (config §16.6);
  zero or multiple matches → the unmatched path (log + metric — the
  §9 sweep recovers the truth by key). Rationale: a mis-matched
  settlement completes a scope that was never paid, whose real
  payment then re-pays via §6.8 — a double-pay; and a mis-matched
  REJECT has no amount guard at all. Index decision (2026-07-11, PO
  review — supersedes the earlier UNIQUE-index sentence): the field
  carries a NON-UNIQUE lookup index until TL-12 confirms the
  uniqueness scope in writing; reuse is made loud by a METRIC (the
  fallback lookup finding >1 candidate → counted + alerted), never
  by a constraint — a UNIQUE index would fail OUR response-persistence
  transaction on a legitimate reuse AFTER the engine already accepted
  the payment, converting acceptances into manufactured MAYBE rows.
  A UNIQUE (or compound-scoped unique) index may be added only after
  written TL-12 confirmation, by explicit decision.
- The evidence rules shall never be weakened because the inbox
  exists; a re-keyed duplicate passes the inbox and must die on the
  CAS row count.
- Anomaly disambiguation: a KNOWN event_id is a benign redelivery
  (silent skip). A NEW event_id whose CAS affects zero rows against a
  TERMINAL request is a real anomaly — a settlement arriving for an
  outcome = REJECTED/CANCELLED request is a CRITICAL alert, not a log
  line (this is also the §5.2 replay-divergence tripwire).
- Event amount must equal the request amount. CONTRACT FACT
  (confirmed; RE-CONFIRMED by the PO, including the
  cross-corridor/fee-deduction question): the payment engine settles
  all-or-nothing — partial settlement and fee-deducted amounts are
  impossible by contract.
  A mismatched amount is therefore evidence of a DEFECT (ours or the
  engine's), not a business state: → stage_state = BLOCKED
  (AMOUNT_MISMATCH) + CRITICAL alert; `confirmed_amount` does not
  move. The park also sets submission_state = SUBMITTED (terminal evidence applies to ANY active row, so a mismatched
  settlement can land on a MAYBE row — a settlement event proves
  the engine holds an executable payment; tightening is always safe
  §9.4, and it takes the row off the MAYBE clocks/queues). Resolution is external: the engine emits a corrected event
  (which satisfies the equality guard and completes normally), or the
  dispute concludes in the payment platform and the scope is
  reconciled there — local counters corrected only via the future
  manual-adjustment operation (§19.2 family). No
  settle-at-actual-amount operation exists or is planned; I2's
  definition stands.
- An authoritative engine negative (feed reject, resolver REJECTED)
  sets outcome = REJECTED and the `provider_rejected` marker (§2.1)
  in the same transaction (marker totality) — a payment the
  business requested is now not happening, and it must surface as an
  exception and an alert, never as a silent IN_PROGRESS scope.
- A post-settlement return/refund-style event for an EXECUTED
  request: no state change — this system has no return status and no
  visibility into money flowing back (see §19.2). Log + CRITICAL
  alert + ack.
- Concurrent in-flight duplicates (rebalance mid-poll): the second
  transaction's inbox insert blocks on the first's row lock, then
  fails with duplicate key after the first commits. Covered by a test,
  not assumed.
- The sync POST response and the async feed event for the same payment
  may race on different threads; both converge on the same
  evidence-guarded CAS (§10.1 terminal-evidence + mirror rules).

------

## 9. Status-Query Resolution (Ambiguous and Missed Outcomes)

An ambiguous outcome (submission_state = MAYBE_SUBMITTED) is a
first-class situation with a designed exit path. Its "retry" is a
**status query** by idempotency key (persisted before POST, §5) —
or by UETR where a POST response delivered one — never a re-POST.

### 9.1 Query outcomes

```text
EXECUTED      → apply as authoritative settlement through the normal
                evidence-guarded path (§4.4; amount equality)
REJECTED      → outcome = REJECTED; reservation released (§3);
                provider_rejected marker set (§8)
NOT_FOUND     → meaning depends on age — trust-age rule (§9.2);
                never taken at face value; applies to MAYBE_SUBMITTED
                AND SUBMITTED rows alike
INDETERMINATE → reschedule; ask again later
ACCEPTED /
in-flight     → intermediate evidence per §4.4: no-op on a CLAIMED
                row; otherwise sets submission_state = SUBMITTED
                (a tightening, which §9.4 permits), stage → CONFIRM,
                stage_state → READY (BLOCKED preserved); reschedule
```

Query API failure / timeout: treated as INDETERMINATE — reschedule
with backoff. Escalation clocks keep running during a query-API
outage: fail toward a human, never toward silence.

### 9.2 NOT_FOUND semantics (trust-age rule)

A NOT_FOUND answer has four real-world causes: genuinely never
received; received but not yet visible to the engine's query store
(async ingest lag); query lookback-window expiry; key mapping. Only
the first is safe to act on, and the query answer cannot distinguish
them. Therefore (keyed on submission_state, NEVER on
status/stage):

```text
- Before the relevant attempt is older than NOT_FOUND_TRUST_AGE
  (config; set from the engine's max ingest lag + margin, §18):
  NOT_FOUND = INDETERMINATE. Reschedule, nothing else.
  For MAYBE_SUBMITTED rows the age is measured from
  last_post_attempt_at (§2.2 — each attempt restarts this clock);
  for SUBMITTED rows, from submitted_at (§2.2). (both anchors
  are named columns; state_changed_at churns and must not be used.)
- MAYBE_SUBMITTED row (ANY stage/stage_state — a park does not by
  itself remove the exit), NOT_FOUND after trust-age, AND
  repost_permitted (§7.0) — the downgrade fires ONLY if the re-POST
  it enables is permitted; a row is never un-parked for an action
  the next gate would forbid:
  → stage = POST, stage_state = RETRY_WAIT (the ONE sanctioned
    backward stage move — same-key re-POST, assembled FRESH per §7.0:
    newest details, the request's own immutable amount);
    next_retry_at = now, attempt_count reset (post_attempt_seq
    unaffected — monotonic §14.1 identity), under the §7.4
    downgrade policy class (L7 is satisfied by this explicit write);
    submission_state REMAINS MAYBE_SUBMITTED; blocked_reason cleared.
  Rows failing repost_permitted — divergent payload (§7.2 key
  collision) or a stale amount on a MAYBE
  row (amendment-parked, §6.4) — are NOT downgraded: they resolve
  via status query, authoritative feed evidence, or ops
  (wait-then-decide), with §9.3 escalation on the maybe_since clock
  as the bounded human hand-off. When the failing term is amount
  staleness and the row is not already parked (a §6.4 park deferred
  by a live claim), the resolver applies the AMENDMENT_PARKED
  park itself — idempotent, non-CLAIMED rows only — so the deferred
  park always lands. (replaces the earlier reason-keyed
  ENGINE_INCONSISTENCY exception, which a blocked_reason overwrite
  could launder, and closes the park ⇄ downgrade livelocks.)
- SUBMITTED row, NOT_FOUND after trust-age (a single answer
  suffices — no consecutive-answer counting state exists,
  deliberately; a lag-caused false park is REVERSIBLE, the row
  remains in resolver scope §9.5 and the next successful query
  resolves it): the engine acknowledged a payment it now
  cannot find → CRITICAL engine-inconsistency anomaly → stage_state
  = BLOCKED (ENGINE_INCONSISTENCY). NEVER a downgrade — re-posting an
  acknowledged payment is forbidden. The row REMAINS in resolver
  scope (§9.5).
- The response to a downgrade's new POST is what settles
  submission_state: acceptance → SUBMITTED; synchronous definitive
  reject → NOT_SUBMITTED; DUPLICATE_REQUEST → MAYBE_SUBMITTED +
  query (§7.2: a hidden earlier attempt surfaced); timeout →
  MAYBE_SUBMITTED again.
- Lookback aging (REVISED round 10 — the local cutoff was retired
  with the PO's calendar answer, §7.4): a request that only looks
  NOT_FOUND because it aged past the engine's query lookback has
  NO local calendar guard anymore; its safety is carried by the §1
  assumed collision contract AT THE RETENTION EDGE — §18-1(c)
  proves TTL ≥ max row lifetime, or repost_permitted gains the
  named TTL term (decision hygiene, §18-1(c)) and such aged rows
  become ops-only.
```

Why the auto-downgrade exists at all (recorded): a query answer
cannot distinguish never-received from not-yet-visible, and during an
engine ingest outage EVERY in-flight payment answers NOT_FOUND —
without a self-healing path the entire population would land on ops.
The auto-downgrade is that self-heal, and its safety leans on the
engine collision contract — which the design ASSUMES (§1 assumed
contract facts): there is NO runtime gate, flag, or per-path
disable, and a lag-premature downgrade is harmless by the same
assumption (the engine dedupes or rejects the collision). The
assumption is proven, not policed: §18 BLOCKING item 1 (sandbox
test) gates go-live itself. NOT_FOUND-after-trust-age frequency is
measured in production (§18) to revisit auto vs ops-triggered with
data.

Accepted consequence: a genuinely-never-received request cannot be
auto-cancelled during its retry window (it is still MAYBE_SUBMITTED
until a fresh POST response says otherwise); an amendment-down
against it parks BLOCKED (AMENDMENT_PARKED) instead (§6.4) and
follows WAIT-THEN-DECIDE (superseding the earlier rule "the downgrade
still applies to the parked row" — that rule collided with the §6.4
retry-guard to form a park ⇄ un-park livelock in which the settling
re-POST was never actually issued). The parked row is not wedged:
the resolver keeps querying (§9.5), the feed can settle it at any
time, and §9.3 escalation (maybe_since clock) brings ops in with two
exits — the conscious dual-control stale-amount re-POST (§7.0
override) or a platform-side formal rejection of the UETR that flows
back as authoritative evidence (tech-lead ask 10, §18).

### 9.3 Escalation

If a MAYBE_SUBMITTED row is unresolved within a bounded age —
measured on maybe_since (§2.2), NEVER on state_changed_at, which
dimension churn resets — set stage_state = BLOCKED
(blocked_reason = ESCALATED, §2.2) with a CRITICAL alert, early
early enough for ops to act while the payment still matters
(age-based — the engine owns the cutoff calendar, §7.4). The STATE WRITE is
gated twice: only if escalated_at IS NULL (set it in the same
transaction — escalation fires ONCE per MAYBE episode; without this
gate, a §9.2 downgrade un-parking an ESCALATED row against an
already-elapsed maybe_since would be re-parked immediately, a
downgrade ⇄ escalate cycle racing the retry scanner), and only on
non-CLAIMED rows (a live claim owns its row, §4.4/§11 — the alert
still fires; the write defers to claim resolution). The scanner's
scope carries outcome IS NULL (frozen rows keep submission_state,
§10.2 clears maybe_since as belt-and-braces). Escalation changes
who is watching, not whether the question needs answering — and
therefore escalation of a row that is ALREADY BLOCKED for
another reason raises the same CRITICAL alert and rank-1 exception
WITHOUT touching the row: there is nothing an overwrite would add,
and it would churn state_changed_at and the ops queue metrics.
(blocked_reason carries no rules either way — §10.1.)

Tiered: a second, higher threshold on the same maybe_since clock
(config §16.6) re-pages / raises an incident, so an escalated row
cannot quietly live in the ops queue indefinitely.

Considered and REJECTED (recorded so it is not relitigated): a
materiality threshold under which a stale-amount MAYBE row would
auto-re-POST. The bounded middle already exists — a NOT-stale row
auto-downgrades (§9.2) and a stale one has the dual-control override —
and a materiality re-POST that executes lands confirmed > required:
the overpay latch. The manual toil is not avoided, only moved to
AFTER the money.

```text
- submission_state remains MAYBE_SUBMITTED; the resolver KEEPS
  querying (§9.5 — scope is submission-keyed, so BLOCKED does not
  remove the row), and the §9.2 downgrade remains available where
  repost_permitted (§7.0) passes.
- Terminal-negative outcomes (reject / supersede / cancel — manual
  or automated) are FORBIDDEN while submission_state is
  MAYBE_SUBMITTED or SUBMITTED, unless driven by an authoritative
  engine negative (feed reject or resolver REJECTED). Ops tooling
  must disable release actions for such rows; the offered actions
  are "resolve via engine status query", (after trust-age, where
  repost_permitted passes) the same-key re-POST downgrade, the
  dual-control stale-amount re-POST (§7.0 override — the ONLY
  overridable term), and requesting a platform-side formal
  rejection of the UETR (tech-lead ask 10, §18) so the negative
  arrives through the normal evidence path — and, ALWAYS AVAILABLE
  AT MVP (closing the permanent-wedge class): the dual-control
  APPLY-PLATFORM-VERIFIED-OUTCOME operation. Normative design:
  ops verifies the payment's true fate in the payment platform's
  own records (the platform holds the authoritative audit trail,
  §1), then executes the AUDITED VERIFIED-OUTCOME OPERATION — an
  authorized, enterprise-authenticated endpoint of the payment
  APPLICATION invoking the same shared transition service as the
  orchestrator (execution boundary decided 2026-07-11: Java
  application, never a PL/SQL reimplementation — a raw stored
  procedure cannot reuse the shared CAS/derivation helpers, check
  the Hazelcast freeze, emit §14/§15 telemetry, or verify
  enterprise identities; the §10.3 triggers remain the DB backstop;
  restricted role; spec = §16.6 artifact 8)
  with: request_id, the verified outcome (EXECUTED or REJECTED), a
  mandatory ticket/evidence reference (§20-8), and dual control
  enforced by the operation itself, not by convention. PROTOCOL
  (decided 2026-07-11 round 3; CANONICALIZED round 4 — an
  authenticated caller plus a second identity FIELD is not dual
  control, and execution inputs shall NEVER carry approver
  identities): THE MVP mechanism is the TWO-STEP APPROVAL WORKFLOW.
  (1) The initiator's authenticated session (enterprise
  access-management identity — unique, non-bypassable) records a
  pending approval BOUND to (request_id, intended outcome/action,
  parameter hash, ticket reference, environment, expiry, nonce —
  nonce UNIQUE); for reprocess-snapshot the binding additionally
  carries (business_id, xml_storage_id/version, and the CANONICAL
  BUSINESS-PAYLOAD DIGEST of the snapshot fetched and validated AT
  APPROVAL TIME — round 4: approval authorizes CONTENT, not an
  opaque id). (2) The second approver, in their OWN authenticated
  session, is shown the binding — including the digest and the
  masked diff, and (reprocess-snapshot, round 7) the notice that a
  newer LIVE snapshot admitted mid-execution SUPERSEDES the
  unapplied remainder (§6.1 block-level supersession) — and
  approves that exact record (approver ≠ initiator, verified from
  session identity). (3) EXECUTION takes
  ONE input: the approval_id. Initiator and approver identities are
  DERIVED from the trusted record, never from parameters. The
  approval-state machine is PENDING → APPROVED → CONSUMED (plus
  REJECTED / EXPIRED), each move a row-count-checked CAS on a
  version column. ATOMICITY (round 4; SCOPED round 5 — the round-4
  rule as written could not cover a multi-transaction operation):
  for SINGLE-TRANSITION operations (verified-outcome, retry/reject,
  supersede/close), the APPROVED → CONSUMED CAS and the privileged
  payment transition COMMIT IN THE SAME database transaction and
  session — any refusal or exception rolls back BOTH (a failed
  transition never burns an approval; a crash after the transition
  never leaves a replayable approval; two concurrent executors race
  on the CAS and exactly one wins). For REPROCESS-SNAPSHOT
  (multi-block — §20-10 applies each block in its OWN transaction)
  the rule is CONSUME-AT-START: execution re-fetches the snapshot,
  recomputes the canonical digest, and REFUSES on any mismatch with
  the approved digest BEFORE any consumption or lock (content
  changed behind an id is a HARD refusal + alert, never applied —
  and a refusal burns nothing); then the CONSUMED CAS commits ALONE,
  BEFORE the §6.1 admission/fan-out. Consequences (accepted,
  round 5): consumption precedes any money movement, so NO replay
  window exists and concurrent executors still race exactly one
  CAS; a crash mid-fan-out leaves the approval CONSUMED and the
  trade partially applied — the remedy is a NEW approval of the
  SAME document (same storage id, same digest; §20-10 per-block
  convergence applies only the remainder). Burning an approval on
  a crash of a rare-by-construction operation is the accepted
  cost: the failure direction demands MORE authorization, never
  less. Considered and REJECTED for MVP (recorded so it is not
  relitigated): a resumable APPROVED → EXECUTING → COMPLETED
  execution record with lease/fencing and a progress cursor —
  machinery disproportionate to the hazard; revisit post-MVP only
  if reprocess frequency proves material. COMPLETION EVIDENCE
  (round 6 — consume-at-start must never fail SILENTLY): the
  reprocess execution stamps the approval record (ops schema, not
  payment data) with completed_at + a per-block summary
  (applied / no-op / dropped / abandoned) in a small transaction
  after the last block; a §15 alert fires for any CONSUMED
  reprocess approval without completion evidence past a
  configured SLA (crash mid-fan-out OR correct newest-wins
  abandonment by a concurrent newer snapshot, §6.1 — both need
  eyes); the runbook: check the trade row; if the document is now
  stale, the abandonment was correct — annotate and close; else
  create a NEW approval of the same immutable document and let
  convergence apply the remainder. A crash between the last block
  and the stamp only causes a false alert; the runbook re-run
  no-ops and stamps.
  The pending-approval record lives in a small OPS-SCHEMA store —
  operational workflow state, explicitly OUTSIDE the §2 payment
  data model (the four §2 tables) and sanctioned as the
  ONE such store. A SIGNED enterprise approval assertion is an
  EXPLICITLY-GATED alternative only (it must carry the same
  binding fields incl. the digest, define issuer/keys/audience/
  expiry/skew, and still consume a durable UNIQUE nonce in the
  SAME transaction as the transition — adopt only by recorded
  decision; agent-facing cards specify the workflow, not the
  alternative). CA-9 carries the schema, state machine, and
  negative tests: parameter substitution, expired approval, replay
  of a consumed approval, identical identities, role revoked
  between approval and execution, digest mismatch, concurrent
  double-execution, mid-transaction failure (approval must survive
  unconsumed). The operation sets the §10.3
  evidence session flag — the release-guard trigger is passed
  LEGITIMATELY, never disabled — and applies the outcome through
  the SAME evidence-guarded CAS as feed evidence (§4.4):
    EXECUTED → outcome = EXECUTED, SUB = SUBMITTED, amount
    equality enforced, +confirmed (a platform amount that DIFFERS
    is not applicable here — that is the §8 AMOUNT_MISMATCH
    defect path);
    REJECTED / confirmed-never-executable → outcome = REJECTED,
    provider_rejected marker (L9 totality), reservation released.
  Refuses CLAIMED rows (a live claim owns its row) and terminal
  rows (L1). Every use raises a §15 alert; the §14 log line
  carries trigger_source = OPS_PLATFORM_VERIFIED + the ticket
  reference. This is the SINGLE sanctioned exception to §9.4: the
  evidence is authoritative platform records — only its transport
  is a human.
  Consequence: NO MAYBE row is ever permanently wedged at
  MVP. Even when repost_permitted is false forever, the key has
  aged past the query lookback (NOT_FOUND unfalsifiable), and
  TL-10 is unavailable, the escalation path ends in a resolvable
  state — the reservation is confirmed or released, the scope can
  complete (§4.1), and a released shortfall re-pays under a NEW
  key via §6.8 where its guards permit (§19.3 pattern). Guard note
  (clarified 2026-07-11): after a verified REJECTED the
  provider_rejected marker is LIVE and correctly BLOCKS an
  automatic successor — re-payment happens only via a strictly
  newer valid upstream message (§6.8 successor policy) or the
  future §19.3 clear. The un-wedging claim is about the RESERVATION
  and the scope's exits, never about automatic re-pay.
- Rationale: releasing the reservation re-opens the shortfall and
  pays again; if the original payment had in fact executed, its
  eventual confirmation arrives AFTER the money left twice.
```

The read model reports this state distinctly (rank 1, §4.2):

```text
active_exception_code = PAYMENT_OUTCOME_UNKNOWN
severity              = CRITICAL
retryable             = true (system still resolving)
```

Never reported as generic `SYSTEM_UNAVAILABLE`: "dependency down,
nothing sent" and "money may have moved, outcome unknown" are
operationally different events.

### 9.4 The release-rights invariant

```text
submission_state moves toward "safe to release" ONLY on evidence from
this system's own POST responses or an authoritative engine
REJECTED — NEVER from a status-query answer. (Single sanctioned
exception: the §9.3 apply-platform-verified-outcome operation —
platform-records-verified, dual-control, audited.) (Moves AWAY from
releasable — e.g. a query answer proving acceptance → SUBMITTED —
are permitted: tightening is always safe.) §10.1 release rights are
therefore immune to query-store inconsistency: a false NOT_FOUND can
cause at most a same-key re-POST, never a reservation release plus a
new-key successor. (that re-POST is harmless when
the engine HAS the key — it dedupes or rejects the collision (§7.0/
§7.2, the load-bearing engine contract), either way executing
nothing new; when the engine truly never received it, the re-POST
executes the freshly-assembled instruction (§7.0: newest details,
original amount) — the intended settle-the-truth behavior.)
```

Race safety: the resolver's answer and the status feed may both
arrive, in any order; both apply through the same evidence-guarded
CAS — whichever lands second affects zero rows and is ignored.

### 9.5 Resolver scope

Normative — keyed on submission_state and outcome, NEVER on stage,
stage_state, or how a row got where it is:

```text
- Every ACTIVE request (outcome IS NULL) with submission_state =
  MAYBE_SUBMITTED — any stage, any stage_state (including BLOCKED).
- Every ACTIVE request with submission_state = SUBMITTED older than
  the bounded confirmation age — any stage_state (including BLOCKED:
  an ENGINE_INCONSISTENCY row keeps being queried).
```

Sweep load shaping (normative): the sweep queries in BOUNDED,
prioritized batches — oldest first (round 10: no local cutoff
knowledge exists; the engine owns the calendar)
maybe_since — under a per-sweep query budget derived from the
engine's stated query-API rate limit (§18 tech-lead ask; that number
is as load-bearing as the ingest lag). Each row carries its own
next-query-at with backoff, so a 2-minute cadence over thousands of
post-outage MAYBE rows never becomes a thundering herd against an
engine that just recovered. A sweep that overruns its cadence emits
the §15 sweep-overrun metric and the next sweep starts only after it
finishes (never overlap). While the feed-lag metric exceeds the
confirmation age, the SUBMITTED branch damps — a delayed feed is
distinguishable from a lost feed via §16.2's lag metric; the MAYBE
branch never damps.

The sweep applies whatever final status the query returns through the
normal evidence-guarded path. This makes outcome recovery fully
independent of feed delivery: a notification that was missed (§8),
lost, or never sent is recovered by asking, keyed by the request's
persisted identity. The feed is a latency optimization; the
status-query API is the durable source of payment outcomes. Terminal
rows are never queried (outcome IS NULL term) and never surface
rank-1 exceptions (§4.2 considers active requests only).
History-based and stage-based scoping are explicitly rejected: they
silently exclude rows that reached a state by another path and break
every time a new path is added — the recurring lesson of four review
rounds.

------

## 10. Request State Model

The state is the tuple (stage, stage_state, submission_state,
outcome). This section is NORMATIVE: per-dimension rules (§10.2), the
legality matrix (§10.3), and the flow table (§10.5). The display
labels (§10.4) are for humans only.

### 10.0 State machine at a glance (diagram)

A request's state is the TUPLE of four independent columns. These
diagrams are illustrative; §10.1–§10.5 are the normative rules —
where a diagram and the legality matrix disagree, the matrix wins.

stage — forward-only, with ONE sanctioned backward move:

```text
   ENRICH ────────▶ POST ────────▶ CONFIRM
                      ▲                │
                      └────────────────┘
        §9.2 downgrade: same key, fresh assembly (§7.0),
        gated by repost_permitted — never fires while any
        repost_permitted term fails (wait-then-decide)
```

stage_state — the work cycle within any stage:

```text
              claim (lease, L6)         stage work succeeds
   READY ──────────▶ CLAIMED ──────────▶ next stage · READY
     ▲               │
     │               ├─ transient failure ─▶ RETRY_WAIT (L7)
     │               │                          │   ▲
     │               │            due → re-claim ┘   │ exhaustion
     │               │                              │ → BLOCKED
     │               └─ park / unmapped / escalate ─▶ BLOCKED
     │                                    (blocked_reason set, L8)
     └── exits from BLOCKED: ops action (§10.2), §9.2 downgrade,
         or late authoritative evidence. POST-bound exits pass
         repost_permitted (§7.0); terminal exits pass the release
         guard (§10.1). blocked_reason is descriptive only — no
         rule keys on it (§10.1).
```

submission_state — what the engine may hold (§7.1); moves toward
certainty, loosens only on own-POST evidence (§9.4):

```text
   NOT_SUBMITTED ── ambiguous POST ──▶ MAYBE_SUBMITTED ──▶ SUBMITTED
        ▲          (timeout, crash,        │     (acceptance, query-
        │           lease expiry,          │      proven, feed —
        │           DUPLICATE_REQUEST)     │      tightening, always
        │                                  │      safe)
        └── sync definitive reject of ────┘
            a re-POST (own-POST evidence)
   Never loosened by a status-query answer. Single sanctioned
   exception: the §9.3 apply-platform-verified-outcome operation.
```

outcome — write-once; setting it FREEZES the row (L1) and
normalizes stage_state/claim/retry fields (§10.2):

```text
   ∅ ──▶ EXECUTED    settlement / resolver / §9.3 operation;
                      +confirmed under amount equality
   ∅ ──▶ REJECTED    sync definitive / feed / resolver / §9.3;
                      exactly one marker set (L9); reservation released
   ∅ ──▶ CANCELLED   §6.4 auto-cancel (NOT_SUBMITTED only) / ops
   ∅ ──▶ SUPERSEDED  ops supersede/close (release guard §10.1)
```

Lifecycle map in display labels (§10.4 — labels are read-only
projections of the tuple; no rule keys on them):

```text
        §6.8 creates — ONE active request per obligation (I6)
                             │
                             ▼
  CREATED ─▶ ENRICHING ─▶ READY_TO_POST ─▶ POSTING ─┬▶ COMMITTED_WAITING_CONFIRMATION ─▶ COMPLETED
     │           │                                  │          (CONFIRM·READY·SUBMITTED)      (EXECUTED)
     │           ▼                                  ├▶ UNKNOWN (CONFIRM·READY·MAYBE)
     │   ENRICHMENT_RETRYABLE → (re-claim)          │     └─ resolver §9: query → settle;
     │   or REJECTED_FINAL (invalid data)           │        parked MAYBE waits-then-decides,
     │                                              │        escalates once per episode (§9.3)
     │                                              ├▶ POST_FAILED_RETRYABLE (POST·RETRY_WAIT)
     │                                              ├▶ REJECTED_FINAL (sync definitive reject)
     │                                              └▶ NEEDS_REVIEW (any stage · BLOCKED)
     └▶ CANCELLED / SUPERSEDED (auto-cancel §6.4; ops — release guard)
```

### 10.1 Global rules

```text
Release guard: a terminal-negative outcome (REJECTED / CANCELLED /
SUPERSEDED — automated or manual) is PERMITTED only when:
    submission_state = NOT_SUBMITTED
    OR the transition is driven by an authoritative engine negative
       (status-feed reject, resolver REJECTED)
    OR it is executed by the §9.3 apply-platform-verified-outcome
       operation (platform-records evidence, dual-control — the
       single sanctioned manual path; §9.4, §10.3).
Releasing a reservation whose money may have moved is forbidden (§9.4).
```

```text
blocked_reason rule (mirrors the §10.4 display-label rule, one
layer down): blocked_reason is DESCRIPTIVE ONLY — queue label, alert
routing, ops display. NO rule may key on it. Every load-bearing
re-POST exemption lives in repost_permitted (§7.0), on durable or
derived facts (divergent_payload_at, amount staleness,
outcome) — so overwriting or clearing a reason can never launder a
safety rule. Automation never overwrites the reason of an
already-BLOCKED row (§9.3: escalation alerts without touching parked
rows).
```

```text
Terminal-evidence rule: authoritative terminal evidence (feed
settlement/reject, resolver EXECUTED/REJECTED) applies to ANY ACTIVE
request — whatever its stage and stage_state — via the
evidence-guarded CAS, with the amount-equality guard on settlement
and the marker/money effects of §3/§8. Settlement evidence also sets
submission_state = SUBMITTED (L4).

State records evidence ARRIVAL order, not the payment's physical
lifecycle: the engine always passes through acceptance, but this
system's row may witness settlement first (fast rails, or a feed
event racing the executor's own in-flight HTTP response).

Mirror rule: the executor's response-processing CAS is
evidence-guarded too — a late "accepted" response against a row whose
outcome is already EXECUTED affects zero rows and never regresses a
settled payment.
```

### 10.2 Per-dimension transition rules

```text
stage        ENRICH → POST      on enrichment success
             POST → CONFIRM     on engine acceptance (sync or via
                                evidence §4.4) or an ambiguous POST
                                attempt outcome routed to CONFIRM
                                (§7.2). Note: some MAYBE_SUBMITTED
                                rows legally remain at POST
                                (BLOCKED fail-closed cases §7.2;
                                RETRY_WAIT after the §9.2 downgrade)
                                — L3 permits MAYBE at POST
             CONFIRM → POST     ONLY via the §9.2 trust-age downgrade
                                (same-key re-attempt) — the single
                                sanctioned backward move
             frozen             once outcome is set (L1)

stage_state  READY → CLAIMED    worker claim (lease, §11)
             CLAIMED → READY    stage advanced / work done
             CLAIMED → RETRY_WAIT  transient failure (schedule)
             CLAIMED → (via stage/submission change) per §7.2
             RETRY_WAIT → CLAIMED  scanner claim when due; the claim
                                checks repost_permitted (§7.0 —
                                retry-guard §6.4, divergent
                                payload, freeze)
             any → BLOCKED      fail-closed events, exhaustion,
                                amendment-park, escalation, anomaly
                                (blocked_reason set, L8; never
                                overwritten on an already-BLOCKED
                                row — §9.3/§10.1)
             BLOCKED → RETRY_WAIT / READY  ONLY via an explicit ops
                                action (§20) or the sanctioned §9.2
                                downgrade — both gated by
                                repost_permitted (§7.0) when the exit
                                is toward a re-POST
             CLAIMED lease expiry: at ENRICH/POST-pre-call →
                                re-claimable in place; after a POST
                                may have been sent → stage CONFIRM,
                                READY, MAYBE_SUBMITTED (§11)

submission_  NOT → MAYBE        ambiguous POST attempt (§7.2) or
state                           posting-claim lease expiry (§11)
             NOT/MAYBE → SUBMITTED  engine acceptance evidence (sync
                                ack, feed/query acceptance or
                                settlement) — tightening, always safe
             MAYBE → NOT        ONLY a fresh synchronous definitive
                                reject of this system's own POST
                                (§9.4); never from a query answer

outcome      any outcome write  also sets, in the same transaction:
                                stage_state := READY (the frozen-row
                                convention), claim fields,
                                next_retry_at, blocked_reason, and
                                maybe_since / escalated_at cleared
                                (frozen rows keep
                                submission_state, and an uncleared
                                maybe_since would leave them on the
                                MAYBE-age clocks) — so L6/L7/L8 hold
                                trivially on terminal rows and a
                                terminal transition out of CLAIMED
                                cannot violate L6
             NULL → EXECUTED    settlement evidence; amount equality;
                                sets SUBMITTED (L4)
             NULL → REJECTED    authoritative negative (sync
                                definitive, feed reject, resolver
                                REJECTED, ops reject under the
                                release guard); sets EXACTLY ONE
                                marker (L9 totality)
             NULL → CANCELLED   auto-cancel §6.4 or ops close, under
                                the release guard
             NULL → SUPERSEDED  ops supersede, under the release
                                guard
             write-once; the row freezes (L1)
```

### 10.3 Legality matrix

Normative; L2–L8 are DB CHECK constraints. L1 is SPLIT (honest
about what Oracle can enforce): the terminal-row SHAPE (outcome set ⇒
stage_state READY, claim fields / next_retry_at / blocked_reason
NULL) is a CHECK constraint, but the FREEZE — no further changes
after outcome is set — is a TRANSITION property no CHECK can see
(a CHECK sees only the resulting row image). The freeze is enforced
by the universal CAS discipline (every dimension-changing WHERE
carries `outcome IS NULL`) plus a DB backstop: an UPDATE trigger
rejecting any dimension change on a row whose outcome was already
non-NULL — or a restricted-role model where humans write only via
audited stored procedures (choose at build; the trigger is the
default). The same backstop class covers the §10.1 release guard: a trigger rejects a terminal-negative outcome write on a row
with submission_state MAYBE_SUBMITTED/SUBMITTED unless the session
context carries the evidence flag — set by the authoritative-negative
code path or by the §9.3 apply-platform-verified-outcome audited
operation (the single legitimate MANUAL setter) — so raw
fat-finger SQL in the no-console interim fails loudly instead of
silently releasing money. L9 is code +
drift-scanner verified (cross-table):

```text
L1  outcome IS NOT NULL ⇒ row frozen: no further dimension changes.
    The outcome-setting transaction itself normalizes the row:
    stage_state := READY, claim fields / next_retry_at /
    blocked_reason cleared (§10.2 outcome rule)
L2  stage = CONFIRM     ⇒ submission_state >= MAYBE_SUBMITTED
L3  submission_state >= MAYBE_SUBMITTED ⇒ stage >= POST
L4  outcome = EXECUTED  ⇒ submission_state = SUBMITTED
L5  stage = CONFIRM     ⇒ stage_state IN (READY, BLOCKED)
L6  stage_state = CLAIMED ⇒ claimed_by AND claim_expires_at set;
    otherwise both NULL
L7  stage_state = RETRY_WAIT ⇒ next_retry_at set
L8  stage_state = BLOCKED ⇒ blocked_reason set; otherwise NULL
L9  outcome = REJECTED ⇒ exactly one obligation marker
    (validation_failed or provider_rejected) was set in the same
    transaction (totality)
```

### 10.4 Display labels (humans only — no rule may key on these)

Derived (view or computed expression) for dashboards, the card, log
lines, and ops:

```text
CREATED                        = ENRICH · READY
ENRICHING                      = ENRICH · CLAIMED
ENRICHMENT_RETRYABLE           = ENRICH · RETRY_WAIT
READY_TO_POST                  = POST · READY
POSTING                        = POST · CLAIMED
POST_FAILED_RETRYABLE          = POST · RETRY_WAIT
UNKNOWN                        = CONFIRM · READY · MAYBE_SUBMITTED
COMMITTED_WAITING_CONFIRMATION = CONFIRM · READY · SUBMITTED
NEEDS_REVIEW                   = any stage · BLOCKED (+ blocked_reason)
COMPLETED                      = outcome EXECUTED
REJECTED_FINAL                 = outcome REJECTED
CANCELLED / SUPERSEDED         = outcome CANCELLED / SUPERSEDED
```

Labels are display-only in the strictest sense: they shall not
appear in any machine-consumed API payload. The card read contract
returns the dimension columns (plus the label, for display); no
consumer may parse the label.

### 10.5 Flow reference table

Common paths in tuple deltas (S=stage, SS=stage_state,
SUB=submission_state, O=outcome; guards from §10.1–§10.3 always
apply):

| Event | Before → After | Money / markers |
|---|---|---|
| §6.8 creates request | — → ENRICH·READY·NOT·∅ (creating_ordering recorded) | +committed |
| Enrichment claim / success | ENRICH·READY → ENRICH·CLAIMED → POST·READY | — |
| Enrichment transient / exhausted / unmapped | ENRICH·CLAIMED → ENRICH·RETRY_WAIT / ENRICH·BLOCKED(RETRY_EXHAUSTED) / ENRICH·BLOCKED(UNMAPPED_CODE) | — |
| Enrichment invalid-data | ENRICH·CLAIMED → O=REJECTED | −committed; validation_failed set |
| Posting claim (identity + instruction hash persisted §5/§7.0; assembly fresh per attempt; repost_permitted checked §7.0) | POST·READY/RETRY_WAIT → POST·CLAIMED | — |
| Engine accepted (sync) | POST·CLAIMED → CONFIRM·READY, SUB=SUBMITTED | — |
| NOT_SUBMITTED failure (connect refused, insufficient balance) | POST·CLAIMED → POST·RETRY_WAIT | — |
| Ambiguous (read timeout, crash, DUPLICATE_REQUEST, lease expiry) | POST·CLAIMED → CONFIRM·READY, SUB=MAYBE | — |
| Sync definitive reject — invalid data | POST·CLAIMED → O=REJECTED (SUB stays NOT) | −committed; validation_failed set |
| Sync definitive reject — other | POST·CLAIMED → O=REJECTED (SUB stays NOT) | −committed; provider_rejected set |
| Unmapped engine code | POST·CLAIMED → POST·BLOCKED(UNMAPPED_CODE), SUB=MAYBE | — |
| Key collision, EXPECTED divergence (divergence_expected = true, §7.2) | POST·CLAIMED → CONFIRM·READY, SUB=MAYBE; divergent_payload_at set (no park, no CRITICAL — evidence the original arrived; resolver recovers the outcome) | — |
| Key collision, ANOMALOUS divergence (divergence_expected = false, §7.2) | POST·CLAIMED → POST·BLOCKED(ENGINE_INCONSISTENCY), SUB=MAYBE; divergent_payload_at set + CRITICAL | — |
| Feed settlement (any active row — terminal evidence) | any active → O=EXECUTED, SUB=SUBMITTED | +confirmed |
| Feed / resolver reject (any active row) | any active → O=REJECTED | −committed; provider_rejected set |
| Amount mismatch (§8; any active row) | any active → same stage·BLOCKED(AMOUNT_MISMATCH), SUB=SUBMITTED (settlement evidence tightens) | — |
| Resolver NOT_FOUND after trust-age, MAYBE row (incl. BLOCKED), repost_permitted §7.0 passes (safety = the §1 assumed collision contract; proven before go-live, §18 item 1) | any·any·MAYBE → POST·RETRY_WAIT·MAYBE (sanctioned backward move; next_retry_at = now, attempt_count reset per §7.4 downgrade class; blocked_reason cleared). repost_permitted failing → row stays parked, wait-then-decide | — |
| NOT_FOUND-for-SUBMITTED after trust-age | CONFIRM·READY·SUBMITTED → CONFIRM·BLOCKED(ENGINE_INCONSISTENCY); stays in resolver scope (a lag-caused false park self-heals) | — |
| MAYBE escalation (max age on maybe_since, §9.3; once per episode: escalated_at IS NULL) | non-BLOCKED·non-CLAIMED·MAYBE → same stage·BLOCKED(ESCALATED), escalated_at set; already-BLOCKED or CLAIMED rows: alert only, state write deferred/skipped; resolver continues | — |
| Retry exhaustion | POST·RETRY_WAIT → POST·BLOCKED(RETRY_EXHAUSTED) (CUTOFF_EXPIRED: RESERVED, round 10) | — |
| Auto-cancel (§6.4) | ENRICH·any / POST·(READY,RETRY_WAIT) · NOT_SUBMITTED, not BLOCKED → O=CANCELLED | −committed |
| Amendment-down vs MAYBE row, ANY stage incl. CONFIRM (§6.4 row-count 0) | → same·BLOCKED(AMENDMENT_PARKED) + alert; wait-then-decide — no auto-downgrade while the amount is stale (§7.0) | — |
| Ops retry (NOT_SUBMITTED rows; POST-stage exits gated by repost_permitted §7.0, next_retry_at set per policy — L7) | any·BLOCKED·NOT → SAME-stage·RETRY_WAIT (an ENRICH-blocked row re-enriches; it never skips to POST with unresolved data) | — |
| Ops actions on BLOCKED·MAYBE rows (§9.3) | resolve-via-query (no state change); ops-triggered §9.2 downgrade where repost_permitted passes → POST·RETRY_WAIT·MAYBE; dual-control stale-amount re-POST (§7.0 override of the staleness term ONLY) → POST·RETRY_WAIT·MAYBE | — |
| Ops apply-platform-verified-outcome (§9.3 — dual-control audited operation; evidence flag set legitimately) | any active·MAYBE/SUBMITTED → O=EXECUTED (SUB=SUBMITTED, amount equality) or O=REJECTED | +confirmed on EXECUTED; −committed + provider_rejected on REJECTED |
| Ops reject / supersede / close (release guard) | any·BLOCKED (or stalled active) → O=REJECTED / SUPERSEDED / CANCELLED | −committed; marker per L9 on REJECTED |
| Late feed settlement for BLOCKED row | any·BLOCKED (active) → O=EXECUTED, SUB=SUBMITTED (amount equality) | +confirmed |

------

## 11. Concurrency Model

Two-tier:

```text
Obligation lock (pessimistic, SELECT ... FOR UPDATE) owns money math:
  shortfall calculation, request creation (§6.8), amount updates,
  overpay detection, completion evaluation, derivation (§4).

Request CAS (optimistic, conditional UPDATE + row count) owns:
  claims, dimension transitions, retry ownership, stale-worker
  protection.
```

Rules:

- Global lock ordering: any transaction touching both tables acquires
  the obligation lock FIRST.
- Multi-payment trades (§1 contract facts): the state machine (§10)
  is per REQUEST, and I6 caps active requests per OBLIGATION — so a
  trade with N payments legitimately runs N request state machines
  IN PARALLEL; no rule may treat concurrent active requests across
  one trade's obligations as an anomaly. Their only coupling is the
  trade's snapshot messages: one snapshot may drive transitions on
  several obligations in one consumption, each under its own
  obligation lock, taken in the §6.1 deterministic tuple order
  (never all at once — no cross-obligation transaction exists).
- Every DIMENSION-CHANGING update (stage, stage_state,
  submission_state, outcome, blocked_reason) acquires the obligation
  lock first and re-derives (§4) in the same transaction — a
  dimension change is by definition a derivation-input change. Only
  claim-field-only updates (claimed_by, claim_expires_at, attempt
  counters — no dimension, no money, no derivation input) may skip
  the obligation lock. Cost is negligible: I6 already limits
  per-obligation write concurrency to ~1.
- Every CAS is a conditional UPDATE whose WHERE carries the full
  dimension precondition and whose row count is the verdict; every
  call site branches on `rowCount == 1`. No ORM dirty-checking on
  these tables.
- Claims are leases: `claimed_by` + `claim_expires_at`, and the claim
  sets stage_state = CLAIMED so a second scanner cannot re-claim
  mid-processing (L6).
- The POSTING claim additionally: carries
  `divergent_payload_at IS NULL` in its CAS WHERE and re-checks the
  derived repost_permitted terms (§7.0) before launch — the last
  gate before the wire. Every claim assembles the instruction FRESH
  (§7.0) and persists, in the claim transaction before the HTTP
  call: last_sent_hash, the divergence_expected flag (computed
  against the PRIOR hash before overwriting it — §2.2),
  last_post_attempt_at, the post_attempt_seq increment (§2.2), and
  the §14.1 ATTEMPT_STARTED journal insert (the content
  write-ahead) — plus identity on the first claim (§5.1).
  The UETR cannot be persisted pre-wire: the platform SDK mints it
  inside the POST call; it is persisted from ACCEPTANCE-class
  responses only (§5).
- Ambiguous claim-commit: a worker whose posting-claim COMMIT
  outcome is unknown (failover / connection loss mid-commit) shall
  NOT proceed to the HTTP call — the write-ahead rule (§5) requires
  provably-durable identity and hash before the wire.
  Treat the claim as lost; lease expiry takes the row to MAYBE and
  the resolver owns it. (Test-catalog entry, §16.6 artifact 6.)
- Scanner claim protocol (NORMATIVE — decided 2026-07-11; replaces
  the earlier `FOR UPDATE SKIP LOCKED` guidance, which could invert
  the global lock order):
  1. Candidate selection takes NO row locks: a plain bounded read
     (dimension/anchor predicates, DB time, deterministic order).
  2. Per candidate, a NEW transaction: obligation lock FIRST (the
     global order), then the claim CAS whose WHERE carries the full
     expected state. Row count 0 = lost race — skip, never retry.
  3. Per-item transaction boundaries; one failed item never poisons
     the batch.
  The CAS is the contention-resolution mechanism; SKIP LOCKED is
  unnecessary and shall not be used in any step that could precede
  an obligation lock in the same transaction.
- Claim-transition classification (decided 2026-07-11): READY →
  CLAIMED, the expired-lease takeover, and unclaim are CLAIM
  MECHANICS — they run under the obligation lock (acquired first by
  the per-item transaction above) but do NOT trigger §4
  re-derivation: no derived column reads CLAIMED. The
  skip-the-obligation-lock exemption below is thereby NARROWED to
  pure counter/lease-field updates that change no dimension
  (attempt_count, next_retry_at, claim_expires_at extension).

Claim-expiry recovery:

```text
- Expired claim at ENRICH → re-claimable in place; the work is
  repeatable (no external effect has occurred).
- Expired claim at POST → stage CONFIRM, stage_state READY,
  submission_state MAYBE_SUBMITTED. NEVER re-claimed for posting, NO
  exceptions: the worker may have died before, during, or after the
  HTTP call, and re-POSTing a possibly-sent payment is a
  double-payment path. (No "provably not launched" carve-out exists:
  identity is persisted in the claim transaction itself (§5), so a
  committed POST claim can never prove non-launch.) The resolver
  (§9) recovers the outcome by status query.
```

Graceful shutdown ordering:

```text
1. Stop Kafka listener containers (no new inbound work).
2. Stop scanners.
3. Drain in-flight POSTs (bounded wait).
4. Release only the ENRICH claims this worker still holds.
   POST claims are NEVER released on shutdown — a POST may be in
   flight; lease expiry (→ CONFIRM, MAYBE) is the only exit.
```

------

## 12. Read Model for the UI Card

The card reads; this system never pushes.

```text
ui_step_status:      NOT_STARTED → IN_PROGRESS → COMPLETED
                                              ↘ CANCELLED
                     (IN_PROGRESS again after reopening, §6.5 —
                      from COMPLETED or CANCELLED alike; CANCELLED
                      = the §4.1 zero-required terminal branch,
                      round 11: the payment was removed by newer
                      upstream truth and fully unwound. NEVER
                      displayed as COMPLETED.)

active exception:    a separate concept from step status; a step can
                     be IN_PROGRESS with an active exception without
                     treating a retryable error as final failure
```

Both are derived per §4; the card only ever reads them. Requests are
shown to humans via the §10.4 display labels.

Card addressing: the card looks up state by `business_id` ONLY, and
the lookup returns ALL obligations of that trade — one entry per
payment (§1 contract facts: a trade can carry multiple payments;
multiple results are the NORMAL case, not an error — no rule may
treat result count as a health signal). An index on business_id
backs the lookup. `ui_process_instance_id` / `ui_step_instance_id`
remain stored as display/reference fields.

ALL-PAYMENTS TABLE projection (added 2026-07-17 — the second
defined read surface; review 7ab31e5 M4 closed the granularity
gap). The step CARD above stays obligation-granular; the
all-payments TABLE is REQUEST-granular, informational only (no
create/modify/retry/cancel actions), and is a pure projection —
no schema change, no new state:

```text
Row granularity and identity:
  - one row per payment_request where requests exist
    (row_type = REQUEST, keyed by the request id);
  - one placeholder row when an obligation exists with NO request
    (row_type = OBLIGATION_ONLY, keyed by the obligation id).
  NO DUPLICATES BY CONSTRUCTION: the projection is obligation LEFT
  JOIN request — the moment the first request exists, the join
  yields REQUEST rows instead of the placeholder; there is no
  cleanup logic to get wrong.
Field separation (amounts can never be conflated):
  - obligation context on EVERY row: required / committed /
    confirmed amounts, ui_step_status, active exception (+ manual-
    action flag), reopened indicator;
  - request fields on REQUEST rows only: request amount, §10.4
    display label, blocked_reason, timestamps; empty/n-a on
    OBLIGATION_ONLY rows.
  Example: required 120 fulfilled as 100 + 20 renders as TWO
  REQUEST rows (amounts 100 and 20), each carrying required 120
  and the cumulative counters — never one synthetic 120 row.
Pre-request visibility: the OBLIGATION_ONLY row shows the scope
  tuple, the required amount (blank for a §6.6 anchor), "no
  request created", and a NULLABLE reason (review d00ef6a M2):
  the derived active exception (§4.2) when one is LIVE; otherwise
  NULL, with the status itself carrying the story — a §6.2
  covered-on-arrival scope shows COMPLETED with no exception; an
  anchor retired by absence shows CANCELLED (display note
  REMOVED_BEFORE_REQUEST). Exception-blocked states always have a
  live exception by construction (§4); the frontend never invents
  a reason.
Status precedence: step status is obligation-derived (§4.1) ONLY;
  REQUEST rows additionally carry the request's §10.4 label; the
  frontend recomputes NOTHING and no rule keys on any of it
  (§10.4).
Decided display defaults (PO 2026-07-17): terminal/historical
  request rows (e.g. a REJECTED predecessor) remain visible by
  default — history is never laundered — with client-side
  filtering permitted; a fully removed scope (required = 0) KEEPS
  its row(s), the obligation context showing CANCELLED (§4.1),
  consistent with the rollup algebra below.
Edges (unchanged, restated): NOT_STARTED = row absence; the one
  blind spot remains the unparseable-scope message that can
  produce no row at all (§6.6 — DLT + alert covers it, and the
  TL-7 key-only anchor stays the FUTURE extension for it).
Read discipline: read-only, no locks; masking + content rules per
  §16.3/§12; freshness indicator wired to the §15 lag metric;
  result count is never a health signal.
API/read contract (2026-07-17, review d00ef6a M2 — the projection
is an API, not just a row shape):
  - SCOPE: exactly two modes — single trade (business_id, the
    card's sibling) and BOUNDED estate listing (mandatory
    server-side filters + pagination; an unbounded all-rows dump
    is never offered);
  - AUTHORIZATION: the same read-only enterprise role/scoping as
    the card (§16.3); entitlement enforced server-side — no
    client-driven enumeration beyond the caller's scope;
  - ROW KEY: composite (row_type, source_id) — REQUEST rows keyed
    by request id, OBLIGATION_ONLY rows by obligation id; stable
    across refreshes and pages;
  - ORDERING/PAGINATION: deterministic order (obligation identity,
    then request_seq); keyset cursor on that order (never OFFSET);
    fixed page cap (config §16.6);
  - PAGINATION SEMANTICS = LIVE BROWSE (decided 2026-07-17, review
    c8a92f1 M2): each page is truthful at its read instant (the
    freshness indicator applies per response); no row appears
    twice within one traversal, but CROSS-PAGE COMPLETENESS under
    concurrent writes is NOT guaranteed — rows inserted or
    re-derived between pages may be missed. A stable
    traversal/export mode (cursor carrying an as-of token) is
    FUTURE work, designed only if a consumer needs it; tests
    assert the live-browse contract, never exactly-once
    enumeration under concurrent writes;
  - FILTERS: server-side in estate mode at production history
    volumes (status/exception/date); client-side filtering is a
    single-trade convenience only;
  - ESTATE QUERY CONTRACT (review c8a92f1 M2; scoped per review
    4d5cb83 M4): the CONCEPT is authorization scope first, then
    server-side filters, ordered by (obligation identity,
    row_type, request_seq); the EXECUTABLE contract — resolved
    SQL, authorization predicate/join, total order incl.
    tie-breaker + NULL encoding for OBLIGATION_ONLY rows, cursor
    fields, supported filter-shape matrix, exact index(es), plan
    acceptance table — lives in CA-4's §12 estate-query resolution
    and is BLOCKING for estate mode only (single-trade mode is not
    gated). No agent invents any of those facts.
```

Step granularity (open, folded into the TL-2 read contract, §18):
does the UI render one step per PAYMENT, or one rolled-up step per
TRADE? The §4 derivations are per obligation; a per-trade rollup
is a display aggregation the read contract must define — and the
answer must cover the FULL state algebra (round 12), not the old
completed/not-completed binary: {NOT_STARTED, IN_PROGRESS,
COMPLETED, CANCELLED} × active exceptions, including MIXED
COMPLETED/CANCELLED (suggested: rolled-up COMPLETED when every
non-CANCELLED payment completes and at least one COMPLETED
exists), ALL-CANCELLED (suggested: rolled-up CANCELLED), and a
trade whose current snapshot carries an EMPTY derived set with no
obligation rows (renders NOT_STARTED by row absence, §6.0). NOTE:
"trade cancelled" (PO-5) and "payment removed from the settlement
set" (§4.1 CANCELLED) are DIFFERENT concepts unless the PO
explicitly equates them
— no core-model change either way.

Lookup semantics:

- No obligation row found = `NOT_STARTED` — the defined display for a
  step whose first message has not arrived. Not an error. (See §6.6
  key-only anchoring for the malformed-message caveat.)
- If the read surface cannot reach the database, the card shows
  "unavailable" — never cached/stale data presented as authoritative.
- Freshness: under inbound lag the DATABASE ITSELF is behind
  the world — no-stale-cache does not mean current. When consumer
  lag on either inbound flow exceeds its §15 threshold, the card
  surfaces a data-as-of / lag indicator: ops and users act on the
  card (recalls, counterparty calls) and must see that the picture
  may be minutes old. The read contract (TL-2, §18) must state a
  freshness SLA including replica lag if a replica serves reads.

Content rules, applied at field write time: understandable by an
operations user; no sensitive account information; no stack traces
(stack traces live in logs, keyed by correlation id).

Trade cancelled after the step started: currently displays
"completed" (open PO question, §18).

------

## 13. Exception Categories and Overpay Policy

```text
DATA_VALIDATION_FAILED        retryable = false
INSUFFICIENT_ACCOUNT_BALANCE  retryable = policy-driven
SYSTEM_UNAVAILABLE            retryable = true
OVERPAY_DETECTED              retryable = false,
                              manual_action_required = true
PAYMENT_OUTCOME_UNKNOWN       retryable = true, severity = CRITICAL
PROVIDER_REJECTED             retryable = false,
                              manual_action_required = true
                              (data held locally is valid; the
                               engine or ops refused the payment)
BLOCKED (derived)             retryable = false,
                              manual_action_required = true
                              codes = blocked_reason (§2.2):
                              RETRY_EXHAUSTED, UNMAPPED_CODE,
                              AMOUNT_MISMATCH (CRITICAL, §8),
                              CUTOFF_EXPIRED (RESERVED round 10 —
                              never produced; §2.2),
                              ENGINE_INCONSISTENCY (§9.2, §7.2),
                              AMENDMENT_PARKED (§6.4),
                              OPS_PARKED, ESCALATED (§9.3 —
                              CRITICAL; the escalated
                              MAYBE_SUBMITTED class)
```

Category and retryability are separate fields; severity is
WARNING / ERROR / CRITICAL.

Overpay: `overpay_blocked` is a latch set when
`confirmed_amount > required_amount` (only re-evaluated, never
silently un-set by generic recalculation). Decided rule: **once
overpay appears on the row, this trade's payment is ignored moving
forward** — no new requests are created or submitted, no automated
compensation is attempted, and later amendments never resume
automated payment (§6.5 latch guard). Upstream and feed processing
continues; the step is not completed; the read model shows the
exception; resolution is manual in other systems. Overpaid money
eventually returns to the account, but this system has no visibility
into that flow — see §19.2. Setting the latch fires an alert (§15 —
the latch itself pages, not only its integrity check).

Rationale, upgraded from policy to correctness (decided): the
latch is a ONE-WAY DOOR into manual territory. From the instant it
sets, overpay recovery (recall/refund) proceeds at the payment
platform with NO visibility here (§19.2) — so committed/confirmed
amounts stop being trustworthy inputs for automation; any automated
resumption could pay through an invisible refund window. Only a
human who can check the platform's books may resume the scope.

Accepted consequence (the cross-stream race): the upstream flow
and the status feed share no ordering domain, so a settlement can
land between two amendments. Trace: amend(80) applies → R1(100)
settles EXECUTED → latch sets (100 > 80) → amend(150) applies
(§6.7-legal, strictly newer). End state, permanent: required 150,
confirmed 100, latch set — the card shows OVERPAY_DETECTED on a
scope that is UNDERPAID against the latest truth. The
AMENDMENT_ON_LATCHED_SCOPE alert (§15) is the designed manual path.
Auto-unlatching when required rises to ≥ confirmed was considered
and REJECTED for the rationale above: post-latch, the stored amounts
may already be fiction.

------

## 14. Storage Scope and Logging

This system stores only the current state needed for its own
processing:

- `payment_request` carries its current dimensions only; no
  transition-history/journal table. The authoritative transition
  audit trail is the §14 log line plus the payment platform. (The
  §14.1 attempt journal, added 2026-07-16, is a CONTENT record, not
  transition history — it does not change this rule.)
- Posting-claim log lines additionally carry the sent instruction
  hash (last_sent_hash) and the attempt count: incidents can
  answer "which hash did we send on attempt N" from the log alone —
  the attempt-history table considered in review was REJECTED as a
  REPLACEMENT for these fields (§2.2), and that rejection stands.
  What the log CANNOT answer is "what were the BYTES" — recorded
  2026-07-16: the request actually sent to the engine is not
  visible to this team (status is queryable, content is not), which
  is the driver for §14.1.
- Compensating control: every successful dimension-changing CAS emits
  one structured INFO log line —
  `request_id, idempotency_key, request_seq, correlation_id,
  (stage, stage_state, submission_state, outcome) before → after,
  display label, trigger_source, trigger_event_id` (key + seq added: the log platform lives OUTSIDE the payment database, so the
  log is a durable, restore-surviving record of every issued and
  every POSTED key — §5.2 step 5b derives its enumeration bound
  from it, and the retention floor below already covers the replay
  window by definition). ATTEMPT-class lines (posting claim,
  outcome recording, lease-expiry recovery) ALSO carry
  `post_attempt_seq` and the attempt event type (2026-07-17 —
  review 7ab31e5 M5): they are the stable join to the §14.1
  journal's event pairs; `attempt_count` on the line is
  retry-budget context only (it resets on the §9.2 downgrade).
  This is the only local forensic record for
  drift alerts, inbox anomalies, and BLOCKED-queue triage — and
  the ONLY restore-surviving one (§14.1 restore posture).
- Log retention FLOOR (required now, because those commitments exist
  now): at least the greater of 90 days and the DR replay window +
  investigation SLA. VALIDATED with the business (PO review):
  the 90-day floor is sufficient; no dispute-driven extension is
  required.

### 14.1 Local attempt journal (team-internal audit — added 2026-07-16, simplified 2026-07-17)

Driver (PO-recorded 2026-07-16): the request actually sent to the
payment engine is NOT directly visible to this team — the
SDK/platform own the wire form; the engine's status is queryable,
its content is not. The team therefore keeps a local record of the
CANONICAL INSTRUCTION each posting attempt submitted to the SDK.

GOVERNING STANCE (PO 2026-07-17, review 7ab31e5; wording unified
per review c8a92f1 H1 — ONE formulation, used verbatim everywhere):
this journal is PURELY team-internal tracking and audit. THE
JOURNAL IS NEVER A BUSINESS OR MONEY-SAFETY GATE: statement-local
insert failures proven by T-38 are caught around the single JDBC
statement, recorded in memory, and alerted only AFTER host commit;
FATAL connection/session/transaction/commit failures propagate as
ordinary host infrastructure failures. The guarantee is "NO
INCORRECT PAYMENT OUTCOME" — not "no journal failure can ever fail
an attempt". If the journal has gaps, the fallback is the §14 log
line (key, seq, hash — always present) plus asking the payment
platform: STATUS/outcome/reference are recoverable by
UETR/idempotency-key QUERY (§9); exact CONTENT is recoverable only
via a MANUAL platform-team request — an UNPROVEN capability
(review d00ef6a M1; see the wire-capture ask below). Until that is
evidenced, a missing journal row is an accepted, PERMANENT
canonical-content gap. This is why §5's UETR persistence rules
matter more than this journal.

Position in the model: the §14 log line remains the transition
record; this journal is the CONTENT record — two sinks of one
attempt, joined by (request_id, post_attempt_seq, event type). It
REPLACES NOTHING: divergence_expected, last_sent_hash, and the log
line all stay (the §2.2 rejection of replacing those columns
stands unchanged), and NO runtime rule, scanner, gate, resolver,
or derivation may EVER read this table.

Table — ops/audit schema in the SAME DATABASE (the same-transaction
inserts below require it; no XA/distributed transactions), own
tablespace; the §2 payment model remains exactly four tables:

```text
payment_attempt_journal
  journal_id        identity PK
  request_id        (NO foreign key — request archival stays
                     unblocked; rows are self-contained)
  idempotency_key   (denormalized)
  post_attempt_seq  (copied from the request row, §2.2 — NEVER
                     attempt_count, which resets on the §9.2
                     downgrade and would collide on the recovery
                     re-POST)
  event_type        ATTEMPT_STARTED | ATTEMPT_RESOLVED
  occurred_at       UTC; monthly interval-partition key
  trigger_source, correlation_id
  payload_hash      (= last_sent_hash, §7.0/CA-6 algorithm)
  payload_content   (STARTED rows: the FULL CA-6 canonical
                     serialization, EVERY attempt — simplicity
                     rule below)
  outcome           (RESOLVED rows — the §7.2 classes VERBATIM plus
                     LEASE_EXPIRED_MAYBE; never an invented
                     vocabulary)
  error_code, error_detail, response_excerpt
  UNIQUE(request_id, post_attempt_seq, event_type) — necessarily a
  GLOBAL unique index (it cannot contain the partition key), so
  partition maintenance MUST use
  DROP PARTITION ... UPDATE GLOBAL INDEXES; CA-10 carries the full
  DDL contract (types, nullability, event-shape CHECKs, LOB
  storage, index strategy)
  local index on idempotency_key
```

SIMPLICITY RULE (2026-07-17 — the dedup-by-hash design was REJECTED
as unimplementable under the no-read invariant, review 7ab31e5 H1;
this rule replaces it): payload_content is stored IN FULL on EVERY
ATTEMPT_STARTED row. Retries usually repeat identical bytes; that
redundancy is ACCEPTED — storage is bounded by partitions and is
an audit-side cost, never a correctness input. FUTURE optimization,
permitted ONLY if Q31 evidence shows a real volume/latency problem:
CONSECUTIVE-identical dedup backed by a durable
last_content_post_attempt_seq column on payment_request — never a
journal read, never a global once-per-hash rule (both are
structurally impossible under the no-read invariant).

Events — each insert rides a transaction that already exists (no
new commit points, no new locks):

- ATTEMPT_STARTED: inserted in the posting-claim transaction beside
  the §2.2 write-ahead fields. When healthy, this makes the content
  record durable BEFORE any byte leaves — write-ahead as a GOAL,
  not a gate (coupling rule below).
- ATTEMPT_RESOLVED: inserted in whichever transaction ends the
  attempt episode — the §7.2 classification write, or the
  lease-expiry recovery (outcome LEASE_EXPIRED_MAYBE) — and only
  when that transaction's dimension CAS affected exactly one row
  (the CAS arbitrates the worker/sweep race; the UNIQUE constraint
  backstops).

COUPLING — NEVER LOAD-BEARING, stated as a NARROW GUARANTEE
(2026-07-17, revised per review d00ef6a H3 — honest about the
Spring/Oracle mechanics; the earlier fail-the-claim rule was REJECTED,
as was the over-broad "any error is harmless" claim):

- The rider INSERT runs INSIDE the host transaction wrapped in
  STATEMENT-LEVEL isolation: a plain try/catch around the single
  INSERT, with NO inner transaction boundary (an inner
  @Transactional participation would mark the host rollback-only —
  the exact trap this rule forbids). STATEMENT-LOCAL means ONLY
  the pinned, T-38-proven Oracle vendor-code signatures —
  ORA-00001, ORA-02290, the two journal-trigger codes, the
  evidenced space-error family; TIMEOUTS AND EVERY UNKNOWN OR
  AMBIGUOUS TRANSLATION ARE FATAL BY DEFAULT (2026-07-17, review
  928341a H2 — a timeout does not prove the session is usable).
  Allowed failures are swallowed: the gap is recorded in
  memory/metrics
  and the host transaction proceeds; the AUDIT-GAP alert is
  emitted AFTER the host COMMIT (side effects after commit, §11 —
  a rolled-back host must never report a phantom gap).
- FATAL failures — connection loss, session termination,
  commit-time failure, transaction invalidation — are NOT
  isolatable by any mechanism. They fail the host transaction as
  ORDINARY infrastructure failures, which the existing machinery
  already recovers (an uncommitted claim leaves the row
  READY/RETRY_WAIT for the next scan; a committed claim recovers
  via lease expiry → MAYBE, §11). The PROVABLE guarantee is
  therefore: THE JOURNAL CAN NEVER CAUSE AN INCORRECT PAYMENT
  OUTCOME — not "no journal error can ever fail an attempt".
  T-38 exercises both classes on the real JDBC/Spring stack.
- Because the insert shares the host transaction, a host ROLLBACK
  still removes the journal row — no phantom STARTED rows.
  AUTONOMOUS TRANSACTIONS remain FORBIDDEN (they would create
  exactly those phantoms).
- Gap recovery: the §14 line still records key/seq/hash for the
  attempt; status/outcome are recoverable by query (§9); exact
  content only via the manual platform-team path above. A journal
  gap is an audit degradation, never a money event.

ENABLEMENT GATE (2026-07-17): journal writes sit behind a plain
config switch, DEFAULT OFF in production, enabled only after the
Q30 journal items are EVIDENCED: encryption at rest ENABLED (or an
explicitly approved, expiry-dated compensating-control exception)
AND the compliance-approved retention schedule. Payments go-live
does NOT wait for journal enablement — an OFF journal is simply the
pre-2026-07-16 designed state (log-only forensics).
SWITCH-TRANSITION RULE (review d00ef6a M3; drain defined per
review c8a92f1 M1): the switch may change state ONLY under posting
freeze + drain (§16.1) — flipping mid-traffic manufactures
half-pairs (a RESOLVED without its STARTED, or vice versa) that
would false-alarm the unmatched-pair alert. DRAIN COMPLETION means
ZERO outstanding POST claims/attempt episodes — including claims
abandoned at the pre-wire freeze check, whose episodes end only at
their lease-expiry resolution; the switch waits for those too.
Planned transitions are recorded so alert triage can distinguish
them (§15/N.1); T-38 exercises both toggle directions and the
abandoned-claim boundary.

Security — the ONE controlled exception to §16.3's no-local-content
rule:

- payload_content is real payment data. Grants: a restricted audit
  role ONLY; reads of the journal are themselves DB-audited;
  encryption at rest per the enablement gate above; NEVER
  replicated/copied to lower environments; retention = partition
  drop per the compliance answer (open ask below). §16.3 masking
  deliberately does NOT apply INSIDE the journal — access control
  replaces it; the card, logs, and traces remain masked exactly as
  before.

Honesty note — what this journal proves (review 7ab31e5 M2): the
CANONICAL INSTRUCTION COMMITTED BEFORE ATTEMPTED TRANSMISSION —
application intent. It is NOT proof of the post-SDK wire bytes
(the SDK may transform the message); the wire-capture ask below
governs that gap. Restore posture (review 7ab31e5 M1): the journal
lives in the SAME database as the payment tables, so a
full-database point-in-time restore rewinds it with everything
else; it survives logical/schema-level restores of the payment
schema and payment-tablespace TSPITR. The ONLY restore-surviving
record is the external §14 log platform.

Scope and guardrails:

- POSTING attempts only — not ENRICH retries, not resolver
  settlements, not lifecycle events (those live in §14 and on the
  rows).
- INSERT-only forever: no UPDATE or DELETE grants; retention is
  partition drop.
- Never read at runtime; §5.2 post-restore forensics MAY read it
  (a human runbook, not a rule).

OPEN (recorded 2026-07-16; blocking status revised 2026-07-17):

- TL/platform ask (non-blocking): can the post-SDK WIRE form be
  captured (interceptor/SDK hook)? This journal records the
  canonical instruction WE assemble; if the SDK transforms it,
  wire-digest capture is a possible extension — design only after
  that answer.
- Compliance ask (BLOCKS JOURNAL ENABLEMENT, never payments
  go-live): retention horizon and legal-deletion obligations for
  payment content at rest (sets the partition-drop policy), and
  whether partition-level deletion satisfies erasure demands or a
  DBA-executed, audited redaction procedure is required.

------

## 15. Monitoring

Clock discipline: AGE alerts key on the set-once episode
anchors (§2.1/§2.2 — maybe_since, escalated_at, submitted_at,
last_post_attempt_at, validation_failed_first_at; divergent_payload_at
is the sixth anchor, consumed as IS-NULL by repost_permitted §7.0
rather than as an age). state_changed_at serves only
non-churning states (BLOCKED age — safe once §9.3 stops overwriting
parked rows) and is-anything-moving checks: dimension churn resets
state_changed_at and silently re-arms any age alert keyed on it.
Scopes key on the dimension columns, never on display labels and
never on blocked_reason as a rule input (§10.1).

```text
- Drift scanner mismatches (I1/I2, L9)         → page
- Stuck reservation age (active request, no
  progress)                                    → alert
- Oldest MAYBE_SUBMITTED age (maybe_since)     → alert (age threshold)
- MAYBE_SUBMITTED past tier-2 age (maybe_since,
  §9.3)                                     → re-page / incident
- BLOCKED count and age (by blocked_reason)    → ops queue metric
- Unmatched feed events (metric/log only)      → alert on volume
- provider_rejected marker set                 → alert (a requested
                                                 payment is not
                                                 happening)
- provider_reject_count reaches 2 (§2.1)       → alert (ops-only
                                                 clearing from here)
- validation_reject_count reaches 3 (§2.1) → alert (repeat
                                                 validation-reject
                                                 cycle; no gate — a
                                                 corrected message is
                                                 the designed
                                                 recovery)
- Overpay latch SET (§13)                      → alert (business
                                                 hours)
- AMOUNT_MISMATCH (all-or-nothing violated)    → CRITICAL (defect
                                                 evidence, §8)
- ENGINE_INCONSISTENCY (§9.2; §7.2 anomalous
  branch)                                 → CRITICAL
- AMENDMENT_TIE_CONFLICT (§6.7)                → alert (manual
                                                 application needed)
- Reprocess approval CONSUMED, no completion
  evidence past SLA (§9.3 round 6)             → alert (crash or
                                                 newest-wins
                                                 abandonment —
                                                 runbook decides)
- AMENDMENT_ON_LATCHED_SCOPE (§6.5)            → alert (manual
                                                 handling)
- Payment DISAPPEARANCE (round 11 — the §6.1
  absence fan-out zeroed ≥ 1 obligation)       → metric + mandatory
  log line (business_id, zeroed scope tuples
  MASKED per §16.3 — never a raw debit_account;
  an irreversible scope fingerprint is
  acceptable — and doc.ordering); alert on
  volume/spike. P5:
  absence-as-cancellation must never be
  silent — this is the local detector for the
  accidental-omission class (H-1; upstream
  complete-set guarantee = ask 5)
- Money-truth divergence found (§19.2 policy)  → CRITICAL incident
- Live marker (validation_failed or
  provider_rejected) with NO active request,
  older than max age                           → alert
  (round 13: on required_amount = 0 scopes, ONLY the historical
   provider_rejected marker is excluded — ordering <
   upstream_ordering, live solely via count >= 2; nothing is being
   refused. A LIVE validation_failed marker on a zeroed scope STAYS
   IN SCOPE: it is the malformed-reappearance signature and must
   age into this alert (§4.2 round-13 narrowing).
   Generalizes the anchor alert: covers anchors AND scopes whose
   correction never arrives. For validation_failed the age keys
   on validation_failed_first_at — the re-tag timestamp is refreshed
   by every newer failing message and can never age. Note:
   provider_rejected_at churns the same way but acceptably — the
   ≥ 2 ops-only gate stops auto-successors after the second reject,
   bounding re-tags to at most one before a human owns the scope)
- Stale marker writes dropped (§6.9 monotonic) → metric; alert on
                                                 volume
- Evidence for a TERMINAL request (new
  event_id, zero-row CAS)                      → CRITICAL
- Kafka DLT depth > 0                          → page
- Overpay latched without visible exception    → alert (integrity)
- Inbox table growth vs purge policy           → health metric
- Consumer lag (each inbound flow)             → page over SLA
- Scanner heartbeat (any scanner silent
  for 3x its interval)                         → page
- Engine circuit breaker OPEN                  → ticket; page at 30m
- Generic stuck-state age (any active request
  older than its per-(stage,stage_state) max)  → ticket
  (split: retry states alert on next_retry_at OVERDUE beyond a
   threshold — a due row nobody claimed is a scanner problem, and
   claim/retry churn resets state_changed_at so it cannot serve
   here; non-retry states, which do not churn, alert on
   state_changed_at)
- Stale upstream messages (§6.7)               → alert on volume
- ORA-00060 deadlock count                     → ticket (lock-order
                                                 regression tripwire)
- Per-obligation request count                 → ticket over sanity
                                                 threshold
- Posting freeze EFFECTIVE (toggle set OR
  Hazelcast unreachable) without an
  acknowledged freeze ticket (§16.1)      → page (the freeze is
                                                 silent by design:
                                                 scanners idle green,
                                                 breaker CLOSED, lag
                                                 zero — this alert is
                                                 the only signal)
- Resolver sweep overran its cadence (§9.5)    → metric; alert on
                                                 repeat
- Observed-lag watchdog: feed-confirmed
  payment that was NOT_FOUND past trust-age    → alert (the ingest-
                                                 lag config is wrong)
- Overpay-latched scopes: count + oldest age   → alert (each latched
                                                 scope is frozen
                                                 manual work; a bad
                                                 feed day makes them
                                                 in bulk)
- apply-platform-verified-outcome executed
  (§9.3)                                  → alert (every use —
                                                 an audited manual
                                                 money action; never
                                                 routine)
```

Alert rollup (normative): while a root-cause condition holds
(engine circuit breaker OPEN, posting freeze active), per-row
consequence alerts — escalations, tier-2 re-pages, stuck-state
tickets — aggregate into ONE grouped incident carrying a running
count; state writes remain per-row. One engine outage must read as
one incident, not thousands of pages: the single genuine anomaly (a
real ENGINE_INCONSISTENCY) must not drown in outage collateral at
03:00. Known-outage suppression semantics live in the runbook
artifact (§16.6).

Alerting practices: metric absence is treated as bad, not as zero (a
dead gauge query during a DB outage must alert, not report green);
duplicate-skip counters spike during DR replays and dashboards must
present that as healthy; every alert definition carries a runbook
link; correlation_id is propagated through MDC and outbound headers
so one id greps the whole story.


------

## 16. Engineering & Operational Requirements

### 16.1 Resiliency

```text
- Every external call has an explicit timeout from a per-dependency
  budget (enrichment, account service, engine POST, status-query
  API). No unbounded calls: a hung call holds Kafka partitions and
  triggers rebalance storms. Values live in externalized config
  (§16.6).
- Circuit breaker per dependency. Business rejects (e.g. insufficient
  funds) are SUCCESSES to the breaker — counting them as failures
  opens the breaker during normal operations.
- Scanners gate on breaker state before claiming a batch (an outage
  becomes quiet waiting, not a thundering herd), use jittered
  exponential backoff with a cap, and claim bounded batches
  (backpressure without leader election).
- Poison-row cap: a row that fails deterministically every scan cycle
  hits an attempt cap → BLOCKED + alert; the scanner never loops on
  it.
- Bulkheads: posting, enrichment, and card-read serving never share a
  thread pool; in-memory queues are bounded (the database is the
  queue; the in-memory layer is only a latency optimization).
- Global posting freeze: a toggle in Hazelcast (existing infra,
  deliberately OUTSIDE the payment database) — a database restore
  therefore cannot un-freeze posting (§5.2). Fail-safe default: if
  the toggle is absent or Hazelcast is unreachable, posting is
  DISABLED — a grid outage pauses payments (fail-blocked) rather
  than un-freezing them. PO SIGN-OFF: a grid outage halting
  all payments is accepted — same fail-blocked philosophy as
  everywhere else. Checked before every claim and every POST.
  The check itself has a bounded timeout; on timeout it reads FROZEN
  (a hung check must not hang posting — the exact pathology the
  per-dependency timeout rule targets). Only the FROZEN answer
  may be cached; "unfrozen" is always re-read (a TTL cache of
  "unfrozen" would violate fail-safe). The toggle carries reason,
  operator, and ticket id — deliberate freeze and infra
  failure must be distinguishable, and §15 pages when the freeze is
  EFFECTIVE without an acknowledged ticket. Propagation bound: a
  flip is effective cluster-wide within a stated interval (config
  §16.6); the §5.2 runbook waits it out before proceeding.
  OPERATIONAL FACT: toggle access is governed by SEPARATE
  ROLE CONTROL on the Hazelcast grid — authorized personnel can
  execute the flip today, without an ops console. The §20
  kill-switch question is about a dedicated audited surface, not
  about capability.
- Per-payment-type disables remain durable database config (they are
  operational levers, not DR-critical).
- A kill switch stops POSTs ONLY — inline and scanner alike. Feed
  consumption, §9 status queries, and card reads always continue:
  the post-restore runbook (§5.2) depends on the resolver running
  while posting is frozen, and consumption must never be stopped.
- Freeze/outage clock semantics (simplified by the 2026-07-11 retry
  bounds decision, §7.4): retry limits are max attempts + the
  attempt budget (round 10: max attempts only — the engine
  owns the cutoff calendar) — there is NO wall-clock retry deadline, so there
  is nothing to "suspend" and no outage bookkeeping to persist.
  While posting is frozen or a breaker is OPEN, gated scanners make
  zero attempts, so the attempt budget structurally cannot burn —
  a 6-hour engine outage leaves the RETRY_WAIT population exactly
  where it was, ready at recovery, with no BLOCKED flood.
  LINEARIZATION of the freeze check (2026-07-17, review 4d5cb83
  L2): "zero attempts" means zero WIRE calls. A worker that passed
  its pre-claim freeze read before the flip is IN FLIGHT — it may
  still commit its claim; the pre-wire re-check stops the wire
  call, the abandoned claim resolves via lease expiry, and the
  propagation bound + drain procedures own that boundary. A grid
  read cannot form an atomic fence with the Oracle claim, BY
  DECISION — a durable fencing token was considered and rejected
  as machinery that protects nothing money-real. (Round
  10: NO cutoff check exists at attempt time or anywhere else —
  the engine owns its calendar and classifies late submissions
  itself, CA-1.) An in-flight POST call at flip time completes
  (drain semantics, §11).
- In-process micro-retries are permitted ONLY for idempotent reads on
  provably-unsubmitted failures (e.g. enrichment lookups) — never on
  the payment POST. Durable retries (§7.4) are the single retry owner
  per operation; stacked retry layers are forbidden.
```

### 16.2 Kafka consumption (both inbound flows)

```text
- enable-auto-commit = false; ack after the listener returns
  (ack-mode = record); offsets commit only after the DB transaction
  commits. At-least-once end to end; idempotency absorbs duplicates.
- auto-offset-reset = earliest — 'latest' silently skips money events
  on any consumer-group change.
- ErrorHandlingDeserializer wrapping is mandatory — otherwise a
  poison pill throws inside poll and loops the consumer forever.
- DLT is for poison messages only (deserialization / semantic
  validation). Transient infra errors retry IN PLACE or pause the
  container — dead-lettering a money event breaks per-payment
  ordering and hides a financial fact. DLT depth > 0 pages someone;
  the replay tool preserves original keys.
- Non-blocking retry topics (@RetryableTopic-style) are forbidden for
  ordered money events — they reorder per-payment history.
- Partition keying: status feed by UETR; upstream flow by
  business_id (§6.0). If a topic is not usefully keyed, consume with
  concurrency 1 per partition; the ordering guards (§4.4, §6.7) are
  then the only protection.
- max.poll.interval.ms sized for worst-case obligation-lock
  contention; keep max-poll-records small.
- The retention chain (inbox_retention > kafka_retention ≥
  replay_window) has a NAMED OWNER, and a scheduled check compares
  actual broker topic retention against the required window and
  alerts on violation — broker retention is another team's config
  and can change without notice.
```

### 16.3 Security

```text
- Read surface (card): mTLS or OAuth2 client-credentials with a
  read-only scope. Lookup semantics per §12.
- Data sensitivity everywhere, not just exception text: debit_account
  masked in the read model, logs, and traces (masking applied in the
  logging encoder, not by call-site discipline). Stack traces stay in
  logs, keyed by correlation id.
- Instruction content is persisted locally ONLY in the §14.1
  attempt journal (2026-07-16 driver: the sent request is not
  otherwise visible to us) under §14.1's controlled-access rules —
  restricted audit role, DB-audited reads, encryption at rest per
  DBA standard, no lower-environment replication. EVERYWHERE ELSE
  the original rule stands: the payment tables store only
  last_sent_hash (a hash is not sensitive data); party/account data
  lives transiently in the posting path's memory and in the
  engine's records; no other PII surface exists.
- Secrets in a vault, rotatable without redeploy.
- Inbound feed authenticity: ACLs on the topics; message signing if
  the broker is shared.
```

### 16.4 Amount & time hygiene

```text
- Intake validates currency scale: 100.555 in a 2-decimal currency is
  a validation failure, never silently rounded. JPY (scale 0) and
  BHD/KWD (scale 3) must survive the pipeline end to end.
- All amount comparisons use BigDecimal.compareTo (never equals);
  the completion boundary is compareTo == 0.
- No tolerance: a confirmation amount must equal the request amount
  exactly — grounded in the confirmed contract fact that the payment
  engine settles all-or-nothing; any mismatch is a defect signal →
  BLOCKED (AMOUNT_MISMATCH) + CRITICAL alert (§8).
- All timestamps UTC; every due-time comparison uses database time,
  never application-node time. (The former local-cutoff timezone
  exception is RETIRED — round 10, the engine owns its calendar (§7.4, §18
  BLOCKING item) represents them timezone-aware — local time + zone
  id, DST-correct, per currency/market including holidays —
  converted to UTC at comparison time, never stored as fixed UTC
  constants (a fixed constant is wrong twice a year per market).
```

### 16.5 Deployment, capacity, contracts

```text
- Schema migrations are expand/contract (two app versions run
  concurrently during rollout; new columns nullable-with-default
  first, drops only after the old version is gone). Managed by
  Flyway/Liquibase from day one; enum/legality CHECK changes are
  migrations, not hotfixes. Claim semantics must remain
  version-compatible across one release boundary.
- Volume NFR (from the PO): peak ~3,000 trades/day → at most
  ~10,000 upstream messages/day, plus the matching feed volume.
  Pool math, partition counts, the §9.5 sweep budget, and the drift
  scan are sized against this; at this volume every mechanism in
  this document is comfortably within a single modest instance's
  capacity, and the §9.5 shaping exists for the post-outage burst
  case, not steady state.
- Capacity: the obligation lock serializes all activity per scope.
  Estimate peak per-scope transaction rate before build; any scope
  approaching ~10 sustained tx/s needs a design conversation (hot
  row). One pathological upstream flooding a single scope is also
  monitored (§15 per-obligation request count).
- Connection-pool math documented: listeners x concurrency + scanners
  + read serving <= pool size minus headroom.
- Three external contracts exist: the upstream message schema (§6.0),
  the engine API + status feed, and the card read contract. Each is
  enforced at build time (consumer-driven contract tests or schema
  registry) so a new engine status code fails a build, not on-call
  at 2 a.m. — runtime fail-closed classification (§7) remains the
  backstop.
- Enum evolution: adding a blocked_reason (or any CHECKed
  enum) value is an expand/contract migration — the Oracle CHECK is
  swapped (add NOVALIDATE, validate after), and the OLD app version
  still runs during rollout, so every enum READ is defensive: an
  unknown value maps to an UNKNOWN sentinel (contract-tested), never
  a naive Enum.valueOf, which would throw in every reader — queue
  metrics, §4.2 derivation, the card. The four dimension enums
  (stage, stage_state, submission_state, outcome) are CLOSED:
  extending one is a design change requiring a review round, not a
  migration.
```

### 16.6 Configuration inventory and required companion artifacts

Configuration inventory (normative once numbers land; owner column to
be filled at kickoff; every §15 alert names its consuming entry):

```text
NOT_FOUND_TRUST_AGE            §9.2   from engine ingest lag (§18)
confirmation age (SUBMITTED
  sweep)                       §9.5   suggest 15m — NOT yet in §18;
                                      needs an owner
MAYBE escalation max age       §9.3   suggested 30m (§18 PO-3);
                                      keyed on maybe_since
MAYBE tier-2 escalation age    §9.3   re-page/incident threshold
downgrade retry class          §7.4   next_retry_at = now, max
                                      attempts 2–3 (no deadline —
                                      attempts only, round 10)
validation reject alert count  §2.1   suggest 3 (alert only,
                                      no gate)
status-query cadence           §9     suggested 2m (§18 PO-2)
claim lease durations          §11    per stage
retry policy per error class   §7.4   base, multiplier, max
anchor / live-marker max age   §15    alert threshold
stale-message volume threshold §6.7   alert threshold
stale-marker-write volume      §6.9   alert threshold
unmatched-event volume         §8     alert threshold
per-(stage,stage_state) max
  ages                         §15    generic stuck alert
poison-row attempt cap         §16.1
breaker thresholds, timeouts   §16.1  per dependency
batch sizes                    §16.1
inbox purge retention          §2.3   > kafka retention ≥ replay
                                      window (§16.2 owner)
log retention                  §14    ≥ floor
max.poll.interval.ms           §16.2
resolver sweep query budget    §9.5   from the engine query-API rate
                                      limit (§18 tech-lead ask)
per-row query backoff          §9.5   next-query-at schedule
feed-lag damping threshold     §9.5   = confirmation age
provider_reference match
  recency window               §8     fail-closed fallback
freeze propagation bound       §16.1  cluster-wide flip latency
DR key-enumeration stop count  §5.2   FALLBACK only: the
                                      step-5b bound is log-derived;
                                      K consecutive NOT_FOUNDs
                                      applies only when the log
                                      platform is unavailable
                                      (post-MVP with the runbook)
```

Config-load validation: the loader REJECTS a configuration set
whose ordering is inconsistent —
`trust_age + query cadence < escalation age < tier-2 age`
(the former cutoff-margin ceiling is RETIRED — round 10). Nothing
else orders these values; a p99-driven
trust-age quietly reaching the escalation age would silently degrade
wait-then-decide into everything-goes-to-ops.

Required companion artifacts (each with a named owner; §16.5 contract
tests point at them):

```text
1. Engine error-code → classification table (§7 taxonomy, code by
   code). Must explicitly classify the replay-original-response
   class: an engine that answers a same-key-same-payload
   re-POST by replaying the ORIGINAL response, with no DUPLICATE
   code — money-safe by fail-closed (unmapped → UNMAPPED_CODE →
   resolver) but it must be classified deliberately, not discovered.
2. Engine status vocabulary: full enum, precedence/evidence mapping
   (§4.4), feed event schema (event_id, UETR, status, amount,
   provider_reference — names and types). Must state whether
   the engine emits feed events under the UETR of a REJECTED
   duplicate/collision submission; if yes, such events shall never
   apply as authoritative negatives (§5 — foreclosed anyway by
   never persisting those UETRs).
3. Status-query response → §9.1 outcome mapping (incl. the decided
   rule: acceptance answers promote submission_state to SUBMITTED).
4. Full DDL migration set (Flyway), including the exact I6 index
   expression, the L1-shape/L2–L8 CHECK constraints, and the L1
   freeze + release-guard trigger backstops (§10.3) — PLUS a
   normative index list: one index per standing scan (resolver
   sweep, retry scanner, escalation scanner, BLOCKED queue,
   stuck-state, drift, the §5.2 created_at window), each
   ACTIVE-ROW-BOUNDED via the I6 function-index trick (expressions
   NULL for terminal rows), so every scheduled scan's plan is
   independent of terminal-row count.
5. Identity-derivation spec + golden vectors (§5.1), extended
   with the canonical instruction serialization + hash definition
   used by last_sent_hash (§7.0) — same byte-exactness discipline,
   so hash comparisons across attempts and DR replays are meaningful.
6. Test catalog aligned to this document (seed from
   design-review-v3 §3 + the v5/v7 interleavings). Includes:
   a §9.2 downgrade re-POST answered DUPLICATE_REQUEST leaves the
   prior `uetr` value (or NULL) intact. MANDATORY additions
   (2026-07-11 external-review fold): (a) crash-point tests at
   every commit/external-call boundary — before claim commit;
   unknown claim commit; after claim commit before POST; during
   POST; after engine acceptance before response persistence;
   after DB commit before Kafka ack; during snapshot fan-out;
   during validation-marker fan-out; (b) a property-based sweep of
   the §10.3 legality matrix (every illegal tuple write refused);
   (c) the §11 claim-protocol concurrency/deadlock test on real
   Oracle (scanner vs feed vs auto-cancel interleavings — no
   lock-order inversion, no ORA-00060); (d) reprocess-snapshot
   adversarial set (§20-10 rounds 3–4): non-tying document → no
   relaxation (ordinary guard only); document business_id ≠
   addressed trade → refused; re-run after apply → no-op (single
   use); purged/missing id → clean refusal, no partial apply;
   content changed behind an id (ask-8 violation simulated) →
   HARD REFUSAL + alert on the §9.3 digest mismatch, BEFORE any
   lock (round 4 — never merely "inside the ordering guard");
   plus the §20-10 mixed-snapshot per-block set: one changed tied
   block + one identical tied block + one new block + one
   already-newer obligation + one absent obligation (round 10:
   asserts the PO-9 amendment-to-zero consequences — cancel /
   wait / latch) +
   trade-reference-only difference (round 5: converges via the
   admission update — re-run digest-equal, no-op) +
   crash-mid-reprocess re-run under a NEW approval (round 5:
   consumed approval refused; new approval applies only the
   remainder); (e) dual-control negative set (§9.3): parameter
   substitution, expired approval, replayed consumed approval,
   identical identities, role revoked between approval and
   execution, digest mismatch, concurrent double-execution
   (exactly one CONSUMED CAS wins), mid-transaction failure
   (approval survives unconsumed — single-transition atomicity),
   crash-after-consume-before-fan-out (reprocess consume-at-start:
   approval burned, nothing applied, NEW approval succeeds);
   (f) admission-gate set (§6.1/§2.4, rounds 5–6): the
   never-seen-scope trace (newer snapshot without B commits first;
   delayed older snapshot containing B is refused whole — B and
   its request are NEVER created); two disjoint first snapshots
   serialize on the trade row (both scopes exist afterwards, one
   ordering wins the row); a failed-validation message advances
   neither watermark; round-6 fence set: pause a worker AFTER
   admission and AFTER block 1, admit a newer snapshot, resume —
   the paused worker's next block ABORTS on the trade-snapshot
   fence and creates nothing (abandoned blocks logged + counted,
   round 7); kill the paused worker — redelivery/alert
   recovers; zombie consumer re-applying an already-converged
   document → all no-ops. (The former bootstrap/digest-NULL set was REMOVED round 10 —
   greenfield fact, §2.4.)
7. Runbook stubs, one per §15 alert; the §5.2 restore runbook; the
   unqueryable aged MAYBE row (past the engine lookback — §9.3): platform-side lookup → TL-10 rejection or the
   apply-platform-verified-outcome operation.
8. The apply-platform-verified-outcome OPERATION spec
   (§9.3 — §18 BLOCKING item 3; execution boundary decided
   2026-07-11: an authorized, enterprise-authenticated endpoint of
   the payment application calling the shared transition service —
   never a PL/SQL reimplementation; §10.3 triggers stay as the DB
   backstop): endpoint authorization + operation contract —
   EXECUTION INPUT IS THE approval_id (round 4: identities are
   derived from the approval record, NEVER passed as parameters) —
   the §9.3 two-step approval workflow (approval-record schema +
   PENDING→APPROVED→CONSUMED state machine with version/nonce
   uniqueness; binding fields incl. the reprocess content digest;
   approver ≠ initiator; consumption semantics PER OPERATION CLASS
   (round 5): single-transition → the CONSUMED CAS and the payment
   transition commit in ONE transaction/session; reprocess-snapshot
   → CONSUME-AT-START after the digest check, crash remedied by a
   NEW approval — §9.3), with
   the full §9.3 negative-test set (substitution, expiry, replay,
   identical identities, revoked role, digest mismatch, concurrent
   double-execution, mid-transaction failure) — evidence-flag
   mechanics, refusal conditions (CLAIMED, terminal, amount
   mismatch), audit fields, and the ops drill script. The
   signed-assertion alternative is documented but GATED (explicit
   decision required; agent-facing cards specify the workflow).
```

------

## 17. Core Requirements Summary

```text
Money derives status
=
request CAS row count gates amount updates; step status, the active
exception, and the next actor are derived under the obligation lock,
never copied from events, never accumulated

One column per fact
=
stage / stage_state / submission_state / outcome are independent;
the old 13-value status survives only as a display label no rule may
key on

committed_amount
=
reservation: money spoken for, not money at the engine

Failure direction
=
fail-blocked with an alarm (reservation held + live condition +
alert), never pay-twice, never fail silently

Identity
=
deterministic to the byte, persisted before POST; retries reuse it;
no fresh keys, ever — deterministic identity keeps a restore
RECOVERABLE (re-creations collide at the engine instead of paying
twice); FULL restore recovery is the §5.2 runbook, post-MVP — until
it exists, a restore is a major incident (PO decision, §5.2)

Request creation
=
one standing re-evaluation point (§6.8) with a normative trigger
inventory; deferred, never lost

Ordering
=
one staleness guard per mutable input — reads and writes (§6.9)

Ambiguous outcomes
=
resolved by asking, never by retrying; release rights move only on
first-party evidence (§9.4)

Retryable exception
≠
terminal payment failure

The card
=
display only; reads current state; never a source of truth

This system
=
payment orchestrator; current payment state + the §14 transition
log (restore-surviving, external) + the §14.1 attempt-content
journal (team-internal audit, never load-bearing); execution truth
lives in the payment platform
```

------

## 18. Open Items

### BLOCKING — must be answered before implementation

```text
0. Snapshot-contract residue (the multi-payment snapshot model
   itself is a §1 contract fact; these are its open edges, blocking
   before the §6 implementation freeze):
     a. WRITTEN upstream confirmation of the snapshot schema and the
        within-snapshot uniqueness guarantee (upstream ask 5) — the
        cross-snapshot half of the identity ("equal tuple = same
        payment") is unverifiable at runtime and rests entirely on
        this contract.
     b. The within-snapshot uniqueness intake validation (§6.0)
        implemented — the runtime-checkable half.
     c. CLOSED 2026-07-11: PO-9 ANSWERED (absence = amendment to
        zero; BA-2 amended §1.1) and TL-16 ANSWERED round 5 (§6.1
        admission + §2.4). Nothing in 0(c) remains open.
     d. Upstream ask 8 IN WRITING (added round 4 — elevated from
        the ask list because intake itself fetches by id and the
        NON-WAIVABLE reprocess-snapshot operation depends on it):
        sanctioned fetch-by-id, stable unique versioned ids,
        consistent reads, IMMUTABILITY (corrections = new
        id/version), retention ≥ the ops/tie SLA. Gates IN-01/
        IN-02 with the rest of this item; go-live Q1 verifies it.
1. Engine idempotency-collision contract —
   PROVEN by a sandbox test, not asked: executed before go-live and
   re-run on engine releases. Test matrix:
     a. re-POST a known key with an IDENTICAL payload → deduped /
        acked (or original response replayed — the artifact-1
        class); nothing executes.
     b. re-POST a known key with a DIFFERENT payload → rejected
        without execution, with a code distinguishable from plain
        DUPLICATE_REQUEST (TL-4).
     c. the engine states its KEY-RETENTION TTL in writing, and
        (a)/(b) are verified at the retention edge — the §9.2
        re-POST lane is precisely the DELAYED one (parked days,
        escalated); a key aged out of the dedup store executes a
        duplicate. If TTL < max row lifetime (incl. ops-queue SLA),
        re-POSTs past the TTL are forbidden — repost_permitted
        (§7.0) gains a TTL term — and such rows are ops-only.
        DECISION HYGIENE (round 9): the TTL outcome is recorded as
        a NAMED design decision here and in §7.0/§16.6 by the
        design owner BEFORE any implementation card acts on it —
        an implementation card (RC-03) implements the resulting
        FIXED predicate; it never mutates the canonical safety
        predicate on its own initiative.
     d. re-POST of a key the engine synchronously REJECTED earlier
        (business reject) → settles TL-6's working assumption
        (re-executes vs replays the rejection); either answer is
        handled (§7.1), but it must be KNOWN, not assumed.
   The behaviors under test are the §1 ASSUMED contract facts: the design assumes them and carries no runtime gating —
   this test is the PROOF, and it blocks GO-LIVE, not runtime
   behavior. Because nothing is live until it passes, TL-4's
   revert-to-payload-freeze clause remains executable while it
   matters.
2. CLOSED 2026-07-11 (PO answer): the PAYMENT ENGINE owns its own
   cutoff calendar (engine-owned, round 10) — this system initiates at any time and carries
   NO local calendar, cutoff gate, or cutoff config. A late
   submission returns as an ordinary engine response, classified
   per CA-1. Residual ask (folded into the CA-1/Q-08 provider
   asks): the engine confirms IN WRITING that submission is
   accepted at any time, and names the late-submission response
   code (if one exists) for the CA-1 table. CUTOFF_EXPIRED stays
   RESERVED in the blocked_reason enum (§2.2).
3. MVP MAYBE-row terminal exit: the §9.3
   apply-platform-verified-outcome audited operation (an authorized
   application endpoint — execution boundary decided 2026-07-11;
   §16.6 artifact 8) must EXIST AND BE DRILLED before go-live —
   OR TL-10 (platform formal reject) AND TL-5's lookback ≥ maximum
   row lifetime (incl. ops-queue SLA) are both answered
   affirmatively. Without one of these, an unresolvable
   MAYBE_SUBMITTED row (repost_permitted permanently false — stale
   amount or divergent payload — plus a key aged
   past the query lookback) holds its reservation FOREVER: the
   scope can never complete (§4.1) and I6 blocks any successor.
```

### Requiring PO review

```text
1. Approval of the ask-then-retry model on the engine status-query
   API (§9).
2. Status-query cadence (suggested: every 2 minutes).
3. Maximum MAYBE_SUBMITTED age before BLOCKED escalation (suggested:
   30 minutes).
4. CLOSED 2026-07-11 (calendar answer, §18-2): no local cutoff
   exists to "pass"; a late submission is the engine's ordinary
   business response.
5. Step display for a trade cancelled after the payment step started
   (currently "completed" — acceptable?).
6. Deferred successor creation (§6.8): when an amendment increases
   required_amount while a payment request is in flight, the delta is
   paid AFTER that request resolves (typically minutes), not in
   parallel. Benefits: preserves the one-active-request DB constraint
   (I6) — the strongest backstop in the schema, converting any
   double-payment bug into a loud constraint violation instead of
   silent duplicate money — and keeps exception display, auto-cancel,
   and retry logic single-request simple. Cost: the deferral latency.
   PO to confirm the latency is acceptable.
7. Ops retry-after-provider-reject (§19.3, FUTURE): approve the
   concept and policy — after a definitive engine reject, may ops
   (4-eyes) authorize a fresh payment attempt without waiting for an
   upstream correction? Benefit: a recovery path for rejects caused
   by transient engine-side conditions (e.g. beneficiary account
   temporarily blocked) that upstream cannot fix by re-sending the
   same data. Until approved and built, such scopes wait on upstream
   (or on the §6.8 one-newer-message attempt).
8. Fresh-assembly consequence (§7.0): a details correction (e.g.
   agent bank) after launch is carried by the next same-key re-POST
   and takes effect IFF the original payment never actually arrived;
   if the payment already executed, it paid the details current at
   its execution, and correcting THAT is a platform-side recall
   (§19.2 family) — like any executed payment. Confirm the business
   accepts this (it is the physical reality of payments; the
   alternative, freezing the payload, was considered and REJECTED —
   it pays with stale details in the never-arrived case).
9. Absence semantics under the snapshot model (§6.1 — a BA-2
   AMENDMENT question, hence the PO's alone): when a payment that
   exists as an obligation is ABSENT from a newer snapshot, does
   that mean "this payment no longer exists" (→ amendment to zero:
   auto-cancel if provably unsent per §6.4; wait-then-decide if it
   may have been sent), or "unchanged"? Overwrite semantics argue
   for "cancelled"; BA-2 currently records that no cancellation
   signal exists. Risk either way: "cancelled" lets a producer bug
   (accidentally dropped block) cancel real unsent payments;
   "unchanged" leaves a genuinely-removed payment paying.
   ANSWERED BY THE PO 2026-07-11: absence = CANCELLED — an
   amendment to zero (BA-2 amended, §1.1; §6.1 RESOLVED block).
   Unsent → §6.4 auto-cancel; in-flight → wait-then-decide;
   already-paid → the §6.5 overpay latch, WE STOP (the PO's "same
   as overpay"). The round-7 RIDER is satisfied: the PO's answer
   RATIFIES the §6.1 BLOCK-LEVEL SUPERSESSION rule (relayed with
   the PO-9 answer by the design owner, 2026-07-11).
   Round 11: lifecycle COMPLETED — §4.1 CANCELLED terminal branch;
   0..N derived payment set (§1 role derivation, PO 2026-07-12);
   ordering-aware anchor retirement; watermark advance (§6.1).
```

### Requiring tech lead review

```text
1. Confirm the status feed carries a stable, unique event_id per
   event; otherwise choose the synthesis strategy (payload hash vs
   topic+partition+offset) and accept its dedup blind spots (§8).
2. Read contract for the card: query API vs read replica/view, field
   list (candidates: step started/completed
   timestamps; retry progress "next attempt at / attempt N of M" for
   retryable exceptions), freshness, authentication, volume
   (§12). Also (multi-payment, §12): step granularity — one step per
   PAYMENT, or one rolled-up step per TRADE? The §4 derivations are
   per obligation; a rollup is a display aggregation this contract
   must define — round 12: the answer must specify the FULL
   aggregation table over {NOT_STARTED, IN_PROGRESS, COMPLETED,
   CANCELLED} + exceptions (mixed completed/cancelled, all-cancelled,
   empty-derived-set trade = row absence); trade-cancel (PO-5) and
   payment-removed (§4.1 CANCELLED) are different concepts unless
   the PO equates them.
3. RPO/RTO sign-off for the database, and ownership of the
   post-restore runbook (§5.2). The deterministic-key rule stands
   regardless; the runbook's urgency depends on these numbers.
4. Engine contract — LOAD-BEARING: exact behavior when
   a submission reuses a known idempotency key with a DIFFERENT
   payload (§5.1, §7.0, §7.2). REQUIRED guarantee: such a submission
   is NEVER executed as a new payment (rejected with a distinct
   code, or deduped) — the §7.0 fresh-assembly rule (details
   re-resolved on every re-POST) stands on this guarantee; if the
   engine keyed idempotency on key+payload composite, a re-assembled
   re-POST would be a double-pay path and §7.0 must revert to a
   payload freeze. Also confirm the collision code is distinguishable
   from a plain DUPLICATE_REQUEST. (answered ONLY by the
   BLOCKING item 1 sandbox test — a written yes does not close
   this item.)
5. Engine contract: maximum ingest lag between POST acceptance and
   status-query visibility (sets NOT_FOUND_TRUST_AGE, §9.2) — asked
   as a DISTRIBUTION (p50/p99/max), not a single number; if no
   contractual bound exists, trust-age is set conservatively; the
   residual lag risk is carried by the §1 assumed collision contract
   (a lag-premature re-POST dedupes or rejects) and surfaced
   by the §15 observed-lag watchdog. And the
   query lookback window: it must be ≥ the maximum ROW LIFETIME
   including the ops-queue SLA, not merely the escalation age (parked rows live days; past the lookback, NOT_FOUND is
   unfalsifiable and the §9.3 resolve-via-query action can never
   succeed, holding the reservation forever). The §9.4 invariant
   protects release rights regardless; on the SUBMITTED branch a
   lag-caused false ENGINE_INCONSISTENCY park is reversible — the
   row stays in resolver scope and the next successful query
   resolves it.
6. Payment-team confirmation: after a synchronous business rejection,
   does a same-key re-POST re-execute, or does the engine replay the
   cached rejection? Working assumption in §7.1: RE-EXECUTES. If the
   engine instead replays the rejection, retries of that error class
   are no-ops and its retry policy must change: fresh successor via
   §6.8 instead of same-key retry. (answered ONLY by BLOCKING
   item 1's test case (d) — not by a written yes.)
7. Key-only anchoring (§6.6, recommended improvement): confirm
   upstream guarantees business_id as the Kafka message key (upstream
   ask 2), then schedule the implementation.
8. Owner + suggested value for the SUBMITTED-sweep confirmation age
   (§9.5, §16.6) — the one tunable not previously tracked.
9. Companion artifacts (§16.6): assign owners for the engine
   error-code table, status vocabulary/rank mapping, and query-
   response mapping.
10. Payment-platform ask: can the platform formally REJECT a
    pending or never-received payment by UETR on request (or by
    idempotency key / end_to_end_id for rows that never received a
    UETR — the SDK mints the UETR in the POST call, so exactly
    the never-received candidates may lack one), so the
    negative flows back through the feed/status-query as
    authoritative evidence? This is the clean ops exit for parked
    MAYBE rows (§9.2/§9.3 wait-then-decide); without it, ops's only
    exits are the dual-control stale-amount re-POST (§7.0) or
    waiting out the engine's own resolution. (the §9.3
    apply-platform-verified-outcome operation exists at MVP
    regardless — §18 BLOCKING item 3; TL-10 remains the CLEANER
    path because its negative arrives through the normal feed/query
    evidence machinery with no human transport.)
11. SDK contract (UETR is SDK-generated — PO fact, §5):
    confirm (a) the SDK's validate-and-POST response RETURNS the
    generated UETR so it can be persisted for feed matching — and
    in which field; (b) the SDK accepts our CALLER-SUPPLIED
    idempotency key (the §5.1 keystone assumes it); (c) ABOVE ALL,
    engine duplicate detection keys on that idempotency key, not
    the UETR — a re-POST of the same key may carry a fresh
    SDK-minted UETR, so key-based dedup is blocking-grade.
12. provider_reference: uniqueness scope and lifetime
    (global? per day? per batch or rail?). Until confirmed globally
    unique, §8's fail-closed fallback rule applies; if confirmed,
    the extra guards may be relaxed by explicit decision.
13. Status-query API rate limit / quota, sizing the §9.5
    sweep budget — as load-bearing as the ingest-lag number.
14. Terminal-row retention/archival, co-designed with the
    money invariants: at archival each obligation gains an
    archived-confirmed rollup and I1/I2 become rollup + Σ live rows;
    the §5.2 created_at query window must survive archival. (§2.2's
    terminal-time convention is the enabler.) Round 6: the
    trade_snapshot_state row archives WITH its trade.
15. Production measurement, first quarter: NOT_FOUND-after-
    trust-age frequency — how often the §9.2 downgrade would fire —
    to revisit auto vs ops-triggered enablement with data (§9.2).
16. ANSWERED 2026-07-11 (round 5, design owner): option (b),
    STRENGTHENED — trade_snapshot_state (§2.4) + the §6.1 admission
    gate. The round-5 review exposed the sharper failure the
    original options missed: neither (a) nor per-obligation
    watermarks can stop a delayed older snapshot from CREATING a
    never-seen scope (no row → no watermark → the first-message
    path pays a payment the newer authoritative snapshot says does
    not exist). A document older than the trade watermark is now
    refused WHOLE at admission and can never create a scope;
    disjoint concurrent first snapshots serialize on the trade row.
    Option (c) was rejected — it contradicts the stated
    out-of-order condition (§6.7's own motivating trace is a late
    original). (PO-9 was later ANSWERED 2026-07-11: absence =
    amendment to zero, §6.1; round 11 — the zeroing write ADVANCES
    the per-obligation watermark, superseding the round-5 no-write
    detail.)
```

### Upstream contract asks

```text
1. Strictly increasing ordering value per business_id (until the
   explicit sequence field arrives) — the §6.7 tie-conflict alert
   exists because this is not yet guaranteed.
2. business_id as the Kafka message key, guaranteed by contract (not
   just as a partitioning convention) — §6.0; prerequisite for §6.6
   key-only anchoring.
3. The §6.0 message schema formalized (field names — including the
   ordering field — types, the correlation_id, and the payment-block
   list structure of the snapshot).
4. Emission contract: confirm a new message is emitted ONLY
   when a business field changed — no blind re-emissions of
   identical snapshots (§6.0 contract fact; bounds the validation
   reject cycle, §2.1). HONESTY NOTE (round 11 — this ask is NOT
   a complete guard): ask 4 prevents blind RE-emissions, but a
   producer can emit only on real changes and still serialize an
   INCOMPLETE payment set, and no consumer check can distinguish
   "intentionally removed" from "accidentally omitted." The
   controls for that class are: the ask-5 COMPLETE-SET guarantee
   (below), the §15 disappearance alert (every absence-zeroing
   fan-out is logged + counted, so a removal is VISIBLE, not
   silent — P5), §6.4 touching only provably-unsent requests
   (money never moves wrongly), and reappearance (§6.1 — a
   corrected snapshot reopens the payment). Residual risk owner:
   the PO accepted absence-as-cancellation knowing this class
   (PO-9); if upstream cannot give the complete-set guarantee in
   writing, the PO must re-confirm or fund an independent
   reconciliation feed (recorded as OPTIONAL, not built by
   default).
5. CONFIRMED verbally 2026-07-11 (design-owner relay) — the
   WRITTEN document remains the go-live evidence (Q1).
   Within-snapshot uniqueness IN WRITING: no two payment blocks in
   one snapshot share (payment_type + debit_account + currency),
   and an equal tuple across snapshots always denotes the SAME
   payment (§1 contract facts, §18 BLOCKING item 0a). We validate
   the within-snapshot half at intake (§6.0); the cross-snapshot
   half is unverifiable at runtime and rests on this written
   contract alone. ROUND-11 ADDITION (goes in the SAME written
   filing): each snapshot carries the COMPLETE current settlement
   set of the trade — a GUARANTEE, not "usually" (absence is now
   the cancellation signal, BA-2; an incomplete serialization
   cancels real payments, H-1 round 11).
6. Scope-key provenance IN WRITING: payment_type, debit_account,
   and currency are carried IN the message as stable identifiers
   (§6.0) — none of them is derived by this system via any external
   lookup before intake. The §2.1 scope key and the §5.1
   deterministic idempotency key are computed from message fields
   only; enrichment (§7.0/§7.3) resolves settlement DETAILS from
   them on a durable request row, never the identity itself. If any
   scope field required a failable pre-intake lookup, obligation
   creation — and with it every table-driven retry — would inherit
   that call's availability, and key determinism across a restore
   (§5.1) would break. One written sentence forecloses the whole
   failure class (raised by the PO's account-resolution question;
   see failure-recovery-walkthrough.md GAP-1).
7. Emission monitoring: a trade that reaches the payment step but
   whose message is never PRODUCED or never DELIVERED is invisible
   to this system — no row, no anchor, no alert; the card's
   NOT_STARTED is indistinguishable from "not yet due" (§12). This
   is the single lost-payment class with no local detector
   (failure-recovery-walkthrough.md GAP-2). Ask upstream to confirm
   emit-failure monitoring on their side (emitted-vs-acked, DLQ
   alerts on the producer path) — or, failing that, to provide a
   periodic trade-count signal this system's reconciliation can
   check. No local machinery is proposed: the detector belongs
   where the data is.
8. CONFIRMED verbally 2026-07-11 (design-owner relay) — the
   WRITTEN document remains the go-live evidence (Q1). XML snapshot
   store contract (§6.0 transport note — added
   2026-07-11; IMMUTABILITY clause added round 3): confirm IN
   WRITING (a) fetch-by-id from the store by this service is a
   sanctioned interface (not an internal we happen to reach);
   (b) the storage id in the Kafka notification is stable and
   unique per snapshot; (c) store retention ≥ the maximum
   ops-queue / tie-adjudication SLA — the §20-10
   reprocess-snapshot operation re-fetches by id, potentially days
   later; a purged row makes tie resolution and DLT reprocessing
   impossible; (d) IMMUTABILITY: read-by-id returns the SAME bytes
   forever (or the id embeds an immutable version), reads are
   consistent, and any correction is a NEW id/version with a new
   notification — content behind an existing id never changes.
   THIS ASK IS PART OF §18 BLOCKING ITEM 0(d) (round 4): the
   ordering guard alone proves a fetched document is a valid
   tie/newer snapshot, NOT that it is the content the approvers
   reviewed — that provenance comes from the §9.3 digest binding
   (approval-time digest, re-verified at execution, mismatch =
   hard refusal + alert), and (d) is what makes "re-read the SAME
   document" literally true.
   Optional future improvement (not required): an on-request
   re-emission capability (fresh notification, fresh ordering
   value) would let ties resolve through the fully ordinary path
   with no ordering relaxation at all.
9. WITHDRAWN 2026-07-11: greenfield (PO — this flow is a new
   feature; no pre-existing trades, nothing to bootstrap or
   export; §2.4 GREENFIELD FACT).
```

### Resolved: workflow advancement

The payment step is the LAST step of the process; today no downstream
system consumes a completion signal from this system. Nothing blocks
the pull-only model. See §19.1 for the anticipated future need.

------

## 19. Future Work (documented, not scheduled)

### 19.1 Outbound completion signal to the reconciliation system

Today the payment step is the LAST step of the process and no
downstream system consumes a signal from us. In the future, this
system may need to inform the system performing the actual
reconciliation when the payment step completes or fails. When that
need materializes, the design shall be a minimal push channel —
deliberately NOT a general notification subsystem:

```text
- Exactly one event type: terminal step outcome
  (completed / failed-with-category), evaluated from the §4
  derivation, never from a raw feed event.
- Durable state on the obligation row: signal_status,
  signal_attempt_count, signal_next_retry_at.
- Delivery: at-least-once, post-commit send with a recovery scanner
  as the guarantee; per-obligation serialization via CAS claim.
- Idempotency key: deterministic —
  scope key + ui_step_instance_id (+ reopen cycle if §6.5 reopening
  must re-signal).
- Failure to signal never blocks or rolls back payment processing.
- Reopening (§6.5) after a sent signal requires a contract decision
  with the consumer: re-signal, compensating event, or ignore.
```

This is the single revival of the earlier push machinery that the pull
model (§1) made unnecessary; anything beyond one terminal signal per
obligation should reopen the outbox discussion rather than grow this
mechanism incrementally.

### 19.2 Returned funds and reconciliation visibility

In theory, any overpaid amount eventually returns to our account.
This system has no visibility into that flow: there is no return
status, no operation decrements `confirmed_amount`, and unexpected
return-style feed events are logged + CRITICAL-alerted + acked, with
no state change (§8).

Current rule (decided): once overpay is latched on the row, this
trade's payment is ignored moving forward (§13).

Decided policy (money-truth divergence, CRITICAL): whenever reality
and the state model disagree — a CANCELLED request that actually
executed (§5.2 replay divergence), an amount mismatch (§8), a
returned payment — the case parks as a CRITICAL incident and is
reconciled in the payment platform, which owns the authoritative
record. This system's counters are corrected only via the future
manual-adjustment operation; §10 deliberately does not model these
repairs.

Future conversation, owned by the reconciliation workstream:

```text
- How are returned funds detected (account statement matching?
  a reconciliation system feed?) and by whom?
- Should this system be informed at all? If yes, it needs: a
  RETURNED outcome, a confirmed_amount decrement operation under the
  obligation lock (preserving I1-I3), and reopening semantics — none
  of which exist today by design.
- Where does the money-level truth of a return live: here, or only
  in the payment platform / reconciliation system?
```

### 19.3 Ops retry-after-provider-reject (FUTURE, pending PO approval)

When a submitted payment was definitively rejected by the engine
(provider_rejected marker live, §2.1/§8) and the business decides to
attempt payment again without an upstream correction, an ops operation
shall exist that:

```text
1. Records the decision (operator identity, reason, ticket reference —
   §20 audit rules) and clears the provider_rejected marker.
2. Lets §6.8's standing re-evaluation create a FRESH successor request
   (next_request_seq, new deterministic key) — correct here,
   because the engine definitively rejected the original key's
   payment, making a new attempt a genuinely new payment, not a
   duplicate.
3. Is a 4-eyes operation (it initiates a money movement).
```

This is consistent with §10.1: the original request's reservation
release was driven by an authoritative engine negative. Until this
operation exists, recovery from an engine reject is a corrected
upstream message (one §6.8 ordering-newer attempt) only.

------

## 20. Manual Operations — Open Questions (PO discussion needed · future implementation)

Interim model (today): operations users read payment state from the
card, then go to other systems to check details and make payments.
This system offers no mutation surface to ops.

PO DECISION: shipping the MVP without an ops CONSOLE is ACCEPTED.
Dead-end states are exited in the interim via CONTROLLED, AUTHORIZED
ADMIN OPERATIONS — endpoints of the payment APPLICATION itself
(execution boundary decided 2026-07-11: enterprise-authenticated,
restricted-role endpoints invoking the same shared transition
service as the orchestrator; a PL/SQL implementation was rejected —
it cannot reuse the shared CAS/derivation helpers, check the freeze,
emit §14/§15 telemetry, or verify enterprise identities. The §10.1
release guard applies in code AND the §10.3 trigger backstops still
make a raw fat-finger DB write fail loudly). The console remains
future work per `ops-console-proposal.md` — it will only ever be a
UI over these same operations. The §9.3
apply-platform-verified-outcome operation is the §18 BLOCKING
item-3 gate (the guaranteed terminal exit for
otherwise-unresolvable MAYBE rows); together with supersede/close
and reprocess-snapshot it forms the NON-WAIVABLE minimal exit set
below (round-4 normalization — the earlier "exactly one
non-waivable operation" phrasing described only the §18-3 gate;
there are THREE non-waivable operations, one of which is
additionally a §18 BLOCKING item).

NON-WAIVABLE MINIMAL EXIT SET (round-3 normalization, 2026-07-11 —
resolves the contradiction between §3's "required feature" and a
waivable Q29): THREE operations must exist before go-live and are
not risk-acceptable — (1) the §9.3 verified-outcome operation
(§18-3, above), (2) SUPERSEDE/CLOSE (§3 explicitly REQUIRES it —
without it a stalled NOT_SUBMITTED request holds its reservation
forever), and (3) REPROCESS-SNAPSHOT (item 10 — the only §6.7 tie
exit, server-verified). The go-live checklist marks these within
Q4/Q29 as non-waivable line items.

The remaining interim OPERATION SET — ordinary MVP scope (Q29;
like every non-§18 item it may be risk-accepted only by the PO
with a named owner and dated plan): ops retry of a BLOCKED request
(item 1, L7 semantics), ops reject of a BLOCKED request (item 1;
release guard + L9 marker), overpay annotation (item 4), and the
four queue views. Every operation
requires operator identity, reason, and the external ticket
reference in its CONTRACT (item 8), plus a second distinct approver
where the action moves or releases money; all run the same guarded
CAS + obligation-lock flow as the orchestrator. Alongside them,
read-only QUEUE VIEWS over the §15 ops-queue metrics (BLOCKED by
reason with ESCALATED first, stuck reservations, aged MAYBE,
overpay latches) make the dead-end states findable — the card (§12)
is a user surface keyed by business_id and does not serve this.

Exit honesty (wording fixed 2026-07-11; scoped round 3; NARROWED
round 4): the exit GUARANTEE covers exactly THREE dead-end
classes — (1) MAYBE/SUBMITTED rows → verified-outcome; (2)
provably-unsent ACTIVE requests → supersede/close (and reject);
(3) snapshot ties → reprocess-snapshot — where "exit" may be a
terminal GIVE-UP (reject/supersede release the reservation and
close the scope's question). Everything else is a documented STOP
STATE, deliberately WITHOUT a current exit: a scope whose
provider_rejected marker is live with NO active request waits on a
strictly-newer valid message or the FUTURE §19.3/O11 clear; an
overpay-latched scope is a one-way door (§13) resolved
platform-side (§19.2). Considered and REJECTED (round 4): an
obligation-level terminal/give-up state for marker-only and
latched scopes — new state-model machinery whose only yield is
renaming a documented stop state; the markers and latch already
say precisely what is being waited on, and the §15 marker-age /
latch alerts keep them visible. The waivable operations are
ergonomics, not the guarantee.

The design, however, produces states that REQUIRE a database mutation
to leave — and today no tool can execute them. These questions need a
PO discussion and, eventually, an implementation (a proposed ops
console design is documented separately in `ops-console-proposal.md`
/ `ops-console-proposal.html`; note its O4/O5 parked-event operations
are obsolete, and its state displays should use the §10.4 labels):

```text
1. BLOCKED resolution: who decides retry / reject / supersede per
   blocked_reason, and what tool executes the transition? (Each is a
   CAS with money effects — reject/supersede release the
   reservation; all subject to the §10.1 release guard.)
2. Stalled-reservation release: §3 makes the supersede/close
   operation a REQUIRED feature; without a surface, a stuck request
   permanently blocks re-payment and completion of its scope.
3. Aged MAYBE_SUBMITTED requests: may ops trigger the status-query
   resolver on demand, ahead of its schedule?
4. Overpay latch: per §13 the trade is ignored forever — does ops
   need to annotate/acknowledge it on the card? (Clearing the latch
   is FUTURE, tied to §19.2.)
5. Returned-funds adjustment: FUTURE, blocked on §19.2.
6. Kill-switch operation: flipping the Hazelcast posting freeze
   (§16.1) via a dedicated, audited surface. (fact: separate
   role control over the toggle already exists — authorized
   personnel can execute the flip today; this item is about a nicer
   surface, not raw capability. §5.2 is post-MVP regardless.)
7. Authorization model: which roles may view vs act; 4-eyes approval
   for any action that moves amounts or releases reservations?
8. Audit: with no local journal (§14), every manual action must be
   logged with operator identity AND carry a mandatory external
   ticket reference — the ticket trail is the only record that
   survives a database restore.
9. Retry-after-provider-reject (§19.3): 4-eyes operation clearing the
   provider_rejected marker so §6.8 creates a fresh successor.
   Pending PO approval (§18 item 7).
10. Tie resolution (§6.7, REVISED 2026-07-11 round 3 —
    server-verified; round 4 — digest-bound approval + per-block
    algorithm): the REPROCESS-SNAPSHOT operation — trade-level,
    4-eyes ALWAYS via the §9.3 approval workflow (it can initiate
    money movement via §6.8). Approval time: fetch + validate the
    snapshot, compute the canonical business-payload digest, bind
    it into the approval (§9.3 — the approvers authorize CONTENT,
    not an opaque id; the approver sees digest + masked diff).
    Execution input = the approval_id; the operation re-fetches,
    recomputes the digest, and REFUSES on mismatch (hard refusal +
    alert) BEFORE any consumption or lock; then consumes the
    approval (CONSUME-AT-START — §9.3 round-5 scoping; a crash
    mid-fan-out is remedied by a NEW approval of the same
    document), verifies the document's business_id, and re-runs
    the normal §6.1 fan-out THROUGH THE ADMISSION GATE (round 5):
    the approved digest authorizes the ≥ relaxation AT ADMISSION
    (== trade watermark + differing digest → admit + update
    trade_snapshot_state); a document OLDER than the trade
    watermark is refused even with an approval — a stale
    adjudication is re-initiated against current state, never
    applied. Round 6: reprocess block transactions carry the SAME
    §6.1 TRADE-SNAPSHOT FENCE (trade row locked first, admitted
    ordering + digest re-verified per block) — if live intake
    admits a newer snapshot mid-reprocess, the remaining blocks
    are ABANDONED (§6.1 block-level supersession, round 7 — the
    approvers were told at approval time), the §9.3
    consumed-without-completion alert surfaces it, and a
    re-approval of the now-stale document is REFUSED at
    admission — the right answer, not a defect.
    PER-BLOCK ALGORITHM (normative, round 4 — the relaxation
    decision is PER OBLIGATION; whole-snapshot equality is only
    the §6.7 tie-DETECTION rule at intake; no whole-snapshot
    digest or id is persisted on obligations, and per-block
    transactions mean NO atomic whole-trade application exists):

```text
PRECONDITION: the document has passed §6.1 ADMISSION (trade row
  locked; ordinary strictly-newer, OR the approved-tie ≥ relaxation
  applied THERE; trade_snapshot_state updated). A refused document
  reaches no rule below and creates nothing. Round 6: EACH block
  transaction re-locks the trade row and re-verifies the admitted
  (ordering, digest) — the trade-snapshot FENCE — before the rules
  below run; mismatch stops the fan-out (§6.1 block-level
  supersession).
for each payment block of the ADMITTED document,
    sorted by scope tuple (§6.1), each its own transaction:
  no obligation exists            -> create (normal first-message
                                     path, §6.1 — safe only because
                                     admission refused stale
                                     documents, round 5)
  doc.ordering >  watermark       -> apply (ordinary strictly-newer)
  doc.ordering == watermark:
      block payload == applied    -> no-op (this is what makes a
                                     re-run converge)
      block payload != applied    -> APPLY (the ≥ relaxation — this
                                     block IS the tie being
                                     adjudicated)
  doc.ordering <  watermark       -> drop as stale (guard)
obligations ABSENT from the document -> AMENDMENT TO ZERO (PO-9
                                     ANSWERED 2026-07-11; lifecycle
                                     round 11): required := 0 AND
                                     upstream_ordering := doc.ordering
                                     under fence + obligation lock,
                                     strictly-newer guard; then §6.4
                                     auto-cancel (unsent) /
                                     wait-then-decide (in-flight) /
                                     §6.5 overpay latch = STOP
                                     (confirmed > 0); cleanly unwound
                                     row derives §4.1 CANCELLED.
                                     §6.6 anchors: zeroed ONLY when
                                     doc.ordering >
                                     validation_failed_ordering
                                     (ordering-aware retirement,
                                     §6.1). An EMPTY derived set is
                                     valid: the fan-out is pure
                                     absence (§6.0/§6.1).
trade-reference-only difference   -> blocks no-op per the rules
                                     above; the ADMISSION update to
                                     trade_snapshot_state (ordering,
                                     storage id, digest) IS the
                                     application (round 5): §7.0
                                     fresh assembly picks the new
                                     reference up from the stored
                                     pointer, and a re-run compares
                                     digest-EQUAL at admission and
                                     no-ops — the tie converges
after each applied block: set upstream_ordering := doc.ordering
  (idempotent), then §6.4/§6.5/§6.8 consequences unchanged.
Crash mid-reprocess: the approval is already CONSUMED (§9.3
  consume-at-start); the re-run happens under a NEW approval of the
  SAME document — admission sees == ordering + equal digest,
  re-fans-out, applied blocks no-op, remaining blocks apply
  (converges; same §6.1 property).
```

    The same endpoint serves as the general re-trigger after a
    DLT-parked notification: the corrected document is a NEW
    immutable storage id/version (upstream ask 8 — content behind
    an id NEVER changes; "fixing in place" is forbidden by
    contract), and the ordinary guard rows above govern it. Rare
    by construction; the tie class disappears when upstream ask
    1's explicit sequence field arrives.
```
