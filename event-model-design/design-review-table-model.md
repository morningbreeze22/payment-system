# Review: Baseline State Model (v4) vs Event-Model Schema — Direction, Evidence, and the Repaired Alternative

Revision 2. Incorporates: (a) the confirmed upstream sequence number, (b) the team's concern about the four-dimension state model, (c) a sharpened, evidence-based version of the money-safety case — including one retraction — and (d) the repaired event design (`event-model-v2.md`), which changes the end-state comparison. Scope remains table-design direction, judged against the full requirement set, in your priority order: money safety, simplicity, performance.

## 0. New fact first: the sequence number is confirmed

Upstream's ordering value is a real per-trade sequence number carried in the message. Cash this in before comparing anything, because it deletes the single largest complexity cluster in the baseline: the entire tie class. AMENDMENT_TIE_CONFLICT, payload-equality and digest-based tie detection at admission, the `≥` relaxation, and most of the §20-10 reprocess-snapshot apparatus (digest-bound dual-control approvals, consume-at-start semantics, completion-evidence alerts) exist *only* because a business timestamp could tie. With a strictly-increasing sequence, an equal sequence must be an identical redelivery, and differing content at an equal sequence is an upstream defect (refuse + CRITICAL alert), not a manual-adjudication workflow. Get "strictly increasing per business_id" in writing — BA-3 already places ordering correctness upstream — and delete that machinery. This benefits both designs equally (admission is shared), so it does not change the comparison; but it does mean the v4 the team read is meaningfully larger than the v4 they would build.

## 1. Money safety

### 1.1 A retraction, then the argument that survives it

The team pushed back on the "independent witness" argument as originally stated, and the pushback is partially correct: a *shared semantic misunderstanding* — the team simply believing a wrong amount rule — writes v4's counters and rows consistently, and the drift scan passes. Both designs fail identically on that class. Retracted as stated.

What survives is narrower and concrete. v4's drift scan compares two **independently maintained representations** of the same money: increment/decrement choreography on the obligation row versus summation over request-row states. It therefore catches *mechanical divergence* — a code path that forgot a decrement, a race that violated the CAS discipline, a bad migration, a partial manual fix, restore inconsistency. The v1 event model's self-check re-runs *the same fold* against its own cached output: it can detect nondeterminism and projection staleness, but never a wrong or changed rule, because both sides of the comparison are one function.

The sharp, operational form of this concern is **fold versioning**. In v4, fixing a bug changes future writes only; history stays as written, and a wrong row is corrected by an audited operation. In a fold-derived design, deploying a fold fix retroactively re-derives every historical payment's state — paid totals for settled payments can change over a weekend deploy, with no record of which fold version made which decision unless versioning governance is built. This is not an outside critique: the v1 event doc itself ranks it CRITICAL (its L2) and calls the governance undesigned.

### 1.2 The identity-under-restore point, restated fairly

The team is also right that the idempotency key "can be defined differently" — the v1 draft's flaw (identity from version slots, which restore re-deals; its own L6, self-labeled a money-safety blocker) is a flaw of *that draft*, not of event sourcing. The fair statement is: v4 has the restore-deterministic identity **designed and closed**; the v1 event draft has it **open by its own admission**. The repaired design (`event-model-v2.md` §3) closes it the same way v4 does — a request ordinal decoupled from stream position — at which point the two designs are equivalent on this axis.

### 1.3 DB-enforced backstops

v4's I6 (function-based unique index: at most one ACTIVE request per obligation) makes the classic double-pay state — two live requests for one shortfall — a loud constraint violation regardless of which code path is buggy, plus legality CHECKs, the freeze trigger, and the release-guard trigger. The v1 event draft concedes (its L1, CRITICAL-accepted) that cross-row legality has no schema backstop: two `REQUEST_OPENED` events carry *different* keys, so its uniqueness constraints do not object to exactly the state that pays twice. The repaired design closes the critical part of this with a fold-independent head-row CAS plus trigger backstops (v2 §5.2–5.3); full temporal legality remains code-enforced there, comparable in kind to v4's non-CHECK-able rules.

### 1.4 Where the event model is genuinely safer

Append-only eliminates the wrong-UPDATE bug class entirely — v4's safety depends on every writer forever carrying the CAS discipline, which is a culture, not a constraint. `POST_STARTED` as a durable event makes write-ahead structural, and "no POST_STARTED in the stream = provably never sent" is a cleaner safe-release predicate than a `submission_state` column discipline. Contradiction evidence is recorded first-class rather than alert-only. Episode-anchor timestamps (a chronically fiddly part of v4 — maybe_since, escalated_at, the churn rules) become trivial: event timestamps are set-once by immutability.

**Money-safety verdict, updated:** against the v1 draft — v4, clearly; the draft's own ledger concedes the money items. Against the repaired v2 — approximate parity by mechanism, with a residual asymmetry of *maturity*: v4's mechanisms have survived fourteen adversarial review rounds; v2's equivalents are one refactor old and have survived none.

## 2. Simplicity — including the four-dimension question

First, calibration: the 5,000-line requirement doc is mostly **essential** complexity (at-least-once delivery, out-of-order full snapshots, absence-as-cancellation, ambiguous POSTs, restore semantics). The event schema doc looks simple partly because it inherits "§4/§6/§7/§9/§10 verbatim" by reference — the fold must implement every one of those rules. Complexity moved; it did not shrink. Comparing 300 lines to 5,000 is the trap to avoid — especially now that §0 shrinks the 5,000.

### 2.1 Does event-driven solve the four-field problem? Mostly no — and partly it regresses it

The team finds `stage / stage_state / submission_state / outcome` hard to understand and fears implementation bugs. Two honest observations:

The four facts do not go away under events. The fold must still track pipeline position, money truth, who acts next, and why blocked — the v1 draft's own fold output contains `phase: OPEN / POSTING / AWAITING / MAYBE / RETRY_WAIT / PARKED`, which is a *compound* status. Your own project history is the evidence for why that shape is dangerous: v3 had a 13-value compound status, and v4's changelog records that **four separate review rounds found bugs whose root cause was a rule keyed on the compound status when it meant one dimension**. The four fields are scar tissue, not decoration. Under events they still exist — implicitly, un-SELECT-able, and without CHECK enforcement.

The legitimate kernel of the complaint: in v4, every write site must produce a correct four-column tuple, and scattered UPDATE sites are scattered chances to get it wrong. Event sourcing fixes that by centralizing interpretation in one fold. **But the same benefit is purchasable in v4 without changing the storage model:** implement one transition module — a pure function `(currentTuple, event) → newTuple + money effects`, golden-vector tested exactly as a fold would be — and make every CAS site call it. Developers then use named operations (`recordAcceptance()`, `recordAmbiguousPost()`), never raw tuples; v4 already half-mandates this ("shared transition service"). And v4's CHECK constraints turn a developer's misunderstanding into a **loud constraint violation in the first integration test**, where the event model folds the same misunderstanding into legal-shaped, silently wrong state. If the fear is "implementation issues," declarative enforcement is the strongest insurance either design can offer, and v4 has more of it.

### 2.2 The honest simplicity ledger

The event model genuinely wins: one write discipline (in v2, the same lock-then-write idiom the team already knows); testability (pure fold + golden vectors beats concurrency tests over mutable rows); no UPDATE-discipline to police; set-once timestamps for free; and in v2, the transition history v4 lacks locally comes built-in.

v4 genuinely wins: state is SELECT-able (incident diagnosis is a query, not a tool); reads/UI/ops are direct; the team's stack and instincts are relational; and — the underweighted item — **fourteen rounds of review capital are embodied in its rules**. The v1 event draft demonstrated the relearning cost empirically in a single cycle: it dropped the inbox and had to restore it after review, asserted the restore story "applies unchanged" and later marked that claim WRONG in its own text, and needed review to catch the NULL/three-valued-logic CHECK gap and the stale-projection false-negative hazard. Four-plus re-discoveries in one pass, over ground v4 had already secured; the v2 refactor found two more (no ops marker-clear event; missing query result codes). There are roughly fifty such closed findings in v4. That is the measured error rate of rewriting, not a theory about it.

## 3. Performance — zero weight

At ~3,000 trades/day both designs are idle. Streams are short, so folds cost microseconds; lock granularity is identical (per payment) in both; v2's head-row protocol is literally v4's locking idiom. The event table grows without bound and needs an archival story eventually; v4 has the same issue for terminal request rows. Do not let performance arguments influence this decision in either direction.

## 4. The evidence-based case for the baseline, in two arguments

If the team asks "why stick with design 1," these are the two arguments that rest on evidence rather than theory:

**Argument 1 — the designs' own status ledgers.** The v1 event doc self-reports two accepted CRITICALs (L1, L2), one undesigned money-safety blocker (L6: "required before adoption," its words), and four open MEDIUMs (shape matrix, projection freshness, inbox transaction boundary, UI read contract). v4's blockers are external contract proofs (§18) — which any event design inherits anyway, since both stand on the same engine facts. One design is blocked on the payment engine; the other is blocked on the payment engine *and on itself*. (The v2 refactor closes the self-blocking items — but v2 is new, and closure claims are exactly the kind of thing v4's history says need adversarial review.)

**Argument 2 — the measured cost of re-deriving closed findings.** The rewrite's first pass re-opened at least four issues v4 had already closed and missed two vocabulary gaps found only in the second pass (§2.2 above). Extrapolated over v4's ~fifty closed findings, choosing the event path means re-earning a large fraction of the review history — while the thing the team actually dislikes (tuple handling spread across write sites) is fixable in v4 with one module and zero schema change.

And one argument to *stop making* (retracted in §1.1): "a bug in our code would corrupt the event model" — stated that broadly, it is symmetric and the team is right to reject it. The defensible forms are the mechanical-divergence witness and fold-version reinterpretation, both of which are specific, checkable, and acknowledged inside the event doc itself.

## 5. End state: two viable paths, and what each costs

**Path A — v4-slim.** v4 minus the tie machinery (§0), minus the deferrable items below, plus a single pure transition module as the only writer of the four columns, plus (optional, recommended) an insert-only event-history table written in the same transaction as every mutation — which subsumes the §14.1 journal, de-fangs the §14 best-effort log gap, and buys the event model's forensics without moving authority. Cost: living with the four-column model (made ergonomic, not removed). Risk profile: lowest — every mechanism is review-hardened.

**Path B — the repaired event model (`event-model-v2.md`).** Append-only authority, request-ordinal identity, transactional head row (lock + money witness + open-request backstop + scanner index), atomic inbox, view-based request-granular UI, defined contradiction exit, fold deploy gate. Cost: the fold must re-implement ~fifty review-closed rules and then survive its own adversarial review rounds; the team must build and keep the fold-explain tooling and the deploy-gate discipline; ops must learn to read streams. Risk profile: higher near-term (unreviewed mechanisms), attractive long-term properties (immutability, built-in history, testability).

Deferrable in either path: the request-granular all-payments table at MVP (in Path B it is nearly free via the view, so this mostly matters for A); the §14.1 content journal (switch-gated, default off — in Path B the stream subsumes most of its driver); provider_reference fallback matching; the graduated reject-count rules (flatten to one rule at this volume); escalation tiering polish.

Non-negotiable in either path: deterministic write-ahead identity; the single-active/open-request invariant with a fold-independent backstop; release rights moving only on first-party or platform-verified evidence; the trade-level admission watermark; amount equality under the all-or-nothing contract; the §18 item-1 sandbox proof of the engine collision contract — every safety argument in both designs stands on that engine behavior.

## 6. Bottom line

Judged as artifacts on the table today: v4 is a hardened design with external blockers; the v1 event schema was a promising draft with internal money blockers, and the v2 refactor closes those blockers on paper. The remaining, real differentiators are no longer schema properties — they are **review maturity** (v4 has it; v2 must earn it, and the v1 cycle showed re-earning is not free) versus **write-path properties the team values** (immutability, one interpretation point, testability, built-in history). If money safety is genuinely priority one and the timeline is real, Path A is the conservative recommendation. If the team commits to Path B, `event-model-v2.md` is the version to present — and it should be presented together with a plan for the adversarial review rounds it has not yet had, because that, not the table design, is now the honest gap between the two.
