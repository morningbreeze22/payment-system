# History Cleanup — Outcome Record

> Non-normative evidence record. The maintained documents win on any conflict.

- **Objective (PO decision):** delete review-provenance annotations that add no
  value, at ZERO risk to validated content. No rewrites, no ADR register, no
  new lint rules, no repairs of surrounding prose — ambiguous or fused
  references stay in place.
- **Baseline:** `5597dcd` (see `baseline-freeze-5597dcd.md`).
- **First attempt (`8a196ad`) was REJECTED** by external review (M1): the
  transformer's whole-line punctuation/whitespace cleanup modified characters
  outside the declared annotation spans (including meaning-bearing `+` tokens)
  and left malformed fragments. All 411 of its line changes were fully
  reverted; every maintained file was verified byte-identical to the
  `5597dcd` blobs before the replay.
- **Accepted mechanism (this commit):** literal span deletion only. A deletion
  is a single-line, self-contained provenance parenthetical (every token =
  known commit sha / finding label / round ref incl. en-dash ranges / ISO
  date / review connective; at least one anchor token required — a bare
  "(M1)" or "(L2)" never qualifies), whose declared span is the parenthetical
  plus exactly one adjacent space. `new_line = old_line minus span` — no other
  byte changes, ever. Seam guards skip (never repair) anything that would
  create doubled spaces or orphaned punctuation.
- **Applied:** 152 deletions across 40 maintained files (162 initially; the
  re-review of `af4525e` — 0H/0M/2L — flagged 10: nine orphaned-colon seams
  and one semantic `(review)` false positive; those 10 lines were RESTORED to
  baseline form and their manifest entries removed). Everything else left
  untouched by design.
- **Extractor tightened per that re-review:** (1) the seam check now inspects
  the two characters that become adjacent at EACH deletion boundary (right
  neighbor punctuation + left neighbor line-start/whitespace/'/' ⇒ skip —
  covers "^: …", "//: …", "  : …"); (2) bare `review`/`reviews`/`follow-up`
  never anchor a parenthetical by themselves — a concrete provenance token
  (known sha, review-sha, round ref, date) is required. Both encoded as
  executed self-tests using the reviewer's damage cases.
- **Proof (tools/history-verify.py, both invariants):**
  1. per entry, byte-exact with ZERO normalization:
     `delete_declared_spans(old_line) == new_line`;
  2. replay: `frozen 5597dcd blobs + manifest == worktree`, and every
     maintained file without entries byte-identical to its blob.
- **Other checks:** DOC-LINT clean (no rule or allow-regex touched);
  `git diff --check` certified against the NAMED RANGE `5597dcd..HEAD`
  (result quoted in the commit message).
- **Machine record:** `history-extraction-manifest.json` (the large Markdown
  rendering was removed — limited value once review is done).
