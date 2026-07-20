package proof.guarded;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import proof.TestHooks;
import proof.provider.FakeProvider;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * THE REVIEWED DESIGN, reduced to its essentials (requirment-v4.md):
 *
 *  - the OBLIGATION row = money ledger + serialization point + the
 *    next_request_seq counter (§2.1, §3); the documented lock order is
 *    followed on EVERY mutation: obligation lock -> request CAS ->
 *    obligation amount changes (review finding 4);
 *  - §6.7 strictly-newer admission: a stale upstream snapshot can
 *    never regress REQUIRED_AMOUNT (review finding 5);
 *  - WRITE-AHEAD identity (§5.1) before the wire; guarded CAS
 *    transitions (§10); ask-before-retry (§7.0/§9.1); release guard
 *    (§10.3);
 *  - DB-enforced invariants as BACKSTOPS — I6 via a GENERATED column
 *    no writer can supply (review finding 6), UNIQUE(idem_key),
 *    UNIQUE(obligation, seq), STATE CHECK, FK.
 */
@Service
public class GuardedPaymentService {

    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final FakeProvider provider;
    private final TestHooks hooks;

    public GuardedPaymentService(JdbcTemplate jdbc, TransactionTemplate tx,
                                 FakeProvider provider, TestHooks hooks) {
        this.jdbc = jdbc;
        this.tx = tx;
        this.provider = provider;
        this.hooks = hooks;
    }

    /** Upstream entry point with the §6.7 strictly-newer ordering guard. */
    public void onUpstreamAmount(String scope, long amount, long ordering) {
        tx.executeWithoutResult(s -> {
            try {
                jdbc.update("INSERT INTO OBLIGATION (SCOPE_KEY, REQUIRED_AMOUNT, UPSTREAM_ORDERING) VALUES (?,?,?)",
                        scope, amount, ordering);
            } catch (DuplicateKeyException e) {
                Map<String, Object> ob = lockObligation(scope);
                long current = ((Number) Objects.requireNonNull(ob).get("UPSTREAM_ORDERING")).longValue();
                if (ordering > current) {   // strictly newer wins; a delayed old snapshot is ignored
                    jdbc.update("UPDATE OBLIGATION SET REQUIRED_AMOUNT = ?, UPSTREAM_ORDERING = ? WHERE SCOPE_KEY = ?",
                            amount, ordering, scope);
                }
            }
        });
        settle(scope);
    }

    private record Plan(long obligationId, long requestId, int seq, String idemKey, long delta) {}

    /** Decide-and-claim in ONE locked transaction (write-ahead), then wire, then CAS. */
    public void settle(String scope) {
        hooks.pause("guarded.beforeTx." + scope);  // the zombie can only park HERE —
                                                   // its "plan" does not exist yet, so it cannot be stale
        Plan plan = tx.execute(s -> {
            Map<String, Object> ob = lockObligation(scope);   // serialization point
            if (ob == null || (Boolean) ob.get("BLOCKED")) return null;
            long required = ((Number) ob.get("REQUIRED_AMOUNT")).longValue();
            long confirmed = ((Number) ob.get("CONFIRMED_AMOUNT")).longValue();
            long obligationId = ((Number) ob.get("ID")).longValue();
            Integer active = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM REQUEST WHERE OBLIGATION_ID=? AND STATE='IN_FLIGHT'",
                    Integer.class, obligationId);
            if (active != null && active > 0) return null;    // §7: ambiguous work is resolved, never raced
            long delta = required - confirmed;
            if (delta <= 0) return null;
            int seq = ((Number) ob.get("NEXT_REQUEST_SEQ")).intValue();  // counter, NOT MAX(history)+1
            jdbc.update("UPDATE OBLIGATION SET NEXT_REQUEST_SEQ = NEXT_REQUEST_SEQ + 1 WHERE ID = ?", obligationId);
            String idemKey = scope + "#" + seq;
            // WRITE-AHEAD (§5.1): identity + payload hash durable BEFORE the wire
            jdbc.update("INSERT INTO REQUEST (OBLIGATION_ID, SEQ, AMOUNT, IDEM_KEY, PAYLOAD_HASH, STATE) "
                            + "VALUES (?,?,?,?,?, 'IN_FLIGHT')",
                    obligationId, seq, delta, idemKey, "H" + delta);
            Long requestId = jdbc.queryForObject(
                    "SELECT ID FROM REQUEST WHERE IDEM_KEY = ?", Long.class, idemKey);
            return new Plan(obligationId, Objects.requireNonNull(requestId), seq, idemKey, delta);
        });
        if (plan == null) return;
        hooks.crashIfArmed("guarded.afterWriteAhead." + scope);
        provider.post(plan.idemKey(), plan.delta());
        hooks.crashIfArmed("guarded.afterPost." + scope);      // crash in the ambiguity window
        confirmExecuted(plan.obligationId(), plan.requestId(), plan.delta());
    }

    /** Resolver (§9): every IN_FLIGHT row is resolved by ASKING, never by re-POST. */
    public void recover(String scope) {
        List<Map<String, Object>> inflight = jdbc.queryForList(
                "SELECT R.ID, R.OBLIGATION_ID, R.AMOUNT, R.IDEM_KEY FROM REQUEST R "
                        + "JOIN OBLIGATION O ON O.ID = R.OBLIGATION_ID "
                        + "WHERE O.SCOPE_KEY = ? AND R.STATE = 'IN_FLIGHT'", scope);
        for (Map<String, Object> r : inflight) {
            FakeProvider.QueryResult q = provider.query((String) r.get("IDEM_KEY"));
            long id = ((Number) r.get("ID")).longValue();
            long obId = ((Number) r.get("OBLIGATION_ID")).longValue();
            if ("EXECUTED".equals(q.status())) {
                confirmExecuted(obId, id, q.executedAmount());
            } else if ("NOT_FOUND".equals(q.status())) {
                // authoritative inside the lookback window: provably not executed
                tx.executeWithoutResult(s -> {
                    lockObligationById(obId);                                // lock order (finding 4)
                    jdbc.update("UPDATE REQUEST SET STATE='REJECTED' WHERE ID=? AND STATE='IN_FLIGHT'", id);
                });
                settle(scope);  // fresh successor with a FRESH seq (§6.8)
            }
            // LOOKBACK_EXPIRED: nothing may be concluded — the row STAYS
            // IN_FLIGHT (the design's MAYBE state) for the §9.3 ops path.
        }
    }

    /** Ops endpoint — an UNROUTED writer, and the release guard refusing
     *  to let a human terminal a row whose outcome is uncertain (§10.3). */
    public void opsReject(String scope) {
        tx.executeWithoutResult(s -> {
            Map<String, Object> ob = lockObligation(scope);
            if (ob == null) return;
            long obligationId = ((Number) ob.get("ID")).longValue();
            Integer active = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM REQUEST WHERE OBLIGATION_ID=? AND STATE='IN_FLIGHT'",
                    Integer.class, obligationId);
            if (active != null && active > 0) {
                throw new IllegalStateException(
                        "release guard: an in-flight request has an uncertain outcome — "
                                + "resolve it via the §9 verified-outcome path first");
            }
            jdbc.update("UPDATE OBLIGATION SET BLOCKED = TRUE WHERE ID = ?", obligationId);
        });
    }

    /** Documented lock order (review finding 4):
     *  obligation lock -> request CAS -> obligation amount changes. */
    private void confirmExecuted(long obligationId, long requestId, long amount) {
        tx.executeWithoutResult(s -> {
            lockObligationById(obligationId);
            // CAS: the WHERE states the expected world; the row count is the verdict
            int n = jdbc.update(
                    "UPDATE REQUEST SET STATE='EXECUTED' WHERE ID=? AND STATE='IN_FLIGHT'", requestId);
            if (n == 1) {
                jdbc.update("UPDATE OBLIGATION SET CONFIRMED_AMOUNT = CONFIRMED_AMOUNT + ? WHERE ID = ?",
                        amount, obligationId);
            } // n == 0: a late/duplicate confirmation — refused by the row count
        });
    }

    private Map<String, Object> lockObligation(String scope) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT ID, REQUIRED_AMOUNT, CONFIRMED_AMOUNT, NEXT_REQUEST_SEQ, UPSTREAM_ORDERING, BLOCKED "
                        + "FROM OBLIGATION WHERE SCOPE_KEY = ? FOR UPDATE", scope);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private void lockObligationById(long id) {
        jdbc.queryForList("SELECT ID FROM OBLIGATION WHERE ID = ? FOR UPDATE", id);
    }

    // ---- believed state, for truth-vs-belief assertions ----

    public long confirmedAmount(String scope) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT CONFIRMED_AMOUNT FROM OBLIGATION WHERE SCOPE_KEY = ?", scope);
        return rows.isEmpty() ? 0 : ((Number) rows.get(0).get("CONFIRMED_AMOUNT")).longValue();
    }

    public long requiredAmount(String scope) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT REQUIRED_AMOUNT FROM OBLIGATION WHERE SCOPE_KEY = ?", scope);
        return rows.isEmpty() ? 0 : ((Number) rows.get(0).get("REQUIRED_AMOUNT")).longValue();
    }

    public int requestCount(String scope) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM REQUEST R JOIN OBLIGATION O ON O.ID = R.OBLIGATION_ID WHERE O.SCOPE_KEY = ?",
                Integer.class, scope);
        return n == null ? 0 : n;
    }
}
