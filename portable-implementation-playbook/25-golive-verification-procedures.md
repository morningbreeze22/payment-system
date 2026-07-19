> **Purpose:** HOW to verify each go-live checklist item Q1–Q31: the concrete check to perform, the evidence artifact a PASS must attach, and who signs. Companion to 17-go-live-checklist.md, which stays the scorecard.
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
- TWO non-waivable classes (round 16):
  (1) §18 BLOCKING (Q1–Q4, Q28) — the external contract proofs.
  (2) MONEY_SAFETY_BLOCKING — Q5a, Q5b, Q8, Q9, Q11, Q12, Q14, Q16, Q17, Q27, and Q29's minimal exit set:
      the duplicate-payment / wrong-release / unrecoverable-
      uncertainty controls. A FAIL or MISSING EVIDENCE in either
      class is NO-GO. No owner+plan waiver exists for them;
      reclassifying an item OUT of MONEY_SAFETY_BLOCKING requires
      a new architecture/safety review, never meeting-time risk
      acceptance.
- Only items OUTSIDE both classes may proceed on a FAIL as an
  accepted risk, with a named owner + dated remediation plan
  recorded in the checklist row.
- Sign-off roles: DEV (implementing team lead), OPS (operations owner),
  TL (tech lead), PO (product owner), EXT (external party in writing).

## V.1 Per-item procedures

| Q | How to verify (do this) | Evidence artifact | Sign-off |
|---|---|---|---|
| Q1 | Read the filed upstream confirmation (ask 5) — it must state snapshot schema + within-snapshot uniqueness as a GUARANTEE ("usually" = FAIL). Run the B-01/IN-02 intake validation tests (within-snapshot tuple-collision seed → whole-snapshot failure + anchors; test-ID reference corrected 2026-07-11 — T-01 is key generation, not intake). Confirm the PO-9 answer recorded in §18 AND the §6.1 fan-out implements it; confirm TL-16's round-5 answer IMPLEMENTED (S-10 trade_snapshot_state + the IN-02 admission gate; T-35 green). Read the filed ask-8 store contract (§18-0(d)) — it must state fetch-by-id sanction, versioned-id stability, consistent reads, IMMUTABILITY (corrections = new id/version), and retention ≥ the ops/tie SLA as GUARANTEES. | signed upstream doc; ask-8 store contract; intake-test run ID; §18 answer entries with date+source | TL + EXT(upstream) |
| Q2 | Confirm CT-02..05 executed against the REAL sandbox (not mocks): (a) same-payload dedupe, (b) different-payload reject with distinguishable code, (c) retention-TTL edge with the TTL stated in writing, (d) post-reject re-POST behavior recorded. Confirm the re-run procedure for engine releases is scheduled (calendar/pipeline entry). ROUND-16 EVIDENCE STANDARD: every CT record carries redacted RAW request/response bytes, canonical payload hash, idempotency key, timestamps, correlation/provider reference, environment + endpoint/API/SDK/engine versions, provider-side EXECUTION COUNT (a status query showing one visible payment is NOT execution-count proof — provider dedup/query semantics can collapse records; obtain an execution/audit count or settlement-ledger equivalent), status-query result, expected/actual outcome, reviewer signature; raw evidence preserved immutably, never prose transcript or screenshot alone. Plus the provider PRODUCTION-PARITY statement: which idempotency/TTL/error-code/query-retention behaviors are identical to production, with exceptions listed; provider + TL sign off on the CA-1/2/3 mappings produced from the observations. | CT run transcript + RAW captures + execution-count proof; TTL letter; parity statement; re-run schedule link | TL + EXT(provider) |
| Q3 | CLOSED round 10 (engine-owned calendar, §18-2): verify the engine's WRITTEN any-time-submission line is filed, the CA-1 table carries the late-submission response class (or its recorded absence), and NO local cutoff config/constants exist in the target env (grep the deployed config). | written line filed; CA-1 row; config grep output | TL |
| Q4 | Verify the OP-01 audited operation deployed (endpoint restricted to the enterprise ops role — attempt it with an unauthorized identity, must fail; 2026-07-11 boundary: authorized application endpoint), OP-02 suite green on real Oracle (incl. the marker-blocks-successor case AND the §9.3 approval-workflow negative set), OP-03 drill report SIGNED by the ops owner with real operators + real ticket. Alternative path only with TL-10 ∧ TL-5 letters + PO re-confirmation. | endpoint authz config + refused-attempt log; T-24 run ID; signed drill report | OPS + TL |
| Q5a | Diff deployed schema vs CA-4 DDL (constraints VALIDATED state, both triggers live, artifact-4 index list present). Run the migration test incl. dual-run (old+new app versions concurrently). EXPLAIN one scanner query per standing scan → each rides its ACTIVE-row-bounded index. GREENFIELD (round 10 — the bootstrap/pointer evidence set was RETIRED: T-36, S-11 coverage, the enablement gate, and pointer coverage may NOT be required here): instead verify trade_snapshot_state deploys EMPTY, the S-10 creation path is live, and T-35 + T-37 are green (admission + fence + absence lifecycle). Round 20: verify the RUN-2 queries + scope predicate are REVIEWED and manifest-bound (query checksum recorded) — Q5a must be fully PASS before GO-04 can issue the conditional authorization. | schema diff output; migration test run; captured plans; T-35/T-37 run IDs; reviewed RUN-2 query pack in manifest.yaml | DEV + DBA |
| Q5b | CUTOVER_POPULATION_GREENFIELD RUN 2 (file 26 T.1): inside GO-03's F0 activation window — after old in-scope writers are drained/fenced (or under change freeze) — re-run the reviewed population queries IMMEDIATELY before enabling F0; counts must be ZERO; DBA/TL sign; manifest closure (environment, scope predicate, query checksum, timestamp, RC/config version, owner, reviewer); stale on env/predicate/restore/seed/old-writer-activity/rollback/query change; post-activation re-enables use the ADMISSION-COVERAGE form instead (runbook RB-F0). At GO-04 this row is PENDING-CUTOVER (legal only with Q5a PASS — round 20); a nonzero count or missing signature ABORTS the change window: NO-GO + architecture review (bootstrap restoration considered — git 9a53c75). | RUN-2 result + signatures bound in manifest.yaml; abort record if triggered | DBA + TL |
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
| Q23 | Rollback REHEARSED in a pre-prod env (not just documented): execute GO-05's rollback from the mid-rollout state; record timings; point-of-no-return step identified in the plan. (Round 19: GO-05 runs BEFORE the GO-04 authorization, so this evidence EXISTS at the meeting.) | rehearsal report | OPS + TL |
| Q24 | GO-02 shadow soak report: duration per plan, zero unexplained divergences (each divergence dispositioned in writing). | soak report | TL + PO |
| Q25 | Walk Section K (Q-01..Q-22): every BLOCKING answered (verbatim answer + source + date), every HIGH answered or risk-owned (Q-22 is FUTURE/BLOCKING-FOR-DR — open is acceptable for MVP go-live, recorded as such); §16.6 config inventory has an owner per row, no TBD values in the target env. | K register export; config inventory | TL + PO |
| Q26 | T-31 green incl.: multi-payment trade returns ALL obligations (count never an error); anchors show DATA_VALIDATION_FAILED; MAYBE shows rank-1 PAYMENT_OUTCOME_UNKNOWN never SYSTEM_UNAVAILABLE; unavailable ≠ stale; amount-series stamp cases (100+20 rows stamped 100/120; NULL renders "not captured (pre-F0)"). PLUS the RG-06 creation-stamp suite green (top-up 100→120; reject-retry stamps 100 NOT 200; downgrade/re-POST invariance; SQL-inventory no-UPDATE assertion). PRE-CUTOVER evidence only — the first-post-F0 stamp check belongs to GO-03 post-enable closure, never here (aa4399c M1). | run IDs (T-31 + RG-06 stamp suite) | DEV |
| Q27 | IN-09 checklist per environment: auto-commit off/ack-after-commit verified from consumer config dump; earliest reset; DLT wired + paged; retention chain check job live (inbox > kafka ≥ replay) with named owner. | config dumps; retention-check job link | OPS |
| Q28 | Final aggregate: Q1–Q4 all PASS with evidence; §18 register shows zero open BLOCKING; the four B-cards closed in the tracker. | this checklist, completed | PO + TL + OPS |
| Q30 | Run the security/supply-chain evidence set on the EXACT RC build (round 16): SAST + dependency-vuln + SBOM + license + secret scans; dump and review Kafka ACLs, DB grants, service/ops-role privileges (least privilege — diff against the sanctioned role spec), endpoint authn/authz config, TLS + cert-rotation config; run the masking test suite across success AND failure paths (structured logs, traces, metric tags, exceptions, dead letters — seeded sensitive values must never appear raw); verify config/secret provenance from the sanctioned vault (no inline secrets in the deployed config dump); verify the §14.1 journal protections (INSERT-only grants enforced, restricted audit role only, DB-audited reads live, no lower-env replication — T-38 case G run ID) AND the T-38 switch-OFF INERTNESS sub-case (case F: switch OFF ⇒ zero inserts, zero errors, posting unaffected — this sub-case gates PAYMENT go-live because the rider code ships even while OFF; review 1d8a650 M2) AND the §14.1 ENABLEMENT GATE (which additionally requires the FULL applicable T-38 A–J evidence set — never case G alone — before the switch may turn ON): record the switch state; journal writes may be ON only with encryption at rest ENABLED + evidenced (or an approved expiry-dated compensating-control exception) AND the compliance-approved retention schedule on file; an OFF journal never blocks payments go-live. | scan reports bound to the RC SHA in manifest.yaml; ACL/grant/authz dumps; masking-test run ID; provenance note; T-38G run ID; enablement-gate record | TL + OPS |
| Q31 | Run the capacity evidence set (round 16): load test at peak + post-outage burst per the §16.5 volume NFR; drive connection-pool/bulkhead saturation and record degradation behavior (no cross-dependency starvation — §16.1 pool math); scanner backlog recovery after a seeded outage window; provider quota shaping respected under load (§9.5 budget vs TL-13); card-read latency percentiles under load; resource alarms fire before exhaustion; record headroom under the tested RC configuration. | load-test report + seeds/config; saturation + recovery traces; alarm receipts; headroom summary | OPS + TL |
| Q29 | Run T-33 on real Oracle incl. the refusal cases (MAYBE reject refused at code AND trigger layer; missing ticket / identical approvers / unauthorized role refused) and reprocess-snapshot idempotency + latch respect + purged-xml clean refusal + FABRICATION refusal (a non-tying or wrong-business_id document invokes NO relaxation — the server recomputes the tie; no ordering parameter exists). Audit endpoint authorization (enterprise ops role only — attempt with an unauthorized identity). Open each §15 queue alert definition → its view link resolves. Fire a seeded tie-conflict → inspect that the record carries the IDENTIFIERS (business_id, tied ordering, XML storage id, masked diff — §6.7 REVISED) and that the XML re-fetch by id works. | T-33 run ID; endpoint authz config; seeded tie record capture | OPS + TL |

## V.2 The evidence pack

Assemble ONE folder (local, like everything naming local systems):

```text
/golive-evidence/
  manifest.yaml         — the binding identities (round 16): RC/app
                          build SHA + dependency/SBOM digest;
                          migration set + checksums + resulting
                          schema version; runtime config version;
                          Oracle edition/patch; provider sandbox
                          environment + engine/SDK versions;
                          test-data set/version; target environment
  SHA256SUMS            — checksum of EVERY retained artifact
  invalidation-map.md   — input change → Q items whose evidence is
                          STALE (app SHA → code/integration tests;
                          migration checksum → Q5a + captured plans;
                          provider/SDK/engine version → CT
                          evidence; retry/trust-age config →
                          resolver tests; authz/deployment →
                          Q4/Q29; alert definitions/routing →
                          Q18–Q21; environment/scope-predicate
                          change, restore/seed/data migration,
                          old-writer activity, rollback, or query
                          change → the Q5b CUTOVER_POPULATION_
                          GREENFIELD proof, round 18; round 19:
                          a post-activation re-enable uses the
                          ADMISSION-COVERAGE form, file 26 T.1 /
                          runbook RB-F0 — zero-count applies to
                          INITIAL activation only; old-writer
                          activity after RUN 2 = STOP + incident
                          review, not a rerun)
  test-runs.md          — matrix test ID → run ID/link → date →
                          exact build/env/version
  signoffs.md           — Q → role → name → date (the roles above)
  open-risks.md         — WAIVABLE items only: owner, plan, date
  non-waivable-gates.md — §18 + MONEY_SAFETY_BLOCKING rows: every
                          one PASS, each with its evidence link
  Q01-..                — one subfolder per Q (V.1 Evidence column)
```

A Q without its subfolder is not PASS, whatever the scorecard says.
Evidence is IMMUTABLE once captured (round 16): a change to any
manifest input reverts the mapped Q items to MISSING — folder
presence is NOT freshness. Time-sensitive provider/config/security
evidence carries an explicit expiry; between final capture and
deployment either a change freeze holds or the invalidation map
triggers automatic re-runs.

TWO evidence snapshots, both immutable (review 5156f1f M2 — this is
how Q5b's PENDING-CUTOVER coexists with immutability):

```text
1. GO-04 PRE-CUTOVER pack: every non-waivable row PASS EXCEPT Q5b,
   which is the sole PENDING-CUTOVER row (legal only with Q5a PASS —
   round 20). Q5b's subfolder EXISTS at GO-04 and contains the
   reviewed RUN-2 query pack + checksum (from Q5a) — present, not
   empty, deliberately incomplete.
2. GO-03 CLOSURE pack: a NEW manifest VERSION that APPENDS the RUN-2
   result, DBA + TL signatures, and the Q5b PASS row. The
   pre-cutover manifest, SHA256SUMS, and signoffs are PRESERVED
   UNCHANGED as version 1 — closure never overwrites or rewrites
   captured evidence; it adds a second, final version.
```

## V.3 GO-04 go/no-go script (60–90 min meeting)

```text
1. Walk Q1–Q4 + Q28 first (§18 class). Any gap → NO-GO, stop here.
2. Walk the MONEY_SAFETY_BLOCKING class next (round 16): Q5a, Q5b, Q8, Q9, Q11, Q12, Q14, Q16, Q17, Q27, and Q29's minimal exit set. Any FAIL or missing
   evidence → NO-GO, stop here — this class has NO owner+plan
   waiver; reclassification needs a safety review, not this meeting.
   ONE defined exception (round 20): Q5b ALONE may stand as
   PENDING-CUTOVER when Q5a is PASS — the meeting then issues the
   CONDITIONAL GO (step 6) and GO-03 converts Q5b to PASS inside
   the F0 window; any OTHER missing/non-PASS non-waivable item is
   still NO-GO.
3. Walk the remaining Qs in order; for each FAIL read the owner+plan
   aloud and record explicit PO acceptance (or NO-GO).
4. Evidence integrity (round 16): validate manifest.yaml against the
   EXACT release-candidate build and target environment; verify
   SHA256SUMS over every retained artifact; walk the invalidation
   map — any input changed since capture (app SHA, migration
   checksum, config version, provider/SDK/engine version, authz or
   alert-routing change) reverts its mapped Q items to MISSING
   (re-run before GO). Then open 2 randomly chosen Q subfolders
   substantively and re-run 1 randomly chosen matrix test live
   against the RC build.
5. Confirm day-1 operational readiness: on-call rota loaded, alert
   channels tested (Q18/Q19 receipts), freeze-toggle access list
   verified, rollback runbook (Q23) at hand.
6. Record the decision + conditions in signoffs.md. GO requires PO,
   TL, and OPS signatures on the same dated entry. Round 19: this
   meeting is PRE-CUTOVER and the GO is CONDITIONAL — it authorizes
   GO-03's F0 change window contingent only on RUN 2 = zero + a
   clean activation sequence; GO-03's post-enable verification
   entry closes the record afterwards.
```
