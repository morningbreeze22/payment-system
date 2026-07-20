# Documentation Cleanup Strategy

> **STATUS 2026-07-19 (superseded in part — PO descope):** the history
> extraction was executed as LITERAL DELETION ONLY (see
> `history-cleanup-outcome.md`): no Class-C rewrites, no ADR register, no
> annotation-ban lint rule, no three-commit ceremony — those parts of this
> document and of the reviewed extraction plan were NOT adopted. Phases 3–7
> below (control IDs, authority normalization, generation) are an OPTIONAL
> FUTURE program, not adopted process; if ever picked up, they run behind
> implementation and never block it. This file is NON-NORMATIVE — the
> maintained documents win on any conflict.
>
> **Amendments at adoption:**
> 1. **Sequencing is decided:** Phases 1–2 (baseline freeze + history extraction)
>    run BEFORE the coding-agent handoff; Phases 3–7 run BEHIND implementation as
>    infrastructure work (bundle-eligible). Nothing in Phases 3–7 delays or blocks
>    the handoff green-lit by the 8bf0aba closure review.
> 2. **Control IDs are narrow:** Phase 3 assigns stable IDs ONLY to the frozen
>    high-value contracts (the set the doc-lint already freezes: P3 order,
>    canonical keyset tuple, consequence vocabulary, no-POST-re-claim boundary,
>    closing-PASS rule, I6, lock order, delivery contract, …), not to
>    "important requirements" broadly. §-references remain the general citation
>    system.
> 3. **Generators mirror authored decisions; they never make them.** The
>    execution order is AUTHORED in file 20 (it encodes Oracle semantics and
>    conditional human decisions, not derivable dependencies); generators emit
>    the index line, tracker, and next-task chain FROM it. The same rule applies
>    to every generated view of an authored fact.
> 4. **Packet generation stays metadata-only** until the mapping generators have
>    earned trust; packet BODIES remain authored (they contain engineered
>    judgment about which warnings to repeat).
>
> The history extraction was completed as the descoped literal-span deletion
> recorded in `history-cleanup-outcome.md`; the remaining amendments above
> (narrow control IDs; generators mirror authored decisions; packet bodies
> stay authored) bind the OPTIONAL future program only.

## Executive recommendation

The best long-term cleanup is not to make all existing files individually cleaner. It is to reduce the number of independently maintained copies of the same fact.

The recommended model is:

> A small set of authoritative authored sources, machine-generated execution views, and a separate non-normative decision history.

The proposed annotation/history extraction remains useful, but it should be treated as the first cleanup phase rather than the final document architecture. It removes accumulated review noise; authority normalization and generation prevent that noise and cross-file drift from returning.

## Target document architecture

Each kind of information should have one authoritative owner:

| Information | Authoritative source | Treatment elsewhere |
|---|---|---|
| Business facts, external contracts, and money invariants | Requirements | Refer to stable control IDs |
| Schema, state machine, locks, and transaction boundaries | Architecture | Task cards must not redefine them |
| Implementation order and dependencies | Execution registry | Generate indexes, trackers, and phase tables |
| Test definitions | Test catalog | Cards reference test IDs |
| Go-live gates and evidence requirements | Gate registry | Generate checklists and evidence layout |
| Current open and closed decisions | Decision register | Requirements reference blocking decision IDs |
| Historical evolution | `decision-history.md` | Non-normative; excluded from normal agent context |
| Agent working context | Generated packets | Generated from the authoritative sources |

A practical authored core could contain approximately five to seven sources:

```text
requirment-v4.md              business facts and invariants
architecture.md              schema, state, and concurrency
implementation-mechanics.md  Java, Spring, Kafka, and Oracle recipes
operations.md                alerts, recovery, and manual operations
delivery.md                  migration, rollout, and go-live
controls.yaml                traceability and frozen tokens
decisions.md                 current open and closed decisions
```

The existing `requirment-v4.md` filename should not be renamed during cleanup. Renaming, semantic restructuring, and history removal should not be combined in one change.

## Markdown owns meaning; registries own mappings

Do not convert the payment design into a large YAML domain-specific language. That would replace document duplication with tooling complexity.

Markdown should continue to express:

- business meaning and rationale;
- exact invariants;
- failure direction;
- external contract assumptions;
- state transitions; and
- money effects.

Machine-readable registries should contain only mechanical relationships and frozen identifiers. For example:

```yaml
controls:
  PAY-I6:
    title: One active request per obligation
    requirement: requirements.md#I6
    architecture: architecture.md#active-request-index
    tasks: [S-05, RG-06]
    tests: [T-12, T-19]
    gates: [Q8]
    severity: money_safety_blocking
```

The generator does not need to understand payment semantics. It should generate references, inventories, coverage tables, and consistency checks.

## Recommended cleanup phases

### Phase 1 — Freeze the current clean baseline

Before restructuring:

- record the baseline commit SHA;
- retain a complete document snapshot;
- record the current review result;
- save documentation-lint output;
- save card/packet parity, link, section-reference, and frozen-rule results; and
- identify the exact maintained-file inventory.

This creates a trusted comparison point for every later cleanup.

### Phase 2 — Extract review history

Execute the approved annotation-extraction plan using exact, human-approved replacements:

- Class A/B mechanical removals;
- individually approved Class C present-tense rewrites;
- byte-identical Class D keep-spans;
- a non-normative `decision-history.md`; and
- a verifier that applies the frozen replacement manifest to the old Git blobs.

The result should remove provenance noise while preserving current rules and rationale.

### Phase 3 — Assign stable control IDs

Assign stable identifiers to important requirements and safeguards, for example:

```text
PAY-MONEY-01
PAY-IDEMP-03
PAY-STATE-07
PAY-CONC-04
PAY-UI-02
PAY-GO-05
```

Control IDs should represent durable concepts, not section numbers, review rounds, or commit hashes. Tasks, tests, gates, and implementation reports should reference these identifiers instead of copying long explanations.

### Phase 4 — Establish one author per fact type

For every repeated statement, decide which source owns it:

- requirements own business rules;
- architecture owns data and concurrency design;
- mechanics owns implementation recipes;
- task cards own local implementation actions and acceptance conditions;
- the test catalog owns full test definitions;
- the gate registry owns go-live gate definitions; and
- the decision history owns superseded alternatives and review provenance.

Other documents should reference or generate the fact. They should not independently restate it.

### Phase 5 — Generate the highest-drift artifacts first

Begin with mechanical artifacts whose duplication provides little semantic value:

1. Card/packet ID inventory and parity.
2. Requirement-to-task-to-test-to-gate traceability.
3. Execution sequence and progress tracker.
4. Frozen vocabulary and ordering tables.
5. Go-live checklist structure.
6. Metadata portions of minimal-context packets.

Do not initially generate complex business explanations or task instructions. The first generators should handle mappings, ordering, identifiers, and boilerplate only.

### Phase 6 — Reduce the maintained set

Classify every document as one of:

```text
NORMATIVE_SOURCE
AUTHORED_EXECUTION_SOURCE
GENERATED
NON_NORMATIVE_GUIDE
ARCHIVED
```

Documentation lint, history lint, generation, link validation, and other checks must consume this same inventory. The goal is not necessarily fewer files; it is fewer files that can independently change the design.

### Phase 7 — Switch the agent reading path

The normal coding-agent context should become:

```text
task card
→ generated minimal-context packet
→ cited canonical control or requirement
→ required implementation-mechanics slice
→ locally mapped source files
```

Agents should not normally load:

- decision history;
- previous review annotations;
- the complete test catalog;
- the complete go-live document;
- the archived monolithic playbook; or
- HTML explainers.

## Recommended treatment of current files

| Current artifact | Recommended treatment |
|---|---|
| `requirment-v4.md` | Keep canonical initially; remove history, then slim incrementally |
| `db-schema-dictionary.md` | Retain as an architecture view; generate selected tables later |
| Task cards | Keep locally authored implementation instructions |
| Minimal-context packets | Highest-priority generation candidate |
| `10-test-matrix.md` | Make the single test catalog or generate it from structured test records |
| `20-execution-sequence-and-decision-defaults.md` | Generate sequencing from task dependencies where possible |
| `21-progress-tracker-template.md` | Generate completely |
| `22-requirement-section-map.md` | Generate completely |
| `17-go-live-checklist.md` | Generate from the gate registry |
| `25-golive-verification-procedures.md` | Keep detailed authored procedures; generate its checklist/index portions |
| `implementation-playbook.md` | Remain archived and untouched |
| HTML files | Remain non-normative explanatory artifacts |
| `decision-history.md` | Add as non-normative history; exclude from normal agent context |

## Validation model

Every generated artifact should include a generated-file header and fail CI if manually edited. CI should verify:

- generated output is current and reproducible;
- all control references resolve;
- every required control maps to its implementation task, test, and gate;
- task/card/packet inventories match;
- execution dependencies are acyclic and the published linear order is valid;
- frozen vocabularies and ordering tuples match their authoritative source;
- no normative file contains forbidden review annotations;
- archived and non-normative files are not included in agent packages; and
- high-value money, identity, state, concurrency, provider, and go-live rule slices remain stable.

Generators should be deterministic, small, and easy to inspect. A simple YAML/Markdown/Python toolchain is preferable to a documentation platform or complex schema language.

## Migration approach

Do not perform a big-bang rewrite. Use projection migration:

1. Keep the current clean baseline authoritative.
2. Build the new registry and generated views alongside it.
3. Produce source maps from every new paragraph or generated section to the old authoritative location.
4. Run both old and new validation views until coverage and frozen-rule checks agree.
5. Switch the README authority declaration only after an external closure review.
6. Archive superseded views only after the new source is proven complete.

This avoids silently losing a rule while reorganizing otherwise correct documents.

## Practices to avoid

- Do not rewrite all requirements at once.
- Do not combine cleanup, filename renaming, task restructuring, and design changes.
- Do not use unconstrained model summarization to generate normative prose.
- Do not give a generator business-decision authority.
- Do not delete current rationale merely because it contains provenance.
- Do not allow the ADR register to become a second normative specification.
- Do not replace the current package with another large monolith.
- Do not archive old sources before baseline, source-map, and coverage verification.
- Do not generate implementation instructions until the simpler mapping generators are stable.

## Final recommendation

Use a three-part strategy:

1. **Extract history** — complete the annotation cleanup safely.
2. **Normalize authority** — establish one owner and stable control ID for every fact.
3. **Generate repetition** — generate packets, maps, trackers, checklists, and traceability from the authoritative sources.

The annotation sweep addresses accumulated historical noise. Authority normalization and deterministic generation address the reason that noise and inconsistency recur.

The success criterion should not be that every document is shorter. It should be:

> A design change requires judgment in exactly one authoritative location; every other affected artifact is generated or mechanically verified.
