# Failure & Recovery Walkthrough — Every Way It Breaks, Every Way It Heals

**Purpose:** team + PO walkthrough of every failure scenario this process can
encounter, and the recovery path for each — to verify nothing is missing from
the design. Companion visualization: `failure-recovery-map.html`.
**Baseline:** `requirment-v4.md` (all § references) + `implementation-playbook.md`.
**Date:** 2026-07-10.
**Assumption (per PO):** data, system, cloud, user — everything can break.

This document is a DERIVED VIEW for review purposes. Where it and
`requirment-v4.md` disagree, the requirement doc wins. It deliberately does
not re-challenge the §1 contract facts or the §1.1 Basic Agreements — those
are settled ground; scenarios that touch them state the agreed behavior.

------

## 0. The recovery ladder

Every scenario below resolves at one of five tiers. The design philosophy
(P5) is: prefer the lowest tier that is safe; when automation cannot be safe,
fail BLOCKED with an alarm — never pay twice, never fail silently.

```text
T0  PREVENTED       The failure is unrepresentable: DB constraints (I6,
                    unique keys, legality-matrix CHECKs, trigger backstops),
                    write-once outcome, evidence-guarded CAS, deterministic
                    identity. Nothing to recover — the bad write never lands.

T1  SELF-HEALING    The system fixes it alone: durable retries from persisted
                    request rows (§7.4), the status-query resolver (§9),
                    redelivery convergence (§6.1/§6.7/§8 inbox), circuit
                    breakers + suspended deadlines (§16.1), derivation
                    re-running after every mutation (§4). No human involved.

T2  SELF-HEALING,   Same as T1, but a human is INFORMED (volume alerts,
    ALERTED         watchdogs, CRITICAL anomaly pages) because the event is
                    evidence of something worth investigating — the payment
                    itself still resolves automatically.

T3  OPS ACTION      A sanctioned human action is required: marker clears
                    (§19.3-pattern), ops retry of a BLOCKED row (§20-1),
                    dual-control stale-amount re-POST (§7.0 override),
                    APPLY-PLATFORM-VERIFIED-OUTCOME procedure (§9.3),
                    supersede/close (§3), Hazelcast freeze flip (§16.1),
                    DLT replay (§16.2), manual tie application (§6.7).
                    MVP REALITY: these run as controlled DB procedures +
                    role-controlled toggles — the PO accepted shipping
                    without an ops console (§20). The future ops console
                    (`ops-console-proposal.md`) gives this tier its API/UI.

T4  EXTERNAL        Recovery lives outside this system: a corrected upstream
    RECONCILIATION  message (§6.8), a platform-side recall/refund or formal
                    reject (§19.2 family, TL-10), engine-side records, the
                    client/treasury funding an account. This system's job is
                    to hold the reservation, display the exception, and wait.
```

Two standing facts frame everything:

- **Retry never depends on upstream resending.** Every retryable failure
  after intake lives on a durable `payment_request` row; scanners and the
  resolver work from tables, not from Kafka redelivery. Upstream re-sends
  only ever carry *corrections* (new business truth), never "please retry."
- **The failure direction is fail-blocked.** A payment can be DELAYED
  (blocked + alarmed + reservation held) by any failure below; it can be
  LOST (silently unpaid, no signal) in exactly one place — scenario U-1 —
  which is upstream's blind spot, now tracked as upstream ask 7 (§18).

------

## 1. Upstream & intake failures (U)

| # | What breaks | How we notice | Recovery | Tier |
|---|-------------|---------------|----------|------|
| U-1 | Upstream NEVER emits the message (producer bug, trade stuck before the payment step) | **We cannot** — no row exists. Card shows NOT_STARTED (defined display, §12). | Upstream monitoring / business chase. **GAP CLOSED THIS ROUND:** upstream ask 7 (§18) — upstream must confirm emit-failure monitoring or a count-recon feed; this is the ONLY lost-payment class with no local detector. | T4 |
| U-2 | Message emitted but Kafka delivery delayed/broker outage | Consumer-lag alert per flow (§15); card shows data-as-of/lag indicator (§12) | At-least-once delivery resumes; ordering guard absorbs any replay | T1/T2 |
| U-3 | Message malformed, scope key extractable | Anchor obligation created: IN_PROGRESS + DATA_VALIDATION_FAILED on the card (§6.6); marker-age alert (§15) | Corrected upstream message creates the first request (§6.8) | T4 |
| U-4 | Message body unparseable, Kafka key readable | Key-only anchor (trade-level display row) + DLT page (§6.6 tiered handling, TL-7) | Corrected message; anchor deleted when first valid snapshot applies | T3 (DLT triage) + T4 |
| U-5 | Message unreadable including the key | DLT depth > 0 pages (§16.2) | Accepted blind spot, shrunk to key corruption only (§6.6); manual DLT replay after fix | T3 |
| U-6 | Snapshot violates within-snapshot tuple uniqueness (§6.0) — would silently merge two payments | Whole-snapshot validation failure: anchors + validation_failed marker on ALL the trade's scopes (§6.6 blast radius), alert | Corrected snapshot clears markers by ordering; in-flight requests untouched | T4 |
| U-7 | Duplicate redelivery (identical snapshot) | Nothing to notice — second apply sees shortfall 0 / ordering not newer | Converges silently (§6.1, §6.7) | T0/T1 |
| U-8 | Out-of-order delivery — older snapshot arrives after newer | Stale-message metric; alert on volume (§6.7) | Watermark drops it; BY DESIGN under BA-3 | T1/T2 |
| U-9 | Two genuine amendments share an ordering timestamp, payloads differ | AMENDMENT_TIE_CONFLICT alert (§6.7) | Manual application by ops (§20-10 / console O12 — **GAP-3, closed this round**) from the tie-conflict record's preserved payload (§6.7 executability requirement); a resend cannot fix a tie (same timestamp) | T3 |
| U-10 | Consumer crashes mid-fan-out of a multi-payment snapshot | Nothing visible — redelivered snapshot re-applies | Applied blocks drop as stale, unapplied blocks apply; per-block transactions converge (§6.1) | T1 |
| U-11 | Amendment lowers amount while request un-posted | — | Auto-cancel (§6.4) + right-sized successor via §6.8 | T1 |
| U-12 | Amendment lowers amount while request MAYBE_SUBMITTED | AMENDMENT_PARKED + alert; rank-1 exception on card | Wait-then-decide: resolver keeps querying; feed/query settles it; §9.3 escalation brings ops in (dual-control stale re-POST, TL-10, or §9.3 procedure) | T1 → T3 |
| U-13 | Amendment raises amount while request in flight | — | Deferred successor: §6.8 creates it when the active request resolves (PO-6). Never lost. | T1 |
| U-14 | Scope-key field changes (payment_type / debit_account / currency) | New obligation appears (card shows both) | BA-1: NEW obligation paid under new info — agreed business behavior, NOT a duplicate. Old scope follows its own lifecycle. | Settled (§1.1) |
| U-15 | Payment absent from a newer snapshot | Nothing (interim) | OPEN — PO-9 (§18): interim absence = NO-OP; if PO answers "absence = cancel", §6.4 machinery handles it | T4 (pending PO) |
| U-16 | Delayed older snapshot hits an obligation absent from newer snapshots (stale watermark) | Stale-amount application risk | OPEN — TL-16 (§18): watermark-advance rule; decide with PO-9 | tracked |
| U-17 | Upstream data wrong but valid (wrong amount/account that passes validation) | Not detectable locally — data is the contract | Corrected message (amendment machinery §6.3–§6.8); if already executed: platform recall (§19.2 family) | T4 |
| U-18 | Poison pill breaks deserialization loop | ErrorHandlingDeserializer + DLT page (§16.2) | Fix producer / replay DLT preserving keys | T3 |

**Deep-dive trace (U-3 → recovery):** malformed message for a new trade →
anchor row (required_amount NULL, marker LIVE by definition) → card shows the
exception instead of a healthy NOT_STARTED → §4.1 predicate cannot complete
the scope → corrected message arrives with newer ordering → marker goes
not-live in the same transaction → §6.8 creates the first request → normal
flow. Nothing was lost; the whole failure window was visible.

------

## 2. Enrichment failures (E)

The PO's key question — "the account-identification API fails; the account is
in the scope key; can we retry?" — is answered by ordering: the scope key is
built from MESSAGE FIELDS ONLY (§6.0), the request row + reservation exist
BEFORE any external call (§6.8), and the enrichment API is called at stage
ENRICH on that durable row. Retry needs nothing from upstream.
(The §6.0 premise itself is now pinned in writing as upstream ask 6, §18.)

| # | What breaks | How we notice | Recovery | Tier |
|---|-------------|---------------|----------|------|
| E-1 | Enrichment/account API transient failure (timeout, 5xx, connect) | Retry metrics; breaker state | ENRICH·RETRY_WAIT → retry scanner, per-class policy (§7.3, §7.4); breaker gates scanners during outage, deadlines suspended (§16.1) | T1 |
| E-2 | Enrichment retries exhausted | BLOCKED(RETRY_EXHAUSTED) → ops-queue metric + stuck alert (§15) | Ops retry re-enriches in place (§20-1, L7 — never skips to POST) | T3 |
| E-3 | Definitive invalid-data result | outcome = REJECTED, validation_failed marker, exception on card | Corrected upstream message → §6.8 successor (NOT_SUBMITTED, so release was legal) | T4 |
| E-4 | Unmapped/unclassifiable result | BLOCKED(UNMAPPED_CODE) + alert — fail closed (§7.3) | Ops classifies; artifact-1 table updated; ops retry | T3 |
| E-5 | Enrichment returns WRONG data that validates (bad reference data) | Not detectable locally | Next attempt re-resolves fresh (§7.0) if not yet executed; executed payment paid what was current → platform recall (§19.2 family) | T4 |
| E-6 | Trade store unavailable during fresh assembly of a re-POST (§9.2 downgrade) | Retry metrics | Assembly failure = nothing sent; row returns to RETRY_WAIT under its policy; submission_state unchanged | T1 |
| E-7 | Worker dies holding ENRICH claim | Lease expiry (§11) | Re-claimable in place — enrichment is read-only, no external effect | T1 |

------

## 3. Posting failures (P)

| # | What breaks | How we notice | Recovery | Tier |
|---|-------------|---------------|----------|------|
| P-1 | Connect timeout / refused — request never left | last_error_code; retry metrics | NOT_SUBMITTED → POST·RETRY_WAIT; durable retry (§7.2) | T1 |
| P-2 | Read timeout / reset after write / crash mid-call | MAYBE_SUBMITTED; rank-1 PAYMENT_OUTCOME_UNKNOWN on card; MAYBE-age alerts | Resolver owns it (§9): query by key; trust-age rule; §9.2 downgrade re-POST if truly never received | T1 |
| P-3 | Worker crashes between claim and response | Lease expires (§11); last_sent_hash + last_post_attempt_at persisted BEFORE the call are the record of what may be executing | Lease-expiry recovery → MAYBE path → resolver | T1 |
| P-4 | DUPLICATE_REQUEST on first known attempt (lost earlier attempt got through) | Classified §7.2 | MAYBE → CONFIRM·READY + status query; dead UETR NOT persisted (§5) | T1 |
| P-5 | Known key + different payload, divergence EXPECTED (we re-assembled newer details) | §7.2 branch on divergence_expected flag | Evidence the original arrived: stay MAYBE, query resolves; divergent_payload_at set → no further re-POSTs ever | T1 |
| P-6 | Known key + different payload, ANOMALOUS (DR replay or engine disagrees) | BLOCKED(ENGINE_INCONSISTENCY) + CRITICAL | Ops investigation; resolver keeps querying; §9.3 exits | T3 |
| P-7 | Unmapped engine error code | MAYBE → BLOCKED(UNMAPPED_CODE) + alert — never "assume retryable" (§7.2) | Ops + artifact-1 classification update | T3 |
| P-8 | Insufficient balance (business reject) | Rank-3 exception, retryable per policy | Policy retry (T1); if funding is the issue: client/treasury funds the account (T4); optionally OPS_PARKED (T3) | T1/T3/T4 |
| P-9 | Invalid-data sync reject | REJECTED + validation_failed marker | Corrected upstream message → §6.8 successor | T4 |
| P-10 | Other definitive sync reject | REJECTED + provider_rejected marker + alert (§15 — a requested payment is NOT happening) | ONE newer valid message may retry (§6.8); from the 2nd reject: ops-only marker clear (§2.1, §19.3 pattern) — stops upstream-paced reject/re-pay loops | T4 → T3 |
| P-11 | Retry exhaustion / cutoff passed | BLOCKED(RETRY_EXHAUSTED / CUTOFF_EXPIRED) | Ops decision: retry next window (repost_permitted-gated), reject, or supersede (§20-1) | T3 |
| P-12 | Engine total outage | Breaker OPEN → ticket, page at 30m; ONE rolled-up incident (§15) | Scanners gate quietly; attempt/deadline budgets SUSPENDED while breaker open (§16.1) — no BLOCKED flood at recovery; automatic resume | T1/T2 |
| P-13 | Engine accepts but its ingest lags → mass NOT_FOUND | Observed-lag watchdog (§15) | Trust-age rule makes NOT_FOUND = INDETERMINATE until safe; auto-downgrade self-heals the population after trust-age (§9.2) | T1/T2 |
| P-14 | Hazelcast grid unreachable | Freeze-effective-without-ticket PAGE (§15) | Fail-safe: posting DISABLED (PO signed off); feed/resolver/card continue; infra restore un-freezes | T2 (halt) |
| P-15 | Posting freeze left on / flipped without ticket | Same page (§16.1) | Ops acknowledges or flips back (role-controlled toggle exists today) | T3 |

**Deep-dive trace (P-2, the MAYBE lifecycle):** POST times out → MAYBE_SUBMITTED,
reservation held (§3 — the safe default) → resolver queries by idempotency key
every cadence → (a) EXECUTED: confirm via evidence path, done; (b) REJECTED:
release + marker, §6.8 successor; (c) NOT_FOUND before trust-age: wait —
indistinguishable from ingest lag; (d) NOT_FOUND after trust-age +
repost_permitted: same-key re-POST — engine dedup makes it safe either way
(§1 assumed contract, proven by §18-1 sandbox gate); (e) nothing resolves
within escalation age: BLOCKED(ESCALATED) + CRITICAL, ops gets four exits
(§9.3), terminally the audited APPLY-PLATFORM-VERIFIED-OUTCOME procedure —
**no MAYBE row is ever permanently wedged at MVP** (§18 BLOCKING item 3).

------

## 4. Confirmation & status-feed failures (C)

| # | What breaks | How we notice | Recovery | Tier |
|---|-------------|---------------|----------|------|
| C-1 | Feed event lost / never emitted / missed | Unconfirmed SUBMITTED row ages past confirmation age | §9.5 sweep queries by key — feed is a latency optimization, the query API is the durable source | T1 |
| C-2 | Feed event arrives before executor commits (race) | Unmatched-event metric | Log + count + ack; sweep recovers at cadence latency (accepted trade-off, §8) | T1/T2 |
| C-3 | Feed event with unknown UETR (foreign/orphan) | Unmatched volume alert (§8) | Investigated in the payment platform (authoritative record); no local replay by design | T2/T4 |
| C-4 | Feed redelivery (same event_id) | — | Inbox table drops it before any lock (§2.3) | T0 |
| C-5 | Re-keyed duplicate (different event_id, same meaning) | — | Dies on evidence-guarded CAS row count (§8 layering) | T0 |
| C-6 | Settlement amount ≠ request amount | BLOCKED(AMOUNT_MISMATCH) + CRITICAL — all-or-nothing violated = defect evidence (§8) | Engine emits corrected event (completes normally) OR dispute concludes platform-side; local counters only via future manual adjustment (§19.2) | T3/T4 |
| C-7 | Settlement arrives for a TERMINAL (rejected/cancelled) row | CRITICAL anomaly — new event_id + zero-row CAS (§8); the §5.2 replay-divergence tripwire | Ops + platform reconciliation — money-truth divergence policy (§19.2) | T3/T4 |
| C-8 | Reject event under the dead UETR of a duplicate/collision submission | — | FORECLOSED: reject/collision UETRs are never persisted (§5), so the event cannot match and cannot release a live reservation | T0 |
| C-9 | Return/refund-style event for an EXECUTED request | Log + CRITICAL + ack; no state change (§8) | §19.2 future work; reconciliation lives platform-side | T4 |
| C-10 | Feed outage / consumer lag | Lag page (§15); card freshness indicator (§12) | Sweep continues independently; SUBMITTED-branch damping while lag exceeds confirmation age (§9.5) prevents false ENGINE_INCONSISTENCY parks | T1/T2 |
| C-11 | Contradictory evidence (reject after settle, stale status) | Stale evidence affects zero rows | Outcome write-once + evidence monotonicity (§4.4); anomalies page per C-7 | T0 |
| C-12 | provider_reference collision (reused reference) | UNIQUE index makes reuse loud (§8) | Fail-closed fallback already requires single-active-match + amount + recency; no match → sweep recovers by key | T0/T1 |

------

## 5. Resolver & status-query failures (R)

| # | What breaks | How we notice | Recovery | Tier |
|---|-------------|---------------|----------|------|
| R-1 | Query API down / timing out | INDETERMINATE + backoff; escalation clocks KEEP RUNNING (§9.1) | Fail toward a human, never toward silence: if the outage outlives escalation age, ops is paged with the row parked | T1 → T3 |
| R-2 | NOT_FOUND ambiguity (never received vs ingest lag vs lookback expiry) | Trust-age rule (§9.2) | Before trust-age: wait. After: MAYBE → same-key re-POST (safe by collision contract); SUBMITTED → ENGINE_INCONSISTENCY park (reversible — next good query resolves it) | T1/T2 |
| R-3 | Key aged past the engine's query lookback — NOT_FOUND unfalsifiable | Escalated row that query can never resolve | TL-5 ask (lookback ≥ max row lifetime); terminal exit = §9.3 procedure with platform-records verification | T3 |
| R-4 | Engine acknowledged a payment it now cannot find | CRITICAL ENGINE_INCONSISTENCY (§9.2 SUBMITTED branch) | Never re-POST an acknowledged payment; resolver keeps querying; platform ticket (§16.6 runbook) | T2/T3 |
| R-5 | Post-outage MAYBE population floods the query API | Sweep-overrun metric (§15) | Bounded prioritized batches (cutoff-first), per-row backoff, per-sweep budget from the engine's rate limit (§9.5) | T1 |
| R-6 | Resolver scheduler dies | Scanner-heartbeat page — any scanner silent 3× its interval (§15) | Restart; rows unaffected (state is in tables) | T2 |

------

## 6. Money-truth & invariant violations (M)

| # | What breaks | How we notice | Recovery | Tier |
|---|-------------|---------------|----------|------|
| M-1 | Counter drift — I1/I2 mismatch (bug or corruption) | Drift scanner PAGES (not logs); snapshot-read + locked re-check so read skew never pages (§3) | Investigation from §14 log lines; correction via platform-verified truth; the stored counter exists precisely as this tripwire | T3 |
| M-2 | Overpay — confirmed > required | Latch alert on SET; rank-1 OVERPAY_DETECTED (§13) | One-way door: automated payment never resumes on this trade; recovery (recall/refund) is platform-side (§19.2); ops annotation on card (§20-4) | T3/T4 |
| M-3 | Amendment lands on a latched scope | AMENDMENT_ON_LATCHED_SCOPE alert (§6.5) | Manual handling — post-latch local amounts may be fiction (refund window invisible) | T3 |
| M-4 | Cross-stream race: latched scope that is UNDERPAID vs latest truth | Same latch alerting; documented trace (§13) | Accepted permanent manual state; ops resolves with platform books | T3/T4 |
| M-5 | Stuck reservation — active request not progressing | Stuck-reservation age alert; per-(stage,stage_state) max-age tickets (§15) | Ops: retry / supersede-close (§3 required op; §20-2) — release FORBIDDEN while MAYBE/SUBMITTED unless engine negative (§10.1) | T3 |
| M-6 | Live marker with no active request, aging (correction never arrives) | Live-marker-no-request alert keyed on validation_failed_first_at (§15) | Chase upstream for the corrected message; ops may supersede the scope | T3/T4 |
| M-7 | Repeat validation-reject loop (upstream keeps sending bad data) | validation_reject_count ≥ 3 alert — no gate, correction IS the recovery (§2.1) | Escalate to upstream team | T4 |
| M-8 | Repeat provider-reject loop (reject → new message → reject…) | provider_reject_count ≥ 2 alert; auto-successor STOPS — ops-only clear from the 2nd reject (§2.1) | Ops investigates root cause with provider before re-enabling | T3 |

------

## 7. Infrastructure & platform failures (I)

| # | What breaks | How we notice | Recovery | Tier |
|---|-------------|---------------|----------|------|
| I-1 | Service crash / pod eviction (any worker) | Scanner heartbeats; lease expiry | Leases expire → ENRICH re-claims in place; POST pre-call → re-claim, post-call → MAYBE → resolver (§11). State is in tables; restart is stateless | T1 |
| I-2 | Stale worker resumes after GC pause / lease expiry | — | CAS version fencing: its writes hit row count 0 (§11) | T0 |
| I-3 | Database down | Everything halts; dead-gauge rule — metric ABSENCE alerts, not reads-as-green (§15) | Kafka unacked (at-least-once); on DB recovery consumers/scanners resume; ordering guards + inbox absorb the replay | T1 after restore |
| I-4 | Database RESTORED to an earlier point (RPO > 0) | Post-restore state internally consistent but WRONG vs the engine | Deterministic keys (§5.1) make re-creations collide at the engine (DUPLICATE_REQUEST → ambiguous-outcome handling) instead of double-paying. §5.2 runbook (freeze → restore → replay → sweep → key-space enumeration → drift check → unfreeze) is POST-MVP (PO decision); until built: major incident + manual engine-side reconciliation | T3/T4 |
| I-5 | Hazelcast outage | Freeze page (§15) | Posting halts fail-safe (PO signed off); everything else continues (§16.1) | T2 |
| I-6 | Kafka broker retention shortened by another team | Scheduled retention check alerts (§16.2) | Fix retention before the replay-window guarantee is violated | T2/T3 |
| I-7 | Consumer-group reset / redeployment replays history | Duplicate-skip counters spike — dashboards must show this as HEALTHY (§15 practices) | auto-offset-reset=earliest is deliberate; inbox + evidence + ordering guards make replay a no-op | T1 |
| I-8 | Bad config set (ordering violated, e.g. trust-age ≥ escalation age) | Loader REJECTS the set at startup (§16.6) | Fix config; nothing degraded silently | T0 |
| I-9 | Deployment: two app versions concurrent | — | Expand/contract migrations; defensive enum reads (UNKNOWN sentinel); claim semantics version-compatible across one release (§16.5) | T0/T1 |
| I-10 | Clock skew between nodes | — | Due-time comparisons use DATABASE time only; cutoffs tz-aware from the calendar (§16.4) | T0 |
| I-11 | Log platform outage | Ops observability degraded | Money processing unaffected (logs are not in the money path); §5.2 step-5b falls back to the K-heuristic (recorded as heuristic) | T2 |
| I-12 | Card read surface down | Card shows "unavailable" — never stale-as-authoritative (§12) | At launch the card is ops' only window: alerts (§15) + direct DB reads remain; read-path bulkhead keeps it isolated from posting (§16.1) | T2/T3 |
| I-13 | Hot scope / upstream floods one obligation | Per-obligation request-count ticket (§15); ~10 tx/s sanity line (§16.5) | Capacity review; the obligation lock serializes so correctness holds while throughput degrades | T2/T3 |
| I-14 | Hung external call holds Kafka partitions | Rebalance storms averted by per-dependency timeouts (§16.1) | Bounded timeouts everywhere; breaker isolates the dependency | T0/T1 |

**Deep-dive trace (I-4, the restore):** the one scenario where the database
itself lies. Example from §5.2: pre-restore, an auto-cancel LOST its race and
the payment executed; the replay (posting frozen) lets the auto-cancel WIN —
the DB now says CANCELLED for money that moved. The §5.2 pre-unfreeze query
sweep + per-obligation key-space enumeration finds the executed payment under
a key the DB no longer owns → CRITICAL evidence-for-terminal path → ops
reconciles BEFORE posting resumes. Interim (pre-runbook): same logic run as a
major-incident procedure with the engine's records as truth.

------

## 8. Human & ops-error failures (H)

| # | What breaks | How we notice | Recovery | Tier |
|---|-------------|---------------|----------|------|
| H-1 | Fat-finger direct DB write (illegal state) | CHECK constraints + L1-freeze/release-guard TRIGGERS reject it loudly (§10.3) | Nothing to recover — the write fails | T0 |
| H-2 | Ops releases a reservation whose money may have moved | §10.1 release guard: terminal-negative FORBIDDEN while MAYBE/SUBMITTED without engine negative; trigger backstop | Sanctioned exits only: §9.3 procedure (platform-verified, dual-control) | T0 → T3 |
| H-3 | Wrong outcome fed to APPLY-PLATFORM-VERIFIED-OUTCOME | Dual control (two authenticated approvers), mandatory ticket ref, refuses CLAIMED/terminal/amount-mismatch rows, every use raises a §15 alert | Residual risk accepted: the procedure demands platform-records verification; audit trail = ticket + §14 log (trigger_source=OPS_PLATFORM_VERIFIED) | T3 |
| H-4 | Ops retries a BLOCKED row that must not repost | repost_permitted checked at BOTH ends — un-parking writer AND posting claim (§7.0); divergent_payload / terminal never overridable | The gate, not the operator, is the safety | T0 |
| H-5 | Ops clears a marker prematurely (2nd+ provider reject) | Clear is ops-ONLY by design from the 2nd reject; §19.3 records operator/reason/ticket; 4-eyes for money movement | Audit + alert on reject-count increment | T3 |
| H-6 | Deliberate freeze forgotten | Freeze-effective-without-acknowledged-ticket PAGE (§15) | Flip back (role-controlled) | T3 |
| H-7 | Manual action with no ticket trail | §20-8: mandatory external ticket reference — the ONLY record surviving a DB restore | Procedure refuses/audits; process rule | T0/T3 |

------

## 9. Business & client-level scenarios (B)

| # | What breaks | How we notice | Recovery | Tier |
|---|-------------|---------------|----------|------|
| B-1 | Client claims "my payment is lost" | Card lookup by business_id — one entry per payment, NOT_STARTED = no message yet (§12); correlation_id greps the whole story (§14) | Triage path: card → request state/labels (§10.4) → engine status query by key → platform audit trail. Every state in this document has a named owner | T3 investigative |
| B-2 | Business cancels a trade after the payment step started | Trade still pays (BA-2 — no cancellation signal exists) | Platform-side recall (§19.2 family); PO-5 tracks only the DISPLAY question | Settled (§1.1) / T4 |
| B-3 | Client double-funded / duplicate payment suspicion | Deterministic identity chain (§5): one key per logical attempt, engine dedup proven by §18-1 sandbox gate | Evidence available by construction: key + UETR + platform records | T4 |
| B-4 | Business wants payment despite engine reject (transient beneficiary-side condition) | provider_rejected marker + alert | Today: one newer upstream message (§6.8) or wait; FUTURE §19.3 ops retry-after-reject (4-eyes) pending PO-7 | T3/T4 (partly FUTURE) |
| B-5 | Funds returned to our account (post-settlement return) | CRITICAL log-only event (§8) | §19.2 future workstream — detection, RETURNED outcome, confirmed decrement all deliberately absent today; platform owns the truth | T4 |
| B-6 | Cutoff passes while payment unresolved | CUTOFF_EXPIRED / escalation sized to land BEFORE cutoff (§9.3, §16.6 ordering) | Ops decision next window; PO-4 tracks the policy | T3 |

------

## 10. Verdict — gaps found by this walkthrough

Everything above resolves to a designed path except the items below. Two are
NEW (added to `requirment-v4.md` §18 this round); the rest were already
tracked — listed here so the walkthrough is a complete review artifact.

**NEW — folded this round:**

```text
GAP-1  Scope-key provenance not in writing (upstream ask 6, §18 — NEW).
       The whole intake/retry story rests on §6.0's premise that
       payment_type / debit_account / currency arrive IN the message as
       stable identifiers. If any of them were actually derived via a
       failable lookup BEFORE intake, the scope key and the §5.1
       deterministic key would inherit that fragility (the PO's
       "account API" concern — valid in exactly that world). One
       written sentence from upstream closes it.

GAP-2  The never-emitted message is invisible (upstream ask 7, §18 — NEW).
       Every failure AFTER the message arrives is detected locally.
       A payment whose upstream message was never produced/delivered
       has no local row, no anchor, no alert — the single lost-payment
       class with no detector on our side. Ask upstream to confirm
       emit-failure monitoring (or provide a count-recon signal we can
       check daily).

GAP-3  Tie application had no operation (§20-10 + O12 — NEW, found by
       the "minimal ops surface" review of this walkthrough).
       §6.7 said tied-but-differing snapshots go to "manual
       application", but no operation existed in §20 or the
       ops-console catalog, and nothing guaranteed the dropped
       snapshot's payload survived to be applied (the message is
       acked, no parked-message store exists, a resend ties
       forever). Folded: §6.7 executability requirement (the
       tie-conflict record carries the canonicalized payload),
       §20 item 10 (trade-level 4-eyes operation), and
       ops-console-proposal.md O12 + §3.1 coverage matrix.
```

**Already tracked (no change needed — verified still open and correctly owned):**

```text
- §18 BLOCKING 0–3: snapshot residue; collision-contract sandbox proof;
  cutoff calendar; MAYBE terminal exit (procedure + drill).
- PO-9 / TL-16: absence semantics + snapshot watermark rule (U-15/U-16).
- TL-5 / Q-10: query lookback ≥ max row lifetime (R-3).
- TL-10 / Q-12: platform formal reject — the cleanest parked-MAYBE exit.
- PO-7 / §19.3: ops retry-after-provider-reject (B-4) — FUTURE.
- §19.2: returned-funds visibility (B-5, C-9) — FUTURE workstream.
- §20 console: the entire T3 tier gets an API/UI surface post-MVP;
  at MVP it is the §20 interim procedure set (supersede/close, retry,
  reject, annotate, tie-apply — now enumerated in §20 and delivered
  by playbook RG-05 + OP-04, gated by checklist Q29) + the §9.3
  stored procedure (the one REQUIRED-at-MVP piece) + the four ops
  queue views + role-controlled toggles.
- §5.2 DR runbook: post-MVP by PO decision; interim = major incident
  (I-4). Deterministic keys keep the restore recoverable regardless.
```

**Accepted limitations (deliberate, PO-visible, re-verified):**

```text
- DLT blind spot when even the Kafka key is unreadable (U-5).
- Unmatched feed events are log-and-ack, recovery at sweep latency (C-2).
- A grid outage halts ALL posting — fail-blocked philosophy (P-14).
- Post-latch overpay scopes are permanently manual (M-2/M-4).
- Upstream data that is wrong-but-valid pays as instructed; recovery is
  a corrected message or platform recall (U-17, E-5) — garbage-in is a
  contract fact, not a system defect.
```
