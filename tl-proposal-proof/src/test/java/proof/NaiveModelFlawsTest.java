package proof;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import proof.naive.NaivePaymentService;
import proof.provider.FakeProvider;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MODEL 1 — the proposal AS LITERALLY STATED, under fire.
 *
 * What these tests prove (and only this): fold-then-act over an
 * unconstrained insert-only table, with no fencing, no idempotent
 * folding, and blind retry, fails in the documented windows. They do
 * NOT prove that a properly guarded single-writer model fails — that
 * is what MinimalSingleWriterSurvivesTest exists to answer.
 *
 * Every GREEN test here is a PROVEN FLAW: the assertions assert that
 * the damage happened.
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
        // This suite runs against the ADVERSARIAL provider — the world the
        // CT-02..05 sandbox gates exist to rule out. The naive model has no
        // defense in EITHER world; scenario 3 needs this one to show how
        // invisible the damage is when contract evidence is missing.
        provider.setMode(FakeProvider.Mode.ADVERSARIAL);
    }

    @Test
    @DisplayName("FLAW 1 — zombie redelivery corrupts unconstrained insert-only history; a later amendment is then SILENTLY UNPAID (requirment-v4 §11 lease / U-1 disappearance class)")
    void zombieRedeliveryCorruptsHistoryAndSilentlyUnderpays() throws Exception {
        String S = "S1";
        // Thread A = the ORIGINAL partition owner. It folds (sees nothing paid)
        // and then stalls — GC pause, slow wire, stop-the-world. Kafka's
        // session timeout expires; the partition is REASSIGNED. A is now a
        // zombie, but it does not know that.
        hooks.arm("naive.afterFold." + S);
        TestThreads.Handle zombie = TestThreads.run(() -> naive.onUpstreamAmount(S, 100));
        hooks.awaitArrival("naive.afterFold." + S);

        // Thread B = the NEW owner. At-least-once delivery hands it the same
        // message (offset not committed), then the amendment to 150.
        naive.onUpstreamAmount(S, 100);   // redelivery -> pays seq1/K1: 100
        naive.onUpstreamAmount(S, 150);   // amendment  -> pays seq2/K2: 50

        // The zombie wakes and finishes its STALE plan. Nothing stops it:
        // no lock, no fence, no unique constraint. It appends a DUPLICATE
        // seq-1 request row and a DUPLICATE outcome row.
        hooks.release("naive.afterFold." + S);
        zombie.finish();

        assertEquals(2, naive.countRequestRows(S, 1),
                "insert-only + no unique constraint admitted a duplicate request row");

        long truth = provider.moneyMoved();
        long believed = naive.fold(S).paidBelieved();
        assertEquals(150, truth);
        assertEquals(250, believed, "the non-idempotent fold double-counts the duplicated outcome");

        // The kill shot: the NEXT amendment (250) folds "paid 250" and sends
        // NOTHING — 100 is silently missing, no alert, no trace (U-1 class).
        naive.onUpstreamAmount(S, 250);
        assertEquals(150, provider.moneyMoved(),
                "amendment to 250 paid nothing: 100 is silently missing");
    }

    @Test
    @DisplayName("FLAW 2 — an UNROUTED writer (ops reject over HTTP) races fold-then-act: money moves AFTER the human said stop (§9.3 / §10.3 release guard)")
    void unroutedOpsWriterIsOverriddenByStaleFold() throws Exception {
        String S = "S2";
        hooks.arm("naive.afterFold." + S);
        TestThreads.Handle worker = TestThreads.run(() -> naive.onUpstreamAmount(S, 100));
        hooks.awaitArrival("naive.afterFold." + S);

        // A human rejects via the ops endpoint — never on the hash ring.
        naive.opsReject(S);

        hooks.release("naive.afterFold." + S);
        worker.finish();

        assertTrue(naive.fold(S).opsRejected(), "the reject IS in the history");
        assertEquals(100, provider.moneyMoved(),
                "and yet 100 moved AFTER the human said stop — the fold predates the reject");
    }

    @Test
    @DisplayName("FLAW 3 — instance overlap + MAX(seq)+1: SAME idempotency key, DIFFERENT amounts; with failed contract evidence the response is indistinguishable (§5.1 / §7.2 / CT-03)")
    void instanceOverlapProducesDivergentPayloadUnderOneKey() throws Exception {
        String S = "S3";
        hooks.arm("naive.afterFold." + S);
        TestThreads.Handle oldInstance = TestThreads.run(() -> naive.onUpstreamAmount(S, 100));
        hooks.awaitArrival("naive.afterFold." + S);

        // New instance processes the amendment first: its fold ALSO computes
        // MAX(seq)+1 = 1 -> the SAME key S3#1, but amount 150 — and pays it.
        naive.onUpstreamAmount(S, 150);

        // Old instance wakes with its stale plan: S3#1, amount 100.
        // Same key, different payload — and this adversarial engine silently
        // collapses it and answers ACCEPTED.
        hooks.release("naive.afterFold." + S);
        oldInstance.finish();

        assertEquals(2, naive.countRequestRows(S, 1),
                "two request rows share seq 1 with DIFFERENT amounts under ONE idempotency key");
        assertEquals(1, provider.executions(), "the engine executed once (the 150)");
        assertEquals(150, provider.moneyMoved());
        assertEquals(250, naive.fold(S).paidBelieved(),
                "the model believes both 'succeeded' — belief and reality have diverged, undetectably");
    }

    @Test
    @DisplayName("FLAW 4 — crash between wire and append, then the dedup retention edge: BLIND retry double-pays. (This proves blind retry is unsafe — model 2 shows the same table CAN recover safely by ASKING; the flaw is the missing discipline, not the storage shape)")
    void crashInAmbiguityWindowThenRetentionEdgeDoublePays() throws Exception {
        String S = "S4";
        // The worker posts and dies before appending the outcome. The history
        // says "request created, no outcome" — exactly what it would say if
        // the POST had never happened.
        hooks.armCrash("naive.afterPost." + S);
        TestThreads.Handle worker = TestThreads.run(() -> naive.onUpstreamAmount(S, 100));
        worker.finish();
        assertEquals(100, provider.moneyMoved(), "the wire call DID happen");

        // Crash Friday, retry Monday: the dedup entry ages out (§18-1(c) —
        // finite in BOTH provider modes; that is the CT-04 fact itself).
        provider.expireDedupEntry(S + "#1");

        // THIS model's retry re-POSTs blind — it never recorded an intent it
        // could query on, and never asks. Past the retention edge the engine
        // executes again.
        naive.retrySweep(S);

        assertEquals(2, provider.executions(), "the engine executed TWICE");
        assertEquals(200, provider.moneyMoved(), "DOUBLE PAYMENT: 200 moved for a required 100");
        assertEquals(100, naive.fold(S).paidBelieved(),
                "and the model believes 100 — it cannot even see what it did");
    }
}
