# Five Issues That Remain After Assuming Correct Service Code

> **Status: DRAFT — companion to `03-known-limits.md`.**
> `03` is the full cost sheet (every known limitation, ranked by
> remediation shape). THIS document answers a narrower question: **of
> everything in 03, which issues survive the assumption that the team
> writes and enforces good service code — and what KIND of issue is
> each one?**
>
> The filter applied to every candidate: *assume excellent, enforced
> service code. Does the problem go away?* If yes, the issue is demoted
> or excluded — a design should not be blamed for work ordinary
> engineering discipline resolves. The five survivors are NOT all the
> same kind of problem, and this document says which is which:
>
> | # | Issue | Classification |
> |---|---|---|
> | 1 | No declarative temporal-legality backstop | **Inherent trade-off** — accept or reject |
> | 2 | No local independent witness for the money | **Inherent assurance cost** — externally mitigable, never locally |
> | 3 | Fold changes reinterpret history by default | **Open design requirement** — versioning + correction governance |
> | 4 | Restore erases the design's only memory of identities | **Adoption blocker** — recovery design must exist first |
> | 5 | Load-bearing history vs data-erasure obligations | **Pre-production prerequisite** — conditional on data classification |
>
> Issues 1–2 are structural: no quality of service code converts them
> back into database-guaranteed properties (though 2 has real external
> mitigations). Issue 3 is solvable with known event-model techniques
> that are currently undesigned. Issues 4–5 are absences of required
> design work. **None of this document is proof that the five disqualify
> the model** — it is the honest statement of what adopting it commits
> the team to accepting (1–2) and building (3–5).
>
> **All five are runnable:** `tl-proposal-proof`'s
> `EventModelLimitsTest` reproduces each issue against the event-table
> model's executable stand-in (and, for issue 1, shows the same wrong
> decision dying on the baseline's I6 emulation). Every assertion there
> asserts the damage HAPPENS — `.\mvnw.cmd test` to see it.

---

## Issue 1 — No declarative temporal-legality backstop

**(Inherent trade-off · from limit L1)**

The database enforces slot uniqueness (the fence), write-once identity,
valid types, and per-row shape. It cannot declaratively express
cross-row temporal rules — "no new `REQUEST_OPENED` while an earlier
request lacks a terminal event." That legality lives in one place: the
canonical append validator. Code.

Good service code lowers the probability of an illegal append; this
issue is about the day that code is wrong — and code cannot backstop
its own defects.

**Simple example — with every mechanism working perfectly.** Note first
what the fence DOES catch: a writer folding a stale prefix (through v3)
attempts slot 4, collides with the existing row, and is forced to
re-fold. Staleness alone cannot corrupt the stream. So the failure
needs a current writer with a wrong decision — next year's
"nudge stuck payments" endpoint re-folds correctly, sees everything,
and its legality check contains the defect: it treats `AMBIGUOUS` as
terminal-negative.

| V | EVENT_TYPE | EVENT_CODE | note |
|---|---|---|---|
| 4 | `POST_RESULT_RECORDED` | `AMBIGUOUS` | truth: outcome unknown — no new request may open |
| 5 | `REQUEST_OPENED` | · | writer was CURRENT, fence won fairly, shape ✓ — **COMMITS** |

Two requests are now open; if `#2` also executed, the payment doubles.
The identical wrong decision against the baseline dies at commit with
`ORA-00001` on the I6 unique index — zero rows, loud, no cleanup —
because the constraint checks the OUTCOME of the decision, not the
process that made it.

**What would and would not help.** Routing every append through a
stored procedure that independently re-validates is a genuine second
implementation (two codebases must share a defect to lose) — an
*implementation-level* backstop, bought with a duplicated state
machine, permanent cross-language maintenance, divergence risk, and
folding on the hot write path. What no option provides is the
baseline's property: a **declarative** constraint the write path cannot
forget, mis-implement, or skip. That property is the trade-off; adopt
means accept.

---

## Issue 2 — No local independent witness for the money

**(Inherent assurance cost · from limit L2)**

The amount paid is never stored; it is the output of the canonical
fold. The model's own safety rule — ONE shared, golden-vector-tested
fold, never re-implemented per reader — deliberately makes every
monitor, scanner, and dashboard the SAME witness. A fold defect
produces a wrong answer the entire system confirms, because agreement
is what the rule is for.

**Simple example.** The fold classifies with
`outcome.endsWith("EXECUTED")`; `PLATFORM_VERIFIED_NOT_EXECUTED` ends
with `EXECUTED`. A payment the provider never executed folds as paid.
UI: paid. Drift scanner (same fold): green. Daily totals (same fold):
matches. The beneficiary is silently unpaid.

**Stated precisely — including what the baseline does NOT give.** The
baseline stores request outcomes and obligation counters as
*mechanically redundant persisted representations*. They are usually
written by the same code in the same transaction — so a
classification bug that corrupts both consistently ALSO passes I1/I2.
What the redundancy reliably catches is choreography defects: a
request updated without its counter, a counter bumped twice, one code
path forgetting the obligation. The event model removes even that
local, mechanical redundancy.

**The narrowed claim:** one canonical fold cannot verify itself.
Independent assurance remains possible — a separately implemented
verification fold (a real second witness, at permanent
dual-maintenance cost) or reconciliation against the provider's ledger
(external, slow, authoritative). The cost is inherent; the exposure is
mitigable from outside.

---

## Issue 3 — Fold changes reinterpret history by default

**(Open design requirement · from limit L2 · previously overstated as unresolvable — corrected)**

Deploying a changed fold changes what every committed history MEANS,
at once, backdated. Payment P-123 read PAID under fold v6; v7 fixes a
classification bug; P-123 now reads UNPAID — after the books closed
and the customer was notified. Absent any countermeasure, this
reinterpretation is **silent**: nothing in the database records that
the answer changed, or why.

**What actually resolves it (known event-model techniques, none of
them designed here yet):**

1. **Correction events** — the bug-fix path. Instead of letting a
   deploy silently flip P-123, the governed sequence is: replay
   history under the candidate fold, diff the answers, and for every
   payment whose answer changes append an explicit correction event
   (e.g. `OPS_VERIFIED_OUTCOME_APPLIED`, or a dedicated
   interpretation-correction type). History then truthfully records:
   *classified PAID until date X; reconciliation proved NOT_EXECUTED;
   corrected by an audited event.* Reinterpretation becomes an
   appended fact, not a deploy side-effect.
2. **Semantic versioning of interpretation** — the evolution path.
   Events carry a semantic version; the fold retains interpreters for
   prior versions, so old events keep the meaning they had when
   written. Note the honest boundary: this is for DELIBERATE semantic
   changes — for a defect, pinning old events to the old interpreter
   preserves the *wrong* answer, which is why path 1 exists.
3. **Epoch pinning per stream** — a stream stays on its interpretation
   epoch until an explicit, validated migration marker moves it.

**The fair two-sided comparison:** the baseline's persisted state is
stable under code deploys — and therefore its ERRORS are also
persisted, staying wrong until an explicit, audited backfill fixes
them. The event model can recompute correct state — and therefore
needs the machinery above to keep recomputation explicit and governed.
Neither gets correctness for free; they pay in different currencies.

**Status:** a substantial, mandatory design requirement (replay
governance, correction vocabulary, versioning rules) with known
solutions — not an in-principle impossibility, and this document
previously overstated it as one.

---

## Issue 4 — Restore erases the design's only memory of identities

**(Adoption blocker · from limit L6 · the clearest concrete blocker)**

Provider-facing identity is derived from the stream and its uniqueness
is enforced by `IDEM_CLAIM` — a generated column OF the event table. A
point-in-time restore erases the very rows that remember which keys
were burned. The provider does not roll back with the database. No
service code can consult rows that no longer exist.

**Simple example.** `REQUEST_OPENED` at v6 derives key K; provider
executes K; restore to the v4 backup erases v5–v7 and the claim on K.
The re-run takes a different interleave (an amendment lands first),
derives the SAME slot with DIFFERENT content, mints K as if fresh, and
sends. The provider dedups on K and returns the OLD execution — the
system records success for content that is not what was executed.

**The required design — and its honest fine print:**

- An **epoch alone is not sufficient.** If K executed before the
  restore, minting a fresh K2 afterward and immediately re-posting
  pays TWICE. The epoch prevents identity *collision*; it does nothing
  about identity *amnesia*.
- The safe sequence is a procedure, not a column: detect restore →
  **hold all posting** → enumerate potentially-burned identities
  (opening slots at/after the restore point) → query and reconcile
  per key with the provider → reconstruct or correct local history →
  bump the epoch **for genuinely new work only** → sign off per
  affected stream → resume.
- The epoch must survive the restore, i.e. live OUTSIDE the restored
  database — which concedes that **the event stream is no longer the
  sole authoritative input to identity generation.** That may be
  acceptable; it must be stated, not discovered.

Until this procedure is written and tested, a restore is a
money-safety event, not an infrastructure event — which is why this
issue, not 1 or 2, is the clearest adoption blocker.

---

## Issue 5 — A load-bearing history cannot forget

**(Pre-production prerequisite · from limit L9 · conditional, not yet a demonstrated defect)**

The model's foundation is that committed history is immutable and
load-bearing. Data-erasure obligations demand the opposite. IF
erasable personal data enters a load-bearing event, the conflict is
real: redact and the fold's input changes; refuse and the obligation
is breached.

**Simple example (a real row shape from this schema — the risk vector
is free text and identifiers, not a dedicated PII event):**

| V | EVENT_TYPE | ACTOR | DETAIL |
|---|---|---|---|
| 12 | `OPS_ANNOTATED` | OPS:tom | "beneficiary called, new IBAN DE89 3704 0044 0532 0130 00…" |

**Why "conditional":** whether this ever becomes a live defect depends
on questions no design document can answer alone — which jurisdictions
apply; which fields (UETRs? provider references? the payment key's
linkability) legally count as personal data; which records are under
MANDATORY retention (payment records commonly are, and erasure rights
are not absolute); how legal holds and backups are treated. Without
that data classification, no one can assert that any specific event
must ever be deleted.

**Why it is still a prerequisite, and what is model-specific:** every
design can leak PII into rows, journals, and logs — this obligation is
not unique to the event model. What IS specific: if PII lands in a
load-bearing event, deletion changes replay input — the model makes a
data-classification mistake uniquely hard to repair, and retrofitting
means rewriting history, the one thing it forbids. So the direction is
cheap and must precede the first production event: no erasable PII in
events, ever — sensitive free text banned from `DETAIL` (same scanner
rule as the baseline's display fields), PII in a separate erasable
vault referenced by opaque keys, per-subject encryption
(crypto-shredding) where the vault itself must forget, and fold
correctness never depending on any of it.

---

## What was filtered OUT — and what the review returned to open

Candidates excluded because enforced good code or bounded additive
work genuinely resolves them:

| Candidate | Why excluded |
|---|---|
| Stale projection reads (L5) | Never-load-bearing rule + same-tx update + monitored sweep — enforceable code discipline with a written contract (test set pending) |
| Request-granular UI parity (L8) | A second, additive projection — bounded work |

Items an earlier draft of this document closed too early, now honestly
back on the open list (details in `03`):

| Item | Actual status |
|---|---|
| Structured event codes (L4) | Partially fixed: the Oracle NULL/UNKNOWN gap in the shape CHECKs is closed in `01` (`EVENT_CODE IS NOT NULL` added), but the full 17-type EVENT_CODE matrix is unwritten |
| Delivery dedup (L7) | Inbox structure exists, but its transaction boundary vs multi-payment fan-out is unspecified — "seen" must never mean "fully processed" |
| Contradictory evidence (L3) | Contained (record + park + CRITICAL), but the resolution/unpark flow after the park is undefined |

## Bottom line

Issues 1 and 2 are the model's identity — enforcement and verification
live in code, and adopting the model means accepting that as a
permanent property, with issue 2's exposure mitigable only from
outside (second implementation, provider ledger). Issue 3 has known
event-model answers that must be designed, not assumed. Issue 4 is the
single clearest adoption blocker: no restore procedure, no production
money. Issue 5 is a prerequisite whose scope depends on data
classification that has not been done. **This is decision material,
not a disqualification proof** — a team choosing this model signs up
to accept 1–2 and to build 3–5 before the first production event.
