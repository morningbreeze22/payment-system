# Legacy Migration and Approval of the Current Payment Delta

> Status: proposed extension to the implemented head + event model, based on the team's migration and approval decisions. Reviewed against this repository at `08295fb`.
>
> The team reports that the event-driven system has been implemented. The older documents in this folder still describe an exploration/pre-production stage; those historical status statements do not describe the deployment assumptions of this document. This document specifies the migration extension, not evidence that its schema, triggers, or application changes already exist. It does not revise the separate persisted-state baseline.

## 1. Purpose and agreed operating assumptions

Migrate legacy payment information by portfolio without making normal payment processing depend on the legacy database. Import historical data first; create payment-ledger facts only when a payment actually needs processing. Users approve the **current concrete payment delta**, not a separate historical-baseline workflow.

The design depends on these agreed facts:

1. Payment is the downstream end of trade processing. Upstream services own portfolio routing and ensure that the systems process non-overlapping portfolios. Payment does not introduce an ownership registry or choose the processing system.
2. The legacy system exposes a **handoff marker and payment-instruction XML**, not reliable execution confirmation. Every legacy payment is processed and independently checked by users in another system.
3. The business convention is to assume that users correctly processed the handed-off instructions. The new system records an **assumed legacy accounted amount**, not a fabricated bank-confirmed settlement.
4. Users have an external reconciliation system. The new system does not implement a reverse payment-data import into the legacy system when processing moves back there.
5. A portfolio may later return to the new system. Re-entry refreshes legacy evidence while preserving every request and execution fact already recorded by the new system.
6. Users approve each concrete positive delta request covered by this workflow. There is no separate baseline-approval step and no permanent trade-level approval.

The safety objective is to avoid duplicate payment caused by migration replay, stale assumptions, stale approvals, or local retries. This design cannot independently establish that a legacy handoff was actually paid, or detect an unrecorded duplicate manual payment. Those facts remain within the stated manual-processing and reconciliation convention. Missing or inconsistent evidence must block processing rather than silently become zero.

## 2. Design philosophy

### 2.1 Separate evidence acquisition, accounting, and authorization

There are three different responsibilities:

| Layer | Responsibility | What it does not imply |
|---|---|---|
| Local migration data | Preserve a complete, versioned view of legacy handoffs and their XML/evidence | Import does not create a payment, approve a payment, or prove settlement |
| Payment event stream | Record the particular assumed legacy baseline adopted for a scope and the new system's own payment history | A legacy baseline is not a provider outcome |
| Request approval | Authorize one request, with specific payment content and calculation context | Approval is not evidence that either system has already executed that request |

The reviewer sees the calculation and approves the proposed instruction once. The system records the supporting baseline version for audit, without asking the user to approve it separately.

### 2.2 Import eagerly; adopt lazily

Migration saves source records into the new system's local migration area. It does **not** create a head or a baseline event for every historical payment.

When an incoming XML, retry, or another command needs a payment decision, the command checks the local published migration data. It adopts the applicable baseline into the event stream if necessary. Historical payments that never need processing remain migration records only.

After adoption, the fold reads the recorded amount and revision from events. It does not reparse XML, query staging tables, or consult the old database to reconstruct historical money state. Mutable migration readiness is an additional operational veto, not a replacement source for recorded money facts.

### 2.3 Preserve the existing write path

All payment events, including import adoption and human decisions, use the existing canonical writer:

```text
lock PAYMENT_HEAD
→ fold the stream
→ compare the fold with the head witness
→ validate the command and any current migration-readiness veto
→ insert each event and apply its head effect in order
→ commit
```

The importer has no privileged path to update payment counters. The approval endpoint commits events and wakes existing processing; it does not introduce its own provider-call path.

## 3. Design decisions and reasons

| Decision | Reason and consequence |
|---|---|
| Lazy baseline adoption instead of migration-time event creation | Avoids initializing historical scopes that will never receive further work. Every money-enabling entry point must therefore check migration applicability. |
| Local imported data instead of runtime legacy queries | Removes legacy availability, credentials, schema, and latency from normal payment processing. |
| An import API may live in the payment application | Separation is about responsibilities and dependencies, not a mandatory new service or deployment. |
| Cumulative legacy baseline with replacement semantics | Re-importing a full historical snapshot must not add the same amount again. |
| Separate assumed legacy amount from new-system paid amount | Keeps evidence provenance clear and prevents re-entry from overwriting the new system's payment history. |
| Approve the concrete delta request only | Matches the user's actual decision and avoids a two-step baseline/payment approval workflow. |
| Bind approval to request identity and content/context | The same number can describe different recipients or different payment obligations. |
| Preserve approval for an unchanged, legally retryable request | Avoids reapproval solely because a scanner ran or a retry clock changed; existing ambiguity and retry rules still apply. |
| Immutable published batches and evidence revisions | Makes approved calculations explainable after refresh and supports resumable imports. |
| No reverse database synchronization | Legacy processing is manual and users already reconcile externally. Re-entry still must distinguish legacy-origin amounts from new-system payments. |
| A local migration-readiness gate, not portfolio routing | Upstream ownership does not stop queued local retries or invalidate old approvals. Readiness controls whether local payment work may proceed. |

## 4. Local data import and publication

### 4.1 Exporter and import API

Recommended boundary:

```text
legacy database / handoff records / XML
→ migration exporter or script
→ new-system import API or file loader
→ local immutable migration data
```

The exporter alone needs legacy access. The import API may be a module in the same Spring Boot application, with restricted operational access. It accepts records, validates them, and publishes complete batches. It must not call the provider or create payment requests.

An API that directly connects to the old database is still an import API, but it retains a legacy dependency in the deployed service. Use an external exporter if eliminating that dependency is the requirement.

A minimal API surface is create batch, upload records idempotently, validate, and publish. No distributed transaction across the two databases is required: only a completed, verified local batch becomes eligible for business use.

### 4.2 Logical records

The following are logical structures, not executable DDL or a requirement to deploy a separate migration service:

| Structure | Suggested fields and purpose |
|---|---|
| `MIGRATION_BATCH` | Batch ID, migration cycle, coverage identifier/portfolio, source snapshot boundary, manifest digest, parser/mapping version, expected/loaded counts, validation summary, status |
| `LEGACY_HANDOFF_RECORD` | Batch ID, stable source record/instruction identity, trade/business ID, handoff marker, source revision, evidence reference/hash, mapped payment key or unresolved mapping, parsed amount/currency, classification/error |
| Local migration-readiness record | Stable coverage identifier, current cycle, published batch ID, `PREPARING / READY / PAUSED`. Can extend the existing onboarding/control store; it contains no old/new owner or routing decision. |

Published records are immutable. A refresh creates a new batch; an atomic pointer publication makes the complete batch current. A batch can be uploaded in many transactions, but partial contents are never published.

For simple serialization, publication and local money-command readiness checks can use the stable readiness row. If implemented with a short row lock, take it before `TRADE_HEAD` and then sorted `PAYMENT_HEAD` locks on paths needing all three. Never hold it during enrichment, human review, or a provider call. This may serialize short decisions within the migration coverage; measure that cost before introducing a more complicated mechanism. All participating paths must use one documented lock order.

The readiness row prevents a publication/decision race inside the database. It does not cancel a worker that already committed `POST_STARTED`; section 10 defines the required pause and drain boundary.

### 4.3 Completeness and identity

Export from the **handoff registry**, then locate each XML. Enumerating only files loses the very records whose XML is missing.

`trade_id` locates candidates; the final mapping uses the existing canonical payment key: business ID, payment type, debit account, and currency. Do not aggregate unrelated currencies or instruction identities into a trade-wide number. A handoff XML may itself contain multiple payment instructions.

Source instruction identity identifies a business instruction. A content hash detects a revision; it is not an instruction identity. Two instructions can have identical content, and one instruction can have different byte representations. Batch ID is not a payment deduplication key either.

The parser must understand whether legacy XML records are separate instructions, revisions replacing an earlier instruction, or cumulative snapshots. Summing every historical XML is unsafe when one instruction has multiple versions. If the source cannot distinguish those meanings, classify the affected scope as an exception rather than infer a total.

Batch verification checks the coverage contract, source-to-import counts, identity uniqueness, source boundary, and that every handoff is represented as usable evidence or an explicit exception. Hashes detect transfer changes; they do not prove that the source enumerated every handoff. The export contract must supply that completeness assertion.

### 4.4 Runtime lookup outcomes

| Result | Required behavior |
|---|---|
| Complete applicable data with valid mapped records | Adopt the versioned cumulative baseline on demand |
| Complete applicable data with no handoff for the scope | Record an explicit zero baseline with `NO_HANDOFF_IN_COMPLETE_SCOPE` provenance when first needed |
| Handoff present but XML missing, malformed, or mapping ambiguous | Record/block the exception; no automatic amount-zero fallback |
| Coverage not ready, missing, stale, or unknown | No payment authorization; wait for valid publication |
| Scope explicitly outside legacy migration | Existing non-migration policy applies; absence of configuration must not silently establish this classification |

When a missing XML cannot be mapped to one payment key, retain a trade-level blocker in the local source lookup. It must affect future scopes created for that trade, not just heads that happened to exist during import. Resolving it requires explicit replacement evidence or a documented manual reconciliation result in a new revision. Approving a delta cannot waive an unresolved mapping or completeness error.

## 5. Money model and refresh semantics

### 5.1 Three quantities and one reservation

For each payment scope:

```text
required_amount        = latest admitted required amount (existing rule)
legacy_accounted       = amount on the latest valid LEGACY_BASELINE_SET
new_system_paid        = existing authoritative-outcome aggregation by request ordinal
reserved               = amount of the existing open request, or zero

accounted_total        = legacy_accounted + new_system_paid
shortfall              = required_amount - accounted_total - reserved
```

`legacy_accounted` is cumulative **legacy-origin** handling under the manual-processing assumption. It is not the combined total displayed by an external reconciliation screen. New-system payments must not be included in both quantities.

Keep the existing `PAID_TOTAL` witness meaning as new-system paid money and add `LEGACY_ACCOUNTED` as a separate witness. All accounting comparisons that previously used paid money alone must use the appropriate combined total. In particular:

```text
opening amount = REQUIRED_AMOUNT - LEGACY_ACCOUNTED - PAID_TOTAL
                (only when no request is open)

excess condition = LEGACY_ACCOUNTED + PAID_TOTAL + RESERVED > REQUIRED_AMOUNT
```

The latest baseline amount is selected by valid stream order and explicit revision succession. It is **not the sum of baseline-event amounts**. The fold must never recompute it using a newly deployed XML parser.

### 5.2 Example across a return to legacy processing

| Step | Required | Legacy cumulative baseline | New-system paid | Unreserved difference |
|---|---:|---:|---:|---:|
| First entry | 150 | 100 | 0 | 50 |
| User approves request 1; new system executes 50 | 150 | 100 | 50 | 0 |
| Processing moves to legacy; users additionally handle 30 | 180 | 130 | 50 | 0 |
| Re-entry adopts refreshed legacy baseline | 180 | 130 | 50 | 0 |

Re-entry replaces legacy baseline 100 with 130. It does not add another 130, reset new-system paid to zero, or restart request ordinals.

If a refreshed XML states total obligation 180 but the user actually handled only an additional 30 after reconciliation, parsing 180 as an additional legacy payment is incorrect. The import mapping or an auditable manual correction must establish the legacy cumulative 130. Without that distinction, stop the scope for manual handling.

### 5.3 Refresh rules

- Identical data in the same cycle is an adoption no-op, even if transported again in a different batch.
- A material evidence/mapping change produces a new baseline revision. Explicitly identify the prior adopted revision; reject stale or out-of-order succession.
- Re-entry always creates a new migration cycle. An unchanged amount does not revive an approval from a previous cycle.
- A smaller source file, a missing record, or a parser failure is not evidence that the cumulative baseline decreased. A decrease needs explicit correction provenance and a valid complete replacement; any resulting positive delta requires fresh request approval.
- Imported but unadopted revisions do not change replay of previous events. Current readiness/version mismatch blocks new submission until adoption and revalidation occur.
- Publication does not fan out events to every historic payment. Revalidation occurs on the next relevant command, including existing retries with no new XML.
- Zero difference creates no payment approval task. Negative difference invokes existing excess handling; it never creates an automatic refund. Data exceptions remain visible even when no positive difference exists.

## 6. User workflow: approve the current delta once

### 6.1 Normal sequence aligned with the existing model

The current schema requires a payload hash on `REQUEST_OPENED` and supports pre-open enrichment failure. Preserve that sequencing:

```text
new XML admitted under existing trade sequencing rules
→ check local migration coverage
→ adopt baseline if needed
→ calculate positive shortfall
→ perform enrichment outside database locks
→ revalidate facts under the canonical locks
→ REQUEST_OPENED + PAYMENT_REVIEW_REQUESTED in one transaction
→ user reviews the concrete instruction
→ PAYMENT_REVIEW_APPROVED
→ existing dispatcher validates and records POST_STARTED
→ provider call outside the transaction
→ existing response/query/feed/outcome handling
```

Enrichment results carry the business/baseline context they were computed for; reject stale results at opening. Pre-open enrichment failure uses existing `ENRICH_FAILED` semantics, without inventing an ordinal or pretending an approval exists.

An open request waiting for review retains its normal reservation. This prevents duplicate requests while the reviewer is deciding. Approval itself does not change the request amount or reservation.

### 6.2 What the reviewer sees and approves

The page shows current required amount, assumed legacy accounted amount and evidence status, new-system paid amount, any unresolved request, and the proposed positive delta. It also shows the debtor, recipient, recipient account, currency, and other payment-critical instruction fields.

The action is **Approve this payment**, not Approve this trade or Confirm legacy settlement. One authenticated action creates one request-scoped authorization. Separate baseline approval is not required.

Critical recipient fields must be available before the review is created. A later enrichment cannot silently substitute an account beneath an approved amount.

### 6.3 Review context

Use a versioned canonical representation and hash, with retained protected evidence for the display:

```text
payment_key + request_ordinal + idempotency_key
request amount + currency
payment-critical instruction fields (including recipient and debtor identities)
migration_cycle_id + adopted baseline revision/evidence digest
accepted upstream sequence + required amount
new-system paid amount and relevant outcome revision
review-context format version
```

The reservation of the request being reviewed is not subtracted a second time. To validate its amount, compare it with `required - legacy_accounted - new_system_paid`, while requiring that it remains the sole open request. The generic shortfall including its reservation will normally be zero.

For the first implementation, an accepted upstream sequence change conservatively invalidates pending/unsubmitted review context, even if the apparent amount is unchanged. A later semantic-equivalence optimization requires its own explicit rules. Scanner timestamps, query-poll timestamps, and unrelated audit events do not change the review context.

Do not bind approval to the entire stream version: ordinary retry events would invalidate it. Do not use raw XML byte equality as business equivalence. The review hash is separate from the existing write-ahead provider payload hash, which may change during re-enrichment. A whitelist defines fields allowed to change without changing the approved business instruction; unknown changes invalidate approval.

Recipient details and raw XML belong in the existing protected evidence/vault mechanism. Permanent event rows carry opaque references and structured non-PII authorization facts, not erasable personal data in `DETAIL`.

### 6.4 Retry, rejection, and invalidation

| Situation | Approval behavior |
|---|---|
| Same request, same business instruction/context, existing protocol allows retry | Reuse approval; record its review ID on the next `POST_STARTED` |
| Same amount but another request ordinal or recipient | Fresh review required |
| Changed required sequence, adopted baseline, cycle, or material payment content | Invalidate the old context and re-evaluate before submission |
| Request has executed or has a possibly-executed claim | Record evidence and resolve through existing rules; approval cannot authorize a replacement |
| User rejects | Latch review rejection for that request/context; keep it blocked rather than immediately recreating the same proposal |
| Only scheduling/backoff changes | Approval remains valid |

For a simple rejection implementation, retain the open reservation and require an explicit new review/rework action or a legitimate business revision. Do not automatically create a new review on every scan. Cancellation or manual supersession, if needed, must use the existing release predicate and the existing authorization requirements for that operation.

If changed facts require a different amount, never mutate the opened amount. Close a releasable obsolete request through its legal existing transition and create a new ordinal when appropriate. A possibly-executed request stays reserved and resolver-owned. Merely withdrawing approval never proves non-execution.

## 7. Event-model extension

### 7.1 Existing mechanisms reused

Reuse `REQUIRED_AMOUNT_SET`, `REQUEST_OPENED`, `ENRICH_FAILED`, `POST_STARTED`, `POST_RESULT_RECORDED`, query/feed events, `OUTCOME_RECORDED`, and existing ops/correction events with their current identities and release rules.

Do not synthesize historical `REQUEST_OPENED / POST_STARTED / SETTLED` sequences for legacy handoffs. Do not change outcome vocabulary to encode waiting for a human. Review is an orthogonal control gate.

### 7.2 Proposed new event vocabulary

All names below are new proposals; the current 19-type DDL does not yet accept them.

| Event | Required structured content | Fold/head effect |
|---|---|---|
| `LEGACY_BASELINE_SET` | Cycle, baseline revision, prior revision if present, cumulative amount >= 0, provenance code, batch/evidence reference | Replace effective legacy accounted amount; update its witness and provenance. No provider outcome, reservation, or authorization is created. |
| `MIGRATION_EXCEPTION_RECORDED` | Exception ID, cycle/source revision, typed reason, evidence reference | Block money-enabling work for the affected scope; preserve monetary and execution history. |
| `MIGRATION_EXCEPTION_RESOLVED` | Exact exception ID, replacement evidence/revision, resolution code and actor | Resolve only that exception after validation. Does not approve a request or clear unrelated parks. |
| `PAYMENT_REVIEW_REQUESTED` | Review ID, open ordinal, request amount, cycle/baseline revision, review context hash/version, protected display reference | Set review to pending; keep existing reservation. |
| `PAYMENT_REVIEW_APPROVED` | Exact review ID, ordinal, amount and context hash, authenticated actor | Approve that pending review only, after current-context validation. No paid or reserved delta. |
| `PAYMENT_REVIEW_REJECTED` | Exact review ID, ordinal/context, authenticated actor, reason code | Reject that pending review; block POST, with no automatic release or new request. |
| `PAYMENT_REVIEW_INVALIDATED` | Exact review ID, ordinal, reason code, triggering revision reference | Make that authorization permanently unusable; no money effect. |

Baseline provenance codes include `HANDOFF_ASSUMED`, `NO_HANDOFF_IN_COMPLETE_SCOPE`, and `MANUAL_RECONCILED`. The last means an explicitly recorded human reconciliation input, not an independent provider confirmation. The normal happy path adopts a baseline automatically; it still has only one human payment-approval step.

An exception-resolution command may append replacement baseline and resolution events in the same transaction. Re-evaluate new requests only after all required events/head effects have been applied. Resolving a record by ID cannot clear a still-current coverage or mapping veto.

### 7.3 Typed fields and shape rules

Add structured fields or a typed immutable payload schema for migration cycle, baseline/prior revision, evidence reference, exception ID, review ID, review context hash/version, and reason code. The fold must never extract these from free-text `DETAIL`.

Suggested shape requirements, to be expanded into the existing complete R/N/O matrix:

- Baseline events have no request ordinal, provider key, provider outcome, or UETR. Their amount is nonnegative and currency/scope-bound. Record insertion time normally; source handoff time is separate evidence.
- Review requested/approved events restate the opened amount; it must equal that ordinal's immutable opening amount. Approval also matches the exact requested context.
- Review rejected/invalidated events carry no monetary effect; require the review ID, its ordinal, and the appropriate typed reason. Provider/UETR fields remain null.
- Every review requires a stable review ID, scoped to one payment/request. One pending review can produce only one user decision; duplicate identical commands return the recorded result, while conflicting decisions fail.
- Baseline adoption is idempotent by payment scope, cycle, and semantic baseline revision. Reusing that identity with conflicting content is an exception, not an update.
- Request review events ordinarily target the open ordinal. If an invalidation is emitted as part of closure, insert it while that ordinal is still open. Terminality itself makes the authorization unusable; do not introduce an unrestricted closed-ordinal write door just to append an audit decoration later.

The existing `APPROVAL_REF` is reserved for the typed **ops dual-control approval** protocol and has existing shape/consumption rules. Use a distinct `REVIEW_ID` for ordinary delta authorization. A reviewer click is not an ops verified-outcome approval, a retry-budget reset, or permission to bypass the current supersession gates.

For review-required requests, extend `POST_STARTED` to carry the approved review ID and context hash. These refer to the durable authorization for that same request; they do not allocate a new approval per wire attempt. Persist whether review is required on the opening event (policy code/version if applicable), so a later configuration change cannot silently exempt an existing request. Pre-extension open requests in a re-entering scope default to blocked until explicitly brought through this review protocol.

### 7.4 Fold and witness changes

In addition to existing outputs, derive:

```text
legacy_accounted, adopted_cycle, adopted_baseline_revision
active migration exceptions
review requirement for the open request
active review ID, context, and pending/approved/rejected/invalidated status
```

Extend the synchronous witness comparison, rebuild, drift checks, and fold deployment comparison to include legacy money and safety-relevant review/baseline state. A head rebuild must not copy the latest staging amount over a recorded baseline or reconstruct an approval from mutable UI data.

The external readiness pointer remains a fail-closed runtime veto. Pure replay can explain a historic approval from events; a live submission additionally checks that the current published cycle and source revision still match. This is analogous in purpose to the existing external posting freeze, not a hidden monetary input to the fold.

### 7.5 Database and command backstops to update together

1. Extend the event-type enumeration, source/field shapes, generated checks, and shape parity tests. Existing Oracle null-handling rules still apply.
2. Extend the opening-amount backstop to subtract `LEGACY_ACCOUNTED` as well as `PAID_TOTAL`; keep positive amount, one-open-request, ordinal claim, and key-echo checks.
3. Maintain the baseline witness in the per-event apply order. Validate prior revision and scope binding when adopting replacements.
4. Validate review amount/ordinal/context binding and serialize decision uniqueness under the head lock, with a database uniqueness backstop for decision identity.
5. Add a veto at `POST_STARTED` for a missing, wrong-request, rejected, or invalidated required review. The canonical validator also checks the full current context and migration readiness; a non-null review ID alone is insufficient.
6. Keep existing outcome amount equality, version continuity, ops approval consumption, contradiction handling, and release guards unchanged.

This is a schema and fold extension, not only an API/UI addition. Updating the calculation while leaving the old `required - paid` opening trigger in place would reject legitimate deltas or undermine the intended independent witness.

## 8. Transaction boundaries and concurrency

### 8.1 Lazy adoption and opening

```text
read immutable published data; prepare mapping/enrichment outside locks

transaction:
  acquire readiness / trade / payment locks as applicable in fixed order
  verify current published cycle, batch completeness, and record revision
  fold and witness-check
  reject stale business/enrichment inputs
  if source is invalid: record exception; no opening
  else if baseline changed:
    invalidate affected live review
    handle any obsolete open request under existing release rules
    append LEGACY_BASELINE_SET; apply its witness
  after all effects, evaluate existing gates and combined money state
  if a positive difference may open and instruction is ready:
    append REQUEST_OPENED; apply reservation/ordinal effect
    append PAYMENT_REVIEW_REQUESTED; apply pending-review effect
  commit
```

If a request may already have executed, adoption may record new source facts but cannot release that request, create a successor, or use approval to suppress its evidence. Keep the appropriate block and continue resolution. Do not run the standing rule halfway through a multi-event correction/adoption command.

### 8.2 Approval command

```text
approve(review_id, expected_context_hash, command_id)

transaction:
  check authenticated permissions
  acquire applicable locks; fold and witness-check
  verify migration readiness and adopted source currency
  require exact review, open ordinal, pending state, and matching current context
  require that this request is still a legal payment candidate
  append PAYMENT_REVIEW_APPROVED
  update review projection and make work discoverable by existing scanner
  commit
```

An identical repeat after an API timeout returns success with the recorded decision. Concurrent approve/reject operations serialize; the loser receives the existing decision or a conflict. If the context changed after page load, reject the stale approval and present a refreshed proposal. The server supplies actor identity and validates authorization; neither a client-supplied reviewer nor a context hash alone proves permission.

If an approval is already committed and now invalidated, replaying the same command may report its historical result, but must never make it effective again. Current review state must be returned separately.

### 8.3 Submit and result recording

```text
transaction 1:
  fold + witness-check + current migration readiness
  require the sole open ordinal, existing submit/retry eligibility,
          current valid required review, and all existing money gates
  append POST_STARTED with key, actual payload hash, review reference
  commit

outside transaction:
  apply existing freeze and mandatory pre-wire recheck,
  extended to review invalidation and migration pause
  call provider only if still permitted

transaction 2:
  record result through existing evidence protocol
```

Approval does not bypass fleet freeze, provider idempotency, trust-age, release predicates, or ambiguity. A skipped call after a durable `POST_STARTED` is still handled conservatively by the existing claim-resolution protocol. Do not erase it or assert non-execution merely because this worker believes it skipped the wire.

## 9. All payment-capable paths share the same gate

The conceptual gate is:

```text
existing money / evidence / retry eligibility
AND applicable local migration coverage is READY
AND adopted cycle and source facts are current
AND no relevant migration exception
AND required review is valid for this request and current context
AND operational posting controls permit dispatch
```

Apply it to first submission, automatic retry, manual retry/reprocess, lease recovery, downgrade-for-repost execution, queued/outbox dispatch, and any successor opened after an outcome. A new successor gets a new review; an approved predecessor cannot authorize it.

Scanner head filters are an optimization only. A worker selected before refresh must recheck in the canonical command and at the existing pre-wire boundary. An SDK's hidden POST retry must not create an uncontrolled second submission outside the existing attempt/idempotency protocol.

Evidence recording remains active when payment is paused or approval is pending. A late accepted/settled response must still close or reconcile the right original ordinal. Do not put a broad migration/approval check in front of all writes and accidentally suppress evidence needed for recovery.

Waiting for import or review produces no `POST_STARTED` and consumes no posting retry budget. Refresh and approval do not reset budgets, allocate replacement keys, or grant downgrade permission. Preserve existing retry accounting, including its conservative treatment of durable claims whose actual wire execution is uncertain. Avoid a hot scanner loop for human-owned pending reviews; approval/rework makes the request eligible for normal level-triggered discovery.

## 10. Refresh, pause, return to legacy, and re-entry

### 10.1 Operational sequence

1. Upstream arranges the processing transition. Payment does not route the portfolio.
2. Pause local money-enabling work for the affected coverage before source cutover/publication. Merely stopping inbound XML is insufficient for existing retries.
3. Drain workers already permitted to submit, or reliably prevent their outgoing calls. Keep already-dispatched, possibly-executed requests visible to reconciliation.
4. Export a consistent, complete legacy view at the agreed manual-processing boundary. The business handoff must account for XMLs still awaiting manual processing under the legacy convention.
5. Import and validate a new immutable batch; on re-entry assign a new cycle. Atomically publish it while local submission remains paused.
6. Resume local processing when the upstream transition and source-readiness conditions are complete. Each touched scope adopts the new revision/cycle lazily; every positive delta request requires a current review.

Preparing a batch can occur in advance while normal work continues. Publishing a changed live baseline/cycle requires the pause boundary above. A harmless repeat upload of identical current content need not invalidate approvals.

The existing event model explicitly accepts a commit-to-wire race: a database change cannot unsend a network request. The pre-wire check narrows this gap but is not a proof of immediate revocation. A migration pause is complete only after already-authorized workers are accounted for; an approval revocation after dispatch does not cancel a payment already in progress. This document does not claim stronger fencing than the actual dispatch mechanism provides.

### 10.2 What is preserved across moves

Keep all heads, event streams, request ordinals, idempotency keys, new-system paid outcomes, unresolved claims, and historical reviews. Never initialize an already-known trade from XML as if it were new.

No reverse import to the legacy database is required. Users consult external reconciliation during legacy processing. On return, the imported cumulative legacy amount must exclude the new system's already-recorded payments. If that cannot be established from the source convention, the user supplies documented reconciliation evidence through the exception/correction path.

An old queued task cannot inherit validity from an old cycle. An old review cannot become valid again because the portfolio returns. Re-entry may produce no payment at all; in that case no artificial zero-value approval is requested.

A disaster restore is separate from routine re-entry. If the new event history is unavailable or restored behind possible submissions, retain the existing restore freeze/reconciliation protocol. A fresh legacy import cannot reconstruct missing new-system claims or prove they never executed.

## 11. Failure-prone details and worked examples

| Pitfall | Required response |
|---|---|
| Import is retried with another batch ID | Same semantic baseline is not added again; adoption identity is independent of transport batch |
| XML exists but has no usable instruction identity | Do not guess whether it duplicates another instruction; exception/manual handling |
| XML is missing but handoff exists | Preserve the marker and block the applicable scope/trade |
| No record is found during partial import | Wait; absence is not zero until coverage is complete |
| Re-entry overwrites the head's paid amount | Prohibited; legacy and new-system contributions remain separate |
| Reviewer approved 50, refreshed baseline makes the delta 20 | Invalidate, safely dispose of any unsubmitted obsolete request, create/review 20 as a new request |
| Reviewer approved 50, required increases and the delta becomes 80 | Old approval cannot authorize 80; re-evaluate and review |
| Same 50 now goes to a different recipient | New business context; no authorization reuse |
| Existing retry receives no new XML after re-entry | The common submission gate still detects the new cycle and blocks stale approval |
| Review rejected, scanner notices positive difference | Do not automatically cancel/recreate the same proposal; retain rejection control |
| Refresh arrives while previous POST is ambiguous | Retain that claim and resolve it; no replacement authorization from a refreshed baseline |
| Settlement arrives during review or migration pause | Record it through existing evidence handling; invalidate incompatible pending work as necessary |
| Baseline amount decreases after a record disappears | Treat as an exception, not an automatically payable increase |
| Approval API times out after commit | Idempotent replay returns the original decision; no second effect |
| Review or baseline fields are placed only in `DETAIL` | Invalid design: use typed fields/payloads with explicit validation |
| An ordinary review ID is passed to the ops correction door | Refuse; ordinary delta approval and existing dual-control ops approval are different authorities |

Example with an existing reservation: required 150, legacy 100, new paid 0, open request 50, reserved 50. Generic shortfall is zero, but approving the existing request is valid: its amount equals `150 - 100 - 0`. A validator that requires a positive generic shortfall at approval would deadlock every correctly reserved review.

Example with ambiguous execution: required 180, legacy refreshed from 100 to 130, request 1 for 50 has a `POST_STARTED` without a conclusive result. The system must not decide that the new delta is 50 or 0 by ignoring the claim. Reservation and ambiguity remain; user approval of a new proposal cannot settle the uncertainty. Reconcile request 1 first.

## 12. Verification and implementation checklist

These are acceptance scenarios for the implementation, not claims that this documentation change has implemented or run them.

| Area | Required scenario and assertion |
|---|---|
| Lazy import | Import a historical portfolio; no payment head/event is created until a relevant command touches a scope |
| Complete absence | A complete batch with no handoff can establish zero; an incomplete/unmapped batch cannot |
| Missing evidence | Handoff with absent XML blocks even future scopes under the affected trade |
| Deduplication | Re-upload and re-adopt identical records/cumulative totals without double accounting |
| Revision ordering | Older batches cannot supersede a newer adopted source revision; same identity with conflicting content fails |
| Refresh | Replace 100 with cumulative 130 while preserving new-system paid 50; total accounted is 180 |
| Decrease | Missing source data cannot manufacture a payable delta; explicit correction remains auditable |
| Replay | Re-fold after changing parser deployment and staging publication; old adopted money remains unchanged |
| Opening backstop | Oracle rejects an opening that ignores legacy accounted amount or exceeds the current full difference |
| Review binding | Refuse approval for wrong ordinal, amount, recipient, cycle, baseline revision, or stale required sequence |
| Reservation | Approve an otherwise valid reserved request even though generic shortfall is zero |
| Concurrency | Simultaneous approve/reject yields one decision; duplicate command is idempotent |
| Retry | Same-context legal retry reuses approval; new request or changed material context cannot |
| Rejection | Scanner does not loop into newly created proposals after rejection |
| All entry points | Direct retry/reprocess/recovery without fresh XML still checks current migration and approval state |
| Publish race | A preselected worker cannot proceed on old readiness after publication; already-committed dispatch is handled by drain/claim protocol |
| Ambiguity | Refresh or review cancellation cannot release a possibly-executed request |
| Evidence | Paused or pending-review scopes still ingest late provider evidence correctly |
| Atomicity | Crash between baseline/review events and head effects rolls back the complete payment transaction |
| Crash recovery | Partial batch remains unpublished; approval committed before crash remains discoverable by the scanner |
| Re-entry | A new cycle invalidates old authorization without resetting request ordinals or new-system outcomes |
| Backstops | Extend real-Oracle shape, amount, review-binding, and per-event apply-order tests; retain existing correction/release suites |

Implementation work spans the import API/adapter, batch publication, lazy adoption command, fold and witness schema, approval API/UI, common submission validator, scanner selection, and the tests above. Update all monetary comparisons, request views, explanatory output, rebuild tools, and deploy gates together; do not ship only the UI or event names.

## 13. Integration references and scope boundaries

- [Event model v2](event-model-v2.md): sections 3–6 define request identity, fold, witness/backstops, and correction semantics; section 9 defines freeze, write-ahead, pre-wire recheck, and permanent history.
- [Schema reference](01-event-table-schema.md): sections 2/2.2 define the currently closed event shapes and `APPROVAL_REF`; sections 4–6 define vocabulary, fold, lock protocol, and opening/release backstops; section 7 defines upstream admission/fan-out.
- [Event-model index](00-README.md): existing design context and assurance checklist.

The explicit extensions are the assumed legacy accounting component, local migration evidence/readiness, lazy baseline events, and request-scoped delta authorization. Upstream routing, legacy manual execution and reconciliation, provider confirmation semantics, and the existing ops dual-control authority remain with their current owners. There is no standalone baseline approval, eager population of historical payment streams, or automatic reverse synchronization to the legacy database.
