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
    ("approver identities as execution inputs (round 4: approval_id only)",
     re.compile(r"approvers\[2\]|two distinct (?:authenticated |enterprise-authenticated )?approver|secondApprover|second_approver", re.I),
     re.compile(r"derived|NEVER passed|never from parameters|never approver-identity|NEVER an approver identity|lint", re.I)),
    ("three-tables claim (round 5: trade_snapshot_state makes four)",
     re.compile(r"three (?:core )?tables|three-table model", re.I),
     re.compile(r"four|lint", re.I)),
    ("'every dead-end' exit over-claim (round 4/5: exactly three covered classes)",
     re.compile(r"every dead-end", re.I),
     re.compile(r"three|classes|lint", re.I)),
    ("TL-16 phrased as still open (round 5: answered — §6.1 admission)",
     re.compile(r"TL-16[^)]{0,40}watermark", re.I),
     re.compile(r"answered|resolved|lint", re.I)),
    ("stale 'procedure' terminology for §9.3/OP operations (rounds 3–5)",
     re.compile(r"§9\.3 procedure|§9\.3 \(procedure|outcome procedure|OP(?:-01)? procedure|MVP(?:-required)? procedure|\(default: procedure\)|procedure suite", re.I),
     re.compile(r"operation|lint", re.I)),
    ("'currency check' for the trade-snapshot fence (round 7: currency is a scope-key field)",
     re.compile(r"currency check|currency-check|verif\w* currency|proving currency|proved currency|trade-row currency|check currency", re.I),
     re.compile(r"fence|renamed|never|lint", re.I)),
    ("SNAPSHOT_POINTER_MISSING as a blocked_reason (round 8: structural claim gate; display label only)",
     re.compile(r"BLOCKED\(SNAPSHOT_POINTER_MISSING\)|blocked_reason\s*=\s*SNAPSHOT_POINTER_MISSING", re.I),
     re.compile(r"lint|violated|never", re.I)),
    ("unqualified 'two approvers' as a signature/input (round 8: approval_id only)",
     re.compile(r"two approvers", re.I),
     re.compile(r"derived|record|approval_id|lint", re.I)),
    ("PO-9 phrased as still open (round 10: answered — absence = amendment to zero)",
     re.compile(r"PO-9 (?:is |remains |stays |unanswered|open\b)|absence is a no-op|absence = a? ?NO-OP|INTERIM (?:until PO-9|absence)|PO-9 interim|until PO-9 is answered", re.I),
     re.compile(r"answered|amended|lint|was\b", re.I)),
    ("local cutoff machinery — history-tolerant nouns (round 10: the engine owns its calendar)",
     re.compile(r"cutoff calendar|cutoff margin|cutoff config|cutoff proximity", re.I),
     re.compile(r"retired|closed|engine|owns|lint|history|round 10|no cutoff|no local", re.I)),
    ("local cutoff machinery — imperative forms (round 11: narrow allowlist; 'engine'/'round 10' on the line is NOT evidence the instruction is dead)",
     re.compile(r"cutoff pre-?check|cutoff term|before (?:the )?cutoff|past the payment cutoff|cutoff-blocked|cutoff checks? (?:still )?appl|nearest cutoff|cutoff[- ]first|clear (?:the )?cutoff|must clear cutoff|cutoff passe[sd]|cutoff stub|bounded by the (?:payment )?cutoff|cutoff (?:→|->) ?BLOCKED|cutoff always wins|pre-cutoff|bypass the (?:payment )?cutoff", re.I),
     re.compile(r"retired|removed|no (?:local )?cutoff|never produced|RESERVED|may (?:NOT|not) be (?:built|required)|lint", re.I)),
    ("retired bootstrap/pointer machinery (round 10: greenfield)",
     re.compile(r"BOOTSTRAP_INCOMPLETE|bootstrap-incomplete|digest-NULL|pointer-residue|pointer[ -]coverage|\bS-11\b", re.I),
     re.compile(r"retired|removed|greenfield|lint|history|cannot exist|unreachable", re.I)),
    ("retired test/task ids as live evidence (round 11: T-21/T-36 retired, S-11 removed — never a requirement)",
     re.compile(r"\bT-36\b|\bT-21\b", re.I),
     re.compile(r"retired|removed|greenfield|lint|history|stub", re.I)),
    ("blanket anchor exclusion (round 11: ordering-aware retirement)",
     re.compile(r"anchors? (?:scopes? )?are (?:NOT|never) zeroed|(?:NOT|never) zeroed by absence|never zeroed by a", re.I),
     re.compile(r"round 11|ordering-aware|only when|strictly newer|lint", re.I)),
    ("absent-obligation watermark no-advance (round 11: the zeroing write ADVANCES it)",
     re.compile(r"(?:NOT|never) advanced for absent|absent obligations?.{0,30}(?:NOT|never) advanced", re.I),
     re.compile(r"retired|superseded|superseding|round 11|pre-PO-9|lint", re.I)),
    ("stored ui_step_status missing CANCELLED (round 12: three stored values)",
     re.compile(r"IN_PROGRESS / COMPLETED as stored|IN_PROGRESS/COMPLETED stored", re.I),
     re.compile(r"CANCELLED|lint", re.I)),
    ("reopening from COMPLETED only (round 12: CANCELLED reopens identically)",
     re.compile(r"reached .?COMPLETED.?:|if already COMPLETED;|increase after COMPLETED\)|increase on COMPLETED\b|against a COMPLETED scope", re.I),
     re.compile(r"CANCELLED|lint", re.I)),
    ("zeroed-overpay described as request BLOCKED (round 12: obligation derives IN_PROGRESS + latch)",
     re.compile(r"lands BLOCKED|latched/BLOCKED", re.I),
     re.compile(r"never|request state|lint", re.I)),
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
card_id_list = []
for path in sorted((PORTABLE / "08-task-cards").glob("phase-*.md")):
    for l in lines_of(path):
        m = re.match(r"### ([A-Z]{1,3}-\d+[a-z]?) ", l)
        if m:
            if m.group(1) in card_ids:
                errors.append(f"{rel(path)}: DUPLICATE card id {m.group(1)}")
            card_ids.add(m.group(1))
            card_id_list.append(m.group(1))
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

# ---- Rule 6b (round 9): tracker parity — every card has a tracker row, no ghosts ----
tracker = PORTABLE / "21-progress-tracker-template.md"
tracker_ids = set()
for l in lines_of(tracker):
    m = re.match(r"\|\s*[0-9]+[a-z]?\s*\|\s*([A-Z]{1,3}-\d+[a-z]?)\s*\|", l)
    if m:
        if m.group(1) in tracker_ids:
            errors.append(f"{rel(tracker)}: DUPLICATE tracker row {m.group(1)}")
        tracker_ids.add(m.group(1))
tracker_n = {norm(i) for i in tracker_ids}
for missing in sorted(card_n - tracker_n):
    errors.append(f"{rel(tracker)}: card {missing} has NO tracker row (the round-9 H-2 defect class)")
for ghost in sorted(tracker_n - card_n):
    errors.append(f"{rel(tracker)}: tracker row {ghost} has no task card")

# ---- Rule 6d (round 12): walkthrough scenario IDs are unique ----
walk = ROOT / "failure-recovery-walkthrough.md"
wids = {}
for n, l in enumerate(lines_of(walk), 1):
    m = re.match(r"\|\s*([A-Z]{1,2}-\d+)\s*\|", l)
    if m:
        if m.group(1) in wids:
            errors.append(f"{rel(walk)}:{n}: DUPLICATE walkthrough scenario id {m.group(1)} (first at line {wids[m.group(1)]})")
        else:
            wids[m.group(1)] = n

# ---- Rule 6c (round 9): the P3 chain order is stated verbatim in file 20 ----
P3_ORDER = "S-01, S-02, S-03, S-04, S-10, S-05, S-06, S-07, S-08, S-09"
if P3_ORDER not in seq_text:
    errors.append(f"{rel(seq)}: canonical P3 order not stated verbatim ({P3_ORDER})")

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
