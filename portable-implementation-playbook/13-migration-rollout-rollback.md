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
   soak confirms label parity.
```

### M.2 Feature flag strategy

```text
Structural changes (dual-write, CAS discipline, derivation) ship as
code — no flag (they are behavior-preserving against the legacy
representation and proven by GO-02's shadow comparison).
Config-flagged enablement (default OFF):
  F1 new-machinery scanners (retry scanner in new claim/policy mode)
  F2 resolver sweep (query-based recovery)
  F3 escalation scanner
  F4 §9.2 auto-downgrade   ← LAST; requires P8 PASS + TL-5 trust age
  F5 posting-freeze enforcement mode (check-and-block vs check-and-log
     during early soak — must reach BLOCK before F1..F4 ship traffic)
Kill behavior: each flag OFF returns that mechanism to inert; rows
remain valid states (parked/waiting), recoverable when re-enabled.
```

### M.3 Dual-read / shadow / dry-run

```text
GO-02: dual-write soak with tuple-vs-legacy comparison (report must
be CLEAN, every disagreement dispositioned).
Derivation shadow: derived ui_step_status/exception vs legacy display
behavior compared on real traffic before the card reads switch.
Resolver dry-run mode (recommended, cheap): sweep + query + LOG the
would-be action without writing, one soak window, before F2 flips to
write mode — validates scope + mapping against production answers.
```

### M.4 Safe enablement order (GO-03 executes)

```text
1. Schema through VALIDATE + triggers live (M.1) — raw-SQL guard
   demonstrated in the environment.
2. Dual-write code + CAS discipline + derivation live (structural).
3. GO-02 shadow soak → clean.
4. Card/display readers on derived labels (ST-04) — display-only risk.
5. F5 freeze enforcement → BLOCK mode; freeze-effective page live.
6. Observability pack (OB-03..07) + runbook links live.
7. F1 retry scanner (new mode). Soak: retry outcomes sane, no
   stacked-retry double-fires.
8. F2 resolver (dry-run → write). Soak: query volume within budget,
   outcomes applied correctly.
9. F3 escalation. Soak: no false escalation storms.
10. F4 auto-downgrade — ONLY after CT-02..05 PASS on file, TL-5-derived
    trust age configured, observed-lag watchdog live.
11. OP-01 operation deployed + OP-03 drill done (any time after step
    1; MUST be before go-live).
12. GO-04 gates → go-live.
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

