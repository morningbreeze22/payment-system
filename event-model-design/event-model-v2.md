# Event-Model Schema v2 (Refactored)

> Status: PROPOSAL. This refactor keeps the one fixed constraint — the
> append-only `PAYMENT_EVENT` table is the single authority for what
> happened to a payment — and redesigns everything else until, in the
> author's judgment, no MAJOR issue remains. Every L-item from the v1
> draft's known-limits list is either FIXED here or explicitly accepted
> with its mitigation. Business semantics (amount rules, evidence
> precedence, release rights, trust-age, escalation, absence-as-zero)
> are inherited from requirement v4 unchanged; this document defines
> the storage model and the mechanisms that make it safe.
>
> New input honored throughout: upstream's ORDERING is a confirmed
> per-trade SEQUENCE NUMBER carried in the message. The entire tie
> class (payload-equality tie detection, AMENDMENT_TIE_CONFLICT, the
> reprocess ≥ relaxation) is deleted, not inherited.

## 0. What changed from the v1 draft, in one view

| v1 problem (its own ranking) | v2 resolution |
|---|---|
| L6 (HIGH, money blocker): identity derived from version slots; restore re-deals slots → key reuse | **FIXED** — identity now derives from a request ordinal, not a version slot (§3). Restore behaves exactly like baseline v4: replay regenerates the same keys. |
| L2 (CRITICAL): no independent money witness; fold changes reinterpret history | **FIXED in its money-bearing part** — money facts are explicit, amount-carrying events the fold may only aggregate (§4); a transactionally-maintained witness row is cross-checked by a drift scan (§5); fold changes are gated by a deploy-time re-fold-compare (§4.2). Control-state reinterpretation remains, accepted (§10). |
| L1 (CRITICAL): no DB backstop for cross-row legality (e.g. two open requests) | **LARGELY FIXED** — the head row makes "at most one open request" a row-count-checked CAS independent of the fold, plus a trigger backstop (§5.2); a small set of cheap cross-row trigger checks is added (§5.3). Full temporal legality stays code-enforced, accepted (§10). |
| L4: per-type EVENT_CODE / column shape matrix incomplete | **FIXED** — complete matrix, including must-be-NULL rules under Oracle three-valued logic (§2.2). |
| L5: projection staleness can strand a payment invisibly | **FIXED by construction** — the projection is merged into the head row and updated in the append transaction; there is no async projector to fall behind (§5). |
| L7: inbox "seen ≠ processed" for multi-payment deliveries | **FIXED** — the inbox covers feed deliveries only, inserted in the same transaction as the append it causes; snapshot deliveries need no inbox because admission + per-payment sequence guards make redelivery converge (§7). |
| L8: request-granular UI undecided | **FIXED** — a read-only SQL view over the event table itself; opening events ARE the request rows; no second projection (§8). |
| L3: contradiction park designed, unpark not | **FIXED** — single exit defined: dual-control verified-outcome events (§6). |
| (new, found in this refactor) no ops event to clear a reject marker; no query REJECTED/ACCEPTED codes | **FIXED** — `OPS_MARKER_CLEARED` added; query result codes completed (§2.2). |
| Fence-retry write loop (optimistic, unfamiliar idiom) | **REPLACED** — pessimistic head-row lock as the write protocol; the fence unique constraint demoted to backstop (§5.1). The write path becomes the same lock-then-write idiom as v4. |

The v1 draft's companion documents (scenario walkthroughs, the
known-limits sheet, the five-issue digest) analyzed the v1 schema and
are removed from this folder — they and the review history that
produced this refactor remain in git history. The runnable
`tl-proposal-proof` suites remain valid where they target mechanisms
this refactor keeps (fence backstop, write-once identity, write-ahead,
idempotent fold) and are superseded where they target v1-specific
choices (slot-derived identity, projection staleness).

First external adversarial review of v2 (2026-07-21): 3 CRITICAL /
4 HIGH / 1 MEDIUM, every finding closed here by mechanism — terminal
amount binding (§4, §5.2, §5.3), full-population deploy gate + the
synchronous write-path witness check (§4.2, §5.1 step 3), pre-wire
recheck + the honest commit-to-wire window (§9, §10.7), the
closed-ordinal correction door with outcome supersession (§4, §5.3,
§6), ordinal claim + counter-equality CAS + key echo (§2, §3, §5.2,
§5.3), the snapshot fan-out equality fence (§7), and version
continuity (§5.3).

Second round (2026-07-21, same harness, targeting the round-1 fixes):
3 CRITICAL / 3 HIGH / 1 MEDIUM, all closed — correction-door amount
equality re-anchored to the ordinal's OWN opening amount + the
excess-direction park-persistence rule (§5.3, §6), query results now
carry and echo the QUERIED key (§2.2, §5.3), the head's open key/amount
copied FROM the opening row instead of program variables (§5.2), the
`TX_ID` + typed `APPROVAL_REF` pair binding for the correction door
(§2, §5.3), verified-NOT-executed latches the inherited
`provider_rejected` marker — the same-transaction-successor promise
RETRACTED (§6), the two-watermark `TRADE_HEAD` so invalid-snapshot
markers fan out under the fence (§1, §7), and amount equality extended
to every outcome code (§5.3).

Third round (2026-07-21, same harness, targeting the round-2 fixes):
2 CRITICAL / 4 HIGH / 1 MEDIUM, all closed — the pair gate extended to
EVERY verified outcome, open or closed (§5.3); full-current-state
fan-out: the seen-fence owner catches up the stored ACCEPTED truth
before marking invalid, so a fenced-out cancellation can never starve
(§7); `TRADE_HEAD` carries digest + pointer PER WATERMARK and a
nullable accepted pair for first-delivery-invalid trades (§1, 01 §7);
`APPROVAL_REF` required on the money-enabling single ops actions
(§2.2); the witness check covers required_amount and gained the
WITNESS_DIVERGED quarantine + rebuild path (§5.1); the correction
delta pinned to the decide-time prior (§6); the request view selects
the authoritative outcome (01 §9).

Fourth round (2026-07-21, targeting the round-3 fixes): 0 CRITICAL /
5 HIGH, all closed — first-contact `REQUIRED_AMOUNT` = NULL with a
NULL-safe witness comparison (§5.1); the dual-control door runs in
RECORDING mode under persistent divergence (fold-authoritative, head
rebuilt in the same transaction) so the prescribed exit is reachable
(§5.1); the backstop trigger set specified as a COMPOUND trigger +
single-row-insert guard, with the mutating-table behavior itself
checklist-5 evidence (§5.3); WITNESS_DIVERGED added to the pre-wire
skip set (§9); `CREATED_AT` guard-trigger-stamped like `TX_ID` — an
immutable episode anchor may never be writer-supplied (§2).

Fifth round (2026-07-21, fresh-eyes sweep of untouched sections):
2 CRITICAL / 2 HIGH / 2 MEDIUM, all closed — the v4 release-guard
transplanted as trigger checks (`CANCELLED_NOT_SUBMITTED` /
`REJECTED_VALIDATION` / `SUPERSEDED_OPS` require zero `POST_STARTED`;
`REJECTED_PROVIDER` requires first-party negative evidence — §5.3);
archival finality rules: PERMANENT heads, event-only archival behind
terminality + reprocessing + key-retention windows, rehydration before
any write (§9, §10.4 reworded: design pre-production, execution
far-future); pre-open definitive enrichment failure = payment-level
marker with NULL ordinal, no impossible outcome pair (§2.2);
`ESCALATION_MARKED` given its real inherited fold effect — the episode
transitions to ops-owned BLOCKED once per episode (01 §4);
`REQUIRED_AT_OPEN` required + head-equality-checked at opening (§2.2);
feed multiplicity counted in DISTINCT payments (§8).

Sixth round (2026-07-21, targeting the round-5 fixes): 3 CRITICAL /
3 HIGH, all closed — feed candidates = UNION of head and event-index
matches, both always consulted, unmatched terminal evidence paged
never silently acked (§8); `REJECTED_PROVIDER` evidence must be
terminal-class AND post-date the latest `POST_STARTED` —
`BUSINESS_REJECT` never qualifies (§5.3); archival eligibility also
waits out every evidence channel's lateness bound and the archive
stays fold-readable, with the deploy gate covering archived streams
(§4.2, §9); pre-open `ENRICH_FAILED` carries the seq it enriched for
(marker provenance + strictly-newer unlatch) and ordinal-bearing
`ENRICH_FAILED` joined the open-ordinal trigger (§2.2, §5.3);
`ESCALATION_MARKED` reverted to §9.3-verbatim semantics — escalated
MAYBE stays resolver-owned, the resolver keeps querying, no separate
unblock exists (01 §4).

Seventh round (2026-07-21): 2 CRITICAL / 4 HIGH / 1 MEDIUM. Four of
seven findings attacked the archival mechanism itself — the honest
remediation was REMOVAL, not more machinery: event rows are now
PERMANENT alongside the heads (§9, §10.4; partitioning-only tiering;
the fold-readable-archive / rehydration / lateness-bound clauses of
rounds 5–6 are superseded). The rest: feed-candidate union re-checked
INSIDE the booking transaction + the honest same-instant residual
(§8); `FEED_RESULT_RECORDED(REJECTED)` added as the 19th type so an
authoritative feed rejection is recordable and release-whitelisted
(§2.1, §2.2, §5.3); unmatched terminal evidence = durable
`MATCH_STATUS` on the inbox row with LEVEL-TRIGGERED paging (§8);
`PAYMENT_KEY` canonical encoding = byte-exact spec + golden vectors +
one shared encoder (§3).

Eighth round (2026-07-22, targeting the round-7 fixes): 3 CRITICAL /
4 HIGH / 1 LOW, all closed — `PE_UETR_UQ` on a generated acceptance
claim column now backs the §8 post-recheck residual with a schema
constraint instead of a promise (§2, §8); `PAYMENT_HEAD.BUSINESS_ID`
bound to the key (decoder-derived + CHECK) so a mis-bound head cannot
drop out of its trade's fan-out worklist (§5); `DOWNGRADED_FOR_REPOST`
trigger-gated on post-attempt NOT_FOUND evidence + the
attempt-attribution question answered by the §18 key-dedup contract,
stated (§5.3); `FEED_REJECTS_OUTCOME` contradiction code added
(§2.2); unmatched inbox rows carry the evidence CONTENT, resolve
atomically, and are purge-exempt (§7); partition maintenance must be
index-preserving with a usability gate before writes resume (§9);
`CREATED_AT` = SYS_EXTRACT_UTC per the inherited single-UTC rule
(§2); two stale cross-references fixed (01).

Ninth round (2026-07-22, targeting the round-8 fixes): 2 CRITICAL /
2 HIGH, all closed — the event-level UETR claim RETRACTED (it missed
the query/feed association channels and refused the legal
re-recording of a CT-dedup-returned acceptance) and replaced by the
HEAD-level claim `PH_UETR_UQ` (one head per UETR, every channel
funnels through the head effect; historical overlaps stay covered by
the UNION rule) (§2, §5, §8); `PH_BIZ_BIND_CK` now IN the DDL,
consuming the newly frozen canonical-form fact (leading business_id +
`|` delimiter) (§3, §5); the downgrade gate gained the
no-intervening-acceptance conjunct — recency alone proved recording
order, not the absence of acceptance (§5.3); the unmatched inbox
evidence content is SHAPE-BOUND per status (§7).

Tenth round (2026-07-22, targeting the round-9 fixes): 2 CRITICAL /
4 HIGH, all closed — the canonical form made INJECTIVE by rule (no
`|` in components, encoder fails closed, intake classifies violations
INVALID; head checks exactly three delimiters) (§3, §5); the
UETR-association trigger — write-once per ordinal + global
first-claim probe on the permanent `PE_UETR_IX` — closes both the
intra-payment wrong-ordinal association and the abandoned-historical-
UETR claim, with `PH_UETR_UQ` kept as the simultaneous-first-claim
belt (§5.3); `FEED_RESULT_RECORDED` gained code ACCEPTED so
intermediate feed acceptance is recordable and visible to the
downgrade gate's acceptance list (§2.1, §2.2, §5.3); inbox resolution
= third status RESOLVED with evidence retained (the two-state flip
was schema-illegal as specified) and the EV class vocabulary is
CHECK-closed (§7).

Eleventh round (2026-07-22, targeting the round-10 fixes):
1 CRITICAL / 1 HIGH / 1 LOW, all closed — UETR REQUIRED on every
feed-matched evidence event and on sync acceptance (a NULL-UETR feed
event was the one door around the §5.3 association gate; recording a
UETR-matched delivery without its UETR discards the identity it was
matched by) (§2.2); inbox resolution split into RESOLVED_MATCHED
(append-bound: records payment key + version of the append it rode
with) and RESOLVED_DISPOSED (ops exit: actor + four-eyes approval +
reason, shape-CHECKed) (§7); the README round-count header fixed
(LOW).

Twelfth round (2026-07-22, targeting the round-11 fixes): 1 CRITICAL
/ 3 HIGH, all closed — resolution provenance VERIFIED, not
decorative: the cited event must correspond to the stored evidence in
class, UETR, and amount (a wrong mapping of SETTLED evidence to a
fabricated rejection dies at the flip) (§7); `RESOLVED_AGREED` added
— the benign no-op path appends nothing, so agreeing evidence cites
the EXISTING terminal it agrees with (§7); `RESOLVED_DISPOSED` bound
to the inherited §9.3 approval protocol (consumption CAS, not a
non-null string) (§7); the `PE_SHAPE_RESULT_CK` UETR conjunct fixed
to R-when-ACCEPTED both ways, with the cell's dependence on the §18
"acceptance always carries UETR" upstream ask stated fail-closed
(01 §2, §2.2).

Thirteenth round (2026-07-22, targeting the round-12 fixes):
1 CRITICAL / 4 HIGH / 1 LOW, all in the inbox-resolution subsystem,
all closed — the correspondence relation made SEMANTIC: legal
resolution targets per class are THE RECORDING, THE CONTRADICTION the
evidence produced (refusing it rolled back the park itself — the
critical), or (for AGREED) an equivalent-class authoritative terminal
(executed-class outcomes agree with SETTLED evidence); reject-class
amount compared to the ordinal's opening amount (the feed-result
event carries none); `RESOLVED_AGREED` added to the shape check's
evidence arm (it was in the status list but not the arm — the update
was schema-illegal); `EV_CLASS IS NOT NULL` added (a NULL class
passed the bare IN-list as UNKNOWN — the round-1 Oracle-3VL lesson,
re-learned in a new place); README round-count header corrected
again (§7, 01 §8).

Fourteenth round (2026-07-22): 1 CRITICAL / 4 HIGH / 1 LOW — the
third consecutive round concentrated in the inbox-resolution
correspondence machinery, which was therefore RETRACTED as a class
(the same removal judgment as round 7's archival): the write path's
gates are the sole legality authority, the inbox keeps only a
non-load-bearing audit pointer (`RESOLVED_HANDLED` = payment key +
head version at handling), and disposal gained the audited
`RECONCILED_BY_KEY` category for the lost-UETR settlement that no
automatic match can ever reach (§7). The round's one write-path
finding fixed for real: `FEED_RESULT_RECORDED` joined the
open-ordinal trigger set, so feed evidence against a CLOSED ordinal
must route through the contradiction park (§5.3). Provenance columns
are now bound in every shape arm (LOW).

Fifteenth round (2026-07-22): 3 CRITICAL / 1 MEDIUM, all closed — the
round-14 retraction rationale HONESTLY CORRECTED: delivery-class
fidelity is real protection the write-path gates cannot see, restored
as the two-rule DELIVERY-FIDELITY BACKSTOP (no class inversion in the
flip transaction; no witnessless no-op) without the failed
enumeration (§7); `RECONCILED_BY_KEY` state-gated — legal only when
the stream ALREADY agrees with the evidence, else the dual-control
verified-outcome door FIRST (§7); the opening amount bound to the
computed shortfall (`AMOUNT = REQUIRED_AMOUNT − PAID_TOTAL` on the
transaction-fresh head) — a 14-round-old gap in the flagship
invariant (§5.3); the single-UTC rule made GLOBAL across all four
structures' timestamps (§2, MEDIUM).

Sixteenth round (2026-07-22): 3 CRITICAL, all closed — the fidelity
rules made UNIVERSAL across matched and unmatched paths (scoping them
to the flip left the ordinary matched path unprotected; terminal
deliveries now retain evidence content in every status, enforcement =
one compound trigger on the inbox write reading its own transaction's
appends); the MISMATCH inversion arm added (a mismatch could be
laundered into a clean rejection); `RECONCILED_BY_KEY` given its real
enforcement point — the disposal transaction locks the named
payment's head, and the agreeing-outcome + not-parked checks run
under that lock, serialized against concurrent corrections (§7). The
transcription-fidelity residue stated honestly as the inherited CA-1
recorded-at-the-time class.

Seventeenth round (2026-07-22): 3 CRITICAL / 1 HIGH, all closed —
the no-op witness rule sharpened in the three dimensions review
proved wrong: the witness is the ASSOCIATED ORDINAL's AUTHORITATIVE
outcome (not any historical event — a superseded EXECUTED cannot
witness), agreement includes AMOUNT for settled/mismatch classes,
and the ordinal is resolved through any UETR-bearing event so the
legal no-UETR query-recovered outcome qualifies (§7);
`RECONCILED_BY_KEY` now names the REQUEST ORDINAL and the locked
check runs at that granularity, the approval bound to the stated
association claim (§7).

Eighteenth round (2026-07-22): 1 CRITICAL / 2 HIGH / 1 LOW, all
closed — reconciled UETR associations made DURABLE and GUARD-VISIBLE
(`RECONCILED_BY_KEY` rows permanent; UNION + first-claim probe
include them, so platform reuse of a reconciled UETR is loud, never
a silent duplicate booking); the MISMATCH no-op witness redefined as
the existing equal mismatch row (mismatch rows are not outcomes —
the authoritative-outcome form was unsatisfiable and looped legal
redeliveries); `QUERY_RESULT_RECORDED` joined the open-ordinal gate
(a query result could attach a first UETR claim to a never-opened
ordinal and wedge the real request's evidence);
`RES_REQUEST_ORDINAL` bound in every shape arm (LOW) (§5.3, §7, 01
§8).

Nineteenth round (2026-07-22): 0 CRITICAL / 2 HIGH — the first
zero-critical round since round 4, both findings in the round-18
reconciliation work, both closed: the reconciled association gained a
HEAD EFFECT (disposal sets the NULL head UETR slot under its lock, so
`PH_UETR_UQ` fences the simultaneous first claim; the
successor-owned-slot residual resolves fail-closed as a parked
multiplicity anomaly under dual control) and the stale "all resolved
rows age out" sentence — which contradicted the permanence rule with
v2 winning precedence — was corrected in place (§7).

Twentieth round (2026-07-22): 0 CRITICAL / 2 HIGH / 1 MEDIUM — second
consecutive zero-critical round, all closed: the ASSOCIATION RELATION
defined once (UETR-bearing events ∪ reconciled rows) and consumed by
every resolver — the reconciled row had been a veto only, leaving the
lost-UETR ordinal positively unresolvable (re-emission deadlock,
unreachable park) (§5.3); `OUTCOME_RECORDED(SUPERSEDED_OPS)` joined
the typed-approval set — the manual close releases a reservation and
re-arms the standing rule, which the inherited approval workflow
gates (§2.2); head rebuild input = stream + reconciled rows, the one
sanctioned non-stream input, so the round-19 head effect is
rebuild-stable (§5.1, MEDIUM).

Twenty-first round (2026-07-22): 0 CRITICAL / 1 HIGH — a single
residual wording contradiction: the detailed no-op witness rule still
resolved the ordinal through UETR-bearing events only, while the
round-20 unified association relation governs; the two load-bearing
specifications are now textually identical (§7, 01 §8). Everything
else checked and found sound.

Twenty-second round (2026-07-22, effort redirected at the non-inbox
sections): 2 CRITICAL / 1 HIGH / 1 MEDIUM, all closed — admission
validity is judged against ACCEPTED truth, not SEEN (a valid
cancellation arriving after a newer invalid snapshot was being
discarded — inherited §6.6 restored; any watermark change re-triggers
the current-state fan-out under the current seen token) (§7); the
release predicate corrected to "provably NOT SUBMITTED" — a latest
attempt closed by a synchronous BUSINESS/DEFINITIVE reject is
cancellable (demanding zero attempts wedged the mandatory
cancel-after-reject flow) — and `SUPERSEDED_OPS` carries the same
predicate (§5.3); the stale `APPROVAL_REF` DDL comments now defer to
the normative §2.2 rule (HIGH); invalid-marker worklist includes
canonically extractable keys so first-delivery-invalid trades anchor
their heads and markers (MEDIUM) (§7).

Twenty-third round (2026-07-22): 0 CRITICAL / 1 HIGH — the round-22
predicate widening had reached only the release trigger; the fold's
`provably_unsent` output and the §6 excess-direction rule still said
"no POST_STARTED", so decide() could never emit the cancellation the
trigger now accepts and a synchronously-rejected excess request
parked forever. The predicate is now UNIFIED — one definition
(`provably_not_submitted`), consumed identically by the fold, the §6
correction rules, the release trigger, and the §9 restatement (§5.3,
§6, §9, 01 §4/§5). Everything else, including every round-22 fix
under direct attack, checked and found sound.

Twenty-fourth round (2026-07-22): 0 CRITICAL / 1 HIGH — the SAME
unification, incomplete at two remaining 01 sites (the
`OUTCOME_RECORDED` vocabulary row and the §5 correction gates still
said "provably-unsent"); all sites now cite the one predicate, and
the §9 skipped-send sentence uses the unified terminology. The
reviewer's soundness list covered the full gate inventory under
direct attack.

Twenty-fifth round (2026-07-22): 0 CRITICAL / 1 HIGH — a genuine
inherited-semantics gap outside every prior finding: the synchronous
engine INVALID-DATA rejection (§7.2 class) had no admissible
`REJECTED_VALIDATION` path (the trigger treated all validation
rejects as pre-wire), forcing it into `REJECTED_PROVIDER` and the
wrong marker whose repeat rules are not newer-truth-recoverable.
`REJECTED_VALIDATION` is now admissible in its two inherited forms —
pre-wire (enrichment) and post-wire synchronous (definitive
invalid-data response on the latest attempt) — with the CA-1
classification deciding the code (§5.3, 01 §4/§6.3). Everything else
in the full gate inventory checked and found sound, including the
completed predicate unification.

Twenty-sixth round (2026-07-22): 1 CRITICAL — the NOT_SUBMITTED
predicate lacked the downgrade gate's no-later-acceptance exclusion:
a delayed key-scoped query ACCEPTED landing after a business-rejected
attempt proves SUBMITTED (§9.4) but did not revoke releasability, so
a cancel + new-key successor could run against an
acknowledged-accepted claim. The predicate's FULL definition (now
with the acceptance-exclusion conjunct) lives in exactly TWO places —
§5.3 and 01 §5 — and every other site cites it WITHOUT restating its
arms, which is also the structural fix for the four rounds of
restatement drift.

Twenty-seventh round (2026-07-22): 0 CRITICAL / 1 HIGH / 1 LOW —
`SETTLEMENT_MISMATCH_RECORDED` joined the open-ordinal trigger set
(a mismatch against a CLOSED ordinal could commit as a free-standing
park with no §6 exit instead of routing to
`MISMATCH_AFTER_TERMINAL`); the last arm-level rendering of the
release predicate (01 §6.3) reduced to a citation (LOW). The
round-26 acceptance exclusion held under the downgrade-cycle attack,
and the full gate inventory checked sound.

Twenty-eighth round (2026-07-22): 0 CRITICAL / 1 HIGH — the
current-state fan-out applied the invalid marker BLANKET-wide: a
payment first introduced by later-admitted valid truth (never named
by the invalid document) inherited the marker and was blocked behind
an unlatch bar no necessary future snapshot would clear. The marker
now applies only to the MARKER SET stamped durably at seen-admission
(existing heads ∪ extractable keys, recorded alongside the seen
pair), restoring inherited §6.6 (§7, 01 §7). The full gate inventory
otherwise checked sound.

Twenty-ninth round (2026-07-22): 0 CRITICAL / 1 HIGH — the round-28
marker set had NO durable schema home and 01 §4 still said "EVERY
payment of the trade". The mechanism was SIMPLIFIED instead of given
a sidecar: `SNAPSHOT_INVALID_MARKED` appends happen INSIDE the
seen-admission transaction, atomically with the watermark, over
exactly the set knowable then — the stream rows ARE the durable
record (no sidecar, no resume rule; crash = full-admission rollback +
redelivery re-run); the current-state fan-out carries ACCEPTED truth
only, and the extractable-key worklist branch moved into admission
with the markers (§7, 01 §4/§7).

Thirtieth round (2026-07-22): 1 CRITICAL / 1 HIGH — admission never
explicitly serialized the watermark comparison (two concurrent valid
arrivals could both judge themselves newer, regress the watermarks,
and hand the equality fence to the stale snapshot, which then posts
a cancelled amount): admission now takes `SELECT FOR UPDATE` on
`TRADE_HEAD` as its FIRST action, held through comparison, update,
and every in-admission marker, lock order identical to fan-out (§7,
01 §7); and both §1 structure summaries still authorized the retired
deferred-marker fan-out — corrected to the round-29 in-admission
rule (§1, 01 §1).

Thirty-first round (2026-07-22): 0 CRITICAL / 2 HIGH, both
consistency-level, both closed — one more stale round-22 sentence
("applies accepted truth 150 plus the invalid-200 marker")
contradicted the round-29 rule and was corrected in place (§7); the
universal fidelity trigger gained its MANDATORY statement order —
event inserts + head effects first, terminal inbox write LAST, then
commit — because Oracle triggers fire at statement time and an
inbox-first order would misapply the witnessless-no-op rule to a
legitimate settlement and wedge every redelivery (§7, 01 §8).

Thirty-second round (2026-07-22): **0 CRITICAL / 0 HIGH** / 1 MEDIUM
— the first clean round of the campaign. The one Medium
(`PROVIDER_REFERENCE` fallback matching absent) is recorded as an
ACCEPTED DEFERRAL (§8): the baseline's own deferrable list carries
the identical item, and the key-query path bounds it to latency,
never safety. The reviewer's soundness list covered the full
mechanism inventory. Campaign totals across 32 rounds: 24 CRITICAL,
44 HIGH, 12 MEDIUM, 5 LOW findings folded — every one closed by
mechanism, removal, or recorded acceptance.

## 1. Physical structures — four, same count as v4

| Structure | Kind | Role |
|---|---|---|
| `PAYMENT_EVENT` | append-only, THE authority | everything that ever happened to a payment; per-payment total order |
| `PAYMENT_HEAD` | ONE mutable row per payment | write serialization lock, money WITNESS, open-request backstop, scanner/UI index — updated in the append transaction, rebuildable from the stream, NEVER read by a money decision |
| `TRADE_HEAD` | one mutable row per trade | TWO snapshot watermarks, EACH with its own digest + storage pointer — accepted (`LAST_ACCEPTED_SEQ`, newest VALID snapshot = trade truth; NULL until the first valid one) and seen (`LAST_SEEN_SEQ`, newest processed at all, valid or invalid; `≥` accepted). The fan-out equality fence (§7) checks SEEN; invalid-snapshot markers are appended INSIDE the seen-admission transaction itself (§7 — never by the fan-out, which carries accepted truth only); the seen digest/pointer make an invalid redelivery deduplicable (a single accepted-only digest would misread an identical invalid redelivery as an upstream defect). With a contractual sequence number there is no tie to adjudicate — an equal-seq redelivery (equal digest, per the SEEN pair) is admitted-without-update; equal seq with different content is an upstream DEFECT (refuse + CRITICAL alert), not a workflow |
| `INBOUND_EVENT_INBOX` | `UNIQUE(source, event_id)` | dedup of FEED deliveries only, atomic with processing (§7) |

Everything else — required amount, paid, reserved, phase, markers,
retry state, MAYBE, parked, escalation — is derived by the fold.

## 2. `PAYMENT_EVENT`

```sql
CREATE TABLE PAYMENT_EVENT (
  ID                 NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  PAYMENT_KEY        VARCHAR2(200)  NOT NULL,   -- canonical scope tuple
  VERSION            NUMBER(10)     NOT NULL,   -- fence slot; total order
  EVENT_TYPE         VARCHAR2(40)   NOT NULL,
  EVENT_CODE         VARCHAR2(40),              -- typed classification, per-type CHECK-bound
  REQUEST_ORDINAL    NUMBER(10),                -- WHICH request this event concerns (identity input, §3)
  UPSTREAM_SEQ       NUMBER(19),                -- confirmed upstream sequence (snapshot-derived events)
  AMOUNT             NUMBER(18,3),
  REQUIRED_AT_OPEN   NUMBER(18,3),              -- display stamp on REQUEST_OPENED only (UI amount series); never load-bearing
  IDEMPOTENCY_KEY    VARCHAR2(128),
  PAYLOAD_HASH       VARCHAR2(64),
  UETR               VARCHAR2(64),
  PROVIDER_REFERENCE VARCHAR2(128),
  PROVIDER_CODE      VARCHAR2(64),
  EVIDENCE_SOURCE    VARCHAR2(16),              -- SYNC_RESPONSE / QUERY / FEED / OPS / SYSTEM
  APPROVAL_REF       VARCHAR2(64),              -- dual-control approval id, TYPED (fold-visible);
                                                --   REQUIRED per the NORMATIVE §2.2 rule (the pair,
                                                --   the money-enabling ops actions, SUPERSEDED_OPS);
                                                --   never DETAIL — §2.2 governs, not this comment
  ACTOR              VARCHAR2(64)   NOT NULL,
  DETAIL             VARCHAR2(1000),            -- human text; the fold NEVER reads it
  TX_ID              VARCHAR2(64),              -- stamped BY THE GUARD TRIGGER with the local
                                                --   transaction id; writers cannot supply it
  CREATED_AT         TIMESTAMP      NOT NULL,   -- ALSO guard-trigger-stamped, UTC:
                                                --   SYS_EXTRACT_UTC(SYSTIMESTAMP) per the
                                                --   inherited v4 §16.4 single-UTC rule (a
                                                --   local-clock stamp crosses DST jumps and
                                                --   corrupts trust-age arithmetic forever);
                                                --   no writer may supply it. The single-UTC
                                                --   rule is GLOBAL: every persisted timestamp
                                                --   in all four structures (incl. inbox
                                                --   RECEIVED_AT — a retention anchor — and
                                                --   the heads' UPDATED_AT) is UTC the same way

  -- identity is claimed exactly once, in the schema:
  IDEM_CLAIM         VARCHAR2(128)  GENERATED ALWAYS AS
                       (CASE WHEN EVENT_TYPE = 'REQUEST_OPENED' THEN IDEMPOTENCY_KEY END),
  -- and so is the ordinal (one opening per (payment, ordinal), ever):
  ORDINAL_CLAIM      VARCHAR2(220)  GENERATED ALWAYS AS
                       (CASE WHEN EVENT_TYPE = 'REQUEST_OPENED'
                             THEN PAYMENT_KEY || '#' || TO_CHAR(REQUEST_ORDINAL) END),
  -- (UETR uniqueness is enforced at the HEAD, not here: an event-level
  --  acceptance claim missed the query/feed association channels AND
  --  collided with the legal re-recording of a CT-dedup-returned
  --  original acceptance — see PH_UETR_UQ, §5)

  CONSTRAINT PE_FENCE_UQ   UNIQUE (PAYMENT_KEY, VERSION),  -- backstop fence (§5.1)
  CONSTRAINT PE_IDEM_UQ    UNIQUE (IDEM_CLAIM),            -- identity write-once
  CONSTRAINT PE_ORDINAL_UQ UNIQUE (ORDINAL_CLAIM)          -- ordinal write-once
  -- plus the full §2.2 shape-check set
);
CREATE INDEX PE_KEY_IX  ON PAYMENT_EVENT (PAYMENT_KEY, VERSION);
CREATE INDEX PE_UETR_IX ON PAYMENT_EVENT (UETR);
```

Grants: application role has INSERT and SELECT only. A guard trigger
raises on any UPDATE or DELETE — history is immutable against every
writer including humans in an incident.

### 2.1 Event vocabulary (19 types)

Unchanged in meaning from v1/v4 except where noted: `REQUIRED_AMOUNT_SET`,
`SNAPSHOT_INVALID_MARKED`, `REQUEST_OPENED`, `ENRICH_FAILED`,
`POST_STARTED`, `POST_RESULT_RECORDED`, `QUERY_RESULT_RECORDED`,
`DOWNGRADED_FOR_REPOST`, `OUTCOME_RECORDED`, `SETTLED`,
`SETTLEMENT_MISMATCH_RECORDED`, `EVIDENCE_CONTRADICTION_RECORDED`,
`ESCALATION_MARKED`, `OPS_VERIFIED_OUTCOME_APPLIED`, `OPS_RETRY_REARMED`,
`OPS_BLOCKED`, `OPS_ANNOTATED`, **new** `OPS_MARKER_CLEARED`
(the v4 §19.3 ops clear of a live reject marker — the v1 draft had no
way to express it; without it a twice-rejected payment could never be
re-armed except by a newer upstream message), and **new**
`FEED_RESULT_RECORDED` (codes `ACCEPTED` / `REJECTED`: the feed
channel's evidence about an active request — REJECTED is its terminal
rejection (without it an authoritative feed reject was UNRECORDABLE:
`SETTLED` means money moved, contradiction events require an existing
terminal, and the §5.3 release whitelist demands a recorded evidence
row); ACCEPTED is the intermediate status-feed acceptance that
tightens submission knowledge per the inherited §4.4 precedence —
without it, feed acceptance was silently droppable and the §5.3
downgrade gate could not see it. Feed-executed remains `SETTLED`).

### 2.2 Complete shape matrix (closes L4)

R = required (IS NOT NULL enforced), N = must be NULL, O = optional.
Every classified type's EVENT_CODE check carries an explicit
`IS NOT NULL` (a bare IN-list CHECK evaluates UNKNOWN on NULL and
silently passes); every N cell is a real CHECK, not a convention.

| EVENT_TYPE | EVENT_CODE | ORDINAL | UPSTREAM_SEQ | AMOUNT | IDEM_KEY | PAYLOAD_HASH | EVIDENCE_SOURCE |
|---|---|---|---|---|---|---|---|
| REQUIRED_AMOUNT_SET | N | N | R | R (≥0; 0 = removal) | N | N | SYSTEM |
| SNAPSHOT_INVALID_MARKED | N | N | R | N | N | N | SYSTEM |
| REQUEST_OPENED | N | R | N | R (>0) | R | R | SYSTEM/OPS |
| ENRICH_FAILED | R: TRANSIENT, DEFINITIVE | O — NULL when enrichment fails BEFORE any opening (no request exists to cite; fold effect = latch `validation_failed`, block new opens, NO outcome pair — an outcome would need an open ordinal and a payload hash that was never assembled); R when re-enrichment fails for an OPEN request (then open-ordinal trigger-checked like attempt events) | R when ORDINAL is NULL — the seq of the required-amount truth being enriched, so the marker has replayable provenance and unlatches on strictly NEWER truth (without it a delayed stale failure could outlive the correction that superseded it); N when ORDINAL is R | N | N | N | SYSTEM |
| POST_STARTED | N | R | N | N | R | R | SYSTEM |
| POST_RESULT_RECORDED | R: ACCEPTED, BUSINESS_REJECT, DEFINITIVE_REJECT, AMBIGUOUS, COLLISION, UNMAPPED | R | N | N | R | N | SYNC_RESPONSE |
| *(UETR rule for evidence events)* | *UETR is REQUIRED — never optional — on every feed-matched evidence event (`SETTLED`, `FEED_RESULT_RECORDED` both codes, `SETTLEMENT_MISMATCH_RECORDED`, feed-sourced contradictions) and on `POST_RESULT_RECORDED(ACCEPTED)`: the delivery was MATCHED BY its UETR, and recording it without the UETR discards the identity the §5.3 association gate keys on — a NULL-UETR feed event was the one door around that gate* | | | | | | |
| QUERY_RESULT_RECORDED | R: EXECUTED, REJECTED, ACCEPTED, NOT_FOUND, LOOKBACK_EXPIRED | R | N | N | R (the key that was QUERIED — echo-checked, §5.3) | N | QUERY |
| DOWNGRADED_FOR_REPOST | N | R | N | N | N | N | SYSTEM/OPS |
| OUTCOME_RECORDED | R: EXECUTED, REJECTED_VALIDATION, REJECTED_PROVIDER, CANCELLED_NOT_SUBMITTED, SUPERSEDED_OPS, PLATFORM_VERIFIED_EXECUTED, PLATFORM_VERIFIED_NOT_EXECUTED | R | N | R (the request amount, restated — §4) | N | N | R |
| SETTLED | N | R | N | R | N | N | FEED |
| FEED_RESULT_RECORDED | R: ACCEPTED, REJECTED | R | N | N | N | N | FEED |
| SETTLEMENT_MISMATCH_RECORDED | N | R | N | R (the wrong amount) | N | N | FEED |
| EVIDENCE_CONTRADICTION_RECORDED | R: SETTLED_AFTER_TERMINAL, MISMATCH_AFTER_TERMINAL, QUERY_CONTRADICTS_OUTCOME, FEED_REJECTS_OUTCOME | R | N | O | N | N | R |
| ESCALATION_MARKED | N | R | N | N | N | N | SYSTEM |
| OPS_VERIFIED_OUTCOME_APPLIED | N | R | N | N | N | N | OPS |
| OPS_RETRY_REARMED / OPS_BLOCKED / OPS_MARKER_CLEARED / OPS_ANNOTATED | N | O | N | N | N | N | OPS |

(`REQUIRED_AT_OPEN` is REQUIRED on `REQUEST_OPENED` and must-be-NULL
elsewhere — aligned with the derived schema, which wins nothing: this
matrix is authoritative — and the opening backstop additionally checks
`REQUIRED_AT_OPEN = REQUIRED_AMOUNT` on the transaction-fresh head, so
the immutable UI amount-series stamp cannot be born wrong even though
it is never load-bearing. `UETR` only on acceptance-class and feed
events, per the v4 §5 UETR-persistence rule, inherited verbatim:
reject/collision UETRs are never recorded.)

Ops events carry the approval reference in the TYPED `APPROVAL_REF`
column — R on `OPS_VERIFIED_OUTCOME_APPLIED` and its paired
`OUTCOME_RECORDED(PLATFORM_VERIFIED_*)` (equal on both), R on the
two MONEY-ENABLING single ops actions, `OPS_MARKER_CLEARED` and
`OPS_RETRY_REARMED` (the v4 §19.3-class clears carry four-eyes
authorization; a nullable approval on the event that re-opens the
road to fresh payment would make that authorization unrepresentable),
AND R on `OUTCOME_RECORDED(SUPERSEDED_OPS)` — the manual close
RELEASES a reservation and re-arms the standing rule, which v4's
approval workflow explicitly gates; without a typed consumed
approval, one actor could release money and enable a successor. All
approvals consume APPROVED → CONSUMED by CAS in their transaction.
N on `OPS_BLOCKED`/`OPS_ANNOTATED` (restrictive/neutral actions) and
every non-ops type — free text in DETAIL binds nothing. The verified
pair is appended in the SAME transaction — dual-control protocol
inherited from v4 §9.3 unchanged, and the same-transaction fact is
DB-checkable via `TX_ID` (§5.3), not merely promised by the
application.

## 3. Identity — the L6 fix

```
request_ordinal  = 1 + (count of prior REQUEST_OPENED events in this stream)
                   — maintained as PAYMENT_HEAD.NEXT_REQUEST_ORDINAL,
                     consumed and stamped onto the opening event in the
                     opening transaction (write-once, shape-checked)
idempotency_key  = hash(business_id | payment_type | debit_account |
                        currency | request_ordinal)
                   — byte-exact spec + golden vectors, identical
                     discipline to v4 §5.1
```

Version slots no longer participate in identity. Consequences:

- A database restore + Kafka replay regenerates the SAME ordinals from
  business history — recreated requests carry the SAME keys, the
  engine rejects the collision, and the v4 §5.2 restore runbook
  applies essentially unchanged (posting freeze lives in Hazelcast,
  outside the database; the 5b enumeration sweep enumerates
  hash(scope | 1..N+K) exactly as v4 does).
- `IDEM_CLAIM` remains the schema-level write-once guarantee: two
  opening events can never carry the same key, whatever produced them.
- **The ordinal itself is schema-bound**: `PE_ORDINAL_UQ` makes ordinal
  reuse collide loudly, and the §5.2 opening CAS carries
  `NEXT_REQUEST_ORDINAL = :ordinal`, so a writer supplying a stale or
  skipped ordinal aborts on row count 0 — counter, ordinal, and claim
  cannot drift apart. What remains code discipline (stated honestly):
  the KEY DERIVATION content — that the hash was computed over THIS
  ordinal — which lives under the same golden-vector regime as v4 §5.1.
- No epoch/generation machinery is needed for the main hazard. (An
  epoch component remains a cheap optional hardening; not required.)
- **The `PAYMENT_KEY` encoding is itself identity input** and gets the
  SAME discipline: a byte-exact canonical encoding specification,
  golden vectors, and ONE shared encoder — two "reasonable" encodings
  of the same scope tuple would create two heads for one payment and
  re-derive the same idempotency keys under a fresh stream. Every
  uniqueness guarantee in this design keys on the canonical form
  existing exactly once. The NORMATIVE canonical form (consumed by
  the `PH_BIZ_BIND_CK` binding check, §5):
  `business_id || '|' || payment_type || '|' || debit_account || '|'
  || currency` — and the form is INJECTIVE by rule, not by luck: no
  component may contain `|` (the shared encoder FAILS CLOSED on one;
  intake whole-document validation classifies such a snapshot
  INVALID — without this rule, `(B, T|X, D)` and `(B, T, X|D)`
  encode identically and two real payments merge into one head). The
  head additionally CHECKs exactly three delimiters in the key. The
  leading business_id component and the `|` delimiter are FROZEN
  facts of the form.

## 4. Money facts are events; the fold only aggregates them — the L2 fix (money half)

**Rule (normative):** every money-bearing fact is an explicit event
carrying its own amount and its own classification, recorded at
decision time. The fold may AGGREGATE money events; it may never
re-classify them, re-derive them from raw provider codes, or infer an
amount not present on an event.

```
required_amount = AMOUNT of the REQUIRED_AMOUNT_SET with the highest UPSTREAM_SEQ
authoritative_outcome(ordinal)
                = the LATEST outcome-class event (OUTCOME_RECORDED or
                  SETTLED) for that ordinal in stream order — a
                  PLATFORM_VERIFIED_* outcome appended through the §6
                  dual-control door SUPERSEDES the outcome it corrects
paid_total      = Σ AMOUNT over request ordinals whose authoritative
                  outcome is executed-class (EXECUTED |
                  PLATFORM_VERIFIED_EXECUTED | SETTLED)  (idempotent by ordinal)
reserved        = AMOUNT of the open request (opening event exists,
                  no outcome event for its ordinal), else 0
shortfall       = required − paid_total − reserved
```

Amount binding (normative): a money-terminal event for an ordinal must
carry AMOUNT equal to that ordinal's OPENING amount — the all-or-nothing
engine contract makes any other number defect evidence, recorded as
`SETTLEMENT_MISMATCH_RECORDED` (or a contradiction), never as
`OUTCOME_RECORDED(EXECUTED)`/`SETTLED`. §5.2/§5.3 enforce this equality
in the database, so a wrong terminal number cannot silently enter
either bookkeeper and reopen a phantom shortfall.

Because the inputs are literal recorded numbers, deploying a changed
fold CANNOT retroactively change a settled payment's money totals
unless the aggregation itself changes — which is exactly what §4.2
gates.

### 4.2 Fold governance

One shared fold artifact, semantically versioned, golden-vector
frozen; no consumer (UI, scanner, resolver, ops surface) may
re-implement any part of it. Two standing controls:

1. **Deploy gate:** before a release containing a fold change goes
   live, re-fold EVERY payment stream with a head row — open AND
   terminal, no sampling, no exclusions (events are permanent, §9,
   so the full population is always foldable) — and compare against
   the `PAYMENT_HEAD` witness (§5). Any money-field difference is a
   page and blocks the deploy. Sampling is forbidden: a sampled gate
   provably lets a terminal-stream fold defect deploy silently and
   later reopen a phantom shortfall when new upstream traffic touches
   the unsampled stream. (At ~3,000 trades/day the full population is
   minutes of work.)
2. **Drift scan (continuous):** a scheduled job re-folds open payments
   and compares fold output to the head witness; mismatch pages. This
   is the same two-bookkeepers control as v4's I1/I2 scan: the head is
   maintained by simple mechanical increments (§5), the fold by rule
   evaluation — two independent computations of the same money.

History is never edited. A wrong recorded decision is corrected by a
dual-control ops event (§6), which is itself a new recorded fact.

## 5. `PAYMENT_HEAD` — lock, witness, backstop, index (one row per payment)

```sql
CREATE TABLE PAYMENT_HEAD (
  PAYMENT_KEY          VARCHAR2(200) PRIMARY KEY,
  BUSINESS_ID          VARCHAR2(64)  NOT NULL,     -- card lookup (indexed); BOUND to the key:
                                                   --   derived FROM PAYMENT_KEY by the one
                                                   --   canonical decoder at first contact, and
                                                   --   CHECK-enforced against the key's leading
                                                   --   component (an unbound copy would drop the
                                                   --   payment from its trade's fan-out worklist
                                                   --   and skip an absence-cancellation)
  LAST_VERSION         NUMBER(10)    NOT NULL,     -- must equal stream max
  NEXT_REQUEST_ORDINAL NUMBER(10)    NOT NULL,     -- identity counter (§3)
  OPEN_REQUEST_ORDINAL NUMBER(10),                 -- NULL = no open request
  OPEN_IDEMPOTENCY_KEY VARCHAR2(128),              -- the open request's key (echo check, §5.3)
  -- money WITNESS (mechanical increments; never read by decisions):
  REQUIRED_AMOUNT      NUMBER(18,3),
  PAID_TOTAL           NUMBER(18,3) DEFAULT 0 NOT NULL,
  RESERVED             NUMBER(18,3) DEFAULT 0 NOT NULL,
  -- scanner / UI index (display + candidate selection only):
  PHASE                VARCHAR2(24),
  NEXT_ACTION_AT       TIMESTAMP,
  UETR                 VARCHAR2(64),
  UI_STEP_STATUS       VARCHAR2(16),
  ESCALATED            CHAR(1) DEFAULT 'N',
  UPDATED_AT           TIMESTAMP
);
CREATE INDEX PH_DUE_IX  ON PAYMENT_HEAD (PHASE, NEXT_ACTION_AT);
CREATE INDEX PH_BIZ_IX  ON PAYMENT_HEAD (BUSINESS_ID);
CREATE UNIQUE INDEX PH_UETR_UQ ON PAYMENT_HEAD (UETR);
-- plus, in the CREATE TABLE: the key/business binding constraint
--   CONSTRAINT PH_BIZ_BIND_CK CHECK
--     (BUSINESS_ID = SUBSTR(PAYMENT_KEY, 1, INSTR(PAYMENT_KEY,'|') - 1)
--      AND LENGTH(PAYMENT_KEY)
--          - LENGTH(REPLACE(PAYMENT_KEY,'|','')) = 3)   -- injectivity assist
-- which depends on the NORMATIVE canonical form (§3):
--   PAYMENT_KEY = business_id || '|' || payment_type || '|' ||
--                 debit_account || '|' || currency  (no '|' in components)
```

**The UETR claim lives on the head:** every UETR-bearing evidence
recording sets `PAYMENT_HEAD.UETR` in its per-event head effect, and
`PH_UETR_UQ` makes the CURRENT association unique across ALL payments
— two heads can never claim the same UETR, whichever channel (sync
acceptance, query, feed) recorded it, and the competing claim dies
loudly at ITS commit. Historical associations (a head moved on to a
successor's UETR) are covered by the §8 UNION multiplicity rule. An
event-level acceptance claim was tried and RETRACTED: it missed the
query/feed channels entirely, and it refused the LEGAL re-recording
of a CT-dedup-returned original acceptance (same payment, same UETR)
after a downgrade re-post — the head claim is a no-op update in that
case, exactly right.

The head is a CACHE with respect to truth (rebuildable from the
stream at any time) but a CONTRACT with respect to freshness: it is
updated **in the append transaction**, so there is no async projector
to fall behind and no stale-false-negative class (v1's L5 is closed by
construction, not by a reconciling sweep).

### 5.1 Write protocol — the only write path

```
1. SELECT ... FOR UPDATE on PAYMENT_HEAD (insert-on-first-contact:
   LAST_VERSION = 0, NEXT_REQUEST_ORDINAL = 1, PAID_TOTAL = 0,
   RESERVED = 0, REQUIRED_AMOUNT = NULL — NULL is the inherited
   "no valid data ever applied" value and matches the empty stream's
   fold, where 0 would fail the first witness check ever run;
   explicit initial values, never NULL+1 arithmetic; PK-race retry —
   the same idiom as v4's obligation row)
2. fold(stream)                       -- read PAYMENT_EVENT by (key, version)
3. WITNESS CHECK (fail closed): compare the fold's money outputs —
   required_amount, paid_total, reserved, AND open ordinal — against
   the locked head row (the COMPLETE money witness; omitting
   required_amount would let a mis-witnessed requirement drive an
   oversized opening). Comparison is NULL-safe: fold-NULL equals
   head-NULL (the first-contact state). ANY mismatch: abort with NO
   decision, page, and QUARANTINE: set the head's PHASE to
   WITNESS_DIVERGED through the same narrow head-only exemption as
   scheduling updates (a display/candidate-selection mutation, never
   a derivation input) — scanners skip diverged payments, so the
   quarantine needs no append and cannot itself be blocked by the
   check it serves. Repair = the head REBUILD runbook (§5.1), which
   now triggers on fence collision OR witness divergence: rebuild the
   head from the stream PLUS the payment's permanent
   RECONCILED_BY_KEY rows (§7 — the ONE sanctioned non-stream rebuild
   input: the reconciled UETR association exists nowhere in the
   stream, and a stream-only rebuild would silently drop it out of
   the PH_UETR_UQ fence) under the lock; if the divergence clears,
   the head was wrong — resume. If it persists, the stream itself is under
   dispute and only the §6 dual-control door may resolve it — and so
   that exit is REACHABLE, the door transaction alone runs under
   divergence with the witness check in RECORDING mode: it proceeds on
   the step-2 fold (the stream is the authority; the head is a cache),
   appends the gated pair, then REBUILDS the head from the post-append
   stream in the same transaction and clears the quarantine. Nothing
   else may write while diverged. The check is a VETO, never an
   authorization
4. decide                             -- pure function: fold state -> events
5. FOR EACH decided event, IN ORDER:
     INSERT it at the next version slot,
     THEN apply ITS head effect (§5.2 CAS, witness change, phase)
6. COMMIT   (multi-event decisions remain ONE transaction — atomic)
```

Step 5 is **per event, not batched**: the §5.2/§5.3 backstops check each
insert against the head state its predecessors IN THIS TRANSACTION left
behind. The atomic outcome+successor decision is legal only in this
interleaving — outcome insert (its ordinal still open) → close CAS
(column goes NULL) → successor `REQUEST_OPENED` insert (trigger sees
NULL) → open CAS. Batching all inserts before one head update would
make the backstops reject the design's own legal writes.

The head lock serializes writers per payment — no optimistic retry
loop, no lock-order subtleties beyond TRADE_HEAD → PAYMENT_HEAD
(sorted) during snapshot fan-out, which is v4's exact lock order.
`PE_FENCE_UQ` remains as the backstop: any writer that bypasses the
lock discipline collides on the unique constraint and fails loudly
instead of forking history. A fence collision seen by a writer that
HOLDS the head lock means the head has lost sync with the stream (a
bypass write happened): page, stop this stream's writes, rebuild the
head row from the stream, then resume — the head is a cache and this
is its rebuild path, exercised deliberately in tests.

Narrow exemption (mirrors v4's claim-field rule): pure scheduling
updates on the head (`NEXT_ACTION_AT` backoff after an INDETERMINATE
query) may be written without an append — they are not derivation
inputs and not money.

### 5.2 The one-open-request backstop — the L1 fix (critical part)

Opening a request executes, in the opening transaction:

```
UPDATE PAYMENT_HEAD h
   SET h.OPEN_REQUEST_ORDINAL = :ordinal,
       (h.OPEN_IDEMPOTENCY_KEY, h.RESERVED) =
         (SELECT e.IDEMPOTENCY_KEY, e.AMOUNT     -- copied FROM THE OPENING
            FROM PAYMENT_EVENT e                 -- ROW, never from a program
           WHERE e.ORDINAL_CLAIM =               -- variable (binding rule)
                 :key || '#' || TO_CHAR(:ordinal)),
       h.NEXT_REQUEST_ORDINAL = h.NEXT_REQUEST_ORDINAL + 1
 WHERE h.PAYMENT_KEY = :key
   AND h.OPEN_REQUEST_ORDINAL IS NULL
   AND h.NEXT_REQUEST_ORDINAL = :ordinal    -- the counter IS the ordinal
```

The head's open-request key and amount are COPIES OF THE OPENING EVENT
ROW (read back through `PE_ORDINAL_UQ` inside the same transaction) —
a program variable can therefore never put one key into the schema
claim and a different one onto the head that the wire echo checks
against; the wire key is transitively bound to the claimed key.

Row count 0 aborts the append. This is a second bookkeeper for the
single most dangerous invariant — it does not trust the fold. Closing
(outcome) clears the ordinal and key columns the same way
(`WHERE OPEN_REQUEST_ORDINAL = :ordinal`), zeroes `RESERVED`, and adds
the amount to `PAID_TOTAL` only when the outcome is money-terminal
(`EXECUTED` / `PLATFORM_VERIFIED_EXECUTED` / `SETTLED`) — and the
money-terminal close additionally carries `AND RESERVED = :amount`:
the witness binds not just WHICH ordinal closes but AT WHAT NUMBER it
closes (the §4 amount-binding rule made mechanical; a differing feed
amount can only enter as `SETTLEMENT_MISMATCH_RECORDED`). A trigger on
`PAYMENT_EVENT` additionally rejects a `REQUEST_OPENED` insert when
the head's column is non-NULL — belt and braces, both independent of
fold correctness.

**Witness-exactly-once (consequence, worth stating):** because §5.3
admits money-bearing events for an ordinal only WHILE that ordinal is
the open one, and the close CAS fires exactly once per ordinal (row
count 0 otherwise), `PAID_TOTAL` increments exactly once per request
ordinal — the mechanical witness cannot double-count the same request
even if late confirming evidence arrives (that lands as a no-op or a
contradiction event, §6, never as a second increment).

### 5.3 Cheap cross-row trigger backstops (the rest of L1, partial)

Because the head is transaction-fresh, a set of cross-row legality
checks becomes DB-enforceable as BEFORE-INSERT triggers on
`PAYMENT_EVENT` (each a single indexed head read under the lock
already held):

- **Open-ordinal**: `POST_STARTED` / `POST_RESULT_RECORDED` /
  `QUERY_RESULT_RECORDED` / `OUTCOME_RECORDED` / `SETTLED` /
  `FEED_RESULT_RECORDED` / `SETTLEMENT_MISMATCH_RECORDED` — and
  `ENRICH_FAILED` when it names an ordinal — require
  `OPEN_REQUEST_ORDINAL = :new.REQUEST_ORDINAL`
  (except the terminal-evidence contradiction path, which must
  instead append `EVIDENCE_CONTRADICTION_RECORDED` — the trigger
  enforces that routing; omitting `FEED_RESULT_RECORDED` let a feed
  rejection be recorded against a CLOSED executed ordinal without
  the park, and omitting `QUERY_RESULT_RECORDED` let a query result
  attach a FIRST UETR claim to an arbitrary never-opened ordinal,
  wedging the real request's terminal evidence; omitting
  `SETTLEMENT_MISMATCH_RECORDED` let a mismatch against a CLOSED
  ordinal commit as a free-standing park with NO §6 exit instead of
  routing to `MISMATCH_AFTER_TERMINAL`);
  `REQUEST_OPENED` requires it NULL (§5.2).
- **The dual-control pair gate — on EVERY verified outcome, open or
  closed**: `OUTCOME_RECORDED(PLATFORM_VERIFIED_*)` — for ANY ordinal
  state — is admitted only when the row at `VERSION − 1` is
  `OPS_VERIFIED_OUTCOME_APPLIED` for the SAME ordinal with the SAME
  `TX_ID` (same transaction, DB-checked — a dangling approval
  committed earlier can never be paired later) and equal
  `APPROVAL_REF`. Gating only the closed-ordinal case would let a solo
  verified outcome close the OPEN ordinal and book money with no
  approval at all. The closed-ordinal case is additionally the ONLY
  write admitted for a closed ordinal (§6). A solo
  `OPS_VERIFIED_OUTCOME_APPLIED` has no fold effect and is surfaced by
  the drift scan as an anomaly.
- **Opening amount = shortfall**: `REQUEST_OPENED` requires
  `:new.AMOUNT = REQUIRED_AMOUNT − PAID_TOTAL` on the
  transaction-fresh head (the standing rule pays the FULL remaining
  shortfall — v4 §6.8; both head fields were just witness-checked
  against the fold, and `RESERVED` is 0 with no open ordinal at
  opening time). Without this conjunct, a wrong decision could open
  for MORE than the shortfall and every downstream gate — reservation
  copy, key echo, terminal amount equality, close CAS — would
  faithfully carry the oversized amount to the wire and book it.
- **Amount equality (every outcome)**: `OUTCOME_RECORDED` of ANY code
  and `SETTLED` for the OPEN ordinal require `:new.AMOUNT = RESERVED`
  ("the request amount, restated" is an enforced equality, not a
  convention — a reject recording amount 999 for a 100 request is
  false history and is refused). Through the closed-ordinal door the
  comparison is instead against THAT ordinal's OPENING amount (one
  indexed read via `PE_ORDINAL_UQ` — NEVER against the payment-wide
  `RESERVED`, which may belong to a LATER open request). A differing
  feed amount is only insertable as `SETTLEMENT_MISMATCH_RECORDED`
  (routing enforced).
- **Key echo**: `POST_STARTED` / `POST_RESULT_RECORDED` /
  `QUERY_RESULT_RECORDED` require `:new.IDEMPOTENCY_KEY =
  OPEN_IDEMPOTENCY_KEY` — an attempt event can never cite a key other
  than its opening's, and a query result must name the key that was
  actually queried, so stale evidence for an EARLIER request's key
  cannot be recorded against the current one.
- **UETR-association binding (write-once per ordinal, first-claim
  global)**: THE ASSOCIATION RELATION is defined ONCE — UETR-bearing
  `PAYMENT_EVENT` rows ∪ permanent `RECONCILED_BY_KEY` rows (§7) —
  and EVERY consumer resolves UETR→ordinal through it: this binding,
  the no-op witness's ordinal resolution, and contradiction routing
  (a reconciled row consulted only as a first-claim VETO would leave
  the lost-UETR ordinal positively unresolvable — re-emissions would
  deadlock and the mandatory park would be unreachable for exactly
  the case reconciliation exists to serve). An event carrying a UETR
  must either MATCH its ordinal's existing association (so a
  resolver cannot re-attach ordinal 1's UETR to open ordinal 2 and
  let ordinal 1's stale feed reject release ordinal 2's
  reservation), or be the FIRST association of that UETR anywhere:
  no event row with this UETR exists on any OTHER payment (one probe
  on `PE_UETR_IX` — events are permanent, so the index IS the full
  history and a claim of an ABANDONED historical UETR dies at ITS
  commit, not as a later permanent matching ambiguity), no RECONCILED
  inbox association names it (§7 — the lost-UETR association lives
  ONLY there, which is why those rows are permanent), and no other
  ordinal of this payment carries it. `PH_UETR_UQ` (§5) stays as the
  belt for two SIMULTANEOUS first claims, which committed-data probes
  cannot see.
- **Release rights (the v4 release-guard trigger, transplanted)** —
  the release predicate is a CHECK, not a convention — and it means
  "provably NOT SUBMITTED", which is WIDER than "never sent"
  (inherited §6.4/§7.1: a synchronously business-rejected attempt is
  NOT_SUBMITTED and cancellable — demanding zero attempts wedged the
  mandatory cancel-after-reject flow and left a stale requirement
  retryable past its accepted cancellation):
  the predicate's FULL definition (defined here and in 01 §5; every
  other site must CITE it, never restate its arms — four rounds of
  drift came from restatements): ZERO `POST_STARTED` rows for the
  ordinal, OR (the LATEST `POST_STARTED` is followed by a
  same-ordinal `POST_RESULT_RECORDED(BUSINESS_REJECT |
  DEFINITIVE_REJECT)`, with no later attempt AND no acceptance-class
  row — `POST_RESULT_RECORDED(ACCEPTED)` /
  `QUERY_RESULT_RECORDED(ACCEPTED | EXECUTED)` /
  `FEED_RESULT_RECORDED(ACCEPTED)` — post-dating that latest
  `POST_STARTED`). The acceptance exclusion mirrors the downgrade
  gate's: key-scoped evidence is attempt-agnostic, so a delayed
  query ACCEPTED landing after a business-rejected attempt proves
  SUBMITTED and must revoke releasability — without it, a release +
  new-key successor could run against an acknowledged-accepted
  claim (inherited §9.4). `CANCELLED_NOT_SUBMITTED` requires this
  predicate; `REJECTED_VALIDATION` is
  admissible in TWO forms — pre-wire: no `POST_STARTED` plus its
  same-transaction `ENRICH_FAILED(DEFINITIVE)`; or post-wire
  synchronous: the unified predicate's second arm holds (latest
  attempt definitively rejected, no later attempt, no post-dating
  acceptance) with `DEFINITIVE_REJECT` specifically, where the CA-1
  classification of the definitive response is invalid-data (inherited §7.2: a synchronous engine invalid-data
  rejection releases and latches `validation_failed`, which is
  recoverable by strictly newer corrected truth — treating all
  validation rejects as pre-wire forced this case into
  `REJECTED_PROVIDER` and the WRONG marker); `SUPERSEDED_OPS`
  carries the same not-submitted predicate as
  `CANCELLED_NOT_SUBMITTED` — a claim that MAY have executed closes
  only on evidence or through the verified door;
  `REJECTED_PROVIDER` requires a same-ordinal first-party negative
  evidence row that POST-DATES THE LATEST ATTEMPT — evidence
  `VERSION >` the version of the ordinal's latest `POST_STARTED` —
  and only terminal-class evidence qualifies:
  `POST_RESULT_RECORDED(DEFINITIVE_REJECT)`,
  `QUERY_RESULT_RECORDED(REJECTED)`, or
  `FEED_RESULT_RECORDED(REJECTED)`; `BUSINESS_REJECT` NEVER
  qualifies (it is retry-class — its negative fact expires the
  moment a later attempt starts, and consuming it would let a lost
  response on attempt 2 be closed by attempt 1's stale reject).
  Without this set, one wrong "it was never sent" / "it was
  rejected" decision after a crash releases an executed request's
  reservation and the successor pays a second time.
- **Downgrade gate**: `DOWNGRADED_FOR_REPOST` requires a same-ordinal
  `QUERY_RESULT_RECORDED(NOT_FOUND)` row post-dating the latest
  `POST_STARTED` AND the absence of ANY acceptance-class row
  (`POST_RESULT_RECORDED(ACCEPTED)`,
  `QUERY_RESULT_RECORDED(ACCEPTED | EXECUTED)`, or
  `FEED_RESULT_RECORDED(ACCEPTED)`) post-dating that same
  `POST_STARTED` — NOT_FOUND recency alone proves recording order,
  not that no acceptance intervened; without the second conjunct a
  wrong decision downgrades an ACCEPTED request, which is the
  backward transition v4 forbids (the trust-age arithmetic stays
  fold policy; EVIDENCE EXISTENCE/ABSENCE is the trigger's part). Attempt attribution
  note (contract-backed): evidence is keyed, and the engine's
  collision/dedup contract (§18 keystone) means ONE key = ONE
  engine-side instruction — key-scoped evidence is therefore
  attempt-agnostic BY CONTRACT; there is no "stale attempt's
  evidence" class distinct from false provider evidence, which every
  design fails on equally.
- **Version continuity**: every insert requires `:new.VERSION =
  LAST_VERSION + 1`, and the per-event head effect sets
  `LAST_VERSION = :new.VERSION` — closing the skipped-slot gap the
  fence alone cannot see (a unique constraint rejects duplicates, not
  holes); the drift scan additionally asserts density
  (`COUNT(*) = MAX(VERSION) = LAST_VERSION`).

**Enforcement point (Oracle-real):** the `:new`-style predicates above
are the SPECIFICATION; the implementation is a COMPOUND trigger that
validates each inserted row at after-statement time against
`PAYMENT_EVENT` and `PAYMENT_HEAD` (raising aborts the transaction —
backstop semantics preserved). The per-event apply order means every
event arrives by its own single-row `INSERT ... VALUES` — where
Oracle's mutating-table restriction does not bite — and a statement
guard FORBIDS multi-row inserts into `PAYMENT_EVENT` so no path can
reintroduce it. Proving this behavior on real Oracle is checklist
item 5 evidence, not an assumption.

Full temporal legality beyond this set stays code-enforced (honesty
box, §10).

## 6. Contradictions and the unpark path — the L3 fix

`EVIDENCE_CONTRADICTION_RECORDED` has a FIXED fold effect: book
nothing, PARK the payment (no posting, no new opens), CRITICAL alert.
The ONLY exit is the dual-control verified-outcome operation (v4 §9.3
protocol verbatim — two-step approval, approval_id as the sole
execution input), which appends `OPS_VERIFIED_OUTCOME_APPLIED` +
`OUTCOME_RECORDED(PLATFORM_VERIFIED_*)` in one transaction. The fold
treats a verified outcome AFTER a contradiction as the authoritative
resolution: money books per the recorded outcome and the standing rule
resumes UNDER THE INHERITED GATES (markers, park consistency — see
correction mechanics below). Nothing else unparks a contradicted
payment.

**Correction mechanics (the closed-ordinal door).** The dual-control
pair is the ONLY write admitted for a closed ordinal (§5.3 exception —
without it, the design's sole unpark path would be rejected by its own
trigger). Fold rule: an ordinal's authoritative outcome is the LATEST
outcome-class event in stream order (§4), so the verified outcome
supersedes the wrong recorded one while history keeps both. Head
effect of a superseding verified outcome: `PAID_TOTAL` adjusted by the
signed difference (verified NOT_EXECUTED over a booked EXECUTED
subtracts the amount; verified EXECUTED over a booked reject adds it).
**The delta is pinned at decide time**: the prior side of the
difference is the authoritative outcome over events with `VERSION <`
the correction's version — i.e., the step-2 fold the decision was made
from — never re-derived after the insert (a post-insert "latest
outcome" lookup would see the correction itself and apply a zero
delta, silently splitting the bookkeepers). `RESERVED` untouched — the
ordinal stays closed. The same transaction then re-evaluates the
payment on the corrected numbers, in BOTH directions and under the
INHERITED gates:

- **Shortfall direction — markers gate it (v4 §9.3 inherited
  verbatim):** a verified NOT_EXECUTED latches the `provider_rejected`
  marker exactly as the baseline's platform-verified rejection does,
  so NO successor opens automatically in the correction transaction —
  re-payment requires the marker to unlatch (strictly newer upstream
  truth) or the explicit ops re-arm/clear path. An earlier draft of
  this section promised a same-transaction successor; that contradicted
  the inherited marker semantics and is retracted.
- **Excess direction — the correction may strand an open successor:**
  if the corrected numbers show `paid_total + reserved > required`
  and a request is open, the open request is now excess commitment.
  If it is provably NOT SUBMITTED (the UNIFIED §5.3 predicate,
  cited without restating its arms — an earlier draft restated a
  stale version here and a synchronously-rejected excess request
  parked forever), the same transaction closes it
  `CANCELLED_NOT_SUBMITTED`. Only a claim that
  MAY have executed keeps **the park in place**: the payment stays
  parked until that claim resolves through the ask path, and the §9
  pre-wire recheck (which sees the persisting park) blocks any
  not-yet-sent wire call. The unpark is then the resolution of that claim — never
  a state in which a corrected history and a live excess request run
  concurrently.

**Scope rule — contradiction means CONFLICT, not repetition.** Evidence
that AGREES with an already-recorded terminal outcome (the routine
case: feed settlement arriving for an ordinal already closed
`EXECUTED`-class with an equal amount, or a query re-confirming an
executed outcome) is a benign no-op delivery — inbox-deduped, nothing
appended, nothing parked. `EVIDENCE_CONTRADICTION_RECORDED` is
reserved for evidence that CONFLICTS: settlement after a
rejected-class terminal, an amount that differs, a query verdict
contradicting the recorded outcome. Without this rule the happy path
(executed-by-query, then the feed catches up) would CRITICAL-park
healthy payments.

## 7. Inbox semantics — the L7 fix

- **Feed deliveries (single-payment):** the inbox INSERT rides the
  SAME transaction as the resulting append (or the same transaction
  that decides "no-op": stale evidence). "Seen" and "processed" commit
  atomically; a crash before commit leaves neither. An UNMATCHED
  terminal delivery's inbox row additionally carries the EVIDENCE
  CONTENT (UETR, result class, amount, payload reference) — a status
  flag without the payload would be unresolvable by the sweep, so the
  content is SHAPE-BOUND, not conventional: a CHECK requires the
  evidence columns NOT NULL on UNMATCHED_TERMINAL rows (amount
  optional only for reject-class), the class drawn from the CLOSED
  vocabulary (`SETTLED` / `REJECTED` / `MISMATCH` — a misspelled
  class would be a permanently unroutable purge-exempt row), and NULL
  on plain PROCESSED rows. **Resolution (SIMPLIFIED by design — the
  correspondence-verification relation of two earlier revisions is
  RETRACTED):** three review rounds proved that a schema-verified
  correspondence between inbox evidence and specific stream events is
  a SECOND implementation of write-path legality — it refused legal
  parks, legal agreements, and legal rejections, and it could not see
  fold state without re-implementing the fold in a trigger, which is
  the duplicated-state-machine cost this design refuses everywhere
  else. The honest rule: the NORMAL write path (open-ordinal +
  contradiction routing + amount equality + release rights +
  UETR-association gates, under the head lock) is the SOLE legality
  authority for whatever the re-match decides — append, contradiction
  park, or benign no-op. The inbox row then closes as
  `RESOLVED_HANDLED` in the same transaction, recording an
  AUDIT POINTER: the payment key it was handled against and the
  head's `LAST_VERSION` at handling time. One honest correction to
  the retraction rationale (next round proved it overclaimed): the
  write-path gates validate the EVENT against the STREAM, but nothing
  in them sees the DELIVERY — class fidelity between what the feed
  said and what got recorded is real protection the old machinery
  provided. It returns as a DELIVERY-FIDELITY BACKSTOP of exactly two
  narrow, fold-state-free rules (none of the failed enumeration: no
  target lists, no amount arithmetic) — and the rules are UNIVERSAL:
  they bind EVERY feed-delivery transaction, matched-at-arrival and
  unmatched-flip alike (scoping them to the flip left the ordinary
  matched path free to record SETTLED as a rejection). TERMINAL
  deliveries therefore retain their evidence content on the inbox row
  in ALL statuses, including PROCESSED; the enforcement point is one
  compound trigger on the inbox write, reading its own transaction's
  appends — which requires a MANDATORY statement order: every decided
  event INSERT and its per-event head effect run FIRST, the terminal
  inbox INSERT/status flip runs LAST, then COMMIT (Oracle triggers
  fire at statement time, not commit time — an inbox-first order
  would make the trigger see zero appends, misapply the
  witnessless-no-op rule to a legitimate settlement, and wedge every
  redelivery; tolerating that would instead disable the check
  entirely):
  1. **No class inversion**: EV SETTLED forbids same-transaction
     rejected-class evidence/outcomes for the associated ordinal;
     EV REJECTED forbids executed-class/`SETTLED` bookings;
     EV MISMATCH forbids BOTH executed-class bookings AND
     rejected-class outcomes (its legal appends are
     `SETTLEMENT_MISMATCH_RECORDED` or a contradiction — this arm
     was missing, and a 90-mismatch could be laundered into a clean
     100-rejection). Contradiction events are ALWAYS admissible.
  2. **No witnessless no-op**: a terminal delivery handled with NO
     append requires that THE ASSOCIATED ORDINAL — the ordinal the
     delivery's UETR is bound to, resolved through THE UNIFIED
     ASSOCIATION RELATION of §5.3 (UETR-bearing events ∪ permanent
     `RECONCILED_BY_KEY` rows — an acceptance row suffices; requiring
     the UETR on the terminal itself refused the legal
     query-recovered outcome, and an events-only lookup here would
     make the reconciled ordinal unresolvable and loop re-emissions)
     — has an AUTHORITATIVE outcome (the §4 latest-outcome-class
     rule, NOT any historical event: a superseded `EXECUTED` under a
     later verified NOT_EXECUTED is precisely what must NOT witness
     a no-op) whose CLASS agrees with the evidence (executed-class /
     `SETTLED` for EV SETTLED; rejected-class for EV REJECTED; a
     mismatch row for EV MISMATCH) AND, for settled/mismatch
     classes, whose AMOUNT equals the evidence amount (§6 already
     declares a differing terminal amount CONTRADICTORY — a
     100-executed witness must not no-op a 90-settlement). One
     class-specific witness form: EV MISMATCH's witness is an
     existing same-ordinal `SETTLEMENT_MISMATCH_RECORDED` row of
     equal UETR and amount — mismatch rows are deliberately NOT
     outcomes (the request stays open, the payment parks), so an
     authoritative-outcome requirement would make the legal
     repeated-mismatch no-op unsatisfiable and loop the redelivery
     forever. Disagreeing evidence can never be silently no-opped
     past the contradiction park.
  The irreducible residue, stated honestly: TRANSCRIPTION fidelity —
  that the handler wrote the delivery's actual class/amount/UETR into
  the EV columns at all — is code: exactly the CA-1
  recorded-at-the-time class this design already inherits for
  provider-code mapping, under the same one-shared-implementation +
  golden-vector regime. The trigger checks action-vs-transcription;
  only the tested transcriber can vouch for transcription-vs-wire.
  Every other wrong handling decision is the same front-door decide()
  risk class as any write, mitigated by the same gates.
  The second exit, `RESOLVED_DISPOSED`, is for evidence that CANNOT
  be handled through a payment's write path, in two audited
  categories: `FOREIGN` (genuinely not ours) and `RECONCILED_BY_KEY`
  (the lost-UETR case: the response timed out, the key-query recovery
  carried no UETR, so no stream ever contains this delivery's UETR
  and no automatic match can ever succeed — a human reconciles it to
  the payment via the §9.1 query trail and records that payment key).
  `RECONCILED_BY_KEY` is an ACKNOWLEDGMENT that the money truth is
  ALREADY in the stream, never a substitute for putting it there —
  and the state gate has a REAL ENFORCEMENT POINT, not a prose
  predicate: the disposal transaction is a write-path citizen. The
  reconciliation names BOTH the payment key AND the request ordinal
  (the §9.1 query trail identifies the request, not just the
  payment — a payment-level check let ordinal 1's hidden settlement
  be acknowledged against ordinal 2's equal-sized execution). It
  takes `SELECT FOR UPDATE` on the NAMED payment's head, and under
  that lock the inbox compound trigger verifies: THE NAMED ORDINAL's
  authoritative outcome AGREES with the evidence (class and amount),
  and the head is not parked or quarantined. The head lock
  serializes the check against any concurrent verified-outcome
  correction; the explicit ordinal makes the human's association
  claim visible to the four-eyes approver, whose approval is bound
  to it (a wrong association now needs two people wrong about a
  stated, checkable claim — the same residue class as any §9.3
  action). The reconciled association is DURABLE AND GUARD-VISIBLE:
  `RECONCILED_BY_KEY` rows are PERMANENT (exempt from inbox purge —
  they carry a UETR→(payment, ordinal) association recorded NOWHERE
  else; purging one would un-forget the UETR), and both the §8
  feed-matching UNION and the §5.3 first-claim association probe
  include them as a source — a later platform reuse of a reconciled
  UETR dies loudly at its commit instead of silently booking a
  duplicate settlement against a successor ordinal.
  If the stream DISAGREES —
  the evidence says settled, the stream says rejected — disposal
  FAILS; the truth must first enter through the §6 dual-control
  verified-outcome door (booking the money and re-evaluating under
  the inherited gates), and only then may the delivery be
  reconciled. Approval CAS proves authorization; the locked state
  check proves the acknowledgment is true.
  Both categories: actor + reason + an approval through the INHERITED
  v4 §9.3 protocol — bound to exactly this (source, event id) and
  action, consumed APPROVED → CONSUMED by CAS in the same transaction
  (the columns are the echo; the approval-store CAS is the
  enforcement). PROCESSED, RESOLVED_HANDLED, and FOREIGN-disposed
  rows age out on the purge chain; inbox purge NEVER removes an
  UNMATCHED_TERMINAL row NOR a RECONCILED_BY_KEY row — the permanence
  rule stated above (an earlier draft's "all resolved rows age out"
  would have deleted the ONLY record of the lost-UETR association and
  let a re-emitted settlement book against the wrong payment).
  And the reconciled association gains a HEAD EFFECT so the
  simultaneous-first-claim fence covers it: the disposal transaction
  (already holding the named head's lock) sets that head's `UETR` to
  the reconciled UETR when the head's slot is NULL — a concurrent
  first claim of the same UETR on another payment then collides on
  `PH_UETR_UQ` at commit, exactly like every other channel. If the
  head slot is already owned by a successor's UETR, the residual
  concurrent window (until the reconciled row commits) resolves
  FAIL-CLOSED: the three-source UNION returns two payments, the
  delivery parks as a multiplicity anomaly, and dual-control
  adjudicates ownership — loud and blocked, never a silent booking.
- **Snapshot deliveries (multi-payment):** NO inbox row at all, and an
  EXPLICIT transaction boundary. ADMISSION is SERIALIZED at the trade:
  its FIRST action is `SELECT FOR UPDATE` (insert-on-first-contact) on
  the `TRADE_HEAD` row, held through the watermark comparison, the
  watermark update, AND every in-admission marker append — an
  unserialized read-compare-then-update lets two concurrent valid
  arrivals (200 and delayed 150) both judge themselves newer, commit
  in either order, REGRESS the watermarks, and hand the equality
  fence to the stale snapshot, which then posts an amount the newer
  truth cancelled. The lock order is `TRADE_HEAD → PAYMENT_HEAD`
  (sorted) for admission exactly as for fan-out. Under that lock,
  admission compares the arrival against the RIGHT watermark: acceptance is judged against
  `LAST_ACCEPTED_SEQ` — a VALID snapshot strictly newer than accepted
  truth is ADMITTED even when an even-newer INVALID snapshot was
  already seen (inherited §6.6: valid 150 after invalid 200 over
  accepted 100 must land; comparing validity arrivals against SEEN
  discarded the cancellation and let a stale requirement post) —
  while seen-ness advances `LAST_SEEN_SEQ` monotonically. The
  admission updates the accepted pair on validity, the seen pair when
  newer-than-seen; ANY watermark change re-triggers the
  full-current-state fan-out, whose fence token is the
  `LAST_SEEN_SEQ` current at worklist derivation (after admitting
  valid 150 under seen 200, the fan-out carries token 200 and applies
  accepted truth 150 — and ONLY accepted truth: the invalid-200
  markers were already appended inside seq-200's own admission
  transaction over its stamped set, and the fan-out never applies
  markers, per the rule below). FAN-OUT then runs as separate per-payment
  transactions, each of which (1) locks `TRADE_HEAD` (the v4 lock
  order), (2) verifies its carried snapshot seq still EQUALS
  `LAST_SEEN_SEQ` — the **equality fence**: if a newer arrival owns
  the trade, abort — then (3) locks the payment head and appends
  seq-guarded. **The current owner fans out the FULL current trade
  state, not just its own document** (this is what makes "the newer
  fan-out covers everything" true): per payment, FIRST the accepted
  truth's `REQUIRED_AMOUNT_SET` from the STORED accepted snapshot
  (seq-guarded — catching up any accepted admission whose own fan-out
  was fenced out, including cancels-to-zero), THEN, if the seen
  snapshot is invalid (`LAST_SEEN_SEQ > LAST_ACCEPTED_SEQ`),
  nothing — because invalid markers are NOT this fan-out's job: the
  `SNAPSHOT_INVALID_MARKED(seq)` appends happen INSIDE the
  seen-admission transaction itself, atomically with the watermark
  update, over exactly the set knowable at that moment (payments
  whose heads exist ∪ the invalid document's canonically extractable
  keys, heads locked in sorted key order — the v4 lock order; the
  set is small at this volume). The appended stream rows ARE the
  durable record — no sidecar set, no resume rule, and a crash rolls
  the whole admission back for redelivery to re-run. A payment FIRST
  INTRODUCED by later-admitted valid truth the invalid document
  never named therefore never inherits the marker (inherited §6.6 —
  a deferred blanket marker blocked such a payment behind an unlatch
  bar of `> LAST_SEEN_SEQ` that no necessary future snapshot would
  ever clear, and a deferred SET had no durable home). The
  current-state fan-out carries ACCEPTED truth only. An
  accepted-catch-up skip would starve an already-accepted
  cancellation behind the fence and let a cancelled payment post.
  Worklist = payments named in the stored ACCEPTED snapshot ∪ existing
  head rows of the trade (the extractable-key branch moved INTO the
  seen-admission transaction with the markers themselves: the
  distrust rule bars an invalid document from supplying MONEY truth,
  not from anchoring visibility — a first-delivery-invalid trade
  gets its head + `SNAPSHOT_INVALID_MARKED` at admission, so the
  card shows the inherited validation-failed state instead of
  NOT_STARTED; such a head has NULL required amount, so nothing can
  open from it). Resume after a crash re-derives the worklist from
  the CURRENT watermarks' stored state, never from an in-memory
  snapshot, so a stale resumed worker can neither create nor touch a
  payment from superseded trade truth. Kafka ack only after fan-out
  completes; redelivery re-runs and converges. Side effects (metrics,
  alerts) key on state CHANGES (an append that actually happened), so
  re-runs do not re-fire them.
- Retention: inbox purge > Kafka retention ≥ replay window (owner
  rule inherited from v4 §16.2).

## 8. Read surfaces — the L8 fix

- **Step card (obligation-granular):** reads `PAYMENT_HEAD`
  (`UI_STEP_STATUS`, exception summary via phase) — display-only, and
  the head is transaction-fresh. Lookup by BUSINESS_ID returns all of
  a trade's payments; row absence = NOT_STARTED.
- **All-payments table (request-granular):** a read-only SQL VIEW over
  `PAYMENT_EVENT` — one row per `REQUEST_OPENED` (request identity =
  payment_key + ordinal), outcome joined by ordinal, obligation-only
  placeholders from head rows with `NEXT_REQUEST_ORDINAL = 1`. The
  amount series is free: `REQUIRED_AT_OPEN` is stamped on the opening
  event (immutable by nature — no set-once discipline needed, no F0
  capture-boundary machinery). Keyset pagination on
  (payment_key, version). No projection, no maintenance job.
- **Scanners/resolver:** select CANDIDATES from the head
  (`PHASE, NEXT_ACTION_AT`); every ACTION folds the stream under the
  head lock. A stale candidate costs a wasted fold, never a wrong
  payment — and the head cannot be stale by more than an uncommitted
  transaction (§5).
- **Feed matching multiplicity (explicit, counted in PAYMENTS not
  rows, over the UNION of all THREE sources):** the candidate set is
  DISTINCT `PAYMENT_KEY`s from head matches ∪ event-index matches ∪
  RECONCILED inbox associations (`RECONCILED_BY_KEY` rows, §7 — the
  only place a lost-UETR association exists) — ALL always consulted
  (a head-first shortcut would return one head match and never see
  that another source names a DIFFERENT payment for the same UETR,
  silently booking the wrong one; three indexed queries are trivial
  at this volume). One payment carrying the same
  UETR on several events resolves as ONE candidate. 0 payments →
  unmatched path: the inbox row is written with
  `MATCH_STATUS = UNMATCHED_TERMINAL` — a DURABLE fact, so paging is
  LEVEL-TRIGGERED from data (a sweep re-matches and pages while any
  such row exists; a crash between commit and page loses nothing,
  and a later-created head is found by the re-match), never an
  edge-triggered side effect that a crash can drop; 1 → lock that
  payment, then RE-RUN the union query INSIDE the transaction before
  deciding (the initial lookup is unserialized check-then-act — a
  concurrent acceptance claiming the same UETR on another payment
  commits between lookup and lock, and booking on the stale set
  would seal the wrong payment under the inbox); recheck still 1 →
  fold + append; recheck 2+ → abort, CRITICAL, nothing committed,
  redelivery retries. 2+ DISTINCT payments at any point → CRITICAL
  anomaly, no state change. The post-recheck race is caught BY THE
  SCHEMA, not by a promise: `PH_UETR_UQ` (§5 — one HEAD per UETR,
  every association channel funnels through the head effect) makes
  the competing same-UETR claim die loudly at ITS commit with a
  constraint failure + CRITICAL page. UETR uniqueness is a platform
  contract fact (§18 class); its violation is now a loud event,
  never a silent double-booking.
- **Deferred, exactly as the baseline itself defers it:**
  `PROVIDER_REFERENCE` fallback matching (inherited §8's
  single-active-match branch). A UETR-less delivery lands
  `UNMATCHED_TERMINAL` and recovers through the key-query path — a
  bounded latency regression, never a safety gap (round-32 review,
  accepted at MEDIUM; the baseline's own deferrable list carries the
  same item).

## 9. Operational inheritances (unchanged from v4, restated as binding)

Posting freeze in Hazelcast (outside the DB; absent = frozen);
write-ahead rule (identity + payload hash durable before the wire —
here structural: `POST_STARTED` IS the durable claim, and the release
predicate is the UNIFIED §5.3 "provably NOT SUBMITTED", cited without
restating its arms) — with
one mandatory addition: between the COMMIT of `POST_STARTED` and the
wire call the worker re-reads the head (no lock) and SKIPS the send if
the payment is parked/blocked, in WITNESS_DIVERGED quarantine (a
diverged payment must not reach the wire on a claim decided from
disputed numbers), or the ordinal is no longer open; the
committed claim then resolves through the standard §9.1-style ask path
under the park (it fails the unified provably-NOT-SUBMITTED
predicate — a claim with no closing synchronous result — so it is
never released —
only asked about). This narrows the irreducible commit-to-wire window
to the recheck-to-send gap (honesty box item 7);
evidence precedence and release rights (§9.4/§10.1 semantics live in
the fold, golden-vector tested); trust-age / downgrade / escalation
clocks (event timestamps are the episode anchors — set-once by
immutability, the v4 clock-discipline problem disappears); engine
collision contract as the keystone, proven by the §18 item-1 sandbox
test before go-live.

**Permanence (the archival decision — REMOVAL, not machinery):**
`PAYMENT_HEAD`, `TRADE_HEAD`, AND `PAYMENT_EVENT` rows are all
PERMANENT for the system's operational lifetime. An earlier revision
designed event-row archival with finality windows and rehydration;
two review rounds then showed the mechanism itself breeds
CRITICAL-class defects faster than it can be fenced (archived
`IDEM_CLAIM` rows leave the unique index and un-forget burned keys;
rehydration cannot pass the version-continuity trigger it must use;
the required evidence-lateness eligibility bound is not an obtainable
contract fact; archived requests vanish from the view). The honest
remediation is removal: at ~3,000 trades/day the event table grows by
well under a million rows a year — decades of headroom for Oracle
with `PE_KEY_IX` — so nothing forces rows out of the table.
Storage tiering, if ever wanted, is PARTITIONING that never removes
rows from the table or its global unique indexes (the claims, fence,
and view keep working unchanged) — with the maintenance rule stated,
because Oracle partition operations mark global indexes UNUSABLE by
default: every partition DDL MUST use index-maintaining form
(`UPDATE INDEXES` / online operations), and writes may resume only
after verifying every `PE_*` unique index reports VALID — an
unusable claims index is a global liveness outage, checklist-5
evidence territory. Compliance-deletion pressure lands
on the PII VAULT (00-README item 6: no erasable PII in events, ever),
never on event rows. The heads never forget, the claims never leave
the indexes, and the entire restore/matching story needs no
archive-aware branch.

## 10. Honesty box v2 — what remains accepted, with mitigations

1. **Full temporal legality is still code-enforced** beyond the §5.2/
   §5.3 backstops. Mitigation: the backstopped subset covers the
   money-moving transitions; the rest is fold logic under golden
   vectors. Residual risk: a fold bug producing a legal-shaped but
   semantically wrong event sequence. Comparable in kind to a wrong
   CAS in v4; caught by the same class of tests.
2. **Control-state reinterpretation on fold change** (marker liveness,
   retry pacing — not money, per §4). A fold deploy can change how
   existing streams BEHAVE next. This is the same risk as any code
   deploy in v4; the §4.2 deploy gate bounds the money side to zero.
3. **The fold is a single point of interpretation.** Deliberate — it
   is also the single place to test. The witness (§5), the deploy gate
   (§4.2), and the DB backstops (§5.2/5.3) are the independent checks.
4. **Event-table growth.** Rows are PERMANENT by design (§9 — the
   archival mechanism was removed after review proved it a defect
   factory; partitioning that never leaves the indexes is the only
   sanctioned tiering). Under a million rows a year at this volume:
   decades of headroom. The accepted residue is that this design
   answer is volume-dependent — a future 100× volume would reopen
   the question, and would have to solve the archived-claims /
   rehydration problems this design deliberately refused to carry.
5. **Everything still stands on the engine collision contract** —
   exactly as v4 does. §18 BLOCKING item 1 gates go-live for this
   design identically.
6. **Ops learning curve:** incident diagnosis reads the head for
   "where is it" and the stream for "why" — the stream IS the
   transition history v4 lacks locally, but the team must learn to
   read it. A `fold --explain <payment_key>` debug tool (prints the
   stream and the fold's state after each event) should ship with the
   MVP; it is the event model's answer to "SELECT the state" and it
   must be treated as a deliverable, not a nice-to-have.
7. **The commit-to-wire window.** A park or contradiction committed
   after `POST_STARTED` commits but before the wire call cannot
   retract the claim — identical in kind to v4's write-ahead window.
   The mandatory pre-wire head recheck (§9) narrows it to the
   recheck-to-send gap; the residual is an executed request whose
   outcome is still truthfully recorded and then reconciled through
   the §6 dual-control flow. No write-ahead design closes this window
   from the database alone — claiming "park = no posting, absolutely"
   would be dishonest, so the claim here is "park = no NEW posting
   decisions + in-flight sends re-checked at the last possible
   moment."

## 11. Why this version is presentable

Every item the v1 draft itself ranked CRITICAL or HIGH is closed by a
mechanism, not by a promise: identity survives restore because it no
longer depends on stream position; money cannot be silently
reinterpreted because money is recorded, witnessed twice, and
deploy-gated; the killer invariant has a fold-independent CAS + trigger;
freshness is transactional; the inbox is atomic; the UI needs no
second projection. What remains in §10 is the honest residue every
design carries — and for each item, the v4 baseline has a directly
comparable counterpart risk. The comparison between the two designs
is now a fair fight between representations, not between a finished
design and a draft.
