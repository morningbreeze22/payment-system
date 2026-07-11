# Payment Orchestration System

This repository contains the implementation baseline and supporting design material for the payment orchestration system.

## Start here

- [`requirment-v4.md`](requirment-v4.md) — canonical requirements baseline (all other documents are derived views).
- [`payment-state-playground.html`](payment-state-playground.html) — animated state-model map: every state and legal transition, playable scenario stories, rule inspector.
- [`open-questions-board.html`](open-questions-board.html) — interactive tracker for all open questions (§18 + §20) with answer capture and import/export; run locally in any browser.
- [`payment-system-explained.html`](payment-system-explained.html) — interactive system guide (architecture, money model, simulator).
- [`failure-recovery-walkthrough.md`](failure-recovery-walkthrough.md) — every failure scenario with its recovery path, tiered T0 (prevented) → T4 (external reconciliation); the gap review artifact.
- [`failure-recovery-map.html`](failure-recovery-map.html) — interactive visualization of the failure/recovery catalog: filter by domain and tier, expand each scenario's recovery chain.
- [`implementation-playbook.md`](implementation-playbook.md) — implementation plan: phases, task cards, gates.
- [`portable-implementation-playbook/`](portable-implementation-playbook/) — the playbook split into transferable per-phase files.
- [`ops-console-proposal.md`](ops-console-proposal.md) — operations-console proposal (future work; `ops-console-proposal.html` is a superseded rendering kept for layout, `ops-console-mockup.html` is the visual mockup).
- [`docs/superpowers/specs/`](docs/superpowers/specs/) — session design specs.

## Review provenance

`requirment-v4-annotated.md` preserves review annotations. The `design-review*.md` documents contain the review rounds that produced the baseline; review v2 is retained as HTML because no Markdown source exists.

## Repository scope

Earlier requirement drafts, generated HTML copies of reviews, duplicate proposal rendering, and planning screenshots are intentionally excluded. The v4 document is the implementation baseline.
