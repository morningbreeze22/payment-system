package proof;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import proof.naive.NaivePaymentService;
import proof.provider.FakeProvider;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * THE TECH-LEAD MODEL UNDER FIRE.
 *
 * Every test here drives the model exactly as proposed — thread
 * affinity in steady state, insert-only event table, fold-then-act,
 * no locks, no DB constraints — through a failure window the
 * requirement is built around. Every GREEN test in this class is a
 * PROVEN FLAW: the assertions assert that the damage happened.
 *
 * The mirror class (GuardedModelSurvivesTest) runs the same scenarios
 * against the reviewed design and asserts the damage does NOT happen.
 */
@SpringBootTest
class NaiveModelFlawsTest {

    @Autowired NaivePaymentService naive;
    @Autowired FakeProvider provider;
    @Autowired TestHooks hooks;

    @BeforeEach
    void reset() {
        provider.reset();
        hooks.reset();
    }

    @Test
    @DisplayName("FLAW 1 — zombie redelivery corrupts insert-only history; a later amendment is then SILENTLY UNPAID (requirment-v4 §11 lease / U-1 disappearance class)")
    void zombieRedeliveryCorruptsHistoryAndSilentlyUnderpays() throws Exception {
        String S = "S1";
        // Thread A = the ORIGINAL partition owner. It folds (sees nothing paid)
        // and then stalls — GC pause, slow wire, long stop-the-world. Kafka's
        // session timeout expires; the partition is REASSIGNED. A is now a
        // zombie, but it does not know that.
        hooks.arm("naive.afterFold." + S);
        Thread zombie = runAsync(() -> naive.onUpstreamAmount(S, 100));
        hooks.awaitArrival("naive.afterFold." + S);

        // Thread B = the NEW owner. At-least-once delivery hands it the same
        // message (offset not committed), then the amendment to 150.
        naive.onUpstreamAmount(S, 100);   // redelivery -> pays seq1/K1: 100
        naive.onUpstreamAmount(S, 150);   // amendment  -> pays seq2/K2: 50

        // The zombie wakes and finishes its STALE plan. Nothing stops it:
        // no lock, no unique constraint, no claim. It appends a DUPLICATE
        // seq-1 request row and a DUPLICATE outcome row (the provider dedups
        // the wire call — deterministic identity, borrowed from §5.1, saves
        // the money HERE — but the history is now corrupt).
        hooks.release("naive.afterFold." + S);
        zombie.join(15_000);

        // Corruption, part 1: "reconstruct the whole flow from this table"
        // now reconstructs TWO seq-1 requests.
        assertEquals(2, naive.countRequestRows(S, 1),
                "insert-only + no unique constraint admitted a duplicate request row");

        // Corruption, part 2: the fold double-counts the duplicated outcome.
        long truth = provider.moneyMoved();          // 150 actually moved
        long believed = naive.fold(S).paidBelieved(); // 250 believed
        assertEquals(150, truth);
        assertEquals(250, believed, "the fold believes 100 more was paid than reality");

        // The kill shot: the NEXT amendment (250) folds "paid 250, required 250"
        // and sends NOTHING. The customer is short 100 and NO signal exists —
        // the silently-unpaid class the requirement calls out as the worst
        // failure direction (U-1).
        naive.onUpstreamAmount(S, 250);
        assertEquals(150, provider.moneyMoved(),
                "amendment to 250 paid nothing: 100 is silently missing, no alert, no trace");
    }

    @Test
    @DisplayName("FLAW 2 — an UNROUTED writer (ops reject over HTTP) races fold-then-act: money moves AFTER the human said stop (§9.3 / §10.3 release guard)")
    void unroutedOpsWriterIsOverriddenByStaleFold() throws Exception {
        String S = "S2";
        // The routed thread folds (decides to pay 100) and stalls before acting.
        hooks.arm("naive.afterFold." + S);
        Thread worker = runAsync(() -> naive.onUpstreamAmount(S, 100));
        hooks.awaitArrival("naive.afterFold." + S);

        // A human rejects the payment via the ops endpoint. This writer is NOT
        // on the hash ring — no HTTP request ever is. The event commits.
        naive.opsReject(S);

        // The routed thread resumes its stale plan and pays anyway.
        hooks.release("naive.afterFold." + S);
        worker.join(15_000);

        assertTrue(naive.fold(S).opsRejected(), "the reject IS in the history");
        assertEquals(100, provider.moneyMoved(),
                "and yet 100 moved AFTER the human said stop — the fold was taken before the reject landed");
    }

    @Test
    @DisplayName("FLAW 3 — instance overlap reuses seq via MAX+1: SAME idempotency key, DIFFERENT amounts, and the engine response is indistinguishable (§5.1 identity / §7.2 collision / I6)")
    void instanceOverlapProducesDivergentPayloadUnderOneKey() throws Exception {
        String S = "S3";
        // Old instance folds for the 100-amount message and stalls
        // (rolling deploy: old instance lingers past rebalance).
        hooks.arm("naive.afterFold." + S);
        Thread oldInstance = runAsync(() -> naive.onUpstreamAmount(S, 100));
        hooks.awaitArrival("naive.afterFold." + S);

        // New instance processes the amendment first: folds MAX(seq)+1 = 1,
        // key S3#1, amount 150 — and pays it.
        naive.onUpstreamAmount(S, 150);

        // Old instance wakes: ITS fold also computed seq 1 -> SAME key S3#1,
        // but amount 100. Same key, different payload — the CT-03 hazard.
        // This engine silently collapses onto the original execution and
        // answers ACCEPTED; the caller cannot tell anything went wrong.
        hooks.release("naive.afterFold." + S);
        oldInstance.join(15_000);

        assertEquals(2, naive.countRequestRows(S, 1),
                "two request rows share seq 1 — and they carry DIFFERENT amounts under ONE idempotency key");
        assertEquals(1, provider.executions(), "the engine executed once (the 150)");
        assertEquals(150, provider.moneyMoved());
        assertEquals(250, naive.fold(S).paidBelieved(),
                "the model believes both 'succeeded' — belief and reality have permanently diverged");
    }

    @Test
    @DisplayName("FLAW 4 — crash between wire and append + dedup retention edge: blind re-POST DOUBLE-PAYS (§5.1 write-ahead / §7.0 ask-before-retry / §18-1(c))")
    void crashInAmbiguityWindowThenRetentionEdgeDoublePays() throws Exception {
        String S = "S4";
        // The worker posts to the wire and dies before appending the outcome
        // event. The insert-only history now says "request created, no outcome"
        // — which is EXACTLY what it would say if the POST had never happened.
        // The model recorded nothing before the wire call that distinguishes
        // the two worlds.
        hooks.armCrash("naive.afterPost." + S);
        Thread worker = runAsync(() -> naive.onUpstreamAmount(S, 100));
        worker.join(15_000);
        assertEquals(100, provider.moneyMoved(), "the wire call DID happen");

        // Time passes (crash Friday, retry Monday). The dedup entry ages out —
        // the retention fact §18-1(c) says we may not assume is infinite,
        // which is why CT-04 exists.
        provider.expireDedupEntry(S + "#1");

        // The retry worker's only move is a blind re-POST with the same key
        // (option b is 'never retry' = payment silently lost). Past the
        // retention edge, the engine executes it AGAIN.
        naive.retrySweep(S);

        assertEquals(2, provider.executions(), "the engine executed TWICE");
        assertEquals(200, provider.moneyMoved(), "DOUBLE PAYMENT: 200 moved for a required 100");
        assertEquals(100, naive.fold(S).paidBelieved(),
                "and the model believes 100 — it cannot even see what it did");
    }

    // ---- helper: run a role on its own thread; simulated crashes are expected deaths ----
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
