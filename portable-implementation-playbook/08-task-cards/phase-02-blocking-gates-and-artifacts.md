> **Purpose:** Task cards B-01..B-04 and CA-1..CA-9 (§18 gates + companion artifacts) (original Section H, phase P2).
> **When to use this file:** When executing the tasks of this phase, one card at a time, with the matching packet file from 09-minimal-context-packets/.
> **Depends on:** 08-task-cards/README.md; 01-playbook-index.md; 07-placeholder-glossary.md; the requirement sections cited per card; the locally filled mapping template.
> **Used by:** The local coding agent executing phase P2.
> **Safe to transfer:** yes
> **Contains local code names:** no

## H-Phase 2 — Blocking gates and contract artifacts (P2)

### B-01 — Resolve §18 BLOCKING item 0 (payments-per-trade / scope key)

- **Task ID:** B-01
- **Title:** Drive the snapshot-contract residue: written confirmation, intake validation, PO-9
- **Classification:** §18 BLOCKING go-live gate
- **Purpose:** the §1 contract facts record the model: one trade carries MULTIPLE payments; each message is a FULL-TRADE SNAPSHOT (newer overwrites older); (payment_type + debit_account + currency) is unique WITHIN a snapshot, and an equal tuple ACROSS snapshots means the same payment. Consequence: the §2.1 scope key needs NO discriminator, §5.1 identity stands unchanged, and the schema/identity freeze (S-02/S-03/S-05/K-02/K-03/CA-4/CA-5) is NOT gated here. §12 lookup: business_id returns ALL of the trade's obligations (multiple results = normal). This task drives the model's four open edges to closure.
- **Prerequisites:** none (human task).
- **Requirement sections / concepts to read:** §1 contract facts (trade-payment cardinality), §6.0, §6.1, §12, §18 BLOCKING item 0.
- **Implementation instructions (residue):** (1) obtain the WRITTEN upstream confirmation of the snapshot schema + within-snapshot uniqueness (upstream ask 5) — the cross-snapshot identity half is unverifiable at runtime and rests on this document; (2) ensure IN-02 implements the §6.0 within-snapshot uniqueness intake validation (whole-snapshot validation failure, fail closed); (3) drive PO-9 (absence semantics — a BA-2 amendment, PO-only) to an answer BEFORE the IN-02 consumer freeze — it shapes §6.1's fan-out (TL-16 was ANSWERED 2026-07-11 round 5: the §6.1 trade-level admission gate + §2.4 — no longer a residue item); (4) TL-2's read contract now must also answer step granularity (per-payment vs per-trade rollup, §12); (5) upstream ask 8 IN WRITING (round 4 — §18-0(d)): sanctioned fetch-by-id, stable unique versioned ids, consistent reads, IMMUTABILITY (corrections = new id/version), retention ≥ the ops/tie SLA.
- **Do not change:** code.
- **Tests to add:** intake test — snapshot with two blocks sharing a tuple → whole-snapshot validation failure + anchors (§6.0/§6.6); fan-out convergence test — kill consumer mid-fan-out, redeliver, assert per-obligation ordering guard converges (§6.1).
- **Edge cases:** "usually unique" is NOT an answer for ask 5 — the identity contract needs a guarantee; PO-9 unanswered means absence = NO-OP (BA-2 stands), which knowingly leaves a genuinely-removed payment paying.
- **Manual validation:** written confirmation attributed and filed; the PO-9 answer recorded in §18 (TL-16 already answered round 5).
- **Expected outcome:** B-01 fully closed; IN-02 consumer freeze unblocked.
- **Failure signs:** IN-02 frozen while PO-9 is open; treating a verbal model confirmation as the written contract.
- **Common mistakes:** re-litigating the §1 contract fact instead of driving its open edges.
- **Completion criteria:** all four residue items closed; blocked-task list updated.
- **Stop condition:** residue items closed (or explicitly pending — then IN-02 stays BLOCKED).
- **Next task:** B-02 (parallel); S-01/S-02/K-02 are not gated by this item.

### B-02 — Secure sandbox access + engine written statements (§18 item 1 inputs)

- **Task ID:** B-02
- **Title:** Obtain engine sandbox access, key-retention TTL statement, ingest-lag distribution, query lookback, rate limits
- **Classification:** §18 BLOCKING go-live gate (enabler for CT suite)
- **Purpose:** §18-1(c) requires the TTL in writing; TL-5 needs ingest lag (p50/p99/max) + lookback; TL-13 needs the query rate limit — all load-bearing config inputs.
- **Prerequisites:** none (human/provider task; parallel with B-01).
- **Requirement sections / concepts to read:** §18 BLOCKING item 1, §18 TL items 4, 5, 11, 13; §9.2, §9.5.
- **Placeholder components involved:** [Contract Test Suite] (future consumer).
- **Local placeholder mappings required before starting:** none.
- **Local code areas to discover:** none.
- **How to locate:** n/a.
- **Implementation instructions:** request from the provider: sandbox credentials/endpoints suitable for CT-01..CT-05; written key-retention TTL; ingest-lag distribution; query lookback window vs maximum row lifetime including ops-queue SLA (TL-5 framing); status-query rate limit (TL-13); SDK answers (TL-11 a/b/c). Record each answer verbatim with source. Note per §18-1: written answers configure the tests; only the EXECUTED tests close the gate.
- **Do not change:** code.
- **Tests to add:** none (CT-xx implement them).
- **Edge cases:** provider refuses a TTL statement → CT-04 tests at the oldest achievable edge and the gap is recorded as a go-live risk owned by the accountable human.
- **Manual validation:** answers filed; config inventory (OB-07) values drafted from them.
- **Expected outcome:** sandbox usable; numbers recorded.
- **Failure signs:** treating these written answers as closing §18-1 (they don't — CT-02..05 do).
- **Common mistakes:** not asking lookback ≥ MAX ROW LIFETIME incl. ops-queue SLA (the §18/TL-5 framing — parked rows live days).
- **Completion criteria:** access + all five answer sets recorded.
- **Stop condition:** recorded; CT-01 unblocked.
- **Next task:** B-03 (parallel); CT-01.

### B-03 — Resolve cutoff calendar sourcing (§18 item 2)

- **Task ID:** B-03
- **Title:** Identify cutoff-calendar source system, owner, semantics, refresh, fail direction
- **Classification:** §18 BLOCKING go-live gate
- **Purpose:** repost_permitted (§7.0), §7.4 deadlines, §9.2 lookback guard, and escalation sizing all consume the calendar; a wrong calendar blocks a currency early or re-POSTs after bank close (§18-2).
- **Prerequisites:** none (human task).
- **Requirement sections / concepts to read:** §18 BLOCKING item 2, §16.4 (tz-aware representation), §7.4, §16.6 (config entry).
- **Placeholder components involved:** [Retry Resolver Job] (consumer), config.
- **Local placeholder mappings required before starting:** none.
- **Local code areas to discover:** none.
- **How to locate:** n/a.
- **Implementation instructions:** record: source system; named owner; per-currency/market semantics incl. holidays; timezone-aware representation (local time + zone id, DST-correct — §16.4, never fixed UTC constants); refresh cadence; stale/missing-calendar fail direction (spec recommends fail-blocked per payment_type).
- **Do not change:** code.
- **Tests to add:** none here (RC-04 tests consume it).
- **Edge cases:** no source system exists → the owner question escalates to the PO; RC-04 cutoff config stays BLOCKED; interface work proceeds with fail-blocked default.
- **Manual validation:** owner has acknowledged ownership in writing.
- **Expected outcome:** calendar contract recorded; RC-04 config unblocked.
- **Failure signs:** hardcoded UTC cutoff constants anywhere ("wrong twice a year per market", §16.4).
- **Common mistakes:** accepting a calendar without holiday semantics.
- **Completion criteria:** all six attributes recorded.
- **Stop condition:** recorded (or explicitly pending — RC-04 cutoff config remains BLOCKED).
- **Next task:** B-04.

### B-04 — Record the §18 item 3 resolution path (MAYBE terminal exit)

- **Task ID:** B-04
- **Title:** Confirm the MVP MAYBE-row terminal exit: the audited procedure (default) or the TL-10 + TL-5 alternative
- **Classification:** §18 BLOCKING go-live gate
- **Purpose:** §18-3: without a terminal exit, an unresolvable MAYBE row holds its reservation forever, the scope never completes (§4.1) and I6 blocks successors.
- **Prerequisites:** B-02 (TL-5/TL-10 answers inform the alternative).
- **Requirement sections / concepts to read:** §18 BLOCKING item 3, §9.3 (operation), TL-10, TL-5.
- **Placeholder components involved:** [Operator Admin Procedure Area] (default path).
- **Local placeholder mappings required before starting:** none.
- **Local code areas to discover:** none.
- **How to locate:** n/a.
- **Implementation instructions:** default decision per spec: BUILD the operation (OP-01..03, CA-9). Only if TL-10 (platform formal reject) AND TL-5 (lookback ≥ max row lifetime incl. ops-queue SLA) are BOTH answered affirmatively in writing may the operation be de-scoped — record whichever path, and note §20's PO decision already REQUIRES the operation at MVP, so de-scoping needs explicit PO re-confirmation.
- **Do not change:** code.
- **Tests to add:** none.
- **Edge cases:** partial alternative (TL-10 yes, TL-5 no) → procedure stays required.
- **Manual validation:** decision recorded with approver.
- **Expected outcome:** OP-xx confirmed in scope (expected default).
- **Failure signs:** de-scoping the operation on optimistic unwritten answers.
- **Common mistakes:** reading §18-3 as optional because an alternative exists.
- **Completion criteria:** path recorded.
- **Stop condition:** recorded.
- **Next task:** CA-1.

### CA-1 — Author the engine error-code classification table

- **Task ID:** CA-1
- **Title:** Engine error-code → classification table (§16.6 artifact 1)
- **Classification:** §16.6 companion artifact
- **Purpose:** RC-01's classifier is generated FROM this table; §7 requires a closed taxonomy keyed on cause, code by code.
- **Prerequisites:** B-02 (provider engagement channel); D-05 (locally observed branch inventory as input).
- **Requirement sections / concepts to read:** §7.0–7.3 (taxonomy + target dimensions), §16.6 artifact 1 (incl. the replay-original-response class), §13 (categories/severities).
- **Placeholder components involved:** [Provider Response Parser] (consumer).
- **Local placeholder mappings required before starting:** none for authoring; D-05 memo desirable.
- **Local code areas to discover:** none (document task).
- **How to locate:** n/a.
- **Implementation instructions:** produce a table: engine code → (exception_category, exception_code, retryable, severity, submission_state, target stage/stage_state/outcome) per §7.2/§7.3 semantics; explicitly classify: DUPLICATE_REQUEST; known-key-different-payload collision (distinguishable code — TL-4); the replay-original-response class (§16.6-1); every synchronous business reject; unmapped default = fail closed (MAYBE → BLOCKED(UNMAPPED_CODE)). Name an owner. Version the table.
- **Do not change:** the §7.2 branch semantics — the table fills codes INTO them, never invents new branches.
- **Tests to add:** none here (RC-01 tests consume the table as fixtures).
- **Edge cases:** codes the provider cannot explain → classified fail-closed, flagged to the owner.
- **Manual validation:** provider (or tech lead) has reviewed the table; every code from D-05's observed inventory appears.
- **Expected outcome:** versioned table with owner.
- **Failure signs:** any "assume retryable" default (§7.2 forbids).
- **Common mistakes:** classifying by HTTP status line; omitting the replay-original-response class.
- **Completion criteria:** table complete, owned, versioned.
- **Stop condition:** table published to the team.
- **Next task:** CA-2.

### CA-2 — Author the engine status vocabulary + evidence mapping

- **Task ID:** CA-2
- **Title:** Engine status vocabulary, precedence/evidence mapping, feed event schema (§16.6 artifact 2)
- **Classification:** §16.6 companion artifact
- **Purpose:** IN-07's evidence application and §4.4's ranking consume this; the feed event schema (event_id, UETR, status, amount, provider_reference — names and types) feeds §16.5 contract tests.
- **Prerequisites:** B-02.
- **Requirement sections / concepts to read:** §4.4, §8, §16.6 artifact 2 (incl. the dead-UETR question), §18 TL-1 (event_id stability).
- **Placeholder components involved:** [Payment Status Feed Consumer] (consumer).
- **Local placeholder mappings required before starting:** none.
- **Local code areas to discover:** none.
- **How to locate:** n/a.
- **Implementation instructions:** full status enum; per-status: terminal vs intermediate class + evidence rank; the feed event schema with field names/types; answer (or record as pending) whether the engine emits events under a REJECTED duplicate/collision submission's UETR — note the design forecloses harm by never persisting those UETRs (§5); record TL-1's event_id answer or the synthesis fallback choice.
- **Do not change:** §4.4's application rules.
- **Tests to add:** none here.
- **Edge cases:** statuses with context-dependent meaning → classify fail-closed with the owner's sign-off.
- **Manual validation:** provider review; cross-check against CA-1 (same vocabulary family).
- **Expected outcome:** versioned artifact with owner.
- **Failure signs:** intermediate statuses mapped as terminal (would freeze rows early).
- **Common mistakes:** leaving amount/typing of the event schema informal (contract tests need exact types).
- **Completion criteria:** artifact complete, owned.
- **Stop condition:** published.
- **Next task:** CA-3.

### CA-3 — Author the status-query response mapping

- **Task ID:** CA-3
- **Title:** Status-query response → §9.1 outcome mapping (§16.6 artifact 3)
- **Classification:** §16.6 companion artifact
- **Purpose:** RC-06 applies §9.1 outcomes; this maps every real query response to EXECUTED/REJECTED/NOT_FOUND/INDETERMINATE/ACCEPTED, including the decided rule that acceptance answers promote submission_state to SUBMITTED.
- **Prerequisites:** B-02.
- **Requirement sections / concepts to read:** §9.1, §9.2 (NOT_FOUND never taken at face value), §16.6 artifact 3.
- **Placeholder components involved:** [Status Query Resolver] (consumer).
- **Local placeholder mappings required before starting:** none.
- **Local code areas to discover:** none.
- **How to locate:** n/a.
- **Implementation instructions:** per query-response shape: mapped §9.1 outcome; unmapped/error/timeout → INDETERMINATE (reschedule); document which key the query accepts (idempotency key vs UETR) per B-02's answers; owner + version.
- **Do not change:** §9.1/§9.2 semantics.
- **Tests to add:** none here (RC-06 fixtures).
- **Edge cases:** responses that embed partial/held states → INDETERMINATE unless provider confirms a class.
- **Manual validation:** provider review; CT-06 later verifies empirically.
- **Expected outcome:** versioned mapping with owner.
- **Failure signs:** NOT_FOUND mapped to "not submitted" (forbidden — §9.2).
- **Common mistakes:** omitting query-API failure/timeout handling from the mapping.
- **Completion criteria:** artifact complete, owned.
- **Stop condition:** published.
- **Next task:** CA-4.

### CA-4 — Author the full DDL migration set spec

- **Task ID:** CA-4
- **Title:** Flyway/Oracle DDL migration set: tables, CHECKs, I6 expression, triggers, index list (§16.6 artifact 4)
- **Classification:** §16.6 companion artifact
- **Purpose:** P3's authoritative spec — exact I6 function-index expression, L1-shape + L2–L8 CHECKs, freeze + release-guard triggers, one active-row-bounded index per standing scan.
- **Prerequisites:** scope key settled (§1 contract facts — multi-payment snapshot model, no discriminator; B-01 residue does not gate this); D-02 gap inventory.
- **Requirement sections / concepts to read:** §2.1, §2.2, §2.3, §10.3, §3 (I6), §16.5 (expand/contract, enum evolution), §16.6 artifact 4.
- **Placeholder components involved:** [DB Migration Directory], [Stored Procedure / Trigger Area].
- **Local placeholder mappings required before starting:** D-02 rows (real current shape).
- **Local code areas to discover:** none beyond D-02's inventory.
- **How to locate:** n/a.
- **Implementation instructions:** specify (schema-shape pseudocode, not final SQL): every §2.1/§2.2 column with type/nullability; scope-key UNIQUE (per B-01!); UNIQUE(idempotency_key), UNIQUE(uetr) (NULL-ignoring); I6 as CASE WHEN outcome IS NULL THEN payment_obligation_id END unique function-based index; per-enum CHECKs; L1-shape + L2–L8 CHECK expressions; freeze trigger + release-guard trigger with evidence session flag mechanics; the normative index list — resolver sweep, retry scanner, escalation scanner, BLOCKED queue, stuck-state, drift, §5.2 created_at window — each expression NULL for terminal rows (§16.6-4); expand/contract sequencing notes per migration.
- **Do not change:** the four-table model (§2.1–§2.4) — any "needs another table" is SPEC_CONFLICT.
- **Tests to add:** none here (S-09 executes them).
- **Edge cases:** existing-column type conflicts from D-02 → each gets an explicit expand/contract path in the spec.
- **Manual validation:** DBA-owner review (privileges for triggers/procedures confirmed — from D-02).
- **Expected outcome:** versioned DDL spec ready for S-02..S-07.
- **Failure signs:** CHECK constraints written VALIDATE-first against unmigrated data.
- **Common mistakes:** forgetting Oracle NULL-in-unique-index semantics for uetr; omitting the active-row-bounded trick on scan indexes.
- **Completion criteria:** spec complete, DBA-reviewed.
- **Stop condition:** published; S-02..S-07 unblocked (schema freeze).
- **Next task:** CA-5.

### CA-5 — Author the identity-derivation spec + golden vectors

- **Task ID:** CA-5
- **Title:** Identity derivation spec (byte-exact, versioned) + golden vectors (§16.6 artifact 5, first half)
- **Classification:** §16.6 companion artifact
- **Purpose:** §5.1 exactness: hash algorithm, field serialization order, delimiter, canonicalization (case, trimming, encoding, account-number normalization), versioning — frozen by golden vectors. Byte-identical reproducibility IS the DR property.
- **Prerequisites:** scope key settled (§1 contract facts — derivation input list final; B-01 residue does not gate this).
- **Requirement sections / concepts to read:** §5.1 (all rules: amount NOT in key; UETR NOT in derivation), §2.1 (next_request_seq), §16.6 artifact 5.
- **Placeholder components involved:** [Payment Request Creation Component] (consumer).
- **Local placeholder mappings required before starting:** none for authoring.
- **Local code areas to discover:** none.
- **How to locate:** n/a.
- **Implementation instructions:** specify: input fields = business_id | payment_type | debit_account | currency | request_seq (no discriminator — scope key settled, §1 contract facts); canonicalization per field; delimiter + encoding; hash algorithm + output format; version identifier embedded in the scheme; at least a dozen golden vectors covering: case variants, whitespace variants, account-number normalization cases, seq increments, and scope variants — each vector = inputs + exact expected key bytes.
- **Do not change:** the input list — amount and UETR stay OUT (§5.1 records why); the scope fields are a §1 contract fact (changing them requires the PO).
- **Tests to add:** none here (K-03 turns vectors into tests).
- **Edge cases:** fields that can legally contain the delimiter — the spec must make that unambiguous (length-prefix or escaping — choose and freeze).
- **Manual validation:** two independent implementations (or one implementation + manual computation) reproduce all vectors.
- **Expected outcome:** frozen versioned spec + vector file.
- **Failure signs:** vectors computed only by the code under test (circular).
- **Common mistakes:** locale-dependent case folding; unspecified encoding.
- **Completion criteria:** spec + vectors published.
- **Stop condition:** published; K-02/K-03 unblocked.
- **Next task:** CA-6.

### CA-6 — Author the canonical instruction serialization / last_sent_hash definition

- **Task ID:** CA-6
- **Title:** Canonical instruction serialization + hash definition for last_sent_hash (§16.6 artifact 5, second half)
- **Classification:** §16.6 companion artifact
- **Purpose:** §7.0/§2.2: the claim transaction persists the hash of the canonically-serialized instruction; hash comparisons across attempts and DR replays are meaningful only under the same byte-exactness discipline as CA-5.
- **Prerequisites:** CA-5 (shared discipline); D-05 (what the instruction payload contains locally — field-level, no proprietary values in the artifact).
- **Requirement sections / concepts to read:** §7.0, §2.2 (last_sent_hash / divergence_expected), §5.1 (instruction hash paragraph), §16.6 artifact 5.
- **Placeholder components involved:** [Provider POST Client], [Request Status Persistence Layer].
- **Local placeholder mappings required before starting:** D-05 memo (instruction field inventory — kept local; the artifact defines RULES, not values).
- **Local code areas to discover:** none beyond D-05.
- **How to locate:** n/a.
- **Implementation instructions:** define: which instruction fields enter the hash (the business content actually sent — MUST_VERIFY_LOCALLY against the real payload shape, recorded locally); canonical field order; canonicalization rules per CA-5's discipline; hash algorithm + versioning; the rule that the CONTENT is never persisted, only the hash (§16.3/§7.0).
- **Do not change:** the no-payload-freeze decision (§7.0 — details re-resolved fresh per attempt).
- **Tests to add:** none here (K-05 tests).
- **Edge cases:** envelope/transport fields (timestamps, message ids) must be EXCLUDED — else every attempt looks divergent and divergence_expected is always true.
- **Manual validation:** same instruction serialized twice → identical hash; one business-field change → different hash.
- **Expected outcome:** versioned definition.
- **Failure signs:** hash including per-attempt envelope noise.
- **Common mistakes:** hashing the raw SDK request object (unstable field order).
- **Completion criteria:** definition published.
- **Stop condition:** published; K-05 unblocked.
- **Next task:** CA-7.

### CA-7 — Author the test catalog

- **Task ID:** CA-7
- **Title:** Test catalog aligned to requirment-v4.md (§16.6 artifact 6)
- **Classification:** §16.6 companion artifact
- **Purpose:** the named, owned catalog every phase's tests trace to; Section J of this playbook is its seed.
- **Prerequisites:** none hard; grows with CA-1..3.
- **Requirement sections / concepts to read:** §16.6 artifact 6 (incl. the named entries: §9.2 downgrade re-POST answered DUPLICATE_REQUEST leaves prior uetr intact; §11 ambiguous claim-commit; §8 concurrent in-flight duplicates), Section J of this playbook.
- **Placeholder components involved:** [Integration Test Suite], [Contract Test Suite].
- **Local placeholder mappings required before starting:** none.
- **Local code areas to discover:** none.
- **How to locate:** n/a.
- **Implementation instructions:** adopt Section J's matrix (T-01..T-36) as the seed; add the spec-named entries above; assign each entry an owner-type and the phase whose task implements it; keep IDs stable; version the catalog.
- **Do not change:** Section J's BLOCKING flags without the accountable owner.
- **Tests to add:** none here (the catalog IS the index of tests).
- **Edge cases:** local discovery may reveal existing equivalent tests — map, don't duplicate.
- **Manual validation:** every §18-1 matrix case (a–d) appears; every Section Q test item appears.
- **Expected outcome:** versioned catalog.
- **Failure signs:** catalog entries without requirement-section traceability.
- **Common mistakes:** catalog drifting from Section J numbering.
- **Completion criteria:** published, owned.
- **Stop condition:** published.
- **Next task:** CA-8.

### CA-8 — Author runbook stubs

- **Task ID:** CA-8
- **Title:** Runbook stubs: one per §15 alert + the aged-MAYBE runbook (§16.6 artifact 7)
- **Classification:** §16.6 companion artifact + operational runbook
- **Purpose:** §15 requires every alert definition to carry a runbook link; §16.6-7 also names the unqueryable-aged-MAYBE runbook (platform-side lookup → TL-10 rejection or the apply-platform-verified-outcome operation). The §5.2 restore runbook is POST-MVP and only stubbed as "major incident — manual engine-side reconciliation" per §5.2's MVP scope.
- **Prerequisites:** Section N (this playbook) drafted; OB-xx alert names as they land.
- **Requirement sections / concepts to read:** §15 (list + rollup + practices), §16.6 artifact 7, §9.3 (ops actions), §5.2 (MVP scope statement).
- **Placeholder components involved:** [Metrics / Alerting Layer].
- **Local placeholder mappings required before starting:** none for stubs.
- **Local code areas to discover:** none.
- **How to locate:** n/a.
- **Implementation instructions:** per §15 alert: a stub with Trigger / Severity / Why it matters / Immediate operator action / Data to collect / Escalation target / Safe stop condition (Section N provides the content for the major ones); the aged-MAYBE runbook per §16.6-7; known-outage suppression semantics (§15 rollup) documented here.
- **Do not change:** alert semantics.
- **Tests to add:** none.
- **Edge cases:** alerts whose operator action is "nothing local — investigate in the payment platform" must SAY so explicitly.
- **Manual validation:** ops-owner review.
- **Expected outcome:** stub per alert, linked from alert definitions (OB-06 wires links).
- **Failure signs:** stubs that instruct disabling guards/triggers (forbidden — §9.3 passes guards legitimately).
- **Common mistakes:** writing the full §5.2 DR runbook (post-MVP — do not).
- **Completion criteria:** stubs published.
- **Stop condition:** published.
- **Next task:** CA-9.

### CA-9 — Author the apply-platform-verified-outcome operation spec

- **Task ID:** CA-9
- **Title:** apply-platform-verified-outcome OPERATION spec (authorized application endpoint — 2026-07-11 execution boundary) + ops drill script (§16.6 artifact 8)
- **Classification:** §16.6 companion artifact + §18 BLOCKING item 3 input
- **Purpose:** OP-01 implements exactly this spec: signature, dual-control enforcement, evidence-flag mechanics, refusal conditions, audit fields, drill script.
- **Prerequisites:** B-04 (path confirmed); CA-4 (trigger/evidence-flag mechanics defined there must match).
- **Requirement sections / concepts to read:** §9.3 (full procedure design), §10.1, §10.3 (evidence flag + backstops), §20-8 (audit/ticket rule), §16.6 artifact 8, §18-3.
- **Placeholder components involved:** [Operator Admin Procedure Area].
- **Local placeholder mappings required before starting:** none for authoring.
- **Local code areas to discover:** none.
- **How to locate:** n/a.
- **Implementation instructions:** specify (round-4 canonical model — execution input is the approval_id, NEVER approver-identity parameters): the §9.3 two-step approval workflow — approval-record schema + PENDING→APPROVED→CONSUMED state machine (plus REJECTED/EXPIRED; version column, UNIQUE nonce), binding fields (request_id, outcome, parameter hash, ticket, environment, expiry, nonce; + content digest for reprocess), approver ≠ initiator derived from session identities, and ATOMIC consumption (the CONSUMED CAS commits in the SAME transaction/session as the payment transition — refusal/exception rolls back both); sets the evidence session flag; applies via the SAME evidence-guarded CAS as feed evidence; EXECUTED → outcome=EXECUTED, SUB=SUBMITTED, amount equality enforced, +confirmed; REJECTED → outcome=REJECTED, provider_rejected marker (L9), −committed; refuses CLAIMED and terminal rows and amount mismatch; every use → §15 alert; log line carries trigger_source=OPS_PLATFORM_VERIFIED + ticket ref; restricted role; drill script = end-to-end rehearsal steps on a seeded row in a non-prod environment.
- **Do not change:** §9.4's single-sanctioned-exception framing — the operation is the ONLY manual path.
- **Tests to add:** none here (OP-02).
- **Edge cases:** platform amount differs from request amount → NOT applicable here; that is the §8 AMOUNT_MISMATCH defect path (spec is explicit).
- **Manual validation:** DBA + ops-owner review; the approval store (ops schema) + session-identity plumbing confirmed workable in the real environment (from D-10 — else UNCLEAR flagged); the signed-assertion alternative documented as GATED, not offered to the implementer.
- **Expected outcome:** implementable spec + drill script.
- **Failure signs:** dual control specified as runbook convention instead of procedure-enforced.
- **Common mistakes:** allowing outcome values beyond EXECUTED/REJECTED.
- **Completion criteria:** spec published.
- **Stop condition:** published; OP-01 unblocked.
- **Next task:** S-01 (Phase P3).


---

## Phase handoff summary (P2 → P3)

- **Phase outputs:** written answers/records for §18 items 0–3 (B-01..B-04); companion artifacts CA-1..CA-9 authored, owned, versioned.
- **Blockers to carry forward:** any unanswered §18 item keeps its dependents BLOCKED — §18-0's residue blocks IN-02 ONLY (the §6 consumer freeze; the scope model is a settled §1 contract fact, so S-02/S-03/S-05, K-02/K-03 and the CA-4/CA-5 freeze are NOT gated — normalized 2026-07-11); §18-1 blocks go-live (CT proof) and P10 auto-downgrade reliance; §18-2 blocks RC-04 cutoff config; §18-3 default path = OP-01..03.
- **Local mapping rows expected filled:** none new (document phase).
- **Tests expected to exist:** none new; CA-5 golden vectors DRAFTED (executed as tests in P4); CA-7 catalog seeded from the test matrix.
- **Next phase entry condition:** CA-4 published (DBA-reviewed) → schema freeze may proceed (S-01). B-01's residue continues in parallel and gates IN-02, not schema (normalized 2026-07-11).
