> **Purpose:** Go-live readiness checklist Q1-Q31 with PASS/FAIL/BLOCKED states — plus ONE additional defined state, PENDING-CUTOVER, legal ONLY for Q5b at GO-04 (round 20) — and evidence columns; TWO non-waivable classes (round 16): the §18 BLOCKING items (Q1-Q4, Q28) AND the MONEY_SAFETY_BLOCKING set (Q5a, Q5b, Q8, Q9, Q11, Q12, Q14, Q16, Q17, Q27, Q29-minimal) (original Section Q).
> **When to use this file:** Executed at GO-04; tracked continuously from Phase P8 onward.
> **Depends on:** All phase outputs; 10-test-matrix.md; 11-provider-techlead-po-questions.md.
> **Used by:** GO-04 go/no-go decision.
> **Safe to transfer:** yes
> **Contains local code names:** no

# Q. Go-live readiness checklist

Execute at GO-04. Every PASS carries linked evidence (test run,
report, signed document). TWO non-waivable classes (round 16): §18
BLOCKING (Q1–Q4, Q28) AND MONEY_SAFETY_BLOCKING (Q5a, Q5b, Q8, Q9, Q11, Q12, Q14, Q16, Q17, Q27, and Q29's minimal exit set —
the duplicate-payment / wrong-release controls; FAIL or missing
evidence = NO-GO; reclassification requires a safety review).
Round 20: Q5b's PENDING-CUTOVER is a DEFINED state, not missing
evidence and not a waiver — legal only at GO-04, only while Q5a is
PASS; GO-03 converts it to PASS inside the F0 window. Only
items outside both classes may proceed as owned, dated risks.
**HOW to verify each row — the concrete check, the required evidence
artifact, and who signs — is 25-golive-verification-procedures.md
(V.1); the evidence-pack layout is V.2 and the GO-04 meeting script
is V.3. Evidence is filed per task at completion time (rule 18), not
collected retroactively at GO-04.**

| # | Item | Source | PASS/FAIL/BLOCKED | Evidence |
|---|------|--------|-------------------|----------|
| Q1 | §18 BLOCKING item 0 residue closed: WRITTEN filings of ask 5 (snapshot schema + uniqueness) and ask 8 (store contract incl. IMMUTABILITY) — both CONFIRMED verbally 2026-07-11, the filed papers are the evidence; §6.0 intake validation live; PO-9 (absence = amendment to zero) IMPLEMENTED per §6.1; TL-16 round 5 (admission — S-10/IN-02, T-35 green) | §18-0, B-01 | | |
| Q2 | §18 BLOCKING item 1: sandbox collision matrix (a)–(d) EXECUTED and PASSED; re-run procedure scheduled for engine releases | §18-1, CT-02..05 | | |
| Q3 | §18 item 2 CLOSED (round 10 — the engine owns its cutoff calendar; verify the CA-1 table carries the engine's late-submission response class + the written any-time-submission line) | §18-2 (closed), CA-1 | | |
| Q4 | §18 BLOCKING item 3: apply-platform-verified-outcome OPERATION (authorized application endpoint) EXISTS (OP-01/02) AND DRILLED (OP-03) — or TL-10 ∧ TL-5 alternative affirmed in writing + PO re-confirmation | §18-3, B-04 | | |
| Q5a | Schema at CA-4 target: constraints VALIDATED, triggers live, indexes in place; migration test pass green (incl. dual-run); T-35/T-37 green; the CUTOVER_POPULATION_GREENFIELD RUN-2 queries + scope predicate REVIEWED and manifest-bound (round 20) — ALL PASS before the GO-04 authorization | S-05..09, file 26 T.1 | | |
| Q5b | Time-of-cutover CUTOVER_POPULATION_GREENFIELD RUN 2 (file 26 T.1): the reviewed queries re-run inside GO-03's F0 window AFTER in-scope writer drain/fence, counts ZERO, DBA/TL signatures, manifest closure — an empty trade_snapshot_state alone proves nothing about pre-existing in-scope obligations. State at GO-04 = PENDING-CUTOVER (defined state, NOT a waiver; legal only while Q5a is PASS); GO-03 converts it to PASS BEFORE F0 flips; a nonzero count or incomplete signature ABORTS the change window (round 20) | GO-03, file 26 T.1 | | |
| Q6 | Factored state model implemented: dual-write live, CAS discipline audited, legality suite green | ST-01..03 | | |
| Q7 | Legacy status not used for business rules: ST-05 inventory empty or fully dispositioned; display via derived labels only | ST-04/05 | | |
| Q8 | Idempotency key generation deterministic + persisted write-ahead; K-06 crash/retry/restore set green | K-01..06, T-03/08/09/10 | | |
| Q9 | Identity golden-vector tests green and frozen in the build | K-03, T-02 | | |
| Q10 | Provider idempotency sandbox tests green (same as Q2, listed for the test-evidence pack) + SDK contract checks (CT-07) recorded | CT suite, T-11..14 | | |
| Q11 | Duplicate-prevention tests green (I6, UNIQUE key, engine-dedup routing) | T-17, S-05 | | |
| Q12 | Retry / crash / restore recovery tests green | T-08/09/10, ST-10 | | |
| Q13 | CLOSED round 10 — no local cutoff calendar exists (engine-owned, §18-2); verify no cutoff machinery crept into the target env config | §18-2 (closed) | | |
| Q14 | MAYBE_SUBMITTED recovery lifecycle tests green (resolver, trust-age, downgrade, escalation, parked rows) | T-22/23, RC-05..08 | | |
| Q15 | apply-platform-verified-outcome test suite + drill report on file | T-24, OP-02/03 | | |
| Q16 | Reservation release / confirmation correctness green (I1–I6, redelivery safety, overpay latch) | T-26/27, RG-01..04 | | |
| Q17 | Evidence session flag / release guard validated (code + trigger layers; pool non-leakage) | T-25, S-06, RG-05 | | |
| Q18 | Reconciliation tripwires live (terminal-evidence CRITICAL, count sanity, both post-F0 creation-column scans: NULL stamp → ticket, NULL request_seq → alert — 6cb3005 L1) | T-30, OB-02 | | |
| Q19 | Drift scanner live, paging, read-skew-safe | T-29, OB-01 | | |
| Q20 | Observability dashboards + alerts live per §15 with runbook links; rollup verified; config ordering validation active | T-32, OB-03..07 | | |
| Q21 | Runbook stubs published (CA-8) incl. the aged-MAYBE runbook | CA-8, OB-06 | | |
| Q22 | Backwards compatibility with existing payment logic: D-11 baseline green at the release candidate; no BUSINESS_RULE_CHANGE_REQUIRED unresolved | D-11, M.8 | | |
| Q23 | Migration/rollout/rollback plan approved; rollback rehearsed; point of no return documented | GO-01/05, Section M | | |
| Q24 | Shadow validation soak report clean | GO-02 | | |
| Q25 | Tech-lead / provider / PO question register (Section K) current: all BLOCKING answered; HIGH answered or risk-owned; §16.6 config values have owners | Section K, OB-07 | | |
| Q26 | UI/card correctness tests green (no false completion; §12 multi-obligation lookup; amount-series stamp: RG-06 creation-stamp suite green + T-31 projection stamps/NULL rendering — PRE-CUTOVER evidence ONLY, aa4399c M1: the first-post-F0 stamp check is GO-03 POST-ENABLE evidence and is NEVER a Q26 criterion, since Q26 is evaluated at GO-04 before F0 exists) | T-31, RG-06 stamp suite | | |
| Q27 | Kafka hardening compliant per §16.2 checklist in all target environments | IN-09 | | |
| Q28 | ALL §18 BLOCKING items resolved — final aggregate check before go-live | §18, Q1–Q4 | | |
| Q29 | §20 interim ops surface live: authorized admin endpoints + four queue views deployed, enterprise-role-restricted, exercised on real Oracle. NON-WAIVABLE line items (§20 minimal exit set, with Q4): supersede/close and reprocess-snapshot. Waivable only by PO with owner + dated plan: retry, reject, annotate, views | §20, RG-05, OP-04a–e, T-33 | | |
| Q30 | Security/supply-chain gate (round 16) on the EXACT RC: SAST + dependency vulnerability scan + SBOM + license policy + secret scan; Kafka ACLs, DB grants, service/ops-role least privilege, endpoint authn/authz, TLS config + certificate rotation; masking tests across success AND failure paths (logs, traces, metric tags, exceptions, dead letters); config/secret provenance from the sanctioned vault; §14.1 journal protections verified (INSERT-only grants, restricted audit role, DB-audited reads, no lower-env replication) AND the T-38 switch-OFF INERTNESS sub-case green (case F: OFF ⇒ zero inserts/zero errors/posting unaffected — the ONE T-38 piece that gates PAYMENT go-live, since the rider code ships while OFF; 1d8a650 M2) AND the §14.1 ENABLEMENT GATE state recorded: journal writes stay OFF unless encryption at rest is ENABLED + evidenced (or an approved expiry-dated exception exists) AND the compliance-approved retention schedule is on file AND the FULL applicable T-38 A–J evidence set is green (never case G alone) — payments go-live never waits on the journal (never load-bearing) | §16.1, §16.3, §14.1, §14 | | |
| Q31 | Capacity gate (round 16): peak + post-outage burst test at the §16.5 volume NFR; connection-pool/bulkhead saturation behavior; scanner backlog recovery; provider quota shaping (TL-13 budget); card-read latency under load; resource alarms + recorded headroom under the tested RC configuration | §16.1, §16.5, §9.5 | | |

