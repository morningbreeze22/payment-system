package proof.provider;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The payment engine, reduced to the contract facts the requirement
 * assumes (§1 / §18-1) — including the DANGEROUS ones the sandbox
 * tests CT-02..CT-05 exist to verify:
 *
 *  - same idempotency key while the key is in the DEDUP STORE ->
 *    nothing re-executes, the response is indistinguishable from a
 *    fresh acceptance (§18-1(a); and per the CT-03 hazard class this
 *    engine silently collapses a DIFFERENT payload onto the original
 *    execution — the caller cannot tell from the response);
 *  - dedup entries AGE OUT (§18-1(c)): a re-POST past the retention
 *    edge EXECUTES A DUPLICATE — the reason CT-04 exists;
 *  - the EXECUTION LEDGER (money actually moved) is permanent and
 *    queryable (§9.1 status query) even after the dedup entry aged out;
 *  - POST responses carry status + reference, NOT an amount echo —
 *    like real engines.
 *
 * Tests compare each model's BELIEVED state against this class's
 * INTERNAL TRUTH (executions / money actually moved).
 */
@Component
public class FakeProvider {

    public record PostResult(String status, String providerRef) {}
    public record QueryResult(String status, long executedAmount) {}

    private record Execution(String idemKey, long amount) {}

    private final Map<String, Long> dedupStore = new ConcurrentHashMap<>();
    private final List<Execution> executionLedger = new ArrayList<>();

    public synchronized PostResult post(String idemKey, long amount) {
        if (dedupStore.containsKey(idemKey)) {
            // dedup hit — original execution stands; a divergent payload is
            // silently collapsed; the response looks exactly like success
            return new PostResult("ACCEPTED", "ref-" + idemKey);
        }
        dedupStore.put(idemKey, amount);
        executionLedger.add(new Execution(idemKey, amount));
        return new PostResult("ACCEPTED", "ref-" + idemKey);
    }

    /** §9.1 status query by key — the ask-before-retry primitive; reads the
     *  permanent execution ledger, not the aging dedup store. */
    public synchronized QueryResult query(String idemKey) {
        return executionLedger.stream()
                .filter(e -> e.idemKey().equals(idemKey))
                .findFirst()
                .map(e -> new QueryResult("EXECUTED", e.amount()))
                .orElse(new QueryResult("NOT_FOUND", 0));
    }

    /** Test control: age a key out of the dedup store (§18-1(c) retention edge). */
    public synchronized void expireDedupEntry(String idemKey) {
        dedupStore.remove(idemKey);
    }

    /** TRUTH: number of distinct wire executions. */
    public synchronized int executions() { return executionLedger.size(); }

    /** TRUTH: money the engine actually moved. */
    public synchronized long moneyMoved() {
        return executionLedger.stream().mapToLong(Execution::amount).sum();
    }

    public synchronized void reset() {
        dedupStore.clear();
        executionLedger.clear();
    }
}
