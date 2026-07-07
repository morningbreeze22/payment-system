> **Purpose:** Minimal context packets CT-01..CT-07 — paste-alone briefs for a small-context local agent (original Section I, phase P8).
> **When to use this file:** Paired with the matching task-card file 08-task-cards/08-provider-contract-tests.md — one packet per task, used as the working brief.
> **Depends on:** 09-minimal-context-packets/README.md; the matching task card; the requirement sections each packet cites; 07-placeholder-glossary.md.
> **Used by:** The local coding agent executing phase P8.
> **Safe to transfer:** yes
> **Contains local code names:** no

# Minimal Context Packets — Phase P8

```text
[CT-01] Sandbox harness
Read: §18-1 (intro+matrix) §1 (assumed facts). Invariant: tests use the REAL derivation + serialization or the proof is void.
Placeholders: [Contract Test Suite] [Provider POST Client]. Mappings: sandbox credentials (vaulted).
Objective: runnable suite (excluded from default CI): POST helpers via real identity path; evidence capture (timestamped, engine-versioned).
Tests: smoke POST. Stop: smoke green.
```

```text
[CT-02] Identical-payload re-POST
Read: §18-1(a) §7.0 §16.6-1. Invariant: nothing executes twice; the second response's class feeds CA-1 (dedup vs original-replay).
Placeholders: [Contract Test Suite]. Mappings: harness.
Objective: POST, re-POST byte-identical, assert single execution engine-side, classify + file evidence.
Tests: the run. Stop: result recorded. Double execution → STOP ALL re-POST reliance, escalate.
```

```text
[CT-03] Divergent-payload re-POST
Read: §18-1(b) TL-4 §7.2 §5.1. Invariant: never executed; rejection code distinguishable from plain DUPLICATE_REQUEST.
Placeholders: [Contract Test Suite]. Mappings: harness.
Objective: re-POST with changed business field and (separately) changed amount; assert no execution; capture + compare codes; file into CA-1.
Tests: two variants. Stop: recorded. Execution → STOP, escalate (TL-4 payload-freeze clause is a human decision).
```

```text
[CT-04] TTL edge
Read: §18-1(c) §7.0 §9.3. Invariant: a key aged out of the dedup store executes a duplicate — TTL vs max row lifetime decides a repost_permitted TTL term.
Placeholders: [Contract Test Suite]. Mappings: harness; written TTL.
Objective: re-run (a)/(b) at the achievable retention edge; compare TTL vs max lifetime incl. ops SLA; record the RC-03 follow-up if TTL is short.
Tests: edge runs (provider-assisted acceptable, documented). Stop: evidence + consequence note filed.
```

```text
[CT-05] Re-POST after sync business reject
Read: §18-1(d) TL-6 §7.1. Invariant: either answer is handled but must be KNOWN by test, not by documentation.
Placeholders: [Contract Test Suite]. Mappings: harness; inducible business reject.
Objective: induce reject; re-POST same key; record re-executes vs replays; if replays → record the RC-04 policy consequence (fresh successor via §6.8).
Tests: the run per retryable class. Stop: recorded.
```

```text
[CT-06] Query mapping verification
Read: §9.1 §9.2 (four causes); CA-3. Invariant: CA-3 verified empirically; never-sent key → NOT_FOUND observed.
Placeholders: [Contract Test Suite] [Status Query Resolver]. Mappings: harness + query client.
Objective: query executed/rejected/never-sent/accepted cases; opportunistic ingest-lag observations; feed findings to CA-3's owner.
Tests: four runs. Stop: recorded; owner sign-off.
```

```text
[CT-07] SDK contract (TL-11)
Read: §5 (chain+rules) TL-11. Invariant: engine dedup keys on the CALLER key even under a fresh SDK-minted UETR — blocking-grade.
Placeholders: [Contract Test Suite] [Provider POST Client]. Mappings: harness.
Objective: verify (a) UETR field in acceptance response; (b) caller key transmitted; (c) dedup by key despite fresh UETR.
Tests: three checks. Stop: recorded; §18-1 summary updated. SDK rejects caller keys → STOP, escalate.
```

