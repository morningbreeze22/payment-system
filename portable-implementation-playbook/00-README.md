> **Purpose:** Package overview: what this is, how to use it on the work laptop, the minimal reading set per task, safety rules, and the original-section map.
> **When to use this file:** FIRST — before anything else in the package; re-read at each phase boundary.
> **Depends on:** requirment-v4.md (transferred alongside this package — the baseline specification every card cites).
> **Used by:** Everyone and every task.
> **Safe to transfer:** yes
> **Contains local code names:** no

# Portable Implementation Playbook — README

This package is the refactored, phase-split form of the monolithic
`implementation-playbook.md`. Content and intent are preserved verbatim;
only navigation metadata, README guidance, the execution-order table,
and per-phase handoff summaries were added. Task IDs, requirement-
section traceability, §18 BLOCKING semantics, and all local-only rules
are unchanged.

## What to transfer

Transfer this whole `portable-implementation-playbook/` directory plus
`requirment-v4.md` (the baseline specification) to the work laptop.
That is the complete one-way transfer. Nothing ever comes back: the
filled mapping template and filled execution reports MUST stay on the
work laptop.

## How to use (work laptop)

1. Read this file, then `16-local-agent-instructions.md` (binding
   rules), then `01-playbook-index.md`. Teams assigning parallel
   work (and anyone hitting a local-vs-playbook difference) also
   read `26-team-execution-and-divergence-protocol.md` — the
   handover premise (T.1: a happy-path flow exists, business logic
   is preserved, this kit adds the failure paths), the divergence
   protocol (T.2), the facts sheet (T.3), and the work streams
   (T.4).
2. Run Phase P1 (discovery ONLY) with
   `08-task-cards/phase-01-discovery.md`; fill a LOCAL copy of
   `15-local-placeholder-mapping-template.md` (D-01 also creates
   the LOCAL divergence register and facts sheet from file 26).
3. Record §18 answers as they arrive (Phase P2). Never start a task
   the index lists as BLOCKED on an unanswered §18 item.
4. Execute one task card at a time, in index order, using its Minimal
   Context Packet as the working brief. Stop at each card's stop
   condition; fill the execution report
   (`19-local-task-execution-report-template.md`).
5. Track go-live readiness against `17-go-live-checklist.md`.

## Minimal reading set per task (small-context executors)

For any single task you need ONLY:

- this file (once),
- `01-playbook-index.md` (dependency order + BLOCKED list),
- `07-placeholder-glossary.md` (the placeholders your task names),
- the matching `09-minimal-context-packets/phase-xx-*.md` packet
  (and its task card in `08-task-cards/` when you need the full
  field set — the card wins on conflict),
- the `requirment-v4.md` sections the packet cites,
- your locally filled mapping template,
- the LOCAL divergence-register rows touching the card's
  tables/components (file 26 T.2).

Do not read the whole package per task. Repository handling follows
rule 4's split (round 18): when a card uses inventory/audit wording
("every site", "all writers", "grep"), repository-wide READ-ONLY
SEARCH is mandatory — search wide, LOAD only the relevant hits,
MODIFY only the card's scope. Bulk-loading the repository is never
allowed; full-repo orientation reading is for discovery cards only.

## Safety rules (repeated deliberately — full set in 16-local-agent-instructions.md)

- One task card at a time; stop at its stop condition.
- Do not start BLOCKED tasks; do not guess missing mappings.
- Do not broaden scope; do not change business rules
  (BUSINESS_RULE_CHANGE_REQUIRED escape hatch).
- Do not invent tables/journals/outboxes/parked-event/attempt-history
  tables (SPEC_CONFLICT escape hatch). Sole exceptions, per rule 13:
  the CA-9 approval store and the OPTIONAL CA-10 attempt-audit
  journal — ops schema, each only on its own card.
- The filled mapping template and filled execution reports NEVER leave
  the work laptop.
- If anything here conflicts with `requirment-v4.md`, the requirement
  document wins — report the discrepancy.

## Original-section map (monolith → package)

| Original | Package file |
|---|---|
| Title/preamble + Section A (executive summary) | 00-README.md (below) |
| Playbook Index | 01-playbook-index.md |
| Section B | 02-assumptions-and-non-goals.md |
| Section C | 03-requirement-classification.md |
| Section D | 04-dependency-graph.md |
| Section E | 05-implementation-phases.md |
| Section F | 06-local-discovery-workflow.md |
| Section G | 07-placeholder-glossary.md |
| Section H (task cards — the file-20 execution sequence is the authoritative ID inventory; do not trust hard-coded counts) | 08-task-cards/ (14 phase files + README) |
| Section I (minimal-context packets — card↔packet parity is lint-enforced; do not trust hard-coded counts) | 09-minimal-context-packets/ (14 phase files + README) |
| Section J | 10-test-matrix.md |
| Section K | 11-provider-techlead-po-questions.md |
| Section L | 12-companion-artifacts.md |
| Section M | 13-migration-rollout-rollback.md |
| Section N | 14-observability-reconciliation-runbooks.md |
| Section O | 15-local-placeholder-mapping-template.md |
| Section P | 16-local-agent-instructions.md |
| Section Q | 17-go-live-checklist.md |
| Section R.1 | 18-playbook-quality-self-check.md |
| Section R.2 | 19-local-task-execution-report-template.md |

Cross-references inside preserved content that say "Section X" or
"Playbook Index" refer to the original letters — resolve them with the
table above. References to "§" numbers always mean `requirment-v4.md`.

---

## Original playbook preamble

# Payment Orchestration — Portable Implementation Playbook

**Baseline spec:** `requirment-v4.md` (Requirements v4, Factored State Model, 2026-07-05 — BASELINE, hardened through fourteen review rounds).
**Date:** 2026-07-06
**Transfer rule:** this document is one-way portable. It contains NO local source-code names, NO proprietary logic, NO confidential details. All local components are placeholders (Section G) mapped on the work laptop only (Section O). Nothing from the work laptop flows back.
**Executor:** designed for a weak, low-context local coding agent. Every task is executable from: the task card + the referenced requirement sections + the locally-mapped files + the validation steps. No task requires whole-design understanding.

> **For the local agent:** Follow Section P. Execute exactly one task card at a time, using its Minimal Context Packet (Section I). Discovery first (Phase P1), no implementation during discovery.


---

# A. Executive summary

**What this playbook is for.** It converts the settled requirements
document `requirment-v4.md` into an ordered, source-code-agnostic
implementation workflow: phases, small task cards, tests, companion
artifacts, open questions, and go-live gates. It is the single
document transferred to the work laptop; local execution happens
there against the real codebase.

**What it deliberately does not assume.** It assumes NO knowledge of
the real repository: no file names, class names, package names, job
names, stored-procedure names, or local conventions. It assumes only
what `requirment-v4.md` documents: the stack (Java Spring Boot,
Oracle, Spring Kafka, Hazelcast), the four documented services
(`PaymentOrchestrationService`, `PaymentEnrichmentService`,
`PaymentExecutionService`, `PaymentNotificationConsumerService`), the
four core tables (`payment_obligation`, `payment_request`,
`processed_inbound_event`, `trade_snapshot_state`), the documented
columns/states, and the
documented companion artifacts. Everything else is a placeholder
(Section G) to be mapped locally (Section O).

**How to use it on the work laptop.**

```text
1. Run Phase P1 (discovery only) and fill the Section O mapping
   template. No implementation during discovery.
2. Record §18 BLOCKING answers as they arrive (Phase P2). Do not
   start a task marked BLOCKED on an unanswered item.
3. Execute task cards one at a time, in dependency order, each with
   its Minimal Context Packet (Section I). Validate, report
   (Section R template), stop, then take the next card.
4. Track go-live readiness against Section Q.
```

**Why it is source-code-agnostic.** The work laptop is on the far
side of a one-way transfer: no source-code details can ever come
back. A playbook that guessed local names would be unverifiable and
wrong in unknowable ways. Instead, every task names placeholder
components plus local discovery instructions; the mapping from
placeholder to real code exists only on the work laptop.

**How it protects against a weak / low-context execution agent.**

```text
- Every task card is self-contained: prerequisites, the exact §s to
  read, the exact placeholders to have mapped, instructions, tests,
  stop condition. No task requires remembering earlier reasoning.
- Minimal Context Packets (Section I) are paste-alone briefs.
- Tasks never mix schema + state machine + provider integration +
  recovery + observability + rollout.
- Anything requiring local judgment is marked MUST_VERIFY_LOCALLY;
  anything unanswerable is marked UNCLEAR or BLOCKED — the agent is
  instructed to stop, not guess (Section P).
- Section Q gates prevent "done locally" from being confused with
  "safe to go live".
```


---

## Small-context executor kit (files 20–26)

Added for weak, low-context executors (validated against a 200K-token
window):

| File | Role |
|---|---|
| 20-execution-sequence-and-decision-defaults.md | ONE flat task order (no parallel tracks) + AGENT/HUMAN tags + hard defaults DD-1..DD-9 for every "coordinate/choose" point in the cards |
| 21-progress-tracker-template.md | durable cross-session memory; copy locally, update after every task (filled copy stays local) |
| 22-requirement-section-map.md | § citation → exact requirment-v4.md heading text, so sections are found by search, never by scrolling |
| 23-task-kickoff-prompt.md | copy-paste per-task bootstrap prompt enforcing the minimal reading set |
| 24-implementation-mechanics.md | the concrete HOW: transaction/CAS/lock/claim/scanner/consumer recipes (M1–M6), Spring/Oracle traps (M7), binding per-shape edge-case checklists (M8) — mandatory for every implementation card (rules 17–18) |
| 25-golive-verification-procedures.md | per-Q go-live verification: the check to run, the evidence artifact, the sign-off role; evidence-pack layout + GO-04 meeting script |
| 26-team-execution-and-divergence-protocol.md | TEAM execution: handover premise (happy path exists; business logic preserved), the divergence protocol + LOCAL register (reference model vs local reality — rule 21), the local facts sheet (enrichment steps/latencies, volumes, Oracle version), and the sanctioned parallel work streams (rule 22) |

**Context budget (200K-token window):** the complete per-task reading
set (this file + index + glossary entries + one phase card file + one
packet + the cited spec sections + the 24-implementation-mechanics
recipes for the card's shape) is ≈ 30–40K tokens, leaving well
over 150K for local code. Never load the whole package or the whole
repository for one task; the kickoff prompt enforces this. The whole
`requirment-v4.md` is ≈ 40K tokens — loading it entirely is allowed
ONLY for discovery tasks that need broad orientation (D-12) and is
never needed for implementation tasks.

**Session protocol for the local agent:** at session start read the
LOCAL progress tracker; resume per its session-start rule; at session
end update it per its session-end rule. The tracker — not memory — is
the source of truth for where execution stands.
