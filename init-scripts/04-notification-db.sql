-- notification_db initialization
CREATE TABLE IF NOT EXISTS notifications (
    id              BIGSERIAL PRIMARY KEY,
    transaction_id  VARCHAR(36) NOT NULL,
    user_id         VARCHAR(255) NOT NULL,
    type            VARCHAR(20) NOT NULL,    -- EMAIL | SMS | WEBHOOK
    message         TEXT NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING | SENT | FAILED
    sent_at         TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_notif_user_id        ON notifications (user_id);
CREATE INDEX IF NOT EXISTS idx_notif_transaction_id ON notifications (transaction_id);
