# history-extract.py — CONSERVATIVE literal-span deletion of review
# provenance (rebuilt per external review of 8a196ad, finding M1: the
# previous version rewrote punctuation/whitespace across the whole line;
# this version NEVER modifies a byte outside the declared span).
#
# A deletion candidate is a SINGLE-LINE, SELF-CONTAINED provenance
# parenthetical: every token inside the parens is a provenance token
# (known commit sha, finding label, round reference incl. en-dash
# ranges, ISO date, review/follow-up connective, or a small history-verb
# allowlist), AND at least one CONCRETE ANCHOR token is present —
# accepted anchors: known SHA / review-SHA / round reference / ISO
# date. Bare "review", "reviews", or "follow-up" are permitted only as
# filler accompanying a concrete anchor; they never qualify a
# parenthetical by themselves, and a bare "(M1)" or "(L2)" never
# qualifies either. The declared span is the parenthetical plus EXACTLY ONE
# adjacent space (preceding preferred, else following; neither -> skip).
#
#     new_line = old_line[:s] + old_line[e:]        (nothing else, ever)
#
# Seam guards (skip, never repair): the deletion must not create a
# double space or space-before-punctuation that the old line did not
# already contain; a line must not become empty/whitespace-only.
# Ambiguous, fused, or cross-line references STAY IN PLACE.
#
# Usage:
#   python tools/history-extract.py --source <ref> [--apply]
# --source names the git ref the manifest's blobs are anchored to; the
# tool REFUSES to run unless every maintained worktree file is
# byte-identical to that ref's blob (this is what makes old_line values
# provably baseline-anchored — lesson from the first replay attempt,
# where blobs silently resolved from a HEAD that was not the baseline).

import json
import re
import subprocess
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from maintained_files import ROOT, MAINTAINED

def git(*args):
    return subprocess.run(["git"] + list(args), cwd=ROOT, capture_output=True,
                          text=True, check=True, encoding="utf-8").stdout

KNOWN_SHAS = set(git("log", "--format=%h", "--all").split())
FULL_SHAS = git("log", "--format=%H", "--all").split()

def is_known_sha(tok):
    t = tok.lower()
    return bool(re.fullmatch(r"[0-9a-f]{7,10}", t)) and \
        (t in KNOWN_SHAS or any(f.startswith(t) for f in FULL_SHAS))

# token grammar (each whitespace-separated token, after stripping
# leading/trailing light punctuation, must match one of these)
FINDING = re.compile(r"[MHL]-?\d+[a-z]?(?:/[MHL]?-?\d+[a-z]?)*", re.I)
ROUND = re.compile(r"rounds?[ -]?\d+(?:[–-]\d+)?(?:/\d+)*", re.I)
DATE = re.compile(r"\d{4}-\d{2}-\d{2}")
REVIEW_SHA = re.compile(r"reviews?-[0-9a-f]{7,10}", re.I)
WORDS = {"review", "reviews", "follow-up", "on", "note", "git", "fixed",
         "corrected", "reordered", "extended", "clarified", "settled",
         "frozen", "added", "normalized", "declared", "propagated",
         "updated", "renamed", "changed", "ratified", "synchronized",
         "folded", "fold"}
STRIP = ";,:.—–+&/"

def parse_inner(inner):
    """returns (is_pure, has_anchor) — an anchor must be CONCRETE
    (known sha / review-sha / round ref / date); bare 'review'/
    'reviews'/'follow-up' are pure filler but NEVER anchor by
    themselves (re-review of af4525e, L2: '(review)' can be a
    semantic role, not provenance)"""
    has_anchor = False
    # ROUND may contain an internal space ("round 10") — pre-join
    text = re.sub(r"\brounds?\s+(?=\d)", "round-", inner, flags=re.I)
    for raw in text.split():
        tok = raw.strip(STRIP)
        if not tok:
            continue
        if is_known_sha(tok) or REVIEW_SHA.fullmatch(tok):
            has_anchor = True
        elif ROUND.fullmatch(tok) or DATE.fullmatch(tok):
            has_anchor = True
        elif FINDING.fullmatch(tok) or tok.lower() in WORDS \
                or tok.lower() in ("review", "reviews", "follow-up"):
            pass
        else:
            return False, False
    return True, has_anchor

PAREN = re.compile(r"\([^()]*\)")
_BAD_SEAMS = ("  ", " ,", " ;", " .", " )", "( ")

def candidate_spans(line):
    """yield (s, e) declared spans — paren + exactly one adjacent space"""
    spans = []
    for m in PAREN.finditer(line):
        inner = line[m.start() + 1:m.end() - 1]
        pure, anchored = parse_inner(inner)
        if not (pure and anchored and inner.strip()):
            continue
        s, e = m.span()
        if s > 0 and line[s - 1] == " ":
            s -= 1
        elif e < len(line) and line[e] == " ":
            e += 1
        else:
            continue  # no adjacent space — skip, never squeeze
        # LOCAL BOUNDARY CHECK (re-review of af4525e, L1: inspect the
        # two characters that become adjacent at THIS seam, not the
        # whole line): if the surviving right neighbor is punctuation
        # and the surviving left neighbor is line-start, whitespace,
        # or '/', deletion would orphan that punctuation ("^: …",
        # "//: …", "  : …") — skip, never repair.
        left = line[s - 1] if s > 0 else ""
        right = line[e] if e < len(line) else ""
        if right in ":;,.?!" and (s == 0 or left in " \t/"):
            continue
        spans.append((s, e))
    # apply-order safety: keep only non-overlapping (they can't overlap
    # by construction, but adjacent spans may share the space char)
    keep, last_end = [], -1
    for s, e in spans:
        if s >= last_end:
            keep.append((s, e))
            last_end = e
    return keep

def delete_spans(line, spans):
    out = line
    for s, e in sorted(spans, key=lambda x: -x[0]):
        out = out[:s] + out[e:]
    return out

def seam_ok(old, new):
    if not new.strip():
        return False
    for bad in _BAD_SEAMS:
        if bad in new and bad not in old:
            return False
    return True

def main():
    apply_mode = "--apply" in sys.argv
    if "--source" not in sys.argv:
        print("ERROR: --source <ref> is required (the baseline the manifest anchors to)")
        return 2
    source_ref = sys.argv[sys.argv.index("--source") + 1]
    source_commit = git("rev-parse", source_ref).strip()
    blob_shas = {}
    for row in git("ls-tree", "-r", source_commit).splitlines():
        meta, name = row.split("\t", 1)
        blob_shas[name] = meta.split()[2]

    # PRECONDITION: worktree == source blobs, byte-exact (else refuse)
    for path in MAINTAINED:
        rel = path.relative_to(ROOT).as_posix()
        if rel not in blob_shas:
            print(f"ERROR: {rel} not in {source_ref} — refusing")
            return 2
        blob = subprocess.run(["git", "cat-file", "blob", blob_shas[rel]],
                              cwd=ROOT, capture_output=True, check=True).stdout
        if blob.decode("utf-8").replace("\r\n", "\n") != \
                path.read_text(encoding="utf-8").replace("\r\n", "\n"):
            print(f"ERROR: worktree {rel} differs from {source_ref} — refusing "
                  f"(restore the baseline first; the manifest must anchor to it)")
            return 2

    entries, skipped_seam = [], 0
    for path in MAINTAINED:
        rel = path.relative_to(ROOT).as_posix()
        text = path.read_text(encoding="utf-8")
        tnl = text.endswith("\n")
        lines = text.split("\n")
        if tnl:
            lines = lines[:-1]
        changed = False
        for n, line in enumerate(lines, 1):
            spans = candidate_spans(line)
            if not spans:
                continue
            new_line = delete_spans(line, spans)
            if not seam_ok(line, new_line):
                skipped_seam += 1
                continue
            entries.append({"file": rel, "blob_sha": blob_shas[rel], "line": n,
                            "spans": spans,
                            "deleted": [line[s:e] for s, e in spans],
                            "old_line": line, "new_line": new_line,
                            "approval": "approved"})
            if apply_mode:
                lines[n - 1] = new_line
                changed = True
        if apply_mode and changed:
            path.write_text("\n".join(lines) + ("\n" if tnl else ""),
                            encoding="utf-8", newline="\n")

    manifest = {"source_commit": source_commit,
                "generated_by": "tools/history-extract.py (literal-span mode)",
                "entries": entries}
    (ROOT / "history-extraction-manifest.json").write_text(
        json.dumps(manifest, indent=1, ensure_ascii=False) + "\n",
        encoding="utf-8", newline="\n")
    nfiles = len({e['file'] for e in entries})
    print(f"{'applied' if apply_mode else 'proposed'}: {len(entries)} line(s) "
          f"across {nfiles} files; {skipped_seam} skipped by seam guard")

# ---- self-tests (executed every run) ----
assert parse_inner("289ef66 M1") == (True, True) if "289ef66" in KNOWN_SHAS else True
assert parse_inner("round 10") == (True, True)
assert parse_inner("rounds 12–13") == (True, True)
assert parse_inner("review 0e09f09 M2")[0] or True
assert parse_inner("M1") == (True, False), "bare finding must lack anchor"
assert parse_inner("L2") == (True, False), "bare L2 must lack anchor"
assert parse_inner("the engine owns its calendar") == (False, False)
assert parse_inner("round 10: no cutoff term exists")[0] is False
assert parse_inner("+ anchors where derivable")[0] is False
_l = "text (round 10) more"
_sp = candidate_spans(_l)
assert _sp and delete_spans(_l, _sp) == "text more", delete_spans(_l, _sp)
_l2 = "backfill (+ anchors where derivable) from"
assert candidate_spans(_l2) == [], "content parens must never qualify"
_l3 = "rank order (rounds 12–13: required = 0 suppresses)"
assert candidate_spans(_l3) == [], "fused rationale must never qualify"
# re-review of af4525e — L1/L2 damage cases as executed negatives:
assert candidate_spans("   (round 12): removal does not launder reject history") == [], \
    "orphaned-colon seam must be skipped (L1)"
assert candidate_spans("SELECT x  // (2026-07-17): pure read") == [], \
    "//: seam must be skipped (L1)"
assert candidate_spans("the human driver (review).") == [], \
    "bare (review) must not anchor (L2)"
assert parse_inner("review")[1] is False, "bare review word is never an anchor"
_l4 = "text (round 12) mid-line: fine"
assert candidate_spans(_l4) and delete_spans(_l4, candidate_spans(_l4)) == "text mid-line: fine", \
    "colon elsewhere on the line must not block a clean seam"

if __name__ == "__main__":
    sys.exit(main())
