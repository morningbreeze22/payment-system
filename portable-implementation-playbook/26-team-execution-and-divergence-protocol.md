> **Purpose:** How a TEAM (multiple people/agents in parallel) executes this playbook against a real codebase whose business details, schema, and conventions the playbook has never seen: the starting premise, the divergence protocol + register, the local facts sheet, and the sanctioned parallel work streams.
> **When to use this file:** Read once before Phase P1; consult the divergence protocol whenever local reality differs from the reference model; consult the work-stream map when assigning work to more than one person/agent.
> **Depends on:** 15-local-placeholder-mapping-template.md; 06-local-discovery-workflow.md; 20-execution-sequence-and-decision-defaults.md; 16-local-agent-instructions.md.
> **Used by:** The human driver planning assignments; every executor when a divergence appears.
> **Safe to transfer:** yes (the FILLED register and facts sheet are LOCAL-ONLY, like the mapping)
> **Contains local code names:** no while blank; the filled copies stay on the work laptop

# T. Team execution and divergence protocol

## T.1 What you are starting from (the handover premise)

This kit rests on TWO INDEPENDENT premises (round 17 — do not
conflate them); discovery verifies both (F.3/F.4/F.24 for the
first, the D-12 population proof for the second):

```text
PREMISE P-A (code): happy-path payment CODE already EXISTS and pays
  today — intake, enrichment, construction, POST, some status
  handling. This kit does not rebuild it. The BUSINESS LOGIC (what
  to pay, whom, account/party resolution, validation, construction)
  is YOURS and is PRESERVED — no card changes it (rule 6). What is
  likely THIN or MISSING is what this kit adds: failure paths,
  money invariants, duplicate-payment defenses, recovery, tests,
  an ops surface, go-live evidence. The playbook's schema and
  service names are a REFERENCE MODEL (T.2), not a demand that
  your codebase look like it.
PREMISE P-B (population — the §2.4 greenfield fact; NOT implied by
  P-A): the target cutover POPULATION for this flow contains ZERO
  pre-existing trades and obligations in the snapshot-admission
  scope — trade_snapshot_state deploys EMPTY and no obligation in
  scope predates its trade's first admitted message. "Existing
  happy path" means existing CODE (and possibly OTHER/legacy
  populations covered by the S-08 status backfill); it NEVER means
  pre-existing trades in this flow's admission scope. The retired
  bootstrap/pointer machinery (rounds 6–9; git 9a53c75) was removed
  BECAUSE of P-B — P-B failing silently would reopen the
  stale-snapshot money hole the machinery guarded.
```

PROOF REQUIRED — RUN TWICE (rounds 17–18; a discovery snapshot is
NOT a cutover invariant — populations change between D-12 and
go-live):

```text
RUN 1 (D-12, architectural eligibility): the named queries over
  the mapped obligation/trade tables scoped to this flow, per
  target environment — query text, timestamps, RESULT COUNTS
  (zero expected), owner, reviewer, date — recorded in the facts
  sheet (T.3).
RUN 2 (controlled cutover, go-live evidence): the SAME queries
  re-run at GO-03 IMMEDIATELY BEFORE enabling the new intake
  path, AFTER old in-scope writers are drained/fenced (or inside
  an equivalent change freeze) so no row can appear between the
  query and enablement. Converts Q5b from PENDING-CUTOVER to
  PASS (round 20); filed as Q5b evidence.
BINDING (both runs, in the evidence manifest): exact environment,
  the schema/service SCOPE PREDICATE used, query checksum,
  timestamp, RC/config version, owner, reviewer.
INVALIDATION (evidence goes STALE, rerun forced): target-
  environment change; scope-predicate change; any restore, seed,
  or data migration; old-writer activity after RUN 2; deployment
  rollback; query change.
TEST DATA: a non-production fixture demonstrably OUTSIDE the
  production cutover scope does not fail P-B — but its exclusion
  predicate is REVIEWED and RECORDED in the register, never
  improvised by the executor.
LIFECYCLE (round 19): the ZERO-population form applies to INITIAL
  activation ONLY. Any post-activation re-enable (rollback
  recovery, incident restart) instead uses the ADMISSION-COVERAGE
  form: every in-scope trade/obligation is attributable to an
  admitted trade whose row carries watermark + storage pointer +
  digest, and NO row was created by a legacy/out-of-band writer.
  Old-writer activity discovered AFTER RUN 2 is NOT cured by a
  rerun: STOP/disable F0, preserve evidence, classify affected
  rows, incident + architecture review before resumption
  (uncovered rows may require restoring the retired bootstrap
  machinery — git 9a53c75). The EXECUTABLE procedure for the
  admission-coverage form is runbook RB-F0 (file 14) — owner,
  queries, zero-uncovered threshold, sign-offs, evidence (round 20).
```

If P-A fails (no happy path): STOP after D-12 — the human owner
re-scopes; the card sequence assumes an enhancing refactor, not a
greenfield build. If P-B fails (ANY pre-existing in-scope
population): that is DIV-4 — STOP for an architecture review;
ordinary S-08 status backfill is NOT sufficient (existing trades
would lack the admission watermark, storage pointer, and digest),
and the retired bootstrap/export/pointer-coverage machinery may
need restoration from git history. A weak executor must never
classify a P-B failure as routine local divergence.

## T.2 The reference model and the divergence protocol

Every card, packet, recipe, and SQL snippet speaks the REFERENCE
MODEL's names: `payment_obligation`, `payment_request`,
`processed_inbound_event`, `trade_snapshot_state`, the §2 columns,
the four documented service names. Your codebase almost certainly
differs — different table/column names, different types, an existing
status column, extra columns, split or merged entities. That is
EXPECTED, not a defect.

**The binding rule: INVARIANTS are non-negotiable; IDENTIFIERS are
not.** What must hold is the behavior the spec names (write-once
outcome, CAS row-count verdict, reservation accounting I1–I6, lock
order, admission watermark, marker monotonicity...). Whether the
column is called `committed_amount` or `amt_reserved` is a mapping
row.

Classify every difference you meet into one of five classes and
apply its fixed resolution:

```text
DIV-1 NAME-ONLY      Same meaning, same shape, different identifier.
                     → Map it (mapping template row / register
                     entry). Translate every snippet at execution.
                     No approval needed.
DIV-2 TYPE/SHAPE     Same meaning, different representation. →
                     Record in the register with the exact local
                     DDL; adapt the snippet per M0 (file 24); the
                     card's tests must still prove the spec
                     invariant against the REAL shape; the phase
                     reviewer sees the entry. Round 17 — DIV-2
                     WITHOUT approval is allowed ONLY when the
                     executor proves ALL of: exact precision AND
                     rounding preserved; equivalent NULL
                     semantics; same atomic transaction;
                     equivalent lockability (FOR UPDATE on the
                     same row scope); ONE authoritative writer;
                     NO independently mutable copy. ANY
                     scale/rounding change, cross-table atomicity
                     change, additional writer, or denormalized-
                     ownership ambiguity AUTO-PROMOTES the item to
                     DIV-3 (recorded approval) or DIV-4 (stop)
                     BEFORE implementation — money, identity, and
                     lock columns are never "cosmetically"
                     reshaped.
DIV-3 SEMANTIC REUSE A local column/mechanism EXISTS with similar
                     but not identical semantics (e.g. a "reserved"
                     counter that actually means "sent to engine" —
                     the F.12 case). → NEVER silently reuse. Record
                     both semantics in the register; the card either
                     uses the spec's own new column or the human
                     owner approves the reuse IN the register entry
                     (name + date). MUST_VERIFY_LOCALLY.
DIV-4 STRUCTURAL     Local reality contradicts a spec invariant
                     (e.g. two writers of payment state that cannot
                     be consolidated; a shared table another domain
                     writes). → STOP the card; report SPEC_CONFLICT
                     with the register entry; human owner + (if
                     needed) the design owner decide. Never "work
                     around" an invariant.
DIV-5 BUSINESS       The difference is business behavior (enrichment
                     computes something else; validation rules
                     differ). → BUSINESS_RULE_CHANGE_REQUIRED, stop.
                     The kit adapts to business logic, never the
                     reverse.
```

**The LOCAL DIVERGENCE REGISTER** — copy this table into a local
untracked file next to the filled mapping template (D-01 creates
both). It is the memory that keeps twenty small-context sessions
consistent; a divergence resolved in session 3 must not be
re-litigated (or worse, re-decided differently) in session 14.

```text
| # | Reference-model element | Local reality (exact DDL/code ref) |
|   Class (DIV-1..5) | Resolution + who approved + date |
|   Affected cards/snippets | Register status (OPEN/RESOLVED) |
```

Rules of use (binding — rule 21 in file 16):

```text
- Cards and snippets are NEVER edited to local names — translation
  happens at execution time via mapping + register. This keeps the
  package re-usable and the cards reviewable.
- Before ANY implementation card: consult the register rows touching
  the card's tables/components (the kickoff prompt includes this).
- A DIV-3/DIV-4 without a recorded resolution BLOCKS every card that
  touches it — like an UNMAPPED placeholder.
- The register (filled) never leaves the work laptop.
- At each phase boundary (rule 19 mapping refresh) also re-check
  register rows the next phase touches.
```

## T.3 The local facts sheet (numbers only the team knows)

Several spec mechanisms need LOCAL numbers the playbook cannot know.
Collect them during discovery (D-12 consolidates; F.26 lists the
measurements), keep them in a local FACTS SHEET next to the mapping,
and treat them as the input to the §16.6 configuration values
(OB-07 validates the ordering rules):

```text
- Enrichment step inventory: the ordered list of lookups the
  existing enrichment performs, each with typical + p99 latency.
  Feeds: ENRICH claim-lease duration, per-dependency timeouts
  (§16.1), and the bulkhead/pool sizing.
- POST call p50/p99 latency + current timeout — measured WITH the
  §14.1 journal riders enabled (the LOB write is part of the claim
  transaction; §14.1 records this as a managed concern). Feeds: POST lease,
  timeout budget, breaker thresholds (§16.1).
- Feed volume + typical ingest lag. Feeds: §15 lag thresholds,
  Q-08's ingest-lag ask cross-check.
- Peak and average daily payment/message volume vs the §16.5 NFR
  (~3k trades/day baseline). Feeds: Q31 capacity gate, scanner
  batch sizes, §9.5 sweep budget vs the TL-13 quota.
- Oracle edition + exact version + patch level; available privileges
  (triggers? contexts? — F.18/F.20). Feeds: M0 dialect checks, S-05/
  S-06 feasibility, Q5 evidence.
- Migration tool + version (F.17); test lanes available (F.23:
  real-Oracle? testcontainers? H2-only = recorded gap).
- Current retry/scheduling behavior (F.9): what retries exist today
  and their intervals — the removal inventory for §16.1's single-
  retry-owner rule.
- Connection pool sizes per service; Kafka partitions per topic.
  Feeds: §16.1 pool math, concurrency settings (§16.2).
- CUTOVER_POPULATION_GREENFIELD proof (premise P-B, T.1): query
  text, environment, timestamps, result counts (ZERO expected),
  owner, reviewer, date. Nonzero = DIV-4 stop.
```

Numbers you cannot measure locally (engine-side TTLs, quotas,
lookback) stay EXTERNAL asks (Section K) — never guess them into
the facts sheet.

## T.4 Parallel work streams (team assignment map)

File 20's flat order remains authoritative in two ways: it is the
COMPLETE order for a single executor, and WITHIN each stream below
the stream's rows execute in file-20 relative order. This section
only says which contiguous segments may run CONCURRENTLY, each on
its own phase integration branch (rule 19) with ONE owner.

```text
STREAM 0  P1 discovery. D-02..D-11 are read-only and MAY be split
          across people by F-area; D-01 first and D-12 last are ONE
          owner. HARD JOIN: the human D-12 review gates everything.

Then, concurrently:
STREAM A  "Spine": P3 schema (S-01..S-10) → P4 identity (K) →
          P5 UETR (U). DBA involvement in P3. One owner per phase.
STREAM B  "Provider proof": P8 contract tests. HUMAN+AGENT. TWO
          SUB-STAGES (round 17): B-prep — sandbox access requests,
          credentials, test-plan DRAFTING — may start right after
          D-12; B-execute — the CT-01 harness build and CT-02..07
          runs — WAITS for its card prerequisites on merged main
          (CT-01 requires B-02 and K-02/K-03: provider "proof"
          collected against a temporary or invented identity
          derivation is VOID evidence). Results feed CA-1/2/3 and
          the P8 PASS gate consumed much later (GO-03).
STREAM C  "Papers": P2 B-cards (external asks/filings) + CA
          artifact drafts. Human-driven; agents may draft. Start
          immediately (B-01..B-03 "ask immediately" rows).

After STREAM A's P3 merges to main, STREAM A continues P4 → P5.
After P3 → P4 → P5 have ALL merged to main (round 17: P6 sits
downstream of identity/persistence work in the authoritative
order; no "safe early P6 subset" is enumerated, so none is
offered — do not invent one):
STREAM D  "State & money": P6 factored state → P7 guards. STRONGLY
          recommended: ONE owner for both phases — the CAS helpers
          and the guards that ride them are one mental model; the
          rule-20 review still sits between the phases.

After P6 + P7 merged (and P4 for identity-dependent cards):
STREAM E  "Inbound": P9 (IN-01..09) → P10 (RC). IN-02 is ONE CARD,
          ONE OWNER, ONE SITTING — admission + fence + absence
          semantics must not be split across sessions or people.
          P10 starts only after IN-07 (the shared evidence helper)
          is on main.
STREAM F  "Ops surface": P11 — parallel with STREAM E once P6/P7
          are on main, but ONLY the cards whose prerequisites are
          already merged: OP-01..OP-03, the CA-9 store, OP-04a.
          OP-04b/c WAIT for S-10 + IN-02 on merged main (their
          cards say so — the stream map never overrides a card);
          OP-04d/e follow their own prerequisites; T-33 joins
          after the reprocess paths exist.
STREAM G  "Watch": P12 drift + P13 observability scaffolding —
          parallel after P7; FINAL alert wiring (OB-03..07) waits
          for the metric sources of E/F to exist.

HARD JOIN: P14 rollout (GO cards) — single owner, starts only when
every stream's phases are merged and reviewed.
```

Binding constraints (rule 22 in file 16):

```text
- AUTHORITY (round 17): card prerequisites + file 20's gates are
  the ONLY scheduling authority. This stream map is an
  OPTIMIZATION HINT constrained by them — wherever this section
  and a card's prerequisites seem to disagree, the CARD wins and
  the stream waits. (A machine-readable dependency manifest was
  considered and remains NOT adopted — round-9 decision:
  lint-enforced parity + file-20 authority instead; this
  AUTHORITY rule is the compensating control.)
- One stream holds ONE ACTIVE phase integration branch at a time
  (a stream spanning phases opens the next phase's branch only
  after the previous phase passed its rule-19 review and merged);
  one owner per active branch. "One card at a time" (rule 3)
  applies PER STREAM.
- Streams consume each other's outputs ONLY via merged main —
  never cherry-picks, never shared WIP branches. The shared-helper
  interface points are: ST-02 (CAS helper), IN-04 (marker helper),
  IN-07 (evidence helper), RG-06 (creation point), S-06 session
  context. A stream needing an unmerged helper is BLOCKED — that is
  the quality gate, not an obstacle to route around.
- The mapping template, divergence register, facts sheet, and
  progress tracker are SHARED single documents — one writer at a
  time (team convention: the stream owner edits only their rows;
  merge conflicts in the tracker are a process smell).
- Quality-over-parallelism (do NOT split): IN-02 (one sitting);
  P6+P7 (same owner recommended); S-08+S-09 (migration coherence);
  the GO cards (one owner). When in doubt, serialize — the flat
  file-20 order is always correct.
- Every rule-20 human review checkpoint applies unchanged per
  stream. Parallelism never waives a review.
```
