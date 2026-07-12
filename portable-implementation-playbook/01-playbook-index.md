> **Purpose:** The compact index: sections, phases, stable task IDs, dependency order, BLOCKED tasks, go-live blockers, and the execution-order table (original Playbook Index).
> **When to use this file:** With EVERY task — it is part of the minimal reading set; consult the BLOCKED list before starting any card.
> **Depends on:** 00-README.md.
> **Used by:** Everything — the navigation spine of the package.
> **Safe to transfer:** yes
> **Contains local code names:** no

# PLAYBOOK INDEX (compact — task IDs are stable throughout)

## Section list

```text
A  Executive summary
B  Assumptions and non-goals
C  Requirement extraction and classification
D  Implementation dependency graph
E  Recommended implementation phases (P1–P14)
F  Local source-code discovery workflow
G  Placeholder component glossary
H  Small executable task cards (grouped by phase)
I  Minimal context packets (one per task card)
J  Test-first strategy and test matrix (T-xx)
K  Provider / tech-lead / PO contract questions (Q-xx)
L  Companion artifact plan (CA-1 … CA-9)
M  Migration / rollout / rollback plan
N  Observability, reconciliation, and runbook plan
O  Local-only placeholder mapping template
P  Instructions for the local coding agent
Q  Go-live readiness checklist
R  Playbook quality self-check + task execution report template
```

## Phase list

```text
P1   Local codebase discovery only                          (tasks D-xx)
P2   §18 BLOCKING gate resolution + companion artifacts     (tasks B-xx, CA-x)
P3   Schema and migration foundation                        (tasks S-xx)
P4   Deterministic idempotency key generation/persistence   (tasks K-xx)
P5   SDK-assigned UETR response persistence / feed matching (tasks U-xx)
P6   Factored request lifecycle + state-machine hardening   (tasks ST-xx)
P7   Reservation / obligation consistency + release guards  (tasks RG-xx)
P8   Provider idempotency sandbox contract test suite       (tasks CT-xx)
P9   Upstream and feed contract handling                    (tasks IN-xx)
P10  Retry / recovery / MAYBE_SUBMITTED resolver            (tasks RC-xx)
P11  MVP apply-platform-verified-outcome operation + interim ops surface (tasks OP-xx)
P12  Reconciliation / drift scanner / tripwires             (tasks OB-01..02)
P13  Observability, alerts, and runbook stubs               (tasks OB-03..07)
P14  Migration, rollout, rollback, and go-live gates        (tasks GO-xx)
```

## Task ID list

```text
P1  Discovery:        D-01 D-02 D-03 D-04 D-05 D-06 D-07 D-08 D-09 D-10 D-11 D-12
P2  Gates/artifacts:  B-01 B-02 B-03 B-04
                      CA-1 CA-2 CA-3 CA-4 CA-5 CA-6 CA-7 CA-8 CA-9
P3  Schema:           S-01 S-02 S-03 S-04 S-10 S-05 S-06 S-07 S-08 S-09
P4  Identity:         K-01 K-02 K-03 K-04 K-05 K-06
P5  UETR:             U-01 U-02 U-03
P6  State model:      ST-01 ST-02 ST-03 ST-04 ST-05 ST-06 ST-07 ST-08 ST-09 ST-10 ST-11
P7  Reservation:      RG-01 RG-02 RG-03 RG-04 RG-05 RG-06 RG-07 RG-08 RG-09 RG-10
P8  Contract tests:   CT-01 CT-02 CT-03 CT-04 CT-05 CT-06 CT-07
P9  Inbound flows:    IN-01 IN-02 IN-03 IN-04 IN-05 IN-06 IN-07 IN-08 IN-09
P10 Retry/recovery:   RC-01 RC-02 RC-03 RC-04 RC-05 RC-06 RC-07 RC-08 RC-09 RC-10
P11 Operator proc:    OP-01 OP-02 OP-03
P12 Drift:            OB-01 OB-02
P13 Observability:    OB-03 OB-04 OB-05 OB-06 OB-07
P14 Rollout:          GO-01 GO-02 GO-03 GO-04 GO-05
Tests (Section J):    T-01 … T-32
Questions (Section K): Q-01 … Q-20
```

## Dependency order (phase-level; details in Section D)

```text
P1 → P2 → P3 → P4 → P5 → P6 → P7 → P9 → P10 → P11 → P12 → P13 → P14
                 └──────────── P8 runs in parallel from P4 onward;
                               its PASS gates P10 auto-downgrade
                               reliance and gates go-live (§18-1)
```

## BLOCKED tasks (unsafe before a §18 BLOCKING decision)

```text
BLOCKED on §18 item 0 residue (snapshot contract — task B-01):
  Per the §1 contract facts (one trade carries MULTIPLE payments;
  each message is a full-trade snapshot, newer overwrites older;
  scope tuple unique within a snapshot), the scope key needs NO
  discriminator and §5.1 identity stands — S-02/S-03/S-05/K-02/
  K-03/CA-4/CA-5 are NOT gated by this item. What it DOES gate is
  the §6 consumer freeze (IN-02):
    - written upstream confirmation (upstream ask 5) of the snapshot
      schema + within-snapshot uniqueness
    - within-snapshot uniqueness intake validation (§6.0)
    - PO-9 ANSWERED 2026-07-11 (absence = amendment to zero; BA-2
      amended §1.1) and TL-16 ANSWERED round 5 (§6.1 admission +
      §2.4) — IN-02 implements both; the remaining B-01 residue is
      the WRITTEN filing of asks 5 + 8 (confirmed verbally)
    - §12 card lookup rewrite (returns ALL obligations of the trade;
      step-granularity clause added to TL-2)

BLOCKED on §18 item 1 (collision contract proof — tasks CT-01..05):
  Nothing at implementation time (the design carries no runtime gate —
  §1 assumed contract facts); but GO-04 (go-live) is BLOCKED until
  CT-02..CT-05 pass in sandbox.

§18 item 2 (cutoff calendar): CLOSED 2026-07-11 — the ENGINE owns
  its calendar; no local cutoff config, interface, or B-03 sourcing
  work exists (B-03 rescoped to recording the fact + the CA-1
  late-submission ask).

BLOCKED on §18 item 3 (MAYBE terminal exit — task B-04):
  OP-01..OP-03 implement the DEFAULT resolution (the audited
  operation — an authorized application endpoint). GO-04 is BLOCKED until OP-03 (drill) passes OR the
  stated alternative (TL-10 + TL-5 lookback ≥ max row lifetime) is
  affirmatively answered.
```

## Go-live blockers (full checklist in Section Q)

```text
1. §18 item 0 residue closed: written snapshot-contract confirmation
   (upstream ask 5 — confirmed, paper pending), §6.0 intake
   validation (PO-9 + TL-16 both ANSWERED; B-01 files the papers)
2. §18 item 1 sandbox proof executed and PASSED (CT-02..CT-05)
3. §18 item 2 CLOSED (engine owns the calendar — round 10)
4. §18 item 3 apply-platform-verified-outcome operation EXISTS and is
   DRILLED (OP-01..03) — or the stated alternative fully satisfied
5. Identity golden-vector tests pass (K-03, CA-5)
6. Duplicate-prevention + crash/restore retry tests pass (Section J)
7. Observability + runbook stubs live (P13); rollout plan approved (P14)
```


## Execution order (phase-level)

| Phase | Tasks | Prerequisites | Blockers (§18 / other) | Output artifact | Next phase |
|---|---|---|---|---|---|
| P1 Discovery | D-01..D-12 | none | none (read-only) | filled local mapping + D-12 report | P2 (after human review) |
| P2 Gates + artifacts | B-01..B-04, CA-1..CA-9 | D-12 report | §18-0..3 are THE work here | recorded answers + CA-1..9 published | P3 (needs CA-4; B-01 residue does NOT gate — see BLOCKED list above) |
| P3 Schema | S-01..S-10 | CA-4 published (scope model settled as a §1 contract fact — B-01 residue NOT required; it gates the §6 consumer freeze IN-02, not schema) | §18-0 gates IN-02, not this phase | schema at target + S-09 proof | P4 |
| P4 Identity | K-01..K-06 | S-09; CA-5 | §18-0 (via CA-5) | deterministic identity + golden vectors | P5 (P8 may start) |
| P5 UETR | U-01..U-03 | S-03; P4 claim path | TL-11(a) if unclear | acceptance-only UETR rules | P6 |
| P6 State model | ST-01..ST-11 | P3; S-08 backfill | none new | factored model + CAS + leases | P7 |
| P7 Reservation | RG-01..RG-10 | P6 | none new | money choreography + derivation + guards | P9 (P8 parallel) |
| P8 Contract tests | CT-01..CT-07 | B-02; K-02/K-03 | §18-1 — this phase IS the proof | §18-1 evidence pack | gates GO-03 F4 + GO-04 |
| P9 Inbound | IN-01..IN-09 | P6, P7 | upstream asks open → comparator pluggable | hardened intake + feed + evidence helper | P10 |
| P10 Retry/recovery | RC-01..RC-10 | P6, P7, P9; CA-1/CA-3 | P8 PASS gates auto-downgrade ENABLEMENT (§18-2 closed round 10) | resolver machinery | P11 |
| P11 Operator ops | OP-01..OP-03, OP-04a–e (round-9 pre-split) | CA-9; S-06; P6/P7 | §18-3 — this phase satisfies it | operation + signed drill + §20 interim surface | P12 |
| P12 Drift | OB-01..OB-02 | P7 live; S-07 | none new | drift scan + tripwires | P13 |
| P13 Observability | OB-03..OB-07 | P6-P12 metric sources | config owners pending → marked | §15 alert surface + config validation | P14 |
| P14 Rollout | GO-01..GO-05 | ALL phases; P8 PASS; OP-03 | §18-0..3 non-waivable at Q1-Q4/Q28 | live system + signed go/no-go | steady state |
