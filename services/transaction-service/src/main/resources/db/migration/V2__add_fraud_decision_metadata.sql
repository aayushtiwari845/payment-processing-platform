ALTER TABLE transaction_service.transactions
    ADD COLUMN IF NOT EXISTS fraud_approved BOOLEAN,
    ADD COLUMN IF NOT EXISTS fraud_score NUMERIC(5, 4),
    ADD COLUMN IF NOT EXISTS fraud_model_version VARCHAR(128),
    ADD COLUMN IF NOT EXISTS fraud_reason VARCHAR(255),
    ADD COLUMN IF NOT EXISTS fraud_decision_at TIMESTAMP;
