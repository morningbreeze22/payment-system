> **Purpose:** Minimal context packets K-01..K-06 — paste-alone briefs for a small-context local agent (original Section I, phase P4).
> **When to use this file:** Paired with the matching task-card file 08-task-cards/04-identity-and-idempotency.md — one packet per task, used as the working brief.
> **Depends on:** 09-minimal-context-packets/README.md; the matching task card; the requirement sections each packet cites; 07-placeholder-glossary.md.
> **Used by:** The local coding agent executing phase P4.
> **Safe to transfer:** yes
> **Contains local code names:** no

# Minimal Context Packets — Phase P4

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
Objective: claim transaction persists identity (first claim), COMMITS, then the HTTP call; commit-unknown → abandon, lease expiry owns it. §14.1 rider in the SAME transaction: post_attempt_seq++ (monotonic, never reset) + ATTEMPT_STARTED insert (FULL content every attempt; switch-gated). Canonical failure rule: statement-local failures caught around the single statement (no inner @Transactional), gap alerted AFTER host commit, claim proceeds; FATAL failures = ordinary infra failures; guarantee = no incorrect payment outcome. Autonomous transactions forbidden.
Tests: ordering fault-injection (commit vs stub-received); ambiguous-commit → no call; T-38 (rollback leaves no STARTED; downgrade reset → no key collision; outage → posting continues + gap alert). Stop: merged.
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

