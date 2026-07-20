> **Purpose:** End-to-end pseudocode for the four spec services (PaymentOrchestrationService, PaymentEnrichmentService, PaymentExecutionService, PaymentNotificationConsumerService): threads, transactions, locks, money movements, exception handling, and the frontend exception contract — one readable flow per service.
> **When to use this file:** ONBOARDING ONLY — read once when joining the project to understand how the pieces fit. It is deliberately NOT part of any card's minimal reading set (rule 4): per-card work uses the card + packet + the cited spec sections + file 24. Never implement from this file.
> **Depends on:** requirment-v4.md (wins on every conflict); 24-implementation-mechanics.md (M1–M9 are the binding shapes); 26-team-execution-and-divergence-protocol.md (local reality may differ — record divergences, rule 21).
> **Used by:** onboarding only (deliberately outside the per-card context budget — review 7ab31e5 L2).
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
    doc = parse(record)                          // truly unparseable → POISON
                                                 // (no scope extractable — §6.6:
                                                 // no row possible; DLT + page)

    // ---- VALIDATION: ONE verdict for the WHOLE snapshot (§6.0) ----
    verdict = validateSnapshot(doc)              // schema, amounts, within-snapshot
                                                 // tuple uniqueness — evaluated for
                                                 // the WHOLE document, never per-block
    if verdict == INVALID:                       // §6.6 snapshot note (review
                                                 //): WHOLE-SNAPSHOT
                                                 // validation failure —
        // NO amounts applied from ANY block; upstream_ordering NOT
        // advanced; blast radius = the TRADE (deliberate, fail
        // closed): the marker lands on the UNION of the document's
        // extractable scopes AND every EXISTING obligation/anchor of
        // this business_id — including payments ABSENT from the
        // invalid document (§6.6 "existing obligations and anchors
        // alike"). Anchors are upserted only for
        // document-only scopes; existing rows just get the marker.
        targetScopes = extractableScopes(doc)
                       ∪ existingScopes(business_id)
        // NOTE (§6.6 consistency windows, ratified): (a) a scope
        // introduced LATER by a VALID out-of-order document carries
        // no marker and MAY create a request from that valid state;
        // (b) the concurrent race is SCHEDULE-DEPENDENT BY DECISION
        // (review 928341a H1 — no trade-row lock spans this pass):
        // a concurrently-created scope lands marked or unmarked
        // depending on schedule, BOTH outcomes correct — no
        // convergence claim, no trade-level fence, BY DECISION
        for scope in sortByScopeTuple(targetScopes):
          TX(anchorOrMark):
            ob = SELECT payment_obligation FOR UPDATE
                 (upsert anchor if absent: required := NULL — §6.6;
                  colliding tuples share one anchor)
            write validation_failed marker(doc.ordering)
                 // §6.9 monotonic; recorded as
                 // validation_failed_ordering; in-flight requests
                 // UNTOUCHED (the marker is not a state input)
            derive(ob)          // frontend: DATA_VALIDATION_FAILED
            COMMIT
        ack(record); return     // no admission, no fan-out; a later
                                // CORRECTED message applies normally

    // ---- ADMISSION: its own TX; FIRST lock in the global order ----
    TX(admission):
      snap = SELECT trade_snapshot_state WHERE business_id FOR UPDATE
             // insert-on-first-contact; PK-race → retry read (§6.1)
      if doc.ordering < snap.last_accepted_ordering:
          COMMIT; ack; return                    // STALE — refused WHOLE (§6.7)
      if doc.ordering == snap.last_accepted_ordering:
          if doc.digest == snap.last_payload_digest:
              pass                               // redelivery — re-run blocks (heals
                                                 // a crash mid fan-out; blocks no-op)
          else:
              rollback; alert AMENDMENT_TIE_CONFLICT (§6.7)
              ack; return                        // TIE: same ordering, DIFFERENT
                                                 // content — refused WHOLE; never
                                                 // applied, watermark untouched
      else:
          snap.watermark  := doc.ordering        // admit (strictly newer only)
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

        // (no invalid-block branch here: validation was a
        //  WHOLE-SNAPSHOT verdict above — every block in an
        //  admitted snapshot is valid by construction)
        if block.absentFromSnapshot:
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
                                                 // at the first posting claim (K-04);
                                                 // the INSERT also stamps
                                                 // required_total_at_creation :=
                                                 // ob.required — set-once display
                                                 // stamp, §2.2/§6.8, never read by
                                                 // any money logic)
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

getAllPaymentsTable(filter):                     // §12 TABLE projection
  SELECT obligation LEFT JOIN request            // (2026-07-17): pure read
  per joined row emit:
    row_type = REQUEST        (one row per request, request-id key)
             | OBLIGATION_ONLY (no request yet: obligation-id key,
               "no request created" + NULLABLE reason = the derived
               active exception when LIVE, else NULL with the
               status carrying the story (§6.2 covered-on-arrival →
               COMPLETED; retired anchor → CANCELLED /
               REMOVED_BEFORE_REQUEST); request fields n/a)
    + obligation context on EVERY row (required/committed/confirmed,
      ui_step_status, exception, reopened)
    + request fields on REQUEST rows (amount, §10.4 label,
      blocked_reason)
  // 120 required fulfilled as 100+20 → TWO REQUEST rows;
  // no duplicates by join construction (placeholder disappears the
  // moment the first request row exists); terminal rows visible
  // (history never laundered); removed scopes show CANCELLED context
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
  catch VALIDATION_FAILED (DEFINITIVE invalid data — §7.3 flow row):
    TX(fail):
      lockObligation
      CAS ENRICH·CLAIMED → outcome := REJECTED    // terminal, write-once
          + committed -= amount                    // release rides the CAS (§3)
          + validation_failed marker (ordering-tagged, §6.9)
      derive(ob)   // frontend: DATA_VALIDATION_FAILED exception;
                   // §6.8 creates NO successor until a NEWER message
                   // flips the marker not-live (corrected data comes
                   // through the front door, never a park-and-hope)
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
  candidates = lock-free read WHERE stage='POST'
               AND ( stage_state='READY'
                     OR (stage_state='RETRY_WAIT'                 // L7: retries live
                         AND next_retry_at <= now) )              // HERE, not READY
               AND repost_permitted               // §7.0: divergent_payload_at IS
                                                  // NULL AND NOT MAYBE_SUBMITTED...
                                                  // (§9.2-downgraded rows ARE
                                                  // RETRY_WAIT+MAYBE and DO pass)
               FETCH FIRST :N ROWS ONLY
  for c in candidates: workerPool.submit(postAttempt(c))

[WORKER] — one attempt:
postAttempt(id):
  if freezeEffective(): return                   // §16.1 BOTH-ENDS, end 1 of 2
                                                 //: a queued
                                                 // candidate may postdate the
                                                 // scan's check — re-read HERE,
                                                 // before ANY claim mutation.
                                                 // LINEARIZATION (§16.1, review
                                                 //): a worker past
                                                 // THIS check is IN FLIGHT — a
                                                 // flip after it may still see
                                                 // one claim commit; the
                                                 // pre-wire check stops the
                                                 // wire; propagation bound +
                                                 // drain own the boundary
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
        WHERE id AND stage='POST'
          AND (stage_state='READY' OR
               (stage_state='RETRY_WAIT' AND next_retry_at <= now))
          AND outcome IS NULL
          AND <ALL repost_permitted terms — §7.0/§11: divergent_payload_at
               IS NULL AND NOT the stale-amount MAYBE condition; the claim
               RE-CHECKS the full derived gate, never just the scan's view
               (review d00ef6a H4 — the scan-to-claim race)>
    if n == 0: rollback; return                  // lost race / gate closed
    payload = assembleFRESH(admittedSnapshotVia(trade_snapshot_state))
              // never cached bytes; enriched + current data (§7.0)
    hash    = canonicalHash(payload)             // CA-6
    request.divergence_expected := (last_sent_hash IS NOT NULL
                                    AND last_sent_hash != hash)
    request.last_sent_hash := hash               // write-ahead: the DB knows what
                                                 // may be sent BEFORE any byte leaves
    [§14.1 rider 1 (switch-gated): INSERT ATTEMPT_STARTED — same TX,
     FULL content every attempt; canonical failure rule: statement-
     local error → gap recorded, alert AFTER commit, claim proceeds;
     fatal → ordinary infra failure]
    COMMIT
  on commit-outcome-UNKNOWN (connection died mid-commit):
    DO NOT POST. Walk away. Either the claim never landed (row still
    READY) or the lease expires into MAYBE — both safe (§11).
  after commit: publish the buffered §14 line
    (attempt_event_type='ATTEMPT_STARTED') — best-effort,
    at-most-once (§14 delivery contract, 4098532 H1); publication
    failure NEVER blocks the wire call — the write-ahead identity
    in the DB, not the log, prevents duplicate payment.

  // ---------- THE WIRE: no TX, no locks ----------
  if freezeEffective(): return                   // §16.1 BOTH-ENDS, end 2 of 2:
                                                 // re-checked before the wire; an
                                                 // abandoned claim resolves via
                                                 // lease expiry (RC-09) — and the
                                                 // §14.1 switch drain waits for it
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
        CAS CLAIMED → RETRY_WAIT, next_retry_at per policy (L7)
                                                   // same key next attempt
      TRANSPORT_ERROR_NOT_SENT:
        CAS CLAIMED → RETRY_WAIT, next_retry_at per policy (L7)
                                                   // provably unsent only
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
    [§14.1 rider 2 (switch-gated): INSERT ATTEMPT_RESOLVED on
     rowCount 1 — canonical failure rule as rider 1]
    derive(ob)                                     // frontend sees the outcome AND
                                                   // its exception atomically
    COMMIT
  after commit: alerts/acks; publish the buffered §14 line
    (best-effort, at-most-once — §14 delivery contract, 4098532 H1)
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
        [§14.1 rider 2 (switch-gated): ATTEMPT_RESOLVED
         outcome=LEASE_EXPIRED_MAYBE — canonical failure rule]
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
      if sub == SUBMITTED:
        if withinTrustAge(submitted_at): keep SUBMITTED; requery
          // §2.2/§9.2 SUBMITTED-branch trust-age — feed/index lag
          // is normal; "not found" may mean "not indexed yet"
        else: BLOCKED(ENGINE_INCONSISTENCY) + CRITICAL
          // never downgrade an acknowledged payment (§9.1); the
          // park is REVERSIBLE — row stays in resolver scope (§9.5)
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
      // §8: the amount-equality GUARD runs BEFORE any terminal
      // outcome write and before any money movement
      if evt.status == EXECUTED
         and evt.amount.compareTo(req.amount) != 0:
        CAS stage_state := BLOCKED(AMOUNT_MISMATCH),
            sub := SUBMITTED                    // SET, not "stays" — §8:
            // settlement evidence TIGHTENS even a MAYBE row (review
            //); outcome NOT written; CRITICAL;
            // NO money movement — the guard PRECEDES the outcome CAS
      else:
        n = CAS request dims per rank
            WHERE expected pre-state (outcome IS NULL for terminal writes)
        if n == 1:
          if evt.status == EXECUTED:
            outcome := EXECUTED (in that CAS); confirmed += req.amount
          if terminal-negative: committed -= req.amount // once, on the CAS
        else:                                      // n == 0
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
