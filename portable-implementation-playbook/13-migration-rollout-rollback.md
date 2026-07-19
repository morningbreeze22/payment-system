> **Purpose:** Migration / rollout / rollback plan M.1-M.10: schema sequencing, flags, shadow, enablement order, backfill, rollback constraints, point of no return (original Section M).
> **When to use this file:** When planning and executing Phase P14; GO-01 localizes it.
> **Depends on:** requirment-v4.md section 16.5; 04-dependency-graph.md.
> **Used by:** GO-01..GO-05; 17-go-live-checklist.md Q23.
> **Safe to transfer:** yes
> **Contains local code names:** no

# M. Migration / rollout / rollback plan

Source-code-agnostic; GO-01 localizes it. Governing rule: §16.5
expand/contract — two app versions run concurrently during rollout;
new columns nullable-with-default first; drops only after the old
version is gone; claim semantics version-compatible across one
release boundary.

### M.1 Schema migration sequencing

```text
1. Additive columns (S-02, S-03) + inbox table (S-04) — old version
   unaffected (nullable/defaulted).
2. UNIQUEs + I6 (S-05 part) — verify no legacy violations first
   (S-02's duplicate-scope check; S-08 ordering for constraints that
   need backfill).
3. Backfill dimensions + anchors (S-08), quiet window, idempotent.
4. CHECK constraints NOVALIDATE → VALIDATE after backfill (S-05).
5. Triggers (S-06) — freeze + release guard; app code that legally
   writes terminal-negatives must already set the evidence flag
   (deploy order: flag-setting code BEFORE trigger activation, or
   activate triggers in the same release with the code — record
   locally which).
6. Scan indexes (S-07).
7. CONTRACT steps (drop/NOT NULL tightening of legacy columns):
   deferred until after full rollout + soak; a separate,
   individually-approved migration set. The legacy status column
   is dropped LAST, after ST-05 shows zero rule sites and GO-02's
   soak is clean (round 13: clean = zero UNEXPLAINED disagreements;
   CANCELLED rows are EXPECTED, classified deltas — legacy display
   has no such label). ui_step_status tightens to NOT NULL here too
   (round 14): only after the old writer is gone AND the M.3
   catch-up derivation pass reports ZERO NULL rows;
   active_exception_* fields stay nullable.
```

### M.1a Reader-first compatibility ladder (round 14 — conditional)

```text
The CANCELLED stored value must never reach a reader that cannot
tolerate it. A DB CHECK change NEVER solves an application-reader
incompatibility. Ladder, decided by DISCOVERY EVIDENCE:
1. Discovery (D-phase) proves: does the CURRENTLY DEPLOYED version
   read ui_step_status at all, and are unknown enum values handled
   defensively (§16.5)?
2. Does NOT read the column → record the proof; the compatibility
   requirement is N/A for that reader path.
3. Reads it NON-defensively (e.g. two-value enum + Enum.valueOf) →
   FIRST ship a compatibility release that reads/serializes
   CANCELLED (or maps unknown values to the sanctioned sentinel)
   and does NOT write the new value.
4. Verify the ENTIRE reader fleet is upgraded; only THEN enable
   the round-11 derivation that WRITES CANCELLED.
5. Soak + rollback rehearsal precede the M.1-7 contract step.
6. Reader-fleet upgrade and writer-fleet drain are SEPARATE
   evidence items (round 15): a compatibility release may READ
   CANCELLED while still WRITING legacy/NULL status; the M.3
   fenced cutover requires the WRITER drain, not just the reader
   upgrade.
```

### M.2 Feature flag strategy

```text
Structural changes (dual-write, CAS discipline, derivation) ship as
code — no flag (they are behavior-preserving against the legacy
representation and proven by GO-02's shadow comparison).
Config-flagged enablement (default OFF):
  F0 NEW-FLOW TRAFFIC GATE (round 19 — the activation boundary the
     RUN-2 proof anchors to): whether upstream traffic reaches this
     flow AT ALL (product launch / routing). Default OFF. F0 gates
     TRAFFIC ONLY and NEVER bypasses admission — once ON, every
     message enters through the always-on §6.1 admission gate. If
     routing is controlled OUTSIDE this application (upstream
     starts producing, topic subscription, gateway), GO-01's plan
     NAMES that external action and its owner as F0 — never an
     invented local flag.
  F1 new-machinery scanners (retry scanner in new claim/policy mode)
  F2 resolver sweep (query-based recovery)
  F3 escalation scanner
  F4 §9.2 auto-downgrade   ← LAST; requires P8 PASS + TL-5 trust age
  F5 posting-freeze enforcement mode (check-and-block vs check-and-log
     during early soak — must reach BLOCK before F1..F4 ship traffic)
Kill behavior: each flag OFF returns that mechanism to inert; rows
remain valid states (parked/waiting), recoverable when re-enabled.

F0 ACTIVATION WINDOW (round 19 — atomic, inside a change freeze):
  prevent all legacy/in-scope creation → verify the writer fence →
  execute the reviewed RUN-2 queries (file 26 T.1) → require ZERO →
  DBA/TL sign the result → enable F0 / execute the named external
  routing action → verify the FIRST admitted row carries watermark
  + storage pointer + digest AND the FIRST post-F0 payment_request
  row carries NON-NULL required_total_at_creation + NON-NULL
  request_seq (both filed in the evidence pack beside the signed
  F0 timestamp; NO-SAMPLE rule: no request in the window → the
  manifest records FIRST_REQUEST_CREATION_COLUMNS=PENDING_SAMPLE
  with owner (ops), a bounded SLA date, and the exact bounded
  created_at >= F0 query + checksum; a durable ticket is opened
  at closure; the later PASS is an append-only manifest update —
  file 25 V.2 item 3 carries the full lifecycle; 6cb3005 L2 /
  7cc9f49 L2). Any nonzero count → STOP, NO-GO,
  architecture review (never proceed, never waive).
```

### M.3 Dual-read / shadow / dry-run

```text
GO-02: dual-write soak with tuple-vs-legacy comparison (report must
be CLEAN — zero UNEXPLAINED disagreements, each dispositioned;
EXPECTED CANCELLED semantic deltas are classified, not fixed —
round 13).
Catch-up + FENCED CUTOVER (rounds 14-15): during dual-run, OLD
writers may leave ui_step_status NULL. A one-time batch cannot
hold an invariant while a writer capable of violating it stays
live, so the card read switch requires this ORDER:
  1. WRITER fleet drained/upgraded — no binary that writes
     legacy/NULL status remains — AND old writer versions FENCED
     from reconnecting (deployment control).
  2. THEN the final catch-up pass re-runs the shared §4
     derivation over all obligations (S-08 machinery, idempotent).
  3. Verify ZERO NULL ui_step_status rows.
  4. THEN switch the card reader.
  5. The M.1-7 NOT NULL contract migration follows.
Cutover assertion (GO-02/GO-03): a fenced old-writer version
attempting to reconnect is REJECTED. READER-fleet upgrade and
WRITER-fleet drain are SEPARATE evidence items — the M.1a
compatibility release may READ CANCELLED safely while still
WRITING legacy/NULL status.
Derivation shadow: derived ui_step_status/exception vs legacy display
behavior compared on real traffic before the card reads switch.
Resolver dry-run mode (recommended, cheap): sweep + query + LOG the
would-be action without writing, one soak window, before F2 flips to
write mode — validates scope + mapping against production answers.
```

### M.4 Safe enablement order (GO-03 executes — round-20 THREE SEGMENTS)

```text
ENTRY CONDITION (round 20): the GO-04 PRE-CUTOVER CONDITIONAL GO is
recorded — every gate PASS, Q5b alone PENDING-CUTOVER. GO-04 is a
PREREQUISITE of this sequence, not a step inside it.

SEGMENT 1 — PRE-TRAFFIC (F0 OFF; real traffic CANNOT arrive):
1. Schema through VALIDATE + triggers live (M.1) — raw-SQL guard
   demonstrated in the environment.
2. Dual-write code + CAS discipline + derivation live (structural).
3. GO-02 shadow soak → clean.
4. Card/display readers on derived labels (ST-04) — display-only
   risk; reader compatibility + M.3 catch-up complete.
5. F5 freeze enforcement → BLOCK mode AND tested; freeze-effective
   page live.
6. Observability pack (OB-03..07) + runbook links + on-call alert
   routing live.
7. OP-01 operation deployed + OP-03 drill done.

SEGMENT 2 — ATOMIC ACTIVATION (the M.2 F0 window):
8. Prevent all legacy/in-scope creation → verify the writer fence →
   execute the reviewed RUN-2 queries → require ZERO → convert Q5b
   to PASS + DBA/TL sign → enable F0 (or the named external routing
   action) → verify the FIRST admitted row carries watermark +
   storage pointer + digest AND the FIRST post-F0 payment_request
   row carries NON-NULL required_total_at_creation + NON-NULL
   request_seq — both filed in the evidence pack beside the signed
   F0 timestamp (NO-SAMPLE rule: no request created in the window
   → manifest FIRST_REQUEST_CREATION_COLUMNS=PENDING_SAMPLE with
   owner ops + SLA + query/checksum + durable ticket; append-only
   PASS on the first real sample — full lifecycle in file 25
   V.2 item 3; 6cb3005 L2 / 7cc9f49 L2). Nonzero or missing signature → ABORT
   the window (F0 stays OFF), NO-GO, architecture review.

SEGMENT 3 — POST-TRAFFIC (traffic flowing; each stage soaked):
9. F1 retry scanner (new mode). Soak: retry outcomes sane, no
   stacked-retry double-fires.
10. F2 resolver (dry-run → write). Soak: query volume within budget,
    outcomes applied correctly.
11. F3 escalation. Soak: no false escalation storms.
12. F4 auto-downgrade — LAST; ONLY after CT-02..05 PASS on file,
    TL-5-derived trust age configured, observed-lag watchdog live.
13. Post-enable verification + evidence closure (GO-03 card).
```

### M.5 Existing rows and backfill

```text
S-08 is the backfill: legacy → tuple per the reviewed map; ambiguous
wire-capable states → MAYBE_SUBMITTED (fail toward the resolver);
anchors set defensibly; anomalies (status contradicts money) listed
for human disposition, never auto-resolved. Rows created by the OLD
version during dual-run are covered by the same map (columns nullable
until contract). Legacy in-flight rows keep their persisted
idempotency keys forever (K-02 rule).
```

### M.6 Rollback constraints

```text
- Any stage before F1: rollback = deploy old version. Expand/contract
  guarantees schema compatibility (S-09 proof).
- F1–F3 enabled: rollback = flags OFF. State written so far is valid
  and stands; rows in new-only states (e.g. BLOCKED(ESCALATED),
  parked AMENDMENT_PARKED) wait — they are wait-then-decide states by
  design; ops handles per runbook if the pause is long.
- POINT OF NO RETURN: once the new machinery writes money-affecting
  outcomes (terminal-negatives with releases, settlements with
  confirms) in production, there is NO data un-migration: rollback is
  flags OFF + the recorded state stands; recovery of any disputed row
  goes through the platform-verified paths (§9.3), never through
  reverse-migration. Schema stays; drops were deferred anyway (M.1.7).
- Never roll back the schema VALIDATE/trigger steps while the new
  code runs — the code assumes the backstops exist.
- TRADE-ADMISSION (round 10 — §2.4 GREENFIELD FACT): this flow is
  a NEW feature; no prior application version consumes its
  snapshots and no pre-existing trades exist. Admission enforcement
  is ON from day one — no bootstrap, no drain step, no §7.0
  assembly flag, no second point of no return. (The round-6..9
  gate/ladder lives in git history at 9a53c75.)
```

### M.7 Data compatibility

```text
Old version ignores new columns (nullable); new version tolerates
legacy-only rows via the S-08 map + defensive enum reads (UNKNOWN
sentinel — §16.5); enum evolution (adding a blocked_reason value)
follows the §16.5 NOVALIDATE swap sequence; the four dimension enums
are CLOSED — extending one is a design change with a review round,
not a migration.
```

### M.8 Protecting existing payment behavior

```text
- Business logic components ([Payment Enrichment Component], payment
  construction, decision rules) are READ-ONLY throughout (Section B).
- D-11's baseline green bar re-runs at every stage checkpoint;
  failures in preserved tests = stop-the-line.
- ST-05 re-keying is behavior-preserving by test (verdict-matrix pins).
- The §16.5 contract tests guard the three external contracts.
```

### M.9 Validating before enabling automated recovery

```text
F2 (resolver) precondition: CA-3 verified (CT-06); evidence helper
race tests green (T-19); dry-run soak clean.
F4 (auto-downgrade) precondition: §18-1 PASS (T-11/T-12), TL-5 trust
age (Q-11) configured, repost_permitted both-ends tests green (T-23),
observed-lag watchdog live.
Until F4, NOT_FOUND-after-trust-age rows wait for ops-triggered
downgrade decisions (the §9.2 ops action) — safe, more manual.
```

### M.10 What cannot be rolled back after money-affecting writes begin

```text
Released reservations, confirmed amounts, terminal outcomes, and their
downstream successor creations are FACTS once written — protected by
write-once outcome + freeze trigger by design. Any correction is a
forward operation through sanctioned paths (§9.3 operation at MVP;
§19.2-family platform reconciliation). Plan reviews at GO-05 must
name this line explicitly per environment.
```

### M.11 Migration SQL review checklist (round 16 — apply when CA-4/Flyway SQL exists)

```text
[ ] immutable, ordered migrations with checksums; fix-forward only
[ ] explicit Oracle types, precision/scale, UTC timestamp behavior,
    identifier lengths
[ ] preflight queries: duplicate scopes, duplicate active requests,
    duplicate keys/UETRs, illegal legacy tuples, NULL status residue
[ ] function-based index expressions EXACTLY match scanner SQL
[ ] NOVALIDATE → backfill → VALIDATE with captured USER_CONSTRAINTS
    evidence
[ ] I6/unique-index creation behavior verified on existing data
[ ] trigger compilation/status; session-context lifecycle; pool
    non-leakage; least-privilege grants
[ ] DDL lock duration, online/index-build strategy, redo/undo/temp
    usage, production-sized runtime estimates
[ ] idempotent batch backfill with restart checkpoints and bounded
    transactions
[ ] writer fence, final catch-up, zero-NULL query, reader switch,
    NOT-NULL cutover — full transcript retained (M.3)
[ ] schema diff vs CA-4 after deployment AND after rollback
    rehearsal
Test on a clean schema AND a production-shaped clone — a tiny
synthetic schema cannot produce DDL-lock or index-build evidence.
```

