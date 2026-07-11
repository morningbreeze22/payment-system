> **Purpose:** Minimal context packets OP-01..OP-04 — paste-alone briefs for a small-context local agent (original Section I, phase P11).
> **When to use this file:** Paired with the matching task-card file 08-task-cards/11-operator-verified-outcome.md — one packet per task, used as the working brief.
> **Depends on:** 09-minimal-context-packets/README.md; the matching task card; the requirement sections each packet cites; 07-placeholder-glossary.md.
> **Used by:** The local coding agent executing phase P11.
> **Safe to transfer:** yes
> **Contains local code names:** no

# Minimal Context Packets — Phase P11

```text
[OP-01] Verified-outcome operation (authorized application endpoint)
Read: §9.3 (operation) §10.1 §10.3 §20-8; CA-9; mechanics SHAPE-PROC. Invariant: Java endpoint calling the SHARED transition helpers (never PL/SQL reimplementation — 2026-07-11 boundary); dual control enforced IN the operation (two distinct enterprise-authenticated identities); evidence flag set legitimately (S-06 session context); refuses CLAIMED/terminal/mismatch; every use alerts.
Placeholders: [Operator Admin Procedure Area] [Stored Procedure / Trigger Area] (triggers = backstop). Mappings: enterprise auth delivers two identities to the app (else BLOCKED).
Objective: implement CA-9 exactly; EXECUTED → RG-03 path; REJECTED → REJECTED+marker+release; §14 line trigger_source=OPS_PLATFORM_VERIFIED + ticket; endpoint restricted to the enterprise ops role.
Tests: in OP-02. Stop: deployed to test env.
```

```text
[OP-02] Procedure tests
Read: §9.3 §10.3; CA-9. Invariant: raw SQL fails where the procedure succeeds (trigger demonstrated); lane runs the REAL triggers.
Placeholders: [Integration Test Suite] [Operator Admin Procedure Area]. Mappings: Oracle lane + procedure.
Objective: both outcomes' money effects; all refusals; dual-control; guard interplay; wedge-opens assertion (scope completes / successor creates).
Tests: the suite. Stop: green on real Oracle; evidence filed.
```

```text
[OP-03] Ops drill
Read: §18-3 §20-8; CA-9 drill script. Invariant: drilled by REAL operators with the restricted role and a real ticket reference.
Placeholders: [Operator Admin Procedure Area]. Mappings: drill environment.
Objective: seed an unresolvable MAYBE row; run the full drill; verify outcome/alert/log; file the signed report.
Tests: none (drill). Stop: signed report; §18-3 satisfiable in Section Q.
```

```text
[OP-04] Interim ops surface (§20 endpoint set + queue views)
Read: §20 (minimal exit set + items 1/4/8/10) §6.7 (tie, round 3 — server-verified) §6.0 (transport note) §10.1 §9.3 (approval workflow); ops-console-proposal §6.1; mechanics M1/M8. Invariant: AUTHORIZED application endpoints calling the shared CAS/money helpers only (2026-07-11 boundary); contract ENFORCES operator+reason+ticket + the §9.3 two-step approval workflow on retry/reject/reprocess (approver ≠ initiator, bound + single-use); reprocess-snapshot takes xml_id ONLY — payload never a parameter, NO caller-supplied ordering: the server RECOMPUTES the tie (≥ applies iff fetched ordering == upstream_ordering ∧ payload differs).
Placeholders: [Operator Admin Procedure Area] [Stored Procedure / Trigger Area] (triggers = backstop) [Obligation Repository] [Metrics / Alerting Layer]. Mappings: OP-01 auth pattern; IN-02 tie record carries business_id + tied ordering + XML id + masked diff (else reopen IN-02 first).
Objective: retry-blocked (L7, same stage) / reject-blocked (NOT_SUBMITTED only, L9 marker, release) / annotate / reprocess-snapshot(xml_id) server-verified via normal §6.1 fan-out (§6.4/§6.5/§6.8 guards unchanged; 4-eyes always); four queue views (BLOCKED-by-reason ESCALATED-first, stuck, aged-MAYBE, overpay) on artifact-4 indexes; §15 alerts link to them. NOTE: supersede/close (RG-05) + reprocess-snapshot are NON-WAIVABLE (§20 minimal exit set); retry/reject/annotate/views are Q29-waivable.
Tests: T-33 incl. MAYBE-reject refused at code AND trigger layer; reprocess idempotent + latch-respecting + purged-xml clean refusal + non-tying/fabrication refusal. Stop: merged; SHAPE-PROC+READ ticked; Q29 evidence filed.
```

