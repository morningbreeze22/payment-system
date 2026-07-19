> **Purpose:** Observability, reconciliation, and runbook plan: metric/alert inventory, 13 runbook seeds, reconciliation rules (original Section N; seeds companion artifact CA-8).
> **When to use this file:** When implementing OB-01..OB-07 and authoring CA-8 runbook stubs; during incident response as the runbook seed reference.
> **Depends on:** requirment-v4.md sections 15, 16.1, 16.2, 19.2; 12-companion-artifacts.md (CA-8).
> **Used by:** OB tasks; CA-8; on-call operators.
> **Safe to transfer:** yes
> **Contains local code names:** no

# N. Observability, reconciliation, and runbook plan

Implemented by OB-03..07; runbook stubs live in CA-8 — the blocks
below are their seeds. Global rules (§15): ages on episode anchors
only; scopes on dimension columns; no rule on labels/blocked_reason;
metric absence = bad; every alert links a runbook; correlation_id
greps the whole story; rollup groups consequence alerts under a
root-cause incident.

### N.1 Metric/alert inventory (§15 + instruction-required set)

```text
- duplicate idempotency-key POST attempts (DUPLICATE_REQUEST answers,
  §7.2)                                → metric; alert on volume
- divergent-payload attempts (collision responses; expected vs
  anomalous split, §7.2)               → metric; anomalous = CRITICAL
- MAYBE_SUBMITTED count + age (maybe_since; tier-1 age threshold,
  tier-2 re-page, §9.3)                → alert / re-page
- rows approaching provider query-lookback expiry (created/attempt age
  vs TL-5 lookback)                    → alert (act before
                                          NOT_FOUND becomes
                                          unfalsifiable, §9.3)
- reservations held, by state and age (stuck-reservation, §3/§15)
                                       → alert
- apply-platform-verified-outcome usage (§9.3)  → alert EVERY use
- provider-side payments per key vs local EXECUTED count — CONDITIONAL:
  requires an engine-side report/API (MUST_VERIFY_LOCALLY); if
  unavailable, the drift scanner + terminal-evidence tripwire are the
  MVP coverage                         → CRITICAL on divergence
- retry scanner outcomes (per class: retried/exhausted)
                                       → metric; exhaustion spikes alert
- resolver failure reasons (INDETERMINATE rates, query errors)
                                       → metric; alert on sustained
- status-query lag + NOT_FOUND-after-trust-age frequency (§18/TL-15
  production measurement)              → metric + observed-lag watchdog
- stale-message volume (§6.7)          → alert on volume
- stale-marker-write volume (§6.9)     → alert on volume
- unmatched feed events (§8)           → metric; alert on volume
- drift scanner mismatches (I1/I2, L9) → PAGE
- UI/card false-completion prevention: completion-predicate
  anomalies (COMPLETED with active request — should be impossible;
  presence = defect)                    → alert
- §14.1 journal: statement-local write failure → AUDIT-GAP alert,
  EMITTED AFTER the host commit (posting proceeds; FATAL failures
  surface as ordinary infra alerts instead; gap recoverable via
  the §14 line + the manual platform ask)       → alert
- §14.1 journal: unmatched ATTEMPT_STARTED older
  than one lease window                 → alert (crash evidence;
  planned §14.1 switch transitions — freeze-gated — are recorded
  and EXCLUDED by triage)
- §6.6 accepted-window CANDIDATE diagnostic (revised per review
  2b697fb M1; scoped per review b1d91dc M1; delivery semantics +
  safe-execution envelope per review b760786 M1 — a candidate
  list for MANUAL review, NOT a classifier, NOT a required
  standing scan). DELIVERY SEMANTICS, stated exactly (corrected
  4098532 M1): shipping the query + its correctness test is a
  REQUIRED deliverable within OB-01, but its test failing does
  NOT block OB-01 completion — the failure becomes an EXPLICIT
  OPEN ITEM in the P12 handoff, deadline = before FIRST
  production use of this marker-triage runbook. INVOCATION is
  on-demand at operator discretion — never scheduled. NOTHING
  about it gates payment go-live — literally true only under the
  non-blocking rule above, because OB-01 → P12 → Q19 sit on the
  go-live path. For each obligation M with a
  LIVE validation_failed marker, flag sibling payment_request
  rows r (same business_id, different scope) where
  r.creating_ordering < M.validation_failed_ordering AND
  r.created_at > M.validation_failed_first_at → metric/log event
  LOWER_ORDER_SIBLING_REQUEST_AFTER_VALIDATION_MARKER_CANDIDATE
  (masked trade/scope ids + both orderings + both timestamps).
  Reference SQL (physical names resolve locally; the RELATIONAL
  PREDICATE is EXACT — review b760786 L1; marker liveness is the
  §2.1 definition verbatim):
    SELECT <masked ids, both orderings, both timestamps>
    FROM   payment_obligation M
    JOIN   payment_obligation s
           ON s.business_id = M.business_id
          AND s.id <> M.id      -- different scope, same trade
    JOIN   payment_request r
           ON r.payment_obligation_id = s.id
    WHERE  M.business_id = :business_id  -- REQUIRED bind (below)
      AND  M.validation_failed_ordering IS NOT NULL
      AND  (M.upstream_ordering IS NULL
            OR M.validation_failed_ordering >= M.upstream_ordering)
      AND  r.creating_ordering < M.validation_failed_ordering
      AND  r.created_at        > M.validation_failed_first_at
    ORDER  BY M.id, r.creating_ordering, r.id  -- deterministic,
                                    -- repeatable operator evidence
    FETCH  FIRST 500 ROWS ONLY                 -- hard cap
  SAFE-EXECUTION ENVELOPE (b760786 M1 — an on-demand incident
  query can still hurt a primary database):
  (1) the :business_id bind (or an explicit obligation-id list
      taken from the marker under triage) is REQUIRED — never run
      unbound across the estate;
  (2) the hard row limit AND the NAMED statement timeout are
      REQUIRED: accepted_window_diagnostic_timeout_ms — owner:
      ops, with DBA sign-off; default 10000 ms, maximum 30000 ms
      (values locally adjustable, the NAME and ownership are
      not); mechanism: JDBC Statement.setQueryTimeout (or the
      Spring JdbcTemplate queryTimeout equivalent) on the
      read-only connection — an application-layer-only timeout
      that never cancels the Oracle statement does NOT satisfy
      this (review 4098532 L1);
  (3) read-only execution only; prefer a replica/reporting
      connection where one exists — primary execution is allowed
      ONLY with the bind + limit + timeout all in place;
  (4) before FIRST production use an operator inspects one
      representative EXPLAIN plan AND proves the timeout actually
      cancels the real Oracle statement within the bound (one
      pre-production evidence step covering both — a one-time
      sanity look, not a CA-4 plan contract): the join reads
      historical/terminal request rows by design, the
      active-row-bounded indexes deliberately EXCLUDE those rows,
      and Oracle creates no index for a foreign key by itself.
  The CA-4 standing-scan index discipline does NOT apply
  (explicit exception, b1d91dc M1): no new index, no schedule, no
  plan contract — the envelope above, not an assumed "small
  driving set", is the protection (live markers can accumulate).
  HONEST COVERAGE: observes ONLY the post-marker chronology
  subset (the escape schedule); in the other ratified schedule B
  carries the LIVE marker itself and is visible directly (§6.6).
  Persisted state cannot distinguish the intentional window from
  a missed-marker crash (no marker-source discriminator exists,
  BY DECISION); candidates go to manual triage. Not detectable
  online (no trade-level watermark)
                                  → on-demand query + manual review
- post-F0 NULL request_seq (IDENTITY CONTRACT, 4dbdf2b M1 —
  OB-02): created_at >= the F0 activation timestamp AND
  request_seq IS NULL                     → ALERT + ticket (rogue
                                            or pre-fence writer;
                                            higher severity than
                                            the stamp ticket;
                                            never a gate)
- post-F0 NULL required_total_at_creation (data quality,
  aa4399c L1 — OB-02): created_at >= the F0 activation timestamp
  (signed manifest) AND stamp IS NULL     → LOW ticket (never a
                                            page, never a gate)
- plus the full §15 list wired in OB-03..05 (latch alerts, marker
  alerts, DLT, lag, heartbeats, stuck-state, freeze page, deadlocks,
  inbox growth, breaker, sweep overrun, tie/latched-amendment alerts)
```

### N.2 Runbook seeds (Trigger / Severity / Why / Action / Data / Escalation / Safe stop)

```text
DRIFT MISMATCH (I1/I2/L9)
Trigger: drift scanner pages after locked re-check.
Severity: PAGE (money-math integrity).
Why: counters vs row state disagree — row corruption or a money bug.
Action: freeze posting (Hazelcast toggle, ticketed); do NOT correct
  counters by hand (no sanctioned operation exists at MVP).
Data: scanner output (obligation ids, expected vs actual), §14 CAS
  log lines for the obligation's requests, recent deploys.
Escalate: tech lead + DBA immediately; incident channel.
Safe stop: root cause identified; correction plan through sanctioned
  paths; posting unfrozen only after drift re-scan is clean.
```

```text
PAYMENT_OUTCOME_UNKNOWN ESCALATED (MAYBE age tier-1/tier-2)
Trigger: BLOCKED(ESCALATED) write + CRITICAL alert (maybe_since age).
Severity: CRITICAL (money may be moving).
Why: a payment's fate is unknown and automation has not resolved it.
Action: per §9.3 offered actions ONLY: trigger resolve-via-query;
  after trust-age + repost_permitted → ops-triggered downgrade;
  dual-control stale-amount re-POST (only overridable term); request
  TL-10 platform rejection; LAST: apply-platform-verified-outcome
  after verifying the fate in platform records. NEVER manually
  release/cancel (release guard will refuse — that is correct).
Data: request id, key, uetr, maybe_since, last_post_attempt_at,
  divergent_payload_at, resolver's recent answers.
Escalate: tier-2 age → incident + payments duty manager.
Safe stop: outcome applied via evidence or the verified-outcome operation; scope
  re-derived; reservation confirmed or released.
```

```text
AMOUNT_MISMATCH (all-or-nothing violated)
Trigger: settlement/query amount ≠ request amount → BLOCKED park.
Severity: CRITICAL (defect evidence — ours or the engine's).
Why: contract says partial/fee-deducted settlement is impossible; a
  mismatch means a defect, and confirmed money must not move on it.
Action: verify the event against platform records; open a provider
  ticket; do not adjust local amounts (no operation exists).
Data: event payload, request amount, provider_reference, correlation.
Escalate: tech lead + provider support same day.
Safe stop: corrected event settles the row normally, or the dispute
  resolves platform-side and the row exits via the §9.3 operation.
```

```text
ENGINE_INCONSISTENCY
Trigger: SUBMITTED row NOT_FOUND after trust-age (§9.2), or anomalous
  same-key divergence (§7.2).
Severity: CRITICAL.
Why: the engine acknowledged something it now can't find, or
  disagrees about payload identity — engine-side integrity question.
Action: keep resolver querying (automatic — row stays in scope); pull
  platform-side records; provider ticket if it persists past one
  ingest-lag window.
Data: key, uetr, submitted_at/last_post_attempt_at, query answers
  timeline, divergence_expected + hashes (log line).
Escalate: provider support; tech lead if >1 row (systemic).
Safe stop: next successful query resolves it (lag-caused false park
  self-heals), or platform records settle it via the §9.3 path.
```

```text
FREEZE EFFECTIVE WITHOUT ACKNOWLEDGED TICKET
Trigger: posting freeze effective (toggle set OR Hazelcast
  unreachable) with no acknowledged freeze ticket (§16.1/§15).
Severity: PAGE (the freeze is silent by design — this is the only
  signal; every payment is pausing).
Why: either an unannounced deliberate freeze or grid failure.
Action: check the toggle's reason/operator/ticket payload; if infra:
  engage the grid owner; if deliberate-but-unticketed: get the
  operator to file the ticket; do NOT unfreeze without the operator.
Data: toggle payload, Hazelcast cluster health, freeze metric history.
Escalate: infra on-call for grid failure; payments lead otherwise.
Safe stop: freeze either acknowledged (ticketed) or lifted; retry
  no attempts were made while gated (no BLOCKED flood expected — verify).
```

```text
UNMATCHED FEED EVENTS VOLUME
Trigger: unmatched count over threshold (§8).
Severity: alert.
Why: routine singles are the feed-beats-commit race (sweep recovers);
  volume means a matching defect, a UETR persistence gap, or foreign
  traffic.
Action: sample events: known UETRs? (persistence gap → check U-01
  paths); foreign? (investigate in the platform, which owns the
  record); recovery of real outcomes is automatic via §9.
Data: sampled (event_id, UETR, status) log lines; U-01 test status.
Escalate: tech lead if a persistence gap is suspected.
Safe stop: volume back under threshold with explanation.
```

```text
KAFKA DLT DEPTH > 0
Trigger: any DLT message (either flow).
Severity: PAGE (§16.2 — poison money messages).
Why: a message failed deserialization/semantic validation; per-payment
  ordering for that key is now suspended pending replay.
Action: inspect the poison message; fix cause (schema drift? producer
  bug?); replay preserving original keys (§16.2 tool).
Data: DLT payload + headers, deserializer error, schema versions.
Escalate: upstream/provider team per flow; tech lead for schema drift.
Safe stop: DLT drained via keyed replay; consumer healthy.
```

```text
CONSUMER LAG OVER SLA
Trigger: lag page (either inbound flow, §15/§16.2).
Severity: PAGE over SLA.
Why: the DATABASE ITSELF is behind the world; card users may act on
  stale money state (§12).
Action: confirm the card's data-as-of indicator is showing (§12);
  diagnose consumer health (rebalance storm? poll interval? DB
  contention on the obligation lock?).
Data: lag per partition, poll metrics, DB session waits.
Escalate: infra + tech lead.
Safe stop: lag under SLA; indicator clears.
```

```text
OVERPAY LATCHED (incl. count/age rollup)
Trigger: overpay_blocked set (§13); bulk alert on count+oldest age.
Severity: alert (business hours).
Why: confirmed > required — the scope is frozen for automation
  FOREVER (one-way door); recovery is platform-side.
Action: annotate via ops_annotation (§20-4 display note); initiate
  recall/refund in the payment platform; a later amendment will NOT
  resume payment (AMENDMENT_ON_LATCHED_SCOPE alerts instead).
Data: obligation amounts, settling request, feed event trail.
Escalate: business ops for the refund workflow.
Safe stop: platform-side recovery underway; annotation records it.
```

```text
PROVIDER_REJECTED (and repeat-reject ≥2)
Trigger: marker set alert; count=2 alert (ops-only clearing begins).
Severity: alert.
Why: a requested payment is not happening; from the second reject the
  marker no longer clears via newer messages (anti-loop, §2.1).
Action: read the reject code (CA-1 meaning); coordinate the data fix
  upstream (one ordering-newer auto-attempt exists, §6.8) or accept;
  from count≥2: only the future §19.3 ops clear can re-enable
  auto-successors — until it exists, resolution is a PO/ops decision
  recorded in the ticket.
Data: reject code, creating_ordering vs upstream_ordering, count.
Escalate: business owner of the payment.
Safe stop: successor executed after a corrected message, or the scope
  consciously left rejected.
```

```text
AMENDMENT_TIE_CONFLICT
Trigger: §6.7 tie with differing payload.
Severity: alert (manual application needed).
Why: two genuine amendments share an ordering value — automation
  refuses to pick; a resend carries the same timestamp and would be
  rejected forever, so a human MUST apply the right one.
Action: obtain the correct current values from upstream; apply via
  the supported manual path with the release-guard-safe operation
  (ops-applied amendment is a message-equivalent write, not a raw
  UPDATE — if no tool exists yet, escalate to the tech lead; §20-1).
Data: both payloads, ordering value, current obligation amounts.
Escalate: upstream team + tech lead.
Safe stop: correct amount applied; §6.8 re-evaluated.
```

```text
SCANNER HEARTBEAT SILENT / SWEEP OVERRUN
Trigger: any scanner silent 3× its interval; resolver overruns
  repeatedly (§15).
Severity: PAGE (silent scanner = silent recovery machinery).
Why: MAYBE recovery, retries, escalation all ride on scanners.
Action: check scheduler health, DB connectivity, breaker states
  (breaker-gated quiet is EXPECTED during engine outages — verify
  against the rollup incident before treating as failure).
Data: last heartbeat, batch metrics, breaker state, lock waits.
Escalate: infra/tech lead.
Safe stop: heartbeat resumed; backlog draining within budget.
```

```text
EVIDENCE FOR TERMINAL REQUEST
Trigger: new event_id, zero-row CAS on a terminal row (§8).
Severity: CRITICAL.
Why: the engine asserts an outcome for a row we closed — possible
  replay divergence (§5.2 signature) or a serious mis-match.
Action: FREEZE posting for the affected scope's payment type if
  volume >1 (ticketed); reconcile the request against platform
  records; if the terminal state is wrong, the correction path is the
  §9.3 operation (never a raw un-freeze of the row).
Data: event payload, the row's terminal outcome + §14 history, key.
Escalate: tech lead immediately.
Safe stop: explained (true duplicate/foreign) or corrected via
  sanctioned path.
```

### N.3 Reconciliation

```text
Drift scan (OB-01) — every run recomputes I1/I2 (snapshot + locked
re-check) and verifies L9; PAGE on confirmed mismatch.
Terminal-evidence tripwire (OB-02) — the §5.2 replay-divergence
signature, live from day one.
Money-truth divergence policy (§19.2, decided): reality vs state
model disagreement = CRITICAL incident, reconciled in the payment
platform; local counters corrected only by the FUTURE
manual-adjustment operation — never ad hoc.
Retention-chain check (OB-05): inbox_retention > kafka_retention ≥
replay_window, verified on schedule against actual broker config.
Engine-side count comparison: conditional on an engine report/API
(N.1 note) — decide at kickoff with the provider answer.
```


## RB-F0 — F0 re-enable after rollback / incident (round 20)

The executable form of the ADMISSION-COVERAGE proof (file 26 T.1
lifecycle): used for ANY post-activation re-enable of the F0 traffic
gate — rollback recovery, incident restart, deployment reversal. The
initial-activation ZERO-POPULATION form is NEVER reused here.

```text
OWNER: OPS executes; DBA + TL approve. F0 stays OFF until PASS.
PRECONDITION: the triggering event is closed (rollback complete /
  incident stabilized); writer fence re-verified.
CHECKS (evidence retained in the go-live pack, manifest updated):
1. COVERAGE: every in-scope trade joins to a trade_snapshot_state
   row whose last_accepted_ordering, last_xml_storage_id, and
   last_payload_digest are ALL non-NULL; every in-scope obligation
   belongs to such a trade. Query text + results retained.
2. PROVENANCE: no in-scope row was created since the fence by a
   legacy/out-of-band writer. AUTHORITY (define BEFORE first use —
   review 5156f1f M3): the DB-side audit trail on the four §2
   tables (Oracle unified audit or the DBA-standard equivalent,
   enabled as part of GO-01) PLUS the deployment-fence record
   (which app versions were fenced, when). If NO DB-side audit
   source exists in this environment, SAY SO in the evidence and
   fall back to: the fence record + zero in-scope rows stamped
   since the fence by non-current writer versions (writer-version
   column / connection service tag) — and record the weaker basis.
   Reference queries (templates in the evidence pack): obligations
   lacking a complete trade admission row; trade rows with NULL
   watermark/pointer/digest; the exact in-scope population union.
   SCOPE (stated in the evidence): this proves LOCAL admission
   coverage — it cannot prove upstream emitted every trade; that
   remains the separate upstream emitted-vs-acked control (§18).
3. THRESHOLD: ZERO uncovered rows. No percentage passes; no
   sampling.
PASS: DBA + TL sign; manifest records the admission-coverage run
  (environment, queries, checksums, counts, signatures, date);
  re-enable F0 through the M.2 window semantics (fence rules
  still apply).
FAIL (any uncovered row): F0 stays OFF; classify each uncovered
  row; incident + architecture review (file 26 T.1 — restoring the
  retired bootstrap machinery, git 9a53c75, is on the table);
  re-run only after remediation, never "accept and monitor".
```
