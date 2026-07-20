package proof;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import proof.minimal.MinimalSingleWriterService;
import proof.provider.FakeProvider;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MODEL 2 — the STRONGEST reasonable version of the proposal, under
 * the SAME scenarios that break model 1.
 *
 * This suite passing is the honest half of the demo: an append-only,
 * no-obligation-row, no-FOR-UPDATE design SURVIVES — once it carries
 * the version fence, fenced write-once identity, idempotent folding,
 * write-ahead, ask-before-retry, and the strictly-newer guard.
 * Which is the point: those are exactly the mechanisms the original
 * proposal said were unnecessary.
 */
@SpringBootTest
class MinimalSingleWriterSurvivesTest {

    @Autowired MinimalSingleWriterService minimal;
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
    @DisplayName("SCENARIO 1 — the zombie's stale plan LOSES the version fence and refolds into a no-op; the fold is idempotent; the amendment pays")
    void zombieRedeliveryIsHarmless() throws Exception {
        String S = "M1";
        hooks.arm("minimal.afterFold." + S);   // the SAME stale window as the naive model
        TestThreads.Handle zombie = TestThreads.run(() -> minimal.onUpstreamAmount(S, 100, 1));
        hooks.awaitArrival("minimal.afterFold." + S);

        minimal.onUpstreamAmount(S, 100, 1);   // redelivery -> pays 100
        minimal.onUpstreamAmount(S, 150, 2);   // amendment  -> pays the 50 delta

        hooks.release("minimal.afterFold." + S);
        zombie.finish();   // its fenced insert conflicted -> refold -> nothing owed

        assertEquals(150, provider.moneyMoved());
        assertEquals(150, minimal.fold(S).paid(), "belief == truth");
        assertEquals(2, minimal.requestEventCount(S), "exactly two requests — no duplicate identity");

        minimal.onUpstreamAmount(S, 250, 3);
        assertEquals(250, provider.moneyMoved(), "nothing silently unpaid");
        assertEquals(250, minimal.fold(S).paid());
    }

    @Test
    @DisplayName("SCENARIO 2 — the unrouted ops writer wins a version slot like any writer; the worker's stale plan loses the fence and sees the reject")
    void unroutedOpsWriterIsSerializedByTheFence() throws Exception {
        String S = "M2";
        hooks.arm("minimal.afterFold." + S);
        TestThreads.Handle worker = TestThreads.run(() -> minimal.onUpstreamAmount(S, 100, 1));
        hooks.awaitArrival("minimal.afterFold." + S);

        minimal.opsReject(S);   // HTTP, not on any ring — the FENCE is the ring

        hooks.release("minimal.afterFold." + S);
        worker.finish();

        assertEquals(0, provider.moneyMoved(), "nothing moved after the human said stop");
        assertTrue(minimal.fold(S).opsRejected());
    }

    @Test
    @DisplayName("SCENARIO 3 — identity comes from the fenced version slot, so seq reuse is impossible; the schema refuses a reused key from ANY writer")
    void instanceOverlapCannotReuseIdentity() throws Exception {
        String S = "M3";
        hooks.arm("minimal.afterFold." + S);
        TestThreads.Handle oldInstance = TestThreads.run(() -> minimal.onUpstreamAmount(S, 100, 1));
        hooks.awaitArrival("minimal.afterFold." + S);

        minimal.onUpstreamAmount(S, 150, 2);   // new instance pays 150

        hooks.release("minimal.afterFold." + S);
        oldInstance.finish();                  // stale plan loses the fence -> no-op

        assertEquals(1, provider.executions());
        assertEquals(150, provider.moneyMoved());
        assertEquals(150, minimal.fold(S).paid());
        assertEquals(1, minimal.requestEventCount(S), "one request, one key, one payload");

        // Schema backstops hold against ANY writer:
        // a rogue append into an already-claimed version slot -> fence refuses
        assertThrows(DuplicateKeyException.class, () -> jdbc.update(
                "INSERT INTO MINIMAL_EVENT (SCOPE_KEY, VERSION, EVENT_TYPE, AMOUNT, IDEM_KEY) "
                        + "VALUES (?, 1, 'AMOUNT_REQUIRED', 999, NULL)", S),
                "UNIQUE(scope, version): one writer per slot, routed or not");

        // a rogue REQUEST_CREATED reusing an existing identity -> refused
        String usedKey = S + "#3";
        assertThrows(DuplicateKeyException.class, () -> jdbc.update(
                "INSERT INTO MINIMAL_EVENT (SCOPE_KEY, VERSION, EVENT_TYPE, AMOUNT, IDEM_KEY) "
                        + "VALUES (?, 99, 'REQUEST_CREATED', 1, ?)", S, usedKey),
                "an identity is allocated exactly once — enforced by the generated-column UNIQUE");
    }

    @Test
    @DisplayName("SCENARIO 4 — write-ahead + ask-before-retry: the crash window is resolved by QUERYING; no double pay, no identity reuse, even past the retention edge")
    void crashInAmbiguityWindowIsResolvedByAsking() throws Exception {
        String S = "M4";
        hooks.armCrash("minimal.afterPost." + S);
        TestThreads.Handle worker = TestThreads.run(() -> minimal.onUpstreamAmount(S, 100, 1));
        worker.finish();
        assertEquals(100, provider.moneyMoved(), "the wire call DID happen");

        provider.expireDedupEntry(S + "#2");   // same retention edge as the naive test

        minimal.recover(S);                    // ASK first -> EXECUTED(100) -> record

        assertEquals(1, provider.executions(), "the engine executed exactly once");
        assertEquals(100, provider.moneyMoved());
        assertEquals(100, minimal.fold(S).paid(), "belief == truth");

        // and the next request takes a FRESH version slot — identity never reused
        minimal.onUpstreamAmount(S, 150, 2);
        assertEquals(150, provider.moneyMoved());
        assertEquals(2, minimal.requestEventCount(S));
    }

    @Test
    @DisplayName("SCENARIO 5 — a delayed OLD snapshot cannot regress the required amount (§6.7 strictly-newer)")
    void staleSnapshotCannotRegress() {
        String S = "M5";
        minimal.onUpstreamAmount(S, 150, 2);   // newer snapshot first
        minimal.onUpstreamAmount(S, 100, 1);   // delayed old snapshot arrives late

        assertEquals(150, minimal.fold(S).required(), "the old snapshot was ignored");
        assertEquals(150, provider.moneyMoved(), "and no over/under payment resulted");
    }
}
