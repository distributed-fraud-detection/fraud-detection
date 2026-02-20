-- analytics_db initialization
CREATE TABLE IF NOT EXISTS aggregated_metrics (
    id                   BIGSERIAL PRIMARY KEY,
    metric_date          DATE NOT NULL UNIQUE,
    total_transactions   BIGINT DEFAULT 0,
    fraud_count          BIGINT DEFAULT 0,
    review_count         BIGINT DEFAULT 0,
    block_count          BIGINT DEFAULT 0,
    fraud_rate           DOUBLE PRECISION DEFAULT 0.0,
    top_risk_geography   VARCHAR(255),
    avg_risk_score       DOUBLE PRECISION DEFAULT 0.0,
    created_at           TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_metric_date ON aggregated_metrics (metric_date DESC);

-- Spring Batch metadata tables are auto-created by Spring Boot Batch on startup.
-- No need to create them manually here.
