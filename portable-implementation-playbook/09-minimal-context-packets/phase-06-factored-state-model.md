> **Purpose:** Minimal context packets ST-01..ST-11 — paste-alone briefs for a small-context local agent (original Section I, phase P6).
> **When to use this file:** Paired with the matching task-card file 08-task-cards/06-factored-state-model.md — one packet per task, used as the working brief.
> **Depends on:** 09-minimal-context-packets/README.md; the matching task card; the requirement sections each packet cites; 07-placeholder-glossary.md.
> **Used by:** The local coding agent executing phase P6.
> **Safe to transfer:** yes
> **Contains local code names:** no

# Minimal Context Packets — Phase P6

```text
[ST-01] Dual-write dimensions
Read: §2.2 §10.4 (bridge map) §16.5. Invariant: every writer produces a consistent (legacy, tuple) pair from ONE shared mapping helper.
Placeholders: [Request Status Persistence Layer]. Mappings: D-04 writer inventory COMPLETE.
Objective: each status writer also writes the tuple per the reviewed S-08 map; no reader/WHERE changes yet.
Tests: per-writer pair-consistency. Stop: all writers dual-write; baseline green.
```

```text
[ST-02] CAS discipline
Read: §11 (rules) §10.3 (L1 via CAS) §10.1 (mirror). Invariant: WHERE carries full dimension precondition + outcome IS NULL; row count is the verdict; rowCount 0 is HANDLED; no ORM dirty-checking.
Placeholders: [Request Status Persistence Layer]. Mappings: writer inventory; ORM-save sites.
Objective: convert every dimension writer to conditional UPDATE + row-count branch; dimension changes take the obligation lock first + re-derive hook.
Tests: row-count-0 on stale precondition; mirror rule (late accepted vs EXECUTED → 0 rows); no-dirty-checking. Stop: merged.
```

```text
[ST-03] Legality conformance tests
Read: §10.2 §10.3 §10.5 (seed). Invariant: every §10.5 row has a test; illegal transitions die at code AND (where CHECKable) at DB.
Placeholders: [Integration Test Suite] [Request Status Persistence Layer]. Mappings: Oracle lane.
Objective: table-driven suite: all legal flow rows + illegal cases per L1–L8; assert which layer refuses.
Tests: the suite. Stop: green; coverage recorded (pending §9.2 case noted).
```

```text
[ST-04] Display labels + §12 all-payments table projection
Read: §10.4 (mapping + strictness) §2.2 §12 (ALL-PAYMENTS TABLE projection block). Invariant: labels derived from dimensions, display-only, never parsed by consumers, never in machine-consumed payloads; the table projection is a pure read (obligation LEFT JOIN request) — no schema, no state, no frontend recomputation, no duplicates by construction.
Placeholders: [Request Status Persistence Layer] + display readers. Mappings: D-04 display-reader list.
Objective: implement the §10.4 mapping (view/expression); route dashboards/card label/log/ops reads to it; card returns dimensions + label. Implement the §12 table projection per its contract: row_type REQUEST (one per request) | OBLIGATION_ONLY (no-request placeholder: scope + required-or-blank + "no request created" + derived-exception reason; request fields n/a); obligation context on every row; REQUEST rows pass through required_total_at_creation verbatim (§2.2 amount-series stamp — NULL renders "not captured (pre-F0)"; NEVER computed in the projection; one stamp per payment_request row, NOT per POST attempt; read = UI projection only, never money/workflow logic); terminal rows visible; removed scopes show CANCELLED context.
Tests: mapping per label row; NEEDS_REVIEW includes blocked_reason; T-31 TABLE cases (placeholder; placeholder→request no-dup; 100+20 = two rows with stamps 100/120; NULL stamp = "not captured (pre-F0)"; mixed active/terminal; CANCELLED context; reappearance). Stop: merged.
```

```text
[ST-05] Migrate rule sites off legacy status (template — apply per site)
Read: §2.2 (dimension meanings) §10.1 (blocked_reason rule) §4.5; the § governing the specific rule. Invariant: behavior-preserving re-keying; each rule means ONE dimension; blocked_reason is never a rule input.
Placeholders: per site. Mappings: rule-site inventory with per-site dimension classification.
Objective: per site: pick the dimension the rule MEANS (money→submission_state, position→stage, claimability→stage_state, finality→outcome); rewrite; irreducibly-compound → UNCLEAR, report.
Tests: per site, before/after verdict-matrix pin. Stop: inventory empty or dispositioned.
```

```text
[ST-06] Outcome normalization
Read: §10.2 (outcome block) §2.2 (anchor clears) §10.3 (L1 split). Invariant: one canonical terminal shape; submission_state kept; maybe_since/escalated_at cleared.
Placeholders: [Request Status Persistence Layer]. Mappings: outcome-writer list.
Objective: single helper used by every outcome write: outcome + stage_state=READY + NULL claim/retry/blocked + clear maybe_since/escalated_at.
Tests: from CLAIMED/RETRY_WAIT/BLOCKED → canonical shape; L6 holds through terminal-from-CLAIMED. Stop: merged.
```

```text
[ST-07] Episode anchors
Read: §2.2 (maybe_since/submitted_at/escalated_at/last_post_attempt_at) §15 (clock discipline). Invariant: set-once per episode; churn never refreshes; ages NEVER read state_changed_at.
Placeholders: [Request Status Persistence Layer]. Mappings: submission-state writers.
Objective: maybe_since set on first MAYBE entry, cleared on leave + outcome; submitted_at on SUBMITTED; escalated_at contract (written by RC-08, cleared with maybe_since).
Tests: churn preserves maybe_since; re-entry = new episode; outcome clears. Stop: merged.
```

```text
[ST-08] CAS log line
Read: §14 (whole incl. DELIVERY CONTRACT) §16.3 (masking). Invariant: emitted only on rowCount==1 AND published only from an afterCommit callback (buffer in-tx; rollback discards — NO phantom ever; crash-window gap accepted, at-most-once, no retry; publication failure never fails the transition; claim ordering: commit → publish best-effort → provider call); carries key+seq+correlation+tuple before→after+label+trigger fields; no account data, no instruction content.
Placeholders: [Request Status Persistence Layer] [Metrics / Alerting Layer]. Mappings: logging conventions.
Objective: one emission point in the CAS helper; posting-claim line adds hash + attempt count + post_attempt_seq (K-05 convention); EVERY ATTEMPT-class line (posting claim, response resolution, lease-expiry resolution) carries post_attempt_seq + attempt_event_type — the STABLE §14.1 join keys (attempt_count resets on §9.2 downgrade; never the join key). attempt_event_type = EXACT field name, values BYTE-EQUAL to journal event_type: claim = 'ATTEMPT_STARTED', both resolutions = 'ATTEMPT_RESOLVED' (local vocabularies FORBIDDEN; not trigger_event_id).
Tests: log-capture per transition family; ATTEMPT-class lines assert post_attempt_seq + attempt_event_type exact tokens; delivery contract: rollback → NO line; crash-after-commit → gap tolerated, no phantom; publication failure → transition unaffected; masking. Stop: merged.
```

```text
[ST-09] Claims as leases
Read: §11 (claims + claim protocol) §2.2 L6; mechanics M4/M5. Invariant: claim = CAS to CLAIMED + claimed_by + claim_expires_at; candidate selection takes NO locks; per-item tx locks the OBLIGATION first, then the claim CAS (rowCount 0 = lost race); DB time; no re-derivation on claim/unclaim.
Placeholders: [Retry Resolver Job] + stage workers, [Request Status Persistence Layer]. Mappings: D-08 claim reality.
Objective: standard claim/complete CASs with L6 both directions; lease durations from config.
Tests: double-claim race; stale-worker fence; L6. Stop: merged.
```

```text
[ST-10] Lease-expiry recovery
Read: §11 (expiry + rationale) §10.2. Invariant: expired POST claim → CONFIRM·READY·MAYBE + maybe_since; NEVER re-claimed for posting; no "provably not launched" carve-out.
Placeholders: [Retry Resolver Job] (or expiry sweep). Mappings: ST-09 shape.
Objective: expiry handling: ENRICH → READY in place; POST → CONFIRM·READY·MAYBE (+maybe_since), claim fields cleared. §14.1 rider: POST-expiry CAS inserts ATTEMPT_RESOLVED outcome LEASE_EXPIRED_MAYBE, same transaction, rowCount==1 only; switch-gated; matching §14 log line: attempt_event_type = 'ATTEMPT_RESOLVED' (exact field name, byte-equal, NOT 'LEASE_EXPIRED' — b760786 M2). Canonical failure rule: statement-local failures caught + alerted AFTER host commit, recovery proceeds; fatal = ordinary infra failure; guarantee = no incorrect payment outcome.
Tests: both paths; slow-worker fence; expired POST row structurally unclaimable for posting; T-38 (one RESOLVED per attempt under the race). Stop: merged.
```

```text
[ST-11] Graceful shutdown
Read: §11 (shutdown) §16.1 (drain). Invariant: listeners → scanners → drain POSTs (bounded) → release ENRICH claims only; POST claims never released.
Placeholders: consumers, [Retry Resolver Job], [Provider POST Client]. Mappings: lifecycle wiring.
Objective: ordered shutdown per §11's four steps.
Tests: shutdown idle / mid-ENRICH / mid-POST. Stop: merged.
```

