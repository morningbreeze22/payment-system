> **Purpose:** Task cards OB-01..OB-02 (drift scanner + reconciliation tripwires) (original Section H, phase P12).
> **When to use this file:** When executing the tasks of this phase, one card at a time, with the matching packet file from 09-minimal-context-packets/.
> **Depends on:** 08-task-cards/README.md; 01-playbook-index.md; 07-placeholder-glossary.md; the requirement sections cited per card; the locally filled mapping template.
> **Used by:** The local coding agent executing phase P12.
> **Safe to transfer:** yes
> **Contains local code names:** no

## H-Phase 12 — Drift, reconciliation (P12)

### OB-01 — Drift scanner

- **Task ID:** OB-01
- **Title:** Scheduled drift scan: recompute I1/I2 from a consistent snapshot; re-check mismatches under the obligation lock; PAGE on confirmed mismatch; verify L9
- **Classification:** MVP normative implementation
- **Purpose:** §3: the stored counter is a deliberate tripwire; the scanner is what makes the redundancy pay.
- **Prerequisites:** RG-01..03 live; S-07 (drift index); D-10 (paging integration).
- **Requirement sections / concepts to read:** §3 (drift block + invariants), §10.3 (L9), §15 (drift page).
- **Placeholder components involved:** [Reconciliation / Drift Scanner], [Metrics / Alerting Layer].
- **Local placeholder mappings required before starting:** flashback/SCN availability confirmed (P12 phase note — else the consistent-read strategy is UNCLEAR: ask the DBA; do not fake it with plain reads).
- **Local code areas to discover:** job infra.
- **How to locate:** D-08.
- **Implementation instructions:** per obligation (batched): snapshot-read (SCN/flashback) committed/confirmed vs Σ per I1/I2; mismatch → re-check UNDER the obligation lock; still mismatched → PAGE (not log); L9 check: outcome=REJECTED rows have exactly one marker set (cross-table — code check, the drift job is its verifier per §10.3); metrics per run. ALSO ship — do NOT schedule — the §6.6 accepted-window CANDIDATE diagnostic (reviews 2b697fb M1 + b1d91dc M1 + b760786 M1; corrected 4098532 M1 — delivery semantics stated exactly: SHIPPING the query + its correctness test is a REQUIRED deliverable here, but its test failing does NOT block OB-01 completion — record the failure as an EXPLICIT OPEN ITEM in the P12 handoff with deadline = before FIRST production marker-triage use; this is what keeps "NOTHING about it gates payment go-live" literally true, because OB-01 → P12 → Q19 sit on the go-live path; INVOCATION is on-demand at operator discretion; "optional" still never means "may omit the deliverable"): implement the documented query (14-observability N.1 carries the exact relational predicate + the SAFE-EXECUTION ENVELOPE: required :business_id bind, hard row limit + statement timeout, read-only with replica preferred, one-time representative EXPLAIN inspection before first production use) — obligations with a LIVE validation_failed marker → sibling payment_request rows (same business_id, different scope) with creating_ordering < the marker's validation_failed_ordering AND created_at > its validation_failed_first_at → LOWER_ORDER_SIBLING_REQUEST_AFTER_VALIDATION_MARKER_CANDIDATE (masked ids + both orderings + timestamps); CANDIDATES ONLY — manual triage, never auto-classified, never a page/gate; NOT in CA-4's standing-scan index contract (it reads historical rows by design — explicit exception; no new index, no plan contract), NOT a go-live item; covers ONLY the post-marker chronology subset — the other ratified schedule is visible via the LIVE marker on B itself (§6.6).
- **Do not change:** counters (the scanner READS; corrections are the future manual-adjustment op — §19.2).
- **Tests to add:** seeded I1 violation pages; seeded I2 violation pages; read-skew scenario does NOT page (uncommitted concurrent create); L9 violation detected; accepted-window diagnostic (query correctness only — no schedule/plan assertions, it is not a standing scan): a seeded escape-schedule window (request created after a sibling's failure anchor, below its failure ordering) IS flagged as LOWER_ORDER_SIBLING_REQUEST_AFTER_VALIDATION_MARKER_CANDIDATE; an ordinary request created BEFORE the failure is NOT flagged; output masked; the candidate is a metric/log event, never a page or gate.
- **Edge cases:** obligations mid-transaction during the sweep — the locked re-check absorbs them.
- **Manual validation:** corrupt a counter in a test env → page arrives.
- **Expected outcome:** money-math tripwire live.
- **Failure signs:** paging without the locked re-check (false pages erode trust).
- **Common mistakes:** scanning with a plain read and calling it a snapshot.
- **Completion criteria:** drift/L9 tests green; page route confirmed; the diagnostic sub-case green OR recorded as the explicit open item (4098532 M1 — its failure never blocks this card).
- **Stop condition:** merged.
- **Next task:** OB-02.

### OB-02 — Reconciliation tripwires

- **Task ID:** OB-02
- **Title:** Wire the anomaly tripwires: evidence-for-terminal CRITICAL, per-obligation request-count sanity
- **Classification:** MVP normative implementation
- **Purpose:** §8's anomaly disambiguation + §15's tripwire entries; the §5.2 replay-divergence tripwire is the same alert (post-MVP runbook consumes it).
- **Prerequisites:** IN-07 (zero-row CAS detection point).
- **Requirement sections / concepts to read:** §8 (anomaly rules), §15 (entries).
- **Placeholder components involved:** [Payment Status Feed Consumer], [Metrics / Alerting Layer].
- **Local placeholder mappings required before starting:** IN-07 in place.
- **Local code areas to discover:** none new.
- **How to locate:** n/a.
- **Implementation instructions:** evidence-for-terminal: NEW event_id + zero-row CAS against a TERMINAL row → CRITICAL (already hooked in IN-07 — verify + alert-route here); per-obligation request count over sanity threshold → ticket (§15); post-F0 NULL-stamp data-quality scan (aa4399c L1): payment_request WHERE created_at >= the F0 activation timestamp (sourced from the signed activation manifest, GO-03) AND required_total_at_creation IS NULL → LOW-severity data-quality ticket (never a page, never a gate — the stamp stays display-only; this catches a regressed secondary creation path after GO-03's one-time first-row check); post-F0 NULL-request_seq scan (4dbdf2b M1 — IDENTITY CONTRACT, higher severity): same window AND request_seq IS NULL → ALERT + ticket (a rogue or pre-fence writer creating rows outside the K-01 discipline; still never a gate on other rows). (Card lookups returning multiple obligations are the NORMAL case per §12 — result count is never a health signal; no card tripwire exists.)
- **Do not change:** benign-redelivery silent skip (KNOWN event_id — §8).
- **Tests to add:** each tripwire fires on its seeded condition; benign redelivery does NOT fire; post-F0 NULL-stamp: a seeded post-F0 NULL row raises the data-quality ticket, a pre-F0 NULL row stays silent (aa4399c L1).
- **Edge cases:** provider-side-count vs local EXECUTED comparison (Section N lists it) requires engine-side data — mark MUST_VERIFY_LOCALLY whether any engine report/API supports it; if not, record as unavailable (the §15 list does not mandate it; Section N flags it as conditional).
- **Manual validation:** seeded runs.
- **Expected outcome:** tripwires live.
- **Failure signs:** CRITICALs routed to a quiet channel.
- **Common mistakes:** aggregating the terminal-evidence CRITICAL into volume metrics (it is per-event CRITICAL, §8).
- **Completion criteria:** tests green.
- **Stop condition:** merged.
- **Next task:** OB-03.


---

## Phase handoff summary (P12 → P13)

- **Phase outputs:** I1/I2 drift scan (snapshot + locked re-check, PAGE on confirmed mismatch, L9 verification); terminal-evidence CRITICAL tripwire routed; per-obligation count sanity; the §6.6 accepted-window candidate diagnostic documented + shipped (required deliverable within OB-01; its test failure does NOT block OB-01 — it becomes an open item listed below; invocation on-demand — never a standing scan or go-live gate; reviews b1d91dc M1 + b760786 M1 + 4098532 M1).
- **Blockers to carry forward:** engine-side count comparison remains CONDITIONAL on a provider report/API (see 14-observability-reconciliation-runbooks.md N.1 note); IF the accepted-window diagnostic sub-case failed, the explicit open item rides here (deadline: before first production marker-triage use — NOT a go-live gate; 4098532 M1).
- **Local mapping rows expected filled:** [Reconciliation / Drift Scanner] CONFIRMED incl. the SCN/flashback strategy.
- **Tests expected to exist:** T-29 (seeded drift pages, skew does not; candidate-diagnostic query correctness), T-30 (tripwire fires, benign silent).
- **Next phase entry condition:** OB-02 done.
