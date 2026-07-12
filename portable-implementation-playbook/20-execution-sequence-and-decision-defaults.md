> **Purpose:** The single flat task-level execution order for a SINGLE small-context agent (no parallel tracks), with AGENT/HUMAN ownership tags and hard decision defaults for every judgment point in the cards.
> **When to use this file:** Before starting any task — take the next row; when a card says "coordinate", "helpful but not required", "if X not yet done", or "parallel" — apply the matching decision default instead of judging.
> **Depends on:** 01-playbook-index.md (BLOCKED list); 08-task-cards/ (the cards themselves).
> **Used by:** The local coding agent and its human driver; 21-progress-tracker-template.md mirrors this order.
> **Safe to transfer:** yes
> **Contains local code names:** no

# Execution sequence and decision defaults

This file changes NO card content. It linearizes the phase plan for one
executor and resolves every coordination point with a hard default.
The cards' own prerequisites still apply; if a row's prerequisite is
not met, the row is BLOCKED — stop and report, never skip ahead past a
money-affecting dependency.

**Phase-boundary gate (rule 19, adopted 2026-07-11):** cards commit to
the PHASE INTEGRATION BRANCH; the branch merges to main only when the
phase's last row is done AND (a) an integration review passed, (b) the
phase's cross-path tests are green, (c) the local mapping rows the
NEXT phase needs were refreshed (re-run that phase's discovery
searches — stale rows re-BLOCK their tasks), and (d) every
intentionally-unwired safety path recorded in this phase's reports
has a NAMED closing gate. Roughly 14 integration merges instead of
one per card.

Ownership tags:

```text
AGENT        the local coding agent executes the card
HUMAN        a human executes/decides; the agent may PREPARE material
             (drafts, question lists) but STOPS before the decision
HUMAN+AGENT  the agent drafts/implements; a named human must review,
             approve, or physically run it (sandbox, drill, sign-off)
```

## Linear task order (one row at a time, top to bottom)

| # | Task | Owner | Wait-on / gate | Card file (08-task-cards/) |
|---|------|-------|----------------|----------------------------|
| 1–12 | D-01 → D-12 (in ID order) | AGENT | none (read-only) | phase-01-discovery.md |
| — | STOP: human reviews the D-12 report before any implementation | HUMAN | D-12 | — |
| 13 | B-01 | HUMAN | none — ask immediately | phase-02-blocking-gates-and-artifacts.md |
| 14 | B-02 | HUMAN | none — ask immediately | phase-02 |
| 15 | B-03 | HUMAN | none — ask immediately | phase-02 |
| 16 | B-04 | HUMAN | B-02 answers | phase-02 |
| 17 | CA-1 | HUMAN+AGENT | B-02 channel; D-05 memo | phase-02 |
| 18 | CA-2 | HUMAN+AGENT | B-02 | phase-02 |
| 19 | CA-3 | HUMAN+AGENT | B-02 | phase-02 |
| 20 | CA-4 | HUMAN+AGENT | scope key settled (§1 contract facts — B-01 residue does not gate) | phase-02 |
| 21 | CA-5 | HUMAN+AGENT | scope key settled (§1 contract facts — B-01 residue does not gate) | phase-02 |
| 22 | CA-6 | HUMAN+AGENT | CA-5; D-05 | phase-02 |
| 23 | CA-7 | HUMAN+AGENT | none | phase-02 |
| 24 | CA-8 | HUMAN+AGENT | Section-N seeds (14-observability file) | phase-02 |
| 25 | CA-9 | HUMAN+AGENT | B-04; CA-4 | phase-02 |
| 26–34+34a | P3 — EXACT ORDER (round 9, normative; S-11 RETIRED round 10 — greenfield): S-01, S-02, S-03, S-04, S-10, S-05, S-06, S-07, S-08, S-09 | AGENT (S-01 plan + S-08 map need HUMAN approval) | CA-4 published (scope key settled, §1 contract facts); S-10 = §2.4 table (round 5, after S-04); S-09 proof pass runs LAST | phase-03-schema-and-migration.md |
| 35–40 | K-01 → K-06 | AGENT | S-09 green; CA-5 for K-02/K-03 | phase-04-identity-and-idempotency.md |
| 41–47 | CT-01 → CT-07 | HUMAN+AGENT | B-02 sandbox access; K-02/K-03 (see DD-6 if access missing) | phase-08-provider-contract-tests.md |
| 48–50 | U-01 → U-03 | AGENT | S-03; K-04 path | phase-05-uetr-response-persistence.md |
| 51–61 | ST-01 → ST-11 | AGENT | S-08/S-09 | phase-06-factored-state-model.md |
| 62–71 | RG-01 → RG-10 | AGENT | P6 done; DD-2/DD-3 apply | phase-07-reservation-and-release-guards.md |
| 72–80 | IN-01 → IN-09 | AGENT | P6/P7 done; DD-2/DD-3 reuse rules | phase-09-inbound-flows-and-status-feed.md |
| 81–90 | RC-01 → RC-10 | AGENT | CA-1/CA-3; DD-4 stubs | phase-10-retry-recovery-maybe.md |
| — | RETURN: close pending named cases per DD-7 | AGENT | RC-07/RC-08 done | phase-06 / phase-07 / phase-05 files |
| 91 | OP-01 | AGENT | CA-9; S-06 | phase-11-operator-verified-outcome.md |
| 92 | OP-02 | AGENT | OP-01 | phase-11 |
| 93 | OP-03 | HUMAN | OP-02 green; real operators | phase-11 |
| 93a | OP-04a | AGENT | OP-02 green; RG-05; RG-06 (round 9 pre-split: shared contract + retry/reject/annotate) | phase-11 |
| 93b | OP-04b | AGENT | OP-04a; IN-02 tie record + fetch path + §6.1 admission gate verified (reprocess approval side) | phase-11 |
| 93c | OP-04c | AGENT | OP-04b; S-10/IN-02 admission + fence live (reprocess execution) | phase-11 |
| 93d | OP-04d | AGENT | OP-04a; S-07 indexes (queue views) | phase-11 |
| 93e | OP-04e | AGENT | OP-04a..d merged (cross-path suite + Q29 evidence) | phase-11 |
| 94 | OB-01 | AGENT | RG money paths live | phase-12-drift-reconciliation.md |
| 95 | OB-02 | AGENT | IN-07 | phase-12 |
| 96–100 | OB-03 → OB-07 | AGENT | metric sources from P6–P12 | phase-13-observability-and-runbooks.md |
| 101 | GO-01 | HUMAN+AGENT | all merges | phase-14-rollout-and-go-live.md |
| 102 | GO-02 | AGENT | dual-write live in env | phase-14 |
| 103 | GO-03 | HUMAN+AGENT | GO-02 clean; stage F4 needs CT PASS (DD-6) | phase-14 |
| 104 | GO-04 | HUMAN | all gates; 17-go-live-checklist.md | phase-14 |
| 105 | GO-05 | HUMAN+AGENT | GO-01 stages | phase-14 |

## Decision defaults (apply mechanically — do not re-judge)

```text
DD-1  K-04 vs ST-09 (claim shape): execute K-04 as written when you
      reach it. ST-09 later converts the claim to the standard lease
      CAS; K-04's persist-then-commit-then-wire ordering and its tests
      must stay green through that conversion. Do not wait for ST-09.

DD-2  RG-08 vs IN-04 (marker liveness): in THIS order RG-08 comes
      first. Implement the marker LIVENESS predicate inside RG-08 as
      ONE shared helper. When you later reach IN-04, EXTEND that same
      helper with the monotonic WRITE rules and counters. Never create
      a second liveness predicate.

DD-3  RG-06 vs IN-02 (trigger T1): implement evaluate() in RG-06 wired
      to triggers T2/T3/T4. The T1 wiring (ordering advance) is added
      when you reach IN-02. Record "T1 pending IN-02" in the tracker
      when finishing RG-06.

DD-4  RC-03 stubs (round 11 sweep: no cutoff term exists;
      no cutoff stub may be built — the round-10 closure is total):
      until RC-09 is done, the freeze term reads a stub that always
      answers FROZEN. Replace the stub in RC-09 and re-run RC-03's
      term-by-term tests.

DD-5  U-03 feed-side case: defer the feed-event-under-dead-UETR case
      to IN-06 as a pending named case in the tracker. Do not build a
      throwaway feed harness at U-03 time.

DD-6  CT block placement: run CT-01..CT-07 immediately after K-06 IF
      sandbox access (B-02) exists. If not, continue with U-01 onward
      and run the CT block as soon as access arrives. HARD GATE either
      way: CT-02..CT-05 must be PASSED before GO-03 stage F4
      (auto-downgrade) and before checklist items Q2/Q10 can PASS.
      Never interleave CT tasks with implementation tasks.

DD-7  Pending named cases — close them at these points, then update
      the tracker:
        after RC-07: ST-03's §9.2 downgrade legality case; RG-07's
                     park-stability assert.
        after RC-08: ST-06's frozen-row-excluded-from-MAYBE-scans case.
        after IN-05/IN-06: U-03's deferred feed-side case (DD-5).

DD-8  HUMAN-tagged rows: the agent may draft questions, seed test
      plans, or prepare scripts, then STOPS. The agent never answers a
      §18 question itself, never runs the drill, never signs a gate.

DD-9  Catch-all: if a card says "coordinate", "choose", or offers an
      alternative NOT covered by DD-1..DD-8, stop and ask the human.
      Record the answer in the tracker notes. Do not improvise.
```
