> **Purpose:** Task cards IN-01..IN-09 (upstream intake, ordering guard, anchors, markers, feed consumption, Kafka hardening) (original Section H, phase P9).
> **When to use this file:** When executing the tasks of this phase, one card at a time, with the matching packet file from 09-minimal-context-packets/.
> **Depends on:** 08-task-cards/README.md; 01-playbook-index.md; 07-placeholder-glossary.md; the requirement sections cited per card; the locally filled mapping template.
> **Used by:** The local coding agent executing phase P9.
> **Safe to transfer:** yes
> **Contains local code names:** no

## H-Phase 9 — Inbound flows and status feed (P9)

### IN-01 — Upstream message validation + contract enforcement

- **Task ID:** IN-01
- **Title:** Validate the §6.0 message contract at intake (incl. currency-scale); wire build-time schema enforcement
- **Classification:** MVP normative implementation
- **Purpose:** §6.0's field set is one of the three build-time-enforced contracts (§16.5); §16.4 scale validation happens here.
- **Prerequisites:** S-02; D-07 (consumer mapped); upstream ask 3 (schema formalization) — proceed on the observed schema, mark deltas UNCLEAR.
- **Requirement sections / concepts to read:** §6.0 (fields + payload-equality definition + emission contract fact), §16.4 (scale), §16.5 (contracts), §6.6 (failure routing).
- **Placeholder components involved:** upstream consumer (maps via [Obligation Repository] flow), [Contract Test Suite].
- **Local placeholder mappings required before starting:** upstream consumer Confirmed.
- **Local code areas to discover:** current intake validation.
- **How to locate:** D-07.
- **Implementation instructions:** validate presence/typing of: business_id (also the Kafka key — observe and record whether it is; feeds upstream ask 2), scope fields, required_amount positive + currency-scale-valid (JPY 0 / BHD-KWD 3 — §16.4), ordering value, trade reference, ui ids, correlation_id; implement the canonical payload-equality function over the CANONICALIZED BUSINESS-FIELD SUBSET (scope + required_amount + trade reference — §6.0, used by §6.7 ties); failures route per §6.6 (IN-03); wire schema enforcement (registry or consumer-driven test) so a contract change fails the build.
- **Do not change:** message semantics; upstream topics.
- **Tests to add:** field-validation cases; scale cases (100.555 in 2-dec → reject, never round); payload-equality (envelope fields excluded — redelivery is a tie-IDENTICAL).
- **Edge cases:** absent/zero BLOCK amount → reject (BA-2 context: a present block's amount is strictly positive; required = 0 is writable ONLY by the §6.1 absence path); an EMPTY derived payment set is the DIFFERENT, VALID case (round 11, §6.0 role derivation — zero payments for us, still admitted, never a validation failure).
- **Manual validation:** seeded valid/invalid messages locally.
- **Expected outcome:** contract-guarded intake.
- **Failure signs:** silent rounding anywhere.
- **Common mistakes:** payload equality over raw bytes (every redelivery becomes a false tie-conflict — §6.0 warns).
- **Completion criteria:** tests green.
- **Stop condition:** merged.
- **Next task:** IN-02.

### IN-02 — Snapshot admission + obligation upsert + §6.7 ordering guard

- **Task ID:** IN-02
- **Title:** Trade-level snapshot ADMISSION (round 5); snapshot fan-out; locked obligation upsert; strictly-newer ordering mutation; tie handling; stale counting
- **Classification:** MVP normative implementation
- **Purpose:** §6.1/§6.7/§2.4: a message is a FULL-TRADE SNAPSHOT that must pass the trade-level ADMISSION gate before ANY per-block work (round 5: per-obligation watermarks cannot stop a stale snapshot from CREATING a never-seen scope), then fans out to one application per payment block; a redelivered older message must never regress required_amount; ties are digest-detected at admission; the comparison is one pluggable point (future explicit sequence, upstream ask 1).
- **Prerequisites:** IN-01; S-02; S-10 (trade_snapshot_state); B-01 RESIDUE (upstream asks 5 + 8 CONFIRMED 2026-07-11, WRITTEN docs pending — §18-0(a)/(d): the freeze needs the filed paper, the design questions are settled; PO-9 ANSWERED — absence = amendment to zero; TL-16 ANSWERED round 5 — the admission gate below).
- **Requirement sections / concepts to read:** §1 contract facts (trade-payment cardinality), §2.4 (trade_snapshot_state), §6.0 (snapshot shape + within-snapshot uniqueness validation), §6.1 (ADMISSION + fan-out + convergence + the RESOLVED absence block), §6.7 (whole), §6.9 (required_amount row).
- **Placeholder components involved:** [Obligation Repository].
- **Local placeholder mappings required before starting:** obligation upsert path.
- **How to locate:** F.1.
- **Local code areas to discover:** current amount-update path.
- **Implementation instructions:** validate the snapshot ONCE (schema, amounts, within-snapshot tuple uniqueness → whole-snapshot validation failure per §6.0/§6.6); then ADMISSION in its own transaction (§6.1, round 5): upsert-lock the trade_snapshot_state row (INSERT on first contact, PK race → retry; SELECT FOR UPDATE) — doc ordering NEWER than last_accepted_ordering → admit + update row (ordering, xml storage id, canonical digest) + commit; EQUAL with digest EQUAL → admit WITHOUT update (redelivery/re-run); EQUAL with digest DIFFERING → AMENDMENT_TIE_CONFLICT alert, NO application, NO creation; OLDER → refuse WHOLE document (stale metric — NO new scope is ever created from a refused document); only THEN fan out per payment block in deterministic tuple order (fixed lock order); per block (round 6 — the TRADE-SNAPSHOT FENCE, not optional; renamed round 7, never "currency check"): lock the trade row FIRST (SELECT FOR UPDATE), re-verify the admitted (ordering, digest) still match — on mismatch STOP the fan-out (§6.1 BLOCK-LEVEL SUPERSESSION: the newer snapshot supersedes the unapplied remainder; log each abandoned block's scope identifiers + metric; NEVER run the check without holding the lock in the same transaction) — then, under that obligation's lock (global order trade → obligation → request): upsert by scope key (ORA-00001 → retry + re-read, §6.1); mutate required_amount + advance upstream_ordering ONLY if message ordering strictly newer; else count stale (metric) and drop; ack the Kafka record ONLY after the fan-out completes; ordering comparison isolated behind ONE pluggable comparator shared by admission and blocks (business timestamp today; explicit sequence later — no logic change on cutover); after application → RG-06 evaluation (T1: even without amount change). ABSENT payments (PO-9 ANSWERED 2026-07-11 — §6.1 RESOLVED block): after the per-block fan-out, enumerate the trade's obligations NOT in the document; per obligation (own tx, trade-snapshot fence + obligation lock, strictly-newer guard) set required_amount := 0 AND upstream_ordering := doc.ordering (round 11: the zeroing IS an application of the document — it advances the watermark) — then existing machinery: §6.4 auto-cancel (unsent), wait-then-decide (in-flight), §6.5 overpay latch = STOP (confirmed > 0); a cleanly unwound row derives the §4.1 CANCELLED terminal branch (displayed CANCELLED, never COMPLETED); §6.6 anchors are zeroed ONLY when doc.ordering > validation_failed_ordering (ordering-aware retirement, round 11); an EMPTY derived payment set (§6.0 role derivation) is a VALID admitted snapshot whose fan-out is pure absence — this is how removal of the trade's ONLY payment is represented; EVERY absence fan-out that zeroes ≥ 1 obligation emits the §15 disappearance log+metric (business_id, zeroed scope tuples, doc.ordering).
- **Do not change:** BA-3 stance — no compensating ordering machinery beyond §6.7 + the §2.4 admission row; lock order is ALWAYS trade row first, then obligations in tuple order.
- **Tests to add:** the §6.7 failure trace (late original must not regress 120→100); strictly-newer applies; equal-older counted+dropped; both tie branches at admission (digest equal → silent convergent re-run; digest differing → tie alert, nothing applied); T1 fires on ordering advance without amount change; snapshot fan-out: two-block snapshot updates two obligations; new-tuple block creates its obligation; within-snapshot collision → whole-snapshot validation failure; crash mid-fan-out + redelivery converges (admission re-admits ==/equal-digest; applied blocks drop stale, unapplied apply); absence set (PO-9): absent + unsent active request → auto-cancelled + released; absent + confirmed > 0 → overpay latch fires, no clawback; absent + in-flight MAYBE → parked wait-then-decide; absent anchor scope: document NOT strictly newer than the failure marker → untouched, document strictly newer than validation_failed_ordering → retired (zeroed, watermark advanced, derives CANCELLED); removed payment REAPPEARS in a strictly newer snapshot → reopens (required := new positive value, IN_PROGRESS); empty derived set admitted → all non-anchor obligations zeroed + §2.4 row advanced; absence write respects the strictly-newer guard (a redelivered old snapshot cannot zero a newer amount); every zeroing fan-out emits the disappearance log+metric; the artifact-6(f) admission set: NEVER-SEEN-SCOPE trace (newer snapshot without B first; delayed older with B → refused whole, B never created, no request ever exists), two disjoint first snapshots serialize on the trade row, failed-validation message advances NEITHER watermark; the T-37 absence-lifecycle set is BLOCKING for this card.
- **Edge cases:** first message (no trade row, NULL stored ordering) admits + applies; failed-validation messages never advance ordering (IN-03's rule — assert here too, for BOTH the trade row and obligations); the old TL-16 stale-amounts trace (delayed-older-snapshot-containing-an-absent-payment) — now refused at admission, assert it.
- **Manual validation:** seeded out-of-order sequence incl. one delayed-older snapshot carrying a never-seen scope.
- **Expected outcome:** regression-proof amounts AND creation-proof stale snapshots.
- **Failure signs:** amount writes outside the comparator's gate; ANY obligation insert reachable without an admitted document.
- **Common mistakes:** >= instead of strictly-newer; tie-differing silently dropped ("upstream resends" is NOT a recovery for ties — §6.7); creating a scope in a block transaction WITHOUT locking the trade row and passing the fence in that SAME transaction (check-then-act — the round-6 race); implementing the fence as ISO-currency validation (it is snapshot CURRENTNESS — round-7 rename exists precisely for this); advancing the trade row inside a block transaction; acking the Kafka record before the last block commits.
- **Completion criteria:** tests green.
- **Stop condition:** merged.
- **Next task:** IN-03.

### IN-03 — Validation-failure anchors + DLT routing

- **Task ID:** IN-03
- **Title:** Create anchor obligations for failed-validation messages with extractable scope; DLT for unidentifiable ones
- **Classification:** MVP normative implementation
- **Purpose:** §6.6: the durable anchor readers query; failing ordering recorded on the marker but upstream_ordering NOT advanced; card shows the problem.
- **Prerequisites:** IN-02. Marker-write helper: implement it HERE as the single shared helper (DECIDED 2026-07-11 — this is not a coordination point); IN-04 then EXTENDS it with monotonic re-tag coverage and counters. Do not wait for IN-04.
- **Requirement sections / concepts to read:** §6.6 (normal anchor path; key-only anchoring is NOT in scope — TL-7 future), §2.1 (validation_failed fields), §4.1 (anchor completion impossibility).
- **Placeholder components involved:** [Obligation Repository], DLT wiring.
- **Local placeholder mappings required before starting:** upstream consumer + DLT (D-07).
- **Local code areas to discover:** DLT publish path.
- **How to locate:** D-07.
- **Implementation instructions:** validation failure with extractable scope + ui_process_instance_id → upsert anchor: required_amount NULL, ui_step_status IN_PROGRESS, DATA_VALIDATION_FAILED (retryable=false) via the marker (validation_failed_at/_ordering = failing message's ordering; first_at + count per §2.1); upstream_ordering untouched; too-malformed-to-identify → DLT + ops alert (accepted blind spot).
- **Do not change:** scope of key-only anchoring (tiers 2–3 of §6.6 are TL-7 future — do NOT implement).
- **Tests to add:** anchor created with NULL amount; §4.1 cannot complete it; later valid message populates + clears liveness + creates first request (via RG-06); DLT on unidentifiable.
- **Edge cases:** repeat failing messages → monotonic marker re-tags + count increments (validation_reject_count alert ≥3 is OB-04's — the counter behavior lands with IN-04).
- **Manual validation:** card shows the anchor's exception locally.
- **Expected outcome:** no invisible NOT_STARTED for broken messages with readable scope.
- **Failure signs:** anchors advancing upstream_ordering (poisons §6.7).
- **Common mistakes:** completing anchors via a missing predicate guard (RG-08's terms exist for this).
- **Completion criteria:** tests green.
- **Stop condition:** merged.
- **Next task:** IN-04.

### IN-04 — Monotonic marker writes + counters

- **Task ID:** IN-04
- **Title:** Implement §6.9's marker write/read discipline: monotonic ordering-tagged writes, liveness predicates, reject counters
- **Classification:** MVP normative implementation
- **Purpose:** one staleness guard per mutable input (P3): stale replays cannot poison markers; provider_rejected gains ops-only clearing from the second reject.
- **Prerequisites:** S-02 (columns); shared helper consumed by IN-03, RC-01/RC-02, IN-07.
- **Requirement sections / concepts to read:** §2.1 (both marker blocks + counters + first_at), §6.9 (write AND read rules), §19.3 (ops clear — future; only the counter reset contract matters now).
- **Placeholder components involved:** [Obligation Repository].
- **Local placeholder mappings required before starting:** none new.
- **Local code areas to discover:** none new.
- **How to locate:** n/a.
- **Implementation instructions:** one marker-write helper per marker: overwrite only if new ordering strictly newer than stored marker ordering (stale write → dropped + counted metric); validation_failed_first_at set on not-live→live transition, untouched by re-tags, cleared when marker goes not-live; counters increment per set (validation_reject_count resets when marker clears; provider_reject_count resets only by the future §19.3 ops clear — no auto-reset); liveness predicates per §2.1: validation_failed LIVE iff ordering >= upstream_ordering OR upstream_ordering IS NULL; provider_rejected LIVE iff ordering >= upstream_ordering OR count >= 2.
- **Do not change:** overpay_blocked (deliberately un-gated — §6.9).
- **Tests to add:** monotonic overwrite; stale write dropped+counted; liveness both markers (incl. the anchor clause and the count>=2 persistence against newer messages); first_at semantics (never refreshed by re-tags); counter resets.
- **Edge cases:** marker set by enrichment reject tagged with creating_ordering (§7.3) vs by message validation tagged with message ordering — same helper, caller supplies the tag.
- **Manual validation:** seeded replay sequences.
- **Expected outcome:** marker discipline exact.
- **Failure signs:** any un-tagged marker write.
- **Common mistakes:** re-tagging refreshing first_at (kills the §15 age alert — spec explains).
- **Completion criteria:** tests green.
- **Stop condition:** merged.
- **Next task:** IN-05.

### IN-05 — Feed consumption transaction order

- **Task ID:** IN-05
- **Title:** Rebuild feed consumption to §8's exact order: inbox insert → resolve → evidence CAS → amounts on row-count 1 → re-derive → commit → ack
- **Classification:** MVP normative implementation
- **Purpose:** §8: inbox stops identical redeliveries cheaply BEFORE locks; evidence rules protect the money; offsets commit after DB commit.
- **Prerequisites:** S-04 (inbox), ST-02 (CAS), RG-02/03 (money), IN-04.
- **Requirement sections / concepts to read:** §8 (consumption transaction + layering), §16.2 (ack semantics), §4.4.
- **Placeholder components involved:** [Payment Status Feed Consumer], [Inbox / Processed Event Repository], [Request Status Persistence Layer], [Reservation Repository].
- **Local placeholder mappings required before starting:** consumer + inbox Confirmed.
- **Local code areas to discover:** listener transaction boundary.
- **How to locate:** D-07.
- **Implementation instructions:** per event: (1) INSERT inbox — duplicate key → return (no locks); (2) resolve request (UETR primary; fallback per IN-06); no match → log(event_id, UETR, status)+count+ack+drop; (3) obligation lock → evidence-guarded CAS (IN-07's rules) → amounts on row-count 1 → re-derive; (4) commit, THEN ack.
- **Do not change:** topic/partition setup; evidence rules are IN-07's (this task is the SKELETON order).
- **Tests to add:** duplicate event_id short-circuits pre-lock; concurrent in-flight duplicate (rebalance mid-poll): second blocks on the row lock then duplicate-keys after first commits (§8 explicit test); ack strictly after commit (failure between → redelivery reprocesses safely); unmatched path.
- **Edge cases:** crash after commit before ack → redelivery hits inbox duplicate → clean skip (assert).
- **Manual validation:** local feed run with induced redeliveries.
- **Expected outcome:** §8 skeleton exact.
- **Failure signs:** locks taken before the inbox insert.
- **Common mistakes:** acking in a listener error handler before commit.
- **Completion criteria:** tests green.
- **Stop condition:** merged.
- **Next task:** IN-06.

### IN-06 — Unmatched events + provider_reference fallback

- **Task ID:** IN-06
- **Title:** Implement the fail-closed provider_reference fallback and the unmatched-event policy
- **Classification:** MVP normative implementation
- **Purpose:** §8: reference uniqueness UNCONFIRMED (TL-12) — fallback only on exactly ONE ACTIVE match + amount equality + recency window; zero/multiple → unmatched path; a mis-match is a double-pay or an unguarded reject.
- **Prerequisites:** IN-05; U-02.
- **Requirement sections / concepts to read:** §8 (fallback block + rationale), §16.6 (recency-window config).
- **Placeholder components involved:** [Payment Status Feed Consumer].
- **Local placeholder mappings required before starting:** IN-05 skeleton.
- **Local code areas to discover:** none new.
- **How to locate:** n/a.
- **Implementation instructions:** resolution: UETR match first; else provider_reference candidates filtered to ACTIVE + amount-equal + within recency window; exactly one → use; zero/multiple → unmatched (log + metric + ack — §9 recovers by key); NO durable record, NO replay (decided — §8); no parked-event table (SPEC_CONFLICT trap).
- **Do not change:** the decided no-parked-event stance.
- **Tests to add:** single-match fallback works; two candidates → unmatched; amount-unequal → unmatched; outside recency → unmatched; unmatched logged+counted+acked.
- **Edge cases:** §5.2 replay-window CRITICAL exception for unmatched events is POST-MVP (runbook-time); leave a named hook comment only if trivial, else nothing.
- **Manual validation:** seeded fallback scenarios.
- **Expected outcome:** fail-closed matching.
- **Failure signs:** fuzzy matching creep (business_id or amount-only matching — forbidden).
- **Common mistakes:** counting terminal rows as candidates (ACTIVE only).
- **Completion criteria:** tests green.
- **Stop condition:** merged.
- **Next task:** IN-07.

### IN-07 — Evidence application rules

- **Task ID:** IN-07
- **Title:** Implement §4.4/§10.1 evidence application: terminal → any ACTIVE row; intermediate → non-CLAIMED only; stale → zero rows
- **Classification:** MVP normative implementation
- **Purpose:** the correctness layer money is protected by (§8 layering); shared by feed (here) and resolver (RC-06).
- **Prerequisites:** IN-05 skeleton; RG-03 settlement helper; RG-05 guard; IN-04 markers; CA-2 (status ranks).
- **Requirement sections / concepts to read:** §4.4, §10.1 (terminal-evidence + mirror rules), §8 (marker totality, negative handling), §9.4.
- **Placeholder components involved:** [Payment Status Feed Consumer], [Request Status Persistence Layer].
- **Local placeholder mappings required before starting:** CA-2 rank mapping available.
- **Local code areas to discover:** none new.
- **How to locate:** n/a.
- **Implementation instructions:** one evidence-application helper (feed + resolver share it): TERMINAL settlement → RG-03 helper (any active row; sets SUBMITTED, L4); TERMINAL reject → outcome=REJECTED CAS + evidence flag (S-06) + provider_rejected marker in the same transaction (totality — §8) + release (RG-02); INTERMEDIATE acceptance → non-CLAIMED rows only: SUB=SUBMITTED, stage=CONFIRM, stage_state=READY (clear next_retry_at), BLOCKED preserved (CONFIRM·BLOCKED legal, L5); CLAIMED → no-op; anything stale/duplicate → CAS row-count 0 → ignored; NEW event_id + zero-row CAS on a TERMINAL row → CRITICAL anomaly alert (§8); return/refund-style event for EXECUTED → log + CRITICAL + ack, NO state change (§19.2 context).
- **Do not change:** ui_step_status (derivation only — §4.1).
- **Tests to add:** each rule; feed-races-own-response (both orders — second affects 0 rows, §8/§10.1 mirror); reject sets marker in same transaction; anomaly alert on terminal-row settlement; BLOCKED preserved on acceptance.
- **Edge cases:** settlement on a BLOCKED row → EXECUTED + confirmed (late feed settlement row, §10.5).
- **Manual validation:** seeded event sequences.
- **Expected outcome:** evidence machinery correct + shared.
- **Failure signs:** evidence rules weakened "because the inbox dedups" (§8 forbids).
- **Common mistakes:** applying intermediate evidence to CLAIMED rows; forgetting SUBMITTED-tightening on settlement.
- **Completion criteria:** tests green.
- **Stop condition:** merged.
- **Next task:** IN-08.

### IN-08 — Amount-mismatch park + anomaly wiring

- **Task ID:** IN-08
- **Title:** Wire the §8 AMOUNT_MISMATCH park (BLOCKED + SUBMITTED tightening + CRITICAL) into the evidence helper
- **Classification:** MVP normative implementation
- **Purpose:** all-or-nothing is a confirmed contract fact — a mismatch is DEFECT evidence; confirmed_amount must not move.
- **Prerequisites:** RG-03 (mismatch branch exists), IN-07 (helper).
- **Requirement sections / concepts to read:** §8 (mismatch block), §16.4 (no tolerance), §13 (AMOUNT_MISMATCH CRITICAL).
- **Placeholder components involved:** [Payment Status Feed Consumer], [Metrics / Alerting Layer].
- **Local placeholder mappings required before starting:** IN-07 done.
- **Local code areas to discover:** none new.
- **How to locate:** n/a.
- **Implementation instructions:** confirm the RG-03 mismatch branch is reachable from feed evidence: park same-stage BLOCKED(AMOUNT_MISMATCH), SUB=SUBMITTED, CRITICAL alert; resolution is EXTERNAL (corrected engine event completes normally; platform-side dispute — §19.2 family); no settle-at-actual-amount operation (rejected by design).
- **Do not change:** I2's definition.
- **Tests to add:** mismatch on MAYBE row (off the MAYBE clocks after park — maybe_since cleared? NO: submission tightens to SUBMITTED, maybe_since clears with the submission change per ST-07 — assert); corrected event later completes the row normally.
- **Edge cases:** mismatch event redelivered → inbox short-circuit; re-keyed duplicate → zero-row CAS.
- **Manual validation:** seeded mismatch.
- **Expected outcome:** defect path exact.
- **Failure signs:** confirmed moving on mismatch.
- **Common mistakes:** treating the park as retryable.
- **Completion criteria:** tests green.
- **Stop condition:** merged.
- **Next task:** IN-09.

### IN-09 — Kafka consumer hardening

- **Task ID:** IN-09
- **Title:** Bring both consumers to §16.2: manual ack after commit, earliest, ErrorHandlingDeserializer, DLT-for-poison-only, no retry topics, keying, poll sizing, retention-chain check
- **Classification:** MVP normative implementation
- **Purpose:** §16.2 line by line; D-07's gap checklist is the work list.
- **Prerequisites:** D-07 checklist; IN-05 (transaction order in place).
- **Requirement sections / concepts to read:** §16.2 (whole).
- **Placeholder components involved:** [Payment Status Feed Consumer], upstream consumer, [Metrics / Alerting Layer] (DLT depth, lag).
- **Local placeholder mappings required before starting:** consumer configs mapped.
- **Local code areas to discover:** container factory / properties per environment.
- **How to locate:** D-07.
- **Implementation instructions:** per checklist gap: enable-auto-commit=false + record ack-mode + offsets after DB commit; auto-offset-reset=earliest; ErrorHandlingDeserializer wrapping; DLT only for poison (deserialization/semantic validation) — transient infra retries IN PLACE or pauses the container; remove/forbid @RetryableTopic on money events; verify partition keying (feed by UETR, upstream by business_id — if a topic is not usefully keyed: concurrency 1 per partition, record); max.poll.interval sized for worst-case lock contention, small max-poll-records; scheduled retention-chain check (broker retention vs required window → alert; owner per §16.2).
- **Do not change:** broker-side config (another team's — the CHECK exists because it can change without notice).
- **Tests to add:** poison pill → DLT, consumer keeps running; transient DB error → in-place retry/pause, NOT DLT; offset committed only post-commit (crash test).
- **Edge cases:** consumer-group changes replaying history → inbox + evidence absorb (assert via replay test).
- **Manual validation:** config review against §16.2 per environment profile.
- **Expected outcome:** consumers production-hard.
- **Failure signs:** DLT depth used as a retry queue.
- **Common mistakes:** 'latest' reset surviving in some profile (silently skips money events).
- **Completion criteria:** checklist all compliant; tests green.
- **Stop condition:** merged; Phase P9 report.
- **Next task:** RC-01.


---

## Phase handoff summary (P9 → P10)

- **Phase outputs:** §6.0 contract-guarded intake (scale validation, payload equality); §6.7 ordering guard + tie handling; §6.6 anchors + DLT; §6.9 monotonic markers + counters; §8 feed skeleton (inbox-first, ack-after-commit); fail-closed provider_reference fallback; shared evidence-application helper (§4.4/§10.1); amount-mismatch park; §16.2-compliant consumers.
- **Blockers to carry forward:** §18 items unchanged; upstream asks 1–7 may still be open (asks 1–4 = Q-16; ask 5 = Q-01; asks 6–7 = Q-21) — comparator stays pluggable, tie alert stays live.
- **Local mapping rows expected filled:** [Payment Status Feed Consumer], [Inbox / Processed Event Repository], upstream consumer rows complete with the §16.2 checklist all-compliant.
- **Tests expected to exist:** ordering/tie tests, anchor lifecycle, marker truth-table, inbox concurrency (T-18 part), evidence rules + races (T-19), mismatch park, poison/DLT/offset tests.
- **Next phase entry condition:** IN-09 checklist compliant; phase report filed.
