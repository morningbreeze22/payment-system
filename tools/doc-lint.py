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
    ("over-broad zero-required suppression (round 13: ONLY historical PROVIDER_REJECTED; validation always visible)",
     re.compile(r"skips? the marker-based ranks|all marker-based|NO marker-based exception|marker-based ranks are skipped|suppresses? (?:all|every) marker", re.I),
     re.compile(r"historical|ONLY the historical|lint", re.I)),
    ("§18-only waiver rule (round 16: MONEY_SAFETY_BLOCKING is a second non-waivable class)",
     re.compile(r"§18 BLOCKING items are non-waivable|only §18 .{0,30}non-waivable|any other FAIL", re.I),
     re.compile(r"MONEY_SAFETY|TWO non-waivable|two non-waivable|outside both classes|lint", re.I)),
    ("retired deadline/budget suspension model (round 3)",
     re.compile(r"deadline suspension|deadlines?\s+(?:are\s+)?suspend(?:ed|s)?\b|budgets?\s+(?:are\s+)?(?:suspended|frozen)\b|suspends the (?:retry )?budget", re.I),
     re.compile(r"had no durable|nothing to|nothing needs|never suspend|zero attempts|no wall-clock|REMOVED|lint", re.I)),
    ("superseded journal design (2026-07-17: full content, never load-bearing — review d00ef6a H1)",
     re.compile(r"content_ref|dedup-by-hash|once per distinct hash|journal failure (?:fails|pauses)|fail-the-claim", re.I),
     re.compile(r"no content_ref|no dedup|REJECTED|FUTURE|historical|replaces it|lint", re.I)),
    ("journal absolute failure wording (c8a92f1 H1: the narrow guarantee is canon)",
     re.compile(r"never pause, fail, or gate|NEVER pauses posting|no journal (?:condition|failure) (?:may|can) (?:pause|fail)|any insert error", re.I),
     re.compile(r"money-safety gate|incorrect payment outcome|statement-local|lint", re.I)),
    ("timeout classified as statement-local (928341a H2: timeouts are FATAL by default)",
     re.compile(r"statement timeout.{0,60}(?:swallow|local|continues|proceeds)|(?:swallow|statement-local).{0,60}statement timeout", re.I | re.S),
     re.compile(r"NOT here|FATAL|are not|never|lint", re.I)),
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

# ---- Rule 6e (rounds 19-20): safety-critical tokens checked PER TASK block, card vs packet ----
def _card_block(path, task_id):
    txt = "\n".join(lines_of(path))
    m = re.search(r"^### " + re.escape(task_id) + r" .*?(?=^### |\Z)", txt, re.M | re.S)
    return m.group(0) if m else ""

def _packet_block(path, task_id):
    txt = "\n".join(lines_of(path))
    m = re.search(r"^\[" + re.escape(task_id) + r"\].*?(?=^```\s*$)", txt, re.M | re.S)
    return m.group(0) if m else ""

P14C = PORTABLE / "08-task-cards" / "phase-14-rollout-and-go-live.md"
P14P = PORTABLE / "09-minimal-context-packets" / "phase-14-rollout-and-go-live.md"
P01C = PORTABLE / "08-task-cards" / "phase-01-discovery.md"
P01P = PORTABLE / "09-minimal-context-packets" / "phase-01-discovery.md"
P04C = PORTABLE / "08-task-cards" / "phase-04-identity-and-idempotency.md"
P04P = PORTABLE / "09-minimal-context-packets" / "phase-04-identity-and-idempotency.md"
P06C = PORTABLE / "08-task-cards" / "phase-06-factored-state-model.md"
P06P = PORTABLE / "09-minimal-context-packets" / "phase-06-factored-state-model.md"
P10C = PORTABLE / "08-task-cards" / "phase-10-retry-recovery-maybe.md"
P10P = PORTABLE / "09-minimal-context-packets" / "phase-10-retry-recovery-maybe.md"
SENTINEL_PAIRS = [
    ("GO-01", "F0", P14C, P14P),
    ("GO-01", "CUTOVER_POPULATION_GREENFIELD", P14C, P14P),
    ("GO-03", "F0", P14C, P14P),
    ("GO-03", "CUTOVER_POPULATION_GREENFIELD", P14C, P14P),
    ("GO-04", "CUTOVER_POPULATION_GREENFIELD", P14C, P14P),
    ("D-12", "CUTOVER_POPULATION_GREENFIELD", P01C, P01P),
    ("K-04", "ATTEMPT_STARTED", P04C, P04P),
    ("RC-02", "ATTEMPT_RESOLVED", P10C, P10P),
    ("ST-10", "LEASE_EXPIRED_MAYBE", P06C, P06P),
]


def _check_pair(cb, pb, token):
    missing = []
    if token not in cb:
        missing.append("card")
    if token not in pb:
        missing.append("packet")
    return missing


# negative self-test (review 5156f1f L1): the checker itself must catch a
# token missing from either side, and from both — run on every lint pass
assert _check_pair("x TOKEN y", "no", "TOKEN") == ["packet"]
assert _check_pair("no", "x TOKEN y", "TOKEN") == ["card"]
assert _check_pair("no", "no", "TOKEN") == ["card", "packet"]
assert _check_pair("a TOKEN", "b TOKEN", "TOKEN") == []

for task_id, token, cardf, packf in SENTINEL_PAIRS:
    cb, pb = _card_block(cardf, task_id), _packet_block(packf, task_id)
    if not cb:
        errors.append(f"{rel(cardf)}: card block {task_id} not found (rule 6e)")
        continue
    for side in _check_pair(cb, pb, token):
        where = rel(cardf) if side == "card" else rel(packf)
        errors.append(f"{where}: {task_id} {side} block lacks required safety token '{token}' (rule 6e TWO-SIDED — review 5156f1f L1)")

# ---- Rule 6f (round 20): canonical P14 execution order stated in index + file 20 ----
P14_ORDER = "GO-01 GO-02 GO-05 GO-04 GO-03"
idx_text = "\n".join(lines_of(PORTABLE / "01-playbook-index.md"))
if P14_ORDER not in idx_text:
    errors.append(f"01-playbook-index.md: canonical P14 order not stated verbatim ({P14_ORDER})")

# ---- Rule 6g (round 20): no trailing whitespace in maintained files ----
for path in MAINTAINED:
    for n, line in enumerate(lines_of(path), 1):
        if re.search(r"[ \t]+$", line):
            errors.append(f"{rel(path)}:{n}: trailing whitespace (breaks git diff --check)")

# ---- Rule 6h (CA-10): journal guard sentences present wherever the journal exists ----
ca10_present = any(
    "payment_attempt_journal" in "\n".join(lines_of(p)) for p in MAINTAINED
)
if ca10_present:
    ca_text = "\n".join(lines_of(PORTABLE / "12-companion-artifacts.md"))
    if "INSERT-only" not in ca_text or "NO runtime rule" not in ca_text:
        errors.append("12-companion-artifacts.md: payment_attempt_journal exists in the doc set but the CA-10 INSERT-only / no-runtime-read guard sentences are missing (rule 6h)")
    r16_text = "\n".join(lines_of(PORTABLE / "16-local-agent-instructions.md"))
    if "CA-10" not in r16_text:
        errors.append("16-local-agent-instructions.md: payment_attempt_journal exists in the doc set but rule 13 carries no CA-10 exception (rule 6h)")
    spec_text = "\n".join(lines_of(ROOT / "requirment-v4.md"))
    if "### 14.1" not in spec_text or "post_attempt_seq" not in spec_text:
        errors.append("requirment-v4.md: payment_attempt_journal exists in the doc set but the spec lacks §14.1 and/or post_attempt_seq — the journal must be spec-anchored, never playbook-only (rule 6h; review 5156f1f H2)")

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
