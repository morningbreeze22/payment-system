# history-verify.py — two-invariant prover for the literal-span history
# cleanup (rebuilt per external review of 8a196ad, M1).
#
# INVARIANT 1 (deletion-only, per entry, ZERO normalization):
#     delete_declared_spans(old_line) == new_line        (byte-exact)
#   No whitespace normalization, no punctuation repair — if the entry's
#   new_line is anything but the old line minus its declared spans, the
#   proof fails.
#
# INVARIANT 2 (replay, per file):
#     frozen blob + approved manifest replacements == worktree file
#   (line endings normalized to \n; nothing else), and every maintained
#   file WITHOUT entries is byte-identical to its frozen blob.
#
# Also asserts: every deleted span's bytes contain no obviously
# normative token (belt — the extractor's grammar should make this
# impossible), and reports every deletion for the record.
#
# Usage: python tools/history-verify.py [manifest.json] [--quiet]

import json
import re
import subprocess
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from maintained_files import ROOT, MAINTAINED

RISK = re.compile(r"\b(?:MUST|ONLY|NEVER|STOP|NOT NULL|NOVALIDATE|UNIQUE|Oracle|"
                  r"ORA-\d+|BLOCKING|FORBIDDEN)\b"
                  r"|\b(?:S|T|K|U|ST|RG|CT|IN|RC|OP|OB|GO|CA|D|B|AUD)-\d+[a-z]?\b"
                  r"|\bQ\d+[ab]?\b"
                  r"|\b[A-Z]{2,}(?:_[A-Z]+)+\b")

def blob_text(sha):
    out = subprocess.run(["git", "cat-file", "blob", sha], cwd=ROOT,
                         capture_output=True, check=True).stdout
    return out.decode("utf-8").replace("\r\n", "\n")

def main():
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    quiet = "--quiet" in sys.argv
    mpath = Path(args[0]) if args else ROOT / "history-extraction-manifest.json"
    manifest = json.loads(mpath.read_text(encoding="utf-8"))
    by_file = {}
    for e in manifest["entries"]:
        by_file.setdefault(e["file"], []).append(e)

    failures = span_ok = 0
    for path in MAINTAINED:
        rel = path.relative_to(ROOT).as_posix()
        actual = path.read_text(encoding="utf-8").replace("\r\n", "\n")
        fentries = by_file.get(rel, [])
        if not fentries:
            head = subprocess.run(["git", "show", f"{manifest['source_commit']}:{rel}"],
                                  cwd=ROOT, capture_output=True).stdout
            if head and head.decode("utf-8").replace("\r\n", "\n") != actual:
                print(f"FAIL {rel}: modified but has NO manifest entries")
                failures += 1
            continue
        expected = blob_text(fentries[0]["blob_sha"]).split("\n")
        for e in sorted(fentries, key=lambda x: x["line"]):
            i = e["line"] - 1
            if i >= len(expected) or expected[i] != e["old_line"]:
                print(f"FAIL {rel}:{e['line']}: old_line does not match the frozen blob")
                failures += 1
                continue
            # INVARIANT 1 — byte-exact span deletion, nothing else
            recon = e["old_line"]
            for s, en in sorted((tuple(x) for x in e["spans"]), key=lambda x: -x[0]):
                if not (0 <= s < en <= len(e["old_line"])):
                    print(f"FAIL {rel}:{e['line']}: span {s},{en} out of bounds")
                    failures += 1
                    break
                recon = recon[:s] + recon[en:]
            if recon != e["new_line"]:
                print(f"FAIL {rel}:{e['line']}: new_line is NOT old_line minus declared spans")
                print(f"  span-deleted: {recon[:140]!r}")
                print(f"  manifest new: {e['new_line'][:140]!r}")
                failures += 1
                continue
            for d in e["deleted"]:
                if RISK.search(d):
                    print(f"FAIL {rel}:{e['line']}: normative token inside deleted span: {d!r}")
                    failures += 1
            span_ok += 1
            if e.get("approval") == "approved":
                expected[i] = e["new_line"]
        # INVARIANT 2 — replay
        exp_text = "\n".join(expected)
        if exp_text != actual:
            failures += 1
            ea, aa = exp_text.split("\n"), actual.split("\n")
            for n, (a, b) in enumerate(zip(ea, aa), 1):
                if a != b:
                    print(f"FAIL {rel}: replay divergence at line {n}")
                    print(f"  EXPECTED: {a[:150]!r}")
                    print(f"  ACTUAL:   {b[:150]!r}")
                    break
            else:
                print(f"FAIL {rel}: line-count differs")

    print(f"\ninvariant-1 (literal span deletion): {span_ok} entries byte-verified")
    if failures:
        print(f"PROOF FAILED: {failures} problem(s)")
        return 1
    print("PROOF HOLDS: every change = old line minus its declared spans; "
          "worktree == frozen blobs + manifest; untouched files byte-identical")
    return 0

if __name__ == "__main__":
    sys.exit(main())
