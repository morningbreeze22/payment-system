> **Purpose:** Task cards RC-01..RC-10 (classifier, repost gate, retry scanner, resolver, trust-age/downgrade, escalation, freeze, breakers) (original Section H, phase P10).
> **When to use this file:** When executing the tasks of this phase, one card at a time, with the matching packet file from 09-minimal-context-packets/.
> **Depends on:** 08-task-cards/README.md; 01-playbook-index.md; 07-placeholder-glossary.md; the requirement sections cited per card; the locally filled mapping template.
> **Used by:** The local coding agent executing phase P10.
> **Safe to transfer:** yes
> **Contains local code names:** no

## H-Phase 10 — Retry / recovery / MAYBE_SUBMITTED (P10)

### RC-01 — POST-failure classifier

- **Task ID:** RC-01
- **Title:** Implement the closed classifier from CA-1: cause → (category, code, retryable, severity, submission_state, target dimensions); fail closed
- **Classification:** MVP normative implementation
- **Purpose:** §7.0/§7.2: unmapped mid-call → MAYBE·CONFIRM·READY; unmapped engine code → MAYBE·BLOCKED(UNMAPPED_CODE); never "assume retryable"; HTTP 200 classified from the body.
- **Prerequisites:** CA-1 published; D-05 branch inventory.
- **Requirement sections / concepts to read:** §7.2 (whole), §7.3, §7.1, CA-1.
- **Placeholder components involved:** [Provider Response Parser].
- **Local placeholder mappings required before starting:** parser mapped.
- **Local code areas to discover:** existing branch code (to be routed through the classifier).
- **How to locate:** D-05.
- **Implementation instructions:** classifier as data-driven mapping loaded from CA-1's table (externalized so a table version bump is config, not code — §16.6); inputs: transport outcome (connect-fail / read-timeout / reset-after-write / crash marker) + HTTP body/code; outputs per CA-1; defaults per §7.2's two fail-closed rows; enrichment outcomes route via the same taxonomy (§7.3 rows).
- **Do not change:** business-data extraction on success.
- **Tests to add:** fixture per CA-1 row; the fail-closed defaults; 200-with-error-body classified from body; §7.3 rows.
- **Edge cases:** replay-original-response class (from CT-02's finding) classified deliberately per CA-1.
- **Manual validation:** classifier coverage report vs CA-1 row count.
- **Expected outcome:** closed taxonomy live.
- **Failure signs:** a default-retryable catch-all anywhere.
- **Common mistakes:** classifying DUPLICATE_REQUEST as an error (it is MAYBE + query — §7.2).
- **Completion criteria:** tests green.
- **Stop condition:** merged.
- **Next task:** RC-02.

### RC-02 — Response-driven state application

- **Task ID:** RC-02
- **Title:** Apply classifier outputs as §7.2's exact tuple transitions, incl. the collision branch on divergence_expected and UETR/marker side effects
- **Classification:** MVP normative implementation
- **Purpose:** the POST·CLAIMED → next-state map, exactly per §7.2/§10.5 rows.
- **Prerequisites:** RC-01; ST-02/ST-06/ST-07; U-01; IN-04 (markers); RG-02 (release on sync definitive rejects).
- **Requirement sections / concepts to read:** §7.2 (every row), §10.5 (POST rows), §2.2 (divergent_payload_at write-once), §7.1.
- **Placeholder components involved:** [Provider Response Parser], [Request Status Persistence Layer].
- **Local placeholder mappings required before starting:** RC-01 outputs wired.
- **Local code areas to discover:** response-processing transaction.
- **How to locate:** D-05.
- **Implementation instructions:** per class: connect-fail → NOT_SUBMITTED, POST·RETRY_WAIT; ambiguous (read timeout/reset/crash) → MAYBE, CONFIRM·READY (+maybe_since); sync accepted → SUBMITTED (+submitted_at), CONFIRM·READY; DUPLICATE_REQUEST → MAYBE, CONFIRM·READY + schedule query; NO uetr write; collision → set divergent_payload_at (write-once, same transaction); branch divergence_expected: TRUE → MAYBE, CONFIRM·READY + query (no park, no CRITICAL); FALSE → MAYBE, POST·BLOCKED(ENGINE_INCONSISTENCY) + CRITICAL; NO uetr write; unmapped engine code → MAYBE, POST·BLOCKED(UNMAPPED_CODE) + alert; sync business reject (insufficient balance class) → NOT_SUBMITTED, POST·RETRY_WAIT (policy) or BLOCKED(OPS_PARKED) per CA-1; sync definitive invalid-data → outcome=REJECTED + validation_failed marker + release; other definitive → outcome=REJECTED + provider_rejected marker + release. All via evidence-mirror-guarded CAS (late response vs already-terminal row → 0 rows). §14.1 rider (switch-gated): when the applied CAS affects exactly one row, INSERT the ATTEMPT_RESOLVED journal row (outcome = the §7.2 class VERBATIM) in the SAME transaction; rowCount 0 inserts nothing; the matching §14 log line (ST-08 convention) carries attempt_event_type = 'ATTEMPT_RESOLVED' — exact field name, byte-equal to the journal token (review b760786 M2). Canonical failure rule (§14.1): statement-local insert failures caught around the single statement, alerted AFTER host commit, transition proceeds; FATAL failures = ordinary infra failures; guarantee = no incorrect payment outcome.
- **Do not change:** classifier internals (RC-01).
- **Tests to add:** one test per row above; mirror-rule (late accepted vs EXECUTED row); collision write-once (second collision doesn't overwrite the timestamp); marker totality on both reject flavors (exactly one marker); T-38: ATTEMPT_RESOLVED written iff rowCount==1 (the 0-rows path inserts nothing).
- **Edge cases:** DR-replay row (no prior hash → flag false) collision → ANOMALOUS branch (ties K-05's edge case).
- **Manual validation:** stub-driven run of every class.
- **Expected outcome:** POST outcomes exact.
- **Failure signs:** any response path bypassing the CAS helper.
- **Common mistakes:** releasing on the business-reject RETRY_WAIT path (NOT_SUBMITTED but still active — no release until terminal).
- **Completion criteria:** tests green.
- **Stop condition:** merged.
- **Next task:** RC-03.

### RC-03 — repost_permitted gate

- **Task ID:** RC-03
- **Title:** Implement §7.0's repost_permitted as ONE function, checked by every POST-routing writer AND by the posting claim
- **Classification:** MVP normative implementation
- **Purpose:** the single normative re-POST gate; both-ends checking kills the park⇄un-park livelock class structurally; blocked_reason plays NO part.
- **Prerequisites:** RC-02 (divergent_payload_at written); RC-09 (freeze check — can stub as FROZEN-safe until RC-09 lands). (The former B-03/cutoff prerequisite was RETIRED round 10 — no cutoff term exists, §7.4.)
- **Requirement sections / concepts to read:** §7.0 (predicate + both-ends + override), §6.4 (staleness term), §11 (claim carries the durable term).
- **Placeholder components involved:** [Request Status Persistence Layer], [Retry Resolver Job], [Status Query Resolver], [Provider POST Client].
- **Local placeholder mappings required before starting:** claim CAS site; POST-routing writers list (ops actions later).
- **Local code areas to discover:** none new.
- **How to locate:** ST-09/K-04 sites.
- **Implementation instructions:** repost_permitted(request) = divergent_payload_at IS NULL ∧ NOT(amount stale vs current shortfall ∧ MAYBE_SUBMITTED) ∧ freeze OFF ∧ outcome IS NULL (round 10: NO cutoff term — the engine owns the calendar, §7.4); called by: §9.2 downgrade writer (RC-07), ops re-POST writers (OP scope + §10.5 ops rows), retry scanner pre-claim (RC-04); AND the posting-claim CAS carries divergent_payload_at IS NULL in its WHERE + re-checks derived terms pre-launch (K-04/ST-09 site); dual-control override wired to override ONLY the staleness term (consumed by the future ops action; expose the parameter, no UI).
- **Do not change:** the term list except CT-04's recorded TTL consequence (if that follow-up exists, add the TTL term HERE with its own test).
- **Tests to add:** term-by-term falsification; both-ends test (a laundered blocked_reason cannot enable a forbidden re-POST — writer refuses AND claim hits row-count 0/refuses); override overrides staleness ONLY.
- **Edge cases:** amount staleness evaluated under the obligation lock (shortfall is I5 — lock-bound).
- **Manual validation:** trace one denied and one permitted re-POST.
- **Expected outcome:** one gate, two doors.
- **Failure signs:** any writer consulting blocked_reason for re-POST decisions (§10.1).
- **Common mistakes:** caching an "unfrozen" freeze answer (§16.1 — only FROZEN may be cached).
- **Completion criteria:** tests green.
- **Stop condition:** merged.
- **Next task:** RC-04.

### RC-04 — Retry scanner + policy

- **Task ID:** RC-04
- **Title:** Retry scanner per §7.4: per-error-class policy from config, exhaustion → BLOCKED, downgrade policy class, zero-attempt outage windows (structural; round 10 — no cutoff pre-checks, the engine owns the calendar)
- **Classification:** MVP normative implementation
- **Purpose:** exactly one retry owner (the DB scanner); §16.1's zero-attempt gating prevents an outage from converting the RETRY_WAIT population to BLOCKED.
- **Prerequisites:** ST-09 (claims), RC-03 (gate), RC-01/02 (classification + application). (B-03 prerequisite RETIRED round 10.)
- **Requirement sections / concepts to read:** §7.4 (whole incl. downgrade class), §16.1 (scanner rules, clock semantics, poison cap), §16.6 (config entries).
- **Placeholder components involved:** [Retry Resolver Job], [Metrics / Alerting Layer].
- **Local placeholder mappings required before starting:** job infra; S-07 index expressions (queries must match).
- **Local code areas to discover:** in-process retry wrappers on the POST path (from D-05 — REMOVE them here, the single-owner rule).
- **How to locate:** D-05/D-08 inventories.
- **Implementation instructions:** scanner: breaker-gated, §11 claim protocol (lock-free bounded candidate selection, jittered backoff; per candidate a NEW transaction locks the OBLIGATION first then runs the claim CAS — mechanics M5); per row (ROUND 10: the local cutoff pre-check is RETIRED — the engine owns the calendar §7.4, MAX ATTEMPTS is the only retry bound, nothing wired to retry_deadline_at; the round-8/9 pointer claim-gate was likewise REMOVED with the §2.4 greenfield fact): repost_permitted for POST-stage rows (retry-guard branch per RG-07); execute stage work; failure → policy: next_retry_at per class config (base/multiplier/max), attempt_count++; exhaustion → BLOCKED(RETRY_EXHAUSTED) (+ MAYBE rows keep submission_state — stay in resolver scope, maybe_since keeps running — §7.4); downgrade policy class: next_retry_at=now, attempt_count RESET, small max (config 2–3); while freeze effective or breaker OPEN → zero attempts (structural safety — no budget mechanism exists to build); poison-row cap → BLOCKED + alert; remove stacked in-process retries on the POST.
- **Do not change:** enrichment micro-retries for idempotent reads (§16.1 permits those).
- **Tests to add:** policy schedule math; exhaustion → BLOCKED with MAYBE preserved; 6-hour simulated outage → zero attempts made, attempt_count unchanged, zero BLOCKED conversions (the §16.1 scenario, structural); poison cap; single-owner (no nested retry on POST — structural assert/test where feasible).
- **Edge cases:** downgrade-class rows re-posting immediately (next_retry_at=now) — L7 satisfied by the explicit write (§9.2).
- **Manual validation:** seeded RETRY_WAIT population through a scripted breaker-OPEN window.
- **Expected outcome:** one disciplined retry owner.
- **Failure signs:** any attempt made (or attempt budget consumed) while frozen/breaker-OPEN.
- **Common mistakes:** counting business rejects as breaker failures (§16.1 — they are successes to the breaker).
- **Completion criteria:** tests green.
- **Stop condition:** merged.
- **Next task:** RC-05.

### RC-05 — Resolver sweep scope + shaping

- **Task ID:** RC-05
- **Title:** Build the §9.5 resolver sweep: submission-keyed scope, bounded prioritized batches, per-row next_query_at backoff, never-overlap, SUBMITTED damping
- **Classification:** MVP normative implementation
- **Purpose:** §9.5: scope keyed on submission_state + outcome ONLY (stage/history scoping explicitly rejected); post-outage herds shaped under the engine's rate limit.
- **Prerequisites:** ST-07 (anchors), S-07 (indexes), CA-3 (mapping — application is RC-06), TL-13 answer (budget value — config; stub conservative if pending, record).
- **Requirement sections / concepts to read:** §9.5 (whole), §9 (intro), §16.6 (cadence/budget/backoff/damping entries).
- **Placeholder components involved:** [Status Query Resolver], [Metrics / Alerting Layer].
- **Local placeholder mappings required before starting:** query client (D-06); job infra.
- **Local code areas to discover:** none new.
- **How to locate:** D-06/D-08.
- **Implementation instructions:** scope: ACTIVE ∧ MAYBE (any stage/stage_state incl. BLOCKED) ∪ ACTIVE ∧ SUBMITTED older than confirmation age (incl. BLOCKED); order: oldest maybe_since first (round 10 — no cutoff exists); per-sweep query budget from config (rate-limit-derived); per-row next_query_at with backoff; a sweep overrun → §15 metric, next sweep waits (never overlap — single-flight guard); SUBMITTED branch damps while feed-lag metric exceeds confirmation age; MAYBE branch never damps; ops-triggered mode: query an explicit key set regardless of state (§5.2 step 5's executor — the MODE exists at MVP as a callable entry point; the runbook that uses it is post-MVP).
- **Do not change:** evidence application (RC-06).
- **Tests to add:** scope inclusion/exclusion table (BLOCKED·MAYBE in; terminal out; CONFIRM·BLOCKED·SUBMITTED in past age); ordering; budget respected over a seeded 1000-row herd; overlap prevented; damping on/off; backoff progression.
- **Edge cases:** rows with next_query_at in the future skipped within scope (assert).
- **Manual validation:** seeded herd run with a throttled stub.
- **Expected outcome:** disciplined sweep.
- **Failure signs:** scope containing stage conditions (the recurring four-round lesson — §9.5).
- **Common mistakes:** damping the MAYBE branch.
- **Completion criteria:** tests green.
- **Stop condition:** merged.
- **Next task:** RC-06.

### RC-06 — Query-outcome application

- **Task ID:** RC-06
- **Title:** Apply §9.1 outcomes through the shared evidence helper: EXECUTED/REJECTED/INDETERMINATE/ACCEPTED (+ NOT_FOUND routed to RC-07)
- **Classification:** MVP normative implementation
- **Purpose:** ask, never blind-retry; both resolver and feed converge on the same CAS (race-safe by construction, §9.4).
- **Prerequisites:** RC-05 (sweep), IN-07 (shared helper), CA-3.
- **Requirement sections / concepts to read:** §9.1, §9.4 (race safety), §4.4.
- **Placeholder components involved:** [Status Query Resolver], [Request Status Persistence Layer].
- **Local placeholder mappings required before starting:** IN-07 helper.
- **Local code areas to discover:** none new.
- **How to locate:** n/a.
- **Implementation instructions:** EXECUTED → settlement via the helper (amount equality; RG-03); REJECTED → authoritative negative via the helper (outcome, marker, release, evidence flag); ACCEPTED/in-flight → intermediate evidence rules (no-op on CLAIMED; else SUBMITTED + CONFIRM·READY, BLOCKED preserved) + reschedule; INDETERMINATE (incl. query failure/timeout) → reschedule with backoff; escalation clocks keep running through query outages (assert — nothing pauses maybe_since); NOT_FOUND → RC-07's rule.
- **Do not change:** the helper (shared).
- **Tests to add:** each outcome; resolver-vs-feed race both orders (second lands 0 rows); query outage → INDETERMINATE + clocks unaffected.
- **Edge cases:** REJECTED answer for a SUBMITTED row — authoritative negative applies (release rights: engine negative is the sanctioned driver — §10.1).
- **Manual validation:** stub-driven outcomes.
- **Expected outcome:** recovery independent of feed delivery (§9.5's point).
- **Failure signs:** resolver writing anything not via the helper.
- **Common mistakes:** treating INDETERMINATE as NOT_FOUND.
- **Completion criteria:** tests green.
- **Stop condition:** merged.
- **Next task:** RC-07.

### RC-07 — Trust-age rule + §9.2 downgrade

- **Task ID:** RC-07
- **Title:** Implement NOT_FOUND trust-age semantics: MAYBE downgrade (the one sanctioned backward move) gated by repost_permitted; SUBMITTED → ENGINE_INCONSISTENCY park; resolver-applied deferred AMENDMENT_PARKED
- **Classification:** MVP normative implementation
- **Purpose:** §9.2 exactly: the self-heal for engine ingest outages; a row is never un-parked for an action the next gate would forbid.
- **Prerequisites:** RC-06; RC-03 (gate); RC-04 (downgrade policy class); ST-07 (anchors); NOT_FOUND_TRUST_AGE config (TL-5 — conservative stub if pending, record).
- **Requirement sections / concepts to read:** §9.2 (whole, incl. the recorded rationale + accepted consequence), §7.4 (downgrade class), §10.5 (downgrade + SUBMITTED rows).
- **Placeholder components involved:** [Status Query Resolver], [Request Status Persistence Layer].
- **Local placeholder mappings required before starting:** RC-03/RC-04 in place.
- **Local code areas to discover:** none new.
- **How to locate:** n/a.
- **Implementation instructions:** NOT_FOUND handler: age from last_post_attempt_at (MAYBE) / submitted_at (SUBMITTED) — NEVER state_changed_at; before trust-age → INDETERMINATE; after: MAYBE (any stage/stage_state) + repost_permitted → CAS to stage=POST, stage_state=RETRY_WAIT, next_retry_at=now, attempt_count reset (downgrade class), blocked_reason cleared, SUB stays MAYBE; MAYBE + gate fails → stay parked (wait-then-decide); if the failing term is amount staleness and the row is NOT already parked → resolver applies AMENDMENT_PARKED itself (idempotent, non-CLAIMED only — the deferred §6.4 park always lands); SUBMITTED + NOT_FOUND after trust-age → CONFIRM·BLOCKED(ENGINE_INCONSISTENCY) + CRITICAL, single answer suffices (no counter column exists — deliberate), row STAYS in resolver scope; downgrade's own POST response settles SUB per §9.2's list.
- **Do not change:** §9.4 (a query answer NEVER releases).
- **Tests to add:** age anchors (attempt restarts MAYBE clock; churn doesn't); pre-trust-age NOT_FOUND → reschedule only; permitted downgrade full tuple (add the pending ST-03 legality case: the sanctioned backward move); gate-fail stays parked (each failing term); deferred-park application; SUBMITTED park + reversibility (next successful query resolves it); downgraded row claimable by RC-04 immediately; escalated row downgrade doesn't cycle (with RC-08 — pending case there).
- **Edge cases:** rows aged past the engine's query lookback ride the §18-1(c) retention proof / named TTL decision (round 10 — no local cutoff guard exists); DUPLICATE_REQUEST answering the downgrade re-POST → MAYBE + query (hidden earlier attempt surfaced; prior uetr intact — the §16.6-6 named test).
- **Manual validation:** scripted ingest-outage simulation: population NOT_FOUND → downgrades fire only where permitted.
- **Expected outcome:** self-healing MAYBE machinery, livelock-free.
- **Failure signs:** downgrade firing on a blocked_reason condition (must be gate-only).
- **Common mistakes:** measuring MAYBE age from maybe_since for the TRUST-AGE (it is last_post_attempt_at — maybe_since drives ESCALATION, a different clock; §2.2 explains both).
- **Completion criteria:** tests green.
- **Stop condition:** merged.
- **Next task:** RC-08.

### RC-08 — Escalation scanner

- **Task ID:** RC-08
- **Title:** §9.3 escalation on the maybe_since clock: once per episode (escalated_at gate), non-CLAIMED writes, already-BLOCKED alert-only, tier-2 re-page
- **Classification:** MVP normative implementation
- **Purpose:** bounded human hand-off for unresolved MAYBE rows, early enough to act while the payment still matters (age-based — round 10); never a downgrade⇄escalate cycle.
- **Prerequisites:** ST-07 (anchors + escalated_at contract), RC-07 (downgrade interplay), config (escalation age PO-3; tier-2).
- **Requirement sections / concepts to read:** §9.3 (whole), §2.2 (escalated_at), §13 (ESCALATED code), §16.6 (ordering validation).
- **Placeholder components involved:** [Retry Resolver Job] family (scanner), [Metrics / Alerting Layer].
- **Local placeholder mappings required before starting:** scanner infra; S-07 escalation index.
- **Local code areas to discover:** none new.
- **How to locate:** n/a.
- **Implementation instructions:** scanner scope: outcome IS NULL ∧ MAYBE ∧ maybe_since older than threshold; per row: CRITICAL alert ALWAYS; state write CAS to same-stage BLOCKED(ESCALATED) + escalated_at, gated on escalated_at IS NULL ∧ non-CLAIMED ∧ non-BLOCKED (already-BLOCKED → alert only, NO overwrite — §9.3/§10.1; CLAIMED → alert, write deferred); tier-2 threshold on the same clock → re-page/incident; escalated_at cleared with maybe_since (ST-07).
- **Do not change:** blocked_reason of already-BLOCKED rows (never overwritten — §10.1).
- **Tests to add:** once-per-episode (downgrade un-parks an ESCALATED row with elapsed maybe_since → no immediate re-park — the §9.3 cycle gate); already-BLOCKED → alert-only; CLAIMED deferred; tier-2 fires; frozen rows excluded (outcome term + cleared maybe_since, belt and braces — §2.2).
- **Edge cases:** escalation racing the resolver's settle — CAS decides; alert may fire once redundantly (harmless; note in runbook).
- **Manual validation:** seeded aging rows through both tiers.
- **Expected outcome:** bounded, non-cycling escalation.
- **Failure signs:** age keyed on state_changed_at (churn re-arms — the exact §15 discipline violation).
- **Common mistakes:** overwriting OPS_PARKED with ESCALATED.
- **Completion criteria:** tests green.
- **Stop condition:** merged.
- **Next task:** RC-09.

### RC-09 — Posting freeze check (Hazelcast)

- **Task ID:** RC-09
- **Title:** Fail-safe posting-freeze check: absent/unreachable/timeout = FROZEN; only FROZEN cached; reason/operator/ticket on the toggle; checked before every claim and POST
- **Classification:** MVP normative implementation
- **Purpose:** §16.1: a DB restore cannot un-freeze posting (toggle outside the DB); a grid outage pauses payments (fail-blocked, PO signed off).
- **Prerequisites:** D-10 (Hazelcast topology + existing toggle shape); RC-03 (consumes the answer).
- **Requirement sections / concepts to read:** §16.1 (freeze block, incl. cache rule, propagation bound, silent-by-design note), §15 (freeze-effective page — OB-05 wires it).
- **Placeholder components involved:** [Provider POST Client], [Retry Resolver Job], Hazelcast client.
- **Local placeholder mappings required before starting:** Hazelcast client Confirmed; existing role-controlled toggle located or created per local convention (record).
- **Local code areas to discover:** grid access patterns.
- **How to locate:** D-10.
- **Implementation instructions:** freeze read with bounded timeout; toggle absent OR grid unreachable OR timeout → FROZEN; only the FROZEN answer cacheable; "unfrozen" always re-read; toggle payload carries reason/operator/ticket id; checked before every posting claim AND before every POST (both ends — §16.1); kill-switch semantics: stops POSTs ONLY — feed consumption, §9 queries, card reads continue (§16.1); expose freeze-effective state as a metric for OB-05's page.
- **Do not change:** toggle role control (exists operationally — §16.1 operational fact).
- **Tests to add:** all three fail-safe conditions read FROZEN; unfrozen never cached (two reads hit the grid); frozen blocks claim and POST; resolver/feed/reads unaffected while frozen; QUEUE-RACE (review c8a92f1 H2): scanner reads unfrozen → candidate queued → freeze becomes effective → worker runs → ZERO claim mutation and ZERO journal row (the worker's own pre-claim check catches it, not just the pre-wire check).
- **Edge cases:** flip mid-flight: in-flight POST completes (drain semantics §11/§16.1) — assert no interruption machinery exists. LINEARIZATION (§16.1, review 4d5cb83 L2): a worker that passed its pre-claim freeze read before the flip is IN FLIGHT — it may still commit one claim (the pre-wire re-check stops the wire; lease expiry resolves the claim; propagation bound + drain own the boundary); "zero attempts" means zero WIRE calls; a grid read cannot form an atomic fence with the Oracle claim, BY DECISION (no fencing token). Test: worker passes pre-claim check → freeze flips → claim may commit, wire NEVER called, row resolves via lease expiry.
- **Manual validation:** local grid kill → posting stops, resolver continues.
- **Expected outcome:** fail-safe freeze.
- **Failure signs:** a TTL cache on "unfrozen" (§16.1 names this violation).
- **Common mistakes:** freeze check inside the claim transaction only (must also guard the wire call).
- **Completion criteria:** tests green.
- **Stop condition:** merged.
- **Next task:** RC-10.

### RC-10 — Breaker + budget-suspension integration

- **Task ID:** RC-10
- **Title:** Circuit breaker per dependency (business rejects = successes); scanner gating; structural attempt-budget safety across freeze/breaker windows
- **Classification:** MVP normative implementation
- **Purpose:** §16.1: an outage becomes quiet waiting; a 6-hour engine outage must not flood the ops queue at recovery. SIMPLIFIED by the 2026-07-11 retry-bounds decision (§7.4; cutoff retired round 10 — engine owns the calendar): the retry limit is MAX ATTEMPTS — there is NO wall-clock deadline and NO cutoff, so there is NO suspension mechanism to build; suspension is structural (gated scanners make zero attempts).
- **Prerequisites:** RC-04 (retry scanner), RC-09 (freeze state).
- **Requirement sections / concepts to read:** §16.1 (breaker + clock semantics + bulkheads + timeouts), §7.4 (bounds decision), §16.6 (thresholds config).
- **Placeholder components involved:** [Provider POST Client], [Retry Resolver Job], [Status Query Resolver], [Metrics / Alerting Layer].
- **Local placeholder mappings required before starting:** breaker library/conventions (D-10).
- **Local code areas to discover:** existing breaker/timeouts per dependency.
- **How to locate:** D-05/D-10.
- **Implementation instructions:** per-dependency breakers (enrichment, account service, engine POST, status-query API) with explicit timeout budgets (config §16.6); business rejects recorded as successes; scanners gate on breaker state pre-claim; VERIFY structurally (no mechanism to build): while frozen or OPEN, scanners claim nothing, attempt_count does not move, and no row transitions to BLOCKED for time-based reasons; round 10: no cutoff check exists at attempt time or anywhere; bulkhead check: posting, enrichment, card-read pools separate (record local reality; if shared, this is a change task — bounded queues, DB as the real queue).
- **Do not change:** breaker library choice (local convention); retry_deadline_at stays reserved/unused (§2.2 — do not wire it into any rule).
- **Tests to add:** breaker opens on transport failures only; scanner claims zero while OPEN; attempt_count unchanged across a simulated 6-hour OPEN window and the RETRY_WAIT population intact at recovery (zero BLOCKED conversions); query-API breaker → INDETERMINATE handling (RC-06) not NOT_FOUND.
- **Edge cases:** breaker half-open probes are attempts (consume attempt budget normally — only OPEN windows make zero attempts).
- **Manual validation:** scripted outage rehearsal.
- **Expected outcome:** outage-shaped behavior per spec.
- **Failure signs:** RETRY_WAIT population converting to BLOCKED during a simulated outage.
- **Common mistakes:** one global breaker for all dependencies.
- **Completion criteria:** tests green.
- **Stop condition:** merged; Phase P10 report. NOTE: production ENABLEMENT of the §9.2 auto-downgrade remains gated on P8 PASS (Section M order).
- **Next task:** OP-01.


---

## Phase handoff summary (P10 → P11)

- **Phase outputs:** CA-1-driven fail-closed classifier; §7.2 tuple transitions (incl. collision branch on the claim-time flag); repost_permitted checked at BOTH ends; §7.4 retry scanner with structural outage safety + poison cap; §9.5 submission-keyed shaped resolver sweep; §9.1 outcome application via the shared helper; §9.2 trust-age + downgrade + SUBMITTED park; §9.3 once-per-episode tiered escalation; fail-safe Hazelcast freeze; per-dependency breakers.
- **Blockers to carry forward:** PRODUCTION ENABLEMENT of the §9.2 auto-downgrade stays gated on P8 PASS + TL-5-derived trust age (rollout stage F4); TL-13 rate limit for the real sweep budget (§18-2 CLOSED round 10 — engine owns the calendar).
- **Local mapping rows expected filled:** [Retry Resolver Job], [Status Query Resolver] rows complete; stacked-retry removals recorded.
- **Tests expected to exist:** classifier fixtures, §7.2 row tests, gate term-by-term + both-ends (T-23), zero-attempt outage rehearsals (part of T-32), sweep scope/budget tests, T-22 lifecycle set, escalation cycle-gate tests, freeze fail-safe tests.
- **Next phase entry condition:** RC-10 done; phase report filed.
