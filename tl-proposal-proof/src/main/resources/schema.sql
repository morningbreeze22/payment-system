-- ============================================================
-- NAIVE MODEL (the tech-lead proposal, built faithfully):
-- one INSERT-ONLY event table, no obligation row, no locks,
-- no unique constraints beyond the PK — "reconstruct the whole
-- flow from this table".
-- ============================================================
CREATE TABLE IF NOT EXISTS NAIVE_EVENT (
  ID          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  SCOPE_KEY   VARCHAR(64)  NOT NULL,
  EVENT_TYPE  VARCHAR(32)  NOT NULL,  -- AMOUNT_REQUIRED / REQUEST_CREATED / OUTCOME / OPS_REJECTED
  SEQ         INT,                    -- request sequence (REQUEST_CREATED / OUTCOME)
  AMOUNT      BIGINT,
  IDEM_KEY    VARCHAR(128),
  CREATED_AT  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS NAIVE_EVENT_SCOPE ON NAIVE_EVENT(SCOPE_KEY, ID);

-- ============================================================
-- GUARDED MODEL (the reviewed design, reduced to its essentials):
-- obligation row = money ledger + serialization point + counter;
-- request row = mutable current state with DB-enforced invariants.
-- ACTIVE_KEY emulates Oracle's I6 unique function index
-- (CASE WHEN outcome IS NULL THEN payment_obligation_id END):
-- it holds OBLIGATION_ID while the request is ACTIVE, NULL once
-- terminal; UNIQUE(ACTIVE_KEY) = at most one active request per
-- obligation (H2: NULLs are distinct in unique indexes).
-- ============================================================
CREATE TABLE IF NOT EXISTS OBLIGATION (
  ID               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  SCOPE_KEY        VARCHAR(64) NOT NULL,
  REQUIRED_AMOUNT  BIGINT      NOT NULL,
  CONFIRMED_AMOUNT BIGINT      NOT NULL DEFAULT 0,
  NEXT_REQUEST_SEQ INT         NOT NULL DEFAULT 1,
  BLOCKED          BOOLEAN     NOT NULL DEFAULT FALSE,
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
  ACTIVE_KEY    BIGINT,                  -- I6 emulation (see header comment)
  CONSTRAINT REQUEST_IDEM_UQ   UNIQUE (IDEM_KEY),
  CONSTRAINT REQUEST_SEQ_UQ    UNIQUE (OBLIGATION_ID, SEQ),
  CONSTRAINT REQUEST_ACTIVE_I6 UNIQUE (ACTIVE_KEY)
);
