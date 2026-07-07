> **Purpose:** Map from the section numbers cited by every card/packet to the exact heading text in requirment-v4.md, so a small-context agent can search-and-read only the sections it needs.
> **When to use this file:** Every task, when loading the requirement sections the packet cites. Search the spec for the exact heading text listed here.
> **Depends on:** requirment-v4.md (transferred alongside the package).
> **Used by:** Every task; 23-task-kickoff-prompt.md step 4.
> **Safe to transfer:** yes
> **Contains local code names:** no

# Requirement section map (requirment-v4.md)

A citation like "§6.4" means: search requirment-v4.md for the heading text on the 6.4 row below and read that section (until the next heading of the same or higher level). "§18 BLOCKING item N" and "TL-N"/"PO-N"/"upstream ask N" live in section 18. This map lists heading TEXT (stable), not line numbers (fragile).

```text
Payment Orchestration System — Requirements v4 (Factored State Model)
1. System Context and Integration Model
1.1 Basic Agreements (settled with PO and users — not re-challengeable)
2. Data Model
2.1 payment_obligation
2.2 payment_request — the factored state model
2.3 processed_inbound_event (inbox)
3. Money: Reservation Semantics and Invariants
4. Derived State
4.1 Step-status predicate
4.2 Active-exception derivation
4.3 Stored derivation inputs
4.4 Evidence rules (feed-status precedence)
4.5 Next-actor derivation (never stored)
5. Request Identity and Disaster Recovery
5.1 Deterministic derivation (the DR keystone)
5.2 Post-restore runbook (post-MVP — good to have)
6. Inbound Flow 1: Upstream Trade Messages
6.0 Message contract (normative)
6.1 Normal processing
6.2 Zero shortfall
6.3 Amount immutability
6.4 Auto-cancellation of un-posted requests
6.5 Step reopening
6.6 Validation failure before any request exists
6.7 Message ordering and staleness guard
6.8 Standing shortfall re-evaluation (single request-creation point)
6.9 Ordering-guard inventory
7. POST Execution and Failure Classification
7.0 Instruction assembly (fresh per attempt) and repost_permitted
7.1 Submission-state definitions
7.2 POST-failure classification
7.3 Enrichment outcome classification
7.4 Retry policy
8. Inbound Flow 2: Payment Platform Status Feed
9. Status-Query Resolution (Ambiguous and Missed Outcomes)
9.1 Query outcomes
9.2 NOT_FOUND semantics (trust-age rule)
9.3 Escalation
9.4 The release-rights invariant
9.5 Resolver scope
10. Request State Model
10.0 State machine at a glance (diagram)
10.1 Global rules
10.2 Per-dimension transition rules
10.3 Legality matrix
10.4 Display labels (humans only — no rule may key on these)
10.5 Flow reference table
11. Concurrency Model
12. Read Model for the UI Card
13. Exception Categories and Overpay Policy
14. Storage Scope and Logging
15. Monitoring
16. Engineering & Operational Requirements
16.1 Resiliency
16.2 Kafka consumption (both inbound flows)
16.3 Security
16.4 Amount & time hygiene
16.5 Deployment, capacity, contracts
16.6 Configuration inventory and required companion artifacts
17. Core Requirements Summary
18. Open Items
BLOCKING — must be answered before implementation
Requiring PO review
Requiring tech lead review
Upstream contract asks
Resolved: workflow advancement
19. Future Work (documented, not scheduled)
19.1 Outbound completion signal to the reconciliation system
19.2 Returned funds and reconciliation visibility
19.3 Ops retry-after-provider-reject (FUTURE, pending PO approval)
20. Manual Operations — Open Questions (PO discussion needed · future implementation)
```
