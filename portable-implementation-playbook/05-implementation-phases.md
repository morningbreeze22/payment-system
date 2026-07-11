> **Purpose:** Phase-by-phase plan P1-P14: goal, sequencing rationale, tests, edge cases, common mistakes, completion criteria, go-live flags (original Section E).
> **When to use this file:** At phase entry (goal + prerequisites) and phase exit (completion criteria).
> **Depends on:** 04-dependency-graph.md; requirment-v4.md sections named per phase.
> **Used by:** 08-task-cards/ phase files; phase handoff reviews.
> **Safe to transfer:** yes
> **Contains local code names:** no

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
                 the §11 claim protocol: lock-free selection,
                 obligation-first claim CAS + lease).
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
                 with per-class policy, exhaustion, cutoff pre-checks
                 (bounds = attempts + cutoff, §7.4 2026-07-11 — no
                 wall-clock deadline; gated scanners make zero
                 attempts, so nothing needs suspending); resolver sweep
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
                 zero attempts + zero BLOCKED conversions across a
                 simulated freeze/breaker-OPEN window (§16.1).
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

### Phase P11 — MVP apply-platform-verified-outcome audited operation (+ interim ops surface)

```text
Goal:            Implement CA-9's spec: the audited verified-outcome
                 OPERATION — an authorized, enterprise-authenticated
                 application endpoint calling the shared transition
                 service (execution boundary decided 2026-07-11;
                 §10.3 triggers stay as the DB backstop); inputs =
                 the §9.3 approval_id ONLY (round 4 — a prior
                 two-step approval bound the action; identities
                 derived from the record; APPROVED→CONSUMED CAS
                 atomic with the transition);
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
                 (runbook) instead of in the operation; omitting the
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

