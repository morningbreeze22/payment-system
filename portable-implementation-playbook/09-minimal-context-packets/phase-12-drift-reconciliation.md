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
Objective: recompute I1/I2 per obligation; re-check under lock; page; verify L9 totality. PLUS the §6.6 accepted-window CANDIDATE scan (review 2b697fb M1): obligations with a LIVE validation_failed marker → flag sibling requests (same business_id, different scope) with creating_ordering < validation_failed_ordering AND created_at > validation_failed_first_at → VALID_SCOPE_CREATED_BELOW_KNOWN_VALIDATION_FAILURE_ORDERING (masked; candidates for MANUAL triage — never auto-classified, never a page/gate).
Tests: seeded I1/I2 violations page; read-skew non-page; L9 detection; accepted-window seeded case flagged, pre-failure request NOT flagged, output masked, candidate = metric/log only. Stop: merged incl. the candidate scan.
```

```text
[OB-02] Reconciliation tripwires
Read: §8 (anomaly) §15. Invariant: NEW event_id + zero-row CAS on a TERMINAL row = CRITICAL; benign redelivery (known event_id) = silent skip.
Placeholders: [Payment Status Feed Consumer] [Metrics / Alerting Layer]. Mappings: IN-07.
Objective: terminal-evidence CRITICAL routed; per-obligation count sanity ticket.
Tests: each fires; benign doesn't. Stop: merged.
```

