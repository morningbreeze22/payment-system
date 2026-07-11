#!/usr/bin/env python3
"""Doc-set drift lint (added 2026-07-11; extended after review round 3).

STRUCTURAL SMOKE TEST — catches forbidden stale phrases, ID parity,
and reference existence. It does NOT prove semantic consistency;
manual review still owns contradictions between normative statements.
Executable form of the drift checklist in
portable-implementation-playbook/18-playbook-quality-self-check.md.
Exit 0 = clean; exit 1 = violations (printed with file:line).

Scope: the MAINTAINED doc set only. Excluded by design:
implementation-playbook.md (frozen archived snapshot), the external
review documents, and *.html (explanatory, never loaded by agents).
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
PORTABLE = ROOT / "portable-implementation-playbook"

MAINTAINED = [ROOT / "requirment-v4.md", ROOT / "ops-console-proposal.md",
              ROOT / "failure-recovery-walkthrough.md", ROOT / "README.md"]
MAINTAINED += sorted(PORTABLE.rglob("*.md"))

errors = []


def lines_of(path):
    try:
        return path.read_text(encoding="utf-8").splitlines()
    except FileNotFoundError:
        errors.append(f"{path}: MISSING FILE")
        return []


def rel(path):
    return str(path.relative_to(ROOT))


# ---- Rule 1..4: forbidden stale phrases (with per-rule allow tokens) ----
FORBIDDEN = [
    # (name, trip regex, allow-if-line-contains regex)
    ("card->1-error rule",
     re.compile(r">\s*1\s+obligation.*(error|alert)|(error|alert).*>\s*1\s+obligation", re.I),
     re.compile(r"never|not a|no rule|NORMAL|lint", re.I)),
    ("SKIP LOCKED scanner guidance",
     re.compile(r"FOR UPDATE SKIP LOCKED|SKIP LOCKED", re.I),
     re.compile(r"NOT used|not be used|no FOR UPDATE|replaces the earlier|could invert|corrected|usage|Search:|queries;|contention-resolution|lint", re.I)),
    ("rule wired to retry_deadline_at",
     re.compile(r"retry_deadline_at", re.I),
     re.compile(r"RESERVED|unused|no rule|nothing wired|do not wire|not wire|lint|corrected|re-key", re.I)),
    ("provider_reference UNIQUE-index claim while TL-12 open",
     re.compile(r"provider_reference.*UNIQUE index|UNIQUE index.*provider_reference|UNIQUE\(provider_reference\)", re.I),
     re.compile(r"NON-UNIQUE|non-unique|only after|would|do not add|lint", re.I)),
    ("stored-procedure ops boundary (2026-07-11: Java endpoints)",
     re.compile(r"audited stored procedure", re.I),
     re.compile(r"choose at build|lint", re.I)),
    ("dual control 'enforced by the procedure' (round 3: operation + approval workflow)",
     re.compile(r"enforced by the procedure", re.I),
     re.compile(r"lint", re.I)),
    ("retired deadline/budget suspension model (round 3)",
     re.compile(r"deadline suspension|deadlines?\s+(?:are\s+)?suspend(?:ed|s)?\b|budgets?\s+(?:are\s+)?(?:suspended|frozen)\b|suspends the (?:retry )?budget", re.I),
     re.compile(r"had no durable|nothing to|nothing needs|never suspend|zero attempts|no wall-clock|REMOVED|lint", re.I)),
]

for path in MAINTAINED:
    if path.name == "18-playbook-quality-self-check.md":
        continue  # the checklist NAMES the forbidden phrases by design
    for n, line in enumerate(lines_of(path), 1):
        for name, trip, allow in FORBIDDEN:
            if trip.search(line) and not allow.search(line):
                errors.append(f"{rel(path)}:{n}: forbidden phrase [{name}]: {line.strip()[:120]}")

# ---- Rule 5: test-matrix header range matches the actual max test id ----
matrix = PORTABLE / "10-test-matrix.md"
mlines = lines_of(matrix)
tids = [int(m.group(1)) for l in mlines for m in [re.match(r"### T-(\d+)", l)] if m]
if tids:
    header = "\n".join(mlines[:3])
    m = re.search(r"T-01\.\.T-(\d+)", header)
    if not m:
        errors.append(f"{rel(matrix)}: header lacks a T-01..T-NN range")
    elif int(m.group(1)) != max(tids):
        errors.append(f"{rel(matrix)}: header says T-01..T-{m.group(1)} but max test is T-{max(tids):02d}")

# ---- Rule 6: card-ID parity between task-card files and file 20 ----
card_ids = set()
for path in sorted((PORTABLE / "08-task-cards").glob("phase-*.md")):
    for l in lines_of(path):
        m = re.match(r"### ([A-Z]{1,3}-\d+[a-z]?) ", l)
        if m:
            card_ids.add(m.group(1))
seq = PORTABLE / "20-execution-sequence-and-decision-defaults.md"
seq_text = "\n".join(lines_of(seq))
seq_ids = set(re.findall(r"\b([A-Z]{1,3}-\d+[a-z]?)\b", seq_text))
for a, b, c, d in re.findall(r"([A-Z]{1,3})-(\d+) → ([A-Z]{1,3})-(\d+)", seq_text):
    if a == c:
        for i in range(int(b), int(d) + 1):
            seq_ids.add(f"{a}-{i:02d}")
            seq_ids.add(f"{a}-{i}")


def norm(s):
    p, _, num = s.partition("-")
    suffix = num.lstrip("0123456789")
    digits = num[: len(num) - len(suffix)] if suffix else num
    return f"{p}-{int(digits)}{suffix}" if digits.isdigit() else s


card_n, seq_n = {norm(i) for i in card_ids}, {norm(i) for i in seq_ids}
for missing in sorted(card_n - seq_n):
    errors.append(f"{rel(seq)}: card {missing} exists but is absent from the execution sequence")

# ---- Rule 7: every §N(.N) cited in the portable package exists in the spec ----
spec_lines = lines_of(ROOT / "requirment-v4.md")
headings = set()
for l in spec_lines:
    m = re.match(r"#{2,3}\s+(\d+)(?:\.(\d+))?", l)
    if m:
        headings.add(m.group(1))
        if m.group(2):
            headings.add(f"{m.group(1)}.{m.group(2)}")
for path in sorted(PORTABLE.rglob("*.md")):
    for n, line in enumerate(lines_of(path), 1):
        for m in re.finditer(r"§(\d+)(?:\.(\d+))?", line):
            key = f"{m.group(1)}.{m.group(2)}" if m.group(2) else m.group(1)
            if key not in headings:
                errors.append(f"{rel(path)}:{n}: cites §{key} — no such heading in requirment-v4.md")

if errors:
    print(f"DOC-LINT: {len(errors)} violation(s)")
    for e in errors:
        print("  " + e)
    sys.exit(1)
print("DOC-LINT: clean")
