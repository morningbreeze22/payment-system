> **Purpose:** Test matrix T-01..T-38 with setup/action/expected/failure-meaning/type/blocking per test (original Section J; seeds companion artifact CA-7).
> **When to use this file:** When writing a task card's tests and when assembling GO-04 gate evidence.
> **Depends on:** requirment-v4.md sections cited per test; 12-companion-artifacts.md (CA-7).
> **Used by:** All test-bearing task cards; 17-go-live-checklist.md evidence column.
> **Safe to transfer:** yes
> **Contains local code names:** no

# J. Test-first strategy and test matrix

Strategy: every behavior task card lists its tests; where practical
write the test first against the § it implements (red), implement
(green), then run the surrounding suite. Types: UNIT / INTEGRATION
(real Oracle + Kafka where constraints/consumption are under test) /
CONTRACT (sandbox or build-time schema) / MIGRATION / RECOVERY
(crash/fault injection) / CONCURRENCY / OPERATIONAL (drill, seeded
alert). This matrix seeds CA-7; IDs are stable. "Blocking" = must be
green for go-live (Section Q).

### T-01 — Deterministic key generation

```text
Section: §5.1        Type: UNIT           Blocking: YES
Purpose: key derived from business state via CA-5, never random.
Setup:   fixed scope fields + seq; CA-5 spec loaded.
Action:  derive twice, across JVM restarts.
Expect:  identical keys; derivation uses only the CA-5 input list.
Failure: generation is random or environment-dependent → DR keystone
         broken; a restore would mint fresh keys → duplicate payment.
Implemented by: K-02.
```

### T-02 — Key byte-exactness / golden vectors

```text
Section: §5.1, §16.6-5   Type: UNIT       Blocking: YES
Purpose: freeze the derivation to the byte across releases.
Setup:   CA-5 golden-vector file (authored independently of the code).
Action:  derive each vector's inputs.
Expect:  exact expected bytes, every vector; version constant pinned.
Failure: canonicalization/encoding drift → post-restore or
         cross-release keys diverge → engine dedup silently misses.
Implemented by: K-03.
```

### T-03 — Key persistence before POST

```text
Section: §5, §11      Type: RECOVERY (fault injection)   Blocking: YES
Purpose: no POST under an unpersisted caller-supplied identity.
Setup:   posting path with stub engine; kill point between claim
         commit and HTTP call.
Action:  drive a payment; kill at the point; inspect DB + stub.
Expect:  row holds identity+hash (committed); stub received nothing.
Failure: identity written late → a crash leaves an untraceable
         possible submission; §9 recovery has no key to query.
Implemented by: K-04.
```

### T-04 — Same business input → same key

```text
Section: §5.1         Type: UNIT           Blocking: YES
Purpose: re-creation of the same logical request regenerates the key.
Setup:   same obligation state (same seq about to be consumed).
Action:  create, roll back harness-side, re-create.
Expect:  identical key both times.
Failure: hidden nondeterministic input in the derivation.
Implemented by: K-02, K-06(3).
```

### T-05 — Different sequence/business identity → different key

```text
Section: §5.1         Type: UNIT           Blocking: YES
Purpose: key space separates attempts and scopes.
Setup:   vary request_seq; vary each scope field independently.
Action:  derive.
Expect:  distinct keys per variation.
Failure: collision across seq/scope → I6-adjacent identity collision;
         engine dedup would swallow a legitimate new payment.
Implemented by: K-02.
```

### T-06 — Amount is not part of the key

```text
Section: §5.1         Type: UNIT           Blocking: YES
Purpose: replay with a diverged amount must regenerate the SAME key
         (routed to §7.2, never silently re-keyed).
Setup:   same scope+seq, two amounts.
Action:  derive.
Expect:  identical keys.
Failure: amount hashed in → post-restore divergent replay mints a
         fresh key → duplicate payment (the exact §5.1 rationale).
Implemented by: K-02.
```

### T-07 — last_sent_hash / divergence_expected behavior

```text
Section: §2.2, §7.0   Type: INTEGRATION    Blocking: YES
Purpose: flag computed at claim time against the PRIOR hash; drives
         the §7.2 collision branch.
Setup:   stubbed enrichment permitting controlled assembly changes.
Action:  attempt 1; attempt 2 unchanged; attempt 3 with changed detail.
Expect:  flags false / false / true; hashes persisted pre-wire each
         attempt; last_post_attempt_at stamped pre-wire.
Failure: flag wrong → expected divergence parks as CRITICAL (ops
         noise) or replay anomaly sails through as expected (missed
         defect).
Implemented by: K-05.
```

### T-08 — Retry after crash BEFORE provider POST

```text
Section: §5, §11      Type: RECOVERY       Blocking: YES
Purpose: crash between claim commit and wire → safe re-attempt.
Setup:   kill point pre-wire; lease expiry configured short.
Action:  crash; let the lease expire; observe recovery.
Expect:  row → CONFIRM·READY·MAYBE (no carve-out); resolver queries;
         NOT_FOUND path eventually re-POSTs the SAME key via §9.2
         where permitted.
Failure: re-claim for posting or a fresh key → double-payment path.
Implemented by: ST-10, RC-07, K-06(1).
```

### T-09 — Retry after crash AFTER provider POST

```text
Section: §7.2, §9, §11   Type: RECOVERY    Blocking: YES
Purpose: crash after wire, before response processing.
Setup:   stub that accepts, kill point post-wire pre-processing.
Action:  crash; lease expiry; resolver runs against the stub.
Expect:  row MAYBE; resolver query finds EXECUTED-class → settlement
         applies through the evidence path; reservation confirmed.
Failure: blind re-POST, or the outcome never recovered → §9's
         ask-not-retry principle broken.
Implemented by: ST-10, RC-05/06, K-06(2).
```

### T-10 — DB restore / lost local row

```text
Section: §5.1         Type: RECOVERY (harness-simulated)  Blocking: YES
Purpose: a restore-recreated request regenerates the SAME key and the
         engine collision routes to §7.2, not a new payment.
Setup:   full flow to POSTed against a recording stub; harness resets
         DB rows to the pre-insert image (simulated restore).
Action:  re-drive creation for the same shortfall; POST.
Expect:  identical key on the wire; stub's duplicate answer routes to
         MAYBE + query (§7.2); no second execution recorded.
Failure: fresh key → the exact restore-duplicate §5.1 exists to kill.
Implemented by: K-06(3).
```

### T-11 — Known key + same payload (engine contract)

```text
Section: §18-1(a), §1   Type: CONTRACT (sandbox)   Blocking: YES
Purpose: prove the assumed contract fact: identical re-POST never
         executes twice.
Setup:   CT-01 harness.
Action:  POST; re-POST byte-identical.
Expect:  deduped/acked or original-response replay; ONE execution.
Failure: §18-1 FAILS — the entire re-POST design (fresh assembly,
         §9.2 downgrade) is unsafe; go-live blocked; escalate.
Implemented by: CT-02.
```

### T-12 — Known key + divergent payload (engine contract)

```text
Section: §18-1(b), TL-4   Type: CONTRACT (sandbox)   Blocking: YES
Purpose: divergent re-POST rejected without execution, code
         distinguishable from plain DUPLICATE_REQUEST.
Setup:   CT-01 harness.
Action:  POST; re-POST with changed field; separately changed amount.
Expect:  rejection, no execution, distinct code captured for CA-1.
Failure: execution → double-pay path; §7.0 must revert to payload
         freeze (TL-4) — a design decision, halt and escalate.
Implemented by: CT-03.
```

### T-13 — Rejected-outcome retention (engine query)

```text
Section: §9.1, §18-1(d), TL-6   Type: CONTRACT (sandbox)   Blocking: YES
Purpose: rejected outcomes are retained and queryable; re-POST
         behavior after a sync business reject is KNOWN.
Setup:   CT-01 harness; inducible business reject.
Action:  induce reject; query by key; re-POST same key.
Expect:  query returns a REJECTED-class answer; re-POST behavior
         recorded (re-executes vs replays) and fed to retry policy.
Failure: rejects unqueryable → §9 recovery blind for rejects;
         unknown re-POST behavior → retry policy built on assumption.
Implemented by: CT-05, CT-06.
```

### T-14 — Provider query lookback

```text
Section: TL-5, §9.3    Type: CONTRACT (sandbox/provider-assisted)   Blocking: YES
Purpose: lookback ≥ max row lifetime incl. ops-queue SLA — past it,
         NOT_FOUND is unfalsifiable and resolve-via-query can never
         succeed.
Setup:   B-02's written lookback; aged sandbox data where achievable.
Action:  query the oldest achievable executed payment by key.
Expect:  found within the stated lookback; statement + evidence filed;
         if lookback < max lifetime → §18-3 alternative dies and the
         OP operation is mandatory (it is anyway, by default).
Failure: unverified lookback → aged MAYBE rows may be permanently
         unresolvable by query.
Implemented by: CT-04/CT-06 evidence + B-02.
```

### T-15 — UETR persisted from acceptance-class only

```text
Section: §5           Type: INTEGRATION    Blocking: YES
Purpose: acceptance (incl. original-response replay) persists the
         SDK-minted UETR for feed matching.
Setup:   stub returning acceptance with a UETR.
Action:  POST; inspect row; deliver a feed event under that UETR.
Expect:  uetr persisted; feed event matches and settles.
Failure: no UETR → all feed events unmatched; recovery only at sweep
         latency (degraded, not unsafe — but §5's design intent).
Implemented by: U-01, U-03.
```

### T-16 — UETR NOT persisted from rejection/collision

```text
Section: §5, §7.2, §16.6-6   Type: INTEGRATION   Blocking: YES
Purpose: dead UETRs never persisted or overwritten — a feed reject
         under a dead UETR must never release a real payment's
         reservation.
Setup:   stub returning DUPLICATE_REQUEST / collision / sync reject,
         each carrying a UETR; one row with a prior persisted uetr.
Action:  drive each response class.
Expect:  uetr unchanged (prior value or NULL) in every case; a feed
         event under the dead UETR goes unmatched.
Failure: dead UETR persisted → orphaned real feed events + possible
         false authoritative negative → double payment.
Implemented by: U-01, U-03.
```

### T-17 — Duplicate prevention (end to end)

```text
Section: §5, §2.2, §6.8   Type: INTEGRATION   Blocking: YES
Purpose: the layered protections hold: UNIQUE(idempotency_key), I6,
         single creation point, engine dedup handling.
Setup:   full local flow with stub engine.
Action:  attempt to create a second active request for one obligation;
         attempt to POST a duplicated key.
Expect:  I6 rejects the second active row; duplicate key POST answers
         route to §7.2 MAYBE+query; no path creates two executions.
Failure: any silent second execution path.
Implemented by: K-06, S-05, RG-06.
```

### T-18 — Concurrent workers

```text
Section: §11          Type: CONCURRENCY    Blocking: YES
Purpose: claims, CAS row counts, and the §11 claim protocol
         (lock-free selection, obligation-first per-item claim)
         make concurrent scanners safe. (T-34 covers lock-order/
         deadlock freedom under interleaving.)
Setup:   two scanner instances; seeded READY/RETRY_WAIT rows; two
         concurrent feed duplicates (rebalance case).
Action:  run concurrently.
Expect:  each row processed once; losers see row-count 0 / duplicate
         key; ZERO ORA-00060 (any deadlock is a §11 lock-order
         regression to FIX, never to retry-normalize — see T-34;
         corrected 2026-07-11).
Failure: double processing → duplicate POST risk; lock-order
         violation → deadlock storms (§15 tripwire).
Implemented by: ST-09, IN-05.
```

### T-19 — Stale request state (evidence + mirror rules)

```text
Section: §4.4, §10.1, §6.9   Type: INTEGRATION   Blocking: YES
Purpose: stale/duplicate/racing writes affect zero rows.
Setup:   rows in terminal and post-evidence states; replayed and
         re-keyed duplicate events; late POST responses.
Action:  apply each stale input.
Expect:  zero-row CAS everywhere; terminal rows unchanged (freeze);
         markers not overwritten by stale ordering values.
Failure: a stale input regressing state → the entire evidence
         model's guarantee is void.
Implemented by: ST-02/03, IN-04, IN-07.
```

### T-20 — Amount amendment after request creation

```text
Section: §6.3, §6.4, §6.8   Type: INTEGRATION   Blocking: YES
Purpose: amendments defer/cancel/park correctly; amounts immutable
         per request.
Setup:   in-flight requests at each cancellable/uncancellable state.
Action:  apply increase and decrease amendments.
Expect:  increase → deferred successor (never lost); decrease →
         auto-cancel where NOT_SUBMITTED-cancellable; MAYBE → parked
         AMENDMENT_PARKED; POST·CLAIMED deferred to retry-guard;
         request.amount never mutated.
Failure: lost deferral (underpayment), or a MAYBE release
         (double-pay), or in-place amount mutation.
Implemented by: RG-06, RG-07.
```

### T-21 — RETIRED (round 10 — the engine owns the cutoff calendar)

```text
RETIRED 2026-07-11 (PO calendar answer, §7.4/§18-2): no local
cutoff, calendar, or tz machinery exists to test. A late
submission is an ordinary engine response classified per CA-1
(covered by T-13/T-16 classification tests). ID kept — IDs are
stable; body retained in git history.
```

### T-22 — MAYBE_SUBMITTED recovery (full lifecycle)

```text
Section: §9.1–9.5     Type: INTEGRATION    Blocking: YES
Purpose: the ask-then-retry loop resolves every MAYBE class.
Setup:   MAYBE rows: fresh; aged past trust-age; parked
         (AMENDMENT_PARKED); escalated; SUBMITTED aged.
Action:  resolver sweeps against a scripted stub (EXECUTED / REJECTED
         / NOT_FOUND / INDETERMINATE / ACCEPTED sequences).
Expect:  per §9.1/§9.2: settle / reject+release / downgrade-or-wait /
         reschedule / tighten; escalation fires once per episode;
         parked rows keep being queried (§9.5 scope).
Failure: any MAYBE class with no exit → wedge (see T-28); scope
         misses parked rows → the four-round scoping lesson repeats.
Implemented by: RC-05..08.
```

### T-23 — Repost forbidden by staleness / divergence / freeze

```text
Section: §7.0, §6.4, §9.2   Type: INTEGRATION   Blocking: YES
Purpose: repost_permitted's terms hold at BOTH ends; no livelock.
         (cutoff term RETIRED round 10 — engine owns the calendar.)
Setup:   MAYBE rows with: stale amount; set
         divergent_payload_at; freeze ON.
Action:  attempt downgrade, ops-style un-park, and direct claim.
Expect:  each denied at the writer AND (if forced) at the claim; rows
         stay parked wait-then-decide; no park⇄un-park cycling.
Failure: laundered blocked_reason or writer bug reaching the wire.
Implemented by: RC-03, RC-07.
```

### T-24 — apply-platform-verified-outcome operation

```text
Section: §9.3, §16.6-8   Type: INTEGRATION + OPERATIONAL   Blocking: YES
Purpose: the MVP terminal exit works and refuses correctly.
Setup:   real Oracle with S-06 triggers; seeded MAYBE rows.
Action:  execute with valid dual-control inputs (both outcomes); then
         each refusal case (CLAIMED, terminal, same approver twice,
         missing ticket, amount mismatch).
Expect:  outcomes applied with money effects + alert + audit line;
         every refusal refuses; raw SQL equivalent fails on the
         trigger.
Failure: operation bypassable or refusals soft → the single
         sanctioned manual path isn't the single path.
Implemented by: OP-01/02; drilled by OP-03.
```

### T-25 — Evidence session flag / release guard

```text
Section: §10.3, §10.1   Type: INTEGRATION (real Oracle)   Blocking: YES
Purpose: the trigger backstop rejects unflagged terminal-negative
         writes on MAYBE/SUBMITTED rows; flag never leaks.
Setup:   S-06 deployed; pooled connections.
Action:  unflagged UPDATE (refused); flagged via the authoritative
         path (accepted); flag-leak probe across two pooled sessions.
Expect:  per the above; fat-finger SQL fails loudly.
Failure: guard bypass or cross-session leak → silent money release.
Implemented by: S-06, RG-05.
```

### T-26 — Reservation release

```text
Section: §3           Type: INTEGRATION    Blocking: YES
Purpose: −committed exactly once, same transaction, terminal-negative
         only.
Setup:   active requests reaching REJECTED / CANCELLED / SUPERSEDED,
         with event redelivery.
Action:  drive each; redeliver.
Expect:  one decrement each; redelivery zero-row → no second move;
         I1 holds throughout.
Failure: double release → shortfall reopens → double payment via
         §6.8.
Implemented by: RG-02.
```

### T-27 — Reservation confirmation / consumed state

```text
Section: §3, §4.1     Type: INTEGRATION    Blocking: YES
Purpose: settlement confirms exactly; completion derives; the
         "accepted money" derived figure needs no counter.
Setup:   requests settling normally and after amendment increases.
Action:  settle; inspect amounts + derived status.
Expect:  confirmed += amount once; committed unchanged by settlement
         (EXECUTED rows stay in I1); step completes only when §4.1
         holds; I2/I3 hold.
Failure: confirmation double-applied or completion derived early.
Implemented by: RG-03, RG-08.
```

### T-28 — Permanent wedge prevention

```text
Section: §18-3, §9.3, §4.1, §6.8   Type: INTEGRATION   Blocking: YES
Purpose: NO MAYBE row is permanently wedged at MVP.
Setup:   the worst row: divergent_payload_at set +
         stale amount + (simulated) key past query lookback.
Action:  walk the escalation path to the OP-01 operation; apply a
         platform-verified REJECTED; then a remaining shortfall.
Expect:  reservation released legitimately; scope completes or
         re-pays under a NEW key via §6.8 (guards permitting); I6
         frees.
Failure: any state in the chain with no sanctioned exit → the §18-3
         wedge exists.
Implemented by: OP-02's wedge assertion + RC-07/RC-08.
```

### T-29 — Drift scanner

```text
Section: §3, §10.3 L9, §6.6   Type: INTEGRATION + OPERATIONAL   Blocking: YES
Purpose: I1/I2 violations page; read skew does not; L9 verified.
         PLUS (non-blocking sub-case, b1d91dc M1) the §6.6
         accepted-window candidate diagnostic: query correctness
         only — it is OPTIONAL and ON-DEMAND, so NO schedule, index,
         or EXPLAIN-plan assertions exist for it.
Setup:   seeded counter corruption; concurrent uncommitted create;
         REJECTED row missing its marker; seeded escape-schedule
         window (sibling request created after a LIVE
         validation_failed anchor, below its failure ordering) +
         an ordinary request created BEFORE the failure.
Action:  run the scan; run the candidate diagnostic one-shot.
Expect:  corruption pages after locked re-check; skew does not page;
         L9 violation detected; the seeded window row IS emitted as
         LOWER_ORDER_SIBLING_REQUEST_AFTER_VALIDATION_MARKER_CANDIDATE
         (masked), the pre-failure request is NOT; the candidate is
         a metric/log event only — never a page or gate.
Failure: silent drift → the deliberate counter redundancy pays
         nothing; candidate auto-classified or paging → violates
         the §6.6 manual-triage-only decision.
Implemented by: OB-01.
```

### T-30 — Reconciliation tripwire

```text
Section: §8, §15      Type: INTEGRATION    Blocking: YES
Purpose: evidence-for-terminal is CRITICAL; benign redeliveries are
         silent.
Setup:   terminal REJECTED row; new-event_id settlement for it;
         known-event_id redelivery.
Action:  deliver both.
Expect:  new event → CRITICAL alert (zero-row CAS detected); known
         event → silent inbox skip.
Failure: the replay-divergence signature (a §5.2 tripwire) missed, or
         redelivery noise paging humans.
Implemented by: OB-02, IN-07.
```

### T-31 — UI/card step status correctness

```text
Section: §12, §4.1, §4.2, §10.4   Type: INTEGRATION   Blocking: YES
Purpose: the card never lies: no false completion, correct exception
         precedence, correct multi-obligation lookup.
Setup:   scopes at each §4-derivable state incl. anchors, MAYBE rows,
         latched overpay, reopened steps; a multi-payment trade
         (several obligations under one business_id).
Action:  read through the card path AND the §12 all-payments TABLE
         projection (request-granular; added 2026-07-17).
Expect:  NOT_STARTED = absence; a zeroed removed payment shows
         CANCELLED, never COMPLETED (round 11 — §4.1 second
         branch); anchors show DATA_VALIDATION_FAILED;
         MAYBE shows PAYMENT_OUTCOME_UNKNOWN (rank 1, never
         SYSTEM_UNAVAILABLE); labels per §10.4; the multi-payment
         trade returns ALL its obligations, one entry per payment —
         result count is never an error or alert (§12);
         unavailable ≠ stale-as-authoritative.
         TABLE projection cases (§12 contract, review 7ab31e5 M4):
         - obligation with NO request → exactly one OBLIGATION_ONLY
           row: scope tuple + required (blank for an anchor) +
           "no request created" + the derived-exception reason;
           request fields n/a;
         - single request → exactly one REQUEST row, placeholder
           GONE (no duplicate — join construction);
         - required 120 fulfilled as 100 + 20 → exactly TWO REQUEST
           rows (amounts 100, 20), both carrying required 120 and
           cumulative counters;
         - mixed active/terminal (REJECTED predecessor + live
           successor) → both rows visible, history never laundered;
         - fully removed scope (required = 0) → rows remain,
           obligation context CANCELLED;
         - reappearance after CANCELLED → rows return to
           IN_PROGRESS context, no duplicate placeholders;
         - NULLABLE-reason edges (review d00ef6a M2): a §6.2
           covered-on-arrival scope → OBLIGATION_ONLY row,
           COMPLETED, reason NULL; an anchor retired by absence →
           OBLIGATION_ONLY row, CANCELLED, reason NULL
           (REMOVED_BEFORE_REQUEST display note);
         - API contract (§12, LIVE-BROWSE semantics — review
           c8a92f1 M2): deterministic ordering + keyset pagination —
           no row appears TWICE within one traversal; under
           CONCURRENT WRITES cross-page completeness is NOT asserted
           (the test inserts rows mid-traversal and asserts the
           live-browse contract, never exactly-once enumeration);
           estate mode REFUSES unbounded queries; server-side
           authorization scoping enforced; composite row key
           (row_type, source_id) stable across refreshes; the estate
           query's captured plan rides CA-4's §12 estate index.
Failure: false completion (the predicate's whole point), a
         multi-payment trade surfacing as an error or partial
         result, a duplicate placeholder beside a request row, or
         100+20 collapsed into one synthetic row.
Implemented by: RG-08/09, ST-04.
```

### T-32 — Observability / alerting

```text
Section: §15, §16.6   Type: OPERATIONAL    Blocking: YES
Purpose: the alert surface works before rollout relies on it.
Setup:   seeded condition per §15 entry (table-driven); a simulated
         breaker-OPEN storm; a dead gauge; a mis-ordered config set.
Action:  fire each.
Expect:  every alert fires on its anchor clock with its runbook link;
         storm groups to ONE incident preserving detail; dead gauge
         alerts; config loader rejects the bad ordering at startup.
Failure: silent alert gaps discovered during a real incident instead.
Implemented by: OB-03..07.
```

### T-33 — Interim ops surface (operations + queue views)

```text
Section: §20, §6.7, §10.1   Type: INTEGRATION   Blocking: YES
Purpose: the §20 NON-WAIVABLE minimal exit set (verified-outcome,
         supersede/close, reprocess-snapshot) works before go-live,
         and the waivable ergonomics endpoints work where built.
Setup:   seeded rows per dead-end class (BLOCKED·NOT_SUBMITTED,
         BLOCKED·MAYBE, stalled ENRICH, overpay latch); a recorded
         tie-conflict (XML storage id + tied ordering) whose STORED
         snapshot has one changed and one identical block; Oracle
         lane with the REAL triggers.
Action:  run each OP-04a..e endpoint/view (+ RG-05 supersede/close) with
         valid and invalid inputs; query the four views.
Expect:  retry → SAME-stage RETRY_WAIT (an ENRICH row re-enriches);
         reject releases + sets the L9 marker, NOT_SUBMITTED only —
         a MAYBE row is refused at the code layer AND the trigger
         layer (raw-SQL demo); reprocess-snapshot executes by
         approval_id, re-fetches and HARD-REFUSES on digest
         mismatch BEFORE any consumption or lock
         (content-changed-behind-id → alert fired — round 4; the
         refused approval is NOT burned), consumes AT START
         (round 5: crash-after-consume seeded → approval burned,
         nothing applied, a NEW approval of the same document
         applies the remainder — the consumed one stays refused),
         enters through the §6.1 ADMISSION gate (≥ relaxation at
         admission; a document older than the trade watermark is
         refused whole even with a valid approval), RE-VERIFIES
         the tie server-side PER BLOCK (§20-10 algorithm; a
         non-tying or wrong-business_id document is refused — no
         relaxation; run the artifact-6(d) mixed-snapshot set),
         amends exactly the changed block, converges a
         trade-reference-only tie via the admission row update
         (re-run digest-equal → no-op), no-ops on re-run,
         respects the §6.5 latch, and cleanly REFUSES a purged xml
         id (no partial apply); approval-workflow negatives refused
         (expired / replayed-consumed / identical identities /
         concurrent double-execution — one CONSUMED CAS wins);
         missing ticket / unauthorized role refused; every call
         writes the §14 MANUAL_OPS line; views rank ESCALATED first
         and list one row per obligation.
Failure: any dead-end exit that works only via raw SQL, or an
         endpoint whose audit inputs are optional in practice.
Implemented by: OP-04a..OP-04e (pre-split round 9), RG-05.
```

### T-34 — Claim protocol: lock order, lost races, deadlock freedom

```text
Section: §11 (claim protocol, decided 2026-07-11)   Type: INTEGRATION
Blocking: YES
Purpose: the scanner claim protocol cannot invert the global lock
         order or double-claim; real Oracle, real contention.
Setup:   one obligation with a claimable request; concurrent actors
         on real Oracle: retry scanner, feed consumer applying
         evidence, auto-cancel amendment, second scanner instance.
Action:  TWO LANES (round 16). DETERMINISTIC lane: separate
         physical DB sessions with barriers/latches at transaction
         boundaries forcing exact schedules for scanner-vs-feed,
         scanner-vs-auto-cancel, two scanners, lease-expiry-vs-
         worker-completion, evidence-vs-terminal-operation, and
         admission/fence races. STRESS lane: randomized scheduling
         over thousands of iterations, bounded lock waits,
         REPRODUCIBLE SEEDS, Oracle wait-event capture. Plus
         session-kill / connection-loss at the four commit
         boundaries: claim commit, POST-response persistence,
         approval consumption, fan-out block commit. Instrument
         lock acquisition order throughout.
Expect:  every both-table transaction locks the obligation FIRST
         (candidate selection takes NO row locks); exactly one actor
         wins each claim (CAS rowCount 1), losers see 0 rows and
         skip silently; zero ORA-00060 across the run; claim/unclaim
         triggers no §4 re-derivation; deterministic-lane
         assertions per forced schedule: row counts, final tuples,
         amount invariants, lock acquisition order; recorded with
         the evidence: exact Oracle edition/version, isolation
         level, driver/pool versions, DDL, seeds, lock/deadlock
         traces (round 16 — "no deadlock observed under load"
         alone is NOT proof; the deterministic lane is the proof;
         H2/mock-DB results NEVER satisfy this gate).
Failure: any FOR UPDATE on payment_request before the obligation
         lock in the same transaction, or a deadlock under
         interleaving.
Implemented by: ST-09..11, RC-04/RC-05 (protocol per §11 + mechanics
M5), OB-xx dashboards unaffected.
```

### T-35 — Snapshot admission gate + trade-snapshot fence (rounds 5–7)

```text
Section: §6.1, §2.4, §20-10   Type: INTEGRATION / CONCURRENCY
Blocking: YES
Purpose: a stale snapshot can neither mutate NOR CREATE anything;
         BLOCK transactions serialize on the trade row and pass the
         trade-snapshot FENCE (round 6 — admission alone is a
         point-in-time fact, not ownership); §6.1 BLOCK-LEVEL
         SUPERSESSION is the ratified outcome rule (round 7 — NOT
         full-snapshot convergence); the reference-only tie
         converges.
Setup:   real Oracle; trade with snapshot S2 (ordering 200, payment
         A only) APPLIED; delayed snapshot S1 (ordering 100,
         payments A + B) available in the XML store; a second trade
         with NO rows; a reference-only tie pair (equal ordering,
         identical blocks, different trade reference); a harness
         that can PAUSE a fan-out worker between transactions and
         KILL it.
Action:  deliver S1 after S2; run two concurrent FIRST snapshots
         with disjoint scopes on the empty trade; pause a reprocess
         fan-out AFTER admission and again AFTER block 1, admit a
         newer live snapshot, resume the paused worker; kill the
         paused worker instead of resuming; run a zombie consumer
         re-applying an already-converged document; deliver a
         failed-validation message; run the reference-only tie
         through detection + approved reprocess + re-run.
Expect:  S1 refused WHOLE at admission (stale metric): A untouched,
         B NEVER created, no payment_request for B ever exists (the
         round-5 H-1 trace); concurrent first snapshots serialize
         on the trade-row insert/lock and the outcome follows
         BLOCK-LEVEL SUPERSESSION (round 7 — deterministic per
         schedule, NOT "both scopes exist": blocks the older worker
         applied BEFORE the newer admission exist; its unapplied
         remainder is abandoned — test BOTH paused schedules:
         supersession before block 1 → only the newer document's
         scopes exist; supersession after block N → all of the
         older document's scopes exist; each abandoned block is
         logged with scope identifiers + counted); the paused
         worker's NEXT block transaction locks the trade row, sees
         the admitted (ordering, digest) no longer current, ABORTS
         the fan-out, and creates/mutates NOTHING (the §9.3
         consumed-without-completion alert fires for the reprocess
         case); the killed worker's document converges via
         redelivery (intake) or is correctly refused on
         re-approval when a newer snapshot owns the trade
         (reprocess); the zombie's blocks all no-op; the
         failed-validation message advances NEITHER
         trade_snapshot_state nor upstream_ordering — AND (review
         c8a92f1 H3) its validation_failed marker lands on the
         UNION of the document's extractable scopes and EVERY
         existing obligation/anchor of the trade: regression case =
         last valid snapshot carried A + B, the invalid document
         carries only a malformed A block → B's existing obligation
         is ALSO marked (no new request creation for B until a
         corrected message), while A gets its anchor/marker;
         DEFINED-WINDOW trace (review 4d5cb83 H1 — ratified
         behavior, NOT a defect): valid 100 (A only) → invalid 200
         (malformed A; A marked at 200) → out-of-order VALID 150
         introducing NEW scope B → 150 ADMITS (150 > 100), B's
         obligation AND request ARE created (from valid-150 state —
         §6.6 consistency window (a)), and A remains marker-blocked
         until a valid document newer than 200; the concurrent
         invalid-200/valid-150 race is SCHEDULE-DEPENDENT BY
         DECISION (review 928341a H1 — no serialization exists):
         BOTH end states are asserted as ALLOWED — (i) B created
         after enumeration → B unmarked, proceeds; (ii) B created
         before enumeration → B carries the live marker, its
         in-flight request untouched, successors blocked + UI shows
         DATA_VALIDATION_FAILED until a newer valid document; the
         test runs both schedules and rejects any THIRD state;
         reference-only tie: detected at admission (digest differs),
         approved reprocess updates ONLY the trade row (blocks
         no-op), §7.0 assembly reads the new reference, re-run is
         digest-equal → no-op (converged, no repeat tie alert).
Failure: any block transaction that applies or creates without
         holding the trade lock and passing the fence IN THAT
         transaction; an abandoned block that is not logged; a tie
         that re-alerts after adjudication.
Implemented by: S-10, IN-02, OP-04c (reprocess entry path).
```

### T-36 — RETIRED (round 10 — greenfield: no bootstrap exists)

```text
RETIRED 2026-07-11 (PO fact, §2.4 GREENFIELD): this flow starts
with no pre-existing trades — trade_snapshot_state legitimately
begins empty, every row is born from an admitted message with
pointer + digest populated, and no old application version
consumes these snapshots. The retired bootstrap / digest-NULL /
pointer / mixed-version cases have no reachable subject.
ID kept — IDs are stable; body retained in git history
(commit 9a53c75). Admission itself is fully covered by T-35.
```

### T-37 — Absence lifecycle: removal, CANCELLED terminal, anchors, reappearance (round 11)

```text
Section: §6.1, §4.1, §4.2, §6.5, §6.0, §12, §15   Type: INTEGRATION
Blocking: YES
Purpose: PO-9's answer is a complete lifecycle, not just a zeroing
         write: a removed payment TERMINATES (CANCELLED, never
         COMPLETED, never wedged IN_PROGRESS forever), the trade's
         ONLY payment is removable (empty derived set — §6.0 role
         derivation), anchors retire ordering-aware, removal is
         visible (§15 disappearance alert), and a reappearing
         payment reopens.
Setup:   real Oracle; trade with payments A (unsent active request)
         and B (confirmed = required); a one-payment trade C
         (unsent); a trade with a MAYBE-parked payment D; a trade
         with a §6.6 anchor E (validation_failed_ordering = 300);
         snapshots: S-newer omitting A (ordering above the trade
         watermark), S-empty with ZERO derived blocks for trade C,
         S-anchor-old (ordering 250, omits E), S-anchor-new
         (ordering 350, omits E), S-reappear carrying A again with
         a new positive amount, plus a REDELIVERY of each zeroing
         document; a payment F with provider_reject_count = 1
         (marker ordering below the removal ordering) and a
         payment G with provider_reject_count = 2, each with
         removal + reappearance snapshots (round 12); a payment H
         removed (zeroed), then targeted by a NEWER snapshot that
         FAILS validation (H's scope extractable), then by a
         corrected valid snapshot (round 13).
Action:  deliver each snapshot; for D deliver removal first, then
         REJECTED evidence, then (separate run) EXECUTED evidence;
         re-deliver the zeroing documents; run removal +
         reappearance for F and G (round 12); deliver H's
         malformed then corrected snapshots (round 13); read
         every affected card through the §12 path.
Expect:  A: required := 0, upstream_ordering advanced, §6.4
         auto-cancel + release → row derives §4.1 CANCELLED —
         displayed CANCELLED, NEVER COMPLETED, and the step is
         TERMINAL (no live exception, no ops queue entry); B:
         overpay latch fires (confirmed > required(0)) → the
         obligation derives IN_PROGRESS with OVERPAY_DETECTED,
         never CANCELLED (round 12: NO request-state mutation —
         the executed request stays terminal/frozen; BLOCKED is a
         request state), no clawback; C (only-payment removal):
         the EMPTY derived set is ADMITTED (trade watermark
         advances) and C zeroes exactly like A — this is C-1a's
         trace, the one the 1..N contract could not represent; D:
         zeroing parks wait-then-decide; REJECTED evidence →
         release → CANCELLED; EXECUTED evidence → confirmed > 0 →
         latch (stop); E: S-anchor-old (250 < 300) leaves the
         anchor UNTOUCHED (marker still LIVE); S-anchor-new
         (350 > 300) retires it — required := 0, watermark
         advanced past the marker, row derives CANCELLED (no
         permanent marker-only wedge); reappearance: S-reappear
         applies normally (strictly newer), required := the new
         positive value, step returns to IN_PROGRESS, a fresh
         request is creatable (§6.8); redelivery of every zeroing
         document converges (equal ordering + digest → admission
         no-op; obligations already zeroed → stale-guard no-op);
         F (count 1, round 12): the zeroing advance leaves the
         marker not-live → clean CANCELLED; reappearance creates
         a successor normally; G (count 2): the marker stays LIVE
         (ops-only clear) yet the scope derives CANCELLED with NO
         provider exception (round-13 HISTORICAL suppression —
         the marker's ordering is below the removal watermark);
         reappearance → IN_PROGRESS, PROVIDER_REJECTED
         resurfaces, NO automatic successor until the ops clear;
         H (round 13): the malformed reappearance writes a LIVE
         validation marker WITHOUT advancing the watermark →
         required stays 0, the CANCELLED predicate fails →
         IN_PROGRESS with DATA_VALIDATION_FAILED VISIBLE (never
         suppressed) and the §15 marker-age alert in scope; NO
         request exists; the corrected valid snapshot then
         applies (required := positive, marker not-live) →
         normal recovery; EVERY zeroing
         fan-out emitted the §15 disappearance metric + log line
         (business_id, zeroed tuples MASKED per §16.3 —
         log-capture assertion proves no raw debit_account —
         doc.ordering) — checked for A, C, D, E-new; NONE fired
         for refused/no-op documents.
Failure: a removed payment stuck IN_PROGRESS forever (the C-1b
         wedge), CANCELLED displayed as COMPLETED, an
         unretirable anchor (the C-1c wedge), an unrepresentable
         only-payment removal (C-1a), a silent disappearance
         (P5 violation), a raw account in the disappearance log,
         a reappearance that auto-pays through a live
         provider_rejected marker (round 12), or a zeroed scope
         with a LIVE validation marker showing no exception (the
         round-12 over-suppression — round 13).
Implemented by: IN-02 (absence fan-out + admission), RG-08
(CANCELLED derivation), RG-09 (round-13 narrowed suppression),
RG-10 (reopening), OB-03 (disappearance + validation marker-age
alerts).
```


### T-38 - attempt-journal reliability set (never load-bearing)

```text
Setup:   real Oracle lane; journal deployed (AUD-01); posting path
         with the riders (K-04 / RC-02 / ST-10); enablement switch ON
         unless a case says otherwise.
Cases:
  A  claim-transaction rollback (fault injected after the rider
     insert): NO ATTEMPT_STARTED row survives; attempt_count and
     post_attempt_seq unbumped (atomicity, no phantom rows).
  B  the reset lifecycle: attempt 1 -> MAYBE -> trust-age downgrade
     (attempt_count RESETS; post_attempt_seq does NOT) -> re-claim
     -> re-POST: NO unique-key collision; the journal shows seq 1
     and seq 2 pairs (the review-5156f1f H1 regression case).
  C  lease-expiry vs slow-worker race: exactly ONE ATTEMPT_RESOLVED
     per attempt (the losing CAS hits 0 rows and inserts nothing).
  D  duplicate/replayed response processing: journal unchanged
     beyond its single pair (rowCount==0 -> no insert).
  E  full-content presence: EVERY STARTED row carries the complete
     canonical payload with a matching hash - including the H/H/H
     identical-retry run AND the H1 -> H2 -> H1 alternating run
     (review 7ab31e5 H1: no NULL content, no refs, ever); changed
     bytes also record divergence_expected TRUE at claim. FULLNESS
     is proven HERE at application level (byte-compare against the
     assembled instruction) - the DDL trigger proves NON-EMPTY
     PRESENCE only (zero-length content is refused by the trigger,
     T-38 H; byte-for-byte completeness is provable only here).
  F  failure isolation on the REAL JDBC/Spring stack, BOTH riders
     (review d00ef6a H3 - the narrow guarantee):
     (i) allowlisted statement-local signatures ONLY - the pinned
         ORA codes (00001 unique, 02290 check, 20141/20142 journal
         triggers, evidenced space-error family; timeouts are NOT
         here - review 928341a H2): posting CONTINUES
         (wire calls proceed, money outcomes identical), the gap is
         recorded, and the AUDIT-GAP alert fires AFTER the host
         COMMIT (a rolled-back host reports nothing); assert NO
         rollback-only marking / no UnexpectedRollbackException
         (the rider uses a plain try/catch, no inner
         @Transactional). THE CLASSIFIER IS PART OF THE DELIVERABLE
         and is a NARROW ALLOWLIST OF PINNED ORACLE VENDOR CODES
         (review 4d5cb83 M3 — never instanceof-only: a Spring type
         such as QueryTimeoutException does not prove the session
         is usable): allowed = ORA-00001 (unique), ORA-02290
         (check), ORA-20141/20142 (the paj triggers),
         ORA-01653/01654-family (space), read from the SQLException
         vendor code on the PINNED driver + Spring versions;
         EVERYTHING ELSE — including timeouts and any unknown or
         ambiguous translation — is FATAL by default. Per allowed
         signature, prove ALL of: the translation is exactly the
         expected one; the connection remains valid; a subsequent
         host-transaction statement succeeds; host COMMIT succeeds
         with no rollback-only state; the after-commit gap alert
         fires exactly once.
     (ii) fatal classes - connection kill mid-insert, session
         termination, commit-time failure: the attempt fails as an
         ORDINARY infra failure and existing recovery handles it
         (uncommitted claim -> row READY/RETRY_WAIT; committed ->
         lease expiry -> MAYBE); money is never wrong.
     Sub-case: enablement switch OFF -> zero inserts, zero errors,
     posting unaffected.
  G  grants + audit coverage (expanded per review 4d5cb83 M2): the
     app role cannot SELECT; no app/reporting role has
     UPDATE/DELETE; the audit policy is ENABLED (evidence captured,
     container scope recorded); audit-role reads AND app INSERTs
     AND denied-access attempts AND owner/DBA maintenance (incl.
     the partition-drop ALTER) all appear in the unified audit
     trail — the policy scope is ALL object actions.
  H  partition maintenance: DROP PARTITION ... UPDATE GLOBAL INDEXES
     leaves BOTH global unique structures (paj_pk AND paj_pair_uq)
     USABLE (verified); the scalar shape CHECK and the content
     trigger reject malformed rows in BOTH directions: STARTED
     without content (or zero-length content) refused; RESOLVED
     WITH content refused; outcome outside the paj_outcome_ck
     vocabulary refused.
  I  log join: every journal event pair joins unambiguously to its
     ATTEMPT-class log lines via (request_id, post_attempt_seq,
     event type) - review 7ab31e5 M5.
  J  switch transitions (review d00ef6a M3): OFF->ON and ON->OFF
     under posting freeze + drain -> NO half-pairs; a mid-traffic
     flip in the harness DOES create one (documents why the rule
     exists); planned transitions are recorded and excluded by the
     unmatched-pair alert triage. BOUNDARY (review c8a92f1 M1):
     claim committed while ON -> freeze effective before the wire ->
     worker abandons -> the switch may NOT change until that
     episode's lease-expiry resolution lands (drain includes
     abandoned claims); the harness asserts the half-pair appears
     ONLY when this rule is violated.
Expected: all cases green + grep/review evidence that NO runtime
         code path SELECTs the journal.
Failure meaning (review 4d5cb83 L1 — aligned with the narrow
         guarantee): an INCORRECT money outcome, a MISCLASSIFIED
         failure (statement-local treated as fatal or vice versa),
         a PHANTOM gap alert (emitted for a rolled-back host), or
         journal state becoming runtime input - each violates
         section 14.1. (A fatal infra failure failing the attempt
         is CORRECT behavior, not a test failure.)
Type: integration (real Oracle) + fault injection. BLOCKING: yes
         (it proves the journal CANNOT hurt the money path).
Implemented by: AUD-01 (schema slice + G + H + the switch), K-04
(A, B, E, F), RC-02 (D), ST-10 (C), OB-05 (F alert wiring +
J triage rules); I = ST-08 (the §14 line convention: post_attempt_seq
+ event type on ATTEMPT-class lines) together with K-04/RC-02/ST-10
(the three ATTEMPT-class emission sites) — review b1d91dc M3.
```
