> **Purpose:** Minimal context packets OB-03..OB-07 — paste-alone briefs for a small-context local agent (original Section I, phase P13).
> **When to use this file:** Paired with the matching task-card file 08-task-cards/13-observability-and-runbooks.md — one packet per task, used as the working brief.
> **Depends on:** 09-minimal-context-packets/README.md; the matching task card; the requirement sections each packet cites; 07-placeholder-glossary.md.
> **Used by:** The local coding agent executing phase P13.
> **Safe to transfer:** yes
> **Contains local code names:** no

# Minimal Context Packets — Phase P13

```text
[OB-03] Money/MAYBE alerts
Read: §15 (list + clock discipline) §13 §2.1 §2.2. Invariant: AGE alerts read episode anchors ONLY (maybe_since, first_at, …), never state_changed_at.
Placeholders: [Metrics / Alerting Layer]. Mappings: metric conventions.
Objective: implement the money-facing §15 entries (MAYBE ages+tier-2, stuck reservation, BLOCKED queue, marker + counter alerts, latch alerts+age, MISMATCH/INCONSISTENCY CRITICALs, tie/latched-amendment alerts, live-marker-no-request age, operation-use every-use alert, latch-integrity alert).
Tests: seeded condition per alert. Stop: merged.
```

```text
[OB-04] Flow/stuck alerts
Read: §15 (entries + stuck-state split) §12 (freshness) §16.2 (lag). Invariant: stuck-state split — retry states on next_retry_at OVERDUE (2026-07-11 re-key), non-churning on state_changed_at; metric absence = bad.
Placeholders: [Metrics / Alerting Layer], card read path. Mappings: metric sources from IN/RC tasks.
Objective: unmatched volume, stale message/marker-write volumes, DLT page, consumer-lag page + card lag indicator, scanner heartbeats, stuck-state, sweep overrun, observed-lag watchdog, deadlock ticket, inbox growth, dead-gauge alerting.
Tests: seeded per alert; dead-gauge check. Stop: merged.
```

```text
[OB-05] Freeze page + rollup + retention check
Read: §15 (freeze entry + rollup) §16.1 §16.2. Invariant: freeze is silent by design — the freeze-effective-without-ticket page is the ONLY signal; one outage = ONE grouped incident (state writes stay per-row).
Placeholders: [Metrics / Alerting Layer]. Mappings: grouping capability (else emission-side rollup, record).
Objective: freeze page; breaker ticket + 30m page; root-cause rollup preserving per-alert detail; retention-chain scheduled check.
Tests: freeze page; storm groups to one incident; retention alert. Stop: merged.
```

```text
[OB-06] Runbooks + logging practices
Read: §15 (practices) §16.3 §14 (retention floor). Invariant: masking in the ENCODER; every alert carries a runbook link; one correlation id greps the whole story.
Placeholders: [Metrics / Alerting Layer]. Mappings: CA-8 stubs; encoder config.
Objective: link runbooks; encoder masking verified end to end; MDC + outbound-header propagation; retention ≥ 90-day floor verified (report if below).
Tests: masking capture test; MDC through an async hop. Stop: merged.
```

```text
[OB-07] Config inventory + validation
Read: §16.6 (inventory + ordering rule) §16.5. Invariant: loader REJECTS unless trust_age + cadence < escalation < tier-2 < cutoff margin; load-bearing values never silently default.
Placeholders: config, [Metrics / Alerting Layer]. Mappings: config conventions.
Objective: one namespace for every §16.6 entry with owner notes; startup ordering validation; retry-policy completeness vs CA-1 classes.
Tests: each mis-ordering rejected; valid set accepted; missing entry fails startup. Stop: merged.
```

