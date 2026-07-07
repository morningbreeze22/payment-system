> **Purpose:** Local source-code discovery workflow for 24 design concepts: what to find, search patterns, confirmation, IMPLEMENTED/PARTIAL/MISSING/UNCLEAR evidence rules (original Section F).
> **When to use this file:** During Phase P1 (D-xx cards) and whenever a placeholder mapping is missing or UNCLEAR.
> **Depends on:** 07-placeholder-glossary.md; 15-local-placeholder-mapping-template.md.
> **Used by:** 08-task-cards/phase-01-discovery.md; any card whose mapping is incomplete.
> **Safe to transfer:** yes
> **Contains local code names:** no

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

