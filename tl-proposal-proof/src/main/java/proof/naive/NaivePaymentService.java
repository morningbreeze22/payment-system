package proof.naive;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import proof.TestHooks;
import proof.provider.FakeProvider;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * THE TECH-LEAD PROPOSAL, built faithfully and given every benefit:
 *
 *  - ONE insert-only event table; no obligation row; no locks; no
 *    unique business constraints ("reconstruct the whole flow from
 *    this table");
 *  - fold-then-act: every decision derives current state by folding
 *    the event history;
 *  - deterministic identity is ADOPTED from the reviewed design
 *    (idemKey = scope + "#" + seq, seq = MAX(seq)+1) — without it the
 *    model fails instantly, so it gets our §5.1 idea for free;
 *  - thread affinity is assumed: the tests hand each message to one
 *    thread at a time. The scenarios attack exactly the windows
 *    affinity cannot cover: redelivery after rebalance (zombie),
 *    unrouted writers (ops), instance overlap, and crash between the
 *    wire call and the event append.
 */
@Service
public class NaivePaymentService {

    private final JdbcTemplate jdbc;
    private final FakeProvider provider;
    private final TestHooks hooks;

    public NaivePaymentService(JdbcTemplate jdbc, FakeProvider provider, TestHooks hooks) {
        this.jdbc = jdbc;
        this.provider = provider;
        this.hooks = hooks;
    }

    /** Folded view of one payment, reconstructed from history. */
    public record Folded(long required, long paidBelieved, Integer activeSeq,
                         long activeAmount, int maxSeq, boolean opsRejected) {}

    public Folded fold(String scope) {
        List<Map<String, Object>> events = jdbc.queryForList(
                "SELECT EVENT_TYPE, SEQ, AMOUNT, IDEM_KEY FROM NAIVE_EVENT WHERE SCOPE_KEY = ? ORDER BY ID",
                scope);
        long required = 0;
        long paid = 0;
        boolean opsRejected = false;
        int maxSeq = 0;
        Integer activeSeq = null;
        long activeAmount = 0;
        java.util.Set<Integer> outcomeSeqs = new java.util.HashSet<>();
        for (Map<String, Object> e : events) {
            if ("OUTCOME".equals(e.get("EVENT_TYPE"))) {
                paid += ((Number) e.get("AMOUNT")).longValue();
                outcomeSeqs.add(((Number) e.get("SEQ")).intValue());
            }
        }
        for (Map<String, Object> e : events) {
            String type = (String) e.get("EVENT_TYPE");
            switch (type) {
                case "AMOUNT_REQUIRED" -> required = ((Number) e.get("AMOUNT")).longValue();
                case "OPS_REJECTED" -> opsRejected = true;
                case "REQUEST_CREATED" -> {
                    int seq = ((Number) e.get("SEQ")).intValue();
                    maxSeq = Math.max(maxSeq, seq);
                    if (!outcomeSeqs.contains(seq)) {
                        activeSeq = seq;
                        activeAmount = ((Number) e.get("AMOUNT")).longValue();
                    }
                }
                default -> { }
            }
        }
        return new Folded(required, paid, activeSeq, activeAmount, maxSeq, opsRejected);
    }

    /** Upstream Kafka entry point (routed by hash in the proposal). */
    public void onUpstreamAmount(String scope, long amount) {
        append(scope, "AMOUNT_REQUIRED", null, amount, null);
        settle(scope);
    }

    /** fold-then-act — the heart of the proposal. No lock exists between
     *  the fold (read) and the act (insert + wire call). */
    public void settle(String scope) {
        Folded f = fold(scope);
        hooks.pause("naive.afterFold." + scope);   // GC pause / slow thread / zombie window
        if (f.opsRejected()) return;
        if (f.activeSeq() != null) return;
        long delta = f.required() - f.paidBelieved();
        if (delta <= 0) return;
        int seq = f.maxSeq() + 1;                  // identity from OBSERVED HISTORY (MAX+1)
        String idemKey = scope + "#" + seq;
        append(scope, "REQUEST_CREATED", seq, delta, idemKey);
        hooks.pause("naive.beforePost." + scope);
        provider.post(idemKey, delta);
        hooks.crashIfArmed("naive.afterPost." + scope);  // crash BETWEEN wire and append
        // response carries no amount echo; the model records what it asked for
        append(scope, "OUTCOME", seq, delta, idemKey);
    }

    /** Retry worker entry point. An active request without an outcome is
     *  ambiguous: this model recorded nothing BEFORE the wire call, so it
     *  cannot know whether the POST happened. Its only options are
     *  (a) blind re-POST with the same key — relying entirely on the
     *  provider dedup facts, or (b) never retry — payment silently lost.
     *  It picks (a), like every implementation of this shape does. */
    public void retrySweep(String scope) {
        Folded f = fold(scope);
        if (f.opsRejected()) return;
        if (f.activeSeq() != null) {
            String idemKey = scope + "#" + f.activeSeq();
            provider.post(idemKey, f.activeAmount());
            append(scope, "OUTCOME", f.activeSeq(), f.activeAmount(), idemKey);
            return;
        }
        settle(scope);
    }

    /** The UNROUTED writer: a human ops decision arriving via HTTP,
     *  not via the hash ring. */
    public void opsReject(String scope) {
        append(scope, "OPS_REJECTED", null, null, null);
    }

    public int countRequestRows(String scope, int seq) {
        return Objects.requireNonNull(jdbc.queryForObject(
                "SELECT COUNT(*) FROM NAIVE_EVENT WHERE SCOPE_KEY=? AND EVENT_TYPE='REQUEST_CREATED' AND SEQ=?",
                Integer.class, scope, seq));
    }

    private void append(String scope, String type, Integer seq, Long amount, String idemKey) {
        jdbc.update("INSERT INTO NAIVE_EVENT (SCOPE_KEY, EVENT_TYPE, SEQ, AMOUNT, IDEM_KEY) VALUES (?,?,?,?,?)",
                scope, type, seq, amount, idemKey);
    }
}
