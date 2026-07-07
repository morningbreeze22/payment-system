> **Purpose:** Minimal context packets U-01..U-03 — paste-alone briefs for a small-context local agent (original Section I, phase P5).
> **When to use this file:** Paired with the matching task-card file 08-task-cards/05-uetr-response-persistence.md — one packet per task, used as the working brief.
> **Depends on:** 09-minimal-context-packets/README.md; the matching task card; the requirement sections each packet cites; 07-placeholder-glossary.md.
> **Used by:** The local coding agent executing phase P5.
> **Safe to transfer:** yes
> **Contains local code names:** no

# Minimal Context Packets — Phase P5

```text
[U-01] Acceptance-only UETR persistence
Read: §5 (persistence rules) §7.2 §2.2. Invariant: rejection/collision UETRs name submissions under which NOTHING executes — never persisted, never overwritten.
Placeholders: [Provider Response Parser] [Request Status Persistence Layer]. Mappings: parser; uetr write sites.
Objective: single uetr write path keyed on response class; acceptance + original-replay persist; DUPLICATE/collision/rejects never; non-NULL never overwritten.
Tests: per-class persistence matrix; DUPLICATE leaves prior value intact. Stop: merged.
```

```text
[U-02] provider_reference persistence
Read: §2.2 (provider_reference) §8 (fallback) §5. Invariant: distinct field from uetr; UNIQUE makes silent reuse loud; never a dedup key.
Placeholders: [Provider Response Parser] [Request Status Persistence Layer]. Mappings: parser; CA-2 field name.
Objective: extract + persist; UNIQUE index; reuse observed → raise Q-17, don't drop the index silently.
Tests: persistence; loud uniqueness violation; no cross-assignment. Stop: merged.
```

```text
[U-03] UETR behavior tests
Read: §5 §8 (matching) §16.6-6. Invariant: a dead-UETR feed event never matches a row.
Placeholders: [Integration Test Suite]. Mappings: matching logic locatable.
Objective: tests: acceptance-persisted UETR matches its feed event; never-persisted rejection UETR → unmatched; uetr-NULL crash row → unmatched (recovered by §9).
Tests: the above. Stop: green.
```

