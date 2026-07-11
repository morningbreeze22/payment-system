> **Purpose:** Task cards OP-01..OP-04 (apply-platform-verified-outcome audited procedure — §18-3 — plus the §20 interim ops surface) (original Section H, phase P11).
> **When to use this file:** When executing the tasks of this phase, one card at a time, with the matching packet file from 09-minimal-context-packets/.
> **Depends on:** 08-task-cards/README.md; 01-playbook-index.md; 07-placeholder-glossary.md; the requirement sections cited per card; the locally filled mapping template.
> **Used by:** The local coding agent executing phase P11.
> **Safe to transfer:** yes
> **Contains local code names:** no

## H-Phase 11 — Operator verified-outcome procedure (P11)

### OP-01 — Implement the apply-platform-verified-outcome procedure

- **Task ID:** OP-01
- **Title:** Implement CA-9's audited verified-outcome OPERATION — an authorized application endpoint calling the shared transition service (dual control, evidence flag, refusal conditions, audit + alert; execution boundary decided 2026-07-11 — never a PL/SQL reimplementation; §10.3 triggers stay as backstop)
- **Classification:** §18 BLOCKING go-live gate (item 3) + MVP normative
- **Purpose:** the guaranteed terminal exit for otherwise-unresolvable MAYBE rows; the SINGLE sanctioned manual exception to §9.4.
- **Prerequisites:** CA-9 published; S-06 (evidence-flag mechanics live); RG-02/03 (money paths); ST-06 (normalization); B-04 recorded.
- **Requirement sections / concepts to read:** §9.3 (procedure block), §10.1, §10.3, §20-8, CA-9.
- **Placeholder components involved:** [Operator Admin Procedure Area], [Stored Procedure / Trigger Area].
- **Local placeholder mappings required before starting:** procedure area + restricted-role model Confirmed (D-10/D-20 findings); Oracle session-context mechanics from S-06.
- **Local code areas to discover:** how the ENTERPRISE ACCESS-MANAGEMENT identities reach the operation (mechanism DECIDED 2026-07-11, §9.3: each operator has a unique, non-bypassable identity supplied by the enterprise tooling; the operation verifies distinctness and records both — with the Java-endpoint boundary this is ordinary enterprise SSO/service auth, far simpler than DB-layer identity). Discovery confirms the plumbing only. If the enterprise tooling cannot deliver two authenticated identities to the application, BLOCKED — escalate; two free-text strings are non-compliant, never weaken to convention.
- **How to locate:** DBA/role model documentation (local).
- **Implementation instructions:** per CA-9: an AUTHORIZED ENDPOINT (enterprise-authenticated, restricted ops role) whose service-layer implementation calls the SHARED transition helpers (RG-02/RG-03/ST-06 — never a private update path). Inputs: request_id, outcome EXECUTED|REJECTED, ticket/evidence reference NOT NULL, two distinct enterprise-authenticated approver identities — the operation REFUSES identical or unauthenticated pairs. Inside ONE transaction: re-check row state (refuse CLAIMED, refuse terminal); set the evidence session flag (S-06 session-context, from the same JDBC session); EXECUTED → the RG-03 settlement path (amount equality enforced; +confirmed; SUB=SUBMITTED; outcome=EXECUTED; normalization); REJECTED → outcome=REJECTED + provider_rejected marker (L9) + release (RG-02) + normalization; emit the §14 line with trigger_source=OPS_PLATFORM_VERIFIED + ticket ref; raise the §15 every-use alert. Endpoint restricted to the enterprise ops role; unauthorized attempts refused and logged.
- **Do not change:** the trigger (passed legitimately, never disabled — §9.3); §9.4's single-exception framing.
- **Tests to add:** in OP-02 (next card).
- **Edge cases:** row becomes CLAIMED between the operator's check and execution — the in-transaction re-check refuses (assert in OP-02); amount mismatch on EXECUTED → refuse (that is the §8 defect path, not this procedure's job — §9.3 note).
- **Manual validation:** DBA review of grants; procedure visible only to the restricted role.
- **Expected outcome:** MVP terminal exit exists.
- **Failure signs:** procedure updating rows directly without the shared CAS semantics (must route through the SAME evidence-guarded CAS shape as feed evidence — §9.3).
- **Common mistakes:** dual control by runbook convention; optional ticket reference.
- **Completion criteria:** procedure deployed to test env; OP-02 green.
- **Stop condition:** merged.
- **Next task:** OP-02.

### OP-02 — Procedure test suite

- **Task ID:** OP-02
- **Title:** Test the procedure: refusals, dual control, money effects, guard passage, audit artifacts
- **Classification:** §18 BLOCKING go-live gate evidence
- **Purpose:** prove every CA-9 property on real Oracle.
- **Prerequisites:** OP-01.
- **Requirement sections / concepts to read:** §9.3, §10.3, CA-9.
- **Placeholder components involved:** [Integration Test Suite], [Operator Admin Procedure Area].
- **Local placeholder mappings required before starting:** Oracle test lane with the procedure deployed.
- **Local code areas to discover:** none new.
- **How to locate:** n/a.
- **Implementation instructions:** tests: EXECUTED on a seeded MAYBE row → outcome, SUBMITTED, +confirmed, normalization, alert, log line with ticket; REJECTED → outcome, marker, −committed; refusal: CLAIMED row; terminal row; same approver twice; missing ticket; amount mismatch on EXECUTED; guard interplay: the operation succeeds WHERE raw SQL fails (run the raw-SQL attempt in the same test to demonstrate the trigger); after the REJECTED case with a remaining shortfall: reservation RELEASED, provider_rejected marker LIVE, and NO successor created — the §6.8 marker gate correctly blocks blind re-pay (corrected 2026-07-11; the earlier assert-successor instruction was wrong and would push an implementer to weaken the marker); then apply a strictly-NEWER valid upstream message and assert the successor DOES create (the §6.8 successor policy); unauthorized-role endpoint attempt refused.
- **Do not change:** production code (failures reopen OP-01).
- **Tests to add:** the suite above.
- **Edge cases:** frozen-row convention holds after the procedure's outcome write (maybe_since cleared → off the MAYBE clocks).
- **Manual validation:** review evidence with the ops owner.
- **Expected outcome:** §18-3's "EXISTS" half proven.
- **Failure signs:** tests passing with the trigger disabled in the lane (the lane must run S-06's triggers).
- **Common mistakes:** skipping the raw-SQL-fails demonstration.
- **Completion criteria:** suite green on real Oracle.
- **Stop condition:** green; evidence filed.
- **Next task:** OP-03.

### OP-03 — Ops drill

- **Task ID:** OP-03
- **Title:** Execute CA-9's drill script end to end with real operators in a non-prod environment
- **Classification:** §18 BLOCKING go-live gate (the "AND BE DRILLED" half) + operational runbook / drill
- **Purpose:** §18-3: the procedure must EXIST AND BE DRILLED before go-live.
- **Prerequisites:** OP-02 green; CA-9 drill script; two real operators with the restricted role in the drill environment.
- **Requirement sections / concepts to read:** §18-3, CA-9 drill section, §20-8 (ticket trail).
- **Placeholder components involved:** [Operator Admin Procedure Area].
- **Local placeholder mappings required before starting:** drill environment provisioned.
- **Local code areas to discover:** none.
- **How to locate:** n/a.
- **Implementation instructions:** seed an unresolvable MAYBE row (divergent_payload_at set, cutoff passed — repost_permitted permanently false); operators verify the "platform truth" per the drill script's staged evidence; execute the procedure with a real ticket reference; verify: outcome applied, alert fired, log line correct, scope re-evaluated; record timings + friction; file the drill report.
- **Do not change:** the procedure based on drill friction without re-running OP-02.
- **Tests to add:** none (this is the drill).
- **Edge cases:** operator errors during the drill are FINDINGS (usability of the runbook), not failures — record.
- **Manual validation:** drill report signed by the ops owner.
- **Expected outcome:** §18-3 fully satisfied (with B-04's default path).
- **Failure signs:** drill executed by developers instead of the real operator role.
- **Common mistakes:** skipping the ticket-reference realism (the ticket trail is the restore-surviving record — §20-8).
- **Completion criteria:** signed drill report.
- **Stop condition:** report filed; §18-3 marked satisfiable in Section Q.
- **Next task:** OP-04.

### OP-04 — Guarded interim ops procedures + ops queue views

- **Task ID:** OP-04
- **Title:** Implement the §20 interim ops surface: retry / reject / annotate / reprocess-snapshot as AUTHORIZED APPLICATION ENDPOINTS + the four ops queue views
- **Classification:** MVP normative implementation (the §20 interim procedure set; complements RG-05's supersede/close and OP-01's verified-outcome procedure)
- **Purpose:** §20's accepted interim model presumes a controlled exit for every dead-end state — without them fail-blocked degrades to fail-forever (failure-recovery-walkthrough: E-2/E-4/P-11 retry, M-6 reject, M-2 annotate, U-9 tie) and the §15 queues page humans who have no lever.
- **Prerequisites:** OP-01/OP-02 (endpoint auth pattern, restricted role, evidence-flag mechanics proven on real Oracle); RG-05 (release guard + the supersede/close pattern to copy); RG-06 (§6.8 evaluation callable); IN-02 — VERIFY the tie-conflict record carries the identifiers per the REVISED §6.7 executability requirement (business_id, tied ordering, XML storage id, masked diff summary) and that the XML fetch-by-id path exists (§6.0 transport note); if not, reopen IN-02 BEFORE this card.
- **Requirement sections / concepts to read:** §20 (interim procedure set + items 1, 4, 8, 10), §6.7 (tie handling incl. executability requirement), §10.1, §10.5 (ops rows), §14, §15 (queue metrics), §12 (read semantics).
- **Placeholder components involved:** [Operator Admin Procedure Area], [Stored Procedure / Trigger Area], [Obligation Repository], [Metrics / Alerting Layer].
- **Local placeholder mappings required before starting:** OP-01's role/approver mechanics; view deployment target.
- **Local code areas to discover:** none new.
- **How to locate:** n/a.
- **Implementation instructions:** (a) three request-level AUTHORIZED ENDPOINTS (enterprise-authenticated, restricted ops role — the OP-01 pattern) whose service layer calls the SAME shared CAS/money helpers (never a private UPDATE path): retry-blocked → SAME-stage RETRY_WAIT per L7 (outcome IS NULL ∧ BLOCKED ∧ NOT_SUBMITTED ∧ divergent_payload_at IS NULL; POST-stage exits re-check the remaining repost_permitted terms); reject-blocked → outcome=REJECTED + L9 marker + release via the RG-02 path (release guard: NOT_SUBMITTED only); annotate → ops_annotation write (display-only, no state change). (b) reprocess-snapshot(business_id, xml_storage_id, tied_ordering?) — REVISED 2026-07-11: fetch the snapshot XML from the store by id (§6.0 transport note — the payload is NEVER a parameter), then re-run the normal §6.1 fan-out; when tied_ordering is supplied (tie adjudication, §6.7/§20-10) the §6.7 ordering check is relaxed to ≥ for EXACTLY that recorded value; without it, plain reprocess (the §6.6 DLT-recovery re-trigger — the ordering guard handles staleness normally); every money guard (§6.4/§6.5/§6.8, I6) applies unchanged. (c) Every endpoint enforces IN ITS CONTRACT: operator id, reason, external ticket ref (§20-8), plus a second DISTINCT enterprise-authenticated approver where 4-eyes applies (retry, reject, reprocess-with-relaxation — they move or release money via §6.8); §14 line with trigger_source=MANUAL_OPS:<operator>. (d) Four read-only queue views on the artifact-4 ACTIVE-row-bounded indexes: BLOCKED by reason (ESCALATED ranked first), stuck reservations by age, aged MAYBE by maybe_since + cutoff proximity, overpay latches; §15 alert definitions link to them. Execution-semantics reference: ops-console-proposal.md §6.1; mechanics: 24-implementation-mechanics.md M1/M8 (SHAPE-PROC + SHAPE-READ).
- **Do not change:** RG-05's supersede/close (already delivered); OP-01's operation; the §10.3 triggers; the §6.7 guard for NON-tied orderings (the ≥ relaxation applies to exactly one recorded value, and only when tied_ordering is explicitly supplied).
- **Tests to add:** T-33.
- **Edge cases:** reject attempted on a MAYBE row → refused at BOTH layers (code guard AND trigger — reuse OP-02's raw-SQL demo pattern); reprocess-snapshot re-run → every block no-ops (idempotent — ordering guard); reprocess onto a latched scope → amount applies, NO request created, AMENDMENT_ON_LATCHED_SCOPE fires (§6.5 latch guard unchanged); reprocess of a PURGED xml id → clean refusal + upstream-ask-8 escalation (never a partial apply); queue views on a multi-payment trade list one row per obligation — count is never an error (§12).
- **Manual validation:** run each endpoint against seeded rows in the OP-02 lane; compare view output against the seeded queue states; attempt each endpoint with a non-restricted role (must fail).
- **Expected outcome:** every §20 dead-end state has a working, audited, findable exit before go-live (exit may be a terminal give-up — §20 exit-honesty note).
- **Failure signs:** any endpoint with its own UPDATE path instead of the shared helpers; reprocess accepting payload/amounts as parameters; a view keyed on display labels.
- **Common mistakes:** relaxing the ordering guard for ALL messages instead of exactly the recorded tied value; making ticket/approver optional "for now"; exposing the endpoints beyond the enterprise ops role.
- **Completion criteria:** T-33 green on real Oracle; grants audited.
- **Stop condition:** merged; SHAPE-PROC + SHAPE-READ ticked in the report; Q29 evidence filed.
- **Next task:** OB-01.


---

## Phase handoff summary (P11 → P12)

- **Phase outputs:** the CA-9 audited operation implemented as an authorized application endpoint (dual control via enterprise-authenticated identities, evidence flag, refusals, audit + every-use alert), tested on real Oracle, and DRILLED by real operators (signed report) — §18-3's default path satisfied; PLUS the §20 interim ops surface (retry/reject/annotate/reprocess-snapshot endpoints + four queue views, OP-04).
- **Blockers to carry forward:** none new; §18-3 marked satisfiable in the go-live checklist (Q4, Q15); Q29 evidence filed with OP-04.
- **Local mapping rows expected filled:** [Operator Admin Procedure Area] CONFIRMED incl. the restricted-role/approver-identity mechanics.
- **Tests expected to exist:** T-24 suite (outcomes, refusals, guard interplay, raw-SQL-fails demo), T-28 wedge-prevention chain, T-33 interim-surface suite.
- **Next phase entry condition:** OP-03 drill report signed; OP-04 merged.
