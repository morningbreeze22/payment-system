> **Purpose:** Requirement extraction and classification C1-C72 with section traceability, dependencies, go-live flags (original Section C).
> **When to use this file:** Planning and audit: find which tasks implement a requirement section and its classification (MVP/GATE/ARTIFACT/RUNBOOK/QUESTION/FUTURE/DISCOVERY).
> **Depends on:** requirment-v4.md; 01-playbook-index.md.
> **Used by:** Phase planning; GO-04 gate audit; 17-go-live-checklist.md.
> **Safe to transfer:** yes
> **Contains local code names:** no

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
| C11 | §3, §20 | Supersede/close operation (release-guarded); at MVP exercised via the RG-05 authorized application endpoint (2026-07-11 Java boundary) under §10.3 backstops, not a console | MVP guard + RUNBOOK | RG-05, S-06, CA-8 | S-06 | no | yes | no |
| C12 | §4.1 | Step-status predicate — BOTH branches: COMPLETED + the round-11 CANCELLED zero-required terminal (incl. vacuous-completion guards; required = 0 writable only by the §6.1 absence path) | MVP | RG-08 | RG-01..03 | no | yes | no |
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
| C33 | §7.4 | Retry policy per error class (externalized config); exhaustion → BLOCKED; downgrade policy class (attempt reset); bound = max attempts (round 10 — no cutoff) | MVP | RC-04 | — | partially | yes | no |
| C34 | §8 | Feed consumption transaction order (inbox → resolve → evidence CAS → commit → ack); unmatched = log+count+ack; amount equality; marker totality; anomaly disambiguation | MVP | IN-05..08 | S-04, ST-02 | yes | yes | no |
| C35 | §8 | provider_reference fallback — fail closed (single active match + amount + recency) until uniqueness confirmed | MVP + QUESTION (TL-12) | IN-06 | U-02 | no | yes | yes |
| C36 | §9.1 | Status-query outcome mapping (EXECUTED/REJECTED/NOT_FOUND/INDETERMINATE/ACCEPTED) | MVP + ARTIFACT (CA-3) | RC-06 | CA-3 | no | yes | yes |
| C37 | §9.2 | NOT_FOUND trust-age rule; §9.2 auto-downgrade (the ONE backward stage move) gated by repost_permitted; SUBMITTED branch → ENGINE_INCONSISTENCY park | MVP | RC-07 | RC-03, TL-5 config | yes | yes | TL-5 (ingest lag) |
| C38 | §9.3 | MAYBE escalation (maybe_since clock, once per episode, tiered); ops action set for BLOCKED·MAYBE rows; apply-platform-verified-outcome as MVP terminal exit | MVP + GATE (§18-3) | RC-08, OP-01..03 | B-04 | YES | yes | no |
| C39 | §9.4 | Release-rights invariant (single sanctioned exception: the §9.3 operation) | MVP | RG-05, S-06 | — | yes | yes | no |
| C40 | §9.5 | Resolver scope keyed on submission_state+outcome ONLY; bounded prioritized sweep; per-row backoff; never-overlap; SUBMITTED damping | MVP | RC-05 | TL-13 (rate limit) | yes | yes | yes |
| C41 | §10.1–10.5 | Factored state model: global rules, per-dimension transitions, legality matrix L1–L9, display labels (no rule keys on label or blocked_reason) | MVP | ST-01..08, S-05..06 | S-xx | yes | yes | no |
| C42 | §11 | Two-tier concurrency (obligation lock + request CAS); lock ordering; claims as leases; posting-claim persistence; ambiguous claim-commit; lease-expiry recovery; graceful shutdown | MVP | ST-02, ST-09..11 | S-xx | yes | yes | no |
| C43 | §12 | Card read model: business_id-only lookup returning ALL of the trade's obligations (one entry per payment; multiple results = NORMAL, never a health signal), NOT_STARTED = absence, unavailable ≠ stale, freshness/lag indicator | MVP + QUESTION (TL-2 read contract) | RG-08..09, OB-04 | B-01 | no | yes | yes |
| C44 | §13 | Exception categories/severities; overpay latch = one-way door, ignore-forward, alert on set | MVP | RG-04, RG-09 | — | no | yes | no |
| C45 | §14 | No TRANSITION-HISTORY journal replaces the structured CAS log line (key+seq+dimensions before→after; posting-claim line carries sent hash; 90-day retention floor); the switch-gated §14.1 attempt-content journal is a SEPARATE audit sink — never runtime state, never a log replacement (review 2b697fb M2) | MVP | ST-08, AUD-01 | — | no | yes | no |
| C46 | §15 | Monitoring list (drift page, MAYBE ages, BLOCKED queue, marker alerts, freeze-effective page, watchdogs); clock discipline (episode anchors); alert rollup; the §6.6 accepted-window candidate diagnostic (OPTIONAL, on-demand — OB-01 ships the query, never a standing scan; b1d91dc M1) | MVP | OB-01 (diagnostic), OB-03..05 | P6–P12 | yes | yes | no |
| C47 | §16.1 | Resiliency: timeouts, breakers (business rejects = success), scanner gating, poison-row cap, bulkheads, Hazelcast posting freeze fail-safe (absent/unreachable/timeout = FROZEN; only FROZEN cached), freeze/breaker windows make zero attempts (structural — no wall-clock deadline exists, §7.4 2026-07-11) | MVP | RC-09, RC-10 | — | yes | yes | no |
| C48 | §16.2 | Kafka rules: manual ack after commit, earliest, ErrorHandlingDeserializer, DLT for poison only, no retry topics for money events, partition keying, retention-chain check | MVP + RUNBOOK | IN-09, OB-05 | — | yes | yes | no |
| C49 | §16.3 | Security: read-surface auth, account masking in encoder, instruction content persisted ONLY in the switch-gated §14.1 journal (otherwise hash only — the controlled exception), secrets vaulted, topic ACLs | MVP | cross-cutting (IN, U, OB, AUD) | — | no | yes | no |
| C50 | §16.4 | Amount/time hygiene: currency-scale validation, BigDecimal.compareTo, no tolerance, UTC + DB time (cutoff-calendar clause retired round 10) | MVP | IN-01, RC-04 | — | no | yes | no |
| C51 | §16.5 | Expand/contract migrations (Flyway/Liquibase); volume NFR ~3k trades/day; contract tests for 3 external contracts; defensive enum reads (UNKNOWN sentinel); 4 dimension enums CLOSED | MVP | S-01..10, GO-01 | — | yes | yes | no |
| C52 | §16.6 | Configuration inventory + config-load ordering validation (trust_age + cadence < escalation < tier-2; cutoff margin retired round 10) | MVP | OB-07 | TL-5, TL-13 | no | yes | values needed |
| C53 | §16.6-1 | Engine error-code → classification table (incl. replay-original-response class) | ARTIFACT | CA-1 | provider | YES (feeds RC-01) | no | yes |
| C54 | §16.6-2 | Engine status vocabulary + precedence/evidence mapping + feed event schema | ARTIFACT | CA-2 | provider | YES (feeds IN-07) | no | yes |
| C55 | §16.6-3 | Status-query response → §9.1 outcome mapping | ARTIFACT | CA-3 | provider | YES (feeds RC-06) | no | yes |
| C56 | §16.6-4 | Full Flyway DDL migration set (I6 expression, CHECKs, triggers, active-row-bounded index list) | ARTIFACT | CA-4 | — (scope key settled, §1) | YES | yes | no |
| C57 | §16.6-5 | Identity-derivation spec + golden vectors | ARTIFACT | CA-5 | — (scope key settled, §1) | YES | no | no |
| C58 | §16.6-5 | Canonical instruction serialization + last_sent_hash definition | ARTIFACT | CA-6 | CA-5 | YES | yes | no |
| C59 | §16.6-6 | Test catalog aligned to the spec | ARTIFACT | CA-7 | — | YES | yes | no |
| C60 | §16.6-7 | Runbook stubs (one per §15 alert; aged-MAYBE runbook) | ARTIFACT + RUNBOOK | CA-8 | OB-xx | YES | no | no |
| C61 | §16.6-8 | apply-platform-verified-outcome OPERATION spec (authorized application endpoint — 2026-07-11 boundary) + drill script | ARTIFACT + GATE (§18-3) | CA-9, OP-01..03 | B-04 | YES | yes | no |
| C62 | §18-0 | BLOCKING residue of the snapshot contract: WRITTEN filing of asks 5 + 8 (confirmed verbally 2026-07-11) + §6.0 intake validation — gates IN-02 (PO-9 ANSWERED: absence = zero; TL-16 answered round 5) | GATE | B-01 | upstream/UI teams + PO | YES | no | YES |
| C63 | §18-1 | BLOCKING: engine idempotency-collision contract proven by sandbox test (a–d), re-run on engine releases | GATE | B-02, CT-01..05 | sandbox access | YES | no | YES |
| C64 | §18-2 | CLOSED 2026-07-11: the engine owns its cutoff calendar — no local calendar work; B-03 records the fact + the CA-1 late-submission ask | CLOSED | B-03 | — | no | no | no |
| C65 | §18-3 | BLOCKING: MVP MAYBE-row terminal exit (operation EXISTS + DRILLED, or TL-10 ∧ TL-5 alternative) | GATE | B-04, OP-01..03 | CA-9 | YES | yes | possibly |
| C66 | §18 PO 1–8 | PO items: ask-then-retry approval, query cadence, escalation age, cutoff-passed-while-MAYBE (closed round 10), cancelled-trade display, deferral latency, retry-after-reject concept, fresh-assembly consequence | QUESTION | Section K | — | no | no | YES |
| C67 | §18 TL 1–15 | Tech-lead items: event_id stability, card read contract, RPO/RTO, collision contract, ingest lag + lookback, re-execute-after-reject, key-only anchoring, confirmation age, artifact owners, TL-10 platform reject, SDK contract, provider_reference, rate limits, archival, downgrade telemetry | QUESTION | Section K | — | TL-4/5 feed gates | no | YES |
| C68 | §18 upstream 1–4 | Upstream asks: strict ordering, business_id as Kafka key, schema formalization, emission contract | QUESTION | Section K | — | no | no | YES |
| C69 | §19.1 | Outbound completion signal | FUTURE | none | — | no | no | no |
| C70 | §19.2 | Returned funds / reconciliation visibility / manual-adjustment op | FUTURE | none | — | no | no | no |
| C71 | §19.3 | Ops retry-after-provider-reject (4-eyes, marker clear) | FUTURE (pending PO-7) | none | PO-7 | no | no | yes |
| C72 | §20 | Ops console / manual operations beyond the §20 MVP operation set | QUESTION / FUTURE | none (only OP-xx at MVP) | PO | no | no | yes |


**Note on §17:** "Core Requirements Summary" restates §1–§16 in
condensed form and contains no independent normative items — it is
deliberately not classified. If §17 ever appears to disagree with a
detailed section, the detailed section wins.
