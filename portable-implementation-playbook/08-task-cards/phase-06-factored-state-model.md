> **Purpose:** Task cards ST-01..ST-11 (factored state model, CAS discipline, claims/leases, shutdown) (original Section H, phase P6).
> **When to use this file:** When executing the tasks of this phase, one card at a time, with the matching packet file from 09-minimal-context-packets/.
> **Depends on:** 08-task-cards/README.md; 01-playbook-index.md; 07-placeholder-glossary.md; the requirement sections cited per card; the locally filled mapping template.
> **Used by:** The local coding agent executing phase P6.
> **Safe to transfer:** yes
> **Contains local code names:** no

## H-Phase 6 — Factored state model and transitions (P6)

### ST-01 — Dual-write the dimension columns

- **Task ID:** ST-01
- **Title:** Every status writer also writes the four dimension columns (dual-write); legacy status becomes derived-display-bound
- **Classification:** MVP normative implementation
- **Purpose:** land the factored model additively: writers produce both representations during the migration window; no reader changes yet.
- **Prerequisites:** S-08/S-09 (columns + backfill); D-04 writer inventory.
- **Requirement sections / concepts to read:** §2.2 (dimensions), §10.4 (label mapping — used as the legacy-value bridge), §16.5 (dual-run).
- **Placeholder components involved:** [Request Status Persistence Layer].
- **Local placeholder mappings required before starting:** writer inventory (D-04) complete.
- **Local code areas to discover:** each writer site.
- **How to locate:** D-04 list.
- **Implementation instructions:** for each writer: compute the tuple for the transition it performs (per the reviewed S-08 mapping table) and write dimensions + legacy value together in the same UPDATE. Do not yet change any WHERE clause semantics (ST-02 does). Keep the mapping table as the single source (one local translation helper, not per-site literals).
- **Do not change:** transition SEMANTICS; readers.
- **Tests to add:** per-writer: resulting row has consistent (legacy, tuple) pair per the mapping table.
- **Edge cases:** writers reachable only via rare paths (ops scripts, error handlers) — the D-04 inventory's completeness is the protection; if a new writer is found now, ADD it to the inventory and this task.
- **Manual validation:** run the existing integration suite; sample rows show consistent pairs.
- **Expected outcome:** every write produces both representations.
- **Failure signs:** rows with tuple/legacy disagreement (the GO-02 shadow comparison will catch stragglers — but fix now).
- **Common mistakes:** per-site hand-rolled mappings drifting from the table.
- **Completion criteria:** all writers dual-write; baseline green.
- **Stop condition:** merged.
- **Next task:** ST-02.

### ST-02 — CAS discipline on dimension writes

- **Task ID:** ST-02
- **Title:** Every dimension-changing UPDATE becomes a conditional CAS: full precondition WHERE + outcome IS NULL + row-count verdict
- **Classification:** MVP normative implementation
- **Purpose:** §11: WHERE carries the full dimension precondition; row count is the verdict; every call site branches on rowCount == 1; universal `outcome IS NULL` implements the L1 freeze in code; no ORM dirty-checking on these tables.
- **Prerequisites:** ST-01.
- **Requirement sections / concepts to read:** §11 (rules), §10.3 (L1 freeze via CAS discipline), §10.1 (mirror rule).
- **Placeholder components involved:** [Request Status Persistence Layer].
- **Local placeholder mappings required before starting:** writer inventory; ORM-save sites flagged in D-04.
- **Local code areas to discover:** each writer's WHERE clause; obligation-lock acquisition around dimension changes.
- **How to locate:** D-04.
- **Implementation instructions:** convert each writer to a hand-written conditional UPDATE: WHERE id = ? AND outcome IS NULL AND <expected dimension values> (+ version where used); return row count; call site branches — rowCount 0 is a HANDLED outcome (stale/duplicate/racing event → per that flow's spec section), never an ignored one. Dimension-changing updates acquire the obligation lock first and re-derive in the same transaction (§11 — re-derivation itself may be a stub until RG-08/09 land; acquire-and-hook now). Claim-field-only updates may skip the lock (§11).
- **Do not change:** claim-only fast paths beyond adding the CAS shape; unrelated tables.
- **Tests to add:** row-count-0 on wrong precondition; late/duplicate write affects zero rows (mirror-rule test: "accepted" response against outcome=EXECUTED row → 0 rows, no regression); no dirty-checking (repository test ensuring explicit UPDATEs).
- **Edge cases:** transitions that legally change several dimensions at once (§7.2 rows) — one CAS carrying the whole tuple delta, preconditioned on the whole prior tuple.
- **Manual validation:** grep-level check locally: no save()-style persistence remains on the request table.
- **Expected outcome:** all dimension writes CAS-gated.
- **Failure signs:** call sites discarding row counts.
- **Common mistakes:** WHERE carrying only the id + version but not dimensions (version alone can't express evidence rules); forgetting outcome IS NULL.
- **Completion criteria:** writer audit clean; tests green.
- **Stop condition:** merged.
- **Next task:** ST-03.

### ST-03 — Legality-matrix conformance tests

- **Task ID:** ST-03
- **Title:** Test every code transition against L1–L8 and the per-dimension rules
- **Classification:** MVP normative implementation
- **Purpose:** prove code paths and DB constraints agree BEFORE behavior phases build on them.
- **Prerequisites:** ST-02; S-05/S-06.
- **Requirement sections / concepts to read:** §10.2, §10.3, §10.5 (flow table as the test seed).
- **Placeholder components involved:** [Integration Test Suite], [Request Status Persistence Layer].
- **Local placeholder mappings required before starting:** Oracle test lane.
- **Local code areas to discover:** none new.
- **How to locate:** n/a.
- **Implementation instructions:** table-driven test: each §10.5 flow row = one legal-transition case (before-tuple → event → after-tuple asserted); plus illegal cases: stage regression (except the sanctioned §9.2 move), outcome overwrite, CONFIRM with NOT_SUBMITTED (L2), MAYBE at ENRICH (L3), EXECUTED without SUBMITTED (L4), CONFIRM·RETRY_WAIT (L5), CLAIMED without claim fields (L6), RETRY_WAIT without next_retry_at (L7), BLOCKED without reason / reason without BLOCKED (L8) — each dies at code (row-count 0 / refused) or DB (constraint) — assert WHICH layer, both must hold where applicable.
- **Do not change:** production code except bugs this exposes (fix within the task if local and small; else report).
- **Tests to add:** the table-driven suite.
- **Edge cases:** the §9.2 downgrade row is added when RC-07 lands — leave a named pending case.
- **Manual validation:** coverage check: every §10.5 row has a test id.
- **Expected outcome:** transition surface pinned.
- **Failure signs:** cases passing only because DB constraints fire where code should have refused first (fix the code path; the DB is the backstop, §2.2).
- **Common mistakes:** testing through service facades that mask row-count semantics.
- **Completion criteria:** suite green, coverage recorded.
- **Stop condition:** merged.
- **Next task:** ST-04.

### ST-04 — Display label derivation

- **Task ID:** ST-04
- **Title:** Implement §10.4 labels as a derived view/expression; route dashboards/card/log/ops reads to it; implement the §12 ALL-PAYMENTS TABLE projection (request-granular read surface, 2026-07-17)
- **Classification:** MVP normative implementation
- **Purpose:** the old 13-value status survives ONLY as a derived display label; labels never appear in machine-consumed API payloads. The §12 table projection is the frontend's request-level view — defined here so no agent or frontend invents row semantics.
- **Prerequisites:** ST-01.
- **Requirement sections / concepts to read:** §10.4 (mapping + strictness), §2.2 ("no rule may key on it"), §12 (ALL-PAYMENTS TABLE projection block — the full row contract).
- **Placeholder components involved:** [Request Status Persistence Layer], [Metrics / Alerting Layer] (log line), card read path.
- **Local placeholder mappings required before starting:** reader inventory (D-04).
- **Local code areas to discover:** display readers of the legacy status.
- **How to locate:** D-04 reader list, display-flagged entries.
- **Implementation instructions:** implement the §10.4 mapping exactly (DB view or shared expression — choose per local convention, record); migrate DISPLAY consumers (dashboards, card payload's label field, log lines, ops queries you control) to it; the card read contract returns dimension columns + label per §10.4's rule (no consumer may parse the label). ALSO implement the §12 ALL-PAYMENTS TABLE projection exactly per its contract block: obligation LEFT JOIN request; row_type REQUEST (one per request, request id key) or OBLIGATION_ONLY (obligation id key, request fields n/a, reason = the derived active exception); obligation context on every row (required/committed/confirmed, ui_step_status, exception, reopened); REQUEST rows also carry required_total_at_creation (§2.2 — the UI amount series, 2026-07-19: pass the stored column through verbatim, NULL renders "not captured (pre-F0)"; NEVER compute or fill it in the projection; one stamp per payment_request row, NOT per POST attempt); no duplicates by join construction; terminal rows visible (client filtering allowed); removed scopes render CANCELLED context; ORDERING = THE CANONICAL KEYSET TUPLE byte-for-byte: (obligation_identity, row_type, request_seq NULLS FIRST, created_at NULLS FIRST, source_id) — the cursor encodes every term + NULL semantics; read-only, SHAPE-READ rules.
- **Do not change:** rule-keyed readers (ST-05's job); external report SQL you don't own (record as UNCLEAR for owners); the §4 derivations (the projection READS them, never recomputes).
- **Tests to add:** label mapping per §10.4 row; NEEDS_REVIEW includes blocked_reason display; the T-31 TABLE projection cases (no-request placeholder; placeholder gone on first request; 100+20 as two rows; mixed active/terminal; removed scope CANCELLED; reappearance).
- **Edge cases:** legacy display values with no §10.4 equivalent — map to the nearest label per the S-08 reviewed table; record each.
- **Manual validation:** card/dashboard smoke check in a local run.
- **Expected outcome:** display decoupled from stored legacy status.
- **Failure signs:** any API consumer parsing labels (grep for label literals in consumer-facing code you can see).
- **Common mistakes:** deriving the label from the legacy column instead of the dimensions.
- **Completion criteria:** display consumers on derived labels; tests green.
- **Stop condition:** merged.
- **Next task:** ST-05.

### ST-05 — Migrate rule sites off the legacy status

- **Task ID:** ST-05
- **Title:** Re-key every business-rule site from the legacy compound status to the correct dimension(s) — incrementally, site by site
- **Classification:** MVP normative implementation
- **Purpose:** §2.2/§10.4: no rule may key on the display status; the compound enum entangled four facts and caused the bug class v4 exists to kill. Existing code must migrate GRADUALLY and SAFELY.
- **Prerequisites:** ST-02, ST-03; D-04 rule-site inventory COMPLETE.
- **Requirement sections / concepts to read:** §2.2 (dimension meanings), §10.1 (blocked_reason rule), §4.5 (who-acts-next is DERIVED); per-site: the § governing that rule's meaning.
- **Placeholder components involved:** [Request Status Persistence Layer] + every component with a rule site.
- **Local placeholder mappings required before starting:** rule-site inventory with, per site, WHICH dimension the rule actually means (fill this classification locally as part of the task).
- **Local code areas to discover:** each site.
- **How to locate:** D-04 inventory.
- **Implementation instructions:** for each site (one commit-sized step per site or small cluster): decide the dimension the rule MEANS (money truth → submission_state; pipeline position → stage; claimability → stage_state; finality → outcome); rewrite the condition on that dimension; if a site appears to need the COMPOUND meaning, re-read the governing §: v4's position is that each rule means one dimension — if genuinely irreducible, mark UNCLEAR and report (do not invent a compound predicate). SPLIT LOCALLY as needed: this card is a template applied per site.
- **Do not change:** rule OUTCOMES (behavior-preserving re-keying; any semantic change discovered = report, possibly BUSINESS_RULE_CHANGE_REQUIRED).
- **Tests to add:** per site: a test pinning the rule's behavior before + after (same verdicts on a case matrix).
- **Edge cases:** sites keying on status STRINGS in SQL (jobs, monitors) — same migration, in SQL, using dimensions.
- **Manual validation:** decreasing count of legacy-enum usages tracked in the mapping doc per session.
- **Expected outcome:** zero rule sites keyed on legacy status (display-only remains via ST-04).
- **Failure signs:** a "temporary" compound helper reintroducing the entanglement.
- **Common mistakes:** mapping NEEDS_REVIEW-style rules to blocked_reason (FORBIDDEN — §10.1: no rule keys on blocked_reason; use stage_state = BLOCKED and durable facts).
- **Completion criteria:** inventory shows all sites migrated or UNCLEAR-reported.
- **Stop condition:** inventory empty (or fully dispositioned); report.
- **Next task:** ST-06.

### ST-06 — Outcome-write normalization (freeze convention)

- **Task ID:** ST-06
- **Title:** Every outcome-setting transaction normalizes the row: stage_state := READY, claim/retry/blocked fields cleared, maybe_since/escalated_at cleared
- **Classification:** MVP normative implementation
- **Purpose:** §10.2 outcome rule / L1 shape: terminal rows hold one canonical shape so L6/L7/L8 hold trivially; frozen rows keep submission_state; uncleared maybe_since would leave frozen rows on the MAYBE-age clocks (§2.2).
- **Prerequisites:** ST-02.
- **Requirement sections / concepts to read:** §10.2 (outcome block), §2.2 (maybe_since / escalated_at), §10.3 (L1 split).
- **Placeholder components involved:** [Request Status Persistence Layer].
- **Local placeholder mappings required before starting:** all outcome-writing sites known (subset of D-04 inventory).
- **Local code areas to discover:** outcome writers.
- **How to locate:** D-04.
- **Implementation instructions:** one shared normalization applied by every outcome-setting CAS (single helper): sets outcome + stage_state=READY + NULLs claimed_by/claim_expires_at/next_retry_at/blocked_reason + clears maybe_since/escalated_at + updates state_changed_at (then frozen — L1); submission_state untouched.
- **Do not change:** which events set which outcome (those live in their flow tasks).
- **Tests to add:** outcome write from each prior shape (CLAIMED, RETRY_WAIT, BLOCKED) → canonical terminal shape; terminal transition out of CLAIMED does not violate L6 (same transaction); frozen row absent from MAYBE-age scans (once RC-08 exists — pending named case).
- **Edge cases:** terminal write racing a claim — CAS precondition decides; loser sees row-count 0.
- **Manual validation:** row images inspected for each outcome path in a local run.
- **Expected outcome:** single canonical terminal shape.
- **Failure signs:** any outcome writer bypassing the helper.
- **Common mistakes:** clearing submission_state (frozen rows KEEP it — §10.2).
- **Completion criteria:** tests green; helper adopted by all outcome writers.
- **Stop condition:** merged.
- **Next task:** ST-07.

### ST-07 — Episode anchor stamping

- **Task ID:** ST-07
- **Title:** Stamp/clear the set-once episode anchors: maybe_since, submitted_at, escalated_at (clear rules), per §2.2
- **Classification:** MVP normative implementation
- **Purpose:** §2.2/§15 clock discipline: every AGE rule keys on a set-once anchor because state_changed_at churns; wrong anchors silently re-arm or never-fire alerts.
- **Prerequisites:** ST-02, ST-06 (clears on outcome).
- **Requirement sections / concepts to read:** §2.2 (maybe_since, submitted_at, escalated_at, last_post_attempt_at blocks), §15 (clock discipline).
- **Placeholder components involved:** [Request Status Persistence Layer].
- **Local placeholder mappings required before starting:** submission-state writers known.
- **Local code areas to discover:** every submission_state transition site.
- **How to locate:** ST-02's converted writers.
- **Implementation instructions:** maybe_since: set ONCE when submission_state first becomes MAYBE_SUBMITTED (not refreshed by churn), cleared when it leaves MAYBE and by outcome normalization; submitted_at: set when submission_state becomes SUBMITTED; escalated_at: written only by RC-08 (leave the column dormant, define the helper contract now: cleared with maybe_since); last_post_attempt_at: already stamped by K-05 (verify interaction only).
- **Do not change:** state_changed_at semantics (last-write clock only).
- **Tests to add:** MAYBE→(dimension churn)→still original maybe_since; leave-and-re-enter MAYBE → NEW maybe_since (new episode); SUBMITTED stamps submitted_at; outcome write clears both per ST-06.
- **Edge cases:** §7.4 downgrade-exhaustion keeps MAYBE — maybe_since must survive stage/stage_state churn throughout (assert).
- **Manual validation:** row inspection through a scripted churn sequence.
- **Expected outcome:** age anchors reliable.
- **Failure signs:** any age computation reading state_changed_at (grep locally when OB tasks land).
- **Common mistakes:** refreshing maybe_since on every MAYBE-preserving write.
- **Completion criteria:** tests green.
- **Stop condition:** merged.
- **Next task:** ST-08.

### ST-08 — Structured CAS log line

- **Task ID:** ST-08
- **Title:** Emit the §14 structured INFO line on every successful dimension-changing CAS
- **Classification:** MVP normative implementation
- **Purpose:** §14: the transition record — no TRANSITION-HISTORY journal replaces it (the switch-gated §14.1 attempt-content journal is a separate audit sink, never a log replacement — review 2b697fb M2): request_id, idempotency_key, request_seq, correlation_id, (stage, stage_state, submission_state, outcome) before → after, display label, trigger_source, trigger_event_id, plus post_attempt_seq + attempt_event_type on ATTEMPT-class lines (§14; exact tokens per b760786 M2); the ONLY restore-surviving forensic record — BEST-EFFORT-COMPLETE, never gapless (§14 delivery contract, review 4098532 H1; §5.2 step 5b treats its log-derived figure as a heuristic starting limit accordingly — never a bound, and never sole unfreeze authority).
- **Prerequisites:** ST-02 (all writers CAS'd), ST-04 (label).
- **Requirement sections / concepts to read:** §14 (whole), §16.3 (masking — no account data in the line).
- **Placeholder components involved:** [Request Status Persistence Layer], [Metrics / Alerting Layer].
- **Local placeholder mappings required before starting:** logging conventions (D-10).
- **Local code areas to discover:** MDC/correlation propagation.
- **How to locate:** F.21.
- **Implementation instructions:** one emission point in the shared CAS helper (fires only on rowCount==1) — and DELIVERY per the §14 contract: the helper BUFFERS the line and registers an after-commit callback (Spring TransactionSynchronization afterCommit) that publishes it; NEVER publish inside the transaction (a rollback must leave NO line); publication is at-most-once, never retried, and its failure NEVER fails or delays the transition (alert only); posting-claim ordering: claim commit → best-effort publication → provider call — publication failure does NOT block the call; fields exactly per §14; trigger_source = the flow (values per flow tasks; OPS_PLATFORM_VERIFIED reserved for OP-01); correlation_id from MDC; posting-claim line additionally carries last_sent_hash + attempt_count + post_attempt_seq (K-05 emits it — verify one convention, not two); EVERY ATTEMPT-class line (posting claim, response resolution, lease-expiry resolution) carries post_attempt_seq + attempt_event_type (§14 — these are the STABLE join keys to the §14.1 journal; attempt_count RESETS on §9.2 downgrade and can NEVER serve as the join key). attempt_event_type is the EXACT structured-field name and its values are BYTE-EQUAL to the journal's event_type tokens: posting claim = 'ATTEMPT_STARTED'; response resolution AND lease-expiry resolution = 'ATTEMPT_RESOLVED'; local vocabularies (POSTING_CLAIM / RESPONSE_RESOLVED / LEASE_EXPIRED and similar) are FORBIDDEN as values, and the field is never conflated with trigger_event_id.
- **Do not change:** log platform config beyond adding the line; retention (§14 floor) is an OB-05/owner item — record current retention vs the 90-day floor, report if below.
- **Tests to add:** log-capture test per transition family: line present, fields populated, before/after correct, no account data; ATTEMPT-class capture: the posting-claim, response-resolution, and lease-expiry-resolution lines each assert post_attempt_seq present AND the field attempt_event_type carrying the exact token — 'ATTEMPT_STARTED' on the claim line, 'ATTEMPT_RESOLVED' on both resolution lines — byte-equal to payment_attempt_journal.event_type; delivery-contract tests: a rolled-back CAS emits NO line (phantom impossible); simulated crash after commit before publication → committed row without a line is TOLERATED (gap, not a failure) and no phantom exists; publication failure (appender throws in the callback) → the transition commits unaffected and the failure is alerted.
- **Edge cases:** transitions inside batch scanners — line per row, not per batch.
- **Manual validation:** grep a local run by one correlation id: full story reads end to end (§15 practice).
- **Expected outcome:** forensic line live.
- **Failure signs:** line emitted on row-count-0 attempts (would fabricate history).
- **Common mistakes:** logging the instruction content (only the hash is permitted — §16.3).
- **Completion criteria:** tests green.
- **Stop condition:** merged.
- **Next task:** ST-09.

### ST-09 — Claims as leases

- **Task ID:** ST-09
- **Title:** Claim = CAS to CLAIMED + claimed_by + claim_expires_at (L6); scanners use the §11 claim protocol (lock-free selection → per-item obligation-first claim CAS), DB time, per-item transactions
- **Classification:** MVP normative implementation
- **Purpose:** §11: second scanner cannot re-claim mid-processing; stale workers are fenced by CAS row counts.
- **Prerequisites:** ST-02; D-08 job inventory.
- **Requirement sections / concepts to read:** §11 (claims, scanner rules), §2.2 (claim fields), L6.
- **Placeholder components involved:** [Retry Resolver Job], [Request Status Persistence Layer], stage workers.
- **Local placeholder mappings required before starting:** claim-column reality from D-08 (exists? semantics?).
- **Local code areas to discover:** current claim/pick-up logic in each worker/scanner.
- **How to locate:** D-08 inventory.
- **Implementation instructions:** standard claim CAS: READY/RETRY_WAIT(due) → CLAIMED + claimed_by + claim_expires_at, WHERE carries prior state + outcome IS NULL; work; completion CAS moves onward and NULLs claim fields (L6); scanners follow the §11 claim protocol (decided 2026-07-11, mechanics M5): candidate SELECT takes NO locks (no FOR UPDATE / SKIP LOCKED); per candidate a NEW transaction locks the OBLIGATION first, then runs the claim CAS — rowCount 0 = lost race, skip; claim/unclaim triggers no §4 re-derivation; next_retry_at compared against DB time; one transaction per item; lease durations per stage from config (§16.6).
- **Do not change:** what the workers DO with claimed rows.
- **Tests to add:** double-claim race (two scanners, one wins); stale-worker fence (expired worker's completion CAS → row-count 0); L6 both directions.
- **Edge cases:** worker completing exactly at expiry — CAS precondition includes claimed_by = self, so a re-claimed row fences the old worker regardless.
- **Manual validation:** two local scanner instances against seeded rows — no double processing.
- **Expected outcome:** lease discipline live.
- **Failure signs:** app-time comparisons (a §11 violation D-08 may have flagged).
- **Common mistakes:** releasing claims without CAS (blind NULLing).
- **Completion criteria:** tests green.
- **Stop condition:** merged.
- **Next task:** ST-10.

### ST-10 — Lease-expiry recovery

- **Task ID:** ST-10
- **Title:** Expired ENRICH claims re-claimable in place; expired POST claims → CONFIRM·READY·MAYBE_SUBMITTED, never re-claimed for posting
- **Classification:** MVP normative implementation
- **Purpose:** §11 claim-expiry recovery: a POST worker may have died before/during/after the wire — re-POSTing a possibly-sent payment is a double-payment path; NO "provably not launched" carve-out exists (identity is persisted in the claim transaction itself).
- **Prerequisites:** ST-09; ST-07 (maybe_since stamping on the MAYBE write).
- **Requirement sections / concepts to read:** §11 (claim-expiry recovery + rationale), §10.2 (SUB NOT→MAYBE on posting-claim lease expiry).
- **Placeholder components involved:** [Retry Resolver Job] (or a dedicated expiry sweep — follow local convention; record which).
- **Local placeholder mappings required before starting:** ST-09 claim shape.
- **Local code areas to discover:** where expiry detection best lives locally.
- **How to locate:** D-08.
- **Implementation instructions:** expiry sweep (or claim-time check): CLAIMED + claim_expires_at < DB now → ENRICH: CAS back to READY (clear claim fields); POST: CAS to stage=CONFIRM, stage_state=READY, submission_state=MAYBE_SUBMITTED, clear claim fields, stamp maybe_since (ST-07 helper). No exceptions, no carve-outs. §14.1 rider (switch-gated): the POST-expiry CAS also INSERTs ATTEMPT_RESOLVED with outcome LEASE_EXPIRED_MAYBE in the SAME transaction (rowCount==1 only — the CAS arbitrates the race with a slow-but-alive worker); the matching §14 log line (ST-08 convention) carries attempt_event_type = 'ATTEMPT_RESOLVED' — exact field name, byte-equal to the journal token, NOT 'LEASE_EXPIRED'. Canonical failure rule (§14.1): statement-local insert failures caught around the single statement, alerted AFTER host commit, the recovery proceeds; FATAL failures = ordinary infra failures; guarantee = no incorrect payment outcome.
- **Do not change:** CONFIRM-stage rows' claim semantics (resolver rows are not CLAIMED workers — §4.4 note).
- **Tests to add:** ENRICH expiry → re-claimable, work repeats safely (read-only); POST expiry → MAYBE row, resolver-owned; expired POST row NEVER selectable by the posting claim query (assert the claim WHERE excludes it structurally); T-38: exactly one ATTEMPT_RESOLVED per attempt when sweep and worker race (loser inserts nothing).
- **Edge cases:** worker still alive but slow past expiry — its completion CAS hits row-count 0 (fenced); test explicitly.
- **Manual validation:** kill a worker mid-POST locally (stub hang) → observe the row land CONFIRM·READY·MAYBE.
- **Expected outcome:** crash recovery per spec.
- **Failure signs:** any "we can prove it didn't launch" optimization (§11 forbids it by construction).
- **Common mistakes:** forgetting maybe_since at the expiry write.
- **Completion criteria:** tests green.
- **Stop condition:** merged.
- **Next task:** ST-11.

### ST-11 — Graceful shutdown ordering

- **Task ID:** ST-11
- **Title:** Shutdown: stop listeners → stop scanners → drain in-flight POSTs (bounded) → release only ENRICH claims; POST claims never released on shutdown
- **Classification:** MVP normative implementation
- **Purpose:** §11 graceful shutdown: a released POST claim could be re-claimed while the original POST is still in flight — double-payment path; lease expiry (→ MAYBE) is the only exit.
- **Prerequisites:** ST-09, ST-10.
- **Requirement sections / concepts to read:** §11 (shutdown block), §16.1 (drain semantics under freeze).
- **Placeholder components involved:** [Payment Status Feed Consumer] + upstream consumer (containers), [Retry Resolver Job], [Provider POST Client].
- **Local placeholder mappings required before starting:** container/lifecycle wiring (D-07/D-08).
- **Local code areas to discover:** Spring lifecycle hooks / SmartLifecycle phases in use.
- **How to locate:** application lifecycle config.
- **Implementation instructions:** ordered shutdown per §11's four steps; bounded drain wait on in-flight POSTs; explicit ENRICH-claim release (CAS, own claims only); POST claims left to expire.
- **Do not change:** container factory conventions beyond lifecycle ordering.
- **Tests to add:** shutdown during: idle (clean), in-flight ENRICH (claim released), in-flight POST (claim NOT released; row later expires to MAYBE — combine with ST-10's test rig).
- **Edge cases:** shutdown racing a posting-claim commit — the ambiguous-commit rule (K-04) already forbids proceeding; assert no wire call after the drain window.
- **Manual validation:** local SIGTERM run; log ordering inspected.
- **Expected outcome:** deploys never create double-payment exposure.
- **Failure signs:** POST claims released "to speed up recovery".
- **Common mistakes:** stopping scanners before listeners (order matters: no new inbound first — §11 stops listeners first).
- **Completion criteria:** tests green.
- **Stop condition:** merged; Phase P6 report.
- **Next task:** RG-01.


---

## Phase handoff summary (P6 → P7)

- **Phase outputs:** dual-written dimensions; universal CAS discipline (full-precondition WHERE + outcome IS NULL + row-count verdicts); legality suite green; derived display labels; legacy-status rule sites migrated (or dispositioned); outcome-write normalization; episode anchors; §14 CAS log line; claims as leases; lease-expiry recovery (POST → CONFIRM·READY·MAYBE, never re-claimed); graceful shutdown ordering.
- **Blockers to carry forward:** §18 items unchanged; ST-03 has a named pending case for the §9.2 downgrade transition (lands with RC-07).
- **Local mapping rows expected filled:** [Request Status Persistence Layer] fully mapped with writer inventory dispositioned; ST-05 rule-site inventory empty or UNCLEAR-reported.
- **Tests expected to exist:** legality conformance suite (per §10.5 row + illegal cases), CAS row-count/mirror tests, anchor-churn tests, lease/fencing/expiry tests, shutdown tests, log-line capture tests.
- **Next phase entry condition:** ST-11 done; phase P6 report filed.
