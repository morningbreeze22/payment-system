# Baseline vs Event Model — Comparison and Recommendation

> Status: DECISION-SUPPORT document, 2026-07-22. Compares the adopted
> baseline (`requirment-v4.md` + the implementation playbook) against
> the event-model proposal (`event-model-v2.md` +
> `01-event-table-schema.md`), after the event model's 32-round
> adversarial campaign closed clean (00-README item 1).
>
> Method: one full external comparison review (fresh session, high
> reasoning effort, both designs read end-to-end, every claim
> section-cited) plus the maintainer's own analysis. Where the two
> agree the verdict is stated once; where they differ the difference
> is called out. The final opinion section is the maintainer's own.
>
> This document decides nothing. The decision belongs to the team; the
> final comparison gate remains 00-README item 7 (full §-by-§ parity
> review on TOTAL complexity).

## 1. Verdict table

| Dimension | Winner | One-line rationale |
|---|---|---|
| Money-safety | **Event model** | Both are fail-closed designs; the event model adds immutable authoritative history plus independent fold / witness / CAS / trigger / shape / version backstops, so a single defective front-door path can silently rewrite or mis-book less (v4 §3/§9.4/§10.3/§14 vs v2 §2/§4/§5.1–5.3) |
| Simplicity | **Baseline** | Current state is directly represented in rows; the event model demands the canonical fold, 19 event shapes, generated constraints, a transaction-fresh witness, compound triggers, and stream-reading skills (v4 §2/§11–12 vs v2 §1–2/§5/§8/§10.6) |
| Scalability / performance | **Baseline, narrowly** | Lock contention is a tie (per-obligation vs per-payment-head — same granularity); the baseline avoids folding a growing stream under lock on every action and reads current state directly. Immaterial at ~3,000 trades/day for both (v4 §11/§16.5 vs v2 §5.1/§9/§10.4) |
| Implementability & assurance cost | **Baseline** | Its burden is already DECOMPOSED: DDL, test catalog, cards, runbooks, blocking gates exist and were reviewed to "ready to hand to coding models". The event model's equivalents (fold + golden vectors, generated 19-constraint set, real-Oracle trigger evidence, fold-deploy procedure, parity review) are enumerated but unbuilt (v4 §16.6/§18/§20 vs 00-README items 2–7) |
| Operability | **Tie** | The event model wins diagnosis and correction audit (head = "where", permanent stream = "why", corrections supersede instead of overwrite); the baseline wins day-one readiness (monitoring scope, queue views, runbooks, alert rollup already specified). Failure recovery is a wash — the event model deliberately inherits the identity/restore/freeze story (v2 §3/§9) |
| Evolvability | **Event model, narrowly** | Behavior evolves in one versioned fold without rewriting historical facts; corrections are new facts. Bounded advantage: new event types are design + DDL changes, and every fold change pays the full-population re-fold gate (v2 §4.2/§10.2 vs v4 §2.2/§16.5) |

Score-counting the table misleads: the dimensions are not equally
weighted, and the stated priority of this project (PO standard: rank
by remediation shape; critique via front-door defect blast radius)
weights the first row far above the rest. That is what §5 turns on.

## 2. The dimensions in detail

### 2.1 Money-safety — event model

The baseline is already fail-blocked, not fail-open: money arithmetic
under the obligation lock, one-active-request as a function-based
unique index (I6), terminal-negative releases row-count-gated, the
legality matrix and release guard trigger-backstopped, deterministic
keys turning a restore-recreated request into an engine collision
instead of a second payment (v4 §3, §5.1–5.2, §6.8, §9.4, §10.3, §11).
Nothing here calls it unsafe.

The event model is structurally stronger against exactly one class —
the class this project's critique standard says matters most: **a
defective front-door application path emitting legal-looking writes.**

- **What one wrong write can destroy.** In the baseline, a wrong but
  well-formed `UPDATE` replaces the previous local state; the §14
  transition log is after-commit, at-most-once, gaps possible by
  contract; the §14.1 attempt journal is never-load-bearing content,
  not transition history. Reconstruction after a bad deploy is
  cross-system forensics. In the event model, history cannot be
  edited by anyone — the application role has INSERT+SELECT only, a
  guard trigger raises on UPDATE/DELETE, rows are permanent — so a
  wrong decision adds a wrong fact NEXT TO the true ones; it cannot
  erase evidence (v2 §2, 01 §2).
- **How many independent nets a wrong money write must pass.** The
  event model stacks vetoes that do not trust the fold or each other:
  the witness check (fold vs mechanically-incremented head, every
  write), the one-open-request CAS, opening-amount = shortfall,
  terminal-amount = opening amount (both directions, both
  bookkeepers), key echo, release-rights predicate, downgrade gate,
  dual-control pair gate with DB-checked same-transaction membership,
  UETR association + `PH_UETR_UQ`, version continuity, and the fence
  (v2 §5.1–5.3, 01 §6). The baseline has strong but fewer independent
  layers, and its journal/log cannot arbitrate a dispute.
- **Honest residue on both sides.** A legal-SHAPED but semantically
  wrong event sequence still harms (v2 §10.1) — the same class as a
  wrong CAS in the baseline. The commit-to-wire window is identical
  in kind in both. Both stand entirely on the §18 engine collision
  contract. The event model makes silent loss HARDER, not impossible.

### 2.2 Simplicity — baseline

At the surface both have four structures. The baseline's complexity
is choreography: four request dimensions, counters, markers, episode
clocks, CAS predicates, token-lock discipline, a lint-frozen SQL
inventory — substantial, but each piece reads like ordinary
row-oriented engineering, and "SELECT the state" works.

The event model concentrates its complexity: a new engineer must
internalize stream-authority vs veto-only head, the fold, 19
type/shape combinations, per-event insert + head-effect ordering,
closed-ordinal correction mechanics, and inbox association rules —
and needs `fold --explain` (an MVP deliverable, v2 §10.6) because
"SELECT the state" no longer answers "why". One fairness note the
external review also lands on: the final comparison must be on TOTAL
complexity — build + assurance + operations — which is precisely
00-README item 7, still open. The baseline's complexity is spread
across a large playbook; the event model's is dense in one place.

### 2.3 Scalability / performance — baseline, narrowly

Contention is a genuine tie: one hot row requires one hot payment in
both designs, and both use the same TRADE→PAYMENT lock order. The
baseline reads current state from indexed mutable columns with
active-row-bounded scan indexes; the event model folds the full
stream under the lock before every action and keeps every row
forever (partitioning-only tiering, index-maintaining DDL). At
~3,000 trades/day (<1M event rows/year, decades of Oracle headroom —
v2 §10.4) neither design is stressed; the baseline simply has more
margin. A future 100× volume reopens the event model's archival
questions it deliberately refused to carry.

### 2.4 Implementability & assurance cost — baseline

This is the baseline's decisive dimension and it should be stated
plainly: the baseline is not "less work in principle", it is **work
already done**. Years-equivalent of review are embedded in artifacts
a coding-agent workforce can execute under the existing human gates:
exact DDL, the frozen keyset/query inventory, the test catalog,
producer cards, go-live evidence packets, runbook stubs, and the §18
blocking gates — reviewed to the explicit verdict "ready to hand to
the coding models under the playbook's human gates."

The event model's paper is now comparably hardened (32 rounds,
24C/44H/12M/5L folded, exit clean), but its equivalents of those
artifacts do not exist: the fold + golden vectors (item 2), the
fold-deploy operational procedure (item 3), the upstream sequence
contract in writing (item 4), real-Oracle proof of the compound
trigger / statement order / generated 19-constraint parity set (item
5), the PII vault decision forced by permanence (item 6), and the
§-by-§ parity review (item 7). Two honest cuts on the same fact:

- For an agent workforce, the event model's backstops protect
  AGAINST agent-written front-door bugs better — but only after
  humans have proven the backstops themselves on real Oracle.
- Choosing the event model costs a bounded, enumerable list of
  proofs (a schedule cost); choosing the baseline keeps a structural
  property gap (mutable authoritative state, gap-tolerant history)
  for the system's lifetime.

### 2.5 Operability — tie

Event model: better forensics (the stream is complete local history;
`UNMATCHED_TERMINAL` is durable, level-triggered; corrections
supersede rather than overwrite; a dispute between bookkeepers
quarantines loudly as `WITNESS_DIVERGED`). Baseline: better day-one
readiness (monitoring scope, alert rollup, queue views, freeze
behavior, the three non-waivable manual exits — all specified) and a
lower on-call learning curve. Both share dual-control §9.3 exits, the
Hazelcast freeze, deterministic-key restore. Neither dominates.

### 2.6 Evolvability — event model, narrowly

The event model changes BEHAVIOR in one versioned fold and never
rewrites facts; the baseline more often needs column/CHECK
migrations under its (correct) no-churn discipline. The advantage is
real but bounded: `EVENT_TYPE` is CHECK-closed, a new type is a
design change entering the generated constraint set, and every fold
change pays the full-population deploy gate. Neither design permits
casual schema evolution — by explicit project rule.

## 3. Pros and cons, compressed

**Baseline — pros:** implementation-ready (cards/tests/gates exist,
reviewed); simpler mental model, direct current-state reads;
already-strong money controls (I6, release guard, legality matrix,
drift scans). **Cons:** authoritative state is mutable — a front-door
defect can destroy prior local state; transition history is external
and gap-tolerant by contract; wide choreography (dimensions,
counters, markers, token locks) must stay synchronized forever.

**Event model — pros:** strongest structural protection against
silent rewriting (immutability + witness + CAS + triggers + fence,
all fold-independent); complete local history with append-only
dual-control corrections; one interpretation point (the fold) that is
pure, versioned, golden-vector-testable. **Cons:** highest
implementation-assurance burden, still unbuilt (00-README items 2–7);
permanent rows + fold-under-lock on the action path (fine at this
volume, a real question at 100×); ops must learn to read streams
(`fold --explain` is load-bearing for adoption).

## 4. What changed since the last time this question was asked

When the PO previously asked for an honest pick (before the v2
campaign), the answer here was BASELINE, and the stated reason was
review maturity: the baseline's rules embodied a long adversarial
history; the event model's equivalents had earned none of that. That
gap is what the 32-round campaign was for, and it is now materially
closed **at the design level**: the event model's guard inventory
survived the same class of external attack the baseline's did, and
its two weakest subsystems (archival, inbox correspondence) were
removed rather than patched — the same removal-over-machinery
judgment the baseline campaign kept making. What did NOT change: the
baseline still owns the only implementation-grade artifact set.

## 5. Opinion — which one I would choose

**I would choose the event model — conditional on its checklist
gates, with a defined fallback.** The external comparison review
independently reached the same verdict; the reasoning below is mine.

Decisive factors, in order:

1. **The system is not live.** There is no migration on either path
   (PO-established fact, struck both ways). This is the one moment
   where the structurally better storage model can be chosen at
   schedule cost alone; after go-live that option closes permanently.
2. **The project's own top value picks the winner of its top
   dimension.** By the PO's critique standard — front-door defect
   blast radius — the event model is the stronger design, and the
   margin is structural (immutability + more independent vetoes),
   not incremental. A coding-agent workforce makes this weigh MORE:
   the marginal defect this project will actually produce is a
   plausible-looking wrong write in generated code, which is exactly
   the class the event model's fold-independent backstops refuse.
3. **The costs are asymmetric in kind.** The event model's deficit
   is a bounded, enumerable work list (00-README items 2–7 — every
   item has a named artifact and a pass/fail shape). The baseline's
   deficit is a permanent property of the system (mutable authority,
   gap-tolerant history) that no amount of later work removes.
4. **The volume fits.** Permanent rows and fold-on-action are the
   right trade at ~3,000 trades/day; this recommendation does not
   survive a 100× volume assumption unchanged.

The conditions, without which this opinion flips to the baseline:

- Items 2–5 of the checklist (fold + golden vectors; fold-deploy
  procedure; upstream sequence contract in writing; real-Oracle
  evidence for the compound trigger, statement order, and the
  generated constraint parity set) pass under the SAME human gates
  the baseline's playbook mandates — no dilution, no "agents proved
  it to themselves".
- The PII-vault rule (item 6) is decided BEFORE the first production
  event, because permanence makes it unfixable after.
- The §-by-§ parity review (item 7) confirms no inherited v4
  semantic was lost in translation — the fold IS v4's §4/§6/§7/§9/
  §10 or the design forfeits its "same semantics, better storage"
  claim.
- If the team cannot fund those proofs inside the delivery window,
  build the baseline: it is the design that is READY, its safety is
  proven-good rather than merely weaker-on-one-dimension, and most
  of its business-semantics artifacts (golden vectors, CA-1 mapping,
  §18 gates, ops protocol) transfer to a future event-model
  migration study anyway.

What I would NOT do, under either choice: treat the event model's
event vocabulary as a license for schema churn (the no-churn rule
binds identically), or dilute either design's guard set in the name
of simplification — the runnable `tl-proposal-proof` suite in this
repo exists precisely because every deleted guard came back as a
demonstrated double-pay.

## 6. Pointers

- Concepts, tab by tab: `event-model-explained.html`
- Scenario walkthroughs (stream + head + fold, step by step):
  `event-model-playground.html`
- Normative design: `event-model-v2.md` (rationale + campaign log §0),
  `01-event-table-schema.md` (schema reference)
- Decision gates: `00-README.md` checklist items 2–7
- Baseline: `requirment-v4.md` + the portable implementation playbook
