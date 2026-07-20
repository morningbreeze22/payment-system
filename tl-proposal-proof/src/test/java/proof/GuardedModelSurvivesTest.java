package proof;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import proof.guarded.GuardedPaymentService;
import proof.provider.FakeProvider;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * THE SAME SCENARIOS against the reviewed design (obligation row +
 * SELECT FOR UPDATE + write-ahead identity + CAS + I6-style unique
 * constraints + ask-before-retry + release guard).
 *
 * Every test asserts the damage from NaiveModelFlawsTest does NOT
 * happen, and that BELIEVED state == the provider's INTERNAL TRUTH.
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
    }

    @Test
    @DisplayName("SCENARIO 1 under guards — the zombie's late attempt re-reads under the lock and becomes a no-op; history stays clean; the amendment pays")
    void zombieRedeliveryIsHarmless() throws Exception {
        String S = "G1";
        // The zombie parks BEFORE its transaction — the only place it CAN
        // park, because in this design decide-and-claim are ONE transaction:
        // a stale plan cannot exist outside the lock.
        hooks.arm("guarded.beforeTx." + S);
        Thread zombie = runAsync(() -> guarded.onUpstreamAmount(S, 100));
        hooks.awaitArrival("guarded.beforeTx." + S);

        guarded.onUpstreamAmount(S, 100);   // redelivery on the new owner -> pays 100
        guarded.onUpstreamAmount(S, 150);   // amendment -> pays the 50 delta

        hooks.release("guarded.beforeTx." + S);
        zombie.join(15_000);

        // The zombie locked, re-read (required 150, confirmed 150), did nothing.
        assertEquals(150, provider.moneyMoved());
        assertEquals(150, guarded.confirmedAmount(S), "belief == truth");
        assertEquals(2, guarded.requestCount(S), "exactly seq 1 and seq 2 — no duplicate rows");

        // And the next amendment pays exactly the missing delta.
        guarded.onUpstreamAmount(S, 250);
        assertEquals(250, provider.moneyMoved(), "nothing silently unpaid");
        assertEquals(250, guarded.confirmedAmount(S));
    }

    @Test
    @DisplayName("SCENARIO 2 under guards — the unrouted ops writer serializes on the SAME lock; the worker sees BLOCKED and refuses; and the release guard refuses to reject an uncertain outcome")
    void unroutedOpsWriterIsSerializedByTheRowLock() throws Exception {
        String S = "G2";
        hooks.arm("guarded.beforeTx." + S);
        Thread worker = runAsync(() -> guarded.onUpstreamAmount(S, 100));
        hooks.awaitArrival("guarded.beforeTx." + S);

        // Ops rejects. Same lock, no special routing needed — the DB is the ring.
        guarded.opsReject(S);

        hooks.release("guarded.beforeTx." + S);
        worker.join(15_000);

        assertEquals(0, provider.moneyMoved(), "nothing moved after the human said stop");

        // Release-guard half: with a genuinely UNCERTAIN in-flight request,
        // ops CANNOT terminal-reject it — the guard forces the §9 resolve-first
        // path instead of letting a human create a lie.
        String S2 = "G2b";
        hooks.armCrash("guarded.afterPost." + S2);
        Thread crashed = runAsync(() -> guarded.onUpstreamAmount(S2, 100));
        crashed.join(15_000);   // wire happened, process died before confirming

        assertThrows(IllegalStateException.class, () -> guarded.opsReject(S2),
                "release guard: an uncertain outcome cannot be human-rejected into a false negative");

        guarded.recover(S2);    // ask the provider -> EXECUTED(100) -> record truth
        assertEquals(100, guarded.confirmedAmount(S2), "belief == truth after resolution");
        guarded.opsReject(S2);  // NOW the human decision lands safely
    }

    @Test
    @DisplayName("SCENARIO 3 under guards — the counter (not MAX+1) and the lock make seq reuse impossible; the DB constraints refuse divergent/duplicate rows from ANY writer")
    void instanceOverlapCannotReuseIdentity() throws Exception {
        String S = "G3";
        hooks.arm("guarded.beforeTx." + S);
        Thread oldInstance = runAsync(() -> guarded.onUpstreamAmount(S, 100));
        hooks.awaitArrival("guarded.beforeTx." + S);

        guarded.onUpstreamAmount(S, 150);   // new instance pays 150 as seq 1

        hooks.release("guarded.beforeTx." + S);
        oldInstance.join(15_000);           // locks, re-reads, no-op

        assertEquals(150, provider.moneyMoved());
        assertEquals(150, guarded.confirmedAmount(S));
        assertEquals(1, guarded.requestCount(S), "one request, one key, one payload");

        // THE BACKSTOP ARGUMENT: even a writer that bypasses every service
        // convention — the hotfix script, the future endpoint nobody routed —
        // is refused BY THE SCHEMA. Probe it while a genuinely ACTIVE row
        // exists (crash leaves an IN_FLIGHT request holding its ACTIVE_KEY):
        String S3b = "G3b";
        hooks.armCrash("guarded.afterPost." + S3b);
        Thread crashed = runAsync(() -> guarded.onUpstreamAmount(S3b, 100));
        crashed.join(15_000);
        Number obId = jdbc.queryForObject("SELECT ID FROM OBLIGATION WHERE SCOPE_KEY=?", Number.class, S3b);

        // a SECOND active request for the same obligation -> I6 refuses
        assertThrows(DuplicateKeyException.class, () -> jdbc.update(
                "INSERT INTO REQUEST (OBLIGATION_ID, SEQ, AMOUNT, IDEM_KEY, PAYLOAD_HASH, STATE, ACTIVE_KEY) "
                        + "VALUES (?, 99, 1, 'rogue-key-1', 'H1', 'IN_FLIGHT', ?)",
                obId, obId), "I6: at most one active request per obligation — enforced against ANY writer");

        // a reused idempotency key -> UNIQUE(IDEM_KEY) refuses
        assertThrows(DuplicateKeyException.class, () -> jdbc.update(
                "INSERT INTO REQUEST (OBLIGATION_ID, SEQ, AMOUNT, IDEM_KEY, PAYLOAD_HASH, STATE, ACTIVE_KEY) "
                        + "VALUES (?, 98, 1, ?, 'H1', 'EXECUTED', NULL)",
                obId, S3b + "#1"), "one identity, one row — a divergent payload cannot even exist");

        guarded.recover(S3b);   // and the uncertain row still resolves to truth
        assertEquals(100, guarded.confirmedAmount(S3b));
    }

    @Test
    @DisplayName("SCENARIO 4 under guards — write-ahead + ask-before-retry: the crash window is resolved by QUERYING, not guessing; no double pay, no seq reuse, even past the retention edge")
    void crashInAmbiguityWindowIsResolvedByAsking() throws Exception {
        String S = "G4";
        // Crash between the wire call and the confirmation — but the identity
        // row was committed BEFORE the wire (§5.1 write-ahead): durable
        // evidence that a POST was in flight.
        hooks.armCrash("guarded.afterPost." + S);
        Thread worker = runAsync(() -> guarded.onUpstreamAmount(S, 100));
        worker.join(15_000);
        assertEquals(100, provider.moneyMoved(), "the wire call DID happen");

        // Same retention edge as the naive test — and it does not matter,
        // because this design never blind-re-POSTs.
        provider.expireDedupEntry(S + "#1");

        guarded.recover(S);   // §9: ask first -> EXECUTED(100) -> record truth

        assertEquals(1, provider.executions(), "the engine executed exactly once");
        assertEquals(100, provider.moneyMoved());
        assertEquals(100, guarded.confirmedAmount(S), "belief == truth");

        // The counter survived the crash too: the next request takes seq 2 —
        // identity is never re-derived from observed history (§5.2's lesson).
        guarded.onUpstreamAmount(S, 150);
        assertEquals(150, provider.moneyMoved());
        assertEquals(2, guarded.requestCount(S));
        Integer seq2 = jdbc.queryForObject(
                "SELECT MAX(SEQ) FROM REQUEST R JOIN OBLIGATION O ON O.ID=R.OBLIGATION_ID WHERE O.SCOPE_KEY=?",
                Integer.class, S);
        assertEquals(2, seq2, "fresh seq — no reuse after the crash");
    }

    private Thread runAsync(Runnable r) {
        AtomicReference<Throwable> unexpected = new AtomicReference<>();
        Thread t = new Thread(() -> {
            try {
                r.run();
            } catch (TestHooks.SimulatedCrash ignored) {
                // the process died here — that is the scenario
            } catch (Throwable other) {
                unexpected.set(other);
            }
        });
        t.setDaemon(true);
        t.start();
        return t;
    }
}
