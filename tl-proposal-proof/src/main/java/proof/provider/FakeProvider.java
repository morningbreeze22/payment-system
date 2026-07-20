package proof.provider;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The payment engine, with TWO explicit modes (external review of the
 * first demo cut, finding 3):
 *
 * CONTRACT_COMPLIANT — the world the requirement ASSUMES and the CT
 * gates verify:
 *   - same key + same payload within retention -> dedup, nothing
 *     re-executes (§18-1(a));
 *   - same key + DIFFERENT payload -> DISTINGUISHABLE reject, no
 *     execution (§18-1(b));
 *   - dedup entries age out (§18-1(c)) — a re-POST past the retention
 *     edge EXECUTES A DUPLICATE even here: retention is finite in BOTH
 *     modes, that is the CT-04 fact itself;
 *   - the status query honors a LOOKBACK WINDOW: inside it, EXECUTED /
 *     NOT_FOUND are authoritative; beyond it the answer is
 *     LOOKBACK_EXPIRED and nothing may be concluded (§9.1).
 *
 * ADVERSARIAL — the world CT-02..05 exist to RULE OUT (failed contract
 * evidence): a divergent payload is silently collapsed onto the
 * original execution and still answers ACCEPTED.
 *
 * POST responses carry status + reference, never an amount echo.
 * Tests compare each model's BELIEVED state against this class's
 * INTERNAL TRUTH (executions / money actually moved).
 */
@Component
public class FakeProvider {

    public enum Mode { CONTRACT_COMPLIANT, ADVERSARIAL }

    public record PostResult(String status, String providerRef) {}
    public record QueryResult(String status, long executedAmount) {}

    private record Execution(String idemKey, long amount) {}

    private Mode mode = Mode.CONTRACT_COMPLIANT;
    private final Map<String, Long> dedupStore = new ConcurrentHashMap<>(); // key -> original payload amount
    private final List<Execution> executionLedger = new ArrayList<>();
    private final Set<String> lookbackExpired = ConcurrentHashMap.newKeySet();

    public synchronized void setMode(Mode m) { this.mode = m; }

    public synchronized PostResult post(String idemKey, long amount) {
        Long original = dedupStore.get(idemKey);
        if (original != null) {
            if (original != amount && mode == Mode.CONTRACT_COMPLIANT) {
                // §18-1(b): divergent payload -> distinguishable reject, no execution
                return new PostResult("DIVERGENT_REJECTED", "ref-" + idemKey);
            }
            // same payload (both modes), or ADVERSARIAL collapse: looks like success
            return new PostResult("ACCEPTED", "ref-" + idemKey);
        }
        dedupStore.put(idemKey, amount);
        executionLedger.add(new Execution(idemKey, amount));
        return new PostResult("ACCEPTED", "ref-" + idemKey);
    }

    /** §9.1 status query — authoritative only inside the lookback window. */
    public synchronized QueryResult query(String idemKey) {
        if (lookbackExpired.contains(idemKey)) {
            return new QueryResult("LOOKBACK_EXPIRED", 0);
        }
        return executionLedger.stream()
                .filter(e -> e.idemKey().equals(idemKey))
                .findFirst()
                .map(e -> new QueryResult("EXECUTED", e.amount()))
                .orElse(new QueryResult("NOT_FOUND", 0));
    }

    /** Test control: age a key out of the DEDUP store (§18-1(c) retention edge). */
    public synchronized void expireDedupEntry(String idemKey) { dedupStore.remove(idemKey); }

    /** Test control: age a key past the QUERY lookback window (§9.1). */
    public synchronized void expireQueryLookback(String idemKey) { lookbackExpired.add(idemKey); }

    /** TRUTH: number of distinct wire executions. */
    public synchronized int executions() { return executionLedger.size(); }

    /** TRUTH: money the engine actually moved. */
    public synchronized long moneyMoved() {
        return executionLedger.stream().mapToLong(Execution::amount).sum();
    }

    public synchronized void reset() {
        mode = Mode.CONTRACT_COMPLIANT;
        dedupStore.clear();
        executionLedger.clear();
        lookbackExpired.clear();
    }
}
