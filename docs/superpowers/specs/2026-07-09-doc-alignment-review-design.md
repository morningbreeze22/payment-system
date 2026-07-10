# Doc-set alignment review — design

Date: 2026-07-09
Status: approved by user (this session)

## Goal

Verify every tracked document agrees with the current state of the design
after today's changes (multi-payment snapshot model fold, state-machine
edge-case additions, B-01 regating, new HTML docs). Fix mechanical drift;
surface judgment calls as decisions instead of deciding them.

## Source of truth

`requirment-v4.md` as of commit `4aa9306`. All other documents are
derived views and must not contradict it.

## Alignment invariants (the yardstick)

Every document must be consistent with:

1. **Multi-payment snapshot contract** (§1 contract facts, §6.0, §6.1):
   one trade carries multiple payments; each message is a full-trade
   snapshot, newer overwrites older; scope tuple unique within a
   snapshot (intake-validated); equal tuple across snapshots = same
   payment.
2. **Card lookup** (§12): business_id returns ALL of the trade's
   obligations; multiple results are normal; no ">1 = error" rule.
3. **Open items are exactly**: B-0 residue (written confirmation,
   intake validation, PO-9, TL-16), PO-1..9, TL-1..16, upstream asks
   1..5, §20 items — and no document narrates "answered" history;
   documents state current truth + open items only.
4. **Gating**: schema/identity freeze is NOT gated on §18-0; the §6
   consumer (IN-02) is gated on the B-01 residue.
5. **I6 framing**: one active request per OBLIGATION; a trade
   legitimately runs N request state machines in parallel (§11);
   whole-snapshot validation failure has trade-wide blast radius with
   precisely scoped effects (§6.6).
6. **Key-only anchor** (§6.6): trade-level placeholder, deleted on the
   first valid snapshot; never becomes a payment scope.

## Documents in scope, in suspicion order

| Doc | Depth | Known risk |
|---|---|---|
| payment-system-explained.html | full read | untouched except gates row; money walkthrough, simulator, §12 card text, settled-ground table may narrate one-payment world |
| ops-console-proposal.md / .html, ops-console-mockup.html | full read | card/lookup assumptions; state labels vs §10.4; single-obligation framing |
| README.md | full read | accuracy; whether to index the new HTML docs |
| requirment-v4.md | targeted self-check | internal cross-refs of today's insertions (PO-9 / TL-16 / ask-5 numbering, §6.1 open markers) |
| implementation-playbook.md ↔ portable mirror | parity spot-check | today's regating edits identical in both copies |
| payment-state-playground.html | claims check vs v4 | edge details, story narrations, tuple accuracy |
| open-questions-board.html | claims check vs v4 | question texts, option sets, cross-links vs §18/§20 |

## Handling rule

- **Mechanical drift** (a sentence contradicting a recorded fact, a stale
  gate, a missing cross-ref): fix inline; commit.
- **Judgment calls** (anything that changes scope, meaning, schema, or
  presentation strategy — e.g., rewrite explained.html's walkthrough for
  snapshots vs. mark it superseded by the playground): report as a
  decision question with options; do NOT act without the user.

## Output

An alignment report: per document — VERIFIED / FIXED (what) /
DECISION NEEDED (what + options) — plus the fix commits.

## Success criteria

Every tracked document is in exactly one of the three report states;
no document contradicts an alignment invariant without being either
fixed or flagged.
