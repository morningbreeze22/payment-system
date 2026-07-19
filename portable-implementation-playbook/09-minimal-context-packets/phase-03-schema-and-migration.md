> **Purpose:** Minimal context packets S-01..S-10 + AUD-01 (§14.1 journal schema) — paste-alone briefs for a small-context local agent (S-11 RETIRED round 10) (original Section I, phase P3).
> **When to use this file:** Paired with the matching task-card file 08-task-cards/03-schema-and-migration.md — one packet per task, used as the working brief.
> **Depends on:** 09-minimal-context-packets/README.md; the matching task card; the requirement sections each packet cites; 07-placeholder-glossary.md.
> **Used by:** The local coding agent executing phase P3.
> **Safe to transfer:** yes
> **Contains local code names:** no

# Minimal Context Packets — Phase P3

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
Objective: add §2.1 columns (amounts, markers+counters+first_at, ordering fields, read-model fields, reopened_at, next_request_seq), scope-key UNIQUE, amounts>=0 CHECK, business_id index; entity mapping additive. next_request_seq MUST be INITIALIZED on every existing row + defaulted on new rows (CA-5 initial value; Oracle NULL+1 IS NULL — uninitialized wedges K-01; 4dbdf2b M1). PREREQ: CA-4 + CA-5 PUBLISHED — if either is absent STOP, never improvise the value (6cb3005 M1).
Tests: apply on clean+prod-shaped schema; entity round-trip; ZERO NULL next_request_seq after apply; overflow bound fails closed. Stop: merged, D-11 baseline green. Duplicate-scope data → STOP and report.
```

```text
[S-03] Request columns
Read: §2.2 (whole) §16.5. Invariant: dimension columns nullable until S-08 backfill; legacy status column untouched.
Placeholders: [DB Migration Directory] [Request Status Persistence Layer]. Mappings: both.
Objective: add the four dimensions + blocked_reason + request_seq (§2.2 immutable per-request sequence — write-once at creation, NULL on legacy rows, 1d8a650 M1) + identity (idempotency_key/end_to_end_id)/uetr/provider_reference + version/claim/retry/next_query_at + created_at/state_changed_at/creating_ordering/required_total_at_creation (§2.2 set-once display stamp — TYPE identical to the amount domain in type/precision/scale, JPY scale-0 + 3-decimal in scope; one stamp per row, NOT per POST attempt; write = RG-06 creation INSERT only; UPDATE forbidden; pre-F0 rows NULL) + last_sent_hash/divergence_expected/divergent_payload_at + maybe_since/escalated_at/submitted_at/last_post_attempt_at. (post_attempt_seq: added by AUD-01, not here.)
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
[S-10] trade_snapshot_state (admission row — round 5)
Read: §2.4 (field list given) §6.1 (ADMISSION consumer) §6.7 (pluggable comparator) §7.0 (read path). Invariant: ONE row per trade, overwritten (never an append log); the §6.1 admission transaction is the ONLY writer; digest = the SAME canonical algorithm as the §9.3 approval digest (one shared implementation).
Placeholders: [DB Migration Directory] [Obligation Repository]. Mappings: directory.
Objective: business_id PK; last_accepted_ordering (comparator-agnostic representation); last_xml_storage_id (+version); last_payload_digest; updated_at (DB time). Repository: insert-if-absent (PK race → retry) + SELECT FOR UPDATE by business_id only.
Tests: duplicate-insert race clean; FOR UPDATE blocks same-trade, not other trades. Stop: merged.
```

```text
[S-05] CHECKs, UNIQUEs, I6
Read: §10.3 (matrix) §2.2 constraints §2.1 (ui_step_status stored set) CA-4. Invariant: DB is the backstop; L9 is NOT a CHECK (drift-scanner verified); the ui_step_status CHECK (IN_PROGRESS/COMPLETED/CANCELLED) lands HERE, not in S-02 (round 13).
Placeholders: [DB Migration Directory]. Mappings: real-Oracle test lane (STOP if H2-only).
Objective: enum CHECKs; L2–L8 + L1-shape CHECKs; UNIQUE(idempotency_key); NULL-ignoring UNIQUE(uetr); NULL-ignoring fn-based UNIQUE (payment_obligation_id, request_seq) — plain composite would reject NULL-seq legacy rows (1d8a650 M1); I6 = unique fn index CASE WHEN outcome IS NULL THEN payment_obligation_id END; stamp tripwire CHECK (required_total_at_creation IS NULL OR >= amount — §2.2; a corruption tripwire only — set-once is proven by RG-06's SQL-inventory assertion, not by this CHECK). NOVALIDATE→VALIDATE per plan.
Tests: one violation test per constraint; I6 second-active rejected; stamp < amount refused; request_seq index ISOLATION set (7cc9f49 M1 — neutralize EVERY competitor mechanically: distinct idempotency keys AND same-obligation rows TERMINAL-NEGATIVE so I6's CASE is NULL — active rows would hit I6 first and fake a pass; distinct request ids, NULL uetr, legal terminal shape): same oblig + same seq (two terminal rows) → ORA-00001 naming the FROZEN index exactly; same oblig + NULL-seq terminal legacy rows → both insert; cross-obligation same seq → both insert; expression byte-matches CA-4; negative-control expression mutation makes a case fail. Stop: validated + green.
```

```text
[S-06] Freeze + release-guard triggers
Read: §10.3 (backstops) §10.1 §9.3 (flag setters). Invariant: raw SQL on MAYBE/SUBMITTED rows fails loudly; flag setters are exactly the authoritative-negative path and the §9.3 operation.
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
Objective: reviewed legacy→tuple map; idempotent backfill; anchors defensibly set; terminal rows L1-normalized; required_total_at_creation stays NULL on pre-migration AND dual-run-old-writer rows (back-compute FORBIDDEN — §2.2; capture boundary = F0 activation, GO-03 verifies the first post-F0 stamp); request_seq stays NULL on legacy rows (never fabricated — 1d8a650 M1); VERIFY zero obligations with NULL next_request_seq (S-02 init ran — 4dbdf2b M1); run in a quiet window; THEN re-derive ui_step_status + exceptions for EVERY obligation via the shared §4 derivation (round 14 — never from the legacy label where money predicates are evaluable; batches + per-row lock; greenfield: still run — the zero-NULL evidence is required).
Tests: idempotency; per-value spot checks; constraint dry-validate; read-model pass per-branch cases + ZERO NULL ui_step_status after the pass. Stop: validated; anomaly list dispositioned.
```

```text
[S-09] Migration test pass
Read: §16.5; Section M.1a decision record. Invariant: the OLD app version must run against the NEW schema; the CANCELLED-read proof is CONDITIONAL on M.1a (round 15) — not-read → N/A with proof; defensive reader → test the deployed version; non-defensive → test the COMPATIBILITY release + prove the incompatible original is fenced; name the EXACT build tested.
Placeholders: [DB Migration Directory] [Integration Test Suite]. Mappings: Oracle lane (set it up first if missing).
Objective: prove: clean-schema apply; prod-shaped apply + backfill; old-version boot+smoke on new schema (old writers create rows with NULL required_total_at_creation AND NULL request_seq — EXPECTED during dual-run, the capture boundary is F0 not the migration; §2.2/0e09f09 M1/4dbdf2b M1); constraint suite in CI; evidence: ZERO obligations with NULL next_request_seq; request_seq column + NULL-ignoring unique present.
Tests: the four proofs; old-writer row carries NULL stamp + NULL request_seq and violates nothing. Stop: green; report filed.
```


```text
[AUD-01] Deploy the §14.1 attempt-journal schema
Read: §14.1 (all) §2.2 (post_attempt_seq) §16.3; file 12 CA-10; file 24 M9. Invariant: ops/audit schema OUTSIDE the §2 model; INSERT-only (no UPDATE/DELETE grants to application or reporting roles; owner/DBA access change-controlled and audited — role-accurate, review 928341a M2); SELECT = restricted audit role only, reads DB-audited; never read at runtime; own tablespace; not part of the S-01..S-09 chain.
Placeholders: [DB Migration Directory]. Mappings: audit schema/tablespace/role names; TDE availability.
Objective: RESOLVE CA-10's DDL TEMPLATE (zero angle-bracket tokens; substitution manifest fact→value→source; preflight rejects leftovers; audit-policy block = DBA-executed with AUDIT_ADMIN — review 4d5cb83 M1/M2) then run the migration (Flyway-versioned): typed columns; SCALAR shape CHECK + paj_content_bi BEFORE INSERT trigger for CLOB presence (Oracle forbids LOB CHECKs — never convert the trigger to a CHECK); BOTH global unique structures (paj_pk + paj_pair_uq) with DROP PARTITION ... UPDATE GLOBAL INDEXES; monthly interval partitions on occurred_at; SECUREFILE LOB + named tablespaces; local index on idempotency_key; grants + unified audit on all access; §14.1 enablement switch (writes DEFAULT OFF in prod; transitions only under posting freeze); retention = partition drop per compliance answer; ALSO add post_attempt_seq NUMBER DEFAULT 0 to payment_request (§2.2 - the one §2 column here, coordinated with CA-4).
Tests (NEVER weaker than the card — review 2b697fb M3): PREFLIGHT zero angle-bracket tokens + substitution manifest validates (fact→value→source); T-38 schema slice (INSERT-only for app/reporting roles; unique pair; shape CHECK + content trigger reject BOTH directions; partition drop + BOTH global indexes usable; app role cannot SELECT); T-38 G full set (audit policy ENABLED in the intended PDB/container with evidence; audit-role reads + app INSERTs + denied-access attempts + owner/DBA maintenance incl. partition-drop ALTER all in the unified audit trail). Stop: preflight + manifest recorded; T-38 slice + full G evidence FILED; grants verified role-accurately; merged; K-04/RC-02/ST-10 riders unblocked.
```
