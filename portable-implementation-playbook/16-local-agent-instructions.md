> **Purpose:** Binding instructions for the local coding agent: 22 rules + the per-task loop (original Section P).
> **When to use this file:** ALWAYS - read before every working session; re-read when tempted to broaden scope.
> **Depends on:** 00-README.md; 01-playbook-index.md.
> **Used by:** Every task execution; the README files of 08-task-cards/ and 09-minimal-context-packets/ repeat its core rules.
> **Safe to transfer:** yes
> **Contains local code names:** no

# P. Instructions for the local coding agent on the work laptop

You are executing a portable implementation playbook against a
codebase the playbook's author has never seen. The playbook is
authoritative about WHAT and WHY; you resolve WHERE locally. The
baseline specification is `requirment-v4.md`; every task card cites
the sections that govern it.

**Rules (binding):**

```text
1.  FIRST perform discovery only (Phase P1, cards D-01..D-12). Do not
    implement anything during discovery — not even "obvious" fixes.
2.  Map placeholder components to real local files/classes/tables/
    jobs in the Section O template. Keep the mapping LOCAL; never
    send it externally, never paste real names into anything that
    leaves this laptop.
3.  Execute exactly ONE task card at a time, in dependency order
    (Playbook Index). Do not start a card whose prerequisites or
    required mappings are incomplete.
4.  Before each task, LOAD ONLY: the task card, its Minimal Context
    Packet (Section I), the requirement sections it lists, and the
    locally discovered files it names. Scope split (amended
    2026-07-11): MODIFICATION scope is always exactly the card's;
    read-only SEARCH scope is repository-wide whenever the card's
    instructions demand an inventory or audit ("every site", "all
    writers", "grep", inventory/audit wording) — search wide, load
    only the hits you need, change only the card's concern. Never
    load *.html files (visualizations/mockups — explanatory only,
    some contain superseded behavior).
5.  Use the Minimal Context Packet as your working brief. If your
    context is small, the packet alone plus the named §s suffices.
6.  Do NOT change business rules (payment decision logic, enrichment,
    validation, account detection, payment construction). If a task
    appears to require it, stop and report
    BUSINESS_RULE_CHANGE_REQUIRED with the requirement section that
    creates the need.
7.  Do NOT broaden scope. No unrelated cleanup. No style refactoring.
    No drive-by fixes. One card = one concern.
8.  Add or update tests BEFORE relying on new behavior; run the
    card's listed tests plus the surrounding suite.
9.  STOP after each task. Fill the Section R execution report. Wait
    for review per the local team's process before the next card.
10. If a task cannot be mapped to real code, mark it BLOCKED locally
    with what you searched and what was ambiguous. NEVER guess a
    mapping.
11. If a test fails OUTSIDE the task's scope, stop and report; do not
    attempt broad repairs. (D-11's baseline flaky list tells you
    which failures are pre-existing.)
12. If a task needs more context than you can hold, split it locally
    into sub-tasks (Task-ID.1, .2, …), each with its own report; the
    ST-05 card is explicitly designed to be split per rule site.
    EXCEPTIONS (round 17 — rule 22's no-split list WINS over this
    rule): IN-02 is NEVER split across sessions or owners — if it
    cannot fit your context, STOP and escalate to a larger-context
    executor; S-08/S-09 and the GO sequence keep one owner, and any
    split inside them needs an invariant-preserving boundary with
    human approval. Any other split that would cross a transaction
    boundary, a spec invariant, or a rule-20 review boundary
    requires a human-approved subtask plan BEFORE splitting.
13. Never invent tables, journals, outboxes, parked-event tables,
    attempt-history tables, or audit-history tables. If an
    implementation seems to need one, report SPEC_CONFLICT.
    SINGLE SANCTIONED EXCEPTION (2026-07-11 round 3): the §9.3
    two-step approval workflow's pending-approval record — a small
    OPS-SCHEMA store, operational workflow state OUTSIDE the §2
    payment data model (the four §2 tables). Its spec lives
    in CA-9; nothing payment-state may ever be stored there.
14. Tasks marked BLOCKED on §18 items stay blocked until the human
    owner records the answer. Do not "unblock" them by assuming.
15. Rejected design alternatives recorded in requirment-v4.md
    (derived committed_amount, attempt-history table, payload freeze,
    auto-unlatch, materiality re-POST, consecutive-answer counter,
    UETR generation/validation) are settled. Do not re-propose or
    implement them.
16. When the spec and this playbook seem to disagree, the spec
    (`requirment-v4.md`) wins — report the discrepancy.
17. Before ANY implementation card (every card except discovery and
    pure-document B/CA cards), read
    24-implementation-mechanics.md. Where a card says "the CAS",
    "under the lock", "the claim", or "the scanner", it means the
    M1–M6 recipes in that file — do not improvise transaction
    boundaries, lock order, or ack ordering.
18. Before declaring a card done, tick the matching SHAPE
    checklist(s) from 24-implementation-mechanics.md M8 (CAS / SCAN /
    CONSUME / PROC / READ) and record the result in the Section R
    report. An unticked line is either fixed or reported as a named
    exception — never silently skipped. Tasks feeding a go-live
    checklist item also file their evidence per
    25-golive-verification-procedures.md at completion time, not at
    GO-04.
19. Delivery model (adopted 2026-07-11): a card is a COMMIT unit on
    the current PHASE INTEGRATION BRANCH — every "Stop condition:
    merged" means merged to that branch. The phase branch merges to
    main only at the phase boundary, gated by: integration review,
    the phase's cross-path tests green, and a LOCAL MAPPING REFRESH
    (re-run the phase's relevant discovery searches; a stale mapping
    row re-BLOCKS its tasks). A card that leaves a safety path
    intentionally unwired must NAME its integration gate (the task
    ID or phase boundary that closes it) in the execution report —
    unnamed dangling paths fail the phase review.
20. HUMAN REVIEW CHECKPOINTS (round 16): a human review is REQUIRED
    at each of these risk boundaries before the phase branch merges
    — P3 schema/migrations, P4 identity/write-ahead persistence,
    P6 factored state/CAS helpers, P7 reservation/release guards,
    P9 admission/fence/marker flows, P10 MAYBE/retry/resolver,
    P11 ops approval + outcome operation, P14 rollout/evidence
    pack. An agent NEVER implements across two of these boundaries
    in one unreviewed branch.
21. DIVERGENCE PROTOCOL (file 26 T.2 — binding): the playbook's
    schema and names are a REFERENCE MODEL — invariants are
    non-negotiable, identifiers are not. Classify every local
    difference as DIV-1..DIV-5 and apply its fixed resolution;
    record it in the LOCAL divergence register. DIV-3 (semantic
    reuse of an existing column/mechanism) requires a recorded
    human approval BEFORE use; DIV-4 (structural conflict with an
    invariant) = SPEC_CONFLICT, stop; DIV-5 (business behavior) =
    BUSINESS_RULE_CHANGE_REQUIRED, stop. Cards and snippets are
    NEVER edited to local names — translate at execution time via
    the mapping + register. An OPEN DIV-3/DIV-4 row BLOCKS every
    card touching it. DIV-2 WITHOUT approval requires proving ALL
    SIX (round 18 — the file-26 T.2 checklist, repeated here
    because this file is in every session's context): exact
    precision + rounding preserved; equivalent NULL semantics;
    same atomic transaction; equivalent lockability; ONE
    authoritative writer; NO independently mutable copy — record
    the six-item proof in the execution report; any failure
    AUTO-PROMOTES to DIV-3/DIV-4 BEFORE implementation.
22. TEAM PARALLELISM (file 26 T.4): rule 3's "one card at a time"
    applies PER WORK STREAM. A stream holds ONE ACTIVE phase
    integration branch at a time (round 18 — the next phase's
    branch opens only after the previous phase's rule-19 review +
    merge); one owner per active branch; card prerequisites +
    file-20 gates ALWAYS outrank the stream map. Streams consume
    each other's helpers ONLY via merged main (never cherry-picks
    or shared WIP branches).
    The quality-over-parallelism list is binding: IN-02 in one
    sitting by one owner; P6+P7 same owner recommended; S-08+S-09
    together; GO cards one owner. Rule-20 reviews are never waived
    by parallelism. A single agent ignores this rule and follows
    file 20's flat order.
```

**Per-task loop:**

```text
read card + packet + cited §s (+ mechanics file per rule 17)
  → verify prerequisites + mappings
  → write/adjust tests (red where applicable)
  → implement the card's instructions only (M1–M6 recipes)
  → run card tests + surrounding suite
  → manual validation step from the card
  → tick the SHAPE checklist(s) (rule 18)
  → fill Section R report (incl. SHAPE result + evidence filed)
  → STOP
```

