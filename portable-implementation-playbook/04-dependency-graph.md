> **Purpose:** Implementation dependency graph and the eight mandated orderings, including all four §18 BLOCKING gates (original Section D).
> **When to use this file:** Before entering any phase; whenever a re-ordering or shortcut is proposed.
> **Depends on:** 01-playbook-index.md; 03-requirement-classification.md.
> **Used by:** Phase entry/exit decisions; GO-01 rollout sequencing.
> **Safe to transfer:** yes
> **Contains local code names:** no

# D. Implementation dependency graph

Source-code-agnostic graph of implementation AREAS (phase IDs from
Section E). Arrows read "must be settled before".

```text
                      ┌────────────────────────────────────────────┐
                      │ P1 DISCOVERY (read-only; fills Section O)  │
                      └───────────────┬────────────────────────────┘
                                      ▼
        ┌───────────────────────────────────────────────────────────┐
        │ P2 §18 BLOCKING GATES + COMPANION ARTIFACTS                │
        │  B-01 snapshot residue (§18-0) ── blocks IN-02 (§6 flow)   │
        │  B-02 sandbox access (§18-1) ── blocks P8 execution        │
        │  B-03 §18-2 CLOSED r10 — engine owns the cutoff calendar   │
        │  B-04 MAYBE exit decision (§18-3) ── default = P11         │
        │  CA-1..9 artifact authoring (owners per §16.6)             │
        └───────┬───────────────────────────────────────────────────┘
                ▼
        ┌──────────────────────────────┐
        │ P3 SCHEMA & MIGRATION        │  ← scope key settled
        │ (tables, CHECKs, I6, triggers│    (§1 contract facts);
        │  indexes; expand/contract)   │    CA-4 is the gate
        └───────┬──────────────────────┘
                ▼
        ┌──────────────────────────────┐     ┌───────────────────────┐
        │ P4 IDENTITY (deterministic   │────▶│ P8 PROVIDER CONTRACT  │
        │ key gen + write-ahead        │     │ TESTS (sandbox; §18-1)│
        │ persistence + golden vectors)│     │ runs in parallel;     │
        └───────┬──────────────────────┘     │ PASS gates re-POST    │
                ▼                            │ reliance (P10) and    │
        ┌──────────────────────────────┐     │ go-live               │
        │ P5 UETR RESPONSE PERSISTENCE │     └───────────────────────┘
        │ (acceptance-class only)      │
        └───────┬──────────────────────┘
                ▼
        ┌──────────────────────────────┐
        │ P6 FACTORED STATE MODEL      │  schema (P3) before state-
        │ (dimensions, CAS discipline, │  machine persistence; state
        │  legality, claims/leases)    │  legality before retries
        └───────┬──────────────────────┘
                ▼
        ┌──────────────────────────────┐
        │ P7 RESERVATION / RELEASE     │  reservation semantics before
        │ GUARDS + DERIVATION (§3, §4) │  completion derivation
        └───────┬──────────────────────┘
                ▼
        ┌──────────────────────────────┐
        │ P9 UPSTREAM + FEED CONTRACT  │  needs state legality (P6),
        │ HANDLING (§6, §8)            │  money choreography (P7)
        └───────┬──────────────────────┘
                ▼
        ┌──────────────────────────────┐
        │ P10 RETRY / RECOVERY /       │  needs P6 legality, P7 money,
        │ MAYBE RESOLVER (§7.4, §9)    │  P8 PASS before auto-downgrade
        └───────┬──────────────────────┘  reliance (§18-2 closed r10)
                ▼
        ┌──────────────────────────────┐
        │ P11 APPLY-PLATFORM-VERIFIED- │  MAYBE-row terminal exit must
        │ OUTCOME OPERATION (§9.3)     │  exist before go-live (§18-3)
        └───────┬──────────────────────┘
                ▼
        ┌──────────────────────────────┐
        │ P12 DRIFT / RECONCILIATION   │  verifies P7 invariants in
        │ TRIPWIRES (§3, §8, L9)       │  production
        └───────┬──────────────────────┘
                ▼
        ┌──────────────────────────────┐
        │ P13 OBSERVABILITY + RUNBOOKS │  observability/runbooks
        │ (§15, §16.6-7)               │  before rollout
        └───────┬──────────────────────┘
                ▼
        ┌──────────────────────────────┐
        │ P14 MIGRATION / ROLLOUT /    │  all §18 gates + Section Q
        │ ROLLBACK / GO-LIVE GATES     │  checklist
        └──────────────────────────────┘
```

**Why this order (each required ordering, explicitly):**

```text
1. §18 item 0 residue BEFORE the §6 consumer freeze: the model is a
   §1 contract fact (multiple payments per trade; snapshot messages;
   tuple unique within a snapshot → NO discriminator), so the
   scope-key/identity/schema freeze is NOT gated here. The §6
   consumer (IN-02) waits on the B-01 residue: written uniqueness
   guarantee (upstream ask 5 — confirmed, paper pending), intake
   validation (§6.0). (PO-9 ANSWERED: absence = zero; TL-16
    : §6.1 admission + §2.4.)
2. Schema BEFORE state-machine persistence: the four dimension
   columns, CHECK constraints, I6, and triggers (§2.2, §10.3) are the
   substrate every CAS in P6 writes against; code written before the
   DB backstops exists is unverifiable against them.
3. Identity persistence BEFORE provider POST changes: §5's write-ahead
   rule ("no POST under an unpersisted caller-supplied identity") is
   the money-safety keystone; the posting path (P5/P6/P10) must find
   identity generation + persistence already in place.
4. State legality BEFORE retries/resolvers: retry scanner, resolver,
   downgrade, and escalation (P10) are all expressed as legality-
   guarded CASs (§10.3); building them before L1–L9 enforcement means
   their tests prove nothing.
5. Reservation semantics BEFORE completion derivation: §4.1's
   predicate is only correct because every ACTIVE request holds a
   reservation (§3); deriving completion before P7's money
   choreography is in place derives wrong answers.
6. Provider contract tests BEFORE relying on re-POST behavior: the
   §9.2 auto-downgrade and §7.0 fresh assembly stand entirely on the
   §1 assumed collision contract; P8's sandbox PASS (§18-1) is the
   proof. P10 may be BUILT in parallel but not TRUSTED/enabled toward
   production until P8 passes.
7. MAYBE-row terminal exit BEFORE go-live: without P11 (or the §18-3
   alternative), an unresolvable MAYBE row holds its reservation
   forever — scope never completes, I6 blocks successors (§18-3).
8. Observability / runbooks BEFORE rollout: §15 alerts + §16.6-7
   runbook stubs are the operating safety net; P14's enablement order
   requires the freeze-effective page, drift page, and MAYBE-age
   alerts to already be live.
```

