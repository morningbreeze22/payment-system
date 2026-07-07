> **Purpose:** Settled assumptions and non-goals binding every task (original Section B).
> **When to use this file:** Read once before Phase P1; re-read whenever scope pressure or a rewrite temptation appears.
> **Depends on:** 00-README.md; requirment-v4.md sections 1, 1.1, 18-20.
> **Used by:** Every task card; 16-local-agent-instructions.md rules 6/13/15.
> **Safe to transfer:** yes
> **Contains local code names:** no

# B. Assumptions and non-goals

**Assumptions**

```text
1. The existing system already implements the core payment-processing
   business logic: how to make a payment, account detection,
   debit-party lookup, address lookup, enrichment, validation, and
   payment construction. All of it MUST BE PRESERVED.
2. This work is an ENHANCEMENT of the current system, not a rewrite.
   Prefer additive changes (new columns, new guards, new jobs) over
   destructive rewrites.
3. `requirment-v4.md` is the baseline implementation specification;
   all accepted review findings are already folded into it. This is a
   fresh planning session — nothing is inferred from prior review
   conversations.
4. The target stack is as documented: Java Spring Boot, Oracle DB,
   Spring Kafka, Hazelcast (§ front matter). Oracle DDL / CHECK
   constraints / function-based unique indexes / triggers / audited
   stored procedures; Spring transactions, repositories, scheduled
   jobs; Spring Kafka consumers with inbox idempotency; Hazelcast
   posting freeze.
5. The data model is exactly three core tables: payment_obligation,
   payment_request, processed_inbound_event (§2). No new persistent
   tables, journals, outboxes, parked-event tables, attempt-history
   tables, manual-action tables, or audit-history tables. A task that
   appears to need a new table is a SPEC_CONFLICT, not a new table.
```

**Non-goals**

```text
1. NO business-rule redesign. NO rewrite of payment decision logic.
   NO change to existing payment attributes unless a concrete
   correctness invariant in requirment-v4.md requires it. If a local
   implementation appears to require changing a business rule, mark
   it BUSINESS_RULE_CHANGE_REQUIRED and name the requirement section
   that creates the need — then stop.
2. NO source-code-specific guesses. Placeholders + local discovery
   only.
3. NO new findings. This playbook does not review, critique, or
   improve the design. Rejected alternatives recorded in the spec
   (derived committed_amount, attempt-history table, materiality
   re-POST, auto-unlatch, UETR generation/validation, runtime
   collision-contract gating) stay rejected and are not re-proposed.
4. NO re-opening of §1.1 Basic Agreements (BA-1 scope-key mutability,
   BA-2 no upstream cancellation, BA-3 ordering is upstream's
   responsibility) or the §1 contract facts.
5. NO implementation of future work (§19.1 completion signal, §19.2
   returned funds, §19.3 retry-after-reject, §5.2 DR runbook tooling,
   ops console beyond the one MVP procedure, §6.6 key-only anchoring
   before TL-7 confirms) unless §18 explicitly makes it BLOCKING.
6. NO full ops console. The ONLY manual-operation implementation work
   at MVP is the §9.3 apply-platform-verified-outcome audited stored
   procedure (§18 BLOCKING item 3, §20). Everything else in §20 is
   future / PO discussion.
7. The old compound status may survive only as a derived display
   label (§10.4); migration of business logic away from any legacy
   compound status enum is gradual and safe (Phase P6), never a
   big-bang deletion.
```

