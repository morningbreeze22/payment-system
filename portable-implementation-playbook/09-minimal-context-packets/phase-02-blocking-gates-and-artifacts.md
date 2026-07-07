> **Purpose:** Minimal context packets B-01..B-04, CA-1..CA-9 — paste-alone briefs for a small-context local agent (original Section I, phase P2).
> **When to use this file:** Paired with the matching task-card file 08-task-cards/02-blocking-gates-and-artifacts.md — one packet per task, used as the working brief.
> **Depends on:** 09-minimal-context-packets/README.md; the matching task card; the requirement sections each packet cites; 07-placeholder-glossary.md.
> **Used by:** The local coding agent executing phase P2.
> **Safe to transfer:** yes
> **Contains local code names:** no

# Minimal Context Packets — Phase P2

```text
[B-01] Resolve §18-0 scope key
Read: §18 item 0, §2.1, §5.1, §12. Invariant: schema/identity freeze is unsafe until answered; answerer must know the scope key is at stake.
Placeholders: none. Mappings: none.
Objective: obtain the written payments-per-trade decision; if multiple, record the discriminator and flag CA-4/CA-5/S-02/S-03/K-02/IN-02/§12 for re-draft.
Tests: none. Stop: written decision recorded (or downstream stays BLOCKED).
```

```text
[B-02] Sandbox access + engine statements
Read: §18 item 1, TL-4/5/11/13, §9.2 §9.5. Invariant: written answers configure tests; only EXECUTED tests close §18-1.
Placeholders: [Contract Test Suite] (future). Mappings: none.
Objective: obtain sandbox access; written TTL; ingest-lag distribution; query lookback (≥ max row lifetime incl. ops SLA framing); rate limit; TL-11 a/b/c answers. Record verbatim.
Tests: none. Stop: all recorded; CT-01 unblocked.
```

```text
[B-03] Cutoff calendar sourcing
Read: §18 item 2, §16.4, §7.4. Invariant: tz-aware local-time+zone representation; never fixed UTC constants.
Placeholders: config. Mappings: none.
Objective: record calendar source, named owner, per-currency/market+holiday semantics, refresh cadence, stale/missing fail direction (recommend fail-blocked per payment_type).
Tests: none. Stop: six attributes recorded (or RC-04 cutoff config stays BLOCKED).
```

```text
[B-04] §18-3 resolution path
Read: §18 item 3, §9.3, TL-10, TL-5, §20 (PO decision). Invariant: the procedure is required unless TL-10 AND TL-5 both affirm in writing AND the PO re-confirms de-scope.
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
Read: §2.1 §2.2 §2.3 §10.3 §3(I6) §16.5 §16.6 artifact 4. Invariant: three tables only; new-table needs = SPEC_CONFLICT.
Placeholders: [DB Migration Directory] [Stored Procedure / Trigger Area]. Mappings: D-02 inventory; B-01 ANSWERED.
Objective: spec all columns, scope-key UNIQUE, UNIQUE(idempotency_key), NULL-ignoring UNIQUE(uetr), I6 function index, enum+L1-shape+L2–L8 CHECKs, freeze+release-guard triggers w/ evidence-flag mechanics, active-row-bounded index list, expand/contract sequencing.
Tests: none (S-09 executes). Stop: DBA-reviewed spec published.
```

```text
[CA-5] Identity spec + golden vectors
Read: §5.1 (amount/UETR excluded), §2.1 (seq), §16.6 artifact 5. Invariant: byte-exact, versioned; vectors computed independently of the implementation.
Placeholders: [Payment Request Creation Component] (consumer). Mappings: B-01 ANSWERED.
Objective: spec inputs (scope|seq + B-01 discriminator if any), canonicalization, delimiter/encoding, algorithm, version; ≥12 vectors incl. canonicalization + delimiter-in-field cases.
Tests: none (K-03). Stop: spec + vectors published.
```

```text
[CA-6] Canonical instruction serialization / last_sent_hash
Read: §7.0 §2.2 §5.1 (hash paragraph) §16.6 artifact 5. Invariant: business content only — envelope fields excluded or every attempt looks divergent.
Placeholders: [Provider POST Client] [Request Status Persistence Layer]. Mappings: D-05 field inventory (kept local).
Objective: define hashed field set, canonical order, canonicalization, algorithm, version; content never persisted, hash only.
Tests: none (K-05). Stop: published.
```

```text
[CA-7] Test catalog
Read: §16.6 artifact 6; playbook Section J. Invariant: stable IDs; every entry §-traceable.
Placeholders: [Integration Test Suite] [Contract Test Suite]. Mappings: none.
Objective: adopt Section J (T-01..T-32) + spec-named entries (downgrade-DUPLICATE leaves uetr intact; ambiguous claim-commit; concurrent inbox duplicates); owner per entry.
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
Read: §9.3 (procedure) §10.1 §10.3 §20-8 §16.6 artifact 8 §18-3. Invariant: dual control enforced BY the procedure; guard passed legitimately, never disabled.
Placeholders: [Operator Admin Procedure Area]. Mappings: none.
Objective: spec signature (request_id, EXECUTED|REJECTED, ticket ref, two approvers), evidence-flag mechanics, refusals (CLAIMED/terminal/amount mismatch), money effects, audit fields, alert, restricted role, drill script.
Tests: none (OP-02). Stop: published; OP-01 unblocked.
```

