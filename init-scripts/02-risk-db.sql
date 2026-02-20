-- risk_db initialization
CREATE TABLE IF NOT EXISTS risk_profiles (
    id                BIGSERIAL PRIMARY KEY,
    user_id           VARCHAR(255) NOT NULL UNIQUE,
    risk_score        DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    risk_level        VARCHAR(20) NOT NULL DEFAULT 'LOW',
    recent_fraud_count INTEGER DEFAULT 0,
    txn_frequency     INTEGER DEFAULT 0,
    top_risk_factor   VARCHAR(255),
    last_updated      TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS feature_weights (
    id           BIGSERIAL PRIMARY KEY,
    feature_name VARCHAR(100) NOT NULL UNIQUE,
    weight       DOUBLE PRECISION NOT NULL,
    description  VARCHAR(500)
);

CREATE INDEX IF NOT EXISTS idx_risk_user_id ON risk_profiles (user_id);

-- Seed default feature weights
INSERT INTO feature_weights (feature_name, weight, description) VALUES
    ('HIGH_AMOUNT',      0.35, 'Transaction amount > 10,000'),
    ('MEDIUM_AMOUNT',    0.20, 'Transaction amount 5,000–10,000'),
    ('UNKNOWN_LOCATION', 0.25, 'Transaction from unknown/offshore location'),
    ('HIGH_RISK_MERCHANT', 0.20, 'Casino, crypto, or gambling merchant type'),
    ('HIGH_FREQUENCY',   0.15, 'More than 8 transactions per minute')
ON CONFLICT (feature_name) DO NOTHING;
