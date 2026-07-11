> **Purpose:** Go-live readiness checklist Q1-Q29 with PASS/FAIL/BLOCKED and evidence columns; the four §18 BLOCKING items (Q1-Q4, Q28) are non-waivable (original Section Q).
> **When to use this file:** Executed at GO-04; tracked continuously from Phase P8 onward.
> **Depends on:** All phase outputs; 10-test-matrix.md; 11-provider-techlead-po-questions.md.
> **Used by:** GO-04 go/no-go decision.
> **Safe to transfer:** yes
> **Contains local code names:** no

# Q. Go-live readiness checklist

Execute at GO-04. Every PASS carries linked evidence (test run,
report, signed document). §18 BLOCKING items are non-waivable; other
FAILs need a named owner and dated plan to proceed as risks.
**HOW to verify each row — the concrete check, the required evidence
artifact, and who signs — is 25-golive-verification-procedures.md
(V.1); the evidence-pack layout is V.2 and the GO-04 meeting script
is V.3. Evidence is filed per task at completion time (rule 18), not
collected retroactively at GO-04.**

| # | Item | Source | PASS/FAIL/BLOCKED | Evidence |
|---|------|--------|-------------------|----------|
| Q1 | §18 BLOCKING item 0 residue closed: written snapshot-contract confirmation (upstream ask 5); §6.0 within-snapshot uniqueness intake validation live; PO-9 (absence semantics) and TL-16 (ordering watermark) answered and implemented | §18-0, B-01 | | |
| Q2 | §18 BLOCKING item 1: sandbox collision matrix (a)–(d) EXECUTED and PASSED; re-run procedure scheduled for engine releases | §18-1, CT-02..05 | | |
| Q3 | §18 BLOCKING item 2: cutoff calendar sourced, owned, tz-aware, refresh + fail direction configured | §18-2, B-03 | | |
| Q4 | §18 BLOCKING item 3: apply-platform-verified-outcome procedure EXISTS (OP-01/02) AND DRILLED (OP-03) — or TL-10 ∧ TL-5 alternative affirmed in writing + PO re-confirmation | §18-3, B-04 | | |
| Q5 | Schema at CA-4 target: constraints VALIDATED, triggers live, indexes in place; migration test pass green (incl. dual-run) | S-05..09 | | |
| Q6 | Factored state model implemented: dual-write live, CAS discipline audited, legality suite green | ST-01..03 | | |
| Q7 | Legacy status not used for business rules: ST-05 inventory empty or fully dispositioned; display via derived labels only | ST-04/05 | | |
| Q8 | Idempotency key generation deterministic + persisted write-ahead; K-06 crash/retry/restore set green | K-01..06, T-03/08/09/10 | | |
| Q9 | Identity golden-vector tests green and frozen in the build | K-03, T-02 | | |
| Q10 | Provider idempotency sandbox tests green (same as Q2, listed for the test-evidence pack) + SDK contract checks (CT-07) recorded | CT suite, T-11..14 | | |
| Q11 | Duplicate-prevention tests green (I6, UNIQUE key, engine-dedup routing) | T-17, S-05 | | |
| Q12 | Retry / crash / restore recovery tests green | T-08/09/10, ST-10 | | |
| Q13 | Cutoff calendar configured and validated in the target environment (tz + holiday spot checks) | B-03, T-21 | | |
| Q14 | MAYBE_SUBMITTED recovery lifecycle tests green (resolver, trust-age, downgrade, escalation, parked rows) | T-22/23, RC-05..08 | | |
| Q15 | apply-platform-verified-outcome test suite + drill report on file | T-24, OP-02/03 | | |
| Q16 | Reservation release / confirmation correctness green (I1–I6, redelivery safety, overpay latch) | T-26/27, RG-01..04 | | |
| Q17 | Evidence session flag / release guard validated (code + trigger layers; pool non-leakage) | T-25, S-06, RG-05 | | |
| Q18 | Reconciliation tripwires live (terminal-evidence CRITICAL, count sanity) | T-30, OB-02 | | |
| Q19 | Drift scanner live, paging, read-skew-safe | T-29, OB-01 | | |
| Q20 | Observability dashboards + alerts live per §15 with runbook links; rollup verified; config ordering validation active | T-32, OB-03..07 | | |
| Q21 | Runbook stubs published (CA-8) incl. the aged-MAYBE runbook | CA-8, OB-06 | | |
| Q22 | Backwards compatibility with existing payment logic: D-11 baseline green at the release candidate; no BUSINESS_RULE_CHANGE_REQUIRED unresolved | D-11, M.8 | | |
| Q23 | Migration/rollout/rollback plan approved; rollback rehearsed; point of no return documented | GO-01/05, Section M | | |
| Q24 | Shadow validation soak report clean | GO-02 | | |
| Q25 | Tech-lead / provider / PO question register (Section K) current: all BLOCKING answered; HIGH answered or risk-owned; §16.6 config values have owners | Section K, OB-07 | | |
| Q26 | UI/card correctness tests green (no false completion; §12 multi-obligation lookup) | T-31 | | |
| Q27 | Kafka hardening compliant per §16.2 checklist in all target environments | IN-09 | | |
| Q28 | ALL §18 BLOCKING items resolved — final aggregate check before go-live | §18, Q1–Q4 | | |
| Q29 | §20 interim ops surface live: guarded procedures (retry / reject / supersede / annotate / tie-apply) + four queue views deployed with restricted-role grants and exercised on real Oracle | §20, RG-05, OP-04, T-33 | | |

