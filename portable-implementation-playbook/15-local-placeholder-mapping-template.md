> **Purpose:** Blank local placeholder mapping template (original Section O). LOCAL-ONLY once filled.
> **When to use this file:** Copy to a LOCAL untracked file during D-01; fill during Phase P1; consult before every task.
> **Depends on:** 07-placeholder-glossary.md; 06-local-discovery-workflow.md.
> **Used by:** Every task card's local-mappings-required gate.
> **Safe to transfer:** yes (BLANK template only - once filled locally it must NEVER leave the work laptop or be sent externally)
> **Contains local code names:** no while blank; YES after local fill - the filled copy stays on the work laptop, never transferred

# O. Local-only placeholder mapping template

Copy this table to a LOCAL file on the work laptop (an ignored/
untracked location). Fill during Phase P1. It must NEVER leave the
work laptop — it is the one document that contains real names.
Status values: UNMAPPED / CONFIRMED / PARTIAL / MISSING / UNCLEAR /
BLOCKED. Two companion LOCAL files live beside this one, created by
D-01 from file 26 (T.2/T.3): the DIVERGENCE REGISTER and the FACTS
SHEET — same local-only rule.

| Placeholder component | Local file/class/table/job found | How I confirmed it | Existing tests found | Existing behavior to preserve | Required change | Requirement sections | Risk level | Owner / reviewer | Status | Notes |
|---|---|---|---|---|---|---|---|---|---|---|
| [Payment Request Creation Component] | | | | | | §6.8, §5.1, §3 | | | UNMAPPED | |
| [Payment Enrichment Component] | | | | | | §7.3, §7.0 | | | UNMAPPED | |
| [Provider POST Client] | | | | | | §5, §7.0, §11, §16.1 | | | UNMAPPED | |
| [Provider Response Parser] | | | | | | §7.2, §5 | | | UNMAPPED | |
| [Request Status Persistence Layer] | | | | | | §2.2, §10, §11, §14 | | | UNMAPPED | |
| [Reservation Repository] | | | | | | §3, §13 | | | UNMAPPED | |
| [Obligation Repository] | | | | | | §2.1, §6.7, §6.9, §11 | | | UNMAPPED | |
| [Retry Resolver Job] | | | | | | §7.4, §16.1, §11 | | | UNMAPPED | |
| [Status Query Resolver] | | | | | | §9.1–9.5 | | | UNMAPPED | |
| [Payment Status Feed Consumer] | | | | | | §8, §16.2 | | | UNMAPPED | |
| [Inbox / Processed Event Repository] | | | | | | §2.3, §8 | | | UNMAPPED | |
| [DB Migration Directory] | | | | | | §16.5, §16.6-4 | | | UNMAPPED | |
| [Stored Procedure / Trigger Area] | | | | | | §10.3 | | | UNMAPPED | |
| [Operator Admin Procedure Area] | | | | | | §9.3, §20 | | | UNMAPPED | |
| [Metrics / Alerting Layer] | | | | | | §14, §15, §16.3 | | | UNMAPPED | |
| [Reconciliation / Drift Scanner] | | | | | | §3, §10.3 L9 | | | UNMAPPED | |
| [Integration Test Suite] | | | | | | §16.6-6 | | | UNMAPPED | |
| [Contract Test Suite] | | | | | | §18-1, §16.5 | | | UNMAPPED | |
| payment_obligation (real table) | | | | | | §2.1 | | | UNMAPPED | |
| payment_request (real table) | | | | | | §2.2 | | | UNMAPPED | |
| processed_inbound_event (real table) | | | | | | §2.3 | | | UNMAPPED | |
| PaymentOrchestrationService (documented service) | | | | | | front matter | | | UNMAPPED | |
| PaymentEnrichmentService (documented service) | | | | | | front matter | | | UNMAPPED | |
| PaymentExecutionService (documented service) | | | | | | front matter | | | UNMAPPED | |
| PaymentNotificationConsumerService (documented service) | | | | | | front matter | | | UNMAPPED | |
| Upstream trade-message consumer | | | | | | §6.0–6.9, §16.2 | | | UNMAPPED | |
| Hazelcast posting-freeze toggle | | | | | | §16.1 | | | UNMAPPED | |
| Legacy compound status enum | | | | | | §10.4, ST-05 | | | UNMAPPED | |
| trade_snapshot_state (real table — NEW §2.4, deploys empty; map = confirm none exists/name collision check) | | | | | | §2.4, §6.1 | | | UNMAPPED | |
| [Upstream Snapshot Store Client] | | | | | | §6.0 (transport fact), §20-10 | | | UNMAPPED | |

