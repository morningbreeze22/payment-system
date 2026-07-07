> **Purpose:** Task cards U-01..U-03 (SDK-assigned UETR response persistence + feed matching) (original Section H, phase P5).
> **When to use this file:** When executing the tasks of this phase, one card at a time, with the matching packet file from 09-minimal-context-packets/.
> **Depends on:** 08-task-cards/README.md; 01-playbook-index.md; 07-placeholder-glossary.md; the requirement sections cited per card; the locally filled mapping template.
> **Used by:** The local coding agent executing phase P5.
> **Safe to transfer:** yes
> **Contains local code names:** no

## H-Phase 5 — UETR response persistence (P5)

### U-01 — Acceptance-class-only UETR persistence

- **Task ID:** U-01
- **Title:** Persist uetr ONLY from acceptance-class responses; rejection/collision responses never write or overwrite it
- **Classification:** MVP normative implementation
- **Purpose:** §5: a rejection/collision response's UETR names a submission under which NOTHING EXECUTES; persisting it would orphan the real payment's feed events and could let a dead-UETR feed reject release a reservation of a payment that executed.
- **Prerequisites:** S-03 (uetr column UNIQUE); D-05 (response parsing map); TL-11(a) answer helpful (which field) — if unknown, mark the extraction site UNCLEAR and stub behind it.
- **Requirement sections / concepts to read:** §5 (persistence rules + identity chain), §7.2 (which responses are which class), §2.2 (uetr).
- **Placeholder components involved:** [Provider Response Parser], [Request Status Persistence Layer].
- **Local placeholder mappings required before starting:** parser mapped; response classes enumerable (CA-1 helps; the CLASS rule is implementable before the full table: acceptance + original-response-replay = persist; everything else = don't).
- **Local code areas to discover:** every write site of the uetr column.
- **How to locate:** F.16.
- **Implementation instructions:** centralize uetr writes to one code path taking the response class; persist on acceptance-class only (engine accepted; original-response replay per §16.6-1); never overwrite a non-NULL uetr from any response; DUPLICATE_REQUEST / collision / sync rejects never write (§7.2 flow rows).
- **Do not change:** feed-matching reads (IN tasks own them).
- **Tests to add:** acceptance persists; DUPLICATE_REQUEST leaves prior value (or NULL) intact (§16.6-6 named catalog entry); collision leaves intact; sync reject leaves intact; non-NULL never overwritten.
- **Edge cases:** a response carrying both acceptance semantics and a warning code — classify by CA-1; until classified, fail toward NOT persisting (reversible; the §9 sweep recovers by key).
- **Manual validation:** stub run of each class; inspect the row.
- **Expected outcome:** dead UETRs never persisted.
- **Failure signs:** "persist whatever the response carries" convenience code anywhere.
- **Common mistakes:** overwriting on the downgrade re-POST's fresh SDK-minted UETR after a DUPLICATE_REQUEST answer.
- **Completion criteria:** tests green.
- **Stop condition:** merged.
- **Next task:** U-02.

### U-02 — provider_reference persistence

- **Task ID:** U-02
- **Title:** Persist any other engine-assigned reference as provider_reference — a distinct field, never merged with uetr
- **Classification:** MVP normative implementation
- **Purpose:** §2.2: secondary feed-matching key (§8) with fail-closed fallback semantics; UNIQUE index makes silent reuse loud.
- **Prerequisites:** S-03; U-01.
- **Requirement sections / concepts to read:** §2.2 (provider_reference), §8 (fallback rule), §5 ("any OTHER engine-assigned reference").
- **Placeholder components involved:** [Provider Response Parser], [Request Status Persistence Layer].
- **Local placeholder mappings required before starting:** parser mapped.
- **Local code areas to discover:** which response field(s) carry a non-UETR reference (MUST_VERIFY_LOCALLY / CA-2).
- **How to locate:** D-05 memo + CA-2.
- **Implementation instructions:** extract + persist into provider_reference; UNIQUE index per CA-4 (violation → loud error + investigation, per §8's "silent reuse loud" intent — TL-12 pending); never copied into uetr.
- **Do not change:** uetr logic (U-01).
- **Tests to add:** persistence; uniqueness violation surfaces loudly; fields never cross-assigned.
- **Edge cases:** engine reuses references per day/batch (TL-12 UNCONFIRMED) — the UNIQUE index may then reject legitimate rows: if observed locally/sandbox, record and raise Q-17; do not silently drop the index (decision belongs to the owner).
- **Manual validation:** stub run; row inspection.
- **Expected outcome:** reference captured, distinct, loud on reuse.
- **Failure signs:** merged uetr/reference field.
- **Common mistakes:** treating the reference as a dedup key (§5: nothing money-safe keys on it).
- **Completion criteria:** tests green.
- **Stop condition:** merged.
- **Next task:** U-03.

### U-03 — UETR behavior test set

- **Task ID:** U-03
- **Title:** Feed-matching + non-persistence integration tests for UETR rules
- **Classification:** MVP normative implementation
- **Purpose:** lock the §5 persistence rules against regressions and prove a dead-UETR feed event cannot match.
- **Prerequisites:** U-01, U-02; IN-05 not required (use a direct call to the matching logic if the consumer isn't rebuilt yet — else defer the feed-side case to IN-06 and note it).
- **Requirement sections / concepts to read:** §5, §8 (matching), §16.6-6 (named entry).
- **Placeholder components involved:** [Integration Test Suite].
- **Local placeholder mappings required before starting:** matching logic locatable.
- **How to locate:** F.7/F.16.
- **Local code areas to discover:** none new.
- **Implementation instructions:** tests: acceptance-class persists + a feed event under that UETR matches the row; a rejection-class response's UETR (never persisted) → a feed event under it goes UNMATCHED (logged + counted + acked path once IN-05/06 exist; before that, assert no row resolves).
- **Do not change:** production code.
- **Tests to add:** the above.
- **Edge cases:** crash-before-response rows (uetr NULL) — feed event unmatched, recovered by §9 (assert unmatched here).
- **Manual validation:** n/a.
- **Expected outcome:** UETR rules regression-locked.
- **Failure signs:** matching falls through to fuzzy matching on anything besides the §8 fallback rule.
- **Common mistakes:** none beyond the above.
- **Completion criteria:** green.
- **Stop condition:** green; report.
- **Next task:** ST-01.


---

## Phase handoff summary (P5 → P6)

- **Phase outputs:** uetr persisted from acceptance-class responses ONLY (never overwritten; never from DUPLICATE_REQUEST/collision/rejects); provider_reference persisted as a distinct field with loud uniqueness.
- **Blockers to carry forward:** TL-11(a) field confirmation if still UNCLEAR (CT-07 settles it); TL-12 reference-uniqueness question open (fail-closed fallback stands).
- **Local mapping rows expected filled:** [Provider Response Parser] change notes; uetr/provider_reference write sites recorded.
- **Tests expected to exist:** per-response-class persistence matrix (T-15/T-16), dead-UETR non-matching, DUPLICATE-leaves-prior-uetr-intact (spec-named test).
- **Next phase entry condition:** U-03 green.
