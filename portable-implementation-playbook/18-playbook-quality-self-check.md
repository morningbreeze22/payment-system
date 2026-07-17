> **Purpose:** Playbook quality self-check: the 13 authoring rules verified, plus the companion-artifact start-before summary (original Section R.1).
> **When to use this file:** After any modification to this package; during audits of the plan itself.
> **Depends on:** The whole package.
> **Used by:** Package maintainers; GO-04 audit.
> **Safe to transfer:** yes
> **Contains local code names:** no

# R. Playbook quality self-check

### R.1 Self-check against the authoring rules

```text
[x] No design review or redesign performed: all rules trace to
    requirment-v4.md sections; rejected alternatives (derived
    counter, attempt-history table, payload freeze, auto-unlatch,
    materiality re-POST, consecutive-answer counter, parked-event
    table, UETR generation) are listed as settled and guarded against
    re-introduction (Sections B, P-15, task "Do not change" fields).
[x] No new findings created: open items are exactly §18's, carried
    into Section K; UNCLEAR/MUST_VERIFY_LOCALLY markers ask for local
    or external facts, they do not challenge the design.
[x] §1.1 Basic Agreements not re-opened: BA-1/2/3 appear only as
    settled constraints (C4) with explicit do-not-build notes.
[x] No source-code names invented: all local components are Section G
    placeholders; the only concrete names used are the spec's own
    (four tables, four documented services, documented columns/
    states/artifacts).
[x] No invented persistent tables/journals/outboxes/parked-event/
    attempt-history/manual-action/audit-history tables: Section B
    non-goal 5 + P-13 SPEC_CONFLICT rule; IN-06/S-04 explicitly
    guard the parked-event trap. The two rule-13 ops-schema
    exceptions (CA-9 approval store; OPTIONAL CA-10 attempt-audit
    journal, 2026-07-16) are sanctioned, spec'd, and never
    payment-state; CA-10 is INSERT-only and never read at runtime,
    so the V11-17 rejection scope stays intact.
[x] No future/post-MVP/PO-discussion work promoted to MVP: §5.2 DR
    runbook (C18), key-only anchoring (C25), §19.1/19.2/19.3
    (C69-71), ops console (C72) are classified FUTURE/QUESTION;
    the §20 implementation work is the three-operation NON-WAIVABLE
    exit set + OP-04a–e's waivable ergonomics endpoints (rounds 3–9).
[x] Every task has requirement-section traceability: each card's
    "Requirement sections / concepts to read" field; classification
    table maps C-items to task IDs.
[x] Every task has local discovery instructions: per-card "How to
    locate" + Section F workflow + Section O mapping gates.
[x] Every task has validation and a stop condition: per-card "Tests
    to add", "Manual validation", "Stop condition" fields; packets
    repeat tests + stop.
[x] Every task is small enough for a weak/small-context executor or
    explicitly says to split (ST-05 is a per-site template; S-09 lane
    setup and P-12 give the local split rule).
[x] Every §18 BLOCKING item appears in the dependency graph (Section
    D box P2 + ordering #1/#6/#7) and the go-live checklist (Q1–Q4,
    Q28).
[x] Every §16.6 companion artifact is an actionable deliverable:
    CA-1..9 each have a task card (Section H Phase 2), a Section L
    plan entry (deliverable, owner type, validation, dependents,
    start-before rule via prerequisites), and packet.
[x] Every manual operation beyond the §20 MVP operation set is
    classified future/PO discussion (C72; RG-05's supersede/close is
    delivered only as the §20-sanctioned guarded interim operation
    that §3 REQUIRES, subject to the release guard).
[x] Drift lint (added 2026-07-11; EXECUTABLE since the second
    external review — run `python tools/doc-lint.py`, wired to CI in
    .github/workflows/doc-lint.yml; the manual list below is its
    specification, not a substitute): after
    ANY package modification, verify at minimum — (a) no
    "card >1 obligation = error/alert" phrasing anywhere; (b) no
    UNIQUE-index claim on provider_reference while TL-12 is open;
    (c) no FOR UPDATE SKIP LOCKED in scanner guidance; (d) no rule
    wired to retry_deadline_at; (e) card-ID SET membership in file
    20 + duplicate-ID detection + card↔file-21 tracker SET parity
    + the canonical P3 order stated verbatim in file 20 (round 9;
    the lint does NOT verify full linear ordering beyond P3 or
    prerequisite-before-dependent relations — those stay MANUAL
    review, stated here so the contract matches the script); (f) every §18
    ask/item appears in the K register; (g) no "enforced by the
    procedure" dual-control phrasing (round 3: operation + §9.3
    approval workflow); (h) no deadline/budget-suspension language
    (round 3, cutoff retired round 10: bound = max attempts; gated
    scanners make zero attempts). The monolithic
    implementation-playbook.md is an ARCHIVED SNAPSHOT and is
    exempt (never edited, never loaded for execution).
```

Companion-artifact "can implementation start before the artifact
exists?" summary (Section L complement):

```text
CA-1: RC-01's classifier SHAPE can start; the code table must exist
      before RC-01 completes. CA-2: IN-05/06 skeleton yes; IN-07's
      ranks need it. CA-3: RC-05 yes; RC-06 needs it. CA-4: NO —
      S-02+ implement it (drafting may overlap D-02). CA-5/CA-6: NO
      for K-02/K-05 (they IMPLEMENT the specs); K-01 may proceed.
      CA-7: tests proceed from Section J; the catalog consolidates.
      CA-8: OB tasks proceed; OB-06 needs the stubs. CA-9: NO —
      OP-01 implements it.
