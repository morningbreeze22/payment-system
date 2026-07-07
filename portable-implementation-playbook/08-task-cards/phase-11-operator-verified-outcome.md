> **Purpose:** Task cards OP-01..OP-03 (apply-platform-verified-outcome audited procedure — §18-3) (original Section H, phase P11).
> **When to use this file:** When executing the tasks of this phase, one card at a time, with the matching packet file from 09-minimal-context-packets/.
> **Depends on:** 08-task-cards/README.md; 01-playbook-index.md; 07-placeholder-glossary.md; the requirement sections cited per card; the locally filled mapping template.
> **Used by:** The local coding agent executing phase P11.
> **Safe to transfer:** yes
> **Contains local code names:** no

## H-Phase 11 — Operator verified-outcome procedure (P11)

### OP-01 — Implement the apply-platform-verified-outcome procedure

- **Task ID:** OP-01
- **Title:** Implement CA-9's audited stored procedure (dual control, evidence flag, refusal conditions, audit + alert)
- **Classification:** §18 BLOCKING go-live gate (item 3) + MVP normative
- **Purpose:** the guaranteed terminal exit for otherwise-unresolvable MAYBE rows; the SINGLE sanctioned manual exception to §9.4.
- **Prerequisites:** CA-9 published; S-06 (evidence-flag mechanics live); RG-02/03 (money paths); ST-06 (normalization); B-04 recorded.
- **Requirement sections / concepts to read:** §9.3 (procedure block), §10.1, §10.3, §20-8, CA-9.
- **Placeholder components involved:** [Operator Admin Procedure Area], [Stored Procedure / Trigger Area].
- **Local placeholder mappings required before starting:** procedure area + restricted-role model Confirmed (D-10/D-20 findings); Oracle session-context mechanics from S-06.
- **Local code areas to discover:** how approver identities authenticate to the DB layer (MUST_VERIFY_LOCALLY; if the role model cannot produce two distinct authenticated identities, BLOCKED — escalate, do not weaken to convention).
- **How to locate:** DBA/role model documentation (local).
- **Implementation instructions:** per CA-9: inputs (request_id, outcome EXECUTED|REJECTED, ticket/evidence reference NOT NULL, two distinct approver identities — procedure REFUSES identical/unauthenticated pairs); inside one transaction: re-check row state (refuse CLAIMED, refuse terminal); set the evidence session flag; EXECUTED → the RG-03 settlement path (amount equality enforced; +confirmed; SUB=SUBMITTED; outcome=EXECUTED; normalization); REJECTED → outcome=REJECTED + provider_rejected marker (L9) + release (RG-02) + normalization; emit the §14 line with trigger_source=OPS_PLATFORM_VERIFIED + ticket ref; raise the §15 every-use alert; grant EXECUTE to the restricted role only.
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
- **Implementation instructions:** tests: EXECUTED on a seeded MAYBE row → outcome, SUBMITTED, +confirmed, normalization, alert, log line with ticket; REJECTED → outcome, marker, −committed; refusal: CLAIMED row; terminal row; same approver twice; missing ticket; amount mismatch on EXECUTED; guard interplay: procedure succeeds WHERE raw SQL fails (run the raw-SQL attempt in the same test to demonstrate the trigger); scope completion after the applied outcome (§4.1 — the wedge actually opens: released shortfall re-pays under a NEW key via §6.8 where guards permit; assert successor creation on the REJECTED case with a remaining shortfall).
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
- **Next task:** OB-01.


---

## Phase handoff summary (P11 → P12)

- **Phase outputs:** the CA-9 procedure implemented (dual control, evidence flag, refusals, audit + every-use alert), tested on real Oracle, and DRILLED by real operators (signed report) — §18-3's default path satisfied.
- **Blockers to carry forward:** none new; §18-3 marked satisfiable in the go-live checklist (Q4, Q15).
- **Local mapping rows expected filled:** [Operator Admin Procedure Area] CONFIRMED incl. the restricted-role/approver-identity mechanics.
- **Tests expected to exist:** T-24 suite (outcomes, refusals, guard interplay, raw-SQL-fails demo), T-28 wedge-prevention chain.
- **Next phase entry condition:** OP-03 drill report signed.
