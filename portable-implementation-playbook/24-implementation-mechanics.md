> **Purpose:** The concrete HOW for every implementation card: transaction skeletons, CAS shapes, lock rules, claim/lease mechanics, scanner and consumer recipes, Spring/Oracle traps, and the per-shape edge-case checklists every task must tick.
> **When to use this file:** MANDATORY reading before ANY implementation card (rule 17 in 16-local-agent-instructions.md). Re-read the matching SHAPE checklist at the end of every card.
> **Depends on:** requirment-v4.md §3, §4, §5, §6.8, §7.0, §8, §10.1–10.3, §11, §14, §16.1/§16.2/§16.4.
> **Used by:** Every implementation task; the execution report (19) records the ticked SHAPE checklist.
> **Safe to transfer:** yes
> **Contains local code names:** no

# M-REF. Implementation mechanics reference

This file adds HOW, never WHAT. It restates the spec's mechanics as
code-level recipes so a small-context executor does not have to
re-derive them. **If anything here seems to disagree with
`requirment-v4.md`, the spec wins — report the discrepancy (rule 16).**
Table/column names are the spec's own (§2); everything else is a
placeholder to map locally.

------

## M1. The canonical write-path skeleton

Every transaction that changes a request DIMENSION (stage,
stage_state, submission_state, outcome, blocked_reason) or moves money
has this exact shape (§11). Do not improvise variations.

```sql
-- ONE database transaction. In Spring: ONE @Transactional service
-- method (see M7) — never split across methods/threads.

-- STEP 1 · obligation lock FIRST (global lock order, §11)
SELECT id, required_amount, committed_amount, confirmed_amount,
       overpay_blocked, upstream_ordering, next_request_seq
  FROM payment_obligation
 WHERE id = :obligation_id
   FOR UPDATE;

-- STEP 2 · the request CAS. The WHERE clause IS the guard:
-- full dimension precondition, evaluated atomically by the DB.
UPDATE payment_request
   SET <target dimension values>,
       version = version + 1,
       state_changed_at = SYS_EXTRACT_UTC(SYSTIMESTAMP)
       -- + episode anchors iff this transition owns them (§2.2):
       --   maybe_since (set once entering MAYBE / cleared leaving or on outcome),
       --   submitted_at, escalated_at, divergent_payload_at ...
 WHERE id = :request_id
   AND outcome IS NULL                        -- write-once outcome
   AND <the card's dimension preconditions>;  -- e.g. stage_state='BLOCKED'
                                              --      AND submission_state='NOT_SUBMITTED'

-- STEP 3 · branch on row count. THIS IS THE VERDICT.
--   rowCount == 0 → the state moved (stale event, lost race, fenced
--                   stale worker). Do NOTHING else: no money, no
--                   derivation, no error state. Log + return/ack.
--   rowCount == 1 → continue, all in the SAME transaction:

-- STEP 4 · money movement — ONLY on rowCount 1, ONLY per §3:
--   +committed_amount  in the transaction that INSERTS a request (§6.8)
--   -committed_amount  when outcome becomes REJECTED/CANCELLED/SUPERSEDED
--   +confirmed_amount  when outcome becomes EXECUTED (amount equality, §8)
--   NOTHING at POST time, on POST response, on same-key retry.
UPDATE payment_obligation SET committed_amount = committed_amount - :amt
 WHERE id = :obligation_id;

-- STEP 5 · marker writes iff the transition owns one (monotonic, §6.9;
--   REJECTED outcome sets exactly one marker — L9 totality)

-- STEP 6 · §6.8 standing re-evaluation (if this transition is a
--   T1–T4 trigger) — the SINGLE request-creation point, still under
--   the same lock

-- STEP 7 · §4 re-derivation, same transaction: ui_step_status (§4.1),
--   active_exception_* (§4.2 rank order). Never skipped, never async.

-- STEP 8 · emit the §14 structured log line:
--   request_id, idempotency_key, request_seq, correlation_id,
--   (stage, stage_state, submission_state, outcome) before→after,
--   display label, trigger_source, trigger_event_id / ticket ref.

COMMIT;  -- Kafka ack, if any, comes AFTER this commit (M6)
```

Exception: updates touching ONLY claim fields / attempt counters
(claimed_by, claim_expires_at, attempt_count, next_retry_at — no
dimension, no money, no derivation input) may skip the obligation
lock (§11).

## M2. CAS discipline (what "the CAS" means in every card)

- Plain SQL (JdbcTemplate or equivalent). **No JPA/Hibernate
  dirty-checking on payment_request / payment_obligation** (§11) — an
  ORM flush cannot express "WHERE carries the precondition and row
  count is the verdict".
- The WHERE always carries: `outcome IS NULL` (unless the write IS the
  outcome write, then still `outcome IS NULL` — outcome is
  write-once), plus every dimension value the card's transition
  assumes. Copy the precondition from the card / §10.5 flow table
  verbatim.
- `version = version + 1` in every SET; external callers (ops
  endpoints later) pass expected version; internal flows rely on the
  dimension WHERE.
- rowCount 0 is a NORMAL result, not an error: stale evidence dies
  here (§8), fenced workers die here (§11), lost races die here. The
  caller logs and moves on. Never retry a CAS in a loop to "make it
  win".
- Contradictory/duplicate evidence needs no special code: outcome
  write-once + dimension WHERE means the second writer hits 0 rows.
  A NEW event_id hitting 0 rows against a TERMINAL row additionally
  raises the §8 CRITICAL anomaly (that check is explicit code).

## M3. Locks and lock ordering

- Obligation lock = `SELECT ... FOR UPDATE` on the payment_obligation
  row. It owns ALL money math: shortfall, creation, amount updates,
  overpay latch, completion + exception derivation (§11).
- Global order: obligation lock FIRST, then request CAS. Never the
  reverse; never lock two obligations except in the §6.1 fan-out —
  and there strictly in sorted scope-tuple order, one block's
  transaction at a time (no cross-obligation transaction exists).
- Never hold the lock across an external call (HTTP, engine, account
  service). The posting flow persists-then-calls: claim transaction
  commits BEFORE the HTTP call (M4).
- Deadlock seen (ORA-00060) = lock-order regression → §15 ticket
  metric; fix the order, do not add retries around it.

## M4. Claims are leases (scanner → worker handoff)

Claim CAS (generic stage claim):

```sql
-- THE claim CAS. It NEVER touches an already-CLAIMED row, expired
-- or not (corrected 2026-07-11 — an expired-takeover branch here
-- was a duplicate-POST hazard for stage=POST):
UPDATE payment_request
   SET stage_state = 'CLAIMED', claimed_by = :worker_id,
       claim_expires_at = :db_now_plus_lease, version = version + 1
 WHERE id = :id
   AND outcome IS NULL
   AND stage = :expected_stage
   AND (stage_state = 'READY'
        OR (stage_state = 'RETRY_WAIT' AND next_retry_at <= :db_now));
```

Expired-lease RECOVERY is a SEPARATE transition, owned exclusively
by the lease-expiry path (ST-10) — never folded into a claim:

```sql
-- ENRICH expiry: work is repeatable → back to READY (re-claimable)
UPDATE payment_request
   SET stage_state = 'READY', claimed_by = NULL,
       claim_expires_at = NULL, version = version + 1
 WHERE id = :id AND outcome IS NULL
   AND stage = 'ENRICH' AND stage_state = 'CLAIMED'
   AND claim_expires_at < :db_now;

-- POST expiry: the POST may have executed → MAYBE, resolver owns it.
-- NEVER back to a claimable posting state, NO exceptions (§11).
UPDATE payment_request
   SET stage = 'CONFIRM', stage_state = 'READY',
       submission_state = 'MAYBE_SUBMITTED',
       maybe_since = COALESCE(maybe_since, :db_now),
       claimed_by = NULL, claim_expires_at = NULL,
       version = version + 1
 WHERE id = :id AND outcome IS NULL
   AND stage = 'POST' AND stage_state = 'CLAIMED'
   AND claim_expires_at < :db_now;
```

POSTING claim additions (§11 — the last gate before the wire), all in
the claim transaction, committed BEFORE the HTTP call:

- WHERE additionally carries `divergent_payload_at IS NULL`; code
  re-checks the derived repost_permitted terms (§7.0: cutoff,
  freeze, amount-vs-shortfall staleness for MAYBE rows).
- Persist: identity (first claim — idempotency key §5.1),
  `last_sent_hash` of the freshly assembled instruction,
  `divergence_expected` (compare against the PRIOR hash BEFORE
  overwriting it), `last_post_attempt_at` (DB time).
- If the claim COMMIT outcome is unknown (connection lost mid-commit):
  do NOT proceed to the HTTP call. Treat the claim as lost; lease
  expiry → MAYBE → resolver (§11 ambiguous claim-commit).

Lease-expiry recovery (§11 — memorize this asymmetry):

```text
expired ENRICH claim → re-claimable in place (read-only work)
expired POST claim   → CONFIRM · READY · MAYBE_SUBMITTED.
                       NEVER re-claimed for posting, NO exceptions —
                       no "provably not launched" carve-out exists.
```

Stale-worker fencing needs no extra code: the zombie's later CAS
carries its old expectations → rowCount 0.

## M5. Scanner recipe (retry scanner, resolver sweep, escalation, drift)

```sql
-- STEP A · candidate selection: bounded batch, DB time, NO LOCKS
-- (plain read — the claim CAS below is the contention resolution;
--  FOR UPDATE SKIP LOCKED is NOT used: locking request rows before
--  the obligation lock would invert the §11 global lock order)
SELECT id FROM payment_request
 WHERE outcome IS NULL
   AND <scope predicate — dimension columns / episode anchors ONLY>
 ORDER BY <the scope's ordering rule>
 FETCH FIRST :batch_size ROWS ONLY;

-- STEP B · per candidate, a NEW transaction (the §11 claim protocol):
--   1. obligation lock FIRST (SELECT ... FOR UPDATE on the parent)
--   2. claim CAS (M4) whose WHERE carries the full expected state
--   3. rowCount 0 → lost race: skip silently, next candidate
-- Claim/unclaim transitions are CLAIM MECHANICS (§11): they run
-- under this obligation lock but trigger NO §4 re-derivation.
```

- Gate on the dependency's circuit breaker BEFORE claiming a batch
  (§16.1) — an outage becomes quiet waiting, not a thundering herd.
- Per-ITEM transaction boundaries: one failed item never poisons the
  batch; a deterministic per-item failure hits the poison-row cap →
  BLOCKED + alert (§16.1).
- Clocks: `next_retry_at <= DB now`; AGE rules key on the set-once
  episode anchors (maybe_since, submitted_at, last_post_attempt_at,
  validation_failed_first_at) — NEVER on state_changed_at (§15).
- Scope predicates come from §4.5/§9.5 verbatim: e.g. resolver scope =
  `outcome IS NULL AND (submission_state='MAYBE_SUBMITTED' OR
  (submission_state='SUBMITTED' AND submitted_at < :now - :confirmation_age))`
  — ANY stage, ANY stage_state, including BLOCKED. Never scope by
  stage or by how a row got somewhere (§9.5).
- Retry bounds are max attempts + payment cutoff ONLY (§7.4,
  2026-07-11 decision) — retry_deadline_at is reserved/unused; wire
  no rule to it. While frozen / breaker OPEN, gated scanners make
  zero attempts, so the attempt budget is structurally safe — there
  is no suspension mechanism to implement. Cutoff checks still
  apply at attempt time (never suspend).
- Every scanner exports a heartbeat (§15: silent 3× interval → page).
- Expected indexes: the §16.6 artifact-4 ACTIVE-row-bounded function
  indexes (expressions NULL for terminal rows). If a scanner query
  plan scans terminal rows, the index set is wrong — stop and report.

## M6. Kafka consumer recipe (both inbound flows)

```text
poll → for each record:
  open DB transaction
    (feed flow only) INSERT INTO processed_inbound_event  -- §8 step 1
       duplicate key → COMMIT nothing, ack, next record
    resolve target → evidence-guarded CAS per M1/M2
    rowCount 0 → fine (stale/duplicate): log + count
  COMMIT
  ack the record            -- ack ALWAYS after commit, never before
```

- `enable-auto-commit=false`, ack-mode=record, offsets commit only
  after the DB commit (§16.2). auto-offset-reset=earliest.
- Upstream flow: whole-snapshot validation FIRST (§6.0), then fan out
  per payment block in sorted tuple order, ONE transaction per block
  (§6.1). A crash mid-fan-out is fine: redelivery re-applies; applied
  blocks drop on the §6.7 ordering guard.
- ErrorHandlingDeserializer is mandatory; DLT is for POISON messages
  only (deserialization/semantic validation). Transient infra errors
  retry IN PLACE or pause the container — NEVER dead-letter a money
  event, NEVER use non-blocking retry topics (§16.2 — they reorder
  per-payment history).
- Feed events: unmatched UETR → log + metric + ack (§8); no parked
  storage, no replay (rule 13). provider_reference fallback only
  under the §8 fail-closed guards.
- Never call external services inside the record transaction beyond
  the flow's own defined calls; never hold the poll thread past
  max.poll.interval.ms (size it per §16.2).

## M7. Spring / Java / Oracle traps (each one has burned someone)

```text
1.  ONE @Transactional boundary per M1 unit, at the service method a
    listener/scanner calls. No self-invocation (a this.method() call
    bypasses the proxy — the tx silently doesn't exist). No
    REQUIRES_NEW inside the money path (it splits M1 into two
    transactions; a crash between them breaks I1).
2.  Amounts: BigDecimal everywhere; compareTo, NEVER equals (§16.4);
    completion boundary is compareTo == 0. JPY scale 0 / BHD scale 3
    must survive round-trips.
3.  Time: every due/age comparison uses DATABASE time
    (SYS_EXTRACT_UTC), never application-node time. Cutoffs are
    tz-aware calendar values converted at comparison time (§16.4).
4.  Retries: durable retry state on the row is the ONLY retry owner
    per operation (§7.4/§16.1). No @Retryable, no resilience4j
    retry, no client-library retry on the payment POST. In-process
    micro-retries only for idempotent reads on provably-unsubmitted
    failures (e.g. enrichment lookups).
5.  Classification: unmapped engine/enrichment result → fail CLOSED
    (BLOCKED(UNMAPPED_CODE) / MAYBE per §7.2-§7.3), never "assume
    retryable". The code-by-code table is CA-1; a new code should
    fail the CA contract test at build time (§16.5).
6.  Every external call: explicit timeout from the per-dependency
    budget + circuit breaker; business rejects count as breaker
    SUCCESSES (§16.1).
7.  Enum reads are defensive: unknown CHECK-enum value → UNKNOWN
    sentinel, never Enum.valueOf (§16.5 rollout rule). The four
    dimension enums are CLOSED — extending one is a design change.
8.  debit_account is MASKED in the read model, logs, and traces —
    masking in the logging encoder, not call-site discipline
    (§16.3). Stack traces stay in logs keyed by correlation_id.
9.  correlation_id flows through MDC and outbound headers (§15).
10. Bulkheads: posting, enrichment, and card-read serving never share
    a thread pool; in-memory queues bounded — the DATABASE is the
    queue (§16.1).
11. The Hazelcast freeze check: bounded timeout; timeout/absence
    reads FROZEN; only the FROZEN answer may be cached (§16.1).
    Checked before every claim and every POST.
12. Read surface (card + queues): plain MVCC reads, NO locks, no
    writes; business_id lookup returns ALL of the trade's obligations
    (§12 — result count is never an error).
```

## M8. SHAPE checklists (BINDING — tick in the execution report)

Identify which shape(s) the card's code is, run that checklist before
declaring the card done, and record `SHAPE-<X>: all ticked` (or the
named exceptions) in the report (19). Rule 18 makes this mandatory.

**SHAPE-CAS — any writer of dimensions or money:**
```text
[ ] obligation lock acquired FIRST, same transaction end-to-end (M1)
[ ] WHERE carries outcome IS NULL + the full dimension precondition
[ ] rowCount==0 branch exists, is silent-safe, moves no money
[ ] money movement only on rowCount==1, only per the §3 table
[ ] marker write iff the transition owns one; monotonic (§6.9);
    L9 totality on REJECTED
[ ] §6.8 re-evaluation invoked iff this is a T1–T4 trigger
[ ] §4 re-derivation in the SAME transaction
[ ] §14 log line emitted with before→after + trigger_source
[ ] episode anchors set/cleared per §2.2 (maybe_since, submitted_at…)
[ ] no external call between lock acquisition and commit
[ ] test: replay/duplicate hits 0 rows and changes nothing (money
    asserted unchanged)
```

**SHAPE-SCAN — any scheduled scanner/sweeper:**
```text
[ ] scope predicate on dimensions/anchors only — never stage-history,
    never labels, never blocked_reason
[ ] lock-free candidate selection (NO FOR UPDATE / SKIP LOCKED) +
    per-item transactions: obligation lock FIRST, then the claim CAS;
    rowCount 0 = lost race, skipped silently (§11 claim protocol)
[ ] breaker-gated before claiming; jittered backoff
[ ] DB time everywhere; AGE rules on set-once anchors
[ ] freeze/breaker windows: zero attempts made (attempt budget
    structurally safe); cutoff checked at attempt time, never
    suspended; nothing wired to retry_deadline_at
[ ] poison-row cap → BLOCKED + alert (no infinite loop)
[ ] heartbeat metric exported; overrun behavior defined (no overlap)
[ ] plan check: query rides an ACTIVE-row-bounded index
```

**SHAPE-CONSUME — any Kafka listener path:**
```text
[ ] ack strictly after DB commit; auto-commit off
[ ] (feed) inbox insert first; duplicate-key → ack + skip
[ ] rowCount==0 on the evidence CAS treated as normal (stale) — but
    NEW event_id + 0 rows vs TERMINAL row raises the §8 CRITICAL
[ ] (upstream) whole-snapshot validation before fan-out; per-block
    transactions in sorted tuple order; §6.7 guard makes redelivery
    converge (test: kill mid-fan-out, redeliver, assert convergence)
[ ] poison pill → DLT + page; transient error → in-place retry/pause,
    NEVER DLT, NEVER retry topic
[ ] amount equality (compareTo) before any +confirmed; mismatch →
    BLOCKED(AMOUNT_MISMATCH) + SUBMITTED + CRITICAL, no money move
[ ] unmatched event → log + metric + ack (no storage, no replay)
```

**SHAPE-PROC — any guarded ops operation (OP-xx, RG-05 supersede,
future console). Execution boundary (decided 2026-07-11): an
AUTHORIZED ENDPOINT of the payment APPLICATION calling the shared
Java transition service — never a PL/SQL reimplementation (a stored
procedure cannot reuse the shared helpers, check the freeze, emit
§14/§15 telemetry, or verify enterprise identities); the §10.3
triggers stay as the DB backstop:**
```text
[ ] mandatory inputs enforced IN the operation contract: operator
    id, reason, ticket ref; where the catalog says 4-eyes/dual the
    execution input is the §9.3 approval_id ONLY — identities are
    DERIVED from the approval record (round 4: never approver-
    identity parameters, never free-text strings)
[ ] approval consumption is ATOMIC with the transition: the
    APPROVED→CONSUMED CAS (row count 1) and the payment CAS commit
    in ONE transaction/session; refusal or exception rolls back
    BOTH (test: concurrent double-execution — exactly one wins;
    mid-transaction failure — approval survives unconsumed)
[ ] endpoint authorization: restricted to the enterprise ops role;
    unauthorized-role attempt refused (and tested)
[ ] release guard honored: terminal-negative only on NOT_SUBMITTED or
    with the legitimately-set evidence flag (§10.1/§10.3)
[ ] refuses CLAIMED rows and terminal rows; re-checks state INSIDE
    the transaction (operator screens are stale by definition)
[ ] routes through the SAME shared CAS/money helpers + M1 skeleton
    (never a private UPDATE path); freeze check where the operation
    can lead to a POST
[ ] §14 line with trigger_source=MANUAL_OPS:<id> (or
    OPS_PLATFORM_VERIFIED) + ticket; §15 alert where specified
[ ] test proves raw SQL fails where the operation succeeds (trigger
    demonstration — the OP-02 pattern)
```

**SHAPE-READ — any read-only surface (card, queue views):**
```text
[ ] no locks, no writes, read-only transaction/scope
[ ] business_id lookup returns ALL obligations; count never a signal
[ ] NOT_STARTED = row absence; unavailable ≠ stale-as-authoritative;
    freshness/lag indicator wired to the §15 lag metric (§12)
[ ] content rules: ops-readable text, masked account data, no stack
    traces (§12/§16.3)
[ ] no rule/decision logic keyed on display labels (§10.4)
```
