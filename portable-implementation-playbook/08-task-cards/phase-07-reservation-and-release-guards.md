> **Purpose:** Task cards RG-01..RG-10 (reservation choreography, release guards, derivation) (original Section H, phase P7).
> **When to use this file:** When executing the tasks of this phase, one card at a time, with the matching packet file from 09-minimal-context-packets/.
> **Depends on:** 08-task-cards/README.md; 01-playbook-index.md; 07-placeholder-glossary.md; the requirement sections cited per card; the locally filled mapping template.
> **Used by:** The local coding agent executing phase P7.
> **Safe to transfer:** yes
> **Contains local code names:** no

## H-Phase 7 — Reservation and release guards (P7)

### RG-01 — Reservation increment at creation

- **Task ID:** RG-01
- **Title:** +committed_amount in the SAME transaction as the payment_request insert
- **Classification:** MVP normative implementation
- **Purpose:** §3 INCREMENT rule: shortfall always sees in-flight money; the request-creation double-pay window does not exist.
- **Prerequisites:** S-09; K-01 (same transaction already carries seq++); D-03 money-semantics memo reviewed (if an existing counter has different semantics, this task writes the §2.1 column and leaves the legacy counter untouched until P14 — record).
- **Requirement sections / concepts to read:** §3 (increment + consequences), §6.8 (creation point), §11 (lock).
- **Placeholder components involved:** [Payment Request Creation Component], [Reservation Repository], [Obligation Repository].
- **Local placeholder mappings required before starting:** all three; creation sites known.
- **Local code areas to discover:** creation transaction.
- **How to locate:** F.2.
- **Implementation instructions:** inside the locked creation transaction: committed_amount += request.amount together with the insert; no other flow increments it.
- **Do not change:** legacy counters (parallel until P14 contract).
- **Tests to add:** insert+increment atomicity (rollback rolls both); I1 holds after creation; concurrent create attempts (I6 blocks the second; counter incremented once).
- **Edge cases:** creation retried after ORA-00001 obligation race (§6.1) — no double increment (transaction retry semantics).
- **Manual validation:** row + counter inspection after a local creation.
- **Expected outcome:** reservation exists from birth.
- **Failure signs:** increments at POST time anywhere (§3: NO MOVEMENT at POST).
- **Common mistakes:** incrementing outside the lock.
- **Completion criteria:** tests green.
- **Stop condition:** merged.
- **Next task:** RG-02.

### RG-02 — Reservation release on terminal-negative

- **Task ID:** RG-02
- **Title:** −committed_amount in the SAME transaction as the terminal-negative CAS, only on row-count 1
- **Classification:** MVP normative implementation
- **Purpose:** §3 DECREMENT rule: once per transition, redelivery-safe.
- **Prerequisites:** ST-02, ST-06; RG-01.
- **Requirement sections / concepts to read:** §3 (decrement), §10.2 (outcome writes), §10.1 (release guard context).
- **Placeholder components involved:** [Reservation Repository], [Request Status Persistence Layer].
- **Local placeholder mappings required before starting:** outcome writers list (ST-06 helper sites).
- **Local code areas to discover:** none new.
- **How to locate:** ST-06.
- **Implementation instructions:** extend the ST-06 outcome path: when outcome ∈ {REJECTED, CANCELLED, SUPERSEDED} and the CAS returned 1, decrement committed_amount by request.amount in the same transaction (obligation lock already held per §11).
- **Do not change:** which flows may set terminal-negative (release GUARD is RG-05 + S-06).
- **Tests to add:** decrement iff row-count 1 (replayed event → 0 rows → no decrement); I1 after each terminal-negative; EXECUTED does NOT decrement committed (I1 includes EXECUTED rows — assert).
- **Edge cases:** CANCELLED via §6.4 vs REJECTED via feed — same decrement path (single helper).
- **Manual validation:** counter trace across a seeded reject.
- **Expected outcome:** release exactly once per transition.
- **Failure signs:** decrement on non-terminal failures (F.13 PARTIAL sites — must be gone).
- **Common mistakes:** decrementing on EXECUTED (I1: committed covers NULL + EXECUTED).
- **Completion criteria:** tests green.
- **Stop condition:** merged.
- **Next task:** RG-03.

### RG-03 — Confirmation increment with amount equality

- **Task ID:** RG-03
- **Title:** +confirmed_amount on outcome=EXECUTED under the amount-equality guard; mismatch → BLOCKED(AMOUNT_MISMATCH) path
- **Classification:** MVP normative implementation
- **Purpose:** §3 (confirmation moves confirmed only) + §8/§16.4 (no tolerance; mismatch is defect evidence, not business state).
- **Prerequisites:** RG-02; ST-06.
- **Requirement sections / concepts to read:** §3, §8 (amount-mismatch block), §16.4 (compareTo, no tolerance), §10.5 (mismatch row).
- **Placeholder components involved:** [Reservation Repository], [Request Status Persistence Layer].
- **Local placeholder mappings required before starting:** settlement application path (shared by IN-07/RC-06 later; build the helper here).
- **Local code areas to discover:** none new.
- **How to locate:** n/a.
- **Implementation instructions:** settlement helper: evidence amount compareTo request.amount == 0 → outcome=EXECUTED CAS (sets SUBMITTED, L4) + confirmed_amount += amount, same transaction; mismatch → NO outcome write, NO confirmed movement: CAS to same-stage BLOCKED(AMOUNT_MISMATCH) + submission_state=SUBMITTED (settlement evidence tightens — §8) + CRITICAL alert hook; overpay evaluation after confirmed moves (RG-04).
- **Do not change:** all-or-nothing assumption (confirmed contract fact — no partial-settlement handling exists by design).
- **Tests to add:** equality via compareTo (scale variants: 10 vs 10.00 equal); mismatch parks + tightens + no money movement; I2/I3 after settlement; JPY/BHD scale round-trips (§16.4).
- **Edge cases:** mismatched settlement landing on a MAYBE row (legal — terminal evidence applies to any active row; park takes it off MAYBE clocks per §8).
- **Manual validation:** seeded settlement + seeded mismatch traces.
- **Expected outcome:** confirmation safe and exact.
- **Failure signs:** BigDecimal.equals anywhere in amount comparisons (§16.4).
- **Common mistakes:** treating mismatch as retryable; moving confirmed on the park.
- **Completion criteria:** tests green.
- **Stop condition:** merged.
- **Next task:** RG-04.

### RG-04 — Overpay latch

- **Task ID:** RG-04
- **Title:** Set overpay_blocked when confirmed_amount > required_amount; latch is one-way; alert on set
- **Classification:** MVP normative implementation
- **Purpose:** §13: one-way door into manual territory; post-latch amounts stop being trustworthy automation inputs; §15 alerts on the latch itself.
- **Prerequisites:** RG-03.
- **Requirement sections / concepts to read:** §13 (latch + rationale + cross-stream race), §3 (I4), §6.5 (latch guard, consumed by RG-10).
- **Placeholder components involved:** [Reservation Repository], [Metrics / Alerting Layer].
- **Local placeholder mappings required before starting:** RG-03 helper.
- **Local code areas to discover:** none new.
- **How to locate:** n/a.
- **Implementation instructions:** after any confirmed/required change under the lock: if confirmed > required (compareTo) → set latch (idempotent), fire the §15 latch alert hook; NEVER auto-clear (no code path unsets it — the un-set operation does not exist at MVP).
- **Do not change:** the rejected auto-unlatch alternative stays rejected (§13).
- **Tests to add:** latch sets on overpay; survives later required_amount increase (the §13 trace: required 150 > confirmed 100 with latch STILL set); I4; alert fired once per set.
- **Edge cases:** overpay via amendment-down after EXECUTED (required drops below confirmed) — same latch.
- **Manual validation:** seeded trace per the §13 example.
- **Expected outcome:** latch semantics exact.
- **Failure signs:** any generic recalculation clearing it (§13: "never silently un-set").
- **Common mistakes:** keying the latch on committed instead of confirmed.
- **Completion criteria:** tests green.
- **Stop condition:** merged.
- **Next task:** RG-05.

### RG-05 — Release guard in code + supersede/close operation

- **Task ID:** RG-05
- **Title:** Enforce §10.1's release guard in every terminal-negative code path; implement the guarded supersede/close operation
- **Classification:** MVP normative implementation
- **Purpose:** releasing a reservation whose money may have moved is the one remaining double-payment path (§3); the supersede/close operation is a §3 REQUIRED feature, executed at MVP as a controlled admin operation — an authorized application endpoint per the §20 execution boundary (2026-07-11).
- **Prerequisites:** RG-02; S-06 (trigger backstop + evidence flag).
- **Requirement sections / concepts to read:** §10.1 (release guard), §9.4, §3 (supersede/close + FORBIDDEN clause), §20 (interim model).
- **Placeholder components involved:** [Request Status Persistence Layer], [Operator Admin Procedure Area] (the supersede/close procedure), [Stored Procedure / Trigger Area].
- **Local placeholder mappings required before starting:** S-06 evidence-flag mechanics.
- **Local code areas to discover:** every terminal-negative initiator (auto-cancel RG-07, feed reject IN-07, resolver reject RC-06, ops paths).
- **How to locate:** outcome-writer inventory.
- **Implementation instructions:** shared guard check before any terminal-negative CAS: permitted iff submission_state=NOT_SUBMITTED OR driven by an authoritative engine negative (which sets the evidence flag for the trigger) OR the §9.3 procedure (OP-01, its own flag setter). Supersede/close: a guarded admin operation — an authorized application endpoint (restricted enterprise role; ticket + operator identity enforced in the contract per §20-8; the OP-01 auth pattern once it exists, else the same shape) setting SUPERSEDED/CANCELLED on a stalled active request via the shared helpers, refused while MAYBE/SUBMITTED unless evidence-driven; releases the reservation via the RG-02 path.
- **Do not change:** the S-06 trigger (defense in depth — code guard AND trigger both live).
- **Tests to add:** guard denies terminal-negative on MAYBE row from a non-evidence path (code layer AND trigger layer asserted separately); allows on NOT_SUBMITTED; supersede/close on a stalled ENRICH·BLOCKED row works + releases; refused on MAYBE.
- **Edge cases:** ops reject of a BLOCKED·NOT_SUBMITTED row is legal (release guard passes on NOT_SUBMITTED).
- **Manual validation:** attempted manual release on a seeded MAYBE row fails loudly (trigger), demonstrating the §10.3 fat-finger story.
- **Expected outcome:** release rights enforced everywhere.
- **Failure signs:** flows setting the evidence flag without carrying authoritative evidence (grep flag-setter call sites — must be exactly: authoritative-negative path, OP-01).
- **Common mistakes:** treating a status-query answer as evidence for RELEASE (§9.4 forbids — query answers only tighten).
- **Completion criteria:** tests green.
- **Stop condition:** merged.
- **Next task:** RG-06.

### RG-06 — Standing shortfall re-evaluation (single creation point)

- **Task ID:** RG-06
- **Title:** Consolidate request creation into §6.8's standing re-evaluation with the trigger inventory T1–T4 and the successor policy
- **Classification:** MVP normative implementation
- **Purpose:** §6.8: exactly ONE creation point; deferred, never lost; ordering-aware successor policy bounds blind re-pay of rejects.
- **Prerequisites:** RG-01 (creation transaction shape), IN-02 helpful (ordering advance trigger) — coordinate; K-01/K-02 (identity inside creation).
- **Requirement sections / concepts to read:** §6.8 (whole: gate conditions, T1–T4, successor policy, consequences), §3 (I5 under lock), §6.2.
- **Placeholder components involved:** [Payment Request Creation Component], [Obligation Repository].
- **Local placeholder mappings required before starting:** ALL legacy creation sites (D-04/F.2 — if any site cannot be routed through the single point, STOP and report).
- **Local code areas to discover:** each legacy creation call site.
- **How to locate:** F.2 inventory.
- **Implementation instructions:** implement evaluate(obligation) under the lock exactly per §6.8's condition list (shortfall > 0 ∧ I6 free ∧ latch clear ∧ validation_failed not live ∧ provider_rejected not live ∧ successor policy permits) → create (RG-01 + K-01/K-02 choreography; creating_ordering := upstream_ordering); invoke from T1 (message applied — even without amount change), T2 (outcome set), T3 (marker liveness change), T4 (ops clear/retry); route every legacy creation site through it (delete none until routed).
- **Do not change:** WHAT amount a request gets (business logic).
- **Tests to add:** each T-trigger fires evaluation; each gate condition blocks; successor policy: CANCELLED/SUPERSEDED permit; EXECUTED permits iff shortfall remains; REJECTED permits iff upstream_ordering strictly newer than creating_ordering ∧ reject_count < 2 ∧ no live marker; deferred amendment (in-flight request → no create; on resolve → successor); zero-shortfall message → no request (§6.2).
- **Edge cases:** correction-races-enrichment-reject (§6.8 consequence — marker tagged with OLD creating_ordering, correction newer → not live → create passes): seed and assert.
- **Manual validation:** trace T1..T4 locally with logs.
- **Expected outcome:** one creation point; no second path.
- **Failure signs:** any residual direct-insert site (grep for insert on the request table outside the component).
- **Common mistakes:** evaluating outside the lock; forgetting T1's "whether or not required_amount changed".
- **Completion criteria:** tests green; site inventory routed.
- **Stop condition:** merged.
- **Next task:** RG-07.

### RG-07 — Auto-cancel with row-count-0 branching

- **Task ID:** RG-07
- **Title:** Implement §6.4 auto-cancel CAS (exact set semantics) + the submission-state-first row-count-0 branches + the retry-guard
- **Classification:** MVP normative implementation
- **Purpose:** §6.4: amendment-down cancels only provably-releasable requests; MAYBE rows park (AMENDMENT_PARKED); the retry-guard re-checks amount staleness before every re-POST.
- **Prerequisites:** RG-05 (release guard), RG-06 (successor creation), ST-02.
- **Requirement sections / concepts to read:** §6.4 (whole: CAS shape, set semantics, branches, retry-guard), §7.0 (staleness term), §10.5 (rows).
- **Placeholder components involved:** [Payment Request Creation Component] (amendment flow), [Request Status Persistence Layer], [Retry Resolver Job] (retry-guard site).
- **Local placeholder mappings required before starting:** amendment-processing path (IN-02's home) identified.
- **Local code areas to discover:** none new.
- **How to locate:** message-processing flow.
- **Implementation instructions:** the §6.4 CAS verbatim (outcome IS NULL ∧ stage IN (ENRICH, POST) ∧ NOT (POST·CLAIMED) ∧ stage_state <> BLOCKED ∧ submission_state = NOT_SUBMITTED → CANCELLED); row-count 1 → release (RG-02) + re-derive + RG-06 successor; row-count 0 → branch submission_state FIRST: MAYBE (any stage, not CLAIMED) → CAS to BLOCKED(AMENDMENT_PARKED) + alert; MAYBE·CLAIMED → DEFER (no park mid-claim — the retry-guard or resolver applies it later; §6.4); SUBMITTED → leave alone; NOT_SUBMITTED-yet-unreleasable (POST·CLAIMED or BLOCKED) → per §6.4 last branch. Retry-guard in the retry worker: before re-POST of POST·RETRY_WAIT, re-validate amount vs current shortfall under the lock; stale + NOT_SUBMITTED → cancel; stale + MAYBE → park AMENDMENT_PARKED.
- **Do not change:** ENRICH·CLAIMED cancellability (normative — read-only work).
- **Tests to add:** each CAS row (§10.5): cancellable set; ENRICH·CLAIMED cancelled + stale worker fenced; POST·CLAIMED untouched; BLOCKED untouched; MAYBE park (incl. CONFIRM·READY); deferred park under live claim lands later; retry-guard both branches; park is stable (no un-park by §9.2 while stale — full assert when RC-07 exists; pending named case).
- **Edge cases:** §10.5's amendment-down row against MAYBE at ANY stage incl. CONFIRM.
- **Manual validation:** seeded amendment scenarios.
- **Expected outcome:** amendment safety complete.
- **Failure signs:** cancel of a MAYBE row anywhere (the §3 double-pay path).
- **Common mistakes:** branching on stage first (spec: money truth first).
- **Completion criteria:** tests green.
- **Stop condition:** merged.
- **Next task:** RG-08.

### RG-08 — Step-status predicate

- **Task ID:** RG-08
- **Title:** Implement §4.1's completion predicate as THE ui_step_status derivation, run under the lock after every applied mutation
- **Classification:** MVP normative implementation
- **Purpose:** money derives status (P1): completion never copied from events; vacuous-completion guards protect anchors.
- **Prerequisites:** RG-01..04 (amounts trustworthy); IN-04 marker liveness helper (coordinate — implement the liveness predicate here if IN-04 not yet done, single shared helper).
- **Requirement sections / concepts to read:** §4.1 (predicate + all four bullets), §4 (derivation order), §2.1 (marker liveness definition incl. the §6.6 anchor clause).
- **Placeholder components involved:** [Obligation Repository], [Request Status Persistence Layer] (hook from ST-02's re-derive).
- **Local placeholder mappings required before starting:** derivation hook point (ST-02).
- **Local code areas to discover:** current ui_step_status writers (must become derivation-only).
- **How to locate:** column write sites.
- **Implementation instructions:** predicate exactly per §4.1: required NOT NULL ∧ required > 0 ∧ confirmed >= required ∧ committed = confirmed ∧ latch clear ∧ validation_failed not LIVE (LIVE = marker set ∧ (marker ordering >= upstream_ordering ∨ upstream_ordering IS NULL)); PLUS the round-11 CANCELLED terminal branch (§4.1): required = 0 ∧ committed = 0 ∧ confirmed = 0 ∧ no ACTIVE request ∧ latch clear ∧ validation_failed not LIVE → CANCELLED (displayed CANCELLED, NEVER COMPLETED; reopenable like COMPLETED — a strictly newer positive block returns it to IN_PROGRESS; required = 0 is writable ONLY by the §6.1 absence path, inbound blocks are strictly positive); output IN_PROGRESS/COMPLETED/CANCELLED stored; NOT_STARTED is row absence (§12); wire into the ST-02 re-derive hook (same transaction, under lock); remove/route any event-copy writer of ui_step_status.
- **Do not change:** feed handlers may NEVER write ui_step_status directly (§4.1 last bullet).
- **Tests to add:** each predicate term isolated (anchor row cannot complete; post-decrement zero-zero cannot complete; active request blocks completion; recovered anchor completes after valid message); derivation runs after every mutating flow (hook coverage); zeroed obligation (0/0/0, no active request, validation marker not live — provider_rejected does NOT block the branch, round 12) derives CANCELLED — never COMPLETED; zeroed scope with confirmed > 0 derives IN_PROGRESS + overpay latch + OVERPAY_DETECTED (round 12: obligation-level only — BLOCKED is a payment_request state; NO request mutation; never CANCELLED); reappearance returns CANCELLED → IN_PROGRESS (T-37 set).
- **Edge cases:** terminal-negative leaves committed=confirmed=0 with required unpaid → IN_PROGRESS (the mandatory confirmed>=required term).
- **Manual validation:** card-visible status trace through a full happy path locally.
- **Expected outcome:** completion always derived, never copied.
- **Failure signs:** COMPLETED on a scope with an active request (impossible per predicate — if seen, reservation choreography is broken).
- **Common mistakes:** omitting the upstream_ordering IS NULL clause (anchor liveness undefined exactly where §6.6 needs it).
- **Completion criteria:** tests green.
- **Stop condition:** merged.
- **Next task:** RG-09.

### RG-09 — Active-exception + next-actor derivation

- **Task ID:** RG-09
- **Title:** Implement §4.2's precedence-ordered exception derivation and §4.5's next-actor derivation (never stored)
- **Classification:** MVP normative implementation
- **Purpose:** derive, never accumulate (P2): no set/clear rules to get wrong; rank-1 conditions never masked by transient errors.
- **Prerequisites:** RG-08 (shared derivation pass); ST-05 (rules keyed on dimensions).
- **Requirement sections / concepts to read:** §4.2 (ranks + rationale), §4.3 (stored inputs), §4.5 (actor table + dual-actor note), §13 (categories/retryability/severity).
- **Placeholder components involved:** [Obligation Repository] (read-model fields), [Request Status Persistence Layer].
- **Local placeholder mappings required before starting:** RG-08 hook.
- **Local code areas to discover:** current exception writers (become derivation-only).
- **How to locate:** active_exception field writers.
- **Implementation instructions:** in the same derivation pass: evaluate §4.2's ranks in order over live conditions (active requests only; round 12: required_amount = 0 SKIPS the marker-based ranks — DATA_VALIDATION_FAILED / PROVIDER_REJECTED — markers stay STORED; rank-1 conditions and the latch derive normally) → write active_exception_* fields (§13 attributes; codes per §4.2/§13: PAYMENT_OUTCOME_UNKNOWN, OVERPAY_DETECTED, DATA_VALIDATION_FAILED, PROVIDER_REJECTED, BLOCKED-derived via blocked_reason, INSUFFICIENT_ACCOUNT_BALANCE, SYSTEM_UNAVAILABLE; content rules per §12: ops-readable, no sensitive account data, no stack traces); next-actor: implement §4.5 as a pure function of the tuple (+ ages) for scanner scoping/metrics — NEVER persisted.
- **Do not change:** rank order; the two rank-1 conditions' precedence rationale.
- **Tests to add:** precedence (MAYBE outranks OVERPAY outranks validation etc. per ranks); derivation clears by construction (corrected message → DATA_VALIDATION_FAILED gone in the same transaction); round-12 suppression: zeroed scope with live provider_rejected (count 2) → NO exception, same scope reappeared → PROVIDER_REJECTED resurfaces; zeroed scope with in-flight MAYBE → still PAYMENT_OUTCOME_UNKNOWN; dual-actor rows (BLOCKED+MAYBE → ops AND resolver; RETRY_WAIT+MAYBE → scanner AND resolver) — assert via the function.
- **Edge cases:** PAYMENT_OUTCOME_UNKNOWN must never surface as SYSTEM_UNAVAILABLE (§9.3 display block — explicit test).
- **Manual validation:** card fields through seeded scenarios.
- **Expected outcome:** exceptions/actors always current, never stale accumulations.
- **Failure signs:** any imperative setException call outside the derivation.
- **Common mistakes:** deriving from terminal rows (§4.2: active requests only).
- **Completion criteria:** tests green.
- **Stop condition:** merged.
- **Next task:** RG-10.

### RG-10 — Step reopening + latch guard

- **Task ID:** RG-10
- **Title:** Implement §6.5 reopening (required increase after COMPLETED, or positive-again after CANCELLED — round 12) with reopened_at, and the latch guard (no reopening-created requests on latched scopes)
- **Classification:** MVP normative implementation
- **Purpose:** §6.5: re-activation via the standing re-evaluation; latch wins — AMENDMENT_ON_LATCHED_SCOPE alerts instead of paying.
- **Prerequisites:** RG-06 (creation), RG-04 (latch), RG-08 (status re-derives).
- **Requirement sections / concepts to read:** §6.5 (incl. latch-guard rationale), §6.3 (increase path), §2.1 (reopened_at).
- **Placeholder components involved:** [Obligation Repository], [Payment Request Creation Component].
- **Local placeholder mappings required before starting:** amendment-application path (IN-02's home).
- **Local code areas to discover:** none new.
- **How to locate:** message flow.
- **Implementation instructions:** on an applied required increase against a scope whose derived status is COMPLETED or CANCELLED (round 12 — a reappeared removed payment reopens IDENTICALLY): recalc shortfall under lock; RG-06 evaluation creates requests (unless gated — ALL §6.8 gates apply: a live provider_rejected marker blocks the successor, count >= 2 = ops-only clear; removal never laundered reject history); set reopened_at; derivation returns IN_PROGRESS; overpay re-evaluates; if latched: apply the amount (§6.7 permitting), create NOTHING, fire AMENDMENT_ON_LATCHED_SCOPE.
- **Do not change:** the latch (RG-04 one-way rule).
- **Tests to add:** reopening full trace (COMPLETED → IN_PROGRESS + reopened_at + successor); CANCELLED → IN_PROGRESS + reopened_at + successor (clean reappearance — round 12); reappearance with provider_reject_count = 1 (marker went not-live via the zeroing watermark advance) → successor created; with count = 2 (marker LIVE) → NO successor until the ops-only clear, PROVIDER_REJECTED resurfaces (T-37 F/G cases); latched-scope amendment → amount applied, no request, alert fired; overpay re-eval on reopening.
- **Edge cases:** reopening while a live marker exists — RG-06's gates still apply (no special path).
- **Manual validation:** seeded reopening trace.
- **Expected outcome:** reopening = ordinary standing consequence.
- **Failure signs:** a dedicated reopening creation path (must be RG-06).
- **Common mistakes:** clearing reopened_at (derivation input; card indicates reopening — §4.3).
- **Completion criteria:** tests green.
- **Stop condition:** merged; Phase P7 report.
- **Next task:** CT-01 (parallel track) / IN-01.


---

## Phase handoff summary (P7 → P8/P9)

- **Phase outputs:** +committed at creation / −committed on terminal-negative rowCount-1 / +confirmed under amount equality; overpay latch (one-way); release guard in code + guarded supersede/close; §6.8 single creation point with T1–T4 triggers + successor policy; §6.4 auto-cancel + retry-guard; §4.1 completion predicate; §4.2/§4.5 exception + actor derivation; §6.5 reopening + latch guard.
- **Blockers to carry forward:** §18 items unchanged; RG-07 has a named pending assert (park stability under §9.2 — completes with RC-07).
- **Local mapping rows expected filled:** [Reservation Repository], [Obligation Repository] change notes; legacy-counter coexistence decision recorded (D-03 semantics memo consumed).
- **Tests expected to exist:** I1–I6 invariant tests, T-26/T-27 precursors, auto-cancel/park matrix (§10.5 rows), successor-policy tests, completion-predicate term isolation, precedence tests.
- **Next phase entry condition:** RG-10 done; P8 runs in parallel; P9 may start.
