> **Purpose:** Companion artifact plan CA-1..CA-9: owner type, required contents, validation, dependents, go-live relevance, failure-if-omitted (original Section L).
> **When to use this file:** When authoring or validating any section-16.6 companion artifact; checking whether implementation may start before an artifact exists (see also 18-playbook-quality-self-check.md complement).
> **Depends on:** requirment-v4.md section 16.6; 08-task-cards/phase-02-blocking-gates-and-artifacts.md.
> **Used by:** CA-consuming tasks (RC-01, IN-07, RC-06, S-xx, K-xx, OP-xx, OB-xx).
> **Safe to transfer:** yes
> **Contains local code names:** no

# L. Companion artifact plan

All nine are first-class deliverables with task cards (Section H,
Phase P2). Owner types: PROVIDER-FACING (needs provider input),
TEAM (authored locally from the spec), DBA (schema authority),
OPS (operations authority).

### CA-1 — Engine error-code classification table

```text
Section: §16.6 artifact 1; §7.
Owner type: TEAM + PROVIDER-FACING (engine codes), named owner req'd.
Purpose: drive RC-01's closed classifier; kill "assume retryable".
Required contents: code → (category, code, retryable, severity,
  submission_state, target dimensions); DUPLICATE_REQUEST; collision
  (distinguishable code); replay-original-response class; sync
  business rejects; fail-closed default rows; version + owner.
Validation: provider/tech-lead review; every D-05-observed branch and
  every CT-02/03/05-observed code present; RC-01 fixture suite green.
Dependent tasks: RC-01, RC-02, CT-02/03/05 (feed it), OB-07 (retry
  classes).
Go-live relevance: YES — unclassified codes fail closed into ops load;
  wrong classes misroute money states.
Failure if omitted: every unmapped engine code lands MAYBE·BLOCKED;
  ops queue floods; retryable/terminal confusion risks blind re-POSTs.
```

### CA-2 — Engine status vocabulary + evidence mapping

```text
Section: §16.6 artifact 2; §4.4, §8.
Owner type: TEAM + PROVIDER-FACING.
Purpose: rank feed statuses (terminal vs intermediate) for IN-07;
  define the feed event schema for contract tests.
Required contents: full status enum; per-status evidence class + rank;
  feed schema (event_id, UETR, status, amount, provider_reference —
  names, types); dead-UETR emission answer; TL-1 event_id answer.
Validation: provider review; §16.5 contract test derived from the
  schema; IN-07 tests keyed to the ranks.
Dependent tasks: IN-05/06/07, OB-04 (unmatched), U-03.
Go-live relevance: YES — a new engine status must fail a build, not
  on-call at 2 a.m. (§16.5).
Failure if omitted: intermediate/terminal confusion can freeze rows
  early or regress settled state; schema drift discovered in prod.
```

### CA-3 — Status-query response mapping

```text
Section: §16.6 artifact 3; §9.1.
Owner type: TEAM + PROVIDER-FACING.
Purpose: map query responses to §9.1 outcomes for RC-06.
Required contents: response → EXECUTED/REJECTED/NOT_FOUND/
  INDETERMINATE/ACCEPTED; acceptance-promotes-to-SUBMITTED rule;
  failure/timeout → INDETERMINATE; query key(s) supported.
Validation: provider review + CT-06 empirical verification.
Dependent tasks: RC-06, RC-07, CT-06.
Go-live relevance: YES — MAYBE recovery is built on it.
Failure if omitted: resolver misreads answers; worst case NOT_FOUND
  treated as "not submitted" → release → double pay (§9.2 forbids).
```

### CA-4 — Flyway/Oracle DDL migration set

```text
Section: §16.6 artifact 4; §2, §10.3, §3.
Owner type: TEAM + DBA.
Purpose: the authoritative schema spec P3 implements.
Required contents: all columns/types; scope-key UNIQUE (per B-01);
  UNIQUE(idempotency_key); NULL-ignoring UNIQUE(uetr); exact I6
  expression; enum CHECKs; L1-shape + L2–L8 CHECK expressions (with
  the dimension-ordering encoding); freeze + release-guard triggers +
  evidence-flag mechanics; normative active-row-bounded index list
  (one per standing scan); expand/contract sequencing.
Validation: DBA review; S-05/S-06/S-07 violation + plan tests green
  on real Oracle; S-09 dual-run proof.
Dependent tasks: S-01..S-09, OP-01 (flag mechanics), OB-01 (indexes).
Go-live relevance: YES — the DB backstop for every invariant.
Failure if omitted: illegal states representable; fat-finger releases
  silent; scans degrade with terminal-row growth.
```

### CA-5 — Identity-derivation spec + golden vectors

```text
Section: §16.6 artifact 5 (first half); §5.1.
Owner type: TEAM (scope key settled, §1 contract facts — no B-01 gate).
Purpose: byte-exact, versioned DR keystone.
Required contents: input list (scope|seq — no discriminator; §1
  contract facts);
  per-field canonicalization; delimiter/encoding (delimiter-in-field
  rule); algorithm; version; ≥12 vectors authored independently.
Validation: independent reproduction of all vectors; K-03 suite green;
  a deliberate mutation makes vectors fail.
Dependent tasks: K-02, K-03, CT harness (real keys), §5.2 step-5b
  (future).
Go-live relevance: YES — identity golden vectors are a Q item.
Failure if omitted: unfrozen derivation drifts across releases → the
  restore-duplicate returns silently.
```

### CA-6 — Canonical instruction serialization / last_sent_hash

```text
Section: §16.6 artifact 5 (second half); §7.0, §2.2.
Owner type: TEAM.
Purpose: make hash comparisons across attempts and DR replays
  meaningful; the §7.2 branch discriminator's foundation.
Required contents: hashed field set (business content only; envelope
  excluded); canonical order; canonicalization; algorithm; version;
  the content-never-persisted rule.
Validation: same instruction → same hash; one business-field change →
  different hash; K-05 tests green.
Dependent tasks: K-05, RC-02 (collision branch), ST-08/§14 line.
Go-live relevance: YES (via the collision branch's correctness).
Failure if omitted: divergence_expected is noise → expected
  divergences park as CRITICAL or anomalies pass as expected.
```

### CA-7 — Test catalog

```text
Section: §16.6 artifact 6.
Owner type: TEAM, named owner.
Purpose: single owned index of every required test.
Required contents: Section J's T-01..T-32; the spec-named entries
  (downgrade re-POST answered DUPLICATE_REQUEST leaves prior uetr
  intact; §11 ambiguous claim-commit; §8 concurrent in-flight
  duplicates); per-entry §-traceability, owner type, implementing
  phase, blocking flag.
Validation: cross-check against Section Q's test items and §18-1's
  matrix; every entry mapped to an implemented test or an open task.
Dependent tasks: all test-bearing cards; GO-04 evidence assembly.
Go-live relevance: YES — it is how "all the tests exist" is audited.
Failure if omitted: coverage claims unauditable; the §18-1 matrix can
  silently lose a case.
```

### CA-8 — Runbook stubs

```text
Section: §16.6 artifact 7; §15.
Owner type: OPS + TEAM.
Purpose: every alert actionable; the aged-MAYBE path documented.
Required contents: one stub per §15 alert (Trigger / Severity / Why /
  Immediate action / Data to collect / Escalation / Safe stop —
  Section N seeds the majors); the unqueryable-aged-MAYBE runbook
  (platform lookup → TL-10 rejection or the OP procedure); known-
  outage suppression semantics; §5.2 restore = post-MVP stub only
  ("major incident — manual engine-side reconciliation").
Validation: ops-owner review; OB-06 links every alert to its stub.
Dependent tasks: OB-03..06, OP-03 (drill references it).
Go-live relevance: YES — runbook stubs are a Q item.
Failure if omitted: 03:00 alerts without actions; operators improvise
  against money states (the exact fat-finger scenario the triggers
  guard).
```

### CA-9 — apply-platform-verified-outcome procedure spec

```text
Section: §16.6 artifact 8; §9.3, §18-3, §20-8.
Owner type: TEAM + DBA + OPS.
Purpose: the implementable spec for OP-01 and the §18-3 drill.
Required contents: signature (request_id, EXECUTED|REJECTED, mandatory
  ticket/evidence ref, two distinct authenticated approvers); dual-
  control enforcement IN the operation (two-step approval workflow / signed assertion — §9.3); evidence-flag mechanics;
  application through the same evidence-guarded CAS; money effects
  per outcome; refusal conditions (CLAIMED, terminal, amount
  mismatch); §15 every-use alert; §14 audit line
  (trigger_source=OPS_PLATFORM_VERIFIED + ticket); restricted role;
  the ops drill script.
Validation: DBA + ops review; OP-02 suite green; OP-03 drill signed.
Dependent tasks: OP-01/02/03; RG-05 (guard interplay); B-04.
Go-live relevance: YES — §18 BLOCKING item 3.
Failure if omitted: unresolvable MAYBE rows hold reservations forever;
  scopes never complete; I6 blocks successors (§18-3's wedge).
```

