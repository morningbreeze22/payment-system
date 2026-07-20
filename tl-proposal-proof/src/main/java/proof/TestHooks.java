package proof;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Deterministic interleaving control. A test arms a named pause point;
 * the FIRST service thread that reaches it parks there (one-shot — later
 * threads pass through) until the test releases it. Unarmed points are
 * no-ops. This simulates GC pauses, slow wire calls, rebalance windows,
 * and process crashes WITHOUT sleeps or luck.
 */
@Component
public class TestHooks {

    private record Gate(CountDownLatch arrived, CountDownLatch release) {}

    private final Map<String, Gate> armed = new ConcurrentHashMap<>();    // consumed by the first arrival
    private final Map<String, Gate> registry = new ConcurrentHashMap<>(); // kept for the test side

    /** Test side: arm a one-shot pause point. */
    public void arm(String point) {
        Gate g = new Gate(new CountDownLatch(1), new CountDownLatch(1));
        armed.put(point, g);
        registry.put(point, g);
    }

    /** Test side: wait until a service thread is parked at the point. */
    public void awaitArrival(String point) throws InterruptedException {
        Gate g = registry.get(point);
        if (g == null || !g.arrived().await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("no thread arrived at " + point);
        }
    }

    /** Test side: release the parked thread. */
    public void release(String point) {
        Gate g = registry.get(point);
        if (g != null) g.release().countDown();
    }

    /** Service side: park here if the test armed this point (first arrival only). */
    public void pause(String point) {
        Gate g = armed.remove(point);
        if (g == null) return;
        g.arrived().countDown();
        try {
            if (!g.release().await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("never released from " + point);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    // ---- crash points: simulate the process dying at a precise spot ----

    public static class SimulatedCrash extends RuntimeException {
        public SimulatedCrash(String point) { super("simulated crash at " + point); }
    }

    private final Set<String> crashPoints = ConcurrentHashMap.newKeySet();

    /** Test side: the next thread reaching this point dies. */
    public void armCrash(String point) { crashPoints.add(point); }

    /** Service side: die here if the test armed a crash (one-shot). */
    public void crashIfArmed(String point) {
        if (crashPoints.remove(point)) {
            throw new SimulatedCrash(point);
        }
    }

    public void reset() {
        armed.clear();
        registry.clear();
        crashPoints.clear();
    }
}
