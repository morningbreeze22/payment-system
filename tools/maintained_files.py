# maintained_files.py — THE single maintained-file inventory.
#
# Consumed by doc-lint.py, history-extract.py, and history-verify.py so a
# newly added normative document is either checked by the WHOLE toolchain
# or by none of it (cleanup-plan review, required change 9).
#
# NOT maintained (never add here): implementation-playbook.md (frozen
# monolith), requirment-v4-annotated.md, HTML explainers, review artifacts,
# baseline-freeze records, decision-history.md (non-normative).

from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
PORTABLE = ROOT / "portable-implementation-playbook"

MAINTAINED = [ROOT / "requirment-v4.md", ROOT / "ops-console-proposal.md",
              ROOT / "failure-recovery-walkthrough.md", ROOT / "README.md",
              ROOT / "db-schema-dictionary.md"]
MAINTAINED += sorted(PORTABLE.rglob("*.md"))

if __name__ == "__main__":
    for p in MAINTAINED:
        print(p.relative_to(ROOT).as_posix())
    print(f"-- {len(MAINTAINED)} maintained files")
