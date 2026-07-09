# Payment Orchestration — Portable Implementation Playbook

**Baseline spec:** `requirment-v4.md` (Requirements v4, Factored State Model, 2026-07-05 — BASELINE, hardened through fourteen review rounds).
**Date:** 2026-07-06
**Transfer rule:** this document is one-way portable. It contains NO local source-code names, NO proprietary logic, NO confidential details. All local components are placeholders (Section G) mapped on the work laptop only (Section O). Nothing from the work laptop flows back.
**Executor:** designed for a weak, low-context local coding agent. Every task is executable from: the task card + the referenced requirement sections + the locally-mapped files + the validation steps. No task requires whole-design understanding.

> **For the local agent:** Follow Section P. Execute exactly one task card at a time, using its Minimal Context Packet (Section I). Discovery first (Phase P1), no implementation during discovery.

---

# PLAYBOOK INDEX (compact — task IDs are stable throughout)

## Section list

```text
A  Executive summary
B  Assumptions and non-goals
C  Requirement extraction and classification
D  Implementation dependency graph
E  Recommended implementation phases (P1–P14)
F  Local source-code discovery workflow
G  Placeholder component glossary
H  Small executable task cards (grouped by phase)
I  Minimal context packets (one per task card)
J  Test-first strategy and test matrix (T-xx)
K  Provider / tech-lead / PO contract questions (Q-xx)
L  Companion artifact plan (CA-1 … CA-9)
M  Migration / rollout / rollback plan
N  Observability, reconciliation, and runbook plan
O  Local-only placeholder mapping template
P  Instructions for the local coding agent
Q  Go-live readiness checklist
R  Playbook quality self-check + task execution report template
```

## Phase list

```text
P1   Local codebase discovery only                          (tasks D-xx)
P2   §18 BLOCKING gate resolution + companion artifacts     (tasks B-xx, CA-x)
P3   Schema and migration foundation                        (tasks S-xx)
P4   Deterministic idempotency key generation/persistence   (tasks K-xx)
P5   SDK-assigned UETR response persistence / feed matching (tasks U-xx)
P6   Factored request lifecycle + state-machine hardening   (tasks ST-xx)
P7   Reservation / obligation consistency + release guards  (tasks RG-xx)
P8   Provider idempotency sandbox contract test suite       (tasks CT-xx)
P9   Upstream and feed contract handling                    (tasks IN-xx)
P10  Retry / recovery / MAYBE_SUBMITTED resolver            (tasks RC-xx)
P11  MVP apply-platform-verified-outcome stored procedure   (tasks OP-xx)
P12  Reconciliation / drift scanner / tripwires             (tasks OB-01..02)
P13  Observability, alerts, and runbook stubs               (tasks OB-03..07)
P14  Migration, rollout, rollback, and go-live gates        (tasks GO-xx)
```

## Task ID list

```text
P1  Discovery:        D-01 D-02 D-03 D-04 D-05 D-06 D-07 D-08 D-09 D-10 D-11 D-12
P2  Gates/artifacts:  B-01 B-02 B-03 B-04
                      CA-1 CA-2 CA-3 CA-4 CA-5 CA-6 CA-7 CA-8 CA-9
P3  Schema:           S-01 S-02 S-03 S-04 S-05 S-06 S-07 S-08 S-09
P4  Identity:         K-01 K-02 K-03 K-04 K-05 K-06
P5  UETR:             U-01 U-02 U-03
P6  State model:      ST-01 ST-02 ST-03 ST-04 ST-05 ST-06 ST-07 ST-08 ST-09 ST-10 ST-11
P7  Reservation:      RG-01 RG-02 RG-03 RG-04 RG-05 RG-06 RG-07 RG-08 RG-09 RG-10
P8  Contract tests:   CT-01 CT-02 CT-03 CT-04 CT-05 CT-06 CT-07
P9  Inbound flows:    IN-01 IN-02 IN-03 IN-04 IN-05 IN-06 IN-07 IN-08 IN-09
P10 Retry/recovery:   RC-01 RC-02 RC-03 RC-04 RC-05 RC-06 RC-07 RC-08 RC-09 RC-10
P11 Operator proc:    OP-01 OP-02 OP-03
P12 Drift:            OB-01 OB-02
P13 Observability:    OB-03 OB-04 OB-05 OB-06 OB-07
P14 Rollout:          GO-01 GO-02 GO-03 GO-04 GO-05
Tests (Section J):    T-01 … T-32
Questions (Section K): Q-01 … Q-20
```

## Dependency order (phase-level; details in Section D)

```text
P1 → P2 → P3 → P4 → P5 → P6 → P7 → P9 → P10 → P11 → P12 → P13 → P14
                 └──────────── P8 runs in parallel from P4 onward;
                               its PASS gates P10 auto-downgrade
                               reliance and gates go-live (§18-1)
```

## BLOCKED tasks (unsafe before a §18 BLOCKING decision)

```text
BLOCKED on §18 item 0 residue (snapshot contract — task B-01):
  Per the §1 contract facts (one trade carries MULTIPLE payments;
  each message is a full-trade snapshot, newer overwrites older;
  scope tuple unique within a snapshot), the scope key needs NO
  discriminator and §5.1 identity stands — S-02/S-03/S-05/K-02/
  K-03/CA-4/CA-5 are NOT gated by this item. What it DOES gate is
  the §6 consumer freeze (IN-02):
    - written upstream confirmation (upstream ask 5) of the snapshot
      schema + within-snapshot uniqueness
    - within-snapshot uniqueness intake validation (§6.0)
    - PO-9 (absence semantics — BA-2 amendment) and TL-16 (snapshot
      ordering-watermark rule): both shape §6.1 fan-out (IN-02)
    - §12 card lookup rewrite (returns ALL obligations of the trade;
      step-granularity clause added to TL-2)

BLOCKED on §18 item 1 (collision contract proof — tasks CT-01..05):
  Nothing at implementation time (the design carries no runtime gate —
  §1 assumed contract facts); but GO-04 (go-live) is BLOCKED until
  CT-02..CT-05 pass in sandbox.

BLOCKED on §18 item 2 (cutoff calendar — task B-03):
  RC-04 cutoff-check configuration and GO-04. Implementation of the
  cutoff INTERFACE proceeds with fail-blocked default.

BLOCKED on §18 item 3 (MAYBE terminal exit — task B-04):
  OP-01..OP-03 implement the DEFAULT resolution (the audited stored
  procedure). GO-04 is BLOCKED until OP-03 (drill) passes OR the
  stated alternative (TL-10 + TL-5 lookback ≥ max row lifetime) is
  affirmatively answered.
```

## Go-live blockers (full checklist in Section Q)

```text
1. §18 item 0 residue closed: written snapshot-contract confirmation
   (upstream ask 5), §6.0 intake validation, PO-9, TL-16 (B-01)
2. §18 item 1 sandbox proof executed and PASSED (CT-02..CT-05)
3. §18 item 2 cutoff calendar sourced, owned, configured (B-03)
4. §18 item 3 apply-platform-verified-outcome procedure EXISTS and is
   DRILLED (OP-01..03) — or the stated alternative fully satisfied
5. Identity golden-vector tests pass (K-03, CA-5)
6. Duplicate-prevention + crash/restore retry tests pass (Section J)
7. Observability + runbook stubs live (P13); rollout plan approved (P14)
```

---

# A. Executive summary

**What this playbook is for.** It converts the settled requirements
document `requirment-v4.md` into an ordered, source-code-agnostic
implementation workflow: phases, small task cards, tests, companion
artifacts, open questions, and go-live gates. It is the single
document transferred to the work laptop; local execution happens
there against the real codebase.

**What it deliberately does not assume.** It assumes NO knowledge of
the real repository: no file names, class names, package names, job
names, stored-procedure names, or local conventions. It assumes only
what `requirment-v4.md` documents: the stack (Java Spring Boot,
Oracle, Spring Kafka, Hazelcast), the four documented services
(`PaymentOrchestrationService`, `PaymentEnrichmentService`,
`PaymentExecutionService`, `PaymentNotificationConsumerService`), the
three core tables (`payment_obligation`, `payment_request`,
`processed_inbound_event`), the documented columns/states, and the
documented companion artifacts. Everything else is a placeholder
(Section G) to be mapped locally (Section O).

**How to use it on the work laptop.**

```text
1. Run Phase P1 (discovery only) and fill the Section O mapping
   template. No implementation during discovery.
2. Record §18 BLOCKING answers as they arrive (Phase P2). Do not
   start a task marked BLOCKED on an unanswered item.
3. Execute task cards one at a time, in dependency order, each with
   its Minimal Context Packet (Section I). Validate, report
   (Section R template), stop, then take the next card.
4. Track go-live readiness against Section Q.
```

**Why it is source-code-agnostic.** The work laptop is on the far
side of a one-way transfer: no source-code details can ever come
back. A playbook that guessed local names would be unverifiable and
wrong in unknowable ways. Instead, every task names placeholder
components plus local discovery instructions; the mapping from
placeholder to real code exists only on the work laptop.

**How it protects against a weak / low-context execution agent.**

```text
- Every task card is self-contained: prerequisites, the exact §s to
  read, the exact placeholders to have mapped, instructions, tests,
  stop condition. No task requires remembering earlier reasoning.
- Minimal Context Packets (Section I) are paste-alone briefs.
- Tasks never mix schema + state machine + provider integration +
  recovery + observability + rollout.
- Anything requiring local judgment is marked MUST_VERIFY_LOCALLY;
  anything unanswerable is marked UNCLEAR or BLOCKED — the agent is
  instructed to stop, not guess (Section P).
- Section Q gates prevent "done locally" from being confused with
  "safe to go live".
```

---

# B. Assumptions and non-goals

**Assumptions**

```text
1. The existing system already implements the core payment-processing
   business logic: how to make a payment, account detection,
   debit-party lookup, address lookup, enrichment, validation, and
   payment construction. All of it MUST BE PRESERVED.
2. This work is an ENHANCEMENT of the current system, not a rewrite.
   Prefer additive changes (new columns, new guards, new jobs) over
   destructive rewrites.
3. `requirment-v4.md` is the baseline implementation specification;
   all accepted review findings are already folded into it. This is a
   fresh planning session — nothing is inferred from prior review
   conversations.
4. The target stack is as documented: Java Spring Boot, Oracle DB,
   Spring Kafka, Hazelcast (§ front matter). Oracle DDL / CHECK
   constraints / function-based unique indexes / triggers / audited
   stored procedures; Spring transactions, repositories, scheduled
   jobs; Spring Kafka consumers with inbox idempotency; Hazelcast
   posting freeze.
5. The data model is exactly three core tables: payment_obligation,
   payment_request, processed_inbound_event (§2). No new persistent
   tables, journals, outboxes, parked-event tables, attempt-history
   tables, manual-action tables, or audit-history tables. A task that
   appears to need a new table is a SPEC_CONFLICT, not a new table.
```

**Non-goals**

```text
1. NO business-rule redesign. NO rewrite of payment decision logic.
   NO change to existing payment attributes unless a concrete
   correctness invariant in requirment-v4.md requires it. If a local
   implementation appears to require changing a business rule, mark
   it BUSINESS_RULE_CHANGE_REQUIRED and name the requirement section
   that creates the need — then stop.
2. NO source-code-specific guesses. Placeholders + local discovery
   only.
3. NO new findings. This playbook does not review, critique, or
   improve the design. Rejected alternatives recorded in the spec
   (derived committed_amount, attempt-history table, materiality
   re-POST, auto-unlatch, UETR generation/validation, runtime
   collision-contract gating) stay rejected and are not re-proposed.
4. NO re-opening of §1.1 Basic Agreements (BA-1 scope-key mutability,
   BA-2 no upstream cancellation, BA-3 ordering is upstream's
   responsibility) or the §1 contract facts.
5. NO implementation of future work (§19.1 completion signal, §19.2
   returned funds, §19.3 retry-after-reject, §5.2 DR runbook tooling,
   ops console beyond the one MVP procedure, §6.6 key-only anchoring
   before TL-7 confirms) unless §18 explicitly makes it BLOCKING.
6. NO full ops console. The ONLY manual-operation implementation work
   at MVP is the §9.3 apply-platform-verified-outcome audited stored
   procedure (§18 BLOCKING item 3, §20). Everything else in §20 is
   future / PO discussion.
7. The old compound status may survive only as a derived display
   label (§10.4); migration of business logic away from any legacy
   compound status enum is gradual and safe (Phase P6), never a
   big-bang deletion.
```

---

# C. Requirement extraction and classification

Classification codes:

```text
MVP        MVP normative implementation
GATE       §18 BLOCKING go-live gate
ARTIFACT   §16.6 companion artifact
RUNBOOK    operational runbook / drill
QUESTION   PO / tech-lead / provider / upstream open question
FUTURE     future work / not scheduled
DISCOVERY  local discovery task only
```

Column key: **Impl** = implementation relevance (task IDs);
**Dep** = principal dependency; **GL** = blocks go-live;
**Loc** = requires local source discovery; **Ext** = requires
provider / tech-lead / PO / upstream confirmation.

| # | § | Requirement summary | Class | Impl | Dep | GL | Loc | Ext |
|---|---|---------------------|-------|------|-----|----|-----|-----|
| C1 | §1 | Pull-only orchestrator: two inbound flows, no notifications, no outbox, card reads state | MVP (constraint) | shapes all phases | — | — | yes | no |
| C2 | §1, §8, §16.4 | Confirmed contract fact: engine settles all-or-nothing; amount mismatch = defect | MVP | IN-08, RG-03 | — | no | yes | confirmed |
| C3 | §1 | Assumed contract facts: known-key re-POST never executes new payment; key retention covers row lifetime; dedup keys on caller key | GATE (§18-1) | CT-01..05; no runtime gating anywhere | B-02 | YES | no | yes (proof by test) |
| C4 | §1.1 | BA-1..3 Basic Agreements (scope-key mutability; no upstream cancel; ordering is upstream's) | settled constraint | none — do NOT build machinery for these | — | no | no | no |
| C5 | §2.1 | payment_obligation: scope key, amounts, ordering fields, markers (validation_failed, provider_rejected + counters + first_at), read-model fields | MVP | S-02, S-05 | — (scope key settled, §1) | yes | yes | no |
| C6 | §2.2 | payment_request: 4 dimension columns; supporting fields (identity, uetr, version, claim/retry/resolver fields, last_sent_hash, divergence_expected, divergent_payload_at, episode anchors) | MVP | S-03, S-05 | — (scope key settled, §1) | yes | yes | no |
| C7 | §2.2, §10.3 | Constraints: UNIQUE(idempotency_key), UNIQUE(uetr), I6 function-based unique index, enum CHECKs, L1-shape/L2–L8 CHECKs, freeze + release-guard triggers | MVP | S-05, S-06 | S-02/S-03 | yes | yes | no |
| C8 | §2.3, §16.2 | processed_inbound_event inbox + purge policy (inbox_retention > kafka_retention ≥ replay_window, named owner) | MVP + RUNBOOK | S-04, OB-05 | — | no | yes | owner needed |
| C9 | §3 | Reservation semantics: +committed at creation, −committed on terminal-negative row-count-1, no movement at POST/confirm; I1–I6 | MVP | RG-01..03, RG-06 | S-xx | yes | yes | no |
| C10 | §3 | Drift scanner: recompute I1/I2, snapshot + re-check under lock, page on mismatch; verifies L9 | MVP | OB-01 | RG-xx | yes | yes | no |
| C11 | §3, §20 | Supersede/close operation (release-guarded); at MVP exercised via controlled manual DB procedure under §10.3 backstops, not a console | MVP guard + RUNBOOK | RG-05, S-06, CA-8 | S-06 | no | yes | no |
| C12 | §4.1 | Step-status completion predicate (incl. vacuous-completion guards) | MVP | RG-08 | RG-01..03 | no | yes | no |
| C13 | §4.2, §4.5 | Active-exception derivation (precedence ranks) + next-actor derivation — derived, never stored/accumulated | MVP | RG-09 | RG-08 | no | yes | no |
| C14 | §4.4, §10.1 | Evidence rules: terminal evidence → any active row; intermediate → non-CLAIMED only; stale/duplicate → zero rows | MVP | IN-07, RC-06 | ST-02 | yes | yes | no |
| C15 | §5 | Write-ahead identity: no POST under a caller-supplied identity not durably persisted | MVP | K-04 | K-02 | YES | yes | no |
| C16 | §5, §5.1 | Deterministic idempotency key: hash(scope + request_seq), byte-exact, versioned, amount NOT in key, golden vectors | MVP + ARTIFACT (CA-5) | K-01..03 | — (scope key settled, §1) | YES | yes | no |
| C17 | §5 | UETR is SDK/engine-assigned; never generated/validated here; persisted ONLY from acceptance-class responses; never a dedup key | MVP | U-01..03 | D-05 | yes | yes | TL-11 |
| C18 | §5.2 | Post-restore DR runbook + step-5b enumeration tooling | FUTURE (post-MVP, PO decision) | none now; deterministic key (C16) stays | — | no | no | TL-3 |
| C19 | §6.0 | Upstream message contract (fields, Kafka key = business_id, payload-equality definition); build-time enforcement | MVP + QUESTION | IN-01; Q upstream 1–4 | — | no | yes | yes |
| C20 | §6.1–6.2 | Normal processing under obligation lock; zero-shortfall short-circuit; ORA-00001 race retry | MVP | IN-02 | S-xx | yes | yes | no |
| C21 | §6.3 | Request amount immutable after creation; changes only via new upstream message | MVP | ST-01, RG-06 | — | yes | yes | no |
| C22 | §6.4 | Auto-cancel of un-posted requests (CAS set semantics); row-count-0 branching on submission_state; AMENDMENT_PARKED; retry-guard staleness re-check | MVP | RG-07, RC-03 | ST-02 | yes | yes | no |
| C23 | §6.5 | Step reopening on required_amount increase; reopened_at; overpay latch guard (AMENDMENT_ON_LATCHED_SCOPE) | MVP | RG-10 | RG-04 | no | yes | no |
| C24 | §6.6 | Validation-failure anchor obligation rows; DLT for unidentifiable messages | MVP | IN-03 | S-02 | no | yes | no |
| C25 | §6.6 | Key-only anchoring (tiered) | QUESTION (TL-7) → scheduled later | none at MVP | upstream ask 2 | no | no | yes |
| C26 | §6.7 | Ordering guard: strictly-newer mutation, stale counted, tie handling (identical→drop; differing→AMENDMENT_TIE_CONFLICT); pluggable comparison point | MVP | IN-02 | — | yes | yes | upstream ask 1 |
| C27 | §6.8 | Standing shortfall re-evaluation — the SINGLE request-creation point; trigger inventory T1–T4; ordering-aware successor policy | MVP | RG-06 | RG-01, IN-02 | yes | yes | PO-6 (latency accepted) |
| C28 | §6.9 | One staleness guard per mutable input (normative inventory; monotonic marker writes) | MVP | IN-04 | S-02 | no | yes | no |
| C29 | §7.0 | Fresh instruction assembly per attempt; last_sent_hash + divergence_expected at claim time; repost_permitted single gate, checked at BOTH ends; dual-control override of staleness term only | MVP | K-05, RC-03 | K-02, CA-6 | yes | yes | TL-4 via §18-1 |
| C30 | §7.1 | submission_state definitions (NOT/MAYBE/SUBMITTED — "cannot execute" is the criterion) | MVP | RC-02 | — | no | yes | TL-6 via §18-1(d) |
| C31 | §7.2 | POST-failure classification (closed taxonomy; MAYBE fail-closed; collision branch on divergence_expected; UETR not persisted on rejects) | MVP + ARTIFACT (CA-1) | RC-01, RC-02 | CA-1 | yes | yes | yes (code table) |
| C32 | §7.3 | Enrichment outcome classification (all NOT_SUBMITTED) | MVP | RC-01 | — | no | yes | no |
| C33 | §7.4 | Retry policy per error class (externalized config); exhaustion → BLOCKED; cutoff pre-checks; downgrade policy class (attempt reset) | MVP | RC-04 | B-03 (cutoff) | partially | yes | no |
| C34 | §8 | Feed consumption transaction order (inbox → resolve → evidence CAS → commit → ack); unmatched = log+count+ack; amount equality; marker totality; anomaly disambiguation | MVP | IN-05..08 | S-04, ST-02 | yes | yes | no |
| C35 | §8 | provider_reference fallback — fail closed (single active match + amount + recency) until uniqueness confirmed | MVP + QUESTION (TL-12) | IN-06 | U-02 | no | yes | yes |
| C36 | §9.1 | Status-query outcome mapping (EXECUTED/REJECTED/NOT_FOUND/INDETERMINATE/ACCEPTED) | MVP + ARTIFACT (CA-3) | RC-06 | CA-3 | no | yes | yes |
| C37 | §9.2 | NOT_FOUND trust-age rule; §9.2 auto-downgrade (the ONE backward stage move) gated by repost_permitted; SUBMITTED branch → ENGINE_INCONSISTENCY park | MVP | RC-07 | RC-03, TL-5 config | yes | yes | TL-5 (ingest lag) |
| C38 | §9.3 | MAYBE escalation (maybe_since clock, once per episode, tiered); ops action set for BLOCKED·MAYBE rows; apply-platform-verified-outcome as MVP terminal exit | MVP + GATE (§18-3) | RC-08, OP-01..03 | B-04 | YES | yes | no |
| C39 | §9.4 | Release-rights invariant (single sanctioned exception: the §9.3 procedure) | MVP | RG-05, S-06 | — | yes | yes | no |
| C40 | §9.5 | Resolver scope keyed on submission_state+outcome ONLY; bounded prioritized sweep; per-row backoff; never-overlap; SUBMITTED damping | MVP | RC-05 | TL-13 (rate limit) | yes | yes | yes |
| C41 | §10.1–10.5 | Factored state model: global rules, per-dimension transitions, legality matrix L1–L9, display labels (no rule keys on label or blocked_reason) | MVP | ST-01..08, S-05..06 | S-xx | yes | yes | no |
| C42 | §11 | Two-tier concurrency (obligation lock + request CAS); lock ordering; claims as leases; posting-claim persistence; ambiguous claim-commit; lease-expiry recovery; graceful shutdown | MVP | ST-02, ST-09..11 | S-xx | yes | yes | no |
| C43 | §12 | Card read model: business_id-only lookup, >1 row = error+alert, NOT_STARTED = absence, unavailable ≠ stale, freshness/lag indicator | MVP + QUESTION (TL-2 read contract) | RG-08..09, OB-04 | B-01 | no | yes | yes |
| C44 | §13 | Exception categories/severities; overpay latch = one-way door, ignore-forward, alert on set | MVP | RG-04, RG-09 | — | no | yes | no |
| C45 | §14 | No journal; structured CAS log line (key+seq+dimensions before→after); posting-claim line carries sent hash; 90-day retention floor | MVP | ST-08 | — | no | yes | no |
| C46 | §15 | Monitoring list (drift page, MAYBE ages, BLOCKED queue, marker alerts, freeze-effective page, watchdogs); clock discipline (episode anchors); alert rollup | MVP | OB-03..05 | P6–P12 | yes | yes | no |
| C47 | §16.1 | Resiliency: timeouts, breakers (business rejects = success), scanner gating, poison-row cap, bulkheads, Hazelcast posting freeze fail-safe (absent/unreachable/timeout = FROZEN; only FROZEN cached), freeze/breaker deadline suspension | MVP | RC-09, RC-10 | — | yes | yes | no |
| C48 | §16.2 | Kafka rules: manual ack after commit, earliest, ErrorHandlingDeserializer, DLT for poison only, no retry topics for money events, partition keying, retention-chain check | MVP + RUNBOOK | IN-09, OB-05 | — | yes | yes | no |
| C49 | §16.3 | Security: read-surface auth, account masking in encoder, no instruction content persisted, secrets vaulted, topic ACLs | MVP | cross-cutting (IN, U, OB) | — | no | yes | no |
| C50 | §16.4 | Amount/time hygiene: currency-scale validation, BigDecimal.compareTo, no tolerance, UTC + DB time, tz-aware cutoff calendar | MVP | IN-01, RC-04 | B-03 | no | yes | no |
| C51 | §16.5 | Expand/contract migrations (Flyway/Liquibase); volume NFR ~3k trades/day; contract tests for 3 external contracts; defensive enum reads (UNKNOWN sentinel); 4 dimension enums CLOSED | MVP | S-01..09, GO-01 | — | yes | yes | no |
| C52 | §16.6 | Configuration inventory + config-load ordering validation (trust_age + cadence < escalation < tier-2 < cutoff margin) | MVP | OB-07 | B-03, TL-5, TL-13 | no | yes | values needed |
| C53 | §16.6-1 | Engine error-code → classification table (incl. replay-original-response class) | ARTIFACT | CA-1 | provider | YES (feeds RC-01) | no | yes |
| C54 | §16.6-2 | Engine status vocabulary + precedence/evidence mapping + feed event schema | ARTIFACT | CA-2 | provider | YES (feeds IN-07) | no | yes |
| C55 | §16.6-3 | Status-query response → §9.1 outcome mapping | ARTIFACT | CA-3 | provider | YES (feeds RC-06) | no | yes |
| C56 | §16.6-4 | Full Flyway DDL migration set (I6 expression, CHECKs, triggers, active-row-bounded index list) | ARTIFACT | CA-4 | — (scope key settled, §1) | YES | yes | no |
| C57 | §16.6-5 | Identity-derivation spec + golden vectors | ARTIFACT | CA-5 | — (scope key settled, §1) | YES | no | no |
| C58 | §16.6-5 | Canonical instruction serialization + last_sent_hash definition | ARTIFACT | CA-6 | CA-5 | YES | yes | no |
| C59 | §16.6-6 | Test catalog aligned to the spec | ARTIFACT | CA-7 | — | YES | yes | no |
| C60 | §16.6-7 | Runbook stubs (one per §15 alert; aged-MAYBE runbook) | ARTIFACT + RUNBOOK | CA-8 | OB-xx | YES | no | no |
| C61 | §16.6-8 | apply-platform-verified-outcome stored procedure spec + drill script | ARTIFACT + GATE (§18-3) | CA-9, OP-01..03 | B-04 | YES | yes | no |
| C62 | §18-0 | BLOCKING residue of the snapshot contract (model = §1 contract fact: multiple payments; snapshot messages; tuple unique within snapshot; no discriminator — schema freeze not gated): upstream ask 5 in writing, §6.0 intake validation, PO-9, TL-16 — gates IN-02 | GATE | B-01 | upstream/UI teams + PO | YES | no | YES |
| C63 | §18-1 | BLOCKING: engine idempotency-collision contract proven by sandbox test (a–d), re-run on engine releases | GATE | B-02, CT-01..05 | sandbox access | YES | no | YES |
| C64 | §18-2 | BLOCKING: payment cutoff calendar (source, owner, semantics, tz-aware, refresh, fail direction) | GATE | B-03 | calendar owner | YES | no | YES |
| C65 | §18-3 | BLOCKING: MVP MAYBE-row terminal exit (procedure EXISTS + DRILLED, or TL-10 ∧ TL-5 alternative) | GATE | B-04, OP-01..03 | CA-9 | YES | yes | possibly |
| C66 | §18 PO 1–8 | PO items: ask-then-retry approval, query cadence, escalation age, cutoff-passed-while-MAYBE, cancelled-trade display, deferral latency, retry-after-reject concept, fresh-assembly consequence | QUESTION | Section K | — | no | no | YES |
| C67 | §18 TL 1–15 | Tech-lead items: event_id stability, card read contract, RPO/RTO, collision contract, ingest lag + lookback, re-execute-after-reject, key-only anchoring, confirmation age, artifact owners, TL-10 platform reject, SDK contract, provider_reference, rate limits, archival, downgrade telemetry | QUESTION | Section K | — | TL-4/5 feed gates | no | YES |
| C68 | §18 upstream 1–4 | Upstream asks: strict ordering, business_id as Kafka key, schema formalization, emission contract | QUESTION | Section K | — | no | no | YES |
| C69 | §19.1 | Outbound completion signal | FUTURE | none | — | no | no | no |
| C70 | §19.2 | Returned funds / reconciliation visibility / manual-adjustment op | FUTURE | none | — | no | no | no |
| C71 | §19.3 | Ops retry-after-provider-reject (4-eyes, marker clear) | FUTURE (pending PO-7) | none | PO-7 | no | no | yes |
| C72 | §20 | Ops console / manual operations beyond the one MVP procedure | QUESTION / FUTURE | none (only OP-xx at MVP) | PO | no | no | yes |

---

# D. Implementation dependency graph

Source-code-agnostic graph of implementation AREAS (phase IDs from
Section E). Arrows read "must be settled before".

```text
                      ┌────────────────────────────────────────────┐
                      │ P1 DISCOVERY (read-only; fills Section O)  │
                      └───────────────┬────────────────────────────┘
                                      ▼
        ┌───────────────────────────────────────────────────────────┐
        │ P2 §18 BLOCKING GATES + COMPANION ARTIFACTS                │
        │  B-01 snapshot residue (§18-0) ── blocks IN-02 (§6 flow)   │
        │  B-02 sandbox access (§18-1) ── blocks P8 execution        │
        │  B-03 cutoff calendar (§18-2) ── blocks cutoff config      │
        │  B-04 MAYBE exit decision (§18-3) ── default = P11         │
        │  CA-1..9 artifact authoring (owners per §16.6)             │
        └───────┬───────────────────────────────────────────────────┘
                ▼
        ┌──────────────────────────────┐
        │ P3 SCHEMA & MIGRATION        │  ← scope key settled
        │ (tables, CHECKs, I6, triggers│    (§1 contract facts);
        │  indexes; expand/contract)   │    CA-4 is the gate
        └───────┬──────────────────────┘
                ▼
        ┌──────────────────────────────┐     ┌───────────────────────┐
        │ P4 IDENTITY (deterministic   │────▶│ P8 PROVIDER CONTRACT  │
        │ key gen + write-ahead        │     │ TESTS (sandbox; §18-1)│
        │ persistence + golden vectors)│     │ runs in parallel;     │
        └───────┬──────────────────────┘     │ PASS gates re-POST    │
                ▼                            │ reliance (P10) and    │
        ┌──────────────────────────────┐     │ go-live               │
        │ P5 UETR RESPONSE PERSISTENCE │     └───────────────────────┘
        │ (acceptance-class only)      │
        └───────┬──────────────────────┘
                ▼
        ┌──────────────────────────────┐
        │ P6 FACTORED STATE MODEL      │  schema (P3) before state-
        │ (dimensions, CAS discipline, │  machine persistence; state
        │  legality, claims/leases)    │  legality before retries
        └───────┬──────────────────────┘
                ▼
        ┌──────────────────────────────┐
        │ P7 RESERVATION / RELEASE     │  reservation semantics before
        │ GUARDS + DERIVATION (§3, §4) │  completion derivation
        └───────┬──────────────────────┘
                ▼
        ┌──────────────────────────────┐
        │ P9 UPSTREAM + FEED CONTRACT  │  needs state legality (P6),
        │ HANDLING (§6, §8)            │  money choreography (P7)
        └───────┬──────────────────────┘
                ▼
        ┌──────────────────────────────┐
        │ P10 RETRY / RECOVERY /       │  needs P6 legality, P7 money,
        │ MAYBE RESOLVER (§7.4, §9)    │  P8 PASS before auto-downgrade
        └───────┬──────────────────────┘  reliance; B-03 for cutoffs
                ▼
        ┌──────────────────────────────┐
        │ P11 APPLY-PLATFORM-VERIFIED- │  MAYBE-row terminal exit must
        │ OUTCOME PROCEDURE (§9.3)     │  exist before go-live (§18-3)
        └───────┬──────────────────────┘
                ▼
        ┌──────────────────────────────┐
        │ P12 DRIFT / RECONCILIATION   │  verifies P7 invariants in
        │ TRIPWIRES (§3, §8, L9)       │  production
        └───────┬──────────────────────┘
                ▼
        ┌──────────────────────────────┐
        │ P13 OBSERVABILITY + RUNBOOKS │  observability/runbooks
        │ (§15, §16.6-7)               │  before rollout
        └───────┬──────────────────────┘
                ▼
        ┌──────────────────────────────┐
        │ P14 MIGRATION / ROLLOUT /    │  all §18 gates + Section Q
        │ ROLLBACK / GO-LIVE GATES     │  checklist
        └──────────────────────────────┘
```

**Why this order (each required ordering, explicitly):**

```text
1. §18 item 0 residue BEFORE the §6 consumer freeze: the model is a
   §1 contract fact (multiple payments per trade; snapshot messages;
   tuple unique within a snapshot → NO discriminator), so the
   scope-key/identity/schema freeze is NOT gated here. The §6
   consumer (IN-02) waits on the B-01 residue: written uniqueness
   guarantee (upstream ask 5), intake validation (§6.0), PO-9
   (absence), TL-16 (watermark).
2. Schema BEFORE state-machine persistence: the four dimension
   columns, CHECK constraints, I6, and triggers (§2.2, §10.3) are the
   substrate every CAS in P6 writes against; code written before the
   DB backstops exists is unverifiable against them.
3. Identity persistence BEFORE provider POST changes: §5's write-ahead
   rule ("no POST under an unpersisted caller-supplied identity") is
   the money-safety keystone; the posting path (P5/P6/P10) must find
   identity generation + persistence already in place.
4. State legality BEFORE retries/resolvers: retry scanner, resolver,
   downgrade, and escalation (P10) are all expressed as legality-
   guarded CASs (§10.3); building them before L1–L9 enforcement means
   their tests prove nothing.
5. Reservation semantics BEFORE completion derivation: §4.1's
   predicate is only correct because every ACTIVE request holds a
   reservation (§3); deriving completion before P7's money
   choreography is in place derives wrong answers.
6. Provider contract tests BEFORE relying on re-POST behavior: the
   §9.2 auto-downgrade and §7.0 fresh assembly stand entirely on the
   §1 assumed collision contract; P8's sandbox PASS (§18-1) is the
   proof. P10 may be BUILT in parallel but not TRUSTED/enabled toward
   production until P8 passes.
7. MAYBE-row terminal exit BEFORE go-live: without P11 (or the §18-3
   alternative), an unresolvable MAYBE row holds its reservation
   forever — scope never completes, I6 blocks successors (§18-3).
8. Observability / runbooks BEFORE rollout: §15 alerts + §16.6-7
   runbook stubs are the operating safety net; P14's enablement order
   requires the freeze-effective page, drift page, and MAYBE-age
   alerts to already be live.
```

---

# E. Recommended implementation phases

### Phase P1 — Local codebase discovery only

```text
Goal:            Map every placeholder component (Section G) to real
                 local code/DB objects; classify each design concept
                 as implemented / partial / missing; fill Section O.
Why here:        Everything downstream depends on knowing what exists.
                 Zero risk: read-only.
Sections:        whole spec skimmed; concept list in Section F.
Classification:  DISCOVERY (local discovery task only).
Required concepts in codebase: existing payment business logic
                 (preserved), existing tables, existing consumers,
                 existing POST client, existing jobs.
Placeholders:    ALL (Section G).
Data/schema deps: none.
State-transition deps: none.
Tests required:  none to write; INVENTORY existing tests (D-11).
Edge cases:      concepts implemented under unexpected shapes (e.g.
                 compound status enum instead of dimensions; random
                 keys instead of deterministic) — record as PARTIAL
                 with evidence, do not "fix" during discovery.
Common mistakes: implementing during discovery; guessing a mapping
                 instead of marking UNCLEAR; reading the whole repo
                 instead of targeted searches.
Completion:      Section O table filled for every placeholder, each
                 row Confirmed / UNCLEAR / MISSING; D-12 report done.
Verify locally:  everything — this phase IS local verification.
Go-live blocking: no (but P2+ cannot start safely without it).
```

### Phase P2 — §18 BLOCKING gate resolution + companion artifacts

```text
Goal:            Drive answers to §18 BLOCKING items 0–3 (record, not
                 decide); author the nine §16.6 companion artifacts
                 with named owners.
Why here:        §18-0's residue gates the §6 consumer (D graph #1);
                 CA-1/2/3/5/6 are inputs to implementation phases;
                 CA-4 is the P3 deliverable spec; CA-9 is P11's spec.
Sections:        §18 (all), §16.6, §5.1, §7, §9.1, §10.3, §9.3.
Classification:  GATE + ARTIFACT.
Required concepts: none in code (documents + provider engagement).
Placeholders:    [Contract Test Suite] (planning only), owners.
Data/schema deps: none (CA-4 drafted; freeze not gated on B-01 —
                 scope key settled, §1 contract facts).
State-transition deps: none.
Tests required:  golden vectors DRAFTED in CA-5 (executed in P4).
Edge cases:      CA-4/CA-5 freeze is NOT gated on §18-0 — no
                 discriminator under the §1 contract facts (multiple
                 payments per trade; snapshot messages; tuple unique
                 within snapshot). §12 lookup returns ALL of the
                 trade's obligations. IN-02 residue: upstream ask 5
                 (written uniqueness), §6.0 intake validation, PO-9
                 (absence), TL-16 (watermark).
Common mistakes: treating a written "yes" as closing TL-4/TL-6 (only
                 the §18-1 sandbox test closes them); letting artifact
                 authoring drift unowned; promoting PO-discussion
                 items to MVP.
Completion:      B-01 answered in writing; B-02 sandbox access + TTL
                 statement obtained; B-03 calendar source/owner named;
                 B-04 decision recorded (default: procedure); CA-1..9
                 drafted with owners; blocked-task list updated.
Verify locally:  nothing (no code).
Go-live blocking: YES — items 0–3 are the §18 gates.
```

### Phase P3 — Schema and migration foundation

```text
Goal:            Bring the three-table model to the §2/§10.3 target
                 shape via expand/contract Flyway migrations: new
                 columns (nullable-with-default first), enum CHECKs,
                 L-shape CHECKs, UNIQUE keys, I6 function-based unique
                 index, L1-freeze + release-guard triggers,
                 active-row-bounded index set, inbox table + purge.
Why here:        D graph #2 — schema before state-machine persistence.
Sections:        §2.1, §2.2, §2.3, §10.3, §16.5, §16.6-4, §3 (I6).
Classification:  MVP normative.
Required concepts: existing obligation/request-equivalent tables and
                 their real current shape (from D-02).
Placeholders:    [DB Migration Directory], [Stored Procedure / Trigger
                 Area], [Reservation Repository], [Obligation
                 Repository], [Request Status Persistence Layer].
Data/schema deps: scope key settled (§1 contract facts); CA-4 as
                 the authoritative DDL spec.
State-transition deps: none yet (columns land before rules).
Tests required:  migration apply on clean + prod-shaped schema;
                 constraint violation tests (each CHECK, I6, UNIQUEs);
                 trigger backstop tests (freeze; release guard denies
                 without evidence flag).
Edge cases:      existing rows under a legacy compound status —
                 backfill derivation must be reversible and NOVALIDATE
                 first (§16.5 enum evolution); Oracle UNIQUE on uetr
                 relies on NULL-ignoring index semantics.
Common mistakes: destructive rewrites instead of expand/contract;
                 adding CHECKs as VALIDATE against unmigrated rows;
                 inventing extra tables (SPEC_CONFLICT instead);
                 forgetting the active-row-bounded index trick.
Completion:      all migrations apply cleanly both ways per §16.5;
                 constraints/triggers demonstrably reject illegal rows;
                 S-09 migration tests green.
Verify locally:  real table names/shapes, existing migration tool
                 (Flyway vs Liquibase), existing data distributions.
Go-live blocking: YES (schema readiness is on the Q checklist).
```

### Phase P4 — Deterministic idempotency key generation and persistence

```text
Goal:            Implement §5.1: next_request_seq incremented under
                 the obligation lock in the request-insert transaction;
                 key = versioned byte-exact hash(scope | seq); identity
                 persisted in the posting-claim transaction BEFORE the
                 HTTP call; golden-vector tests frozen.
Why here:        D graph #3 — identity persistence before provider
                 POST work; also the P8 sandbox tests need real keys.
Sections:        §5, §5.1, §2.1 (next_request_seq), §2.2 (identity
                 fields), §11 (posting-claim persistence), §16.6-5.
Classification:  MVP normative (+ CA-5 artifact dependency).
Required concepts: request creation point; posting claim transaction;
                 any EXISTING key generation (from D-09 — if random
                 keys exist today, this REPLACES the generation rule
                 for new rows; existing in-flight rows keep theirs —
                 MUST_VERIFY_LOCALLY).
Placeholders:    [Payment Request Creation Component], [Provider POST
                 Client], [Request Status Persistence Layer],
                 [Reservation Repository].
Data/schema deps: P3 columns (idempotency_key UNIQUE, next_request_seq).
State-transition deps: none beyond claim transaction existing.
Tests required:  golden vectors (byte-exact, cross-JVM); same input →
                 same key; different seq/scope → different key; amount
                 NOT in key; persistence-before-POST ordering test;
                 UNIQUE violation handling.
Edge cases:      account-number canonicalization (case/trim/encoding)
                 per CA-5; hash versioning field; restore determinism
                 (seq re-derivation by construction).
Common mistakes: including amount or UETR in the derivation; deriving
                 from mutable/display fields; generating at POST time
                 instead of persisting in the claim transaction;
                 non-canonical serialization that breaks byte-exactness.
Completion:      K-01..K-06 done; golden vectors green and frozen in
                 the build.
Verify locally:  where the claim transaction boundary actually is;
                 whether identity columns already exist/are populated.
Go-live blocking: YES (identity golden vectors are a Q item).
```

### Phase P5 — SDK-assigned UETR response persistence and feed matching

```text
Goal:            Persist uetr ONLY from acceptance-class responses
                 (engine accepted / original-response replay); NEVER
                 from rejection/collision responses; persist any other
                 engine reference as provider_reference; feed matching
                 primary = UETR, fallback per §8 fail-closed rule.
Why here:        response-parser behavior must be correct before P9
                 feed matching and P10 resolver rely on stored uetr.
Sections:        §5 (identity chain + persistence rules), §2.2 (uetr,
                 provider_reference), §7.2 (which responses), §8
                 (matching), §16.6-6 (test: DUPLICATE_REQUEST leaves
                 prior uetr intact).
Classification:  MVP normative.
Required concepts: provider POST response parsing; SDK
                 validate-and-POST response shape (TL-11: which field
                 carries the UETR — MUST_VERIFY via provider/SDK docs).
Placeholders:    [Provider POST Client], [Provider Response Parser],
                 [Request Status Persistence Layer].
Data/schema deps: P3 (uetr UNIQUE NULL-tolerant; provider_reference).
State-transition deps: response classification branches (P10 RC-02)
                 may land after; U-01 only touches WHICH responses
                 write uetr.
Tests required:  acceptance persists uetr; DUPLICATE_REQUEST /
                 collision / sync reject do NOT write or overwrite;
                 feed event under a dead UETR never matches.
Edge cases:      response carrying a UETR AND a rejection; replayed
                 original response (artifact-1 class) IS acceptance-
                 class; crash before response → uetr stays NULL and
                 the row is recovered by key (§9).
Common mistakes: treating uetr as a dedup key; merging uetr with
                 provider_reference; persisting from every response
                 "for completeness".
Completion:      U-01..U-03 done; CA-2's dead-UETR question answered
                 or the fail-closed behavior tested regardless.
Verify locally:  SDK response field names; existing uetr handling.
Go-live blocking: indirectly (duplicate-prevention tests, Q).
```

### Phase P6 — Factored request lifecycle and state-machine hardening

```text
Goal:            Persist and enforce the four-dimension state model:
                 CAS discipline (conditional UPDATE + row-count
                 verdict, outcome IS NULL everywhere), legality matrix
                 in code paths, outcome-write normalization (freeze),
                 episode anchors, claims/leases + expiry recovery +
                 shutdown ordering, display label as derived-only,
                 gradual migration of rules off any legacy compound
                 status, structured CAS log line.
Why here:        D graph #4 — state legality before retries/resolvers.
Sections:        §2.2, §10.1–10.5, §11, §14, §16.5 (enum reads).
Classification:  MVP normative.
Required concepts: existing status persistence (likely a compound
                 enum — D-04); existing claim/lease or locking pattern.
Placeholders:    [Request Status Persistence Layer], [Payment Request
                 Creation Component], [Retry Resolver Job] (touch
                 points only), [Metrics / Alerting Layer] (log line).
Data/schema deps: P3 complete (columns + CHECKs + triggers).
State-transition deps: this phase DEFINES them.
Tests required:  per-transition legality tests (each L rule); CAS
                 row-count-0 on stale/duplicate writes; freeze
                 convention on outcome write; lease expiry at ENRICH
                 (re-claim) vs POST (→ CONFIRM·READY·MAYBE, never
                 re-claimed); ambiguous claim-commit → no HTTP call;
                 label derivation; defensive enum read (UNKNOWN
                 sentinel).
Edge cases:      dual-run window where old app version still writes
                 legacy status (expand/contract: dual-write, derived
                 label view); concurrent CAS races (two scanners —
                 SKIP LOCKED + lease).
Common mistakes: ORM dirty-checking on these tables; keying any rule
                 on the display label or blocked_reason; re-claiming
                 an expired POST claim; writing dimensions without the
                 obligation lock when a dimension changes; deleting
                 the legacy enum in one step.
Completion:      ST-01..ST-11 done; no business rule keys on legacy
                 compound status in MIGRATED paths (remaining sites
                 inventoried in ST-05 for follow-up); all transition
                 tests green.
Verify locally:  every legacy-status rule site (ST-05 inventory);
                 real transaction boundaries; shutdown hooks.
Go-live blocking: YES (factored model + "legacy status not used for
                 business rules" are Q items).
```

### Phase P7 — Reservation / obligation consistency and release guards

```text
Goal:            Money choreography per §3 (+committed at creation,
                 −committed on terminal-negative row-count-1,
                 +confirmed on EXECUTED with amount equality, overpay
                 latch); §6.8 standing re-evaluation as the SINGLE
                 creation point with trigger inventory + successor
                 policy; §6.4 auto-cancel + retry-guard; §4 derivation
                 (completion predicate, active exception, next actor);
                 §6.5 reopening + latch guard; §10.1 release guard in
                 code; supersede/close capability under guard.
Why here:        D graph #5 — reservation semantics before completion
                 derivation; needs P6 CAS discipline.
Sections:        §3, §4, §6.3–6.5, §6.8, §10.1, §13.
Classification:  MVP normative.
Required concepts: existing request-creation flow; existing amount
                 fields; obligation locking (SELECT FOR UPDATE).
Placeholders:    [Obligation Repository], [Reservation Repository],
                 [Payment Request Creation Component], [Request Status
                 Persistence Layer].
Data/schema deps: P3; P6 CAS discipline.
State-transition deps: terminal-negative CAS (P6) drives decrements.
Tests required:  I1–I6 invariant tests; same-transaction coupling
                 (amount moves iff CAS row-count 1); completion
                 predicate incl. vacuous-completion guards; overpay
                 latch one-way; auto-cancel set semantics + row-count-0
                 branches; successor policy (REJECTED ordering test,
                 reject-count gate); deferred amendment; reopening +
                 AMENDMENT_ON_LATCHED_SCOPE.
Edge cases:      §13 cross-stream race (latched scope underpaid
                 against newest truth — permanent, alert-only);
                 ENRICH·CLAIMED cancellable, POST·CLAIMED untouchable;
                 MAYBE row amendment → AMENDMENT_PARKED with deferred
                 park under live claim.
Common mistakes: moving money at POST time or on confirmation of
                 committed; releasing on MAYBE/SUBMITTED without
                 authoritative negative; a second request-creation
                 code path; deriving committed_amount (REJECTED
                 alternative — keep the stored counter).
Completion:      RG-01..RG-10 done; invariant tests green.
Verify locally:  existing money fields' semantics vs reservation
                 semantics (MUST_VERIFY — if the current counter means
                 "money at engine", a semantic migration note is
                 needed, not a silent reuse).
Go-live blocking: YES (reservation correctness is a Q item).
```

### Phase P8 — Provider idempotency sandbox contract test suite

```text
Goal:            Executable sandbox tests proving the §1 assumed
                 contract facts: (a) known key + identical payload →
                 dedup/ack/original-replay, nothing executes; (b)
                 known key + different payload → rejected without
                 execution, code distinguishable from DUPLICATE_REQUEST;
                 (c) key-retention TTL stated in writing + edge-tested;
                 (d) re-POST after sync business reject (re-executes vs
                 replays). Plus status-query mapping verification and
                 SDK contract checks (TL-11).
Why here:        D graph #6 — before RELYING on re-POST behavior
                 (P10 downgrade); runs in parallel from P4 (needs real
                 key generation).
Sections:        §1 (assumed facts), §18 BLOCKING item 1 (matrix),
                 §7.0–7.2, §9.1, TL-4/5/6/11/13.
Classification:  GATE (§18-1) — the tests ARE the go-live proof.
Required concepts: sandbox environment access (B-02); the real POST
                 client or a thin sandbox harness around it.
Placeholders:    [Contract Test Suite], [Provider POST Client],
                 [Status Query Resolver] (read side).
Data/schema deps: P4 key generation (tests must use the real
                 derivation).
State-transition deps: none (tests hit the engine, not our DB rules).
Tests required:  CT-02..CT-07 themselves; re-run procedure documented
                 for engine releases (§18-1).
Edge cases:      TTL edge (test at the retention boundary — the §9.2
                 lane is precisely the DELAYED one); engine replaying
                 the ORIGINAL response with no DUPLICATE code
                 (artifact-1 class — must map deliberately in CA-1).
Common mistakes: accepting a written "yes" instead of the executed
                 test (TL-4/TL-6 close ONLY via the test); testing
                 with synthetic keys that bypass the real derivation;
                 not scheduling the re-run on engine releases.
Completion:      CT-01..CT-07 executed with recorded evidence;
                 outcomes fed into CA-1/CA-3; if (c) TTL < max row
                 lifetime → repost_permitted gains a TTL term (RC-03
                 follow-up) and such rows are ops-only.
Verify locally:  sandbox credentials/endpoints (never in this doc).
Go-live blocking: YES — §18 BLOCKING item 1.
```

### Phase P9 — Upstream and feed contract handling

```text
Goal:            Upstream intake per §6 (validation incl. currency
                 scale, obligation upsert under lock, §6.7 ordering
                 guard + tie handling, §6.6 anchor rows + DLT, §6.9
                 monotonic marker writes + counters); feed consumption
                 per §8 (inbox-first transaction order, evidence-guarded
                 CAS, unmatched log+count+ack, provider_reference
                 fail-closed fallback, amount-mismatch park, anomaly
                 alerts); Kafka hardening per §16.2.
Why here:        needs P6 legality + P7 money choreography; feeds P10
                 (evidence application is shared machinery).
Sections:        §6.0–6.9, §8, §4.4, §16.2, §16.4.
Classification:  MVP normative.
Required concepts: both existing consumers (D-07); existing intake
                 validation; existing DLT setup.
Placeholders:    [Payment Status Feed Consumer], [Inbox / Processed
                 Event Repository], [Obligation Repository], [Payment
                 Request Creation Component] (via §6.8 trigger),
                 [Provider Response Parser] (shared evidence path).
Data/schema deps: P3 (inbox table, marker columns).
State-transition deps: §4.4 evidence rules (IN-07) sit on P6 CAS.
Tests required:  ordering guard (stale regression prevented; tie
                 branches); anchor-row completion impossibility;
                 monotonic marker writes (stale write dropped+counted);
                 inbox duplicate under concurrent redelivery (row-lock
                 test per §8); unmatched-event path; fallback guards
                 (single-active + amount + recency); amount-mismatch →
                 BLOCKED + SUBMITTED tightening; evidence-for-terminal
                 CRITICAL; ack-after-commit ordering.
Edge cases:      §6.7 timestamp ties (two genuine amendments share a
                 timestamp — never silent-drop the differing case);
                 feed event racing the executor's own response (both
                 converge on the same CAS); return-style event for
                 EXECUTED row (log + CRITICAL + ack, no state change).
Common mistakes: weakening evidence rules because the inbox exists;
                 dead-lettering transient infra errors (breaks
                 ordering); @RetryableTopic on money events; auto-commit
                 offsets; applying ui_step_status from a feed event.
Completion:      IN-01..IN-09 done; contract enforcement per §16.5
                 (schema registry / consumer-driven tests) wired for
                 the upstream schema and feed schema.
Verify locally:  real topic names/keys, consumer config, DLT wiring,
                 current validation gaps.
Go-live blocking: YES (duplicate prevention + evidence tests, Q).
```

### Phase P10 — Retry / recovery / MAYBE_SUBMITTED resolver

```text
Goal:            POST classifier per CA-1 (closed taxonomy, fail
                 closed); submission_state branches incl. collision
                 handling on divergence_expected; repost_permitted
                 implemented once, checked at both ends; retry scanner
                 with per-class policy, exhaustion, cutoff pre-checks,
                 freeze/breaker deadline suspension; resolver sweep
                 (submission-keyed scope, bounded prioritized batches,
                 per-row backoff, never-overlap, SUBMITTED damping);
                 §9.1 outcome application; §9.2 trust-age + downgrade +
                 SUBMITTED-branch park; §9.3 escalation (once per
                 episode, tiered); Hazelcast posting-freeze check with
                 fail-safe semantics.
Why here:        needs P6 legality, P7 money, P9 evidence machinery;
                 P8 PASS gates production reliance on re-POST.
Sections:        §7.0–7.4, §9.1–9.5, §16.1, §2.2 (anchors), §4.5.
Classification:  MVP normative (auto-downgrade reliance gated by P8).
Required concepts: existing retry scanner/jobs (D-08); existing
                 status-query usage (D-06); Hazelcast client (D-10).
Placeholders:    [Retry Resolver Job], [Status Query Resolver],
                 [Provider POST Client], [Provider Response Parser],
                 [Request Status Persistence Layer], [Metrics /
                 Alerting Layer].
Data/schema deps: P3 (next_query_at, anchors, divergent_payload_at).
State-transition deps: full P6 matrix; the ONE sanctioned backward
                 move (CONFIRM → POST) exists ONLY here (§9.2).
Tests required:  classifier fail-closed defaults; each §7.2 branch;
                 collision expected vs anomalous; repost_permitted
                 term-by-term + both-ends check; downgrade fires only
                 when permitted; SUBMITTED NOT_FOUND → park (reversible);
                 escalation once-per-episode; parked-MAYBE stability
                 (no park⇄un-park cycle); freeze fail-safe (absent /
                 unreachable / timeout = FROZEN; only FROZEN cached);
                 deadline suspension under freeze/breaker OPEN.
Edge cases:      DUPLICATE_REQUEST answering a downgrade re-POST
                 (hidden earlier attempt surfaced → MAYBE + query);
                 query-API outage → INDETERMINATE with escalation
                 clocks still running; post-outage MAYBE herd (sweep
                 shaping); downgraded row is both retry-scanner and
                 resolver actor (intended).
Common mistakes: blind re-POST as MAYBE "retry"; keying resolver
                 scope on stage/stage_state or history; keying any
                 rule on blocked_reason; measuring ages on
                 state_changed_at; releasing on a query answer (§9.4);
                 stacked in-process retries on the POST.
Completion:      RC-01..RC-10 done; config values wired to §16.6
                 inventory with load-order validation (OB-07 pairs).
Verify locally:  existing retry ownership (exactly one owner per
                 operation/error class — find and, if stacked retries
                 exist on the POST path, inventory them for removal).
Go-live blocking: YES (MAYBE recovery tests, cutoff config, Q items).
```

### Phase P11 — MVP apply-platform-verified-outcome audited stored procedure

```text
Goal:            Implement CA-9's spec: audited stored procedure,
                 restricted role; inputs = request_id, verified outcome
                 (EXECUTED|REJECTED), mandatory ticket/evidence
                 reference, TWO distinct authenticated approver
                 identities (dual control enforced BY the procedure);
                 sets the §10.3 evidence session flag legitimately;
                 applies through the SAME evidence-guarded CAS as feed
                 evidence; refuses CLAIMED rows, terminal rows, amount
                 mismatch; every use raises the §15 alert; log line
                 carries trigger_source = OPS_PLATFORM_VERIFIED +
                 ticket reference.
Why here:        §18 BLOCKING item 3 — the guaranteed MAYBE-row
                 terminal exit must exist AND be drilled before
                 go-live; needs P6/P7 CAS + trigger machinery.
Sections:        §9.3 (procedure design), §10.1, §10.3 (evidence
                 flag), §16.6-8 (CA-9), §20-8 (audit rules), §18-3.
Classification:  GATE (§18-3) + MVP normative.
Required concepts: DB session-context mechanism used by the release-
                 guard trigger (built in P3/S-06).
Placeholders:    [Operator Admin Procedure Area], [Stored Procedure /
                 Trigger Area], [Request Status Persistence Layer].
Data/schema deps: S-06 triggers + evidence flag mechanics.
State-transition deps: EXECUTED path (amount equality, +confirmed,
                 SUB=SUBMITTED); REJECTED path (provider_rejected
                 marker L9, −committed).
Tests required:  dual-control enforced (same approver twice =
                 refused); refusal conditions (CLAIMED, terminal,
                 amount mismatch); release guard passed LEGITIMATELY
                 (flag set by procedure; raw SQL without flag fails
                 loudly); money effects on both outcomes; alert + audit
                 log emitted.
Edge cases:      row becomes CLAIMED between verification and
                 execution (procedure must re-check inside its own
                 transaction); frozen-row convention on the outcome
                 write (maybe_since/escalated_at cleared).
Common mistakes: disabling the trigger instead of passing it
                 legitimately; enforcing dual control by convention
                 (runbook) instead of in the procedure; omitting the
                 ticket reference (the only restore-surviving record,
                 §20-8).
Completion:      OP-01..02 done; OP-03 drill EXECUTED and recorded.
Verify locally:  Oracle session-context facility available to the
                 app's trigger design; ops role model.
Go-live blocking: YES — §18 BLOCKING item 3.
```

### Phase P12 — Reconciliation / drift scanner / tripwires

```text
Goal:            Drift scanner recomputing I1/I2 from a consistent
                 snapshot (SCN/flashback), re-checking mismatches under
                 the obligation lock before paging; L9 totality check;
                 evidence-for-terminal CRITICAL tripwire; per-obligation
                 request-count sanity metric.
Why here:        verifies P7's invariants continuously; must be live
                 before rollout ramps traffic.
Sections:        §3 (drift), §10.3 (L9), §8 (anomaly), §15.
Classification:  MVP normative.
Required concepts: scheduled-job infrastructure (D-08/D-10).
Placeholders:    [Reconciliation / Drift Scanner], [Metrics /
                 Alerting Layer].
Data/schema deps: P3; P7 money choreography in place.
State-transition deps: none (read-only + page).
Tests required:  seeded-drift detection; read-skew non-page (snapshot
                 + re-check); L9 violation detection.
Edge cases:      routine read skew between counter update and request
                 insert in another uncommitted transaction MUST NOT
                 page; archival future-proofing noted (TL-14) but not
                 built.
Common mistakes: paging on first-read mismatch without the locked
                 re-check; running the scan without the active-row-
                 bounded indexes (plan degrades with terminal rows).
Completion:      OB-01..OB-02 done; drift page wired.
Verify locally:  flashback/SCN query availability and privileges.
Go-live blocking: YES (drift scanner + tripwires are Q items).
```

### Phase P13 — Observability, alerts, and runbook stubs

```text
Goal:            Implement the §15 alert/metric list with clock
                 discipline (episode anchors, never state_changed_at
                 for ages, never labels/blocked_reason as rule
                 inputs); alert rollup under root-cause conditions;
                 alerting practices (absence = bad, runbook links,
                 correlation_id propagation); config inventory
                 externalized with load-order validation; runbook
                 stubs per CA-8.
Why here:        D graph #8 — observability/runbooks before rollout.
Sections:        §15, §14, §16.1 (freeze-effective page), §16.2 (lag),
                 §16.6 (config + artifact 7).
Classification:  MVP normative + RUNBOOK.
Required concepts: existing metrics/alerting stack (D-10).
Placeholders:    [Metrics / Alerting Layer], [Reconciliation / Drift
                 Scanner] (rollup inputs).
Data/schema deps: anchors from P3/P6.
State-transition deps: none.
Tests required:  each alert fires on a seeded condition; rollup
                 groups under breaker-OPEN/freeze; config loader
                 REJECTS mis-ordered values (trust_age + cadence <
                 escalation < tier-2 < cutoff margin); dead-gauge
                 alerting.
Edge cases:      freeze is silent by design — the freeze-effective-
                 without-ticket page is the ONLY signal; duplicate-skip
                 counters spike healthily during replays.
Common mistakes: age alerts on state_changed_at; per-row alert storm
                 during an engine outage (rollup missing); alert
                 without runbook link.
Completion:      OB-03..OB-07 done; §15 list demonstrably covered.
Verify locally:  metrics naming conventions, alert routing, MDC use.
Go-live blocking: YES (dashboards/alerts/runbooks are Q items).
```

### Phase P14 — Migration, rollout, rollback, and go-live gates

```text
Goal:            Execute the Section M plan: expand/contract sequence,
                 backfill/dual-write for factored columns, shadow
                 validation (derived dimensions agree with legacy
                 status), safe enablement order (constraints VALIDATE →
                 read paths → write paths → scanners → resolver →
                 auto-downgrade), rollback constraints, Section Q
                 checklist execution.
Why here:        last — everything prior is its input.
Sections:        §16.5, §10.4 (label as bridge), §18 (gates), M, Q.
Classification:  MVP normative + GATE aggregation.
Required concepts: deployment pipeline; two-version dual-run window.
Placeholders:    [DB Migration Directory], all runtime components.
Data/schema deps: all migrations merged; backfill S-08 complete.
State-transition deps: full model live behind enablement order.
Tests required:  dual-run compatibility (old version + new schema);
                 shadow-comparison report clean; rollback rehearsal on
                 pre-money-write stages.
Edge cases:      what cannot be rolled back once money-affecting
                 writes begin under new machinery (Section M); claim
                 semantics version-compatible across one release
                 boundary (§16.5).
Common mistakes: enabling the auto-downgrade before P8 PASS; VALIDATE
                 constraints before backfill; dropping legacy columns
                 while the old version can still run.
Completion:      GO-01..GO-05 done; Section Q all PASS (or explicitly
                 accepted BLOCKED→waiver by the accountable owner —
                 §18 BLOCKING items are NOT waivable).
Verify locally:  release process, environment promotion path.
Go-live blocking: this phase IS the gate.
```

---

# F. Local source-code discovery workflow

Run on the work laptop BEFORE any implementation (Phase P1). Rules:
read-only; record every finding in the Section O template; when
evidence is ambiguous, mark UNCLEAR — never guess. Search terms below
are GENERIC (domain vocabulary + stack idioms), not guesses at real
names; adapt spelling to whatever the codebase actually uses and
record the real terms locally.

General status codes used below:

```text
IMPLEMENTED — behavior exists and matches the spec's rule
PARTIAL     — the concept exists but violates/omits a spec rule
MISSING     — no evidence of the concept
UNCLEAR     — evidence conflicts or cannot be confirmed; stop, record
BLOCKED     — cannot be assessed until a §18 item / mapping resolves
```

### F.1 Payment obligation creation / lookup

```text
Find:       the persistent entity representing "one payment scope" —
            what §2.1 calls payment_obligation — and where it is
            created/looked up when an upstream message arrives.
Search:     table/entity names containing obligation|payment_scope|
            payment_case; SQL/JPA with the scope-key fields
            (payment_type + debit_account + currency together);
            "SELECT ... FOR UPDATE" near those.
Confirm:    the found entity has (or maps to) required/committed/
            confirmed amount fields and a scope-key uniqueness
            constraint; message intake writes it.
Don't touch: the business rules that DECIDE payment details around it.
Tests:      persistence tests naming the entity; intake integration
            tests creating one row per scope.
IMPLEMENTED: unique scope key + amount fields + locked upsert exist.
PARTIAL:    entity exists but no scope-key uniqueness, or amounts have
            different semantics (record the actual semantics!).
MISSING:    payments processed without a per-scope aggregate row.
Mark UNCLEAR: if several candidate entities exist; if the scope key
            differs from §2.1 (that finding also feeds B-01 — the
            scope key is at stake there).
```

### F.2 Payment request creation

```text
Find:       where a payment attempt row (§2.2 payment_request) is
            inserted, and HOW MANY code paths insert it (§6.8 requires
            exactly ONE creation point).
Search:     INSERT into request-like tables; entity names containing
            payment_request|payment_instruction|payment_attempt;
            calls near shortfall/amount-comparison logic.
Confirm:    insert happens inside a transaction that also increments
            a committed/reserved counter (or will, after RG-01).
Don't touch: amount computation / payment construction business rules.
Tests:      creation tests asserting row + counter together.
IMPLEMENTED: single creation point under an obligation-level lock.
PARTIAL:    multiple creation call sites (inventory ALL — RG-06
            consolidates them); creation without reservation.
MISSING:    requests modeled only implicitly (e.g. fire-and-forget).
Mark UNCLEAR: if creation is spread across services in ways you
            cannot enumerate confidently.
```

### F.3 Existing payment enrichment

```text
Find:       the component resolving account numbers/party info before
            POST (spec: PaymentEnrichmentService is a documented
            service name).
Search:     enrich|lookup|resolve near party/account/agent terms;
            calls from the request pipeline between creation and POST.
Confirm:    it is read-only lookups (no amount mutation — §6.3).
Don't touch: ALL of it — enrichment business logic is preserved.
            Only its OUTCOME CLASSIFICATION (§7.3) may need wrapping.
Tests:      enrichment unit/integration tests (inventory for D-11).
IMPLEMENTED: distinct enrichment step with classifiable outcomes.
PARTIAL:    enrichment inline with posting (no separable outcome).
MISSING:    unlikely (existing system pays today) — if not found,
            mark UNCLEAR, do not conclude missing.
Mark BUSINESS_RULE_CHANGE_REQUIRED: if any §7.3 classification seems
            to require changing what enrichment computes.
```

### F.4 Provider POST call

```text
Find:       the HTTP/SDK call submitting a payment to the engine —
            per §5/TL-11 likely a platform-SDK validate-and-POST
            method.
Search:     http client beans / SDK artifacts in build files; terms
            post|submit|execute near payment; timeout configuration.
Confirm:    exactly one call site actually hits the wire (bulkheads/
            retries around it inventoried, §16.1: no stacked retries).
Don't touch: payment construction feeding it; SDK usage conventions.
Tests:      wiremock/stub-based POST tests; contract tests if any.
IMPLEMENTED: single call site with explicit timeout.
PARTIAL:    multiple call sites; in-process retry wrappers on the
            POST (must be inventoried for removal per §16.1);
            no timeout.
MISSING:    n/a (system pays today) — else UNCLEAR.
Mark UNCLEAR: if you cannot determine whether the SDK accepts a
            caller-supplied idempotency key (that is TL-11(b) — an
            external question, not a local guess).
```

### F.5 Provider response parsing

```text
Find:       where the POST response/exception is turned into a local
            status decision.
Search:     catch blocks around the POST call; response-code mapping
            switch/enum; terms duplicate|rejected|timeout near it.
Confirm:    you can list every branch it currently produces.
Don't touch: successful-payment data extraction used by business
            logic.
Tests:      parser unit tests enumerating response fixtures.
IMPLEMENTED: closed mapping of engine codes → local decisions.
PARTIAL:    default-retryable fallback (violates §7.2 fail-closed —
            RC-01 replaces the DEFAULT only); timeout treated as
            plain failure (must become MAYBE, §7.2).
MISSING:    status decided from HTTP status line alone (§7.2
            violation — record precisely).
Mark UNCLEAR: any response branch whose engine meaning you cannot
            determine (feeds CA-1's code table).
```

### F.6 Status query API usage

```text
Find:       any existing call to the engine's status-query API
            (§1 confirmed fact: it exists, keyed by idempotency
            key / UETR).
Search:     query|status|inquiry near the engine client; scheduled
            jobs calling it.
Confirm:    which key it queries by (idempotency key vs UETR vs
            provider reference).
Don't touch: n/a (read-only integration).
Tests:      stubbed query-response tests.
IMPLEMENTED: query client + some resolver loop exists.
PARTIAL:    client exists, used only manually/ad hoc.
MISSING:    no query usage — RC-05/RC-06 build it around the client
            or a new thin client in [Status Query Resolver].
Mark UNCLEAR: rate limits / lookback (TL-5, TL-13 — external).
```

### F.7 Payment status feed consumer

```text
Find:       the Kafka consumer for engine status notifications (spec
            names PaymentNotificationConsumerService as a documented
            service).
Search:     @KafkaListener / listener container config; topic config
            keyed by UETR terms; deserializer config.
Confirm:    it is the ONLY writer applying feed events to payment
            state.
Don't touch: topic names/ownership; other teams' consumers.
Tests:      embedded-kafka/testcontainers consumer tests.
IMPLEMENTED: listener with manual ack + DB transaction per event.
PARTIAL:    auto-commit offsets; ack before commit; no
            ErrorHandlingDeserializer (each = a §16.2 gap for IN-09,
            record precisely which).
MISSING:    feed not consumed (then §9 sweep is the only source —
            still record as MISSING, IN-05 builds it).
Mark UNCLEAR: whether feed events carry a stable event_id (TL-1 —
            external confirmation).
```

### F.8 Inbox / processed inbound event handling

```text
Find:       dedup persistence for consumed events (§2.3 table shape).
Search:     table names processed|inbox|consumed_event; PK on
            (source, event_id); insert-first patterns in consumers.
Confirm:    insert happens FIRST in the consumption transaction.
Don't touch: unrelated dedup caches.
Tests:      duplicate-delivery tests.
IMPLEMENTED: inbox insert-first + duplicate-key short-circuit.
PARTIAL:    dedup by in-memory cache only; insert after processing.
MISSING:    no dedup — S-04 creates the table, IN-05 wires it.
Mark UNCLEAR: existing purge policy ownership (§16.2 named owner).
```

### F.9 Retry scanner / resolver

```text
Find:       scheduled jobs re-driving failed/pending work.
Search:     @Scheduled|Quartz|ShedLock config; SKIP LOCKED queries;
            next_retry|attempt|due terms.
Confirm:    which states each job claims and what it does with them.
Don't touch: job scheduling infrastructure conventions.
Tests:      scanner tests with seeded due rows.
IMPLEMENTED: DB-driven scanner with lease/claim + backoff.
PARTIAL:    retries in-process only; no claim column (two scanners
            can double-process — feeds ST-09).
MISSING:    no retry machinery — RC-04 builds it.
Mark UNCLEAR: any job whose purpose you cannot determine (list it;
            do not modify).
```

### F.10 Status transition persistence

```text
Find:       how request status changes are written today.
Search:     UPDATE statements on the request table; setStatus|
            transition terms; a status enum type.
Confirm:    whether writes are conditional (WHERE carries expected
            state) or blind saves.
Don't touch: business meaning of existing statuses (map, don't
            reinterpret — ST-05 does the keyed-rule migration).
Tests:      transition tests, if any.
IMPLEMENTED: conditional UPDATE + row-count checks everywhere.
PARTIAL:    ORM save() dirty-checking (explicitly forbidden on these
            tables, §11 — inventory each site for ST-02).
MISSING:    status recomputed on read only.
Mark UNCLEAR: any writer you cannot attribute to a flow.
```

### F.11 CAS update patterns

```text
Find:       existing compare-and-set idioms (version column,
            conditional WHERE + row count).
Search:     version|@Version columns; "rowCount"/"updated == 1"
            branching; optimistic-lock exception handling.
Confirm:    row-count-0 branches exist and are HANDLED (not ignored).
Don't touch: CAS idioms in unrelated domains.
Tests:      concurrency tests using parallel updates.
IMPLEMENTED: CAS with row-count verdict on payment tables.
PARTIAL:    version column exists but ORM-managed (throws instead of
            branching).
MISSING:    no conditional updates — ST-02 introduces the discipline.
```

### F.12 Obligation / reservation creation

```text
Find:       where "money spoken for" is (or could be) counted at
            request creation (§3 INCREMENT rule).
Search:     committed|reserved|allocated amount fields; += updates on
            obligation-level amounts.
Confirm:    increment shares the transaction with request insert.
Don't touch: how amounts are COMPUTED (business rule).
Tests:      money invariant tests (rare — likely to be added, RG-01).
IMPLEMENTED: same-transaction increment exists.
PARTIAL:    counter exists with DIFFERENT semantics (e.g. "sent to
            engine") — record actual semantics; RG-01 must NOT
            silently reuse it (MUST_VERIFY_LOCALLY).
MISSING:    no reservation concept — RG-01 adds the choreography on
            the §2.1 column.
```

### F.13 Reservation release / confirmation

```text
Find:       decrements of the committed counter and increments of the
            confirmed counter.
Search:     -= updates; confirm|settle|complete near amount writes.
Confirm:    release is tied to terminal-negative transitions ONLY and
            confirmation to settlement evidence ONLY (§3).
Don't touch: settlement business interpretation.
Tests:      release/confirm tests.
IMPLEMENTED: row-count-gated, same-transaction movements.
PARTIAL:    releases on non-terminal failures, or movements at POST
            time (§3 violation — inventory each site for RG-02/03).
MISSING:    no movement logic — RG-02/03 build it.
```

### F.14 Duplicate detection

```text
Find:       every mechanism preventing double payment today.
Search:     unique constraints on business identity; existsBy checks
            before POST; duplicate-response handling from the engine.
Confirm:    what actually stops a duplicate: DB constraint, code
            check, engine dedup, or nothing.
Don't touch: constraints in place (extend, never drop).
Tests:      duplicate-attempt tests.
IMPLEMENTED: UNIQUE key + engine-dedup handling both present.
PARTIAL:    only code-level existsBy (race-prone); only engine-side.
MISSING:    none of the above — highest-priority gap; K-06 addresses.
Mark UNCLEAR: whether the engine deduplicates by caller key (that is
            §18-1/TL-11(c) — external proof, never a local guess).
```

### F.15 Idempotency key generation

```text
Find:       how the outbound submission identity is produced today.
Search:     idempotency|end_to_end|e2e id terms; UUID.randomUUID
            calls in the posting path; hash/digest utilities near
            request creation.
Confirm:    whether the key is DETERMINISTIC from business state or
            RANDOM (§5.1 — the DR keystone question).
Don't touch: any key already persisted on in-flight rows.
Tests:      key generation tests.
IMPLEMENTED: deterministic derivation + persisted-before-POST.
PARTIAL:    random key persisted before POST (crash-safe, restore-
            unsafe — K-02 replaces generation for NEW rows only);
            deterministic but unversioned/non-canonical.
MISSING:    key minted at POST time, unpersisted (violates §5 —
            K-04).
Mark UNCLEAR: if multiple identities are sent to the engine and you
            cannot tell which one the engine dedups on (TL-11(c)).
```

### F.16 UETR persistence

```text
Find:       where a UETR (or engine payment reference) lands after a
            POST response, and which responses write it.
Search:     uetr|UETR terms; response-field extraction near the POST
            client; unique columns on the request table.
Confirm:    which response classes currently write it (U-01 requires
            acceptance-class ONLY).
Don't touch: feed-matching reads of the column.
Tests:      response-persistence tests.
IMPLEMENTED: acceptance-only persistence, UNIQUE, NULL-tolerant.
PARTIAL:    persisted from ALL responses incl. rejects (§5 violation
            — U-01 fixes); merged with provider_reference (§2.2
            requires distinct fields).
MISSING:    UETR never captured — U-01 adds it.
Mark UNCLEAR: which SDK response field carries it (TL-11(a) —
            external).
```

### F.17 DB migrations

```text
Find:       the migration tool and its directory ([DB Migration
            Directory]).
Search:     flyway|liquibase in build files/config; db/migration
            resources; changelog XML/SQL.
Confirm:    migrations run automatically per environment; naming/
            numbering convention noted.
Don't touch: historical migration files (append-only discipline).
Tests:      migration-on-clean-schema CI step, if present.
IMPLEMENTED: Flyway/Liquibase from day one (§16.5).
PARTIAL:    manual DDL scripts — S-01 must first bring schema under
            the tool per §16.5 (expand/contract discipline).
MISSING:    no migration control — same as PARTIAL, more urgent.
```

### F.18 Oracle constraints / triggers / indexes

```text
Find:       current constraints on the payment tables; any triggers;
            existing function-based indexes.
Search:     user_constraints/user_triggers/user_indexes queries in a
            local DB session; DDL in the migration directory.
Confirm:    inventory: PKs, UNIQUEs, CHECKs, FKs, triggers, indexes —
            with exact expressions.
Don't touch: constraints other domains depend on.
Tests:      constraint-violation tests, if any.
IMPLEMENTED: (unlikely fully) CHECK-per-enum, I6-style function
            index, backstop triggers.
PARTIAL:    PKs/FKs only.
MISSING:    bare tables.
Mark UNCLEAR: privileges to create triggers/procedures in target
            environments (needed by S-06/OP-01 — ask DBA owner).
```

### F.19 Scheduled jobs

```text
Find:       every scheduled job touching payment tables.
Search:     @Scheduled|cron config; scheduler beans; job registries.
Confirm:    full inventory with schedule + purpose + tables touched.
Don't touch: jobs of other domains.
Tests:      job-level tests.
IMPLEMENTED/PARTIAL/MISSING: per job vs the target set: retry scanner
            (RC-04), resolver sweep (RC-05), escalation scanner
            (RC-08), drift scanner (OB-01), inbox purge (S-04),
            retention-chain check (OB-05), stuck-state alert (OB-04).
Mark UNCLEAR: jobs with unclear purpose — list, never modify.
```

### F.20 Operator actions / stored procedures

```text
Find:       existing DB procedures/scripts ops uses on payment data;
            restricted-role model.
Search:     PL/SQL packages/procedures in migrations or DBA repos;
            GRANT EXECUTE statements; runbook references.
Confirm:    who can write payment tables outside the app (feeds S-06
            trigger backstop design and OP-01 role model).
Don't touch: existing grants without DBA owner involvement.
Tests:      none likely.
IMPLEMENTED: audited procedure pattern + restricted role exists.
PARTIAL:    ad-hoc SQL by privileged users (exactly what the §10.3
            backstops exist to make fail loudly).
MISSING:    no ops write path (fine — MVP needs only OP-01).
Mark UNCLEAR: Oracle session-context facilities available for the
            evidence flag (§10.3) — confirm with DBA before S-06.
```

### F.21 Metrics / logs / alerts

```text
Find:       metrics library, log conventions, alerting stack, MDC/
            correlation-id propagation.
Search:     Micrometer/registry beans; structured-logging encoder
            config; alert rule repos/dashboards.
Confirm:    where §15's list would live; whether correlation ids
            propagate today (§14 requires them on every line).
Don't touch: org-wide logging/alerting conventions — extend them.
Tests:      log/metric assertion tests, if any.
IMPLEMENTED: structured logs + metric registry + alert pipeline.
PARTIAL:    logs unstructured; no correlation id; alerts ad hoc.
MISSING:    print-style logging only.
Mark UNCLEAR: account-number masking currently applied? (§16.3
            requires encoder-level masking — check the encoder, not
            call sites.)
```

### F.22 Reconciliation / drift jobs

```text
Find:       any job comparing money counters against row state.
Search:     reconcil|drift|integrity terms in jobs; aggregate-SUM
            queries over request amounts.
Confirm:    what it checks and what it does on mismatch.
Don't touch: platform-side reconciliation owned elsewhere.
Tests:      seeded-mismatch tests.
IMPLEMENTED: I1/I2-style recompute with paging.
PARTIAL:    logs mismatches without paging (must page — §3/§15) or
            pages on read skew (needs snapshot + locked re-check).
MISSING:    none — OB-01 builds it.
```

### F.23 Integration tests

```text
Find:       the integration test suite(s) and their infrastructure
            (testcontainers Oracle/Kafka? H2 shortcuts?).
Search:     integration-test source sets; container config; CI
            pipeline definitions.
Confirm:    tests can exercise: DB constraints (real Oracle needed
            for CHECK/trigger/function-index tests — H2 CANNOT
            validate S-05/S-06; if only H2 exists, record as a gap),
            Kafka consumption, and the POST client (stubbed).
Don't touch: existing green tests (they encode preserved behavior).
Tests:      n/a (this IS the test inventory).
IMPLEMENTED: real-Oracle + real-Kafka integration lanes.
PARTIAL:    H2-only (S-05/S-06/OP-02 need an Oracle lane — record;
            adding the lane becomes part of S-09 setup).
MISSING:    unit tests only.
```

### F.24 Existing payment business-rule tests

```text
Find:       tests encoding the PRESERVED business logic (account
            detection, debit-party lookup, enrichment, validation,
            payment construction).
Search:     test classes around the components found in F.3 and the
            payment-construction path.
Confirm:    they run green at baseline — this green bar is the
            "backwards compatibility" evidence for Section Q.
Don't touch: the tests themselves (they are the safety net; if one
            must change, that is BUSINESS_RULE_CHANGE_REQUIRED).
IMPLEMENTED: meaningful coverage of decision logic.
PARTIAL:    thin/happy-path only — record; do NOT expand them as part
            of this work unless a task card says so.
MISSING:    none — record prominently; every phase's "existing
            behavior preserved" claim then rests on manual validation
            and the D-12 report must say so.
```

---

# G. Placeholder component glossary

For each placeholder: meaning; probable local shape; how to identify;
design responsibilities (per spec §s); what NOT to change there;
likely tests; and change mode (READ-ONLY / MODIFIED / NEW).

### [Payment Request Creation Component]

```text
Meaning:      the single code path that decides "a new payment
              attempt is needed" and inserts the payment_request row.
Probable:     a service method in the orchestration service called
              from message processing; possibly several today.
Identify:     F.2. All insert sites of the request table.
Responsibilities: §6.8 standing re-evaluation (single creation point,
              trigger inventory, successor policy); §5.1 seq++ and
              key derivation; §3 +committed in the same transaction;
              creating_ordering stamping.
Do not change: WHAT gets paid or the payment attributes — only WHEN a
              request row is created and its identity/reservation
              choreography.
Tests:        creation + invariant tests (RG-01, RG-06).
Mode:         MODIFIED (consolidated if multiple sites exist).
```

### [Payment Enrichment Component]

```text
Meaning:      the existing enrichment step (party/account resolution)
              — a documented existing capability.
Probable:     PaymentEnrichmentService (documented service name) or a
              module within the pipeline.
Identify:     F.3.
Responsibilities: unchanged lookups; outcome CLASSIFICATION per §7.3;
              fresh assembly inputs per §7.0.
Do not change: any lookup/derivation logic; amounts are never touched
              here (§6.3).
Tests:        existing enrichment tests (preserved, F.24).
Mode:         READ-ONLY internally; a thin classification wrapper may
              be MODIFIED/NEW at its boundary.
```

### [Provider POST Client]

```text
Meaning:      the component that performs the engine POST (likely via
              the platform SDK's validate-and-POST method, §5).
Identify:     F.4.
Responsibilities: carries the caller-supplied idempotency key;
              explicit timeout (§16.1); called only under a posting
              claim with identity + hash already persisted (§11);
              breaker + freeze checks before the wire.
Do not change: SDK usage conventions; payment construction inputs.
Tests:        stubbed-response tests; CT suite (sandbox).
Mode:         MODIFIED (call-site discipline), not rewritten.
```

### [Provider Response Parser]

```text
Meaning:      the classification of POST responses/exceptions into
              §7.2's closed taxonomy.
Identify:     F.5.
Responsibilities: §7.2 branch set incl. MAYBE fail-closed defaults,
              DUPLICATE_REQUEST, collision (divergence_expected
              branch), UETR extraction rules (§5, acceptance-class
              only).
Do not change: extraction of business data used downstream on
              success.
Tests:        fixture-per-engine-code unit tests, driven by CA-1.
Mode:         MODIFIED (replaced defaults, added branches).
```

### [Request Status Persistence Layer]

```text
Meaning:      every write of payment_request state — the home of the
              CAS discipline.
Probable:     repository/DAO with hand-written conditional UPDATEs.
Identify:     F.10, F.11.
Responsibilities: four-dimension writes with full WHERE preconditions
              + row-count verdicts (§11); outcome-write normalization
              (§10.2); episode-anchor stamping (§2.2); §14 CAS log
              line.
Do not change: unrelated read queries; no ORM dirty-checking here.
Tests:        transition/legality tests (ST-02/03), concurrency tests.
Mode:         MODIFIED heavily / partially NEW.
```

### [Reservation Repository]

```text
Meaning:      writes of committed_amount/confirmed_amount on the
              obligation row.
Identify:     F.12, F.13.
Responsibilities: §3 choreography — movements only on row-count-1,
              same transaction; I3/I4 evaluation; overpay latch set.
Do not change: amount computation; required_amount writes (those
              belong to intake, §6.7).
Tests:        invariant tests I1–I6 (RG-xx).
Mode:         MODIFIED/NEW.
```

### [Obligation Repository]

```text
Meaning:      persistence of payment_obligation: locked reads
              (SELECT FOR UPDATE), scope-key upsert, ordering/marker
              fields, read-model fields.
Identify:     F.1.
Responsibilities: obligation lock as THE money mutex (§11); §6.7
              ordering-guarded writes; §6.9 monotonic marker writes;
              derivation output writes (§4).
Do not change: scope-key semantics (settled by §1 contract facts;
              changing them requires the PO, not a task).
Tests:        upsert race (ORA-00001 retry), ordering guard, marker
              monotonicity.
Mode:         MODIFIED.
```

### [Retry Resolver Job]

```text
Meaning:      the scheduled scanner claiming due RETRY_WAIT rows and
              re-driving stage work (the "retry scanner" of §7.4).
Identify:     F.9, F.19.
Responsibilities: SKIP LOCKED claims, DB-time due comparisons,
              per-item transactions (§11); per-class retry policy;
              repost_permitted before POST-bound work (§7.0);
              exhaustion/cutoff → BLOCKED; breaker/freeze gating +
              deadline suspension (§16.1).
Do not change: scheduling infrastructure conventions.
Tests:        seeded-row scanner tests; suspension tests.
Mode:         MODIFIED/NEW.
```

### [Status Query Resolver]

```text
Meaning:      the §9 sweep: queries the engine by idempotency key /
              UETR and applies §9.1 outcomes.
Identify:     F.6, F.9.
Responsibilities: §9.5 scope (submission-keyed ONLY); bounded
              prioritized batches + per-row backoff + never-overlap;
              §9.2 trust-age rule, downgrade, SUBMITTED-branch park;
              evidence-guarded application (§4.4/§9.4).
Do not change: the query client's transport conventions.
Tests:        outcome-application tests per §9.1 row; downgrade gate
              tests.
Mode:         NEW (or heavily MODIFIED if a sweep exists).
```

### [Payment Status Feed Consumer]

```text
Meaning:      the Kafka consumer of engine status notifications
              (documented service: PaymentNotificationConsumerService).
Identify:     F.7.
Responsibilities: §8 transaction order (inbox first, ack after
              commit); UETR-primary matching; fail-closed
              provider_reference fallback; amount equality; marker
              totality; anomaly alerts; §16.2 config.
Do not change: topic contracts; other consumers in the group.
Tests:        redelivery, unmatched, mismatch, race tests.
Mode:         MODIFIED.
```

### [Inbox / Processed Event Repository]

```text
Meaning:      persistence for processed_inbound_event (§2.3).
Identify:     F.8.
Responsibilities: insert-first dedup; purge policy hooks.
Do not change: n/a (small, self-contained).
Tests:        duplicate-key and concurrent-redelivery tests.
Mode:         NEW (or MODIFIED to match §2.3).
```

### [DB Migration Directory]

```text
Meaning:      where Flyway/Liquibase migrations live.
Identify:     F.17.
Responsibilities: the CA-4 DDL set as expand/contract migrations
              (§16.5).
Do not change: historical migrations (append-only).
Tests:        migration apply tests (S-09).
Mode:         NEW files appended.
```

### [Stored Procedure / Trigger Area]

```text
Meaning:      where PL/SQL objects (triggers, procedures) are
              defined/migrated.
Identify:     F.18, F.20.
Responsibilities: L1 freeze trigger; release-guard trigger + evidence
              session flag mechanics (§10.3).
Do not change: unrelated DB objects; grants without DBA owner.
Tests:        trigger backstop tests on real Oracle (S-06).
Mode:         NEW.
```

### [Operator Admin Procedure Area]

```text
Meaning:      restricted-role home of the apply-platform-verified-
              outcome procedure (§9.3, CA-9).
Identify:     F.20.
Responsibilities: OP-01's procedure: dual control, ticket reference,
              evidence flag, refusal conditions, audit + alert.
Do not change: existing ops grants without DBA owner.
Tests:        OP-02 suite + OP-03 drill.
Mode:         NEW.
```

### [Metrics / Alerting Layer]

```text
Meaning:      metrics registry + structured logging + alert rules.
Identify:     F.21.
Responsibilities: §15 list with clock discipline; §14 CAS log line;
              alert rollup; masking in the encoder (§16.3).
Do not change: org conventions — extend.
Tests:        alert-fires-on-seeded-condition tests (OB-xx).
Mode:         MODIFIED/NEW rules.
```

### [Reconciliation / Drift Scanner]

```text
Meaning:      the §3 drift job (I1/I2 recompute + L9 verify).
Identify:     F.22.
Responsibilities: consistent snapshot read; locked re-check before
              paging; §15 drift page.
Do not change: platform-side reconciliation.
Tests:        seeded drift + read-skew non-page tests.
Mode:         NEW.
```

### [Integration Test Suite]

```text
Meaning:      the local integration-test lane (real Oracle + Kafka).
Identify:     F.23.
Responsibilities: hosts the Section J matrix's integration/
              concurrency/migration/recovery tests.
Do not change: existing green tests (preserved behavior).
Mode:         MODIFIED (new tests added).
```

### [Contract Test Suite]

```text
Meaning:      executable tests against the engine SANDBOX (§18-1)
              plus build-time contract enforcement for the three
              external contracts (§16.5).
Identify:     F.23 (lane), B-02 (sandbox access).
Responsibilities: CT-01..07; schema-registry/consumer-driven checks
              for upstream schema, engine feed, card read contract.
Do not change: sandbox environments beyond test data.
Mode:         NEW.
```

---

# H. Small executable task cards

Rules of use (binding for the local agent): one card at a time; read
ONLY the card's listed §s and mapped files; every card ends at its
Stop condition with a Section R report. If a required mapping is
UNCLEAR/MISSING, the card is locally BLOCKED — report, don't guess.
If a card proves too big for the local context window, split it
locally into sub-tasks (suffix .1, .2 …) and report the split.
"§" references are to `requirment-v4.md`.

## H-Phase 1 — Discovery (P1)

### D-01 — Set up the local mapping document

- **Task ID:** D-01
- **Title:** Create the local placeholder mapping document from the Section O template
- **Classification:** local discovery task only
- **Purpose:** one place, kept LOCAL, where every placeholder → real component mapping lives.
- **Prerequisites:** none.
- **Requirement sections / concepts to read:** Sections G and O of this playbook.
- **Placeholder components involved:** all (as rows).
- **Local placeholder mappings required before starting:** none.
- **Local code areas to discover:** none yet.
- **How to locate:** n/a.
- **Implementation instructions:** copy Section O's table into a local file (e.g. a local notes file inside the repo's ignored area); one row per placeholder from Section G; add rows for the three core tables and the four documented services.
- **Do not change:** any source file.
- **Tests to add:** none.
- **Edge cases:** none.
- **Manual validation:** table exists, all Section G placeholders present as rows, Status column = UNMAPPED.
- **Expected outcome:** empty mapping table ready to fill.
- **Failure signs:** mapping file placed where it could be committed/pushed to a shared location visible externally — it must stay local.
- **Common mistakes:** starting discovery before the table exists (findings get lost).
- **Completion criteria:** file exists with all rows.
- **Stop condition:** table created; stop.
- **Next task:** D-02.

### D-02 — Discover schema and migration state

- **Task ID:** D-02
- **Title:** Inventory current DB schema, migration tooling, constraints, triggers, indexes
- **Classification:** local discovery task only
- **Purpose:** establish the real starting shape for Phase P3.
- **Prerequisites:** D-01.
- **Requirement sections / concepts to read:** §2.1, §2.2, §2.3, §10.3, §16.5; playbook F.17, F.18.
- **Placeholder components involved:** [DB Migration Directory], [Stored Procedure / Trigger Area].
- **Local placeholder mappings required before starting:** none (this task creates them).
- **Local code areas to discover:** migration directory; DDL for the obligation/request/inbox-equivalent tables; constraint/trigger/index inventory.
- **How to locate:** per F.17/F.18 search patterns.
- **Implementation instructions:** record in the mapping doc: migration tool + version + directory; per-table column list vs the §2.1/§2.2/§2.3 target (three columns: exists-as-specified / exists-different / missing); constraint + trigger + index inventory with exact expressions; DB privileges available for creating triggers/procedures.
- **Do not change:** anything.
- **Tests to add:** none.
- **Edge cases:** column names that LOOK like spec fields but hold different semantics — record the observed semantics, don't assume.
- **Manual validation:** every §2.x field appears in the gap table with a status.
- **Expected outcome:** complete schema gap inventory.
- **Failure signs:** gap table with "probably" entries — replace with UNCLEAR + what blocked confirmation.
- **Common mistakes:** reading application entities instead of actual DDL (ORM annotations can lie about the DB).
- **Completion criteria:** mapping rows for both placeholders filled; gap inventory attached.
- **Stop condition:** inventory recorded; stop.
- **Next task:** D-03.

### D-03 — Discover obligation handling and money fields

- **Task ID:** D-03
- **Title:** Map obligation creation/lookup, locking, and current money-field semantics
- **Classification:** local discovery task only
- **Purpose:** find [Obligation Repository]/[Reservation Repository] and learn what today's amount fields MEAN.
- **Prerequisites:** D-01.
- **Requirement sections / concepts to read:** §2.1, §3, §11 (lock model); playbook F.1, F.12, F.13.
- **Placeholder components involved:** [Obligation Repository], [Reservation Repository].
- **Local placeholder mappings required before starting:** none.
- **Local code areas to discover:** scope entity, its upsert path, SELECT FOR UPDATE usage, amount-field writers.
- **How to locate:** per F.1/F.12/F.13.
- **Implementation instructions:** record: scope-key fields actually in use (compare against §2.1 — any difference is ALSO evidence for B-01); every writer of every amount field with its transaction boundary; whether any committed/reserved-like counter exists and its OBSERVED semantics.
- **Do not change:** anything.
- **Tests to add:** none.
- **Edge cases:** counters updated by DB jobs or other services (writers outside this repo) — record as UNCLEAR with the observed evidence.
- **Manual validation:** every amount-field writer listed with file:line-style local references (locally only).
- **Expected outcome:** mapping rows filled; money-semantics memo.
- **Failure signs:** "committed_amount already exists" without semantics evidence.
- **Common mistakes:** assuming a counter named like the spec has spec semantics (F.12 PARTIAL trap).
- **Completion criteria:** both placeholder rows Confirmed or UNCLEAR with reasons.
- **Stop condition:** recorded; stop.
- **Next task:** D-04.

### D-04 — Discover request persistence and status model

- **Task ID:** D-04
- **Title:** Map request-table persistence, current status enum, transition writers, CAS idioms
- **Classification:** local discovery task only
- **Purpose:** baseline for P6 (factored model) and the ST-05 legacy-status rule inventory.
- **Prerequisites:** D-01.
- **Requirement sections / concepts to read:** §2.2, §10.4, §11; playbook F.2, F.10, F.11.
- **Placeholder components involved:** [Request Status Persistence Layer], [Payment Request Creation Component].
- **Local placeholder mappings required before starting:** none.
- **Local code areas to discover:** request entity + repository; the status enum type; every status writer; every status READER that branches on it (rule sites).
- **How to locate:** per F.2/F.10/F.11; for rule sites, search usages of the status enum type.
- **Implementation instructions:** record: current status values and their meanings as observed; writer list (conditional vs blind save); reader/rule-site list — THIS LIST IS THE ST-05 INPUT, completeness matters; version/CAS column presence.
- **Do not change:** anything.
- **Tests to add:** none.
- **Edge cases:** status strings used in SQL/reports/dashboards outside Java — search migrations and any query files too; note external consumers you can see locally.
- **Manual validation:** rule-site list reviewed once more against enum usages (grep count matches list length).
- **Expected outcome:** complete rule-site inventory + writer inventory.
- **Failure signs:** rule-site list "top 10" instead of complete — ST-05 depends on completeness.
- **Common mistakes:** missing readers in scheduled jobs and monitoring queries.
- **Completion criteria:** both placeholders mapped; inventories attached.
- **Stop condition:** recorded; stop.
- **Next task:** D-05.

### D-05 — Discover provider POST path

- **Task ID:** D-05
- **Title:** Map the engine POST call, SDK usage, response parsing, timeout/retry wrappers
- **Classification:** local discovery task only
- **Purpose:** baseline for P4 (write-ahead identity), P5 (UETR), P10 (classifier).
- **Prerequisites:** D-01.
- **Requirement sections / concepts to read:** §5 (identity chain), §7.0–7.2, §16.1 (retry ownership); playbook F.4, F.5, F.15, F.16.
- **Placeholder components involved:** [Provider POST Client], [Provider Response Parser].
- **Local placeholder mappings required before starting:** none.
- **Local code areas to discover:** POST call site(s); SDK method + response type; every catch/branch after the call; identity fields sent; any retry annotations/wrappers around the call.
- **How to locate:** per F.4/F.5/F.15/F.16.
- **Implementation instructions:** record: exact call-site count; response branches currently produced; which identity fields go to the engine and where each is generated; UETR/reference extraction sites; all retry layers around the POST (each is a §16.1 violation candidate — inventory only).
- **Do not change:** anything.
- **Tests to add:** none.
- **Edge cases:** async/callback-style SDK usage — record threading model (matters for §11 claim ownership).
- **Manual validation:** a colleague-level re-read of the branch inventory against the actual catch blocks.
- **Expected outcome:** POST-path memo with branch inventory.
- **Failure signs:** any response branch labeled "unknown meaning" silently — must be listed as UNCLEAR feeding CA-1.
- **Common mistakes:** missing exception paths (timeouts thrown vs returned).
- **Completion criteria:** both placeholders mapped; branch + retry-layer inventories attached.
- **Stop condition:** recorded; stop.
- **Next task:** D-06.

### D-06 — Discover status-query usage

- **Task ID:** D-06
- **Title:** Map any existing engine status-query client and its callers
- **Classification:** local discovery task only
- **Purpose:** baseline for [Status Query Resolver] (P10).
- **Prerequisites:** D-01.
- **Requirement sections / concepts to read:** §9 intro, §9.1; playbook F.6.
- **Placeholder components involved:** [Status Query Resolver].
- **Local placeholder mappings required before starting:** none.
- **Local code areas to discover:** query client; scheduled callers; query key used.
- **How to locate:** per F.6.
- **Implementation instructions:** record client existence, query key(s), response statuses observed in code, callers.
- **Do not change:** anything.
- **Tests to add:** none.
- **Edge cases:** query API used only in ops scripts — record those too.
- **Manual validation:** n/a.
- **Expected outcome:** mapping row filled (possibly MISSING — valid finding).
- **Failure signs:** conflating the status-query API with the status FEED (§1 lists them as different channels).
- **Common mistakes:** same.
- **Completion criteria:** row Confirmed/MISSING/UNCLEAR.
- **Stop condition:** recorded; stop.
- **Next task:** D-07.

### D-07 — Discover both Kafka consumers

- **Task ID:** D-07
- **Title:** Map upstream trade-message consumer and payment status feed consumer + their config
- **Classification:** local discovery task only
- **Purpose:** baseline for P9 (IN-xx) and §16.2 hardening gaps.
- **Prerequisites:** D-01.
- **Requirement sections / concepts to read:** §6.0, §8, §16.2; playbook F.7.
- **Placeholder components involved:** [Payment Status Feed Consumer], (upstream consumer maps under [Obligation Repository] flow).
- **Local placeholder mappings required before starting:** none.
- **Local code areas to discover:** listener classes, container factory config, ack mode, offset reset, deserializers, DLT wiring, topic keys, concurrency.
- **How to locate:** per F.7; Spring Kafka config classes/properties.
- **Implementation instructions:** record per consumer: ack mode, auto-commit, offset reset, error handler, DLT, key field, concurrency, max.poll settings — as a checklist against §16.2 line by line (each line = compliant / gap).
- **Do not change:** anything.
- **Tests to add:** none.
- **Edge cases:** shared consumer-group or shared container factory with other domains — record.
- **Manual validation:** checklist complete for both consumers.
- **Expected outcome:** §16.2 gap checklist.
- **Failure signs:** properties read from a deployment repo you didn't check — record UNCLEAR if runtime config is elsewhere.
- **Common mistakes:** reading only annotated defaults and missing yaml overrides.
- **Completion criteria:** both consumers mapped with gap checklist.
- **Stop condition:** recorded; stop.
- **Next task:** D-08.

### D-08 — Discover scheduled jobs and retry machinery

- **Task ID:** D-08
- **Title:** Inventory every scheduled job touching payment data; identify retry ownership
- **Classification:** local discovery task only
- **Purpose:** baseline for [Retry Resolver Job], RC-04/05/08, OB-01; §16.1 single-retry-owner rule.
- **Prerequisites:** D-01.
- **Requirement sections / concepts to read:** §7.4, §9.5, §16.1 (retry ownership, scanner rules); playbook F.9, F.19.
- **Placeholder components involved:** [Retry Resolver Job], [Reconciliation / Drift Scanner], [Status Query Resolver].
- **Local placeholder mappings required before starting:** none.
- **Local code areas to discover:** all @Scheduled/cron entries; claim/lease columns; SKIP LOCKED usage; app-time vs DB-time comparisons.
- **How to locate:** per F.9/F.19.
- **Implementation instructions:** job inventory table: name-as-found (locally), schedule, tables touched, states claimed, retry semantics, time source. Flag every job that could claim the same rows as another (double-processing risk) and every app-time due comparison (§11 requires DB time).
- **Do not change:** anything.
- **Tests to add:** none.
- **Edge cases:** jobs enabled per-environment only; leader-election wrappers.
- **Manual validation:** inventory covers every scheduling annotation found by search.
- **Expected outcome:** job inventory + retry-ownership map.
- **Failure signs:** unknown-purpose jobs skipped instead of listed.
- **Common mistakes:** missing non-Spring schedulers (DB jobs, external cron) — check migrations for DBMS_SCHEDULER too.
- **Completion criteria:** placeholders mapped; inventory attached.
- **Stop condition:** recorded; stop.
- **Next task:** D-09.

### D-09 — Discover identity and duplicate protection

- **Task ID:** D-09
- **Title:** Map idempotency-key generation, persistence timing, and every duplicate-prevention mechanism
- **Classification:** local discovery task only
- **Purpose:** the money-critical baseline for P4; feeds K-02/K-04/K-06.
- **Prerequisites:** D-01, D-05.
- **Requirement sections / concepts to read:** §5, §5.1; playbook F.14, F.15.
- **Placeholder components involved:** [Payment Request Creation Component], [Provider POST Client].
- **Local placeholder mappings required before starting:** D-05's POST-path memo.
- **Local code areas to discover:** key generation site(s); when the key is persisted relative to the wire call; UNIQUE constraints on identity; existsBy-style checks.
- **How to locate:** per F.14/F.15.
- **Implementation instructions:** record: deterministic vs random; generation inputs; persistence timing (before/after POST — trace the actual transaction boundaries); every duplicate-prevention layer and what it actually prevents.
- **Do not change:** anything.
- **Tests to add:** none.
- **Edge cases:** different identities for different payment types; identity reuse on retries (or not — §5 requires same-key retries; record current behavior precisely).
- **Manual validation:** trace one real code path creation→POST and confirm the recorded timing.
- **Expected outcome:** identity memo: generation, timing, protection layers.
- **Failure signs:** "persisted before POST" claimed without tracing the commit boundary.
- **Common mistakes:** confusing the UETR/engine reference with the caller-supplied key (§5 identity chain keeps them distinct).
- **Completion criteria:** memo complete; F.15 status assigned.
- **Stop condition:** recorded; stop.
- **Next task:** D-10.

### D-10 — Discover Hazelcast, metrics, alerting, ops procedures

- **Task ID:** D-10
- **Title:** Map Hazelcast usage/toggles, metrics/log/alert stack, existing ops DB procedures
- **Classification:** local discovery task only
- **Purpose:** baseline for RC-09 (freeze), P13 (observability), P11 (procedure area).
- **Prerequisites:** D-01.
- **Requirement sections / concepts to read:** §16.1 (freeze), §14, §15 (skim list), §20 (interim model); playbook F.20, F.21.
- **Placeholder components involved:** [Metrics / Alerting Layer], [Operator Admin Procedure Area], [Stored Procedure / Trigger Area].
- **Local placeholder mappings required before starting:** none.
- **Local code areas to discover:** Hazelcast client config + existing toggles/maps; metrics registry + conventions; logging encoder (masking?); MDC/correlation propagation; alert rule location; existing PL/SQL ops objects + role grants.
- **How to locate:** per F.20/F.21; Hazelcast config beans/properties.
- **Implementation instructions:** record each; specifically: does a posting-freeze-like toggle already exist (§16.1 says the toggle-with-role-control exists operationally — find its technical shape); is account masking in the encoder or call sites; correlation id propagation status.
- **Do not change:** anything.
- **Tests to add:** none.
- **Edge cases:** Hazelcast used embedded vs client-server — record topology (affects freeze-check timeout design).
- **Manual validation:** n/a.
- **Expected outcome:** three mapping rows + infra memo.
- **Failure signs:** assuming the freeze toggle exists in code because §16.1 says role control exists — verify the technical artifact, else UNCLEAR.
- **Common mistakes:** missing alert rules living in a separate ops repo — record UNCLEAR if not visible locally.
- **Completion criteria:** rows filled.
- **Stop condition:** recorded; stop.
- **Next task:** D-11.

### D-11 — Inventory existing tests

- **Task ID:** D-11
- **Title:** Inventory test suites: business-rule tests, integration lanes, infrastructure capability
- **Classification:** local discovery task only
- **Purpose:** establish the preserved-behavior safety net and the test-infrastructure gaps (real Oracle lane needed by S-05/S-06/OP-02).
- **Prerequisites:** D-01.
- **Requirement sections / concepts to read:** playbook F.23, F.24; Section J intro.
- **Placeholder components involved:** [Integration Test Suite], [Contract Test Suite].
- **Local placeholder mappings required before starting:** none.
- **Local code areas to discover:** test source sets, container config, CI definitions, business-rule test coverage.
- **How to locate:** per F.23/F.24.
- **Implementation instructions:** record: suites + how they run; DB used by integration tests (real Oracle? — if H2-only, record the S-09 lane gap); Kafka test infra; baseline green-bar snapshot (run the full suite once, record results — this is the Section Q backwards-compatibility baseline).
- **Do not change:** any test.
- **Tests to add:** none.
- **Edge cases:** flaky tests at baseline — record them BY NAME now, so later failures can be attributed.
- **Manual validation:** baseline run results stored locally.
- **Expected outcome:** test inventory + baseline green bar.
- **Failure signs:** baseline not runnable — that itself is a critical finding for D-12.
- **Common mistakes:** skipping the baseline run.
- **Completion criteria:** inventory + baseline recorded.
- **Stop condition:** recorded; stop.
- **Next task:** D-12.

### D-12 — Discovery report and gap classification

- **Task ID:** D-12
- **Title:** Consolidate discovery into the implemented/partial/missing report; update the BLOCKED list
- **Classification:** local discovery task only
- **Purpose:** the single artifact Phase P2+ planning reads; converts findings into per-task readiness.
- **Prerequisites:** D-02 … D-11 all complete.
- **Requirement sections / concepts to read:** playbook Section F status codes; Playbook Index BLOCKED list.
- **Placeholder components involved:** all.
- **Local placeholder mappings required before starting:** all D-xx rows filled.
- **Local code areas to discover:** none (consolidation).
- **How to locate:** n/a.
- **Implementation instructions:** for each F.1–F.24 concept assign IMPLEMENTED/PARTIAL/MISSING/UNCLEAR with one-line evidence; list every UNCLEAR with what would resolve it; mark which task cards are locally BLOCKED (mapping missing) beyond the §18-BLOCKED list; deliver the report to the human owner.
- **Do not change:** any source file.
- **Tests to add:** none.
- **Edge cases:** conflicts between findings (e.g. two duplicate-prevention layers with different keys) — surface, don't resolve.
- **Manual validation:** human owner reviews the report before P3 starts.
- **Expected outcome:** discovery report; readiness per task phase.
- **Failure signs:** report with unresolved "probably"s.
- **Common mistakes:** turning findings into design changes — findings are inputs, the spec is the design.
- **Completion criteria:** report delivered; mapping table complete.
- **Stop condition:** report delivered; STOP — implementation starts only after human review.
- **Next task:** B-01 (human-driven) / S-01 after P2 gates.

## H-Phase 2 — Blocking gates and contract artifacts (P2)

### B-01 — Resolve §18 BLOCKING item 0 (payments-per-trade / scope key)

- **Task ID:** B-01
- **Title:** Drive the snapshot-contract residue: written confirmation, intake validation, PO-9, TL-16
- **Classification:** §18 BLOCKING go-live gate
- **Purpose:** the §1 contract facts record the model: one trade carries MULTIPLE payments; each message is a FULL-TRADE SNAPSHOT (newer overwrites older); (payment_type + debit_account + currency) is unique WITHIN a snapshot, and an equal tuple ACROSS snapshots means the same payment. Consequence: the §2.1 scope key needs NO discriminator, §5.1 identity stands unchanged, and the schema/identity freeze (S-02/S-03/S-05/K-02/K-03/CA-4/CA-5) is NOT gated here. §12 lookup: business_id returns ALL of the trade's obligations (multiple results = normal). This task drives the model's four open edges to closure.
- **Prerequisites:** none (human task).
- **Requirement sections / concepts to read:** §1 contract facts (trade-payment cardinality), §6.0, §6.1, §12, §18 BLOCKING item 0.
- **Implementation instructions (residue):** (1) obtain the WRITTEN upstream confirmation of the snapshot schema + within-snapshot uniqueness (upstream ask 5) — the cross-snapshot identity half is unverifiable at runtime and rests on this document; (2) ensure IN-02 implements the §6.0 within-snapshot uniqueness intake validation (whole-snapshot validation failure, fail closed); (3) drive PO-9 (absence semantics — a BA-2 amendment, PO-only) and TL-16 (snapshot ordering-watermark rule) to answers BEFORE the IN-02 consumer freeze — both shape §6.1's fan-out; (4) TL-2's read contract now must also answer step granularity (per-payment vs per-trade rollup, §12).
- **Do not change:** code.
- **Tests to add:** intake test — snapshot with two blocks sharing a tuple → whole-snapshot validation failure + anchors (§6.0/§6.6); fan-out convergence test — kill consumer mid-fan-out, redeliver, assert per-obligation ordering guard converges (§6.1).
- **Edge cases:** "usually unique" is NOT an answer for ask 5 — the identity contract needs a guarantee; PO-9 unanswered means absence = NO-OP (BA-2 stands), which knowingly leaves a genuinely-removed payment paying.
- **Manual validation:** written confirmation attributed and filed; PO-9/TL-16 answers recorded in §18.
- **Expected outcome:** B-01 fully closed; IN-02 consumer freeze unblocked.
- **Failure signs:** IN-02 frozen while PO-9/TL-16 are open; treating a verbal model confirmation as the written contract.
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
- **Requirement sections / concepts to read:** §18 BLOCKING item 3, §9.3 (procedure), TL-10, TL-5.
- **Placeholder components involved:** [Operator Admin Procedure Area] (default path).
- **Local placeholder mappings required before starting:** none.
- **Local code areas to discover:** none.
- **How to locate:** n/a.
- **Implementation instructions:** default decision per spec: BUILD the procedure (OP-01..03, CA-9). Only if TL-10 (platform formal reject) AND TL-5 (lookback ≥ max row lifetime incl. ops-queue SLA) are BOTH answered affirmatively in writing may the procedure be de-scoped — record whichever path, and note §20's PO decision already REQUIRES the procedure at MVP, so de-scoping needs explicit PO re-confirmation.
- **Do not change:** code.
- **Tests to add:** none.
- **Edge cases:** partial alternative (TL-10 yes, TL-5 no) → procedure stays required.
- **Manual validation:** decision recorded with approver.
- **Expected outcome:** OP-xx confirmed in scope (expected default).
- **Failure signs:** de-scoping the procedure on optimistic unwritten answers.
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
- **Do not change:** the three-table model — any "needs another table" is SPEC_CONFLICT.
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
- **Implementation instructions:** adopt Section J's matrix (T-01..T-32) as the seed; add the spec-named entries above; assign each entry an owner-type and the phase whose task implements it; keep IDs stable; version the catalog.
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
- **Purpose:** §15 requires every alert definition to carry a runbook link; §16.6-7 also names the unqueryable-aged-MAYBE runbook (platform-side lookup → TL-10 rejection or the apply-platform-verified-outcome procedure). The §5.2 restore runbook is POST-MVP and only stubbed as "major incident — manual engine-side reconciliation" per §5.2's MVP scope.
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

### CA-9 — Author the apply-platform-verified-outcome procedure spec

- **Task ID:** CA-9
- **Title:** apply-platform-verified-outcome stored procedure spec + ops drill script (§16.6 artifact 8)
- **Classification:** §16.6 companion artifact + §18 BLOCKING item 3 input
- **Purpose:** OP-01 implements exactly this spec: signature, dual-control enforcement, evidence-flag mechanics, refusal conditions, audit fields, drill script.
- **Prerequisites:** B-04 (path confirmed); CA-4 (trigger/evidence-flag mechanics defined there must match).
- **Requirement sections / concepts to read:** §9.3 (full procedure design), §10.1, §10.3 (evidence flag + backstops), §20-8 (audit/ticket rule), §16.6 artifact 8, §18-3.
- **Placeholder components involved:** [Operator Admin Procedure Area].
- **Local placeholder mappings required before starting:** none for authoring.
- **Local code areas to discover:** none.
- **How to locate:** n/a.
- **Implementation instructions:** specify: inputs (request_id, verified outcome EXECUTED|REJECTED, mandatory ticket/evidence reference, two distinct authenticated approver identities); dual control enforced IN the procedure; sets the evidence session flag; applies via the SAME evidence-guarded CAS as feed evidence; EXECUTED → outcome=EXECUTED, SUB=SUBMITTED, amount equality enforced, +confirmed; REJECTED → outcome=REJECTED, provider_rejected marker (L9), −committed; refuses CLAIMED and terminal rows and amount mismatch; every use → §15 alert; log line carries trigger_source=OPS_PLATFORM_VERIFIED + ticket ref; restricted role; drill script = end-to-end rehearsal steps on a seeded row in a non-prod environment.
- **Do not change:** §9.4's single-sanctioned-exception framing — the procedure is the ONLY manual path.
- **Tests to add:** none here (OP-02).
- **Edge cases:** platform amount differs from request amount → NOT applicable here; that is the §8 AMOUNT_MISMATCH defect path (spec is explicit).
- **Manual validation:** DBA + ops-owner review; approver-identity mechanism confirmed workable in the real role model (from D-10 — else UNCLEAR flagged).
- **Expected outcome:** implementable spec + drill script.
- **Failure signs:** dual control specified as runbook convention instead of procedure-enforced.
- **Common mistakes:** allowing outcome values beyond EXECUTED/REJECTED.
- **Completion criteria:** spec published.
- **Stop condition:** published; OP-01 unblocked.
- **Next task:** S-01 (Phase P3).

## H-Phase 3 — Schema and migration (P3)

### S-01 — Schema gap plan (freeze)

- **Task ID:** S-01
- **Title:** Freeze the migration plan: ordered migration list from the D-02 gap inventory + CA-4 spec
- **Classification:** MVP normative implementation
- **Purpose:** one ordered, expand/contract-safe migration sequence before any DDL is written.
- **Prerequisites:** B-01 answered; CA-4 published; D-02 done.
- **Requirement sections / concepts to read:** §16.5 (expand/contract), CA-4, D-02 gap inventory (local).
- **Placeholder components involved:** [DB Migration Directory].
- **Local placeholder mappings required before starting:** [DB Migration Directory] Confirmed.
- **Local code areas to discover:** migration numbering/naming convention.
- **How to locate:** F.17 findings.
- **Implementation instructions:** write the ordered migration list (numbers reserved, one concern per migration): obligation columns → request columns → inbox table → UNIQUEs/I6 → CHECKs (NOVALIDATE) → triggers → indexes → backfill → VALIDATE. Each entry: DDL summary, rollback note, dual-run compatibility note (old app version must still run — §16.5).
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
- **Purpose:** land the §2.1 fields: amounts, overpay_blocked, next_request_seq, upstream_ordering, correlation_id, ordering-tagged markers (validation_failed_at/_ordering, provider_rejected_at/code/_ordering), provider_reject_count, validation_reject_count, validation_failed_first_at, reopened_at, read-model fields (ui_step_status, active_exception_*, ops_annotation, ui_process_instance_id, ui_step_instance_id) — all additive, nullable-with-default first.
- **Prerequisites:** S-01; B-01 (scope key final).
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
- **Implementation instructions:** if MISSING: create the table exactly per §2.3 (PK (source, event_id), processed_at default UTC); add a scheduled purge job skeleton (delete older than configured retention; config entry per §16.6, owner per §16.2); if PARTIAL: expand/contract to the §2.3 shape.
- **Do not change:** existing dedup layers until IN-05 consolidates consumption order.
- **Tests to add:** duplicate-key insert returns cleanly; purge deletes only beyond retention.
- **Edge cases:** deliberately NO parked-event table alongside (§2.3 — SPEC_CONFLICT if anything asks for one).
- **Manual validation:** table exists; purge dry-run deletes expected rows on seeded data.
- **Expected outcome:** inbox ready for IN-05.
- **Failure signs:** purge retention < Kafka topic retention (violates the §16.2 chain).
- **Common mistakes:** making event_id globally unique instead of per (source, event_id).
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
- **Implementation instructions:** per CA-4: per-column enum CHECKs for the four dimensions + blocked_reason; L2 (CONFIRM ⇒ SUB >= MAYBE), L3 (SUB >= MAYBE ⇒ stage >= POST), L4 (EXECUTED ⇒ SUBMITTED), L5 (CONFIRM ⇒ stage_state IN (READY, BLOCKED)), L6 (CLAIMED ⇔ claim fields set), L7 (RETRY_WAIT ⇒ next_retry_at), L8 (BLOCKED ⇔ blocked_reason), L1-shape (outcome set ⇒ stage_state READY ∧ claim/retry/blocked fields NULL); UNIQUE(idempotency_key); UNIQUE(uetr) via NULL-ignoring index; I6 unique function index CASE WHEN outcome IS NULL THEN payment_obligation_id END. NOVALIDATE→VALIDATE sequencing per S-01.
- **Do not change:** L9 (cross-table — code + drift scanner, NOT a CHECK; do not attempt).
- **Tests to add:** one violation test per constraint (insert/update illegal row → ORA error); I6 test (second active request for same obligation rejected); uetr NULL-multiplicity test.
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
- **Purpose:** §10.3: the FREEZE is a transition property no CHECK can see — an UPDATE trigger rejects any dimension change on a row whose outcome was already non-NULL; the release-guard trigger rejects a terminal-negative outcome write on a MAYBE/SUBMITTED row unless the session context carries the evidence flag (set by the authoritative-negative code path or the §9.3 procedure). Raw fat-finger SQL fails loudly.
- **Prerequisites:** S-05; CA-4 (mechanics); D-02 (trigger privileges confirmed).
- **Requirement sections / concepts to read:** §10.3 (backstop paragraphs), §10.1 (release guard), §9.3 (legitimate flag setters).
- **Placeholder components involved:** [Stored Procedure / Trigger Area], [DB Migration Directory].
- **Local placeholder mappings required before starting:** both; Oracle session-context facility confirmed (D-10/D-02 — else BLOCKED).
- **Local code areas to discover:** how the app sets DB session state per transaction (connection pooling interaction — MUST_VERIFY_LOCALLY).
- **How to locate:** data-source/session customizer config.
- **Implementation instructions:** freeze trigger: BEFORE UPDATE, if :old.outcome IS NOT NULL and any dimension column changes → raise. Release-guard trigger: BEFORE UPDATE, if :new.outcome IN (terminal-negative set) and :old.submission_state IN (MAYBE_SUBMITTED, SUBMITTED) and evidence flag not set in session context → raise. Evidence-flag mechanics per CA-4: set by the authoritative-negative code path within the transaction, cleared with it; the §9.3 procedure is the single legitimate MANUAL setter. Pool-safety: the flag must be transaction-scoped or explicitly cleared — verify with the real pool.
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
- **Requirement sections / concepts to read:** §16.6 artifact 4 (index list), §9.5 (sweep order: cutoff first, then oldest maybe_since), §15 (scan scopes).
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
- **Title:** Backfill stage/stage_state/submission_state/outcome (+ anchors where derivable) from the legacy status for existing rows
- **Classification:** MVP normative implementation
- **Purpose:** existing rows must satisfy the constraints before VALIDATE and behave correctly under new rules.
- **Prerequisites:** S-03; D-04 (legacy status meanings memo).
- **Requirement sections / concepts to read:** §10.4 (label ↔ tuple mapping — read it in REVERSE as the backfill map), §10.2 (outcome normalization shape), §2.2 anchors.
- **Placeholder components involved:** [DB Migration Directory], [Request Status Persistence Layer].
- **Local placeholder mappings required before starting:** legacy status value list with meanings (D-04) — if any legacy value has no confident tuple mapping, that value's rows are BLOCKED: report, do not guess.
- **Local code areas to discover:** none new.
- **How to locate:** n/a.
- **Implementation instructions:** write the legacy→tuple mapping table locally (reviewed by the human owner BEFORE running); backfill via idempotent migration or supervised script: dimensions per mapping; submission_state conservatively (any legacy state that could have reached the wire and lacks definitive evidence → MAYBE_SUBMITTED, per §7.1's definitions — fail toward resolver, never toward NOT_SUBMITTED); anchors: maybe_since/submitted_at set to a defensible timestamp (e.g. legacy state-change time if one exists, else backfill run time — record choice); terminal rows normalized per L1 shape.
- **Do not change:** legacy status values themselves (dual-run reads them until P14).
- **Tests to add:** backfill idempotency (re-run = no-op); per-legacy-value spot checks; post-backfill constraint dry-validate.
- **Edge cases:** in-flight rows DURING backfill (dual-write not yet on) — run in a quiet window per the S-01 plan; rows whose legacy status contradicts money fields → list for human review, skip, report.
- **Manual validation:** counts per (legacy value → tuple) reviewed; anomalies list empty or owned.
- **Expected outcome:** all rows carry valid tuples; S-05 VALIDATE can proceed.
- **Failure signs:** any row with dimensions violating L2–L8 after backfill.
- **Common mistakes:** optimistic NOT_SUBMITTED backfills (the pay-twice direction — §7.1's criterion is "provably cannot execute").
- **Completion criteria:** backfill complete; anomaly list dispositioned; constraints validated.
- **Stop condition:** validated.
- **Next task:** S-09.

### S-09 — Migration test pass

- **Task ID:** S-09
- **Title:** Full migration test pass: clean schema, prod-shaped schema, dual-run compatibility
- **Classification:** MVP normative implementation
- **Purpose:** prove the whole P3 sequence per §16.5 before any behavior change lands on it.
- **Prerequisites:** S-02..S-08 merged.
- **Requirement sections / concepts to read:** §16.5 (expand/contract, claim compatibility across one release boundary).
- **Placeholder components involved:** [DB Migration Directory], [Integration Test Suite].
- **Local placeholder mappings required before starting:** real-Oracle test lane available (if D-11 found H2-only, FIRST set up the Oracle lane — that setup is part of this task; split locally if large).
- **Local code areas to discover:** CI pipeline hooks for migration tests.
- **How to locate:** D-11 findings.
- **Implementation instructions:** run/automate: full sequence on clean Oracle; full sequence on a prod-shaped copy (with backfill); OLD application version boots and passes its smoke tests against the NEW schema (dual-run proof — additive columns must not break it); constraint violation suite (S-05/S-06 tests) in CI.
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
- **Implementation instructions:** in the creation path: obligation row locked (SELECT FOR UPDATE) → read seq → increment → use in K-02 derivation → insert request with the seq value recorded — all one transaction.
- **Do not change:** what triggers creation (that is RG-06).
- **Tests to add:** two concurrent creations on one obligation → distinct sequential seqs (the lock serializes); rollback does not burn a seq inconsistently with the inserted row (both roll back together).
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
- **Implementation instructions:** restructure the posting path so that: claim transaction persists (first claim) the identity + (every claim, K-05) hash/flag/attempt-stamp, COMMITS, and only then the HTTP call runs; if the claim COMMIT outcome is unknown (failover/connection loss mid-commit) the worker does NOT call the engine — treat the claim as lost; lease expiry → MAYBE and the resolver owns it (§11).
- **Do not change:** payment construction; SDK call semantics.
- **Tests to add:** ordering test (kill/fault injection between commit and call → row shows persisted identity, no wire call made — assert via stub); ambiguous-commit test (§16.6-6 catalog entry: simulated commit-unknown → no HTTP call).
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
- **Implementation instructions:** integration tests: (1) crash before POST (after claim commit) → retry reuses the SAME persisted key; (2) crash after POST, before response → row MAYBE via lease expiry, no fresh key ever minted; (3) restore simulation: delete the request row + reset obligation counters to a pre-insert image (test harness), re-run creation for the same shortfall → derived key EQUALS the deleted row's key; (4) UNIQUE(idempotency_key) violation surfaces as a loud error, never silent.
- **Do not change:** production code (test-only task; failures here reopen K-xx tasks).
- **Tests to add:** the four above (catalog T-07/T-08/T-09 alignment).
- **Edge cases:** test (3) must use the REAL derivation path, not a shortcut call to the hash function.
- **Manual validation:** review that stubs assert on the KEY the engine received.
- **Expected outcome:** duplicate-prevention evidence recorded for Section Q.
- **Failure signs:** test (3) passing only because the harness reused the old row.
- **Common mistakes:** asserting on internal fields instead of what crossed the (stubbed) wire.
- **Completion criteria:** all four green.
- **Stop condition:** green; Q evidence recorded.
- **Next task:** U-01.

## H-Phase 5 — UETR response persistence (P5)

### U-01 — Acceptance-class-only UETR persistence

- **Task ID:** U-01
- **Title:** Persist uetr ONLY from acceptance-class responses; rejection/collision responses never write or overwrite it
- **Classification:** MVP normative implementation
- **Purpose:** §5: a rejection/collision response's UETR names a submission under which NOTHING EXECUTES; persisting it would orphan the real payment's feed events and could let a dead-UETR feed reject release a reservation of a payment that executed.
- **Prerequisites:** S-03 (uetr column UNIQUE); D-05 (response parsing map); TL-11(a) answer helpful (which field) — if unknown, mark the extraction site UNCLEAR and stub behind it.
- **Requirement sections / concepts to read:** §5 (persistence rules + identity chain), §7.2 (which responses are which class), §2.2 (uetr).
- **Placeholder components involved:** [Provider Response Parser], [Request Status Persistence Layer].
- **Local placeholder mappings required before starting:** parser mapped; response classes enumerable (CA-1 helps; the CLASS rule is implementable before the full table: acceptance + original-response-replay = persist; everything else = don't).
- **Local code areas to discover:** every write site of the uetr column.
- **How to locate:** F.16.
- **Implementation instructions:** centralize uetr writes to one code path taking the response class; persist on acceptance-class only (engine accepted; original-response replay per §16.6-1); never overwrite a non-NULL uetr from any response; DUPLICATE_REQUEST / collision / sync rejects never write (§7.2 flow rows).
- **Do not change:** feed-matching reads (IN tasks own them).
- **Tests to add:** acceptance persists; DUPLICATE_REQUEST leaves prior value (or NULL) intact (§16.6-6 named catalog entry); collision leaves intact; sync reject leaves intact; non-NULL never overwritten.
- **Edge cases:** a response carrying both acceptance semantics and a warning code — classify by CA-1; until classified, fail toward NOT persisting (reversible; the §9 sweep recovers by key).
- **Manual validation:** stub run of each class; inspect the row.
- **Expected outcome:** dead UETRs never persisted.
- **Failure signs:** "persist whatever the response carries" convenience code anywhere.
- **Common mistakes:** overwriting on the downgrade re-POST's fresh SDK-minted UETR after a DUPLICATE_REQUEST answer.
- **Completion criteria:** tests green.
- **Stop condition:** merged.
- **Next task:** U-02.

### U-02 — provider_reference persistence

- **Task ID:** U-02
- **Title:** Persist any other engine-assigned reference as provider_reference — a distinct field, never merged with uetr
- **Classification:** MVP normative implementation
- **Purpose:** §2.2: secondary feed-matching key (§8) with fail-closed fallback semantics; UNIQUE index makes silent reuse loud.
- **Prerequisites:** S-03; U-01.
- **Requirement sections / concepts to read:** §2.2 (provider_reference), §8 (fallback rule), §5 ("any OTHER engine-assigned reference").
- **Placeholder components involved:** [Provider Response Parser], [Request Status Persistence Layer].
- **Local placeholder mappings required before starting:** parser mapped.
- **Local code areas to discover:** which response field(s) carry a non-UETR reference (MUST_VERIFY_LOCALLY / CA-2).
- **How to locate:** D-05 memo + CA-2.
- **Implementation instructions:** extract + persist into provider_reference; UNIQUE index per CA-4 (violation → loud error + investigation, per §8's "silent reuse loud" intent — TL-12 pending); never copied into uetr.
- **Do not change:** uetr logic (U-01).
- **Tests to add:** persistence; uniqueness violation surfaces loudly; fields never cross-assigned.
- **Edge cases:** engine reuses references per day/batch (TL-12 UNCONFIRMED) — the UNIQUE index may then reject legitimate rows: if observed locally/sandbox, record and raise Q-17; do not silently drop the index (decision belongs to the owner).
- **Manual validation:** stub run; row inspection.
- **Expected outcome:** reference captured, distinct, loud on reuse.
- **Failure signs:** merged uetr/reference field.
- **Common mistakes:** treating the reference as a dedup key (§5: nothing money-safe keys on it).
- **Completion criteria:** tests green.
- **Stop condition:** merged.
- **Next task:** U-03.

### U-03 — UETR behavior test set

- **Task ID:** U-03
- **Title:** Feed-matching + non-persistence integration tests for UETR rules
- **Classification:** MVP normative implementation
- **Purpose:** lock the §5 persistence rules against regressions and prove a dead-UETR feed event cannot match.
- **Prerequisites:** U-01, U-02; IN-05 not required (use a direct call to the matching logic if the consumer isn't rebuilt yet — else defer the feed-side case to IN-06 and note it).
- **Requirement sections / concepts to read:** §5, §8 (matching), §16.6-6 (named entry).
- **Placeholder components involved:** [Integration Test Suite].
- **Local placeholder mappings required before starting:** matching logic locatable.
- **How to locate:** F.7/F.16.
- **Local code areas to discover:** none new.
- **Implementation instructions:** tests: acceptance-class persists + a feed event under that UETR matches the row; a rejection-class response's UETR (never persisted) → a feed event under it goes UNMATCHED (logged + counted + acked path once IN-05/06 exist; before that, assert no row resolves).
- **Do not change:** production code.
- **Tests to add:** the above.
- **Edge cases:** crash-before-response rows (uetr NULL) — feed event unmatched, recovered by §9 (assert unmatched here).
- **Manual validation:** n/a.
- **Expected outcome:** UETR rules regression-locked.
- **Failure signs:** matching falls through to fuzzy matching on anything besides the §8 fallback rule.
- **Common mistakes:** none beyond the above.
- **Completion criteria:** green.
- **Stop condition:** green; report.
- **Next task:** ST-01.

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
- **Title:** Implement §10.4 labels as a derived view/expression; route dashboards/card/log/ops reads to it
- **Classification:** MVP normative implementation
- **Purpose:** the old 13-value status survives ONLY as a derived display label; labels never appear in machine-consumed API payloads.
- **Prerequisites:** ST-01.
- **Requirement sections / concepts to read:** §10.4 (mapping + strictness), §2.2 ("no rule may key on it").
- **Placeholder components involved:** [Request Status Persistence Layer], [Metrics / Alerting Layer] (log line), card read path.
- **Local placeholder mappings required before starting:** reader inventory (D-04).
- **Local code areas to discover:** display readers of the legacy status.
- **How to locate:** D-04 reader list, display-flagged entries.
- **Implementation instructions:** implement the §10.4 mapping exactly (DB view or shared expression — choose per local convention, record); migrate DISPLAY consumers (dashboards, card payload's label field, log lines, ops queries you control) to it; the card read contract returns dimension columns + label per §10.4's rule (no consumer may parse the label).
- **Do not change:** rule-keyed readers (ST-05's job); external report SQL you don't own (record as UNCLEAR for owners).
- **Tests to add:** label mapping per §10.4 row; NEEDS_REVIEW includes blocked_reason display.
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
- **Purpose:** §14: the only local forensic record (no journal): request_id, idempotency_key, request_seq, correlation_id, (stage, stage_state, submission_state, outcome) before → after, display label, trigger_source, trigger_event_id; restore-surviving record of every issued/POSTed key (§5.2 step 5b leans on it).
- **Prerequisites:** ST-02 (all writers CAS'd), ST-04 (label).
- **Requirement sections / concepts to read:** §14 (whole), §16.3 (masking — no account data in the line).
- **Placeholder components involved:** [Request Status Persistence Layer], [Metrics / Alerting Layer].
- **Local placeholder mappings required before starting:** logging conventions (D-10).
- **Local code areas to discover:** MDC/correlation propagation.
- **How to locate:** F.21.
- **Implementation instructions:** one emission point in the shared CAS helper (fires only on rowCount==1); fields exactly per §14; trigger_source = the flow (values per flow tasks; OPS_PLATFORM_VERIFIED reserved for OP-01); correlation_id from MDC; posting-claim line additionally carries last_sent_hash + attempt_count (K-05 emits it — verify one convention, not two).
- **Do not change:** log platform config beyond adding the line; retention (§14 floor) is an OB-05/owner item — record current retention vs the 90-day floor, report if below.
- **Tests to add:** log-capture test per transition family: line present, fields populated, before/after correct, no account data.
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
- **Title:** Claim = CAS to CLAIMED + claimed_by + claim_expires_at (L6); scanners use FOR UPDATE SKIP LOCKED, DB time, per-item transactions
- **Classification:** MVP normative implementation
- **Purpose:** §11: second scanner cannot re-claim mid-processing; stale workers are fenced by CAS row counts.
- **Prerequisites:** ST-02; D-08 job inventory.
- **Requirement sections / concepts to read:** §11 (claims, scanner rules), §2.2 (claim fields), L6.
- **Placeholder components involved:** [Retry Resolver Job], [Request Status Persistence Layer], stage workers.
- **Local placeholder mappings required before starting:** claim-column reality from D-08 (exists? semantics?).
- **Local code areas to discover:** current claim/pick-up logic in each worker/scanner.
- **How to locate:** D-08 inventory.
- **Implementation instructions:** standard claim CAS: READY/RETRY_WAIT(due) → CLAIMED + claimed_by + claim_expires_at, WHERE carries prior state + outcome IS NULL; work; completion CAS moves onward and NULLs claim fields (L6); scanners: SKIP LOCKED selection, next_retry_at compared against DB time, one transaction per item; lease durations per stage from config (§16.6).
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
- **Implementation instructions:** expiry sweep (or claim-time check): CLAIMED + claim_expires_at < DB now → ENRICH: CAS back to READY (clear claim fields); POST: CAS to stage=CONFIRM, stage_state=READY, submission_state=MAYBE_SUBMITTED, clear claim fields, stamp maybe_since (ST-07 helper). No exceptions, no carve-outs.
- **Do not change:** CONFIRM-stage rows' claim semantics (resolver rows are not CLAIMED workers — §4.4 note).
- **Tests to add:** ENRICH expiry → re-claimable, work repeats safely (read-only); POST expiry → MAYBE row, resolver-owned; expired POST row NEVER selectable by the posting claim query (assert the claim WHERE excludes it structurally).
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

## H-Phase 7 — Reservation and release guards (P7)

### RG-01 — Reservation increment at creation

- **Task ID:** RG-01
- **Title:** +committed_amount in the SAME transaction as the payment_request insert
- **Classification:** MVP normative implementation
- **Purpose:** §3 INCREMENT rule: shortfall always sees in-flight money; the request-creation double-pay window does not exist.
- **Prerequisites:** S-09; K-01 (same transaction already carries seq++); D-03 money-semantics memo reviewed (if an existing counter has different semantics, this task writes the §2.1 column and leaves the legacy counter untouched until P14 — record).
- **Requirement sections / concepts to read:** §3 (increment + consequences), §6.8 (creation point), §11 (lock).
- **Placeholder components involved:** [Payment Request Creation Component], [Reservation Repository], [Obligation Repository].
- **Local placeholder mappings required before starting:** all three; creation sites known.
- **Local code areas to discover:** creation transaction.
- **How to locate:** F.2.
- **Implementation instructions:** inside the locked creation transaction: committed_amount += request.amount together with the insert; no other flow increments it.
- **Do not change:** legacy counters (parallel until P14 contract).
- **Tests to add:** insert+increment atomicity (rollback rolls both); I1 holds after creation; concurrent create attempts (I6 blocks the second; counter incremented once).
- **Edge cases:** creation retried after ORA-00001 obligation race (§6.1) — no double increment (transaction retry semantics).
- **Manual validation:** row + counter inspection after a local creation.
- **Expected outcome:** reservation exists from birth.
- **Failure signs:** increments at POST time anywhere (§3: NO MOVEMENT at POST).
- **Common mistakes:** incrementing outside the lock.
- **Completion criteria:** tests green.
- **Stop condition:** merged.
- **Next task:** RG-02.

### RG-02 — Reservation release on terminal-negative

- **Task ID:** RG-02
- **Title:** −committed_amount in the SAME transaction as the terminal-negative CAS, only on row-count 1
- **Classification:** MVP normative implementation
- **Purpose:** §3 DECREMENT rule: once per transition, redelivery-safe.
- **Prerequisites:** ST-02, ST-06; RG-01.
- **Requirement sections / concepts to read:** §3 (decrement), §10.2 (outcome writes), §10.1 (release guard context).
- **Placeholder components involved:** [Reservation Repository], [Request Status Persistence Layer].
- **Local placeholder mappings required before starting:** outcome writers list (ST-06 helper sites).
- **Local code areas to discover:** none new.
- **How to locate:** ST-06.
- **Implementation instructions:** extend the ST-06 outcome path: when outcome ∈ {REJECTED, CANCELLED, SUPERSEDED} and the CAS returned 1, decrement committed_amount by request.amount in the same transaction (obligation lock already held per §11).
- **Do not change:** which flows may set terminal-negative (release GUARD is RG-05 + S-06).
- **Tests to add:** decrement iff row-count 1 (replayed event → 0 rows → no decrement); I1 after each terminal-negative; EXECUTED does NOT decrement committed (I1 includes EXECUTED rows — assert).
- **Edge cases:** CANCELLED via §6.4 vs REJECTED via feed — same decrement path (single helper).
- **Manual validation:** counter trace across a seeded reject.
- **Expected outcome:** release exactly once per transition.
- **Failure signs:** decrement on non-terminal failures (F.13 PARTIAL sites — must be gone).
- **Common mistakes:** decrementing on EXECUTED (I1: committed covers NULL + EXECUTED).
- **Completion criteria:** tests green.
- **Stop condition:** merged.
- **Next task:** RG-03.

### RG-03 — Confirmation increment with amount equality

- **Task ID:** RG-03
- **Title:** +confirmed_amount on outcome=EXECUTED under the amount-equality guard; mismatch → BLOCKED(AMOUNT_MISMATCH) path
- **Classification:** MVP normative implementation
- **Purpose:** §3 (confirmation moves confirmed only) + §8/§16.4 (no tolerance; mismatch is defect evidence, not business state).
- **Prerequisites:** RG-02; ST-06.
- **Requirement sections / concepts to read:** §3, §8 (amount-mismatch block), §16.4 (compareTo, no tolerance), §10.5 (mismatch row).
- **Placeholder components involved:** [Reservation Repository], [Request Status Persistence Layer].
- **Local placeholder mappings required before starting:** settlement application path (shared by IN-07/RC-06 later; build the helper here).
- **Local code areas to discover:** none new.
- **How to locate:** n/a.
- **Implementation instructions:** settlement helper: evidence amount compareTo request.amount == 0 → outcome=EXECUTED CAS (sets SUBMITTED, L4) + confirmed_amount += amount, same transaction; mismatch → NO outcome write, NO confirmed movement: CAS to same-stage BLOCKED(AMOUNT_MISMATCH) + submission_state=SUBMITTED (settlement evidence tightens — §8) + CRITICAL alert hook; overpay evaluation after confirmed moves (RG-04).
- **Do not change:** all-or-nothing assumption (confirmed contract fact — no partial-settlement handling exists by design).
- **Tests to add:** equality via compareTo (scale variants: 10 vs 10.00 equal); mismatch parks + tightens + no money movement; I2/I3 after settlement; JPY/BHD scale round-trips (§16.4).
- **Edge cases:** mismatched settlement landing on a MAYBE row (legal — terminal evidence applies to any active row; park takes it off MAYBE clocks per §8).
- **Manual validation:** seeded settlement + seeded mismatch traces.
- **Expected outcome:** confirmation safe and exact.
- **Failure signs:** BigDecimal.equals anywhere in amount comparisons (§16.4).
- **Common mistakes:** treating mismatch as retryable; moving confirmed on the park.
- **Completion criteria:** tests green.
- **Stop condition:** merged.
- **Next task:** RG-04.

### RG-04 — Overpay latch

- **Task ID:** RG-04
- **Title:** Set overpay_blocked when confirmed_amount > required_amount; latch is one-way; alert on set
- **Classification:** MVP normative implementation
- **Purpose:** §13: one-way door into manual territory; post-latch amounts stop being trustworthy automation inputs; §15 alerts on the latch itself.
- **Prerequisites:** RG-03.
- **Requirement sections / concepts to read:** §13 (latch + rationale + cross-stream race), §3 (I4), §6.5 (latch guard, consumed by RG-10).
- **Placeholder components involved:** [Reservation Repository], [Metrics / Alerting Layer].
- **Local placeholder mappings required before starting:** RG-03 helper.
- **Local code areas to discover:** none new.
- **How to locate:** n/a.
- **Implementation instructions:** after any confirmed/required change under the lock: if confirmed > required (compareTo) → set latch (idempotent), fire the §15 latch alert hook; NEVER auto-clear (no code path unsets it — the un-set operation does not exist at MVP).
- **Do not change:** the rejected auto-unlatch alternative stays rejected (§13).
- **Tests to add:** latch sets on overpay; survives later required_amount increase (the §13 trace: required 150 > confirmed 100 with latch STILL set); I4; alert fired once per set.
- **Edge cases:** overpay via amendment-down after EXECUTED (required drops below confirmed) — same latch.
- **Manual validation:** seeded trace per the §13 example.
- **Expected outcome:** latch semantics exact.
- **Failure signs:** any generic recalculation clearing it (§13: "never silently un-set").
- **Common mistakes:** keying the latch on committed instead of confirmed.
- **Completion criteria:** tests green.
- **Stop condition:** merged.
- **Next task:** RG-05.

### RG-05 — Release guard in code + supersede/close operation

- **Task ID:** RG-05
- **Title:** Enforce §10.1's release guard in every terminal-negative code path; implement the guarded supersede/close operation
- **Classification:** MVP normative implementation
- **Purpose:** releasing a reservation whose money may have moved is the one remaining double-payment path (§3); the supersede/close operation is a §3 REQUIRED feature, executed at MVP as a controlled procedure (§20 interim model).
- **Prerequisites:** RG-02; S-06 (trigger backstop + evidence flag).
- **Requirement sections / concepts to read:** §10.1 (release guard), §9.4, §3 (supersede/close + FORBIDDEN clause), §20 (interim model).
- **Placeholder components involved:** [Request Status Persistence Layer], [Operator Admin Procedure Area] (the supersede/close procedure), [Stored Procedure / Trigger Area].
- **Local placeholder mappings required before starting:** S-06 evidence-flag mechanics.
- **Local code areas to discover:** every terminal-negative initiator (auto-cancel RG-07, feed reject IN-07, resolver reject RC-06, ops paths).
- **How to locate:** outcome-writer inventory.
- **Implementation instructions:** shared guard check before any terminal-negative CAS: permitted iff submission_state=NOT_SUBMITTED OR driven by an authoritative engine negative (which sets the evidence flag for the trigger) OR the §9.3 procedure (OP-01, its own flag setter). Supersede/close: a guarded procedure (restricted role, ticket + operator identity logged per §20-8) setting SUPERSEDED/CANCELLED on a stalled active request, refused while MAYBE/SUBMITTED unless evidence-driven; releases the reservation via the RG-02 path.
- **Do not change:** the S-06 trigger (defense in depth — code guard AND trigger both live).
- **Tests to add:** guard denies terminal-negative on MAYBE row from a non-evidence path (code layer AND trigger layer asserted separately); allows on NOT_SUBMITTED; supersede/close on a stalled ENRICH·BLOCKED row works + releases; refused on MAYBE.
- **Edge cases:** ops reject of a BLOCKED·NOT_SUBMITTED row is legal (release guard passes on NOT_SUBMITTED).
- **Manual validation:** attempted manual release on a seeded MAYBE row fails loudly (trigger), demonstrating the §10.3 fat-finger story.
- **Expected outcome:** release rights enforced everywhere.
- **Failure signs:** flows setting the evidence flag without carrying authoritative evidence (grep flag-setter call sites — must be exactly: authoritative-negative path, OP-01).
- **Common mistakes:** treating a status-query answer as evidence for RELEASE (§9.4 forbids — query answers only tighten).
- **Completion criteria:** tests green.
- **Stop condition:** merged.
- **Next task:** RG-06.

### RG-06 — Standing shortfall re-evaluation (single creation point)

- **Task ID:** RG-06
- **Title:** Consolidate request creation into §6.8's standing re-evaluation with the trigger inventory T1–T4 and the successor policy
- **Classification:** MVP normative implementation
- **Purpose:** §6.8: exactly ONE creation point; deferred, never lost; ordering-aware successor policy bounds blind re-pay of rejects.
- **Prerequisites:** RG-01 (creation transaction shape), IN-02 helpful (ordering advance trigger) — coordinate; K-01/K-02 (identity inside creation).
- **Requirement sections / concepts to read:** §6.8 (whole: gate conditions, T1–T4, successor policy, consequences), §3 (I5 under lock), §6.2.
- **Placeholder components involved:** [Payment Request Creation Component], [Obligation Repository].
- **Local placeholder mappings required before starting:** ALL legacy creation sites (D-04/F.2 — if any site cannot be routed through the single point, STOP and report).
- **Local code areas to discover:** each legacy creation call site.
- **How to locate:** F.2 inventory.
- **Implementation instructions:** implement evaluate(obligation) under the lock exactly per §6.8's condition list (shortfall > 0 ∧ I6 free ∧ latch clear ∧ validation_failed not live ∧ provider_rejected not live ∧ successor policy permits) → create (RG-01 + K-01/K-02 choreography; creating_ordering := upstream_ordering); invoke from T1 (message applied — even without amount change), T2 (outcome set), T3 (marker liveness change), T4 (ops clear/retry); route every legacy creation site through it (delete none until routed).
- **Do not change:** WHAT amount a request gets (business logic).
- **Tests to add:** each T-trigger fires evaluation; each gate condition blocks; successor policy: CANCELLED/SUPERSEDED permit; EXECUTED permits iff shortfall remains; REJECTED permits iff upstream_ordering strictly newer than creating_ordering ∧ reject_count < 2 ∧ no live marker; deferred amendment (in-flight request → no create; on resolve → successor); zero-shortfall message → no request (§6.2).
- **Edge cases:** correction-races-enrichment-reject (§6.8 consequence — marker tagged with OLD creating_ordering, correction newer → not live → create passes): seed and assert.
- **Manual validation:** trace T1..T4 locally with logs.
- **Expected outcome:** one creation point; no second path.
- **Failure signs:** any residual direct-insert site (grep for insert on the request table outside the component).
- **Common mistakes:** evaluating outside the lock; forgetting T1's "whether or not required_amount changed".
- **Completion criteria:** tests green; site inventory routed.
- **Stop condition:** merged.
- **Next task:** RG-07.

### RG-07 — Auto-cancel with row-count-0 branching

- **Task ID:** RG-07
- **Title:** Implement §6.4 auto-cancel CAS (exact set semantics) + the submission-state-first row-count-0 branches + the retry-guard
- **Classification:** MVP normative implementation
- **Purpose:** §6.4: amendment-down cancels only provably-releasable requests; MAYBE rows park (AMENDMENT_PARKED); the retry-guard re-checks amount staleness before every re-POST.
- **Prerequisites:** RG-05 (release guard), RG-06 (successor creation), ST-02.
- **Requirement sections / concepts to read:** §6.4 (whole: CAS shape, set semantics, branches, retry-guard), §7.0 (staleness term), §10.5 (rows).
- **Placeholder components involved:** [Payment Request Creation Component] (amendment flow), [Request Status Persistence Layer], [Retry Resolver Job] (retry-guard site).
- **Local placeholder mappings required before starting:** amendment-processing path (IN-02's home) identified.
- **Local code areas to discover:** none new.
- **How to locate:** message-processing flow.
- **Implementation instructions:** the §6.4 CAS verbatim (outcome IS NULL ∧ stage IN (ENRICH, POST) ∧ NOT (POST·CLAIMED) ∧ stage_state <> BLOCKED ∧ submission_state = NOT_SUBMITTED → CANCELLED); row-count 1 → release (RG-02) + re-derive + RG-06 successor; row-count 0 → branch submission_state FIRST: MAYBE (any stage, not CLAIMED) → CAS to BLOCKED(AMENDMENT_PARKED) + alert; MAYBE·CLAIMED → DEFER (no park mid-claim — the retry-guard or resolver applies it later; §6.4); SUBMITTED → leave alone; NOT_SUBMITTED-yet-unreleasable (POST·CLAIMED or BLOCKED) → per §6.4 last branch. Retry-guard in the retry worker: before re-POST of POST·RETRY_WAIT, re-validate amount vs current shortfall under the lock; stale + NOT_SUBMITTED → cancel; stale + MAYBE → park AMENDMENT_PARKED.
- **Do not change:** ENRICH·CLAIMED cancellability (normative — read-only work).
- **Tests to add:** each CAS row (§10.5): cancellable set; ENRICH·CLAIMED cancelled + stale worker fenced; POST·CLAIMED untouched; BLOCKED untouched; MAYBE park (incl. CONFIRM·READY); deferred park under live claim lands later; retry-guard both branches; park is stable (no un-park by §9.2 while stale — full assert when RC-07 exists; pending named case).
- **Edge cases:** §10.5's amendment-down row against MAYBE at ANY stage incl. CONFIRM.
- **Manual validation:** seeded amendment scenarios.
- **Expected outcome:** amendment safety complete.
- **Failure signs:** cancel of a MAYBE row anywhere (the §3 double-pay path).
- **Common mistakes:** branching on stage first (spec: money truth first).
- **Completion criteria:** tests green.
- **Stop condition:** merged.
- **Next task:** RG-08.

### RG-08 — Step-status predicate

- **Task ID:** RG-08
- **Title:** Implement §4.1's completion predicate as THE ui_step_status derivation, run under the lock after every applied mutation
- **Classification:** MVP normative implementation
- **Purpose:** money derives status (P1): completion never copied from events; vacuous-completion guards protect anchors.
- **Prerequisites:** RG-01..04 (amounts trustworthy); IN-04 marker liveness helper (coordinate — implement the liveness predicate here if IN-04 not yet done, single shared helper).
- **Requirement sections / concepts to read:** §4.1 (predicate + all four bullets), §4 (derivation order), §2.1 (marker liveness definition incl. the §6.6 anchor clause).
- **Placeholder components involved:** [Obligation Repository], [Request Status Persistence Layer] (hook from ST-02's re-derive).
- **Local placeholder mappings required before starting:** derivation hook point (ST-02).
- **Local code areas to discover:** current ui_step_status writers (must become derivation-only).
- **How to locate:** column write sites.
- **Implementation instructions:** predicate exactly per §4.1: required NOT NULL ∧ required > 0 ∧ confirmed >= required ∧ committed = confirmed ∧ latch clear ∧ validation_failed not LIVE (LIVE = marker set ∧ (marker ordering >= upstream_ordering ∨ upstream_ordering IS NULL)); output IN_PROGRESS/COMPLETED stored; NOT_STARTED is row absence (§12); wire into the ST-02 re-derive hook (same transaction, under lock); remove/route any event-copy writer of ui_step_status.
- **Do not change:** feed handlers may NEVER write ui_step_status directly (§4.1 last bullet).
- **Tests to add:** each predicate term isolated (anchor row cannot complete; post-decrement zero-zero cannot complete; active request blocks completion; recovered anchor completes after valid message); derivation runs after every mutating flow (hook coverage).
- **Edge cases:** terminal-negative leaves committed=confirmed=0 with required unpaid → IN_PROGRESS (the mandatory confirmed>=required term).
- **Manual validation:** card-visible status trace through a full happy path locally.
- **Expected outcome:** completion always derived, never copied.
- **Failure signs:** COMPLETED on a scope with an active request (impossible per predicate — if seen, reservation choreography is broken).
- **Common mistakes:** omitting the upstream_ordering IS NULL clause (anchor liveness undefined exactly where §6.6 needs it).
- **Completion criteria:** tests green.
- **Stop condition:** merged.
- **Next task:** RG-09.

### RG-09 — Active-exception + next-actor derivation

- **Task ID:** RG-09
- **Title:** Implement §4.2's precedence-ordered exception derivation and §4.5's next-actor derivation (never stored)
- **Classification:** MVP normative implementation
- **Purpose:** derive, never accumulate (P2): no set/clear rules to get wrong; rank-1 conditions never masked by transient errors.
- **Prerequisites:** RG-08 (shared derivation pass); ST-05 (rules keyed on dimensions).
- **Requirement sections / concepts to read:** §4.2 (ranks + rationale), §4.3 (stored inputs), §4.5 (actor table + dual-actor note), §13 (categories/retryability/severity).
- **Placeholder components involved:** [Obligation Repository] (read-model fields), [Request Status Persistence Layer].
- **Local placeholder mappings required before starting:** RG-08 hook.
- **Local code areas to discover:** current exception writers (become derivation-only).
- **How to locate:** active_exception field writers.
- **Implementation instructions:** in the same derivation pass: evaluate §4.2's ranks in order over live conditions (active requests only) → write active_exception_* fields (§13 attributes; content rules per §12: ops-readable, no sensitive account data, no stack traces); next-actor: implement §4.5 as a pure function of the tuple (+ ages) for scanner scoping/metrics — NEVER persisted.
- **Do not change:** rank order; the two rank-1 conditions' precedence rationale.
- **Tests to add:** precedence (MAYBE outranks OVERPAY outranks validation etc. per ranks); derivation clears by construction (corrected message → DATA_VALIDATION_FAILED gone in the same transaction); dual-actor rows (BLOCKED+MAYBE → ops AND resolver; RETRY_WAIT+MAYBE → scanner AND resolver) — assert via the function.
- **Edge cases:** PAYMENT_OUTCOME_UNKNOWN must never surface as SYSTEM_UNAVAILABLE (§9.3 display block — explicit test).
- **Manual validation:** card fields through seeded scenarios.
- **Expected outcome:** exceptions/actors always current, never stale accumulations.
- **Failure signs:** any imperative setException call outside the derivation.
- **Common mistakes:** deriving from terminal rows (§4.2: active requests only).
- **Completion criteria:** tests green.
- **Stop condition:** merged.
- **Next task:** RG-10.

### RG-10 — Step reopening + latch guard

- **Task ID:** RG-10
- **Title:** Implement §6.5 reopening (required increase after COMPLETED) with reopened_at, and the latch guard (no reopening-created requests on latched scopes)
- **Classification:** MVP normative implementation
- **Purpose:** §6.5: re-activation via the standing re-evaluation; latch wins — AMENDMENT_ON_LATCHED_SCOPE alerts instead of paying.
- **Prerequisites:** RG-06 (creation), RG-04 (latch), RG-08 (status re-derives).
- **Requirement sections / concepts to read:** §6.5 (incl. latch-guard rationale), §6.3 (increase path), §2.1 (reopened_at).
- **Placeholder components involved:** [Obligation Repository], [Payment Request Creation Component].
- **Local placeholder mappings required before starting:** amendment-application path (IN-02's home).
- **Local code areas to discover:** none new.
- **How to locate:** message flow.
- **Implementation instructions:** on an applied required increase against a COMPLETED scope: recalc shortfall under lock; RG-06 evaluation creates requests (unless gated); set reopened_at; derivation returns IN_PROGRESS; overpay re-evaluates; if latched: apply the amount (§6.7 permitting), create NOTHING, fire AMENDMENT_ON_LATCHED_SCOPE.
- **Do not change:** the latch (RG-04 one-way rule).
- **Tests to add:** reopening full trace (COMPLETED → IN_PROGRESS + reopened_at + successor); latched-scope amendment → amount applied, no request, alert fired; overpay re-eval on reopening.
- **Edge cases:** reopening while a live marker exists — RG-06's gates still apply (no special path).
- **Manual validation:** seeded reopening trace.
- **Expected outcome:** reopening = ordinary standing consequence.
- **Failure signs:** a dedicated reopening creation path (must be RG-06).
- **Common mistakes:** clearing reopened_at (derivation input; card indicates reopening — §4.3).
- **Completion criteria:** tests green.
- **Stop condition:** merged; Phase P7 report.
- **Next task:** CT-01 (parallel track) / IN-01.

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
- **Implementation instructions:** POST once (record response); re-POST byte-identical payload same key; assert: no second execution (engine-side status query shows ONE payment); classify the second response (dedup code vs original-replay) and file into CA-1; record evidence.
- **Do not change:** CA-1 without the owner (the test FEEDS it).
- **Tests to add:** this test.
- **Edge cases:** engine executes twice → §18-1 FAILS: STOP the reliance chain — report immediately; the entire §7.0/§9.2 re-POST design is gated on this (TL-4's revert-to-payload-freeze clause becomes live).
- **Manual validation:** engine-side verification of single execution.
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
- **Implementation instructions:** POST; re-POST same key with a changed business field (and separately a changed amount); assert: no execution of the second; capture the rejection code; assert distinguishable from the plain-duplicate code observed in CT-02; file codes into CA-1; evidence.
- **Do not change:** n/a.
- **Tests to add:** this test (two payload-variant runs).
- **Edge cases:** engine EXECUTES the divergent payload → §18-1 FAILS catastrophically (double-pay path): STOP, report — §7.0 must revert to payload freeze per TL-4, which is a design-level decision for the humans, not a local fix.
- **Manual validation:** engine-side single-execution check.
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
- **Implementation instructions:** as close to the stated TTL edge as sandbox practicality allows (provider cooperation may be needed — aged keys or clock manipulation on their side): re-run (a) and (b) with an aged key; record behavior; compare TTL against max row lifetime incl. ops-queue SLA (from B-02/TL-5 inputs); if TTL < max lifetime: record the REQUIRED design consequence — repost_permitted gains a TTL term and past-TTL rows are ops-only (§18-1(c)) — as a follow-up task for RC-03 (do not silently implement without recording).
- **Do not change:** RC-03 within this task.
- **Tests to add:** the edge run (documented if provider-assisted rather than automated).
- **Edge cases:** TTL untestable in sandbox → record the limitation; the WRITTEN statement + the gap note go to the accountable owner (go-live decision input, not a local pass).
- **Manual validation:** evidence review with the tech lead.
- **Expected outcome:** TTL behavior known; consequences recorded.
- **Failure signs:** treating an untested TTL as verified.
- **Common mistakes:** testing only (a) at the edge (the (b) reject must also still hold).
- **Completion criteria:** evidence + consequence note filed.
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
- **Implementation instructions:** induce a sync business reject; re-POST same key (correctable condition resolved if the sandbox allows); record: re-executes vs replays rejection; if REPLAYS: record the design consequence per TL-6 — retries of that error class are no-ops; policy changes to fresh successor via §6.8 — as an RC-04 follow-up for the humans to schedule.
- **Do not change:** retry policy within this task.
- **Tests to add:** the run.
- **Edge cases:** behavior differs per reject code — test the codes CA-1 marks retryable.
- **Manual validation:** evidence review.
- **Expected outcome:** TL-6 settled empirically.
- **Failure signs:** concluding from documentation alone (§18: "a written yes does not close this item").
- **Common mistakes:** letting the sandbox's test-mode semantics differ from prod semantics unnoted (record engine version + mode).
- **Completion criteria:** evidence filed; consequence recorded if REPLAYS.
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

## H-Phase 9 — Inbound flows and status feed (P9)

### IN-01 — Upstream message validation + contract enforcement

- **Task ID:** IN-01
- **Title:** Validate the §6.0 message contract at intake (incl. currency-scale); wire build-time schema enforcement
- **Classification:** MVP normative implementation
- **Purpose:** §6.0's field set is one of the three build-time-enforced contracts (§16.5); §16.4 scale validation happens here.
- **Prerequisites:** S-02; D-07 (consumer mapped); upstream ask 3 (schema formalization) — proceed on the observed schema, mark deltas UNCLEAR.
- **Requirement sections / concepts to read:** §6.0 (fields + payload-equality definition + emission contract fact), §16.4 (scale), §16.5 (contracts), §6.6 (failure routing).
- **Placeholder components involved:** upstream consumer (maps via [Obligation Repository] flow), [Contract Test Suite].
- **Local placeholder mappings required before starting:** upstream consumer Confirmed.
- **Local code areas to discover:** current intake validation.
- **How to locate:** D-07.
- **Implementation instructions:** validate presence/typing of: business_id (also the Kafka key — observe and record whether it is; feeds upstream ask 2), scope fields, required_amount positive + currency-scale-valid (JPY 0 / BHD-KWD 3 — §16.4), ordering value, trade reference, ui ids, correlation_id; implement the canonical payload-equality function over the CANONICALIZED BUSINESS-FIELD SUBSET (scope + required_amount + trade reference — §6.0, used by §6.7 ties); failures route per §6.6 (IN-03); wire schema enforcement (registry or consumer-driven test) so a contract change fails the build.
- **Do not change:** message semantics; upstream topics.
- **Tests to add:** field-validation cases; scale cases (100.555 in 2-dec → reject, never round); payload-equality (envelope fields excluded — redelivery is a tie-IDENTICAL).
- **Edge cases:** absent/zero amount → reject (BA-2 context: intake keeps rejecting zero/absent amounts).
- **Manual validation:** seeded valid/invalid messages locally.
- **Expected outcome:** contract-guarded intake.
- **Failure signs:** silent rounding anywhere.
- **Common mistakes:** payload equality over raw bytes (every redelivery becomes a false tie-conflict — §6.0 warns).
- **Completion criteria:** tests green.
- **Stop condition:** merged.
- **Next task:** IN-02.

### IN-02 — Obligation upsert + §6.7 ordering guard

- **Task ID:** IN-02
- **Title:** Snapshot fan-out; locked obligation upsert; strictly-newer ordering mutation; tie handling; stale counting
- **Classification:** MVP normative implementation
- **Purpose:** §6.1/§6.7: a message is a FULL-TRADE SNAPSHOT fanning out to one application per payment block; a redelivered older message must never regress required_amount; ties are payload-aware; the comparison is one pluggable point (future explicit sequence, upstream ask 1).
- **Prerequisites:** IN-01; S-02; B-01 RESIDUE (upstream ask 5 in writing; PO-9 absence semantics; TL-16 watermark rule — the fan-out cannot freeze while these are open).
- **Requirement sections / concepts to read:** §1 contract facts (trade-payment cardinality), §6.0 (snapshot shape + within-snapshot uniqueness validation), §6.1 (fan-out + convergence + the two OPEN markers), §6.7 (whole), §6.9 (required_amount row).
- **Placeholder components involved:** [Obligation Repository].
- **Local placeholder mappings required before starting:** obligation upsert path.
- **How to locate:** F.1.
- **Local code areas to discover:** current amount-update path.
- **Implementation instructions:** validate the snapshot ONCE (schema, amounts, within-snapshot tuple uniqueness → whole-snapshot validation failure per §6.0/§6.6); then fan out per payment block in deterministic tuple order (fixed lock order); per block, under that obligation's lock: upsert by scope key (ORA-00001 → retry + re-read, §6.1); mutate required_amount + advance upstream_ordering ONLY if message ordering strictly newer; else count stale (metric) and drop; ties: identical payload (IN-01's function, WHOLE-snapshot equality per §6.0) → silent drop; differing → AMENDMENT_TIE_CONFLICT alert, NO application; ordering comparison isolated behind one pluggable comparator (business timestamp today; explicit sequence later — no logic change on cutover); after application → RG-06 evaluation (T1: even without amount change). ABSENT payments: INTERIM no-op until PO-9 is answered (BA-2 stands); watermark treatment of absent obligations per TL-16's answer.
- **Do not change:** BA-3 stance — no compensating ordering machinery beyond §6.7.
- **Tests to add:** the §6.7 failure trace (late original must not regress 120→100); strictly-newer applies; equal-older counted+dropped; both tie branches; T1 fires on ordering advance without amount change; snapshot fan-out: two-block snapshot updates two obligations; new-tuple block creates its obligation; within-snapshot collision → whole-snapshot validation failure; crash mid-fan-out + redelivery converges (applied blocks drop stale, unapplied apply); absent payment → no-op (interim, until PO-9).
- **Edge cases:** first message (NULL stored ordering) applies; failed-validation messages never advance ordering (IN-03's rule — assert here too); delayed-older-snapshot-containing-an-absent-payment (the TL-16 failure trace) — test per TL-16's answer.
- **Manual validation:** seeded out-of-order sequence.
- **Expected outcome:** regression-proof amounts.
- **Failure signs:** amount writes outside the comparator's gate.
- **Common mistakes:** >= instead of strictly-newer; tie-differing silently dropped ("upstream resends" is NOT a recovery for ties — §6.7).
- **Completion criteria:** tests green.
- **Stop condition:** merged.
- **Next task:** IN-03.

### IN-03 — Validation-failure anchors + DLT routing

- **Task ID:** IN-03
- **Title:** Create anchor obligations for failed-validation messages with extractable scope; DLT for unidentifiable ones
- **Classification:** MVP normative implementation
- **Purpose:** §6.6: the durable anchor readers query; failing ordering recorded on the marker but upstream_ordering NOT advanced; card shows the problem.
- **Prerequisites:** IN-02; IN-04 marker helper (or implement marker write here first — coordinate, single helper).
- **Requirement sections / concepts to read:** §6.6 (normal anchor path; key-only anchoring is NOT in scope — TL-7 future), §2.1 (validation_failed fields), §4.1 (anchor completion impossibility).
- **Placeholder components involved:** [Obligation Repository], DLT wiring.
- **Local placeholder mappings required before starting:** upstream consumer + DLT (D-07).
- **Local code areas to discover:** DLT publish path.
- **How to locate:** D-07.
- **Implementation instructions:** validation failure with extractable scope + ui_process_instance_id → upsert anchor: required_amount NULL, ui_step_status IN_PROGRESS, DATA_VALIDATION_FAILED (retryable=false) via the marker (validation_failed_at/_ordering = failing message's ordering; first_at + count per §2.1); upstream_ordering untouched; too-malformed-to-identify → DLT + ops alert (accepted blind spot).
- **Do not change:** scope of key-only anchoring (tiers 2–3 of §6.6 are TL-7 future — do NOT implement).
- **Tests to add:** anchor created with NULL amount; §4.1 cannot complete it; later valid message populates + clears liveness + creates first request (via RG-06); DLT on unidentifiable.
- **Edge cases:** repeat failing messages → monotonic marker re-tags + count increments (validation_reject_count alert ≥3 is OB-04's — the counter behavior lands with IN-04).
- **Manual validation:** card shows the anchor's exception locally.
- **Expected outcome:** no invisible NOT_STARTED for broken messages with readable scope.
- **Failure signs:** anchors advancing upstream_ordering (poisons §6.7).
- **Common mistakes:** completing anchors via a missing predicate guard (RG-08's terms exist for this).
- **Completion criteria:** tests green.
- **Stop condition:** merged.
- **Next task:** IN-04.

### IN-04 — Monotonic marker writes + counters

- **Task ID:** IN-04
- **Title:** Implement §6.9's marker write/read discipline: monotonic ordering-tagged writes, liveness predicates, reject counters
- **Classification:** MVP normative implementation
- **Purpose:** one staleness guard per mutable input (P3): stale replays cannot poison markers; provider_rejected gains ops-only clearing from the second reject.
- **Prerequisites:** S-02 (columns); shared helper consumed by IN-03, RC-01/RC-02, IN-07.
- **Requirement sections / concepts to read:** §2.1 (both marker blocks + counters + first_at), §6.9 (write AND read rules), §19.3 (ops clear — future; only the counter reset contract matters now).
- **Placeholder components involved:** [Obligation Repository].
- **Local placeholder mappings required before starting:** none new.
- **Local code areas to discover:** none new.
- **How to locate:** n/a.
- **Implementation instructions:** one marker-write helper per marker: overwrite only if new ordering strictly newer than stored marker ordering (stale write → dropped + counted metric); validation_failed_first_at set on not-live→live transition, untouched by re-tags, cleared when marker goes not-live; counters increment per set (validation_reject_count resets when marker clears; provider_reject_count resets only by the future §19.3 ops clear — no auto-reset); liveness predicates per §2.1: validation_failed LIVE iff ordering >= upstream_ordering OR upstream_ordering IS NULL; provider_rejected LIVE iff ordering >= upstream_ordering OR count >= 2.
- **Do not change:** overpay_blocked (deliberately un-gated — §6.9).
- **Tests to add:** monotonic overwrite; stale write dropped+counted; liveness both markers (incl. the anchor clause and the count>=2 persistence against newer messages); first_at semantics (never refreshed by re-tags); counter resets.
- **Edge cases:** marker set by enrichment reject tagged with creating_ordering (§7.3) vs by message validation tagged with message ordering — same helper, caller supplies the tag.
- **Manual validation:** seeded replay sequences.
- **Expected outcome:** marker discipline exact.
- **Failure signs:** any un-tagged marker write.
- **Common mistakes:** re-tagging refreshing first_at (kills the §15 age alert — spec explains).
- **Completion criteria:** tests green.
- **Stop condition:** merged.
- **Next task:** IN-05.

### IN-05 — Feed consumption transaction order

- **Task ID:** IN-05
- **Title:** Rebuild feed consumption to §8's exact order: inbox insert → resolve → evidence CAS → amounts on row-count 1 → re-derive → commit → ack
- **Classification:** MVP normative implementation
- **Purpose:** §8: inbox stops identical redeliveries cheaply BEFORE locks; evidence rules protect the money; offsets commit after DB commit.
- **Prerequisites:** S-04 (inbox), ST-02 (CAS), RG-02/03 (money), IN-04.
- **Requirement sections / concepts to read:** §8 (consumption transaction + layering), §16.2 (ack semantics), §4.4.
- **Placeholder components involved:** [Payment Status Feed Consumer], [Inbox / Processed Event Repository], [Request Status Persistence Layer], [Reservation Repository].
- **Local placeholder mappings required before starting:** consumer + inbox Confirmed.
- **Local code areas to discover:** listener transaction boundary.
- **How to locate:** D-07.
- **Implementation instructions:** per event: (1) INSERT inbox — duplicate key → return (no locks); (2) resolve request (UETR primary; fallback per IN-06); no match → log(event_id, UETR, status)+count+ack+drop; (3) obligation lock → evidence-guarded CAS (IN-07's rules) → amounts on row-count 1 → re-derive; (4) commit, THEN ack.
- **Do not change:** topic/partition setup; evidence rules are IN-07's (this task is the SKELETON order).
- **Tests to add:** duplicate event_id short-circuits pre-lock; concurrent in-flight duplicate (rebalance mid-poll): second blocks on the row lock then duplicate-keys after first commits (§8 explicit test); ack strictly after commit (failure between → redelivery reprocesses safely); unmatched path.
- **Edge cases:** crash after commit before ack → redelivery hits inbox duplicate → clean skip (assert).
- **Manual validation:** local feed run with induced redeliveries.
- **Expected outcome:** §8 skeleton exact.
- **Failure signs:** locks taken before the inbox insert.
- **Common mistakes:** acking in a listener error handler before commit.
- **Completion criteria:** tests green.
- **Stop condition:** merged.
- **Next task:** IN-06.

### IN-06 — Unmatched events + provider_reference fallback

- **Task ID:** IN-06
- **Title:** Implement the fail-closed provider_reference fallback and the unmatched-event policy
- **Classification:** MVP normative implementation
- **Purpose:** §8: reference uniqueness UNCONFIRMED (TL-12) — fallback only on exactly ONE ACTIVE match + amount equality + recency window; zero/multiple → unmatched path; a mis-match is a double-pay or an unguarded reject.
- **Prerequisites:** IN-05; U-02.
- **Requirement sections / concepts to read:** §8 (fallback block + rationale), §16.6 (recency-window config).
- **Placeholder components involved:** [Payment Status Feed Consumer].
- **Local placeholder mappings required before starting:** IN-05 skeleton.
- **Local code areas to discover:** none new.
- **How to locate:** n/a.
- **Implementation instructions:** resolution: UETR match first; else provider_reference candidates filtered to ACTIVE + amount-equal + within recency window; exactly one → use; zero/multiple → unmatched (log + metric + ack — §9 recovers by key); NO durable record, NO replay (decided — §8); no parked-event table (SPEC_CONFLICT trap).
- **Do not change:** the decided no-parked-event stance.
- **Tests to add:** single-match fallback works; two candidates → unmatched; amount-unequal → unmatched; outside recency → unmatched; unmatched logged+counted+acked.
- **Edge cases:** §5.2 replay-window CRITICAL exception for unmatched events is POST-MVP (runbook-time); leave a named hook comment only if trivial, else nothing.
- **Manual validation:** seeded fallback scenarios.
- **Expected outcome:** fail-closed matching.
- **Failure signs:** fuzzy matching creep (business_id or amount-only matching — forbidden).
- **Common mistakes:** counting terminal rows as candidates (ACTIVE only).
- **Completion criteria:** tests green.
- **Stop condition:** merged.
- **Next task:** IN-07.

### IN-07 — Evidence application rules

- **Task ID:** IN-07
- **Title:** Implement §4.4/§10.1 evidence application: terminal → any ACTIVE row; intermediate → non-CLAIMED only; stale → zero rows
- **Classification:** MVP normative implementation
- **Purpose:** the correctness layer money is protected by (§8 layering); shared by feed (here) and resolver (RC-06).
- **Prerequisites:** IN-05 skeleton; RG-03 settlement helper; RG-05 guard; IN-04 markers; CA-2 (status ranks).
- **Requirement sections / concepts to read:** §4.4, §10.1 (terminal-evidence + mirror rules), §8 (marker totality, negative handling), §9.4.
- **Placeholder components involved:** [Payment Status Feed Consumer], [Request Status Persistence Layer].
- **Local placeholder mappings required before starting:** CA-2 rank mapping available.
- **Local code areas to discover:** none new.
- **How to locate:** n/a.
- **Implementation instructions:** one evidence-application helper (feed + resolver share it): TERMINAL settlement → RG-03 helper (any active row; sets SUBMITTED, L4); TERMINAL reject → outcome=REJECTED CAS + evidence flag (S-06) + provider_rejected marker in the same transaction (totality — §8) + release (RG-02); INTERMEDIATE acceptance → non-CLAIMED rows only: SUB=SUBMITTED, stage=CONFIRM, stage_state=READY (clear next_retry_at), BLOCKED preserved (CONFIRM·BLOCKED legal, L5); CLAIMED → no-op; anything stale/duplicate → CAS row-count 0 → ignored; NEW event_id + zero-row CAS on a TERMINAL row → CRITICAL anomaly alert (§8); return/refund-style event for EXECUTED → log + CRITICAL + ack, NO state change (§19.2 context).
- **Do not change:** ui_step_status (derivation only — §4.1).
- **Tests to add:** each rule; feed-races-own-response (both orders — second affects 0 rows, §8/§10.1 mirror); reject sets marker in same transaction; anomaly alert on terminal-row settlement; BLOCKED preserved on acceptance.
- **Edge cases:** settlement on a BLOCKED row → EXECUTED + confirmed (late feed settlement row, §10.5).
- **Manual validation:** seeded event sequences.
- **Expected outcome:** evidence machinery correct + shared.
- **Failure signs:** evidence rules weakened "because the inbox dedups" (§8 forbids).
- **Common mistakes:** applying intermediate evidence to CLAIMED rows; forgetting SUBMITTED-tightening on settlement.
- **Completion criteria:** tests green.
- **Stop condition:** merged.
- **Next task:** IN-08.

### IN-08 — Amount-mismatch park + anomaly wiring

- **Task ID:** IN-08
- **Title:** Wire the §8 AMOUNT_MISMATCH park (BLOCKED + SUBMITTED tightening + CRITICAL) into the evidence helper
- **Classification:** MVP normative implementation
- **Purpose:** all-or-nothing is a confirmed contract fact — a mismatch is DEFECT evidence; confirmed_amount must not move.
- **Prerequisites:** RG-03 (mismatch branch exists), IN-07 (helper).
- **Requirement sections / concepts to read:** §8 (mismatch block), §16.4 (no tolerance), §13 (AMOUNT_MISMATCH CRITICAL).
- **Placeholder components involved:** [Payment Status Feed Consumer], [Metrics / Alerting Layer].
- **Local placeholder mappings required before starting:** IN-07 done.
- **Local code areas to discover:** none new.
- **How to locate:** n/a.
- **Implementation instructions:** confirm the RG-03 mismatch branch is reachable from feed evidence: park same-stage BLOCKED(AMOUNT_MISMATCH), SUB=SUBMITTED, CRITICAL alert; resolution is EXTERNAL (corrected engine event completes normally; platform-side dispute — §19.2 family); no settle-at-actual-amount operation (rejected by design).
- **Do not change:** I2's definition.
- **Tests to add:** mismatch on MAYBE row (off the MAYBE clocks after park — maybe_since cleared? NO: submission tightens to SUBMITTED, maybe_since clears with the submission change per ST-07 — assert); corrected event later completes the row normally.
- **Edge cases:** mismatch event redelivered → inbox short-circuit; re-keyed duplicate → zero-row CAS.
- **Manual validation:** seeded mismatch.
- **Expected outcome:** defect path exact.
- **Failure signs:** confirmed moving on mismatch.
- **Common mistakes:** treating the park as retryable.
- **Completion criteria:** tests green.
- **Stop condition:** merged.
- **Next task:** IN-09.

### IN-09 — Kafka consumer hardening

- **Task ID:** IN-09
- **Title:** Bring both consumers to §16.2: manual ack after commit, earliest, ErrorHandlingDeserializer, DLT-for-poison-only, no retry topics, keying, poll sizing, retention-chain check
- **Classification:** MVP normative implementation
- **Purpose:** §16.2 line by line; D-07's gap checklist is the work list.
- **Prerequisites:** D-07 checklist; IN-05 (transaction order in place).
- **Requirement sections / concepts to read:** §16.2 (whole).
- **Placeholder components involved:** [Payment Status Feed Consumer], upstream consumer, [Metrics / Alerting Layer] (DLT depth, lag).
- **Local placeholder mappings required before starting:** consumer configs mapped.
- **Local code areas to discover:** container factory / properties per environment.
- **How to locate:** D-07.
- **Implementation instructions:** per checklist gap: enable-auto-commit=false + record ack-mode + offsets after DB commit; auto-offset-reset=earliest; ErrorHandlingDeserializer wrapping; DLT only for poison (deserialization/semantic validation) — transient infra retries IN PLACE or pauses the container; remove/forbid @RetryableTopic on money events; verify partition keying (feed by UETR, upstream by business_id — if a topic is not usefully keyed: concurrency 1 per partition, record); max.poll.interval sized for worst-case lock contention, small max-poll-records; scheduled retention-chain check (broker retention vs required window → alert; owner per §16.2).
- **Do not change:** broker-side config (another team's — the CHECK exists because it can change without notice).
- **Tests to add:** poison pill → DLT, consumer keeps running; transient DB error → in-place retry/pause, NOT DLT; offset committed only post-commit (crash test).
- **Edge cases:** consumer-group changes replaying history → inbox + evidence absorb (assert via replay test).
- **Manual validation:** config review against §16.2 per environment profile.
- **Expected outcome:** consumers production-hard.
- **Failure signs:** DLT depth used as a retry queue.
- **Common mistakes:** 'latest' reset surviving in some profile (silently skips money events).
- **Completion criteria:** checklist all compliant; tests green.
- **Stop condition:** merged; Phase P9 report.
- **Next task:** RC-01.

## H-Phase 10 — Retry / recovery / MAYBE_SUBMITTED (P10)

### RC-01 — POST-failure classifier

- **Task ID:** RC-01
- **Title:** Implement the closed classifier from CA-1: cause → (category, code, retryable, severity, submission_state, target dimensions); fail closed
- **Classification:** MVP normative implementation
- **Purpose:** §7.0/§7.2: unmapped mid-call → MAYBE·CONFIRM·READY; unmapped engine code → MAYBE·BLOCKED(UNMAPPED_CODE); never "assume retryable"; HTTP 200 classified from the body.
- **Prerequisites:** CA-1 published; D-05 branch inventory.
- **Requirement sections / concepts to read:** §7.2 (whole), §7.3, §7.1, CA-1.
- **Placeholder components involved:** [Provider Response Parser].
- **Local placeholder mappings required before starting:** parser mapped.
- **Local code areas to discover:** existing branch code (to be routed through the classifier).
- **How to locate:** D-05.
- **Implementation instructions:** classifier as data-driven mapping loaded from CA-1's table (externalized so a table version bump is config, not code — §16.6); inputs: transport outcome (connect-fail / read-timeout / reset-after-write / crash marker) + HTTP body/code; outputs per CA-1; defaults per §7.2's two fail-closed rows; enrichment outcomes route via the same taxonomy (§7.3 rows).
- **Do not change:** business-data extraction on success.
- **Tests to add:** fixture per CA-1 row; the fail-closed defaults; 200-with-error-body classified from body; §7.3 rows.
- **Edge cases:** replay-original-response class (from CT-02's finding) classified deliberately per CA-1.
- **Manual validation:** classifier coverage report vs CA-1 row count.
- **Expected outcome:** closed taxonomy live.
- **Failure signs:** a default-retryable catch-all anywhere.
- **Common mistakes:** classifying DUPLICATE_REQUEST as an error (it is MAYBE + query — §7.2).
- **Completion criteria:** tests green.
- **Stop condition:** merged.
- **Next task:** RC-02.

### RC-02 — Response-driven state application

- **Task ID:** RC-02
- **Title:** Apply classifier outputs as §7.2's exact tuple transitions, incl. the collision branch on divergence_expected and UETR/marker side effects
- **Classification:** MVP normative implementation
- **Purpose:** the POST·CLAIMED → next-state map, exactly per §7.2/§10.5 rows.
- **Prerequisites:** RC-01; ST-02/ST-06/ST-07; U-01; IN-04 (markers); RG-02 (release on sync definitive rejects).
- **Requirement sections / concepts to read:** §7.2 (every row), §10.5 (POST rows), §2.2 (divergent_payload_at write-once), §7.1.
- **Placeholder components involved:** [Provider Response Parser], [Request Status Persistence Layer].
- **Local placeholder mappings required before starting:** RC-01 outputs wired.
- **Local code areas to discover:** response-processing transaction.
- **How to locate:** D-05.
- **Implementation instructions:** per class: connect-fail → NOT_SUBMITTED, POST·RETRY_WAIT; ambiguous (read timeout/reset/crash) → MAYBE, CONFIRM·READY (+maybe_since); sync accepted → SUBMITTED (+submitted_at), CONFIRM·READY; DUPLICATE_REQUEST → MAYBE, CONFIRM·READY + schedule query; NO uetr write; collision → set divergent_payload_at (write-once, same transaction); branch divergence_expected: TRUE → MAYBE, CONFIRM·READY + query (no park, no CRITICAL); FALSE → MAYBE, POST·BLOCKED(ENGINE_INCONSISTENCY) + CRITICAL; NO uetr write; unmapped engine code → MAYBE, POST·BLOCKED(UNMAPPED_CODE) + alert; sync business reject (insufficient balance class) → NOT_SUBMITTED, POST·RETRY_WAIT (policy) or BLOCKED(OPS_PARKED) per CA-1; sync definitive invalid-data → outcome=REJECTED + validation_failed marker + release; other definitive → outcome=REJECTED + provider_rejected marker + release. All via evidence-mirror-guarded CAS (late response vs already-terminal row → 0 rows).
- **Do not change:** classifier internals (RC-01).
- **Tests to add:** one test per row above; mirror-rule (late accepted vs EXECUTED row); collision write-once (second collision doesn't overwrite the timestamp); marker totality on both reject flavors (exactly one marker).
- **Edge cases:** DR-replay row (no prior hash → flag false) collision → ANOMALOUS branch (ties K-05's edge case).
- **Manual validation:** stub-driven run of every class.
- **Expected outcome:** POST outcomes exact.
- **Failure signs:** any response path bypassing the CAS helper.
- **Common mistakes:** releasing on the business-reject RETRY_WAIT path (NOT_SUBMITTED but still active — no release until terminal).
- **Completion criteria:** tests green.
- **Stop condition:** merged.
- **Next task:** RC-03.

### RC-03 — repost_permitted gate

- **Task ID:** RC-03
- **Title:** Implement §7.0's repost_permitted as ONE function, checked by every POST-routing writer AND by the posting claim
- **Classification:** MVP normative implementation
- **Purpose:** the single normative re-POST gate; both-ends checking kills the park⇄un-park livelock class structurally; blocked_reason plays NO part.
- **Prerequisites:** RC-02 (divergent_payload_at written); B-03 (cutoff source — else the cutoff term reads from a stub that returns BLOCKED/fail-closed and the task is PARTIALLY BLOCKED, record); RC-09 (freeze check — can stub as FROZEN-safe until RC-09 lands).
- **Requirement sections / concepts to read:** §7.0 (predicate + both-ends + override), §6.4 (staleness term), §11 (claim carries the durable term).
- **Placeholder components involved:** [Request Status Persistence Layer], [Retry Resolver Job], [Status Query Resolver], [Provider POST Client].
- **Local placeholder mappings required before starting:** claim CAS site; POST-routing writers list (ops actions later).
- **Local code areas to discover:** none new.
- **How to locate:** ST-09/K-04 sites.
- **Implementation instructions:** repost_permitted(request) = divergent_payload_at IS NULL ∧ now < cutoff ∧ NOT(amount stale vs current shortfall ∧ MAYBE_SUBMITTED) ∧ freeze OFF ∧ outcome IS NULL; called by: §9.2 downgrade writer (RC-07), ops re-POST writers (OP scope + §10.5 ops rows), retry scanner pre-claim (RC-04); AND the posting-claim CAS carries divergent_payload_at IS NULL in its WHERE + re-checks derived terms pre-launch (K-04/ST-09 site); dual-control override wired to override ONLY the staleness term (consumed by the future ops action; expose the parameter, no UI).
- **Do not change:** the term list except CT-04's recorded TTL consequence (if that follow-up exists, add the TTL term HERE with its own test).
- **Tests to add:** term-by-term falsification; both-ends test (a laundered blocked_reason cannot enable a forbidden re-POST — writer refuses AND claim hits row-count 0/refuses); override overrides staleness ONLY.
- **Edge cases:** amount staleness evaluated under the obligation lock (shortfall is I5 — lock-bound).
- **Manual validation:** trace one denied and one permitted re-POST.
- **Expected outcome:** one gate, two doors.
- **Failure signs:** any writer consulting blocked_reason for re-POST decisions (§10.1).
- **Common mistakes:** caching an "unfrozen" freeze answer (§16.1 — only FROZEN may be cached).
- **Completion criteria:** tests green.
- **Stop condition:** merged.
- **Next task:** RC-04.

### RC-04 — Retry scanner + policy

- **Task ID:** RC-04
- **Title:** Retry scanner per §7.4: per-error-class policy from config, exhaustion → BLOCKED, cutoff pre-checks, downgrade policy class, freeze/breaker deadline suspension
- **Classification:** MVP normative implementation
- **Purpose:** exactly one retry owner (the DB scanner); §16.1's suspension prevents an outage from converting the RETRY_WAIT population to BLOCKED.
- **Prerequisites:** ST-09 (claims), RC-03 (gate), RC-01/02 (classification + application), B-03 (cutoff config — else BLOCKED for the cutoff term's real values, fail-blocked stub meanwhile).
- **Requirement sections / concepts to read:** §7.4 (whole incl. downgrade class), §16.1 (scanner rules, suspension, poison cap), §16.6 (config entries).
- **Placeholder components involved:** [Retry Resolver Job], [Metrics / Alerting Layer].
- **Local placeholder mappings required before starting:** job infra; S-07 index expressions (queries must match).
- **Local code areas to discover:** in-process retry wrappers on the POST path (from D-05 — REMOVE them here, the single-owner rule).
- **How to locate:** D-05/D-08 inventories.
- **Implementation instructions:** scanner: breaker-gated batch claim (SKIP LOCKED, bounded, jittered backoff); per row: cutoff + deadline pre-checks (violation → BLOCKED(CUTOFF_EXPIRED)); repost_permitted for POST-stage rows (retry-guard branch per RG-07); execute stage work; failure → policy: next_retry_at per class config (base/multiplier/max/cutoff), attempt_count++; exhaustion → BLOCKED(RETRY_EXHAUSTED) (+ MAYBE rows keep submission_state — stay in resolver scope, maybe_since keeps running — §7.4); downgrade policy class: next_retry_at=now, attempt_count RESET, small max (config 2–3), deadline ≤ cutoff; suspension: while freeze effective or breaker OPEN → zero attempts AND attempt/deadline budget frozen (cutoff checks still apply at attempt time); poison-row cap → BLOCKED + alert; remove stacked in-process retries on the POST.
- **Do not change:** enrichment micro-retries for idempotent reads (§16.1 permits those).
- **Tests to add:** policy schedule math; exhaustion → BLOCKED with MAYBE preserved; cutoff pre-check; suspension (6-hour simulated outage consumes no budget — the §16.1 scenario); poison cap; single-owner (no nested retry on POST — structural assert/test where feasible).
- **Edge cases:** downgrade-class rows re-posting immediately (next_retry_at=now) — L7 satisfied by the explicit write (§9.2).
- **Manual validation:** seeded RETRY_WAIT population through a scripted breaker-OPEN window.
- **Expected outcome:** one disciplined retry owner.
- **Failure signs:** wall-clock consuming budget during suspension.
- **Common mistakes:** counting business rejects as breaker failures (§16.1 — they are successes to the breaker).
- **Completion criteria:** tests green.
- **Stop condition:** merged.
- **Next task:** RC-05.

### RC-05 — Resolver sweep scope + shaping

- **Task ID:** RC-05
- **Title:** Build the §9.5 resolver sweep: submission-keyed scope, bounded prioritized batches, per-row next_query_at backoff, never-overlap, SUBMITTED damping
- **Classification:** MVP normative implementation
- **Purpose:** §9.5: scope keyed on submission_state + outcome ONLY (stage/history scoping explicitly rejected); post-outage herds shaped under the engine's rate limit.
- **Prerequisites:** ST-07 (anchors), S-07 (indexes), CA-3 (mapping — application is RC-06), TL-13 answer (budget value — config; stub conservative if pending, record).
- **Requirement sections / concepts to read:** §9.5 (whole), §9 (intro), §16.6 (cadence/budget/backoff/damping entries).
- **Placeholder components involved:** [Status Query Resolver], [Metrics / Alerting Layer].
- **Local placeholder mappings required before starting:** query client (D-06); job infra.
- **Local code areas to discover:** none new.
- **How to locate:** D-06/D-08.
- **Implementation instructions:** scope: ACTIVE ∧ MAYBE (any stage/stage_state incl. BLOCKED) ∪ ACTIVE ∧ SUBMITTED older than confirmation age (incl. BLOCKED); order: nearest cutoff first, then oldest maybe_since; per-sweep query budget from config (rate-limit-derived); per-row next_query_at with backoff; a sweep overrun → §15 metric, next sweep waits (never overlap — single-flight guard); SUBMITTED branch damps while feed-lag metric exceeds confirmation age; MAYBE branch never damps; ops-triggered mode: query an explicit key set regardless of state (§5.2 step 5's executor — the MODE exists at MVP as a callable entry point; the runbook that uses it is post-MVP).
- **Do not change:** evidence application (RC-06).
- **Tests to add:** scope inclusion/exclusion table (BLOCKED·MAYBE in; terminal out; CONFIRM·BLOCKED·SUBMITTED in past age); ordering; budget respected over a seeded 1000-row herd; overlap prevented; damping on/off; backoff progression.
- **Edge cases:** rows with next_query_at in the future skipped within scope (assert).
- **Manual validation:** seeded herd run with a throttled stub.
- **Expected outcome:** disciplined sweep.
- **Failure signs:** scope containing stage conditions (the recurring four-round lesson — §9.5).
- **Common mistakes:** damping the MAYBE branch.
- **Completion criteria:** tests green.
- **Stop condition:** merged.
- **Next task:** RC-06.

### RC-06 — Query-outcome application

- **Task ID:** RC-06
- **Title:** Apply §9.1 outcomes through the shared evidence helper: EXECUTED/REJECTED/INDETERMINATE/ACCEPTED (+ NOT_FOUND routed to RC-07)
- **Classification:** MVP normative implementation
- **Purpose:** ask, never blind-retry; both resolver and feed converge on the same CAS (race-safe by construction, §9.4).
- **Prerequisites:** RC-05 (sweep), IN-07 (shared helper), CA-3.
- **Requirement sections / concepts to read:** §9.1, §9.4 (race safety), §4.4.
- **Placeholder components involved:** [Status Query Resolver], [Request Status Persistence Layer].
- **Local placeholder mappings required before starting:** IN-07 helper.
- **Local code areas to discover:** none new.
- **How to locate:** n/a.
- **Implementation instructions:** EXECUTED → settlement via the helper (amount equality; RG-03); REJECTED → authoritative negative via the helper (outcome, marker, release, evidence flag); ACCEPTED/in-flight → intermediate evidence rules (no-op on CLAIMED; else SUBMITTED + CONFIRM·READY, BLOCKED preserved) + reschedule; INDETERMINATE (incl. query failure/timeout) → reschedule with backoff; escalation clocks keep running through query outages (assert — nothing pauses maybe_since); NOT_FOUND → RC-07's rule.
- **Do not change:** the helper (shared).
- **Tests to add:** each outcome; resolver-vs-feed race both orders (second lands 0 rows); query outage → INDETERMINATE + clocks unaffected.
- **Edge cases:** REJECTED answer for a SUBMITTED row — authoritative negative applies (release rights: engine negative is the sanctioned driver — §10.1).
- **Manual validation:** stub-driven outcomes.
- **Expected outcome:** recovery independent of feed delivery (§9.5's point).
- **Failure signs:** resolver writing anything not via the helper.
- **Common mistakes:** treating INDETERMINATE as NOT_FOUND.
- **Completion criteria:** tests green.
- **Stop condition:** merged.
- **Next task:** RC-07.

### RC-07 — Trust-age rule + §9.2 downgrade

- **Task ID:** RC-07
- **Title:** Implement NOT_FOUND trust-age semantics: MAYBE downgrade (the one sanctioned backward move) gated by repost_permitted; SUBMITTED → ENGINE_INCONSISTENCY park; resolver-applied deferred AMENDMENT_PARKED
- **Classification:** MVP normative implementation
- **Purpose:** §9.2 exactly: the self-heal for engine ingest outages; a row is never un-parked for an action the next gate would forbid.
- **Prerequisites:** RC-06; RC-03 (gate); RC-04 (downgrade policy class); ST-07 (anchors); NOT_FOUND_TRUST_AGE config (TL-5 — conservative stub if pending, record).
- **Requirement sections / concepts to read:** §9.2 (whole, incl. the recorded rationale + accepted consequence), §7.4 (downgrade class), §10.5 (downgrade + SUBMITTED rows).
- **Placeholder components involved:** [Status Query Resolver], [Request Status Persistence Layer].
- **Local placeholder mappings required before starting:** RC-03/RC-04 in place.
- **Local code areas to discover:** none new.
- **How to locate:** n/a.
- **Implementation instructions:** NOT_FOUND handler: age from last_post_attempt_at (MAYBE) / submitted_at (SUBMITTED) — NEVER state_changed_at; before trust-age → INDETERMINATE; after: MAYBE (any stage/stage_state) + repost_permitted → CAS to stage=POST, stage_state=RETRY_WAIT, next_retry_at=now, attempt_count reset (downgrade class), blocked_reason cleared, SUB stays MAYBE; MAYBE + gate fails → stay parked (wait-then-decide); if the failing term is amount staleness and the row is NOT already parked → resolver applies AMENDMENT_PARKED itself (idempotent, non-CLAIMED only — the deferred §6.4 park always lands); SUBMITTED + NOT_FOUND after trust-age → CONFIRM·BLOCKED(ENGINE_INCONSISTENCY) + CRITICAL, single answer suffices (no counter column exists — deliberate), row STAYS in resolver scope; downgrade's own POST response settles SUB per §9.2's list.
- **Do not change:** §9.4 (a query answer NEVER releases).
- **Tests to add:** age anchors (attempt restarts MAYBE clock; churn doesn't); pre-trust-age NOT_FOUND → reschedule only; permitted downgrade full tuple (add the pending ST-03 legality case: the sanctioned backward move); gate-fail stays parked (each failing term); deferred-park application; SUBMITTED park + reversibility (next successful query resolves it); downgraded row claimable by RC-04 immediately; escalated row downgrade doesn't cycle (with RC-08 — pending case there).
- **Edge cases:** cutoff-expired aged rows never downgraded (the §9.2 lookback guard — cutoff term); DUPLICATE_REQUEST answering the downgrade re-POST → MAYBE + query (hidden earlier attempt surfaced; prior uetr intact — the §16.6-6 named test).
- **Manual validation:** scripted ingest-outage simulation: population NOT_FOUND → downgrades fire only where permitted.
- **Expected outcome:** self-healing MAYBE machinery, livelock-free.
- **Failure signs:** downgrade firing on a blocked_reason condition (must be gate-only).
- **Common mistakes:** measuring MAYBE age from maybe_since for the TRUST-AGE (it is last_post_attempt_at — maybe_since drives ESCALATION, a different clock; §2.2 explains both).
- **Completion criteria:** tests green.
- **Stop condition:** merged.
- **Next task:** RC-08.

### RC-08 — Escalation scanner

- **Task ID:** RC-08
- **Title:** §9.3 escalation on the maybe_since clock: once per episode (escalated_at gate), non-CLAIMED writes, already-BLOCKED alert-only, tier-2 re-page
- **Classification:** MVP normative implementation
- **Purpose:** bounded human hand-off for unresolved MAYBE rows, early enough to act before cutoff; never a downgrade⇄escalate cycle.
- **Prerequisites:** ST-07 (anchors + escalated_at contract), RC-07 (downgrade interplay), config (escalation age PO-3; tier-2; cutoff margin).
- **Requirement sections / concepts to read:** §9.3 (whole), §2.2 (escalated_at), §13 (ESCALATED code), §16.6 (ordering validation).
- **Placeholder components involved:** [Retry Resolver Job] family (scanner), [Metrics / Alerting Layer].
- **Local placeholder mappings required before starting:** scanner infra; S-07 escalation index.
- **Local code areas to discover:** none new.
- **How to locate:** n/a.
- **Implementation instructions:** scanner scope: outcome IS NULL ∧ MAYBE ∧ maybe_since older than threshold; per row: CRITICAL alert ALWAYS; state write CAS to same-stage BLOCKED(ESCALATED) + escalated_at, gated on escalated_at IS NULL ∧ non-CLAIMED ∧ non-BLOCKED (already-BLOCKED → alert only, NO overwrite — §9.3/§10.1; CLAIMED → alert, write deferred); tier-2 threshold on the same clock → re-page/incident; escalated_at cleared with maybe_since (ST-07).
- **Do not change:** blocked_reason of already-BLOCKED rows (never overwritten — §10.1).
- **Tests to add:** once-per-episode (downgrade un-parks an ESCALATED row with elapsed maybe_since → no immediate re-park — the §9.3 cycle gate); already-BLOCKED → alert-only; CLAIMED deferred; tier-2 fires; frozen rows excluded (outcome term + cleared maybe_since, belt and braces — §2.2).
- **Edge cases:** escalation racing the resolver's settle — CAS decides; alert may fire once redundantly (harmless; note in runbook).
- **Manual validation:** seeded aging rows through both tiers.
- **Expected outcome:** bounded, non-cycling escalation.
- **Failure signs:** age keyed on state_changed_at (churn re-arms — the exact §15 discipline violation).
- **Common mistakes:** overwriting OPS_PARKED with ESCALATED.
- **Completion criteria:** tests green.
- **Stop condition:** merged.
- **Next task:** RC-09.

### RC-09 — Posting freeze check (Hazelcast)

- **Task ID:** RC-09
- **Title:** Fail-safe posting-freeze check: absent/unreachable/timeout = FROZEN; only FROZEN cached; reason/operator/ticket on the toggle; checked before every claim and POST
- **Classification:** MVP normative implementation
- **Purpose:** §16.1: a DB restore cannot un-freeze posting (toggle outside the DB); a grid outage pauses payments (fail-blocked, PO signed off).
- **Prerequisites:** D-10 (Hazelcast topology + existing toggle shape); RC-03 (consumes the answer).
- **Requirement sections / concepts to read:** §16.1 (freeze block, incl. cache rule, propagation bound, silent-by-design note), §15 (freeze-effective page — OB-05 wires it).
- **Placeholder components involved:** [Provider POST Client], [Retry Resolver Job], Hazelcast client.
- **Local placeholder mappings required before starting:** Hazelcast client Confirmed; existing role-controlled toggle located or created per local convention (record).
- **Local code areas to discover:** grid access patterns.
- **How to locate:** D-10.
- **Implementation instructions:** freeze read with bounded timeout; toggle absent OR grid unreachable OR timeout → FROZEN; only the FROZEN answer cacheable; "unfrozen" always re-read; toggle payload carries reason/operator/ticket id; checked before every posting claim AND before every POST (both ends — §16.1); kill-switch semantics: stops POSTs ONLY — feed consumption, §9 queries, card reads continue (§16.1); expose freeze-effective state as a metric for OB-05's page.
- **Do not change:** toggle role control (exists operationally — §16.1 operational fact).
- **Tests to add:** all three fail-safe conditions read FROZEN; unfrozen never cached (two reads hit the grid); frozen blocks claim and POST; resolver/feed/reads unaffected while frozen.
- **Edge cases:** flip mid-flight: in-flight POST completes (drain semantics §11/§16.1) — assert no interruption machinery exists.
- **Manual validation:** local grid kill → posting stops, resolver continues.
- **Expected outcome:** fail-safe freeze.
- **Failure signs:** a TTL cache on "unfrozen" (§16.1 names this violation).
- **Common mistakes:** freeze check inside the claim transaction only (must also guard the wire call).
- **Completion criteria:** tests green.
- **Stop condition:** merged.
- **Next task:** RC-10.

### RC-10 — Breaker + budget-suspension integration

- **Task ID:** RC-10
- **Title:** Circuit breaker per dependency (business rejects = successes); scanner gating; freeze/breaker suspension of attempt/deadline budgets wired end to end
- **Classification:** MVP normative implementation
- **Purpose:** §16.1: an outage becomes quiet waiting; a 6-hour engine outage must not flood the ops queue at recovery.
- **Prerequisites:** RC-04 (suspension hooks), RC-09 (freeze state).
- **Requirement sections / concepts to read:** §16.1 (breaker + suspension + bulkheads + timeouts), §16.6 (thresholds config).
- **Placeholder components involved:** [Provider POST Client], [Retry Resolver Job], [Status Query Resolver], [Metrics / Alerting Layer].
- **Local placeholder mappings required before starting:** breaker library/conventions (D-10).
- **Local code areas to discover:** existing breaker/timeouts per dependency.
- **How to locate:** D-05/D-10.
- **Implementation instructions:** per-dependency breakers (enrichment, account service, engine POST, status-query API) with explicit timeout budgets (config §16.6); business rejects recorded as successes; scanners gate on breaker state pre-claim; suspension: freeze-effective OR breaker-OPEN windows do not consume attempt/deadline budget (RC-04's mechanism — verify end to end here); bulkhead check: posting, enrichment, card-read pools separate (record local reality; if shared, this is a change task — bounded queues, DB as the real queue).
- **Do not change:** breaker library choice (local convention).
- **Tests to add:** breaker opens on transport failures only; scanner claims zero while OPEN; budget frozen across an OPEN window (extends RC-04's test through the real breaker); query-API breaker → INDETERMINATE handling (RC-06) not NOT_FOUND.
- **Edge cases:** breaker half-open probes are attempts (consume budget normally — only OPEN suspends).
- **Manual validation:** scripted outage rehearsal.
- **Expected outcome:** outage-shaped behavior per spec.
- **Failure signs:** RETRY_WAIT population converting to BLOCKED during a simulated outage.
- **Common mistakes:** one global breaker for all dependencies.
- **Completion criteria:** tests green.
- **Stop condition:** merged; Phase P10 report. NOTE: production ENABLEMENT of the §9.2 auto-downgrade remains gated on P8 PASS (Section M order).
- **Next task:** OP-01.

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

## H-Phase 12 — Drift, reconciliation (P12)

### OB-01 — Drift scanner

- **Task ID:** OB-01
- **Title:** Scheduled drift scan: recompute I1/I2 from a consistent snapshot; re-check mismatches under the obligation lock; PAGE on confirmed mismatch; verify L9
- **Classification:** MVP normative implementation
- **Purpose:** §3: the stored counter is a deliberate tripwire; the scanner is what makes the redundancy pay.
- **Prerequisites:** RG-01..03 live; S-07 (drift index); D-10 (paging integration).
- **Requirement sections / concepts to read:** §3 (drift block + invariants), §10.3 (L9), §15 (drift page).
- **Placeholder components involved:** [Reconciliation / Drift Scanner], [Metrics / Alerting Layer].
- **Local placeholder mappings required before starting:** flashback/SCN availability confirmed (P12 phase note — else the consistent-read strategy is UNCLEAR: ask the DBA; do not fake it with plain reads).
- **Local code areas to discover:** job infra.
- **How to locate:** D-08.
- **Implementation instructions:** per obligation (batched): snapshot-read (SCN/flashback) committed/confirmed vs Σ per I1/I2; mismatch → re-check UNDER the obligation lock; still mismatched → PAGE (not log); L9 check: outcome=REJECTED rows have exactly one marker set (cross-table — code check, the drift job is its verifier per §10.3); metrics per run.
- **Do not change:** counters (the scanner READS; corrections are the future manual-adjustment op — §19.2).
- **Tests to add:** seeded I1 violation pages; seeded I2 violation pages; read-skew scenario does NOT page (uncommitted concurrent create); L9 violation detected.
- **Edge cases:** obligations mid-transaction during the sweep — the locked re-check absorbs them.
- **Manual validation:** corrupt a counter in a test env → page arrives.
- **Expected outcome:** money-math tripwire live.
- **Failure signs:** paging without the locked re-check (false pages erode trust).
- **Common mistakes:** scanning with a plain read and calling it a snapshot.
- **Completion criteria:** tests green; page route confirmed.
- **Stop condition:** merged.
- **Next task:** OB-02.

### OB-02 — Reconciliation tripwires

- **Task ID:** OB-02
- **Title:** Wire the anomaly tripwires: evidence-for-terminal CRITICAL, per-obligation request-count sanity, card multi-row alert
- **Classification:** MVP normative implementation
- **Purpose:** §8's anomaly disambiguation + §15's tripwire entries; the §5.2 replay-divergence tripwire is the same alert (post-MVP runbook consumes it).
- **Prerequisites:** IN-07 (zero-row CAS detection point), RG-08/§12 read path.
- **Requirement sections / concepts to read:** §8 (anomaly rules), §15 (entries), §12 (defensive rule).
- **Placeholder components involved:** [Payment Status Feed Consumer], [Metrics / Alerting Layer], card read path.
- **Local placeholder mappings required before starting:** IN-07 in place.
- **Local code areas to discover:** card lookup site.
- **How to locate:** read-surface mapping (TL-2-adjacent; local).
- **Implementation instructions:** evidence-for-terminal: NEW event_id + zero-row CAS against a TERMINAL row → CRITICAL (already hooked in IN-07 — verify + alert-route here); per-obligation request count over sanity threshold → ticket (§15); card lookup returning >1 obligation → error state + alert (§12 defensive rule — never silently pick one).
- **Do not change:** benign-redelivery silent skip (KNOWN event_id — §8).
- **Tests to add:** each tripwire fires on its seeded condition; benign redelivery does NOT fire.
- **Edge cases:** provider-side-count vs local EXECUTED comparison (Section N lists it) requires engine-side data — mark MUST_VERIFY_LOCALLY whether any engine report/API supports it; if not, record as unavailable (the §15 list does not mandate it; Section N flags it as conditional).
- **Manual validation:** seeded runs.
- **Expected outcome:** tripwires live.
- **Failure signs:** CRITICALs routed to a quiet channel.
- **Common mistakes:** aggregating the terminal-evidence CRITICAL into volume metrics (it is per-event CRITICAL, §8).
- **Completion criteria:** tests green.
- **Stop condition:** merged.
- **Next task:** OB-03.

## H-Phase 13 — Observability (P13)

### OB-03 — Money + MAYBE alert set

- **Task ID:** OB-03
- **Title:** Implement the §15 money-critical alerts: MAYBE ages, stuck reservation, marker alerts, latch alerts, mismatch/inconsistency CRITICALs, procedure-use alert
- **Classification:** MVP normative implementation
- **Purpose:** the money-facing half of §15, on the correct clocks.
- **Prerequisites:** ST-07 (anchors), IN-04 (counters), RG-04 (latch hook), RC-08 (escalation events), OP-01 (procedure alert hook).
- **Requirement sections / concepts to read:** §15 (list + clock discipline), §13 (severities), §2.1/§2.2 (anchor columns).
- **Placeholder components involved:** [Metrics / Alerting Layer].
- **Local placeholder mappings required before starting:** metrics stack conventions (D-10).
- **Local code areas to discover:** none new.
- **How to locate:** n/a.
- **Implementation instructions:** implement, each on its named clock/scope: oldest-MAYBE age (maybe_since) alert before cutoff; MAYBE tier-2 re-page; stuck-reservation age; BLOCKED count+age by blocked_reason (queue metric — display use of the reason is legal, §10.1); provider_rejected set → alert; provider_reject_count=2 → alert; validation_reject_count=3 → alert; overpay latch SET → alert (business hours); overpay-latched count + oldest age; AMOUNT_MISMATCH CRITICAL; ENGINE_INCONSISTENCY CRITICAL; AMENDMENT_TIE_CONFLICT; AMENDMENT_ON_LATCHED_SCOPE; live-marker-no-active-request age (validation_failed on first_at); apply-platform-verified-outcome executed → alert every use; overpay-latched-without-visible-exception integrity alert.
- **Do not change:** alert semantics/severities (§13/§15).
- **Tests to add:** seeded condition per alert (batchable table-driven test).
- **Edge cases:** none beyond clock discipline (already asserted in ST-07 — here assert the ALERT reads the anchor).
- **Manual validation:** dashboard walkthrough with the ops owner.
- **Expected outcome:** money alerts live.
- **Failure signs:** any age alert reading state_changed_at (final grep).
- **Common mistakes:** de-duplicating the every-use procedure alert.
- **Completion criteria:** tests green; routing confirmed.
- **Stop condition:** merged.
- **Next task:** OB-04.

### OB-04 — Queue/flow/stuck alert set

- **Task ID:** OB-04
- **Title:** Implement the flow-health alerts: unmatched events, stale messages/marker-writes, DLT depth, consumer lag, scanner heartbeats, stuck-state, sweep overrun, watchdogs, card >1, deadlocks
- **Classification:** MVP normative implementation
- **Purpose:** the flow-facing half of §15 incl. the observed-lag watchdog (ingest-lag config wrong) and generic stuck-state split rule.
- **Prerequisites:** IN-05/06 (unmatched metric), IN-02 (stale counter), IN-09 (DLT/lag), RC-05 (overrun metric), RC-07 (watchdog data).
- **Requirement sections / concepts to read:** §15 (entries incl. the stuck-state split note), §12 (freshness indicator), §16.2 (lag SLA).
- **Placeholder components involved:** [Metrics / Alerting Layer], card read path (lag indicator).
- **Local placeholder mappings required before starting:** metric sources wired by prior tasks.
- **Local code areas to discover:** none new.
- **How to locate:** n/a.
- **Implementation instructions:** unmatched feed events (volume alert); stale upstream messages volume; stale-marker-writes volume; Kafka DLT depth > 0 → page; consumer lag per flow → page over SLA + drive the §12 card data-as-of/lag indicator; scanner heartbeat (silent 3× interval → page); generic stuck-state per (stage,stage_state) max ages — split per §15: retry states on retry_deadline_at passed without exhaustion; non-churning states on state_changed_at; resolver sweep overrun (repeat → alert); observed-lag watchdog (feed-confirmed payment that was NOT_FOUND past trust-age → alert); card >1 obligation (OB-02 cross-ref); ORA-00060 deadlock count → ticket; inbox growth vs purge → health metric; metric ABSENCE = bad (dead-gauge alerting per §15 practices).
- **Do not change:** SLA values without owners (config §16.6).
- **Tests to add:** seeded per alert where testable; dead-gauge behavior verified for at least the drift gauge.
- **Edge cases:** duplicate-skip spikes during replays must read healthy on dashboards (§15 practice — dashboard note, not an alert change).
- **Manual validation:** ops walkthrough.
- **Expected outcome:** flow alerts live.
- **Failure signs:** lag alert without the card indicator (§12 couples them).
- **Common mistakes:** stuck-state ages on the wrong clock per the split rule.
- **Completion criteria:** tests green.
- **Stop condition:** merged.
- **Next task:** OB-05.

### OB-05 — Freeze page, breaker tickets, rollup

- **Task ID:** OB-05
- **Title:** Freeze-effective-without-ticket page; breaker OPEN ticket + 30m page; root-cause alert rollup; retention-chain check
- **Classification:** MVP normative implementation
- **Purpose:** §16.1's freeze is silent by design — this page is the ONLY signal; §15 rollup: one outage = one incident, not thousands of pages.
- **Prerequisites:** RC-09 (freeze metric), RC-10 (breaker state), IN-09 (retention check hook).
- **Requirement sections / concepts to read:** §15 (freeze entry + rollup block), §16.1, §16.2 (retention chain).
- **Placeholder components involved:** [Metrics / Alerting Layer].
- **Local placeholder mappings required before starting:** alerting stack supports grouped incidents (verify locally — if not, implement rollup at emission: suppression window keyed on the root-cause condition; record approach).
- **Local code areas to discover:** none new.
- **How to locate:** n/a.
- **Implementation instructions:** page when freeze EFFECTIVE (toggle set OR grid unreachable) without an acknowledged freeze ticket (toggle carries ticket id — RC-09); breaker OPEN → ticket, page at 30m; rollup: while a root-cause condition holds (breaker OPEN, freeze active), per-row consequence alerts (escalations, tier-2, stuck tickets) aggregate into ONE grouped incident with a running count — state writes stay per-row; scheduled retention-chain check (broker retention vs inbox retention vs replay window → alert on violation); every alert definition carries its CA-8 runbook link (OB-06 completes).
- **Do not change:** per-row STATE writes (rollup is an alerting concern only — §15).
- **Tests to add:** freeze-without-ticket page; rollup groups a seeded escalation storm under breaker-OPEN into one incident; retention violation alert.
- **Edge cases:** the single genuine anomaly during an outage (a real ENGINE_INCONSISTENCY) must still be individually visible within the grouped incident (§15's 03:00 rationale) — verify the grouping preserves per-alert detail.
- **Manual validation:** simulated outage storm review.
- **Expected outcome:** outage ergonomics per spec.
- **Failure signs:** rollup suppressing CRITICALs entirely.
- **Common mistakes:** rollup implemented by dropping alerts instead of grouping.
- **Completion criteria:** tests green.
- **Stop condition:** merged.
- **Next task:** OB-06.

### OB-06 — Runbook wiring + logging practices

- **Task ID:** OB-06
- **Title:** Link every alert to its CA-8 runbook stub; enforce masking + correlation propagation; verify log retention floor
- **Classification:** MVP normative + operational runbook
- **Purpose:** §15 practices (runbook link per alert; correlation greps the whole story) + §16.3 masking + §14 retention floor.
- **Prerequisites:** CA-8 stubs; OB-03..05 alerts; ST-08 (log line).
- **Requirement sections / concepts to read:** §15 (practices), §16.3, §14 (retention floor: ≥ 90 days validated with the business).
- **Placeholder components involved:** [Metrics / Alerting Layer].
- **Local placeholder mappings required before starting:** alert definitions live.
- **Local code areas to discover:** logging encoder config (masking); MDC filters; outbound header propagation.
- **How to locate:** D-10/F.21.
- **Implementation instructions:** attach runbook links to every alert; masking in the ENCODER (debit_account masked in read model, logs, traces — §16.3; call-site discipline is not acceptable); correlation_id through MDC + outbound headers (§15); confirm log-platform retention ≥ the §14 floor (90 days validated) — below floor → report to owner (config/infra change, not code).
- **Do not change:** org log-platform config unilaterally.
- **Tests to add:** masking test (account value never appears in captured log output across the pipeline's log points); MDC propagation test through one async hop.
- **Edge cases:** stack traces allowed in LOGS only, keyed by correlation id — never in read-model fields (§12 content rules; RG-09 enforced it — re-verify here).
- **Manual validation:** one correlation id greps a full payment story (§15 practice).
- **Expected outcome:** operable alert + log surface.
- **Failure signs:** any alert without a runbook link.
- **Common mistakes:** masking only new log lines (encoder-level catches all).
- **Completion criteria:** checks green.
- **Stop condition:** merged.
- **Next task:** OB-07.

### OB-07 — Config inventory + load-order validation

- **Task ID:** OB-07
- **Title:** Externalize the §16.6 configuration inventory; loader REJECTS mis-ordered value sets
- **Classification:** MVP normative implementation
- **Purpose:** §16.6: nothing else orders trust_age/cadence/escalation/tier-2/cutoff-margin; a p99-driven trust-age quietly reaching the escalation age silently degrades wait-then-decide into everything-goes-to-ops.
- **Prerequisites:** consuming tasks landed (RC-04/05/07/08, RG-xx, IN-xx); B-02/B-03 values where available.
- **Requirement sections / concepts to read:** §16.6 (inventory + validation rule), §16.5 (externalized config).
- **Placeholder components involved:** [Metrics / Alerting Layer] (validation failure surfacing), app config.
- **Local placeholder mappings required before starting:** local config conventions.
- **Local code areas to discover:** config binding/validation infrastructure.
- **How to locate:** application properties structure.
- **Implementation instructions:** one config namespace holding every §16.6 entry (trust age, confirmation age, escalation ages, downgrade class, cadences, lease durations, retry policies, thresholds, batch sizes, retentions, cutoff calendar ref, sweep budget, backoff, damping, recency window, freeze propagation bound, escalation cutoff margin); startup validation: reject unless trust_age + query cadence < escalation age < tier-2 age < cutoff margin; document each entry's owner column (§16.6 — owners at kickoff; record what's known).
- **Do not change:** suggested values into hard values without owners (PO-2/PO-3/TL-8 pending — use suggestions, mark pending).
- **Tests to add:** loader rejects each mis-ordering; accepts a valid set; missing mandatory entry fails startup.
- **Edge cases:** per-error-class retry policies validated for completeness against CA-1's classes.
- **Manual validation:** config review with owners.
- **Expected outcome:** config discipline live.
- **Failure signs:** silent defaults for load-bearing values (trust age defaulting quietly).
- **Common mistakes:** validation as a warning instead of a startup rejection.
- **Completion criteria:** tests green.
- **Stop condition:** merged; Phase P13 report.
- **Next task:** GO-01.

## H-Phase 14 (task group 13) — Rollout and go-live gates (P14)

### GO-01 — Rollout sequencing plan (executable)

- **Task ID:** GO-01
- **Title:** Turn Section M into the environment-specific rollout order with owners and checkpoints
- **Classification:** MVP normative implementation
- **Purpose:** §16.5 expand/contract + Section M's enablement order, made concrete for the local pipeline.
- **Prerequisites:** all P3–P13 phases merged in the target branch; S-09 dual-run proof.
- **Requirement sections / concepts to read:** Section M (this playbook), §16.5, §18 (gate status).
- **Placeholder components involved:** [DB Migration Directory], deployment pipeline.
- **Local placeholder mappings required before starting:** environment promotion path known.
- **Local code areas to discover:** feature-flag/config-toggle conventions (Section M's flags need a local mechanism — record which).
- **How to locate:** deployment repo/config.
- **Implementation instructions:** write the local rollout plan following Section M's stage order verbatim, with per-stage: owner, checkpoint evidence, rollback trigger + procedure; wire the Section M flags (new-writer dual-write is already structural; scanner enablement, resolver enablement, auto-downgrade enablement as config).
- **Do not change:** Section M's ORDER (auto-downgrade last, gated on P8 PASS).
- **Tests to add:** none (plan); flag-off behavior smoke tests per flag.
- **Edge cases:** two app versions during each stage (claim compatibility across one release boundary — §16.5).
- **Manual validation:** plan review with the release owner.
- **Expected outcome:** executable rollout plan.
- **Failure signs:** stages combined to save deploys (each checkpoint exists to bound blast radius).
- **Common mistakes:** flags default-on.
- **Completion criteria:** plan approved.
- **Stop condition:** approved.
- **Next task:** GO-02.

### GO-02 — Shadow validation

- **Task ID:** GO-02
- **Title:** Dual-run/shadow comparison: derived dimensions + labels agree with legacy status; derivation outputs agree with observed behavior
- **Classification:** MVP normative implementation
- **Purpose:** Section M's dry-run stage: prove the factored model tracks reality before any rule ENFORCEMENT relies on it in production.
- **Prerequisites:** GO-01; production-like environment with dual-write live (ST-01).
- **Requirement sections / concepts to read:** §10.4 (mapping), Section M (shadow stage).
- **Placeholder components involved:** [Metrics / Alerting Layer] (comparison metric), [Request Status Persistence Layer].
- **Local placeholder mappings required before starting:** dual-write live in the environment.
- **Local code areas to discover:** none new.
- **How to locate:** n/a.
- **Implementation instructions:** a comparison job/report over a soak window: per row, tuple-derived label vs legacy status per the reviewed mapping; disagreements → itemized report (each is a dual-write bug or a mapping-table error — fix, re-soak); completion-predicate shadow: RG-08's derived ui_step_status vs the legacy step status where observable.
- **Do not change:** production traffic; read-only comparison.
- **Tests to add:** the comparison tooling's own correctness (seeded disagreement detected).
- **Edge cases:** rows written by the OLD app version during dual-run (legacy-only) — the S-08 backfill mapping covers them; comparison must tolerate the window.
- **Manual validation:** soak report clean over the agreed window (owner-defined; record).
- **Expected outcome:** factored model trusted.
- **Failure signs:** "small" disagreement rates waved through — every disagreement has a cause; disposition each.
- **Common mistakes:** comparing labels only (compare the tuple fields too).
- **Completion criteria:** clean soak report.
- **Stop condition:** report filed.
- **Next task:** GO-03.

### GO-03 — Staged enablement

- **Task ID:** GO-03
- **Title:** Enable in Section M order: constraints validated → guards/triggers → scanners → resolver → escalation → (after P8 PASS) auto-downgrade
- **Classification:** MVP normative implementation
- **Purpose:** each mechanism observes before it acts; the auto-downgrade (a money-adjacent self-heal) goes last, gated on the §18-1 proof.
- **Prerequisites:** GO-02 clean; per-stage prerequisites in Section M; P8 gate status for the final stage.
- **Requirement sections / concepts to read:** Section M (enablement order + per-stage validation), §9.2 (what the downgrade risks), §18-1.
- **Placeholder components involved:** all runtime components (config-driven).
- **Local placeholder mappings required before starting:** GO-01 flags wired.
- **Local code areas to discover:** none new.
- **How to locate:** n/a.
- **Implementation instructions:** per Section M's stage list: enable, run the stage's validation checklist (alerts quiet, metrics sane, sampled rows correct), hold the stage soak, proceed; auto-downgrade stage requires: CT-02..05 PASS evidence on file + observed-lag watchdog live (OB-04) + trust-age configured from TL-5 (not the stub).
- **Do not change:** stage order; no skipping soaks under schedule pressure (record any waiver with its owner).
- **Tests to add:** none new (checklists execute existing ones).
- **Edge cases:** a stage's validation failing → its documented rollback (flag off), fix forward, re-enter.
- **Manual validation:** stage sign-offs recorded.
- **Expected outcome:** system live in safe order.
- **Failure signs:** resolver enabled before evidence rules (IN-07) verified in the environment.
- **Common mistakes:** enabling the downgrade with the conservative stub trust-age (must be TL-5-derived by then).
- **Completion criteria:** all stages enabled + soaked.
- **Stop condition:** enablement complete.
- **Next task:** GO-04.

### GO-04 — Go-live gate execution

- **Task ID:** GO-04
- **Title:** Execute the Section Q checklist; assemble gate evidence; obtain go/no-go
- **Classification:** §18 BLOCKING gate aggregation
- **Purpose:** the four §18 BLOCKING items + all Q items PASS before first production payment under the new machinery.
- **Prerequisites:** GO-03; OP-03 drill; CT suite results; K-03 vectors; open-question register (Section K) current.
- **Requirement sections / concepts to read:** Section Q; §18 (all BLOCKING items).
- **Placeholder components involved:** none (evidence task).
- **Local placeholder mappings required before starting:** none.
- **Local code areas to discover:** none.
- **How to locate:** n/a.
- **Implementation instructions:** walk Section Q item by item: PASS with linked evidence / FAIL with owner + plan / BLOCKED with the blocking §18 item; §18 BLOCKING items are non-waivable (Playbook Index rule); deliver to the accountable owner for go/no-go.
- **Do not change:** checklist items (additions allowed; removals need the owner).
- **Tests to add:** none.
- **Edge cases:** unresolved non-BLOCKING Section K questions → recorded risks with owners, not silent passes.
- **Manual validation:** signed go/no-go.
- **Expected outcome:** auditable go-live decision.
- **Failure signs:** "PASS" without linked evidence.
- **Common mistakes:** treating written provider answers as CT evidence (§18-1: the TEST is the proof).
- **Completion criteria:** decision recorded.
- **Stop condition:** decision recorded.
- **Next task:** GO-05.

### GO-05 — Rollback validation

- **Task ID:** GO-05
- **Title:** Rehearse the rollback paths that remain legal at each stage; document the point of no return
- **Classification:** MVP normative implementation
- **Purpose:** Section M's constraint: money-affecting writes under new machinery bound what can be rolled back; rehearse BEFORE relying on it.
- **Prerequisites:** GO-01 plan; a production-like environment.
- **Requirement sections / concepts to read:** Section M (rollback constraints), §16.5.
- **Placeholder components involved:** [DB Migration Directory], deployment pipeline.
- **Local placeholder mappings required before starting:** GO-01 stages.
- **Local code areas to discover:** none new.
- **How to locate:** n/a.
- **Implementation instructions:** rehearse: app-version rollback during dual-run (must be clean — expand/contract guarantees it); flag-off rollback per enabled stage; document the no-return line per Section M (once terminal outcomes + money movements are being written by the new machinery, rollback = flags off + state stands; schema stays; no data un-migration); verify the old version still runs against the final schema (S-09's proof re-run on the release candidate).
- **Do not change:** migrations (rollback is forward-only by expand/contract design — no down-migrations of applied money data).
- **Tests to add:** the rehearsal scripts where automatable.
- **Edge cases:** rollback WHILE rows sit in new-only states (e.g. BLOCKED(ESCALATED)) — the old version must at minimum not corrupt them (it ignores unknown columns by expand/contract; verify the old version's writers don't blank new columns — S-09 dual-run proof covers; re-verify).
- **Manual validation:** rehearsal reports.
- **Expected outcome:** known-good rollback envelope.
- **Failure signs:** a rollback plan that requires down-migrating data.
- **Common mistakes:** rehearsing only the happy rollback (rehearse the mid-incident one: flags off under load).
- **Completion criteria:** rehearsals recorded.
- **Stop condition:** recorded; Phase P14 report. PLAYBOOK IMPLEMENTATION COMPLETE — operate per Section N runbooks.
- **Next task:** none (steady state).

---

# I. Minimal Context Packets

One packet per task card. Each is paste-alone for a small-context
executor. Format: ID/title · Read (§s of `requirment-v4.md` + playbook
refs) · Invariant · Placeholders · Mappings needed · Objective ·
Tests · Stop.

```text
[D-01] Set up local mapping document
Read: playbook Sections G, O. Invariant: mappings stay LOCAL, never transferred out.
Placeholders: all. Mappings: none.
Objective: create the Section O table locally, one row per Section G placeholder, Status=UNMAPPED.
Tests: none. Stop: table exists.
```

```text
[D-02] Discover schema/migration state
Read: §2.1 §2.2 §2.3 §10.3 §16.5; playbook F.17 F.18. Invariant: read-only; record, don't fix.
Placeholders: [DB Migration Directory] [Stored Procedure / Trigger Area]. Mappings: none.
Objective: record migration tool+dir; per-table column gap list vs §2; constraint/trigger/index inventory; DB privileges.
Tests: none. Stop: gap inventory recorded, every §2 field has a status.
```

```text
[D-03] Discover obligation + money fields
Read: §2.1 §3 §11; playbook F.1 F.12 F.13. Invariant: read-only; existing counter semantics recorded, never assumed.
Placeholders: [Obligation Repository] [Reservation Repository]. Mappings: none.
Objective: map scope entity, upsert path, FOR UPDATE usage, every amount-field writer + transaction boundary, observed counter semantics.
Tests: none. Stop: rows Confirmed/UNCLEAR with evidence.
```

```text
[D-04] Discover request persistence + status model
Read: §2.2 §10.4 §11; playbook F.2 F.10 F.11. Invariant: the rule-site inventory must be COMPLETE (it drives ST-05).
Placeholders: [Request Status Persistence Layer] [Payment Request Creation Component]. Mappings: none.
Objective: record status enum values+meanings; every status writer (conditional vs blind); every reader that branches on status; CAS/version column reality.
Tests: none. Stop: writer + rule-site inventories recorded.
```

```text
[D-05] Discover provider POST path
Read: §5 §7.0–7.2 §16.1; playbook F.4 F.5 F.15 F.16. Invariant: read-only; every response branch listed or marked UNCLEAR.
Placeholders: [Provider POST Client] [Provider Response Parser]. Mappings: none.
Objective: record call-site count, SDK method+response shape, all response branches, identity fields sent + where generated, UETR extraction sites, retry wrappers around the POST.
Tests: none. Stop: POST-path memo recorded.
```

```text
[D-06] Discover status-query usage
Read: §9 intro §9.1; playbook F.6. Invariant: the query API ≠ the status feed.
Placeholders: [Status Query Resolver]. Mappings: none.
Objective: record query client existence, query key(s), response statuses seen in code, callers. MISSING is a valid finding.
Tests: none. Stop: row Confirmed/MISSING/UNCLEAR.
```

```text
[D-07] Discover both Kafka consumers
Read: §6.0 §8 §16.2; playbook F.7. Invariant: record gaps vs §16.2 line by line; change nothing.
Placeholders: [Payment Status Feed Consumer] (+upstream consumer). Mappings: none.
Objective: per consumer record ack mode, auto-commit, offset reset, deserializer, DLT, key, concurrency, poll settings → §16.2 gap checklist.
Tests: none. Stop: checklist complete for both.
```

```text
[D-08] Discover scheduled jobs + retry ownership
Read: §7.4 §9.5 §16.1; playbook F.9 F.19. Invariant: exactly one retry owner per operation must be identifiable later; inventory all.
Placeholders: [Retry Resolver Job] [Reconciliation / Drift Scanner] [Status Query Resolver]. Mappings: none.
Objective: job inventory (schedule, tables, states claimed, time source); flag double-claim risks and app-time comparisons.
Tests: none. Stop: inventory recorded.
```

```text
[D-09] Discover identity + duplicate protection
Read: §5 §5.1; playbook F.14 F.15. Invariant: persistence timing claims must be traced to the commit boundary, not assumed.
Placeholders: [Payment Request Creation Component] [Provider POST Client]. Mappings: D-05 memo.
Objective: record key generation (deterministic vs random), inputs, persistence timing vs the wire call, every duplicate-prevention layer.
Tests: none. Stop: identity memo recorded.
```

```text
[D-10] Discover Hazelcast, metrics, ops procedures
Read: §16.1 (freeze) §14 §15 (skim) §20; playbook F.20 F.21. Invariant: verify the freeze toggle's technical shape, don't infer it.
Placeholders: [Metrics / Alerting Layer] [Operator Admin Procedure Area] [Stored Procedure / Trigger Area]. Mappings: none.
Objective: record Hazelcast topology + toggles, metrics/log/alert stack, encoder masking status, MDC propagation, PL/SQL ops objects + grants, session-context facility.
Tests: none. Stop: rows + infra memo recorded.
```

```text
[D-11] Inventory existing tests
Read: playbook F.23 F.24. Invariant: the baseline green bar is the preserved-behavior evidence; record flaky tests by name.
Placeholders: [Integration Test Suite] [Contract Test Suite]. Mappings: none.
Objective: record suites, DB/Kafka test infra (real Oracle? gap if H2-only), business-rule test coverage; run full suite once, record baseline.
Tests: baseline run. Stop: inventory + baseline recorded.
```

```text
[D-12] Discovery report
Read: playbook Section F status codes; Playbook Index BLOCKED list. Invariant: no "probably" — IMPLEMENTED/PARTIAL/MISSING/UNCLEAR only.
Placeholders: all. Mappings: all D-xx rows filled.
Objective: per F.1–F.24 concept assign a status + one-line evidence; list UNCLEARs + resolution needs; update locally-BLOCKED task list; deliver to human owner.
Tests: none. Stop: report delivered; WAIT for human review before implementation.
```

```text
[B-01] §18-0 snapshot-contract residue
Read: §1 contract facts (trade-payment cardinality), §6.0, §6.1, §12, §18 item 0. Model (§1 fact): multiple payments per trade; snapshot messages (newer overwrites older); tuple unique within snapshot → NO discriminator; schema/identity freeze not gated here.
Placeholders: none. Mappings: none.
Objective (residue): written upstream confirmation of snapshot schema + uniqueness (ask 5); §6.0 intake uniqueness validation in IN-02; PO-9 (absence semantics, BA-2 amendment) and TL-16 (ordering-watermark rule) answered before IN-02 freeze; TL-2 gains the step-granularity clause.
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

```text
[S-01] Migration plan freeze
Read: §16.5; CA-4; D-02 inventory. Invariant: expand/contract — additive first, VALIDATE after backfill, drops post-rollout.
Placeholders: [DB Migration Directory]. Mappings: directory Confirmed.
Objective: ordered migration list (one concern each): columns → inbox → UNIQUEs/I6 → CHECKs NOVALIDATE → triggers → indexes → backfill → VALIDATE; per entry rollback + dual-run note.
Tests: none. Stop: plan approved by owner + DBA.
```

```text
[S-02] Obligation columns
Read: §2.1 (whole) §16.5. Invariant: additive only; nullable-with-default first; scope key per B-01.
Placeholders: [DB Migration Directory] [Obligation Repository]. Mappings: both.
Objective: add §2.1 columns (amounts, markers+counters+first_at, ordering fields, read-model fields, reopened_at, next_request_seq), scope-key UNIQUE, amounts>=0 CHECK, business_id index; entity mapping additive.
Tests: apply on clean+prod-shaped schema; entity round-trip. Stop: merged, D-11 baseline green. Duplicate-scope data → STOP and report.
```

```text
[S-03] Request columns
Read: §2.2 (whole) §16.5. Invariant: dimension columns nullable until S-08 backfill; legacy status column untouched.
Placeholders: [DB Migration Directory] [Request Status Persistence Layer]. Mappings: both.
Objective: add the four dimensions + blocked_reason + identity/uetr/provider_reference + version/claim/retry/next_query_at + created_at/state_changed_at/creating_ordering + last_sent_hash/divergence_expected/divergent_payload_at + maybe_since/escalated_at/submitted_at/last_post_attempt_at.
Tests: apply tests; entity round-trip. Stop: merged, baseline green.
```

```text
[S-04] Inbox table + purge
Read: §2.3 (DDL given) §16.2 (retention chain). Invariant: no parked-event table exists or ever will (SPEC_CONFLICT).
Placeholders: [DB Migration Directory] [Inbox / Processed Event Repository]. Mappings: F.8 status.
Objective: create processed_inbound_event (PK (source,event_id), processed_at UTC default) + purge job (retention > kafka retention ≥ replay window).
Tests: duplicate-key clean return; purge boundary. Stop: merged.
```

```text
[S-05] CHECKs, UNIQUEs, I6
Read: §10.3 (matrix) §2.2 constraints CA-4. Invariant: DB is the backstop; L9 is NOT a CHECK (drift-scanner verified).
Placeholders: [DB Migration Directory]. Mappings: real-Oracle test lane (STOP if H2-only).
Objective: enum CHECKs; L2–L8 + L1-shape CHECKs; UNIQUE(idempotency_key); NULL-ignoring UNIQUE(uetr); I6 = unique fn index CASE WHEN outcome IS NULL THEN payment_obligation_id END. NOVALIDATE→VALIDATE per plan.
Tests: one violation test per constraint; I6 second-active rejected. Stop: validated + green.
```

```text
[S-06] Freeze + release-guard triggers
Read: §10.3 (backstops) §10.1 §9.3 (flag setters). Invariant: raw SQL on MAYBE/SUBMITTED rows fails loudly; flag setters are exactly the authoritative-negative path and the §9.3 procedure.
Placeholders: [Stored Procedure / Trigger Area] [DB Migration Directory]. Mappings: session-context facility confirmed; pool interaction verified.
Objective: freeze trigger (dimension change on already-terminal row → raise); release-guard trigger (terminal-negative on MAYBE/SUBMITTED without session evidence flag → raise); flag transaction-scoped.
Tests: rejected/accepted paths; pool non-leakage (two sessions). Stop: green on real Oracle.
```

```text
[S-07] Active-row-bounded indexes
Read: §16.6 artifact 4 (index list) §9.5 §15. Invariant: scan plans independent of terminal-row count; query expressions must textually match index expressions.
Placeholders: [DB Migration Directory]. Mappings: directory.
Objective: per CA-4, one CASE WHEN outcome IS NULL fn index per standing scan (resolver, retry, escalation, BLOCKED queue, stuck-state, drift) + created_at window index; record exact expressions for later scanner queries.
Tests: EXPLAIN plan assertions on terminal-heavy seed. Stop: merged.
```

```text
[S-08] Backfill dimensions
Read: §10.4 (reverse map) §10.2 §2.2 anchors §7.1. Invariant: ambiguous legacy states backfill to MAYBE_SUBMITTED (fail toward resolver, never NOT_SUBMITTED).
Placeholders: [DB Migration Directory] [Request Status Persistence Layer]. Mappings: legacy meanings memo (D-04); unmappable values = BLOCKED, report.
Objective: reviewed legacy→tuple map; idempotent backfill; anchors defensibly set; terminal rows L1-normalized; run in a quiet window.
Tests: idempotency; per-value spot checks; constraint dry-validate. Stop: validated; anomaly list dispositioned.
```

```text
[S-09] Migration test pass
Read: §16.5. Invariant: the OLD app version must run against the NEW schema.
Placeholders: [DB Migration Directory] [Integration Test Suite]. Mappings: Oracle lane (set it up first if missing).
Objective: prove: clean-schema apply; prod-shaped apply + backfill; old-version boot+smoke on new schema; constraint suite in CI.
Tests: the four proofs. Stop: green; report filed.
```

```text
[K-01] next_request_seq discipline
Read: §2.1 (seq) §5.1 §11. Invariant: seq incremented under the obligation lock in the request-insert transaction — the row counter, never an Oracle sequence.
Placeholders: [Payment Request Creation Component] [Obligation Repository]. Mappings: creation sites known.
Objective: lock → read seq → increment → derive → insert, one transaction, all creation sites.
Tests: concurrent creations get distinct sequential seqs; rollback atomicity. Stop: merged.
```

```text
[K-02] Deterministic key derivation
Read: §5.1 (all); CA-5. Invariant: derived from business state, never random; amount and UETR excluded; persisted keys on existing rows NEVER re-derived.
Placeholders: [Payment Request Creation Component]. Mappings: K-01 sites; D-09 memo.
Objective: implement CA-5 exactly; key computed+stored at insert; new rows only.
Tests: determinism across JVMs; input sensitivity; amount-independence; persisted-key-wins. Stop: merged.
```

```text
[K-03] Golden-vector tests
Read: §5.1 (exactness); CA-5 vectors. Invariant: vectors are the frozen truth — never regenerated from the implementation.
Placeholders: [Integration Test Suite]. Mappings: K-02 done.
Objective: load CA-5's vector file verbatim; one byte-exact test per vector + version pin; verify tests bite via a deliberate local mutation (then revert).
Tests: the suite. Stop: green; record as Section Q evidence.
```

```text
[K-04] Write-ahead identity at claim
Read: §5 (rules) §11 (claim + ambiguous commit) §2.2. Invariant: no POST under an unpersisted caller-supplied identity; unknown claim-commit → NO wire call.
Placeholders: [Provider POST Client] [Request Status Persistence Layer] [Payment Request Creation Component]. Mappings: POST site; claim commit boundary traced.
Objective: claim transaction persists identity (first claim), COMMITS, then the HTTP call; commit-unknown → abandon, lease expiry owns it.
Tests: ordering fault-injection (commit vs stub-received); ambiguous-commit → no call. Stop: merged.
```

```text
[K-05] Hash + flag + attempt stamp at claim
Read: §2.2 (hash/flag/anchor blocks) §7.0 §11; CA-6. Invariant: divergence_expected computed BEFORE overwriting the prior hash; anchor stamped pre-wire.
Placeholders: [Provider POST Client] [Request Status Persistence Layer] [Payment Enrichment Component] (read-only). Mappings: K-04 path.
Objective: claim tx: fresh assembly → CA-6 hash → flag := (prior hash NOT NULL ∧ differs) → persist hash+flag+last_post_attempt_at → commit → wire; posting-claim log line carries hash + attempt count.
Tests: first/changed/unchanged attempt flag values; pre-wire stamping; log line. Stop: merged.
```

```text
[K-06] Duplicate-prevention verification
Read: §5.1 (rationale) §7.2 (DUPLICATE row) §2.2. Invariant: a restore-recreated request regenerates the SAME key.
Placeholders: [Integration Test Suite] [Provider POST Client] (stub). Mappings: integration lane.
Objective: tests: crash-before-POST retry reuses key; crash-after-POST → MAYBE, no fresh key; restore simulation regenerates equal key via the REAL path; UNIQUE violation loud.
Tests: the four. Stop: green; Q evidence.
```

```text
[U-01] Acceptance-only UETR persistence
Read: §5 (persistence rules) §7.2 §2.2. Invariant: rejection/collision UETRs name submissions under which NOTHING executes — never persisted, never overwritten.
Placeholders: [Provider Response Parser] [Request Status Persistence Layer]. Mappings: parser; uetr write sites.
Objective: single uetr write path keyed on response class; acceptance + original-replay persist; DUPLICATE/collision/rejects never; non-NULL never overwritten.
Tests: per-class persistence matrix; DUPLICATE leaves prior value intact. Stop: merged.
```

```text
[U-02] provider_reference persistence
Read: §2.2 (provider_reference) §8 (fallback) §5. Invariant: distinct field from uetr; UNIQUE makes silent reuse loud; never a dedup key.
Placeholders: [Provider Response Parser] [Request Status Persistence Layer]. Mappings: parser; CA-2 field name.
Objective: extract + persist; UNIQUE index; reuse observed → raise Q-17, don't drop the index silently.
Tests: persistence; loud uniqueness violation; no cross-assignment. Stop: merged.
```

```text
[U-03] UETR behavior tests
Read: §5 §8 (matching) §16.6-6. Invariant: a dead-UETR feed event never matches a row.
Placeholders: [Integration Test Suite]. Mappings: matching logic locatable.
Objective: tests: acceptance-persisted UETR matches its feed event; never-persisted rejection UETR → unmatched; uetr-NULL crash row → unmatched (recovered by §9).
Tests: the above. Stop: green.
```

```text
[ST-01] Dual-write dimensions
Read: §2.2 §10.4 (bridge map) §16.5. Invariant: every writer produces a consistent (legacy, tuple) pair from ONE shared mapping helper.
Placeholders: [Request Status Persistence Layer]. Mappings: D-04 writer inventory COMPLETE.
Objective: each status writer also writes the tuple per the reviewed S-08 map; no reader/WHERE changes yet.
Tests: per-writer pair-consistency. Stop: all writers dual-write; baseline green.
```

```text
[ST-02] CAS discipline
Read: §11 (rules) §10.3 (L1 via CAS) §10.1 (mirror). Invariant: WHERE carries full dimension precondition + outcome IS NULL; row count is the verdict; rowCount 0 is HANDLED; no ORM dirty-checking.
Placeholders: [Request Status Persistence Layer]. Mappings: writer inventory; ORM-save sites.
Objective: convert every dimension writer to conditional UPDATE + row-count branch; dimension changes take the obligation lock first + re-derive hook.
Tests: row-count-0 on stale precondition; mirror rule (late accepted vs EXECUTED → 0 rows); no-dirty-checking. Stop: merged.
```

```text
[ST-03] Legality conformance tests
Read: §10.2 §10.3 §10.5 (seed). Invariant: every §10.5 row has a test; illegal transitions die at code AND (where CHECKable) at DB.
Placeholders: [Integration Test Suite] [Request Status Persistence Layer]. Mappings: Oracle lane.
Objective: table-driven suite: all legal flow rows + illegal cases per L1–L8; assert which layer refuses.
Tests: the suite. Stop: green; coverage recorded (pending §9.2 case noted).
```

```text
[ST-04] Display labels
Read: §10.4 (mapping + strictness) §2.2. Invariant: labels derived from dimensions, display-only, never parsed by consumers, never in machine-consumed payloads.
Placeholders: [Request Status Persistence Layer] + display readers. Mappings: D-04 display-reader list.
Objective: implement the §10.4 mapping (view/expression); route dashboards/card label/log/ops reads to it; card returns dimensions + label.
Tests: mapping per label row; NEEDS_REVIEW includes blocked_reason. Stop: merged.
```

```text
[ST-05] Migrate rule sites off legacy status (template — apply per site)
Read: §2.2 (dimension meanings) §10.1 (blocked_reason rule) §4.5; the § governing the specific rule. Invariant: behavior-preserving re-keying; each rule means ONE dimension; blocked_reason is never a rule input.
Placeholders: per site. Mappings: rule-site inventory with per-site dimension classification.
Objective: per site: pick the dimension the rule MEANS (money→submission_state, position→stage, claimability→stage_state, finality→outcome); rewrite; irreducibly-compound → UNCLEAR, report.
Tests: per site, before/after verdict-matrix pin. Stop: inventory empty or dispositioned.
```

```text
[ST-06] Outcome normalization
Read: §10.2 (outcome block) §2.2 (anchor clears) §10.3 (L1 split). Invariant: one canonical terminal shape; submission_state kept; maybe_since/escalated_at cleared.
Placeholders: [Request Status Persistence Layer]. Mappings: outcome-writer list.
Objective: single helper used by every outcome write: outcome + stage_state=READY + NULL claim/retry/blocked + clear maybe_since/escalated_at.
Tests: from CLAIMED/RETRY_WAIT/BLOCKED → canonical shape; L6 holds through terminal-from-CLAIMED. Stop: merged.
```

```text
[ST-07] Episode anchors
Read: §2.2 (maybe_since/submitted_at/escalated_at/last_post_attempt_at) §15 (clock discipline). Invariant: set-once per episode; churn never refreshes; ages NEVER read state_changed_at.
Placeholders: [Request Status Persistence Layer]. Mappings: submission-state writers.
Objective: maybe_since set on first MAYBE entry, cleared on leave + outcome; submitted_at on SUBMITTED; escalated_at contract (written by RC-08, cleared with maybe_since).
Tests: churn preserves maybe_since; re-entry = new episode; outcome clears. Stop: merged.
```

```text
[ST-08] CAS log line
Read: §14 (whole) §16.3 (masking). Invariant: emitted only on rowCount==1; carries key+seq+correlation+tuple before→after+label+trigger fields; no account data, no instruction content.
Placeholders: [Request Status Persistence Layer] [Metrics / Alerting Layer]. Mappings: logging conventions.
Objective: one emission point in the CAS helper; posting-claim line adds hash+attempt count (K-05 convention).
Tests: log-capture per transition family; masking. Stop: merged.
```

```text
[ST-09] Claims as leases
Read: §11 (claims + scanner rules) §2.2 L6. Invariant: claim = CAS to CLAIMED + claimed_by + claim_expires_at; SKIP LOCKED; DB time; per-item transactions.
Placeholders: [Retry Resolver Job] + stage workers, [Request Status Persistence Layer]. Mappings: D-08 claim reality.
Objective: standard claim/complete CASs with L6 both directions; lease durations from config.
Tests: double-claim race; stale-worker fence; L6. Stop: merged.
```

```text
[ST-10] Lease-expiry recovery
Read: §11 (expiry + rationale) §10.2. Invariant: expired POST claim → CONFIRM·READY·MAYBE + maybe_since; NEVER re-claimed for posting; no "provably not launched" carve-out.
Placeholders: [Retry Resolver Job] (or expiry sweep). Mappings: ST-09 shape.
Objective: expiry handling: ENRICH → READY in place; POST → CONFIRM·READY·MAYBE (+maybe_since), claim fields cleared.
Tests: both paths; slow-worker fence; expired POST row structurally unclaimable for posting. Stop: merged.
```

```text
[ST-11] Graceful shutdown
Read: §11 (shutdown) §16.1 (drain). Invariant: listeners → scanners → drain POSTs (bounded) → release ENRICH claims only; POST claims never released.
Placeholders: consumers, [Retry Resolver Job], [Provider POST Client]. Mappings: lifecycle wiring.
Objective: ordered shutdown per §11's four steps.
Tests: shutdown idle / mid-ENRICH / mid-POST. Stop: merged.
```

```text
[RG-01] +committed at creation
Read: §3 (increment) §6.8 §11. Invariant: reservation increments in the SAME transaction as the insert; NOTHING moves at POST time.
Placeholders: [Payment Request Creation Component] [Reservation Repository] [Obligation Repository]. Mappings: creation sites; D-03 semantics memo (legacy counters untouched).
Objective: committed_amount += amount with the insert, under the lock.
Tests: atomicity; I1; concurrent-create (I6 + single increment). Stop: merged.
```

```text
[RG-02] −committed on terminal-negative
Read: §3 (decrement) §10.2. Invariant: decrement iff the terminal-negative CAS affected exactly one row, same transaction; EXECUTED never decrements committed.
Placeholders: [Reservation Repository] [Request Status Persistence Layer]. Mappings: ST-06 helper.
Objective: extend the outcome path: REJECTED/CANCELLED/SUPERSEDED + rowCount 1 → decrement.
Tests: redelivery-safe (0 rows → no move); I1 across transitions. Stop: merged.
```

```text
[RG-03] +confirmed with amount equality
Read: §3 §8 (mismatch) §16.4 §10.5. Invariant: compareTo equality only; mismatch → BLOCKED(AMOUNT_MISMATCH)+SUB=SUBMITTED+CRITICAL, NO money movement.
Placeholders: [Reservation Repository] [Request Status Persistence Layer]. Mappings: none new.
Objective: settlement helper: equal → EXECUTED CAS (sets SUBMITTED, L4) + confirmed += amount; unequal → park path.
Tests: scale-variant equality; mismatch park; I2/I3; JPY/BHD round-trip. Stop: merged.
```

```text
[RG-04] Overpay latch
Read: §13 (latch + rationale + race) §3 (I4). Invariant: one-way; set when confirmed > required; NEVER auto-cleared by anything.
Placeholders: [Reservation Repository] [Metrics / Alerting Layer]. Mappings: RG-03 helper.
Objective: post-change check under lock → set latch (idempotent) + alert hook.
Tests: sets on overpay; survives required-amount rise (§13 trace); I4. Stop: merged.
```

```text
[RG-05] Release guard + supersede/close
Read: §10.1 §9.4 §3 (required feature + FORBIDDEN clause) §20. Invariant: terminal-negative only on NOT_SUBMITTED, or authoritative engine negative, or the §9.3 procedure; a query answer never releases.
Placeholders: [Request Status Persistence Layer] [Operator Admin Procedure Area] [Stored Procedure / Trigger Area]. Mappings: S-06 flag mechanics.
Objective: shared guard before every terminal-negative CAS; guarded supersede/close procedure (restricted role, ticket + identity logged) refusing MAYBE/SUBMITTED.
Tests: deny/allow at code AND trigger layers; supersede releases on legal rows only. Stop: merged.
```

```text
[RG-06] Standing shortfall re-evaluation
Read: §6.8 (whole) §3 (I5) §6.2. Invariant: exactly ONE creation point; triggers T1–T4; successor policy gates REJECTED successors (ordering-newer ∧ count<2 ∧ no live marker).
Placeholders: [Payment Request Creation Component] [Obligation Repository]. Mappings: ALL legacy creation sites (unroutable → STOP).
Objective: evaluate() under lock per §6.8's condition list; invoke from T1–T4; route every legacy site through it.
Tests: each trigger, each gate, each successor row; deferred amendment; zero-shortfall no-op. Stop: merged.
```

```text
[RG-07] Auto-cancel + retry-guard
Read: §6.4 (whole) §7.0 (staleness term) §10.5. Invariant: cancellable set is strictly NOT_SUBMITTED (not POST·CLAIMED, not BLOCKED); row-count-0 branches on submission_state FIRST; MAYBE → AMENDMENT_PARKED (deferred under live claim).
Placeholders: [Payment Request Creation Component] [Request Status Persistence Layer] [Retry Resolver Job]. Mappings: amendment path.
Objective: the §6.4 CAS verbatim + branch handling + retry-guard (stale+NOT → cancel; stale+MAYBE → park).
Tests: every §10.5 cancel/park row; ENRICH·CLAIMED cancellable; POST·CLAIMED untouched; deferred park lands. Stop: merged.
```

```text
[RG-08] Step-status predicate
Read: §4.1 (predicate + bullets) §4 §2.1 (liveness incl. anchor clause). Invariant: completion derived only; anchors can't complete; active request blocks completion; feed never writes ui_step_status.
Placeholders: [Obligation Repository] [Request Status Persistence Layer]. Mappings: ST-02 re-derive hook.
Objective: implement the predicate exactly (incl. required NOT NULL ∧ >0 and confirmed>=required terms); wire into every re-derivation; remove event-copy writers.
Tests: each term isolated; recovered anchor completes. Stop: merged.
```

```text
[RG-09] Exception + next-actor derivation
Read: §4.2 (ranks) §4.3 §4.5 §13. Invariant: derived, never accumulated; rank-1 (MAYBE, OVERPAY) never masked; actor never stored; active requests only.
Placeholders: [Obligation Repository] [Request Status Persistence Layer]. Mappings: RG-08 hook.
Objective: precedence evaluation → active_exception_* writes (content per §12 rules); §4.5 actor as a pure function.
Tests: precedence; construction-clearing (corrected message); dual-actor rows; PAYMENT_OUTCOME_UNKNOWN never shows as SYSTEM_UNAVAILABLE. Stop: merged.
```

```text
[RG-10] Reopening + latch guard
Read: §6.5 §6.3 §2.1 (reopened_at). Invariant: reopening = standing re-evaluation; latched scope applies amounts but creates NOTHING (AMENDMENT_ON_LATCHED_SCOPE).
Placeholders: [Obligation Repository] [Payment Request Creation Component]. Mappings: amendment path.
Objective: required-increase on COMPLETED → recalc + RG-06 + reopened_at + IN_PROGRESS + overpay re-eval; latch branch alerts instead.
Tests: reopening trace; latched branch; reopened_at set. Stop: merged.
```

```text
[CT-01] Sandbox harness
Read: §18-1 (intro+matrix) §1 (assumed facts). Invariant: tests use the REAL derivation + serialization or the proof is void.
Placeholders: [Contract Test Suite] [Provider POST Client]. Mappings: sandbox credentials (vaulted).
Objective: runnable suite (excluded from default CI): POST helpers via real identity path; evidence capture (timestamped, engine-versioned).
Tests: smoke POST. Stop: smoke green.
```

```text
[CT-02] Identical-payload re-POST
Read: §18-1(a) §7.0 §16.6-1. Invariant: nothing executes twice; the second response's class feeds CA-1 (dedup vs original-replay).
Placeholders: [Contract Test Suite]. Mappings: harness.
Objective: POST, re-POST byte-identical, assert single execution engine-side, classify + file evidence.
Tests: the run. Stop: result recorded. Double execution → STOP ALL re-POST reliance, escalate.
```

```text
[CT-03] Divergent-payload re-POST
Read: §18-1(b) TL-4 §7.2 §5.1. Invariant: never executed; rejection code distinguishable from plain DUPLICATE_REQUEST.
Placeholders: [Contract Test Suite]. Mappings: harness.
Objective: re-POST with changed business field and (separately) changed amount; assert no execution; capture + compare codes; file into CA-1.
Tests: two variants. Stop: recorded. Execution → STOP, escalate (TL-4 payload-freeze clause is a human decision).
```

```text
[CT-04] TTL edge
Read: §18-1(c) §7.0 §9.3. Invariant: a key aged out of the dedup store executes a duplicate — TTL vs max row lifetime decides a repost_permitted TTL term.
Placeholders: [Contract Test Suite]. Mappings: harness; written TTL.
Objective: re-run (a)/(b) at the achievable retention edge; compare TTL vs max lifetime incl. ops SLA; record the RC-03 follow-up if TTL is short.
Tests: edge runs (provider-assisted acceptable, documented). Stop: evidence + consequence note filed.
```

```text
[CT-05] Re-POST after sync business reject
Read: §18-1(d) TL-6 §7.1. Invariant: either answer is handled but must be KNOWN by test, not by documentation.
Placeholders: [Contract Test Suite]. Mappings: harness; inducible business reject.
Objective: induce reject; re-POST same key; record re-executes vs replays; if replays → record the RC-04 policy consequence (fresh successor via §6.8).
Tests: the run per retryable class. Stop: recorded.
```

```text
[CT-06] Query mapping verification
Read: §9.1 §9.2 (four causes); CA-3. Invariant: CA-3 verified empirically; never-sent key → NOT_FOUND observed.
Placeholders: [Contract Test Suite] [Status Query Resolver]. Mappings: harness + query client.
Objective: query executed/rejected/never-sent/accepted cases; opportunistic ingest-lag observations; feed findings to CA-3's owner.
Tests: four runs. Stop: recorded; owner sign-off.
```

```text
[CT-07] SDK contract (TL-11)
Read: §5 (chain+rules) TL-11. Invariant: engine dedup keys on the CALLER key even under a fresh SDK-minted UETR — blocking-grade.
Placeholders: [Contract Test Suite] [Provider POST Client]. Mappings: harness.
Objective: verify (a) UETR field in acceptance response; (b) caller key transmitted; (c) dedup by key despite fresh UETR.
Tests: three checks. Stop: recorded; §18-1 summary updated. SDK rejects caller keys → STOP, escalate.
```

```text
[IN-01] Message validation + contract
Read: §6.0 (fields+equality+emission fact) §16.4 (scale) §16.5 §6.6. Invariant: payload equality = canonicalized business-field subset, never raw bytes; scale violations reject, never round.
Placeholders: upstream consumer, [Contract Test Suite]. Mappings: consumer.
Objective: field validation (business_id, scope, positive scale-valid amount, ordering, trade ref, ui ids, correlation); equality function; §6.6 failure routing; build-time schema enforcement.
Tests: validation cases; scale cases; equality (envelope excluded). Stop: merged.
```

```text
[IN-02] Upsert + ordering guard
Read: §6.1 §6.7 (whole) §6.9 (required row) §6.0. Invariant: required_amount mutates only on strictly-newer ordering; tie+identical drops; tie+different alerts (never silent); comparator is ONE pluggable point.
Placeholders: [Obligation Repository]. Mappings: upsert path; B-01 answered.
Objective: locked upsert (ORA-00001 retry); guard; tie branches; stale counted; T1 → RG-06 even without amount change.
Tests: §6.7 regression trace; ties; T1. Stop: merged.
```

```text
[IN-03] Validation anchors + DLT
Read: §6.6 (normal path only — tiers 2-3 are TL-7 FUTURE) §2.1 (marker fields) §4.1. Invariant: anchors never advance upstream_ordering; §4.1 cannot complete them.
Placeholders: [Obligation Repository], DLT. Mappings: consumer + DLT.
Objective: extractable-scope failures → anchor (required NULL, IN_PROGRESS, validation_failed marker with failing ordering, first_at, count); unidentifiable → DLT + alert.
Tests: anchor lifecycle incl. recovery by valid message; DLT. Stop: merged.
```

```text
[IN-04] Monotonic markers + counters
Read: §2.1 (marker blocks) §6.9 (write+read rules). Invariant: overwrite only on strictly-newer ordering; stale writes dropped+counted; provider_rejected live while count>=2 regardless of newer messages; first_at never refreshed by re-tags.
Placeholders: [Obligation Repository]. Mappings: none new.
Objective: one write helper per marker + liveness predicates (incl. anchor clause) + counters with the spec'd resets.
Tests: monotonicity; liveness truth table; first_at; counter resets. Stop: merged.
```

```text
[IN-05] Feed transaction order
Read: §8 (transaction + layering) §16.2 (ack) §4.4. Invariant: inbox insert FIRST (no locks); ack strictly after commit; unmatched = log+count+ack+drop.
Placeholders: [Payment Status Feed Consumer] [Inbox / Processed Event Repository] [Request Status Persistence Layer] [Reservation Repository]. Mappings: consumer + inbox.
Objective: rebuild consumption: inbox → resolve (UETR primary) → lock → evidence CAS → amounts on rowCount 1 → re-derive → commit → ack.
Tests: duplicate short-circuit; concurrent in-flight duplicate (row-lock then dup-key); ack-after-commit crash test. Stop: merged.
```

```text
[IN-06] Fallback + unmatched policy
Read: §8 (fallback + rationale) §16.6 (recency config). Invariant: provider_reference fallback iff exactly ONE ACTIVE match ∧ amount equal ∧ within recency; zero/multiple → unmatched; NO parked-event storage.
Placeholders: [Payment Status Feed Consumer]. Mappings: IN-05 skeleton.
Objective: resolution chain + fail-closed fallback + unmatched path.
Tests: single/multi/amount/recency cases; unmatched logged+counted+acked. Stop: merged.
```

```text
[IN-07] Evidence application
Read: §4.4 §10.1 (terminal + mirror) §8 (totality, negatives) §9.4. Invariant: terminal → ANY active row; intermediate → non-CLAIMED only, BLOCKED preserved; stale → zero rows; reject sets its marker in the SAME transaction.
Placeholders: [Payment Status Feed Consumer] [Request Status Persistence Layer]. Mappings: CA-2 ranks.
Objective: ONE shared evidence helper (feed + resolver): settlement → RG-03; reject → REJECTED + flag + marker + release; acceptance → SUBMITTED/CONFIRM/READY on non-CLAIMED; anomaly + return-event rules.
Tests: each rule; race both orders; terminal-evidence CRITICAL. Stop: merged.
```

```text
[IN-08] Amount-mismatch park
Read: §8 (mismatch) §16.4 §13. Invariant: mismatch = defect evidence; BLOCKED(AMOUNT_MISMATCH) + SUB=SUBMITTED + CRITICAL; confirmed does not move; resolution external.
Placeholders: [Payment Status Feed Consumer] [Metrics / Alerting Layer]. Mappings: IN-07.
Objective: verify the park path end to end from feed evidence incl. later corrected event completing normally.
Tests: park; corrected-event completion; redelivery. Stop: merged.
```

```text
[IN-09] Kafka hardening
Read: §16.2 (whole). Invariant: manual ack after commit; earliest; ErrorHandlingDeserializer; DLT for poison only; no retry topics on money events.
Placeholders: both consumers, [Metrics / Alerting Layer]. Mappings: D-07 gap checklist.
Objective: close every checklist gap; retention-chain scheduled check; keying verified (feed by UETR, upstream by business_id) else concurrency 1.
Tests: poison→DLT; transient→in-place; offset-after-commit. Stop: checklist compliant.
```

```text
[RC-01] POST classifier
Read: §7.2 (whole) §7.3 §7.1; CA-1. Invariant: closed taxonomy; unmapped mid-call → MAYBE·CONFIRM·READY; unmapped code → MAYBE·BLOCKED(UNMAPPED_CODE); 200 classified from body.
Placeholders: [Provider Response Parser]. Mappings: parser.
Objective: data-driven classifier from CA-1 (externalized); fail-closed defaults; enrichment outcomes via §7.3.
Tests: fixture per CA-1 row + defaults. Stop: merged.
```

```text
[RC-02] Response-driven transitions
Read: §7.2 (every row) §10.5 (POST rows) §2.2 (divergent_payload_at) §7.1. Invariant: collision sets divergent_payload_at write-once then branches on the CLAIM-TIME divergence_expected flag; rejects never write uetr; marker totality on REJECTED.
Placeholders: [Provider Response Parser] [Request Status Persistence Layer]. Mappings: RC-01 wired.
Objective: implement each §7.2 row as its exact tuple CAS + side effects (anchors, markers, release on definitive rejects).
Tests: one per row; write-once; mirror rule; totality. Stop: merged.
```

```text
[RC-03] repost_permitted
Read: §7.0 (predicate + both ends + override) §6.4 §11. Invariant: ONE function; checked by every POST-routing writer AND the posting claim; blocked_reason plays no part; override = staleness term only, dual-control.
Placeholders: [Request Status Persistence Layer] [Retry Resolver Job] [Status Query Resolver] [Provider POST Client]. Mappings: claim site; writer list.
Objective: divergent_payload_at IS NULL ∧ pre-cutoff ∧ ¬(stale ∧ MAYBE) ∧ freeze OFF ∧ outcome IS NULL; wire both ends.
Tests: term-by-term; both-ends (laundered reason can't re-POST); override scope. Stop: merged.
```

```text
[RC-04] Retry scanner
Read: §7.4 (whole) §16.1 (scanner+suspension+poison) §16.6. Invariant: the DB scanner is the ONLY retry owner on the POST; freeze/breaker-OPEN time consumes no attempt/deadline budget; cutoff checks still apply at attempt time.
Placeholders: [Retry Resolver Job] [Metrics / Alerting Layer]. Mappings: job infra; S-07 expressions; stacked-retry inventory (remove).
Objective: breaker-gated bounded claims; per-class policy; exhaustion → BLOCKED (MAYBE rows keep submission_state); downgrade class (reset, now, small max); suspension; poison cap.
Tests: schedule math; exhaustion-with-MAYBE; suspension over simulated outage; poison cap. Stop: merged.
```

```text
[RC-05] Resolver sweep
Read: §9.5 (whole) §9 intro §16.6. Invariant: scope = ACTIVE ∧ (MAYBE any-stage/state ∪ SUBMITTED older than confirmation age), NEVER stage/history-scoped; MAYBE branch never damps; sweeps never overlap.
Placeholders: [Status Query Resolver] [Metrics / Alerting Layer]. Mappings: query client; job infra.
Objective: prioritized bounded sweep (cutoff first, oldest maybe_since), per-row next_query_at backoff, budget from rate limit, overrun metric, SUBMITTED damping vs feed-lag, ops-triggered explicit-key mode.
Tests: scope table; budget under herd; overlap; damping. Stop: merged.
```

```text
[RC-06] Query-outcome application
Read: §9.1 §9.4 (race) §4.4. Invariant: applied via the SAME shared evidence helper as the feed; INDETERMINATE ⇒ reschedule; clocks never pause during query outages.
Placeholders: [Status Query Resolver] [Request Status Persistence Layer]. Mappings: IN-07 helper.
Objective: EXECUTED/REJECTED/ACCEPTED/INDETERMINATE handling; NOT_FOUND → RC-07.
Tests: each outcome; resolver-vs-feed race both orders; outage → INDETERMINATE. Stop: merged.
```

```text
[RC-07] Trust-age + downgrade
Read: §9.2 (whole) §7.4 (downgrade class) §10.5. Invariant: age from last_post_attempt_at (MAYBE) / submitted_at (SUBMITTED), never state_changed_at; downgrade fires ONLY where repost_permitted passes; SUBMITTED NOT_FOUND → ENGINE_INCONSISTENCY park (single answer, reversible), NEVER a downgrade.
Placeholders: [Status Query Resolver] [Request Status Persistence Layer]. Mappings: RC-03/RC-04 in place.
Objective: pre-trust-age → INDETERMINATE; MAYBE+permitted → POST·RETRY_WAIT·MAYBE (now, reset, reason cleared); gate-fail → parked (resolver applies deferred AMENDMENT_PARKED for stale-amount unparked rows); SUBMITTED → park, stays in scope.
Tests: anchors; downgrade tuple; each gate-fail; deferred park; SUBMITTED reversibility; DUPLICATE answer to downgrade re-POST. Stop: merged.
```

```text
[RC-08] Escalation
Read: §9.3 (whole) §2.2 (escalated_at) §13 §16.6 (ordering). Invariant: fires once per MAYBE episode (escalated_at IS NULL gate); already-BLOCKED/CLAIMED rows: alert only, never overwrite blocked_reason; tier-2 on the same maybe_since clock.
Placeholders: escalation scanner, [Metrics / Alerting Layer]. Mappings: scanner infra; S-07 index.
Objective: scope outcome IS NULL ∧ MAYBE ∧ maybe_since over threshold → CRITICAL always; state write BLOCKED(ESCALATED)+escalated_at only if unescalated ∧ non-CLAIMED ∧ non-BLOCKED; tier-2 re-page.
Tests: once-per-episode (no downgrade⇄escalate cycle); alert-only paths; tier-2; frozen rows excluded. Stop: merged.
```

```text
[RC-09] Posting freeze
Read: §16.1 (freeze block) §15 (freeze page — later). Invariant: absent/unreachable/timeout = FROZEN; only FROZEN cached; checked before every claim AND every POST; stops POSTs only (feed/query/reads continue).
Placeholders: [Provider POST Client] [Retry Resolver Job], Hazelcast. Mappings: grid client; toggle shape.
Objective: bounded-timeout fail-safe read; toggle carries reason/operator/ticket; freeze-effective metric exposed.
Tests: three fail-safe conditions; no unfrozen caching; frozen blocks claim+POST; resolver unaffected. Stop: merged.
```

```text
[RC-10] Breakers + suspension
Read: §16.1 (breaker/suspension/bulkheads/timeouts) §16.6. Invariant: business rejects are breaker SUCCESSES; scanners gate on breaker; OPEN/freeze windows consume no budget; per-dependency breakers + timeouts.
Placeholders: [Provider POST Client] [Retry Resolver Job] [Status Query Resolver] [Metrics / Alerting Layer]. Mappings: breaker conventions.
Objective: breakers per dependency; scanner gating; end-to-end budget suspension; bulkhead verification.
Tests: reject-as-success; zero claims while OPEN; budget frozen; query-breaker → INDETERMINATE. Stop: merged. NOTE: auto-downgrade production enablement stays gated on P8 PASS.
```

```text
[OP-01] Verified-outcome procedure
Read: §9.3 (procedure) §10.1 §10.3 §20-8; CA-9. Invariant: dual control enforced IN the procedure; evidence flag set legitimately; refuses CLAIMED/terminal/mismatch; every use alerts; applies through the SAME evidence-guarded CAS.
Placeholders: [Operator Admin Procedure Area] [Stored Procedure / Trigger Area]. Mappings: role model can produce two authenticated identities (else BLOCKED).
Objective: implement CA-9 exactly; EXECUTED → RG-03 path; REJECTED → REJECTED+marker+release; §14 line trigger_source=OPS_PLATFORM_VERIFIED + ticket; restricted role.
Tests: in OP-02. Stop: deployed to test env.
```

```text
[OP-02] Procedure tests
Read: §9.3 §10.3; CA-9. Invariant: raw SQL fails where the procedure succeeds (trigger demonstrated); lane runs the REAL triggers.
Placeholders: [Integration Test Suite] [Operator Admin Procedure Area]. Mappings: Oracle lane + procedure.
Objective: both outcomes' money effects; all refusals; dual-control; guard interplay; wedge-opens assertion (scope completes / successor creates).
Tests: the suite. Stop: green on real Oracle; evidence filed.
```

```text
[OP-03] Ops drill
Read: §18-3 §20-8; CA-9 drill script. Invariant: drilled by REAL operators with the restricted role and a real ticket reference.
Placeholders: [Operator Admin Procedure Area]. Mappings: drill environment.
Objective: seed an unresolvable MAYBE row; run the full drill; verify outcome/alert/log; file the signed report.
Tests: none (drill). Stop: signed report; §18-3 satisfiable in Section Q.
```

```text
[OB-01] Drift scanner
Read: §3 (drift + invariants) §10.3 (L9) §15. Invariant: snapshot read + locked re-check BEFORE paging; read skew never pages; mismatch PAGES (not logs).
Placeholders: [Reconciliation / Drift Scanner] [Metrics / Alerting Layer]. Mappings: SCN/flashback availability (else UNCLEAR → DBA).
Objective: recompute I1/I2 per obligation; re-check under lock; page; verify L9 totality.
Tests: seeded I1/I2 violations page; read-skew non-page; L9 detection. Stop: merged.
```

```text
[OB-02] Reconciliation tripwires
Read: §8 (anomaly) §15 §12 (defensive rule). Invariant: NEW event_id + zero-row CAS on a TERMINAL row = CRITICAL; benign redelivery (known event_id) = silent skip.
Placeholders: [Payment Status Feed Consumer] [Metrics / Alerting Layer], card read path. Mappings: IN-07.
Objective: terminal-evidence CRITICAL routed; per-obligation count sanity ticket; card >1-row error+alert.
Tests: each fires; benign doesn't. Stop: merged.
```

```text
[OB-03] Money/MAYBE alerts
Read: §15 (list + clock discipline) §13 §2.1 §2.2. Invariant: AGE alerts read episode anchors ONLY (maybe_since, first_at, …), never state_changed_at.
Placeholders: [Metrics / Alerting Layer]. Mappings: metric conventions.
Objective: implement the money-facing §15 entries (MAYBE ages+tier-2, stuck reservation, BLOCKED queue, marker + counter alerts, latch alerts+age, MISMATCH/INCONSISTENCY CRITICALs, tie/latched-amendment alerts, live-marker-no-request age, procedure-use every-use alert, latch-integrity alert).
Tests: seeded condition per alert. Stop: merged.
```

```text
[OB-04] Flow/stuck alerts
Read: §15 (entries + stuck-state split) §12 (freshness) §16.2 (lag). Invariant: stuck-state split — retry states on retry_deadline_at, non-churning on state_changed_at; metric absence = bad.
Placeholders: [Metrics / Alerting Layer], card read path. Mappings: metric sources from IN/RC tasks.
Objective: unmatched volume, stale message/marker-write volumes, DLT page, consumer-lag page + card lag indicator, scanner heartbeats, stuck-state, sweep overrun, observed-lag watchdog, deadlock ticket, inbox growth, dead-gauge alerting.
Tests: seeded per alert; dead-gauge check. Stop: merged.
```

```text
[OB-05] Freeze page + rollup + retention check
Read: §15 (freeze entry + rollup) §16.1 §16.2. Invariant: freeze is silent by design — the freeze-effective-without-ticket page is the ONLY signal; one outage = ONE grouped incident (state writes stay per-row).
Placeholders: [Metrics / Alerting Layer]. Mappings: grouping capability (else emission-side rollup, record).
Objective: freeze page; breaker ticket + 30m page; root-cause rollup preserving per-alert detail; retention-chain scheduled check.
Tests: freeze page; storm groups to one incident; retention alert. Stop: merged.
```

```text
[OB-06] Runbooks + logging practices
Read: §15 (practices) §16.3 §14 (retention floor). Invariant: masking in the ENCODER; every alert carries a runbook link; one correlation id greps the whole story.
Placeholders: [Metrics / Alerting Layer]. Mappings: CA-8 stubs; encoder config.
Objective: link runbooks; encoder masking verified end to end; MDC + outbound-header propagation; retention ≥ 90-day floor verified (report if below).
Tests: masking capture test; MDC through an async hop. Stop: merged.
```

```text
[OB-07] Config inventory + validation
Read: §16.6 (inventory + ordering rule) §16.5. Invariant: loader REJECTS unless trust_age + cadence < escalation < tier-2 < cutoff margin; load-bearing values never silently default.
Placeholders: config, [Metrics / Alerting Layer]. Mappings: config conventions.
Objective: one namespace for every §16.6 entry with owner notes; startup ordering validation; retry-policy completeness vs CA-1 classes.
Tests: each mis-ordering rejected; valid set accepted; missing entry fails startup. Stop: merged.
```

```text
[GO-01] Rollout plan
Read: playbook Section M; §16.5 §18. Invariant: Section M's stage ORDER is fixed; auto-downgrade last, gated on P8 PASS; flags default-off.
Placeholders: [DB Migration Directory], pipeline. Mappings: promotion path; flag mechanism.
Objective: environment-specific plan: per stage owner, checkpoint evidence, rollback trigger/procedure; wire enablement flags.
Tests: flag-off smoke per flag. Stop: plan approved.
```

```text
[GO-02] Shadow validation
Read: §10.4; playbook Section M (shadow stage). Invariant: every tuple/legacy disagreement is a bug or mapping error — disposition each, no thresholds waved through.
Placeholders: [Metrics / Alerting Layer] [Request Status Persistence Layer]. Mappings: dual-write live.
Objective: soak-window comparison job (tuple-derived label vs legacy; derived step status vs legacy); itemized disagreement report; fix + re-soak.
Tests: comparator detects seeded disagreement. Stop: clean soak report filed.
```

```text
[GO-03] Staged enablement
Read: playbook Section M (order + per-stage validation) §9.2 §18-1. Invariant: auto-downgrade enablement requires CT-02..05 PASS + observed-lag watchdog live + TL-5-derived trust age (not the stub).
Placeholders: all (config-driven). Mappings: GO-01 flags.
Objective: enable stage → validate → soak → proceed, per Section M.
Tests: stage checklists (existing suites). Stop: all stages enabled + soaked; sign-offs recorded.
```

```text
[GO-04] Go-live gates
Read: playbook Section Q; §18 (BLOCKING items). Invariant: §18 items 0–3 are non-waivable; every PASS carries linked evidence.
Placeholders: none. Mappings: none.
Objective: execute Section Q; deliver evidence pack; obtain signed go/no-go.
Tests: none. Stop: decision recorded.
```

```text
[GO-05] Rollback validation
Read: playbook Section M (rollback) §16.5. Invariant: rollback is flags-off + state stands; no down-migration of money data; old version must run against final schema.
Placeholders: [DB Migration Directory], pipeline. Mappings: GO-01 stages.
Objective: rehearse app-rollback during dual-run, per-stage flag-off (incl. mid-incident under load), document the point of no return.
Tests: rehearsal scripts where automatable. Stop: rehearsals recorded.
```

---

# J. Test-first strategy and test matrix

Strategy: every behavior task card lists its tests; where practical
write the test first against the § it implements (red), implement
(green), then run the surrounding suite. Types: UNIT / INTEGRATION
(real Oracle + Kafka where constraints/consumption are under test) /
CONTRACT (sandbox or build-time schema) / MIGRATION / RECOVERY
(crash/fault injection) / CONCURRENCY / OPERATIONAL (drill, seeded
alert). This matrix seeds CA-7; IDs are stable. "Blocking" = must be
green for go-live (Section Q).

### T-01 — Deterministic key generation

```text
Section: §5.1        Type: UNIT           Blocking: YES
Purpose: key derived from business state via CA-5, never random.
Setup:   fixed scope fields + seq; CA-5 spec loaded.
Action:  derive twice, across JVM restarts.
Expect:  identical keys; derivation uses only the CA-5 input list.
Failure: generation is random or environment-dependent → DR keystone
         broken; a restore would mint fresh keys → duplicate payment.
Implemented by: K-02.
```

### T-02 — Key byte-exactness / golden vectors

```text
Section: §5.1, §16.6-5   Type: UNIT       Blocking: YES
Purpose: freeze the derivation to the byte across releases.
Setup:   CA-5 golden-vector file (authored independently of the code).
Action:  derive each vector's inputs.
Expect:  exact expected bytes, every vector; version constant pinned.
Failure: canonicalization/encoding drift → post-restore or
         cross-release keys diverge → engine dedup silently misses.
Implemented by: K-03.
```

### T-03 — Key persistence before POST

```text
Section: §5, §11      Type: RECOVERY (fault injection)   Blocking: YES
Purpose: no POST under an unpersisted caller-supplied identity.
Setup:   posting path with stub engine; kill point between claim
         commit and HTTP call.
Action:  drive a payment; kill at the point; inspect DB + stub.
Expect:  row holds identity+hash (committed); stub received nothing.
Failure: identity written late → a crash leaves an untraceable
         possible submission; §9 recovery has no key to query.
Implemented by: K-04.
```

### T-04 — Same business input → same key

```text
Section: §5.1         Type: UNIT           Blocking: YES
Purpose: re-creation of the same logical request regenerates the key.
Setup:   same obligation state (same seq about to be consumed).
Action:  create, roll back harness-side, re-create.
Expect:  identical key both times.
Failure: hidden nondeterministic input in the derivation.
Implemented by: K-02, K-06(3).
```

### T-05 — Different sequence/business identity → different key

```text
Section: §5.1         Type: UNIT           Blocking: YES
Purpose: key space separates attempts and scopes.
Setup:   vary request_seq; vary each scope field independently.
Action:  derive.
Expect:  distinct keys per variation.
Failure: collision across seq/scope → I6-adjacent identity collision;
         engine dedup would swallow a legitimate new payment.
Implemented by: K-02.
```

### T-06 — Amount is not part of the key

```text
Section: §5.1         Type: UNIT           Blocking: YES
Purpose: replay with a diverged amount must regenerate the SAME key
         (routed to §7.2, never silently re-keyed).
Setup:   same scope+seq, two amounts.
Action:  derive.
Expect:  identical keys.
Failure: amount hashed in → post-restore divergent replay mints a
         fresh key → duplicate payment (the exact §5.1 rationale).
Implemented by: K-02.
```

### T-07 — last_sent_hash / divergence_expected behavior

```text
Section: §2.2, §7.0   Type: INTEGRATION    Blocking: YES
Purpose: flag computed at claim time against the PRIOR hash; drives
         the §7.2 collision branch.
Setup:   stubbed enrichment permitting controlled assembly changes.
Action:  attempt 1; attempt 2 unchanged; attempt 3 with changed detail.
Expect:  flags false / false / true; hashes persisted pre-wire each
         attempt; last_post_attempt_at stamped pre-wire.
Failure: flag wrong → expected divergence parks as CRITICAL (ops
         noise) or replay anomaly sails through as expected (missed
         defect).
Implemented by: K-05.
```

### T-08 — Retry after crash BEFORE provider POST

```text
Section: §5, §11      Type: RECOVERY       Blocking: YES
Purpose: crash between claim commit and wire → safe re-attempt.
Setup:   kill point pre-wire; lease expiry configured short.
Action:  crash; let the lease expire; observe recovery.
Expect:  row → CONFIRM·READY·MAYBE (no carve-out); resolver queries;
         NOT_FOUND path eventually re-POSTs the SAME key via §9.2
         where permitted.
Failure: re-claim for posting or a fresh key → double-payment path.
Implemented by: ST-10, RC-07, K-06(1).
```

### T-09 — Retry after crash AFTER provider POST

```text
Section: §7.2, §9, §11   Type: RECOVERY    Blocking: YES
Purpose: crash after wire, before response processing.
Setup:   stub that accepts, kill point post-wire pre-processing.
Action:  crash; lease expiry; resolver runs against the stub.
Expect:  row MAYBE; resolver query finds EXECUTED-class → settlement
         applies through the evidence path; reservation confirmed.
Failure: blind re-POST, or the outcome never recovered → §9's
         ask-not-retry principle broken.
Implemented by: ST-10, RC-05/06, K-06(2).
```

### T-10 — DB restore / lost local row

```text
Section: §5.1         Type: RECOVERY (harness-simulated)  Blocking: YES
Purpose: a restore-recreated request regenerates the SAME key and the
         engine collision routes to §7.2, not a new payment.
Setup:   full flow to POSTed against a recording stub; harness resets
         DB rows to the pre-insert image (simulated restore).
Action:  re-drive creation for the same shortfall; POST.
Expect:  identical key on the wire; stub's duplicate answer routes to
         MAYBE + query (§7.2); no second execution recorded.
Failure: fresh key → the exact restore-duplicate §5.1 exists to kill.
Implemented by: K-06(3).
```

### T-11 — Known key + same payload (engine contract)

```text
Section: §18-1(a), §1   Type: CONTRACT (sandbox)   Blocking: YES
Purpose: prove the assumed contract fact: identical re-POST never
         executes twice.
Setup:   CT-01 harness.
Action:  POST; re-POST byte-identical.
Expect:  deduped/acked or original-response replay; ONE execution.
Failure: §18-1 FAILS — the entire re-POST design (fresh assembly,
         §9.2 downgrade) is unsafe; go-live blocked; escalate.
Implemented by: CT-02.
```

### T-12 — Known key + divergent payload (engine contract)

```text
Section: §18-1(b), TL-4   Type: CONTRACT (sandbox)   Blocking: YES
Purpose: divergent re-POST rejected without execution, code
         distinguishable from plain DUPLICATE_REQUEST.
Setup:   CT-01 harness.
Action:  POST; re-POST with changed field; separately changed amount.
Expect:  rejection, no execution, distinct code captured for CA-1.
Failure: execution → double-pay path; §7.0 must revert to payload
         freeze (TL-4) — a design decision, halt and escalate.
Implemented by: CT-03.
```

### T-13 — Rejected-outcome retention (engine query)

```text
Section: §9.1, §18-1(d), TL-6   Type: CONTRACT (sandbox)   Blocking: YES
Purpose: rejected outcomes are retained and queryable; re-POST
         behavior after a sync business reject is KNOWN.
Setup:   CT-01 harness; inducible business reject.
Action:  induce reject; query by key; re-POST same key.
Expect:  query returns a REJECTED-class answer; re-POST behavior
         recorded (re-executes vs replays) and fed to retry policy.
Failure: rejects unqueryable → §9 recovery blind for rejects;
         unknown re-POST behavior → retry policy built on assumption.
Implemented by: CT-05, CT-06.
```

### T-14 — Provider query lookback

```text
Section: TL-5, §9.3    Type: CONTRACT (sandbox/provider-assisted)   Blocking: YES
Purpose: lookback ≥ max row lifetime incl. ops-queue SLA — past it,
         NOT_FOUND is unfalsifiable and resolve-via-query can never
         succeed.
Setup:   B-02's written lookback; aged sandbox data where achievable.
Action:  query the oldest achievable executed payment by key.
Expect:  found within the stated lookback; statement + evidence filed;
         if lookback < max lifetime → §18-3 alternative dies and the
         OP procedure is mandatory (it is anyway, by default).
Failure: unverified lookback → aged MAYBE rows may be permanently
         unresolvable by query.
Implemented by: CT-04/CT-06 evidence + B-02.
```

### T-15 — UETR persisted from acceptance-class only

```text
Section: §5           Type: INTEGRATION    Blocking: YES
Purpose: acceptance (incl. original-response replay) persists the
         SDK-minted UETR for feed matching.
Setup:   stub returning acceptance with a UETR.
Action:  POST; inspect row; deliver a feed event under that UETR.
Expect:  uetr persisted; feed event matches and settles.
Failure: no UETR → all feed events unmatched; recovery only at sweep
         latency (degraded, not unsafe — but §5's design intent).
Implemented by: U-01, U-03.
```

### T-16 — UETR NOT persisted from rejection/collision

```text
Section: §5, §7.2, §16.6-6   Type: INTEGRATION   Blocking: YES
Purpose: dead UETRs never persisted or overwritten — a feed reject
         under a dead UETR must never release a real payment's
         reservation.
Setup:   stub returning DUPLICATE_REQUEST / collision / sync reject,
         each carrying a UETR; one row with a prior persisted uetr.
Action:  drive each response class.
Expect:  uetr unchanged (prior value or NULL) in every case; a feed
         event under the dead UETR goes unmatched.
Failure: dead UETR persisted → orphaned real feed events + possible
         false authoritative negative → double payment.
Implemented by: U-01, U-03.
```

### T-17 — Duplicate prevention (end to end)

```text
Section: §5, §2.2, §6.8   Type: INTEGRATION   Blocking: YES
Purpose: the layered protections hold: UNIQUE(idempotency_key), I6,
         single creation point, engine dedup handling.
Setup:   full local flow with stub engine.
Action:  attempt to create a second active request for one obligation;
         attempt to POST a duplicated key.
Expect:  I6 rejects the second active row; duplicate key POST answers
         route to §7.2 MAYBE+query; no path creates two executions.
Failure: any silent second execution path.
Implemented by: K-06, S-05, RG-06.
```

### T-18 — Concurrent workers

```text
Section: §11          Type: CONCURRENCY    Blocking: YES
Purpose: claims, CAS row counts, and SKIP LOCKED make concurrent
         scanners safe.
Setup:   two scanner instances; seeded READY/RETRY_WAIT rows; two
         concurrent feed duplicates (rebalance case).
Action:  run concurrently.
Expect:  each row processed once; losers see row-count 0 / duplicate
         key; no deadlocks beyond retried ORA-00060.
Failure: double processing → duplicate POST risk; lock-order
         violation → deadlock storms (§15 tripwire).
Implemented by: ST-09, IN-05.
```

### T-19 — Stale request state (evidence + mirror rules)

```text
Section: §4.4, §10.1, §6.9   Type: INTEGRATION   Blocking: YES
Purpose: stale/duplicate/racing writes affect zero rows.
Setup:   rows in terminal and post-evidence states; replayed and
         re-keyed duplicate events; late POST responses.
Action:  apply each stale input.
Expect:  zero-row CAS everywhere; terminal rows unchanged (freeze);
         markers not overwritten by stale ordering values.
Failure: a stale input regressing state → the entire evidence
         model's guarantee is void.
Implemented by: ST-02/03, IN-04, IN-07.
```

### T-20 — Amount amendment after request creation

```text
Section: §6.3, §6.4, §6.8   Type: INTEGRATION   Blocking: YES
Purpose: amendments defer/cancel/park correctly; amounts immutable
         per request.
Setup:   in-flight requests at each cancellable/uncancellable state.
Action:  apply increase and decrease amendments.
Expect:  increase → deferred successor (never lost); decrease →
         auto-cancel where NOT_SUBMITTED-cancellable; MAYBE → parked
         AMENDMENT_PARKED; POST·CLAIMED deferred to retry-guard;
         request.amount never mutated.
Failure: lost deferral (underpayment), or a MAYBE release
         (double-pay), or in-place amount mutation.
Implemented by: RG-06, RG-07.
```

### T-21 — Cutoff passed

```text
Section: §7.4, §7.0, §16.4   Type: INTEGRATION   Blocking: YES
Purpose: cutoff blocks attempts and downgrades; tz-aware comparison.
Setup:   cutoff calendar fixture with a market past cutoff (tz-aware,
         incl. a DST boundary case).
Action:  retry scanner + §9.2 downgrade candidates against it.
Expect:  attempts blocked (CUTOFF_EXPIRED); downgrade gate fails;
         comparisons correct across the DST case.
Failure: post-cutoff POST (bank-close violation) or a whole currency
         blocked an hour early (§18-2's warning).
Implemented by: RC-04, RC-03; config from B-03.
```

### T-22 — MAYBE_SUBMITTED recovery (full lifecycle)

```text
Section: §9.1–9.5     Type: INTEGRATION    Blocking: YES
Purpose: the ask-then-retry loop resolves every MAYBE class.
Setup:   MAYBE rows: fresh; aged past trust-age; parked
         (AMENDMENT_PARKED); escalated; SUBMITTED aged.
Action:  resolver sweeps against a scripted stub (EXECUTED / REJECTED
         / NOT_FOUND / INDETERMINATE / ACCEPTED sequences).
Expect:  per §9.1/§9.2: settle / reject+release / downgrade-or-wait /
         reschedule / tighten; escalation fires once per episode;
         parked rows keep being queried (§9.5 scope).
Failure: any MAYBE class with no exit → wedge (see T-28); scope
         misses parked rows → the four-round scoping lesson repeats.
Implemented by: RC-05..08.
```

### T-23 — Repost forbidden by staleness or cutoff

```text
Section: §7.0, §6.4, §9.2   Type: INTEGRATION   Blocking: YES
Purpose: repost_permitted's terms hold at BOTH ends; no livelock.
Setup:   MAYBE rows with: stale amount; passed cutoff; set
         divergent_payload_at; freeze ON.
Action:  attempt downgrade, ops-style un-park, and direct claim.
Expect:  each denied at the writer AND (if forced) at the claim; rows
         stay parked wait-then-decide; no park⇄un-park cycling.
Failure: laundered blocked_reason or writer bug reaching the wire.
Implemented by: RC-03, RC-07.
```

### T-24 — apply-platform-verified-outcome procedure

```text
Section: §9.3, §16.6-8   Type: INTEGRATION + OPERATIONAL   Blocking: YES
Purpose: the MVP terminal exit works and refuses correctly.
Setup:   real Oracle with S-06 triggers; seeded MAYBE rows.
Action:  execute with valid dual-control inputs (both outcomes); then
         each refusal case (CLAIMED, terminal, same approver twice,
         missing ticket, amount mismatch).
Expect:  outcomes applied with money effects + alert + audit line;
         every refusal refuses; raw SQL equivalent fails on the
         trigger.
Failure: procedure bypassable or refusals soft → the single
         sanctioned manual path isn't the single path.
Implemented by: OP-01/02; drilled by OP-03.
```

### T-25 — Evidence session flag / release guard

```text
Section: §10.3, §10.1   Type: INTEGRATION (real Oracle)   Blocking: YES
Purpose: the trigger backstop rejects unflagged terminal-negative
         writes on MAYBE/SUBMITTED rows; flag never leaks.
Setup:   S-06 deployed; pooled connections.
Action:  unflagged UPDATE (refused); flagged via the authoritative
         path (accepted); flag-leak probe across two pooled sessions.
Expect:  per the above; fat-finger SQL fails loudly.
Failure: guard bypass or cross-session leak → silent money release.
Implemented by: S-06, RG-05.
```

### T-26 — Reservation release

```text
Section: §3           Type: INTEGRATION    Blocking: YES
Purpose: −committed exactly once, same transaction, terminal-negative
         only.
Setup:   active requests reaching REJECTED / CANCELLED / SUPERSEDED,
         with event redelivery.
Action:  drive each; redeliver.
Expect:  one decrement each; redelivery zero-row → no second move;
         I1 holds throughout.
Failure: double release → shortfall reopens → double payment via
         §6.8.
Implemented by: RG-02.
```

### T-27 — Reservation confirmation / consumed state

```text
Section: §3, §4.1     Type: INTEGRATION    Blocking: YES
Purpose: settlement confirms exactly; completion derives; the
         "accepted money" derived figure needs no counter.
Setup:   requests settling normally and after amendment increases.
Action:  settle; inspect amounts + derived status.
Expect:  confirmed += amount once; committed unchanged by settlement
         (EXECUTED rows stay in I1); step completes only when §4.1
         holds; I2/I3 hold.
Failure: confirmation double-applied or completion derived early.
Implemented by: RG-03, RG-08.
```

### T-28 — Permanent wedge prevention

```text
Section: §18-3, §9.3, §4.1, §6.8   Type: INTEGRATION   Blocking: YES
Purpose: NO MAYBE row is permanently wedged at MVP.
Setup:   the worst row: divergent_payload_at set + cutoff passed +
         stale amount + (simulated) key past query lookback.
Action:  walk the escalation path to the OP-01 procedure; apply a
         platform-verified REJECTED; then a remaining shortfall.
Expect:  reservation released legitimately; scope completes or
         re-pays under a NEW key via §6.8 (guards permitting); I6
         frees.
Failure: any state in the chain with no sanctioned exit → the §18-3
         wedge exists.
Implemented by: OP-02's wedge assertion + RC-07/RC-08.
```

### T-29 — Drift scanner

```text
Section: §3, §10.3 L9   Type: INTEGRATION + OPERATIONAL   Blocking: YES
Purpose: I1/I2 violations page; read skew does not; L9 verified.
Setup:   seeded counter corruption; concurrent uncommitted create;
         REJECTED row missing its marker.
Action:  run the scan.
Expect:  corruption pages after locked re-check; skew does not page;
         L9 violation detected.
Failure: silent drift → the deliberate counter redundancy pays
         nothing.
Implemented by: OB-01.
```

### T-30 — Reconciliation tripwire

```text
Section: §8, §15      Type: INTEGRATION    Blocking: YES
Purpose: evidence-for-terminal is CRITICAL; benign redeliveries are
         silent.
Setup:   terminal REJECTED row; new-event_id settlement for it;
         known-event_id redelivery.
Action:  deliver both.
Expect:  new event → CRITICAL alert (zero-row CAS detected); known
         event → silent inbox skip.
Failure: the replay-divergence signature (a §5.2 tripwire) missed, or
         redelivery noise paging humans.
Implemented by: OB-02, IN-07.
```

### T-31 — UI/card step status correctness

```text
Section: §12, §4.1, §4.2, §10.4   Type: INTEGRATION   Blocking: YES
Purpose: the card never lies: no false completion, correct exception
         precedence, defensive lookup.
Setup:   scopes at each §4-derivable state incl. anchors, MAYBE rows,
         latched overpay, reopened steps; a seeded duplicate
         business_id pair.
Action:  read through the card path.
Expect:  NOT_STARTED = absence; anchors show DATA_VALIDATION_FAILED;
         MAYBE shows PAYMENT_OUTCOME_UNKNOWN (rank 1, never
         SYSTEM_UNAVAILABLE); labels per §10.4; >1 obligation → error
         state + alert; unavailable ≠ stale-as-authoritative.
Failure: false completion (the predicate's whole point) or a silent
         pick between duplicate scopes.
Implemented by: RG-08/09, ST-04, OB-02.
```

### T-32 — Observability / alerting

```text
Section: §15, §16.6   Type: OPERATIONAL    Blocking: YES
Purpose: the alert surface works before rollout relies on it.
Setup:   seeded condition per §15 entry (table-driven); a simulated
         breaker-OPEN storm; a dead gauge; a mis-ordered config set.
Action:  fire each.
Expect:  every alert fires on its anchor clock with its runbook link;
         storm groups to ONE incident preserving detail; dead gauge
         alerts; config loader rejects the bad ordering at startup.
Failure: silent alert gaps discovered during a real incident instead.
Implemented by: OB-03..07.
```

---

# K. Provider / tech-lead / PO contract questions

Priorities: BLOCKING (go-live gate) / HIGH (load-bearing config or
safety margin) / MEDIUM (correctness of a secondary path) / LOW
(convenience) / FUTURE (post-MVP). Every answer is recorded verbatim
with source and date; §18-1-family answers close ONLY via the CT
tests.

| ID | Priority | To | Question | Consumed by |
|----|----------|----|----------|-------------|
| Q-01 | BLOCKING | Upstream + UI teams + PO | §18-0 residue (model = §1 contract fact: multiple payments per trade; snapshot messages, newer overwrites older; tuple unique within snapshot — no discriminator): written confirmation of snapshot schema + uniqueness (upstream ask 5); PO-9 absence semantics (BA-2 amendment); TL-16 ordering-watermark rule; TL-2 step-granularity clause (§12). | B-01 → IN-02 (schema/identity tasks not gated) |
| Q-02 | BLOCKING | Provider (by sandbox test) | §18-1(a): can a known idempotency key + IDENTICAL payload ever execute twice? | CT-02 |
| Q-03 | BLOCKING | Provider (by sandbox test) | §18-1(b)/TL-4: can a known key + DIVERGENT payload execute? Is the rejection code distinguishable from plain DUPLICATE_REQUEST? | CT-03, CA-1, §7.2 branch |
| Q-04 | BLOCKING | Provider | §18-1(c): key-retention TTL IN WRITING; is TTL ≥ max row lifetime incl. ops-queue SLA, weekends, holidays, incidents, cutoff constraints? Verified at the retention edge? | CT-04; repost_permitted TTL term if short |
| Q-05 | BLOCKING | Provider (by sandbox test) | §18-1(d)/TL-6: after a synchronous business rejection, does a same-key re-POST re-execute or replay the cached rejection? | CT-05; §7.1 retry policy |
| Q-06 | BLOCKING | Calendar owner / PO | §18-2: payment cutoff calendar — source system, named owner, per-currency/market + holiday semantics, tz-aware representation, refresh cadence, stale-calendar fail direction? | B-03 → RC-03/04, §9.2 lookback guard |
| Q-07 | BLOCKING | Platform + tech lead | §18-3: is the apply-platform-verified-outcome procedure the confirmed MVP exit (default), or are TL-10 AND TL-5-lookback both affirmed in writing (the only de-scope condition, PO re-confirmation required)? | B-04 → OP-xx |
| Q-08 | HIGH | Provider | Does the engine return a collision / duplicate / prior-outcome signal distinct enough to drive §7.2's three duplicate-family branches? Full error-code list for CA-1, incl. the replay-original-response class. | CA-1, RC-01/02 |
| Q-09 | HIGH | Provider | Are rejected outcomes retained and queryable via the status-query API, and for how long? | CA-3, RC-06, T-13 |
| Q-10 | HIGH | Provider | Query lookback duration — is it ≥ the maximum possible local MAYBE-row lifetime including ops-queue SLA, weekends, holidays, incidents, cutoff constraints (TL-5 framing — parked rows live days)? | §9.3 resolve-via-query viability, §18-3 alternative |
| Q-11 | HIGH | Provider | Maximum ingest lag between POST acceptance and query visibility, as a DISTRIBUTION (p50/p99/max) — sets NOT_FOUND_TRUST_AGE (§9.2). If no contractual bound: state so; trust-age set conservatively + §15 observed-lag watchdog carries the residual. | OB-07 config, RC-07 |
| Q-12 | HIGH | Provider / platform | TL-10: can the platform formally REJECT a pending/never-received payment by UETR (or by idempotency key for rows that never received one) so the negative flows back as authoritative feed/query evidence? | §9.3 ops exits; cleaner path than the procedure |
| Q-13 | HIGH | Provider / SDK team | TL-11: (a) does the validate-and-POST response return the generated UETR, and in which field? (b) does the SDK accept our caller-supplied idempotency key? (c) does engine dedup key on that caller key (not the UETR)? — (c) is blocking-grade. | CT-07, U-01, K-04 |
| Q-14 | HIGH | Provider | Whether the engine deduplicates by caller-supplied idempotency key, UETR, both, or neither — asked explicitly even though (c) above implies it; the answer must be verified by CT-02/CT-07, not accepted in writing alone. | §5, CT suite |
| Q-15 | HIGH | Provider | Status-query API rate limit / quota — sizes the §9.5 sweep budget (TL-13; as load-bearing as ingest lag). | RC-05, OB-07 |
| Q-16 | HIGH | Upstream | Upstream asks 1–4: strictly-increasing ordering per business_id (until the explicit sequence field); business_id as the Kafka message key BY CONTRACT; the §6.0 schema formalized (field names incl. the ordering field, types, correlation_id); emission only on real business change (no blind re-emissions). | IN-01/02, §6.7 tie handling, §6.6 anchoring (TL-7) |
| Q-17 | MEDIUM | Provider | TL-12: provider_reference uniqueness scope and lifetime (global? per day/batch/rail?). Until confirmed globally unique, §8's fail-closed fallback stands; if confirmed, guards may be relaxed by explicit decision. | IN-06, U-02 |
| Q-18 | MEDIUM | Provider | TL-1: does the status feed carry a stable, unique event_id per event? If not: choose synthesis (payload hash vs topic+partition+offset) and accept its dedup blind spots. | CA-2, IN-05 |
| Q-19 | MEDIUM | Tech lead / UI team | TL-2: card read contract — query API vs replica/view, field list (step timestamps; retry progress "next attempt at / attempt N of M"), freshness SLA incl. replica lag, authentication, volume. And PO-5: step display for a trade cancelled after the step started (currently "completed" — acceptable?). | §12 read surface, OB-04 lag indicator |
| Q-20 | MEDIUM | PO / tech lead | Remaining §18 sign-offs: PO-1 ask-then-retry approval; PO-2 query cadence (suggest 2m); PO-3 escalation age (suggest 30m, must clear cutoff); PO-4 cutoff-passes-while-MAYBE behavior; PO-6 deferred-successor latency acceptance; PO-8 fresh-assembly consequence acceptance; TL-3 RPO/RTO + §5.2 runbook ownership (post-MVP); TL-8 confirmation-age owner+value; TL-9 artifact owners; TL-14 terminal-row archival co-design; TL-15 first-quarter NOT_FOUND-after-trust-age measurement. §19.3/PO-7 (retry-after-reject) = FUTURE. | OB-07 config owners; Section Q risk register |

---

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
  control enforcement IN the procedure; evidence-flag mechanics;
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

---

# M. Migration / rollout / rollback plan

Source-code-agnostic; GO-01 localizes it. Governing rule: §16.5
expand/contract — two app versions run concurrently during rollout;
new columns nullable-with-default first; drops only after the old
version is gone; claim semantics version-compatible across one
release boundary.

### M.1 Schema migration sequencing

```text
1. Additive columns (S-02, S-03) + inbox table (S-04) — old version
   unaffected (nullable/defaulted).
2. UNIQUEs + I6 (S-05 part) — verify no legacy violations first
   (S-02's duplicate-scope check; S-08 ordering for constraints that
   need backfill).
3. Backfill dimensions + anchors (S-08), quiet window, idempotent.
4. CHECK constraints NOVALIDATE → VALIDATE after backfill (S-05).
5. Triggers (S-06) — freeze + release guard; app code that legally
   writes terminal-negatives must already set the evidence flag
   (deploy order: flag-setting code BEFORE trigger activation, or
   activate triggers in the same release with the code — record
   locally which).
6. Scan indexes (S-07).
7. CONTRACT steps (drop/NOT NULL tightening of legacy columns):
   deferred until after full rollout + soak; a separate,
   individually-approved migration set. The legacy status column
   is dropped LAST, after ST-05 shows zero rule sites and GO-02's
   soak confirms label parity.
```

### M.2 Feature flag strategy

```text
Structural changes (dual-write, CAS discipline, derivation) ship as
code — no flag (they are behavior-preserving against the legacy
representation and proven by GO-02's shadow comparison).
Config-flagged enablement (default OFF):
  F1 new-machinery scanners (retry scanner in new claim/policy mode)
  F2 resolver sweep (query-based recovery)
  F3 escalation scanner
  F4 §9.2 auto-downgrade   ← LAST; requires P8 PASS + TL-5 trust age
  F5 posting-freeze enforcement mode (check-and-block vs check-and-log
     during early soak — must reach BLOCK before F1..F4 ship traffic)
Kill behavior: each flag OFF returns that mechanism to inert; rows
remain valid states (parked/waiting), recoverable when re-enabled.
```

### M.3 Dual-read / shadow / dry-run

```text
GO-02: dual-write soak with tuple-vs-legacy comparison (report must
be CLEAN, every disagreement dispositioned).
Derivation shadow: derived ui_step_status/exception vs legacy display
behavior compared on real traffic before the card reads switch.
Resolver dry-run mode (recommended, cheap): sweep + query + LOG the
would-be action without writing, one soak window, before F2 flips to
write mode — validates scope + mapping against production answers.
```

### M.4 Safe enablement order (GO-03 executes)

```text
1. Schema through VALIDATE + triggers live (M.1) — raw-SQL guard
   demonstrated in the environment.
2. Dual-write code + CAS discipline + derivation live (structural).
3. GO-02 shadow soak → clean.
4. Card/display readers on derived labels (ST-04) — display-only risk.
5. F5 freeze enforcement → BLOCK mode; freeze-effective page live.
6. Observability pack (OB-03..07) + runbook links live.
7. F1 retry scanner (new mode). Soak: retry outcomes sane, no
   stacked-retry double-fires.
8. F2 resolver (dry-run → write). Soak: query volume within budget,
   outcomes applied correctly.
9. F3 escalation. Soak: no false escalation storms.
10. F4 auto-downgrade — ONLY after CT-02..05 PASS on file, TL-5-derived
    trust age configured, observed-lag watchdog live.
11. OP-01 procedure deployed + OP-03 drill done (any time after step
    1; MUST be before go-live).
12. GO-04 gates → go-live.
```

### M.5 Existing rows and backfill

```text
S-08 is the backfill: legacy → tuple per the reviewed map; ambiguous
wire-capable states → MAYBE_SUBMITTED (fail toward the resolver);
anchors set defensibly; anomalies (status contradicts money) listed
for human disposition, never auto-resolved. Rows created by the OLD
version during dual-run are covered by the same map (columns nullable
until contract). Legacy in-flight rows keep their persisted
idempotency keys forever (K-02 rule).
```

### M.6 Rollback constraints

```text
- Any stage before F1: rollback = deploy old version. Expand/contract
  guarantees schema compatibility (S-09 proof).
- F1–F3 enabled: rollback = flags OFF. State written so far is valid
  and stands; rows in new-only states (e.g. BLOCKED(ESCALATED),
  parked AMENDMENT_PARKED) wait — they are wait-then-decide states by
  design; ops handles per runbook if the pause is long.
- POINT OF NO RETURN: once the new machinery writes money-affecting
  outcomes (terminal-negatives with releases, settlements with
  confirms) in production, there is NO data un-migration: rollback is
  flags OFF + the recorded state stands; recovery of any disputed row
  goes through the platform-verified paths (§9.3), never through
  reverse-migration. Schema stays; drops were deferred anyway (M.1.7).
- Never roll back the schema VALIDATE/trigger steps while the new
  code runs — the code assumes the backstops exist.
```

### M.7 Data compatibility

```text
Old version ignores new columns (nullable); new version tolerates
legacy-only rows via the S-08 map + defensive enum reads (UNKNOWN
sentinel — §16.5); enum evolution (adding a blocked_reason value)
follows the §16.5 NOVALIDATE swap procedure; the four dimension enums
are CLOSED — extending one is a design change with a review round,
not a migration.
```

### M.8 Protecting existing payment behavior

```text
- Business logic components ([Payment Enrichment Component], payment
  construction, decision rules) are READ-ONLY throughout (Section B).
- D-11's baseline green bar re-runs at every stage checkpoint;
  failures in preserved tests = stop-the-line.
- ST-05 re-keying is behavior-preserving by test (verdict-matrix pins).
- The §16.5 contract tests guard the three external contracts.
```

### M.9 Validating before enabling automated recovery

```text
F2 (resolver) precondition: CA-3 verified (CT-06); evidence helper
race tests green (T-19); dry-run soak clean.
F4 (auto-downgrade) precondition: §18-1 PASS (T-11/T-12), TL-5 trust
age (Q-11) configured, repost_permitted both-ends tests green (T-23),
observed-lag watchdog live.
Until F4, NOT_FOUND-after-trust-age rows wait for ops-triggered
downgrade decisions (the §9.2 ops action) — safe, more manual.
```

### M.10 What cannot be rolled back after money-affecting writes begin

```text
Released reservations, confirmed amounts, terminal outcomes, and their
downstream successor creations are FACTS once written — protected by
write-once outcome + freeze trigger by design. Any correction is a
forward operation through sanctioned paths (§9.3 procedure at MVP;
§19.2-family platform reconciliation). Plan reviews at GO-05 must
name this line explicitly per environment.
```

---

# N. Observability, reconciliation, and runbook plan

Implemented by OB-03..07; runbook stubs live in CA-8 — the blocks
below are their seeds. Global rules (§15): ages on episode anchors
only; scopes on dimension columns; no rule on labels/blocked_reason;
metric absence = bad; every alert links a runbook; correlation_id
greps the whole story; rollup groups consequence alerts under a
root-cause incident.

### N.1 Metric/alert inventory (§15 + instruction-required set)

```text
- duplicate idempotency-key POST attempts (DUPLICATE_REQUEST answers,
  §7.2)                                → metric; alert on volume
- divergent-payload attempts (collision responses; expected vs
  anomalous split, §7.2)               → metric; anomalous = CRITICAL
- MAYBE_SUBMITTED count + age (maybe_since; tier-1 before cutoff,
  tier-2 re-page, §9.3)                → alert / re-page
- rows approaching provider query-lookback expiry (created/attempt age
  vs TL-5 lookback)                    → alert (act before
                                          NOT_FOUND becomes
                                          unfalsifiable, §9.3)
- reservations held, by state and age (stuck-reservation, §3/§15)
                                       → alert
- apply-platform-verified-outcome usage (§9.3)  → alert EVERY use
- provider-side payments per key vs local EXECUTED count — CONDITIONAL:
  requires an engine-side report/API (MUST_VERIFY_LOCALLY); if
  unavailable, the drift scanner + terminal-evidence tripwire are the
  MVP coverage                         → CRITICAL on divergence
- retry scanner outcomes (per class: retried/exhausted/cutoff-blocked)
                                       → metric; exhaustion spikes alert
- resolver failure reasons (INDETERMINATE rates, query errors)
                                       → metric; alert on sustained
- status-query lag + NOT_FOUND-after-trust-age frequency (§18/TL-15
  production measurement)              → metric + observed-lag watchdog
- stale-message volume (§6.7)          → alert on volume
- stale-marker-write volume (§6.9)     → alert on volume
- unmatched feed events (§8)           → metric; alert on volume
- drift scanner mismatches (I1/I2, L9) → PAGE
- UI/card false-completion prevention: card >1-obligation lookups
  (§12), completion-predicate anomalies (COMPLETED with active
  request — should be impossible; presence = defect)  → alert
- plus the full §15 list wired in OB-03..05 (latch alerts, marker
  alerts, DLT, lag, heartbeats, stuck-state, freeze page, deadlocks,
  inbox growth, breaker, sweep overrun, tie/latched-amendment alerts)
```

### N.2 Runbook seeds (Trigger / Severity / Why / Action / Data / Escalation / Safe stop)

```text
DRIFT MISMATCH (I1/I2/L9)
Trigger: drift scanner pages after locked re-check.
Severity: PAGE (money-math integrity).
Why: counters vs row state disagree — row corruption or a money bug.
Action: freeze posting (Hazelcast toggle, ticketed); do NOT correct
  counters by hand (no sanctioned operation exists at MVP).
Data: scanner output (obligation ids, expected vs actual), §14 CAS
  log lines for the obligation's requests, recent deploys.
Escalate: tech lead + DBA immediately; incident channel.
Safe stop: root cause identified; correction plan through sanctioned
  paths; posting unfrozen only after drift re-scan is clean.
```

```text
PAYMENT_OUTCOME_UNKNOWN ESCALATED (MAYBE age tier-1/tier-2)
Trigger: BLOCKED(ESCALATED) write + CRITICAL alert (maybe_since age).
Severity: CRITICAL (money may be moving; cutoff approaching).
Why: a payment's fate is unknown and automation has not resolved it.
Action: per §9.3 offered actions ONLY: trigger resolve-via-query;
  after trust-age + repost_permitted → ops-triggered downgrade;
  dual-control stale-amount re-POST (only overridable term); request
  TL-10 platform rejection; LAST: apply-platform-verified-outcome
  after verifying the fate in platform records. NEVER manually
  release/cancel (release guard will refuse — that is correct).
Data: request id, key, uetr, maybe_since, last_post_attempt_at,
  divergent_payload_at, cutoff time, resolver's recent answers.
Escalate: tier-2 age → incident + payments duty manager.
Safe stop: outcome applied via evidence or the procedure; scope
  re-derived; reservation confirmed or released.
```

```text
AMOUNT_MISMATCH (all-or-nothing violated)
Trigger: settlement/query amount ≠ request amount → BLOCKED park.
Severity: CRITICAL (defect evidence — ours or the engine's).
Why: contract says partial/fee-deducted settlement is impossible; a
  mismatch means a defect, and confirmed money must not move on it.
Action: verify the event against platform records; open a provider
  ticket; do not adjust local amounts (no operation exists).
Data: event payload, request amount, provider_reference, correlation.
Escalate: tech lead + provider support same day.
Safe stop: corrected event settles the row normally, or the dispute
  resolves platform-side and the row exits via the §9.3 procedure.
```

```text
ENGINE_INCONSISTENCY
Trigger: SUBMITTED row NOT_FOUND after trust-age (§9.2), or anomalous
  same-key divergence (§7.2).
Severity: CRITICAL.
Why: the engine acknowledged something it now can't find, or
  disagrees about payload identity — engine-side integrity question.
Action: keep resolver querying (automatic — row stays in scope); pull
  platform-side records; provider ticket if it persists past one
  ingest-lag window.
Data: key, uetr, submitted_at/last_post_attempt_at, query answers
  timeline, divergence_expected + hashes (log line).
Escalate: provider support; tech lead if >1 row (systemic).
Safe stop: next successful query resolves it (lag-caused false park
  self-heals), or platform records settle it via the §9.3 path.
```

```text
FREEZE EFFECTIVE WITHOUT ACKNOWLEDGED TICKET
Trigger: posting freeze effective (toggle set OR Hazelcast
  unreachable) with no acknowledged freeze ticket (§16.1/§15).
Severity: PAGE (the freeze is silent by design — this is the only
  signal; every payment is pausing).
Why: either an unannounced deliberate freeze or grid failure.
Action: check the toggle's reason/operator/ticket payload; if infra:
  engage the grid owner; if deliberate-but-unticketed: get the
  operator to file the ticket; do NOT unfreeze without the operator.
Data: toggle payload, Hazelcast cluster health, freeze metric history.
Escalate: infra on-call for grid failure; payments lead otherwise.
Safe stop: freeze either acknowledged (ticketed) or lifted; retry
  deadlines were suspended (no BLOCKED flood expected — verify).
```

```text
UNMATCHED FEED EVENTS VOLUME
Trigger: unmatched count over threshold (§8).
Severity: alert.
Why: routine singles are the feed-beats-commit race (sweep recovers);
  volume means a matching defect, a UETR persistence gap, or foreign
  traffic.
Action: sample events: known UETRs? (persistence gap → check U-01
  paths); foreign? (investigate in the platform, which owns the
  record); recovery of real outcomes is automatic via §9.
Data: sampled (event_id, UETR, status) log lines; U-01 test status.
Escalate: tech lead if a persistence gap is suspected.
Safe stop: volume back under threshold with explanation.
```

```text
KAFKA DLT DEPTH > 0
Trigger: any DLT message (either flow).
Severity: PAGE (§16.2 — poison money messages).
Why: a message failed deserialization/semantic validation; per-payment
  ordering for that key is now suspended pending replay.
Action: inspect the poison message; fix cause (schema drift? producer
  bug?); replay preserving original keys (§16.2 tool).
Data: DLT payload + headers, deserializer error, schema versions.
Escalate: upstream/provider team per flow; tech lead for schema drift.
Safe stop: DLT drained via keyed replay; consumer healthy.
```

```text
CONSUMER LAG OVER SLA
Trigger: lag page (either inbound flow, §15/§16.2).
Severity: PAGE over SLA.
Why: the DATABASE ITSELF is behind the world; card users may act on
  stale money state (§12).
Action: confirm the card's data-as-of indicator is showing (§12);
  diagnose consumer health (rebalance storm? poll interval? DB
  contention on the obligation lock?).
Data: lag per partition, poll metrics, DB session waits.
Escalate: infra + tech lead.
Safe stop: lag under SLA; indicator clears.
```

```text
OVERPAY LATCHED (incl. count/age rollup)
Trigger: overpay_blocked set (§13); bulk alert on count+oldest age.
Severity: alert (business hours).
Why: confirmed > required — the scope is frozen for automation
  FOREVER (one-way door); recovery is platform-side.
Action: annotate via ops_annotation (§20-4 display note); initiate
  recall/refund in the payment platform; a later amendment will NOT
  resume payment (AMENDMENT_ON_LATCHED_SCOPE alerts instead).
Data: obligation amounts, settling request, feed event trail.
Escalate: business ops for the refund workflow.
Safe stop: platform-side recovery underway; annotation records it.
```

```text
PROVIDER_REJECTED (and repeat-reject ≥2)
Trigger: marker set alert; count=2 alert (ops-only clearing begins).
Severity: alert.
Why: a requested payment is not happening; from the second reject the
  marker no longer clears via newer messages (anti-loop, §2.1).
Action: read the reject code (CA-1 meaning); coordinate the data fix
  upstream (one ordering-newer auto-attempt exists, §6.8) or accept;
  from count≥2: only the future §19.3 ops clear can re-enable
  auto-successors — until it exists, resolution is a PO/ops decision
  recorded in the ticket.
Data: reject code, creating_ordering vs upstream_ordering, count.
Escalate: business owner of the payment.
Safe stop: successor executed after a corrected message, or the scope
  consciously left rejected.
```

```text
AMENDMENT_TIE_CONFLICT
Trigger: §6.7 tie with differing payload.
Severity: alert (manual application needed).
Why: two genuine amendments share an ordering value — automation
  refuses to pick; a resend carries the same timestamp and would be
  rejected forever, so a human MUST apply the right one.
Action: obtain the correct current values from upstream; apply via
  the supported manual path with the release-guard-safe procedure
  (ops-applied amendment is a message-equivalent write, not a raw
  UPDATE — if no tool exists yet, escalate to the tech lead; §20-1).
Data: both payloads, ordering value, current obligation amounts.
Escalate: upstream team + tech lead.
Safe stop: correct amount applied; §6.8 re-evaluated.
```

```text
SCANNER HEARTBEAT SILENT / SWEEP OVERRUN
Trigger: any scanner silent 3× its interval; resolver overruns
  repeatedly (§15).
Severity: PAGE (silent scanner = silent recovery machinery).
Why: MAYBE recovery, retries, escalation all ride on scanners.
Action: check scheduler health, DB connectivity, breaker states
  (breaker-gated quiet is EXPECTED during engine outages — verify
  against the rollup incident before treating as failure).
Data: last heartbeat, batch metrics, breaker state, lock waits.
Escalate: infra/tech lead.
Safe stop: heartbeat resumed; backlog draining within budget.
```

```text
EVIDENCE FOR TERMINAL REQUEST
Trigger: new event_id, zero-row CAS on a terminal row (§8).
Severity: CRITICAL.
Why: the engine asserts an outcome for a row we closed — possible
  replay divergence (§5.2 signature) or a serious mis-match.
Action: FREEZE posting for the affected scope's payment type if
  volume >1 (ticketed); reconcile the request against platform
  records; if the terminal state is wrong, the correction path is the
  §9.3 procedure (never a raw un-freeze of the row).
Data: event payload, the row's terminal outcome + §14 history, key.
Escalate: tech lead immediately.
Safe stop: explained (true duplicate/foreign) or corrected via
  sanctioned path.
```

### N.3 Reconciliation

```text
Drift scan (OB-01) — every run recomputes I1/I2 (snapshot + locked
re-check) and verifies L9; PAGE on confirmed mismatch.
Terminal-evidence tripwire (OB-02) — the §5.2 replay-divergence
signature, live from day one.
Money-truth divergence policy (§19.2, decided): reality vs state
model disagreement = CRITICAL incident, reconciled in the payment
platform; local counters corrected only by the FUTURE
manual-adjustment operation — never ad hoc.
Retention-chain check (OB-05): inbox_retention > kafka_retention ≥
replay_window, verified on schedule against actual broker config.
Engine-side count comparison: conditional on an engine report/API
(N.1 note) — decide at kickoff with the provider answer.
```

---

# O. Local-only placeholder mapping template

Copy this table to a LOCAL file on the work laptop (an ignored/
untracked location). Fill during Phase P1. It must NEVER leave the
work laptop — it is the one document that contains real names.
Status values: UNMAPPED / CONFIRMED / PARTIAL / MISSING / UNCLEAR /
BLOCKED.

| Placeholder component | Local file/class/table/job found | How I confirmed it | Existing tests found | Existing behavior to preserve | Required change | Requirement sections | Risk level | Owner / reviewer | Status | Notes |
|---|---|---|---|---|---|---|---|---|---|---|
| [Payment Request Creation Component] | | | | | | §6.8, §5.1, §3 | | | UNMAPPED | |
| [Payment Enrichment Component] | | | | | | §7.3, §7.0 | | | UNMAPPED | |
| [Provider POST Client] | | | | | | §5, §7.0, §11, §16.1 | | | UNMAPPED | |
| [Provider Response Parser] | | | | | | §7.2, §5 | | | UNMAPPED | |
| [Request Status Persistence Layer] | | | | | | §2.2, §10, §11, §14 | | | UNMAPPED | |
| [Reservation Repository] | | | | | | §3, §13 | | | UNMAPPED | |
| [Obligation Repository] | | | | | | §2.1, §6.7, §6.9, §11 | | | UNMAPPED | |
| [Retry Resolver Job] | | | | | | §7.4, §16.1, §11 | | | UNMAPPED | |
| [Status Query Resolver] | | | | | | §9.1–9.5 | | | UNMAPPED | |
| [Payment Status Feed Consumer] | | | | | | §8, §16.2 | | | UNMAPPED | |
| [Inbox / Processed Event Repository] | | | | | | §2.3, §8 | | | UNMAPPED | |
| [DB Migration Directory] | | | | | | §16.5, §16.6-4 | | | UNMAPPED | |
| [Stored Procedure / Trigger Area] | | | | | | §10.3 | | | UNMAPPED | |
| [Operator Admin Procedure Area] | | | | | | §9.3, §20 | | | UNMAPPED | |
| [Metrics / Alerting Layer] | | | | | | §14, §15, §16.3 | | | UNMAPPED | |
| [Reconciliation / Drift Scanner] | | | | | | §3, §10.3 L9 | | | UNMAPPED | |
| [Integration Test Suite] | | | | | | §16.6-6 | | | UNMAPPED | |
| [Contract Test Suite] | | | | | | §18-1, §16.5 | | | UNMAPPED | |
| payment_obligation (real table) | | | | | | §2.1 | | | UNMAPPED | |
| payment_request (real table) | | | | | | §2.2 | | | UNMAPPED | |
| processed_inbound_event (real table) | | | | | | §2.3 | | | UNMAPPED | |
| PaymentOrchestrationService (documented service) | | | | | | front matter | | | UNMAPPED | |
| PaymentEnrichmentService (documented service) | | | | | | front matter | | | UNMAPPED | |
| PaymentExecutionService (documented service) | | | | | | front matter | | | UNMAPPED | |
| PaymentNotificationConsumerService (documented service) | | | | | | front matter | | | UNMAPPED | |
| Upstream trade-message consumer | | | | | | §6.0–6.9, §16.2 | | | UNMAPPED | |
| Hazelcast posting-freeze toggle | | | | | | §16.1 | | | UNMAPPED | |
| Cutoff calendar source | | | | | | §18-2, §16.4 | | | UNMAPPED | |
| Legacy compound status enum | | | | | | §10.4, ST-05 | | | UNMAPPED | |

---

# P. Instructions for the local coding agent on the work laptop

You are executing a portable implementation playbook against a
codebase the playbook's author has never seen. The playbook is
authoritative about WHAT and WHY; you resolve WHERE locally. The
baseline specification is `requirment-v4.md`; every task card cites
the sections that govern it.

**Rules (binding):**

```text
1.  FIRST perform discovery only (Phase P1, cards D-01..D-12). Do not
    implement anything during discovery — not even "obvious" fixes.
2.  Map placeholder components to real local files/classes/tables/
    jobs in the Section O template. Keep the mapping LOCAL; never
    send it externally, never paste real names into anything that
    leaves this laptop.
3.  Execute exactly ONE task card at a time, in dependency order
    (Playbook Index). Do not start a card whose prerequisites or
    required mappings are incomplete.
4.  Before each task, read ONLY: the task card, its Minimal Context
    Packet (Section I), the requirement sections it lists, and the
    locally discovered files it names. Do not read the whole repo
    unless the card is a discovery card.
5.  Use the Minimal Context Packet as your working brief. If your
    context is small, the packet alone plus the named §s suffices.
6.  Do NOT change business rules (payment decision logic, enrichment,
    validation, account detection, payment construction). If a task
    appears to require it, stop and report
    BUSINESS_RULE_CHANGE_REQUIRED with the requirement section that
    creates the need.
7.  Do NOT broaden scope. No unrelated cleanup. No style refactoring.
    No drive-by fixes. One card = one concern.
8.  Add or update tests BEFORE relying on new behavior; run the
    card's listed tests plus the surrounding suite.
9.  STOP after each task. Fill the Section R execution report. Wait
    for review per the local team's process before the next card.
10. If a task cannot be mapped to real code, mark it BLOCKED locally
    with what you searched and what was ambiguous. NEVER guess a
    mapping.
11. If a test fails OUTSIDE the task's scope, stop and report; do not
    attempt broad repairs. (D-11's baseline flaky list tells you
    which failures are pre-existing.)
12. If a task needs more context than you can hold, split it locally
    into sub-tasks (Task-ID.1, .2, …), each with its own report; the
    ST-05 card is explicitly designed to be split per rule site.
13. Never invent tables, journals, outboxes, parked-event tables,
    attempt-history tables, or audit-history tables. If an
    implementation seems to need one, report SPEC_CONFLICT.
14. Tasks marked BLOCKED on §18 items stay blocked until the human
    owner records the answer. Do not "unblock" them by assuming.
15. Rejected design alternatives recorded in requirment-v4.md
    (derived committed_amount, attempt-history table, payload freeze,
    auto-unlatch, materiality re-POST, consecutive-answer counter,
    UETR generation/validation) are settled. Do not re-propose or
    implement them.
16. When the spec and this playbook seem to disagree, the spec
    (`requirment-v4.md`) wins — report the discrepancy.
```

**Per-task loop:**

```text
read card + packet + cited §s
  → verify prerequisites + mappings
  → write/adjust tests (red where applicable)
  → implement the card's instructions only
  → run card tests + surrounding suite
  → manual validation step from the card
  → fill Section R report
  → STOP
```

---

# Q. Go-live readiness checklist

Execute at GO-04. Every PASS carries linked evidence (test run,
report, signed document). §18 BLOCKING items are non-waivable; other
FAILs need a named owner and dated plan to proceed as risks.

| # | Item | Source | PASS/FAIL/BLOCKED | Evidence |
|---|------|--------|-------------------|----------|
| Q1 | §18 BLOCKING item 0 residue closed: written snapshot-contract confirmation (upstream ask 5); §6.0 within-snapshot uniqueness intake validation live; PO-9 (absence semantics) and TL-16 (ordering watermark) answered and implemented | §18-0, B-01 | | |
| Q2 | §18 BLOCKING item 1: sandbox collision matrix (a)–(d) EXECUTED and PASSED; re-run procedure scheduled for engine releases | §18-1, CT-02..05 | | |
| Q3 | §18 BLOCKING item 2: cutoff calendar sourced, owned, tz-aware, refresh + fail direction configured | §18-2, B-03 | | |
| Q4 | §18 BLOCKING item 3: apply-platform-verified-outcome procedure EXISTS (OP-01/02) AND DRILLED (OP-03) — or TL-10 ∧ TL-5 alternative affirmed in writing + PO re-confirmation | §18-3, B-04 | | |
| Q5 | Schema at CA-4 target: constraints VALIDATED, triggers live, indexes in place; migration test pass green (incl. dual-run) | S-05..09 | | |
| Q6 | Factored state model implemented: dual-write live, CAS discipline audited, legality suite green | ST-01..03 | | |
| Q7 | Legacy status not used for business rules: ST-05 inventory empty or fully dispositioned; display via derived labels only | ST-04/05 | | |
| Q8 | Idempotency key generation deterministic + persisted write-ahead; K-06 crash/retry/restore set green | K-01..06, T-03/08/09/10 | | |
| Q9 | Identity golden-vector tests green and frozen in the build | K-03, T-02 | | |
| Q10 | Provider idempotency sandbox tests green (same as Q2, listed for the test-evidence pack) + SDK contract checks (CT-07) recorded | CT suite, T-11..14 | | |
| Q11 | Duplicate-prevention tests green (I6, UNIQUE key, engine-dedup routing) | T-17, S-05 | | |
| Q12 | Retry / crash / restore recovery tests green | T-08/09/10, ST-10 | | |
| Q13 | Cutoff calendar configured and validated in the target environment (tz + holiday spot checks) | B-03, T-21 | | |
| Q14 | MAYBE_SUBMITTED recovery lifecycle tests green (resolver, trust-age, downgrade, escalation, parked rows) | T-22/23, RC-05..08 | | |
| Q15 | apply-platform-verified-outcome test suite + drill report on file | T-24, OP-02/03 | | |
| Q16 | Reservation release / confirmation correctness green (I1–I6, redelivery safety, overpay latch) | T-26/27, RG-01..04 | | |
| Q17 | Evidence session flag / release guard validated (code + trigger layers; pool non-leakage) | T-25, S-06, RG-05 | | |
| Q18 | Reconciliation tripwires live (terminal-evidence CRITICAL, count sanity, card >1) | T-30, OB-02 | | |
| Q19 | Drift scanner live, paging, read-skew-safe | T-29, OB-01 | | |
| Q20 | Observability dashboards + alerts live per §15 with runbook links; rollup verified; config ordering validation active | T-32, OB-03..07 | | |
| Q21 | Runbook stubs published (CA-8) incl. the aged-MAYBE runbook | CA-8, OB-06 | | |
| Q22 | Backwards compatibility with existing payment logic: D-11 baseline green at the release candidate; no BUSINESS_RULE_CHANGE_REQUIRED unresolved | D-11, M.8 | | |
| Q23 | Migration/rollout/rollback plan approved; rollback rehearsed; point of no return documented | GO-01/05, Section M | | |
| Q24 | Shadow validation soak report clean | GO-02 | | |
| Q25 | Tech-lead / provider / PO question register (Section K) current: all BLOCKING answered; HIGH answered or risk-owned; §16.6 config values have owners | Section K, OB-07 | | |
| Q26 | UI/card correctness tests green (no false completion; §12 defensive rule) | T-31 | | |
| Q27 | Kafka hardening compliant per §16.2 checklist in all target environments | IN-09 | | |
| Q28 | ALL §18 BLOCKING items resolved — final aggregate check before go-live | §18, Q1–Q4 | | |

---

# R. Playbook quality self-check

### R.1 Self-check against the authoring rules

```text
[x] No design review or redesign performed: all rules trace to
    requirment-v4.md sections; rejected alternatives (derived
    counter, attempt-history table, payload freeze, auto-unlatch,
    materiality re-POST, consecutive-answer counter, parked-event
    table, UETR generation) are listed as settled and guarded against
    re-introduction (Sections B, P-15, task "Do not change" fields).
[x] No new findings created: open items are exactly §18's, carried
    into Section K; UNCLEAR/MUST_VERIFY_LOCALLY markers ask for local
    or external facts, they do not challenge the design.
[x] §1.1 Basic Agreements not re-opened: BA-1/2/3 appear only as
    settled constraints (C4) with explicit do-not-build notes.
[x] No source-code names invented: all local components are Section G
    placeholders; the only concrete names used are the spec's own
    (three tables, four documented services, documented columns/
    states/artifacts).
[x] No invented persistent tables/journals/outboxes/parked-event/
    attempt-history/manual-action/audit-history tables: Section B
    non-goal 5 + P-13 SPEC_CONFLICT rule; IN-06/S-04 explicitly
    guard the parked-event trap.
[x] No future/post-MVP/PO-discussion work promoted to MVP: §5.2 DR
    runbook (C18), key-only anchoring (C25), §19.1/19.2/19.3
    (C69-71), ops console (C72) are classified FUTURE/QUESTION;
    the ONLY §20 implementation work is the §18-3-required procedure.
[x] Every task has requirement-section traceability: each card's
    "Requirement sections / concepts to read" field; classification
    table maps C-items to task IDs.
[x] Every task has local discovery instructions: per-card "How to
    locate" + Section F workflow + Section O mapping gates.
[x] Every task has validation and a stop condition: per-card "Tests
    to add", "Manual validation", "Stop condition" fields; packets
    repeat tests + stop.
[x] Every task is small enough for a weak/small-context executor or
    explicitly says to split (ST-05 is a per-site template; S-09 lane
    setup and P-12 give the local split rule).
[x] Every §18 BLOCKING item appears in the dependency graph (Section
    D box P2 + ordering #1/#6/#7) and the go-live checklist (Q1–Q4,
    Q28).
[x] Every §16.6 companion artifact is an actionable deliverable:
    CA-1..9 each have a task card (Section H Phase 2), a Section L
    plan entry (deliverable, owner type, validation, dependents,
    start-before rule via prerequisites), and packet.
[x] Every manual operation beyond the MVP-required procedure is
    classified future/PO discussion (C72; RG-05's supersede/close is
    delivered only as the §20-sanctioned guarded interim procedure
    that §3 REQUIRES, subject to the release guard).
```

Companion-artifact "can implementation start before the artifact
exists?" summary (Section L complement):

```text
CA-1: RC-01's classifier SHAPE can start; the code table must exist
      before RC-01 completes. CA-2: IN-05/06 skeleton yes; IN-07's
      ranks need it. CA-3: RC-05 yes; RC-06 needs it. CA-4: NO —
      S-02+ implement it (drafting may overlap D-02). CA-5/CA-6: NO
      for K-02/K-05 (they IMPLEMENT the specs); K-01 may proceed.
      CA-7: tests proceed from Section J; the catalog consolidates.
      CA-8: OB tasks proceed; OB-06 needs the stubs. CA-9: NO —
      OP-01 implements it.
```

### R.2 Local-only task execution report template

Fill after EVERY task card; keep locally with the mapping document.

```text
Task ID:
Local mappings used:
Files changed:
Tests added/updated:
Validation commands run:
Result:
Failed tests:
Unexpected findings:
Business logic changed? yes/no
Requirement sections satisfied:
Remaining blockers:
Safe to proceed to next task? yes/no
```

---

*End of playbook. Baseline: `requirment-v4.md` (v4, 2026-07-05).
Transfer this single file to the work laptop; everything local stays
local.*
