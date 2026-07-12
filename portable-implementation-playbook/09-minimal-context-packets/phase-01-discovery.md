> **Purpose:** Minimal context packets D-01..D-12 — paste-alone briefs for a small-context local agent (original Section I, phase P1).
> **When to use this file:** Paired with the matching task-card file 08-task-cards/01-discovery.md — one packet per task, used as the working brief.
> **Depends on:** 09-minimal-context-packets/README.md; the matching task card; the requirement sections each packet cites; 07-placeholder-glossary.md.
> **Used by:** The local coding agent executing phase P1.
> **Safe to transfer:** yes
> **Contains local code names:** no

# Minimal Context Packets — Phase P1

```text
[D-01] Set up local mapping document
Read: playbook Sections G, O; file 26 T.2/T.3. Invariant: mappings, the divergence register, and the facts sheet stay LOCAL, never transferred out.
Placeholders: all. Mappings: none.
Objective: create the Section O table locally (one row per Section G placeholder + the four §2 tables incl. trade_snapshot_state, Status=UNMAPPED) + the empty divergence register + facts sheet (file 26).
Tests: none. Stop: all three files exist.
```

```text
[D-02] Discover schema/migration state
Read: §2.1 §2.2 §2.3 §10.3 §16.5; playbook F.17 F.18. Invariant: read-only; record, don't fix.
Placeholders: [DB Migration Directory] [Stored Procedure / Trigger Area]. Mappings: none.
Objective: record migration tool+dir; per-table column gap list vs §2; constraint/trigger/index inventory; DB privileges.
Tests: none. Stop: gap inventory recorded, every §2 field has a status.
```

```text
[D-03] Discover obligation + money fields
Read: §2.1 §3 §11; playbook F.1 F.12 F.13. Invariant: read-only; existing counter semantics recorded, never assumed.
Placeholders: [Obligation Repository] [Reservation Repository]. Mappings: none.
Objective: map scope entity, upsert path, FOR UPDATE usage, every amount-field writer + transaction boundary, observed counter semantics.
Tests: none. Stop: rows Confirmed/UNCLEAR with evidence.
```

```text
[D-04] Discover request persistence + status model
Read: §2.2 §10.4 §11; playbook F.2 F.10 F.11. Invariant: the rule-site inventory must be COMPLETE (it drives ST-05).
Placeholders: [Request Status Persistence Layer] [Payment Request Creation Component]. Mappings: none.
Objective: record status enum values+meanings; every status writer (conditional vs blind); every reader that branches on status; CAS/version column reality.
Tests: none. Stop: writer + rule-site inventories recorded.
```

```text
[D-05] Discover provider POST path
Read: §5 §7.0–7.2 §16.1; playbook F.4 F.5 F.15 F.16. Invariant: read-only; every response branch listed or marked UNCLEAR.
Placeholders: [Provider POST Client] [Provider Response Parser]. Mappings: none.
Objective: record call-site count, SDK method+response shape, all response branches, identity fields sent + where generated, UETR extraction sites, retry wrappers around the POST.
Tests: none. Stop: POST-path memo recorded.
```

```text
[D-06] Discover status-query usage
Read: §9 intro §9.1; playbook F.6. Invariant: the query API ≠ the status feed.
Placeholders: [Status Query Resolver]. Mappings: none.
Objective: record query client existence, query key(s), response statuses seen in code, callers. MISSING is a valid finding.
Tests: none. Stop: row Confirmed/MISSING/UNCLEAR.
```

```text
[D-07] Discover both Kafka consumers
Read: §6.0 §8 §16.2; playbook F.7. Invariant: record gaps vs §16.2 line by line; change nothing.
Placeholders: [Payment Status Feed Consumer] (+upstream consumer). Mappings: none.
Objective: per consumer record ack mode, auto-commit, offset reset, deserializer, DLT, key, concurrency, poll settings → §16.2 gap checklist.
Tests: none. Stop: checklist complete for both.
```

```text
[D-08] Discover scheduled jobs + retry ownership
Read: §7.4 §9.5 §16.1; playbook F.9 F.19. Invariant: exactly one retry owner per operation must be identifiable later; inventory all.
Placeholders: [Retry Resolver Job] [Reconciliation / Drift Scanner] [Status Query Resolver]. Mappings: none.
Objective: job inventory (schedule, tables, states claimed, time source); flag double-claim risks and app-time comparisons.
Tests: none. Stop: inventory recorded.
```

```text
[D-09] Discover identity + duplicate protection
Read: §5 §5.1; playbook F.14 F.15. Invariant: persistence timing claims must be traced to the commit boundary, not assumed.
Placeholders: [Payment Request Creation Component] [Provider POST Client]. Mappings: D-05 memo.
Objective: record key generation (deterministic vs random), inputs, persistence timing vs the wire call, every duplicate-prevention layer.
Tests: none. Stop: identity memo recorded.
```

```text
[D-10] Discover Hazelcast, metrics, ops procedures
Read: §16.1 (freeze) §14 §15 (skim) §20; playbook F.20 F.21. Invariant: verify the freeze toggle's technical shape, don't infer it.
Placeholders: [Metrics / Alerting Layer] [Operator Admin Procedure Area] [Stored Procedure / Trigger Area]. Mappings: none.
Objective: record Hazelcast topology + toggles, metrics/log/alert stack, encoder masking status, MDC propagation, PL/SQL ops objects + grants, session-context facility.
Tests: none. Stop: rows + infra memo recorded.
```

```text
[D-11] Inventory existing tests
Read: playbook F.23 F.24. Invariant: the baseline green bar is the preserved-behavior evidence; record flaky tests by name.
Placeholders: [Integration Test Suite] [Contract Test Suite]. Mappings: none.
Objective: record suites, DB/Kafka test infra (real Oracle? gap if H2-only), business-rule test coverage; run full suite once, record baseline.
Tests: baseline run. Stop: inventory + baseline recorded.
```

```text
[D-12] Discovery report
Read: playbook Section F status codes; Playbook Index BLOCKED list; file 26 T.1–T.3. Invariant: no "probably" — IMPLEMENTED/PARTIAL/MISSING/UNCLEAR only; DIV-3/DIV-4 register rows highlighted (they block cards, rule 21).
Placeholders: all. Mappings: all D-xx rows filled.
Objective: per F.1–F.26 concept assign a status + one-line evidence; summarize the divergence register + facts sheet; confirm the T.1 premise (happy path exists — else stop for re-scoping); update locally-BLOCKED task list; deliver to human owner.
Tests: none. Stop: report delivered; WAIT for human review before implementation.
```

