# Documentation Baseline Freeze — 5597dcd

> **Status:** FROZEN COMPARISON POINT (documentation-cleanup-strategy.md, Phase 1).
> This file is non-normative evidence. The maintained documents win on any conflict.

- **Baseline commit:** `5597dcd75163110122b808597a3e0572b1370597`
  (`Follow-up-review-8bf0aba fold: retire the fixed-v3 closure idiom`)
- **Frozen:** 2026-07-19, HEAD == origin/master, working tree clean on all tracked files.
- **Review standing at freeze:** external follow-up review of `8bf0aba` returned
  **0 High / 0 Medium / 1 Low** (first zero-High-zero-Medium round of the campaign);
  the single Low (fixed-v3 closure idiom) is folded IN this baseline commit.
  Reviewer verdict: the documentation baseline is ready for controlled agent
  implementation under the existing human / real-Oracle / provider-contract /
  go-live evidence gates.
- **doc-lint at freeze:** `DOC-LINT: clean` (exit 0). Rule set includes the
  frozen-contract guards: 6c (P3 order on 4 surfaces), 6e (card/packet sentinel
  pairs), 6i (§14 delivery-contract sites), 6j (T/Q range maxima), 6k (17-field
  request inventory slices), 6l (request_seq constraint sentinel slices),
  6m (consequence producers + four-state vocabulary), 6n (no fixed-v3 idiom),
  plus the forbidden/multiline-forbidden phrase classes with executed fixtures.
- **git diff --check:** clean.
- **Card/packet parity:** 113 task cards / 113 minimal-context packets,
  ID sets identical (computed at freeze, matches the external reviewer's count).
- **Maintained-file inventory at freeze (61 files):** the 5 root files
  (`requirment-v4.md`, `ops-console-proposal.md`, `failure-recovery-walkthrough.md`,
  `README.md`, `db-schema-dictionary.md`) + every `*.md` under
  `portable-implementation-playbook/` (56 files). Authoritative single copy:
  `tools/maintained_files.py` (consumed by doc-lint, history-extract,
  history-verify).
- **Explicitly NOT maintained:** `implementation-playbook.md` (frozen monolith,
  lint-exempt), `requirment-v4-annotated.md`, HTML explainers, review artifacts,
  this file, `decision-history.md` (when created — non-normative).

## Purpose

Every later cleanup step (history extraction, authority normalization,
generation) is verified against THIS commit's blobs. If a cleanup cannot prove
its change class against this baseline, it does not merge.
