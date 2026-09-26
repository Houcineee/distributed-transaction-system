-- ==============================================================================
-- V3: Add B-Tree performance indexes on Foreign Keys and High-Traffic Query Filters
-- ==============================================================================

-- 1. Accounts: Index foreign key for fast user account lookups
CREATE INDEX idx_accounts_user_id ON accounts(user_id);

-- 2. Cards: Index foreign key for fast account card lookups
CREATE INDEX idx_cards_account_id ON cards(account_id);

-- 3. Transactions: Index foreign keys and created_at for fast account history queries
CREATE INDEX idx_transactions_source_account_id ON transactions(source_account_id);
CREATE INDEX idx_transactions_destination_account_id ON transactions(destination_account_id);
CREATE INDEX idx_transactions_created_at ON transactions(created_at DESC);

-- 4. Audit Logs: Index actor_id and timestamp for security forensic queries
CREATE INDEX idx_audit_logs_actor_id ON audit_logs(actor_id);
CREATE INDEX idx_audit_logs_timestamp ON audit_logs(timestamp DESC);
