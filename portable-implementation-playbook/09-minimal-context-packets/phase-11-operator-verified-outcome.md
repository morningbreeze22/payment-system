> **Purpose:** Minimal context packets OP-01..OP-03 + OP-04a..OP-04e — paste-alone briefs for a small-context local agent (original Section I, phase P11).
> **When to use this file:** Paired with the matching task-card file 08-task-cards/11-operator-verified-outcome.md — one packet per task, used as the working brief.
> **Depends on:** 09-minimal-context-packets/README.md; the matching task card; the requirement sections each packet cites; 07-placeholder-glossary.md.
> **Used by:** The local coding agent executing phase P11.
> **Safe to transfer:** yes
> **Contains local code names:** no

# Minimal Context Packets — Phase P11

```text
[OP-01] Verified-outcome operation (authorized application endpoint)
Read: §9.3 (operation + approval workflow) §10.1 §10.3 §20-8; CA-9; mechanics SHAPE-PROC. Invariant: Java endpoint calling the SHARED transition helpers (never PL/SQL — 2026-07-11 boundary); execution input = approval_id ONLY (identities DERIVED from the §9.3 approval record — round 4); SINGLE-TRANSITION op → APPROVED→CONSUMED CAS + payment transition commit in ONE transaction (refusal rolls back both; round 5: this rule is for single-transition ops — reprocess consume-at-start lives in OP-04); evidence flag set legitimately (S-06 session context); refuses CLAIMED/terminal/mismatch; every use alerts.
Placeholders: [Operator Admin Procedure Area] [Stored Procedure / Trigger Area] (triggers = backstop). Mappings: ops-schema approval store deployed (CA-9); enterprise session identities reach the app (else BLOCKED).
Objective: implement CA-9 exactly; EXECUTED → RG-03 path; REJECTED → REJECTED+marker+release; §14 line trigger_source=OPS_PLATFORM_VERIFIED + ticket; endpoint restricted to the enterprise ops role.
Tests: in OP-02. Stop: deployed to test env.
```

```text
[OP-02] Operation tests
Read: §9.3 §10.3; CA-9. Invariant: raw SQL fails where the operation succeeds (trigger demonstrated); lane runs the REAL triggers.
Placeholders: [Integration Test Suite] [Operator Admin Procedure Area]. Mappings: Oracle lane + operation endpoint.
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
[OP-04a] Shared ops-endpoint contract + retry/reject/annotate (round-9 pre-split 1/5)
Read: §20 (items 1/4/8) §9.3 (approval workflow) §10.1 §10.5 §14; mechanics M1/M8 SHAPE-PROC. Invariant: ONE shared contract for all OP-04x endpoints — enterprise auth, restricted role, operator+reason+ticket enforced, §9.3 approval adapter (execution by approval_id; approver ≠ initiator; single-use), shared CAS/money helpers only (never a private UPDATE path); retry/reject are 4-eyes.
Placeholders: [Operator Admin Procedure Area] [Obligation Repository]. Mappings: OP-01 auth pattern.
Objective: retry-blocked (L7, same stage; POST exits re-check repost_permitted) / reject-blocked (NOT_SUBMITTED only, L9 marker, RG-02 release) / annotate (display-only).
Tests: T-33 subset — MAYBE-reject refused at code AND trigger layer; missing ticket/role/identical identities refused; §14 line per call. Stop: merged; SHAPE-PROC ticked.
```

```text
[OP-04b] Reprocess APPROVAL side (round-9 pre-split 2/5)
Read: §9.3 (binding + display) §6.7 (tie + executability) §6.0 (transport) §16.3 (masking). Invariant: approvals authorize CONTENT — initiation fetches the snapshot BY ID, validates it, computes the canonical digest (the ONE shared implementation) and binds (business_id, xml id/version, digest, ticket, expiry, UNIQUE nonce); approver display = digest + MASKED diff + the §6.1 supersession notice; no payment state touched.
Placeholders: [Operator Admin Procedure Area]. Mappings: IN-02 tie record + XML fetch path + admission gate VERIFIED (else reopen IN-02 first).
Objective: initiation endpoint (xmlStorageId, reason, ticketRef) → pending §9.3 approval; second-approver display.
Tests: digest binds the fetched document; purged id at initiation → clean refusal, no pending approval; masked diff never shows full accounts; notice present. Stop: merged.
```

```text
[OP-04c] Reprocess EXECUTION (round-9 pre-split 3/5)
Read: §20-10 (whole) §9.3 (consume-at-start + completion evidence) §6.1 (ADMISSION + fence + supersession) §6.4/§6.5/§6.8. Invariant: input = approval_id ONLY; digest re-verified → HARD refusal BEFORE consumption or locks (refusal burns nothing); CONSUMED committed ALONE before fan-out (crash = NEW approval, never resurrect); §6.1 admission entry (≥ relaxation iff == trade watermark ∧ digest differs; older refused even approved; admission update IS the application for reference-only ties); every block passes the trade-snapshot FENCE (overtaken → abandoned per block-level supersession, logged + counted); completed_at + per-block summary stamped after the last block (own tx); §15 consumed-without-completion alert + runbook; money guards unchanged; no caller-supplied ordering.
Placeholders: [Operator Admin Procedure Area] [Obligation Repository] [Metrics / Alerting Layer]. Mappings: none beyond OP-04a/b.
Objective: the non-waivable tie exit, crash-safe and never silent.
Tests: T-33 reprocess core (digest mismatch pre-consumption; crash-after-consume → NEW approval; overtaken → fence abort + alert + stale re-approval refused; stamp-crash → false alert only; artifact-6(d)/(e); latch; purged id; re-run no-ops). Stop: merged; SHAPE-PROC ticked.
```

```text
[OP-04d] Queue views + authz/plan tests (round-9 pre-split 4/5)
Read: §15 (queues) §12 (read semantics) §10.4; mechanics M8 SHAPE-READ. Invariant: READ-ONLY; artifact-4 ACTIVE-row-bounded indexes; never keyed on display labels or blocked_reason-as-rule; restricted role.
Placeholders: [Obligation Repository] [Metrics / Alerting Layer]. Mappings: view deployment target.
Objective: BLOCKED-by-reason (ESCALATED first) / stuck reservations / aged MAYBE / overpay latches (round 10: cutoff-proximity column + pointer-residue view both REMOVED); §15 alerts link to each.
Tests: T-33 views subset — ranking, one-row-per-obligation (§12), EXPLAIN plans on terminal-heavy seed, unauthorized role refused. Stop: merged; SHAPE-READ ticked.
```

```text
[OP-04e] Cross-path integration suite + evidence (round-9 pre-split 5/5)
Read: T-33 (whole) T-35 (reprocess entry) artifact 6 (d)/(e)/(f). Invariant: REAL Oracle, REAL triggers; failures reopen the owning sub-card; evidence filed with run IDs, never retroactively.
Placeholders: [Integration Test Suite]. Mappings: Oracle lane.
Objective: full T-33 across all OP-04x paths + T-35 reprocess-entry cases; Q29 evidence pack filed.
Tests: the suite (any T-33 case not yet covered). Stop: merged; evidence filed.
```

