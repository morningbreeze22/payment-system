> **Purpose:** Task cards K-01..K-06 (deterministic identity + write-ahead persistence) (original Section H, phase P4).
> **When to use this file:** When executing the tasks of this phase, one card at a time, with the matching packet file from 09-minimal-context-packets/.
> **Depends on:** 08-task-cards/README.md; 01-playbook-index.md; 07-placeholder-glossary.md; the requirement sections cited per card; the locally filled mapping template.
> **Used by:** The local coding agent executing phase P4.
> **Safe to transfer:** yes
> **Contains local code names:** no

## H-Phase 4 — Identity and idempotency key persistence (P4)

### K-01 — next_request_seq counter discipline

- **Task ID:** K-01
- **Title:** Increment payment_obligation.next_request_seq under the obligation lock in the request-insert transaction
- **Classification:** MVP normative implementation
- **Purpose:** §2.1/§5.1: the seq is the identity's ordering input; incremented under the lock, in the SAME transaction as the insert — deterministic across a database restore by construction.
- **Prerequisites:** S-09; [Payment Request Creation Component] mapped (D-04/F.2).
- **Requirement sections / concepts to read:** §2.1 (next_request_seq), §5.1, §11 (obligation lock first).
- **Placeholder components involved:** [Payment Request Creation Component], [Obligation Repository].
- **Local placeholder mappings required before starting:** both Confirmed; creation-site count known (if >1 site, RG-06 consolidation is not yet done — this task instruments ALL sites identically and records the debt).
- **Local code areas to discover:** the creation transaction boundary.
- **How to locate:** F.2 findings.
- **Implementation instructions:** in the creation path: obligation row locked (SELECT FOR UPDATE) → read seq → increment → use in K-02 derivation → insert request with the consumed value persisted in the payment_request.request_seq COLUMN (§2.2, write-once — 1d8a650 M1: this column, not the obligation counter, is the source of truth for the §14 line field, the §12 keyset order, and the §5.2 log heuristic) — all one transaction.
- **Do not change:** what triggers creation (that is RG-06).
- **Tests to add:** two concurrent creations on one obligation → distinct sequential seqs (the lock serializes); rollback does not burn a seq inconsistently with the inserted row (both roll back together); WRITE-ONCE controls (4dbdf2b M1 — request_seq is identity-load-bearing, so it gets the same named pattern as the stamp): repository-wide SQL-inventory assertion that request_seq appears ONLY in the creation INSERT and in NO UPDATE SET list; mutation tests asserting CAS transitions, provider-response processing, feed events, lease expiry, and manual operations all preserve request_seq unchanged.
- **Edge cases:** obligation created in the same transaction as its first request (seq starts at the spec'd initial value — per CA-5).
- **Manual validation:** seq column advances by exactly 1 per created request in a local run.
- **Expected outcome:** deterministic seq per obligation.
- **Failure signs:** Oracle sequence objects used instead of the row counter (NOT restore-deterministic — the spec's construction requires the row counter).
- **Common mistakes:** incrementing outside the lock; caching seq in memory.
- **Completion criteria:** tests green.
- **Stop condition:** merged.
- **Next task:** K-02.

### K-02 — Deterministic key derivation

- **Task ID:** K-02
- **Title:** Implement the CA-5 derivation: versioned, byte-exact hash(scope fields | request_seq)
- **Classification:** MVP normative implementation
- **Purpose:** §5.1 — the DR keystone; derived from business state, never random; amount and UETR excluded.
- **Prerequisites:** CA-5 published (B-01 folded in); K-01.
- **Requirement sections / concepts to read:** §5.1 (all), CA-5.
- **Placeholder components involved:** [Payment Request Creation Component].
- **Local placeholder mappings required before starting:** same as K-01; D-09 memo (what generation exists today).
- **Local code areas to discover:** current key-generation site (to be replaced for NEW rows).
- **How to locate:** F.15 findings.
- **Implementation instructions:** implement CA-5 exactly (canonicalization, delimiter/encoding, algorithm, embedded version); wire into the creation transaction (key computed and stored on the row at insert — the write-ahead persistence itself is re-verified at the posting claim, K-04); EXISTING rows keep their persisted keys untouched (retries reuse the PERSISTED key, §5 — never re-derive for a row that already has one).
- **Do not change:** persisted keys on any existing row; the derivation input list.
- **Tests to add:** determinism (same inputs → same key across JVM restarts); input sensitivity (seq/scope change → new key); amount NOT an input (two amounts, same key); persisted-key-wins rule (row with a key never gets re-derived).
- **Edge cases:** legacy in-flight rows with random keys — they proceed under their persisted keys; only NEW rows use the derivation (record this boundary in the mapping doc).
- **Manual validation:** derive a key by hand from CA-5 for one seeded row; compare.
- **Expected outcome:** deterministic generation live for new rows.
- **Failure signs:** key derived at POST time instead of creation; re-derivation on retry.
- **Common mistakes:** platform-default charset creeping into hashing; version omitted.
- **Completion criteria:** tests green.
- **Stop condition:** merged.
- **Next task:** K-03.

### K-03 — Golden-vector tests

- **Task ID:** K-03
- **Title:** Freeze the derivation with CA-5's golden vectors as build-failing tests
- **Classification:** MVP normative implementation (go-live gate evidence)
- **Purpose:** §5.1 exactness: byte-identical reproducibility across releases and restore IS the DR property; the vectors freeze it.
- **Prerequisites:** K-02; CA-5 vectors.
- **Requirement sections / concepts to read:** §5.1 (exactness requirement), CA-5.
- **Placeholder components involved:** [Integration Test Suite].
- **Local placeholder mappings required before starting:** none beyond K-02.
- **Local code areas to discover:** test-fixture conventions.
- **How to locate:** D-11.
- **Implementation instructions:** load the CA-5 vector file verbatim (do NOT re-type values); one test per vector asserting exact output bytes; a version-pinning test (scheme version constant matches CA-5's).
- **Do not change:** vector values (a failing vector means the CODE is wrong or CA-5 must be formally re-versioned — never edit vectors to pass).
- **Tests to add:** the vector suite.
- **Edge cases:** delimiter-in-field vectors; canonicalization vectors — all from CA-5.
- **Manual validation:** deliberately corrupt one canonicalization rule locally → vectors fail (proves the tests bite); revert.
- **Expected outcome:** derivation frozen by the build.
- **Failure signs:** vectors regenerated from the implementation (circular — forbidden).
- **Common mistakes:** asserting on hex-string case-insensitively when CA-5 fixes a case.
- **Completion criteria:** suite green; mutation check done.
- **Stop condition:** merged. GO-LIVE EVIDENCE: record in Section Q.
- **Next task:** K-04.

### K-04 — Write-ahead identity at the posting claim

- **Task ID:** K-04
- **Title:** Enforce §5: no POST under a caller-supplied identity not durably persisted; identity persisted in the posting-claim transaction (first claim)
- **Classification:** MVP normative implementation
- **Purpose:** §5's normative rule + §11's posting-claim content; also §11's ambiguous claim-commit rule (unknown COMMIT outcome → do NOT proceed to the wire).
- **Prerequisites:** K-02; [Provider POST Client] mapped; ST-09 helpful but not required (claim shape may be adapted when ST-09 lands — coordinate via mapping doc note).
- **Requirement sections / concepts to read:** §5 (rules), §11 (posting claim + ambiguous claim-commit), §2.2 (identity fields).
- **Placeholder components involved:** [Provider POST Client], [Request Status Persistence Layer], [Payment Request Creation Component].
- **Local placeholder mappings required before starting:** POST call-site mapping (D-05); claim transaction boundary identified.
- **Local code areas to discover:** the exact commit point before the HTTP call.
- **How to locate:** F.4 + D-09 trace.
- **Implementation instructions:** restructure the posting path so that: claim transaction persists (first claim) the identity + (every claim, K-05) hash/flag/attempt-stamp, COMMITS, and only then the HTTP call runs; if the claim COMMIT outcome is unknown (failover/connection loss mid-commit) the worker does NOT call the engine — treat the claim as lost; lease expiry → MAYBE and the resolver owns it (§11). §14.1 rider (same claim transaction): increment post_attempt_seq (§2.2 — monotonic, NEVER reset; not attempt_count) and, if the §14.1 enablement switch is ON, INSERT the ATTEMPT_STARTED journal row (FULL payload_content EVERY attempt — no dedup). Canonical failure rule (§14.1): statement-local insert failures proven by T-38 are caught around the single JDBC statement (plain try/catch, no inner @Transactional), recorded, and alerted AFTER host commit — the claim proceeds; FATAL connection/session/commit failures propagate as ordinary infra failures; the guarantee is "no incorrect payment outcome". Autonomous transactions FORBIDDEN (host rollback must remove the row). The matching §14 ATTEMPT-class log line (ST-08/K-05 convention) carries attempt_event_type = 'ATTEMPT_STARTED' — the exact field name, byte-equal to the journal token (review b760786 M2).
- **Do not change:** payment construction; SDK call semantics.
- **Tests to add:** ordering test (kill/fault injection between commit and call → row shows persisted identity, no wire call made — assert via stub); ambiguous-commit test (§16.6-6 catalog entry: simulated commit-unknown → no HTTP call); T-38 journal set: claim rollback leaves NO ATTEMPT_STARTED row; the §9.2 downgrade lifecycle (attempt_count resets, post_attempt_seq does not) produces NO unique-key collision on the recovery re-POST.
- **Edge cases:** async SDK internals — the "call" is the SDK invocation; nothing may be handed to the SDK pre-commit.
- **Manual validation:** trace one payment in a local run: DB row with key committed strictly before stub receives the call.
- **Expected outcome:** write-ahead rule enforced structurally.
- **Failure signs:** identity written in the same transaction that processes the response (too late); SDK invoked inside the claim transaction.
- **Common mistakes:** treating "connection lost during commit" as "not committed" (it may have committed — that's the point of the rule).
- **Expected outcome:** as above.
- **Completion criteria:** tests green.
- **Stop condition:** merged.
- **Next task:** K-05.

### K-05 — last_sent_hash, divergence_expected, last_post_attempt_at at claim time

- **Task ID:** K-05
- **Title:** Persist instruction hash + divergence flag + attempt stamp in every posting-claim transaction, before the wire
- **Classification:** MVP normative implementation
- **Purpose:** §2.2/§7.0: the per-attempt record of what may be executing; divergence_expected computed AT CLAIM TIME against the PRIOR hash (comparison impossible at collision-response time); last_post_attempt_at is the §9.2 MAYBE trust-age anchor.
- **Prerequisites:** K-04; CA-6 published.
- **Requirement sections / concepts to read:** §2.2 (last_sent_hash / divergence_expected / last_post_attempt_at blocks), §7.0 (fresh assembly), §11 (claim content list), CA-6.
- **Placeholder components involved:** [Provider POST Client], [Request Status Persistence Layer], [Payment Enrichment Component] (assembly inputs, read-only).
- **Local placeholder mappings required before starting:** K-04's restructured claim path.
- **Local code areas to discover:** where the instruction is fully assembled (must be BEFORE the claim commit now).
- **How to locate:** D-05 memo.
- **Implementation instructions:** in the claim transaction: assemble the instruction FRESH (per §7.0 — enrichment lookups current); serialize + hash per CA-6; compute divergence_expected := (previous last_sent_hash IS NOT NULL AND differs) BEFORE overwriting; persist hash + flag + last_post_attempt_at; commit; then wire. Emit the posting-claim log line carrying the sent hash + attempt count (§14).
- **Do not change:** enrichment internals; no payload freeze (rejected alternative, §7.0).
- **Tests to add:** first attempt → divergence_expected false; changed assembly between attempts → true; unchanged → false; anchor stamped pre-wire (fault injection: crash after commit, before call → anchor set); log line contains hash + attempt count.
- **Edge cases:** DR-replay-recreated rows have no prior hash → flag false (drives §7.2's ANOMALOUS branch — assert in the collision tests, RC-02).
- **Manual validation:** two local attempts with a changed detail → flag observed true on the second claim row image.
- **Expected outcome:** per-attempt forensic + branch-discriminator state correct.
- **Failure signs:** flag computed at response time (the prior hash is already gone — spec calls this impossible; if the code tries, it is wrong).
- **Common mistakes:** hashing after commit; stamping the anchor on response processing (spec: it must be pre-wire — the crash cases are exactly when it matters).
- **Completion criteria:** tests green.
- **Stop condition:** merged.
- **Next task:** K-06.

### K-06 — Duplicate-prevention verification set

- **Task ID:** K-06
- **Title:** Crash/retry/restore duplicate-prevention tests around identity
- **Classification:** MVP normative implementation (go-live gate evidence)
- **Purpose:** prove the §5/§5.1 machinery end to end: same key on retry; DUPLICATE_REQUEST routed to ambiguity handling; restore-recreated request regenerates the SAME key.
- **Prerequisites:** K-01..K-05; RC-02 branches NOT required (stub the engine's responses).
- **Requirement sections / concepts to read:** §5.1 (rationale trace), §7.2 (DUPLICATE_REQUEST row), §2.2 UNIQUE.
- **Placeholder components involved:** [Integration Test Suite], [Provider POST Client] (stubbed).
- **Local placeholder mappings required before starting:** integration lane with engine stub.
- **Local code areas to discover:** none new.
- **How to locate:** n/a.
- **Implementation instructions:** integration tests: (1) crash before POST (after claim commit) → the row reaches its re-POST ONLY via lease expiry → MAYBE_SUBMITTED → resolver → the §9.2 downgrade (NO direct posting re-claim exists — 2a19c20 M1: a committed or commit-unknown claim always expires into MAYBE, even when the wire call provably never started), and the eventual retry reuses the SAME persisted key; (2) crash after POST, before response → row MAYBE via lease expiry, no fresh key ever minted; (3) restore simulation: delete the request row + reset obligation counters to a pre-insert image (test harness), re-run creation for the same shortfall → derived key EQUALS the deleted row's key; (4) UNIQUE(idempotency_key) violation surfaces as a loud error, never silent.
- **Do not change:** production code (test-only task; failures here reopen K-xx tasks).
- **Tests to add:** the four above (catalog T-08/T-09/T-10 alignment — corrected 2a19c20 L2; T-07 is the hash/divergence set, not this one).
- **Edge cases:** test (3) must use the REAL derivation path, not a shortcut call to the hash function.
- **Manual validation:** review that stubs assert on the KEY the engine received.
- **Expected outcome:** duplicate-prevention evidence recorded for Section Q.
- **Failure signs:** test (3) passing only because the harness reused the old row.
- **Common mistakes:** asserting on internal fields instead of what crossed the (stubbed) wire.
- **Completion criteria:** all four green.
- **Stop condition:** green; Q evidence recorded.
- **Next task:** U-01.


---

## Phase handoff summary (P4 → P5)

- **Phase outputs:** next_request_seq lock discipline; CA-5 derivation live for NEW rows (existing rows keep persisted keys); golden vectors frozen in the build; write-ahead identity + hash/flag/attempt-stamp persisted in the posting-claim transaction before the wire; ambiguous claim-commit rule enforced.
- **Blockers to carry forward:** §18-1 sandbox proof still pending — P8 (CT-01..07) may now START in parallel (real derivation available).
- **Local mapping rows expected filled:** [Payment Request Creation Component], [Provider POST Client] change notes updated; claim-transaction boundary recorded.
- **Tests expected to exist:** golden vectors (T-02), determinism/sensitivity/amount-exclusion (T-01/04/05/06), write-ahead ordering (T-03), K-06 crash/retry/restore set (T-08/09/10 precursors), hash/flag behavior (T-07).
- **Next phase entry condition:** K-06 green; Section Q evidence recorded for vectors + duplicate prevention.
