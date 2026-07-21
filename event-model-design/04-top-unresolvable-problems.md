# The Five Problems the Event Model Cannot Solve On Its Own

> **Status: DRAFT — companion to `03-known-limits.md`.**
> `03` is the full cost sheet (every known limitation, ranked by
> remediation shape). THIS document answers a narrower question:
> **of everything in 03, which problems can the design NOT resolve
> by itself — and survive even the assumption of good service code?**
>
> The filter applied to every candidate: *assume the team writes and
> enforces excellent service code. Does the problem go away?* If yes,
> the problem is demoted or excluded — a design should not be blamed
> for work that ordinary engineering discipline resolves. What remains
> below is ranked by how little good code helps: the top three cannot
> be resolved by code **even in principle**; the last two can be
> resolved, but only by **adding design**, not by writing careful code
> against the current schema.

---

## Problem 1 — Code cannot be its own backstop

**(from limit L1 · good code does not help: the problem IS the day the code is wrong)**

The only thing that enforces event *legality* — "is this event allowed
to follow that history?" — is the sole-append library. The database
enforces slot uniqueness (the fence) and per-event shape, but it is
content-blind across rows: it will grant slot v5 to any well-shaped
event, legal or not.

So the design has exactly one line of defense, and it is code. Good
service code lowers the *probability* of an illegal append — but this
problem is precisely about the day that code (or a path around it) is
wrong. There is no second, independent mechanism that catches the
mistake at write time. Asking the append library to guard against its
own defects is circular.

**Simple example.** A well-meaning "nudge stuck payments" endpoint is
added next year. It reads a stale fold, decides payment P-42 is idle,
and appends:

| V | EVENT_TYPE | note |
|---|---|---|
| 4 | `PROVIDER_OUTCOME_AMBIGUOUS` | truth: we must NOT open a new request until resolved |
| 5 | `REQUEST_OPENED` | the buggy nudge — **fence grants the slot, row COMMITS** |

The same bug against the baseline dies at commit with `ORA-00001` on
the I6 unique index — zero rows written, loud failure, no cleanup.
Here it becomes permanent history that every subsequent fold believes.

**Why it can't be resolved from inside.** Moving the check into the
database procedurally (a sole-append stored procedure that re-folds
and validates) is possible — but that is the fold rewritten in PL/SQL:
still code, still one implementation guarding itself. The *declarative*
form of this guarantee — a constraint the engine enforces regardless of
which code path writes — does not exist for cross-row temporal rules in
a single event table. Adopting the model means accepting enforcement by
disciplined code, permanently.

---

## Problem 2 — No independent witness for the money

**(from limit L2 · good code does not help: the fold IS the code, and you cannot verify a witness with itself)**

The amount paid is never stored; it exists only as the output of the
canonical fold. And the model's own safety rule — *one* shared,
golden-vector-tested fold, never re-implemented per reader — is
deliberately designed to make every reader agree. The cost of that
rule: every monitor, scanner, dashboard, and reconciliation-prep job
is the **same witness**. A defect in the fold produces a wrong answer
that the entire system confirms.

**Simple example.** The fold classifies outcomes with:

```java
if (outcome.endsWith("EXECUTED")) paid += amount;
```

`PLATFORM_VERIFIED_NOT_EXECUTED` ends with `EXECUTED`. A payment the
provider never executed now folds as paid. The UI shows paid; the
drift scanner runs the same fold — green; the daily totals job runs
the same fold — matches. Every internal check agrees, because
agreement is what the canonical-fold rule is *for*.

**Why it can't be resolved from inside.** More careful fold code and
more golden vectors reduce probability, never restore the property.
Independent verification requires an oracle **outside** the fold:
provider-side reconciliation (compare our fold's answer to the
provider's records), or a second, independently-written fold
implementation maintained forever (expensive, and itself a discipline).
The baseline stores the money as constrained rows, so its scanner
compares two *independent* representations — that redundancy is the
thing this model structurally removes.

---

## Problem 3 — Every fix is retroactive

**(from limit L2 · good code does not help: retroactivity is a property of derivation itself, not of any defect)**

When the Problem-2 bug is found and the fold is corrected, the fix does
not apply "from now on." Deploying fold v7 silently changes what every
already-committed history **means** — including payments that were
settled, reported, and communicated under fold v6's reading.

**Simple example.** Under fold v6, payment P-123 read as PAID; the
month was closed and the customer notified. Fold v7 fixes the
classification bug; P-123 now reads UNPAID. Both answers were computed
from the same committed rows. The model has no way to say "PAID was
true until Tuesday" — it says v7's answer was *always* the truth, and
the books you closed were always wrong.

**Why it can't be resolved from inside.** There is no "fix forward
only" option. Pinning old histories to old fold versions means storing
the old answers — which is materialized state, i.e., leaving the model.
Careful code changes nothing here: any derived ledger reinterprets its
past whenever the derivation changes. In the baseline, a bug fix
corrects future writes; recorded rows stay what they were, and
correcting them is an explicit, auditable act. Here, reinterpretation
is silent and total. Adopting the model means accepting that the
meaning of settled history is coupled to the current fold version.

---

## Problem 4 — The table is the design's only memory of identities

**(from limit L6 · good code cannot help against the CURRENT schema — but a design addition can: ranked below 1–3 for exactly that reason)**

Provider-facing identity (the idempotency key) is derived
deterministically and its uniqueness is enforced by `IDEM_CLAIM` — a
generated column **of the event table itself**. The design's memory of
"this key was already used" is the very table that a point-in-time
restore truncates. After a restore, the erased events are gone, and no
service code — however careful — can consult a record that no longer
exists.

**Simple example.** Payment P-9 sends key K to the provider; the
provider executes. Disaster strikes; the database is restored to ten
minutes earlier — the rows recording K are erased. The flow re-runs,
but this time an amendment arrives first, so it derives the *same slot*
with **different content**, mints K again as if fresh, and sends it.
The provider deduplicates on K and returns the *old* execution. The
system records success — for a payment whose content is not what was
executed.

**Why the current design can't resolve it — and what would.** This is
not solvable by code discipline; the code has nothing to read. It IS
solvable by redesign: an **epoch component** in the identity derivation
(bumped on every restore, so post-restore keys can never collide with
erased ones) plus a **burned-key reconciliation** that rebuilds the
used-key set from provider records before traffic resumes. That
procedure is currently undesigned, and until it is written, restore is
a money-safety event, not an infrastructure event.

---

## Problem 5 — A load-bearing history cannot forget

**(from limit L9 · good code cannot help: the conflict is between the model's core rule and an external legal requirement — resolvable only by designing PII out of the events, BEFORE the first production event)**

The model's foundation is that committed history is immutable and
load-bearing: the fold's correctness depends on its input never
changing. Compliance deletion (data-subject erasure, PII retention
limits) demands the opposite — that specific recorded facts be removed
on request. The design cannot satisfy both: redact an event and the
fold's input has changed (self-consistency broken); refuse and the
legal requirement is broken. No service-code discipline resolves a
contradiction between the schema's core rule and the law.

**Simple example.** An enrichment event carries beneficiary details:

| V | EVENT_TYPE | DETAIL |
|---|---|---|
| 3 | `REQUEST_ENRICHED` | `{"beneficiary_iban":"DE89 3704 0044 0532 0130 00", ...}` |

Seven years later a deletion obligation applies to that IBAN. `UPDATE`
on the event table is revoked by design; rewriting the row silently
falsifies the very history every reader trusts.

**Why it's ranked last.** The resolution is well-understood and
additive: PII never enters an event — events carry opaque references
into an out-of-band vault (or per-subject encryption whose key can be
destroyed: crypto-shredding). But it must be designed **before the
first production event exists**, because retrofitting means rewriting
committed history — the one thing this model forbids. It is a design
prerequisite the current documents have not written.

---

## What was filtered OUT, and why

For honesty, the candidates that did not make the list — each fails
the filter because enforced good code (or bounded additive work)
genuinely resolves it:

| Candidate | Why excluded |
|---|---|
| Stale projection reads (L5) | Resolved by the never-load-bearing rule + same-tx update + monitored sweep — enforceable code discipline with a written contract |
| UETR multiplicity / duplicate deliveries (L7) | Resolved by the specified fail-closed rule (0→unmatched, 1→fold+append, 2+→CRITICAL) + the inbox — code against a written spec |
| Request-granular UI parity (L8) | A second, additive projection — bounded work |
| Unstructured event classification (L4) | Already fixed (`EVENT_CODE` + shape CHECKs) |
| Contradictory-evidence handling (L3) | Already resolved by design (first-class contradiction events, safe-stop fold) |

## Bottom line

Problems 1–3 are the model's identity: **enforcement, verification,
and meaning all live in code** — the append library and the fold — and
no quality of that code turns them back into properties the database
guarantees. Problems 4–5 are absences, not impossibilities: each has a
known design shape (epoch + burned-key reconciliation; PII vault) that
must be built before this model could carry production money. A team
adopting Alternative B should read 1–3 as the price of admission and
4–5 as mandatory pre-production design work.
