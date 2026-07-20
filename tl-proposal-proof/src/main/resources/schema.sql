-- ============================================================
-- MODEL 1 — NAIVE (the proposal as literally stated):
-- one INSERT-ONLY event table, no obligation row, no locks,
-- no unique constraints beyond the PK, fold-then-act.
-- ============================================================
CREATE TABLE IF NOT EXISTS NAIVE_EVENT (
  ID          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  SCOPE_KEY   VARCHAR(64)  NOT NULL,
  EVENT_TYPE  VARCHAR(32)  NOT NULL,  -- AMOUNT_REQUIRED / REQUEST_CREATED / OUTCOME / OPS_REJECTED
  SEQ         INT,
  AMOUNT      BIGINT,
  IDEM_KEY    VARCHAR(128),
  CREATED_AT  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS NAIVE_EVENT_SCOPE ON NAIVE_EVENT(SCOPE_KEY, ID);

-- ============================================================
-- MODEL 2 — MINIMAL SINGLE-WRITER (the STRONGEST reasonable
-- version of the proposal, per external review): immutable
-- events, per-scope VERSION FENCE (optimistic concurrency),
-- identity from the fenced version (never MAX+1 of history),
-- write-once idempotency keys, idempotent folding, write-ahead,
-- ask-before-retry. NO obligation row, NO SELECT FOR UPDATE,
-- NO stored counters.
--
-- IDEM_UNIQ is a GENERATED column: only REQUEST_CREATED events
-- claim an identity, and no writer can bypass the uniqueness.
-- ============================================================
CREATE TABLE IF NOT EXISTS MINIMAL_EVENT (
  ID                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  SCOPE_KEY         VARCHAR(64)  NOT NULL,
  VERSION           INT          NOT NULL,   -- the fence: one writer wins per (scope, version)
  EVENT_TYPE        VARCHAR(32)  NOT NULL,   -- AMOUNT_REQUIRED / REQUEST_CREATED / OUTCOME / REQUEST_ABANDONED / OPS_REJECTED
  AMOUNT            BIGINT,
  IDEM_KEY          VARCHAR(128),
  UPSTREAM_ORDERING BIGINT,
  IDEM_UNIQ         VARCHAR(128) GENERATED ALWAYS AS
                      (CASE WHEN EVENT_TYPE = 'REQUEST_CREATED' THEN IDEM_KEY END),
  CONSTRAINT MINIMAL_EVENT_TYPE_CK CHECK (EVENT_TYPE IN
      ('AMOUNT_REQUIRED','REQUEST_CREATED','OUTCOME','REQUEST_ABANDONED','OPS_REJECTED')),
  CONSTRAINT MINIMAL_FENCE_UQ UNIQUE (SCOPE_KEY, VERSION),
  CONSTRAINT MINIMAL_IDEM_UQ  UNIQUE (IDEM_UNIQ)
);

-- ============================================================
-- MODEL 3 — GUARDED (the reviewed design, reduced to essentials):
-- obligation row = money ledger + serialization point + counter;
-- request row = mutable current state with DB-enforced invariants.
--
-- ACTIVE_KEY emulates Oracle's I6 unique function index
-- (CASE WHEN outcome IS NULL THEN payment_obligation_id END) as a
-- GENERATED column — no writer can supply or bypass it (review
-- finding 6). STATE is CHECK-bound and requests are FK-bound.
-- ============================================================
CREATE TABLE IF NOT EXISTS OBLIGATION (
  ID                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  SCOPE_KEY         VARCHAR(64) NOT NULL,
  REQUIRED_AMOUNT   BIGINT      NOT NULL,
  CONFIRMED_AMOUNT  BIGINT      NOT NULL DEFAULT 0,
  NEXT_REQUEST_SEQ  INT         NOT NULL DEFAULT 1,
  UPSTREAM_ORDERING BIGINT      NOT NULL DEFAULT 0,   -- §6.7 strictly-newer admission guard
  BLOCKED           BOOLEAN     NOT NULL DEFAULT FALSE,
  CONSTRAINT OBLIGATION_SCOPE_UQ UNIQUE (SCOPE_KEY)
);

CREATE TABLE IF NOT EXISTS REQUEST (
  ID            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  OBLIGATION_ID BIGINT       NOT NULL,
  SEQ           INT          NOT NULL,
  AMOUNT        BIGINT       NOT NULL,
  IDEM_KEY      VARCHAR(128) NOT NULL,
  PAYLOAD_HASH  VARCHAR(64)  NOT NULL,   -- §5.1 write-ahead instruction hash
  STATE         VARCHAR(16)  NOT NULL,   -- IN_FLIGHT / EXECUTED / REJECTED
  ACTIVE_KEY    BIGINT       GENERATED ALWAYS AS
                  (CASE WHEN STATE = 'IN_FLIGHT' THEN OBLIGATION_ID END),
  CONSTRAINT REQUEST_STATE_CK  CHECK (STATE IN ('IN_FLIGHT','EXECUTED','REJECTED')),
  CONSTRAINT REQUEST_OBLIG_FK  FOREIGN KEY (OBLIGATION_ID) REFERENCES OBLIGATION(ID),
  CONSTRAINT REQUEST_IDEM_UQ   UNIQUE (IDEM_KEY),
  CONSTRAINT REQUEST_SEQ_UQ    UNIQUE (OBLIGATION_ID, SEQ),
  CONSTRAINT REQUEST_ACTIVE_I6 UNIQUE (ACTIVE_KEY)
);
