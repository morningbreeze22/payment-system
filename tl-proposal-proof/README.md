# TL-Proposal Proof — a three-model comparison

> A minimal local Spring Boot app (H2 in-memory, no Kafka, no external
> services) that builds THREE designs side by side and drives them through
> the same deterministic failure windows the requirement is built around.
>
> **What this demo claims — precisely:** fold-then-act without guards fails
> (proven). The proposal becomes safe exactly when it re-adds the guard set
> (proven). What remains between the surviving models is a
> **materialization trade-off**, to be argued on engineering grounds — not
> a safety argument.

## Run it

Only a JDK (17+) is required:

```
cd tl-proposal-proof
.\mvnw.cmd test        (Windows)
./mvnw test            (mac/linux)
```

Or open the folder in IntelliJ and run the four test classes.

## The three models

| | Model 1 — `proof.naive` | Model 2 — `proof.minimal` | Model 3 — `proof.guarded` |
|---|---|---|---|
| What it is | The proposal **as literally stated**: insert-only table, fold-then-act, "affinity makes locks unnecessary" | The **strongest reasonable version** of the proposal (added per external review) | The reviewed design, reduced to essentials |
| Storage | 1 unconstrained event table | 1 append-only event table **with constraints** | obligation row + mutable request rows |
| Serialization | thread affinity (convention) | **per-scope version fence**: `UNIQUE(scope, version)` — optimistic concurrency; a stale plan loses the insert and must refold | `SELECT FOR UPDATE` on the obligation row — pessimistic; decide+claim are one transaction |
| Identity | `MAX(seq)+1` from history | **the fenced version slot** (`scope#version`), backstopped by a generated-column `UNIQUE(idem key)` | the locked `next_request_seq` counter, backstopped by `UNIQUE(idem_key)` |
| Money state | fold (non-idempotent) | fold (**idempotent** — distinct executed keys) | stored `confirmed_amount`, CAS-maintained |
| Crash recovery | blind re-POST | **write-ahead + ask-before-retry** | write-ahead + ask-before-retry |
| Stale snapshots | unguarded | strictly-newer ordering guard | strictly-newer ordering guard |
| One-active-request | nothing | *(implicit via fence + inflight fold check)* | **I6**: generated-column unique — enforced against any writer |
| Test result | **fails all 4 scenarios** (by assertion) | **survives all 5** | **survives all 5** |

## The scenarios (identical across models)

1. **Zombie redelivery** — the old partition owner stalls, the partition is
   reassigned, at-least-once redelivers, the zombie later finishes a stale
   plan. Naive: corrupt history, double-counted fold, next amendment
   **silently unpaid** (U-1 class). Minimal: the stale insert **loses the
   fence** and refolds into a no-op. Guarded: the stale plan cannot exist
   outside the lock.
2. **The unrouted writer** — a human ops reject over HTTP (never on any
   hash ring). Naive: **money moves after the human said stop**. Minimal:
   the fence serializes every writer, routed or not. Guarded: the row lock
   does; plus the release guard refuses to terminal-reject an uncertain
   outcome at all.
3. **Instance overlap / identity** — two folds race during a rolling
   deploy. Naive: `MAX+1` reuses seq — one idempotency key, two amounts,
   undetectable under failed contract evidence. Minimal/guarded: identity
   allocation is fenced/locked and write-once; the schema refuses reuse
   from any writer.
4. **Crash between wire and record + dedup retention edge** (§18-1(c)).
   Naive: blind re-POST **double-pays** (200 moved for 100 required).
   Minimal/guarded: write-ahead evidence + **ask** (§9.1 query) — one
   execution, belief == truth, identity never reused.
5. **Stale snapshot regression** (minimal/guarded only) — a delayed old
   amount cannot overwrite a newer one (§6.7 strictly-newer).

**Provider modes** (per external review): `CONTRACT_COMPLIANT` — the world
the requirement assumes and CT-02..05 verify (divergent payload →
distinguishable reject; query honors a lookback window; `LOOKBACK_EXPIRED`
is inconclusive and leaves the row in the MAYBE/ops path).
`ADVERSARIAL` — the world the CT gates exist to rule out (silent payload
collapse). The naive suite runs adversarial (and is unsafe in both);
the surviving suites run compliant. Dedup retention is finite in BOTH
modes — that is the CT-04 fact itself.

**Assertion technique:** every test compares the model's BELIEVED state
against the fake provider's INTERNAL TRUTH (executions, money actually
moved). The surviving models end every scenario with belief == truth.

## The fourth suite — `EventModelLimitsTest` (the other half of the story)

`MinimalSingleWriterSurvivesTest` shows what the event-table model does
WELL. This suite reproduces, runnably, the five issues the v1 event
draft's decision digest identified — what remains when the code, not
the schema, is the last line of defense. **Every test asserts the
damage HAPPENS: green means the limits are real.** (The v1 analysis
docs are superseded and removed — git history has them; the v2
refactor, `event-model-design/event-model-v2.md`, added mechanisms for
exactly these issues, and this suite remains as the runnable record of
WHY each mechanism exists.)

| # | Issue (04) | What the test proves |
|---|---|---|
| 1 | No declarative temporal backstop | A CURRENT writer (fence won fairly — a stale one collides and refolds) with one wrong legality decision commits a second open request: 200 moved for a 100 requirement. The SAME decision against model 3 dies on the I6 emulation — zero rows, before any wire call |
| 2 | No local independent witness | A classification bug in THE canonical fold (verified-NOT-executed booked as paid): UI, drift scanner, and totals job all agree — green everywhere — while the provider moved NOTHING. Only an independent second implementation or the provider's ledger dissents |
| 3 | Fold fixes are retroactive | Books closed at 200 under fold v6; fold v7 deploys; the SAME rows (byte-compared) now say 100 — and no row records that the meaning changed |
| 4 | Restore erases identity memory | Point-in-time restore erases the write-ahead row; the replay pays again with a FRESH key: belief 150==required, truth 250 moved. Also shows an identity epoch would NOT have helped — the failure is amnesia, not collision |
| 5 | History cannot forget | Deleting one event (compliance redaction) flips paid 100→0; the next ROUTINE scan double-pays. In this model an erasure is a money operation |

Honesty notes: where tests write event rows directly they replay exactly
what protocol-following service code writes (next fenced slot, fresh
identity, write-ahead order) — front-door decision defects, never a
privileged bypass; issue 4's DELETE is a restore simulation, not an
actor. In v2 terms: issue 1 → the head CAS + trigger backstops, issue 2
→ the head money witness + drift scan, issue 3 → money-facts-as-events
+ the fold deploy gate, issue 4 → request-ordinal identity, issue 5 →
the no-erasable-PII-in-events prerequisite.

## The honest conclusion

Model 2 survives. So the demo does **not** prove that an append-only,
no-obligation-row design is unsound. What it proves is sharper and more
useful:

1. **The original claim was wrong as stated.** "Hash to the same thread →
   thread-safe → locks unnecessary" and "just insert everything" fail in
   four distinct, reproducible ways (model 1).
2. **The proposal becomes safe exactly by re-adding the guard set:** a
   durable per-scope serialization point (the version fence *is* an
   optimistic lock on an aggregate), write-once fenced identity, DB
   uniqueness backstops, write-ahead evidence, idempotent folding,
   ask-before-retry, and a strictly-newer admission guard (model 2). Every
   one of these is a mechanism the proposal set out to delete, re-invented
   in event-sourcing dress.
3. **The remaining choice is materialization, not safety:**
   - *fold-on-read vs stored ledger* — in model 2, `required/paid` exist
     only as folds: every reader (UI amount series, ops console, drift
     scanner, §12 pagination) must re-implement the fold byte-identically,
     forever; the schema cannot enforce ledger arithmetic (there is no
     counter for I1/I2 cross-checks to anchor on);
   - *optimistic vs pessimistic serialization* — the fence retries on
     conflict (fold loops under contention) where the lock waits;
     equivalence under Oracle contention, and the retry-loop's interaction
     with the outbox/log-delivery contract, would need real-Oracle
     evidence either way;
   - *migration* — model 2 is a greenfield shape; the reviewed design's
     migration/backfill/cutover story (P3/M.x) exists because this system
     replaces a legacy flow in place.

   Choosing between the two surviving models on those grounds is a
   legitimate engineering discussion — with the requirement's evidence
   gates (real-Oracle tests, provider CT proofs, go-live artifacts)
   applying equally to whichever is chosen.

## Notes and limits

- H2 stands in for Oracle: constraint and locking semantics are close but
  not identical; nothing here is performance evidence. The expectation
  that uncontended row locks are cheap at <100k payments/month is exactly
  that — an expectation, to be confirmed by the real-Oracle load tests the
  playbook already gates on (EXPLAIN plans, T-matrix INTEGRATION lanes).
- The fake provider is a model of the contract facts, not of any real
  engine; the real facts are established only by the CT-02..05 sandbox
  evidence.
- Model 2's schema enforces fencing and identity; its *ledger arithmetic*
  lives in fold discipline. A reader that folds differently is a silent
  correctness bug — the class of risk the stored ledger + drift scanner
  exist to catch.
- This demo is isolated from the normative documentation set and is not
  linted by `tools/doc-lint.py`.
