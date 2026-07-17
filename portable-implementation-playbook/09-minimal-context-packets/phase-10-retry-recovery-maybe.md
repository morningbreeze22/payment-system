> **Purpose:** Minimal context packets RC-01..RC-10 — paste-alone briefs for a small-context local agent (original Section I, phase P10).
> **When to use this file:** Paired with the matching task-card file 08-task-cards/10-retry-recovery-maybe.md — one packet per task, used as the working brief.
> **Depends on:** 09-minimal-context-packets/README.md; the matching task card; the requirement sections each packet cites; 07-placeholder-glossary.md.
> **Used by:** The local coding agent executing phase P10.
> **Safe to transfer:** yes
> **Contains local code names:** no

# Minimal Context Packets — Phase P10

```text
[RC-01] POST classifier
Read: §7.2 (whole) §7.3 §7.1; CA-1. Invariant: closed taxonomy; unmapped mid-call → MAYBE·CONFIRM·READY; unmapped code → MAYBE·BLOCKED(UNMAPPED_CODE); 200 classified from body.
Placeholders: [Provider Response Parser]. Mappings: parser.
Objective: data-driven classifier from CA-1 (externalized); fail-closed defaults; enrichment outcomes via §7.3.
Tests: fixture per CA-1 row + defaults. Stop: merged.
```

```text
[RC-02] Response-driven transitions
Read: §7.2 (every row) §10.5 (POST rows) §2.2 (divergent_payload_at) §7.1. Invariant: collision sets divergent_payload_at write-once then branches on the CLAIM-TIME divergence_expected flag; rejects never write uetr; marker totality on REJECTED.
Placeholders: [Provider Response Parser] [Request Status Persistence Layer]. Mappings: RC-01 wired.
Objective: implement each §7.2 row as its exact tuple CAS + side effects (anchors, markers, release on definitive rejects). §14.1 rider: ATTEMPT_RESOLVED insert (outcome = §7.2 class verbatim) in the SAME transaction, only on rowCount==1.
Tests: one per row; write-once; mirror rule; totality; T-38 (RESOLVED iff rowCount 1). Stop: merged.
```

```text
[RC-03] repost_permitted
Read: §7.0 (predicate + both ends + override) §6.4 §11. Invariant: ONE function; checked by every POST-routing writer AND the posting claim; blocked_reason plays no part; override = staleness term only, dual-control.
Placeholders: [Request Status Persistence Layer] [Retry Resolver Job] [Status Query Resolver] [Provider POST Client]. Mappings: claim site; writer list.
Objective: divergent_payload_at IS NULL ∧ ¬(stale ∧ MAYBE) ∧ freeze OFF ∧ outcome IS NULL (round 10: NO cutoff term); wire both ends.
Tests: term-by-term; both-ends (laundered reason can't re-POST); override scope. Stop: merged.
```

```text
[RC-04] Retry scanner
Read: §7.4 (whole — bound = MAX ATTEMPTS, round 10; engine owns the calendar) §16.1 (scanner + clock semantics + poison) §16.6; mechanics M5. Invariant: the DB scanner is the ONLY retry owner on the POST; §11 claim protocol (lock-free select → obligation-first claim CAS); while frozen/breaker-OPEN scanners make ZERO attempts (structural — nothing wired to retry_deadline_at); no cutoff checks exist.
Placeholders: [Retry Resolver Job] [Metrics / Alerting Layer]. Mappings: job infra; S-07 expressions; stacked-retry inventory (remove).
Objective: breaker-gated bounded claims; per-class policy; exhaustion → BLOCKED (MAYBE rows keep submission_state); downgrade class (reset, now, small max); poison cap.
Tests: schedule math; exhaustion-with-MAYBE; simulated 6h outage → zero attempts + zero BLOCKED conversions; poison cap. Stop: merged.
```

```text
[RC-05] Resolver sweep
Read: §9.5 (whole) §9 intro §16.6. Invariant: scope = ACTIVE ∧ (MAYBE any-stage/state ∪ SUBMITTED older than confirmation age), NEVER stage/history-scoped; MAYBE branch never damps; sweeps never overlap.
Placeholders: [Status Query Resolver] [Metrics / Alerting Layer]. Mappings: query client; job infra.
Objective: prioritized bounded sweep (oldest maybe_since first — round 10: no cutoff knowledge), per-row next_query_at backoff, budget from rate limit, overrun metric, SUBMITTED damping vs feed-lag, ops-triggered explicit-key mode.
Tests: scope table; budget under herd; overlap; damping. Stop: merged.
```

```text
[RC-06] Query-outcome application
Read: §9.1 §9.4 (race) §4.4. Invariant: applied via the SAME shared evidence helper as the feed; INDETERMINATE ⇒ reschedule; clocks never pause during query outages.
Placeholders: [Status Query Resolver] [Request Status Persistence Layer]. Mappings: IN-07 helper.
Objective: EXECUTED/REJECTED/ACCEPTED/INDETERMINATE handling; NOT_FOUND → RC-07.
Tests: each outcome; resolver-vs-feed race both orders; outage → INDETERMINATE. Stop: merged.
```

```text
[RC-07] Trust-age + downgrade
Read: §9.2 (whole) §7.4 (downgrade class) §10.5. Invariant: age from last_post_attempt_at (MAYBE) / submitted_at (SUBMITTED), never state_changed_at; downgrade fires ONLY where repost_permitted passes; SUBMITTED NOT_FOUND → ENGINE_INCONSISTENCY park (single answer, reversible), NEVER a downgrade.
Placeholders: [Status Query Resolver] [Request Status Persistence Layer]. Mappings: RC-03/RC-04 in place.
Objective: pre-trust-age → INDETERMINATE; MAYBE+permitted → POST·RETRY_WAIT·MAYBE (now, reset, reason cleared); gate-fail → parked (resolver applies deferred AMENDMENT_PARKED for stale-amount unparked rows); SUBMITTED → park, stays in scope.
Tests: anchors; downgrade tuple; each gate-fail; deferred park; SUBMITTED reversibility; DUPLICATE answer to downgrade re-POST. Stop: merged.
```

```text
[RC-08] Escalation
Read: §9.3 (whole) §2.2 (escalated_at) §13 §16.6 (ordering). Invariant: fires once per MAYBE episode (escalated_at IS NULL gate); already-BLOCKED/CLAIMED rows: alert only, never overwrite blocked_reason; tier-2 on the same maybe_since clock.
Placeholders: escalation scanner, [Metrics / Alerting Layer]. Mappings: scanner infra; S-07 index.
Objective: scope outcome IS NULL ∧ MAYBE ∧ maybe_since over threshold → CRITICAL always; state write BLOCKED(ESCALATED)+escalated_at only if unescalated ∧ non-CLAIMED ∧ non-BLOCKED; tier-2 re-page.
Tests: once-per-episode (no downgrade⇄escalate cycle); alert-only paths; tier-2; frozen rows excluded. Stop: merged.
```

```text
[RC-09] Posting freeze
Read: §16.1 (freeze block) §15 (freeze page — later). Invariant: absent/unreachable/timeout = FROZEN; only FROZEN cached; checked before every claim AND every POST; stops POSTs only (feed/query/reads continue).
Placeholders: [Provider POST Client] [Retry Resolver Job], Hazelcast. Mappings: grid client; toggle shape.
Objective: bounded-timeout fail-safe read; toggle carries reason/operator/ticket; freeze-effective metric exposed.
Tests: three fail-safe conditions; no unfrozen caching; frozen blocks claim+POST; resolver unaffected. Stop: merged.
```

```text
[RC-10] Breakers + structural outage safety
Read: §16.1 (breaker/clock semantics/bulkheads/timeouts) §7.4 (bound = max attempts, round 10) §16.6. Invariant: business rejects are breaker SUCCESSES; scanners gate on breaker; while OPEN/frozen scanners make ZERO attempts (structural — no suspension mechanism exists, nothing wired to retry_deadline_at); per-dependency breakers + timeouts.
Placeholders: [Provider POST Client] [Retry Resolver Job] [Status Query Resolver] [Metrics / Alerting Layer]. Mappings: breaker conventions.
Objective: breakers per dependency; scanner gating; VERIFY zero attempts + zero BLOCKED conversions across an OPEN window; bulkhead verification.
Tests: reject-as-success; zero claims while OPEN; attempt_count unchanged across a simulated 6h outage; query-breaker → INDETERMINATE. Stop: merged. NOTE: auto-downgrade production enablement stays gated on P8 PASS.
```

