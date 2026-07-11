> **Purpose:** Minimal context packets OB-01..OB-02 — paste-alone briefs for a small-context local agent (original Section I, phase P12).
> **When to use this file:** Paired with the matching task-card file 08-task-cards/12-drift-reconciliation.md — one packet per task, used as the working brief.
> **Depends on:** 09-minimal-context-packets/README.md; the matching task card; the requirement sections each packet cites; 07-placeholder-glossary.md.
> **Used by:** The local coding agent executing phase P12.
> **Safe to transfer:** yes
> **Contains local code names:** no

# Minimal Context Packets — Phase P12

```text
[OB-01] Drift scanner
Read: §3 (drift + invariants) §10.3 (L9) §15. Invariant: snapshot read + locked re-check BEFORE paging; read skew never pages; mismatch PAGES (not logs).
Placeholders: [Reconciliation / Drift Scanner] [Metrics / Alerting Layer]. Mappings: SCN/flashback availability (else UNCLEAR → DBA).
Objective: recompute I1/I2 per obligation; re-check under lock; page; verify L9 totality.
Tests: seeded I1/I2 violations page; read-skew non-page; L9 detection. Stop: merged.
```

```text
[OB-02] Reconciliation tripwires
Read: §8 (anomaly) §15. Invariant: NEW event_id + zero-row CAS on a TERMINAL row = CRITICAL; benign redelivery (known event_id) = silent skip.
Placeholders: [Payment Status Feed Consumer] [Metrics / Alerting Layer]. Mappings: IN-07.
Objective: terminal-evidence CRITICAL routed; per-obligation count sanity ticket.
Tests: each fires; benign doesn't. Stop: merged.
```

