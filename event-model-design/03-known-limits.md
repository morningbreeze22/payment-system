# Known Limits of the Event Model — with Simulated Rows

> Status: DRAFT (see `00-README.md`). Consolidates the adversarial
> self-review and the external assessment of it. Every limitation below
> carries a REAL-WORLD EXAMPLE with fully simulated database rows, so the
> discussion is about concrete data, never abstractions.
>
> **The boundary sentence** (adopted verbatim from the assessment):
> *a version fence can replace the obligation lock's serialization
> function, but it cannot replace the baseline schema's enforcement of
> temporal business legality.*
>
> **Greenfield note:** this system is NOT live — there is no legacy
> population, no dual-run, no cutover history. The migration objection
> that dominated earlier drafts of this critique is therefore MOOT and
> does not appear below (and, symmetrically, a slice of the baseline's
> own migration machinery is insurance the greenfield fact makes cheap).
> Everything that remains is real regardless of greenfield.

Row notation: `PAYMENT_EVENT` columns as in `01-event-table-schema.md`
(`·` = NULL; `EVENT_CODE` is the structured classification column added
after limitation L4 was found; `IDEM_CLAIM` is the generated column).

---

## L1 — The schema cannot refuse a semantically illegal append

**Statement.** The fence (`UNIQUE(payment_key, version)`) proves the slot
was won; `IDEM_CLAIM` proves identity is write-once. NOTHING in the
schema proves the event was LEGAL given the prior events — "no second
open while one is ambiguous", the L-shape rules, and "no release without
evidence" are temporal, cross-row predicates an append-only table cannot
express as constraints.

**Real-world example.** Friday incident. A support engineer (or a buggy
new endpoint) decides payment `T7031-1` is "stuck" and helpfully opens a
fresh request by direct SQL, while the real request is sitting in MAYBE:

| PK | V | EVENT_TYPE | EVENT_CODE | ORD | AMOUNT | IDEM_KEY | HASH | SRC | ACTOR | IDEM_CLAIM | DETAIL |
|---|---|---|---|---|---|---|---|---|---|---|---|
| T7031-1 | 1 | REQUIRED_AMOUNT_SET | · | 1 | 100 | · | · | UPSTREAM | ADMISSION | · | · |
| T7031-1 | 2 | REQUEST_OPENED | · | · | 100 | T7031-1#2 | H1 | SYSTEM | SYSTEM | T7031-1#2 | · |
| T7031-1 | 3 | POST_STARTED | · | · | · | T7031-1#2 | H1 | SYSTEM | WORKER | · | · |
| T7031-1 | 4 | POST_RESULT_RECORDED | AMBIGUOUS | · | · | T7031-1#2 | · | SYNC_RESPONSE | WORKER | · | timeout — outcome unknown |
| **T7031-1** | **5** | **REQUEST_OPENED** | · | · | **100** | **T7031-1#5** | H1 | SYSTEM | **OPS:raw-sql** | **T7031-1#5** | "unstuck it" |

Row v5 satisfies EVERY constraint: slot 5 was free (fence ✓), key
`T7031-1#5` is fresh (`IDEM_CLAIM` unique ✓), the type is in the CHECK
list ✓. **The insert succeeds.** There are now two open requests; the
next scanner folds, sees the LAST open request, posts `T7031-1#5` for
100 — and if the ambiguous `#2` also executed, the payment is doubled.

The baseline's answer to the identical row is one line long:

```
ORA-00001: unique constraint (PAY.REQUEST_ACTIVE_I6) violated
```

**Severity: the decisive structural difference.** Mitigations exist
(sole append API via revoked grants + stored procedure, prefix
validation in the append library, poison-stream on illegal history) —
but none reproduces "an arbitrary writer cannot create this row."

---

## L2 — A fold bug is self-consistent, invisible, and retroactive

**Statement.** There is one authoritative representation. Every reader,
scanner, and checker folds the same rows with the same shared fold — a
bug in it produces a WRONG number that every component agrees on.
(Honest moderation from the assessment: the baseline's counters and
request rows are usually written by the same transaction, so its I1/I2
redundancy is also not absolute — but it does catch forgotten/double
counter updates and choreography errors; the event model has no local
equivalent.)

**Real-world example.** Version 1.4.0 of the fold classifies executed
outcomes with a substring shortcut:

```java
if (outcome.endsWith("EXECUTED")) paid += amount;   // the bug
```

Payment `T7031-4`: ops used the dual-control operation to verify the
POST **never executed**:

| PK | V | EVENT_TYPE | EVENT_CODE | AMOUNT | IDEM_KEY | SRC | ACTOR | DETAIL |
|---|---|---|---|---|---|---|---|---|
| T7031-4 | 1 | REQUIRED_AMOUNT_SET | · | 80 | · | UPSTREAM | ADMISSION | · |
| T7031-4 | 2 | REQUEST_OPENED | · | 80 | T7031-4#2 | SYSTEM | SYSTEM | · |
| T7031-4 | 3 | POST_STARTED | · | · | T7031-4#2 | SYSTEM | WORKER | · |
| T7031-4 | 4 | POST_RESULT_RECORDED | AMBIGUOUS | · | T7031-4#2 | SYNC_RESPONSE | WORKER | · |
| T7031-4 | 5 | OPS_VERIFIED_OUTCOME_APPLIED | NOT_EXECUTED | · | T7031-4#2 | OPS | OPS:elena | approval APPR-1043 |
| T7031-4 | 6 | OUTCOME_RECORDED | **PLATFORM_VERIFIED_NOT_EXECUTED** | · | T7031-4#2 | OPS | OPS:elena | · |

`"PLATFORM_VERIFIED_NOT_EXECUTED".endsWith("EXECUTED")` is **true**.
The fold books 80 as paid. The UI shows paid. The scanner sees no
shortfall and never opens a successor — **the beneficiary is silently
unpaid** (the U-1 class). The drift check folds the same rows with the
same bug: green. The projection-vs-fresh-fold comparison compares the
bug to itself: green. Only provider-side reconciliation (external,
weeks-later) can catch it. And when the fold is fixed, the meaning of
EVERY historical stream containing that code changes retroactively.

In the baseline the same bug must independently corrupt BOTH the request
row and the `confirmed_amount` counter in matching ways, or I1/I2 pages
someone.

**Severity: high.** Mitigations (independent verification fold, golden
histories, full-history replay before fold deploys) are payable but
permanent, and they are exactly the N-version burden the one-fold rule
tries to avoid.

---

## L3 — Contradictory evidence must be modeled, because appends cannot be refused

**Statement.** The baseline's guarded CAS makes a late contradicting
write hit 0 rows — the contradiction never enters authoritative state.
Here every append succeeds, so the contradiction WILL be recorded; the
design must make it a first-class, safe-stop event — never an ordinary
event resolved by hidden precedence rules in the fold.

**Real-world example.** The engine definitively rejected — then, three
days later, its feed emits a settlement for the same key (engine-side
defect, or a mis-keyed manual correction on their side):

| PK | V | EVENT_TYPE | EVENT_CODE | AMOUNT | IDEM_KEY | UETR | SRC | ACTOR |
|---|---|---|---|---|---|---|---|---|
| T7031-7 | 4 | POST_RESULT_RECORDED | DEFINITIVE_REJECT | · | T7031-7#2 | · | SYNC_RESPONSE | WORKER |
| T7031-7 | 5 | OUTCOME_RECORDED | REJECTED_PROVIDER | · | T7031-7#2 | · | SYNC_RESPONSE | WORKER |
| T7031-7 | 6 | REQUEST_OPENED | · | 100 | T7031-7#6 | · | SYSTEM | SYSTEM |

*(the successor #6 is already in flight — the reservation was legally
released at v5)*. Now the late feed arrives. The WRONG design appends:

| T7031-7 | 7 | SETTLED | · | 100 | T7031-7#2 | U-79f3 | FEED | FEED |

…and the fold must now secretly decide whether `SETTLED` beats
`REJECTED_PROVIDER`. If it books it: 100 paid on `#2` **plus** the
in-flight `#6` = double payment brewing. The RIGHT design appends:

| T7031-7 | 7 | EVIDENCE_CONTRADICTION_RECORDED | SETTLED_AFTER_TERMINAL | 100 | T7031-7#2 | U-79f3 | FEED | FEED |

with a FIXED fold effect: book nothing, PARK the payment (which also
freezes `#6` from posting), raise CRITICAL, require authoritative
reconciliation. Adopted into the vocabulary in `01`.

**Severity: medium — designable, now designed;** the residual burden is
that the safe-stop rule lives in the fold, not the schema (see L1).

---

## L4 — The original schema could not even ENCODE the fold's inputs

**Statement.** The fold branches on business classifications (`AMBIGUOUS`
vs `BUSINESS_REJECT` vs `DEFINITIVE_REJECT`; `EXECUTED` vs `NOT_FOUND` vs
`LOOKBACK_EXPIRED`; transient vs definitive enrichment). The first draft
gave them no column: `PROVIDER_CODE` carries the ENGINE'S raw code, and
`DETAIL` is fold-forbidden by design rule. Found by the external
assessment; fixed in `01` (structured `EVENT_CODE` + per-type shape
CHECKs) — recorded here because it shows how easily an event schema
under-specifies.

**Real-world example.** Two rows under the ORIGINAL schema (no
EVENT_CODE). The engine's raw codes are opaque vendor strings:

| PK | V | EVENT_TYPE | AMOUNT | IDEM_KEY | PROV_CODE | SRC | ACTOR |
|---|---|---|---|---|---|---|---|
| T7031-2 | 5 | POST_RESULT_RECORDED | · | T7031-2#2 | E-4021 | SYNC_RESPONSE | WORKER |
| T7031-3 | 5 | POST_RESULT_RECORDED | · | T7031-3#2 | E-7740 | SYNC_RESPONSE | WORKER |

`E-4021` means "insufficient funds" (business reject → RETRY the same
key, money stays reserved). `E-7740` means "compliance block"
(definitive → RELEASE the reservation). **Opposite money consequences —
and no column records which is which.** The classification (the CA-1
mapping decision, made at response time) existed only in the code path
that handled the response. A future fold cannot re-derive it: re-mapping
raw codes later is exactly the load-bearing-replay hazard the baseline
rejects. Fixed row:

| T7031-2 | 5 | POST_RESULT_RECORDED | **EVENT_CODE=BUSINESS_REJECT** | · | T7031-2#2 | E-4021 | SYNC_RESPONSE | WORKER |

**Severity: was a blocker; now fixed in the draft.** The lesson stands:
every fold input must be a structured, CHECK-bound column.

---

## L5 — A stale projection can strand work forever (false negatives)

**Statement.** "Never load-bearing" was proven only in one direction. A
stale FALSE POSITIVE costs a wasted fold. A stale FALSE NEGATIVE means no
scanner ever selects the payment — the projection silently decides that
work does not exist.

**Real-world example.** The worker commits the ambiguous result, then the
process dies BEFORE the post-commit projection update:

`PAYMENT_EVENT` (truth):

| PK | V | EVENT_TYPE | EVENT_CODE | IDEM_KEY | AT |
|---|---|---|---|---|---|
| T7031-5 | 3 | POST_STARTED | · | T7031-5#2 | T+15s |
| T7031-5 | 4 | POST_RESULT_RECORDED | AMBIGUOUS | T7031-5#2 | T+45s |

`PAYMENT_STATUS_PROJECTION` (stale — reflects v3, crash before update):

| PAYMENT_KEY | FOLD_VERSION | PHASE | NEXT_ACTION_AT | UETR |
|---|---|---|---|---|
| T7031-5 | **3** | **POSTING** | **·** | · |

The resolver scanner runs `WHERE PHASE='MAYBE' AND NEXT_ACTION_AT <= :now`
→ **0 rows, today and every day**. 100 stays reserved, nobody asks the
provider, no escalation fires (escalation is found the same way). The
truth is in the event table; nothing ever looks at it.

**Fix direction (adopted in `01`):** at this volume, update the
projection IN the append transaction (it stays rebuildable, it stops
being skippable); independently, a timed full-sweep re-fold job whose
own liveness is monitored — the projection then has a bounded staleness
contract instead of a hope.

---

## L6 — Database restore can silently REUSE version-derived identities

**Statement.** Identity = the opening slot's version. A restore rewinds
versions; provider executions do not rewind. Because NON-request events
also consume slots, post-restore traffic re-deals the slots differently —
and the schema's `IDEM_CLAIM` uniqueness is no protection, because the
restored table FORGOT the old claim. (The first draft said the baseline
§5.2 discussion "applies unchanged" — that was wrong; this is at least as
hard as the baseline's sequence-divergence problem and needs its own
design.)

**Real-world example.** Before the incident (backup was taken after v4):

| PK | V | EVENT_TYPE | EVENT_CODE | AMOUNT | IDEM_KEY | IDEM_CLAIM | AT |
|---|---|---|---|---|---|---|---|
| T7031-6 | 5 | REQUIRED_AMOUNT_SET | · | 150 | · | · | T+2h |
| T7031-6 | 6 | REQUEST_OPENED | · | **150** | **T7031-6#6** | T7031-6#6 | T+2h |
| T7031-6 | 7 | POST_STARTED | · | · | T7031-6#6 | · | T+2h |

Provider's ledger (does not rewind): `T7031-6#6 → EXECUTED 150`.

Storage failure; DB restored to the v4 backup — rows v5–v7 are GONE, and
so is the `IDEM_CLAIM` row for `T7031-6#6`. Redelivery now replays a
DIFFERENT interleave (a small amendment event arrives first this time):

| PK | V | EVENT_TYPE | EVENT_CODE | AMOUNT | IDEM_KEY | IDEM_CLAIM | AT |
|---|---|---|---|---|---|---|---|
| T7031-6 | 5 | REQUIRED_AMOUNT_SET | · | **130** | · | · | T+9h |
| T7031-6 | 6 | REQUEST_OPENED | · | **130** | **T7031-6#6** | T7031-6#6 ✓accepted | T+9h |
| T7031-6 | 7 | POST_STARTED | · | · | T7031-6#6 | · | T+9h |

Same key `T7031-6#6`, DIFFERENT amount, and the schema accepted it —
uniqueness cannot remember rows the restore erased. The wire call is now
a same-key/different-payload collision at best (if the engine is
contract-compliant: a distinguishable reject and a confused operator) or
a silent dedup at worst (the app believes 130 went out; 150 actually
moved).

**Severity: money-safety blocker to DESIGN before adoption** (greenfield
does not help — restores happen to new systems too). Direction: a
restore-recovery procedure that treats every key with an opening slot ≥
the restore point as POTENTIALLY BURNED — enumerate, query the provider
per key, reconcile, and hold all posting for affected streams until the
interval is signed off; plus an epoch/generation component in the
identity so post-restore slots can never collide with pre-restore ones.

---

## L7 — UETR matching and inbound-event identity are underspecified

**Statement.** A non-unique `UETR` index answers "which streams mention
this UETR" — not "which single stream owns it." And the three-structure
draft silently dropped the baseline's inbound-event inbox, so duplicate
feed deliveries have no identity to dedup on.

**Real-world example (multi-match).** A manual repair on the engine side
recycled a UETR across two of our payments:

| PK | V | EVENT_TYPE | EVENT_CODE | IDEM_KEY | UETR |
|---|---|---|---|---|---|
| T7031-8 | 4 | POST_RESULT_RECORDED | ACCEPTED | T7031-8#2 | **U-88aa** |
| T9044-1 | 4 | POST_RESULT_RECORDED | ACCEPTED | T9044-1#2 | **U-88aa** |

Feed event arrives: `SETTLED, UETR=U-88aa, amount 100`. The lookup
returns **2 payment keys**. Without a written rule, whichever the code
picks books 100 on possibly the wrong payment. Rule adopted into `01`:

```
0 matches  -> unmatched path (ack; recover by key later)
1 match    -> fold + fenced append
2+ matches -> CRITICAL anomaly, NO state change on any stream
```

**Real-world example (duplicate delivery).** The feed redelivers message
`MSG-778` after a consumer rebalance. Without an inbox, the stream gets:

| PK | V | EVENT_TYPE | AMOUNT | IDEM_KEY | SRC | DETAIL |
|---|---|---|---|---|---|---|
| T7031-8 | 5 | SETTLED | 100 | T7031-8#2 | FEED | msg MSG-778 |
| T7031-8 | 6 | SETTLED | 100 | T7031-8#2 | FEED | msg MSG-778 (redelivery) |

The idempotent fold saves the MONEY (distinct keys count once) — but the
duplicate row re-fires whatever side effects ride the append (metrics,
notifications, incident counters) and pollutes history. Fixed by
restoring the inbox as the 4th structure (`INBOUND_EVENT_INBOX`,
`UNIQUE(source, event_id)` — dedup BEFORE folding), adopted in `01`.

**Severity: medium; now specified — was a genuine omission.**

---

## L8 — The one-row-per-payment projection cannot serve the §12 UI contract

**Statement.** The UI requirement is request-granular: one row per
LOGICAL REQUEST (the "100 + 20" history), an obligation-only placeholder
when no request exists, keyset pagination over the whole estate, and the
creation-time amount series. A per-payment status row cannot produce it.

**Real-world example.** What §12 requires the listing to show for one
payment (after a 100 executed and a 20 in flight):

| row | request | amount | state | shown because |
|---|---|---|---|---|
| 1 | T7031-9#2 | 100 | EXECUTED | historical request row |
| 2 | T7031-9#6 | 20 | AWAITING SETTLEMENT | active request row |
| — | (obligation-only placeholder appears instead when a payment has required > 0 and NO request yet) | | | |

What the projection has:

| PAYMENT_KEY | FOLD_VERSION | PHASE | NEXT_ACTION_AT |
|---|---|---|---|
| T7031-9 | 7 | AWAITING SETTLEMENT | · |

One row, no request granularity, no amount series, nothing to paginate
by `(payment, row_type, request, …)`. The information EXISTS in the
stream — the gap is a read-contract decision: either fold-and-expand per
page (estate-scale cost) or maintain a REQUEST-GRANULAR UI projection
(a second, bigger projection with its own freshness contract — see L5).
Either way, §12 parity is unproven until designed.

**Severity: medium — parity work, not impossibility.**

---

## L9 — Retention, growth, and compliance deletion collide with INSERT-only

**Statement.** The stream keeps every retry, query answer, ops action,
and defect forever; the INSERT-only grant + guard trigger — the model's
integrity anchor — is also an anti-deletion mechanism. Compliance
redaction and archival need explicit design, not defaults.

**Real-world example.** One long-lived payment: 90 days of retries at
4/day ≈ **1,080 rows for one payment** (each retry = POST_STARTED +
POST_RESULT). Harmless at 100k payments/month for years — until someone
writes THIS row:

| PK | V | EVENT_TYPE | EVENT_CODE | SRC | ACTOR | DETAIL |
|---|---|---|---|---|---|---|
| T7031-9 | 12 | OPS_ANNOTATED | · | OPS | OPS:tom | "beneficiary called from +81-90-…, new IBAN DE89 3704 0044 0532 0130 00, waiting for compliance" |

Personal data is now in an append-only table whose own trigger raises on
UPDATE/DELETE, replicated into every backup. When a lawful deletion or
redaction request arrives, the integrity mechanism and the compliance
obligation point in opposite directions. Direction: forbid sensitive
content in `DETAIL` by policy + scanner (the baseline has the same rule
for its display fields), keep a separate mutable annotation store for
free text, and define partition-based archival with signed digests so
archived partitions stay tamper-evident without staying online.

**Severity: low-medium — policy work; must be decided before go-live,
not after.**

---

## Objectives-vs-mechanisms summary (from the assessment, greenfield-adjusted)

| Safety objective | Baseline mechanism | Event-model mechanism | Relative strength |
|---|---|---|---|
| Stop stale writers | obligation lock + CAS | expected-version fence | comparable — IF all writes use the append protocol |
| No duplicate active request | I6 unique index | fence + fold validation | **weaker against buggy/raw writers (L1)** |
| No identity reuse | locked counter + UNIQUE | opening-slot identity + IDEM_CLAIM | comparable normally; **restore requires redesign (L6)** |
| Resolve ambiguous POST | mutable state + resolver | POST_STARTED + query events | comparable; event form arguably clearer |
| Prove never-sent | claim fields + guards | absence of POST_STARTED | **event model clearer** |
| Detect local money drift | counters vs rows (I1/I2) | deterministic fold + external recon | **weaker: no local redundancy (L2)** |
| Contradictory evidence | CAS refuses (0 rows) | first-class contradiction events + safe stop | comparable once L3's vocabulary is used |
| No-request visibility & UI | obligation row / request rows | events exist; read contract undesigned | **unproven (L8)** |
| Find due work | indexed current state | projection | safe only with L5's freshness contract |

## Bottom line

Greenfield removes the migration objection entirely — it does NOT touch
L1, L2, or L6, which are the three that gate adoption:

- **L1** is the principled boundary (where money invariants are
  enforced) and cannot be closed without reintroducing an anchor or
  database-side temporal validation;
- **L2** is a permanent assurance burden accepted in exchange for the
  simpler write path;
- **L6** is an open money-safety design problem with no draft answer yet.

L3, L4, L5, L7 are now specified or fixed in `01`; L8 and L9 are bounded
design work. The comparison that matters is TOTAL complexity after
closing all of this, against a baseline that is already reviewed,
carded, and evidence-gated — that judgment belongs to the team, with
this file as the honest cost sheet.
