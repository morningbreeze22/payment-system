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
Objective: recompute I1/I2 per obligation; re-check under lock; page; verify L9 totality. PLUS ship (do NOT schedule) the §6.6 accepted-window CANDIDATE diagnostic (reviews 2b697fb M1 + b1d91dc M1 + b760786 M1 + 4098532 M1 — REQUIRED deliverable, but its test failing does NOT block OB-01 completion: failure → EXPLICIT OPEN ITEM in the P12 handoff, deadline = before first production marker-triage use; invocation on-demand; NEVER gates payment go-live (literally true only under this non-blocking rule); never a standing scan; exact SQL + SAFE-EXECUTION ENVELOPE (required bind, row cap, named timeout, read-only/replica, one-time plan look) in 14-observability N.1): obligations with a LIVE validation_failed marker → flag sibling requests (same business_id, different scope) with creating_ordering < validation_failed_ordering AND created_at > validation_failed_first_at → LOWER_ORDER_SIBLING_REQUEST_AFTER_VALIDATION_MARKER_CANDIDATE (masked; candidates for MANUAL triage — never auto-classified, never a page/gate; NOT in CA-4's index contract — reads historical rows by design; covers ONLY the post-marker subset, the other ratified schedule is visible via the marker on B itself).
Tests: seeded I1/I2 violations page; read-skew non-page; L9 detection; diagnostic query correctness only (no schedule/plan assertions): seeded escape-schedule case flagged, pre-failure request NOT flagged, output masked, candidate = metric/log only. Stop: merged incl. the documented diagnostic.
```

```text
[OB-02] Reconciliation tripwires
Read: §8 (anomaly) §15. Invariant: NEW event_id + zero-row CAS on a TERMINAL row = CRITICAL; benign redelivery (known event_id) = silent skip.
Placeholders: [Payment Status Feed Consumer] [Metrics / Alerting Layer]. Mappings: IN-07.
Objective: terminal-evidence CRITICAL routed; per-obligation count sanity ticket; post-F0 NULL-stamp data-quality scan (created_at >= F0 activation timestamp from the signed manifest AND required_total_at_creation IS NULL → LOW ticket, never page/gate — aa4399c L1); post-F0 NULL-request_seq scan (same window AND request_seq IS NULL → ALERT + ticket, identity-contract breach, higher severity — 4dbdf2b M1).
Tests: each fires; benign doesn't; seeded post-F0 NULL stamp → ticket, NULL request_seq → alert; pre-F0 NULLs → silent. Stop: merged.
```

