ALTER TABLE users ADD COLUMN token_version BIGINT NOT NULL DEFAULT 0;

-- Legacy tokens were not hashed; only newly issued, hashed tokens may be used.
DELETE FROM verification_tokens;

CREATE INDEX idx_verification_tokens_user_type ON verification_tokens (user_id, type);
