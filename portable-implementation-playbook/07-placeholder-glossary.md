> **Purpose:** Placeholder component glossary: meaning, probable local shape, responsibilities, do-not-change zones, change mode (original Section G).
> **When to use this file:** Alongside EVERY task card and packet - placeholders are only defined here.
> **Depends on:** requirment-v4.md sections cited per placeholder.
> **Used by:** All task cards and minimal context packets; 15-local-placeholder-mapping-template.md rows.
> **Safe to transfer:** yes
> **Contains local code names:** no

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
Responsibilities: §11 claim protocol (lock-free candidate selection,
              obligation-first per-item claim CAS), DB-time due
              comparisons, per-item transactions; per-class retry
              policy (bounds = attempts + cutoff, §7.4 2026-07-11);
              repost_permitted before POST-bound work (§7.0);
              exhaustion/cutoff → BLOCKED; breaker/freeze gating
              (zero attempts while gated — §16.1).
Do not change: scheduling infrastructure conventions.
Tests:        seeded-row scanner tests; zero-attempt outage-window tests.
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
              defined/migrated. NAME IS DELIBERATE (round 7 note):
              placeholder labels are stable local-mapping keys;
              "Procedure" here names real DB object areas — it is
              NOT the retired §9.3 "procedure" terminology (ops
              mutations are Java application endpoints).
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
              outcome operation (§9.3, CA-9).
Identify:     F.20.
Responsibilities: OP-01's operation: dual control, ticket reference,
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

