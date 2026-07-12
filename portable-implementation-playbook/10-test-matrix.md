> **Purpose:** Test matrix T-01..T-36 with setup/action/expected/failure-meaning/type/blocking per test (original Section J; seeds companion artifact CA-7).
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

### T-21 — Cutoff passed

```text
Section: §7.4, §7.0, §16.4   Type: INTEGRATION   Blocking: YES
Purpose: cutoff blocks attempts and downgrades; tz-aware comparison.
Setup:   cutoff calendar fixture with a market past cutoff (tz-aware,
         incl. a DST boundary case).
Action:  retry scanner + §9.2 downgrade candidates against it.
Expect:  attempts blocked (CUTOFF_EXPIRED); downgrade gate fails;
         comparisons correct across the DST case.
Failure: post-cutoff POST (bank-close violation) or a whole currency
         blocked an hour early (§18-2's warning).
Implemented by: RC-04, RC-03; config from B-03.
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

### T-23 — Repost forbidden by staleness or cutoff

```text
Section: §7.0, §6.4, §9.2   Type: INTEGRATION   Blocking: YES
Purpose: repost_permitted's terms hold at BOTH ends; no livelock.
Setup:   MAYBE rows with: stale amount; passed cutoff; set
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
Setup:   the worst row: divergent_payload_at set + cutoff passed +
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
Section: §3, §10.3 L9   Type: INTEGRATION + OPERATIONAL   Blocking: YES
Purpose: I1/I2 violations page; read skew does not; L9 verified.
Setup:   seeded counter corruption; concurrent uncommitted create;
         REJECTED row missing its marker.
Action:  run the scan.
Expect:  corruption pages after locked re-check; skew does not page;
         L9 violation detected.
Failure: silent drift → the deliberate counter redundancy pays
         nothing.
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
Action:  read through the card path.
Expect:  NOT_STARTED = absence; anchors show DATA_VALIDATION_FAILED;
         MAYBE shows PAYMENT_OUTCOME_UNKNOWN (rank 1, never
         SYSTEM_UNAVAILABLE); labels per §10.4; the multi-payment
         trade returns ALL its obligations, one entry per payment —
         result count is never an error or alert (§12);
         unavailable ≠ stale-as-authoritative.
Failure: false completion (the predicate's whole point) or a
         multi-payment trade surfacing as an error or partial result.
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
Action:  run each OP-04 endpoint (+ RG-05 supersede/close) with
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
Implemented by: OP-04, RG-05.
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
Action:  run interleaved rounds (scanner vs feed vs cancel vs
         scanner-2) under load; instrument lock acquisition order.
Expect:  every both-table transaction locks the obligation FIRST
         (candidate selection takes NO row locks); exactly one actor
         wins each claim (CAS rowCount 1), losers see 0 rows and
         skip silently; zero ORA-00060 across the run; claim/unclaim
         triggers no §4 re-derivation.
Failure: any FOR UPDATE on payment_request before the obligation
         lock in the same transaction, or a deadlock under
         interleaving.
Implemented by: ST-09..11, RC-04/RC-05 (protocol per §11 + mechanics
M5), OB-xx dashboards unaffected.
```

### T-35 — Snapshot admission gate + per-block currency (rounds 5–6)

```text
Section: §6.1, §2.4, §20-10   Type: INTEGRATION / CONCURRENCY
Blocking: YES
Purpose: a stale snapshot can neither mutate NOR CREATE anything;
         BLOCK transactions serialize on the trade row and re-verify
         currency (round 6 — admission alone is a point-in-time
         fact, not ownership); the reference-only tie converges.
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
         round-5 H-1 trace); the two first snapshots serialize on
         the trade-row insert/lock — both scopes exist, the newer
         ordering owns the row, no lost update; the paused worker's
         NEXT block transaction locks the trade row, sees the
         admitted (ordering, digest) no longer current, ABORTS the
         fan-out, and creates/mutates NOTHING (round-6 overtake —
         newest-wins abandonment; the §9.3
         consumed-without-completion alert fires for the reprocess
         case); the killed worker's document converges via
         redelivery (intake) or is correctly refused on
         re-approval when a newer snapshot owns the trade
         (reprocess); the zombie's blocks all no-op; the
         failed-validation message advances NEITHER
         trade_snapshot_state nor upstream_ordering;
         reference-only tie: detected at admission (digest differs),
         approved reprocess updates ONLY the trade row (blocks
         no-op), §7.0 assembly reads the new reference, re-run is
         digest-equal → no-op (converged, no repeat tie alert).
Failure: any block transaction that applies or creates without
         holding the trade lock and proving currency IN THAT
         transaction; a tie that re-alerts after adjudication.
Implemented by: S-10, IN-02, OP-04 (reprocess entry path).
```

### T-36 — trade_snapshot_state bootstrap + mixed-version cutover (round 6)

```text
Section: §2.4 BOOTSTRAP, §6.1, Section M   Type: MIGRATION
Blocking: YES
Purpose: an EXISTING production trade must never read as "first
         contact"; bootstrap-incomplete rows fail closed; the
         enablement gate is enforced.
Setup:   prod-shaped copy: trades with obligations at various
         upstream_ordering values and NO trade rows; one §6.6
         anchor-only trade (all NULL ordering); XML store seeded
         with a delayed older snapshot carrying a never-seen scope.
Action:  run the S-11 bootstrap; deliver the delayed older snapshot
         to a bootstrapped trade; deliver an equal-order snapshot
         to a digest-NULL row; deliver a strictly newer snapshot;
         re-run the bootstrap job; simulate the mixed-version
         window (a writer that does not maintain the trade row).
Expect:  every existing business_id gets a row with watermark =
         MAX(upstream_ordering) (anchor-only trade: NULL ordering =
         first-contact semantics); the delayed older snapshot is
         REFUSED — the never-seen scope is NOT created (the H-2
         first-contact trace); equal-order onto digest-NULL →
         FAILS CLOSED into the tie-conflict path (no silent apply,
         no silent drop); strictly newer → applied AND the row
         completed (id + digest populated); bootstrap re-run
         changes nothing (insert-if-absent); the coverage report +
         shadow metric flag the non-maintaining writer's trades
         before enforcement is enabled.
Failure: an older document admitted by a bootstrapped or
         digest-NULL row, or enforcement enabled with coverage
         gaps.
Implemented by: S-11, IN-02.
```

