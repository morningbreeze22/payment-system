# TL-Proposal Proof — runnable A/B comparison

> **Purpose:** a minimal local Spring Boot app (H2 in-memory, no Kafka, no
> external services) that puts BOTH designs side by side and drives them
> through the failure windows the requirement is built around:
>
> 1. **The proposal:** hash-by-scope-key thread affinity replaces the locks;
>    one insert-only request/event table replaces the obligation table
>    ("reconstruct the whole flow from this table").
> 2. **The reviewed design** (requirment-v4.md), reduced to its essentials:
>    obligation row (ledger + lock + counter), write-ahead identity, guarded
>    CAS, I6-style unique constraints, ask-before-retry, release guard.
>
> Nothing here is production code. It exists to make one afternoon's
> conversation concrete.

## Run it

Only a JDK (17+) is required:

```
cd tl-proposal-proof
.\mvnw.cmd test        (Windows)
./mvnw test            (mac/linux)
```

Or open the folder in IntelliJ and run the two test classes.

- `NaiveModelFlawsTest` — the proposal, built faithfully. **Every green
  test is a proven flaw**: the assertions assert the damage happened.
- `GuardedModelSurvivesTest` — identical scenarios against the reviewed
  design; the assertions assert the damage did NOT happen and that
  believed state == the provider's internal truth.

## What the proposal is GIVEN before the tests start (the concessions)

- **Deterministic identity** (`scope#seq`) is borrowed from §5.1 — without
  it the naive model fails instantly, so it gets our idea for free.
- **Thread affinity holds in steady state**: each test hands a message to
  one thread at a time. The scenarios attack only the windows affinity
  structurally cannot cover — which is the entire argument.
- The fake provider honors the §18-1 dedup facts (same key + same payload
  never re-executes while the key is retained).

## The scenarios

| # | Window attacked | Naive result (asserted) | Guarded result (asserted) | Requirement anchor |
|---|---|---|---|---|
| 1 | **Zombie redelivery** — the old partition owner stalls (GC/slow wire), Kafka reassigns, at-least-once redelivers; the zombie later finishes its stale plan | Duplicate seq-1 rows admitted; the fold double-counts outcomes (believes 250 paid, truth 150); the next amendment folds "nothing owed" and **100 is silently unpaid — no alert, no trace** (the U-1 disappearance class) | The zombie's plan cannot exist outside the lock; it re-reads and no-ops; history exact; the amendment pays the delta | §11 claim/lease; §10 CAS; U-1 |
| 2 | **The unrouted writer** — a human rejects via the ops endpoint (HTTP is never on the hash ring) while the routed thread holds a stale fold | **Money moves after the human said stop**; the reject event sits uselessly in the history | Ops serializes on the same row lock; the worker sees BLOCKED and refuses. Bonus: the release guard refuses to let ops terminal-reject an in-flight (uncertain) row at all — resolve-first (§9), then decide | §9.3; §10.3 release guard |
| 3 | **Instance overlap** (rolling deploy) — two folds both compute `MAX(seq)+1 = 1` | **One idempotency key carries two different amounts** (the CT-03 hazard); the engine silently collapses; responses are indistinguishable; belief diverges from truth permanently | Seq comes from the locked counter, not observed history — reuse impossible; and the schema itself (I6 + UNIQUE(idem_key)) refuses rogue rows **from any writer, including the hotfix script nobody routed** | §5.1; §7.2 collision; I6 |
| 4 | **Crash between wire call and record** + dedup retention edge (§18-1(c)) | The history cannot distinguish "posted, record lost" from "never posted"; the only options are blind re-POST (**double-pays past the retention edge: 200 moved for 100 required**) or never retry (payment lost) | Write-ahead identity was committed BEFORE the wire; recovery **asks** (§9.1 query) instead of guessing; one execution, belief == truth, and the counter survives so seq is never reused | §5.1 write-ahead; §7.0 ask-before-retry; CT-04 |
| — | **Belief vs truth** (every test) | The naive model's folded state disagrees with the money the engine actually moved — and nothing inside the model can detect it | `confirmed_amount` == engine truth in every scenario | §10.4; drift scanner I1/I2 |

## How to read a "but you could fix that by..."

Every patch that fixes a naive failure re-invents a piece of the reviewed
design:

- dedup the fold by key → you are hand-writing UNIQUE(idem_key) in
  application code, in every fold, forever;
- fence the zombie → that is a lease (§11 claim protocol);
- route the ops endpoint through the ring → ops is now queued behind a
  consumer thread, and the NEXT unrouted writer (migration script, support
  tooling) still is not;
- record something before the wire call so retry can decide → that is
  §5.1 write-ahead;
- query the provider before re-POSTing → that is §7.0/§9.1
  ask-before-retry;
- keep a per-scope counter row so seq never reuses → that row IS the
  obligation table, minus the ledger you also need for release decisions.

That is the actual claim of the reviewed design: not that locks are
pleasant, but that **each mechanism is the minimum durable form of a fix
you will otherwise write ad hoc after the first incident** — enforced at
the database, where it holds against every writer, thread, instance, and
future colleague.

## What the volume argument actually buys

At <100k payments/month the obligation row costs nothing measurable, and
every lock in these tests is uncontended (nanoseconds). Key-affinity
dispatch is still a fine idea — as an *addition* that reduces contention
and reasoning load. These tests show what happens when it is used as a
*replacement*.
