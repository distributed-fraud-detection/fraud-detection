-- transactions_db initialization
-- This file runs on first container start; Spring Boot ddl-auto=update also handles schema.
-- Kept minimal since JPA/Hibernate creates the table. Indexes added here for production tuning.

CREATE TABLE IF NOT EXISTS transactions (
    id              BIGSERIAL PRIMARY KEY,
    transaction_id  VARCHAR(36) NOT NULL UNIQUE,
    user_id         VARCHAR(255) NOT NULL,
    amount          NUMERIC(15, 2) NOT NULL,
    location        VARCHAR(255) NOT NULL,
    merchant_type   VARCHAR(255) NOT NULL,
    status          VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    timestamp       TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_transaction_id ON transactions (transaction_id);
CREATE INDEX IF NOT EXISTS idx_user_id        ON transactions (user_id);
CREATE INDEX IF NOT EXISTS idx_timestamp      ON transactions (timestamp);
