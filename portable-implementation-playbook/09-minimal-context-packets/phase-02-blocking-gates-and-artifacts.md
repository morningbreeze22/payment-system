> **Purpose:** Minimal context packets B-01..B-04, CA-1..CA-10 — paste-alone briefs for a small-context local agent (original Section I, phase P2; CA-10 OPTIONAL).
> **When to use this file:** Paired with the matching task-card file 08-task-cards/02-blocking-gates-and-artifacts.md — one packet per task, used as the working brief.
> **Depends on:** 09-minimal-context-packets/README.md; the matching task card; the requirement sections each packet cites; 07-placeholder-glossary.md.
> **Used by:** The local coding agent executing phase P2.
> **Safe to transfer:** yes
> **Contains local code names:** no

# Minimal Context Packets — Phase P2

```text
[B-01] §18-0 snapshot-contract residue
Read: §1 contract facts (trade-payment cardinality), §6.0, §6.1, §12, §18 item 0. Model (§1 fact): multiple payments per trade; snapshot messages (newer overwrites older); tuple unique within snapshot → NO discriminator; schema/identity freeze not gated here.
Placeholders: none. Mappings: none.
Objective (residue): FILE the written confirmations for asks 5 + 8 (both confirmed verbally 2026-07-11 — the paper is the Q1 evidence); §6.0 intake uniqueness validation in IN-02; TL-2 gains the step-granularity clause. (PO-9 ANSWERED: absence = amendment to zero; TL-16 answered round 5.)
Tests: within-snapshot collision → whole-snapshot validation failure; mid-fan-out crash + redelivery converges. Stop: residue closed (or IN-02 stays BLOCKED).
```

```text
[B-02] Sandbox access + engine statements
Read: §18 item 1, TL-4/5/11/13, §9.2 §9.5. Invariant: written answers configure tests; only EXECUTED tests close §18-1.
Placeholders: [Contract Test Suite] (future). Mappings: none.
Objective: obtain sandbox access; written TTL; ingest-lag distribution; query lookback (≥ max row lifetime incl. ops SLA framing); rate limit; TL-11 a/b/c answers. Record verbatim.
Tests: none. Stop: all recorded; CT-01 unblocked.
```

```text
[B-03] §18-2 closure record (round 10 — engine owns the calendar)
Read: §18 item 2 (CLOSED), §7.4. Invariant: NO local cutoff calendar, config, or tz machinery exists or may be built; late submissions classify through CA-1 like any engine response.
Placeholders: none. Mappings: none.
Objective: record the PO's 2026-07-11 closure; file the engine's WRITTEN any-time-submission line + late-submission response code (if any) into the CA-1 table (Q-08).
Tests: none. Stop: the §18-2 CLOSED fact + the CA-1 late-submission ask recorded (round 10 — no calendar attributes exist to source).
```

```text
[B-04] §18-3 resolution path
Read: §18 item 3, §9.3, TL-10, TL-5, §20 (PO decision). Invariant: the operation is required unless TL-10 AND TL-5 both affirm in writing AND the PO re-confirms de-scope.
Placeholders: [Operator Admin Procedure Area]. Mappings: none.
Objective: record the chosen path (default: build OP-01..03 per CA-9).
Tests: none. Stop: path recorded.
```

```text
[CA-1] Engine error-code table
Read: §7.0–7.3, §13, §16.6 artifact 1. Invariant: closed taxonomy; unmapped = fail closed; never "assume retryable".
Placeholders: [Provider Response Parser] (consumer). Mappings: D-05 memo desirable.
Objective: author code→(category, code, retryable, severity, submission_state, target dimensions) incl. DUPLICATE_REQUEST, collision (distinguishable code), replay-original-response class, business rejects. Owner + version.
Tests: none (RC-01 consumes). Stop: table published.
```

```text
[CA-2] Status vocabulary + evidence mapping
Read: §4.4 §8 §16.6 artifact 2, TL-1. Invariant: terminal vs intermediate classification errs fail-closed.
Placeholders: [Payment Status Feed Consumer] (consumer). Mappings: none.
Objective: author full status enum + evidence ranks + feed event schema (names/types); record dead-UETR emission answer and TL-1 event_id answer or fallback.
Tests: none. Stop: published.
```

```text
[CA-3] Query response mapping
Read: §9.1 §9.2 §16.6 artifact 3. Invariant: NOT_FOUND is never "not submitted"; unmapped/timeout → INDETERMINATE.
Placeholders: [Status Query Resolver] (consumer). Mappings: none.
Objective: map every query response to EXECUTED/REJECTED/NOT_FOUND/INDETERMINATE/ACCEPTED (acceptance promotes to SUBMITTED — decided rule). Owner + version.
Tests: none. Stop: published.
```

```text
[CA-4] DDL migration set spec
Read: §2.1 §2.2 §2.3 §10.3 §3(I6) §16.5 §16.6 artifact 4. Invariant: four tables only (§2.1–§2.4) + the sanctioned §9.3 ops approval store; other new-table needs = SPEC_CONFLICT.
Placeholders: [DB Migration Directory] [Stored Procedure / Trigger Area]. Mappings: D-02 inventory; scope key settled (§1 contract facts).
Objective: spec all columns, scope-key UNIQUE, UNIQUE(idempotency_key), NULL-ignoring UNIQUE(uetr), I6 function index, enum+L1-shape+L2–L8 CHECKs, freeze+release-guard triggers w/ evidence-flag mechanics, active-row-bounded index list, expand/contract sequencing.
Tests: none (S-09 executes). Stop: DBA-reviewed spec published.
```

```text
[CA-5] Identity spec + golden vectors
Read: §5.1 (amount/UETR excluded), §2.1 (seq), §16.6 artifact 5. Invariant: byte-exact, versioned; vectors computed independently of the implementation.
Placeholders: [Payment Request Creation Component] (consumer). Mappings: scope key settled (§1 contract facts).
Objective: spec inputs (scope|seq — no discriminator, §1 contract facts), canonicalization, delimiter/encoding, algorithm, version; ≥12 vectors incl. canonicalization + delimiter-in-field cases.
Tests: none (K-03). Stop: spec + vectors published.
```

```text
[CA-6] Canonical instruction serialization / last_sent_hash
Read: §7.0 §2.2 §5.1 (hash paragraph) §16.6 artifact 5. Invariant: business content only — envelope fields excluded or every attempt looks divergent.
Placeholders: [Provider POST Client] [Request Status Persistence Layer]. Mappings: D-05 field inventory (kept local).
Objective: define hashed field set, canonical order, canonicalization, algorithm, version. Persistence rule (QUALIFIED, review 928341a M1): content never stored in payment tables/logs/traces — the ONLY permitted persistence is the switch-gated §14.1 journal; otherwise hash only. CA-6 produces BOTH the canonical bytes (consumed by the §14.1 rider) and the hash derived from those exact bytes.
Tests: none (K-05). Stop: published.
```

```text
[CA-7] Test catalog
Read: §16.6 artifact 6; playbook Section J. Invariant: stable IDs; every entry §-traceable.
Placeholders: [Integration Test Suite] [Contract Test Suite]. Mappings: none.
Objective: adopt Section J (T-01..T-38 — T-38 gate = JOURNAL_ENABLEMENT + the switch-OFF inertness sub-case on the payment gate) + spec-named entries (downgrade-DUPLICATE leaves uetr intact; ambiguous claim-commit; concurrent inbox duplicates); owner per entry.
Tests: none. Stop: published.
```

```text
[CA-8] Runbook stubs
Read: §15 (list+rollup+practices) §16.6 artifact 7 §9.3 §5.2 (MVP scope). Invariant: runbooks never instruct disabling guards; §5.2 DR runbook is post-MVP (stub = major-incident note only).
Placeholders: [Metrics / Alerting Layer]. Mappings: none.
Objective: stub per §15 alert (Trigger/Severity/Why/Action/Data/Escalation/Safe stop — seed from playbook Section N); aged-MAYBE runbook; known-outage suppression semantics.
Tests: none. Stop: stubs published.
```

```text
[CA-9] apply-platform-verified-outcome spec
Read: §9.3 (operation + approval workflow) §10.1 §10.3 §20-8 §16.6 artifact 8 §18-3. Invariant: execution input = approval_id (identities derived from the record — round 4); the two-step workflow IS the MVP protocol (signed assertion = gated alternative, not offered to implementers); APPROVED→CONSUMED CAS atomic with the transition; guard passed legitimately, never disabled.
Placeholders: [Operator Admin Procedure Area]. Mappings: none.
Objective: spec EXECUTION signature = approval_id ONLY (round 4 — the approval record carries the authenticated initiator/approver identities, action binding incl. request_id + EXECUTED|REJECTED + ticket ref + nonce; identities are DERIVED, never inputs), consumption semantics per operation class (§9.3), evidence-flag mechanics, refusals (CLAIMED/terminal/amount mismatch), money effects, audit fields, alert, restricted role, drill script.
Tests: none (OP-02). Stop: published; OP-01 unblocked.
```

```text
[CA-10] §14.1 attempt-journal spec (payment_attempt_journal)
Read: §14.1 (all) §2.2 (post_attempt_seq) §16.3 §7.2 §11; file 12 CA-10; file 24 M9. Invariant: audit sink NEVER state — no runtime rule, scanner, gate, resolver, or derivation may read it; INSERT-only forever; ops/audit schema (rule-13 second sanctioned store; §2 model stays four tables); it replaces NOTHING (§14 line + divergence_expected + last_sent_hash all stay — V11-17 rejection scope intact); identity = post_attempt_seq (monotonic, survives the §9.2 attempt_count reset), NEVER attempt_count.
Placeholders: none (DBA ops/audit schema). Mappings/inputs REQUIRED before AUD-01 can resolve (review 4d5cb83 M1): <request_id_type> (D-02), CA-1 category tokens (CA-1 published), audit schema/tablespaces/roles, environment-qualified policy name, audit execution principal + PDB scope (DBA).
Objective: author the spec from §14.1 + the file 12 DDL TEMPLATE (2026-07-17 simplified design; AUD-01 resolves it to zero-placeholder SQL): typed columns; FULL payload_content on EVERY STARTED (simplicity rule — never partial storage); scalar event-shape CHECK + BEFORE INSERT trigger for CLOB presence (Oracle forbids CHECKs on LOB columns); BOTH global unique structures (journal_id PK + the (request_id, post_attempt_seq, event_type) pair key) with DROP PARTITION ... UPDATE GLOBAL INDEXES; monthly interval partitions on occurred_at; the TWO riders — STARTED in the posting claim (K-04; write-ahead when healthy, NEVER a gate), RESOLVED in the episode-ending transaction (RC-02 or ST-10's LEASE_EXPIRED_MAYBE), rowCount==1 only. NARROW-GUARANTEE coupling (the ONE canonical formulation): the journal is never a business or money-safety gate — statement-local insert failures proven by T-38 are caught around the single JDBC statement, recorded, alerted AFTER host commit; FATAL connection/session/commit failures propagate as ordinary host infra failures; the guarantee is "no incorrect payment outcome", not "no journal failure can ever fail an attempt". Enablement switch default OFF (transitions only under posting freeze); autonomous transactions FORBIDDEN; §16.3 security package (restricted audit role, DB-audited reads, encryption per the enablement gate, no lower-env copies, retention per compliance ask); POSTING attempts only.
Tests: none here (T-38 with AUD-01/K-04/RC-02/ST-10). Stop: published; AUD-01 unblocked.
```

