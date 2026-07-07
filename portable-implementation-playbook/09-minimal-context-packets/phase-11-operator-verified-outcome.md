> **Purpose:** Minimal context packets OP-01..OP-03 — paste-alone briefs for a small-context local agent (original Section I, phase P11).
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

