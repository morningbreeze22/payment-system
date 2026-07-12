> **Purpose:** Task cards OP-01..OP-03 + OP-04a..OP-04e (apply-platform-verified-outcome audited operation — §18-3 — plus the §20 interim ops surface, pre-split round 9) (original Section H, phase P11).
> **When to use this file:** When executing the tasks of this phase, one card at a time, with the matching packet file from 09-minimal-context-packets/.
> **Depends on:** 08-task-cards/README.md; 01-playbook-index.md; 07-placeholder-glossary.md; the requirement sections cited per card; the locally filled mapping template.
> **Used by:** The local coding agent executing phase P11.
> **Safe to transfer:** yes
> **Contains local code names:** no

## H-Phase 11 — Operator verified-outcome operation (P11)

### OP-01 — Implement the apply-platform-verified-outcome operation

- **Task ID:** OP-01
- **Title:** Implement CA-9's audited verified-outcome OPERATION — an authorized application endpoint calling the shared transition service (dual control, evidence flag, refusal conditions, audit + alert; execution boundary decided 2026-07-11 — never a PL/SQL reimplementation; §10.3 triggers stay as backstop)
- **Classification:** §18 BLOCKING go-live gate (item 3) + MVP normative
- **Purpose:** the guaranteed terminal exit for otherwise-unresolvable MAYBE rows; the SINGLE sanctioned manual exception to §9.4.
- **Prerequisites:** CA-9 published; S-06 (evidence-flag mechanics live); RG-02/03 (money paths); ST-06 (normalization); B-04 recorded.
- **Requirement sections / concepts to read:** §9.3 (operation block), §10.1, §10.3, §20-8, CA-9.
- **Placeholder components involved:** [Operator Admin Procedure Area], [Stored Procedure / Trigger Area].
- **Local placeholder mappings required before starting:** operator admin area + restricted-role model Confirmed (D-10/D-20 findings); Oracle session-context mechanics from S-06.
- **Local code areas to discover:** how the ENTERPRISE ACCESS-MANAGEMENT identities reach the operation (mechanism DECIDED 2026-07-11, §9.3: each operator has a unique, non-bypassable identity supplied by the enterprise tooling; the operation verifies distinctness and records both — with the Java-endpoint boundary this is ordinary enterprise SSO/service auth, far simpler than DB-layer identity). Discovery confirms the plumbing only. If the enterprise tooling cannot deliver two authenticated identities to the application, BLOCKED — escalate; two free-text strings are non-compliant, never weaken to convention.
- **How to locate:** DBA/role model documentation (local).
- **Implementation instructions:** per CA-9: an AUTHORIZED ENDPOINT (enterprise-authenticated, restricted ops role) whose service-layer implementation calls the SHARED transition helpers (RG-02/RG-03/ST-06 — never a private update path). EXECUTION INPUT: the approval_id ONLY (round 4 — a prior §9.3 two-step approval bound this exact action: request_id, outcome, ticket, parameter hash, expiry, nonce; initiator and approver identities are DERIVED from the record, never passed as parameters; the operation REFUSES a record that is not APPROVED, is expired, or whose approver equals the initiator). Inside ONE transaction (and one JDBC session): CAS the approval APPROVED→CONSUMED (row count 1 — concurrent executors lose); re-check row state (refuse CLAIMED, refuse terminal); set the evidence session flag (S-06 session-context); EXECUTED → the RG-03 settlement path (amount equality enforced; +confirmed; SUB=SUBMITTED; outcome=EXECUTED; normalization); REJECTED → outcome=REJECTED + provider_rejected marker (L9) + release (RG-02) + normalization; emit the §14 line with trigger_source=OPS_PLATFORM_VERIFIED + ticket ref; raise the §15 every-use alert. Endpoint restricted to the enterprise ops role; unauthorized attempts refused and logged. Any refusal or exception rolls back BOTH the approval consumption and the transition (atomicity — a failed transition never burns an approval; a crash never leaves a replayable one).
- **Do not change:** the trigger (passed legitimately, never disabled — §9.3); §9.4's single-exception framing.
- **Tests to add:** in OP-02 (next card).
- **Edge cases:** row becomes CLAIMED between the operator's check and execution — the in-transaction re-check refuses (assert in OP-02); amount mismatch on EXECUTED → refuse (that is the §8 defect path, not this operation's job — §9.3 note).
- **Manual validation:** endpoint-authorization review with security/DBA; the operation reachable only by the enterprise ops role (unauthorized attempt refused + logged).
- **Expected outcome:** MVP terminal exit exists.
- **Failure signs:** the operation updating rows directly without the shared CAS semantics (must route through the SAME evidence-guarded CAS shape as feed evidence — §9.3).
- **Common mistakes:** dual control by runbook convention; optional ticket reference.
- **Completion criteria:** operation deployed to test env; OP-02 green.
- **Stop condition:** merged.
- **Next task:** OP-02.

### OP-02 — Operation test suite

- **Task ID:** OP-02
- **Title:** Test the operation: refusals, dual control (incl. the §9.3 approval-workflow negative set), money effects, guard passage, audit artifacts
- **Classification:** §18 BLOCKING go-live gate evidence
- **Purpose:** prove every CA-9 property on real Oracle.
- **Prerequisites:** OP-01.
- **Requirement sections / concepts to read:** §9.3, §10.3, CA-9.
- **Placeholder components involved:** [Integration Test Suite], [Operator Admin Procedure Area].
- **Local placeholder mappings required before starting:** Oracle test lane with the operation deployed.
- **Local code areas to discover:** none new.
- **How to locate:** n/a.
- **Implementation instructions:** tests: EXECUTED on a seeded MAYBE row → outcome, SUBMITTED, +confirmed, normalization, alert, log line with ticket; REJECTED → outcome, marker, −committed; refusal: CLAIMED row; terminal row; same approver twice; missing ticket; amount mismatch on EXECUTED; the §9.3 approval-workflow negative set (parameter substitution, expired approval, replayed consumed approval, identical identities, role revoked between approve and execute); guard interplay: the operation succeeds WHERE raw SQL fails (run the raw-SQL attempt in the same test to demonstrate the trigger); after the REJECTED case with a remaining shortfall: reservation RELEASED, provider_rejected marker LIVE, and NO successor created — the §6.8 marker gate correctly blocks blind re-pay (corrected 2026-07-11; the earlier assert-successor instruction was wrong and would push an implementer to weaken the marker); then apply a strictly-NEWER valid upstream message and assert the successor DOES create (the §6.8 successor policy); unauthorized-role endpoint attempt refused.
- **Do not change:** production code (failures reopen OP-01).
- **Tests to add:** the suite above.
- **Edge cases:** frozen-row convention holds after the operation's outcome write (maybe_since cleared → off the MAYBE clocks).
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
- **Purpose:** §18-3: the operation must EXIST AND BE DRILLED before go-live.
- **Prerequisites:** OP-02 green; CA-9 drill script; two real operators with the restricted role in the drill environment.
- **Requirement sections / concepts to read:** §18-3, CA-9 drill section, §20-8 (ticket trail).
- **Placeholder components involved:** [Operator Admin Procedure Area].
- **Local placeholder mappings required before starting:** drill environment provisioned.
- **Local code areas to discover:** none.
- **How to locate:** n/a.
- **Implementation instructions:** seed an unresolvable MAYBE row (divergent_payload_at set, cutoff passed — repost_permitted permanently false); operators verify the "platform truth" per the drill script's staged evidence; execute the operation via its authorized endpoint with a real ticket reference (two-step approval per §9.3); verify: outcome applied, alert fired, log line correct, scope re-evaluated; record timings + friction; file the drill report.
- **Do not change:** the operation based on drill friction without re-running OP-02.
- **Tests to add:** none (this is the drill).
- **Edge cases:** operator errors during the drill are FINDINGS (usability of the runbook), not failures — record.
- **Manual validation:** drill report signed by the ops owner.
- **Expected outcome:** §18-3 fully satisfied (with B-04's default path).
- **Failure signs:** drill executed by developers instead of the real operator role.
- **Common mistakes:** skipping the ticket-reference realism (the ticket trail is the restore-surviving record — §20-8).
- **Completion criteria:** signed drill report.
- **Stop condition:** report filed; §18-3 marked satisfiable in Section Q.
- **Next task:** OP-04a.

> **Round-9 note:** the former single OP-04 card was PRE-SPLIT into OP-04a..OP-04e — the split itself contains architectural judgment a small-context agent must not make. Execute strictly in order; each sub-card is one commit-sized unit with its own tests and stop condition.

### OP-04a — Shared ops-endpoint contract + ergonomics endpoints (retry / reject / annotate)

- **Task ID:** OP-04a
- **Title:** Shared authorized-endpoint contract (auth, audit, §9.3 approval adapter) + the three waivable ergonomics endpoints
- **Classification:** MVP normative implementation (§20 interim operation set — waivable ergonomics; the shared contract is reused by every OP-04x card)
- **Purpose:** §20's accepted interim model guarantees a controlled exit for exactly THREE dead-end classes (verified-outcome, supersede/close, reprocess-snapshot — §20 exit-honesty note; marker-only and overpay-latched scopes are documented STOP STATES) — without exits, fail-blocked degrades to fail-forever (walkthrough: E-2/E-4/P-11 retry, M-6 reject, M-2 annotate) and the §15 queues page humans who have no lever.
- **Prerequisites:** OP-01/OP-02 (endpoint auth pattern, restricted role, evidence-flag mechanics proven on real Oracle); RG-05 (release guard + supersede/close pattern to copy); RG-06 (§6.8 evaluation callable).
- **Requirement sections / concepts to read:** §20 (items 1, 4, 8), §9.3 (approval workflow), §10.1, §10.5 (ops rows), §14.
- **Placeholder components involved:** [Operator Admin Procedure Area], [Obligation Repository].
- **Local placeholder mappings required before starting:** OP-01's role/approver mechanics.
- **Local code areas to discover:** none new.
- **How to locate:** n/a.
- **Implementation instructions:** (a) ONE shared endpoint contract, reused by all OP-04x endpoints: enterprise authentication, restricted ops role, mandatory operator id + reason + external ticket ref (§20-8), the §9.3 two-step approval adapter where 4-eyes applies (execution by approval_id; approver ≠ initiator; binding + single-use consumption per §9.3), §14 line with trigger_source=MANUAL_OPS:<operator>; service layer calls the SAME shared CAS/money helpers — never a private UPDATE path. (b) three request-level endpoints on that contract: retry-blocked → SAME-stage RETRY_WAIT per L7 (outcome IS NULL ∧ BLOCKED ∧ NOT_SUBMITTED ∧ divergent_payload_at IS NULL; POST-stage exits re-check the remaining repost_permitted terms); reject-blocked → outcome=REJECTED + L9 marker + release via the RG-02 path (release guard: NOT_SUBMITTED only); annotate → ops_annotation write (display-only, no state change). Retry and reject are 4-eyes (they move or release money via §6.8). Mechanics: M1/M8 SHAPE-PROC.
- **Do not change:** RG-05's supersede/close (already delivered); OP-01's operation; the §10.3 triggers.
- **Tests to add:** T-33 subset: retry → SAME-stage RETRY_WAIT (an ENRICH row re-enriches); reject releases + sets the L9 marker, NOT_SUBMITTED only — a MAYBE row refused at code layer AND trigger layer (raw-SQL demo, OP-02 pattern); annotate has zero state effect; missing ticket / unauthorized role / identical identities refused; every call writes the §14 line.
- **Edge cases:** reject on a MAYBE row → refused at BOTH layers; retry on an ENRICH-blocked row re-enriches, never skips to POST (§10.5).
- **Manual validation:** run each endpoint against seeded rows in the OP-02 lane; attempt with a non-restricted role (must fail).
- **Expected outcome:** the shared contract exists and the waivable ergonomics endpoints work.
- **Failure signs:** any endpoint with its own UPDATE path; approver identity accepted as a parameter.
- **Common mistakes:** making ticket/approver optional "for now"; exposing endpoints beyond the enterprise ops role.
- **Completion criteria:** T-33 subset green on real Oracle.
- **Stop condition:** merged; SHAPE-PROC ticked.
- **Next task:** OP-04b.

### OP-04b — Reprocess-snapshot APPROVAL side (fetch, validate, digest-bind, display)

- **Task ID:** OP-04b
- **Title:** Approval-time snapshot fetch + validation + canonical digest binding + approver display (masked diff, supersession notice)
- **Classification:** MVP normative implementation (NON-WAIVABLE path — §20 minimal exit set)
- **Purpose:** §9.3 rounds 4–7: the approvers authorize CONTENT, not an opaque id — the approval must bind what they actually reviewed.
- **Prerequisites:** OP-04a (approval adapter); IN-02 — VERIFY the tie-conflict record carries the identifiers per §6.7 (business_id, tied ordering, XML storage id, masked diff summary), the XML fetch-by-id path exists (§6.0 transport note), AND the §6.1 trade-level ADMISSION gate is implemented; if not, reopen IN-02 BEFORE this card.
- **Requirement sections / concepts to read:** §9.3 (approval binding), §6.7 (tie + executability), §6.0 (transport), §16.3 (masking).
- **Placeholder components involved:** [Operator Admin Procedure Area].
- **Local placeholder mappings required before starting:** XML-store fetch client (IN-02's).
- **Local code areas to discover:** none new.
- **How to locate:** n/a.
- **Implementation instructions:** the reprocess-snapshot INITIATION endpoint (input: xmlStorageId + reason + ticketRef) FETCHES the snapshot from the store by id, VALIDATES it (schema, business_id), computes the CANONICAL business-payload digest (the ONE shared implementation — same algorithm as §2.4/§6.1), and records the pending §9.3 approval BOUND to (business_id, xml_storage_id/version, digest, ticket, expiry, UNIQUE nonce). The second approver's display shows the digest + the MASKED per-block diff (§16.3 — never raw payload) + the round-7 SUPERSESSION NOTICE (a newer live snapshot admitted mid-execution supersedes the unapplied remainder). No payment state is touched by this card.
- **Do not change:** the §6.7 tie-record contents; the §16.3 masking rules; the digest algorithm (shared, never a second implementation).
- **Tests to add:** approval binds the exact digest of the fetched document; a different document → different digest (binding, not attestation); masked diff never contains full account numbers; the supersession notice present; purged/missing xml id at approval time → clean refusal, NO pending approval created; approval expiry set.
- **Edge cases:** approval created, document corrected upstream (new id) before approval granted → execution (OP-04c) will hard-refuse on digest mismatch — by design, not this card's problem; wrong-business_id document → refused at initiation.
- **Manual validation:** approver screen review with the ops owner (digest + masked diff + notice visible).
- **Expected outcome:** approvals that authorize content.
- **Failure signs:** an approval created without a fetch (id-only attestation — the round-4 H-1 defect class).
- **Common mistakes:** computing the digest with a second implementation; showing the raw payload to approvers.
- **Completion criteria:** tests green.
- **Stop condition:** merged.
- **Next task:** OP-04c.

### OP-04c — Reprocess-snapshot EXECUTION (consume-at-start, admission entry, fence, completion evidence)

- **Task ID:** OP-04c
- **Title:** Execution by approval_id: digest hard-refusal → consume-at-start → §6.1 admission (≥ relaxation) → §20-10 per-block rules under the trade-snapshot fence → completion stamp + alerts
- **Classification:** MVP normative implementation (NON-WAIVABLE — §20 minimal exit set; the U-9 tie exit)
- **Purpose:** §20-10/§9.3: the server-verified, digest-bound, single-use application of an adjudicated snapshot — with crash and supersession behavior that never fails silently.
- **Prerequisites:** OP-04b (approvals exist); S-10/IN-02 (admission gate + fence live).
- **Requirement sections / concepts to read:** §20-10 (whole), §9.3 (consume-at-start + completion evidence), §6.1 (ADMISSION + fence + block-level supersession), §6.7 (tie definition), §6.4/§6.5/§6.8 (money guards).
- **Placeholder components involved:** [Operator Admin Procedure Area], [Obligation Repository], [Metrics / Alerting Layer].
- **Local placeholder mappings required before starting:** none beyond OP-04a/b.
- **Local code areas to discover:** none new.
- **How to locate:** n/a.
- **Implementation instructions:** execution takes the approval_id ONLY: re-fetch the snapshot, recompute the canonical digest, HARD-REFUSE on mismatch (+ alert) BEFORE any consumption or lock (refusal burns nothing); commit APPROVED→CONSUMED ALONE (round 5 — consumption precedes money movement, no replay window; crash mid-fan-out = NEW approval of the SAME document, never resurrect the consumed one); verify document.business_id; enter the §6.1 ADMISSION gate (≥ relaxation AT ADMISSION iff fetched ordering == trade watermark ∧ digest differs; OLDER than the watermark → refused whole even with a valid approval; the admission row update IS the application for a trade-reference-only tie); then the §20-10 PER-BLOCK rules, each block transaction passing the §6.1 trade-snapshot FENCE (overtaken by newer live intake → remaining blocks ABANDONED per block-level supersession, logged + counted); after the last block, stamp the approval record (ops schema) with completed_at + per-block summary (applied/no-op/dropped/abandoned) in its own small transaction; wire the §15 consumed-without-completion alert + runbook (stale document → annotate + close; else NEW approval). Every money guard (§6.4/§6.5/§6.8, I6) applies unchanged; no caller-supplied ordering exists.
- **Do not change:** the §6.7 guard for NON-tied orderings (the ≥ relaxation is SERVER-DERIVED — never caller-supplied); the §10.3 triggers; the fence.
- **Tests to add:** T-33 reprocess core: digest mismatch → hard refusal BEFORE consumption (approval NOT burned); crash after consume before fan-out → burned, nothing applied, NEW approval succeeds; overtaken mid-fan-out → fence aborts, abandonment logged, alert fires, re-approval of the stale document refused (CORRECT); crash between last block and stamp → false alert only, re-run no-ops and stamps; non-tying / wrong-business_id → no relaxation (artifact-6(d)); older-than-watermark → refused whole; reference-only tie converges via the admission update; latched scope → amount applies, NO request, AMENDMENT_ON_LATCHED_SCOPE; purged xml id → clean refusal + ask-8 escalation; re-run under a NEW approval → digest-equal, all no-ops.
- **Edge cases:** concurrent double-execution → exactly one CONSUMED CAS wins; artifact-6(e) negatives all apply.
- **Manual validation:** full tie drill on seeded data in the OP-02 lane.
- **Expected outcome:** the non-waivable tie exit works and never fails silently.
- **Failure signs:** reprocess accepting payload/amounts/ordering as parameters; consumption sharing a transaction with block work; a consumed approval resurrected.
- **Common mistakes:** accepting an ordering parameter (server derives it — round 3); skipping the pre-consumption digest check; forgetting the completion stamp's own transaction.
- **Completion criteria:** T-33 reprocess core green on real Oracle.
- **Stop condition:** merged; SHAPE-PROC ticked.
- **Next task:** OP-04d.

### OP-04d — Ops queue views + authorization/index-plan tests

- **Task ID:** OP-04d
- **Title:** Four read-only queue views on the artifact-4 active-row-bounded indexes + §15 links + plan assertions
- **Classification:** MVP normative implementation (Q29-waivable ergonomics)
- **Purpose:** §15's queues must land humans somewhere actionable; views ride the ACTIVE-row-bounded indexes so they stay flat as terminal rows grow.
- **Prerequisites:** OP-04a (role model); S-07 (index set).
- **Requirement sections / concepts to read:** §15 (queue metrics), §12 (read semantics), §10.4 (labels display-only).
- **Placeholder components involved:** [Obligation Repository], [Metrics / Alerting Layer].
- **Local placeholder mappings required before starting:** view deployment target.
- **Local code areas to discover:** none new.
- **How to locate:** n/a.
- **Implementation instructions:** four READ-ONLY views: BLOCKED by reason (ESCALATED ranked first), stuck reservations by age, aged MAYBE by maybe_since, overpay latches (round 10: no cutoff-proximity column — the engine owns the calendar; the round-8 pointer-residue view was REMOVED with the §2.4 greenfield fact); §15 alert definitions link to each view; restricted to the ops role; mechanics M8 SHAPE-READ (no locks, no writes).
- **Do not change:** any write path (this card is read-only).
- **Tests to add:** T-33 views subset: views rank ESCALATED first; one row per obligation on a multi-payment trade (count is never an error — §12); EXPLAIN plans ride the artifact-4 indexes on a terminal-heavy seed; unauthorized role refused.
- **Edge cases:** a view keyed on display labels or blocked_reason as a rule input = FAILURE (display/routing only).
- **Manual validation:** compare view output against seeded queue states.
- **Expected outcome:** every §15 queue alert links to a working view.
- **Failure signs:** a view whose plan degrades with terminal-row growth.
- **Common mistakes:** sneaking a write or lock into a "view helper".
- **Completion criteria:** T-33 views subset green.
- **Stop condition:** merged; SHAPE-READ ticked.
- **Next task:** OP-04e.

### OP-04e — Interim-surface cross-path integration suite + evidence

- **Task ID:** OP-04e
- **Title:** Full T-33 on real Oracle: crash / overtake / replay / zombie / concurrency across all OP-04x paths; Q29 evidence filed
- **Classification:** MVP normative implementation (integration gate for the OP-04x set)
- **Purpose:** the sub-cards were built separately; money-moving operations need one adversarial pass across their interactions before the phase closes.
- **Prerequisites:** OP-04a..OP-04d merged.
- **Requirement sections / concepts to read:** T-33 (whole), T-35 (reprocess entry cases), artifact 6 sets (d)/(e)/(f).
- **Placeholder components involved:** [Integration Test Suite].
- **Local placeholder mappings required before starting:** Oracle lane with real triggers.
- **Local code areas to discover:** none new.
- **How to locate:** n/a.
- **Implementation instructions:** run/complete the FULL T-33 suite on real Oracle (all endpoints, valid + invalid inputs, the artifact-6(d) mixed-snapshot set, the (e) dual-control negatives incl. concurrent double-execution, the (f) admission set where OP-owned); the T-35 reprocess-entry cases; file the Q29 evidence pack.
- **Do not change:** production code except fixes for failures found (failures reopen the owning sub-card).
- **Tests to add:** any T-33 case not yet covered by OP-04a..d.
- **Edge cases:** the suite must run with the REAL triggers (a lane without S-06's triggers proves nothing).
- **Manual validation:** evidence pack reviewed with the ops owner.
- **Expected outcome:** the §20 NON-WAIVABLE minimal exit set works end to end (verified-outcome via OP-01, supersede/close via RG-05, reprocess-snapshot via OP-04b/c) plus the waivable ergonomics; exits may be terminal give-ups (§20 exit-honesty note).
- **Failure signs:** green tests with triggers disabled; evidence filed retroactively without run IDs.
- **Common mistakes:** treating sub-card unit tests as a substitute for the cross-path suite.
- **Completion criteria:** T-33 green on real Oracle; grants audited; Q29 evidence filed.
- **Stop condition:** merged; evidence filed.
- **Next task:** OB-01.

---

## Phase handoff summary (P11 → P12)

- **Phase outputs:** the CA-9 audited operation implemented as an authorized application endpoint (dual control via enterprise-authenticated identities, evidence flag, refusals, audit + every-use alert), tested on real Oracle, and DRILLED by real operators (signed report) — §18-3's default path satisfied; PLUS the §20 interim ops surface (retry/reject/annotate + reprocess approval/execution + queue views — OP-04a..OP-04e, pre-split round 9).
- **Blockers to carry forward:** none new; §18-3 marked satisfiable in the go-live checklist (Q4, Q15); Q29 evidence filed with OP-04e.
- **Local mapping rows expected filled:** [Operator Admin Procedure Area] CONFIRMED incl. the restricted-role/approver-identity mechanics.
- **Tests expected to exist:** T-24 suite (outcomes, refusals, guard interplay, raw-SQL-fails demo), T-28 wedge-prevention chain, T-33 interim-surface suite.
- **Next phase entry condition:** OP-03 drill report signed; OP-04a..OP-04e merged (OP-04e's evidence filed).
