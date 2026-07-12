> **Purpose:** Task cards GO-01..GO-05 (rollout, shadow validation, staged enablement, gates, rollback) (original Section H, phase P14).
> **When to use this file:** When executing the tasks of this phase, one card at a time, with the matching packet file from 09-minimal-context-packets/.
> **Depends on:** 08-task-cards/README.md; 01-playbook-index.md; 07-placeholder-glossary.md; the requirement sections cited per card; the locally filled mapping template.
> **Used by:** The local coding agent executing phase P14.
> **Safe to transfer:** yes
> **Contains local code names:** no

## H-Phase 14 (task group 13) — Rollout and go-live gates (P14)

### GO-01 — Rollout sequencing plan (executable)

- **Task ID:** GO-01
- **Title:** Turn Section M into the environment-specific rollout order with owners and checkpoints
- **Classification:** MVP normative implementation
- **Purpose:** §16.5 expand/contract + Section M's enablement order, made concrete for the local pipeline.
- **Prerequisites:** all P3–P13 phases merged in the target branch; S-09 dual-run proof.
- **Requirement sections / concepts to read:** Section M (this playbook), §16.5, §18 (gate status).
- **Placeholder components involved:** [DB Migration Directory], deployment pipeline.
- **Local placeholder mappings required before starting:** environment promotion path known.
- **Local code areas to discover:** feature-flag/config-toggle conventions (Section M's flags need a local mechanism — record which).
- **How to locate:** deployment repo/config.
- **Implementation instructions:** write the local rollout plan following Section M's stage order verbatim, with per-stage: owner, checkpoint evidence, rollback trigger + procedure; wire the Section M flags (new-writer dual-write is already structural; scanner enablement, resolver enablement, auto-downgrade enablement as config); carry the M.1a reader-first ladder stage explicitly (round 14): no release writes CANCELLED until reader-fleet compatibility is verified (discovery-proven N/A, or the compatibility release is deployed fleet-wide).
- **Do not change:** Section M's ORDER (auto-downgrade last, gated on P8 PASS).
- **Tests to add:** none (plan); flag-off behavior smoke tests per flag.
- **Edge cases:** two app versions during each stage (claim compatibility across one release boundary — §16.5).
- **Manual validation:** plan review with the release owner.
- **Expected outcome:** executable rollout plan.
- **Failure signs:** stages combined to save deploys (each checkpoint exists to bound blast radius).
- **Common mistakes:** flags default-on.
- **Completion criteria:** plan approved.
- **Stop condition:** approved.
- **Next task:** GO-02.

### GO-02 — Shadow validation

- **Task ID:** GO-02
- **Title:** Dual-run/shadow comparison: derived dimensions + labels agree with legacy status; derivation outputs agree with observed behavior
- **Classification:** MVP normative implementation
- **Purpose:** Section M's dry-run stage: prove the factored model tracks reality before any rule ENFORCEMENT relies on it in production. Round 12: the comparison table maps CANCELLED EXPLICITLY — legacy display has no such value, so CANCELLED rows are EXPECTED disagreements to be classified, and CANCELLED must NEVER be silently mapped to COMPLETED.
- **Prerequisites:** GO-01; production-like environment with dual-write live (ST-01).
- **Requirement sections / concepts to read:** §10.4 (mapping), Section M (shadow stage).
- **Placeholder components involved:** [Metrics / Alerting Layer] (comparison metric), [Request Status Persistence Layer].
- **Local placeholder mappings required before starting:** dual-write live in the environment.
- **Local code areas to discover:** none new.
- **How to locate:** n/a.
- **Implementation instructions:** a comparison job/report over a soak window: per row, tuple-derived label vs legacy status per the reviewed mapping; disagreements fall in TWO CLASSES (round 13): (1) EXPECTED SEMANTIC DELTAS — CANCELLED rows ONLY (legacy display has no such value): verify the row's invariants (required = 0, clean unwind), CLASSIFY in the report, never "fix"; (2) UNEXPLAINED disagreements — each is a dual-write bug or mapping-table error: fix, re-soak. CLEAN = ZERO UNEXPLAINED disagreements — never byte-for-byte legacy label parity. Completion-predicate shadow: RG-08's derived ui_step_status vs the legacy step status where observable.
- **Do not change:** production traffic; read-only comparison.
- **Tests to add:** the comparison tooling's own correctness (seeded disagreement detected); cutover fence assertion: a FENCED old-writer version attempting to reconnect is REJECTED (round 15).
- **Edge cases:** rows written by the OLD app version during dual-run (legacy-only) — the S-08 backfill mapping covers them; comparison must tolerate the window.
- **Manual validation:** soak report clean over the agreed window (owner-defined; record; clean = zero UNEXPLAINED disagreements, expected CANCELLED deltas classified — round 13); evidence: NO obligation row exposed to the card path carries NULL ui_step_status — the M.3 FENCED cutover ran before the read switch (round 15: writer fleet drained AND old versions fenced BEFORE the final catch-up pass; reader-fleet upgrade and writer-fleet drain recorded as SEPARATE evidence items).
- **Expected outcome:** factored model trusted.
- **Failure signs:** "small" disagreement rates waved through — every disagreement has a cause; disposition each.
- **Common mistakes:** comparing labels only (compare the tuple fields too).
- **Completion criteria:** clean soak report.
- **Stop condition:** report filed.
- **Next task:** GO-03.

### GO-03 — Staged enablement

- **Task ID:** GO-03
- **Title:** Enable in Section M order: constraints validated → guards/triggers → scanners → resolver → escalation → (after P8 PASS) auto-downgrade
- **Classification:** MVP normative implementation
- **Purpose:** each mechanism observes before it acts; the auto-downgrade (a money-adjacent self-heal) goes last, gated on the §18-1 proof.
- **Prerequisites:** GO-02 clean; per-stage prerequisites in Section M; P8 gate status for the final stage.
- **Requirement sections / concepts to read:** Section M (enablement order + per-stage validation), §9.2 (what the downgrade risks), §18-1.
- **Placeholder components involved:** all runtime components (config-driven).
- **Local placeholder mappings required before starting:** GO-01 flags wired.
- **Local code areas to discover:** none new.
- **How to locate:** n/a.
- **Implementation instructions:** per Section M's stage list: enable, run the stage's validation checklist (alerts quiet, metrics sane, sampled rows correct), hold the stage soak, proceed; auto-downgrade stage requires: CT-02..05 PASS evidence on file + observed-lag watchdog live (OB-04) + trust-age configured from TL-5 (not the stub).
- **Do not change:** stage order; no skipping soaks under schedule pressure (record any waiver with its owner).
- **Tests to add:** none new (checklists execute existing ones).
- **Edge cases:** a stage's validation failing → its documented rollback (flag off), fix forward, re-enter.
- **Manual validation:** stage sign-offs recorded.
- **Expected outcome:** system live in safe order.
- **Failure signs:** resolver enabled before evidence rules (IN-07) verified in the environment.
- **Common mistakes:** enabling the downgrade with the conservative stub trust-age (must be TL-5-derived by then).
- **Completion criteria:** all stages enabled + soaked.
- **Stop condition:** enablement complete.
- **Next task:** GO-04.

### GO-04 — Go-live gate execution

- **Task ID:** GO-04
- **Title:** Execute the Section Q checklist; assemble gate evidence; obtain go/no-go
- **Classification:** §18 BLOCKING gate aggregation
- **Purpose:** the four §18 BLOCKING items + all Q items PASS before first production payment under the new machinery.
- **Prerequisites:** GO-03; OP-03 drill; CT suite results; K-03 vectors; open-question register (Section K) current.
- **Requirement sections / concepts to read:** Section Q; §18 (all BLOCKING items).
- **Placeholder components involved:** none (evidence task).
- **Local placeholder mappings required before starting:** none.
- **Local code areas to discover:** none.
- **How to locate:** n/a.
- **Implementation instructions:** walk Section Q item by item: PASS with linked evidence / FAIL with owner + plan / BLOCKED with the blocking §18 item; §18 BLOCKING items are non-waivable (Playbook Index rule); deliver to the accountable owner for go/no-go.
- **Do not change:** checklist items (additions allowed; removals need the owner).
- **Tests to add:** none.
- **Edge cases:** unresolved non-BLOCKING Section K questions → recorded risks with owners, not silent passes.
- **Manual validation:** signed go/no-go.
- **Expected outcome:** auditable go-live decision.
- **Failure signs:** "PASS" without linked evidence.
- **Common mistakes:** treating written provider answers as CT evidence (§18-1: the TEST is the proof).
- **Completion criteria:** decision recorded.
- **Stop condition:** decision recorded.
- **Next task:** GO-05.

### GO-05 — Rollback validation

- **Task ID:** GO-05
- **Title:** Rehearse the rollback paths that remain legal at each stage; document the point of no return
- **Classification:** MVP normative implementation
- **Purpose:** Section M's constraint: money-affecting writes under new machinery bound what can be rolled back; rehearse BEFORE relying on it.
- **Prerequisites:** GO-01 plan; a production-like environment.
- **Requirement sections / concepts to read:** Section M (rollback constraints), §16.5.
- **Placeholder components involved:** [DB Migration Directory], deployment pipeline.
- **Local placeholder mappings required before starting:** GO-01 stages.
- **Local code areas to discover:** none new.
- **How to locate:** n/a.
- **Implementation instructions:** rehearse: app-version rollback during dual-run (must be clean — expand/contract guarantees it); flag-off rollback per enabled stage; document the no-return line per Section M (once terminal outcomes + money movements are being written by the new machinery, rollback = flags off + state stands; schema stays; no data un-migration); verify the old version still runs against the final schema (S-09's proof re-run on the release candidate).
- **Do not change:** migrations (rollback is forward-only by expand/contract design — no down-migrations of applied money data).
- **Tests to add:** the rehearsal scripts where automatable.
- **Edge cases:** rollback WHILE rows sit in new-only states (e.g. BLOCKED(ESCALATED)) — the old version must at minimum not corrupt them (it ignores unknown columns by expand/contract; verify the old version's writers don't blank new columns — S-09 dual-run proof covers; re-verify).
- **Manual validation:** rehearsal reports.
- **Expected outcome:** known-good rollback envelope.
- **Failure signs:** a rollback plan that requires down-migrating data.
- **Common mistakes:** rehearsing only the happy rollback (rehearse the mid-incident one: flags off under load).
- **Completion criteria:** rehearsals recorded.
- **Stop condition:** recorded; Phase P14 report. PLAYBOOK IMPLEMENTATION COMPLETE — operate per Section N runbooks.
- **Next task:** none (steady state).


---

## Phase handoff summary (P14 → steady state)

- **Phase outputs:** localized rollout plan executed in the 13-migration-rollout-rollback.md order; clean shadow-soak report; staged enablement with sign-offs (auto-downgrade last, on P8 PASS evidence); go-live checklist executed with linked evidence and a signed go/no-go; rollback rehearsed with the point of no return documented.
- **Blockers to carry forward:** none permitted at go-live — §18 items 0–3 (Q1–Q4, Q28) are non-waivable; open non-BLOCKING questions become owned risks.
- **Local mapping rows expected filled:** all rows final; the filled mapping + all execution reports REMAIN ON THE WORK LAPTOP.
- **Tests expected to exist:** the full catalog green at the release candidate; D-11 baseline green (backwards compatibility, Q22).
- **Next phase entry condition:** n/a — operate per the runbooks (14-observability-reconciliation-runbooks.md / CA-8).
