> **Purpose:** Local task execution report template - filled after EVERY task card (original Section R.2). LOCAL-ONLY once filled.
> **When to use this file:** Immediately after each task card's stop condition; before requesting review.
> **Depends on:** The executed task card; 15-local-placeholder-mapping-template.md (filled local copy).
> **Used by:** Local review process; the fill-the-execution-report rule in 08-task-cards/README.md.
> **Safe to transfer:** yes (BLANK template only - filled reports contain local file names and must NEVER leave the work laptop or be sent externally)
> **Contains local code names:** no while blank; YES after local fill - filled reports stay on the work laptop, never transferred

### R.2 Local-only task execution report template

Fill after EVERY task card; keep locally with the mapping document.

```text
Task ID:
Local mappings used:
Files changed:
Tests added/updated:
Validation commands run:
Result:
Failed tests:
Unexpected findings:
Business logic changed? yes/no
DIV-2 adaptations used? no / yes → six-item proof per row (round 18:
  precision+rounding / NULL semantics / same atomic tx /
  lockability / one writer / no mutable copy):
Requirement sections satisfied:
Remaining blockers:
Safe to proceed to next task? yes/no
```

