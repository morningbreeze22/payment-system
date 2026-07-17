> **Purpose:** Task cards S-01..S-10 + AUD-01 (schema and migration foundation; S-10 = trade_snapshot_state; AUD-01 = the §14.1 attempt-journal schema, off-chain; the former S-11 bootstrap was RETIRED round 10 — §2.4 greenfield fact) (original Section H, phase P3).
> **When to use this file:** When executing the tasks of this phase, one card at a time, with the matching packet file from 09-minimal-context-packets/.
> **Depends on:** 08-task-cards/README.md; 01-playbook-index.md; 07-placeholder-glossary.md; the requirement sections cited per card; the locally filled mapping template.
> **Used by:** The local coding agent executing phase P3.
> **Safe to transfer:** yes
> **Contains local code names:** no

## H-Phase 3 — Schema and migration (P3)

### S-01 — Schema gap plan (freeze)

- **Task ID:** S-01
- **Title:** Freeze the migration plan: ordered migration list from the D-02 gap inventory + CA-4 spec
- **Classification:** MVP normative implementation
- **Purpose:** one ordered, expand/contract-safe migration sequence before any DDL is written.
- **Prerequisites:** CA-4 published; D-02 done. (B-01 residue NOT required — the scope model is a settled §1 contract fact; B-01 gates the §6 consumer freeze IN-02, not schema. Normalized 2026-07-11.)
- **Requirement sections / concepts to read:** §16.5 (expand/contract), CA-4, D-02 gap inventory (local).
- **Placeholder components involved:** [DB Migration Directory].
- **Local placeholder mappings required before starting:** [DB Migration Directory] Confirmed.
- **Local code areas to discover:** migration numbering/naming convention.
- **How to locate:** F.17 findings.
- **Implementation instructions:** write the ordered migration list (numbers reserved, one concern per migration): obligation columns → request columns → inbox table → UNIQUEs/I6 → CHECKs (NOVALIDATE) → triggers → indexes → backfill → VALIDATE. Each entry: DDL summary, rollback note, dual-run compatibility note (old app version must still run — §16.5). The plan includes the M.1a reader-first ladder decision (round 14): discovery evidence of current-reader behavior for ui_step_status decides N/A vs compatibility-release-first — CANCELLED is never written while an incompatible reader is live.
- **Do not change:** any existing migration file.
- **Tests to add:** none (plan task).
- **Edge cases:** columns that exist with wrong type/semantics (from D-02) get their own expand/contract sub-sequence (add new column → dual-write → migrate readers → drop later, drop deferred to post-rollout).
- **Manual validation:** plan reviewed by the human owner + DBA.
- **Expected outcome:** frozen migration plan.
- **Failure signs:** one mega-migration; destructive ALTERs on live columns.
- **Common mistakes:** planning VALIDATE before backfill.
- **Completion criteria:** plan recorded locally next to the mapping doc.
- **Stop condition:** plan approved.
- **Next task:** S-02.

### S-02 — Obligation table migrations

- **Task ID:** S-02
- **Title:** Add/align payment_obligation columns per §2.1
- **Classification:** MVP normative implementation
- **Purpose:** land the §2.1 fields: amounts, overpay_blocked, next_request_seq, upstream_ordering, correlation_id, ordering-tagged markers (validation_failed_at/_ordering, provider_rejected_at/code/_ordering), provider_reject_count, validation_reject_count, validation_failed_first_at, reopened_at, read-model fields (ui_step_status, active_exception_*, ops_annotation, ui_process_instance_id, ui_step_instance_id) — all additive, nullable-with-default first. ui_step_status stored set = IN_PROGRESS/COMPLETED/CANCELLED (round 12 — the §4.1 CANCELLED branch is a STORED value: the Java enum and the serialization/API contract carry it here; the DB CHECK is S-05's, round 13 — S-02 owns COLUMN/entity/Java-enum/API compatibility ONLY; rollout-safe per §16.5 — an old application version must tolerate READING CANCELLED during expand/contract; add contract tests that persist, read, and serialize a CANCELLED value; "roll back" in these tests means application-version compatibility / transaction rollback, NEVER data un-migration).
- **Prerequisites:** S-01. (Scope key is FINAL per the §1 snapshot model — settled contract fact, no discriminator; B-01's open residue does not touch it. Normalized 2026-07-11.)
- **Requirement sections / concepts to read:** §2.1 (whole), §16.5.
- **Placeholder components involved:** [DB Migration Directory], [Obligation Repository] (entity mapping only).
- **Local placeholder mappings required before starting:** [DB Migration Directory], [Obligation Repository].
- **Local code areas to discover:** obligation entity/table DDL.
- **How to locate:** D-02/D-03 findings.
- **Implementation instructions:** one migration (or few, per S-01 plan): add each missing §2.1 column nullable/defaulted; scope-key UNIQUE constraint per B-01 decision (NOVALIDATE if legacy rows could violate — investigate first); CHECK amounts >= 0 (NOVALIDATE→validate per plan); index on business_id (card lookup, §2.1/§12). Update the entity mapping additively; no behavior changes in this task.
- **Do not change:** existing column semantics; required_amount writers (later tasks).
- **Tests to add:** migration applies on clean schema and on a prod-shaped copy; entity round-trip persists new columns.
- **Edge cases:** duplicate scopes already in data (would break the UNIQUE) → STOP, report — this is data reconciliation for the human owner, not an agent decision.
- **Manual validation:** describe-table output matches CA-4 for §2.1.
- **Expected outcome:** obligation table at target shape (constraints may still be NOVALIDATE).
- **Failure signs:** ORA errors during apply on prod-shaped copy; entity mapping drift breaking existing tests (D-11 baseline).
- **Common mistakes:** NOT NULL on new columns with existing rows; renaming existing columns (never — add + migrate).
- **Completion criteria:** migration merged; D-11 baseline still green.
- **Stop condition:** applied + green.
- **Next task:** S-03.

### S-03 — Request table migrations

- **Task ID:** S-03
- **Title:** Add/align payment_request columns per §2.2 (dimensions + supporting fields)
- **Classification:** MVP normative implementation
- **Purpose:** land stage, stage_state, submission_state, outcome, blocked_reason, amount, idempotency_key/end_to_end_id, uetr, version, claim fields, retry fields, next_query_at, created_at, state_changed_at, creating_ordering, provider_reference, last_sent_hash, divergence_expected, divergent_payload_at, maybe_since, escalated_at, submitted_at, last_post_attempt_at — additive, nullable first.
- **Prerequisites:** S-02.
- **Requirement sections / concepts to read:** §2.2 (whole, incl. timestamp discipline), §16.5.
- **Placeholder components involved:** [DB Migration Directory], [Request Status Persistence Layer] (entity only).
- **Local placeholder mappings required before starting:** both above.
- **Local code areas to discover:** request entity/table DDL.
- **How to locate:** D-02/D-04.
- **Implementation instructions:** per S-01 plan; every new column nullable (backfill in S-08 populates dimensions for legacy rows); NO CHECKs yet (S-05); entity mapping additive.
- **Do not change:** the legacy status column (it remains until P14 contract phase; §10.4 keeps it as display only).
- **Tests to add:** migration apply tests; entity round-trip.
- **Edge cases:** amount column exists with different scale/precision → expand/contract sub-sequence per S-01; created_at may exist under another name — map, don't duplicate blindly (record choice).
- **Manual validation:** describe-table matches CA-4 for §2.2.
- **Expected outcome:** request table at target column shape.
- **Failure signs:** baseline tests broken by mapping changes.
- **Common mistakes:** making dimension columns NOT NULL before backfill.
- **Completion criteria:** migration merged; baseline green.
- **Stop condition:** applied + green.
- **Next task:** S-04.

### S-04 — Inbox table + purge

- **Task ID:** S-04
- **Title:** Create processed_inbound_event per §2.3 + purge job skeleton
- **Classification:** MVP normative implementation
- **Purpose:** cheap dedup of identical feed redeliveries; purge with retention > max replay window.
- **Prerequisites:** S-01.
- **Requirement sections / concepts to read:** §2.3 (exact DDL is given in the spec), §16.2 (retention chain).
- **Placeholder components involved:** [DB Migration Directory], [Inbox / Processed Event Repository].
- **Local placeholder mappings required before starting:** [DB Migration Directory]; F.8 status.
- **Local code areas to discover:** any existing dedup store (F.8).
- **How to locate:** F.8.
- **Implementation instructions:** if MISSING: create the table exactly per §2.3 (PK (source, event_id); processed_at TIMESTAMP(6) DEFAULT SYS_EXTRACT_UTC(SYSTIMESTAMP) NOT NULL); add a scheduled purge job skeleton (delete older than configured retention; config entry per §16.6, owner per §16.2); if PARTIAL: expand/contract to the §2.3 shape.
- **Do not change:** existing dedup layers until IN-05 consolidates consumption order.
- **Tests to add:** duplicate-key insert returns cleanly; purge deletes only beyond retention.
- **Edge cases:** deliberately NO parked-event table alongside (§2.3 — SPEC_CONFLICT if anything asks for one).
- **Manual validation:** table exists; purge dry-run deletes expected rows on seeded data.
- **Expected outcome:** inbox ready for IN-05.
- **Failure signs:** purge retention < Kafka topic retention (violates the §16.2 chain).
- **Common mistakes:** making event_id globally unique instead of per (source, event_id).
- **Completion criteria:** merged + green.
- **Stop condition:** applied.
- **Next task:** S-10.

### S-10 — trade_snapshot_state table (admission gate — round 5)

- **Task ID:** S-10
- **Title:** Create trade_snapshot_state per §2.4 (trade-level snapshot admission row)
- **Classification:** MVP normative implementation
- **Purpose:** §2.4/§6.1 (round 5): the trade-level watermark + applied-snapshot pointer. Money safety, not audit — without it, a delayed older snapshot can CREATE a never-seen scope and pay a payment the newer authoritative snapshot says does not exist; it is also the durable pointer §7.0 instruction assembly reads for "the most recent snapshot".
- **Prerequisites:** S-01.
- **Requirement sections / concepts to read:** §2.4 (field list is in the spec), §6.1 (ADMISSION — the consumer of this table), §6.7 (pluggable ordering comparator — the column must store whatever it compares), §7.0 (read path).
- **Placeholder components involved:** [DB Migration Directory], [Obligation Repository] (or a sibling repository area).
- **Local placeholder mappings required before starting:** [DB Migration Directory].
- **Local code areas to discover:** none new.
- **How to locate:** n/a.
- **Implementation instructions:** create the table per §2.4: business_id PRIMARY KEY; last_accepted_ordering in the SAME representation the §6.7 comparator uses (business timestamp today — the future explicit-sequence cutover must not need a table rebuild: keep the comparison in code, the column representation-agnostic); last_xml_storage_id (+ version if the store separates them, ask 8); last_payload_digest (same canonical algorithm as the §9.3 approval digest — one shared implementation, never two); updated_at (DB time). Repository exposes exactly: insert-if-absent (PK race → retry + re-read) and SELECT ... FOR UPDATE by business_id — the §6.1 admission transaction is the ONLY writer.
- **Do not change:** the three existing tables (no obligation column for the trade reference — §7.0 reads the stored snapshot instead, PO-confirmed round 5).
- **Tests to add:** duplicate-insert race returns cleanly (retry + re-read); FOR UPDATE blocks a concurrent admission for the same trade; a different trade is not blocked.
- **Edge cases:** the row is created on FIRST contact — including a failed-validation first message? NO: §6.6/§6.1 — a failed-validation message never advances (or creates) the admission row; only a schema-valid document reaches admission.
- **Manual validation:** table + PK visible; seeded two-session FOR UPDATE demo.
- **Expected outcome:** IN-02's admission gate has its lock and memory.
- **Failure signs:** any second writer path; the digest computed by a second implementation.
- **Common mistakes:** treating this as audit history (it is ONE row per trade, overwritten — not an append log); adding columns "while we're here" (SPEC_CONFLICT).
- **Completion criteria:** merged + green.
- **Stop condition:** applied.
- **Next task:** S-05.

### S-05 — Constraints: CHECKs, UNIQUEs, I6

- **Task ID:** S-05
- **Title:** Add enum CHECKs, L-shape CHECKs (L1-shape, L2–L8), UNIQUE(idempotency_key), UNIQUE(uetr), I6 function-based unique index
- **Classification:** MVP normative implementation
- **Purpose:** make illegal states unrepresentable at the DB — the backstop for every invariant the code enforces (§2.2, §10.3).
- **Prerequisites:** S-03; S-08 backfill DONE for any constraint that legacy rows could violate (apply NOVALIDATE first otherwise, per S-01 plan).
- **Requirement sections / concepts to read:** §10.3 (matrix, incl. what a CHECK can/cannot see), §2.2 constraints block, CA-4.
- **Placeholder components involved:** [DB Migration Directory].
- **Local placeholder mappings required before starting:** [DB Migration Directory]; real Oracle test lane (from D-11 — if H2-only, STOP: lane gap must be fixed first, record under S-09).
- **Local code areas to discover:** none new.
- **How to locate:** n/a.
- **Implementation instructions:** per CA-4: per-column enum CHECKs for the four dimensions + blocked_reason + the obligation's ui_step_status CHECK — IN ('IN_PROGRESS','COMPLETED','CANCELLED') (round 13: this CHECK lands HERE, not in S-02; §2.1); L2 (CONFIRM ⇒ SUB >= MAYBE), L3 (SUB >= MAYBE ⇒ stage >= POST), L4 (EXECUTED ⇒ SUBMITTED), L5 (CONFIRM ⇒ stage_state IN (READY, BLOCKED)), L6 (CLAIMED ⇔ claim fields set), L7 (RETRY_WAIT ⇒ next_retry_at), L8 (BLOCKED ⇔ blocked_reason), L1-shape (outcome set ⇒ stage_state READY ∧ claim/retry/blocked fields NULL); UNIQUE(idempotency_key); UNIQUE(uetr) via NULL-ignoring index; I6 unique function index CASE WHEN outcome IS NULL THEN payment_obligation_id END. NOVALIDATE→VALIDATE sequencing per S-01.
- **Do not change:** L9 (cross-table — code + drift scanner, NOT a CHECK; do not attempt).
- **Tests to add:** one violation test per constraint (insert/update illegal row → ORA error); ui_step_status CHECK violation test (a fourth value refused — round 13); I6 test (second active request for same obligation rejected); uetr NULL-multiplicity test.
- **Edge cases:** legality encodings must match the enum ordering assumptions ("SUB >= MAYBE" needs an explicit encoding — CA-4 defines it; test both sides of each boundary).
- **Manual validation:** user_constraints/user_indexes listing matches CA-4.
- **Expected outcome:** DB rejects every L2–L8/L1-shape violation.
- **Failure signs:** VALIDATE fails on legacy rows (backfill incomplete — go back to S-08).
- **Common mistakes:** implementing dimension comparisons with string inequality instead of the CA-4 encoding.
- **Completion criteria:** all constraints VALIDATED (or explicitly staged NOVALIDATE with a dated follow-up); violation tests green on real Oracle.
- **Stop condition:** merged + green.
- **Next task:** S-06.

### S-06 — Trigger backstops: L1 freeze + release guard

- **Task ID:** S-06
- **Title:** Create the L1-freeze trigger and the release-guard trigger with evidence session flag
- **Classification:** MVP normative implementation
- **Purpose:** §10.3: the FREEZE is a transition property no CHECK can see — an UPDATE trigger rejects any dimension change on a row whose outcome was already non-NULL; the release-guard trigger rejects a terminal-negative outcome write on a MAYBE/SUBMITTED row unless the session context carries the evidence flag (set by the authoritative-negative code path or the §9.3 operation). Raw fat-finger SQL fails loudly.
- **Prerequisites:** S-05; CA-4 (mechanics); D-02 (trigger privileges confirmed).
- **Requirement sections / concepts to read:** §10.3 (backstop paragraphs), §10.1 (release guard), §9.3 (legitimate flag setters).
- **Placeholder components involved:** [Stored Procedure / Trigger Area], [DB Migration Directory].
- **Local placeholder mappings required before starting:** both; Oracle session-context facility confirmed (D-10/D-02 — else BLOCKED).
- **Local code areas to discover:** how the app sets DB session state per transaction (connection pooling interaction — MUST_VERIFY_LOCALLY).
- **How to locate:** data-source/session customizer config.
- **Implementation instructions:** freeze trigger: BEFORE UPDATE, if :old.outcome IS NOT NULL and any dimension column changes → raise. Release-guard trigger: BEFORE UPDATE, if :new.outcome IN (terminal-negative set) and :old.submission_state IN (MAYBE_SUBMITTED, SUBMITTED) and evidence flag not set in session context → raise. Evidence-flag mechanics per CA-4: set by the authoritative-negative code path within the transaction, cleared with it; the §9.3 operation is the single legitimate MANUAL setter. Pool-safety: the flag must be transaction-scoped or explicitly cleared — verify with the real pool.
- **Do not change:** application transaction managers; other triggers.
- **Tests to add:** on real Oracle: dimension update on terminal row → rejected; terminal-negative on MAYBE row without flag → rejected; same WITH flag (set the way the code path will) → accepted; flag does not leak across pooled connections (two-session test).
- **Edge cases:** the outcome-setting transaction itself normalizes stage_state/claim fields (§10.2) — the freeze trigger must permit the outcome-setting UPDATE itself (fires on rows ALREADY terminal, i.e. :old.outcome NOT NULL).
- **Manual validation:** manual SQL attempt in a dev session fails loudly (demonstrate once, record output locally).
- **Expected outcome:** backstops live.
- **Failure signs:** flag leakage across pooled connections (the two-session test exists for this).
- **Common mistakes:** guarding only some terminal-negative values; comparing :new instead of :old submission_state.
- **Completion criteria:** trigger tests green on real Oracle.
- **Stop condition:** merged + green.
- **Next task:** S-07.

### S-07 — Active-row-bounded index set

- **Task ID:** S-07
- **Title:** Create one index per standing scan, ACTIVE-ROW-BOUNDED via the I6 function-index trick
- **Classification:** MVP normative implementation
- **Purpose:** §16.6-4: every scheduled scan's plan independent of terminal-row count — expressions NULL for terminal rows.
- **Prerequisites:** S-05.
- **Requirement sections / concepts to read:** §16.6 artifact 4 (index list), §9.5 (sweep order: oldest maybe_since first — round 10, no cutoff), §15 (scan scopes).
- **Placeholder components involved:** [DB Migration Directory].
- **Local placeholder mappings required before starting:** [DB Migration Directory].
- **Local code areas to discover:** none new.
- **How to locate:** n/a.
- **Implementation instructions:** per CA-4's normative list, one function-based index per scan, each keyed with CASE WHEN outcome IS NULL THEN <scan expression> END: resolver sweep (submission_state + next_query_at), retry scanner (stage_state + next_retry_at), escalation scanner (submission_state + maybe_since), BLOCKED queue (stage_state + state_changed_at), stuck-state (stage/stage_state + anchor), drift (obligation id over active rows), §5.2 created_at window (created_at — plain index acceptable: terminal rows are IN scope for that future query per §5.2 step 5; follow CA-4).
- **Do not change:** existing indexes without plan analysis.
- **Tests to add:** plan assertions (EXPLAIN) for each scanner's query using the index on a dataset seeded with many terminal rows.
- **Edge cases:** Oracle needs the QUERY expression to match the INDEX expression exactly — scanner queries (later tasks) must be written against these expressions; record the exact expressions in the mapping doc for RC-04/RC-05/RC-08/OB-01 to reuse.
- **Manual validation:** EXPLAIN output reviewed.
- **Expected outcome:** scan plans bounded by active-row count.
- **Failure signs:** full scans on the request table in any scanner plan.
- **Common mistakes:** functionally-equivalent-but-textually-different expressions in queries (index unused).
- **Completion criteria:** indexes merged; plan tests green.
- **Stop condition:** merged.
- **Next task:** S-08.

### S-08 — Backfill factored dimensions for existing rows

- **Task ID:** S-08
- **Title:** Backfill stage/stage_state/submission_state/outcome (+ anchors where derivable) from the legacy status for existing rows; re-derive the obligation read-model status for every existing obligation (round 14)
- **Classification:** MVP normative implementation
- **Purpose:** existing rows must satisfy the constraints before VALIDATE and behave correctly under new rules.
- **Prerequisites:** S-03; D-04 (legacy status meanings memo).
- **Requirement sections / concepts to read:** §10.4 (label ↔ tuple mapping — read it in REVERSE as the backfill map), §10.2 (outcome normalization shape), §2.2 anchors.
- **Placeholder components involved:** [DB Migration Directory], [Request Status Persistence Layer].
- **Local placeholder mappings required before starting:** legacy status value list with meanings (D-04) — if any legacy value has no confident tuple mapping, that value's rows are BLOCKED: report, do not guess.
- **Local code areas to discover:** none new.
- **How to locate:** n/a.
- **Implementation instructions:** write the legacy→tuple mapping table locally (reviewed by the human owner BEFORE running); backfill via idempotent migration or supervised script: dimensions per mapping; submission_state conservatively (any legacy state that could have reached the wire and lacks definitive evidence → MAYBE_SUBMITTED, per §7.1's definitions — fail toward resolver, never toward NOT_SUBMITTED); anchors: maybe_since/submitted_at set to a defensible timestamp (e.g. legacy state-change time if one exists, else backfill run time — record choice); terminal rows normalized per L1 shape. OBLIGATION READ-MODEL BACKFILL (round 14): after the dimension backfill, re-derive ui_step_status + active_exception_* for EVERY existing obligation row by RUNNING THE SHARED §4 DERIVATION against canonical state (controlled batches, obligation lock per row, idempotent) — NEVER mapped from the legacy display label where the money predicate is directly evaluable; branch examples: anchor (required NULL, marker live) → IN_PROGRESS + DATA_VALIDATION_FAILED; fully paid → COMPLETED; ordinary partial → IN_PROGRESS; zeroed removal (0/0/0) → CANCELLED; latched overpay → IN_PROGRESS + OVERPAY_DETECTED. GREENFIELD note (§2.4): if the flow's tables started empty, this pass is a cheap no-op — run it anyway; the ZERO-NULL evidence it produces is required either way.
- **Do not change:** legacy status values themselves (dual-run reads them until P14).
- **Tests to add:** backfill idempotency (re-run = no-op); per-legacy-value spot checks; post-backfill constraint dry-validate; obligation read-model pass: idempotency + one test per branch example + post-pass ZERO obligation rows with NULL ui_step_status (round 14).
- **Edge cases:** in-flight rows DURING backfill (dual-write not yet on) — run in a quiet window per the S-01 plan; rows whose legacy status contradicts money fields → list for human review, skip, report; rows written by OLD writers during dual-run may carry NULL ui_step_status until the M.3 catch-up pass; round 15: the FINAL pass runs only after the writer fleet is drained AND old versions are fenced from reconnecting (M.3 fenced cutover — a batch cannot hold an invariant while a violating writer lives).
- **Manual validation:** counts per (legacy value → tuple) reviewed; anomalies list empty or owned.
- **Expected outcome:** all rows carry valid tuples; S-05 VALIDATE can proceed.
- **Failure signs:** any row with dimensions violating L2–L8 after backfill.
- **Common mistakes:** optimistic NOT_SUBMITTED backfills (the pay-twice direction — §7.1's criterion is "provably cannot execute").
- **Completion criteria:** backfill complete; anomaly list dispositioned; constraints validated.
- **Stop condition:** validated.
- **Next task:** S-09. (S-11 was RETIRED round 10 — §2.4 greenfield fact: nothing to bootstrap.)

### S-09 — Migration test pass

- **Task ID:** S-09
- **Title:** Full migration test pass: clean schema, prod-shaped schema, dual-run compatibility
- **Classification:** MVP normative implementation
- **Purpose:** prove the whole P3 sequence per §16.5 before any behavior change lands on it.
- **Prerequisites:** S-02..S-08 + S-10 merged.
- **Requirement sections / concepts to read:** §16.5 (expand/contract, claim compatibility across one release boundary).
- **Placeholder components involved:** [DB Migration Directory], [Integration Test Suite].
- **Local placeholder mappings required before starting:** real-Oracle test lane available (if D-11 found H2-only, FIRST set up the Oracle lane — that setup is part of this task; split locally if large).
- **Local code areas to discover:** CI pipeline hooks for migration tests.
- **How to locate:** D-11 findings.
- **Implementation instructions:** run/automate: full sequence on clean Oracle; full sequence on a prod-shaped copy (with backfill); OLD application version boots and passes its smoke tests against the NEW schema (dual-run proof — additive columns must not break it); the CANCELLED-read proof is CONDITIONAL on the M.1a decision record (round 15): not-read branch → prove the old binary does not query/deserialize ui_step_status and record the read test as N/A; defensive-existing-reader branch → test the deployed old version directly; non-defensive branch → test the COMPATIBILITY RELEASE (the reader actually live at cutover), prove fleet-wide deployment, and separately prove the incompatible original is FENCED before any CANCELLED write; the report names the EXACT build/version tested — "old application version" is not evidence-grade; constraint violation suite (S-05/S-06 tests) in CI; evidence: ZERO obligation rows with NULL ui_step_status after the S-08 read-model pass (round 14).
- **Do not change:** migrations retroactively — fix-forward with new migrations only.
- **Tests to add:** the above as repeatable CI jobs where feasible.
- **Edge cases:** the old version writing rows WITHOUT new dimensions after backfill → those columns must stay nullable until the old version is gone (contract step deferred to P14 — record).
- **Manual validation:** results recorded (Section R report).
- **Expected outcome:** P3 proven; P4+ may build on the schema.
- **Failure signs:** old version fails against new schema (an expand/contract violation — fix the migration approach, don't patch the old version).
- **Common mistakes:** testing only clean-schema application.
- **Completion criteria:** all four proof points green.
- **Stop condition:** green; report filed.
- **Next task:** K-01.

### AUD-01 — Deploy the §14.1 attempt-journal schema (ops/audit schema)

- **Task ID:** AUD-01
- **Title:** payment_attempt_journal DDL + grants + partitioning + DB audit, exactly per §14.1/CA-10 (outside the §2 model and the S-chain; runs any time in/after P3)
- **Classification:** MVP normative implementation (§14.1).
- **Purpose:** the deployed store for the content write-ahead; K-04/RC-02/ST-10's riders insert into it.
- **Prerequisites:** CA-10 published (DBA + security review recorded); CA-1 published (outcome vocabulary source); the <request_id_type> discovery fact recorded (D-02); audit execution principal (AUDIT_ADMIN-class) + environment-qualified policy name + PDB/container scope recorded (DBA); CA-4 alignment; NOT part of the S-01..S-09 chain (own schema; the §2 model is untouched).
- **Requirement sections / concepts to read:** §14.1 (all), §2.2 (post_attempt_seq), §16.3 (security package), file 24 M9.
- **Placeholder components involved:** [DB Migration Directory].
- **Local placeholder mappings required before starting:** ops/audit schema name + tablespace; audit-role name; TDE availability (DBA answer).
- **Local code areas to discover:** none (new objects).
- **How to locate:** n/a.
- **Implementation instructions:** RESOLVE CA-10's DDL TEMPLATE into the migration (review 4d5cb83 M1): substitute EVERY placeholder from its recorded source (request-id type from D-02, outcome tokens from CA-1, DBA names/principal/scope) and record a SUBSTITUTION MANIFEST (fact → value → source); a PREFLIGHT check REJECTS the migration if ANY angle-bracket token remains. The unified-audit block is a DBA-EXECUTED, evidence-bound step (CREATE AUDIT POLICY needs AUDIT_ADMIN — never assume the app migration principal has it; review 4d5cb83 M2). Then run the resolved migration (no improvised types; review d00ef6a H2): SCALAR event-shape CHECK (paj_shape_ck) + the paj_content_bi BEFORE INSERT trigger for CLOB presence (Oracle forbids CHECK constraints referencing LOB columns — never "simplify" the trigger into a CHECK); BOTH global unique structures — paj_pk (journal_id) AND paj_pair_uq (request_id, post_attempt_seq, event_type) — with every retention drop using DROP PARTITION ... UPDATE GLOBAL INDEXES; monthly interval partitioning on occurred_at; SECUREFILE LOB clause + named tablespaces; local index on idempotency_key. GRANTS: INSERT to the application role, SELECT to the restricted audit role ONLY; no UPDATE/DELETE grants to application or reporting roles (owner/DBA access is change-controlled and audited — role-accurate wording, review c8a92f1 M3); enable CA-10's unified-audit policy (<app>_paj_access_pol, environment-qualified) via the DBA-executed step. The §14.1 ENABLEMENT SWITCH config (journal writes DEFAULT OFF in production until the Q30 journal items are evidenced). Retention job = partition drop per the compliance answer (until answered: retain). Document the restore posture per §14.1 (same database — full-DB PITR rewinds it; TSPITR/logical restores of the payment schema do not; the external §14 log is the only restore-surviving record). Also add post_attempt_seq to payment_request per §2.2 (a NUMBER default 0 — the ONE §2 column this task touches, coordinated with CA-4).
- **Do not change:** the four §2 tables beyond the §2.2 post_attempt_seq column; any payment-path code (riders belong to K-04/RC-02/ST-10).
- **Tests to add:** PREFLIGHT (review 928341a M2): the resolved migration contains ZERO angle-bracket tokens and the substitution manifest validates (every placeholder → value → source); T-38 schema slice: INSERT-only enforced (no UPDATE/DELETE grants to app/reporting roles); unique-pair constraint; scalar shape CHECK + content trigger reject malformed rows in BOTH directions (STARTED without/empty content; RESOLVED with content); partition-drop rehearsal with BOTH global indexes (paj_pk, paj_pair_uq) verified USABLE afterwards; app role cannot SELECT; T-38 G full set: audit policy ENABLED in the intended PDB/container (evidence captured); audit-role reads + app INSERTs + denied-access attempts + owner/DBA maintenance (incl. the partition-drop ALTER) all present in the unified audit trail.
- **Edge cases:** SECUREFILE compression is a licensing decision — record it, never assume; lower environments get the TABLE but never production data.
- **Manual validation:** DBA review of deployed objects + grants; security sign-off recorded (Q30 evidence).
- **Expected outcome:** journal deployed; riders unblocked.
- **Failure signs:** FK to payment_request; UPDATE grants; app role with SELECT; journal inside the payment schema.
- **Common mistakes:** putting the table in the §2 schema "for convenience"; skipping the DB-audit-on-reads step.
- **Completion criteria:** preflight (zero placeholders) + substitution manifest recorded; T-38 schema slice green incl. the full G audit-evidence set; grants verified role-accurately; audit-policy enablement + container evidence filed.
- **Stop condition:** merged; K-04/RC-02/ST-10 riders unblocked.
- **Next task:** none (off-chain; the S-chain and K-01 proceed independently).


---

## Phase handoff summary (P3 → P4)

- **Phase outputs:** four-table schema at the CA-4 target: columns, scope-key UNIQUE, UNIQUE(idempotency_key), NULL-ignoring UNIQUE(uetr), I6 function index, enum + L1-shape + L2–L8 CHECKs (VALIDATED), freeze + release-guard triggers with evidence-flag mechanics, active-row-bounded index set, inbox table + purge, trade_snapshot_state (S-10, §2.4 — greenfield: starts empty, no bootstrap), backfilled dimensions.
- **Blockers to carry forward:** §18-1/2/3 unchanged; any staged-NOVALIDATE constraint has a dated follow-up.
- **Local mapping rows expected filled:** [DB Migration Directory], [Stored Procedure / Trigger Area] CONFIRMED; index expressions recorded for later scanner queries (S-07 note).
- **Tests expected to exist:** migration apply (clean + prod-shaped), constraint violation suite, trigger backstop suite (incl. pool non-leakage), backfill idempotency, S-09 dual-run proof (old version runs on new schema).
- **Next phase entry condition:** S-09 all four proof points green.
