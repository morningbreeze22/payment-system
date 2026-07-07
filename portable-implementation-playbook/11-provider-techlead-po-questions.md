> **Purpose:** Provider / tech-lead / PO contract questions Q-01..Q-20 with priorities (original Section K).
> **When to use this file:** When engaging provider/tech-lead/PO/upstream; before any gate decision; keeping the risk register current for checklist item Q25.
> **Depends on:** requirment-v4.md section 18; 03-requirement-classification.md.
> **Used by:** B-01..B-04; CT suite configuration; 17-go-live-checklist.md Q25.
> **Safe to transfer:** yes
> **Contains local code names:** no

# K. Provider / tech-lead / PO contract questions

Priorities: BLOCKING (go-live gate) / HIGH (load-bearing config or
safety margin) / MEDIUM (correctness of a secondary path) / LOW
(convenience) / FUTURE (post-MVP). Every answer is recorded verbatim
with source and date; §18-1-family answers close ONLY via the CT
tests.

| ID | Priority | To | Question | Consumed by |
|----|----------|----|----------|-------------|
| Q-01 | BLOCKING | Upstream + UI teams | §18-0: payments-per-trade — "one payment case per business payment" or "one trade can have multiple payments"? The obligation scope key, §5.1 identity derivation, and §12 card lookup are at stake; if multiple, which discriminator (ui_step_instance_id / upstream payment sequence)? | B-01 → S-xx, K-02, CA-4/5, IN-02 |
| Q-02 | BLOCKING | Provider (by sandbox test) | §18-1(a): can a known idempotency key + IDENTICAL payload ever execute twice? | CT-02 |
| Q-03 | BLOCKING | Provider (by sandbox test) | §18-1(b)/TL-4: can a known key + DIVERGENT payload execute? Is the rejection code distinguishable from plain DUPLICATE_REQUEST? | CT-03, CA-1, §7.2 branch |
| Q-04 | BLOCKING | Provider | §18-1(c): key-retention TTL IN WRITING; is TTL ≥ max row lifetime incl. ops-queue SLA, weekends, holidays, incidents, cutoff constraints? Verified at the retention edge? | CT-04; repost_permitted TTL term if short |
| Q-05 | BLOCKING | Provider (by sandbox test) | §18-1(d)/TL-6: after a synchronous business rejection, does a same-key re-POST re-execute or replay the cached rejection? | CT-05; §7.1 retry policy |
| Q-06 | BLOCKING | Calendar owner / PO | §18-2: payment cutoff calendar — source system, named owner, per-currency/market + holiday semantics, tz-aware representation, refresh cadence, stale-calendar fail direction? | B-03 → RC-03/04, §9.2 lookback guard |
| Q-07 | BLOCKING | Platform + tech lead | §18-3: is the apply-platform-verified-outcome procedure the confirmed MVP exit (default), or are TL-10 AND TL-5-lookback both affirmed in writing (the only de-scope condition, PO re-confirmation required)? | B-04 → OP-xx |
| Q-08 | HIGH | Provider | Does the engine return a collision / duplicate / prior-outcome signal distinct enough to drive §7.2's three duplicate-family branches? Full error-code list for CA-1, incl. the replay-original-response class. | CA-1, RC-01/02 |
| Q-09 | HIGH | Provider | Are rejected outcomes retained and queryable via the status-query API, and for how long? | CA-3, RC-06, T-13 |
| Q-10 | HIGH | Provider | Query lookback duration — is it ≥ the maximum possible local MAYBE-row lifetime including ops-queue SLA, weekends, holidays, incidents, cutoff constraints (TL-5 framing — parked rows live days)? | §9.3 resolve-via-query viability, §18-3 alternative |
| Q-11 | HIGH | Provider | Maximum ingest lag between POST acceptance and query visibility, as a DISTRIBUTION (p50/p99/max) — sets NOT_FOUND_TRUST_AGE (§9.2). If no contractual bound: state so; trust-age set conservatively + §15 observed-lag watchdog carries the residual. | OB-07 config, RC-07 |
| Q-12 | HIGH | Provider / platform | TL-10: can the platform formally REJECT a pending/never-received payment by UETR (or by idempotency key for rows that never received one) so the negative flows back as authoritative feed/query evidence? | §9.3 ops exits; cleaner path than the procedure |
| Q-13 | HIGH | Provider / SDK team | TL-11: (a) does the validate-and-POST response return the generated UETR, and in which field? (b) does the SDK accept our caller-supplied idempotency key? (c) does engine dedup key on that caller key (not the UETR)? — (c) is blocking-grade. | CT-07, U-01, K-04 |
| Q-14 | HIGH | Provider | Whether the engine deduplicates by caller-supplied idempotency key, UETR, both, or neither — asked explicitly even though (c) above implies it; the answer must be verified by CT-02/CT-07, not accepted in writing alone. | §5, CT suite |
| Q-15 | HIGH | Provider | Status-query API rate limit / quota — sizes the §9.5 sweep budget (TL-13; as load-bearing as ingest lag). | RC-05, OB-07 |
| Q-16 | HIGH | Upstream | Upstream asks 1–4: strictly-increasing ordering per business_id (until the explicit sequence field); business_id as the Kafka message key BY CONTRACT; the §6.0 schema formalized (field names incl. the ordering field, types, correlation_id); emission only on real business change (no blind re-emissions). | IN-01/02, §6.7 tie handling, §6.6 anchoring (TL-7) |
| Q-17 | MEDIUM | Provider | TL-12: provider_reference uniqueness scope and lifetime (global? per day/batch/rail?). Until confirmed globally unique, §8's fail-closed fallback stands; if confirmed, guards may be relaxed by explicit decision. | IN-06, U-02 |
| Q-18 | MEDIUM | Provider | TL-1: does the status feed carry a stable, unique event_id per event? If not: choose synthesis (payload hash vs topic+partition+offset) and accept its dedup blind spots. | CA-2, IN-05 |
| Q-19 | MEDIUM | Tech lead / UI team | TL-2: card read contract — query API vs replica/view, field list (step timestamps; retry progress "next attempt at / attempt N of M"), freshness SLA incl. replica lag, authentication, volume. And PO-5: step display for a trade cancelled after the step started (currently "completed" — acceptable?). | §12 read surface, OB-04 lag indicator |
| Q-20 | MEDIUM | PO / tech lead | Remaining §18 sign-offs: PO-1 ask-then-retry approval; PO-2 query cadence (suggest 2m); PO-3 escalation age (suggest 30m, must clear cutoff); PO-4 cutoff-passes-while-MAYBE behavior; PO-6 deferred-successor latency acceptance; PO-8 fresh-assembly consequence acceptance; TL-3 RPO/RTO + §5.2 runbook ownership (post-MVP); TL-8 confirmation-age owner+value; TL-9 artifact owners; TL-14 terminal-row archival co-design; TL-15 first-quarter NOT_FOUND-after-trust-age measurement. §19.3/PO-7 (retry-after-reject) = FUTURE. | OB-07 config owners; Section Q risk register |

