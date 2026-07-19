# DB Schema Dictionary — Payment Orchestration

> **What this is:** the authoritative CONCEPTUAL dictionary of the data
> model — the basic idea behind each table, why each column exists, and
> how it is used at runtime. It is DERIVED from `requirment-v4.md` §2
> (the authority) plus the CA-4 schema/constraint resolution artifact
> and the CA-9/CA-10 companion specs; on any conflict the requirement
> wins, and PHYSICAL resolution (exact Oracle types, index expressions,
> encodings) lives in CA-4, not here. Some approval-store internals are
> summarized rather than enumerated. This file explains — it never
> overrides. (The `requirment` filename spelling is intentional; do not
> "fix" it.)
>
> **The one-paragraph mental model:** the system stores CURRENT STATE
> only. There is no transition-history journal table — history lives in
> the §14 structured log line (best-effort, external) and, for posting
> CONTENT only, in the switch-gated §14.1 attempt journal (a separate
> audit schema, never read at runtime). Every column below exists because
> a NAMED rule, alert, derivation, or display keys on it; columns that
> would merely be "nice to have" were rejected. The database BACKSTOPS
> every DB-ENFORCEABLE money/state invariant named by the schema
> contract (CHECKs, unique indexes, triggers — CA-4 is the resolution
> artifact); explicitly-listed exceptions use NAMED non-DB controls
> instead (the `required_total_at_creation` set-once property = RG-06's
> SQL-inventory assertion + mutation tests; L9 = code + the drift
> scanner — do NOT invent triggers for these). Money-critical facts are
> deliberately stored redundantly (counters) with a scanner that
> verifies the redundancy (§3 drift).

## Table inventory

| Table | Schema | One-line idea |
|---|---|---|
| `payment_obligation` | payment | One row per payment SCOPE — the durable, user-visible payment fact and the money ground truth ("how much must be paid, how much is reserved, how much is confirmed"). |
| `payment_request` | payment | One row per LOGICAL PAYMENT REQUEST against an obligation — the unit that can be sent to the payment engine. A request may carry MULTIPLE provider POST attempts ("posting attempt" = `post_attempt_seq`) while keeping one identity and one row. State = four independent columns, not one status enum. |
| `processed_inbound_event` | payment | Dedup inbox for the status feed — "have we already processed this event id?" |
| `trade_snapshot_state` | payment | One row per TRADE — the snapshot-admission gate's lock and memory (which snapshot ordering this trade has accepted, and which stored document that was). |
| CA-9 approval store | ops (separate, sanctioned) | Approval records for the audited manual operations (apply-platform-verified-outcome, reprocess-snapshot) — PENDING→APPROVED→CONSUMED, 4-eyes. |
| `payment_attempt_journal` (§14.1) | audit (separate, sanctioned) | Switch-gated, INSERT-only content record of what each posting attempt INTENDED to send — team-internal audit, never a runtime input. |

The §2 payment model is exactly the four payment tables. The two ops/audit
stores are the ONLY sanctioned stores outside it (rule 13); nothing else
may be added without a PO decision.

---

## 1. payment_obligation — one row per payment scope

**Basic idea.** When a valid upstream snapshot says "this trade owes a
payment of type X from account Y in currency Z", that fact becomes an
obligation row. The row is created BEFORE any request exists (users must
see the payment exists even if execution is blocked), survives every
retry and amendment, and is the single place money truth is accounted:
`required` (what upstream says), `committed` (what we have reserved via
requests), `confirmed` (what the engine has proven executed). A trade
with multiple payments owns multiple obligation rows — the NORMAL case.

**Writers:** snapshot admission (§6.1, under the trade lock then the
obligation lock), the §6.8 request-creation transaction (counters), the
outcome/settlement paths (counters + markers), the §4 derivation (read-
model fields, same transaction as the state change — never async).

### Scope key (UNIQUE)

| Column | Why it exists / how it is used |
|---|---|
| `business_id` | The trade identifier — the card's ONLY lookup key (§12); returns ALL of the trade's obligations. Indexed. |
| `payment_type`, `debit_account`, `currency` | Complete the scope key. These are message-carried stable identifiers (upstream ask 6) — never derived locally. If one changes upstream, that is a NEW scope and a NEW obligation BY AGREED BUSINESS BEHAVIOR (BA-1) — not a duplicate, no migration machinery. |

### Financial fields

| Column | Why / how |
|---|---|
| `required_amount` | What this scope must pay — mutable ONLY via admitted upstream messages (§6.7 ordering guard). NULL on a §6.6 anchor row (created from a failed-validation message) until the first valid message arrives. `required := 0` is the cancellation representation (BA-2 absence = amendment to zero). |
| `committed_amount` | Reservation counter (§3): += request amount in the transaction that INSERTS a request; −= on terminal-negative outcomes. Deliberately REDUNDANT with the request rows — the §3 drift scanner (OB-01) recomputes and pages on mismatch; the redundancy is a tripwire, not waste. |
| `confirmed_amount` | Authoritatively confirmed money — increases ONLY on EXECUTED evidence under the §8 amount-equality guard. Nothing moves at POST time. |
| `overpay_blocked` | One-way latch, set when `confirmed > required` (e.g. amendment-to-zero after execution). While set: trade ignored forward, alert, annotation only — no clawback, no auto-refund (§13). |

### Ordering, markers, counters

| Column | Why / how |
|---|---|
| `next_request_seq` | Per-obligation counter, incremented under the obligation lock in the SAME transaction that inserts a request. Input to the deterministic identity (§5.1) — which is what makes post-restore re-creations COLLIDE at the engine instead of double-paying. |
| `upstream_ordering` | The last-APPLIED message ordering (§6.7). The staleness guard: older orderings are dropped. Never advanced by a failed-validation message. Also the second term of every marker-liveness predicate. |
| `correlation_id` | From the upstream message (§6.0); carried into every §14 log line for cross-system tracing. |
| `validation_failed_at` + `validation_failed_ordering` | The ordering-tagged validation marker: set by message-validation failure, enrichment definitive invalid-data, or a synchronous invalid-data engine reject. LIVE while `ordering >= upstream_ordering OR upstream_ordering IS NULL` (an anchor's marker is live by definition). Writes are MONOTONIC (§6.9) — only a strictly newer ordering overwrites; stale writes are dropped and counted. While live: blocks §6.8 successor creation on the scope; drives the DATA_VALIDATION_FAILED exception. |
| `validation_failed_first_at` | Set-once episode anchor: stamped when the marker goes not-live → live, untouched by re-tags, cleared when it clears. Exists because the re-tag timestamp is refreshed by every newer failing message and can never age — the §15 marker-age alert keys HERE. Also the chronology term of the §6.6 accepted-window candidate diagnostic. |
| `validation_reject_count` | Counts marker sets; reset on clear. Alert-only at ≥ 3 — deliberately NO gate, because a validation cycle moves no money and a corrected message IS the designed recovery. |
| `provider_rejected_at` + reject code + `provider_rejected_ordering` | The ordering-tagged provider-negative marker (every engine/ops negative that is not invalid-data — §7/§8 marker totality). LIVE while `ordering >= upstream_ordering` OR `provider_reject_count >= 2`. |
| `provider_reject_count` | Why the ≥ 2 term exists: from the SECOND reject the marker is clearable only by ops (§19.3), never by a newer message — this prevents an upstream-paced reject/re-pay loop where each amendment silently re-pays a rejected scope. Alert on increment ≥ 2. |
| `reopened_at` | Set on step reopening (§6.5 — a COMPLETED/CANCELLED scope receiving a real amendment). Derivation input so the card can indicate reopening. |

### Read-model fields (derived by §4, consumed by §12 — no rule may key on them)

| Column | Why / how |
|---|---|
| `ui_step_status` | Stored derived status: IN_PROGRESS / COMPLETED / CANCELLED (CANCELLED = the zero-required terminal branch). NOT_STARTED is virtual = row absence. Re-derived in the SAME transaction as every state change — the frontend never computes status. DB CHECK carries exactly the three values. |
| `active_exception_category/code/message/retryable/severity/manual_action/at` | The single active exception per §4.2's rank order (money-unknown outranks system-unavailable, etc.), derived atomically with state. This is how "report exception to frontend" is guaranteed: the exception and the money state can never disagree. |
| `ops_annotation` | Free-text ops note (e.g. overpay acknowledgement) — display only. |
| `ui_process_instance_id`, `ui_step_instance_id` | Upstream-supplied display/reference strings. NOT lookup keys (the card looks up by business_id only). |

**Constraints:** scope-key UNIQUE; amounts `>= 0` CHECK; business_id
index. The ui_step_status CHECK lands in S-05.

---

## 2. payment_request — one row per logical payment request

**Basic idea.** A request is the unit that can be sent to the payment
engine: right-sized to the shortfall at creation (§6.8), carrying a
deterministic idempotency identity, and moving through a FACTORED state
model — four independent columns, one per independent fact — instead of
one 13-value status enum. One request row may carry MULTIPLE provider
POST attempts (the §9.2 downgrade re-POSTs the SAME row; "posting
attempt" is reserved for `post_attempt_seq`). Requests are never
deleted: a REJECTED predecessor stays visible next to its successor
(history is never laundered). At most ONE ACTIVE request per obligation
(I6).

**Writers:** created ONLY by §6.8 (single creation point, obligation
lock); mutated ONLY through the shared CAS helper (version-guarded,
row-count checked); claims/leases per §11.

### The four dimensions

| Column | Why / how |
|---|---|
| `stage` | WHERE in the pipeline: ENRICH → POST → CONFIRM. Monotonic with exactly ONE sanctioned exception: CONFIRM → POST via the §9.2 trust-age downgrade (same-key re-attempt). |
| `stage_state` | HOW it sits at that stage: READY (claimable) / CLAIMED (leased, §11) / RETRY_WAIT (transient failure, claimable when `next_retry_at` is due) / BLOCKED (needs a human or external resolution; `blocked_reason` set). |
| `submission_state` | MONEY TRUTH (§7.1): NOT_SUBMITTED / MAYBE_SUBMITTED / SUBMITTED. Kept separate from stage precisely so "where the work is" can never corrupt "whether money may have moved". MAYBE fails closed: no blind re-POST. |
| `outcome` | FINAL resolution, write-once: NULL (active) / EXECUTED / REJECTED / CANCELLED / SUPERSEDED. ACTIVE iff NULL; TERMINAL-NEGATIVE = REJECTED/CANCELLED/SUPERSEDED. |

"Who acts next" is DERIVED (§4.5), never stored. The old status enum
survives only as a §10.4 display label — no rule may key on it.

### Identity and engine-facing fields

| Column | Why / how |
|---|---|
| `payment_obligation_id` | Owner. I6 (function-based unique index on `CASE WHEN outcome IS NULL THEN payment_obligation_id END`) enforces at most one active request per obligation AT THE DB. |
| `amount` | IMMUTABLE after creation (§6.3). Amendments never mutate an in-flight request — they supersede or top-up via §6.8. |
| `request_seq` | The IMMUTABLE per-request sequence (1d8a650 M1): the `next_request_seq` value this row consumed, persisted write-once in the creation transaction. Source of truth for the §5.1 identity-hash input (the hash is not invertible), the `request_seq` field on every §14 log line, the §12 keyset order, and the §5.2 DR heuristic. NULL on legacy/pre-F0 rows, never fabricated. NULL-ignoring unique over (obligation, seq). |
| `blocked_reason` | Set iff `stage_state = BLOCKED` (L8 CHECK both ways). The §13 code vocabulary (RETRY_EXHAUSTED, UNMAPPED_CODE, AMOUNT_MISMATCH, ENGINE_INCONSISTENCY, AMENDMENT_PARKED, OPS_PARKED, ESCALATED — ESCALATED kept distinct so the §15 BLOCKED queue ranks the money-critical class first; CUTOFF_EXPIRED reserved, never produced). Display/queue-routing ONLY — §10.1: no rule may key on it. |
| `idempotency_key` / `end_to_end_id` | The deterministic identity `hash(scope | seq)` (§5.1) — computed and persisted BEFORE any POST (write-ahead: K-02 at creation, re-verified at first claim K-04). UNIQUE. This is the duplicate-payment defense: the same logical attempt always presents the same key, even after crash or restore. |
| `uetr` | SDK/engine-assigned (NEVER generated here — BA/§5); persisted ONLY from acceptance-class responses; UNIQUE, NULL until assigned (NULL-ignoring index). The key for platform status queries. Never a dedup key. |
| `provider_reference` | Engine-assigned reference from the POST response; SECONDARY feed-matching key (§8). Distinct field from uetr, never merged. |
| `version` | The CAS counter — every dimension write is `UPDATE ... WHERE version = :expected`; rowCount 0 = lost race, re-read. |
| `claimed_by`, `claim_expires_at` | Claims as LEASES (§11): a dead worker's claim expires instead of wedging the row. POST-stage expiry lands in MAYBE (the payload may be at the engine) — never back to READY. |

### Retry / resolver machinery

| Column | Why / how |
|---|---|
| `attempt_count` | Retry BUDGET counter. RESETS on the §9.2 downgrade (deliberate — the downgrade grants a fresh budget). Therefore NEVER used as a join identity. |
| `post_attempt_seq` | Posting-attempt IDENTITY: +1 in every posting-claim CAS, NEVER reset. Exists because the §14.1 journal and the §14 ATTEMPT-class log lines need a stable pair key that survives the downgrade reset — `(request_id, post_attempt_seq, attempt_event_type)` is the join. |
| `next_retry_at` | Makes RETRY_WAIT claimable-when-due (L7: RETRY_WAIT ⇒ next_retry_at set). |
| `last_error_code` | Last classified failure — display/triage context. |
| `retry_deadline_at` | EXISTS BUT RESERVED/UNUSED: the 2026-07-11 decision made MAX ATTEMPTS the retry limit; the engine owns the cutoff calendar. Kept to avoid schema churn; no rule reads it. |
| `next_query_at` | Per-row status-query backoff (§9.5) so the resolver sweep respects the engine's rate budget. NOTE: a consecutive-answer counter was REJECTED as over-design — the §1 collision contract makes acting on a single post-trust-age answer safe. |

### Send-evidence fields (what may be at the engine)

| Column | Why / how |
|---|---|
| `last_sent_hash` | Hash of the canonically-serialized instruction, persisted in the claim transaction BEFORE the HTTP call, every attempt. If the worker dies mid-call this is the only payment-table record of what may be executing. Content itself is NEVER persisted on payment tables — instructions are re-assembled fresh per attempt (§7.0); the content record is the §14.1 journal. |
| `divergence_expected` | Per-attempt flag computed AT CLAIM TIME: `previous last_sent_hash IS NOT NULL AND differs`. Exists because the comparison is impossible later — the overwrite destroys the prior hash, and this flag is the §7.2 collision-branch discriminator (expected divergence → quiet query path; unexpected → ENGINE_INCONSISTENCY CRITICAL). A DR-replayed row has no prior hash → false → collisions classify ANOMALOUS, correctly. |
| `divergent_payload_at` | Write-once: the engine reported a known-key-different-payload collision. While set, `repost_permitted` is false FOREVER — the engine already holds this key; resolution is query or ops, never another POST. |

### Timestamps (discipline: a timestamp exists iff a NAMED rule keys on it)

| Column | Why / how |
|---|---|
| `created_at` | Insert time; the §5.2 replay-window query and the post-F0 NULL-stamp scan key on it. |
| `state_changed_at` | The single LAST-WRITE clock ("is anything moving"; BLOCKED-queue age). Churns on every CAS — so NO age RULE keys on it (churn would silently re-arm alerts). For terminal rows it IS the outcome time (L1 freezes it). |
| `creating_ordering` | Creation-time stamp: the `upstream_ordering` at creation. Input to the §6.8 successor policy (REJECTED successors need a STRICTLY NEWER ordering) and the marker ordering tags. |
| `required_total_at_creation` | Creation-time stamp for the UI AMOUNT SERIES (2026-07-19): the obligation's `required_amount` read under the lock in the creating transaction. ONE stamp per request row (not per POST attempt); obligation-scope, never trade-wide. Set-once, NEVER load-bearing (no money logic reads it), never UPDATEd. Stored because it is NOT reconstructable later — a reject-then-retry of 100+100 sums to 200 under any derivation. NULL = created before the F0 capture boundary; post-F0 NULLs raise the §15 data-quality ticket. Tripwire CHECK: `IS NULL OR >= amount`. |
| `maybe_since` | Set-once anchor of the current MAYBE episode; cleared on leave and by outcome normalization (§10.2). The §9.3 escalation clock and §15 MAYBE-age alerts key on it. |
| `escalated_at` | Set-once per MAYBE episode when §9.3 escalation first fires; gates the BLOCKED(ESCALATED) write so a §9.2 downgrade cannot enter a downgrade ⇄ escalate cycle. |
| `submitted_at` | Set when SUBMITTED; the §9.2 SUBMITTED-branch trust-age and §9.5 confirmation age key on it (e.g. SUBMITTED+NOT_FOUND parks only past this age). |
| `last_post_attempt_at` | Stamped in the claim transaction BEFORE the HTTP call (stamping on response would leave it stale exactly in the crash/lease-expiry cases that produce MAYBE). The §9.2 MAYBE-branch trust-age keys on it — each attempt restarts that clock, which the first-episode anchor `maybe_since` must not do. |

### Constraints

`UNIQUE(idempotency_key)`; NULL-ignoring `UNIQUE(uetr)`; the
NULL-ignoring conditional unique over `(payment_obligation_id,
request_seq)` (legacy NULL-seq rows exempt — behavior proven by the
S-05 isolation tests, 2a19c20 M2); I6 (one active
request per obligation); per-column enum CHECKs; the §10.3 legality
matrix as CHECKs (L2–L8, L1 terminal shape — e.g. L4: EXECUTED ⇒
SUBMITTED; L8: BLOCKED ⇔ blocked_reason); freeze + release-guard
TRIGGERS as backstops (raw SQL cannot terminal-negative a
MAYBE/SUBMITTED row without the session evidence flag); the stamp
tripwire CHECK.

---

## 3. processed_inbound_event — the inbox

**Basic idea.** The status feed is at-least-once; this table makes
processing exactly-once-per-event: `PRIMARY KEY (source, event_id)`,
insert-first, duplicate key = benign redelivery = silent skip.

| Column | Why / how |
|---|---|
| `event_id` | The feed event's unique id (TL-1/Q-18 — if the feed lacks one, a synthesis is chosen and its blind spots accepted). |
| `source` | Namespace (e.g. PAYMENT_STATUS_FEED) so multiple inbound flows share one inbox without id collisions. |
| `processed_at` | UTC insert time; drives the purge job. Retention MUST exceed the Kafka replay window (inbox > kafka ≥ replay — §16.2, named owner). |

Deliberately NO parked-event table exists: unmatched events are logged,
counted, and acked; any real missed outcome is recovered by QUERY (§9),
keyed by identity we already persist. (Proposing a parked-event store is
a spec conflict.)

---

## 4. trade_snapshot_state — the admission row

**Basic idea.** Snapshots are whole-trade documents; per-obligation
watermarks cannot stop a stale snapshot from CREATING a scope no
obligation row has ever seen. This one-row-per-trade table is the
admission gate: locked FIRST (SELECT FOR UPDATE, insert-on-first-contact
with PK-race retry) in every snapshot transaction — lock order: trade
row, then obligations in scope-tuple order. It is OVERWRITTEN, never an
append log; the §6.1 admission transaction is its only writer. A
failed-validation message never advances it.

| Column | Why / how |
|---|---|
| `business_id` | PRIMARY KEY — one row per trade; the object two concurrent first-snapshots serialize on. |
| `last_accepted_ordering` | The trade-level watermark: ordering of the last ADMITTED snapshot. Same representation + pluggable comparator as `upstream_ordering` (business timestamp today, explicit sequence later, no logic change). |
| `last_xml_storage_id` (+version) | THE durable pointer to "the most recent admitted snapshot" in the upstream-populated immutable store (ask 8). §7.0 fresh assembly and §20-10 reprocessing read it. |
| `last_payload_digest` | Canonical business-payload digest of that snapshot — same algorithm as the §9.3 approval digest (ONE shared implementation). Makes §6.7 tie equality evaluable against applied state. |
| `updated_at` | Audit timestamp (DB time). |

Greenfield fact: the table legitimately starts EMPTY (new feature — no
pre-existing trades); every row is created by its trade's first admitted
message with pointer + digest populated; a NULL-digest row cannot exist.
Restore honesty: a DB restore can regress/remove this row; ordering is
re-derivable from obligations, but the pointer and digest are NOT — an
explicit §5.2 runbook step (post-MVP).

---

## 5. CA-9 approval store (ops schema — sanctioned store #1)

**Basic idea.** The audited manual operations that can move money or
release reservations (apply-platform-verified-outcome for wedged MAYBE
rows; reprocess-snapshot) run on APPROVAL RECORDS, not parameters: the
execution signature is `approval_id` only, and every identity is derived
from the approval record. State machine PENDING → APPROVED → CONSUMED
with version/nonce uniqueness; single-transition operations consume in
the SAME transaction as the payment write; reprocess consumes AT START
after a digest check (crash mid-fan-out ⇒ a NEW approval — consumed
approvals are never resurrected). Binding fields include the reprocess
content digest; `completed_at` + a per-block summary are stamped after
the last block, and a consumed-without-completion alert catches crashes.
Full schema is CA-9's deliverable (OP-01 implements; §18 BLOCKING item 3
— this closes the permanent-wedge risk on MAYBE rows).

---

## 6. payment_attempt_journal (§14.1, audit schema — sanctioned store #2)

**Basic idea.** The request actually sent to the engine is NOT visible
to this team (the SDK/platform own the wire form; status is queryable,
content is not). This journal is the local record of the CANONICAL
INSTRUCTION each posting attempt intended to send. It is purely
team-internal tracking and audit: switch-gated (default OFF; enablement
gated on encryption + compliance retention — payments go-live never
waits for it), INSERT-only, NEVER read at runtime, never a business or
money-safety gate (statement-local insert failures are caught around the
single statement and alerted AFTER host commit; FATAL infra failures
propagate as ordinary failures; the guarantee is NO INCORRECT PAYMENT
OUTCOME). Two events ride existing transactions: ATTEMPT_STARTED in the
posting-claim transaction (FULL content every attempt — dedup-by-hash REJECTED as unimplementable under the no-read invariant, content-reference indirection REJECTED with it) and
ATTEMPT_RESOLVED in whichever transaction ends the episode (including
LEASE_EXPIRED_MAYBE), rowCount==1 only.

| Column | Why / how |
|---|---|
| `journal_id` | Identity PK (global index — both unique structures omit the partition key by necessity). |
| `request_id` | The request (deliberately NO FK — the audit schema must not couple to payment-table lifecycle; type resolved from D-02, never guessed). |
| `idempotency_key` | Denormalized for key-based investigation without a payment-schema join; LOCAL index. |
| `post_attempt_seq` | The pair identity (§2.2 — never `attempt_count`, which resets on downgrade). `UNIQUE(request_id, post_attempt_seq, event_type)` makes STARTED/RESOLVED pairs unambiguous. |
| `event_type` | Exactly 'ATTEMPT_STARTED' or 'ATTEMPT_RESOLVED' (CHECK). Byte-equal to the log field `attempt_event_type` — that equality IS the log join. |
| `occurred_at` | UTC event time; the monthly interval-partition key (retention = partition drop with UPDATE GLOBAL INDEXES). |
| `trigger_source` | Which flow wrote it (mirrors the §14 line vocabulary). |
| `correlation_id` | Cross-system trace, as on the log line. |
| `payload_hash` | Same value as `last_sent_hash` — lets an investigator match journal content to the payment-table evidence without reading the CLOB. |
| `payload_content` | CLOB, SECUREFILE: the full canonical instruction — STARTED rows must carry non-empty content, RESOLVED rows must carry NONE. Enforced by a BEFORE INSERT trigger (Oracle forbids CHECKs on LOBs; the trigger proves presence, T-38 proves fullness). This is the §16.3 controlled content exception — hence the security package: INSERT-only grants, restricted audit role for SELECT, unified audit on all access, own tablespaces, TDE per the enablement gate. |
| `outcome` | RESOLVED rows only: the §7.2 classification VERBATIM (CHECK list generated from CA-1) or 'LEASE_EXPIRED_MAYBE'. |
| `error_code`, `error_detail`, `response_excerpt` | Bounded scalar failure context for RESOLVED rows — triage without the platform. |

What the journal proves: application INTENT. It is NOT proof of the
post-SDK wire bytes, and it rewinds with a full-database restore (the
external §14 log platform is the restore-surviving record, best-effort
complete). If the journal has gaps, the fallback is the §14 line (when
present) plus asking the payment platform.

---

## 7. What is deliberately NOT stored (and why the absence is a feature)

| Absent thing | Why absent |
|---|---|
| Transition-history journal table | Current-state-only decision: history = the §14 log line (external, restore-surviving, best-effort) — no local table can survive the restores it would matter for. The §14.1 journal is a CONTENT sink, not transition history. |
| Parked-event table | Unmatched feed events are logged + acked; real outcomes are recoverable by query (§9). A parked store adds an unbounded replay surface for no recovery value. |
| Per-dimension `*_changed_at` columns | Age rules key on set-once episode anchors; a churning per-dimension clock silently re-arms alerts. |
| Instruction content on payment tables | §16.3: content lives only in the switch-gated journal; payment tables carry the hash. |
| Marker-source discriminator | The §6.6 candidate diagnostic stays a candidate BY DECISION — schema churn for an observability aid was rejected. |
| Consecutive-answer counter, any rule reading `retry_deadline_at` (RESERVED/unused), attempt-history replacing hash/flag | Each considered and REJECTED at PO review — recorded in §2.2 so they are not re-proposed. |

## 8. Reading paths at a glance

- **Card / UI:** business_id → all obligations (one entry per payment) →
  §12 ALL-PAYMENTS TABLE adds request-granular rows; the amount series =
  `required_total_at_creation` per request row.
- **Ops console:** same read surfaces; transition history from the §14
  log store; actions via the O1–O10 catalog preconditioned on the
  dimension columns.
- **Nothing** reads the §14.1 journal at runtime, and no rule keys on
  display labels, `blocked_reason` (routing only), or any read-model
  field.
