# history-extract.py — Phase 0 of the review-history extraction fold.
#
# READ-ONLY: scans the maintained doc set for review-history annotations
# and emits a PER-LINE EXACT-REPLACEMENT manifest for human approval
# (cleanup-plan review, required changes 1/3/5): every proposal is an
# exact (file, frozen blob sha, line number, old_line, new_line) pair.
# history-verify.py later REPLAYS the approved manifest against the
# frozen blobs — it never re-classifies text itself.
#
# Classes:
#   A  bare provenance (whole parenthetical is provenance) -> delete
#   B  marker fused to live rationale -> delete marker only
#   C  rule phrased as history -> new_line left for the human rewrite
#   D  load-bearing decision record -> KEEP (recorded as keep-span)
#   R  unsure / risk-promoted -> human decides
#
# Risk promotion (review change 5): if any DELETED byte-run contains a
# normative token (MUST/ONLY/NEVER/STOP, task/test/Q ids, enum tokens,
# Oracle behavior), the line is promoted to R regardless of class.
# A/B lines in HIGH-RISK files are flagged review=EXPLICIT (never
# auto-approved).

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
    return t in KNOWN_SHAS or any(f.startswith(t) for f in FULL_SHAS)

# ---- annotation grammar ----
FINDING = r"[MHL]-?\d+[a-z]?(?:/[MHL]?-?\d+[a-z]?)*"
HIST_VERB = (r"(?:(?:ORDER\s+)?(?:REORDERED|CORRECTED|EXTENDED|CLARIFIED|FIXED|ADDED|"
             r"FROZEN|DECLARED|SETTLED|NORMALIZED|RENAMED|CHANGED|RATIFIED|"
             r"PROPAGATED|SYNCHRONIZED|UPDATED|SPECIFIED|ANSWERED|CLOSED|folded)\s+)?")
PATTERNS = [
    ("follow_up", re.compile(r"follow-up\s+" + FINDING + r"\s+on\s+\b[0-9a-f]{7,10}\b", re.I)),
    ("sha_finding", re.compile(HIST_VERB + r"(?:reviews?[- ])?\b(?P<sha>[0-9a-f]{7,10})\b(\s+" + FINDING + r"\b|\s+note\b)?", re.I)),
    ("round", re.compile(HIST_VERB + r"\brounds?[ -]\d+(?:/\d+)*\b", re.I)),
    ("date", re.compile(r"\b\d{4}-\d{2}-\d{2}\b")),
]
PROV_FILLER = re.compile(
    r"(?:reviews?[- ])?[0-9a-f]{7,10}\b"
    r"|" + FINDING + r"\b"
    r"|\brounds?[ -]\d+(?:/\d+)*\b"
    r"|\bfollow-up\b|\bon\b|\bnote\b|\breviews?\b"
    r"|\b\d{4}-\d{2}-\d{2}\b"
    r"|\b(?:fixed|corrected|reordered|extended|clarified|settled|frozen|added|"
    r"normalized|answered|closed|folded|declared|propagated|synchronized|"
    r"updated|renamed|retired|removed|split|pre-split|mechanism|shape|"
    r"version model|state model|failure state|fixture|lesson|and|the|a|an|"
    r"per|in|of|at|to|by|via|with|incl|e\.g|i\.e)\b"
    r"|[;,:+&./()—–'\"-]|\s", re.I)

C_HINTS = re.compile(r"\bRETIRED\b|\bretired\b|\bsuperseded\b|\bformer(?:ly)?\b|"
                     r"\bno longer\b|\breplaces\b|\breplaced\b", re.I)
D_HINTS = re.compile(r"\bCLOSED\b|\bANSWERED\b|\bPO answer\b|\bsigned\b|\bdate\+source\b|"
                     r"\bNormalized\b|\bdecided\b|\bdecision\b|\bNORMATIVE\b|"
                     r"\bconfirmed\b|\bin writing\b|\bwritten\b|\bverbally\b", re.I)

# review change 5: normative tokens that may never sit inside an
# auto-approved deletion span
RISK = re.compile(r"\b(?:MUST|ONLY|NEVER|STOP|NOT NULL|NOVALIDATE|UNIQUE|Oracle|"
                  r"ORA-\d+|BLOCKING|FORBIDDEN)\b"
                  r"|\b[A-Z]{1,4}-\d+[a-z]?\b"      # S-05 / T-12 / CA-4 / RC-03 / GO-03
                  r"|\bQ\d+[ab]?\b"                  # Q8 / Q5b
                  r"|\b[A-Z]{2,}(?:_[A-Z]+)+\b")     # ENUM_TOKENS
HIGH_RISK_FILES = re.compile(
    r"^(requirment-v4\.md"
    r"|.*phase-03-schema-and-migration\.md"
    r"|.*phase-06-factored-state-model\.md"
    r"|.*phase-07-reservation-and-release-guards\.md"
    r"|.*phase-08-provider-contract-tests\.md"
    r"|.*phase-10-retry-recovery-maybe\.md"
    r"|.*20-execution-sequence-and-decision-defaults\.md"
    r"|.*24-implementation-mechanics\.md"
    r"|.*25-golive-verification-procedures\.md)$")

def enclosing_paren(line, start, end):
    depth, opens = 0, []
    for i, ch in enumerate(line):
        if ch == "(":
            opens.append(i)
        elif ch == ")" and opens:
            o = opens.pop()
            if o <= start and end <= i + 1:
                return (o, i + 1)
    return None

def classify(line, kind, mstart, mend):
    """returns (class, delete_span_or_None)"""
    if kind == "date":
        return ("D", None) if D_HINTS.search(line) else ("R", None)
    if C_HINTS.search(line):
        return ("C", None)
    span = enclosing_paren(line, mstart, mend)
    if span:
        inner = line[span[0] + 1:span[1] - 1]
        residue = PROV_FILLER.sub("", inner)
        if len([w for w in re.split(r"\W+", residue) if len(w) > 2]) < 3:
            return ("A", span)          # delete the whole parenthetical
        return ("B", (mstart, mend))
    return ("B", (mstart, mend))

_LEAD_WS = re.compile(r"^\s*")
def cleanup(line):
    """separator-debris cleanup after span deletion; leading indent kept"""
    lead = _LEAD_WS.match(line).group(0)
    body = line[len(lead):]
    prev = None
    while prev != body:
        prev = body
        body = re.sub(r"\(\s*[;,—–:+&]\s*", "(", body)
        body = re.sub(r"\s*[;,—–+&]\s*\)", ")", body)
        body = re.sub(r"\(\s*\)|\[\s*\]", "", body)
        body = re.sub(r"([;,])\s*[;,]", r"\1", body)
        body = re.sub(r"—\s*—", "—", body)
        body = re.sub(r"([.!?])\s*:\s+", r"\1 ", body)   # "entry. : this" -> "entry. this"
        body = re.sub(r"\s+([;,.):\]])", r"\1", body)     # "CHECKPOINTS :" -> "CHECKPOINTS:"
        body = re.sub(r"([(\[])\s+", r"\1", body)
        body = re.sub(r"[ \t]{2,}", " ", body)
    return (lead + body).rstrip()

def main():
    source_commit = git("rev-parse", "HEAD").strip()
    blob_shas = {}
    for row in git("ls-tree", "-r", "HEAD").splitlines():
        meta, name = row.split("\t", 1)
        blob_shas[name] = meta.split()[2]

    entries = []
    for path in MAINTAINED:
        rel = path.relative_to(ROOT).as_posix()
        if rel not in blob_shas:
            print(f"WARN: {rel} not in HEAD tree — skipped (commit it first)")
            continue
        lines = path.read_text(encoding="utf-8").splitlines()
        per_line = {}
        for n, line in enumerate(lines, 1):
            for kind, pat in PATTERNS:
                for m in pat.finditer(line):
                    if kind == "sha_finding" and not is_known_sha(m.group("sha")):
                        continue
                    rec = per_line.setdefault(n, {"matches": [], "spans": [],
                                                  "classes": set(), "kinds": set()})
                    s, e = m.span()
                    if any(s < e2 and e > s2 for s2, e2 in rec["spans"]):
                        continue
                    cls, dspan = classify(line, kind, s, e)
                    rec["matches"].append(m.group(0))
                    rec["kinds"].add(kind)
                    rec["classes"].add(cls)
                    rec["spans"].append(dspan if dspan else (s, e))
                    rec.setdefault("del_spans", []).append((cls, dspan))
        for n, rec in sorted(per_line.items()):
            line = lines[n - 1]
            classes = rec["classes"]
            # line-level disposition
            if "C" in classes:
                review, new_line, deleted = "REWRITE", None, []
            elif "R" in classes and classes <= {"R", "D"}:
                review, new_line, deleted = "DECIDE", None, []
            elif classes == {"D"}:
                review, new_line, deleted = "KEEP", line, []
            else:
                # apply A/B deletions (D tokens on the same line are kept)
                spans = sorted([sp for cls, sp in rec["del_spans"]
                                if sp and cls in ("A", "B")],
                               key=lambda x: -x[0])
                new_line, deleted = line, []
                for s, e in spans:
                    deleted.append(new_line[s:e])
                    new_line = new_line[:s] + " " + new_line[e:]
                new_line = cleanup(new_line)
                risk_hits = sorted({t for d in deleted for t in RISK.findall(d)})
                if risk_hits:
                    review, new_line = "DECIDE", None
                    rec["risk"] = f"normative token(s) in deleted text: {', '.join(risk_hits)}"
                elif "R" in classes:
                    review, new_line = "DECIDE", None
                elif HIGH_RISK_FILES.match(rel):
                    review = "EXPLICIT"
                else:
                    review = "AUTO_OK"
            entries.append({
                "file": rel, "blob_sha": blob_shas[rel], "line": n,
                "old_line": line, "new_line": new_line,
                "classes": sorted(classes), "kinds": sorted(rec["kinds"]),
                "matched": rec["matches"], "deleted": deleted,
                "risk": rec.get("risk"), "review": review,
                "approval": None, "adr_id": None,
            })

    manifest = {"source_commit": source_commit,
                "generated_by": "tools/history-extract.py",
                "entries": entries}
    (ROOT / "history-extraction-manifest.json").write_text(
        json.dumps(manifest, indent=1, ensure_ascii=False), encoding="utf-8")

    from collections import Counter
    rv = Counter(e["review"] for e in entries)
    md = ["# History-Extraction Manifest (Phase 0 proposal — nothing applied)", "",
          f"Source commit: `{source_commit}` (all old_line values are against these blobs)",
          f"Line entries: **{len(entries)}** across {len({e['file'] for e in entries})} files", "",
          "| Review bucket | Meaning | Count |", "|---|---|---:|",
          f"| AUTO_OK | pure A/B in a normal file — approve by skim | {rv.get('AUTO_OK', 0)} |",
          f"| EXPLICIT | A/B in a HIGH-RISK file — read old/new pair | {rv.get('EXPLICIT', 0)} |",
          f"| REWRITE | class C — you approve an exact new_line | {rv.get('REWRITE', 0)} |",
          f"| DECIDE | risk-promoted or unsure — keep or specify | {rv.get('DECIDE', 0)} |",
          f"| KEEP | class D decision record — byte-identical keep-span | {rv.get('KEEP', 0)} |", ""]
    cur = None
    for e in entries:
        if e["file"] != cur:
            cur = e["file"]
            md += ["", f"## {cur}", ""]
        md.append(f"**:{e['line']}** `{'/'.join(e['classes'])}` **{e['review']}**"
                  + (f" — {e['risk']}" if e["risk"] else ""))
        md.append(f"- OLD: `{e['old_line'].strip()[:200]}`")
        if e["new_line"] is not None and e["review"] != "KEEP":
            md.append(f"- NEW: `{e['new_line'].strip()[:200] or '(line emptied)'}`")
        elif e["review"] == "KEEP":
            md.append("- KEEP unchanged")
        else:
            md.append(f"- NEW: (to be decided — matched: {', '.join(e['matched'][:4])})")
        md.append("")
    (ROOT / "history-extraction-manifest.md").write_text(
        "\n".join(md) + "\n", encoding="utf-8")

    print(f"manifest: {len(entries)} line entries -> history-extraction-manifest.md/.json")
    print("review buckets:", dict(rv))

if __name__ == "__main__":
    sys.exit(main())
