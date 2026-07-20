# History-Extraction Manifest — APPLIED (descoped, deletion-only)

> **OUTCOME 2026-07-19 (PO decision — descoped to zero-risk pure deletions):**
> 411 line entries APPLIED (pure-provenance A/B deletions only; strict purity
> re-check demoted 130 borderline entries; 3 further lines REVERTED because the
> lint proved their annotation load-bearing — two cutoff-rule allow tokens, one
> rule-6l slice anchor). 381 entries KEPT byte-identical (all REWRITE/DECIDE/KEEP
> buckets + demotions — class-C rewrites, ADR register, and annotation-ban rule
> were dropped from scope). Per-entry approvals live in the .json. Verified:
> tools/history-verify.py --strict PROOF HOLDS (byte-exact vs the frozen 5597dcd
> blobs + this manifest); DOC-LINT clean; git diff --check clean.

Source commit: `5597dcd75163110122b808597a3e0572b1370597` (all old_line values are against these blobs)
Line entries: **792** across 56 files

| Review bucket | Meaning | Count |
|---|---|---:|
| AUTO_OK | pure A/B in a normal file — approve by skim | 281 |
| EXPLICIT | A/B in a HIGH-RISK file — read old/new pair | 263 |
| REWRITE | class C — you approve an exact new_line | 35 |
| DECIDE | risk-promoted or unsure — keep or specify | 165 |
| KEEP | class D decision record — byte-identical keep-span | 48 |


## requirment-v4.md

**:4** `R` **DECIDE**
- OLD: `**Date:** 2026-07-05`
- NEW: (to be decided — matched: 2026-07-05)

**:81** `R` **DECIDE**
- OLD: `- Role derivation (PO fact, 2026-07-12): the snapshot carries the`
- NEW: (to be decided — matched: 2026-07-12)

**:96** `R` **DECIDE**
- OLD: `semantics (PO-9, 2026-07-11 — absence = amendment to zero, BA-2`
- NEW: (to be decided — matched: 2026-07-11)

**:98** `B` **EXPLICIT**
- OLD: `round 5 — trade-level admission §6.1/§2.4).`
- NEW: `— trade-level admission §6.1/§2.4).`

**:135** `R` **DECIDE**
- OLD: `BA-2 (AMENDED 2026-07-11 by the PO's PO-9 answer) Upstream's ONE`
- NEW: (to be decided — matched: 2026-07-11)

**:151** `A` **EXPLICIT**
- OLD: `TERMINAL: the §4.1 CANCELLED branch (round 11), displayed`
- NEW: `TERMINAL: the §4.1 CANCELLED branch, displayed`

**:171** `B/R` **DECIDE**
- OLD: `row — added 2026-07-11 round 5, the one correctness-driven schema`
- NEW: (to be decided — matched: round 5, 2026-07-11)

**:212** `A` **EXPLICIT**
- OLD: `INITIALIZATION (review 4dbdf2b M1): every`
- NEW: `INITIALIZATION: every`

**:295** `B` **EXPLICIT**
- OLD: `round 11; the Java enum, the DB`
- NEW: `; the Java enum, the DB`

**:299** `B` **EXPLICIT**
- OLD: `is S-05's — round 13), rollout-safe per`
- NEW: `is S-05's), rollout-safe per`

**:362** `B/R` **DECIDE**
- OLD: `counter (declared 2026-07-19, review 1d8a650`
- NEW: (to be decided — matched: review 1d8a650, 2026-07-19)

**:381** `B` **EXPLICIT**
- OLD: `index). WRITE-ONCE CONTROL (4dbdf2b M1 —`
- NEW: `index). WRITE-ONCE CONTROL (`

**:397** `B` **EXPLICIT**
- OLD: `CUTOFF_EXPIRED (RESERVED round 10 — never`
- NEW: `CUTOFF_EXPIRED (RESERVED — never`

**:414** `R` **DECIDE**
- OLD: `2026-07-16, §14.1): +1 in EVERY posting-claim`
- NEW: (to be decided — matched: 2026-07-16)

**:423** `D` **KEEP**
- OLD: `unused — the 2026-07-11 bounds decision, §7.4:`
- KEEP unchanged

**:446** `R` **DECIDE**
- OLD: `(added 2026-07-19 — UI amount-series`
- NEW: (to be decided — matched: 2026-07-19)

**:447** `B` **EXPLICIT**
- OLD: `requirement; GRANULARITY, review 0e09f09 M2:`
- NEW: `requirement; GRANULARITY,:`

**:482** `B` **EXPLICIT**
- OLD: `accepted. CAPTURE BOUNDARY (review 0e09f09`
- NEW: `accepted. CAPTURE BOUNDARY (`

**:498** `B` **EXPLICIT**
- OLD: `column, 289ef66 L1: a later good row never`
- NEW: `column,: a later good row never`

**:501** `B` **EXPLICIT**
- OLD: `open at GO-03 closure — 7cc9f49 L2,`
- NEW: `open at GO-03 closure —,`

**:510** `B` **EXPLICIT**
- OLD: `manifest — aa4399c L1).`
- NEW: `manifest).`

**:533** `R` **DECIDE**
- OLD: `2026-07-16, PO) NOT VISIBLE to this team —`
- NEW: (to be decided — matched: 2026-07-16)

**:549** `R` **DECIDE**
- OLD: `for the 2026-07-16 content-visibility driver,`
- NEW: (to be decided — matched: 2026-07-16)

**:609** `B` **EXPLICIT**
- OLD: `Constraints (scoped per review 1d8a650 L2: the DB backstops every`
- NEW: `Constraints (scoped per: the DB backstops every`

**:626** `B` **EXPLICIT**
- OLD: `legacy/pre-F0 rows carry NULL — 1d8a650 M1)`
- NEW: `legacy/pre-F0 rows carry NULL)`

**:655** `A` **EXPLICIT**
- OLD: `### 2.4 trade_snapshot_state (snapshot admission — added 2026-07-11 round 5)`
- NEW: `### 2.4 trade_snapshot_state`

**:678** `A` **EXPLICIT**
- OLD: `EVALUABLE against applied state (round 5)`
- NEW: `EVALUABLE against applied state`

**:682** `A` **DECIDE** — normative token(s) in deleted text: H-1
- OLD: `Why it exists (money safety, not audit — round-5 review H-1): a`
- NEW: (to be decided — matched: round-5)

**:698** `B/R` **DECIDE**
- OLD: `GREENFIELD FACT (PO, 2026-07-11 — supersedes the round-6..9`
- NEW: (to be decided — matched: round-6, 2026-07-11)

**:709** `B` **EXPLICIT**
- OLD: `fact; retained in git history at commit 9a53c75 — restore`
- NEW: `fact; retained in git history at commit — restore`

**:721** `A` **EXPLICIT**
- OLD: `- §5.2 restore note (HONEST bounds, round 11 — post-MVP scope):`
- NEW: `- §5.2 restore note:`

**:844** `B` **EXPLICIT**
- OLD: `step cancelled ⇔                      (round 11 — the zero-required`
- NEW: `step cancelled ⇔ (the zero-required`

**:863** `A` **EXPLICIT**
- OLD: `never CANCELLED. NO request-state mutation is implied (round 12):`
- NEW: `never CANCELLED. NO request-state mutation is implied:`

**:876** `A` **EXPLICIT**
- OLD: `- `required_amount = 0` vs `NULL` is load-bearing (round 11): inbound`
- NEW: `- `required_amount = 0` vs `NULL` is load-bearing: inbound`

**:884** `A` **EXPLICIT**
- OLD: `(round 12): removal does not launder reject history, and reject`
- NEW: `: removal does not launder reject history, and reject`

**:890** `B` **EXPLICIT**
- OLD: `ONLY the historical provider-reject exception (round 13 — markers`
- NEW: `ONLY the historical provider-reject exception (markers`

**:922** `B` **EXPLICIT**
- OLD: `Zero-required suppression (round 12; NARROWED round 13 — the`
- NEW: `Zero-required suppression (NARROWED — the`

**:1100** `B` **EXPLICIT**
- OLD: `1d8a650 M1)`
- NEW: `)`

**:1188** `B` **EXPLICIT**
- OLD: `(corrected review 0e09f09 H1; the earlier "PROVEN upper`
- NEW: `(the earlier "PROVEN upper`

**:1189** `B` **EXPLICIT**
- OLD: `bound" and the 4098532 "+K absorbs it" claims were both`
- NEW: `bound" and the "+K absorbs it" claims were both`

**:1240** `B` **EXPLICIT**
- OLD: `limit alone NEVER authorizes this step (aa4399c M2 — the`
- NEW: `limit alone NEVER authorizes this step (the`

**:1290** `R` **DECIDE**
- OLD: `Derived-set fact (PO, 2026-07-12 — §1 role derivation): the raw`
- NEW: (to be decided — matched: 2026-07-12)

**:1300** `B` **EXPLICIT**
- OLD: `round-5 hole). Display: a trade with only zeroed/no obligations`
- NEW: `hole). Display: a trade with only zeroed/no obligations`

**:1326** `R` **DECIDE**
- OLD: `Transport (contract fact, recorded 2026-07-11 from the PO/team):`
- NEW: (to be decided — matched: 2026-07-11)

**:1353** `B/D` **EXPLICIT**
- OLD: `ADMISSION (trade-level, normative — added 2026-07-11 round 5; runs`
- NEW: `ADMISSION (trade-level, normative — added 2026-07-11; runs`

**:1376** `B` **EXPLICIT**
- OLD: `no block applied, and — the round-5 rule — NO NEW SCOPE IS`
- NEW: `no block applied, and — the rule — NO NEW SCOPE IS`

**:1379** `A` **EXPLICIT**
- OLD: `and admission is a POINT-IN-TIME fact (round 6), so every`
- NEW: `and admission is a POINT-IN-TIME fact, so every`

**:1381** `B` **EXPLICIT**
- OLD: `round 7 — never "currency check": currency is a scope-key`
- NEW: `— never "currency check": currency is a scope-key`

**:1390** `B` **EXPLICIT**
- OLD: `round 7). Consequences: block transactions for one trade`
- NEW: `). Consequences: block transactions for one trade`

**:1406** `B` **EXPLICIT**
- OLD: `(round 7, replacing an INCORRECT round-6 sequential-`
- NEW: `(replacing an INCORRECT sequential-`

**:1407** `R` **DECIDE**
- OLD: `equivalence claim; ratified by the design owner 2026-07-11;`
- NEW: (to be decided — matched: 2026-07-11)

**:1422** `B` **EXPLICIT**
- OLD: `(§9.3 display). Considered and REJECTED (round 6, upheld`
- NEW: `(§9.3 display). Considered and REJECTED (upheld`

**:1423** `B` **EXPLICIT**
- OLD: `round 7): (a) an APPLYING → COMPLETE application state`
- NEW: `): (a) an APPLYING → COMPLETE application state`

**:1455** `B` **EXPLICIT**
- OLD: `passed admission: a stale document never reaches this path (round-5`
- NEW: `passed admission: a stale document never reaches this path (`

**:1461** `D` **KEEP**
- OLD: `RESOLVED — absence semantics (PO-9, ANSWERED by the PO 2026-07-11;`
- KEEP unchanged

**:1462** `B` **EXPLICIT**
- OLD: `BA-2 amended accordingly, §1.1; lifecycle completed round 11): a`
- NEW: `BA-2 amended accordingly, §1.1; lifecycle completed): a`

**:1471** `B` **EXPLICIT**
- OLD: `so it advances the watermark like any applied block (round 11: this`
- NEW: `so it advances the watermark like any applied block (this`

**:1472** `B` **EXPLICIT**
- OLD: `supersedes the older round-5 "no write for absent obligations"`
- NEW: `supersedes the older "no write for absent obligations"`

**:1481** `A` **EXPLICIT**
- OLD: `Reappearance (round 11): removal is not a tombstone forever — a`
- NEW: `Reappearance: removal is not a tombstone forever — a`

**:1484** `B` **EXPLICIT**
- OLD: `inventory fires) and the step returns to IN_PROGRESS. Round 12:`
- NEW: `inventory fires) and the step returns to IN_PROGRESS.:`

**:1493** `C` **REWRITE**
- OLD: `Anchor retirement (round 11 — replaces the round-10 blanket`
- NEW: (to be decided — matched: round 11, round-10)

**:1501** `B` **EXPLICIT**
- OLD: `anchor (the round-10 protection this rule keeps: a failed snapshot`
- NEW: `anchor (the protection this rule keeps: a failed snapshot`

**:1506** `A` **EXPLICIT**
- OLD: `Zero-payment documents (round 11, PO role-derivation fact §1/§6.0):`
- NEW: `Zero-payment documents:`

**:1515** `C/R` **REWRITE**
- OLD: `2026-07-11 round 5; superseded detail round 11): the trade-level`
- NEW: (to be decided — matched: round 5, round 11, 2026-07-11)

**:1516** `B` **EXPLICIT**
- OLD: `ADMISSION gate above is the answer. Round-11 correction: the`
- NEW: `ADMISSION gate above is the answer. correction: the`

**:1517** `B` **EXPLICIT**
- OLD: `round-5 clause "per-obligation watermarks are not advanced for`
- NEW: `clause "per-obligation watermarks are not advanced for`

**:1525** `B` **EXPLICIT**
- OLD: `obligation NOR create a never-seen scope (the sharper round-5`
- NEW: `obligation NOR create a never-seen scope (the sharper`

**:1548** `A` **EXPLICIT**
- OLD: `(round 12); never if overpay-latched — §6.5 latch guard.)`
- NEW: `; never if overpay-latched — §6.5 latch guard.)`

**:1634** `B` **EXPLICIT**
- OLD: `reached `CANCELLED` (round 12: a reappeared removed payment reopens`
- NEW: `reached `CANCELLED` (a reappeared removed payment reopens`

**:1640** `B` **EXPLICIT**
- OLD: `gate applies, incl. live markers (round 12: a reappeared`
- NEW: `gate applies, incl. live markers (a reappeared`

**:1698** `R` **DECIDE**
- OLD: `Consistency semantics (clarified 2026-07-11 after external review):`
- NEW: (to be decided — matched: 2026-07-11)

**:1708** `B/R` **DECIDE**
- OLD: `two further windows (made explicit 2026-07-17, review 4d5cb83 H1 —`
- NEW: (to be decided — matched: review 4d5cb83 H1, 2026-07-17)

**:1721** `B/R` **DECIDE**
- OLD: `2026-07-17, review 928341a H1: the earlier "converges`
- NEW: (to be decided — matched: review 928341a H1, 2026-07-17)

**:1738** `B/R` **DECIDE**
- OLD: `Observability (revised 2026-07-17, review 2b697fb M1; scoped`
- NEW: (to be decided — matched: review 2b697fb M1, 2026-07-17)

**:1739** `B/R` **DECIDE**
- OLD: `2026-07-17, review b1d91dc M1 — an OPTIONAL ON-DEMAND`
- NEW: (to be decided — matched: review b1d91dc M1, 2026-07-17)

**:1750** `A` **EXPLICIT**
- OLD: `(the name encodes uncertainty BY DESIGN, review b1d91dc L1).`
- NEW: `.`

**:1761** `B` **EXPLICIT**
- OLD: `Delivery semantics (review b760786 M1; corrected review`
- NEW: `Delivery semantics (corrected review`

**:1762** `B` **EXPLICIT**
- OLD: `4098532 M1 — the earlier "blocks OB-01 completion" made`
- NEW: `— the earlier "blocks OB-01 completion" made`

**:1789** `R` **DECIDE**
- OLD: `2026-07-17): the atomicity it would buy protects nothing`
- NEW: (to be decided — matched: 2026-07-17)

**:1853** `B` **EXPLICIT**
- OLD: `round 5). The §6.1 admission gate consults the trade watermark`
- NEW: `). The §6.1 admission gate consults the trade watermark`

**:1873** `B` **EXPLICIT**
- OLD: `— round 5: this is what makes equality INCLUDING the trade`
- NEW: `—: this is what makes equality INCLUDING the trade`

**:1878** `B/R` **DECIDE**
- OLD: `Executability requirement (REVISED again 2026-07-11 round 3 —`
- NEW: (to be decided — matched: round 3, 2026-07-11)

**:1890** `A` **EXPLICIT**
- OLD: `time — at the §6.1 ADMISSION gate (round 5): the ≥ relaxation`
- NEW: `time — at the §6.1 ADMISSION gate: the ≥ relaxation`

**:1928** `A` **EXPLICIT**
- OLD: `the row's request_seq (§2.2, write-once — 1d8a650 M1),`
- NEW: `the row's request_seq,`

**:2053** `B/D` **EXPLICIT**
- OLD: `2026-07-11 round 5, PO-confirmed): every attempt re-reads the`
- NEW: `2026-07-11, PO-confirmed): every attempt re-reads the`

**:2059** `A` **EXPLICIT**
- OLD: `GREENFIELD (round 10 — PO fact, §2.4): this flow starts with no`
- NEW: `GREENFIELD: this flow starts with no`

**:2063** `A` **EXPLICIT**
- OLD: `ladder is REMOVED (git history, 9a53c75).`
- NEW: `ladder is REMOVED.`

**:2103** `R` **DECIDE**
- OLD: `(The former cutoff term was RETIRED 2026-07-11 — PO calendar`
- NEW: (to be decided — matched: 2026-07-11)

**:2250** `D` **KEEP**
- OLD: `Retry exhaustion (bounds decided 2026-07-11; REVISED same day by`
- KEEP unchanged

**:2272** `R` **DECIDE**
- OLD: `unused column — kept to avoid schema churn after the 2026-07-11`
- NEW: (to be decided — matched: 2026-07-11)

**:2284** `B` **EXPLICIT**
- OLD: `§16.6) — MAX ATTEMPTS is the ONLY bound (round 10: no cutoff`
- NEW: `§16.6) — MAX ATTEMPTS is the ONLY bound (no cutoff`

**:2343** `D` **KEEP**
- OLD: `REJECT has no amount guard at all. Index decision (2026-07-11, PO`
- KEEP unchanged

**:2482** `C` **REWRITE**
- OLD: `- Lookback aging (REVISED round 10 — the local cutoff was retired`
- NEW: (to be decided — matched: round 10)

**:2579** `D` **KEEP**
- OLD: `orchestrator (execution boundary decided 2026-07-11: Java`
- KEEP unchanged

**:2588** `B/D` **EXPLICIT**
- OLD: `(decided 2026-07-11 round 3; CANONICALIZED round 4 — an`
- NEW: `(decided 2026-07-11; CANONICALIZED — an`

**:2599** `B` **EXPLICIT**
- OLD: `APPROVAL TIME — round 4: approval authorizes CONTENT, not an`
- NEW: `APPROVAL TIME —: approval authorizes CONTENT, not an`

**:2602** `A` **EXPLICIT**
- OLD: `masked diff, and (reprocess-snapshot, round 7) the notice that a`
- NEW: `masked diff, and the notice that a`

**:2611** `B` **EXPLICIT**
- OLD: `version column. ATOMICITY (round 4; SCOPED round 5 — the round-4`
- NEW: `version column. ATOMICITY (SCOPED — the`

**:2627** `B` **EXPLICIT**
- OLD: `round 5): consumption precedes any money movement, so NO replay`
- NEW: `): consumption precedes any money movement, so NO replay`

**:2640** `A` **EXPLICIT**
- OLD: `(round 6 — consume-at-start must never fail SILENTLY): the`
- NEW: `: the`

**:2691** `R` **DECIDE**
- OLD: `(clarified 2026-07-11): after a verified REJECTED the`
- NEW: (to be decided — matched: 2026-07-11)

**:2752** `B` **EXPLICIT**
- OLD: `prioritized batches — oldest first (round 10: no local cutoff`
- NEW: `prioritized batches — oldest first (no local cutoff`

**:2961** `B` **EXPLICIT**
- OLD: `2a19c20 M1, where the earlier`
- NEW: `, where the earlier`

**:3094** `A` **DECIDE** — normative token(s) in deleted text: CUTOFF_EXPIRED
- OLD: `| Retry exhaustion | POST·RETRY_WAIT → POST·BLOCKED(RETRY_EXHAUSTED) (CUTOFF_EXPIRED: RESERVED, round 10) | — |`
- NEW: (to be decided — matched: round 10)

**:3166** `D` **KEEP**
- OLD: `- Scanner claim protocol (NORMATIVE — decided 2026-07-11; replaces`
- KEEP unchanged

**:3179** `D` **KEEP**
- OLD: `- Claim-transition classification (decided 2026-07-11): READY →`
- KEEP unchanged

**:3226** `B` **EXPLICIT**
- OLD: `round 11: the payment was removed by newer`
- NEW: `: the payment was removed by newer`

**:3246** `R` **DECIDE**
- OLD: `ALL-PAYMENTS TABLE projection (added 2026-07-17 — the second`
- NEW: (to be decided — matched: 2026-07-17)

**:3247** `B` **EXPLICIT**
- OLD: `defined read surface; review 7ab31e5 M4 closed the granularity`
- NEW: `defined read surface; closed the granularity`

**:3253** `R` **DECIDE**
- OLD: `schema addition this feature made, 2026-07-19; wording corrected`
- NEW: (to be decided — matched: 2026-07-19)

**:3254** `B` **EXPLICIT**
- OLD: `review 0e09f09 M2 — the earlier "no schema change, no new state"`
- NEW: `— the earlier "no schema change, no new state"`

**:3281** `R` **DECIDE**
- OLD: `the UI amount series (2026-07-19; terminology per review`
- NEW: (to be decided — matched: 2026-07-19)

**:3282** `B` **EXPLICIT**
- OLD: `0e09f09 M2: one stamp per payment_request row, NOT per provider`
- NEW: `: one stamp per payment_request row, NOT per provider`

**:3295** `A` **EXPLICIT**
- OLD: `request created", and a NULLABLE reason (review d00ef6a M2):`
- NEW: `request created", and a NULLABLE reason:`

**:3307** `D` **KEEP**
- OLD: `Decided display defaults (PO 2026-07-17): terminal/historical`
- KEEP unchanged

**:3320** `B/R` **DECIDE**
- OLD: `API/read contract (2026-07-17, review d00ef6a M2 — the projection`
- NEW: (to be decided — matched: review d00ef6a M2, 2026-07-17)

**:3333** `B` **EXPLICIT**
- OLD: `review 4dbdf2b M2; repeated BYTE-FOR-BYTE in CA-4/ST-04/T-31,`
- NEW: `; repeated BYTE-FOR-BYTE in CA-4/ST-04/T-31,`

**:3346** `D` **KEEP**
- OLD: `- PAGINATION SEMANTICS = LIVE BROWSE (decided 2026-07-17, review`
- KEEP unchanged

**:3347** `B` **EXPLICIT**
- OLD: `c8a92f1 M2): each page is truthful at its read instant (the`
- NEW: `): each page is truthful at its read instant (the`

**:3359** `B` **EXPLICIT**
- OLD: `- ESTATE QUERY CONTRACT (review c8a92f1 M2; scoped per review`
- NEW: `- ESTATE QUERY CONTRACT (scoped per review`

**:3360** `B` **EXPLICIT**
- OLD: `4d5cb83 M4): the CONCEPT is authorization scope first, then`
- NEW: `): the CONCEPT is authorization scope first, then`

**:3363** `B` **EXPLICIT**
- OLD: `FIRST, created_at NULLS FIRST, source_id), 4dbdf2b M2, one`
- NEW: `FIRST, created_at NULLS FIRST, source_id), one`

**:3377** `A` **EXPLICIT**
- OLD: `answer must cover the FULL state algebra (round 12), not the old`
- NEW: `answer must cover the FULL state algebra, not the old`

**:3432** `B` **EXPLICIT**
- OLD: `CUTOFF_EXPIRED (RESERVED round 10 —`
- NEW: `CUTOFF_EXPIRED (RESERVED —`

**:3489** `A` **EXPLICIT**
- OLD: `(review 4098532 H1). (The`
- NEW: `. (The`

**:3490** `R` **DECIDE**
- OLD: `§14.1 attempt journal, added 2026-07-16, is a CONTENT record, not`
- NEW: (to be decided — matched: 2026-07-16)

**:3498** `R` **DECIDE**
- OLD: `2026-07-16: the request actually sent to the engine is not`
- NEW: (to be decided — matched: 2026-07-16)

**:3506** `B/R` **DECIDE**
- OLD: `DELIVERY CONTRACT (defined 2026-07-17, review 4098532 H1 — ONE`
- NEW: (to be decided — matched: review 4098532 H1, 2026-07-17)

**:3538** `R` **DECIDE**
- OLD: ``post_attempt_seq` and `attempt_event_type` (2026-07-17 —`
- NEW: (to be decided — matched: 2026-07-17)

**:3539** `B` **EXPLICIT**
- OLD: `review 7ab31e5 M5; field name + token vocabulary FROZEN per`
- NEW: `; field name + token vocabulary FROZEN per`

**:3540** `B` **EXPLICIT**
- OLD: `review b760786 M2): `attempt_event_type` is the EXACT`
- NEW: `): `attempt_event_type` is the EXACT`

**:3562** `R` **DECIDE**
- OLD: `### 14.1 Local attempt journal (team-internal audit — added 2026-07-16, simplified 2026-07-17)`
- NEW: (to be decided — matched: 2026-07-16, 2026-07-17)

**:3564** `R` **DECIDE**
- OLD: `Driver (PO-recorded 2026-07-16): the request actually sent to the`
- NEW: (to be decided — matched: 2026-07-16)

**:3570** `B/R` **DECIDE**
- OLD: `GOVERNING STANCE (PO 2026-07-17, review 7ab31e5; wording unified`
- NEW: (to be decided — matched: review 7ab31e5, 2026-07-17)

**:3571** `B` **EXPLICIT**
- OLD: `per review c8a92f1 H1 — ONE formulation, used verbatim everywhere):`
- NEW: `per — ONE formulation, used verbatim everywhere):`

**:3585** `A` **EXPLICIT**
- OLD: `(review d00ef6a M1; see the wire-capture ask below). Until that is`
- NEW: `. Until that is`

**:3594** `A` **EXPLICIT**
- OLD: `event_type token (§14 / review b760786 M2). It`
- NEW: `event_type token. It`

**:3634** `R` **DECIDE**
- OLD: `SIMPLICITY RULE (2026-07-17 — the dedup-by-hash design was REJECTED`
- NEW: (to be decided — matched: 2026-07-17)

**:3635** `B` **EXPLICIT**
- OLD: `as unimplementable under the no-read invariant, review 7ab31e5 H1;`
- NEW: `as unimplementable under the no-read invariant,`

**:3661** `B/R` **DECIDE**
- OLD: `(2026-07-17, revised per review d00ef6a H3 — honest about the`
- NEW: (to be decided — matched: review d00ef6a H3, 2026-07-17)

**:3673** `R` **DECIDE**
- OLD: `AMBIGUOUS TRANSLATION ARE FATAL BY DEFAULT (2026-07-17, review`
- NEW: (to be decided — matched: 2026-07-17)

**:3674** `B` **EXPLICIT**
- OLD: `928341a H2 — a timeout does not prove the session is usable).`
- NEW: `— a timeout does not prove the session is usable).`

**:3699** `R` **DECIDE**
- OLD: `ENABLEMENT GATE (2026-07-17): journal writes sit behind a plain`
- NEW: (to be decided — matched: 2026-07-17)

**:3705** `R` **DECIDE**
- OLD: `pre-2026-07-16 designed state (log-only forensics).`
- NEW: (to be decided — matched: 2026-07-16)

**:3706** `B` **EXPLICIT**
- OLD: `SWITCH-TRANSITION RULE (review d00ef6a M3; drain defined per`
- NEW: `SWITCH-TRANSITION RULE (drain defined per`

**:3707** `B` **EXPLICIT**
- OLD: `review c8a92f1 M1): the switch may change state ONLY under posting`
- NEW: `): the switch may change state ONLY under posting`

**:3730** `A` **EXPLICIT**
- OLD: `Honesty note — what this journal proves (review 7ab31e5 M2): the`
- NEW: `Honesty note — what this journal proves: the`

**:3734** `A` **EXPLICIT**
- OLD: `governs that gap. Restore posture (review 7ab31e5 M1): the journal`
- NEW: `governs that gap. Restore posture: the journal`

**:3751** `R` **DECIDE**
- OLD: `OPEN (recorded 2026-07-16; blocking status revised 2026-07-17):`
- NEW: (to be decided — matched: 2026-07-16, 2026-07-17)

**:3808** `A` **EXPLICIT**
- OLD: `evidence past SLA (§9.3 round 6)             → alert (crash or`
- NEW: `evidence past SLA → alert (crash or`

**:3814** `B` **EXPLICIT**
- OLD: `- Payment DISAPPEARANCE (round 11 — the §6.1`
- NEW: `- Payment DISAPPEARANCE (the §6.1`

**:3829** `B` **EXPLICIT**
- OLD: `(round 13: on required_amount = 0 scopes, ONLY the historical`
- NEW: `(on required_amount = 0 scopes, ONLY the historical`

**:3834** `A` **EXPLICIT**
- OLD: `age into this alert (§4.2 round-13 narrowing).`
- NEW: `age into this alert.`

**:3856** `B` **EXPLICIT**
- OLD: `4dbdf2b M1): payment_request with created_at`
- NEW: `): payment_request with created_at`

**:3871** `B` **EXPLICIT**
- OLD: `quality, aa4399c L1): payment_request with`
- NEW: `quality): payment_request with`

**:3993** `R` **DECIDE**
- OLD: `- Freeze/outage clock semantics (simplified by the 2026-07-11 retry`
- NEW: (to be decided — matched: 2026-07-11)

**:3995** `B` **EXPLICIT**
- OLD: `attempt budget (round 10: max attempts only — the engine`
- NEW: `attempt budget (max attempts only — the engine`

**:4002** `B/R` **DECIDE**
- OLD: `LINEARIZATION of the freeze check (2026-07-17, review 4d5cb83`
- NEW: (to be decided — matched: review 4d5cb83, 2026-07-17)

**:4061** `R` **DECIDE**
- OLD: `attempt journal (2026-07-16 driver: the sent request is not`
- NEW: (to be decided — matched: 2026-07-16)

**:4090** `C` **REWRITE**
- OLD: `RETIRED (round 10), and its timezone-aware representation rules`
- NEW: (to be decided — matched: round 10)

**:4091** `C` **REWRITE**
- OLD: `are retired with it (2a19c20 L3 — the stale fragment describing`
- NEW: (to be decided — matched: 2a19c20 L3)

**:4152** `B` **EXPLICIT**
- OLD: `attempts only, round 10)`
- NEW: `attempts only)`

**:4179** `B` **EXPLICIT**
- OLD: `0e09f09 H1): (a) the ALWAYS-`
- NEW: `): (a) the ALWAYS-`

**:4205** `C` **REWRITE**
- OLD: `(the former cutoff-margin ceiling is RETIRED — round 10). Nothing`
- NEW: (to be decided — matched: round 10)

**:4246** `R` **DECIDE**
- OLD: `(2026-07-11 external-review fold): (a) crash-point tests at`
- NEW: (to be decided — matched: 2026-07-11)

**:4256** `A` **EXPLICIT**
- OLD: `adversarial set (§20-10 rounds 3–4): non-tying document → no`
- NEW: `adversarial set: non-tying document → no`

**:4262** `A` **EXPLICIT**
- OLD: `lock (round 4 — never merely "inside the ordering guard");`
- NEW: `lock;`

**:4265** `B` **EXPLICIT**
- OLD: `already-newer obligation + one absent obligation (round 10:`
- NEW: `already-newer obligation + one absent obligation (`

**:4268** `B` **EXPLICIT**
- OLD: `trade-reference-only difference (round 5: converges via the`
- NEW: `trade-reference-only difference (converges via the`

**:4270** `B` **EXPLICIT**
- OLD: `crash-mid-reprocess re-run under a NEW approval (round 5:`
- NEW: `crash-mid-reprocess re-run under a NEW approval (`

**:4280** `A` **EXPLICIT**
- OLD: `(f) admission-gate set (§6.1/§2.4, rounds 5–6): the`
- NEW: `(f) admission-gate set: the`

**:4286** `B` **EXPLICIT**
- OLD: `neither watermark; round-6 fence set: pause a worker AFTER`
- NEW: `neither watermark; fence set: pause a worker AFTER`

**:4290** `B` **EXPLICIT**
- OLD: `round 7); kill the paused worker — redelivery/alert`
- NEW: `); kill the paused worker — redelivery/alert`

**:4292** `C` **REWRITE**
- OLD: `document → all no-ops. (The former bootstrap/digest-NULL set was REMOVED round 10 —`
- NEW: (to be decided — matched: round 10)

**:4299** `R` **DECIDE**
- OLD: `2026-07-11: an authorized, enterprise-authenticated endpoint of`
- NEW: (to be decided — matched: 2026-07-11)

**:4303** `B` **EXPLICIT**
- OLD: `EXECUTION INPUT IS THE approval_id (round 4: identities are`
- NEW: `EXECUTION INPUT IS THE approval_id (identities are`

**:4309** `A` **EXPLICIT**
- OLD: `(round 5): single-transition → the CONSUMED CAS and the payment`
- NEW: `: single-transition → the CONSUMED CAS and the payment`

**:4403** `D` **KEEP**
- OLD: `c. CLOSED 2026-07-11: PO-9 ANSWERED (absence = amendment to`
- KEEP unchanged

**:4404** `B` **EXPLICIT**
- OLD: `zero; BA-2 amended §1.1) and TL-16 ANSWERED round 5 (§6.1`
- NEW: `zero; BA-2 amended §1.1) and TL-16 (§6.1`

**:4406** `B` **EXPLICIT**
- OLD: `d. Upstream ask 8 IN WRITING (added round 4 — elevated from`
- NEW: `d. Upstream ask 8 IN WRITING (elevated from`

**:4429** `A` **EXPLICIT**
- OLD: `DECISION HYGIENE (round 9): the TTL outcome is recorded as`
- NEW: `DECISION HYGIENE: the TTL outcome is recorded as`

**:4443** `B` **EXPLICIT**
- OLD: `matters. CONSEQUENCE CLOSURE (review 289ef66 M2; state model`
- NEW: `matters. CONSEQUENCE CLOSURE (state model`

**:4444** `B` **EXPLICIT**
- OLD: `completed follow-up M2 on 0bcb536): (c) and (d) are DECISIONS,`
- NEW: `completed): (c) and (d) are DECISIONS,`

**:4472** `D` **KEEP**
- OLD: `2. CLOSED 2026-07-11 (PO answer): the PAYMENT ENGINE owns its own`
- KEEP unchanged

**:4473** `A` **EXPLICIT**
- OLD: `cutoff calendar (engine-owned, round 10) — this system initiates at any time and carries`
- NEW: `cutoff calendar — this system initiates at any time and carries`

**:4483** `D` **KEEP**
- OLD: `application endpoint — execution boundary decided 2026-07-11;`
- KEEP unchanged

**:4502** `D` **KEEP**
- OLD: `4. CLOSED 2026-07-11 (calendar answer, §18-2): no local cutoff`
- KEEP unchanged

**:4543** `D` **KEEP**
- OLD: `ANSWERED BY THE PO 2026-07-11: absence = CANCELLED — an`
- KEEP unchanged

**:4547** `B` **EXPLICIT**
- OLD: `as overpay"). The round-7 RIDER is satisfied: the PO's answer`
- NEW: `as overpay"). The RIDER is satisfied: the PO's answer`

**:4549** `R` **DECIDE**
- OLD: `the PO-9 answer by the design owner, 2026-07-11).`
- NEW: (to be decided — matched: 2026-07-11)

**:4550** `B` **EXPLICIT**
- OLD: `Round 11: lifecycle COMPLETED — §4.1 CANCELLED terminal branch;`
- NEW: `: lifecycle COMPLETED — §4.1 CANCELLED terminal branch;`

**:4551** `R` **DECIDE**
- OLD: `0..N derived payment set (§1 role derivation, PO 2026-07-12);`
- NEW: (to be decided — matched: 2026-07-12)

**:4568** `B` **EXPLICIT**
- OLD: `must define — round 12: the answer must specify the FULL`
- NEW: `must define —: the answer must specify the FULL`

**:4651** `B` **EXPLICIT**
- OLD: `terminal-time convention is the enabler.) Round 6: the`
- NEW: `terminal-time convention is the enabler.): the`

**:4656** `A/D` **EXPLICIT**
- OLD: `16. ANSWERED 2026-07-11 (round 5, design owner): option (b),`
- NEW: `16. ANSWERED 2026-07-11: option (b),`

**:4658** `B` **EXPLICIT**
- OLD: `gate. The round-5 review exposed the sharper failure the`
- NEW: `gate. The review exposed the sharper failure the`

**:4668** `D` **KEEP**
- OLD: `original). (PO-9 was later ANSWERED 2026-07-11: absence =`
- KEEP unchanged

**:4669** `B` **EXPLICIT**
- OLD: `amendment to zero, §6.1; round 11 — the zeroing write ADVANCES`
- NEW: `amendment to zero, §6.1; — the zeroing write ADVANCES`

**:4670** `B` **EXPLICIT**
- OLD: `the per-obligation watermark, superseding the round-5 no-write`
- NEW: `the per-obligation watermark, superseding the no-write`

**:4689** `B` **EXPLICIT**
- OLD: `reject cycle, §2.1). HONESTY NOTE (round 11 — this ask is NOT`
- NEW: `reject cycle, §2.1). HONESTY NOTE (this ask is NOT`

**:4705** `D` **KEEP**
- OLD: `5. CONFIRMED verbally 2026-07-11 (design-owner relay) — the`
- KEEP unchanged

**:4713** `B` **EXPLICIT**
- OLD: `contract alone. ROUND-11 ADDITION (goes in the SAME written`
- NEW: `contract alone. ADDITION (goes in the SAME written`

**:4717** `B` **EXPLICIT**
- OLD: `cancels real payments, H-1 round 11).`
- NEW: `cancels real payments, H-1).`

**:4742** `D` **KEEP**
- OLD: `8. CONFIRMED verbally 2026-07-11 (design-owner relay) — the`
- KEEP unchanged

**:4745** `B/R` **DECIDE**
- OLD: `2026-07-11; IMMUTABILITY clause added round 3): confirm IN`
- NEW: (to be decided — matched: added round 3, 2026-07-11)

**:4757** `A` **EXPLICIT**
- OLD: `THIS ASK IS PART OF §18 BLOCKING ITEM 0(d) (round 4): the`
- NEW: `THIS ASK IS PART OF §18 BLOCKING ITEM 0(d): the`

**:4768** `R` **DECIDE**
- OLD: `9. WITHDRAWN 2026-07-11: greenfield (PO — this flow is a new`
- NEW: (to be decided — matched: 2026-07-11)

**:4880** `D` **KEEP**
- OLD: `(execution boundary decided 2026-07-11: enterprise-authenticated,`
- KEEP unchanged

**:4893** `B` **EXPLICIT**
- OLD: `below (round-4 normalization — the earlier "exactly one`
- NEW: `below (normalization — the earlier "exactly one`

**:4898** `B/R` **DECIDE**
- OLD: `NON-WAIVABLE MINIMAL EXIT SET (round-3 normalization, 2026-07-11 —`
- NEW: (to be decided — matched: round-3, 2026-07-11)

**:4923** `B/R` **DECIDE**
- OLD: `Exit honesty (wording fixed 2026-07-11; scoped round 3; NARROWED`
- NEW: (to be decided — matched: round 3, 2026-07-11)

**:4924** `B` **EXPLICIT**
- OLD: `round 4): the exit GUARANTEE covers exactly THREE dead-end`
- NEW: `): the exit GUARANTEE covers exactly THREE dead-end`

**:4934** `A` **EXPLICIT**
- OLD: `platform-side (§19.2). Considered and REJECTED (round 4): an`
- NEW: `platform-side (§19.2). Considered and REJECTED: an`

**:4973** `B` **EXPLICIT**
- OLD: `NOT restore-surviving; review b1d91dc M2), every manual action`
- NEW: `NOT restore-surviving), every manual action`

**:4980** `B/R` **DECIDE**
- OLD: `10. Tie resolution (§6.7, REVISED 2026-07-11 round 3 —`
- NEW: (to be decided — matched: round 3, 2026-07-11)

**:4981** `B` **EXPLICIT**
- OLD: `server-verified; round 4 — digest-bound approval + per-block`
- NEW: `server-verified; — digest-bound approval + per-block`

**:4991** `B` **EXPLICIT**
- OLD: `approval (CONSUME-AT-START — §9.3 round-5 scoping; a crash`
- NEW: `approval (CONSUME-AT-START — §9.3 scoping; a crash`

**:4994** `A` **EXPLICIT**
- OLD: `the normal §6.1 fan-out THROUGH THE ADMISSION GATE (round 5):`
- NEW: `the normal §6.1 fan-out THROUGH THE ADMISSION GATE:`

**:5000** `B` **EXPLICIT**
- OLD: `applied. Round 6: reprocess block transactions carry the SAME`
- NEW: `applied. reprocess block transactions carry the SAME`

**:5004** `B` **EXPLICIT**
- OLD: `are ABANDONED (§6.1 block-level supersession, round 7 — the`
- NEW: `are ABANDONED (§6.1 block-level supersession, — the`

**:5009** `B` **EXPLICIT**
- OLD: `PER-BLOCK ALGORITHM (normative, round 4 — the relaxation`
- NEW: `PER-BLOCK ALGORITHM (normative, — the relaxation`

**:5019** `B` **EXPLICIT**
- OLD: `reaches no rule below and creates nothing. Round 6: EACH block`
- NEW: `reaches no rule below and creates nothing. EACH block`

**:5029** `B` **EXPLICIT**
- OLD: `documents, round 5)`
- NEW: `documents)`

**:5039** `D` **KEEP**
- OLD: `ANSWERED 2026-07-11; lifecycle`
- KEEP unchanged

**:5040** `B` **EXPLICIT**
- OLD: `round 11): required := 0 AND`
- NEW: `): required:= 0 AND`

**:5060** `A` **EXPLICIT**
- OLD: `application (round 5): §7.0`
- NEW: `application: §7.0`


## ops-console-proposal.md

**:4** `A/D` **AUTO_OK**
- OLD: `**PO DECISION (recorded in §20; execution boundary decided 2026-07-11):** the MVP ships WITHOUT an ops console. Interim: dead-end states are exited via controlled, AUTHORIZED ADMIN OPERATIONS — enterp`
- NEW: `**PO DECISION (recorded in §20; execution boundary decided 2026-07-11):** the MVP ships WITHOUT an ops console. Interim: dead-end states are exited via controlled, AUTHORIZED ADMIN OPERATIONS — enterp`

**:6** `D` **KEEP**
- OLD: `**Updated:** 2026-07-11 — second-external-review response: **O12 revised to REPROCESS-SNAPSHOT** (the snapshot XML lives durably in the upstream-populated store, Kafka carries only the storage id — §6`
- KEEP unchanged

**:19** `A` **DECIDE** — normative token(s) in deleted text: AMENDMENT_PARKED, AMOUNT_MISMATCH, CUTOFF_EXPIRED, ENGINE_INCONSISTENCY, NEEDS_REVIEW, OPS_PARKED, RETRY_EXHAUSTED, UNMAPPED_CODE
- OLD: `| `stage_state = BLOCKED` (label NEEDS_REVIEW; reasons RETRY_EXHAUSTED, UNMAPPED_CODE, AMOUNT_MISMATCH, ENGINE_INCONSISTENCY, AMENDMENT_PARKED, OPS_PARKED, ESCALATED; CUTOFF_EXPIRED reserved/never pro`
- NEW: (to be decided — matched: round 10)

**:23** `R` **DECIDE**
- OLD: `| Amendment tie (AMENDMENT_TIE_CONFLICT, §6.7) | two snapshots share an ordering value with DIFFERING payloads — the guard cannot pick a winner, and a verbatim upstream resend ties again forever | REP`
- NEW: (to be decided — matched: 2026-07-11)

**:50** `B` **AUTO_OK**
- OLD: `round 10: the engine owns its calendar). The ONLY overridable`
- NEW: `: the engine owns its calendar). The ONLY overridable`

**:86** `A/B` **DECIDE** — normative token(s) in deleted text: U-9
- OLD: `| O12 | Reprocess stored snapshot (**rounds 3–5: SERVER-VERIFIED, DIGEST-BOUND, CONSUME-AT-START** — closes walkthrough U-9 with no payload storage and no caller-supplied ordering) | input = XML stora`
- NEW: (to be decided — matched: rounds 3, round 5, round 5, Rounds 6)

**:99** `R` **DECIDE**
- OLD: `Derived from `failure-recovery-walkthrough.md` (2026-07-10): every`
- NEW: (to be decided — matched: 2026-07-10)

**:171** `B` **AUTO_OK**
- OLD: `maybe_since age; overpay latches (round 10: no cutoff proximity —`
- NEW: `maybe_since age; overpay latches (no cutoff proximity —`

**:221** `B` **AUTO_OK**
- OLD: `round 5: NEVER an approver identity`
- NEW: `: NEVER an approver identity`

**:224** `B` **AUTO_OK**
- OLD: `POST /trades/{businessId}/reprocess-snapshot {xmlStorageId, reason, ticketRef}       (O12 rounds 3–5; trade-level —`
- NEW: `POST /trades/{businessId}/reprocess-snapshot {xmlStorageId, reason, ticketRef} (O12 –5; trade-level —`

**:234** `B` **AUTO_OK**
- OLD: `POST /approvals/{id}/execute                 (round 5: the ONE execution entry —`
- NEW: `POST /approvals/{id}/execute (the ONE execution entry —`

**:259** `B` **AUTO_OK**
- OLD: `freeze, divergence; round 10: no cutoff term exists) checked`
- NEW: `freeze, divergence;: no cutoff term exists) checked`

**:282** `A` **AUTO_OK**
- OLD: `(round 10: no cutoff term exists to override).`
- NEW: `.`

**:287** `R` **DECIDE**
- OLD: `endpoint (spec = §16.6 artifact 8 / CA-9; 2026-07-11 boundary:`
- NEW: (to be decided — matched: 2026-07-11)

**:294** `A` **AUTO_OK**
- OLD: `O12 (round 3 — SERVER-VERIFIED) Fetch the snapshot XML from the`
- NEW: `O12 Fetch the snapshot XML from the`

**:319** `A` **AUTO_OK**
- OLD: `| — | **Already at MVP, outside this console:** the NON-WAIVABLE §20 minimal exit set — the §9.3 verified-outcome operation + drill (also §18-3), supersede/close, reprocess-snapshot — plus the Q29-wai`
- NEW: `| — | **Already at MVP, outside this console:** the NON-WAIVABLE §20 minimal exit set — the §9.3 verified-outcome operation + drill (also §18-3), supersede/close, reprocess-snapshot — plus the Q29-wai`

**:337** `A` **AUTO_OK**
- OLD: `5. ~~May retry bypass the payment cutoff?~~ **OBSOLETE — no local cutoff exists (round 10):**`
- NEW: `5. ~~May retry bypass the payment cutoff?~~ **OBSOLETE — no local cutoff exists:**`


## failure-recovery-walkthrough.md

**:7** `R` **DECIDE**
- OLD: `**Date:** 2026-07-10.`
- NEW: (to be decided — matched: 2026-07-10)

**:47** `R` **DECIDE**
- OLD: `endpoints (§20 execution boundary, 2026-07-11) +`
- NEW: (to be decided — matched: 2026-07-11)

**:83** `A` **DECIDE** — normative token(s) in deleted text: H-1
- OLD: `| U-8 | Out-of-order delivery — older snapshot arrives after newer | Stale-message metric; alert on volume (§6.7) | The §6.1 ADMISSION gate (round 5, trade_snapshot_state §2.4) refuses it WHOLE — incl`
- NEW: (to be decided — matched: round 5, round-5)

**:84** `A/B` **AUTO_OK**
- OLD: `| U-9 | Two genuine amendments share an ordering timestamp, payloads differ | AMENDMENT_TIE_CONFLICT alert (§6.7, detected at admission as digest-vs-stored-digest) carrying identifiers + a masked diff`
- NEW: `| U-9 | Two genuine amendments share an ordering timestamp, payloads differ | AMENDMENT_TIE_CONFLICT alert (§6.7, detected at admission as digest-vs-stored-digest) carrying identifiers + a masked diff`

**:90** `A/B` **AUTO_OK**
- OLD: `| U-15 | Payment absent from a newer snapshot | Unsent attempts auto-cancel; paid scopes latch + alert | RESOLVED round 10 (PO-9 ANSWERED: absence = amendment to zero, BA-2 amended): §6.4 auto-cancel `
- NEW: `| U-15 | Payment absent from a newer snapshot | Unsent attempts auto-cancel; paid scopes latch + alert | RESOLVED (PO-9 ANSWERED: absence = amendment to zero, BA-2 amended): §6.4 auto-cancel (unsent) `

**:91** `A/B` **AUTO_OK**
- OLD: `| U-16 | Delayed older snapshot hits an obligation absent from newer snapshots — or carries a NEVER-SEEN scope (round-5 sharper variant) | Stale-amount application / stale-scope-creation risk | RESOLV`
- NEW: `| U-16 | Delayed older snapshot hits an obligation absent from newer snapshots — or carries a NEVER-SEEN scope | Stale-amount application / stale-scope-creation risk | RESOLVED (TL-16 answered): the §`

**:94** `A` **DECIDE** — normative token(s) in deleted text: H-1
- OLD: `| U-19 | Upstream serializes an INCOMPLETE payment set (producer bug — round 11 H-1, renumbered round 12; ask 4 does NOT close this class) | Absence = cancellation (BA-2): a dropped block CANCELS a re`
- NEW: (to be decided — matched: round 11, round-11)

**:140** `B` **AUTO_OK**
- OLD: `| P-11 | Retry exhaustion | BLOCKED(RETRY_EXHAUSTED) — round 10: MAX ATTEMPTS is the only bound; CUTOFF_EXPIRED stays RESERVED, never produced | Ops decision: retry (repost_permitted-gated), reject, o`
- NEW: `| P-11 | Retry exhaustion | BLOCKED(RETRY_EXHAUSTED) —: MAX ATTEMPTS is the only bound; CUTOFF_EXPIRED stays RESERVED, never produced | Ops decision: retry (repost_permitted-gated), reject, or superse`

**:174** `D` **KEEP**
- OLD: `| C-12 | provider_reference collision (reused reference) | Reuse metric: the fallback lookup finding >1 candidate is counted + alerted (§8 index decision 2026-07-11 — non-unique index until TL-12 conf`
- KEEP unchanged

**:186** `A` **AUTO_OK**
- OLD: `| R-5 | Post-outage MAYBE population floods the query API | Sweep-overrun metric (§15) | Bounded prioritized batches (oldest-first — round 10), per-row backoff, per-sweep budget from the engine's rate`
- NEW: `| R-5 | Post-outage MAYBE population floods the query API | Sweep-overrun metric (§15) | Bounded prioritized batches, per-row backoff, per-sweep budget from the engine's rate limit (§9.5) | T1 |`

**:210** `A` **AUTO_OK**
- OLD: `| I-1 | Service crash / pod eviction (any worker) | Scanner heartbeats; lease expiry | Leases expire → ENRICH re-claims in place. POST (corrected 2a19c20 M1 — the earlier "pre-call → re-claim" wording`
- NEW: `| I-1 | Service crash / pod eviction (any worker) | Scanner heartbeats; lease expiry | Leases expire → ENRICH re-claims in place. POST: once the claim COMMITTED or its commit is UNKNOWN, expiry ALWAYS`

**:219** `A` **AUTO_OK**
- OLD: `| I-10 | Clock skew between nodes | — | Due-time comparisons use DATABASE time only (§16.4; round 10: no local cutoff calendar exists) | T0 |`
- NEW: `| I-10 | Clock skew between nodes | — | Due-time comparisons use DATABASE time only | T0 |`

**:220** `A` **AUTO_OK**
- OLD: `| I-11 | Log platform outage | Ops observability degraded | Money processing unaffected (logs are not in the money path). For any later §5.2 restore whose window overlaps the outage: step 5b's limit i`
- NEW: `| I-11 | Log platform outage | Ops observability degraded | Money processing unaffected (logs are not in the money path). For any later §5.2 restore whose window overlaps the outage: step 5b's limit i`

**:234** `A` **AUTO_OK**
- OLD: `reconciliation gate signed PASS (aa4399c M3). Interim (pre-runbook): same`
- NEW: `reconciliation gate signed PASS. Interim (pre-runbook): same`

**:262** `A` **AUTO_OK**
- OLD: `| B-6 | Engine-side bank cutoff elapses (external event) while payment unresolved | Nothing local — the ENGINE owns its calendar (round 10, §18-2 CLOSED); a late submission returns as an ordinary engi`
- NEW: `| B-6 | Engine-side bank cutoff elapses (external event) while payment unresolved | Nothing local — the ENGINE owns its calendar; a late submission returns as an ordinary engine response (CA-1) | Engi`

**:294** `R` **DECIDE**
- OLD: `REVISED 2026-07-11 after the second external review + the`
- NEW: (to be decided — matched: 2026-07-11)

**:313** `D` **KEEP**
- OLD: `verbally 2026-07-11); collision-contract sandbox proof; MAYBE terminal`
- KEEP unchanged

**:314** `B` **AUTO_OK**
- OLD: `exit (operation + drill). (§18-2 cutoff calendar: CLOSED round 10 —`
- NEW: `exit (operation + drill). (§18-2 cutoff calendar: —`

**:316** `D` **KEEP**
- OLD: `- PO-9 ANSWERED 2026-07-11 (U-15: absence = amendment to zero). (TL-16`
- KEEP unchanged

**:317** `B` **AUTO_OK**
- OLD: `ANSWERED round 5 — U-16: §6.1 admission + §2.4.)`
- NEW: `— U-16: §6.1 admission + §2.4.)`

**:326** `B` **AUTO_OK**
- OLD: `(authorized application endpoints, RG-05 + OP-04a–e). Round-4 exit`
- NEW: `(authorized application endpoints, RG-05 + OP-04a–e). exit`


## README.md

**:8** `A` **AUTO_OK**
- OLD: `> All `.html` artifacts below are NON-NORMATIVE explanatory snapshots, refreshed manually — where they and `requirment-v4.md` disagree, the requirement doc wins (round 9).`
- NEW: `> All `.html` artifacts below are NON-NORMATIVE explanatory snapshots, refreshed manually — where they and `requirment-v4.md` disagree, the requirement doc wins.`

**:17** `R` **DECIDE**
- OLD: `- [`implementation-playbook.md`](implementation-playbook.md) — ARCHIVED single-file snapshot of the playbook (frozen 2026-07-11; no longer updated — the portable package is the only maintained form; k`
- NEW: (to be decided — matched: 2026-07-11)


## db-schema-dictionary.md

**:142** `A` **AUTO_OK**
- OLD: `| `request_seq` | The IMMUTABLE per-request sequence (1d8a650 M1): the `next_request_seq` value this row consumed, persisted write-once in the creation transaction. Source of truth for the §5.1 identi`
- NEW: `| `request_seq` | The IMMUTABLE per-request sequence: the `next_request_seq` value this row consumed, persisted write-once in the creation transaction. Source of truth for the §5.1 identity-hash input`

**:158** `D` **KEEP**
- OLD: `| `retry_deadline_at` | EXISTS BUT RESERVED/UNUSED: the 2026-07-11 decision made MAX ATTEMPTS the retry limit; the engine owns the cutoff calendar. Kept to avoid schema churn; no rule reads it. |`
- KEEP unchanged

**:176** `R` **DECIDE**
- OLD: `| `required_total_at_creation` | Creation-time stamp for the UI AMOUNT SERIES (2026-07-19): the obligation's `required_amount` read under the lock in the creating transaction. ONE stamp per request ro`
- NEW: (to be decided — matched: 2026-07-19)

**:187** `B` **AUTO_OK**
- OLD: `S-05 isolation tests, 2a19c20 M2); I6 (one active`
- NEW: `S-05 isolation tests); I6 (one active`


## portable-implementation-playbook/00-README.md

**:63** `A` **AUTO_OK**
- OLD: `rule 4's split (round 18): when a card uses inventory/audit wording`
- NEW: `rule 4's split: when a card uses inventory/audit wording`

**:120** `R` **DECIDE**
- OLD: `**Baseline spec:** `requirment-v4.md` (Requirements v4, Factored State Model, 2026-07-05 — BASELINE, hardened through fourteen review rounds).`
- NEW: (to be decided — matched: 2026-07-05)

**:121** `R` **DECIDE**
- OLD: `**Date:** 2026-07-06`
- NEW: (to be decided — matched: 2026-07-06)


## portable-implementation-playbook/01-playbook-index.md

**:101** `D` **KEEP**
- OLD: `- PO-9 ANSWERED 2026-07-11 (absence = amendment to zero; BA-2`
- KEEP unchanged

**:102** `B` **AUTO_OK**
- OLD: `amended §1.1) and TL-16 ANSWERED round 5 (§6.1 admission +`
- NEW: `amended §1.1) and TL-16 (§6.1 admission +`

**:113** `D` **KEEP**
- OLD: `§18 item 2 (cutoff calendar): CLOSED 2026-07-11 — the ENGINE owns`
- KEEP unchanged

**:132** `A` **AUTO_OK**
- OLD: `3. §18 item 2 CLOSED (engine owns the calendar — round 10)`
- NEW: `3. §18 item 2 CLOSED`

**:147** `B` **AUTO_OK**
- OLD: `| P3 Schema | S-01..S-10 | CA-4 published for S-01; CA-5 ALSO published before S-02 (initial value + init policy + identity namespace — 2a19c20 L4; scope model settled as a §1 contract fact — B-01 res`
- NEW: `| P3 Schema | S-01..S-10 | CA-4 published for S-01; CA-5 ALSO published before S-02 (initial value + init policy + identity namespace —; scope model settled as a §1 contract fact — B-01 residue NOT re`

**:154** `A` **AUTO_OK**
- OLD: `| P10 Retry/recovery | RC-01..RC-10 | P6, P7, P9; CA-1/CA-3 | P8 PASS gates auto-downgrade ENABLEMENT (§18-2 closed round 10) | resolver machinery | P11 |`
- NEW: `| P10 Retry/recovery | RC-01..RC-10 | P6, P7, P9; CA-1/CA-3 | P8 PASS gates auto-downgrade ENABLEMENT | resolver machinery | P11 |`

**:155** `A` **AUTO_OK**
- OLD: `| P11 Operator ops | OP-01..OP-03, OP-04a–e (round-9 pre-split) | CA-9; S-06; P6/P7 | §18-3 — this phase satisfies it | operation + signed drill + §20 interim surface | P12 |`
- NEW: `| P11 Operator ops | OP-01..OP-03, OP-04a–e | CA-9; S-06; P6/P7 | §18-3 — this phase satisfies it | operation + signed drill + §20 interim surface | P12 |`

**:158** `A` **DECIDE** — normative token(s) in deleted text: GO-04, Q11, Q12, Q14, Q16, Q17, Q27, Q29, Q5a, Q5b, Q8, Q9
- OLD: `| P14 Rollout | GO-01..GO-05 (round-19 execution order: 01→02→05→04→03) | ALL phases; P8 PASS; OP-03 | §18-0..3 (Q1-Q4/Q28) + MONEY_SAFETY_BLOCKING (Q5a+Q5b/Q8/Q9/Q11/Q12/Q14/Q16/Q17/Q27/Q29-min; Q5b `
- NEW: (to be decided — matched: round-19, round 20)


## portable-implementation-playbook/02-assumptions-and-non-goals.md

**:29** `R` **DECIDE**
- OLD: `mutations are authorized JAVA APPLICATION ENDPOINTS (2026-07-11`
- NEW: (to be decided — matched: 2026-07-11)

**:33** `A` **AUTO_OK**
- OLD: `(§2 — the fourth added 2026-07-11 round 5, admission gate). Plus`
- NEW: `. Plus`

**:36** `R` **DECIDE**
- OLD: `(2026-07-16 — content write-ahead; INSERT-only, never read at`
- NEW: (to be decided — matched: 2026-07-16)

**:59** `B` **AUTO_OK**
- OLD: `execution record — round 5, §9.3) stay rejected and are not`
- NEW: `execution record —, §9.3) stay rejected and are not`


## portable-implementation-playbook/03-requirement-classification.md

**:34** `D` **KEEP**
- OLD: `| C6 | §2.2 | payment_request: 4 dimension columns; supporting fields (identity, uetr, version, claim/retry/resolver fields, last_sent_hash, divergence_expected, divergent_payload_at, episode anchors)`
- KEEP unchanged

**:35** `A/R` **DECIDE**
- OLD: `| C7 | §2.2, §10.3 | Constraints: UNIQUE(idempotency_key), UNIQUE(uetr), the NULL-ignoring conditional unique over (payment_obligation_id, request_seq) (2a19c20 M2), I6 function-based unique index, en`
- NEW: (to be decided — matched: 2a19c20 M2, 2026-07-19)

**:39** `R` **DECIDE**
- OLD: `| C11 | §3, §20 | Supersede/close operation (release-guarded); at MVP exercised via the RG-05 authorized application endpoint (2026-07-11 Java boundary) under §10.3 backstops, not a console | MVP guar`
- NEW: (to be decided — matched: 2026-07-11)

**:40** `B` **AUTO_OK**
- OLD: `| C12 | §4.1 | Step-status predicate — BOTH branches: COMPLETED + the round-11 CANCELLED zero-required terminal (incl. vacuous-completion guards; required = 0 writable only by the §6.1 absence path) |`
- NEW: `| C12 | §4.1 | Step-status predicate — BOTH branches: COMPLETED + the CANCELLED zero-required terminal (incl. vacuous-completion guards; required = 0 writable only by the §6.1 absence path) | MVP | RG`

**:46** `A` **AUTO_OK**
- OLD: `| C18 | §5.2 | Post-restore DR runbook + step-5b enumeration tooling; unfreeze conditional on the Q-22 reconciliation gate (provider listing or manual reconciliation — aa4399c M2) | FUTURE (post-MVP, `
- NEW: `| C18 | §5.2 | Post-restore DR runbook + step-5b enumeration tooling; unfreeze conditional on the Q-22 reconciliation gate | FUTURE (post-MVP, PO decision) | none now; deterministic key (C16) stays | `

**:61** `A` **AUTO_OK**
- OLD: `| C33 | §7.4 | Retry policy per error class (externalized config); exhaustion → BLOCKED; downgrade policy class (attempt reset); bound = max attempts (round 10 — no cutoff) | MVP | RC-04 | — | partial`
- NEW: `| C33 | §7.4 | Retry policy per error class (externalized config); exhaustion → BLOCKED; downgrade policy class (attempt reset); bound = max attempts | MVP | RC-04 | — | partially | yes | no |`

**:71** `R` **DECIDE**
- OLD: `| C43 | §12 | Card read model: business_id-only lookup returning ALL of the trade's obligations (one entry per payment; multiple results = NORMAL, never a health signal), NOT_STARTED = absence, unavai`
- NEW: (to be decided — matched: 2026-07-19)

**:73** `C` **REWRITE**
- OLD: `| C45 | §14 | No TRANSITION-HISTORY journal replaces the structured CAS log line (key+seq+dimensions before→after; posting-claim line carries sent hash; 90-day retention floor); the switch-gated §14.1`
- NEW: (to be decided — matched: review 2b697fb M2)

**:74** `A` **DECIDE** — normative token(s) in deleted text: OB-01
- OLD: `| C46 | §15 | Monitoring list (drift page, MAYBE ages, BLOCKED queue, marker alerts, freeze-effective page, watchdogs); clock discipline (episode anchors); alert rollup; the §6.6 accepted-window candi`
- NEW: (to be decided — matched: b1d91dc M1)

**:75** `R` **DECIDE**
- OLD: `| C47 | §16.1 | Resiliency: timeouts, breakers (business rejects = success), scanner gating, poison-row cap, bulkheads, Hazelcast posting freeze fail-safe (absent/unreachable/timeout = FROZEN; only FR`
- NEW: (to be decided — matched: 2026-07-11)

**:78** `C` **REWRITE**
- OLD: `| C50 | §16.4 | Amount/time hygiene: currency-scale validation, BigDecimal.compareTo, no tolerance, UTC + DB time (cutoff-calendar clause retired round 10) | MVP | IN-01, RC-04 | — | no | yes | no |`
- NEW: (to be decided — matched: round 10)

**:80** `C` **REWRITE**
- OLD: `| C52 | §16.6 | Configuration inventory + config-load ordering validation (trust_age + cadence < escalation < tier-2; cutoff margin retired round 10) | MVP | OB-07 | TL-5, TL-13 | no | yes | values ne`
- NEW: (to be decided — matched: round 10)

**:89** `R` **DECIDE**
- OLD: `| C61 | §16.6-8 | apply-platform-verified-outcome OPERATION spec (authorized application endpoint — 2026-07-11 boundary) + drill script | ARTIFACT + GATE (§18-3) | CA-9, OP-01..03 | B-04 | YES | yes |`
- NEW: (to be decided — matched: 2026-07-11)

**:90** `A/D` **DECIDE** — normative token(s) in deleted text: PO-9, TL-16
- OLD: `| C62 | §18-0 | BLOCKING residue of the snapshot contract: WRITTEN filing of asks 5 + 8 (confirmed verbally 2026-07-11) + §6.0 intake validation — gates IN-02 (PO-9 ANSWERED: absence = zero; TL-16 ans`
- NEW: (to be decided — matched: answered round 5, 2026-07-11)

**:92** `D` **KEEP**
- OLD: `| C64 | §18-2 | CLOSED 2026-07-11: the engine owns its cutoff calendar — no local calendar work; B-03 records the fact + the CA-1 late-submission ask | CLOSED | B-03 | — | no | no | no |`
- KEEP unchanged

**:94** `A` **AUTO_OK**
- OLD: `| C66 | §18 PO 1–8 | PO items: ask-then-retry approval, query cadence, escalation age, cutoff-passed-while-MAYBE (closed round 10), cancelled-trade display, deferral latency, retry-after-reject concep`
- NEW: `| C66 | §18 PO 1–8 | PO items: ask-then-retry approval, query cadence, escalation age, cutoff-passed-while-MAYBE, cancelled-trade display, deferral latency, retry-after-reject concept, fresh-assembly `


## portable-implementation-playbook/04-dependency-graph.md

**:96** `B` **AUTO_OK**
- OLD: `answered round 5: §6.1 admission + §2.4.)`
- NEW: `: §6.1 admission + §2.4.)`


## portable-implementation-playbook/05-implementation-phases.md

**:66** `B` **AUTO_OK**
- OLD: `zero; TL-16 answered round 5.)`
- NEW: `zero; TL-16.)`

**:88** `B` **AUTO_OK**
- OLD: `trade_snapshot_state (S-10, §2.4 — round 5;`
- NEW: `trade_snapshot_state (S-10, §2.4 —;`

**:89** `B` **AUTO_OK**
- OLD: `greenfield, no bootstrap — round 10).`
- NEW: `greenfield, no bootstrap).`

**:397** `B` **AUTO_OK**
- OLD: `(bound = MAX ATTEMPTS, §7.4 round 10 — no cutoff, no`
- NEW: `(bound = MAX ATTEMPTS, §7.4 — no cutoff, no`

**:453** `D` **KEEP**
- OLD: `service (execution boundary decided 2026-07-11;`
- KEEP unchanged

**:455** `B` **AUTO_OK**
- OLD: `the §9.3 approval_id ONLY (round 4 — a prior`
- NEW: `the §9.3 approval_id ONLY (a prior`

**:555** `B` **AUTO_OK**
- OLD: `round 10); dead-gauge`
- NEW: `); dead-gauge`

**:595** `B` **AUTO_OK**
- OLD: `Completion:      all five GO cards done in the round-19 order`
- NEW: `Completion: all five GO cards done in the order`

**:599** `B` **AUTO_OK**
- OLD: `MONEY_SAFETY_BLOCKING, rounds 16/20; Q5b's`
- NEW: `MONEY_SAFETY_BLOCKING, Q5b's`


## portable-implementation-playbook/07-placeholder-glossary.md

**:137** `A` **AUTO_OK**
- OLD: `policy (bound = max attempts, §7.4 round 10);`
- NEW: `policy;`

**:204** `A` **AUTO_OK**
- OLD: `defined/migrated. NAME IS DELIBERATE (round 7 note):`
- NEW: `defined/migrated. NAME IS DELIBERATE:`


## portable-implementation-playbook/08-task-cards/phase-01-discovery.md

**:281** `A` **AUTO_OK**
- OLD: `- **Requirement sections / concepts to read:** playbook Section F status codes; Playbook Index BLOCKED list; requirment-v4.md §2.4 (round 18 — the CANONICAL greenfield fact incl. its bootstrap-restora`
- NEW: `- **Requirement sections / concepts to read:** playbook Section F status codes; Playbook Index BLOCKED list; requirment-v4.md §2.4.`

**:286** `C` **REWRITE**
- OLD: `- **Implementation instructions:** for each F.1–F.26 concept assign IMPLEMENTED/PARTIAL/MISSING/UNCLEAR with one-line evidence (F.26 = the facts-sheet completeness check); list every UNCLEAR with what`
- NEW: (to be decided — matched: 9a53c75, round 17)


## portable-implementation-playbook/08-task-cards/phase-02-blocking-gates-and-artifacts.md

**:13** `A` **AUTO_OK**
- OLD: `- **Title:** File the snapshot-contract written confirmations (asks 5 + 8 incl. the round-11 complete-set line; all design questions settled)`
- NEW: `- **Title:** File the snapshot-contract written confirmations`

**:15** `A` **DECIDE** — normative token(s) in deleted text: PO-9
- OLD: `- **Purpose:** the §1 contract facts record the model: one trade carries MULTIPLE payments; each message is a FULL-TRADE SNAPSHOT (newer overwrites older); (payment_type + debit_account + currency) is`
- NEW: (to be decided — matched: answered round 10)

**:18** `A/B/D` **AUTO_OK**
- OLD: `- **Implementation instructions (residue):** (1) obtain the WRITTEN upstream confirmation of the snapshot schema + within-snapshot uniqueness + the ROUND-11 COMPLETE-SET guarantee (each snapshot carri`
- NEW: `- **Implementation instructions (residue):** (1) obtain the WRITTEN upstream confirmation of the snapshot schema + within-snapshot uniqueness + the COMPLETE-SET guarantee (each snapshot carries the tr`

**:21** `A/B/D` **DECIDE** — normative token(s) in deleted text: H-1
- OLD: `- **Edge cases:** "usually unique" is NOT an answer for ask 5 — the identity contract needs a guarantee (confirmed verbally 2026-07-11; the WRITTEN filing is what closes the item); PO-9 is ANSWERED (a`
- NEW: (to be decided — matched: round 11, round 11, 2026-07-11)

**:22** `D` **KEEP**
- OLD: `- **Manual validation:** written confirmations for asks 5 + 8 attributed and FILED (verbal confirmations recorded 2026-07-11); PO-9/TL-16 answers already in §18.`
- KEEP unchanged

**:24** `D` **KEEP**
- OLD: `- **Failure signs:** treating the 2026-07-11 verbal confirmations as the written contract — the FILED paper is the Q1 evidence.`
- KEEP unchanged

**:54** `A` **AUTO_OK**
- OLD: `### B-03 — Record the §18-2 CLOSURE (engine owns the cutoff calendar — round 10)`
- NEW: `### B-03 — Record the §18-2 CLOSURE`

**:58** `A` **AUTO_OK**
- OLD: `- **Classification:** §18 item — CLOSED (record-keeping task only, round 10)`
- NEW: `- **Classification:** §18 item — CLOSED`

**:59** `D` **KEEP**
- OLD: `- **Purpose:** §18-2 was ANSWERED by the PO 2026-07-11: the payment ENGINE owns its cutoff calendar; this system initiates at any time and carries NO local calendar, cutoff gate, or cutoff config. Thi`
- KEEP unchanged

**:66** `D` **KEEP**
- OLD: `- **Implementation instructions:** (1) record the closure fact with date + source (PO, 2026-07-11); (2) obtain the engine's WRITTEN line that submission is accepted at any time; (3) ask whether a dist`
- KEEP unchanged

**:67** `C` **REWRITE**
- OLD: `- **Do not change:** code; no calendar interface, stub, or config may be built (retired round 10).`
- NEW: (to be decided — matched: round 10)

**:72** `C` **REWRITE**
- OLD: `- **Failure signs:** any local cutoff constant, calendar config, or tz machinery appearing anywhere (retired round 10 — the engine owns the calendar).`
- NEW: (to be decided — matched: round 10)

**:73** `C` **REWRITE**
- OLD: `- **Common mistakes:** rebuilding any local cutoff machinery "just in case" (SPEC_CONFLICT — retired round 10).`
- NEW: (to be decided — matched: round 10)

**:186** `A` **AUTO_OK**
- OLD: `- **Implementation instructions:** specify (schema-shape pseudocode, not final SQL): every §2.1/§2.2 column with type/nullability; scope-key UNIQUE (per B-01!); UNIQUE(idempotency_key), UNIQUE(uetr) (`
- NEW: `- **Implementation instructions:** specify (schema-shape pseudocode, not final SQL): every §2.1/§2.2 column with type/nullability; scope-key UNIQUE (per B-01!); UNIQUE(idempotency_key), UNIQUE(uetr) (`

**:192** `A` **AUTO_OK**
- OLD: `- **Failure signs:** CHECK constraints written VALIDATE-first against unmigrated data; a spec missing the request_seq unique expression, the integer/overflow domain, or the keyset tuple — S-02/ST-04 b`
- NEW: `- **Failure signs:** CHECK constraints written VALIDATE-first against unmigrated data; a spec missing the request_seq unique expression, the integer/overflow domain, or the keyset tuple — S-02/ST-04 b`

**:194** `A` **AUTO_OK**
- OLD: `- **Completion criteria:** spec complete INCLUDING the (payment_obligation_id, request_seq) conditional unique expression, the integer/overflow domain, and the canonical keyset tuple — absence of ANY `
- NEW: `- **Completion criteria:** spec complete INCLUDING the (payment_obligation_id, request_seq) conditional unique expression, the integer/overflow domain, and the canonical keyset tuple — absence of ANY `

**:210** `A` **DECIDE** — normative token(s) in deleted text: K-01, Oracle, S-02, S-08
- OLD: `- **Implementation instructions:** specify: input fields = business_id | payment_type | debit_account | currency | request_seq (no discriminator — scope key settled, §1 contract facts); canonicalizati`
- NEW: (to be decided — matched: 4dbdf2b)

**:216** `A` **AUTO_OK**
- OLD: `- **Failure signs:** vectors computed only by the code under test (circular); a spec missing the initial value, the initialization policy, or the namespace/collision analysis — S-02 becomes under-spec`
- NEW: `- **Failure signs:** vectors computed only by the code under test (circular); a spec missing the initial value, the initialization policy, or the namespace/collision analysis — S-02 becomes under-spec`

**:218** `A` **AUTO_OK**
- OLD: `- **Completion criteria:** spec + vectors published INCLUDING the initial sequence value, the counter-initialization policy, and the versioned namespace + collision analysis — absence of ANY = NOT com`
- NEW: `- **Completion criteria:** spec + vectors published INCLUDING the initial sequence value, the counter-initialization policy, and the versioned namespace + collision analysis — absence of ANY = NOT com`

**:234** `A` **AUTO_OK**
- OLD: `- **Implementation instructions:** define: which instruction fields enter the hash (the business content actually sent — MUST_VERIFY_LOCALLY against the real payload shape, recorded locally); canonica`
- NEW: `- **Implementation instructions:** define: which instruction fields enter the hash (the business content actually sent — MUST_VERIFY_LOCALLY against the real payload shape, recorded locally); canonica`

**:258** `A` **DECIDE** — normative token(s) in deleted text: JOURNAL_ENABLEMENT, T-38
- OLD: `- **Implementation instructions:** adopt Section J's matrix (T-01..T-38) as the seed (T-38 included — its gate is JOURNAL_ENABLEMENT plus the switch-OFF inertness sub-case on the payment gate, review `
- NEW: (to be decided — matched: review 1d8a650 M2)

**:297** `R` **DECIDE**
- OLD: `- **Title:** apply-platform-verified-outcome OPERATION spec (authorized application endpoint — 2026-07-11 execution boundary) + ops drill script (§16.6 artifact 8)`
- NEW: (to be decided — matched: 2026-07-11)

**:306** `A` **DECIDE** — normative token(s) in deleted text: NEVER
- OLD: `- **Implementation instructions:** specify (round-4 canonical model — execution input is the approval_id, NEVER approver-identity parameters): the §9.3 two-step approval workflow — approval-record sch`
- NEW: (to be decided — matched: round-4)

**:321** `R` **DECIDE**
- OLD: `- **Title:** §14.1 attempt journal (payment_attempt_journal, ops/audit schema): implementable spec + typed DDL template + the two insert riders + security package (content write-ahead; 2026-07-16 driv`
- NEW: (to be decided — matched: 2026-07-16)

**:324** `A` **AUTO_OK**
- OLD: `- **Prerequisites:** §14.1 (the normative design); CA-4 (schema authority alignment); CA-6 (payload_hash + the canonical serialization payload_content stores); CA-1 PUBLISHED (the paj_outcome_ck vocab`
- NEW: `- **Prerequisites:** §14.1 (the normative design); CA-4 (schema authority alignment); CA-6 (payload_hash + the canonical serialization payload_content stores); CA-1 PUBLISHED.`

**:330** `R` **DECIDE**
- OLD: `- **Implementation instructions:** author the spec exactly per §14.1 / file 12 CA-10 (the versions of record — 2026-07-17 simplified design): the TYPED DDL TEMPLATE in file 12 (AUD-01 produces the res`
- NEW: (to be decided — matched: 2026-07-17)

**:334** `R` **DECIDE**
- OLD: `- **Manual validation:** DBA + security/privacy + ops review; PO driver on record (2026-07-16).`
- NEW: (to be decided — matched: 2026-07-16)

**:348** `A/D` **AUTO_OK**
- OLD: `- **Blockers to carry forward:** any unanswered §18 item keeps its dependents BLOCKED — §18-0's residue blocks IN-02 ONLY (the §6 consumer freeze; the scope model is a settled §1 contract fact, so S-0`
- NEW: `- **Blockers to carry forward:** any unanswered §18 item keeps its dependents BLOCKED — §18-0's residue blocks IN-02 ONLY (the §6 consumer freeze; the scope model is a settled §1 contract fact, so S-0`

**:351** `A/D` **AUTO_OK**
- OLD: `- **Next phase entry condition:** CA-4 published (DBA-reviewed) → schema freeze may proceed (S-01); S-02 additionally waits for CA-5 published (counter initial value + init policy + identity namespace`
- NEW: `- **Next phase entry condition:** CA-4 published (DBA-reviewed) → schema freeze may proceed (S-01); S-02 additionally waits for CA-5 published. B-01's residue continues in parallel and gates IN-02, no`


## portable-implementation-playbook/08-task-cards/phase-03-schema-and-migration.md

**:1** `C` **REWRITE**
- OLD: `> **Purpose:** Task cards S-01..S-10 + AUD-01 (schema and migration foundation; S-10 = trade_snapshot_state; AUD-01 = the §14.1 attempt-journal schema, off-chain; the former S-11 bootstrap was RETIRED`
- NEW: (to be decided — matched: round 10)

**:16** `D` **KEEP**
- OLD: `- **Prerequisites:** CA-4 published; D-02 done. (B-01 residue NOT required — the scope model is a settled §1 contract fact; B-01 gates the §6 consumer freeze IN-02, not schema. Normalized 2026-07-11.)`
- KEEP unchanged

**:22** `A` **DECIDE** — normative token(s) in deleted text: NOVALIDATE, ORA-01452, UNIQUE
- OLD: `- **Implementation instructions:** write the ordered migration list (numbers reserved, one concern per migration; ORDER CORRECTED 289ef66 M1 — backfill precedes the constraint objects legacy data coul`
- NEW: (to be decided — matched: ORDER CORRECTED 289ef66 M1, round 14)

**:39** `A` **DECIDE** — normative token(s) in deleted text: NEVER, ONLY, S-02, S-05
- OLD: `- **Purpose:** land the §2.1 fields: amounts, overpay_blocked, next_request_seq, upstream_ordering, correlation_id, ordering-tagged markers (validation_failed_at/_ordering, provider_rejected_at/code/_`
- NEW: (to be decided — matched: round 12)

**:40** `A/D` **DECIDE** — normative token(s) in deleted text: CA-4, CA-5, STOP
- OLD: `- **Prerequisites:** S-01; CA-4 + CA-5 PUBLISHED (6cb3005 M1 — this task consumes the CA-5 initial value + initialization policy and CA-4's request_seq unique expression + overflow domain; if EITHER d`
- NEW: (to be decided — matched: 6cb3005 M1, 2026-07-11)

**:46** `A` **EXPLICIT**
- OLD: `- **Implementation instructions:** one migration (or few, per S-01 plan): add each missing §2.1 column nullable/defaulted; SCOPE-KEY PREFLIGHT (this task OWNS the scope-key duplicate class — the sanct`
- NEW: `- **Implementation instructions:** one migration (or few, per S-01 plan): add each missing §2.1 column nullable/defaulted; SCOPE-KEY PREFLIGHT (this task OWNS the scope-key duplicate class — the sanct`

**:48** `A` **EXPLICIT**
- OLD: `- **Tests to add:** migration applies on clean schema and on a prod-shaped copy; entity round-trip persists new columns; counter-init (4dbdf2b M1): ZERO obligation rows with NULL next_request_seq afte`
- NEW: `- **Tests to add:** migration applies on clean schema and on a prod-shaped copy; entity round-trip persists new columns; counter-init: ZERO obligation rows with NULL next_request_seq after apply on th`

**:63** `A/R` **DECIDE**
- OLD: `- **Purpose:** land stage, stage_state, submission_state, outcome, blocked_reason, amount, request_seq (§2.2 immutable per-request sequence, 1d8a650 M1 — NUMBER, nullable, write-once at creation), ide`
- NEW: (to be decided — matched: 1d8a650 M1, 2026-07-19)

**:70** `A` **EXPLICIT**
- OLD: `- **Implementation instructions:** per S-01 plan; every new column nullable (backfill in S-08 populates dimensions for legacy rows); NO CHECKs yet (S-05); entity mapping additive. required_total_at_cr`
- NEW: `- **Implementation instructions:** per S-01 plan; every new column nullable (backfill in S-08 populates dimensions for legacy rows); NO CHECKs yet (S-05); entity mapping additive. required_total_at_cr`

**:72** `A` **EXPLICIT**
- OLD: `- **Tests to add:** migration apply tests; entity round-trip; stamp-type round-trip boundary tests at scale 0, 2, and 3 and the maximum supported amount (0e09f09 L2).`
- NEW: `- **Tests to add:** migration apply tests; entity round-trip; stamp-type round-trip boundary tests at scale 0, 2, and 3 and the maximum supported amount.`

**:106** `A` **EXPLICIT**
- OLD: `### S-10 — trade_snapshot_state table (admission gate — round 5)`
- NEW: `### S-10 — trade_snapshot_state table`

**:111** `A` **EXPLICIT**
- OLD: `- **Purpose:** §2.4/§6.1 (round 5): the trade-level watermark + applied-snapshot pointer. Money safety, not audit — without it, a delayed older snapshot can CREATE a never-seen scope and pay a payment`
- NEW: `- **Purpose:** §2.4/§6.1: the trade-level watermark + applied-snapshot pointer. Money safety, not audit — without it, a delayed older snapshot can CREATE a never-seen scope and pay a payment the newer`

**:119** `A` **EXPLICIT**
- OLD: `- **Do not change:** the three existing tables (no obligation column for the trade reference — §7.0 reads the stored snapshot instead, PO-confirmed round 5).`
- NEW: `- **Do not change:** the three existing tables.`

**:128** `A` **EXPLICIT**
- OLD: `- **Next task:** S-08 (backfill runs BEFORE the constraint objects legacy data could violate — 289ef66 M1).`
- NEW: `- **Next task:** S-08.`

**:135** `B` **EXPLICIT**
- OLD: `- **Purpose:** make illegal states unrepresentable at the DB — the backstop for every DB-ENFORCEABLE invariant named by the schema contract (§2.2, §10.3; scoped per 1d8a650 L2: display-only/cross-syst`
- NEW: `- **Purpose:** make illegal states unrepresentable at the DB — the backstop for every DB-ENFORCEABLE invariant named by the schema contract (§2.2, §10.3; scoped per: display-only/cross-system invarian`

**:136** `A` **DECIDE** — normative token(s) in deleted text: NOVALIDATE, ORA-01452, UNIQUE
- OLD: `- **Prerequisites:** S-03; S-08 backfill DONE — the normative order now runs S-08 BEFORE this task (289ef66 M1: I6 is a UNIQUE INDEX; NOVALIDATE does NOT apply to unique indexes, and pre-backfill ever`
- NEW: (to be decided — matched: 289ef66 M1)

**:142** `A` **DECIDE** — normative token(s) in deleted text: S-02
- OLD: `- **Implementation instructions:** per CA-4: per-column enum CHECKs for the four dimensions + blocked_reason + the obligation's ui_step_status CHECK — IN ('IN_PROGRESS','COMPLETED','CANCELLED') (round`
- NEW: (to be decided — matched: 1d8a650 M1, round 13)

**:144** `A` **DECIDE** — normative token(s) in deleted text: UNIQUE
- OLD: `- **Tests to add:** one violation test per constraint (insert/update illegal row → ORA error); ui_step_status CHECK violation test (a fourth value refused — round 13); I6 test (second active request f`
- NEW: (to be decided — matched: 2a19c20 M2, 58f5a64 note, round 13)

**:185** `A` **EXPLICIT**
- OLD: `- **Requirement sections / concepts to read:** §16.6 artifact 4 (index list), §9.5 (sweep order: oldest maybe_since first — round 10, no cutoff), §15 (scan scopes).`
- NEW: `- **Requirement sections / concepts to read:** §16.6 artifact 4 (index list), §9.5, §15 (scan scopes).`

**:205** `A` **EXPLICIT**
- OLD: `- **Title:** Backfill stage/stage_state/submission_state/outcome (+ anchors where derivable) from the legacy status for existing rows; re-derive the obligation read-model status for every existing obl`
- NEW: `- **Title:** Backfill stage/stage_state/submission_state/outcome (anchors where derivable) from the legacy status for existing rows; re-derive the obligation read-model status for every existing oblig`

**:214** `A` **DECIDE** — normative token(s) in deleted text: BLOCKING, S-01, S-02
- OLD: `- **Implementation instructions:** STEP 0 — EXECUTE THE S-01 PREFLIGHT (ownership assigned here — follow-up M1 on 289ef66: S-01 only PLANS the preflight; THIS task runs it, BEFORE any data mutation): `
- NEW: (to be decided — matched: follow-up M1 on 289ef66, 1d8a650 M1, 4dbdf2b M1, round 14)

**:216** `A` **EXPLICIT**
- OLD: `- **Tests to add:** backfill idempotency (re-run = no-op); per-legacy-value spot checks; post-backfill constraint dry-validate; obligation read-model pass: idempotency + one test per branch example + `
- NEW: `- **Tests to add:** backfill idempotency (re-run = no-op); per-legacy-value spot checks; post-backfill constraint dry-validate; obligation read-model pass: idempotency + one test per branch example + `

**:217** `B` **EXPLICIT**
- OLD: `- **Edge cases:** in-flight rows DURING backfill (dual-write not yet on) — run in a quiet window per the S-01 plan; rows whose legacy status contradicts money fields → list for human review, skip, rep`
- NEW: `- **Edge cases:** in-flight rows DURING backfill (dual-write not yet on) — run in a quiet window per the S-01 plan; rows whose legacy status contradicts money fields → list for human review, skip, rep`

**:224** `C` **REWRITE**
- OLD: `- **Next task:** S-05 (with the backfilled data in place, I6 and the UNIQUEs can now build — 289ef66 M1). (S-11 was RETIRED round 10 — §2.4 greenfield fact: nothing to bootstrap.)`
- NEW: (to be decided — matched: 289ef66 M1, round 10)

**:238** `A` **DECIDE** — normative token(s) in deleted text: Oracle, S-05
- OLD: `- **Implementation instructions:** run/automate: full sequence on clean Oracle; full sequence on a prod-shaped copy (with backfill) whose fixture INCLUDES at least one obligation with MULTIPLE histori`
- NEW: (to be decided — matched: 289ef66 M1, 2a19c20 M2, round 15, round 14)

**:262** `A` **DECIDE** — normative token(s) in deleted text: AUDIT_ADMIN
- OLD: `- **Implementation instructions:** RESOLVE CA-10's DDL TEMPLATE into the migration (review 4d5cb83 M1): substitute EVERY placeholder from its recorded source (request-id type from D-02, outcome tokens`
- NEW: (to be decided — matched: review 4d5cb83 M1, review 4d5cb83 M2, review d00ef6a H2, review c8a92f1 M3)

**:264** `A` **EXPLICIT**
- OLD: `- **Tests to add:** PREFLIGHT (review 928341a M2): the resolved migration contains ZERO angle-bracket tokens and the substitution manifest validates (every placeholder → value → source); T-38 schema s`
- NEW: `- **Tests to add:** PREFLIGHT: the resolved migration contains ZERO angle-bracket tokens and the substitution manifest validates (every placeholder → value → source); T-38 schema slice: INSERT-only en`

**:279** `A` **EXPLICIT**
- OLD: `- **Phase outputs:** four-table schema at the CA-4 target: columns, scope-key UNIQUE, UNIQUE(idempotency_key), NULL-ignoring UNIQUE(uetr), the NULL-ignoring (payment_obligation_id, request_seq) unique`
- NEW: `- **Phase outputs:** four-table schema at the CA-4 target: columns, scope-key UNIQUE, UNIQUE(idempotency_key), NULL-ignoring UNIQUE(uetr), the NULL-ignoring (payment_obligation_id, request_seq) unique`

**:282** `A` **EXPLICIT**
- OLD: `- **Tests expected to exist:** migration apply (clean + prod-shaped), constraint violation suite, trigger backstop suite (incl. pool non-leakage), backfill idempotency, counter-init proof (zero NULL n`
- NEW: `- **Tests expected to exist:** migration apply (clean + prod-shaped), constraint violation suite, trigger backstop suite (incl. pool non-leakage), backfill idempotency, counter-init proof, S-09 dual-r`


## portable-implementation-playbook/08-task-cards/phase-04-identity-and-idempotency.md

**:22** `B` **AUTO_OK**
- OLD: `- **Implementation instructions:** in the creation path: obligation row locked (SELECT FOR UPDATE) → read seq → increment → use in K-02 derivation → insert request with the consumed value persisted in`
- NEW: `- **Implementation instructions:** in the creation path: obligation row locked (SELECT FOR UPDATE) → read seq → increment → use in K-02 derivation → insert request with the consumed value persisted in`

**:24** `A` **AUTO_OK**
- OLD: `- **Tests to add:** two concurrent creations on one obligation → distinct sequential seqs (the lock serializes); rollback does not burn a seq inconsistently with the inserted row (both roll back toget`
- NEW: `- **Tests to add:** two concurrent creations on one obligation → distinct sequential seqs (the lock serializes); rollback does not burn a seq inconsistently with the inserted row (both roll back toget`

**:94** `A` **AUTO_OK**
- OLD: `- **Implementation instructions:** restructure the posting path so that: claim transaction persists (first claim) the identity + (every claim, K-05) hash/flag/attempt-stamp, COMMITS, and only then the`
- NEW: `- **Implementation instructions:** restructure the posting path so that: claim transaction persists (first claim) the identity + (every claim, K-05) hash/flag/attempt-stamp, COMMITS, and only then the`

**:143** `A` **AUTO_OK**
- OLD: `- **Implementation instructions:** integration tests: (1) crash before POST (after claim commit) → the row reaches its re-POST ONLY via lease expiry → MAYBE_SUBMITTED → resolver → the §9.2 downgrade (`
- NEW: `- **Implementation instructions:** integration tests: (1) crash before POST (after claim commit) → the row reaches its re-POST ONLY via lease expiry → MAYBE_SUBMITTED → resolver → the §9.2 downgrade, `

**:145** `A` **DECIDE** — normative token(s) in deleted text: T-07, T-08, T-09, T-10
- OLD: `- **Tests to add:** the four above (catalog T-08/T-09/T-10 alignment — corrected 2a19c20 L2; T-07 is the hash/divergence set, not this one).`
- NEW: (to be decided — matched: corrected 2a19c20 L2)


## portable-implementation-playbook/08-task-cards/phase-05-uetr-response-persistence.md

**:39** `D` **KEEP**
- OLD: `- **Purpose:** §2.2: secondary feed-matching key (§8) with fail-closed fallback semantics; reuse made loud by METRIC, never by constraint (§8 index decision 2026-07-11).`
- KEEP unchanged

**:87** `D` **KEEP**
- OLD: `- **Phase outputs:** uetr persisted from acceptance-class responses ONLY (never overwritten; never from DUPLICATE_REQUEST/collision/rejects); provider_reference persisted as a distinct field with a NO`
- KEEP unchanged


## portable-implementation-playbook/08-task-cards/phase-06-factored-state-model.md

**:85** `R` **DECIDE**
- OLD: `- **Title:** Implement §10.4 labels as a derived view/expression; route dashboards/card/log/ops reads to it; implement the §12 ALL-PAYMENTS TABLE projection (request-granular read surface, 2026-07-17)`
- NEW: (to be decided — matched: 2026-07-17)

**:94** `A/D` **EXPLICIT**
- OLD: `- **Implementation instructions:** implement the §10.4 mapping exactly (DB view or shared expression — choose per local convention, record); migrate DISPLAY consumers (dashboards, card payload's label`
- NEW: `- **Implementation instructions:** implement the §10.4 mapping exactly (DB view or shared expression — choose per local convention, record); migrate DISPLAY consumers (dashboards, card payload's label`

**:183** `C` **REWRITE**
- OLD: `- **Purpose:** §14: the transition record — no TRANSITION-HISTORY journal replaces it (the switch-gated §14.1 attempt-content journal is a separate audit sink, never a log replacement — review 2b697fb`
- NEW: (to be decided — matched: review 2b697fb M2, b760786 M2, review 4098532 H1)

**:190** `A/B` **EXPLICIT**
- OLD: `- **Implementation instructions:** one emission point in the shared CAS helper (fires only on rowCount==1) — and DELIVERY per the §14 contract (review 4098532 H1): the helper BUFFERS the line and regi`
- NEW: `- **Implementation instructions:** one emission point in the shared CAS helper (fires only on rowCount==1) — and DELIVERY per the §14 contract: the helper BUFFERS the line and registers an after-commi`

**:192** `A` **EXPLICIT**
- OLD: `- **Tests to add:** log-capture test per transition family: line present, fields populated, before/after correct, no account data; ATTEMPT-class capture (b1d91dc M3 + b760786 M2): the posting-claim, r`
- NEW: `- **Tests to add:** log-capture test per transition family: line present, fields populated, before/after correct, no account data; ATTEMPT-class capture: the posting-claim, response-resolution, and le`

**:214** `D` **KEEP**
- OLD: `- **Implementation instructions:** standard claim CAS: READY/RETRY_WAIT(due) → CLAIMED + claimed_by + claim_expires_at, WHERE carries prior state + outcome IS NULL; work; completion CAS moves onward a`
- KEEP unchanged

**:238** `A` **EXPLICIT**
- OLD: `- **Implementation instructions:** expiry sweep (or claim-time check): CLAIMED + claim_expires_at < DB now → ENRICH: CAS back to READY (clear claim fields); POST: CAS to stage=CONFIRM, stage_state=REA`
- NEW: `- **Implementation instructions:** expiry sweep (or claim-time check): CLAIMED + claim_expires_at < DB now → ENRICH: CAS back to READY (clear claim fields); POST: CAS to stage=CONFIRM, stage_state=REA`


## portable-implementation-playbook/08-task-cards/phase-07-reservation-and-release-guards.md

**:111** `R` **DECIDE**
- OLD: `- **Purpose:** releasing a reservation whose money may have moved is the one remaining double-payment path (§3); the supersede/close operation is a §3 REQUIRED feature, executed at MVP as a controlled`
- NEW: (to be decided — matched: 2026-07-11)

**:144** `C` **REWRITE**
- OLD: `- **Tests to add:** each T-trigger fires evaluation; each gate condition blocks; successor policy: CANCELLED/SUPERSEDED permit; EXECUTED permits iff shortfall remains; REJECTED permits iff upstream_or`
- NEW: (to be decided — matched: 0e09f09 L1)

**:190** `B` **EXPLICIT**
- OLD: `- **Implementation instructions:** predicate exactly per §4.1: required NOT NULL ∧ required > 0 ∧ confirmed >= required ∧ committed = confirmed ∧ latch clear ∧ validation_failed not LIVE (LIVE = marke`
- NEW: `- **Implementation instructions:** predicate exactly per §4.1: required NOT NULL ∧ required > 0 ∧ confirmed >= required ∧ committed = confirmed ∧ latch clear ∧ validation_failed not LIVE (LIVE = marke`

**:192** `A` **EXPLICIT**
- OLD: `- **Tests to add:** each predicate term isolated (anchor row cannot complete; post-decrement zero-zero cannot complete; active request blocks completion; recovered anchor completes after valid message`
- NEW: `- **Tests to add:** each predicate term isolated (anchor row cannot complete; post-decrement zero-zero cannot complete; active request blocks completion; recovered anchor completes after valid message`

**:214** `B` **EXPLICIT**
- OLD: `- **Implementation instructions:** in the same derivation pass: evaluate §4.2's ranks in order over live conditions (active requests only; rounds 12–13: required_amount = 0 suppresses ONLY the histori`
- NEW: `- **Implementation instructions:** in the same derivation pass: evaluate §4.2's ranks in order over live conditions (active requests only; –13: required_amount = 0 suppresses ONLY the historical PROVI`

**:216** `A/B` **EXPLICIT**
- OLD: `- **Tests to add:** precedence (MAYBE outranks OVERPAY outranks validation etc. per ranks); derivation clears by construction (corrected message → DATA_VALIDATION_FAILED gone in the same transaction);`
- NEW: `- **Tests to add:** precedence (MAYBE outranks OVERPAY outranks validation etc. per ranks); derivation clears by construction (corrected message → DATA_VALIDATION_FAILED gone in the same transaction);`

**:229** `A` **EXPLICIT**
- OLD: `- **Title:** Implement §6.5 reopening (required increase after COMPLETED, or positive-again after CANCELLED — round 12) with reopened_at, and the latch guard (no reopening-created requests on latched `
- NEW: `- **Title:** Implement §6.5 reopening with reopened_at, and the latch guard (no reopening-created requests on latched scopes)`

**:238** `A` **EXPLICIT**
- OLD: `- **Implementation instructions:** on an applied required increase against a scope whose derived status is COMPLETED or CANCELLED (round 12 — a reappeared removed payment reopens IDENTICALLY): recalc `
- NEW: `- **Implementation instructions:** on an applied required increase against a scope whose derived status is COMPLETED or CANCELLED: recalc shortfall under lock; RG-06 evaluation creates requests (unles`

**:240** `A` **EXPLICIT**
- OLD: `- **Tests to add:** reopening full trace (COMPLETED → IN_PROGRESS + reopened_at + successor); CANCELLED → IN_PROGRESS + reopened_at + successor (clean reappearance — round 12); reappearance with provi`
- NEW: `- **Tests to add:** reopening full trace (COMPLETED → IN_PROGRESS + reopened_at + successor); CANCELLED → IN_PROGRESS + reopened_at + successor; reappearance with provider_reject_count = 1 (marker wen`


## portable-implementation-playbook/08-task-cards/phase-08-provider-contract-tests.md

**:46** `B` **EXPLICIT**
- OLD: `- **Implementation instructions:** POST once (record response); re-POST byte-identical payload same key; assert: no second execution — round 16: a status query showing ONE visible payment is NOT suffi`
- NEW: `- **Implementation instructions:** POST once (record response); re-POST byte-identical payload same key; assert: no second execution —: a status query showing ONE visible payment is NOT sufficient (pr`

**:50** `A` **EXPLICIT**
- OLD: `- **Manual validation:** engine-side EXECUTION-COUNT verification (round 16 — not status-query visibility alone).`
- NEW: `- **Manual validation:** engine-side EXECUTION-COUNT verification.`

**:70** `B` **EXPLICIT**
- OLD: `- **Implementation instructions:** POST; re-POST same key with a changed business field (and separately a changed amount); assert: no execution of the second — round 16: prove via provider-side EXECUT`
- NEW: `- **Implementation instructions:** POST; re-POST same key with a changed business field (and separately a changed amount); assert: no execution of the second —: prove via provider-side EXECUTION/AUDIT`

**:74** `A` **EXPLICIT**
- OLD: `- **Manual validation:** engine-side execution-COUNT check (round 16).`
- NEW: `- **Manual validation:** engine-side execution-COUNT check.`

**:94** `B` **EXPLICIT**
- OLD: `- **Implementation instructions:** as close to the stated TTL edge as sandbox practicality allows (provider cooperation may be needed — aged keys or clock manipulation on their side): re-run (a) and (`
- NEW: `- **Implementation instructions:** as close to the stated TTL edge as sandbox practicality allows (provider cooperation may be needed — aged keys or clock manipulation on their side): re-run (a) and (`

**:102** `A` **EXPLICIT**
- OLD: `- **Completion criteria:** evidence filed + the typed consequence record in a non-UNRESOLVED state; if IMPLEMENTATION_REQUIRED, THIS card completes but the RELEASE gates (F4/Q2/Q10/go-live) stay block`
- NEW: `- **Completion criteria:** evidence filed + the typed consequence record in a non-UNRESOLVED state; if IMPLEMENTATION_REQUIRED, THIS card completes but the RELEASE gates (F4/Q2/Q10/go-live) stay block`

**:118** `B` **EXPLICIT**
- OLD: `- **Implementation instructions:** induce a sync business reject; re-POST same key (correctable condition resolved if the sandbox allows); record: re-executes vs replays rejection; emit the TYPED CONS`
- NEW: `- **Implementation instructions:** induce a sync business reject; re-POST same key (correctable condition resolved if the sandbox allows); record: re-executes vs replays rejection; emit the TYPED CONS`

**:126** `A` **EXPLICIT**
- OLD: `- **Completion criteria:** evidence filed + the typed consequence record in a non-UNRESOLVED state; if IMPLEMENTATION_REQUIRED, the release gates stay blocked until IMPLEMENTED_AND_VERIFIED (289ef66 M`
- NEW: `- **Completion criteria:** evidence filed + the typed consequence record in a non-UNRESOLVED state; if IMPLEMENTATION_REQUIRED, the release gates stay blocked until IMPLEMENTED_AND_VERIFIED.`


## portable-implementation-playbook/08-task-cards/phase-09-inbound-flows-and-status-feed.md

**:25** `A` **AUTO_OK**
- OLD: `- **Edge cases:** absent/zero BLOCK amount → reject (BA-2 context: a present block's amount is strictly positive; required = 0 is writable ONLY by the §6.1 absence path); an EMPTY derived payment set `
- NEW: `- **Edge cases:** absent/zero BLOCK amount → reject (BA-2 context: a present block's amount is strictly positive; required = 0 is writable ONLY by the §6.1 absence path); an EMPTY derived payment set `

**:37** `A` **AUTO_OK**
- OLD: `- **Title:** Trade-level snapshot ADMISSION (round 5); snapshot fan-out; locked obligation upsert; strictly-newer ordering mutation; tie handling; stale counting`
- NEW: `- **Title:** Trade-level snapshot ADMISSION; snapshot fan-out; locked obligation upsert; strictly-newer ordering mutation; tie handling; stale counting`

**:39** `A` **AUTO_OK**
- OLD: `- **Purpose:** §6.1/§6.7/§2.4: a message is a FULL-TRADE SNAPSHOT that must pass the trade-level ADMISSION gate before ANY per-block work (round 5: per-obligation watermarks cannot stop a stale snapsh`
- NEW: `- **Purpose:** §6.1/§6.7/§2.4: a message is a FULL-TRADE SNAPSHOT that must pass the trade-level ADMISSION gate before ANY per-block work, then fans out to one application per payment block; a redeliv`

**:40** `B/D` **AUTO_OK**
- OLD: `- **Prerequisites:** IN-01; S-02; S-10 (trade_snapshot_state); B-01 RESIDUE (upstream asks 5 + 8 CONFIRMED 2026-07-11, WRITTEN docs pending — §18-0(a)/(d): the freeze needs the filed paper, the design`
- NEW: `- **Prerequisites:** IN-01; S-02; S-10 (trade_snapshot_state); B-01 RESIDUE (upstream asks 5 + 8 CONFIRMED 2026-07-11, WRITTEN docs pending — §18-0(a)/(d): the freeze needs the filed paper, the design`

**:46** `A/D` **AUTO_OK**
- OLD: `- **Implementation instructions:** validate the snapshot ONCE (schema, amounts, within-snapshot tuple uniqueness → whole-snapshot validation failure per §6.0/§6.6); then ADMISSION in its own transacti`
- NEW: `- **Implementation instructions:** validate the snapshot ONCE (schema, amounts, within-snapshot tuple uniqueness → whole-snapshot validation failure per §6.0/§6.6); then ADMISSION in its own transacti`

**:48** `C` **REWRITE**
- OLD: `- **Tests to add:** the §6.7 failure trace (late original must not regress 120→100); strictly-newer applies; equal-older counted+dropped; both tie branches at admission (digest equal → silent converge`
- NEW: (to be decided — matched: round 12)

**:53** `A` **AUTO_OK**
- OLD: `- **Common mistakes:** >= instead of strictly-newer; tie-differing silently dropped ("upstream resends" is NOT a recovery for ties — §6.7); creating a scope in a block transaction WITHOUT locking the `
- NEW: `- **Common mistakes:** >= instead of strictly-newer; tie-differing silently dropped ("upstream resends" is NOT a recovery for ties — §6.7); creating a scope in a block transaction WITHOUT locking the `

**:64** `D` **KEEP**
- OLD: `- **Prerequisites:** IN-02. Marker-write helper: implement it HERE as the single shared helper (DECIDED 2026-07-11 — this is not a coordination point); IN-04 then EXTENDS it with monotonic re-tag cove`
- KEEP unchanged

**:70** `A` **AUTO_OK**
- OLD: `- **Implementation instructions:** validation failure with extractable scope + ui_process_instance_id → persist anchor INPUTS only (round 13): the row with required_amount NULL + the validation_failed`
- NEW: `- **Implementation instructions:** validation failure with extractable scope + ui_process_instance_id → persist anchor INPUTS only: the row with required_amount NULL + the validation_failed marker via`

**:72** `A` **AUTO_OK**
- OLD: `- **Tests to add:** anchor created with NULL amount; §4.1 cannot complete it; both read-model fields provably come from the derivation hook — no direct writer remains (round 13); later valid message p`
- NEW: `- **Tests to add:** anchor created with NULL amount; §4.1 cannot complete it; both read-model fields provably come from the derivation hook — no direct writer remains; later valid message populates + `


## portable-implementation-playbook/08-task-cards/phase-10-retry-recovery-maybe.md

**:46** `A` **EXPLICIT**
- OLD: `- **Implementation instructions:** per class: connect-fail → NOT_SUBMITTED, POST·RETRY_WAIT; ambiguous (read timeout/reset/crash) → MAYBE, CONFIRM·READY (+maybe_since); sync accepted → SUBMITTED (+sub`
- NEW: `- **Implementation instructions:** per class: connect-fail → NOT_SUBMITTED, POST·RETRY_WAIT; ambiguous (read timeout/reset/crash) → MAYBE, CONFIRM·READY (maybe_since); sync accepted → SUBMITTED (submi`

**:64** `C` **REWRITE**
- OLD: `- **Prerequisites:** RC-02 (divergent_payload_at written); RC-09 (freeze check — can stub as FROZEN-safe until RC-09 lands). (The former B-03/cutoff prerequisite was RETIRED round 10 — no cutoff term `
- NEW: (to be decided — matched: 289ef66 M2, round 10)

**:70** `A` **EXPLICIT**
- OLD: `- **Implementation instructions:** repost_permitted(request) = divergent_payload_at IS NULL ∧ NOT(amount stale vs current shortfall ∧ MAYBE_SUBMITTED) ∧ freeze OFF ∧ outcome IS NULL (round 10: NO cuto`
- NEW: `- **Implementation instructions:** repost_permitted(request) = divergent_payload_at IS NULL ∧ NOT(amount stale vs current shortfall ∧ MAYBE_SUBMITTED) ∧ freeze OFF ∧ outcome IS NULL; called by: §9.2 d`

**:85** `A` **EXPLICIT**
- OLD: `- **Title:** Retry scanner per §7.4: per-error-class policy from config, exhaustion → BLOCKED, downgrade policy class, zero-attempt outage windows (structural; round 10 — no cutoff pre-checks, the eng`
- NEW: `- **Title:** Retry scanner per §7.4: per-error-class policy from config, exhaustion → BLOCKED, downgrade policy class, zero-attempt outage windows`

**:88** `C` **REWRITE**
- OLD: `- **Prerequisites:** ST-09 (claims), RC-03 (gate), RC-01/02 (classification + application). (B-03 prerequisite RETIRED round 10.) CT-05 REOPEN TERM (289ef66 M2): if CT-05's typed consequence record re`
- NEW: (to be decided — matched: 289ef66 M2, round 10)

**:94** `C` **REWRITE**
- OLD: `- **Implementation instructions:** scanner: breaker-gated, §11 claim protocol (lock-free bounded candidate selection, jittered backoff; per candidate a NEW transaction locks the OBLIGATION first then `
- NEW: (to be decided — matched: ROUND 10, round-8/9)

**:118** `A` **EXPLICIT**
- OLD: `- **Implementation instructions:** scope: ACTIVE ∧ MAYBE (any stage/stage_state incl. BLOCKED) ∪ ACTIVE ∧ SUBMITTED older than confirmation age (incl. BLOCKED); order: oldest maybe_since first (round `
- NEW: `- **Implementation instructions:** scope: ACTIVE ∧ MAYBE (any stage/stage_state incl. BLOCKED) ∪ ACTIVE ∧ SUBMITTED older than confirmation age (incl. BLOCKED); order: oldest maybe_since first; per-sw`

**:169** `A` **EXPLICIT**
- OLD: `- **Edge cases:** rows aged past the engine's query lookback ride the §18-1(c) retention proof / named TTL decision (round 10 — no local cutoff guard exists); DUPLICATE_REQUEST answering the downgrade`
- NEW: `- **Edge cases:** rows aged past the engine's query lookback ride the §18-1(c) retention proof / named TTL decision; DUPLICATE_REQUEST answering the downgrade re-POST → MAYBE + query (hidden earlier a`

**:183** `A` **EXPLICIT**
- OLD: `- **Purpose:** bounded human hand-off for unresolved MAYBE rows, early enough to act while the payment still matters (age-based — round 10); never a downgrade⇄escalate cycle.`
- NEW: `- **Purpose:** bounded human hand-off for unresolved MAYBE rows, early enough to act while the payment still matters; never a downgrade⇄escalate cycle.`

**:216** `A` **EXPLICIT**
- OLD: `- **Tests to add:** all three fail-safe conditions read FROZEN; unfrozen never cached (two reads hit the grid); frozen blocks claim and POST; resolver/feed/reads unaffected while frozen; QUEUE-RACE (r`
- NEW: `- **Tests to add:** all three fail-safe conditions read FROZEN; unfrozen never cached (two reads hit the grid); frozen blocks claim and POST; resolver/feed/reads unaffected while frozen; QUEUE-RACE: s`

**:217** `A` **EXPLICIT**
- OLD: `- **Edge cases:** flip mid-flight: in-flight POST completes (drain semantics §11/§16.1) — assert no interruption machinery exists. LINEARIZATION (§16.1, review 4d5cb83 L2): a worker that passed its pr`
- NEW: `- **Edge cases:** flip mid-flight: in-flight POST completes (drain semantics §11/§16.1) — assert no interruption machinery exists. LINEARIZATION: a worker that passed its pre-claim freeze read before `

**:231** `C/D` **REWRITE**
- OLD: `- **Purpose:** §16.1: an outage becomes quiet waiting; a 6-hour engine outage must not flood the ops queue at recovery. SIMPLIFIED by the 2026-07-11 retry-bounds decision (§7.4; cutoff retired round 1`
- NEW: (to be decided — matched: round 10, 2026-07-11)

**:238** `B` **EXPLICIT**
- OLD: `- **Implementation instructions:** per-dependency breakers (enrichment, account service, engine POST, status-query API) with explicit timeout budgets (config §16.6); business rejects recorded as succe`
- NEW: `- **Implementation instructions:** per-dependency breakers (enrichment, account service, engine POST, status-query API) with explicit timeout budgets (config §16.6); business rejects recorded as succe`

**:256** `A` **EXPLICIT**
- OLD: `- **Blockers to carry forward:** PRODUCTION ENABLEMENT of the §9.2 auto-downgrade stays gated on P8 PASS + TL-5-derived trust age (rollout stage F4); TL-13 rate limit for the real sweep budget (§18-2 `
- NEW: `- **Blockers to carry forward:** PRODUCTION ENABLEMENT of the §9.2 auto-downgrade stays gated on P8 PASS + TL-5-derived trust age (rollout stage F4); TL-13 rate limit for the real sweep budget.`


## portable-implementation-playbook/08-task-cards/phase-11-operator-verified-outcome.md

**:1** `B` **AUTO_OK**
- OLD: `> **Purpose:** Task cards OP-01..OP-03 + OP-04a..OP-04e (apply-platform-verified-outcome audited operation — §18-3 — plus the §20 interim ops surface, pre-split round 9) (original Section H, phase P11`
- NEW: `> **Purpose:** Task cards OP-01..OP-03 + OP-04a..OP-04e (apply-platform-verified-outcome audited operation — §18-3 — plus the §20 interim ops surface, pre-split) (original Section H, phase P11).`

**:13** `D` **KEEP**
- OLD: `- **Title:** Implement CA-9's audited verified-outcome OPERATION — an authorized application endpoint calling the shared transition service (dual control, evidence flag, refusal conditions, audit + al`
- KEEP unchanged

**:20** `D` **KEEP**
- OLD: `- **Local code areas to discover:** how the ENTERPRISE ACCESS-MANAGEMENT identities reach the operation (mechanism DECIDED 2026-07-11, §9.3: each operator has a unique, non-bypassable identity supplie`
- KEEP unchanged

**:22** `A` **AUTO_OK**
- OLD: `- **Implementation instructions:** per CA-9: an AUTHORIZED ENDPOINT (enterprise-authenticated, restricted ops role) whose service-layer implementation calls the SHARED transition helpers (RG-02/RG-03/`
- NEW: `- **Implementation instructions:** per CA-9: an AUTHORIZED ENDPOINT (enterprise-authenticated, restricted ops role) whose service-layer implementation calls the SHARED transition helpers (RG-02/RG-03/`

**:46** `D` **KEEP**
- OLD: `- **Implementation instructions:** tests: EXECUTED on a seeded MAYBE row → outcome, SUBMITTED, +confirmed, normalization, alert, log line with ticket; REJECTED → outcome, marker, −committed; refusal: `
- KEEP unchanged

**:70** `A` **AUTO_OK**
- OLD: `- **Implementation instructions:** seed an unresolvable MAYBE row (divergent_payload_at set — repost_permitted permanently false; round 10: no cutoff exists); operators verify the "platform truth" per`
- NEW: `- **Implementation instructions:** seed an unresolvable MAYBE row; operators verify the "platform truth" per the drill script's staged evidence; execute the operation via its authorized endpoint with `

**:82** `C` **REWRITE**
- OLD: `> **Round-9 note:** the former single OP-04 card was PRE-SPLIT into OP-04a..OP-04e — the split itself contains architectural judgment a small-context agent must not make. Execute strictly in order; ea`
- NEW: (to be decided — matched: Round-9)

**:113** `B` **AUTO_OK**
- OLD: `- **Purpose:** §9.3 rounds 4–7: the approvers authorize CONTENT, not an opaque id — the approval must bind what they actually reviewed.`
- NEW: `- **Purpose:** §9.3 –7: the approvers authorize CONTENT, not an opaque id — the approval must bind what they actually reviewed.`

**:120** `B` **AUTO_OK**
- OLD: `- **Implementation instructions:** the reprocess-snapshot INITIATION endpoint (input: xmlStorageId + reason + ticketRef) FETCHES the snapshot from the store by id, VALIDATES it (schema, business_id), `
- NEW: `- **Implementation instructions:** the reprocess-snapshot INITIATION endpoint (input: xmlStorageId + reason + ticketRef) FETCHES the snapshot from the store by id, VALIDATES it (schema, business_id), `

**:126** `A` **DECIDE** — normative token(s) in deleted text: H-1
- OLD: `- **Failure signs:** an approval created without a fetch (id-only attestation — the round-4 H-1 defect class).`
- NEW: (to be decided — matched: round-4)

**:144** `A` **AUTO_OK**
- OLD: `- **Implementation instructions:** execution takes the approval_id ONLY: re-fetch the snapshot, recompute the canonical digest, HARD-REFUSE on mismatch (+ alert) BEFORE any consumption or lock (refusa`
- NEW: `- **Implementation instructions:** execution takes the approval_id ONLY: re-fetch the snapshot, recompute the canonical digest, HARD-REFUSE on mismatch (alert) BEFORE any consumption or lock (refusal `

**:151** `A` **AUTO_OK**
- OLD: `- **Common mistakes:** accepting an ordering parameter (server derives it — round 3); skipping the pre-consumption digest check; forgetting the completion stamp's own transaction.`
- NEW: `- **Common mistakes:** accepting an ordering parameter; skipping the pre-consumption digest check; forgetting the completion stamp's own transaction.`

**:168** `A` **AUTO_OK**
- OLD: `- **Implementation instructions:** four READ-ONLY views: BLOCKED by reason (ESCALATED ranked first), stuck reservations by age, aged MAYBE by maybe_since, overpay latches (round 10: no cutoff-proximit`
- NEW: `- **Implementation instructions:** four READ-ONLY views: BLOCKED by reason (ESCALATED ranked first), stuck reservations by age, aged MAYBE by maybe_since, overpay latches; §15 alert definitions link t`

**:208** `A` **DECIDE** — normative token(s) in deleted text: OP-04a, OP-04e
- OLD: `- **Phase outputs:** the CA-9 audited operation implemented as an authorized application endpoint (dual control via enterprise-authenticated identities, evidence flag, refusals, audit + every-use aler`
- NEW: (to be decided — matched: round 9)


## portable-implementation-playbook/08-task-cards/phase-12-drift-reconciliation.md

**:22** `B` **AUTO_OK**
- OLD: `- **Implementation instructions:** per obligation (batched): snapshot-read (SCN/flashback) committed/confirmed vs Σ per I1/I2; mismatch → re-check UNDER the obligation lock; still mismatched → PAGE (n`
- NEW: `- **Implementation instructions:** per obligation (batched): snapshot-read (SCN/flashback) committed/confirmed vs Σ per I1/I2; mismatch → re-check UNDER the obligation lock; still mismatched → PAGE (n`

**:30** `A` **AUTO_OK**
- OLD: `- **Completion criteria:** drift/L9 tests green; page route confirmed; the diagnostic sub-case green OR recorded as the explicit open item (4098532 M1 — its failure never blocks this card).`
- NEW: `- **Completion criteria:** drift/L9 tests green; page route confirmed; the diagnostic sub-case green OR recorded as the explicit open item.`

**:46** `A` **AUTO_OK**
- OLD: `- **Implementation instructions:** evidence-for-terminal: NEW event_id + zero-row CAS against a TERMINAL row → CRITICAL (already hooked in IN-07 — verify + alert-route here); per-obligation request co`
- NEW: `- **Implementation instructions:** evidence-for-terminal: NEW event_id + zero-row CAS against a TERMINAL row → CRITICAL (already hooked in IN-07 — verify + alert-route here); per-obligation request co`

**:48** `A` **AUTO_OK**
- OLD: `- **Tests to add:** each tripwire fires on its seeded condition; benign redelivery does NOT fire; post-F0 NULL-stamp: a seeded post-F0 NULL row raises the data-quality ticket, a pre-F0 NULL row stays `
- NEW: `- **Tests to add:** each tripwire fires on its seeded condition; benign redelivery does NOT fire; post-F0 NULL-stamp: a seeded post-F0 NULL row raises the data-quality ticket, a pre-F0 NULL row stays `

**:63** `A` **DECIDE** — normative token(s) in deleted text: OB-01
- OLD: `- **Phase outputs:** I1/I2 drift scan (snapshot + locked re-check, PAGE on confirmed mismatch, L9 verification); terminal-evidence CRITICAL tripwire routed; per-obligation count sanity; the §6.6 accep`
- NEW: (to be decided — matched: reviews b1d91dc M1)

**:64** `A` **AUTO_OK**
- OLD: `- **Blockers to carry forward:** engine-side count comparison remains CONDITIONAL on a provider report/API (see 14-observability-reconciliation-runbooks.md N.1 note); IF the accepted-window diagnostic`
- NEW: `- **Blockers to carry forward:** engine-side count comparison remains CONDITIONAL on a provider report/API (see 14-observability-reconciliation-runbooks.md N.1 note); IF the accepted-window diagnostic`


## portable-implementation-playbook/08-task-cards/phase-13-observability-and-runbooks.md

**:22** `A` **AUTO_OK**
- OLD: `- **Implementation instructions:** implement, each on its named clock/scope: oldest-MAYBE age (maybe_since) alert on the age threshold (round 10 — no cutoff exists); payment-DISAPPEARANCE metric + man`
- NEW: `- **Implementation instructions:** implement, each on its named clock/scope: oldest-MAYBE age (maybe_since) alert on the age threshold; payment-DISAPPEARANCE metric + mandatory log line; MAYBE tier-2 `

**:46** `D` **KEEP**
- OLD: `- **Implementation instructions:** unmatched feed events (volume alert); stale upstream messages volume; stale-marker-writes volume; Kafka DLT depth > 0 → page; consumer lag per flow → page over SLA +`
- KEEP unchanged

**:111** `C` **REWRITE**
- OLD: `- **Purpose:** §16.6: nothing else orders trust_age/cadence/escalation/tier-2 (cutoff margin RETIRED round 10); a p99-driven trust-age quietly reaching the escalation age silently degrades wait-then-d`
- NEW: (to be decided — matched: round 10)

**:118** `C` **REWRITE**
- OLD: `- **Implementation instructions:** one config namespace holding every §16.6 entry (trust age, confirmation age, escalation ages, downgrade class, cadences, lease durations, retry policies, thresholds,`
- NEW: (to be decided — matched: round 10)


## portable-implementation-playbook/08-task-cards/phase-14-rollout-and-go-live.md

**:22** `A` **AUTO_OK**
- OLD: `- **Implementation instructions:** write the local rollout plan following Section M's stage order verbatim, with per-stage: owner, checkpoint evidence, rollback trigger + procedure; wire the Section M`
- NEW: `- **Implementation instructions:** write the local rollout plan following Section M's stage order verbatim, with per-stage: owner, checkpoint evidence, rollback trigger + procedure; wire the Section M`

**:39** `B` **AUTO_OK**
- OLD: `- **Purpose:** Section M's dry-run stage: prove the factored model tracks reality before any rule ENFORCEMENT relies on it in production. Round 12: the comparison table maps CANCELLED EXPLICITLY — leg`
- NEW: `- **Purpose:** Section M's dry-run stage: prove the factored model tracks reality before any rule ENFORCEMENT relies on it in production. the comparison table maps CANCELLED EXPLICITLY — legacy displa`

**:46** `A` **AUTO_OK**
- OLD: `- **Implementation instructions:** a comparison job/report over a soak window: per row, tuple-derived label vs legacy status per the reviewed mapping; disagreements fall in TWO CLASSES (round 13): (1)`
- NEW: `- **Implementation instructions:** a comparison job/report over a soak window: per row, tuple-derived label vs legacy status per the reviewed mapping; disagreements fall in TWO CLASSES: (1) EXPECTED S`

**:48** `A` **AUTO_OK**
- OLD: `- **Tests to add:** the comparison tooling's own correctness (seeded disagreement detected); cutover fence assertion: a FENCED old-writer version attempting to reconnect is REJECTED (round 15).`
- NEW: `- **Tests to add:** the comparison tooling's own correctness (seeded disagreement detected); cutover fence assertion: a FENCED old-writer version attempting to reconnect is REJECTED.`

**:50** `A` **AUTO_OK**
- OLD: `- **Manual validation:** soak report clean over the agreed window (owner-defined; record; clean = zero UNEXPLAINED disagreements, expected CANCELLED deltas classified — round 13); evidence: NO obligat`
- NEW: `- **Manual validation:** soak report clean over the agreed window; evidence: NO obligation row exposed to the card path carries NULL ui_step_status — the M.3 FENCED cutover ran before the read switch.`

**:56** `B` **AUTO_OK**
- OLD: `- **Next task:** GO-05 (round-19 order: rehearsal → pre-cutover authorization → controlled cutover).`
- NEW: `- **Next task:** GO-05 (order: rehearsal → pre-cutover authorization → controlled cutover).`

**:64** `A/B` **AUTO_OK**
- OLD: `- **Prerequisites:** GO-04 CONDITIONAL GO recorded (round 19 — authorization precedes enablement); per-stage prerequisites in Section M; P8 gate status for the final stage. Round 18/20: enabling the N`
- NEW: `- **Prerequisites:** GO-04 CONDITIONAL GO recorded; per-stage prerequisites in Section M; P8 gate status for the final stage. enabling the NEW intake traffic (the M.2 F0 gate) requires the CUTOVER_POP`

**:70** `A/B` **DECIDE** — normative token(s) in deleted text: FAILED_INCIDENT_OPEN, NEVER
- OLD: `- **Implementation instructions:** execute M.4's THREE SEGMENTS IN ORDER (round 20 — F0 NEVER opens merely because RUN 2 is zero; every pre-traffic safeguard must already be verified): (1) PRE-TRAFFIC`
- NEW: (to be decided — matched: follow-up L1 on 0bcb536, follow-up L1 on 8bf0aba, reviews 0e09f09 M1, 4dbdf2b M1)

**:71** `A` **AUTO_OK**
- OLD: `- **Do not change:** stage order; a soak or checkpoint feeding a NON-WAIVABLE Q item can NEVER be waived (round 19); any other soak waiver needs its owner recorded.`
- NEW: `- **Do not change:** stage order; a soak or checkpoint feeding a NON-WAIVABLE Q item can NEVER be waived; any other soak waiver needs its owner recorded.`

**:80** `A` **DECIDE** — normative token(s) in deleted text: GO-03
- OLD: `- **Next task:** none (steady state — round-19 order: GO-03 is the LAST card).`
- NEW: (to be decided — matched: round-19)

**:85** `A` **AUTO_OK**
- OLD: `- **Title:** Execute the Section Q checklist; assemble gate evidence; obtain the PRE-CUTOVER go/no-go (round 19 — authorization precedes enablement)`
- NEW: `- **Title:** Execute the Section Q checklist; assemble gate evidence; obtain the PRE-CUTOVER go/no-go`

**:87** `B` **AUTO_OK**
- OLD: `- **Purpose:** the four §18 BLOCKING items + all Q items PASS BEFORE the controlled cutover (GO-03) — round 19: the recorded decision is a CONDITIONAL GO authorizing GO-03's change window, contingent `
- NEW: `- **Purpose:** the four §18 BLOCKING items + all Q items PASS BEFORE the controlled cutover (GO-03) —: the recorded decision is a CONDITIONAL GO authorizing GO-03's change window, contingent ONLY on t`

**:88** `A` **DECIDE** — normative token(s) in deleted text: Q23
- OLD: `- **Prerequisites:** GO-02 clean; GO-05 rollback rehearsal recorded (round 19 — Q23's evidence exists BEFORE this meeting); OP-03 drill; CT suite results; K-03 vectors; open-question register (Section`
- NEW: (to be decided — matched: round 19)

**:94** `A` **DECIDE** — normative token(s) in deleted text: Q11, Q12, Q14, Q16, Q17, Q27, Q29, Q5a, Q5b, Q8, Q9
- OLD: `- **Implementation instructions:** walk Section Q item by item: PASS with linked evidence / FAIL with owner + plan / BLOCKED with the blocking §18 item; TWO non-waivable classes (round 16): §18 BLOCKI`
- NEW: (to be decided — matched: round 16, round 20, round 16, round 18)

**:104** `A` **AUTO_OK**
- OLD: `- **Next task:** GO-03 (the controlled cutover — round 19: the LAST card).`
- NEW: `- **Next task:** GO-03.`

**:127** `A` **DECIDE** — normative token(s) in deleted text: GO-04, Q23
- OLD: `- **Stop condition:** rehearsals recorded (round 19: this card runs BEFORE the GO-04 authorization — it is Q23's evidence and a GO-04 prerequisite).`
- NEW: (to be decided — matched: round 19)

**:135** `B` **AUTO_OK**
- OLD: `- **Phase outputs:** (round-19 execution order: GO-01 plan → GO-02 shadow → GO-05 rollback rehearsal → GO-04 PRE-CUTOVER conditional go/no-go → GO-03 controlled cutover + staged enablement + post-enab`
- NEW: `- **Phase outputs:** (execution order: GO-01 plan → GO-02 shadow → GO-05 rollback rehearsal → GO-04 PRE-CUTOVER conditional go/no-go → GO-03 controlled cutover + staged enablement + post-enable verifi`

**:136** `A` **DECIDE** — normative token(s) in deleted text: GO-03, GO-04, Q11, Q12, Q14, Q16, Q17, Q27, Q29, Q5a, Q5b, Q8, Q9
- OLD: `- **Blockers to carry forward:** none permitted at go-live — §18 items 0–3 (Q1–Q4, Q28) AND the MONEY_SAFETY_BLOCKING class (Q5a+Q5b/Q8/Q9/Q11/Q12/Q14/Q16/Q17/Q27/Q29-minimal — rounds 16/20; Q5b's PEN`
- NEW: (to be decided — matched: rounds 16/20)


## portable-implementation-playbook/09-minimal-context-packets/phase-01-discovery.md

**:100** `A` **AUTO_OK**
- OLD: `Read: playbook Section F status codes; Playbook Index BLOCKED list; file 26 T.1–T.3; requirment-v4.md §2.4 (round 18 — the canonical greenfield fact itself, incl. the bootstrap-restoration condition; `
- NEW: `Read: playbook Section F status codes; Playbook Index BLOCKED list; file 26 T.1–T.3; requirment-v4.md §2.4. Invariant: no "probably" — IMPLEMENTED/PARTIAL/MISSING/UNCLEAR only; DIV-3/DIV-4 register ro`


## portable-implementation-playbook/09-minimal-context-packets/phase-02-blocking-gates-and-artifacts.md

**:14** `A/D` **DECIDE** — normative token(s) in deleted text: PO-9, TL-16
- OLD: `Objective (residue): FILE the written confirmations for asks 5 + 8 (both confirmed verbally 2026-07-11 — the paper is the Q1 evidence); §6.0 intake uniqueness validation in IN-02; TL-2 gains the step-`
- NEW: (to be decided — matched: answered round 5, 2026-07-11)

**:27** `A` **AUTO_OK**
- OLD: `[B-03] §18-2 closure record (round 10 — engine owns the calendar)`
- NEW: `[B-03] §18-2 closure record`

**:30** `D` **KEEP**
- OLD: `Objective: record the PO's 2026-07-11 closure; file the engine's WRITTEN any-time-submission line + late-submission response code (if any) into the CA-1 table (Q-08).`
- KEEP unchanged

**:31** `A` **AUTO_OK**
- OLD: `Tests: none. Stop: the §18-2 CLOSED fact + the CA-1 late-submission ask recorded (round 10 — no calendar attributes exist to source).`
- NEW: `Tests: none. Stop: the §18-2 CLOSED fact + the CA-1 late-submission ask recorded.`

**:70** `A` **AUTO_OK**
- OLD: `Objective: spec all columns, scope-key UNIQUE, UNIQUE(idempotency_key), NULL-ignoring UNIQUE(uetr), the EXACT conditional NULL-ignoring UNIQUE over (payment_obligation_id, request_seq), the integer do`
- NEW: `Objective: spec all columns, scope-key UNIQUE, UNIQUE(idempotency_key), NULL-ignoring UNIQUE(uetr), the EXACT conditional NULL-ignoring UNIQUE over (payment_obligation_id, request_seq), the integer do`

**:78** `A` **AUTO_OK**
- OLD: `Objective: spec inputs (scope|seq — no discriminator, §1 contract facts), canonicalization, delimiter/encoding, algorithm, version; the INITIAL sequence value + counter-initialization policy for pre-e`
- NEW: `Objective: spec inputs (scope|seq — no discriminator, §1 contract facts), canonicalization, delimiter/encoding, algorithm, version; the INITIAL sequence value + counter-initialization policy for pre-e`

**:86** `A` **AUTO_OK**
- OLD: `Objective: define hashed field set, canonical order, canonicalization, algorithm, version. Persistence rule (QUALIFIED, review 928341a M1): content never stored in payment tables/logs/traces — the ONL`
- NEW: `Objective: define hashed field set, canonical order, canonicalization, algorithm, version. Persistence rule: content never stored in payment tables/logs/traces — the ONLY permitted persistence is the `

**:108** `A` **AUTO_OK**
- OLD: `Read: §9.3 (operation + approval workflow) §10.1 §10.3 §20-8 §16.6 artifact 8 §18-3. Invariant: execution input = approval_id (identities derived from the record — round 4); the two-step workflow IS t`
- NEW: `Read: §9.3 (operation + approval workflow) §10.1 §10.3 §20-8 §16.6 artifact 8 §18-3. Invariant: execution input = approval_id; the two-step workflow IS the MVP protocol (signed assertion = gated alter`

**:110** `A` **AUTO_OK**
- OLD: `Objective: spec EXECUTION signature = approval_id ONLY (round 4 — the approval record carries the authenticated initiator/approver identities, action binding incl. request_id + EXECUTED|REJECTED + tic`
- NEW: `Objective: spec EXECUTION signature = approval_id ONLY, consumption semantics per operation class (§9.3), evidence-flag mechanics, refusals (CLAIMED/terminal/amount mismatch), money effects, audit fie`

**:117** `A` **AUTO_OK**
- OLD: `Placeholders: none (DBA ops/audit schema). Mappings/inputs REQUIRED before AUD-01 can resolve (review 4d5cb83 M1): <request_id_type> (D-02), CA-1 category tokens (CA-1 published), audit schema/tablesp`
- NEW: `Placeholders: none (DBA ops/audit schema). Mappings/inputs REQUIRED before AUD-01 can resolve: <request_id_type> (D-02), CA-1 category tokens (CA-1 published), audit schema/tablespaces/roles, environm`

**:118** `R` **DECIDE**
- OLD: `Objective: author the spec from §14.1 + the file 12 DDL TEMPLATE (2026-07-17 simplified design; AUD-01 resolves it to zero-placeholder SQL): typed columns; FULL payload_content on EVERY STARTED (simpl`
- NEW: (to be decided — matched: 2026-07-17)


## portable-implementation-playbook/09-minimal-context-packets/phase-03-schema-and-migration.md

**:1** `C` **REWRITE**
- OLD: `> **Purpose:** Minimal context packets S-01..S-10 + AUD-01 (§14.1 journal schema) — paste-alone briefs for a small-context local agent (S-11 RETIRED round 10) (original Section I, phase P3).`
- NEW: (to be decided — matched: round 10)

**:14** `A` **DECIDE** — normative token(s) in deleted text: NOVALIDATE, ORA-01452, UNIQUE
- OLD: `Objective: ordered migration list (one concern each; ORDER CORRECTED 289ef66 M1 — I6 is a UNIQUE index, NOVALIDATE does not apply, pre-backfill legacy NULL-outcome rows all count ACTIVE → ORA-01452): `
- NEW: (to be decided — matched: ORDER CORRECTED 289ef66 M1)

**:22** `A` **DECIDE** — normative token(s) in deleted text: CA-5, K-01, Oracle
- OLD: `Objective: add §2.1 columns (amounts, markers+counters+first_at, ordering fields, read-model fields, reopened_at, next_request_seq); SCOPE-KEY PREFLIGHT owned HERE (the S-01 sanctioned split): duplica`
- NEW: (to be decided — matched: 4dbdf2b M1, 6cb3005 M1)

**:30** `A` **EXPLICIT**
- OLD: `Objective: add the four dimensions + blocked_reason + request_seq (§2.2 immutable per-request sequence — write-once at creation, NULL on legacy rows, 1d8a650 M1) + identity (idempotency_key/end_to_end`
- NEW: `Objective: add the four dimensions + blocked_reason + request_seq + identity (idempotency_key/end_to_end_id)/uetr/provider_reference + version/claim/retry/next_query_at + created_at/state_changed_at/c`

**:43** `A` **EXPLICIT**
- OLD: `[S-10] trade_snapshot_state (admission row — round 5)`
- NEW: `[S-10] trade_snapshot_state`

**:52** `A` **DECIDE** — normative token(s) in deleted text: D-04, NOVALIDATE, ORA-01452, S-08, STOP, UNIQUE
- OLD: `Read: §10.3 (matrix) §2.2 constraints §2.1 (ui_step_status stored set) CA-4. Invariant: DB is the backstop; L9 is NOT a CHECK (drift-scanner verified); the ui_step_status CHECK (IN_PROGRESS/COMPLETED/`
- NEW: (to be decided — matched: 289ef66 M1, round 13)

**:54** `A` **EXPLICIT**
- OLD: `Objective: enum CHECKs; L2–L8 + L1-shape CHECKs; UNIQUE(idempotency_key); NULL-ignoring UNIQUE(uetr); NULL-ignoring fn-based UNIQUE (payment_obligation_id, request_seq) — plain composite would reject `
- NEW: `Objective: enum CHECKs; L2–L8 + L1-shape CHECKs; UNIQUE(idempotency_key); NULL-ignoring UNIQUE(uetr); NULL-ignoring fn-based UNIQUE (payment_obligation_id, request_seq) — plain composite would reject `

**:55** `A` **EXPLICIT**
- OLD: `Tests: one violation test per constraint; I6 second-active rejected; stamp < amount refused; request_seq index ISOLATION set (7cc9f49 M1 — neutralize EVERY competitor mechanically: distinct idempotenc`
- NEW: `Tests: one violation test per constraint; I6 second-active rejected; stamp < amount refused; request_seq index ISOLATION set: same oblig + same seq (two terminal rows) → ORA-00001 naming the FROZEN in`

**:78** `A` **DECIDE** — normative token(s) in deleted text: S-02
- OLD: `Objective: STEP 0 — EXECUTE the S-01-planned immutable preflight BEFORE any data mutation (S-01 only plans it; S-08 owns execution): run the frozen duplicate queries (idempotency-key, uetr, request_se`
- NEW: (to be decided — matched: 1d8a650 M1, 4dbdf2b M1, round 14)

**:84** `A` **EXPLICIT**
- OLD: `Read: §16.5; Section M.1a decision record. Invariant: the OLD app version must run against the NEW schema; the CANCELLED-read proof is CONDITIONAL on M.1a (round 15) — not-read → N/A with proof; defen`
- NEW: `Read: §16.5; Section M.1a decision record. Invariant: the OLD app version must run against the NEW schema; the CANCELLED-read proof is CONDITIONAL on M.1a — not-read → N/A with proof; defensive reader`

**:86** `A` **EXPLICIT**
- OLD: `Objective: prove: clean-schema apply; prod-shaped apply + backfill; old-version boot+smoke on new schema (old writers create rows with NULL required_total_at_creation AND NULL request_seq — EXPECTED d`
- NEW: `Objective: prove: clean-schema apply; prod-shaped apply + backfill; old-version boot+smoke on new schema; constraint suite in CI; evidence: ZERO obligations with NULL next_request_seq; request_seq col`

**:93** `A` **EXPLICIT**
- OLD: `Read: §14.1 (all) §2.2 (post_attempt_seq) §16.3; file 12 CA-10; file 24 M9. Invariant: ops/audit schema OUTSIDE the §2 model; INSERT-only (no UPDATE/DELETE grants to application or reporting roles; ow`
- NEW: `Read: §14.1 (all) §2.2 (post_attempt_seq) §16.3; file 12 CA-10; file 24 M9. Invariant: ops/audit schema OUTSIDE the §2 model; INSERT-only; SELECT = restricted audit role only, reads DB-audited; never `

**:95** `B` **EXPLICIT**
- OLD: `Objective: RESOLVE CA-10's DDL TEMPLATE (zero angle-bracket tokens; substitution manifest fact→value→source; preflight rejects leftovers; audit-policy block = DBA-executed with AUDIT_ADMIN — review 4d`
- NEW: `Objective: RESOLVE CA-10's DDL TEMPLATE (zero angle-bracket tokens; substitution manifest fact→value→source; preflight rejects leftovers; audit-policy block = DBA-executed with AUDIT_ADMIN) then run t`

**:96** `A` **DECIDE** — normative token(s) in deleted text: NEVER
- OLD: `Tests (NEVER weaker than the card — review 2b697fb M3): PREFLIGHT zero angle-bracket tokens + substitution manifest validates (fact→value→source); T-38 schema slice (INSERT-only for app/reporting role`
- NEW: (to be decided — matched: review 2b697fb M3)


## portable-implementation-playbook/09-minimal-context-packets/phase-04-identity-and-idempotency.md

**:14** `A` **AUTO_OK**
- OLD: `Objective: lock → read seq → increment → derive → insert WITH the consumed value persisted in payment_request.request_seq (§2.2, write-once — the column, not the obligation counter, is the source of t`
- NEW: `Objective: lock → read seq → increment → derive → insert WITH the consumed value persisted in payment_request.request_seq (§2.2, write-once — the column, not the obligation counter, is the source of t`

**:38** `A` **AUTO_OK**
- OLD: `Objective: claim transaction persists identity (first claim), COMMITS, then the HTTP call; commit-unknown → abandon, lease expiry owns it. §14.1 rider in the SAME transaction: post_attempt_seq++ (mono`
- NEW: `Objective: claim transaction persists identity (first claim), COMMITS, then the HTTP call; commit-unknown → abandon, lease expiry owns it. §14.1 rider in the SAME transaction: post_attempt_seq++ (mono`

**:54** `A` **AUTO_OK**
- OLD: `Objective: tests: crash-before-POST (claim committed) → re-POST ONLY via lease expiry → MAYBE → resolver → §9.2 downgrade (NO direct posting re-claim — 2a19c20 M1), eventual retry reuses key; crash-af`
- NEW: `Objective: tests: crash-before-POST (claim committed) → re-POST ONLY via lease expiry → MAYBE → resolver → §9.2 downgrade, eventual retry reuses key; crash-after-POST → MAYBE, no fresh key; restore si`


## portable-implementation-playbook/09-minimal-context-packets/phase-05-uetr-response-persistence.md

**:20** `D` **KEEP**
- OLD: `Read: §2.2 (provider_reference) §8 (fallback + index decision 2026-07-11) §5. Invariant: distinct field from uetr; NON-UNIQUE lookup index until TL-12 confirms scope in writing (a UNIQUE index would r`
- KEEP unchanged


## portable-implementation-playbook/09-minimal-context-packets/phase-06-factored-state-model.md

**:36** `A` **DECIDE** — normative token(s) in deleted text: S-03
- OLD: `Read: §10.4 (mapping + strictness) §2.2 §12 (ALL-PAYMENTS TABLE projection block — the FULL contract: authorization, scope modes incl. the CA-4 estate-query resolution, filter shapes, keyset paginatio`
- NEW: (to be decided — matched: 1d8a650 L3)

**:38** `A` **EXPLICIT**
- OLD: `Objective: implement the §10.4 mapping (view/expression); route dashboards/card label/log/ops reads to it; card returns dimensions + label. Implement the §12 table projection per its contract: row_typ`
- NEW: `Objective: implement the §10.4 mapping (view/expression); route dashboards/card label/log/ops reads to it; card returns dimensions + label. Implement the §12 table projection per its contract: row_typ`

**:86** `A` **DECIDE** — normative token(s) in deleted text: LEASE_EXPIRED
- OLD: `Objective: expiry handling: ENRICH → READY in place; POST → CONFIRM·READY·MAYBE (+maybe_since), claim fields cleared. §14.1 rider: POST-expiry CAS inserts ATTEMPT_RESOLVED outcome LEASE_EXPIRED_MAYBE,`
- NEW: (to be decided — matched: b760786 M2)


## portable-implementation-playbook/09-minimal-context-packets/phase-07-reservation-and-release-guards.md

**:54** `A` **EXPLICIT**
- OLD: `Objective: evaluate() under lock per §6.8's condition list; invoke from T1–T4; route every legacy site through it. STAMP INVARIANTS (§2.2, 0e09f09): one stamp per payment_request row, NOT per provider`
- NEW: `Objective: evaluate under lock per §6.8's condition list; invoke from T1–T4; route every legacy site through it. STAMP INVARIANTS: one stamp per payment_request row, NOT per provider POST attempt; wri`

**:68** `A/B` **EXPLICIT**
- OLD: `Read: §4.1 (predicate + BOTH branches + bullets) §4 §2.1 (liveness incl. anchor clause) §12. Invariant: completion derived only; anchors can't complete; active request blocks completion; feed never wr`
- NEW: `Read: §4.1 (predicate + BOTH branches + bullets) §4 §2.1 (liveness incl. anchor clause) §12. Invariant: completion derived only; anchors can't complete; active request blocks completion; feed never wr`

**:70** `B` **EXPLICIT**
- OLD: `Objective: implement BOTH branches exactly (COMPLETED incl. required NOT NULL ∧ >0 and confirmed>=required terms; CANCELLED per round 11); output IN_PROGRESS/COMPLETED/CANCELLED; wire into every re-de`
- NEW: `Objective: implement BOTH branches exactly (COMPLETED incl. required NOT NULL ∧ >0 and confirmed>=required terms; CANCELLED per); output IN_PROGRESS/COMPLETED/CANCELLED; wire into every re-derivation;`

**:71** `A` **EXPLICIT**
- OLD: `Tests: each term isolated; recovered anchor completes; zeroed row → CANCELLED never COMPLETED; zeroed + live provider_rejected (count 2) → still CANCELLED (round 13); zeroed with confirmed>0 → IN_PROG`
- NEW: `Tests: each term isolated; recovered anchor completes; zeroed row → CANCELLED never COMPLETED; zeroed + live provider_rejected (count 2) → still CANCELLED; zeroed with confirmed>0 → IN_PROGRESS + latc`

**:76** `A` **EXPLICIT**
- OLD: `Read: §4.2 (ranks + round-12 suppression) §4.3 §4.5 §13. Invariant: derived, never accumulated; rank-1 (MAYBE, OVERPAY) never masked; actor never stored; active requests only; required = 0 suppresses `
- NEW: `Read: §4.2 §4.3 §4.5 §13. Invariant: derived, never accumulated; rank-1 (MAYBE, OVERPAY) never masked; actor never stored; active requests only; required = 0 suppresses ONLY historical PROVIDER_REJECT`

**:84** `A` **EXPLICIT**
- OLD: `Read: §6.5 (both terminal states) §6.3 §2.1 (reopened_at). Invariant: reopening = standing re-evaluation from COMPLETED or CANCELLED alike (round 12); ALL §6.8 gates apply on reappearance (live provid`
- NEW: `Read: §6.5 (both terminal states) §6.3 §2.1 (reopened_at). Invariant: reopening = standing re-evaluation from COMPLETED or CANCELLED alike; ALL §6.8 gates apply on reappearance (live provider_rejected`


## portable-implementation-playbook/09-minimal-context-packets/phase-08-provider-contract-tests.md

**:38** `B` **EXPLICIT**
- OLD: `Objective: re-run (a)/(b) at the achievable retention edge; compare TTL vs max lifetime incl. ops SLA; emit the TYPED consequence record (289ef66 M2; four-state model + transitions = spec §18-1 — init`
- NEW: `Objective: re-run (a)/(b) at the achievable retention edge; compare TTL vs max lifetime incl. ops SLA; emit the TYPED consequence record (four-state model + transitions = spec §18-1 — initial emission`

**:46** `B` **EXPLICIT**
- OLD: `Objective: induce reject; re-POST same key; record re-executes vs replays; emit the TYPED consequence record (289ef66 M2; four-state model = spec §18-1, as on CT-04); REPLAYS ⇒ IMPLEMENTATION_REQUIRED`
- NEW: `Objective: induce reject; re-POST same key; record re-executes vs replays; emit the TYPED consequence record (four-state model = spec §18-1, as on CT-04); REPLAYS ⇒ IMPLEMENTATION_REQUIRED reopens/blo`


## portable-implementation-playbook/09-minimal-context-packets/phase-09-inbound-flows-and-status-feed.md

**:20** `A` **AUTO_OK**
- OLD: `Read: §6.1 (ADMISSION first — round 5; block TRADE-SNAPSHOT FENCE — rounds 6–7) §2.4 §6.7 (whole) §6.9 (required row) §6.0. Invariant: NO per-block work before the trade-level admission transaction (u`
- NEW: `Read: §6.1 §2.4 §6.7 (whole) §6.9 (required row) §6.0. Invariant: NO per-block work before the trade-level admission transaction (upsert-lock trade_snapshot_state; newer → admit+update; equal+digest-e`


## portable-implementation-playbook/09-minimal-context-packets/phase-10-retry-recovery-maybe.md

**:22** `A` **EXPLICIT**
- OLD: `Objective: implement each §7.2 row as its exact tuple CAS + side effects (anchors, markers, release on definitive rejects). §14.1 rider: ATTEMPT_RESOLVED insert (outcome = §7.2 class verbatim) in the `
- NEW: `Objective: implement each §7.2 row as its exact tuple CAS + side effects (anchors, markers, release on definitive rejects). §14.1 rider: ATTEMPT_RESOLVED insert (outcome = §7.2 class verbatim) in the `

**:30** `A` **EXPLICIT**
- OLD: `Objective: divergent_payload_at IS NULL ∧ ¬(stale ∧ MAYBE) ∧ freeze OFF ∧ outcome IS NULL (round 10: NO cutoff term); wire both ends.`
- NEW: `Objective: divergent_payload_at IS NULL ∧ ¬(stale ∧ MAYBE) ∧ freeze OFF ∧ outcome IS NULL; wire both ends.`

**:36** `A` **EXPLICIT**
- OLD: `Read: §7.4 (whole — bound = MAX ATTEMPTS, round 10; engine owns the calendar) §16.1 (scanner + clock semantics + poison) §16.6; mechanics M5. Invariant: the DB scanner is the ONLY retry owner on the P`
- NEW: `Read: §7.4 §16.1 (scanner + clock semantics + poison) §16.6; mechanics M5. Invariant: the DB scanner is the ONLY retry owner on the POST; §11 claim protocol (lock-free select → obligation-first claim `

**:46** `A` **EXPLICIT**
- OLD: `Objective: prioritized bounded sweep (oldest maybe_since first — round 10: no cutoff knowledge), per-row next_query_at backoff, budget from rate limit, overrun metric, SUBMITTED damping vs feed-lag, o`
- NEW: `Objective: prioritized bounded sweep, per-row next_query_at backoff, budget from rate limit, overrun metric, SUBMITTED damping vs feed-lag, ops-triggered explicit-key mode.`

**:84** `A` **EXPLICIT**
- OLD: `Read: §16.1 (breaker/clock semantics/bulkheads/timeouts) §7.4 (bound = max attempts, round 10) §16.6. Invariant: business rejects are breaker SUCCESSES; scanners gate on breaker; while OPEN/frozen sca`
- NEW: `Read: §16.1 (breaker/clock semantics/bulkheads/timeouts) §7.4 §16.6. Invariant: business rejects are breaker SUCCESSES; scanners gate on breaker; while OPEN/frozen scanners make ZERO attempts (structu`


## portable-implementation-playbook/09-minimal-context-packets/phase-11-operator-verified-outcome.md

**:12** `A/R` **DECIDE** — normative token(s) in deleted text: OP-04c
- OLD: `Read: §9.3 (operation + approval workflow) §10.1 §10.3 §20-8; CA-9; mechanics SHAPE-PROC. Invariant: Java endpoint calling the SHARED transition helpers (never PL/SQL — 2026-07-11 boundary); execution`
- NEW: (to be decided — matched: round 4, round 5, 2026-07-11)

**:35** `A` **AUTO_OK**
- OLD: `[OP-04a] Shared ops-endpoint contract + retry/reject/annotate (round-9 pre-split 1/5)`
- NEW: `[OP-04a] Shared ops-endpoint contract + retry/reject/annotate`

**:43** `A` **AUTO_OK**
- OLD: `[OP-04b] Reprocess APPROVAL side (round-9 pre-split 2/5)`
- NEW: `[OP-04b] Reprocess APPROVAL side`

**:51** `A` **AUTO_OK**
- OLD: `[OP-04c] Reprocess EXECUTION (round-9 pre-split 3/5)`
- NEW: `[OP-04c] Reprocess EXECUTION`

**:59** `A` **AUTO_OK**
- OLD: `[OP-04d] Queue views + authz/plan tests (round-9 pre-split 4/5)`
- NEW: `[OP-04d] Queue views + authz/plan tests`

**:62** `A` **AUTO_OK**
- OLD: `Objective: BLOCKED-by-reason (ESCALATED first) / stuck reservations / aged MAYBE / overpay latches (round 10: cutoff-proximity column + pointer-residue view both REMOVED); §15 alerts link to each.`
- NEW: `Objective: BLOCKED-by-reason (ESCALATED first) / stuck reservations / aged MAYBE / overpay latches; §15 alerts link to each.`

**:67** `A` **AUTO_OK**
- OLD: `[OP-04e] Cross-path integration suite + evidence (round-9 pre-split 5/5)`
- NEW: `[OP-04e] Cross-path integration suite + evidence`


## portable-implementation-playbook/09-minimal-context-packets/phase-12-drift-reconciliation.md

**:14** `B` **AUTO_OK**
- OLD: `Objective: recompute I1/I2 per obligation; re-check under lock; page; verify L9 totality. PLUS ship (do NOT schedule) the §6.6 accepted-window CANDIDATE diagnostic (reviews 2b697fb M1 + b1d91dc M1 + b`
- NEW: `Objective: recompute I1/I2 per obligation; re-check under lock; page; verify L9 totality. PLUS ship (do NOT schedule) the §6.6 accepted-window CANDIDATE diagnostic (REQUIRED deliverable, but its test `

**:22** `A/B` **AUTO_OK**
- OLD: `Objective: terminal-evidence CRITICAL routed; per-obligation count sanity ticket; post-F0 NULL-stamp data-quality scan (created_at >= F0 activation timestamp from the signed manifest AND required_tota`
- NEW: `Objective: terminal-evidence CRITICAL routed; per-obligation count sanity ticket; post-F0 NULL-stamp data-quality scan (created_at >= F0 activation timestamp from the signed manifest AND required_tota`


## portable-implementation-playbook/09-minimal-context-packets/phase-13-observability-and-runbooks.md

**:20** `R` **DECIDE**
- OLD: `Read: §15 (entries + stuck-state split) §12 (freshness) §16.2 (lag). Invariant: stuck-state split — retry states on next_retry_at OVERDUE (2026-07-11 re-key), non-churning on state_changed_at; metric `
- NEW: (to be decided — matched: 2026-07-11)

**:44** `C` **REWRITE**
- OLD: `Read: §16.6 (inventory + ordering rule) §16.5. Invariant: loader REJECTS unless trust_age + cadence < escalation < tier-2 (cutoff margin retired round 10); load-bearing values never silently default.`
- NEW: (to be decided — matched: round 10)


## portable-implementation-playbook/09-minimal-context-packets/phase-14-rollout-and-go-live.md

**:12** `A` **AUTO_OK**
- OLD: `Read: playbook Section M (incl. M.2 F0 + activation window); §16.5 §18. Invariant: Section M's stage ORDER is fixed; auto-downgrade last, gated on P8 PASS; flags default-off; the plan NAMES the F0 act`
- NEW: `Read: playbook Section M (incl. M.2 F0 + activation window); §16.5 §18. Invariant: Section M's stage ORDER is fixed; auto-downgrade last, gated on P8 PASS; flags default-off; the plan NAMES the F0 act`

**:14** `B` **AUTO_OK**
- OLD: `Objective: environment-specific plan: per stage owner, checkpoint evidence, rollback trigger/procedure; wire enablement flags incl. F0; round-19 card order: 01 → 02 → 05 → 04 → 03.`
- NEW: `Objective: environment-specific plan: per stage owner, checkpoint evidence, rollback trigger/procedure; wire enablement flags incl. F0; card order: 01 → 02 → 05 → 04 → 03.`

**:20** `A` **AUTO_OK**
- OLD: `Read: §10.4; playbook Section M (shadow stage). Invariant: disagreements fall in TWO classes (round 13) — EXPECTED CANCELLED semantic deltas (legacy has no such value: invariant-check + classify, neve`
- NEW: `Read: §10.4; playbook Section M (shadow stage). Invariant: disagreements fall in TWO classes — EXPECTED CANCELLED semantic deltas (legacy has no such value: invariant-check + classify, never "fix") vs`

**:22** `A` **AUTO_OK**
- OLD: `Objective: soak-window comparison job (tuple-derived label vs legacy; derived step status vs legacy); itemized two-class disagreement report; fix + re-soak UNEXPLAINED ones only (round 13).`
- NEW: `Objective: soak-window comparison job (tuple-derived label vs legacy; derived step status vs legacy); itemized two-class disagreement report; fix + re-soak UNEXPLAINED ones only.`

**:27** `A` **DECIDE** — normative token(s) in deleted text: GO-04
- OLD: `[GO-03] Controlled cutover + staged enablement (round 19: LAST card — runs only on GO-04's CONDITIONAL GO)`
- NEW: (to be decided — matched: round 19)

**:28** `A` **AUTO_OK**
- OLD: `Read: playbook Section M (M.4 THREE SEGMENTS + M.2 F0 window) §9.2 §18-1; file 26 T.1. Invariant: M.4's segment order is BINDING (round 20) — PRE-TRAFFIC with F0 OFF (F5 freeze in BLOCK mode + tested;`
- NEW: `Read: playbook Section M (M.4 THREE SEGMENTS + M.2 F0 window) §9.2 §18-1; file 26 T.1. Invariant: M.4's segment order is BINDING — PRE-TRAFFIC with F0 OFF (F5 freeze in BLOCK mode + tested; observabil`

**:30** `A` **DECIDE** — normative token(s) in deleted text: GO-03
- OLD: `Objective: M.4 segments in order → the atomic-activation check verifies the FIRST admitted row (watermark + pointer + digest) AND the FIRST post-F0 payment_request row (NON-NULL required_total_at_crea`
- NEW: (to be decided — matched: 0bcb536, 58f5a64 L1, 289ef66 L1)

**:36** `A` **DECIDE** — normative token(s) in deleted text: D-12, GO-03
- OLD: `Read: playbook Section Q; §18 (BLOCKING items); 25-golive V.2/V.3. Invariant: TWO non-waivable classes (round 16) — §18 items 0–3 AND MONEY_SAFETY_BLOCKING (Q5a/Q5b/Q8/Q9/Q11/Q12/Q14/Q16/Q17/Q27/Q29-m`
- NEW: (to be decided — matched: round 16, round 20, round 18)

**:38** `B` **AUTO_OK**
- OLD: `Objective: execute Section Q; deliver evidence pack; obtain the signed PRE-CUTOVER go/no-go (round 19: CONDITIONAL on RUN 2 = zero in GO-03's window — Q5b PENDING-CUTOVER → PASS there; GO-05 rehearsal`
- NEW: `Objective: execute Section Q; deliver evidence pack; obtain the signed PRE-CUTOVER go/no-go (CONDITIONAL on RUN 2 = zero in GO-03's window — Q5b PENDING-CUTOVER → PASS there; GO-05 rehearsal already r`

**:46** `B` **AUTO_OK**
- OLD: `Objective: rehearse app-rollback during dual-run, per-stage flag-off (incl. mid-incident under load), document the point of no return. Round 19: this card runs BEFORE GO-04 (its report is Q23's eviden`
- NEW: `Objective: rehearse app-rollback during dual-run, per-stage flag-off (incl. mid-incident under load), document the point of no return. this card runs BEFORE GO-04 (its report is Q23's evidence and a G`


## portable-implementation-playbook/10-test-matrix.md

**:277** `R` **DECIDE**
- OLD: `corrected 2026-07-11).`
- NEW: (to be decided — matched: 2026-07-11)

**:315** `C` **REWRITE**
- OLD: `### T-21 — RETIRED (round 10 — the engine owns the cutoff calendar)`
- NEW: (to be decided — matched: round 10)

**:318** `R` **DECIDE**
- OLD: `RETIRED 2026-07-11 (PO calendar answer, §7.4/§18-2): no local`
- NEW: (to be decided — matched: 2026-07-11)

**:347** `C` **REWRITE**
- OLD: `(cutoff term RETIRED round 10 — engine owns the calendar.)`
- NEW: (to be decided — matched: round 10)

**:441** `A` **AUTO_OK**
- OLD: `PLUS (b1d91dc M1 + b760786 M1; corrected 4098532 M1) the`
- NEW: `PLUS the`

**:477** `B` **AUTO_OK**
- OLD: `silent. PLUS (aa4399c L1 + 4dbdf2b M1 + 6cb3005 L1,`
- NEW: `silent. PLUS (`

**:494** `B` **AUTO_OK**
- OLD: `contract, higher severity — 4dbdf2b M1); pre-F0 NULL`
- NEW: `contract, higher severity); pre-F0 NULL`

**:500** `B` **AUTO_OK**
- OLD: `routing — 6cb3005 L1).`
- NEW: `routing).`

**:514** `R` **DECIDE**
- OLD: `projection (request-granular; added 2026-07-17).`
- NEW: (to be decided — matched: 2026-07-17)

**:516** `B` **AUTO_OK**
- OLD: `CANCELLED, never COMPLETED (round 11 — §4.1 second`
- NEW: `CANCELLED, never COMPLETED (§4.1 second`

**:523** `A` **AUTO_OK**
- OLD: `TABLE projection cases (§12 contract, review 7ab31e5 M4):`
- NEW: `TABLE projection cases:`

**:533** `R` **DECIDE**
- OLD: `(2026-07-19): the 100-row shows`
- NEW: (to be decided — matched: 2026-07-19)

**:537** `B` **AUTO_OK**
- OLD: `payment_request row, NOT per POST attempt — 0e09f09`
- NEW: `payment_request row, NOT per POST attempt —`

**:540** `B` **AUTO_OK**
- OLD: `FIRST, created_at NULLS FIRST, source_id — 4dbdf2b M2)`
- NEW: `FIRST, created_at NULLS FIRST, source_id)`

**:552** `A` **AUTO_OK**
- OLD: `- NULLABLE-reason edges (review d00ef6a M2): a §6.2`
- NEW: `- NULLABLE-reason edges: a §6.2`

**:558** `B` **AUTO_OK**
- OLD: `c8a92f1 M2): deterministic ordering + keyset pagination —`
- NEW: `): deterministic ordering + keyset pagination —`

**:609** `B` **AUTO_OK**
- OLD: `(content-changed-behind-id → alert fired — round 4; the`
- NEW: `(content-changed-behind-id → alert fired —; the`

**:611** `B` **AUTO_OK**
- OLD: `(round 5: crash-after-consume seeded → approval burned,`
- NEW: `(crash-after-consume seeded → approval burned,`

**:632** `A` **AUTO_OK**
- OLD: `Implemented by: OP-04a..OP-04e (pre-split round 9), RG-05.`
- NEW: `Implemented by: OP-04a..OP-04e, RG-05.`

**:638** `D` **KEEP**
- OLD: `Section: §11 (claim protocol, decided 2026-07-11)   Type: INTEGRATION`
- KEEP unchanged

**:645** `A` **AUTO_OK**
- OLD: `Action:  TWO LANES (round 16). DETERMINISTIC lane: separate`
- NEW: `Action: TWO LANES. DETERMINISTIC lane: separate`

**:666** `B` **AUTO_OK**
- OLD: `traces (round 16 — "no deadlock observed under load"`
- NEW: `traces ("no deadlock observed under load"`

**:676** `A` **AUTO_OK**
- OLD: `### T-35 — Snapshot admission gate + trade-snapshot fence (rounds 5–7)`
- NEW: `### T-35 — Snapshot admission gate + trade-snapshot fence`

**:683** `B` **AUTO_OK**
- OLD: `trade-snapshot FENCE (round 6 — admission alone is a`
- NEW: `trade-snapshot FENCE (admission alone is a`

**:685** `B` **AUTO_OK**
- OLD: `SUPERSESSION is the ratified outcome rule (round 7 — NOT`
- NEW: `SUPERSESSION is the ratified outcome rule (NOT`

**:705** `B` **AUTO_OK**
- OLD: `round-5 H-1 trace); concurrent first snapshots serialize`
- NEW: `H-1 trace); concurrent first snapshots serialize`

**:707** `B` **AUTO_OK**
- OLD: `BLOCK-LEVEL SUPERSESSION (round 7 — deterministic per`
- NEW: `BLOCK-LEVEL SUPERSESSION (deterministic per`

**:725** `B` **AUTO_OK**
- OLD: `c8a92f1 H3) its validation_failed marker lands on the`
- NEW: `) its validation_failed marker lands on the`

**:732** `B` **AUTO_OK**
- OLD: `DEFINED-WINDOW trace (review 4d5cb83 H1 — ratified`
- NEW: `DEFINED-WINDOW trace (ratified`

**:740** `A` **AUTO_OK**
- OLD: `DECISION (review 928341a H1 — no serialization exists):`
- NEW: `DECISION:`

**:758** `C` **REWRITE**
- OLD: `### T-36 — RETIRED (round 10 — greenfield: no bootstrap exists)`
- NEW: (to be decided — matched: round 10)

**:761** `R` **DECIDE**
- OLD: `RETIRED 2026-07-11 (PO fact, §2.4 GREENFIELD): this flow starts`
- NEW: (to be decided — matched: 2026-07-11)

**:768** `A` **AUTO_OK**
- OLD: `(commit 9a53c75). Admission itself is fully covered by T-35.`
- NEW: `. Admission itself is fully covered by T-35.`

**:771** `A` **AUTO_OK**
- OLD: `### T-37 — Absence lifecycle: removal, CANCELLED terminal, anchors, reappearance (round 11)`
- NEW: `### T-37 — Absence lifecycle: removal, CANCELLED terminal, anchors, reappearance`

**:795** `A` **AUTO_OK**
- OLD: `removal + reappearance snapshots (round 12); a payment H`
- NEW: `removal + reappearance snapshots; a payment H`

**:798** `A` **AUTO_OK**
- OLD: `corrected valid snapshot (round 13).`
- NEW: `corrected valid snapshot.`

**:802** `A` **AUTO_OK**
- OLD: `reappearance for F and G (round 12); deliver H's`
- NEW: `reappearance for F and G; deliver H's`

**:803** `A` **AUTO_OK**
- OLD: `malformed then corrected snapshots (round 13); read`
- NEW: `malformed then corrected snapshots; read`

**:811** `B` **AUTO_OK**
- OLD: `never CANCELLED (round 12: NO request-state mutation —`
- NEW: `never CANCELLED (NO request-state mutation —`

**:829** `A` **AUTO_OK**
- OLD: `F (count 1, round 12): the zeroing advance leaves the`
- NEW: `F: the zeroing advance leaves the`

**:833** `B` **AUTO_OK**
- OLD: `provider exception (round-13 HISTORICAL suppression —`
- NEW: `provider exception (HISTORICAL suppression —`

**:837** `A` **AUTO_OK**
- OLD: `H (round 13): the malformed reappearance writes a LIVE`
- NEW: `H: the malformed reappearance writes a LIVE`

**:856** `A` **AUTO_OK**
- OLD: `provider_rejected marker (round 12), or a zeroed scope`
- NEW: `provider_rejected marker, or a zeroed scope`

**:858** `B` **AUTO_OK**
- OLD: `round-12 over-suppression — round 13).`
- NEW: `over-suppression).`

**:860** `A` **AUTO_OK**
- OLD: `(CANCELLED derivation), RG-09 (round-13 narrowed suppression),`
- NEW: `(CANCELLED derivation), RG-09,`

**:879** `A` **AUTO_OK**
- OLD: `and seq 2 pairs (the review-5156f1f H1 regression case).`
- NEW: `and seq 2 pairs.`

**:887** `A` **AUTO_OK**
- OLD: `(review 7ab31e5 H1: no NULL content, no refs, ever); changed`
- NEW: `; changed`

**:894** `A` **AUTO_OK**
- OLD: `(review d00ef6a H3 - the narrow guarantee):`
- NEW: `:`

**:898** `B` **AUTO_OK**
- OLD: `here - review 928341a H2): posting CONTINUES`
- NEW: `here -): posting CONTINUES`

**:906** `B` **AUTO_OK**
- OLD: `(review 4d5cb83 M3 — never instanceof-only: a Spring type`
- NEW: `(never instanceof-only: a Spring type`

**:926** `A` **AUTO_OK**
- OLD: `G  grants + audit coverage (expanded per review 4d5cb83 M2): the`
- NEW: `G grants + audit coverage: the`

**:942** `B` **AUTO_OK**
- OLD: `attempt_event_type) WHEN those lines exist - review 7ab31e5`
- NEW: `attempt_event_type) WHEN those lines exist -`

**:943** `B` **AUTO_OK**
- OLD: `M5, delivery semantics per review 4098532 H1 (afterCommit`
- NEW: `M5, delivery semantics per (afterCommit`

**:950** `A` **AUTO_OK**
- OLD: `assertion (review b760786 M2): the log field is NAMED`
- NEW: `assertion: the log field is NAMED`

**:957** `A` **AUTO_OK**
- OLD: `J  switch transitions (review d00ef6a M3): OFF->ON and ON->OFF`
- NEW: `J switch transitions: OFF->ON and ON->OFF`

**:961** `A` **AUTO_OK**
- OLD: `unmatched-pair alert triage. BOUNDARY (review c8a92f1 M1):`
- NEW: `unmatched-pair alert triage. BOUNDARY:`

**:969** `B` **AUTO_OK**
- OLD: `Failure meaning (review 4d5cb83 L1 — aligned with the narrow`
- NEW: `Failure meaning (aligned with the narrow`

**:977** `B` **AUTO_OK**
- OLD: `BLOCKING: JOURNAL_ENABLEMENT (explicit gate, review 1d8a650 M2 —`
- NEW: `BLOCKING: JOURNAL_ENABLEMENT (explicit gate, —`

**:992** `B` **AUTO_OK**
- OLD: `(the three ATTEMPT-class emission sites) — review b1d91dc M3.`
- NEW: `(the three ATTEMPT-class emission sites) —.`


## portable-implementation-playbook/11-provider-techlead-po-questions.md

**:18** `A/B/D` **AUTO_OK**
- OLD: `| Q-01 | BLOCKING | Upstream + UI teams + PO | §18-0 residue — WRITTEN FILINGS ONLY; every design question is settled (PO-9 ANSWERED 2026-07-11: absence = amendment to zero, lifecycle folded round 11 `
- NEW: `| Q-01 | BLOCKING | Upstream + UI teams + PO | §18-0 residue — WRITTEN FILINGS ONLY; every design question is settled (PO-9 ANSWERED 2026-07-11: absence = amendment to zero, lifecycle — §4.1 CANCELLED`

**:21** `A` **AUTO_OK**
- OLD: `| Q-04 | BLOCKING | Provider | §18-1(c): key-retention TTL IN WRITING; is TTL ≥ max row lifetime incl. ops-queue SLA, weekends, holidays, incidents? Verified at the retention edge? (round 10: no local`
- NEW: `| Q-04 | BLOCKING | Provider | §18-1(c): key-retention TTL IN WRITING; is TTL ≥ max row lifetime incl. ops-queue SLA, weekends, holidays, incidents? Verified at the retention edge? | CT-04; repost_per`

**:23** `A/D` **AUTO_OK**
- OLD: `| Q-06 | CLOSED (round 10) | — | §18-2 ANSWERED by the PO 2026-07-11: the payment engine owns its own calendar; we initiate at any time; residual = the engine's WRITTEN any-time-submission line + late`
- NEW: `| Q-06 | CLOSED | — | §18-2 ANSWERED by the PO 2026-07-11: the payment engine owns its own calendar; we initiate at any time; residual = the engine's WRITTEN any-time-submission line + late-submission`

**:27** `A` **DECIDE** — normative token(s) in deleted text: TL-5
- OLD: `| Q-10 | HIGH | Provider | Query lookback duration — is it ≥ the maximum possible local MAYBE-row lifetime including ops-queue SLA, weekends, holidays, incidents (TL-5 framing — parked rows live days;`
- NEW: (to be decided — matched: round 10)

**:36** `B` **AUTO_OK**
- OLD: `| Q-19 | MEDIUM | Tech lead / UI team | TL-2: card read contract — query API vs replica/view, field list (step timestamps; retry progress "next attempt at / attempt N of M"), freshness SLA incl. repli`
- NEW: `| Q-19 | MEDIUM | Tech lead / UI team | TL-2: card read contract — query API vs replica/view, field list (step timestamps; retry progress "next attempt at / attempt N of M"), freshness SLA incl. repli`

**:37** `A/B` **AUTO_OK**
- OLD: `| Q-20 | MEDIUM | PO / tech lead | Remaining §18 sign-offs: PO-1 ask-then-retry approval; PO-2 query cadence (suggest 2m); PO-3 escalation age (suggest 30m — round 10: no cutoff to clear); PO-4 CLOSED`
- NEW: `| Q-20 | MEDIUM | PO / tech lead | Remaining §18 sign-offs: PO-1 ask-then-retry approval; PO-2 query cadence (suggest 2m); PO-3 escalation age; PO-4 (no local cutoff exists, §18-2); PO-6 deferred-succ`

**:39** `A` **AUTO_OK**
- OLD: `| Q-22 | FUTURE — BLOCKING-FOR-DR (never blocks MVP go-live; DOES block any claim that §5.2 is operationally ready — review aa4399c M2) | Provider / platform | The §5.2 step-5b RECONCILIATION GATE. (a`
- NEW: `| Q-22 | FUTURE — BLOCKING-FOR-DR | Provider / platform | The §5.2 step-5b RECONCILIATION GATE. (a) PROVIDER LISTING: can the platform enumerate EVERY key/payment for a given set of trades over an exa`


## portable-implementation-playbook/12-companion-artifacts.md

**:1** `R` **DECIDE**
- OLD: `> **Purpose:** Companion artifact plan CA-1..CA-10: owner type, required contents, validation, dependents, go-live relevance, failure-if-omitted (original Section L; CA-10 implements the §14.1 attempt`
- NEW: (to be decided — matched: 2026-07-16)

**:11** `R` **DECIDE**
- OLD: `Phase P2); CA-10 (added 2026-07-16) is the implementable spec of the`
- NEW: (to be decided — matched: 2026-07-16)

**:81** `B` **AUTO_OK**
- OLD: `(payment_obligation_id, request_seq) (1d8a650 M1 — legacy rows`
- NEW: `(payment_obligation_id, request_seq) (legacy rows`

**:85** `B` **AUTO_OK**
- OLD: `the bound, never wraparound — 4dbdf2b M1); THE CANONICAL §12`
- NEW: `the bound, never wraparound); THE CANONICAL §12`

**:89** `A` **AUTO_OK**
- OLD: `but may NOT change the logical order (4dbdf2b M2); exact I6`
- NEW: `but may NOT change the logical order; exact I6`

**:90** `B` **AUTO_OK**
- OLD: `expression; enum CHECKs (round 12: the ui_step_status CHECK`
- NEW: `expression; enum CHECKs (the ui_step_status CHECK`

**:94** `R` **DECIDE**
- OLD: `stamp tripwire CHECK (IS NULL OR >= amount — §2.2, 2026-07-19;`
- NEW: (to be decided — matched: 2026-07-19)

**:98** `A` **AUTO_OK**
- OLD: `(type/precision/scale/Java mapping, 0e09f09 L2);`
- NEW: `;`

**:101** `B` **AUTO_OK**
- OLD: `(one per standing scan — EXPLICIT EXCEPTION, review b1d91dc M1:`
- NEW: `(one per standing scan — EXPLICIT EXCEPTION,:`

**:106** `R` **DECIDE**
- OLD: `(added 2026-07-17; scoped as a BLOCKING resolution item for`
- NEW: (to be decided — matched: 2026-07-17)

**:107** `B` **AUTO_OK**
- OLD: `§12 estate mode per review 4d5cb83 M4 — single-trade mode is NOT`
- NEW: `§12 estate mode per — single-trade mode is NOT`

**:111** `B` **AUTO_OK**
- OLD: `created_at NULLS FIRST, source_id) — frozen 4dbdf2b M2, the`
- NEW: `created_at NULLS FIRST, source_id) —, the`

**:117** `B` **AUTO_OK**
- OLD: `not a contract; trade_snapshot_state DDL (§2.4, round 5 —`
- NEW: `not a contract; trade_snapshot_state DDL (§2.4, —`

**:124** `B` **AUTO_OK**
- OLD: `invariant (1d8a650 L2: the stamp's set-once and L9 use named`
- NEW: `invariant (the stamp's set-once and L9 use named`

**:142** `B` **AUTO_OK**
- OLD: `4dbdf2b M1); the VERSIONED IDENTITY NAMESPACE with its`
- NEW: `); the VERSIONED IDENTITY NAMESPACE with its`

**:164** `A` **AUTO_OK**
- OLD: `the QUALIFIED persistence rule (review 928341a M1): canonical`
- NEW: `the QUALIFIED persistence rule: canonical`

**:188** `B` **AUTO_OK**
- OLD: `review 1d8a650 M2); the spec-named entries`
- NEW: `); the spec-named entries`

**:227** `B` **AUTO_OK**
- OLD: `Required contents: execution signature = approval_id ONLY (round 4 —`
- NEW: `Required contents: execution signature = approval_id ONLY (`

**:232** `A` **AUTO_OK**
- OLD: `(round 5): single-transition → CONSUMED CAS + payment transition`
- NEW: `: single-transition → CONSUMED CAS + payment transition`

**:235** `B` **AUTO_OK**
- OLD: `(§9.3 — never resurrect a consumed approval); round 6: completed_at`
- NEW: `(§9.3 — never resurrect a consumed approval);: completed_at`

**:255** `R` **DECIDE**
- OLD: `Section: §14.1 (added 2026-07-16); §2.2 (post_attempt_seq); §16.3`
- NEW: (to be decided — matched: 2026-07-16)

**:261** `R` **DECIDE**
- OLD: `Driver (PO-recorded 2026-07-16): the request actually sent to the`
- NEW: (to be decided — matched: 2026-07-16)

**:272** `B` **AUTO_OK**
- OLD: `- THE DDL TEMPLATE (review 4d5cb83 M1 — this is formally a`
- NEW: `- THE DDL TEMPLATE (this is formally a`

**:296** `A` **AUTO_OK**
- OLD: `-- until it is recorded (review c8a92f1 H4)`
- NEW: `-- until it is recorded`

**:310** `A` **AUTO_OK**
- OLD: `-- CA-10 authoring time — never hand-typed (review c8a92f1 H4)`
- NEW: `-- CA-10 authoring time — never hand-typed`

**:328** `R` **DECIDE**
- OLD: `(TIMESTAMP '2026-08-01 00:00:00 UTC'));`
- NEW: (to be decided — matched: 2026-08-01)

**:331** `A` **AUTO_OK**
- OLD: `-- LOB. BOTH directions enforced (review c8a92f1 H4): STARTED`
- NEW: `-- LOB. BOTH directions enforced: STARTED`

**:366** `A` **AUTO_OK**
- OLD: `-- Unified audit (review 4d5cb83 M2): scope = EVERY auditable`
- NEW: `-- Unified audit: scope = EVERY auditable`

**:382** `B` **AUTO_OK**
- OLD: `c8a92f1 H1): the journal is never a business or money-safety`
- NEW: `): the journal is never a business or money-safety`

**:413** `R` **DECIDE**
- OLD: `record (2026-07-16; never-load-bearing stance 2026-07-17); T-38`
- NEW: (to be decided — matched: 2026-07-16, 2026-07-17)


## portable-implementation-playbook/13-migration-rollout-rollback.md

**:22** `B` **AUTO_OK**
- OLD: `idempotency-key, uetr, request_seq, prospective-I6 — 289ef66`
- NEW: `idempotency-key, uetr, request_seq, prospective-I6 —`

**:27** `A` **DECIDE** — normative token(s) in deleted text: S-05
- OLD: `4. UNIQUEs + I6 + CHECKs (S-05, ORDER CORRECTED 289ef66 M1): I6 is`
- NEW: (to be decided — matched: ORDER CORRECTED 289ef66 M1)

**:47** `B` **AUTO_OK**
- OLD: `soak is clean (round 13: clean = zero UNEXPLAINED disagreements;`
- NEW: `soak is clean (clean = zero UNEXPLAINED disagreements;`

**:50** `A` **AUTO_OK**
- OLD: `(round 14): only after the old writer is gone AND the M.3`
- NEW: `: only after the old writer is gone AND the M.3`

**:55** `A` **AUTO_OK**
- OLD: `### M.1a Reader-first compatibility ladder (round 14 — conditional)`
- NEW: `### M.1a Reader-first compatibility ladder`

**:71** `B` **AUTO_OK**
- OLD: `the round-11 derivation that WRITES CANCELLED.`
- NEW: `the derivation that WRITES CANCELLED.`

**:74** `A` **AUTO_OK**
- OLD: `evidence items (round 15): a compatibility release may READ`
- NEW: `evidence items: a compatibility release may READ`

**:87** `B` **AUTO_OK**
- OLD: `F0 NEW-FLOW TRAFFIC GATE (round 19 — the activation boundary the`
- NEW: `F0 NEW-FLOW TRAFFIC GATE (the activation boundary the`

**:105** `A` **AUTO_OK**
- OLD: `F0 ACTIVATION WINDOW (round 19 — atomic, inside a change freeze):`
- NEW: `F0 ACTIVATION WINDOW:`

**:121** `B` **AUTO_OK**
- OLD: `through requalification (follow-up L1 on 8bf0aba — the first`
- NEW: `through requalification (the first`

**:124** `B` **AUTO_OK**
- OLD: `item 3 carries the full lifecycle; 6cb3005 L2 / 7cc9f49 L2 /`
- NEW: `item 3 carries the full lifecycle; / /`

**:125** `B` **AUTO_OK**
- OLD: `58f5a64 L1). Any nonzero count → STOP, NO-GO,`
- NEW: `). Any nonzero count → STOP, NO-GO,`

**:135** `B` **AUTO_OK**
- OLD: `round 13).`
- NEW: `).`

**:136** `A` **AUTO_OK**
- OLD: `Catch-up + FENCED CUTOVER (rounds 14-15): during dual-run, OLD`
- NEW: `Catch-up + FENCED CUTOVER: during dual-run, OLD`

**:160** `A` **DECIDE** — normative token(s) in deleted text: GO-03
- OLD: `### M.4 Safe enablement order (GO-03 executes — round-20 THREE SEGMENTS)`
- NEW: (to be decided — matched: round-20)

**:163** `A` **AUTO_OK**
- OLD: `ENTRY CONDITION (round 20): the GO-04 PRE-CUTOVER CONDITIONAL GO is`
- NEW: `ENTRY CONDITION: the GO-04 PRE-CUTOVER CONDITIONAL GO is`

**:196** `B` **AUTO_OK**
- OLD: `in file 25 V.2 item 3; 58f5a64 L1). Nonzero or missing signature → ABORT`
- NEW: `in file 25 V.2 item 3). Nonzero or missing signature → ABORT`

**:239** `A` **AUTO_OK**
- OLD: `- TRADE-ADMISSION (round 10 — §2.4 GREENFIELD FACT): this flow is`
- NEW: `- TRADE-ADMISSION: this flow is`

**:243** `B` **AUTO_OK**
- OLD: `assembly flag, no second point of no return. (The round-6..9`
- NEW: `assembly flag, no second point of no return. (The..9`

**:244** `B` **AUTO_OK**
- OLD: `gate/ladder lives in git history at 9a53c75.)`
- NEW: `gate/ladder lives in git history at.)`

**:292** `A` **DECIDE** — normative token(s) in deleted text: CA-4
- OLD: `### M.11 Migration SQL review checklist (round 16 — apply when CA-4/Flyway SQL exists)`
- NEW: (to be decided — matched: round 16)


## portable-implementation-playbook/14-observability-reconciliation-runbooks.md

**:59** `B` **AUTO_OK**
- OLD: `2b697fb M1; scoped per review b1d91dc M1; delivery semantics +`
- NEW: `; scoped per; delivery semantics +`

**:60** `B` **AUTO_OK**
- OLD: `safe-execution envelope per review b760786 M1 — a candidate`
- NEW: `safe-execution envelope per — a candidate`

**:63** `B` **AUTO_OK**
- OLD: `4098532 M1): shipping the query + its correctness test is a`
- NEW: `): shipping the query + its correctness test is a`

**:79** `B` **AUTO_OK**
- OLD: `PREDICATE is EXACT — review b760786 L1; marker liveness is the`
- NEW: `PREDICATE is EXACT —; marker liveness is the`

**:97** `B` **AUTO_OK**
- OLD: `SAFE-EXECUTION ENVELOPE (b760786 M1 — an on-demand incident`
- NEW: `SAFE-EXECUTION ENVELOPE (an on-demand incident`

**:110** `A` **AUTO_OK**
- OLD: `this (review 4098532 L1);`
- NEW: `this;`

**:123** `A` **AUTO_OK**
- OLD: `(explicit exception, b1d91dc M1): no new index, no schedule, no`
- NEW: `: no new index, no schedule, no`

**:134** `B` **AUTO_OK**
- OLD: `- post-F0 NULL request_seq (IDENTITY CONTRACT, 4dbdf2b M1 —`
- NEW: `- post-F0 NULL request_seq (IDENTITY CONTRACT, —`

**:142** `B` **AUTO_OK**
- OLD: `aa4399c L1 — OB-02): created_at >= the F0 activation timestamp`
- NEW: `— OB-02): created_at >= the F0 activation timestamp`

**:368** `A` **AUTO_OK**
- OLD: `## RB-F0 — F0 re-enable after rollback / incident (round 20)`
- NEW: `## RB-F0 — F0 re-enable after rollback / incident`

**:386** `B` **AUTO_OK**
- OLD: `review 5156f1f M3): the DB-side audit trail on the four §2`
- NEW: `): the DB-side audit trail on the four §2`

**:408** `C` **REWRITE**
- OLD: `retired bootstrap machinery, git 9a53c75, is on the table);`
- NEW: (to be decided — matched: 9a53c75)


## portable-implementation-playbook/16-local-agent-instructions.md

**:31** `R` **DECIDE**
- OLD: `2026-07-11): MODIFICATION scope is always exactly the card's;`
- NEW: (to be decided — matched: 2026-07-11)

**:60** `B` **AUTO_OK**
- OLD: `EXCEPTIONS (round 17 — rule 22's no-split list WINS over this`
- NEW: `EXCEPTIONS (rule 22's no-split list WINS over this`

**:74** `B/R` **DECIDE**
- OLD: `(a) 2026-07-11 round 3: the §9.3 two-step approval workflow's`
- NEW: (to be decided — matched: round 3, 2026-07-11)

**:77** `R` **DECIDE**
- OLD: `(b) 2026-07-16 (§14.1 — PO-recorded driver: the request sent to`
- NEW: (to be decided — matched: 2026-07-16)

**:110** `R` **DECIDE**
- OLD: `19. Delivery model (adopted 2026-07-11): a card is a COMMIT unit on`
- NEW: (to be decided — matched: 2026-07-11)

**:120** `A` **AUTO_OK**
- OLD: `20. HUMAN REVIEW CHECKPOINTS (round 16): a human review is REQUIRED`
- NEW: `20. HUMAN REVIEW CHECKPOINTS: a human review is REQUIRED`

**:140** `B` **AUTO_OK**
- OLD: `SIX (round 18 — the file-26 T.2 checklist, repeated here`
- NEW: `SIX (the file-26 T.2 checklist, repeated here`

**:149** `B` **AUTO_OK**
- OLD: `integration branch at a time (round 18 — the next phase's`
- NEW: `integration branch at a time (the next phase's`


## portable-implementation-playbook/17-go-live-checklist.md

**:1** `A` **AUTO_OK**
- OLD: `> **Purpose:** Go-live readiness checklist Q1-Q31 with PASS/FAIL/BLOCKED states — plus ONE additional defined state, PENDING-CUTOVER, legal ONLY for Q5b at GO-04 (round 20) — and evidence columns; TWO`
- NEW: `> **Purpose:** Go-live readiness checklist Q1-Q31 with PASS/FAIL/BLOCKED states — plus ONE additional defined state, PENDING-CUTOVER, legal ONLY for Q5b at GO-04 — and evidence columns; TWO non-waivab`

**:11** `A` **AUTO_OK**
- OLD: `report, signed document). TWO non-waivable classes (round 16): §18`
- NEW: `report, signed document). TWO non-waivable classes: §18`

**:15** `B` **AUTO_OK**
- OLD: `Round 20: Q5b's PENDING-CUTOVER is a DEFINED state, not missing`
- NEW: `: Q5b's PENDING-CUTOVER is a DEFINED state, not missing`

**:27** `B/D` **AUTO_OK**
- OLD: `| Q1 | §18 BLOCKING item 0 residue closed: WRITTEN filings of ask 5 (snapshot schema + uniqueness) and ask 8 (store contract incl. IMMUTABILITY) — both CONFIRMED verbally 2026-07-11, the filed papers `
- NEW: `| Q1 | §18 BLOCKING item 0 residue closed: WRITTEN filings of ask 5 (snapshot schema + uniqueness) and ask 8 (store contract incl. IMMUTABILITY) — both CONFIRMED verbally 2026-07-11, the filed papers `

**:28** `A` **DECIDE** — normative token(s) in deleted text: IMPLEMENTATION_REQUIRED
- OLD: `| Q2 | §18 BLOCKING item 1: sandbox collision matrix (a)–(d) EXECUTED and PASSED; BOTH CT-04/CT-05 TYPED consequence records (the only two producers — CT-02/CT-03 are plain pass/fail, no record) NO_IM`
- NEW: (to be decided — matched: 289ef66 M2)

**:29** `A` **DECIDE** — normative token(s) in deleted text: CA-1
- OLD: `| Q3 | §18 item 2 CLOSED (round 10 — the engine owns its cutoff calendar; verify the CA-1 table carries the engine's late-submission response class + the written any-time-submission line) | §18-2 (clo`
- NEW: (to be decided — matched: round 10)

**:31** `A` **AUTO_OK**
- OLD: `| Q5a | Schema at CA-4 target: constraints VALIDATED, triggers live, indexes in place; migration test pass green (incl. dual-run); T-35/T-37 green; the CUTOVER_POPULATION_GREENFIELD RUN-2 queries + sc`
- NEW: `| Q5a | Schema at CA-4 target: constraints VALIDATED, triggers live, indexes in place; migration test pass green (incl. dual-run); T-35/T-37 green; the CUTOVER_POPULATION_GREENFIELD RUN-2 queries + sc`

**:32** `A` **AUTO_OK**
- OLD: `| Q5b | Time-of-cutover CUTOVER_POPULATION_GREENFIELD RUN 2 (file 26 T.1): the reviewed queries re-run inside GO-03's F0 window AFTER in-scope writer drain/fence, counts ZERO, DBA/TL signatures, manif`
- NEW: `| Q5b | Time-of-cutover CUTOVER_POPULATION_GREENFIELD RUN 2 (file 26 T.1): the reviewed queries re-run inside GO-03's F0 window AFTER in-scope writer drain/fence, counts ZERO, DBA/TL signatures, manif`

**:37** `A` **DECIDE** — normative token(s) in deleted text: Q2
- OLD: `| Q10 | Provider idempotency sandbox tests green (same as Q2, incl. the consequence-closure verification — 289ef66 M2) + SDK contract checks (CT-07) recorded | CT suite, T-11..14 | | |`
- NEW: (to be decided — matched: 289ef66 M2)

**:40** `B` **AUTO_OK**
- OLD: `| Q13 | CLOSED round 10 — no local cutoff calendar exists (engine-owned, §18-2); verify no cutoff machinery crept into the target env config | §18-2 (closed) | | |`
- NEW: `| Q13 | — no local cutoff calendar exists (engine-owned, §18-2); verify no cutoff machinery crept into the target env config | §18-2 (closed) | | |`

**:45** `B` **AUTO_OK**
- OLD: `| Q18 | Reconciliation tripwires live (terminal-evidence CRITICAL, count sanity, both post-F0 creation-column scans: NULL stamp → ticket, NULL request_seq → alert — 6cb3005 L1) | T-30, OB-02 | | |`
- NEW: `| Q18 | Reconciliation tripwires live (terminal-evidence CRITICAL, count sanity, both post-F0 creation-column scans: NULL stamp → ticket, NULL request_seq → alert) | T-30, OB-02 | | |`

**:53** `A` **DECIDE** — normative token(s) in deleted text: GO-03, GO-04, NEVER, ONLY, Q26, RG-06, T-31
- OLD: `| Q26 | UI/card correctness tests green (no false completion; §12 multi-obligation lookup; amount-series stamp: RG-06 creation-stamp suite green + T-31 projection stamps/NULL rendering — PRE-CUTOVER e`
- NEW: (to be decided — matched: aa4399c M1)

**:57** `A` **DECIDE** — normative token(s) in deleted text: T-38
- OLD: `| Q30 | Security/supply-chain gate (round 16) on the EXACT RC: SAST + dependency vulnerability scan + SBOM + license policy + secret scan; Kafka ACLs, DB grants, service/ops-role least privilege, endp`
- NEW: (to be decided — matched: 1d8a650 M2, round 16)

**:58** `A` **AUTO_OK**
- OLD: `| Q31 | Capacity gate (round 16): peak + post-outage burst test at the §16.5 volume NFR; connection-pool/bulkhead saturation behavior; scanner backlog recovery; provider quota shaping (TL-13 budget); `
- NEW: `| Q31 | Capacity gate: peak + post-outage burst test at the §16.5 volume NFR; connection-pool/bulkhead saturation behavior; scanner backlog recovery; provider quota shaping (TL-13 budget); card-read l`


## portable-implementation-playbook/18-playbook-quality-self-check.md

**:33** `R` **DECIDE**
- OLD: `CA-10/AUD-01, 2026-07-16) are sanctioned, spec'd at requirement`
- NEW: (to be decided — matched: 2026-07-16)

**:38** `B` **AUTO_OK**
- OLD: `d00ef6a H1 — a stale phase-02 packet survived one fold): every`
- NEW: `— a stale phase-02 packet survived one fold): every`

**:49** `A` **AUTO_OK**
- OLD: `exit set + OP-04a–e's waivable ergonomics endpoints (rounds 3–9).`
- NEW: `exit set + OP-04a–e's waivable ergonomics endpoints.`

**:72** `R` **DECIDE**
- OLD: `[x] Drift lint (added 2026-07-11; EXECUTABLE since the second`
- NEW: (to be decided — matched: 2026-07-11)

**:82** `B` **AUTO_OK**
- OLD: `+ the canonical P3 order stated verbatim in file 20 (round 9;`
- NEW: `+ the canonical P3 order stated verbatim in file 20 (`

**:87** `B` **AUTO_OK**
- OLD: `procedure" dual-control phrasing (round 3: operation + §9.3`
- NEW: `procedure" dual-control phrasing (operation + §9.3`

**:89** `C` **REWRITE**
- OLD: `(round 3, cutoff retired round 10: bound = max attempts; gated`
- NEW: (to be decided — matched: round 3, round 10)

**:105** `B` **AUTO_OK**
- OLD: `2a19c20 L4); K-01 may proceed.`
- NEW: `); K-01 may proceed.`


## portable-implementation-playbook/19-local-task-execution-report-template.md

**:22** `B` **AUTO_OK**
- OLD: `DIV-2 adaptations used? no / yes → six-item proof per row (round 18:`
- NEW: `DIV-2 adaptations used? no / yes → six-item proof per row (`


## portable-implementation-playbook/20-execution-sequence-and-decision-defaults.md

**:19** `R` **DECIDE**
- OLD: `**Phase-boundary gate (rule 19, adopted 2026-07-11):** cards commit to`
- NEW: (to be decided — matched: 2026-07-11)

**:58** `R` **DECIDE**
- OLD: `| 25a | CA-10 (§14.1 attempt-journal spec) | HUMAN+AGENT | §14.1 (PO driver recorded 2026-07-16); CA-4; CA-6 | phase-02 |`
- NEW: (to be decided — matched: 2026-07-16)

**:60** `C` **REWRITE**
- OLD: `| 26–34+34a | P3 — EXACT ORDER (round 9, normative; REORDERED 289ef66 M1 — backfill BEFORE the constraint objects legacy data could violate, because I6 is a UNIQUE index and NOVALIDATE does not apply `
- NEW: (to be decided — matched: REORDERED 289ef66 M1, 2a19c20 L4, round 9, round 10)

**:72** `A` **EXPLICIT**
- OLD: `| 93a | OP-04a | AGENT | OP-02 green; RG-05; RG-06 (round 9 pre-split: shared contract + retry/reject/annotate) | phase-11 |`
- NEW: `| 93a | OP-04a | AGENT | OP-02 green; RG-05; RG-06 | phase-11 |`

**:82** `A` **DECIDE** — normative token(s) in deleted text: Q23
- OLD: `| 103 | GO-05 | HUMAN+AGENT | GO-01 stages; production-like env (round 19: rehearsal BEFORE authorization — Q23's evidence) | phase-14 |`
- NEW: (to be decided — matched: round 19)

**:83** `A` **EXPLICIT**
- OLD: `| 104 | GO-04 | HUMAN | GO-02 clean + GO-05 recorded + all gates; 17-go-live-checklist.md (round 19: PRE-CUTOVER CONDITIONAL go/no-go) | phase-14 |`
- NEW: `| 104 | GO-04 | HUMAN | GO-02 clean + GO-05 recorded + all gates; 17-go-live-checklist.md | phase-14 |`

**:84** `A` **EXPLICIT**
- OLD: `| 105 | GO-03 | HUMAN+AGENT | GO-04 CONDITIONAL GO; F0 window (fence → RUN-2 zero → sign → enable); stage F4 needs CT PASS (DD-6) (round 19: LAST row) | phase-14 |`
- NEW: `| 105 | GO-03 | HUMAN+AGENT | GO-04 CONDITIONAL GO; F0 window (fence → RUN-2 zero → sign → enable); stage F4 needs CT PASS (DD-6) | phase-14 |`

**:105** `B` **EXPLICIT**
- OLD: `DD-4  RC-03 stubs (round 11 sweep: no cutoff term exists;`
- NEW: `DD-4 RC-03 stubs (sweep: no cutoff term exists;`

**:106** `B` **EXPLICIT**
- OLD: `no cutoff stub may be built — the round-10 closure is total):`
- NEW: `no cutoff stub may be built — the closure is total):`

**:118** `A` **EXPLICIT**
- OLD: `way (extended 289ef66 M2): CT-02..CT-05 must be PASSED — AND`
- NEW: `way: CT-02..CT-05 must be PASSED — AND`


## portable-implementation-playbook/21-progress-tracker-template.md

**:50** `A` **DECIDE** — normative token(s) in deleted text: S-05, S-08
- OLD: `| 29a | S-10 | TODO | | | (round 5: §2.4 table — runs HERE, before S-08/S-05) |`
- NEW: (to be decided — matched: round 5)

**:51** `A` **DECIDE** — normative token(s) in deleted text: NOVALIDATE, S-05, UNIQUE
- OLD: `| 30 | S-08 | TODO | | | (289ef66 M1: backfill BEFORE the S-05 constraint objects — I6 is a UNIQUE index, NOVALIDATE never applies) |`
- NEW: (to be decided — matched: 289ef66 M1)

**:116** `A` **AUTO_OK**
- OLD: `| 93a | OP-04a | TODO | | | (round-9 pre-split 1/5) |`
- NEW: `| 93a | OP-04a | TODO | | | |`

**:130** `A` **DECIDE** — normative token(s) in deleted text: GO-04
- OLD: `| 103 | GO-05 | TODO | | | (round 19: rehearsal BEFORE GO-04) |`
- NEW: (to be decided — matched: round 19)


## portable-implementation-playbook/23-task-kickoff-prompt.md

**:45** `A` **AUTO_OK**
- OLD: `missing proof blocks implementation (round 18).`
- NEW: `missing proof blocks implementation.`

**:73** `A` **AUTO_OK**
- OLD: `Instruction precedence when sources seem to disagree (round 17):`
- NEW: `Instruction precedence when sources seem to disagree:`


## portable-implementation-playbook/24-implementation-mechanics.md

**:143** `B` **EXPLICIT**
- OLD: `--   review 4098532 H1 — NEVER publish inside the transaction):`
- NEW: `-- — NEVER publish inside the transaction):`

**:189** `A` **EXPLICIT**
- OLD: `- Global order (round 6): trade row (§2.4, snapshot paths only) →`
- NEW: `- Global order: trade row (§2.4, snapshot paths only) →`

**:208** `R` **DECIDE**
- OLD: `-- or not (corrected 2026-07-11 — an expired-takeover branch here`
- NEW: (to be decided — matched: 2026-07-11)

**:250** `B` **EXPLICIT**
- OLD: `freeze, amount-vs-shortfall staleness for MAYBE rows — round 10:`
- NEW: `freeze, amount-vs-shortfall staleness for MAYBE rows —:`

**:252** `B` **EXPLICIT**
- OLD: `- (The rounds-8/9 pointer-presence claim-gate was REMOVED in`
- NEW: `- (The pointer-presence claim-gate was REMOVED in`

**:253** `B` **EXPLICIT**
- OLD: `round 10 — §2.4 greenfield fact: every trade row is born from an`
- NEW: `— §2.4 greenfield fact: every trade row is born from an`

**:255** `B` **EXPLICIT**
- OLD: `is unreachable; git history at 9a53c75.)`
- NEW: `is unreachable; git history at.)`

**:310** `B` **EXPLICIT**
- OLD: `- The retry bound is MAX ATTEMPTS ONLY (round 10 — the engine`
- NEW: `- The retry bound is MAX ATTEMPTS ONLY (the engine`

**:312** `D` **KEEP**
- OLD: `2026-07-11 decision) — retry_deadline_at is reserved/unused; wire`
- KEEP unchanged

**:315** `B` **EXPLICIT**
- OLD: `is no suspension mechanism to implement. (Round 10: NO cutoff`
- NEW: `is no suspension mechanism to implement. (NO cutoff`

**:339** `A` **EXPLICIT**
- OLD: `trade-level ADMISSION transaction (§6.1/§2.4, round 5): upsert-lock`
- NEW: `trade-level ADMISSION transaction: upsert-lock`

**:344** `B` **EXPLICIT**
- OLD: `sorted tuple order, ONE transaction per block (§6.1). ROUND 6 —`
- NEW: `sorted tuple order, ONE transaction per block (§6.1). —`

**:345** `B` **EXPLICIT**
- OLD: `the TRADE-SNAPSHOT FENCE, NOT optional (round-7 rename; the old`
- NEW: `the TRADE-SNAPSHOT FENCE, NOT optional (rename; the old`

**:383** `B` **EXPLICIT**
- OLD: `cutoff calendar exists — round 10, §7.4.)`
- NEW: `cutoff calendar exists —, §7.4.)`

**:447** `A` **EXPLICIT**
- OLD: `structurally safe); no cutoff exists to check (round 10), never`
- NEW: `structurally safe); no cutoff exists to check, never`

**:471** `D` **KEEP**
- OLD: `future console). Execution boundary (decided 2026-07-11): an`
- KEEP unchanged

**:481** `B` **EXPLICIT**
- OLD: `DERIVED from the approval record (round 4: never approver-`
- NEW: `DERIVED from the approval record (never approver-`

**:483** `A` **EXPLICIT**
- OLD: `[ ] approval consumption matches the OPERATION CLASS (round 5):`
- NEW: `approval consumption matches the OPERATION CLASS:`

**:529** `B` **EXPLICIT**
- OLD: `per review d00ef6a H3): every rider is SWITCH-GATED (§14.1 switch`
- NEW: `per): every rider is SWITCH-GATED (§14.1 switch`

**:536** `A` **EXPLICIT**
- OLD: `ARE FATAL BY DEFAULT (review 928341a H2). Allowed failures are`
- NEW: `ARE FATAL BY DEFAULT. Allowed failures are`


## portable-implementation-playbook/25-golive-verification-procedures.md

**:17** `A` **EXPLICIT**
- OLD: `- TWO non-waivable classes (round 16):`
- NEW: `- TWO non-waivable classes:`

**:36** `B/D` **EXPLICIT**
- OLD: `| Q1 | Read the filed upstream confirmation (ask 5) — it must state snapshot schema + within-snapshot uniqueness as a GUARANTEE ("usually" = FAIL). Run the B-01/IN-02 intake validation tests (within-s`
- NEW: `| Q1 | Read the filed upstream confirmation (ask 5) — it must state snapshot schema + within-snapshot uniqueness as a GUARANTEE ("usually" = FAIL). Run the B-01/IN-02 intake validation tests (within-s`

**:37** `A/B` **EXPLICIT**
- OLD: `| Q2 | Confirm CT-02..05 executed against the REAL sandbox (not mocks): (a) same-payload dedupe, (b) different-payload reject with distinguishable code, (c) retention-TTL edge with the TTL stated in w`
- NEW: `| Q2 | Confirm CT-02..05 executed against the REAL sandbox (not mocks): (a) same-payload dedupe, (b) different-payload reject with distinguishable code, (c) retention-TTL edge with the TTL stated in w`

**:38** `B` **EXPLICIT**
- OLD: `| Q3 | CLOSED round 10 (engine-owned calendar, §18-2): verify the engine's WRITTEN any-time-submission line is filed, the CA-1 table carries the late-submission response class (or its recorded absence`
- NEW: `| Q3 | (engine-owned calendar, §18-2): verify the engine's WRITTEN any-time-submission line is filed, the CA-1 table carries the late-submission response class (or its recorded absence), and NO local `

**:39** `D` **KEEP**
- OLD: `| Q4 | Verify the OP-01 audited operation deployed (endpoint restricted to the enterprise ops role — attempt it with an unauthorized identity, must fail; 2026-07-11 boundary: authorized application en`
- KEEP unchanged

**:40** `C` **REWRITE**
- OLD: `| Q5a | Diff deployed schema vs CA-4 DDL (constraints VALIDATED state, both triggers live, artifact-4 index list present). Run the migration test incl. dual-run (old+new app versions concurrently). EX`
- NEW: (to be decided — matched: round 10, Round 20)

**:41** `A` **DECIDE** — normative token(s) in deleted text: Q5a
- OLD: `| Q5b | CUTOVER_POPULATION_GREENFIELD RUN 2 (file 26 T.1): inside GO-03's F0 activation window — after old in-scope writers are drained/fenced (or under change freeze) — re-run the reviewed population`
- NEW: (to be decided — matched: 9a53c75, round 20)

**:42** `R` **DECIDE**
- OLD: `| Q6 | ST-01..03 suites green; run the legality verification (the artifact-6 property-based L1–L8 sweep — every illegal tuple write refused by CHECK/trigger — plus T-25 for the trigger layer; referenc`
- NEW: (to be decided — matched: 2026-07-11)

**:46** `A` **DECIDE** — normative token(s) in deleted text: CT-04, CT-05, IMPLEMENTED_AND_VERIFIED, NO_IMPLEMENTATION_CHANGE
- OLD: `| Q10 | Same CT evidence as Q2 filed in the pack — INCLUDING the consequence-closure verification (both CT-04/CT-05 typed records — the only two producers — NO_IMPLEMENTATION_CHANGE or IMPLEMENTED_AND`
- NEW: (to be decided — matched: 289ef66 M2)

**:49** `B` **EXPLICIT**
- OLD: `| Q13 | CLOSED round 10 with Q3 (no local calendar exists; nothing to configure per environment). | — | OPS |`
- NEW: `| Q13 | with Q3 (no local calendar exists; nothing to configure per environment). | — | OPS |`

**:54** `B` **EXPLICIT**
- OLD: `| Q18 | T-30 green: terminal-evidence CRITICAL fires on NEW event_id + zero-row CAS vs TERMINAL row; benign redelivery silent; per-obligation count sanity ticket fires; BOTH post-F0 creation-column sc`
- NEW: `| Q18 | T-30 green: terminal-evidence CRITICAL fires on NEW event_id + zero-row CAS vs TERMINAL row; benign redelivery silent; per-obligation count sanity ticket fires; BOTH post-F0 creation-column sc`

**:56** `R` **DECIDE**
- OLD: `| Q20 | T-32 green: every §15 entry fires on its seeded condition ON ITS ANCHOR CLOCK, carries a runbook link; alert rollup groups outage collateral into one incident (breaker-OPEN storm test); dead-g`
- NEW: (to be decided — matched: 2026-07-11)

**:59** `A` **DECIDE** — normative token(s) in deleted text: GO-04, GO-05
- OLD: `| Q23 | Rollback REHEARSED in a pre-prod env (not just documented): execute GO-05's rollback from the mid-rollout state; record timings; point-of-no-return step identified in the plan. (Round 19: GO-0`
- NEW: (to be decided — matched: Round 19)

**:62** `A` **EXPLICIT**
- OLD: `| Q26 | T-31 green incl.: multi-payment trade returns ALL obligations (count never an error); anchors show DATA_VALIDATION_FAILED; MAYBE shows rank-1 PAYMENT_OUTCOME_UNKNOWN never SYSTEM_UNAVAILABLE; `
- NEW: `| Q26 | T-31 green incl. multi-payment trade returns ALL obligations (count never an error); anchors show DATA_VALIDATION_FAILED; MAYBE shows rank-1 PAYMENT_OUTCOME_UNKNOWN never SYSTEM_UNAVAILABLE; u`

**:65** `A` **EXPLICIT**
- OLD: `| Q30 | Run the security/supply-chain evidence set on the EXACT RC build (round 16): SAST + dependency-vuln + SBOM + license + secret scans; dump and review Kafka ACLs, DB grants, service/ops-role pri`
- NEW: `| Q30 | Run the security/supply-chain evidence set on the EXACT RC build: SAST + dependency-vuln + SBOM + license + secret scans; dump and review Kafka ACLs, DB grants, service/ops-role privileges (le`

**:66** `A` **EXPLICIT**
- OLD: `| Q31 | Run the capacity evidence set (round 16): load test at peak + post-outage burst per the §16.5 volume NFR; drive connection-pool/bulkhead saturation and record degradation behavior (no cross-de`
- NEW: `| Q31 | Run the capacity evidence set: load test at peak + post-outage burst per the §16.5 volume NFR; drive connection-pool/bulkhead saturation and record degradation behavior (no cross-dependency st`

**:75** `A` **EXPLICIT**
- OLD: `manifest.yaml         — the binding identities (round 16): RC/app`
- NEW: `manifest.yaml — the binding identities: RC/app`

**:94** `B` **EXPLICIT**
- OLD: `GREENFIELD proof, round 18; round 19:`
- NEW: `GREENFIELD proof,:`

**:111** `A` **EXPLICIT**
- OLD: `Evidence is IMMUTABLE once captured (round 16): a change to any`
- NEW: `Evidence is IMMUTABLE once captured: a change to any`

**:121** `B` **EXPLICIT**
- OLD: `changed (review 5156f1f M2 — this is how Q5b's PENDING-CUTOVER`
- NEW: `changed (this is how Q5b's PENDING-CUTOVER`

**:122** `B` **EXPLICIT**
- OLD: `coexists with immutability; version model clarified 58f5a64 L3):`
- NEW: `coexists with immutability; version model):`

**:127** `B` **EXPLICIT**
- OLD: `PASS — round 20). Q5b's subfolder EXISTS at GO-04 and contains`
- NEW: `PASS). Q5b's subfolder EXISTS at GO-04 and contains`

**:135** `B` **EXPLICIT**
- OLD: `on 0bcb536 — an INVALID first row observed INSIDE the change`
- NEW: `on — an INVALID first row observed INSIDE the change`

**:145** `B` **EXPLICIT**
- OLD: `FIRST_REQUEST_CREATION_COLUMNS (7cc9f49 L2; mechanism + query`
- NEW: `FIRST_REQUEST_CREATION_COLUMNS (mechanism + query`

**:146** `B` **EXPLICIT**
- OLD: `frozen 58f5a64 L1/L2; failure state added 289ef66 L1 — the ONE`
- NEW: `; failure state — the ONE`

**:154** `B` **EXPLICIT**
- OLD: `no-sample v2 — follow-up L1 on 0bcb536). Three v2 branches:`
- NEW: `no-sample v2). Three v2 branches:`

**:183** `B` **EXPLICIT**
- OLD: `requalification (follow-up L1 on 8bf0aba: the first`
- NEW: `requalification (the first`

**:186** `B` **EXPLICIT**
- OLD: `FROZEN QUERY TEMPLATE (58f5a64 L2 — resolve ONLY the physical`
- NEW: `FROZEN QUERY TEMPLATE (resolve ONLY the physical`

**:204** `B` **EXPLICIT**
- OLD: `CHECKSUM verification against the ticket, 289ef66 L1; the`
- NEW: `CHECKSUM verification against the ticket, the`

**:205** `B` **EXPLICIT**
- OLD: `inside-window invalid case added follow-up L1 on 0bcb536): no`
- NEW: `inside-window invalid case added): no`

**:218** `A` **EXPLICIT**
- OLD: `(289ef66 L1): a NULL request_seq is an IDENTITY-CONTRACT`
- NEW: `: a NULL request_seq is an IDENTITY-CONTRACT`

**:237** `A` **EXPLICIT**
- OLD: `2. Walk the MONEY_SAFETY_BLOCKING class next (round 16): Q5a, Q5b, Q8, Q9, Q11, Q12, Q14, Q16, Q17, Q27, and Q29's minimal exit set. Any FAIL or missing`
- NEW: `2. Walk the MONEY_SAFETY_BLOCKING class next: Q5a, Q5b, Q8, Q9, Q11, Q12, Q14, Q16, Q17, Q27, and Q29's minimal exit set. Any FAIL or missing`

**:240** `A` **EXPLICIT**
- OLD: `ONE defined exception (round 20): Q5b ALONE may stand as`
- NEW: `ONE defined exception: Q5b ALONE may stand as`

**:247** `A` **EXPLICIT**
- OLD: `4. Evidence integrity (round 16): validate manifest.yaml against the`
- NEW: `4. Evidence integrity: validate manifest.yaml against the`

**:260** `B` **EXPLICIT**
- OLD: `TL, and OPS signatures on the same dated entry. Round 19: this`
- NEW: `TL, and OPS signatures on the same dated entry. this`


## portable-implementation-playbook/26-team-execution-and-divergence-protocol.md

**:12** `B` **AUTO_OK**
- OLD: `This kit rests on TWO INDEPENDENT premises (round 17 — do not`
- NEW: `This kit rests on TWO INDEPENDENT premises (do not`

**:35** `A` **AUTO_OK**
- OLD: `bootstrap/pointer machinery (rounds 6–9; git 9a53c75) was removed`
- NEW: `bootstrap/pointer machinery was removed`

**:40** `B` **AUTO_OK**
- OLD: `PROOF REQUIRED — RUN TWICE (rounds 17–18; a discovery snapshot is`
- NEW: `PROOF REQUIRED — RUN TWICE (18; a discovery snapshot is`

**:55** `A` **AUTO_OK**
- OLD: `PASS (round 20); filed as Q5b evidence.`
- NEW: `PASS; filed as Q5b evidence.`

**:67** `A` **AUTO_OK**
- OLD: `LIFECYCLE (round 19): the ZERO-population form applies to INITIAL`
- NEW: `LIFECYCLE: the ZERO-population form applies to INITIAL`

**:77** `B` **AUTO_OK**
- OLD: `machinery — git 9a53c75). The EXECUTABLE procedure for the`
- NEW: `machinery — git). The EXECUTABLE procedure for the`

**:79** `A` **AUTO_OK**
- OLD: `queries, zero-uncovered threshold, sign-offs, evidence (round 20).`
- NEW: `queries, zero-uncovered threshold, sign-offs, evidence.`

**:122** `B` **AUTO_OK**
- OLD: `reviewer sees the entry. Round 17 — DIV-2`
- NEW: `reviewer sees the entry. — DIV-2`

**:244** `A` **AUTO_OK**
- OLD: `SUB-STAGES (round 17): B-prep — sandbox access requests,`
- NEW: `SUB-STAGES: B-prep — sandbox access requests,`

**:257** `B` **AUTO_OK**
- OLD: `After P3 → P4 → P5 have ALL merged to main (round 17: P6 sits`
- NEW: `After P3 → P4 → P5 have ALL merged to main (P6 sits`

**:290** `A` **AUTO_OK**
- OLD: `- AUTHORITY (round 17): card prerequisites + file 20's gates are`
- NEW: `- AUTHORITY: card prerequisites + file 20's gates are`

**:295** `B` **AUTO_OK**
- OLD: `considered and remains NOT adopted — round-9 decision:`
- NEW: `considered and remains NOT adopted — decision:`


## portable-implementation-playbook/27-service-pseudocode.md

**:4** `A` **AUTO_OK**
- OLD: `> **Used by:** onboarding only (deliberately outside the per-card context budget — review 7ab31e5 L2).`
- NEW: `> **Used by:** onboarding only.`

**:122** `B` **AUTO_OK**
- OLD: `// d00ef6a H4): WHOLE-SNAPSHOT`
- NEW: `//): WHOLE-SNAPSHOT`

**:130** `B` **AUTO_OK**
- OLD: `// alike"; review c8a92f1 H3). Anchors are upserted only for`
- NEW: `// alike"). Anchors are upserted only for`

**:138** `A` **AUTO_OK**
- OLD: `// (review 928341a H1 — no trade-row lock spans this pass):`
- NEW: `//:`

**:275** `R` **DECIDE**
- OLD: `SELECT obligation LEFT JOIN request            // (2026-07-17): pure read`
- NEW: (to be decided — matched: 2026-07-17)

**:386** `A` **AUTO_OK**
- OLD: `// (review c8a92f1 H2): a queued`
- NEW: `//: a queued`

**:391** `B` **AUTO_OK**
- OLD: `// 4d5cb83 L2): a worker past`
- NEW: `//): a worker past`

**:416** `A` **AUTO_OK**
- OLD: `(review d00ef6a H4 — the scan-to-claim race)>`
- NEW: `>`

**:435** `A` **AUTO_OK**
- OLD: `at-most-once (§14 delivery contract, 4098532 H1); publication`
- NEW: `at-most-once; publication`

**:493** `A` **AUTO_OK**
- OLD: `(best-effort, at-most-once — §14 delivery contract, 4098532 H1)`
- NEW: `(line emptied)`

**:602** `B` **AUTO_OK**
- OLD: `// d00ef6a H4); outcome NOT written; CRITICAL;`
- NEW: `//); outcome NOT written; CRITICAL;`

