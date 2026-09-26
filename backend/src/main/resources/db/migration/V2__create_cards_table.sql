CREATE TABLE cards (
    id UUID PRIMARY KEY,
    card_number VARCHAR(16) NOT NULL UNIQUE,
    cardholder_name VARCHAR(100) NOT NULL,
    cvv VARCHAR(3) NOT NULL,
    expiration_date DATE NOT NULL,
    card_type VARCHAR(10) NOT NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE',
    account_id UUID NOT NULL REFERENCES accounts(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Cards cannot be hard-deleted (block them via status instead)
CREATE TRIGGER enforce_card_immutability
BEFORE DELETE ON cards
FOR EACH ROW EXECUTE FUNCTION prevent_modification();
