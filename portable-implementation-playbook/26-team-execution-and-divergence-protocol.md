> **Purpose:** How a TEAM (multiple people/agents in parallel) executes this playbook against a real codebase whose business details, schema, and conventions the playbook has never seen: the starting premise, the divergence protocol + register, the local facts sheet, and the sanctioned parallel work streams.
> **When to use this file:** Read once before Phase P1; consult the divergence protocol whenever local reality differs from the reference model; consult the work-stream map when assigning work to more than one person/agent.
> **Depends on:** 15-local-placeholder-mapping-template.md; 06-local-discovery-workflow.md; 20-execution-sequence-and-decision-defaults.md; 16-local-agent-instructions.md.
> **Used by:** The human driver planning assignments; every executor when a divergence appears.
> **Safe to transfer:** yes (the FILLED register and facts sheet are LOCAL-ONLY, like the mapping)
> **Contains local code names:** no while blank; the filled copies stay on the work laptop

# T. Team execution and divergence protocol

## T.1 What you are starting from (the handover premise)

This kit assumes — and discovery (F.3/F.4/F.24) verifies — that:

```text
- A HAPPY-PATH payment flow already EXISTS and pays today: intake,
  enrichment, construction, POST, some status handling. This kit
  does not rebuild it.
- The BUSINESS LOGIC (what to pay, whom, account/party resolution,
  validation rules, payment construction) is YOURS and is PRESERVED
  — no card changes it (rule 6; BUSINESS_RULE_CHANGE_REQUIRED).
- What is likely THIN or MISSING is what this kit adds: failure
  paths (crash, timeout, ambiguous outcomes, out-of-order and
  absent upstream data), money invariants, duplicate-payment
  defenses, recovery machinery, tests for all of it, an ops surface,
  and go-live evidence.
- The playbook's schema and service names are a REFERENCE MODEL
  (T.2), not a demand that your codebase look like it.
```

If discovery contradicts the premise itself (e.g. no happy path
exists), STOP after D-12 — the human owner re-scopes; the card
sequence assumes an enhancing refactor, not a greenfield build.

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
DIV-2 TYPE/SHAPE     Same meaning, different representation (type,
                     scale, nullable, separate table, denormalized
                     copy). → Record in the register with the exact
                     local DDL; adapt the snippet per M0 (file 24);
                     the card's tests must still prove the spec
                     invariant against the REAL shape. Reviewer of
                     the phase sees the register entry.
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
- POST call p50/p99 latency + current timeout. Feeds: POST lease,
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
STREAM B  "Provider proof": P8 contract tests (CT-01..07) — needs
          sandbox access, independent of the spine. HUMAN+AGENT.
          Start any time after D-12; results feed CA-1/2/3 and the
          P8 PASS gate consumed much later (auto-downgrade, GO-03).
STREAM C  "Papers": P2 B-cards (external asks/filings) + CA
          artifact drafts. Human-driven; agents may draft. Start
          immediately (B-01..B-03 "ask immediately" rows).

After STREAM A's P3 merges to main:
STREAM A continues P4 → P5.
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
STREAM F  "Ops surface": P11 (OP cards + CA-9 store) — parallel
          with STREAM E once P6/P7 are on main; its T-33 suite
          joins with reprocess paths from OP-04b/c.
STREAM G  "Watch": P12 drift + P13 observability scaffolding —
          parallel after P7; FINAL alert wiring (OB-03..07) waits
          for the metric sources of E/F to exist.

HARD JOIN: P14 rollout (GO cards) — single owner, starts only when
every stream's phases are merged and reviewed.
```

Binding constraints (rule 22 in file 16):

```text
- One stream = one phase integration branch = one owner at a time.
  "One card at a time" (rule 3) applies PER STREAM.
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
