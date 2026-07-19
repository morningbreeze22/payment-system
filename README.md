# Payment Orchestration System

This repository contains the implementation baseline and supporting design material for the payment orchestration system.

## Start here

- [`requirment-v4.md`](requirment-v4.md) — canonical requirements baseline (all other documents are derived views). The filename misspelling is KNOWN and kept intentionally — renaming would churn every cross-reference; revisit only at a major version.
> All `.html` artifacts below are NON-NORMATIVE explanatory snapshots, refreshed manually — where they and `requirment-v4.md` disagree, the requirement doc wins (round 9).

- [`payment-state-playground.html`](payment-state-playground.html) — animated state-model map: every state and legal transition, playable scenario stories, rule inspector.
- [`open-questions-board.html`](open-questions-board.html) — interactive tracker for all open questions (§18 + §20) with answer capture and import/export; run locally in any browser.
- [`payment-system-explained.html`](payment-system-explained.html) — interactive system guide (architecture, money model, simulator).
- [`failure-recovery-walkthrough.md`](failure-recovery-walkthrough.md) — every failure scenario with its recovery path, tiered T0 (prevented) → T4 (external reconciliation); the gap review artifact.
- [`db-schema-dictionary.md`](db-schema-dictionary.md) — conceptual dictionary of every table and column (why it exists, how it is used); derived from §2 + CA-4/CA-9/CA-10, which stay authoritative.
- [`failure-recovery-map.html`](failure-recovery-map.html) — interactive visualization of the failure/recovery catalog: filter by domain and tier, expand each scenario's recovery chain.
- [`portable-implementation-playbook/`](portable-implementation-playbook/) — **the maintained implementation playbook**: per-phase task cards, minimal-context packets, execution sequence, mechanics reference, go-live verification. This is what implementing agents load.
- [`implementation-playbook.md`](implementation-playbook.md) — ARCHIVED single-file snapshot of the playbook (frozen 2026-07-11; no longer updated — the portable package is the only maintained form; kept for one-file human reading).
- [`ops-console-proposal.md`](ops-console-proposal.md) — operations-console proposal (future work; `ops-console-proposal.html` is a superseded rendering kept for layout, `ops-console-mockup.html` is the visual mockup).
- [`docs/superpowers/specs/`](docs/superpowers/specs/) — session design specs.

## Review provenance

The annotated requirement (`requirment-v4-annotated.md`) and the `design-review-v1…v14` round documents are **maintained locally by the design owner and intentionally excluded from this repository** (see `.gitignore`); they are available on request. The baseline's Status header in `requirment-v4.md` summarizes what those rounds hardened.

## Repository scope

Earlier requirement drafts, generated HTML copies of reviews, duplicate proposal rendering, and planning screenshots are intentionally excluded. The v4 document is the implementation baseline.
