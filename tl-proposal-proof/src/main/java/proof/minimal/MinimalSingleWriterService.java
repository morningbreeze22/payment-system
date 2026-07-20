package proof.minimal;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import proof.TestHooks;
import proof.provider.FakeProvider;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * MODEL 2 — THE STRONGEST REASONABLE VERSION OF THE PROPOSAL
 * (added per external review of the first demo cut: "the naive model
 * assumes weaknesses that were not necessarily part of the proposal").
 *
 * Append-only events, NO obligation row, NO SELECT FOR UPDATE, NO
 * stored committed/confirmed counters — but WITH the guard set a
 * properly designed single-writer model needs:
 *
 *  - PER-SCOPE VERSION FENCE: every append claims (scope, version);
 *    UNIQUE(scope, version) means exactly one writer wins each slot —
 *    a stale plan (zombie, overlapped instance, racing ops action)
 *    loses the insert and must REFOLD. This is optimistic concurrency
 *    on an aggregate: the serialization point the naive model lacked.
 *  - IDENTITY FROM THE FENCED VERSION (idemKey = scope#version), never
 *    MAX(history)+1 — an identity is allocated exactly once, and the
 *    GENERATED-column UNIQUE(idem key) backstops it in the schema.
 *  - WRITE-AHEAD: REQUEST_CREATED commits BEFORE the wire call.
 *  - IDEMPOTENT FOLD: paid = sum over DISTINCT executed idem keys —
 *    a duplicated outcome event cannot double-count.
 *  - ASK-BEFORE-RETRY: an in-flight request is resolved by QUERYING
 *    the provider (§9.1), never by blind re-POST.
 *  - STRICTLY-NEWER upstream ordering guard (§6.7 equivalent).
 */
@Service
public class MinimalSingleWriterService {

    private final JdbcTemplate jdbc;
    private final FakeProvider provider;
    private final TestHooks hooks;

    public MinimalSingleWriterService(JdbcTemplate jdbc, FakeProvider provider, TestHooks hooks) {
        this.jdbc = jdbc;
        this.provider = provider;
        this.hooks = hooks;
    }

    public record Folded(long required, long upstreamOrdering, long paid,
                         String inflightKey, long inflightAmount,
                         int maxVersion, boolean opsRejected, Set<String> paidKeys) {}

    public Folded fold(String scope) {
        List<Map<String, Object>> events = jdbc.queryForList(
                "SELECT VERSION, EVENT_TYPE, AMOUNT, IDEM_KEY, UPSTREAM_ORDERING "
                        + "FROM MINIMAL_EVENT WHERE SCOPE_KEY = ? ORDER BY VERSION", scope);
        long required = 0;
        long ordering = 0;
        boolean opsRejected = false;
        int maxVersion = 0;
        Map<String, Long> created = new HashMap<>();
        Set<String> paidKeys = new HashSet<>();
        Set<String> abandoned = new HashSet<>();
        for (Map<String, Object> e : events) {
            maxVersion = Math.max(maxVersion, ((Number) e.get("VERSION")).intValue());
            switch ((String) e.get("EVENT_TYPE")) {
                case "AMOUNT_REQUIRED" -> {
                    required = ((Number) e.get("AMOUNT")).longValue();
                    ordering = ((Number) e.get("UPSTREAM_ORDERING")).longValue();
                }
                case "REQUEST_CREATED" -> created.put((String) e.get("IDEM_KEY"),
                        ((Number) e.get("AMOUNT")).longValue());
                case "OUTCOME" -> paidKeys.add((String) e.get("IDEM_KEY"));
                case "REQUEST_ABANDONED" -> abandoned.add((String) e.get("IDEM_KEY"));
                case "OPS_REJECTED" -> opsRejected = true;
                default -> { }
            }
        }
        // IDEMPOTENT fold: paid by DISTINCT executed key — never by event count
        long paid = paidKeys.stream().mapToLong(k -> created.getOrDefault(k, 0L)).sum();
        String inflightKey = null;
        long inflightAmount = 0;
        for (Map.Entry<String, Long> c : created.entrySet()) {
            if (!paidKeys.contains(c.getKey()) && !abandoned.contains(c.getKey())) {
                inflightKey = c.getKey();
                inflightAmount = c.getValue();
            }
        }
        return new Folded(required, ordering, paid, inflightKey, inflightAmount,
                maxVersion, opsRejected, paidKeys);
    }

    /** Upstream entry point, strictly-newer guarded and fence-retried. */
    public void onUpstreamAmount(String scope, long amount, long ordering) {
        for (int i = 0; i < 10; i++) {
            Folded f = fold(scope);
            if (ordering <= f.upstreamOrdering() && f.maxVersion() > 0) break;  // stale snapshot ignored
            if (tryAppend(scope, f.maxVersion() + 1, "AMOUNT_REQUIRED", amount, null, ordering)) break;
            // fence conflict: someone else advanced this scope — refold and retry
        }
        settle(scope);
    }

    /** fold-then-act — but the ACT is fenced: a stale plan LOSES the insert. */
    public void settle(String scope) {
        for (int attempt = 0; attempt < 10; attempt++) {
            Folded f = fold(scope);
            if (attempt == 0) {
                hooks.pause("minimal.afterFold." + scope);   // the SAME stale window as the naive model
            }
            if (f.opsRejected() || f.inflightKey() != null) return;
            long delta = f.required() - f.paid();
            if (delta <= 0) return;
            int version = f.maxVersion() + 1;
            String idemKey = scope + "#" + version;          // identity = the fenced version slot
            // WRITE-AHEAD + FENCE in one insert: if this loses the race,
            // the plan was stale — refold, never act on it
            if (!tryAppend(scope, version, "REQUEST_CREATED", delta, idemKey, null)) continue;
            provider.post(idemKey, delta);
            hooks.crashIfArmed("minimal.afterPost." + scope);  // crash in the ambiguity window
            appendOutcome(scope, idemKey, delta);
            return;
        }
        throw new IllegalStateException("fence contention did not settle for " + scope);
    }

    /** Recovery (§9-equivalent): ASK the provider; never blind re-POST. */
    public void recover(String scope) {
        Folded f = fold(scope);
        if (f.inflightKey() == null) {
            settle(scope);
            return;
        }
        FakeProvider.QueryResult q = provider.query(f.inflightKey());
        if ("EXECUTED".equals(q.status())) {
            appendOutcome(scope, f.inflightKey(), q.executedAmount());
        } else if ("NOT_FOUND".equals(q.status())) {
            appendFenced(scope, "REQUEST_ABANDONED", f.inflightAmount(), f.inflightKey(), null);
            settle(scope);   // successor takes a FRESH version slot -> fresh identity
        }
        // LOOKBACK_EXPIRED: nothing may be concluded — stays in flight (ops path)
    }

    /** Ops entry point — unrouted, and it does not matter: the fence
     *  serializes EVERY writer, routed or not. */
    public void opsReject(String scope) {
        appendFenced(scope, "OPS_REJECTED", null, null, null);
    }

    // ---- internals ----

    /** Idempotent, fenced outcome append: at most one OUTCOME per key. */
    private void appendOutcome(String scope, String idemKey, long amount) {
        for (int i = 0; i < 10; i++) {
            Folded f = fold(scope);
            if (f.paidKeys().contains(idemKey)) return;      // already recorded — idempotent
            if (tryAppend(scope, f.maxVersion() + 1, "OUTCOME", amount, idemKey, null)) return;
        }
        throw new IllegalStateException("could not append outcome for " + idemKey);
    }

    private void appendFenced(String scope, String type, Long amount, String idemKey, Long ordering) {
        for (int i = 0; i < 10; i++) {
            Folded f = fold(scope);
            if (tryAppend(scope, f.maxVersion() + 1, type, amount, idemKey, ordering)) return;
        }
        throw new IllegalStateException("could not append " + type + " for " + scope);
    }

    private boolean tryAppend(String scope, int version, String type,
                              Long amount, String idemKey, Long ordering) {
        try {
            jdbc.update("INSERT INTO MINIMAL_EVENT (SCOPE_KEY, VERSION, EVENT_TYPE, AMOUNT, IDEM_KEY, UPSTREAM_ORDERING) "
                            + "VALUES (?,?,?,?,?,?)",
                    scope, version, type, amount, idemKey, ordering);
            return true;
        } catch (DuplicateKeyException fenceConflict) {
            return false;   // someone else won this version slot — the caller must refold
        }
    }

    public int requestEventCount(String scope) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM MINIMAL_EVENT WHERE SCOPE_KEY=? AND EVENT_TYPE='REQUEST_CREATED'",
                Integer.class, scope);
        return n == null ? 0 : n;
    }
}
