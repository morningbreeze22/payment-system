package proof;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Shared async-role helper (review finding 7: unexpected thread failures
 * must never be silently swallowed, and joins must be verified).
 */
public final class TestThreads {

    private TestThreads() {}

    public static final class Handle {
        private final Thread thread;
        private final AtomicReference<Throwable> unexpected;

        private Handle(Thread thread, AtomicReference<Throwable> unexpected) {
            this.thread = thread;
            this.unexpected = unexpected;
        }

        /** Join and ASSERT: the thread terminated and died of nothing unexpected. */
        public void finish() throws InterruptedException {
            thread.join(15_000);
            assertFalse(thread.isAlive(), "role thread did not terminate");
            assertNull(unexpected.get(), "role thread failed unexpectedly: " + unexpected.get());
        }
    }

    /** Run a role on its own thread; a SimulatedCrash is an EXPECTED death,
     *  anything else is captured and re-asserted in finish(). */
    public static Handle run(Runnable r) {
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
        return new Handle(t, unexpected);
    }
}
