> **Purpose:** Task cards D-01..D-12 (discovery only) (original Section H, phase P1).
> **When to use this file:** When executing the tasks of this phase, one card at a time, with the matching packet file from 09-minimal-context-packets/.
> **Depends on:** 08-task-cards/README.md; 01-playbook-index.md; 07-placeholder-glossary.md; the requirement sections cited per card; the locally filled mapping template.
> **Used by:** The local coding agent executing phase P1.
> **Safe to transfer:** yes
> **Contains local code names:** no

## H-Phase 1 — Discovery (P1)

### D-01 — Set up the local mapping document

- **Task ID:** D-01
- **Title:** Create the local placeholder mapping document from the Section O template
- **Classification:** local discovery task only
- **Purpose:** one place, kept LOCAL, where every placeholder → real component mapping lives.
- **Prerequisites:** none.
- **Requirement sections / concepts to read:** Sections G and O of this playbook.
- **Placeholder components involved:** all (as rows).
- **Local placeholder mappings required before starting:** none.
- **Local code areas to discover:** none yet.
- **How to locate:** n/a.
- **Implementation instructions:** copy Section O's table into a local file (e.g. a local notes file inside the repo's ignored area); one row per placeholder from Section G; add rows for the four core tables (§2 — incl. trade_snapshot_state, §2.4) and the four documented services; ALSO create, in the same ignored area, the LOCAL DIVERGENCE REGISTER (file 26 T.2) and the LOCAL FACTS SHEET (file 26 T.3) — both stay local exactly like the mapping.
- **Do not change:** any source file.
- **Tests to add:** none.
- **Edge cases:** none.
- **Manual validation:** table exists, all Section G placeholders present as rows, Status column = UNMAPPED; divergence register + facts sheet files exist (empty, headers only).
- **Expected outcome:** empty mapping table ready to fill.
- **Failure signs:** mapping file placed where it could be committed/pushed to a shared location visible externally — it must stay local.
- **Common mistakes:** starting discovery before the table exists (findings get lost).
- **Completion criteria:** file exists with all rows.
- **Stop condition:** table created; stop.
- **Next task:** D-02.

### D-02 — Discover schema and migration state

- **Task ID:** D-02
- **Title:** Inventory current DB schema, migration tooling, constraints, triggers, indexes
- **Classification:** local discovery task only
- **Purpose:** establish the real starting shape for Phase P3.
- **Prerequisites:** D-01.
- **Requirement sections / concepts to read:** §2.1, §2.2, §2.3, §10.3, §16.5; playbook F.17, F.18.
- **Placeholder components involved:** [DB Migration Directory], [Stored Procedure / Trigger Area].
- **Local placeholder mappings required before starting:** none (this task creates them).
- **Local code areas to discover:** migration directory; DDL for the obligation/request/inbox-equivalent tables; constraint/trigger/index inventory.
- **How to locate:** per F.17/F.18 search patterns.
- **Implementation instructions:** record in the mapping doc: migration tool + version + directory; per-table column list vs the §2.1/§2.2/§2.3 target (three columns: exists-as-specified / exists-different / missing); constraint + trigger + index inventory with exact expressions; DB privileges available for creating triggers/procedures.
- **Do not change:** anything.
- **Tests to add:** none.
- **Edge cases:** column names that LOOK like spec fields but hold different semantics — record the observed semantics, don't assume.
- **Manual validation:** every §2.x field appears in the gap table with a status.
- **Expected outcome:** complete schema gap inventory.
- **Failure signs:** gap table with "probably" entries — replace with UNCLEAR + what blocked confirmation.
- **Common mistakes:** reading application entities instead of actual DDL (ORM annotations can lie about the DB).
- **Completion criteria:** mapping rows for both placeholders filled; gap inventory attached.
- **Stop condition:** inventory recorded; stop.
- **Next task:** D-03.

### D-03 — Discover obligation handling and money fields

- **Task ID:** D-03
- **Title:** Map obligation creation/lookup, locking, and current money-field semantics
- **Classification:** local discovery task only
- **Purpose:** find [Obligation Repository]/[Reservation Repository] and learn what today's amount fields MEAN.
- **Prerequisites:** D-01.
- **Requirement sections / concepts to read:** §2.1, §3, §11 (lock model); playbook F.1, F.12, F.13.
- **Placeholder components involved:** [Obligation Repository], [Reservation Repository].
- **Local placeholder mappings required before starting:** none.
- **Local code areas to discover:** scope entity, its upsert path, SELECT FOR UPDATE usage, amount-field writers.
- **How to locate:** per F.1/F.12/F.13.
- **Implementation instructions:** record: scope-key fields actually in use (compare against §2.1 — any difference is ALSO evidence for B-01); every writer of every amount field with its transaction boundary; whether any committed/reserved-like counter exists and its OBSERVED semantics.
- **Do not change:** anything.
- **Tests to add:** none.
- **Edge cases:** counters updated by DB jobs or other services (writers outside this repo) — record as UNCLEAR with the observed evidence.
- **Manual validation:** every amount-field writer listed with file:line-style local references (locally only).
- **Expected outcome:** mapping rows filled; money-semantics memo.
- **Failure signs:** "committed_amount already exists" without semantics evidence.
- **Common mistakes:** assuming a counter named like the spec has spec semantics (F.12 PARTIAL trap).
- **Completion criteria:** both placeholder rows Confirmed or UNCLEAR with reasons.
- **Stop condition:** recorded; stop.
- **Next task:** D-04.

### D-04 — Discover request persistence and status model

- **Task ID:** D-04
- **Title:** Map request-table persistence, current status enum, transition writers, CAS idioms
- **Classification:** local discovery task only
- **Purpose:** baseline for P6 (factored model) and the ST-05 legacy-status rule inventory.
- **Prerequisites:** D-01.
- **Requirement sections / concepts to read:** §2.2, §10.4, §11; playbook F.2, F.10, F.11.
- **Placeholder components involved:** [Request Status Persistence Layer], [Payment Request Creation Component].
- **Local placeholder mappings required before starting:** none.
- **Local code areas to discover:** request entity + repository; the status enum type; every status writer; every status READER that branches on it (rule sites).
- **How to locate:** per F.2/F.10/F.11; for rule sites, search usages of the status enum type.
- **Implementation instructions:** record: current status values and their meanings as observed; writer list (conditional vs blind save); reader/rule-site list — THIS LIST IS THE ST-05 INPUT, completeness matters; version/CAS column presence.
- **Do not change:** anything.
- **Tests to add:** none.
- **Edge cases:** status strings used in SQL/reports/dashboards outside Java — search migrations and any query files too; note external consumers you can see locally.
- **Manual validation:** rule-site list reviewed once more against enum usages (grep count matches list length).
- **Expected outcome:** complete rule-site inventory + writer inventory.
- **Failure signs:** rule-site list "top 10" instead of complete — ST-05 depends on completeness.
- **Common mistakes:** missing readers in scheduled jobs and monitoring queries.
- **Completion criteria:** both placeholders mapped; inventories attached.
- **Stop condition:** recorded; stop.
- **Next task:** D-05.

### D-05 — Discover provider POST path

- **Task ID:** D-05
- **Title:** Map the engine POST call, SDK usage, response parsing, timeout/retry wrappers
- **Classification:** local discovery task only
- **Purpose:** baseline for P4 (write-ahead identity), P5 (UETR), P10 (classifier).
- **Prerequisites:** D-01.
- **Requirement sections / concepts to read:** §5 (identity chain), §7.0–7.2, §16.1 (retry ownership); playbook F.4, F.5, F.15, F.16.
- **Placeholder components involved:** [Provider POST Client], [Provider Response Parser].
- **Local placeholder mappings required before starting:** none.
- **Local code areas to discover:** POST call site(s); SDK method + response type; every catch/branch after the call; identity fields sent; any retry annotations/wrappers around the call.
- **How to locate:** per F.4/F.5/F.15/F.16.
- **Implementation instructions:** record: exact call-site count; response branches currently produced; which identity fields go to the engine and where each is generated; UETR/reference extraction sites; all retry layers around the POST (each is a §16.1 violation candidate — inventory only).
- **Do not change:** anything.
- **Tests to add:** none.
- **Edge cases:** async/callback-style SDK usage — record threading model (matters for §11 claim ownership).
- **Manual validation:** a colleague-level re-read of the branch inventory against the actual catch blocks.
- **Expected outcome:** POST-path memo with branch inventory.
- **Failure signs:** any response branch labeled "unknown meaning" silently — must be listed as UNCLEAR feeding CA-1.
- **Common mistakes:** missing exception paths (timeouts thrown vs returned).
- **Completion criteria:** both placeholders mapped; branch + retry-layer inventories attached.
- **Stop condition:** recorded; stop.
- **Next task:** D-06.

### D-06 — Discover status-query usage

- **Task ID:** D-06
- **Title:** Map any existing engine status-query client and its callers
- **Classification:** local discovery task only
- **Purpose:** baseline for [Status Query Resolver] (P10).
- **Prerequisites:** D-01.
- **Requirement sections / concepts to read:** §9 intro, §9.1; playbook F.6.
- **Placeholder components involved:** [Status Query Resolver].
- **Local placeholder mappings required before starting:** none.
- **Local code areas to discover:** query client; scheduled callers; query key used.
- **How to locate:** per F.6.
- **Implementation instructions:** record client existence, query key(s), response statuses observed in code, callers.
- **Do not change:** anything.
- **Tests to add:** none.
- **Edge cases:** query API used only in ops scripts — record those too.
- **Manual validation:** n/a.
- **Expected outcome:** mapping row filled (possibly MISSING — valid finding).
- **Failure signs:** conflating the status-query API with the status FEED (§1 lists them as different channels).
- **Common mistakes:** same.
- **Completion criteria:** row Confirmed/MISSING/UNCLEAR.
- **Stop condition:** recorded; stop.
- **Next task:** D-07.

### D-07 — Discover both Kafka consumers

- **Task ID:** D-07
- **Title:** Map upstream trade-message consumer and payment status feed consumer + their config
- **Classification:** local discovery task only
- **Purpose:** baseline for P9 (IN-xx) and §16.2 hardening gaps.
- **Prerequisites:** D-01.
- **Requirement sections / concepts to read:** §6.0, §8, §16.2; playbook F.7.
- **Placeholder components involved:** [Payment Status Feed Consumer], (upstream consumer maps under [Obligation Repository] flow).
- **Local placeholder mappings required before starting:** none.
- **Local code areas to discover:** listener classes, container factory config, ack mode, offset reset, deserializers, DLT wiring, topic keys, concurrency.
- **How to locate:** per F.7; Spring Kafka config classes/properties.
- **Implementation instructions:** record per consumer: ack mode, auto-commit, offset reset, error handler, DLT, key field, concurrency, max.poll settings — as a checklist against §16.2 line by line (each line = compliant / gap).
- **Do not change:** anything.
- **Tests to add:** none.
- **Edge cases:** shared consumer-group or shared container factory with other domains — record.
- **Manual validation:** checklist complete for both consumers.
- **Expected outcome:** §16.2 gap checklist.
- **Failure signs:** properties read from a deployment repo you didn't check — record UNCLEAR if runtime config is elsewhere.
- **Common mistakes:** reading only annotated defaults and missing yaml overrides.
- **Completion criteria:** both consumers mapped with gap checklist.
- **Stop condition:** recorded; stop.
- **Next task:** D-08.

### D-08 — Discover scheduled jobs and retry machinery

- **Task ID:** D-08
- **Title:** Inventory every scheduled job touching payment data; identify retry ownership
- **Classification:** local discovery task only
- **Purpose:** baseline for [Retry Resolver Job], RC-04/05/08, OB-01; §16.1 single-retry-owner rule.
- **Prerequisites:** D-01.
- **Requirement sections / concepts to read:** §7.4, §9.5, §16.1 (retry ownership, scanner rules); playbook F.9, F.19.
- **Placeholder components involved:** [Retry Resolver Job], [Reconciliation / Drift Scanner], [Status Query Resolver].
- **Local placeholder mappings required before starting:** none.
- **Local code areas to discover:** all @Scheduled/cron entries; claim/lease columns; SKIP LOCKED usage; app-time vs DB-time comparisons.
- **How to locate:** per F.9/F.19.
- **Implementation instructions:** job inventory table: name-as-found (locally), schedule, tables touched, states claimed, retry semantics, time source. Flag every job that could claim the same rows as another (double-processing risk) and every app-time due comparison (§11 requires DB time).
- **Do not change:** anything.
- **Tests to add:** none.
- **Edge cases:** jobs enabled per-environment only; leader-election wrappers.
- **Manual validation:** inventory covers every scheduling annotation found by search.
- **Expected outcome:** job inventory + retry-ownership map.
- **Failure signs:** unknown-purpose jobs skipped instead of listed.
- **Common mistakes:** missing non-Spring schedulers (DB jobs, external cron) — check migrations for DBMS_SCHEDULER too.
- **Completion criteria:** placeholders mapped; inventory attached.
- **Stop condition:** recorded; stop.
- **Next task:** D-09.

### D-09 — Discover identity and duplicate protection

- **Task ID:** D-09
- **Title:** Map idempotency-key generation, persistence timing, and every duplicate-prevention mechanism
- **Classification:** local discovery task only
- **Purpose:** the money-critical baseline for P4; feeds K-02/K-04/K-06.
- **Prerequisites:** D-01, D-05.
- **Requirement sections / concepts to read:** §5, §5.1; playbook F.14, F.15.
- **Placeholder components involved:** [Payment Request Creation Component], [Provider POST Client].
- **Local placeholder mappings required before starting:** D-05's POST-path memo.
- **Local code areas to discover:** key generation site(s); when the key is persisted relative to the wire call; UNIQUE constraints on identity; existsBy-style checks.
- **How to locate:** per F.14/F.15.
- **Implementation instructions:** record: deterministic vs random; generation inputs; persistence timing (before/after POST — trace the actual transaction boundaries); every duplicate-prevention layer and what it actually prevents.
- **Do not change:** anything.
- **Tests to add:** none.
- **Edge cases:** different identities for different payment types; identity reuse on retries (or not — §5 requires same-key retries; record current behavior precisely).
- **Manual validation:** trace one real code path creation→POST and confirm the recorded timing.
- **Expected outcome:** identity memo: generation, timing, protection layers.
- **Failure signs:** "persisted before POST" claimed without tracing the commit boundary.
- **Common mistakes:** confusing the UETR/engine reference with the caller-supplied key (§5 identity chain keeps them distinct).
- **Completion criteria:** memo complete; F.15 status assigned.
- **Stop condition:** recorded; stop.
- **Next task:** D-10.

### D-10 — Discover Hazelcast, metrics, alerting, ops procedures

- **Task ID:** D-10
- **Title:** Map Hazelcast usage/toggles, metrics/log/alert stack, existing ops DB procedures
- **Classification:** local discovery task only
- **Purpose:** baseline for RC-09 (freeze), P13 (observability), P11 (procedure area).
- **Prerequisites:** D-01.
- **Requirement sections / concepts to read:** §16.1 (freeze), §14, §15 (skim list), §20 (interim model); playbook F.20, F.21.
- **Placeholder components involved:** [Metrics / Alerting Layer], [Operator Admin Procedure Area], [Stored Procedure / Trigger Area].
- **Local placeholder mappings required before starting:** none.
- **Local code areas to discover:** Hazelcast client config + existing toggles/maps; metrics registry + conventions; logging encoder (masking?); MDC/correlation propagation; alert rule location; existing PL/SQL ops objects + role grants.
- **How to locate:** per F.20/F.21; Hazelcast config beans/properties.
- **Implementation instructions:** record each; specifically: does a posting-freeze-like toggle already exist (§16.1 says the toggle-with-role-control exists operationally — find its technical shape); is account masking in the encoder or call sites; correlation id propagation status.
- **Do not change:** anything.
- **Tests to add:** none.
- **Edge cases:** Hazelcast used embedded vs client-server — record topology (affects freeze-check timeout design).
- **Manual validation:** n/a.
- **Expected outcome:** three mapping rows + infra memo.
- **Failure signs:** assuming the freeze toggle exists in code because §16.1 says role control exists — verify the technical artifact, else UNCLEAR.
- **Common mistakes:** missing alert rules living in a separate ops repo — record UNCLEAR if not visible locally.
- **Completion criteria:** rows filled.
- **Stop condition:** recorded; stop.
- **Next task:** D-11.

### D-11 — Inventory existing tests

- **Task ID:** D-11
- **Title:** Inventory test suites: business-rule tests, integration lanes, infrastructure capability
- **Classification:** local discovery task only
- **Purpose:** establish the preserved-behavior safety net and the test-infrastructure gaps (real Oracle lane needed by S-05/S-06/OP-02).
- **Prerequisites:** D-01.
- **Requirement sections / concepts to read:** playbook F.23, F.24; Section J intro.
- **Placeholder components involved:** [Integration Test Suite], [Contract Test Suite].
- **Local placeholder mappings required before starting:** none.
- **Local code areas to discover:** test source sets, container config, CI definitions, business-rule test coverage.
- **How to locate:** per F.23/F.24.
- **Implementation instructions:** record: suites + how they run; DB used by integration tests (real Oracle? — if H2-only, record the S-09 lane gap); Kafka test infra; baseline green-bar snapshot (run the full suite once, record results — this is the Section Q backwards-compatibility baseline).
- **Do not change:** any test.
- **Tests to add:** none.
- **Edge cases:** flaky tests at baseline — record them BY NAME now, so later failures can be attributed.
- **Manual validation:** baseline run results stored locally.
- **Expected outcome:** test inventory + baseline green bar.
- **Failure signs:** baseline not runnable — that itself is a critical finding for D-12.
- **Common mistakes:** skipping the baseline run.
- **Completion criteria:** inventory + baseline recorded.
- **Stop condition:** recorded; stop.
- **Next task:** D-12.

### D-12 — Discovery report and gap classification

- **Task ID:** D-12
- **Title:** Consolidate discovery into the implemented/partial/missing report; update the BLOCKED list
- **Classification:** local discovery task only
- **Purpose:** the single artifact Phase P2+ planning reads; converts findings into per-task readiness.
- **Prerequisites:** D-02 … D-11 all complete.
- **Requirement sections / concepts to read:** playbook Section F status codes; Playbook Index BLOCKED list; requirment-v4.md §2.4 (round 18 — the CANONICAL greenfield fact incl. its bootstrap-restoration condition: the executor proving P-B must read the fact itself, not only the file-26 paraphrase; the report records that §2.4 was read and that the query scope matches its exact population claim).
- **Placeholder components involved:** all.
- **Local placeholder mappings required before starting:** all D-xx rows filled.
- **Local code areas to discover:** none (consolidation).
- **How to locate:** n/a.
- **Implementation instructions:** for each F.1–F.26 concept assign IMPLEMENTED/PARTIAL/MISSING/UNCLEAR with one-line evidence (F.26 = the facts-sheet completeness check); list every UNCLEAR with what would resolve it; summarize the DIVERGENCE REGISTER (every DIV-3/DIV-4 highlighted — each blocks its cards until resolved, rule 21) and the FACTS SHEET (enrichment step inventory + latencies, volumes, Oracle version, test lanes); verify BOTH T.1 premises (round 17): P-A (a happy path exists — if not, stop for re-scoping) AND P-B (zero pre-existing trades/obligations in this flow's admission scope) — file the CUTOVER_POPULATION_GREENFIELD proof: named queries over the mapped obligation/trade tables scoped to this flow, per target environment, with query text + timestamps + result counts + owner + reviewer recorded in the facts sheet; a NONZERO count is DIV-4 — STOP for architecture review (S-08 status backfill is NOT sufficient; the retired bootstrap/pointer machinery, git 9a53c75, may need restoration); mark which task cards are locally BLOCKED (mapping missing) beyond the §18-BLOCKED list; deliver the report to the human owner.
- **Do not change:** any source file.
- **Tests to add:** none.
- **Edge cases:** conflicts between findings (e.g. two duplicate-prevention layers with different keys) — surface, don't resolve.
- **Manual validation:** human owner reviews the report before P3 starts.
- **Expected outcome:** discovery report; readiness per task phase.
- **Failure signs:** report with unresolved "probably"s.
- **Common mistakes:** turning findings into design changes — findings are inputs, the spec is the design.
- **Completion criteria:** report delivered; mapping table complete.
- **Stop condition:** report delivered; STOP — implementation starts only after human review.
- **Next task:** B-01 (human-driven) / S-01 after P2 gates.


---

## Phase handoff summary (P1 → P2)

- **Phase outputs:** Section O mapping table filled for every placeholder (CONFIRMED / PARTIAL / MISSING / UNCLEAR); D-12 discovery report delivered to the human owner; D-11 baseline green bar recorded.
- **Blockers to carry forward:** §18 BLOCKING items 0–3 still unanswered — B-01..B-04 own them; any locally-BLOCKED mappings listed in D-12.
- **Local mapping rows expected filled:** ALL rows attempted (all placeholders + 3 core tables + 4 documented services + toggle/calendar/legacy-enum rows).
- **Tests expected to exist:** none new; the pre-existing suite baseline (incl. named flaky tests) is recorded.
- **Next phase entry condition:** human owner has reviewed the D-12 report. NO implementation before that review.
