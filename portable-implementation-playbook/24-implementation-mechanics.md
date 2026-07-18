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

## M0. Adapting these recipes to YOUR codebase (read first)

The SQL and pseudo-code below are SHAPES, not paste-ready code.
BINDING: transaction boundaries, lock ORDER, the WHERE carrying the
full precondition, the row-count verdict, ack-after-commit, and the
§3 money rules. NOT binding: identifier names, exact column types,
framework idioms. Translation protocol, per snippet:

```text
1. Replace reference-model table/column names via the LOCAL mapping
   + divergence register (file 26 T.2). Never edit the playbook's
   own copies to local names.
2. Verify column types/scales against the REAL DDL captured in
   F.18 (BigDecimal scale round-trips, VARCHAR2 sizes, TIMESTAMP
   vs DATE semantics) — a type mismatch is DIV-2: record it, adapt,
   and the card's tests must prove the spec invariant against the
   REAL shape.
3. A local column with SIMILAR-but-not-identical semantics (DIV-3)
   needs the register's recorded human approval BEFORE any snippet
   touches it — never silently reuse (the F.12 trap).
4. Run every adapted statement through EXPLAIN — scanner queries
   must ride the ACTIVE-row-bounded indexes (M5); a full scan of
   terminal rows means the adaptation is wrong, not the index.
5. If a snippet CANNOT be expressed against local reality without
   weakening its WHERE/lock/row-count semantics, that is DIV-4 —
   STOP and report SPEC_CONFLICT; never ship "approximately the
   same".
```

Oracle dialect notes:

```text
- Written for Oracle 12c+ (FETCH FIRST n ROWS ONLY). On 11g use a
  ROWNUM subselect — same shape, same ordering-before-limit rule.
- SYS_EXTRACT_UTC(SYSTIMESTAMP) = database-side UTC. If the local
  convention stores DATE or local-zone timestamps, record DIV-2 and
  keep every COMPARISON in one zone — never mix.
- Sequences vs IDENTITY columns: use whatever the local schema
  already uses (F.18); the shapes do not care.
- The two ORA codes the recipes branch on: ORA-00001 (unique
  violation → the §6.1 upsert-retry and I6 refusal paths) and
  ORA-00060 (deadlock → lock-order regression ticket, §15). Verify
  which Spring exceptions YOUR driver/dialect maps them to
  (typically DuplicateKeyException / CannotAcquireLockException) —
  test it, don't assume.
```

Spring notes:

```text
- Snippets assume plain SQL (JdbcTemplate / NamedParameterJdbcTemplate)
  inside ONE @Transactional service method (M7-1). If the codebase
  is JPA/Hibernate-first: these four tables STILL get plain SQL
  (M2 — no dirty-checking); a JdbcTemplate-backed repository beside
  the JPA ones is the usual shape. MyBatis: the mapper XML carries
  the same WHERE shapes. Either way the row count comes back to the
  caller as the verdict.
- The @Transactional method must be invoked through the Spring
  proxy (no self-invocation) and owns the WHOLE M1 unit — verify
  with a transaction-boundary test, not by reading annotations.
```

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

-- STEP 8 · BUFFER the §14 structured log line + register the
--   after-commit publication callback (§14 delivery contract,
--   review 4098532 H1 — NEVER publish inside the transaction):
--   request_id, idempotency_key, request_seq, correlation_id,
--   (stage, stage_state, submission_state, outcome) before→after,
--   display label, trigger_source, trigger_event_id / ticket ref.

COMMIT;  -- Kafka ack, if any, comes AFTER this commit (M6).
         -- The buffered §14 line publishes from afterCommit:
         -- rollback discards it (phantoms impossible); a crash
         -- here loses it (accepted gap, at-most-once, no retry);
         -- publication failure never fails the transition.
         -- Posting claim: commit → publish → provider call.
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
- Global order (round 6): trade row (§2.4, snapshot paths only) →
  obligation → request CAS. Never the reverse; never lock two
  obligations except in the §6.1 fan-out — and there strictly in
  sorted scope-tuple order, one block's transaction at a time (no
  cross-obligation transaction exists). Writers that touch ONE
  obligation (feed, scanners, ops) never take the trade row — no
  inversion is possible.
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
  re-checks the derived repost_permitted terms (§7.0:
  freeze, amount-vs-shortfall staleness for MAYBE rows — round 10:
  no cutoff term, the engine owns the calendar).
- (The rounds-8/9 pointer-presence claim-gate was REMOVED in
  round 10 — §2.4 greenfield fact: every trade row is born from an
  admitted message with its pointer populated, so a NULL pointer
  is unreachable; git history at 9a53c75.)
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
- The retry bound is MAX ATTEMPTS ONLY (round 10 — the engine
  owns the cutoff calendar; §7.4,
  2026-07-11 decision) — retry_deadline_at is reserved/unused; wire
  no rule to it. While frozen / breaker OPEN, gated scanners make
  zero attempts, so the attempt budget is structurally safe — there
  is no suspension mechanism to implement. (Round 10: NO cutoff
  check exists at attempt time — the engine owns its calendar and
  classifies late submissions itself, CA-1.)
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
- Upstream flow: whole-snapshot validation FIRST (§6.0), then the
  trade-level ADMISSION transaction (§6.1/§2.4, round 5): upsert-lock
  the trade_snapshot_state row, compare orderings — newer → admit +
  update row; equal + digest-equal → admit without update; equal +
  digest-differs → tie alert, STOP; older → refuse WHOLE (a refused
  document never creates a scope); THEN fan out per payment block in
  sorted tuple order, ONE transaction per block (§6.1). ROUND 6 —
  the TRADE-SNAPSHOT FENCE, NOT optional (round-7 rename; the old
  name collided with the currency scope-key field): every block
  transaction locks the trade row FIRST (SELECT FOR UPDATE),
  re-verifies the admitted (ordering, digest), THEN locks the
  obligation and applies; on mismatch STOP the fan-out (§6.1
  block-level supersession — abandoned blocks logged + counted).
  Never run the fence check without holding the trade lock in the
  SAME transaction (check-then-act races).
  Ack the Kafka record ONLY after the fan-out completes. A crash
  mid-fan-out is fine: redelivery re-admits (equal + digest-equal)
  and re-applies; applied blocks drop on the §6.7 ordering guard;
  partition ordering guarantees the redelivery runs before any
  newer snapshot of that trade.
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
    (SYS_EXTRACT_UTC), never application-node time. (No local
    cutoff calendar exists — round 10, §7.4.)
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
    structurally safe); no cutoff exists to check (round 10), never
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
[ ] approval consumption matches the OPERATION CLASS (round 5):
    SINGLE-TRANSITION ops → the APPROVED→CONSUMED CAS (row count 1)
    and the payment CAS commit in ONE transaction/session; refusal
    or exception rolls back BOTH (test: concurrent double-execution
    — exactly one wins; mid-transaction failure — approval survives
    unconsumed). MULTI-BLOCK reprocess-snapshot → digest check
    FIRST (refusal burns nothing), then CONSUME-AT-START in its own
    transaction BEFORE fan-out; crash mid-fan-out = NEW approval of
    the same document (test: crash-after-consume — approval burned,
    nothing applied, new approval applies the remainder). NEVER a
    consumption that commits with the LAST block, and NEVER a
    resumable EXECUTING state (rejected, §9.3)
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

## M9. §14.1 attempt-journal riders (content write-ahead — K-04 / RC-02 / ST-10)

The journal (§14.1; deployed by AUD-01) is an audit sink, never
state: INSERT-only, ops/audit schema, and NO runtime rule, scanner,
gate, resolver, or derivation may ever read it (rule 13(b)). The
riders are single INSERT statements added INSIDE two transactions
that already exist — no new commit points, no new locks, no SHAPE
changes. NEVER LOAD-BEARING, as a NARROW GUARANTEE (§14.1, revised
per review d00ef6a H3): every rider is SWITCH-GATED (§14.1 switch
OFF → skip entirely, no error) and STATEMENT-ISOLATED — a plain
try/catch around the single INSERT with NO inner @Transactional
boundary (inner participation would mark the host rollback-only:
the exact Spring trap this rule forbids). Statement-local means
ONLY the pinned ORA-code allowlist (00001, 02290, 20141/20142,
evidenced space-error family) — TIMEOUTS AND UNKNOWN TRANSLATIONS
ARE FATAL BY DEFAULT (review 928341a H2). Allowed failures are
swallowed: record the gap in memory/metrics and CONTINUE; the
AUDIT-GAP alert is emitted AFTER the host COMMIT (side effects
after commit — a rolled-back host never reports a phantom gap).
FATAL failures (connection loss, session kill, commit failure) are
NOT isolatable — they fail the host transaction as ordinary infra
failures, which existing recovery already handles (uncommitted
claim → row stays READY/RETRY_WAIT; committed claim → lease expiry
→ MAYBE). The provable contract: the journal can never cause an
INCORRECT payment outcome (T-38 F exercises both classes on the
real JDBC/Spring stack, both riders).

Rider 1 — ATTEMPT_STARTED, in the posting-claim transaction (M1/M4,
card K-04), beside the §2.2 write-ahead fields. The claim CAS also
increments post_attempt_seq (monotonic — NEVER attempt_count, which
resets on the §9.2 downgrade and would collide):

```java
// inside the claim transaction, after the CAS UPDATE
// (which included: post_attempt_seq = post_attempt_seq + 1)
if (journalSwitch.isOn()) {                    // §14.1 enablement gate
  try {
    jdbc.update(INSERT_ATTEMPT_STARTED,        // ONE statement, host tx —
        id, key, postSeq, trigger, corr,       // no nested @Transactional
        hash, content);                        // (rollback-only trap), no
                                               // autonomous tx (phantoms)
    // content = the FULL CA-6 canonical bytes, EVERY attempt
    // (§14.1 simplicity rule — no dedup, no content_ref)
  } catch (DataAccessException e) {
    if (STATEMENT_LOCAL.test(e)) {             // the T-38 classifier: a
      gapBuffer.record(id, postSeq, e);        // NARROW allowlist of PINNED
                                               // ORA vendor codes (00001,
                                               // 02290, 20141/20142,
                                               // 01653-family) read from the
                                               // SQLException — NEVER
                                               // instanceof-only; swallow;
                                               // host tx proceeds.
      // AUDIT-GAP alert fires AFTER host commit (afterCommit hook)
    } else {
      throw e;                                 // EVERYTHING ELSE — timeouts,
                                               // unknown/ambiguous
                                               // translations, connection/
                                               // session/commit classes — is
                                               // FATAL BY DEFAULT: ordinary
                                               // infra failure, existing
                                               // recovery owns it
    }
  }
}
```

Rider 2 — ATTEMPT_RESOLVED, in WHICHEVER transaction ends the
attempt episode: RC-02's §7.2 classification CAS, OR ST-10's
lease-expiry takeover (then outcome = 'LEASE_EXPIRED_MAYBE'). The
dimension CAS arbitrates the race — insert ONLY on rowCount 1, and
with the SAME statement isolation as rider 1 (try/catch, no inner
transaction, gap alert after commit):

```java
// inside the episode-ending transaction, ONLY when its CAS
// returned rowCount == 1 — same try/catch + STATEMENT_LOCAL
// classifier + after-commit alert as rider 1:
if (journalSwitch.isOn() && casRowCount == 1) {
  try {
    jdbc.update(INSERT_ATTEMPT_RESOLVED,
        id, key, postSeq, trigger, corr,
        outcome, errCode, errDetail, responseExcerpt);
    // outcome = the §7.2 class VERBATIM (+ LEASE_EXPIRED_MAYBE);
    // never an invented vocabulary (the paj_outcome_ck backstops)
  } catch (DataAccessException e) {
    if (STATEMENT_LOCAL.test(e)) { gapBuffer.record(id, postSeq, e); }
    else { throw e; }
  }
}
```

Binding rules (SHAPE — tick with the host card's checklist; T-38):

```text
[ ] both riders run in the SAME transaction as their host CAS —
    NEVER an autonomous transaction (phantom STARTED on rollback),
    NEVER a separate commit
[ ] both riders are STATEMENT-ISOLATED: plain try/catch, NO inner
    @Transactional (rollback-only trap); statement-local failures
    → gap recorded, host PROCEEDS; the AUDIT-GAP alert fires only
    AFTER the host COMMIT; fatal session/commit failures = normal
    infra failures handled by existing recovery (T-38 F, both
    riders, real JDBC/Spring stack)
[ ] both riders are SWITCH-GATED (§14.1 enablement switch; OFF in
    production until the Q30 journal items are evidenced; the
    switch changes state ONLY under posting freeze + drain — §14.1
    switch-transition rule, T-38 J)
[ ] payload_content = the FULL canonical bytes on EVERY STARTED —
    no dedup, no content_ref (T-38 E; consecutive-dedup is a
    FUTURE Q31-gated optimization, §14.1)
[ ] pairing identity = post_attempt_seq, NEVER attempt_count (it
    resets on the §9.2 downgrade — the T-38 case B regression)
[ ] rider 2 executes only on the host CAS rowCount == 1
[ ] POSTING attempts only — no ENRICH retries, no resolver
    settlements, no lifecycle events
[ ] no SELECT from the journal anywhere in runtime code (code review
    greps for reads; ops/reporting queries live outside the app)
[ ] schema/table/column names adapt per M0; shapes above are BINDING
```
