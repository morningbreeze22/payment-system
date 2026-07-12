> **Purpose:** Task cards OB-03..OB-07 (the §15 alert surface, rollup, runbook wiring, config validation) (original Section H, phase P13).
> **When to use this file:** When executing the tasks of this phase, one card at a time, with the matching packet file from 09-minimal-context-packets/.
> **Depends on:** 08-task-cards/README.md; 01-playbook-index.md; 07-placeholder-glossary.md; the requirement sections cited per card; the locally filled mapping template.
> **Used by:** The local coding agent executing phase P13.
> **Safe to transfer:** yes
> **Contains local code names:** no

## H-Phase 13 — Observability (P13)

### OB-03 — Money + MAYBE alert set

- **Task ID:** OB-03
- **Title:** Implement the §15 money-critical alerts: MAYBE ages, stuck reservation, marker alerts, latch alerts, mismatch/inconsistency CRITICALs, procedure-use alert
- **Classification:** MVP normative implementation
- **Purpose:** the money-facing half of §15, on the correct clocks.
- **Prerequisites:** ST-07 (anchors), IN-04 (counters), RG-04 (latch hook), RC-08 (escalation events), OP-01 (procedure alert hook).
- **Requirement sections / concepts to read:** §15 (list + clock discipline), §13 (severities), §2.1/§2.2 (anchor columns).
- **Placeholder components involved:** [Metrics / Alerting Layer].
- **Local placeholder mappings required before starting:** metrics stack conventions (D-10).
- **Local code areas to discover:** none new.
- **How to locate:** n/a.
- **Implementation instructions:** implement, each on its named clock/scope: oldest-MAYBE age (maybe_since) alert on the age threshold (round 10 — no cutoff exists); payment-DISAPPEARANCE metric + mandatory log line (round 11, §15: business_id, zeroed scope tuples, doc.ordering; alert on volume — absence-as-cancellation is never silent); MAYBE tier-2 re-page; stuck-reservation age; BLOCKED count+age by blocked_reason (queue metric — display use of the reason is legal, §10.1); provider_rejected set → alert; provider_reject_count=2 → alert; validation_reject_count=3 → alert; overpay latch SET → alert (business hours); overpay-latched count + oldest age; AMOUNT_MISMATCH CRITICAL; ENGINE_INCONSISTENCY CRITICAL; AMENDMENT_TIE_CONFLICT; AMENDMENT_ON_LATCHED_SCOPE; live-marker-no-active-request age (validation_failed on first_at); apply-platform-verified-outcome executed → alert every use; overpay-latched-without-visible-exception integrity alert.
- **Do not change:** alert semantics/severities (§13/§15).
- **Tests to add:** seeded condition per alert (batchable table-driven test).
- **Edge cases:** none beyond clock discipline (already asserted in ST-07 — here assert the ALERT reads the anchor).
- **Manual validation:** dashboard walkthrough with the ops owner.
- **Expected outcome:** money alerts live.
- **Failure signs:** any age alert reading state_changed_at (final grep).
- **Common mistakes:** de-duplicating the every-use procedure alert.
- **Completion criteria:** tests green; routing confirmed.
- **Stop condition:** merged.
- **Next task:** OB-04.

### OB-04 — Queue/flow/stuck alert set

- **Task ID:** OB-04
- **Title:** Implement the flow-health alerts: unmatched events, stale messages/marker-writes, DLT depth, consumer lag, scanner heartbeats, stuck-state, sweep overrun, watchdogs, deadlocks
- **Classification:** MVP normative implementation
- **Purpose:** the flow-facing half of §15 incl. the observed-lag watchdog (ingest-lag config wrong) and generic stuck-state split rule.
- **Prerequisites:** IN-05/06 (unmatched metric), IN-02 (stale counter), IN-09 (DLT/lag), RC-05 (overrun metric), RC-07 (watchdog data).
- **Requirement sections / concepts to read:** §15 (entries incl. the stuck-state split note), §12 (freshness indicator), §16.2 (lag SLA).
- **Placeholder components involved:** [Metrics / Alerting Layer], card read path (lag indicator).
- **Local placeholder mappings required before starting:** metric sources wired by prior tasks.
- **Local code areas to discover:** none new.
- **How to locate:** n/a.
- **Implementation instructions:** unmatched feed events (volume alert); stale upstream messages volume; stale-marker-writes volume; Kafka DLT depth > 0 → page; consumer lag per flow → page over SLA + drive the §12 card data-as-of/lag indicator; scanner heartbeat (silent 3× interval → page); generic stuck-state per (stage,stage_state) max ages — split per §15 (re-keyed 2026-07-11): retry states on next_retry_at OVERDUE beyond threshold (a due row nobody claimed = scanner problem); non-churning states on state_changed_at; resolver sweep overrun (repeat → alert); observed-lag watchdog (feed-confirmed payment that was NOT_FOUND past trust-age → alert); ORA-00060 deadlock count → ticket; inbox growth vs purge → health metric; metric ABSENCE = bad (dead-gauge alerting per §15 practices).
- **Do not change:** SLA values without owners (config §16.6).
- **Tests to add:** seeded per alert where testable; dead-gauge behavior verified for at least the drift gauge.
- **Edge cases:** duplicate-skip spikes during replays must read healthy on dashboards (§15 practice — dashboard note, not an alert change).
- **Manual validation:** ops walkthrough.
- **Expected outcome:** flow alerts live.
- **Failure signs:** lag alert without the card indicator (§12 couples them).
- **Common mistakes:** stuck-state ages on the wrong clock per the split rule.
- **Completion criteria:** tests green.
- **Stop condition:** merged.
- **Next task:** OB-05.

### OB-05 — Freeze page, breaker tickets, rollup

- **Task ID:** OB-05
- **Title:** Freeze-effective-without-ticket page; breaker OPEN ticket + 30m page; root-cause alert rollup; retention-chain check
- **Classification:** MVP normative implementation
- **Purpose:** §16.1's freeze is silent by design — this page is the ONLY signal; §15 rollup: one outage = one incident, not thousands of pages.
- **Prerequisites:** RC-09 (freeze metric), RC-10 (breaker state), IN-09 (retention check hook).
- **Requirement sections / concepts to read:** §15 (freeze entry + rollup block), §16.1, §16.2 (retention chain).
- **Placeholder components involved:** [Metrics / Alerting Layer].
- **Local placeholder mappings required before starting:** alerting stack supports grouped incidents (verify locally — if not, implement rollup at emission: suppression window keyed on the root-cause condition; record approach).
- **Local code areas to discover:** none new.
- **How to locate:** n/a.
- **Implementation instructions:** page when freeze EFFECTIVE (toggle set OR grid unreachable) without an acknowledged freeze ticket (toggle carries ticket id — RC-09); breaker OPEN → ticket, page at 30m; rollup: while a root-cause condition holds (breaker OPEN, freeze active), per-row consequence alerts (escalations, tier-2, stuck tickets) aggregate into ONE grouped incident with a running count — state writes stay per-row; scheduled retention-chain check (broker retention vs inbox retention vs replay window → alert on violation); every alert definition carries its CA-8 runbook link (OB-06 completes).
- **Do not change:** per-row STATE writes (rollup is an alerting concern only — §15).
- **Tests to add:** freeze-without-ticket page; rollup groups a seeded escalation storm under breaker-OPEN into one incident; retention violation alert.
- **Edge cases:** the single genuine anomaly during an outage (a real ENGINE_INCONSISTENCY) must still be individually visible within the grouped incident (§15's 03:00 rationale) — verify the grouping preserves per-alert detail.
- **Manual validation:** simulated outage storm review.
- **Expected outcome:** outage ergonomics per spec.
- **Failure signs:** rollup suppressing CRITICALs entirely.
- **Common mistakes:** rollup implemented by dropping alerts instead of grouping.
- **Completion criteria:** tests green.
- **Stop condition:** merged.
- **Next task:** OB-06.

### OB-06 — Runbook wiring + logging practices

- **Task ID:** OB-06
- **Title:** Link every alert to its CA-8 runbook stub; enforce masking + correlation propagation; verify log retention floor
- **Classification:** MVP normative + operational runbook
- **Purpose:** §15 practices (runbook link per alert; correlation greps the whole story) + §16.3 masking + §14 retention floor.
- **Prerequisites:** CA-8 stubs; OB-03..05 alerts; ST-08 (log line).
- **Requirement sections / concepts to read:** §15 (practices), §16.3, §14 (retention floor: ≥ 90 days validated with the business).
- **Placeholder components involved:** [Metrics / Alerting Layer].
- **Local placeholder mappings required before starting:** alert definitions live.
- **Local code areas to discover:** logging encoder config (masking); MDC filters; outbound header propagation.
- **How to locate:** D-10/F.21.
- **Implementation instructions:** attach runbook links to every alert; masking in the ENCODER (debit_account masked in read model, logs, traces — §16.3; call-site discipline is not acceptable); correlation_id through MDC + outbound headers (§15); confirm log-platform retention ≥ the §14 floor (90 days validated) — below floor → report to owner (config/infra change, not code).
- **Do not change:** org log-platform config unilaterally.
- **Tests to add:** masking test (account value never appears in captured log output across the pipeline's log points); MDC propagation test through one async hop.
- **Edge cases:** stack traces allowed in LOGS only, keyed by correlation id — never in read-model fields (§12 content rules; RG-09 enforced it — re-verify here).
- **Manual validation:** one correlation id greps a full payment story (§15 practice).
- **Expected outcome:** operable alert + log surface.
- **Failure signs:** any alert without a runbook link.
- **Common mistakes:** masking only new log lines (encoder-level catches all).
- **Completion criteria:** checks green.
- **Stop condition:** merged.
- **Next task:** OB-07.

### OB-07 — Config inventory + load-order validation

- **Task ID:** OB-07
- **Title:** Externalize the §16.6 configuration inventory; loader REJECTS mis-ordered value sets
- **Classification:** MVP normative implementation
- **Purpose:** §16.6: nothing else orders trust_age/cadence/escalation/tier-2 (cutoff margin RETIRED round 10); a p99-driven trust-age quietly reaching the escalation age silently degrades wait-then-decide into everything-goes-to-ops.
- **Prerequisites:** consuming tasks landed (RC-04/05/07/08, RG-xx, IN-xx); B-02/B-03 values where available.
- **Requirement sections / concepts to read:** §16.6 (inventory + validation rule), §16.5 (externalized config).
- **Placeholder components involved:** [Metrics / Alerting Layer] (validation failure surfacing), app config.
- **Local placeholder mappings required before starting:** local config conventions.
- **Local code areas to discover:** config binding/validation infrastructure.
- **How to locate:** application properties structure.
- **Implementation instructions:** one config namespace holding every §16.6 entry (trust age, confirmation age, escalation ages, downgrade class, cadences, lease durations, retry policies, thresholds, batch sizes, retentions, sweep budget, backoff, damping, recency window, freeze propagation bound — the cutoff-calendar ref and cutoff margin were RETIRED round 10, engine-owned calendar); startup validation: reject unless trust_age + query cadence < escalation age < tier-2 age; document each entry's owner column (§16.6 — owners at kickoff; record what's known).
- **Do not change:** suggested values into hard values without owners (PO-2/PO-3/TL-8 pending — use suggestions, mark pending).
- **Tests to add:** loader rejects each mis-ordering; accepts a valid set; missing mandatory entry fails startup.
- **Edge cases:** per-error-class retry policies validated for completeness against CA-1's classes.
- **Manual validation:** config review with owners.
- **Expected outcome:** config discipline live.
- **Failure signs:** silent defaults for load-bearing values (trust age defaulting quietly).
- **Common mistakes:** validation as a warning instead of a startup rejection.
- **Completion criteria:** tests green.
- **Stop condition:** merged; Phase P13 report.
- **Next task:** GO-01.


---

## Phase handoff summary (P13 → P14)

- **Phase outputs:** full §15 alert/metric set on the correct clocks; freeze-effective page; root-cause rollup; runbook links on every alert; encoder-level masking + correlation propagation verified; §16.6 config inventory externalized with startup ordering validation.
- **Blockers to carry forward:** config values still pending owners (PO-2/PO-3/TL-5/TL-8/TL-13) are marked pending in the inventory — GO-03's F4 stage requires the TL-5-derived trust age, not the stub.
- **Local mapping rows expected filled:** [Metrics / Alerting Layer] complete.
- **Tests expected to exist:** T-32 set (per-alert seeds, rollup storm, dead gauge, config-ordering rejection), masking/MDC tests.
- **Next phase entry condition:** OB-07 done; phase report filed; observability live BEFORE rollout ramps (dependency-graph ordering #8).
