> **Purpose:** HOW to verify each go-live checklist item Q1–Q29: the concrete check to perform, the evidence artifact a PASS must attach, and who signs. Companion to 17-go-live-checklist.md, which stays the scorecard.
> **When to use this file:** Continuously from Phase P8 (evidence accumulates per phase); executed in full at GO-04. Also whenever a single Q flips status.
> **Depends on:** 17-go-live-checklist.md; 10-test-matrix.md; 19-local-task-execution-report-template.md; requirment-v4.md §18.
> **Used by:** GO-04 go/no-go; the human driver assembling the evidence pack.
> **Safe to transfer:** yes
> **Contains local code names:** no (evidence artifacts will — the pack stays local)

# Q-V. Go-live verification procedures

Rules of the game:

- A Q is PASS only with its evidence artifact attached in the pack
  (§V.2). "The tests are green" without a linked run is NOT a PASS.
- Test evidence = the CI/run ID + the test names from 10-test-matrix.md,
  executed against the release candidate build, on real Oracle where
  the matrix says INTEGRATION/OPERATIONAL.
- §18 BLOCKING items (Q1–Q4, Q28) are non-waivable. Any other FAIL
  needs a named owner + dated remediation plan recorded in the
  checklist row to proceed as an accepted risk.
- Sign-off roles: DEV (implementing team lead), OPS (operations owner),
  TL (tech lead), PO (product owner), EXT (external party in writing).

## V.1 Per-item procedures

| Q | How to verify (do this) | Evidence artifact | Sign-off |
|---|---|---|---|
| Q1 | Read the filed upstream confirmation (ask 5) — it must state snapshot schema + within-snapshot uniqueness as a GUARANTEE ("usually" = FAIL). Run the B-01/IN-02 intake validation tests (within-snapshot tuple-collision seed → whole-snapshot failure + anchors; test-ID reference corrected 2026-07-11 — T-01 is key generation, not intake). Confirm the PO-9 answer recorded in §18 AND the §6.1 fan-out implements it; confirm TL-16's round-5 answer IMPLEMENTED (S-10 trade_snapshot_state + the IN-02 admission gate; T-35 green). Read the filed ask-8 store contract (§18-0(d)) — it must state fetch-by-id sanction, versioned-id stability, consistent reads, IMMUTABILITY (corrections = new id/version), and retention ≥ the ops/tie SLA as GUARANTEES. | signed upstream doc; ask-8 store contract; intake-test run ID; §18 answer entries with date+source | TL + EXT(upstream) |
| Q2 | Confirm CT-02..05 executed against the REAL sandbox (not mocks): (a) same-payload dedupe, (b) different-payload reject with distinguishable code, (c) retention-TTL edge with the TTL stated in writing, (d) post-reject re-POST behavior recorded. Confirm the re-run procedure for engine releases is scheduled (calendar/pipeline entry). | CT run transcript + engine responses captured; TTL letter; re-run schedule link | TL + EXT(provider) |
| Q3 | CLOSED round 10 (engine-owned calendar, §18-2): verify the engine's WRITTEN any-time-submission line is filed, the CA-1 table carries the late-submission response class (or its recorded absence), and NO local cutoff config/constants exist in the target env (grep the deployed config). | written line filed; CA-1 row; config grep output | TL |
| Q4 | Verify the OP-01 audited operation deployed (endpoint restricted to the enterprise ops role — attempt it with an unauthorized identity, must fail; 2026-07-11 boundary: authorized application endpoint), OP-02 suite green on real Oracle (incl. the marker-blocks-successor case AND the §9.3 approval-workflow negative set), OP-03 drill report SIGNED by the ops owner with real operators + real ticket. Alternative path only with TL-10 ∧ TL-5 letters + PO re-confirmation. | endpoint authz config + refused-attempt log; T-24 run ID; signed drill report | OPS + TL |
| Q5 | Diff deployed schema vs CA-4 DDL (constraints VALIDATED state, both triggers live, artifact-4 index list present). Run the migration test incl. dual-run (old+new app versions concurrently). EXPLAIN one scanner query per standing scan → each rides its ACTIVE-row-bounded index. Round 6: verify T-36 green + the S-11 bootstrap coverage report filed + the Section M trade-admission enablement gate recorded (old consumers drained BEFORE enforcement). Round 7: POINTER coverage verified — zero NULL-pointer rows among wire-capable trades before the legacy assembly path is removed (residuals individually dispositioned); rollback rehearsal covers the transitional-assembly flag. | schema diff output; migration test run; captured plans; T-36 run + coverage report | DEV + DBA |
| Q6 | ST-01..03 suites green; run the legality verification (the artifact-6 property-based L1–L8 sweep — every illegal tuple write refused by CHECK/trigger — plus T-25 for the trigger layer; reference corrected 2026-07-11 — T-15 is UETR persistence, not legality); audit: grep merged code for UPDATEs on the two tables outside the shared CAS helpers (must be none). | test run IDs; grep/audit note in report format | DEV + TL |
| Q7 | ST-05 inventory: every legacy-status rule site listed with disposition (removed / display-only). Grep release build for business logic keyed on display labels or blocked_reason — zero hits. | ST-05 inventory doc; grep output | DEV + TL |
| Q8 | T-03/08/09/10 green (write-ahead identity, crash/retry/restore identity stability). Kill-test evidence: worker killed between claim-commit and HTTP call → row lands MAYBE, resolver recovers (T-08 trace). | run IDs + T-08 trace log | DEV |
| Q9 | T-02 golden vectors green IN CI (build fails on drift — verify by mutating a vector locally once and observing the failure, then revert). | run ID + the observed-failure note | DEV + TL |
| Q10 | Same CT evidence as Q2 filed in the pack + CT-07 SDK checks (UETR returned + field name; caller key accepted; dedup keys on caller key). | CT-07 run/responses | TL |
| Q11 | T-17 green: I6 second-active-insert refused; UNIQUE(idempotency_key) violation refused; DUPLICATE_REQUEST routed to MAYBE+query (not error, not fresh key). | run ID | DEV |
| Q12 | T-08/09/10 + ST-10 green: lease expiry both branches (ENRICH re-claim; POST → MAYBE, never re-claimed); graceful shutdown drains in order. | run IDs | DEV |
| Q13 | CLOSED round 10 with Q3 (no local calendar exists; nothing to configure per environment). | — | OPS |
| Q14 | T-22/23 green: resolver outcomes incl. NOT_FOUND trust-age both branches, §9.2 downgrade fires only where repost_permitted passes, escalation once-per-episode on maybe_since, tier-2 re-page, parked-row stability (no park⇄unpark cycle). | run IDs | DEV + TL |
| Q15 | = Q4 evidence (operation suite + drill), filed under the test pack too. | T-24 + drill report | OPS |
| Q16 | T-26/27 green: I1–I6 invariant suite, redelivery-safe decrement (replay → 0 rows → no double release), EXCLUDED: no confirmed movement on mismatch; overpay latch one-way incl. the §13 cross-stream trace. | run IDs | DEV + TL |
| Q17 | T-25 + S-06 evidence: evidence-flag settable only by the two sanctioned setters (grep call sites — exactly: authoritative-negative path, OP-01); trigger refuses manual release on a seeded MAYBE row (raw-SQL demo captured); session-flag does not leak across pooled connections (pool test). | run IDs; grep output; demo transcript | DEV + TL |
| Q18 | T-30 green: terminal-evidence CRITICAL fires on NEW event_id + zero-row CAS vs TERMINAL row; benign redelivery silent; per-obligation count sanity ticket fires. Verify alert ROUTING to the real channel (send test alert). | run ID; alert-channel screenshot/log | DEV + OPS |
| Q19 | T-29 green: seeded I1/I2 violation PAGES; read-skew case does NOT page (snapshot + locked re-check); L9 totality check detects a seeded marker-less REJECTED row. Verify the page reaches the on-call rota. | run ID; page receipt | OPS |
| Q20 | T-32 green: every §15 entry fires on its seeded condition ON ITS ANCHOR CLOCK, carries a runbook link; alert rollup groups outage collateral into one incident (breaker-OPEN storm test); dead-gauge alerting (stop a metric source → alert); config-ordering validation rejects a bad set at startup (T-32's mis-ordered-config case — reference corrected 2026-07-11). | run ID; dashboard screenshots; rejected-config boot log | OPS + TL |
| Q21 | Open every runbook stub from its alert definition link (no 404s); aged-MAYBE runbook walks to an actual exit (O10/TL-10). | link audit note | OPS |
| Q22 | D-11 baseline suite green on the release candidate; zero unresolved BUSINESS_RULE_CHANGE_REQUIRED reports in the tracker. | run ID; tracker export | DEV + PO |
| Q23 | Rollback REHEARSED in a pre-prod env (not just documented): execute GO-05's rollback from the mid-rollout state; record timings; point-of-no-return step identified in the plan. | rehearsal report | OPS + TL |
| Q24 | GO-02 shadow soak report: duration per plan, zero unexplained divergences (each divergence dispositioned in writing). | soak report | TL + PO |
| Q25 | Walk Section K (Q-01..Q-21): every BLOCKING answered (verbatim answer + source + date), every HIGH answered or risk-owned; §16.6 config inventory has an owner per row, no TBD values in the target env. | K register export; config inventory | TL + PO |
| Q26 | T-31 green incl.: multi-payment trade returns ALL obligations (count never an error); anchors show DATA_VALIDATION_FAILED; MAYBE shows rank-1 PAYMENT_OUTCOME_UNKNOWN never SYSTEM_UNAVAILABLE; unavailable ≠ stale. | run ID | DEV |
| Q27 | IN-09 checklist per environment: auto-commit off/ack-after-commit verified from consumer config dump; earliest reset; DLT wired + paged; retention chain check job live (inbox > kafka ≥ replay) with named owner. | config dumps; retention-check job link | OPS |
| Q28 | Final aggregate: Q1–Q4 all PASS with evidence; §18 register shows zero open BLOCKING; the four B-cards closed in the tracker. | this checklist, completed | PO + TL + OPS |
| Q29 | Run T-33 on real Oracle incl. the refusal cases (MAYBE reject refused at code AND trigger layer; missing ticket / identical approvers / unauthorized role refused) and reprocess-snapshot idempotency + latch respect + purged-xml clean refusal + FABRICATION refusal (a non-tying or wrong-business_id document invokes NO relaxation — the server recomputes the tie; no ordering parameter exists). Audit endpoint authorization (enterprise ops role only — attempt with an unauthorized identity). Open each §15 queue alert definition → its view link resolves. Fire a seeded tie-conflict → inspect that the record carries the IDENTIFIERS (business_id, tied ordering, XML storage id, masked diff — §6.7 REVISED) and that the XML re-fetch by id works. | T-33 run ID; endpoint authz config; seeded tie record capture | OPS + TL |

## V.2 The evidence pack

Assemble ONE folder (local, like everything naming local systems):

```text
/golive-evidence/
  Q01-.. one subfolder per Q: the artifacts from V.1's Evidence column
  test-runs.md      — matrix test ID → run ID/link → date → build SHA
  signoffs.md       — Q → role → name → date (the four roles above)
  open-risks.md     — every non-BLOCKING FAIL: owner, plan, date
```

A Q without its subfolder is not PASS, whatever the scorecard says.

## V.3 GO-04 go/no-go script (60–90 min meeting)

```text
1. Walk Q1–Q4 + Q28 first (non-waivable). Any gap → NO-GO, stop here.
2. Walk the remaining Qs in order; for each FAIL read the owner+plan
   aloud and record explicit PO acceptance (or NO-GO).
3. Spot-check evidence integrity: open 3 randomly chosen Q subfolders
   and re-run 1 randomly chosen matrix test live against the RC build.
4. Confirm day-1 operational readiness: on-call rota loaded, alert
   channels tested (Q18/Q19 receipts), freeze-toggle access list
   verified, rollback runbook (Q23) at hand.
5. Record the decision + conditions in signoffs.md. GO requires PO,
   TL, and OPS signatures on the same dated entry.
```
