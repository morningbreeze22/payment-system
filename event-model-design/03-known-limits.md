# Known Limits of the Event Model — Ranked, with Simulated Rows

> Status: DRAFT (see `00-README.md`). Consolidates the adversarial
> self-review and the external assessment of it, reframed after PO
> review of the argumentation standard (see "How these are judged").
> Every limitation carries a real-world example with simulated rows.

## Severity rubric (fixed)

| Rank | Meaning |
|---|---|
| **CRITICAL** | The property is **actually not achievable** in a one-event-table design — no amount of work inside the model provides it; adopting the model means ACCEPTING its absence (or leaving the model) |
| **HIGH** | Achievable, but the current draft needs **significant redesign** to accommodate it |
| **MEDIUM** | Achievable by a **bounded additive change** — a column, a constraint, a small auxiliary structure or job — no redesign of the core |
| **LOW** | Policy/process work, or already resolved in the current draft |

## How these are judged (argumentation standard)

"Someone can run arbitrary SQL" is NOT a design argument — a privileged
actor can disable a trigger or drop a constraint in the baseline just as
easily as they can UPDATE this table; **no design survives privileged
misuse, and none of the rankings below rely on it.** The valid criterion
is *defect blast radius*: when the same ordinary defect arrives through
the FRONT DOOR — a new code path written next year, a service acting on
a stale read, an incident hotfix calling a helper directly, all using
the normal application role — does the system fail loudly at write time,
or accept the write and let state corrupt silently? That is the same
criterion that justifies `UNIQUE(idempotency_key)` existing at all,
rather than trusting that correct code never inserts duplicates.

| # | Limitation | Rank |
|---|---|---|
| L1 | Temporal business legality has no **declarative** write-time backstop | **CRITICAL** |
| L2 | Money state is fold-derived: bugs are self-consistent and fixes are **retroactive** | **CRITICAL** |
| L6 | Restore-time identity reuse | **HIGH** |
| L4 | Fold inputs need structured columns | MEDIUM — partially fixed (NULL gap closed; full matrix open) |
| L5 | Projection false negatives can strand work | MEDIUM — contract specified |
| L7 | UETR multiplicity + delivery dedup | MEDIUM — partially specified (inbox tx boundary open) |
| L8 | Request-granular UI (§12) parity | MEDIUM |
| L3 | Contradictory evidence | LOW — contained; resolution flow open |
| L9 | Retention / compliance for an append-only store | LOW |

---

## L1 — [CRITICAL] Temporal legality has no declarative write-time backstop

**The property that is not achievable:** in the baseline, "at most one
active request per obligation" (I6), the L-shape rules, and
"no release without evidence" are DECLARATIVE schema facts — a write
that violates them fails with an ORA error no matter which code path
produced it, including code that has never heard of the rules. Those
predicates are relationships BETWEEN rows OVER time; an append-only
single table cannot express any of them as a constraint. This is
structural, not a missing feature.

**Front-door example (no rogue actor, no raw SQL — and the fence
working PERFECTLY).** Next year someone ships a well-meaning "nudge
stuck payments" endpoint. The stale projection selects T7031-1 as a
candidate — legitimate; projections may select (L5). The endpoint then
follows the append protocol correctly: it re-folds the LIVE stream,
sees v4, and claims v5. Note what this rules out: a writer whose fold
were stale (through v3) would attempt slot 4, collide with the
existing row, and be forced to re-fold — **staleness alone cannot
produce this row; the fence catches it.** The writer that wins v5 is
provably CURRENT. The defect is in the one place the model keeps
legality: the validator treats `AMBIGUOUS` as terminal-negative
("no outcome recorded → nothing in flight → open a new request"):

| PK | V | EVENT_TYPE | EVENT_CODE | AMOUNT | IDEM_KEY | SRC | ACTOR | IDEM_CLAIM | DETAIL |
|---|---|---|---|---|---|---|---|---|---|
| T7031-1 | 2 | REQUEST_OPENED | · | 100 | T7031-1#2 | SYSTEM | SYSTEM | T7031-1#2 | standing rule |
| T7031-1 | 3 | POST_STARTED | · | · | T7031-1#2 | SYSTEM | WORKER | · | · |
| T7031-1 | 4 | POST_RESULT_RECORDED | AMBIGUOUS | · | T7031-1#2 | SYNC_RESPONSE | WORKER | · | outcome unknown |
| **T7031-1** | **5** | **REQUEST_OPENED** | · | **100** | **T7031-1#5** | SYSTEM | **SYSTEM (nudge endpoint)** | T7031-1#5 | validator defect: treated AMBIGUOUS as terminal |

Row v5 satisfies every constraint the model has: the writer was
current ✓, the slot was won fairly (fence ✓), the key is fresh
(IDEM_CLAIM ✓), the shape checks pass ✓. **The insert commits.** Two
requests are now open; if the ambiguous `#2` also executed, the
payment doubles. Every mechanism the model owns worked as designed —
that is the point. The identical buggy decision in the baseline is one
line:

```
ORA-00001: unique constraint (PAY.REQUEST_ACTIVE_I6) violated
```

Same bug, same probability of being written — the DESIGN decides
whether it costs zero rows (loud, at write time) or a corrupted history
(silent, discovered later or never).

**Sharpened claim (per assessment + PO review):** DB-side enforcement
is possible *procedurally* — revoke raw INSERT and route every append
through a stored procedure that re-folds the prefix and validates. An
INDEPENDENTLY written PL/SQL validator is genuinely a second
implementation, and it does buy implementation-level protection (two
codebases must share the same defect to lose). What it never becomes is
a *declarative* constraint — and it pays for the protection with a
duplicated state machine, permanent cross-language maintenance,
divergence risk against the canonical fold, and folding history on the
hot write path. What is NOT achievable is the baseline's property: a
declarative guarantee the write path cannot forget, mis-implement, or
skip.

**Adoption meaning:** accepting the model = accepting that temporal
money invariants are protected by discipline (append API + validation +
tests), not by the schema. This is the boundary sentence: *the fence
replaces the lock's serialization function, not the schema's
enforcement of temporal business legality.*

---

## L2 — [CRITICAL] Fold-derived money: self-consistent bugs, retroactive fixes

**The property that is not achievable:** money state whose meaning is
FIXED at write time and independently witnessed. In the fold model,
`paid` does not exist in the database — it is recomputed by code on
every read. Two consequences are inherent, independent of how carefully
the fold is written:

1. **Self-consistency:** every reader, scanner, and checker built on
   the fold agrees with its bugs — there is no second local
   representation to disagree (persisting one = a stored ledger = a
   second, mutable structure = leaving the model).
2. **Retroactivity:** fixing a fold bug silently changes what recorded
   history MEANS, for every payment, at once, backdated. A stored
   ledger's past does not change when code deploys.

**Example (front-door, ordinary bug).** Fold v1.4.0 classifies executed
outcomes with a substring shortcut:

```java
if (outcome.endsWith("EXECUTED")) paid += amount;   // the bug
```

| PK | V | EVENT_TYPE | EVENT_CODE | AMOUNT | IDEM_KEY | SRC | ACTOR |
|---|---|---|---|---|---|---|---|
| T7031-4 | 2 | REQUEST_OPENED | · | 80 | T7031-4#2 | SYSTEM | SYSTEM |
| T7031-4 | 4 | POST_RESULT_RECORDED | AMBIGUOUS | · | T7031-4#2 | SYNC_RESPONSE | WORKER |
| T7031-4 | 6 | OUTCOME_RECORDED | **PLATFORM_VERIFIED_NOT_EXECUTED** | · | T7031-4#2 | OPS | OPS:elena |

`"PLATFORM_VERIFIED_NOT_EXECUTED".endsWith("EXECUTED")` is **true** —
the fold books 80 as paid; the scanner sees no shortfall and never opens
a successor; the beneficiary is **silently unpaid** (the U-1 class). The
drift check folds the same rows with the same bug: green. The
projection-vs-fold comparison compares the bug to itself: green. In the
baseline, the same bug class must corrupt the request row AND the
`confirmed_amount` counter in matching ways or I1/I2 pages someone —
honest concession: that redundancy is not an oracle either (both are
usually written by the same transaction; common-mode bugs pass), but it
exists, is mechanical, and is local.

**Mitigations (real, but none restore the property LOCALLY):** a
deliberately independent verification fold (N-version — a genuine
second witness, at the cost of permanent dual-maintenance), frozen
golden histories, full replay before fold deploys, provider-side
reconciliation (external, slow). Retroactivity cannot be removed by
care — but it CAN be made explicit and governed: correction events
(reinterpretation recorded as an appended, audited fact), semantic
versioning of event interpretation, and replay-impact analysis before
any fold deploy. That machinery is a real design requirement and is
currently unwritten — see `04` P3. The DEFAULT, absent it, is silent
reinterpretation of all history on every fold change.

**Adoption meaning:** accepting the model = accepting that the ledger is
an opinion of the current code version, witnessed only by copies of
itself plus external reconciliation.

---

## L6 — [HIGH] Restore-time identity reuse

**Why HIGH, not CRITICAL:** closable inside the model — but not by a
column alone; it needs an identity-derivation change plus a designed
restore procedure. No bug and no human error is involved anywhere in
this scenario — a storage failure plus a routine restore produces it.

**Example.** Identity = the opening slot. Backup taken after v4; then:

| PK | V | EVENT_TYPE | EVENT_CODE | AMOUNT | IDEM_KEY | IDEM_CLAIM | AT |
|---|---|---|---|---|---|---|---|
| T7031-6 | 5 | REQUIRED_AMOUNT_SET | · | 150 | · | · | T+2h |
| T7031-6 | 6 | REQUEST_OPENED | · | **150** | **T7031-6#6** | T7031-6#6 | T+2h |
| T7031-6 | 7 | POST_STARTED | · | · | T7031-6#6 | · | T+2h |

Provider ledger (does not rewind): `T7031-6#6 → EXECUTED 150`.
Storage fails; restore to the v4 backup — rows v5–v7 are gone, and so
is the `IDEM_CLAIM` for `#6`. Redelivery replays a different interleave
(a small amendment lands first this time):

| PK | V | EVENT_TYPE | EVENT_CODE | AMOUNT | IDEM_KEY | IDEM_CLAIM | AT |
|---|---|---|---|---|---|---|---|
| T7031-6 | 5 | REQUIRED_AMOUNT_SET | · | **130** | · | · | T+9h |
| T7031-6 | 6 | REQUEST_OPENED | · | **130** | **T7031-6#6** | **accepted — the old claim was erased** | T+9h |

Same key, different amount, schema content: uniqueness cannot remember
rows a restore deleted. The wire call is a same-key/different-payload
collision at best, a silent dedup at worst (app believes 130; 150
actually moved).

**Required design (sketch, not yet done):** an EPOCH component in the
identity (bumped as a mandatory step of every restore, held outside the
restored database), so post-restore slots can never collide with
pre-restore ones; plus a restore runbook that treats every key with an
opening slot ≥ the restore point as potentially burned — enumerate,
query the provider per key, reconcile, hold posting for affected
streams until signed off. Comparable in difficulty to the baseline's
§5.2 sequence-divergence problem; currently undesigned.

---

## L4 — [MEDIUM — PARTIALLY FIXED] Fold inputs must be structured columns

Was: the fold branches on classifications (`BUSINESS_REJECT` vs
`DEFINITIVE_REJECT`, query verdicts, outcome kinds) that had NO column —
two rows byte-identical in every field could carry opposite money
consequences:

| PK | V | EVENT_TYPE | AMOUNT | IDEM_KEY | PROV_CODE | SRC |
|---|---|---|---|---|---|---|
| T7031-2 | 5 | POST_RESULT_RECORDED | · | T7031-2#2 | E-4021 | SYNC_RESPONSE |
| T7031-3 | 5 | POST_RESULT_RECORDED | · | T7031-3#2 | E-7740 | SYNC_RESPONSE |

(`E-4021` = insufficient funds → retry, money stays reserved;
`E-7740` = compliance block → release. Nothing recorded which is
which; re-mapping raw codes later is load-bearing replay of a mutable
mapping — forbidden here exactly as in the baseline.)

**Fixed by exactly the rubric's MEDIUM shape:** the `EVENT_CODE` column
plus `PE_SHAPE_*` per-type CHECK constraints (see `01`); the walkthrough
page shows both columns side by side.

**Why only PARTIALLY (review findings, both bounded):**

1. **The NULL gap — closed in `01`.** `EVENT_CODE` is nullable and
   Oracle CHECK constraints pass on UNKNOWN: a bare
   `EVENT_TYPE != X OR EVENT_CODE IN (...)` ACCEPTS a classified-type
   row with a NULL code — exactly the unclassified row the column
   exists to forbid. Every classified-type CHECK now carries an
   explicit `EVENT_CODE IS NOT NULL`.
2. **The constraint set is a self-declared representative subset.**
   The full per-type matrix — every one of the 17 types stating its
   `EVENT_CODE` rule, including which types REQUIRE the code to be
   NULL — is still to be written.

Kept on the list as the lesson: every fold input must be a typed,
CHECK-bound column, **and the CHECK must be NULL-proof.**

---

## L5 — [MEDIUM] Projection false negatives can strand work

A stale false POSITIVE costs a wasted fold. A stale false NEGATIVE
means no scanner ever selects the payment. Crash between the event
commit and a post-commit projection update:

Truth (`PAYMENT_EVENT`): | T7031-5 | 4 | POST_RESULT_RECORDED | AMBIGUOUS | T7031-5#2 | T+45s |

Stale projection:

| PAYMENT_KEY | FOLD_VERSION | PHASE | NEXT_ACTION_AT |
|---|---|---|---|
| T7031-5 | **3** | **POSTING** | **·** |

`WHERE PHASE='MAYBE' AND NEXT_ACTION_AT <= :now` → 0 rows, every day;
100 stays reserved; nobody asks the provider; escalation (found the
same way) never fires.

**Accommodated by a bounded addition (specified in `01` §6):** the
projection row is updated IN the append transaction, plus a monitored
full-sweep re-fold job — bounded staleness by construction. No core
redesign; the residual work is the test set.

---

## L7 — [MEDIUM] UETR multiplicity + delivery dedup

Two bounded gaps, both now specified in `01`:

1. A non-unique UETR index answers "which streams mention it," not
   "which stream owns it." Simulated multi-match (engine-side UETR
   recycling): feed `SETTLED, UETR=U-88aa` finds
   `{T7031-8, T9044-1}` → without a written rule, 100 books on
   possibly the wrong payment. Rule (fail-closed): `0 → unmatched path;
   1 → fold+append; 2+ → CRITICAL, no state change`. Residual design:
   an immutable UETR claim (bounded — a claim uniqueness, the rubric's
   MEDIUM shape).
2. Duplicate delivery: `MSG-778` redelivered → two identical `SETTLED`
   rows; the idempotent fold saves the MONEY, but side effects
   (metrics, incidents) re-fire and history pollutes. The structure is
   restored (`INBOUND_EVENT_INBOX`, `UNIQUE(source, event_id)`) — but
   its TRANSACTION BOUNDARY is unspecified: a delivery marked seen
   before its multi-payment fan-out completes turns a crash into
   permanently lost work — the redelivery arrives, the inbox says
   "seen," and the unfinished streams never get their events. "Seen"
   must never mean "fully processed." The missing rule (stated as an
   open item in `01` §6b): the inbox row commits atomically with
   COMPLETED processing, or the fan-out is separately resumable and
   the inbox suppresses only side-effect re-fires. Bounded, open.

---

## L8 — [MEDIUM] Request-granular UI (§12) parity

The listing contract needs one row per LOGICAL REQUEST plus an
obligation-only placeholder, keyset pagination, and the amount series:

| row | request | amount | state |
|---|---|---|---|
| 1 | T7031-9#2 | 100 | EXECUTED |
| 2 | T7031-9#6 | 20 | AWAITING SETTLEMENT |

The one-row-per-payment status projection cannot produce this. The
information is all in the stream; the accommodation is a SECOND,
request-granular read projection (additive, under the same L5 freshness
contract) or fold-and-expand on read. Bounded, undecided — MEDIUM
because it is additive, though it is the largest of the additive items.

---

## L3 — [LOW — CONTAINED; RESOLUTION FLOW OPEN] Contradictory evidence

Appends cannot be refused the way the baseline's CAS refuses a late
contradicting write (0 rows). Containment, already in the vocabulary:
contradictions are FIRST-CLASS events with one fixed fold effect —
never ordinary events resolved by hidden precedence:

| PK | V | EVENT_TYPE | EVENT_CODE | AMOUNT | IDEM_KEY | SRC |
|---|---|---|---|---|---|---|
| T7031-7 | 7 | EVIDENCE_CONTRADICTION_RECORDED | SETTLED_AFTER_TERMINAL | 100 | T7031-7#2 | FEED |

Fold effect (fixed): book nothing, PARK the payment (freezes any
in-flight successor), CRITICAL, authoritative reconciliation required.
Demonstrated live as walkthrough scenario 16.

**Residual (why "contained," not "resolved"):** the safe STOP is
designed; the way OUT is not. What happens after the park —
investigation, authoritative reconciliation against provider records,
and the event sequence that corrects the fold state and un-parks the
payment (presumably `OPS_VERIFIED_OUTCOME_APPLIED` under the dual
control the baseline's §9.3 requires, but never spelled out here) —
remains undefined. Until that flow is written, every contradiction is
a permanent manual park. And the safe-stop rule itself lives in the
fold — see L1.

---

## L9 — [LOW] Retention / compliance vs INSERT-only

The integrity anchor (INSERT-only grant + guard trigger) is also an
anti-deletion mechanism. The collision arrives via rows like:

| PK | V | EVENT_TYPE | SRC | ACTOR | DETAIL |
|---|---|---|---|---|---|
| T7031-9 | 12 | OPS_ANNOTATED | OPS | OPS:tom | "beneficiary called, new IBAN DE89 3704 0044 0532 0130 00…" |

Personal data in an unredactable table, replicated into every backup.
Policy work, decided before go-live: forbid sensitive content in
`DETAIL` (same rule + scanner as the baseline's display fields), keep
free text in a separate mutable annotation store, partition-based
archival with signed digests.

---

## Objectives vs mechanisms (greenfield-adjusted)

| Safety objective | Baseline mechanism | Event-model mechanism | Relative strength |
|---|---|---|---|
| Stop stale writers | obligation lock + CAS | expected-version fence | comparable — IF all writes use the append protocol |
| No duplicate active request | I6 unique index (declarative) | fence + fold/procedural validation | **weaker against front-door defects (L1)** |
| No identity reuse | locked counter + UNIQUE | opening-slot identity + IDEM_CLAIM | comparable normally; **restore needs the L6 design** |
| Resolve ambiguous POST | mutable state + resolver | POST_STARTED + query events | comparable; event form arguably clearer |
| Prove never-sent | claim fields + guards | absence of POST_STARTED | **event model clearer** |
| Detect local money drift | counters vs rows (I1/I2) | deterministic fold + external recon | **weaker: no local witness, fixes retroactive (L2)** |
| Contradictory evidence | CAS refuses (0 rows) | first-class contradiction events, safe stop | contained (L3 — resolution flow open) |
| No-request visibility & UI | obligation/request rows | events + read contract (L8) | additive work pending |
| Find due work | indexed current state | projection under the L5 contract | comparable once the contract is tested |

## Bottom line

- **The two CRITICALs (L1, L2) are not defects to fix — they are the
  price of the model.** Adopting it means consciously accepting:
  temporal money invariants enforced by disciplined code instead of
  declarative schema, and a ledger that is derived, self-witnessed, and
  retroactively reinterpretable. If either is non-negotiable — and the
  baseline's twenty review rounds repeatedly treated the first one as
  exactly that — the model is disqualified regardless of everything
  below.
- **The one HIGH (L6) must be designed before any adoption** — it is a
  no-fault, infrastructure-triggered money hazard with a known
  direction (epoch + burned-key reconciliation) and no draft yet.
- The MEDIUMs are bounded additive work (L4 partially done — NULL gap
  closed, full matrix open; L5/L7 partially specified — the inbox
  transaction boundary and both test sets remain); the LOWs are policy
  (L9) or contained-with-an-open-exit (L3).
