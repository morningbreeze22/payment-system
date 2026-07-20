package proof;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import proof.guarded.GuardedPaymentService;
import proof.provider.FakeProvider;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MODEL 3 — the reviewed design (obligation row + SELECT FOR UPDATE +
 * write-ahead identity + CAS + I6 + ask-before-retry + release guard)
 * under the same scenarios.
 *
 * Every test asserts the damage does NOT happen and that BELIEVED
 * state == the provider's INTERNAL TRUTH.
 */
@SpringBootTest
class GuardedModelSurvivesTest {

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
    @DisplayName("SCENARIO 1 under guards — the zombie's late attempt re-reads under the lock and becomes a no-op; history stays clean; the amendment pays")
    void zombieRedeliveryIsHarmless() throws Exception {
        String S = "G1";
        // The zombie parks BEFORE its transaction — the only place it CAN
        // park, because decide-and-claim are ONE transaction here: a stale
        // plan cannot exist outside the lock.
        hooks.arm("guarded.beforeTx." + S);
        TestThreads.Handle zombie = TestThreads.run(() -> guarded.onUpstreamAmount(S, 100, 1));
        hooks.awaitArrival("guarded.beforeTx." + S);

        guarded.onUpstreamAmount(S, 100, 1);   // redelivery -> pays 100
        guarded.onUpstreamAmount(S, 150, 2);   // amendment -> pays the 50 delta

        hooks.release("guarded.beforeTx." + S);
        zombie.finish();

        assertEquals(150, provider.moneyMoved());
        assertEquals(150, guarded.confirmedAmount(S), "belief == truth");
        assertEquals(2, guarded.requestCount(S), "exactly seq 1 and seq 2 — no duplicate rows");

        guarded.onUpstreamAmount(S, 250, 3);
        assertEquals(250, provider.moneyMoved(), "nothing silently unpaid");
        assertEquals(250, guarded.confirmedAmount(S));
    }

    @Test
    @DisplayName("SCENARIO 2 under guards — the unrouted ops writer serializes on the SAME row lock; and the release guard refuses to reject an uncertain outcome")
    void unroutedOpsWriterIsSerializedByTheRowLock() throws Exception {
        String S = "G2";
        hooks.arm("guarded.beforeTx." + S);
        TestThreads.Handle worker = TestThreads.run(() -> guarded.onUpstreamAmount(S, 100, 1));
        hooks.awaitArrival("guarded.beforeTx." + S);

        guarded.opsReject(S);   // same lock — the DB is the ring

        hooks.release("guarded.beforeTx." + S);
        worker.finish();

        assertEquals(0, provider.moneyMoved(), "nothing moved after the human said stop");

        // Release-guard half: an UNCERTAIN in-flight request cannot be
        // human-rejected into a false negative — resolve first (§9).
        String S2 = "G2b";
        hooks.armCrash("guarded.afterPost." + S2);
        TestThreads.Handle crashed = TestThreads.run(() -> guarded.onUpstreamAmount(S2, 100, 1));
        crashed.finish();   // wire happened, process died before confirming

        assertThrows(IllegalStateException.class, () -> guarded.opsReject(S2));

        guarded.recover(S2);    // ask the provider -> EXECUTED(100) -> record truth
        assertEquals(100, guarded.confirmedAmount(S2), "belief == truth after resolution");
        guarded.opsReject(S2);  // NOW the human decision lands safely
    }

    @Test
    @DisplayName("SCENARIO 3 under guards — the locked counter makes seq reuse impossible; the schema refuses rogue rows from ANY writer, and the I6 emulation cannot be bypassed")
    void instanceOverlapCannotReuseIdentity() throws Exception {
        String S = "G3";
        hooks.arm("guarded.beforeTx." + S);
        TestThreads.Handle oldInstance = TestThreads.run(() -> guarded.onUpstreamAmount(S, 100, 1));
        hooks.awaitArrival("guarded.beforeTx." + S);

        guarded.onUpstreamAmount(S, 150, 2);   // new instance pays 150 as seq 1

        hooks.release("guarded.beforeTx." + S);
        oldInstance.finish();                  // locks, re-reads, no-op

        assertEquals(150, provider.moneyMoved());
        assertEquals(150, guarded.confirmedAmount(S));
        assertEquals(1, guarded.requestCount(S), "one request, one key, one payload");

        // THE BACKSTOP ARGUMENT — probed while a genuinely ACTIVE row exists
        // (a crash leaves an IN_FLIGHT request holding its generated ACTIVE_KEY):
        String S3b = "G3b";
        hooks.armCrash("guarded.afterPost." + S3b);
        TestThreads.Handle crashed = TestThreads.run(() -> guarded.onUpstreamAmount(S3b, 100, 1));
        crashed.finish();
        Number obId = jdbc.queryForObject("SELECT ID FROM OBLIGATION WHERE SCOPE_KEY=?", Number.class, S3b);

        // a SECOND active request for the same obligation -> I6 refuses
        assertThrows(DuplicateKeyException.class, () -> jdbc.update(
                "INSERT INTO REQUEST (OBLIGATION_ID, SEQ, AMOUNT, IDEM_KEY, PAYLOAD_HASH, STATE) "
                        + "VALUES (?, 99, 1, 'rogue-key-1', 'H1', 'IN_FLIGHT')",
                obId), "I6: at most one active request per obligation — enforced against ANY writer");

        // the review-flagged bypass (supplying ACTIVE_KEY = NULL by hand) is
        // now IMPOSSIBLE: ACTIVE_KEY is a generated column
        assertThrows(DataAccessException.class, () -> jdbc.update(
                "INSERT INTO REQUEST (OBLIGATION_ID, SEQ, AMOUNT, IDEM_KEY, PAYLOAD_HASH, STATE, ACTIVE_KEY) "
                        + "VALUES (?, 97, 1, 'rogue-key-2', 'H1', 'IN_FLIGHT', NULL)",
                obId), "ACTIVE_KEY cannot be supplied by any writer — the DB derives it");

        // a reused idempotency key -> UNIQUE(IDEM_KEY) refuses
        assertThrows(DuplicateKeyException.class, () -> jdbc.update(
                "INSERT INTO REQUEST (OBLIGATION_ID, SEQ, AMOUNT, IDEM_KEY, PAYLOAD_HASH, STATE) "
                        + "VALUES (?, 98, 1, ?, 'H1', 'EXECUTED')",
                obId, S3b + "#1"), "one identity, one row — a divergent payload cannot even exist");

        // an invented state -> CHECK refuses
        assertThrows(DataAccessException.class, () -> jdbc.update(
                "INSERT INTO REQUEST (OBLIGATION_ID, SEQ, AMOUNT, IDEM_KEY, PAYLOAD_HASH, STATE) "
                        + "VALUES (?, 96, 1, 'rogue-key-3', 'H1', 'HACKED')",
                obId), "STATE is CHECK-bound");

        guarded.recover(S3b);   // and the uncertain row still resolves to truth
        assertEquals(100, guarded.confirmedAmount(S3b));
    }

    @Test
    @DisplayName("SCENARIO 4 under guards — write-ahead + ask-before-retry: resolved by QUERYING, not guessing; no double pay, no seq reuse, even past the retention edge")
    void crashInAmbiguityWindowIsResolvedByAsking() throws Exception {
        String S = "G4";
        hooks.armCrash("guarded.afterPost." + S);
        TestThreads.Handle worker = TestThreads.run(() -> guarded.onUpstreamAmount(S, 100, 1));
        worker.finish();
        assertEquals(100, provider.moneyMoved(), "the wire call DID happen");

        provider.expireDedupEntry(S + "#1");   // same retention edge — does not matter here

        guarded.recover(S);   // §9: ask first -> EXECUTED(100) -> record truth

        assertEquals(1, provider.executions(), "the engine executed exactly once");
        assertEquals(100, provider.moneyMoved());
        assertEquals(100, guarded.confirmedAmount(S), "belief == truth");

        guarded.onUpstreamAmount(S, 150, 2);
        assertEquals(150, provider.moneyMoved());
        assertEquals(2, guarded.requestCount(S));
        Integer seq2 = jdbc.queryForObject(
                "SELECT MAX(SEQ) FROM REQUEST R JOIN OBLIGATION O ON O.ID=R.OBLIGATION_ID WHERE O.SCOPE_KEY=?",
                Integer.class, S);
        assertEquals(2, seq2, "fresh seq — identity never re-derived from history (§5.2's lesson)");
    }

    @Test
    @DisplayName("SCENARIO 5 under guards — a delayed OLD snapshot cannot regress the required amount (§6.7 strictly-newer)")
    void staleSnapshotCannotRegress() {
        String S = "G5";
        guarded.onUpstreamAmount(S, 150, 2);   // newer snapshot first
        guarded.onUpstreamAmount(S, 100, 1);   // delayed old snapshot arrives late

        assertEquals(150, guarded.requiredAmount(S), "the old snapshot was ignored");
        assertEquals(150, provider.moneyMoved(), "and no over/under payment resulted");
        assertEquals(150, guarded.confirmedAmount(S));
    }
}
