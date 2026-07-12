> **Purpose:** Minimal context packets GO-01..GO-05 — paste-alone briefs for a small-context local agent (original Section I, phase P14).
> **When to use this file:** Paired with the matching task-card file 08-task-cards/14-rollout-and-go-live.md — one packet per task, used as the working brief.
> **Depends on:** 09-minimal-context-packets/README.md; the matching task card; the requirement sections each packet cites; 07-placeholder-glossary.md.
> **Used by:** The local coding agent executing phase P14.
> **Safe to transfer:** yes
> **Contains local code names:** no

# Minimal Context Packets — Phase P14

```text
[GO-01] Rollout plan
Read: playbook Section M; §16.5 §18. Invariant: Section M's stage ORDER is fixed; auto-downgrade last, gated on P8 PASS; flags default-off.
Placeholders: [DB Migration Directory], pipeline. Mappings: promotion path; flag mechanism.
Objective: environment-specific plan: per stage owner, checkpoint evidence, rollback trigger/procedure; wire enablement flags.
Tests: flag-off smoke per flag. Stop: plan approved.
```

```text
[GO-02] Shadow validation
Read: §10.4; playbook Section M (shadow stage). Invariant: disagreements fall in TWO classes (round 13) — EXPECTED CANCELLED semantic deltas (legacy has no such value: invariant-check + classify, never "fix") vs UNEXPLAINED (bug or mapping error — disposition each, no thresholds waved through); CLEAN = zero UNEXPLAINED.
Placeholders: [Metrics / Alerting Layer] [Request Status Persistence Layer]. Mappings: dual-write live.
Objective: soak-window comparison job (tuple-derived label vs legacy; derived step status vs legacy); itemized two-class disagreement report; fix + re-soak UNEXPLAINED ones only (round 13).
Tests: comparator detects seeded disagreement. Stop: clean soak report filed.
```

```text
[GO-03] Staged enablement
Read: playbook Section M (order + per-stage validation) §9.2 §18-1. Invariant: auto-downgrade enablement requires CT-02..05 PASS + observed-lag watchdog live + TL-5-derived trust age (not the stub).
Placeholders: all (config-driven). Mappings: GO-01 flags.
Objective: enable stage → validate → soak → proceed, per Section M.
Tests: stage checklists (existing suites). Stop: all stages enabled + soaked; sign-offs recorded.
```

```text
[GO-04] Go-live gates
Read: playbook Section Q; §18 (BLOCKING items); 25-golive V.2/V.3. Invariant: TWO non-waivable classes (round 16) — §18 items 0–3 AND MONEY_SAFETY_BLOCKING (Q5/Q8/Q9/Q11/Q12/Q14/Q16/Q17/Q27/Q29-minimal): FAIL or missing evidence = NO-GO, no owner+plan waiver; every PASS carries linked evidence; manifest.yaml + SHA256SUMS must target the exact RC and environment with no stale entries in the invalidation map — incl. the Q5 CUTOVER_POPULATION_GREENFIELD RUN-2 proof (round 18: re-run post-fence at cutover, counts zero, bound; a D-12 snapshot alone is NOT cutover evidence).
Placeholders: none. Mappings: none.
Objective: execute Section Q; deliver evidence pack; obtain signed go/no-go.
Tests: none. Stop: decision recorded.
```

```text
[GO-05] Rollback validation
Read: playbook Section M (rollback) §16.5. Invariant: rollback is flags-off + state stands; no down-migration of money data; old version must run against final schema.
Placeholders: [DB Migration Directory], pipeline. Mappings: GO-01 stages.
Objective: rehearse app-rollback during dual-run, per-stage flag-off (incl. mid-incident under load), document the point of no return.
Tests: rehearsal scripts where automatable. Stop: rehearsals recorded.
```

