> **Purpose:** How to use the minimal context packets.
> **When to use this file:** Before using any packet file in this directory.
> **Depends on:** 00-README.md; 16-local-agent-instructions.md.
> **Used by:** The local coding agent, every task.
> **Safe to transfer:** yes
> **Contains local code names:** no

# Minimal Context Packets — README

Rules (binding):

- **Each packet is paste-alone** for a small-context local agent: packet
  + the requirement sections it cites (from `requirment-v4.md`) + the
  locally filled mapping rows are enough to execute the task.
- **Each packet must be paired with the matching task card** in
  `../08-task-cards/` (same phase file name, same task ID). The card
  carries the full field set (prerequisites, do-not-change, edge cases,
  failure signs, common mistakes); the packet is the compressed brief.
- **If a packet and its task card conflict, the task card wins.**
- **If a task card and `requirment-v4.md` conflict, `requirment-v4.md`
  wins** — report the discrepancy; do not improvise.
- Every packet ends with a Stop condition. Stop there. Fill the
  execution report (19-local-task-execution-report-template.md) before
  taking the next packet.
- Core safety rules (repeated deliberately): one task at a time; no
  blocked tasks; no scope broadening; no business-rule changes; local
  mappings never leave the work laptop.

Original Section I introduction:
One packet per task card. Each is paste-alone for a small-context
executor. Format: ID/title · Read (§s of `requirment-v4.md` + playbook
refs) · Invariant · Placeholders · Mappings needed · Objective ·
Tests · Stop.


**Sequencing note:** packets are taken in the flat order of
`../20-execution-sequence-and-decision-defaults.md`; decision defaults
DD-1..DD-9 there override any "coordinate/choose" wording. Find cited
spec sections by heading text via
`../22-requirement-section-map.md`.
