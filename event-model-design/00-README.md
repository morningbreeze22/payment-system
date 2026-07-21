# Event-Model Design (Alternative B) — Exploration Docs

> **Status: DRAFT — design exploration, NOT the adopted baseline.**
> The authoritative design remains `requirment-v4.md` + the portable
> implementation playbook. This folder finalizes the EVENT-TABLE variant
> far enough that the two can be compared and decided on their merits.
> Nothing here changes the reviewed baseline; no card, test, or gate in
> the playbook refers to this folder.
>
> Origin: the runnable three-model comparison app in this repository
> (`proof.minimal`, "Model 2") proved that an append-only, single-table
> core CAN satisfy the money-safety scenarios — provided it carries the
> full guard set (version fence, write-once fenced identity, write-ahead,
> idempotent fold, ask-before-retry, strictly-newer admission). These
> documents scale that prototype up to the real requirement.

## Contents

| File | What it is |
|---|---|
| `01-event-table-schema.md` | The full schema: the event table, its constraints, the event vocabulary, the canonical fold, projections, and access paths — with the reasoning for every column |
| `02-scenario-walkthroughs.md` | Every scenario from `payment-state-playground.html`, replayed step by step in the event model, with the guard that makes each one safe |

## The two design rules everything else follows from

1. **The event stream is the only truth.** Current state is never stored
   authoritatively; it is computed by THE fold (one shared, versioned,
   golden-vector-tested implementation — never re-implemented per reader).
   Anything materialized (projections) is a rebuildable cache and is never
   read by a money decision.
2. **Every write is fenced.** An append must claim the next
   `(payment_key, version)` slot; the database grants each slot exactly
   once. Winning the slot proves the writer's fold was current; losing it
   forces a re-fold. There is no other write path — not for scanners, not
   for operations, not for humans.

## Open items before this could be adopted (honest list)

- Full §-by-§ parity review against `requirment-v4.md` (these docs cover
  the state machine and money core; the log/journal, observability,
  go-live evidence, and UI read contracts are sketched, not finalized).
- The migration story: the reviewed baseline replaces a legacy flow
  IN PLACE (dual-run, backfill, fenced cutover). The event model is a
  greenfield shape; an equivalent M.x plan does not exist yet.
- Real-Oracle behavior: fence-retry under contention, insert-only table
  growth/partitioning, and the append-only enforcement trigger need the
  same class of Oracle evidence the baseline's constraints have.
- Projection freshness contracts for the scanners and the feed-matching
  path (design says they are correctness-neutral; that needs its own
  test set).
- External re-review, as for every design decision in this repository.
