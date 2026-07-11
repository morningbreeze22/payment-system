> **Purpose:** How to execute the task cards.
> **When to use this file:** Before using any task-card phase file in this directory.
> **Depends on:** 00-README.md; 16-local-agent-instructions.md; 01-playbook-index.md (dependency order + BLOCKED list).
> **Used by:** The local coding agent, every task.
> **Safe to transfer:** yes
> **Contains local code names:** no

# Task Cards — README

Rules (binding):

- **Execute one task card at a time**, in the dependency order given in
  `../01-playbook-index.md`. Never two cards in parallel.
- **Do not start blocked tasks.** Tasks marked BLOCKED on a §18 item
  (see the BLOCKED list in `../01-playbook-index.md`) stay blocked until
  the human owner records the answer. Tasks whose required local
  mappings are missing are locally BLOCKED — report, never guess.
- **Do not broaden scope.** No unrelated cleanup, no style refactoring,
  no drive-by fixes. One card = one concern.
- **Do not change business rules** (payment decision logic, enrichment,
  validation, account detection, payment construction). If a card seems
  to require it: stop, report BUSINESS_RULE_CHANGE_REQUIRED with the
  requirement section that creates the need.
- **Stop after each task** at the card's Stop condition.
- **Fill the execution report** (`../19-local-task-execution-report-template.md`)
  after every card, before the next one. Filled reports stay on the
  work laptop.
- Use each card WITH its Minimal Context Packet
  (`../09-minimal-context-packets/`, same phase file name). If they
  conflict, the card wins; if the card conflicts with
  `requirment-v4.md`, the requirement document wins — report it.
- If a card is too big for your context window, split it locally into
  sub-tasks (Task-ID.1, .2, …), each with its own report. ST-05 is
  explicitly a per-rule-site template.

Original Section H rules of use:
Rules of use (binding for the local agent): one card at a time; read
ONLY the card's listed §s and mapped files; every card ends at its
Stop condition with a Section R report. If a required mapping is
UNCLEAR/MISSING, the card is locally BLOCKED — report, don't guess.
If a card proves too big for the local context window, split it
locally into sub-tasks (suffix .1, .2 …) and report the split.
"§" references are to `requirment-v4.md`.


## Phase files in this directory

| File | Tasks | Phase |
|---|---|---|
| phase-01-discovery.md | D-01..D-12 | P1 |
| phase-02-blocking-gates-and-artifacts.md | B-01..B-04, CA-1..CA-9 | P2 |
| phase-03-schema-and-migration.md | S-01..S-10 | P3 |
| phase-04-identity-and-idempotency.md | K-01..K-06 | P4 |
| phase-05-uetr-response-persistence.md | U-01..U-03 | P5 |
| phase-06-factored-state-model.md | ST-01..ST-11 | P6 |
| phase-07-reservation-and-release-guards.md | RG-01..RG-10 | P7 |
| phase-08-provider-contract-tests.md | CT-01..CT-07 | P8 (parallel from P4) |
| phase-09-inbound-flows-and-status-feed.md | IN-01..IN-09 | P9 |
| phase-10-retry-recovery-maybe.md | RC-01..RC-10 | P10 |
| phase-11-operator-verified-outcome.md | OP-01..OP-04 | P11 |
| phase-12-drift-reconciliation.md | OB-01..OB-02 | P12 |
| phase-13-observability-and-runbooks.md | OB-03..OB-07 | P13 |
| phase-14-rollout-and-go-live.md | GO-01..GO-05 | P14 |

**Sequencing note:** execute in the flat order of
`../20-execution-sequence-and-decision-defaults.md` (single-agent,
no parallel tracks). When a card says "coordinate", "helpful but not
required", or offers an option, apply the matching decision default
DD-1..DD-9 from that file instead of judging. Track progress in your
local copy of `../21-progress-tracker-template.md`.
