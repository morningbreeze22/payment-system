> **Purpose:** A copy-paste kickoff prompt that starts the local agent on exactly one task with exactly the right context — nothing more.
> **When to use this file:** Every task. The human driver (or the agent itself at session start) fills the placeholders and pastes it.
> **Depends on:** 20-execution-sequence-and-decision-defaults.md (which task is next); 22-requirement-section-map.md.
> **Used by:** The human driver / the agent's session bootstrap.
> **Safe to transfer:** yes
> **Contains local code names:** no (the filled prompt may name local files — filled prompts stay local like everything else that names local code)

# Task kickoff prompt (template)

Fill the three placeholders, paste, run. One task per prompt.

```text
You are executing exactly ONE task from a portable implementation
playbook: task <TASK-ID> in
portable-implementation-playbook/08-task-cards/<PHASE-FILE>.

Load ONLY, in this order:
1. portable-implementation-playbook/16-local-agent-instructions.md
   (binding rules — obey all 22).
2. portable-implementation-playbook/20-execution-sequence-and-decision-defaults.md
   (gates and defaults) AND your LOCAL progress tracker copy of
   21-progress-tracker-template.md (confirm <TASK-ID> is the next
   eligible row and its wait-on per file 20 is satisfied;
   the decision defaults DD-1..DD-9 override any "coordinate/choose"
   wording in the card).
3. The <TASK-ID> card in 08-task-cards/<PHASE-FILE> and the <TASK-ID>
   packet in 09-minimal-context-packets/<PHASE-FILE>. The card wins
   over the packet; requirment-v4.md wins over the card.
4. The requirement sections the card cites, from requirment-v4.md —
   find them by heading text via
   portable-implementation-playbook/22-requirement-section-map.md.
   Read ONLY those sections.
5. portable-implementation-playbook/07-placeholder-glossary.md —
   ONLY the entries for placeholders the card names.
6. From the LOCAL mapping file: ONLY the rows for those placeholders.
   If a required row is not CONFIRMED, the task is locally BLOCKED —
   report and stop.
6b. From the LOCAL divergence register (file 26 T.2): ONLY the rows
   touching this card's tables/components — apply their recorded
   resolutions when translating names/types. An OPEN DIV-3/DIV-4
   row on this card's scope = the task is BLOCKED — report and stop.
7. If <TASK-ID> is an implementation card (writes code):
   portable-implementation-playbook/24-implementation-mechanics.md —
   the M1–M6 recipe(s) the card's shape needs plus the matching M8
   SHAPE checklist(s). "The CAS", "under the lock", "the claim",
   "the scanner" in the card MEAN those recipes.

Then execute the card's implementation instructions. Do not read the
whole repository — EXCEPT that when the card uses inventory/audit
wording ("every site", "all writers", "grep"), repository-wide
READ-ONLY SEARCH is mandatory (rule 4): search wide, LOAD only the
hits, MODIFY only the card's scope. Do not touch anything the card's
"Do not change" field names. Do not change business rules (if a
business-rule change seems required → report
BUSINESS_RULE_CHANGE_REQUIRED and stop). Do not create new tables
(report SPEC_CONFLICT and stop) — SINGLE exception, per rule 13:
the CA-9 ops-schema pending-approval store, permitted ONLY when the
current card explicitly requires it (CA-9 / OP cards); every other
new table remains forbidden. If the team runs parallel work streams
(file 26 T.4): confirm <TASK-ID> belongs to YOUR stream and every
cross-stream prerequisite (shared helper) is on merged main — not
on another stream's branch; card prerequisites always outrank the
stream map. If this card touches snapshot admission, obligations,
or trade_snapshot_state and the CUTOVER_POPULATION_GREENFIELD proof
(file 26 T.1, premise P-B) is not on record in the facts sheet, the
task is BLOCKED — report and stop.

Instruction precedence when sources seem to disagree (round 17):
requirment-v4.md → the card's prerequisites/invariants → the
24-implementation-mechanics recipes → recorded divergence-register
resolutions → the file-26 stream map (an optimization hint) → this
prompt's wording. Lower never overrides higher.

Finish by:
- running the card's "Tests to add" and the surrounding suite,
- performing the card's "Manual validation",
- ticking the matching M8 SHAPE checklist(s) from
  24-implementation-mechanics.md (fix or report-by-name any unticked
  line),
- filing go-live evidence per 25-golive-verification-procedures.md if
  the card feeds a checklist Q item,
- filling the execution report
  (19-local-task-execution-report-template.md format),
- updating the LOCAL progress tracker row for <TASK-ID>,
- STOPPING at the card's stop condition. Do not start the next task.

Original-section cross-references inside preserved content ("Section
O", "Playbook Index", …) resolve via the map in 00-README.md.
```

Placeholders:

- `<TASK-ID>` — e.g. RG-07
- `<PHASE-FILE>` — e.g. phase-07-reservation-and-release-guards.md
