CREATE TABLE IF NOT EXISTS account_service.accounts (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    currency VARCHAR(3) NOT NULL,
    balance NUMERIC(19, 4) NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_accounts_customer_id
    ON account_service.accounts (customer_id);
