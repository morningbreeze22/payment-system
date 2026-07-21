package proof;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import proof.guarded.GuardedPaymentService;
import proof.minimal.MinimalSingleWriterService;
import proof.provider.FakeProvider;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * THE OTHER HALF OF THE DEMO — the five issues from
 * event-model-design/04 ("Five Issues That Remain After Assuming
 * Correct Service Code"), each reproduced against Model 2 (the
 * event-table design's runnable stand-in: fence, write-once identity,
 * write-ahead, idempotent fold, ask-before-retry).
 *
 * MinimalSingleWriterSurvivesTest proves the model SURVIVES every
 * concurrency scenario — that suite is what the model does well.
 * THIS suite proves what remains when the code, not the schema, is the
 * last line of defense: every test here asserts the DAMAGE HAPPENS.
 * Green means the limits are real, not that the model is safe.
 *
 * Honesty notes:
 *  - Where a test writes MINIMAL_EVENT rows directly, it is replaying
 *    exactly what protocol-following service code writes (next fenced
 *    slot, fresh identity, write-ahead order) — the "front door", never
 *    a privileged bypass. The defect in each scenario is a DECISION
 *    defect, the one thing the append machinery cannot see.
 *  - Issue 4's DELETE is not an actor at all: it simulates a
 *    point-in-time restore (an infrastructure event).
 *  - Issue 1 also runs the SAME wrong decision against Model 3, where
 *    the I6 emulation refuses it at write time — the blast-radius
 *    comparison from 04, executable.
 */
@SpringBootTest
class EventModelLimitsTest {

    @Autowired MinimalSingleWriterService minimal;
    @Autowired GuardedPaymentService guarded;
    @Autowired FakeProvider provider;
    @Autowired TestHooks hooks;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void reset() {
        provider.reset();
        hooks.reset();
        provider.setMode(FakeProvider.Mode.CONTRACT_COMPLIANT);
    }

    @Test
    @DisplayName("ISSUE 1 (L1) — a CURRENT writer with a legality defect commits an illegal second request (fence ok, identity ok, shape ok) and the money doubles; the guarded schema refuses the SAME decision at write time")
    void issue1_noDeclarativeTemporalBackstop() throws Exception {
        String S = "EML1";
        // The ambiguity fixture: write-ahead committed, wire EXECUTED,
        // process died before the outcome — locally the result is UNKNOWN.
        hooks.armCrash("minimal.afterPost." + S);
        TestThreads.Handle crashed = TestThreads.run(() -> minimal.onUpstreamAmount(S, 100, 1));
        crashed.finish();
        assertEquals(100, provider.moneyMoved(), "the wire call DID happen");
        assertNotNull(minimal.fold(S).inflightKey(), "and locally the outcome is UNKNOWN (the AMBIGUOUS analog)");

        // THE BUGGY NUDGE ENDPOINT (04 issue 1, corrected example). It follows
        // the append protocol EXACTLY: re-folds the LIVE stream — it is
        // CURRENT, it SEES the in-flight request (a stale writer would lose
        // the fence and be forced right back here) — computes the next slot,
        // mints a fresh identity. Its one defect is the LEGALITY DECISION:
        // "request without an outcome = dead -> open a new one".
        MinimalSingleWriterService.Folded f = minimal.fold(S);
        int v = f.maxVersion() + 1;
        String freshKey = S + "#" + v;
        int rows = jdbc.update(
                "INSERT INTO MINIMAL_EVENT (SCOPE_KEY, VERSION, EVENT_TYPE, AMOUNT, IDEM_KEY) "
                        + "VALUES (?,?,?,?,?)",
                S, v, "REQUEST_CREATED", 100L, freshKey);

        assertEquals(1, rows,
                "every mechanism the model owns said yes — the semantically ILLEGAL event is committed history");
        provider.post(freshKey, 100);   // the endpoint completes its 'nudge'

        assertEquals(2, provider.executions(), "two executions for one 100 requirement");
        assertEquals(200, provider.moneyMoved(), "DOUBLE PAY — the cost of enforcement-by-code (04 issue 1)");

        // THE SAME WRONG DECISION against the guarded schema: a second active
        // request row dies on the I6 emulation — zero rows, loud, BEFORE any
        // wire call can depend on it. Same defect, different blast radius.
        String G = "EML1G";
        hooks.armCrash("guarded.afterPost." + G);
        TestThreads.Handle gCrashed = TestThreads.run(() -> guarded.onUpstreamAmount(G, 100, 1));
        gCrashed.finish();
        long movedBefore = provider.moneyMoved();
        Number obId = jdbc.queryForObject("SELECT ID FROM OBLIGATION WHERE SCOPE_KEY=?", Number.class, G);

        assertThrows(DuplicateKeyException.class, () -> jdbc.update(
                "INSERT INTO REQUEST (OBLIGATION_ID, SEQ, AMOUNT, IDEM_KEY, PAYLOAD_HASH, STATE) "
                        + "VALUES (?, 2, 100, ?, 'H1', 'IN_FLIGHT')",
                obId, G + "#2"),
                "baseline: the ORA-00001 moment — the constraint checks the OUTCOME of the decision, not the process");
        assertEquals(movedBefore, provider.moneyMoved(), "and no money moved from the refused decision");
    }

    @Test
    @DisplayName("ISSUE 2 (L2) — one canonical fold = one witness: a classification bug reads PAID on every internal checker while the provider moved NOTHING; only an independent implementation dissents")
    void issue2_noLocalIndependentWitness() {
        String S = "EML2";
        // History exactly as service code writes it: write-ahead committed,
        // process died BEFORE the wire call; recovery asked the provider
        // (NOT_FOUND) and recorded provably-unsent. Frozen right before the
        // successor would settle.
        append(S, 1, "AMOUNT_REQUIRED", 100L, null, 1L);
        append(S, 2, "REQUEST_CREATED", 100L, S + "#2", null);
        append(S, 3, "REQUEST_ABANDONED", 100L, S + "#2", null);
        assertEquals(0, provider.moneyMoved(), "TRUTH: the provider never executed anything");

        // THE canonical fold with 04 issue 2's bug (the endsWith(\"EXECUTED\")
        // analog): a verified-NOT-executed marker is booked as paid. Every
        // internal reader runs THE fold — that is the model's own safety rule.
        long uiPaid          = buggyCanonicalFold(S);
        long scannerPaid     = buggyCanonicalFold(S);   // drift scanner: same fold
        long dailyTotalsPaid = buggyCanonicalFold(S);   // totals job:    same fold

        assertEquals(100, uiPaid, "UI: paid");
        assertEquals(uiPaid, scannerPaid, "drift check compares the bug to ITSELF: green");
        assertEquals(uiPaid, dailyTotalsPaid, "totals job agrees: green");
        // No internal checker CAN dissent — agreement is what the
        // one-canonical-fold rule is FOR. The scanner sees no shortfall, no
        // successor ever opens: the beneficiary is SILENTLY UNPAID.

        // The only dissent comes from OUTSIDE the canonical fold:
        assertEquals(0, minimal.fold(S).paid(),
                "a deliberately INDEPENDENT second implementation disagrees — the N-version cost 04 names");
        assertNotEquals(uiPaid, provider.moneyMoved(),
                "and the provider's ledger (external reconciliation) is the authoritative oracle");
    }

    @Test
    @DisplayName("ISSUE 3 — deploying a fixed fold silently REWRITES what settled history means: same rows byte-for-byte, different money, and the table records NOTHING about the change")
    void issue3_foldFixesAreRetroactive() {
        String S = "EML3";
        // Full history: first request provably-unsent and abandoned;
        // successor paid. Truth: exactly 100 moved.
        append(S, 1, "AMOUNT_REQUIRED", 100L, null, 1L);
        append(S, 2, "REQUEST_CREATED", 100L, S + "#2", null);
        append(S, 3, "REQUEST_ABANDONED", 100L, S + "#2", null);
        append(S, 4, "REQUEST_CREATED", 100L, S + "#4", null);
        provider.post(S + "#4", 100);
        append(S, 5, "OUTCOME", 100L, S + "#4", null);
        assertEquals(100, provider.moneyMoved(), "TRUTH: exactly 100 moved");

        // 'fold v6' (buggy) is in production. The month closes on its answer.
        long booksClosedAt = buggyCanonicalFold(S);
        assertEquals(200, booksClosedAt, "books closed at 200, reports sent, customer notified");

        String rowsBefore = historySnapshot(S);

        // 'fold v7' deploys — the one-line classification fix. Nothing else.
        long afterDeploy = minimal.fold(S).paid();

        assertEquals(rowsBefore, historySnapshot(S), "not one byte of committed history changed");
        assertEquals(100, afterDeploy, "yet paid 'always was' 100 now — v7 claims the books were ALWAYS wrong");
        assertNotEquals(booksClosedAt, afterDeploy,
                "the settled answer changed RETROACTIVELY, and no row records that it did, or why");
        // The governed alternative (04 issue 3): replay-diff before deploy +
        // an explicit correction EVENT per changed answer. That machinery is
        // required design work — this test shows the DEFAULT without it.
    }

    @Test
    @DisplayName("ISSUE 4 (L6) — a point-in-time restore erases the table's memory of a burned identity: the replay pays again, belief stays green, 250 moved for a 150 requirement")
    void issue4_restoreErasesIdentityMemory() throws Exception {
        String S = "EML4";
        // Write-ahead committed, wire EXECUTED 100, crash before the outcome.
        hooks.armCrash("minimal.afterPost." + S);
        TestThreads.Handle worker = TestThreads.run(() -> minimal.onUpstreamAmount(S, 100, 1));
        worker.finish();
        assertEquals(100, provider.moneyMoved(), "TRUTH: 100 executed before the disaster");

        // THE INFRASTRUCTURE EVENT — no actor, no bug: storage fails and the
        // database is restored to the backup taken after v1. Rows v2+ are
        // gone, including the write-ahead row that is the design's ONLY
        // memory that S#2 was ever sent. The provider does not roll back.
        jdbc.update("DELETE FROM MINIMAL_EVENT WHERE SCOPE_KEY=? AND VERSION >= 2", S);
        assertNull(minimal.fold(S).inflightKey(), "the design now believes nothing was ever sent");

        // Kafka replays; this time the amendment interleaves first.
        minimal.onUpstreamAmount(S, 150, 2);

        assertEquals(150, minimal.fold(S).paid(), "belief: required 150, paid 150 — every internal check GREEN");
        assertEquals(2, provider.executions(), "truth: the pre-restore execution never went away");
        assertEquals(250, provider.moneyMoved(), "250 moved for a 150 requirement — 100 is ORPHANED money");
        assertEquals(0, countEventsMentioning(S, S + "#2"),
                "no surviving row mentions S#2 — no code, however good, can consult rows that no longer exist");
        // Note what an identity EPOCH would NOT have fixed: the replay used a
        // fresh key anyway and still double-paid. An epoch prevents identity
        // COLLISION; only hold-posting + burned-key enumeration + provider
        // reconciliation fixes identity AMNESIA (04 issue 4's procedure).
    }

    @Test
    @DisplayName("ISSUE 5 (L9) — history is LOAD-BEARING: erasing one event (compliance redaction) changes the money answer, and the next ROUTINE scan double-pays")
    void issue5_loadBearingHistoryCannotForget() {
        String S = "EML5";
        minimal.onUpstreamAmount(S, 100, 1);      // a clean, fully settled payment
        assertEquals(100, provider.moneyMoved());
        assertEquals(100, minimal.fold(S).paid(), "belief == truth");

        // Compliance erasure lands on the opening event (in the real schema,
        // the row whose free text carried beneficiary data). In a mutable
        // design a redaction is an UPDATE of a display field; here the row
        // IS fold input — there is no non-load-bearing place to erase.
        jdbc.update("DELETE FROM MINIMAL_EVENT WHERE SCOPE_KEY=? AND EVENT_TYPE='REQUEST_CREATED'", S);

        assertEquals(0, minimal.fold(S).paid(),
                "deleting the row CHANGED THE MONEY STATE — the outcome now references a request that 'never happened'");

        // The next ROUTINE scanner pass acts on that belief:
        minimal.settle(S);

        assertEquals(200, provider.moneyMoved(),
                "DOUBLE PAY caused purely by an erasure — in this model, forgetting is a money operation");
        // Which is why 04 issue 5 requires PII to live OUTSIDE events (opaque
        // vault references / crypto-shredding), decided BEFORE the first
        // production event: retrofitting means rewriting load-bearing history.
    }

    // ---- helpers ----

    /** A protocol-following append: exactly what any writer using the append
     *  library produces (used to lay down history the service cannot be
     *  frozen into with the existing hooks — never to bypass anything). */
    private void append(String scope, int version, String type, Long amount, String idemKey, Long ordering) {
        jdbc.update("INSERT INTO MINIMAL_EVENT (SCOPE_KEY, VERSION, EVENT_TYPE, AMOUNT, IDEM_KEY, UPSTREAM_ORDERING) "
                        + "VALUES (?,?,?,?,?,?)",
                scope, version, type, amount, idemKey, ordering);
    }

    /** THE canonical fold carrying 04 issue 2's classification bug: any
     *  terminal marker books the request's money as paid — REQUEST_ABANDONED
     *  is a verified-NOT-executed record (the PLATFORM_VERIFIED_NOT_EXECUTED
     *  analog) and the buggy classifier counts it anyway. Everything else is
     *  identical to the correct fold. */
    private long buggyCanonicalFold(String scope) {
        List<Map<String, Object>> events = jdbc.queryForList(
                "SELECT EVENT_TYPE, AMOUNT, IDEM_KEY FROM MINIMAL_EVENT WHERE SCOPE_KEY=? ORDER BY VERSION", scope);
        Map<String, Long> created = new HashMap<>();
        Set<String> terminal = new HashSet<>();
        for (Map<String, Object> e : events) {
            String type = (String) e.get("EVENT_TYPE");
            if ("REQUEST_CREATED".equals(type)) {
                created.put((String) e.get("IDEM_KEY"), ((Number) e.get("AMOUNT")).longValue());
            } else if ("OUTCOME".equals(type) || "REQUEST_ABANDONED".equals(type)) {   // <-- the bug
                terminal.add((String) e.get("IDEM_KEY"));
            }
        }
        return terminal.stream().mapToLong(k -> created.getOrDefault(k, 0L)).sum();
    }

    private String historySnapshot(String scope) {
        return jdbc.queryForList(
                "SELECT VERSION, EVENT_TYPE, AMOUNT, IDEM_KEY FROM MINIMAL_EVENT WHERE SCOPE_KEY=? ORDER BY VERSION",
                scope).toString();
    }

    private int countEventsMentioning(String scope, String idemKey) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM MINIMAL_EVENT WHERE SCOPE_KEY=? AND IDEM_KEY=?",
                Integer.class, scope, idemKey);
        return n == null ? 0 : n;
    }
}
