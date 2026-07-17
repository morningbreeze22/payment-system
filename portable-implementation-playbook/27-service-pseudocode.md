> **Purpose:** End-to-end pseudocode for the four spec services (PaymentOrchestrationService, PaymentEnrichmentService, PaymentExecutionService, PaymentNotificationConsumerService): threads, transactions, locks, money movements, exception handling, and the frontend exception contract — one readable flow per service.
> **When to use this file:** ORIENTATION before implementation cards — read it to understand how the pieces fit; then implement from the CARD + file 24 recipes, never from this file alone.
> **Depends on:** requirment-v4.md (wins on every conflict); 24-implementation-mechanics.md (M1–M9 are the binding shapes); 26-team-execution-and-divergence-protocol.md (local reality may differ — record divergences, rule 21).
> **Used by:** every implementation card as background; onboarding.
> **Safe to transfer:** yes
> **Contains local code names:** no

# S. Service pseudocode (non-normative orientation)

PRECEDENCE: spec → task card → file 24 recipes → this file. This file
compresses; it never overrides. If code you are writing disagrees
with a card or a recipe, the card/recipe wins; if this file seems to
disagree with the spec, report the discrepancy (rule 16).

The four services are the spec's own (requirment-v4.md front
matter). Locally they may be modules of one deployable or separate
deployables — the transaction/lock/thread rules below are IDENTICAL
either way; record the local shape in the file 26 register (DIV-1 if
it is only naming/packaging).

------

## S0. System-wide rules every service obeys

```text
GLOBAL LOCK ORDER (M3, §11):
  trade_snapshot_state row  →  obligation rows (sorted scope-tuple
  order)  →  request row (conditional CAS UPDATE only; the request
  row is never held under FOR UPDATE).
  Any transaction touching both tables takes the obligation lock
  FIRST. Deadlock is impossible by construction, never by luck.

TX(name) { ... }  = ONE database transaction.
  - Nothing slow inside: no HTTP, no lookups, no Kafka produce
    between lock acquisition and COMMIT (M1).
  - Side effects (acks, alerts, metrics) fire AFTER commit.

CAS(update) → rowCount (M2):
  - WHERE restates the FULL expected world (dimensions + outcome IS
    NULL + claim fields as applicable).
  - rowCount 1 = you own the transition; rowCount 0 = lost race or
    stale — SKIP SILENTLY: no exception, no retry loop, no money.

MONEY (§3) — only ever inside the obligation lock, BigDecimal with
compareTo (M7):
  - committed += amount   in the SAME TX that INSERTs the request.
  - committed -= amount   riding the CAS that sets a terminal-
                          negative outcome (once — rowCount guards).
  - confirmed += amount   riding the CAS that applies EXECUTED
                          evidence (after amount equality check).
  - NO movement at claim, POST, response receipt, or retry.
  - shortfall = required - committed (I5); overpay latch when
    confirmed > required (§13) — set + alert + STOP, never auto-fix.

derive(obligation) — §4, runs in the SAME TX as every applied
mutation, under the lock, LAST before COMMIT:
  1. ui_step_status := completed | cancelled | in_progress
     (§4.1 predicates verbatim — never shortcuts).
  2. active_exception_code / manual-action flag / active_exception_at
     := highest-ranked LIVE condition (§4.2, catalog §13).
     THIS IS THE FRONTEND EXCEPTION REPORT — see S6.
  3. §6.8 re-evaluation if this mutation is a T1–T4 trigger
     (single request-creation point; I6 = one active request).

LOG LINE (§14): every attempt/transition emits one structured line —
idempotency_key, request_seq, correlation_id, dimensions
before→after, payload hash, trigger_source. The §14.1 journal riders
mirror the ATTEMPT lines into the content journal — file 24 M9
(two sinks of one attempt; the journal adds the bytes).

EXCEPTION TAXONOMY — every catch block classifies; silent catch =
review-failing defect:
  POISON            unparseable/contract-violating message →
                    DLT + page, then ack (consumers only; M6).
  TRANSIENT_INFRA   DB/network timeout → bounded in-place retry with
                    jittered backoff; consumers: NO ack, NEVER DLT —
                    at-least-once redelivery is the safety net.
  LOST_RACE         CAS rowCount 0 → normal, silent skip.
  STALE_ORDERING    older snapshot/event → drop as already applied,
                    ack; convergence is the design (§6.7).
  BUSINESS_DEFECT   e.g. §8 amount mismatch, §9.1 engine
                    inconsistency → BLOCKED + CRITICAL alert +
                    frontend exception; no automatic mutation.
  UNMAPPED          engine code not in CA-1 → fail closed:
                    MAYBE_SUBMITTED + BLOCKED(UNMAPPED_CODE) + alert
                    (§7.2).
  UNCAUGHT (worker) let the worker die; the lease recovers it (§11).
                    Never catch-and-continue around a money TX.
```

------

## S1. PaymentOrchestrationService

Owns: upstream intake (trade snapshots), amendments/absence, the
§6.8 creation point, ops endpoints (§9.3 approval workflow + §20
surface), the §12 card read. Threads:

```text
- N Kafka listener threads (upstream topic; partition = trade key,
  so per-trade ordering is broker-given).
- HTTP pool: ops endpoints + card reads.
- 1 scheduler: §9.3 escalation sweep (may live in Execution locally
  — record placement in the file 26 register).
```

### S1.1 Upstream snapshot consumer (one message, end to end)

```text
[KAFKA LISTENER THREAD]
onTradeSnapshot(record):
  try:
    doc = parse(record)                          // fails → POISON
    validateSnapshot(doc)                        // §6.0: schema, strictly-positive
                                                 // amounts, within-snapshot tuple
                                                 // uniqueness; unparseable scope →
                                                 // POISON (no row possible, §6.6)

    // ---- ADMISSION: its own TX; FIRST lock in the global order ----
    TX(admission):
      snap = SELECT trade_snapshot_state WHERE business_id FOR UPDATE
             // insert-on-first-contact; PK-race → retry read (§6.1)
      if doc.ordering < snap.last_accepted_ordering:
          COMMIT; ack; return                    // STALE — refused WHOLE (§6.7)
      if doc.ordering == snap.last_accepted_ordering
         and doc.digest == snap.last_payload_digest:
          pass                                   // redelivery — re-run blocks (heals
                                                 // a crash mid fan-out; blocks no-op)
      else:
          snap.watermark  := doc.ordering        // admit
          snap.pointer    := doc.storageId
          snap.digest     := doc.digest
      COMMIT

    // ---- FAN-OUT: one TX PER payment block, sorted tuple order ----
    blocks = deriveOurPayments(doc)              // role derivation §1: 0..N;
                                                 // empty set is VALID (absence below)
    for block in sortByScopeTuple(blocks ∪ absentScopes(doc)):
      TX(applyBlock):
        // fence (§6.1): trade row first, re-verify THIS admission
        snap = SELECT trade_snapshot_state FOR UPDATE
        if (snap.ordering, snap.digest) != admittedValues: rollback; continue
                                                 // a newer snapshot won mid-flight
        ob = SELECT payment_obligation FOR UPDATE (upsert by scope key)
             // scope key = the 4 valid fields — the ONLY precondition
             // for saving an obligation; deep validation is enrichment's job
        if ob.ordering_tag >= doc.ordering: rollback; continue   // already applied

        if block.scopeExtractableButInvalid:
            ob.required := NULL                  // anchor (§6.6)
            write validation_failed marker(doc.ordering)
            // frontend: DATA_VALIDATION_FAILED surfaces via derive()
        else if block.absentFromSnapshot:
            ob.required := 0                     // removal tombstone — ONLY the
                                                 // absence path writes 0 (§6.1/§4.1)
            handleActiveRequestOnRemoval()       // §6.4/§6.5, see S1.2
        else:
            ob.required := block.amount          // amendment or first value
            handleAmendmentConsequences()        // S1.2

        recomputeOverpay()                       // confirmed > required → latch +
                                                 // alert; STOP (no auto-refund) §13
        ob.ordering_tag := doc.ordering
        reevaluate_6_8()                         // may INSERT request +
                                                 // committed += amount (same TX;
                                                 // identity §5.1: key computed and
                                                 // stored at creation (K-02), then
                                                 // re-verified/persisted-if-absent
                                                 // at the first posting claim (K-04))
        derive(ob)                               // frontend updated atomically
        COMMIT

    ack(record)                                  // ONLY after all blocks (M6)

  catch POISON:            sendToDLT(record); page(); ack(record)
  catch TRANSIENT_INFRA:   backoff(); throw     // no ack → broker redelivers
```

### S1.2 Amendment / removal consequences (inside the block TX)

```text
handleAmendmentConsequences / handleActiveRequestOnRemoval (§6.4/§6.5):
  active = the obligation's active request (I6: at most one)
  if active is null:                    nothing here; §6.8 decides next
  else if active.sub == NOT_SUBMITTED and stage_state != CLAIMED:
      CAS cancel (outcome := CANCELLED) + committed -= active.amount
      // release is legal — provably unsent; §6.8 will size the successor
  else if active.sub == MAYBE_SUBMITTED (any stage_state except CLAIMED):
      CAS stage_state := BLOCKED(AMENDMENT_PARKED) + alert
      // money may have moved — wait-then-decide; resolver keeps querying
  else if active.sub == SUBMITTED:      leave it alone
      // money engaged; overpay evaluation on confirmation handles it
  else if stage_state == CLAIMED:       do NOTHING to the row now
      // a live claim owns its row; the park/cancel applies at the next
      // non-CLAIMED evaluation (repost_permitted guards meanwhile)
```

### S1.3 Ops endpoints (§9.3 / §20) — one shape for all

```text
[HTTP THREAD, enterprise ops role only]
opsEndpoint(approval_id):                        // NEVER approver identities as
                                                 // inputs — derived from the record
  TX(op):
    appr = SELECT approval FOR UPDATE; verify APPROVED, binding, expiry
    CAS approval APPROVED → CONSUMED             // same TX as the payment change
    lockObligation; re-check row state INSIDE the TX (screens are stale)
    refuse if CLAIMED / terminal / amount mismatch
    apply via the SAME shared CAS + money helpers as everything else
    derive(ob)
    COMMIT
  after commit: §15 every-use alert; §14 line trigger_source=
  OPS_PLATFORM_VERIFIED (or MANUAL_OPS:<id>) + ticket ref
```

### S1.4 Card read (§12) — the frontend's only window

```text
[HTTP THREAD, read-only]
getCard(business_id):
  SELECT all obligations of the trade (never a single-row lookup)
  return per obligation: ui_step_status, active_exception_code,
    manual-action flag, active_exception_at, amounts, reopened_at,
    ops annotations; request detail incl. blocked_reason
  masking per §16.3; freshness/lag indicator wired to the §15 metric
  NO locks, NO writes, NO rule logic on display labels (§10.4)
```

------

## S2. PaymentEnrichmentService

Owns: the ENRICH stage — slow, failable reference-data work
(party/account lookups, deep validation), per the local step
inventory in the file 26 facts sheet. NEVER touches money counters.

```text
Threads: 1 scheduler (scan every P seconds) + M worker threads.

[SCHEDULER]
enrichScan():
  if breakerOpen(): return
  candidates = SELECT id FROM payment_request
               WHERE stage='ENRICH' AND stage_state='READY'
               ORDER BY created_at FETCH FIRST :N ROWS ONLY
               // lock-free — no FOR UPDATE, no SKIP LOCKED (§11)
  for c in candidates: workerPool.submit(enrich(c))

[WORKER]
enrich(id):
  TX(claim):
    lockObligation(id.scope)                     // lock order even here
    n = CAS request SET stage_state='CLAIMED', claimed_by=me,
        claim_expires_at=now+LEASE
        WHERE id AND stage='ENRICH' AND stage_state='READY'
    COMMIT
  if n == 0: return                              // lost the race — fine

  try:
    data = partyLookup(); accountLookup(); localSteps()
    // NO TRANSACTION OPEN — this can take seconds, hang, or die
  catch TRANSIENT (timeout, 5xx from reference services):
    TX(release): CAS CLAIMED → READY (WHERE claimed_by=me AND not
    expired) + backoff bookkeeping; COMMIT; return
    // if we die instead: lease expiry makes the row re-claimable —
    // lookups are repeatable reads, redo is safe (§7.3/§11)
  catch VALIDATION_FAILED (bad/missing reference data):
    TX(fail):
      lockObligation
      CAS CLAIMED → BLOCKED(<validation reason>)  // request-side detail
      derive(ob)   // frontend now shows the manual-action exception
      COMMIT; alert per §15; return

  TX(result):
    lockObligation
    n = CAS request SET stage='POST', stage_state='READY',
        claim fields cleared, enriched fields...
        WHERE id AND stage='ENRICH' AND stage_state='CLAIMED'
          AND claimed_by=me AND claim_expires_at > now
    derive(ob) if n == 1
    COMMIT
  if n == 0: discard results; return             // lease was taken over —
                                                 // our work is void, no write
```

------

## S3. PaymentExecutionService

Owns: the POST stage (one attempt = TWO transactions + the wire),
the lease-expiry sweep, the §9 resolver, freeze + breaker.

### S3.1 Posting scanner + worker

```text
[SCHEDULER]
postScan():
  if freezeEffective(): return                   // Hazelcast toggle set OR
                                                 // unreachable = frozen (fail-safe);
                                                 // zero attempts while effective
  if breakerOpen(): return
  candidates = lock-free read WHERE stage='POST' AND stage_state='READY'
               AND repost_permitted               // §7.0: divergent_payload_at IS
                                                  // NULL AND NOT MAYBE_SUBMITTED...
               FETCH FIRST :N ROWS ONLY
  for c in candidates: workerPool.submit(postAttempt(c))

[WORKER] — one attempt:
postAttempt(id):
  // ---------- TX1: the posting claim (write-ahead) ----------
  TX(postingClaim):
    lockObligation(id.scope)
    n = CAS request SET stage_state='CLAIMED', claimed_by=me,
        claim_expires_at=now+LEASE,
        attempt_count = attempt_count + 1,
        post_attempt_seq = post_attempt_seq + 1,   // monotonic §14.1
                                                   // identity — NEVER
                                                   // reset (attempt_count
                                                   // resets on §9.2)
        last_post_attempt_at = now
        WHERE id AND stage='POST' AND stage_state='READY'
          AND outcome IS NULL AND divergent_payload_at IS NULL
    if n == 0: rollback; return                  // lost race
    payload = assembleFRESH(admittedSnapshotVia(trade_snapshot_state))
              // never cached bytes; enriched + current data (§7.0)
    hash    = canonicalHash(payload)             // CA-6
    request.divergence_expected := (last_sent_hash IS NOT NULL
                                    AND last_sent_hash != hash)
    request.last_sent_hash := hash               // write-ahead: the DB knows what
                                                 // may be sent BEFORE any byte leaves
    [§14.1 rider 1: INSERT ATTEMPT_STARTED — same TX ALWAYS; the
     content write-ahead (full bytes iff hash changed, else
     content_ref — dedup); no byte leaves without this committed]
    COMMIT
  on commit-outcome-UNKNOWN (connection died mid-commit):
    DO NOT POST. Walk away. Either the claim never landed (row still
    READY) or the lease expires into MAYBE — both safe (§11).

  // ---------- THE WIRE: no TX, no locks ----------
  try:
    resp = engineClient.post(payload, idempotency_key)   // SDK mints the
                                                         // UETR in-flight
  catch provableTransportBeforeSend (e.g. connect refused):
    resp = TRANSPORT_ERROR_NOT_SENT
  catch anythingAfterBytesMayHaveLeft (timeout, reset, worker death):
    return                                       // say NOTHING; lease expiry →
                                                 // MAYBE; resolver owns it (§7.3)

  // ---------- TX2: outcome recording ----------
  TX(recordOutcome):
    lockObligation
    class = classify(resp)                       // CA-1 closed table; §7.2;
                                                 // body over status line
    switch class:
      ACCEPTED:
        CAS sub := SUBMITTED, stage := CONFIRM, stage_state := READY,
            claim cleared, submitted_at := now
        persist uetr                              // ACCEPTANCE-CLASS ONLY (§5)
        // NO money movement — acceptance is not execution
      SYNC_REJECT_TERMINAL:
        CAS outcome := REJECTED, sub := NOT_SUBMITTED, claim cleared
            + committed -= amount                 // rides THIS CAS, once
            + provider_rejected marker (L9)
      SYNC_ERROR_RETRYABLE (per CA-1):
        CAS CLAIMED → READY + backoff              // same key next attempt
      TRANSPORT_ERROR_NOT_SENT:
        CAS CLAIMED → READY + backoff              // provably unsent only
      DUPLICATE_REQUEST:
        CAS sub := MAYBE_SUBMITTED, stage := CONFIRM·READY + status query
        // a hidden earlier attempt surfaced; uetr NOT persisted (§5)
      KNOWN_KEY_DIFFERENT_PAYLOAD:
        request.divergent_payload_at := now       // write-once; §7.0 fence —
                                                  // no future re-POSTs
        if divergence_expected: sub := MAYBE_SUBMITTED + status query
                                                  // EXPECTED — evidence, no park
        else: stage_state := BLOCKED(ENGINE_INCONSISTENCY) + CRITICAL
      UNMAPPED:
        sub := MAYBE_SUBMITTED; stage_state := BLOCKED(UNMAPPED_CODE)
        + alert                                    // fail closed
    [§14.1 rider 2: INSERT ATTEMPT_RESOLVED on rowCount 1]
    derive(ob)                                     // frontend sees the outcome AND
                                                   // its exception atomically
    COMMIT
  after commit: alerts/acks; §14 line
```

### S3.2 Lease-expiry sweep

```text
[SCHEDULER]
sweepExpiredClaims():
  for row in lock-free read WHERE stage_state='CLAIMED'
                             AND claim_expires_at < now:
    TX(takeover):
      lockObligation
      if row.stage == 'ENRICH':
        CAS CLAIMED → READY (claim cleared)        // repeatable work — re-claimable
      if row.stage == 'POST':
        CAS CLAIMED → stage CONFIRM, stage_state READY,
            sub := MAYBE_SUBMITTED, maybe_since := coalesce(existing, now)
        // NEVER back to POST·READY: the payload may be at the engine;
        // re-POSTing here is the double-payment path (§11)
        [§14.1 rider 2: ATTEMPT_RESOLVED outcome=LEASE_EXPIRED_MAYBE]
      derive(ob)
      COMMIT
```

### S3.3 Resolver (§9) + escalation (§9.3)

```text
[SCHEDULER, resolver scope §9.5]
resolve(row):                                     // MAYBE rows (any stage_state
                                                  // except CLAIMED) + aged SUBMITTED
  answer = engine.statusQuery(idempotency_key)    // NO TX during the call
  switch answer (per CA-3 / §9.1):
    EXECUTED / REJECTED evidence:
      TX: lockObligation; evidence-guarded CAS (§10.1/§4.4);
          money per §3 (confirmed += after amount equality, or
          committed -= on terminal-negative); derive; COMMIT
    ACCEPTED (found, not yet executed):
      TX: CAS sub := SUBMITTED (query-proven, always-safe tightening)
    NOT_FOUND:
      if sub == SUBMITTED: BLOCKED(ENGINE_INCONSISTENCY) + CRITICAL
        // never downgrade an acknowledged payment (§9.1)
      else if withinTrustAge(last_post_attempt_at): keep MAYBE; requery
        // "not found" may mean "not yet" — patience is correctness (§9.2)
      else if pastTrustAge and lookbackStillValid (§7.4 aging)
              and repost_permitted:
        TX: CAS stage CONFIRM→POST, stage_state := RETRY_WAIT
            (next_retry_at = now; attempt_count resets,
             post_attempt_seq does NOT)
            // the ONE sanctioned stage regression; SAME key (§9.2)
            // sub REMAINS MAYBE_SUBMITTED — honest: it may have
            // landed; the re-POST's RESPONSE settles it
            // (acceptance → SUBMITTED; sync reject → NOT_SUBMITTED;
            //  DUPLICATE_REQUEST → MAYBE + query)
    INDETERMINATE (query failed): keep MAYBE; jittered backoff

[SCHEDULER]
escalate():                                       // §9.3, once per episode
  for row where sub=MAYBE_SUBMITTED and age(maybe_since) > threshold
            and escalated_at IS NULL:
    TX: CAS stage_state := BLOCKED(ESCALATED), escalated_at := now
        derive(ob); COMMIT
    page()                                        // ops takes over via S1.3
```

### S3.4 Graceful shutdown (§11)

```text
1. stop Kafka listener containers      // no new inbound work
2. stop scanners                        // no new claims
3. drain in-flight POSTs (bounded wait)
4. release ONLY the ENRICH claims this worker holds
   POST claims are NEVER released — a POST may be mid-flight;
   lease expiry into MAYBE is the only exit.
```

------

## S4. PaymentNotificationConsumerService (platform feed)

Owns: the asynchronous status feed (§8) — the only source of
EXECUTED money truth.

```text
[KAFKA LISTENER THREAD xN]
onFeedEvent(e):
  try:
    evt = parse(e)                                // fails → POISON: DLT + page + ack
    TX(consume):
      INSERT processed_inbound_event(evt.event_id)
        on duplicate key: COMMIT; ack; return     // first fence (M6)
      req = match by UETR (fallback per CA-2)
      if req is null:
        COMMIT; log + metric; ack; return         // unmatched: no storage, no
                                                  // replay (§8); §9 sweep recovers
      lockObligation(req.scope)
      rank = evidenceRank(evt.status)             // CA-2; §4.4 precedence
      n = CAS request dims per rank
          WHERE expected pre-state (outcome IS NULL for terminal writes)
      if n == 1:
        if evt.status == EXECUTED:
          if evt.amount.compareTo(req.amount) != 0:
            CAS stage_state := BLOCKED(AMOUNT_MISMATCH), sub stays
            SUBMITTED + CRITICAL; NO money movement (§8)
          else:
            outcome := EXECUTED; confirmed += req.amount
        if terminal-negative: committed -= req.amount   // once, on the CAS
      else:                                        // n == 0
        if req is terminal AND evt.event_id is NEW:
          CRITICAL (§8 — evidence against a terminal row)
        else: stale/duplicate — normal, no action
      derive(ob)                                   // frontend: confirmation or
                                                   // its exception, atomically
      COMMIT
    ack(e)                                         // strictly after commit
  catch TRANSIENT_INFRA: backoff; throw            // no ack, redelivery
  catch POISON: DLT + page; ack
```

------

## S5. Thread/ownership map (one page)

```text
Service           Threads                      May move money?   Talks to
Orchestration     Kafka xN (upstream), HTTP,   YES (create/      DB only
                  1 sweep                      cancel/latch)
Enrichment        1 scan + M workers           NEVER             DB + reference data
Execution         1 scan + M workers,          YES (reject       DB + engine
                  1 lease sweep, resolver,     decrement)        (POST + query)
                  escalation
NotificationCons. Kafka xN (feed)              YES (confirmed    DB only
                                               += / committed -=)
Every money movement: obligation lock held, rides a rowCount==1 CAS,
derive() in the same TX. No exceptions, no service-specific variants.
```

------

## S6. THE FRONTEND EXCEPTION CONTRACT (emphasized requirement)

Every exception state the system can enter MUST be reported to the
frontend. The spec's mechanism (§4.2, §12, §13) is exactly ONE
channel, and this file's pseudocode enforces it in every service:

```text
1. derive() runs in the SAME transaction as every applied mutation,
   under the obligation lock. It stores ui_step_status AND
   active_exception_code + manual-action flag + active_exception_at
   on the obligation (plus blocked_reason detail on the request).
   CONSEQUENCE: the frontend can never observe money state without
   its matching exception state — they commit together.
2. The catalog (§13) is CLOSED and ranked. Services never invent
   exception codes, never write free-text as a code, never skip
   derive() "because nothing user-visible changed".
3. The card (§12) READS the stored derivation — pull, not push; all
   obligations of the trade; freshness = the §15 lag metric. No
   bespoke per-exception endpoints, no frontend-side re-derivation.
4. Content rules: ops-readable text, account data masked (§16.3),
   never stack traces. No rule may key on display labels (§10.4).
5. A catch block that neither classifies (S0 taxonomy) nor rethrows
   is a review-failing defect: swallowed exceptions are invisible to
   the frontend, which violates this contract.

Where each service's exceptions surface (illustrative, catalog §13):
  intake validation failure      → DATA_VALIDATION_FAILED (+ anchor row visible)
  enrichment reference failure   → manual-action exception + BLOCKED reason
  POST ambiguous / MAYBE         → PAYMENT_OUTCOME_UNKNOWN
  definitive provider reject     → PROVIDER_REJECTED (+ count)
  payload collision (anomalous)  → ENGINE_INCONSISTENCY (CRITICAL)
  feed amount mismatch           → AMOUNT_MISMATCH (CRITICAL)
  overpay                        → OVERPAY_DETECTED (latch — one-way)
  aged MAYBE                     → ESCALATED (ops owns it)
```
