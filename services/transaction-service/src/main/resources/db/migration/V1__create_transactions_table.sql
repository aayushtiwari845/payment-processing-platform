CREATE TABLE IF NOT EXISTS transaction_service.transactions (
    id UUID PRIMARY KEY,
    source_account_id UUID NOT NULL,
    destination_account_id UUID NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(32) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_transactions_source_account
    ON transaction_service.transactions (source_account_id);
