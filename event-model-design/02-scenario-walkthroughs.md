# Scenario Walkthroughs — the Event Model vs Every Playground Story

> Status: DRAFT (see `00-README.md`). One section per scenario in
> `payment-state-playground.html`, in the same order and under the same
> names. Notation: `v1, v2, …` are the fenced version slots of one
> payment's stream; `K@v` is the idempotency key allocated by the
> `REQUEST_OPENED` event that won slot `v`. Events joined by `+` are
> appended in ONE transaction (consecutive slots, atomic).

**The guard set, once** (every story below is just these five doing
their job):

1. **Fence** — every append claims the next `(payment_key, version)`
   slot; a stale plan loses the insert and must re-fold.
2. **Write-once identity** — `K = derive(payment_key, opening slot)`;
   the schema refuses a second claim of any key.
3. **Write-ahead** — `REQUEST_OPENED` (identity + hash) and
   `POST_STARTED` (posting claim + hash) are durable BEFORE the wire.
   Corollary: *no `POST_STARTED` event = provably never sent.*
4. **Ask-before-retry** — an ambiguous request is resolved by
   `QUERY_RESULT_RECORDED`, never by a blind re-POST; the only same-key
   re-send is the audited downgrade after NOT_FOUND past trust age.
5. **Idempotent fold + strictly-newer ordering** — paid is summed over
   DISTINCT executed keys; snapshot-derived events apply only with newer
   `upstream_ordering`.

---

## 1. "Everything works" (happy)

```
v1 REQUIRED_AMOUNT_SET(100, ord=1)
v2 REQUEST_OPENED(100, K@2, hash H1)        ← standing rule: shortfall 100, nothing open
v3 POST_STARTED(K@2, H1)                    ← committed, then the wire call
v4 POST_RESULT_RECORDED(ACCEPTED, uetr)
v5 SETTLED(100, K@2)                        ← feed evidence
```

Fold: required 100, paid 100 (distinct key K@2), shortfall 0 — complete.
"Frozen forever" holds structurally: no event can be rewritten, and any
late event about K@2 changes nothing because the fold already counts that
key exactly once.

## 2. "Temporary failures" (hiccup)

```
v1 REQUIRED(100) · v2 REQUEST_OPENED(100, K@2)
v3 ENRICH_FAILED(transient: account service timeout)
      → fold: retry due at t(v3)+policy; scanner finds it via the
        projection, ACTS by folding + fencing (outage gating: a tripped
        breaker means scanners make zero attempts — no budget burned)
v4 POST_STARTED(K@2) · v5 POST_RESULT(BUSINESS_REJECT: insufficient balance)
      → provably nothing executed; §7 policy: retry SAME key on schedule;
        business rejects count as success to the breaker
v6 POST_STARTED(K@2) · v7 POST_RESULT(ACCEPTED) · v8 SETTLED(100)
```

Guards at work: every retry attempt is a fresh fenced append — two
scanners racing the same due row cannot both act (one loses `v4`);
the key never changes across attempts of one request.

## 3. "Bad data" (baddata)

```
v1 REQUIRED(100) · v2 REQUEST_OPENED(100, K@2)
v3 ENRICH_FAILED(definitive) + OUTCOME_RECORDED(REJECTED_VALIDATION, at ord=1)   [one tx]
      → fold: no open request → reservation released; validation marker
        LATCHED carrying ordering 1
v4 REQUIRED_AMOUNT_SET(100, ord=2)          ← corrected upstream data
      → fold: marker ordering (1) < message ordering (2) → unlatched
v5 REQUEST_OPENED(100, K@5)                 ← standing rule, automatically; NEW key
```

The marker is not a column — it is derived from the outcome event and
compared against orderings, so "newer data unlatches" is pure fold logic
with the ordering guard preventing a stale replay of the OLD snapshot
from re-triggering anything.

## 4. "The bank says no" (bankreject)

```
v1 REQUIRED(100) · v2 REQUEST_OPENED(100, K@2) · v3 POST_STARTED
v4 POST_RESULT(DEFINITIVE_REJECT, code) + OUTCOME_RECORDED(REJECTED_PROVIDER, at ord=1)  [one tx]
      → provably not executed (synchronous definitive) → release safe;
        provider_rejected marker latched, reject count = 1
v5 REQUIRED_AMOUNT_SET(100, ord=2)          ← a newer message permits ONE more try
v6 REQUEST_OPENED(100, K@6)                 ← fresh identity, attempt 2
```

If attempt 2 is also rejected, the fold sees reject count ≥ 2 with no
newer ordering → derived state parks for humans. The "never ping-pong a
rejected payment" rule is the fold refusing to open while the latched
marker's ordering is not exceeded.

## 5. "Timeout → engine answers" (timeout1)

```
… v3 POST_STARTED(K@2) → wire → TIMEOUT (no result event exists)
      → fold: open request with POST_STARTED and no result = MAYBE;
        money stays reserved; nothing re-sends
v4 QUERY_RESULT_RECORDED(EXECUTED)          ← resolver asked by OUR key
   + OUTCOME_RECORDED(EXECUTED)                                [one tx]
```

The refusal to guess is structural: the only code path that could send
money again requires either a `DOWNGRADED_FOR_REPOST` event (below) or a
fresh `REQUEST_OPENED` — and the fold will grant neither while an
ambiguous request is open.

## 6. "Timeout → 'never saw it'" (timeout2)

```
… v3 POST_STARTED(K@2) → timeout
v4 QUERY_RESULT_RECORDED(NOT_FOUND, store young)
      → fold: young NOT_FOUND is not evidence (trust age) — no change
v5 QUERY_RESULT_RECORDED(NOT_FOUND, past trust age)
v6 DOWNGRADED_FOR_REPOST(K@2)               ← the ONE sanctioned backward move, audited
v7 POST_STARTED(K@2, same hash)             ← SAME key: if the first POST secretly
                                              arrived, the engine dedups (§18-tested)
v8 POST_RESULT(ACCEPTED) · v9 SETTLED(100)
```

Every query answer is itself an event — the trust-age reasoning is
reproducible from the stream alone, and the downgrade decision has a
durable audit row.

## 7. "Stuck unknown → ops" (stuck)

```
… v3 POST_STARTED(K@2, 100) → timeout → MAYBE
v4 REQUIRED_AMOUNT_SET(80, ord=2)           ← amendment while unknown
      → fold: required 80, but an ambiguous 100 is open → NO cancel,
        NO successor: wait-then-decide (cancelling could double-pay)
v5 ESCALATION_MARKED                        ← the unknown got old; pages ops; once
v6 OPS_VERIFIED_OUTCOME_APPLIED(NOT_EXECUTED, evidence ref)
   + OUTCOME_RECORDED(PLATFORM_VERIFIED_NOT_EXECUTED)          [one tx]
      → the dual-control operation; the ONLY manual door, demands
        platform evidence; arrives through the SAME fence as everything
v7 REQUEST_OPENED(80, K@7)                  ← standing rule, same tx or next fold
```

The unrouted-writer worry does not exist here: ops appends are fenced
like all appends, and the release guard is the fold refusing to release
reserved money without either a definitive result event or a
platform-verified outcome event.

## 8. "Amount raised mid-flight" (amendup)

```
… v3 POST_STARTED(K@2, 100)                 ← the 100 is on the wire
v4 REQUIRED_AMOUNT_SET(150, ord=2)
      → fold: in-flight amount immutable (it is an EVENT); shortfall 50
        exists but a request is open → successor DEFERRED (not stored as
        a task — recomputed from state, so it cannot be lost or doubled)
v5 POST_RESULT(ACCEPTED) · v6 SETTLED(100, K@2) + v7 REQUEST_OPENED(50, K@7)  [one tx]
```

"One creation point" is the standing rule re-running inside the SAME
transaction as any event that closes a request or changes the required
amount — the successor is born atomically with the fact that justified
it; there is no scheduler to forget it.

## 9. "Raised while unknown" (amendmaybe)

```
… v3 POST_STARTED(K@2, 100) → timeout → MAYBE
v4 REQUIRED_AMOUNT_SET(150, ord=2)
      → fold: shortfall 50, but the open request is AMBIGUOUS → the
        deferral waits on the OUTCOME, not a timer (bounded by query
        cadence, then the escalation clock)
v5 QUERY_RESULT(EXECUTED) + OUTCOME_RECORDED(EXECUTED)
   + REQUEST_OPENED(50, K@v)                                   [one tx]
```

Had the query said NOT_FOUND (past trust age → abandoned): the same
transaction would release and open the FULL 150 — the follow-up amount is
recomputed from live fold state, never remembered.

## 10. "Trade cancelled in time" (cancel)

```
v1 REQUIRED(100) · v2 REQUEST_OPENED(100, K@2)
v3 REQUIRED_AMOUNT_SET(0, ord=2)            ← the trade drops to zero
v4 OUTCOME_RECORDED(CANCELLED_NOT_SUBMITTED)  ← automatic, because…
```

…the fold can PROVE nothing was sent: there is no `POST_STARTED` event
for K@2. Write-ahead makes "provably unsent" a first-class stream fact —
this is one place the event model is naturally elegant. Had a
`POST_STARTED` existed: no cancel, wait-then-decide, exactly scenario 7.

## 11. "Retries run out" (exhaust)

```
… repeated ENRICH_FAILED / POST_RESULT(BUSINESS_REJECT) events
      → fold counts attempts from the stream; budget exceeded →
        derived BLOCKED(RETRY_EXHAUSTED); outages never consumed budget
        (gated scanners appended nothing — zero-attempt episodes leave
        zero events, by construction)
vN OPS_RETRY_REARMED                        ← human re-arms via the ops surface
      → fold: budget window resets from vN; the payment resumes at the
        stage it stopped (the stream knows exactly where that was)
```

## 12. "The helpful collision" (collision)

```
… v3 POST_STARTED(K@2, hash H1) → timeout
v5 NOT_FOUND past trust age · v6 DOWNGRADED_FOR_REPOST
v7 POST_STARTED(K@2, hash H2)               ← re-enrichment changed the payload;
                                              H2 ≠ H1 is IN the durable record
                                              BEFORE the wire → divergence EXPECTED
v8 POST_RESULT(COLLISION: known key, different content)
      → fold: expected divergence → good news — it PROVES the original
        POST arrived; no alarm; ask the resolver
v9 QUERY_RESULT(EXECUTED, original) + OUTCOME_RECORDED(EXECUTED)  [one tx]
```

The baseline's `last_sent_hash`/`divergence_expected` columns become a
comparison between the two `POST_STARTED` events' hashes — same
semantics, derived instead of stored.

## 13. "Two payments, one trade" (twinpay)

```
Admission (unchanged): TRADE_SNAPSHOT_STATE row for trade T —
newest-wins ordering + digest; ONE admission transaction.
Fan-out, in sorted payment_key order:
  stream A: v1 REQUIRED_AMOUNT_SET(100, ord=1)
  stream B: v1 REQUIRED_AMOUNT_SET(250, ord=1)
```

Each payment runs the entire machine on its OWN stream with its OWN
fence — A posting while B enriches is normal; "one active request" is
per stream. Snapshot 2 (A unchanged, B→300): fan-out appends only to B
(A's append is skipped by the ordering guard — a no-op is a no-event).
Fixed fan-out order + per-stream fences + idempotent ordering-guarded
appends ⇒ two redeliveries cannot deadlock, and a crashed half-fan-out
is safely re-run.

## 14. "A trade-mate poisons the snapshot" (poison)

```
Snapshot 2 (A→150, B corrupt) FAILS whole-document validation at
admission → NOTHING of it is applied; fan-out appends instead:
  stream A: vN SNAPSHOT_INVALID_MARKED(ord=2)
  stream B: vN SNAPSHOT_INVALID_MARKED(ord=2)
      → fold(A): required STILL 100; flag blocks NEW opens only —
        A's in-flight attempt is untouched (its events are immutable)
A settles: SETTLED(100) applies normally — terminal evidence is never
blocked by the flag.
Snapshot 3 (corrected, ord=3):
  stream A: REQUIRED_AMOUNT_SET(150, ord=3)   → flag unlatches (newer wins)
            + REQUEST_OPENED(50, K@v)          → the deferred raise starts
  stream B: its own corrected amount, own stream
```

Fail-closed with zero blast radius on running work — the flag is an
event whose only fold effect is vetoing `REQUEST_OPENED`.

## 15. "Wrong-amount settlement" (mismatch)

```
… v4 POST_RESULT(ACCEPTED) — awaiting settlement of 100
v5 SETTLEMENT_MISMATCH_RECORDED(60)
      → all-or-nothing engine ⇒ 60 cannot be real money — it is DEFECT
        EVIDENCE: fold books NOTHING, parks loudly; submission knowledge
        still tightens (even wrong evidence proves it was sent)
v6 SETTLED(100)                             ← the correct settlement, later
      → fold books 100 against K@2 exactly once; the park clears; the
        mismatch event remains forever as the defect's audit record
```

---

## Summary table

| # | Scenario | The guard that carries it |
|---|---|---|
| 1 | Everything works | write-ahead + fence; idempotent fold freezes terminals |
| 2 | Temporary failures | fenced retries (racing scanners can't double-act); same-key policy |
| 3 | Bad data | outcome+marker in one tx; ordering-guarded unlatch; fresh identity |
| 4 | The bank says no | provably-not-executed release; latched marker + one-more-try by ordering |
| 5 | Timeout → answers | no-guess is structural: nothing can re-send while MAYBE is open |
| 6 | Timeout → never saw it | recorded query evidence; audited same-key downgrade |
| 7 | Stuck unknown → ops | wait-then-decide from fold; fenced dual-control verified outcome |
| 8 | Raised mid-flight | immutable in-flight amount; successor born atomically with the trigger fact |
| 9 | Raised while unknown | deferral waits on outcome; follow-up recomputed, never remembered |
| 10 | Cancelled in time | **no POST_STARTED = provably unsent** — auto-cancel is a stream fact |
| 11 | Retries run out | budget counted from events; outages leave no events; ops re-arm is an event |
| 12 | Helpful collision | pre-wire hashes make divergence expectation derivable and auditable |
| 13 | Two payments, one trade | per-stream fences; admission table unchanged; ordered idempotent fan-out |
| 14 | Poisoned snapshot | flag event vetoes new opens only; immutability protects in-flight work |
| 15 | Wrong-amount settlement | defect evidence books nothing; late truth books exactly once |
