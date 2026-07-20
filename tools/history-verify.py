# history-verify.py — exact-replay prover for the history-extraction fold
# (cleanup-plan review, required change 1: the verifier NEVER classifies
# text; it only applies the frozen, human-approved replacement manifest
# and demands byte equality).
#
# PROOF:  for every maintained file,
#           apply_approved_replacements(FROZEN_BLOB) == ACTUAL_WORKTREE_FILE
#         with line endings normalized to \n and NOTHING else normalized.
#
# Manifest entry contract (history-extraction-manifest.json):
#   file, blob_sha, line, old_line, new_line, classes, review,
#   approval: null | "approved" | "keep" | "modified"
#     - "approved": apply new_line as-is
#     - "modified": apply new_line (human edited it during Phase 1)
#     - "keep" / null: no change at that line
#   KEEP entries (class D) are keep-span assertions: the worktree line
#   must be BYTE-IDENTICAL to old_line.
#
# Also reports (does not decide): every applied deletion whose removed
# bytes contain a normative-looking token — the closure reviewer's
# checklist (review change 5 / final invariant 5).
#
# Usage:  python tools/history-verify.py [manifest.json]
#         --strict : unapproved (approval=null) non-KEEP entries are errors
#                    (Phase-4 mode; default warns only, for dry runs)

import json
import re
import subprocess
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from maintained_files import ROOT, MAINTAINED

RISK = re.compile(r"\b(?:MUST|ONLY|NEVER|STOP|NOT NULL|NOVALIDATE|UNIQUE|Oracle|"
                  r"ORA-\d+|BLOCKING|FORBIDDEN)\b"
                  r"|\b[A-Z]{1,4}-\d+[a-z]?\b|\bQ\d+[ab]?\b"
                  r"|\b[A-Z]{2,}(?:_[A-Z]+)+\b")

def blob_text(sha):
    out = subprocess.run(["git", "cat-file", "blob", sha], cwd=ROOT,
                         capture_output=True, check=True).stdout
    return out.decode("utf-8").replace("\r\n", "\n")

def main():
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    strict = "--strict" in sys.argv
    mpath = Path(args[0]) if args else ROOT / "history-extraction-manifest.json"
    manifest = json.loads(mpath.read_text(encoding="utf-8"))
    entries = manifest["entries"]

    by_file = {}
    for e in entries:
        by_file.setdefault(e["file"], []).append(e)

    failures, applied, keeps, risky = 0, {"A/B": 0, "C": 0}, 0, []
    for path in MAINTAINED:
        rel = path.relative_to(ROOT).as_posix()
        actual = path.read_text(encoding="utf-8").replace("\r\n", "\n")
        fentries = by_file.get(rel, [])
        if not fentries:
            # untouched file: must equal HEAD blob exactly
            head = subprocess.run(["git", "show", f"{manifest['source_commit']}:{rel}"],
                                  cwd=ROOT, capture_output=True).stdout
            if head and head.decode("utf-8").replace("\r\n", "\n") != actual:
                print(f"FAIL {rel}: modified but has NO manifest entries")
                failures += 1
            continue
        expected = blob_text(fentries[0]["blob_sha"]).split("\n")
        actual_lines = actual.split("\n")
        for e in sorted(fentries, key=lambda x: x["line"]):
            i = e["line"] - 1
            if i >= len(expected) or expected[i] != e["old_line"]:
                print(f"FAIL {rel}:{e['line']}: manifest old_line does not match the frozen blob (stale manifest?)")
                failures += 1
                continue
            ap = e.get("approval")
            if ap in ("approved", "modified"):
                if e.get("new_line") is None:
                    print(f"FAIL {rel}:{e['line']}: approved but new_line is null")
                    failures += 1
                    continue
                expected[i] = e["new_line"]
                applied["C" if "C" in e["classes"] else "A/B"] += 1
                for d in e.get("deleted", []):
                    hits = sorted(set(RISK.findall(d)))
                    if hits:
                        risky.append((rel, e["line"], hits))
            elif e["review"] == "KEEP" or ap == "keep" or ap is None:
                if e["review"] == "KEEP" or ap == "keep":
                    keeps += 1
                    if i < len(actual_lines) and actual_lines[i] != e["old_line"]:
                        print(f"FAIL {rel}:{e['line']}: KEEP-span changed (must stay byte-identical)")
                        failures += 1
                if ap is None and e["review"] != "KEEP":
                    msg = f"{rel}:{e['line']}: proposal not yet approved (review={e['review']})"
                    if strict:
                        print("FAIL " + msg)
                        failures += 1
                    else:
                        print("WARN " + msg)
        exp_text = "\n".join(expected)
        if exp_text != actual:
            failures += 1
            ea, aa = exp_text.split("\n"), actual.split("\n")
            for n, (a, b) in enumerate(zip(ea, aa), 1):
                if a != b:
                    print(f"FAIL {rel}: first divergence at line {n}")
                    print(f"  EXPECTED: {a[:160]!r}")
                    print(f"  ACTUAL:   {b[:160]!r}")
                    break
            else:
                print(f"FAIL {rel}: length differs (expected {len(ea)} lines, actual {len(aa)})")

    print(f"\napplied: {applied['A/B']} A/B replacements, {applied['C']} C rewrites; "
          f"{keeps} keep-spans verified")
    if risky:
        print(f"\nRISKY DELETIONS for the closure reviewer ({len(risky)}):")
        for rel, ln, hits in risky:
            print(f"  {rel}:{ln}: {', '.join(hits)}")
    if failures:
        print(f"\nPROOF FAILED: {failures} problem(s)")
        return 1
    print("\nPROOF HOLDS: worktree == frozen blobs + approved manifest, byte-exact")
    return 0

if __name__ == "__main__":
    sys.exit(main())
