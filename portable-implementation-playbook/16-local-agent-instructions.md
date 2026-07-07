> **Purpose:** Binding instructions for the local coding agent: 16 rules + the per-task loop (original Section P).
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
4.  Before each task, read ONLY: the task card, its Minimal Context
    Packet (Section I), the requirement sections it lists, and the
    locally discovered files it names. Do not read the whole repo
    unless the card is a discovery card.
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
13. Never invent tables, journals, outboxes, parked-event tables,
    attempt-history tables, or audit-history tables. If an
    implementation seems to need one, report SPEC_CONFLICT.
14. Tasks marked BLOCKED on §18 items stay blocked until the human
    owner records the answer. Do not "unblock" them by assuming.
15. Rejected design alternatives recorded in requirment-v4.md
    (derived committed_amount, attempt-history table, payload freeze,
    auto-unlatch, materiality re-POST, consecutive-answer counter,
    UETR generation/validation) are settled. Do not re-propose or
    implement them.
16. When the spec and this playbook seem to disagree, the spec
    (`requirment-v4.md`) wins — report the discrepancy.
```

**Per-task loop:**

```text
read card + packet + cited §s
  → verify prerequisites + mappings
  → write/adjust tests (red where applicable)
  → implement the card's instructions only
  → run card tests + surrounding suite
  → manual validation step from the card
  → fill Section R report
  → STOP
```

