> **Purpose:** Minimal context packets GO-01..GO-05 — paste-alone briefs for a small-context local agent (original Section I, phase P14).
> **When to use this file:** Paired with the matching task-card file 08-task-cards/14-rollout-and-go-live.md — one packet per task, used as the working brief.
> **Depends on:** 09-minimal-context-packets/README.md; the matching task card; the requirement sections each packet cites; 07-placeholder-glossary.md.
> **Used by:** The local coding agent executing phase P14.
> **Safe to transfer:** yes
> **Contains local code names:** no

# Minimal Context Packets — Phase P14

```text
[GO-01] Rollout plan
Read: playbook Section M (incl. M.2 F0 + activation window); §16.5 §18. Invariant: Section M's stage ORDER is fixed; auto-downgrade last, gated on P8 PASS; flags default-off; the plan NAMES the F0 activation boundary + its owner (a local traffic gate, or the external routing/production action — round 19) and schedules the CUTOVER_POPULATION_GREENFIELD RUN 2 inside the F0 window (fence → queries → ZERO → sign → enable; nonzero = STOP).
Placeholders: [DB Migration Directory], pipeline. Mappings: promotion path; flag mechanism.
Objective: environment-specific plan: per stage owner, checkpoint evidence, rollback trigger/procedure; wire enablement flags incl. F0; round-19 card order: 01 → 02 → 05 → 04 → 03.
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
[GO-03] Controlled cutover + staged enablement (round 19: LAST card — runs only on GO-04's CONDITIONAL GO)
Read: playbook Section M (M.4 THREE SEGMENTS + M.2 F0 window) §9.2 §18-1; file 26 T.1. Invariant: M.4's segment order is BINDING (round 20) — PRE-TRAFFIC with F0 OFF (F5 freeze in BLOCK mode + tested; observability/runbooks/on-call live; readers + catch-up complete) BEFORE the ATOMIC ACTIVATION window (prevent legacy creation → verify writer fence → reviewed RUN-2 queries → ZERO → Q5b := PASS + DBA/TL sign → enable F0/named external action → verify the FIRST admitted row carries watermark + pointer + digest) BEFORE the POST-TRAFFIC stages (F1 → F2 → F3 → F4 last, each soaked); F0 never opens on a zero count alone; nonzero = ABORT the window, never waive; a soak feeding a non-waivable Q item is never waived.
Placeholders: all (config-driven). Mappings: GO-01 flags incl. F0 owner.
Objective: M.4 segments in order → the atomic-activation check verifies the FIRST admitted row (watermark + pointer + digest) AND the FIRST post-F0 payment_request row (NON-NULL required_total_at_creation + NON-NULL request_seq; no-sample rule = manifest item FIRST_REQUEST_CREATION_COLUMNS=PENDING_SAMPLE, MANUAL closure only (58f5a64 L1): durable ticket OPENED BEFORE closure signs (owner ops + SLA + FROZEN deterministic query/checksum per file 25 V.2 item 3 + F0 timestamp); ops re-runs on cadence; append-only v3 PASS or FAILED_INCIDENT_OPEN (bad row observed — append-only evidence, incident disposition required, later good rows never cure it; requalification = new signed boundary + new sample + disposition + further manifest version — 289ef66 L1); ticket closes only after the closing version signs; the ONLY item that may remain open at evidence closure), all filed in the evidence pack beside the signed F0 timestamp → post-enable verification + evidence closure (transcript + signed RUN-2 in the pack; signoffs.md entry; CUTOVER_POPULATION_GREENFIELD Q5b closed).
Tests: stage checklists (existing suites). Stop: all stages enabled + soaked; post-enable verification filed; PLAYBOOK COMPLETE.
```

```text
[GO-04] Go-live gates
Read: playbook Section Q; §18 (BLOCKING items); 25-golive V.2/V.3. Invariant: TWO non-waivable classes (round 16) — §18 items 0–3 AND MONEY_SAFETY_BLOCKING (Q5a/Q5b/Q8/Q9/Q11/Q12/Q14/Q16/Q17/Q27/Q29-minimal): FAIL or missing evidence = NO-GO, no owner+plan waiver — EXCEPT Q5b, which may stand as PENDING-CUTOVER when Q5a is PASS (the defined pre-cutover state, round 20; GO-03 converts it to PASS inside the F0 window); every PASS carries linked evidence; manifest.yaml + SHA256SUMS must target the exact RC and environment with no stale entries in the invalidation map — incl. the Q5 CUTOVER_POPULATION_GREENFIELD RUN-2 proof (round 18: re-run post-fence at cutover, counts zero, bound; a D-12 snapshot alone is NOT cutover evidence).
Placeholders: none. Mappings: none.
Objective: execute Section Q; deliver evidence pack; obtain the signed PRE-CUTOVER go/no-go (round 19: CONDITIONAL on RUN 2 = zero in GO-03's window — Q5b PENDING-CUTOVER → PASS there; GO-05 rehearsal already recorded — Q23).
Tests: none. Stop: decision recorded; next card GO-03.
```

```text
[GO-05] Rollback validation
Read: playbook Section M (rollback) §16.5. Invariant: rollback is flags-off + state stands; no down-migration of money data; old version must run against final schema.
Placeholders: [DB Migration Directory], pipeline. Mappings: GO-01 stages.
Objective: rehearse app-rollback during dual-run, per-stage flag-off (incl. mid-incident under load), document the point of no return. Round 19: this card runs BEFORE GO-04 (its report is Q23's evidence and a GO-04 prerequisite).
Tests: rehearsal scripts where automatable. Stop: rehearsals recorded; next card GO-04.
```

