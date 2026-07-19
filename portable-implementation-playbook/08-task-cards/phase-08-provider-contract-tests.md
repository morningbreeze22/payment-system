> **Purpose:** Task cards CT-01..CT-07 (provider idempotency sandbox contract tests — the §18-1 proof) (original Section H, phase P8).
> **When to use this file:** When executing the tasks of this phase, one card at a time, with the matching packet file from 09-minimal-context-packets/.
> **Depends on:** 08-task-cards/README.md; 01-playbook-index.md; 07-placeholder-glossary.md; the requirement sections cited per card; the locally filled mapping template.
> **Used by:** The local coding agent executing phase P8.
> **Safe to transfer:** yes
> **Contains local code names:** no

## H-Phase 8 — Provider contract tests (P8)

### CT-01 — Sandbox contract-test harness

- **Task ID:** CT-01
- **Title:** Stand up the sandbox harness: real derivation, real POST client (or thin equivalent), recorded evidence output
- **Classification:** §18 BLOCKING go-live gate (enabler)
- **Purpose:** §18-1's matrix must run as EXECUTABLE tests, re-runnable on engine releases.
- **Prerequisites:** B-02 (access); K-02/K-03 (real derivation available).
- **Requirement sections / concepts to read:** §18 BLOCKING item 1 (intro + matrix), §1 (assumed facts under proof).
- **Placeholder components involved:** [Contract Test Suite], [Provider POST Client].
- **Local placeholder mappings required before starting:** sandbox endpoint/credentials wiring (local secret handling per §16.3 — vault, never files).
- **Local code areas to discover:** how to point the POST client at sandbox.
- **How to locate:** client configuration (D-05).
- **Implementation instructions:** a runnable suite, isolated from CI-by-default (sandbox = shared resource): helpers to POST a payment with a chosen key/payload via the REAL identity derivation + REAL client (or a thin harness reusing its serialization); response capture to a durable evidence file (timestamped, engine-version-stamped); teardown notes per sandbox etiquette.
- **Do not change:** production config; sandbox data beyond the tests' own.
- **Tests to add:** a smoke test: one POST accepted end to end.
- **Edge cases:** sandbox behavioral drift vs production — record engine version per run (§18-1: re-run on engine releases).
- **Manual validation:** smoke run green; evidence file produced.
- **Expected outcome:** harness ready for CT-02..05.
- **Failure signs:** harness bypassing the real derivation/serialization (invalidates the proof).
- **Common mistakes:** hardcoding credentials.
- **Completion criteria:** smoke green.
- **Stop condition:** merged (suite excluded from default CI).
- **Next task:** CT-02.

### CT-02 — Matrix (a): identical-payload re-POST

- **Task ID:** CT-02
- **Title:** Prove: re-POST of a known key with IDENTICAL payload → deduped/acked (or original response replayed); nothing executes
- **Classification:** §18 BLOCKING go-live gate
- **Purpose:** §18-1(a) — the collision contract's identical branch; also detects the artifact-1 replay-original-response class for CA-1.
- **Prerequisites:** CT-01.
- **Requirement sections / concepts to read:** §18-1(a), §7.0 (consequences), §16.6-1 (replay class).
- **Placeholder components involved:** [Contract Test Suite].
- **Local placeholder mappings required before starting:** CT-01 harness.
- **Local code areas to discover:** none.
- **How to locate:** n/a.
- **Implementation instructions:** POST once (record response); re-POST byte-identical payload same key; assert: no second execution — round 16: a status query showing ONE visible payment is NOT sufficient (provider dedup/query semantics can collapse records); obtain a provider-side EXECUTION/AUDIT COUNT or settlement-ledger equivalent; classify the second response (dedup code vs original-replay) and file into CA-1; record evidence per the V.1-Q2 round-16 standard (raw bytes, versions, execution count, parity statement).
- **Do not change:** CA-1 without the owner (the test FEEDS it).
- **Tests to add:** this test.
- **Edge cases:** engine executes twice → §18-1 FAILS: STOP the reliance chain — report immediately; the entire §7.0/§9.2 re-POST design is gated on this (TL-4's revert-to-payload-freeze clause becomes live).
- **Manual validation:** engine-side EXECUTION-COUNT verification (round 16 — not status-query visibility alone).
- **Expected outcome:** PASS recorded with evidence.
- **Failure signs:** ambiguous engine answer — treat as NOT passed; escalate to provider.
- **Common mistakes:** payload accidentally differing (envelope timestamps) — byte-identical means byte-identical.
- **Completion criteria:** evidence filed.
- **Stop condition:** result recorded either way.
- **Next task:** CT-03.

### CT-03 — Matrix (b): divergent-payload re-POST

- **Task ID:** CT-03
- **Title:** Prove: re-POST of a known key with a DIFFERENT payload → rejected without execution, code distinguishable from plain DUPLICATE_REQUEST
- **Classification:** §18 BLOCKING go-live gate
- **Purpose:** §18-1(b)/TL-4 — the LOAD-BEARING guarantee behind §7.0 fresh assembly; the distinguishable code drives §7.2's collision branch.
- **Prerequisites:** CT-01.
- **Requirement sections / concepts to read:** §18-1(b), TL-4, §7.2 (collision rows), §5.1 (amount-divergence routing).
- **Placeholder components involved:** [Contract Test Suite].
- **Local placeholder mappings required before starting:** harness.
- **Local code areas to discover:** none.
- **How to locate:** n/a.
- **Implementation instructions:** POST; re-POST same key with a changed business field (and separately a changed amount); assert: no execution of the second — round 16: prove via provider-side EXECUTION/AUDIT COUNT, not status-query visibility alone; capture the rejection code; assert distinguishable from the plain-duplicate code observed in CT-02; file codes into CA-1; evidence per the V.1-Q2 round-16 standard.
- **Do not change:** n/a.
- **Tests to add:** this test (two payload-variant runs).
- **Edge cases:** engine EXECUTES the divergent payload → §18-1 FAILS catastrophically (double-pay path): STOP, report — §7.0 must revert to payload freeze per TL-4, which is a design-level decision for the humans, not a local fix.
- **Manual validation:** engine-side execution-COUNT check (round 16).
- **Expected outcome:** PASS + codes recorded.
- **Failure signs:** rejection code identical to plain duplicate (breaks §7.2's branch discrimination — escalate; CA-1 must then classify on secondary signals per provider guidance).
- **Common mistakes:** changing only envelope fields (not a payload divergence).
- **Completion criteria:** evidence filed.
- **Stop condition:** result recorded.
- **Next task:** CT-04.

### CT-04 — Matrix (c): key-retention TTL edge

- **Task ID:** CT-04
- **Title:** Verify (a)/(b) behavior at the stated key-retention edge; confirm TTL ≥ max row lifetime or trigger the repost_permitted TTL term
- **Classification:** §18 BLOCKING go-live gate
- **Purpose:** §18-1(c): the §9.2 re-POST lane is precisely the DELAYED one; a key aged out of the dedup store executes a duplicate.
- **Prerequisites:** CT-02, CT-03; B-02 (written TTL).
- **Requirement sections / concepts to read:** §18-1(c), §7.0 (TTL term consequence), §9.3 (ops-only consequence).
- **Placeholder components involved:** [Contract Test Suite].
- **Local placeholder mappings required before starting:** harness; TTL statement.
- **Local code areas to discover:** none.
- **How to locate:** n/a.
- **Implementation instructions:** as close to the stated TTL edge as sandbox practicality allows (provider cooperation may be needed — aged keys or clock manipulation on their side): re-run (a) and (b) with an aged key; record behavior; compare TTL against max row lifetime incl. ops-queue SLA (from B-02/TL-5 inputs); emit the TYPED CONSEQUENCE RECORD (289ef66 M2 — one machine/reviewer-checkable state, exactly one of): NO_IMPLEMENTATION_CHANGE | IMPLEMENTATION_REQUIRED | UNRESOLVED_BLOCKING. TTL < max lifetime ⇒ IMPLEMENTATION_REQUIRED: repost_permitted gains a TTL term and past-TTL rows are ops-only (§18-1(c)); that record REOPENS/BLOCKS RC-03 even if previously merged, creates a named change card (owner + explicit tests), binds the fixing commit/build to THIS evidence, re-runs the affected assertions, and advances to IMPLEMENTED_AND_VERIFIED — F4, Q2, Q10, and the go-live authorization CANNOT pass while any CT consequence record is not NO_IMPLEMENTATION_CHANGE or IMPLEMENTED_AND_VERIFIED (never silently implement, never silently skip).
- **Do not change:** RC-03 within this task.
- **Tests to add:** the edge run (documented if provider-assisted rather than automated).
- **Edge cases:** TTL untestable in sandbox → record the limitation; the WRITTEN statement + the gap note go to the accountable owner (go-live decision input, not a local pass).
- **Manual validation:** evidence review with the tech lead.
- **Expected outcome:** TTL behavior known; consequences recorded.
- **Failure signs:** treating an untested TTL as verified.
- **Common mistakes:** testing only (a) at the edge (the (b) reject must also still hold).
- **Completion criteria:** evidence filed + the typed consequence record in a non-UNRESOLVED state; if IMPLEMENTATION_REQUIRED, THIS card completes but the RELEASE gates (F4/Q2/Q10/go-live) stay blocked until IMPLEMENTED_AND_VERIFIED (289ef66 M2).
- **Stop condition:** recorded.
- **Next task:** CT-05.

### CT-05 — Matrix (d): re-POST after synchronous business reject

- **Task ID:** CT-05
- **Title:** Settle TL-6: does a same-key re-POST after a sync business reject re-execute, or replay the cached rejection?
- **Classification:** §18 BLOCKING go-live gate
- **Purpose:** §7.1's working assumption (RE-EXECUTES) underpins the retry design for business rejects; §18-1(d): either answer is handled, but it must be KNOWN.
- **Prerequisites:** CT-01.
- **Requirement sections / concepts to read:** §18-1(d), TL-6, §7.1 (working assumption + consequence).
- **Placeholder components involved:** [Contract Test Suite].
- **Local placeholder mappings required before starting:** harness; a sandbox-inducible business reject (e.g. insufficient-funds equivalent — provider guidance).
- **Local code areas to discover:** none.
- **How to locate:** n/a.
- **Implementation instructions:** induce a sync business reject; re-POST same key (correctable condition resolved if the sandbox allows); record: re-executes vs replays rejection; emit the TYPED CONSEQUENCE RECORD (289ef66 M2): NO_IMPLEMENTATION_CHANGE | IMPLEMENTATION_REQUIRED | UNRESOLVED_BLOCKING. REPLAYS ⇒ IMPLEMENTATION_REQUIRED per TL-6 — retries of that error class are no-ops; policy changes to fresh successor via §6.8: the record REOPENS/BLOCKS RC-04 even if previously merged, creates a named change card (owner + tests), binds the fixing commit to this evidence, re-runs the affected assertions, and advances to IMPLEMENTED_AND_VERIFIED — F4/Q2/Q10/go-live stay blocked until it does.
- **Do not change:** retry policy within this task.
- **Tests to add:** the run.
- **Edge cases:** behavior differs per reject code — test the codes CA-1 marks retryable.
- **Manual validation:** evidence review.
- **Expected outcome:** TL-6 settled empirically.
- **Failure signs:** concluding from documentation alone (§18: "a written yes does not close this item").
- **Common mistakes:** letting the sandbox's test-mode semantics differ from prod semantics unnoted (record engine version + mode).
- **Completion criteria:** evidence filed + the typed consequence record in a non-UNRESOLVED state; if IMPLEMENTATION_REQUIRED, the release gates stay blocked until IMPLEMENTED_AND_VERIFIED (289ef66 M2).
- **Stop condition:** recorded.
- **Next task:** CT-06.

### CT-06 — Status-query mapping verification

- **Task ID:** CT-06
- **Title:** Empirically verify CA-3's response mapping incl. NOT_FOUND for a never-sent key and lookback behavior
- **Classification:** MVP normative (evidence for CA-3 + §9 config)
- **Purpose:** RC-06/RC-07 stand on CA-3; NOT_FOUND semantics and lookback (TL-5) are load-bearing.
- **Prerequisites:** CT-01; CA-3 drafted.
- **Requirement sections / concepts to read:** §9.1, §9.2 (four NOT_FOUND causes), CA-3.
- **Placeholder components involved:** [Contract Test Suite], [Status Query Resolver] (client reuse).
- **Local placeholder mappings required before starting:** harness + query client.
- **Local code areas to discover:** none.
- **How to locate:** n/a.
- **Implementation instructions:** query: an executed payment (→ EXECUTED-class), a rejected one (→ REJECTED-class), a never-sent key (→ NOT_FOUND), an accepted-not-settled one if inducible (→ ACCEPTED-class); measure observed ingest lag (POST-accept → query-visible) opportunistically across runs (feeds NOT_FOUND_TRUST_AGE sizing sanity vs TL-5's stated numbers); record lookback observations if aged data accessible.
- **Do not change:** CA-3 unilaterally — feed findings to its owner.
- **Tests to add:** the four query runs.
- **Edge cases:** responses not in CA-3 → INDETERMINATE mapping confirmed with the owner.
- **Manual validation:** CA-3 owner sign-off on the evidence.
- **Expected outcome:** CA-3 verified/amended.
- **Failure signs:** query keyed by a field the engine doesn't actually support (B-02 said vs observed).
- **Common mistakes:** measuring lag once and calling it the distribution (TL-5 asks p50/p99/max — sandbox numbers are sanity only).
- **Completion criteria:** evidence filed.
- **Stop condition:** recorded.
- **Next task:** CT-07.

### CT-07 — SDK contract checks (TL-11)

- **Task ID:** CT-07
- **Title:** Verify: SDK response returns the generated UETR (which field); SDK accepts our caller-supplied idempotency key; dedup keys on that key, not the UETR
- **Classification:** MVP normative (evidence for TL-11; (c) is blocking-grade)
- **Purpose:** §5/TL-11: key-based dedup is blocking-grade — a re-POST may carry a fresh SDK-minted UETR.
- **Prerequisites:** CT-01.
- **Requirement sections / concepts to read:** §5 (identity chain + rules), TL-11 (a/b/c).
- **Placeholder components involved:** [Contract Test Suite], [Provider POST Client].
- **Local placeholder mappings required before starting:** harness.
- **Local code areas to discover:** SDK invocation surface (D-05).
- **How to locate:** D-05 memo.
- **Implementation instructions:** (a) assert the acceptance response carries the UETR; record the field (feeds U-01 extraction); (b) assert the SDK transmits our supplied key (verify engine-side via query-by-key); (c) re-POST same key → assert the engine's dedup fires even though the SDK minted a fresh UETR (observable via CT-02's machinery) — this IS the key-vs-UETR dedup proof.
- **Do not change:** SDK usage conventions.
- **Tests to add:** the three checks.
- **Edge cases:** SDK does NOT accept a caller key → §5.1's keystone assumption fails: STOP, escalate — design-level input (§18/TL-11(b)).
- **Manual validation:** evidence review.
- **Expected outcome:** TL-11 answered empirically; U-01's UNCLEAR extraction site resolves.
- **Failure signs:** inferring (c) from documentation.
- **Common mistakes:** conflating the SDK's own validation errors with engine rejects (classify separately for CA-1).
- **Completion criteria:** evidence filed; §18-1 gate summary updated (CT-02..05 + this).
- **Stop condition:** recorded; P8 gate summary delivered to the human owner.
- **Next task:** IN-01.


---

## Phase handoff summary (P8 → gates)

- **Phase outputs:** executed §18-1 matrix evidence (a)–(d) + query-mapping verification + SDK contract checks; codes filed into CA-1/CA-3; engine-version-stamped evidence pack; re-run procedure for engine releases.
- **Blockers to carry forward:** ANY failure of CT-02/CT-03 stops all re-POST reliance (P10 auto-downgrade must not be enabled; TL-4 payload-freeze clause escalates to the humans). CT-04 TTL-short finding adds a repost_permitted TTL term (RC-03 follow-up).
- **Local mapping rows expected filled:** [Contract Test Suite] CONFIRMED (sandbox wiring recorded locally; credentials vaulted, never in files).
- **Tests expected to exist:** T-11/T-12/T-13/T-14 evidence on file; CT-06/CT-07 records.
- **Next phase entry condition:** none (parallel track) — but GO-03's auto-downgrade stage and GO-04 (Q2/Q10) consume this phase's PASS evidence.
