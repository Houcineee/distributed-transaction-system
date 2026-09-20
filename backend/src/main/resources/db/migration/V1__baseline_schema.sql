CREATE TABLE users (
    id UUID PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    account_locked BOOLEAN NOT NULL DEFAULT false,
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE accounts (
    id UUID PRIMARY KEY,
    balance BIGINT NOT NULL,
    currency VARCHAR(10) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT true,
    user_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE transactions (
    id UUID PRIMARY KEY,
    amount BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    source_account_id UUID NOT NULL REFERENCES accounts(id),
    destination_account_id UUID NOT NULL REFERENCES accounts(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    actor_id UUID,
    action VARCHAR(50) NOT NULL,
    target_entity_type VARCHAR(50),
    target_entity_id UUID,
    details JSONB NOT NULL,
    ip_address VARCHAR(50) NOT NULL,
    correlation_id VARCHAR(50) NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
-- ==============================================================================
-- ZERO-TRUST IMMUTABILITY COMPLIANCE
-- Enforce soft-deletes and append-only auditing at the database engine level.
-- ==============================================================================

CREATE OR REPLACE FUNCTION prevent_modification()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Compliance Violation: Modification or deletion of immutable records is strictly prohibited.';
END;
$$ LANGUAGE plpgsql;

-- Audit Logs are strictly append-only (no updates, no deletes)
CREATE TRIGGER enforce_audit_log_immutability
BEFORE UPDATE OR DELETE ON audit_logs
FOR EACH ROW EXECUTE FUNCTION prevent_modification();

-- Transactions cannot be deleted (updates allowed for status changes e.g. PENDING -> FAILED)
CREATE TRIGGER enforce_transaction_immutability
BEFORE DELETE ON transactions
FOR EACH ROW EXECUTE FUNCTION prevent_modification();

-- Users cannot be hard-deleted (soft-deletes only via 'enabled' boolean)
CREATE TRIGGER enforce_user_immutability
BEFORE DELETE ON users
FOR EACH ROW EXECUTE FUNCTION prevent_modification();

-- Accounts cannot be hard-deleted (soft-deletes only via 'enabled' boolean)
CREATE TRIGGER enforce_account_immutability
BEFORE DELETE ON accounts
FOR EACH ROW EXECUTE FUNCTION prevent_modification();
