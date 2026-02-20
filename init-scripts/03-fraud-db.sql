-- fraud_db initialization
CREATE TABLE IF NOT EXISTS fraud_cases (
    id              BIGSERIAL PRIMARY KEY,
    case_id         VARCHAR(36) NOT NULL UNIQUE,
    transaction_id  VARCHAR(36) NOT NULL,
    user_id         VARCHAR(255) NOT NULL,
    risk_score      DOUBLE PRECISION NOT NULL,
    decision        VARCHAR(20) NOT NULL,   -- APPROVE | BLOCK | REVIEW
    status          VARCHAR(20) NOT NULL,   -- PENDING | APPROVED | REJECTED | BLOCKED
    flag_reason     VARCHAR(500),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_case_transaction_id ON fraud_cases (transaction_id);
CREATE INDEX IF NOT EXISTS idx_case_user_id        ON fraud_cases (user_id);
CREATE INDEX IF NOT EXISTS idx_case_status         ON fraud_cases (status);
CREATE INDEX IF NOT EXISTS idx_case_created_at     ON fraud_cases (created_at DESC);
