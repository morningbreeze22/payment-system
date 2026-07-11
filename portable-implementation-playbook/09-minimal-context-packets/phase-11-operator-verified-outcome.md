> **Purpose:** Minimal context packets OP-01..OP-04 — paste-alone briefs for a small-context local agent (original Section I, phase P11).
> **When to use this file:** Paired with the matching task-card file 08-task-cards/11-operator-verified-outcome.md — one packet per task, used as the working brief.
> **Depends on:** 09-minimal-context-packets/README.md; the matching task card; the requirement sections each packet cites; 07-placeholder-glossary.md.
> **Used by:** The local coding agent executing phase P11.
> **Safe to transfer:** yes
> **Contains local code names:** no

# Minimal Context Packets — Phase P11

```text
[OP-01] Verified-outcome procedure
Read: §9.3 (procedure) §10.1 §10.3 §20-8; CA-9. Invariant: dual control enforced IN the procedure; evidence flag set legitimately; refuses CLAIMED/terminal/mismatch; every use alerts; applies through the SAME evidence-guarded CAS.
Placeholders: [Operator Admin Procedure Area] [Stored Procedure / Trigger Area]. Mappings: role model can produce two authenticated identities (else BLOCKED).
Objective: implement CA-9 exactly; EXECUTED → RG-03 path; REJECTED → REJECTED+marker+release; §14 line trigger_source=OPS_PLATFORM_VERIFIED + ticket; restricted role.
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
[OP-04] Interim ops surface (§20 procedure set + queue views)
Read: §20 (interim set + items 1/4/8/10) §6.7 (tie + executability) §10.1; ops-console-proposal §6.1; mechanics M1/M8. Invariant: shared CAS/money helpers only; signature ENFORCES operator+reason+ticket (+2nd approver on retry/reject/tie-apply); tie payload from the RECORD, never a parameter; ≥ relaxation for exactly the recorded tied ordering.
Placeholders: [Operator Admin Procedure Area] [Stored Procedure / Trigger Area] [Obligation Repository] [Metrics / Alerting Layer]. Mappings: OP-01 role mechanics; IN-02 tie record carries the payload (else reopen IN-02 first).
Objective: ops_retry_blocked (L7, same stage) / ops_reject_blocked (NOT_SUBMITTED only, L9 marker, release) / ops_annotate / ops_apply_tied_amendment (sorted per-block fan-out, §6.4/§6.5/§6.8 guards unchanged); four queue views (BLOCKED-by-reason ESCALATED-first, stuck, aged-MAYBE, overpay) on artifact-4 indexes; §15 alerts link to them.
Tests: T-33 incl. MAYBE-reject refused at code AND trigger layer; tie-apply idempotent + latch-respecting. Stop: merged; SHAPE-PROC+READ ticked; Q29 evidence filed.
```

