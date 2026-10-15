-- -----------------------------------------------------------------------------
-- Flyway Migration V3: account password reset tokens
-- -----------------------------------------------------------------------------
-- Single-use, expiring password reset tokens for FARMER, PARTNER and OPERATOR
-- accounts (admins are excluded from self-service reset).
--
-- Additive only: creates one new table and does not alter or rewrite any
-- existing table or row. The legacy password_reset_tokens table (plaintext
-- tokens, users table only) is no longer read or written by the application.
--
--   token_hash  SHA-256 of the token, lowercase hex. The token itself is never
--               stored; it only exists in the reset link delivered to the user.
--   account_*   account_type names the single account table that account_id
--               belongs to (farmers / partners / operators).
--   used_at     set exactly once, atomically, when the token is redeemed or
--               invalidated (a newer request or a completed reset invalidates
--               the account's other outstanding tokens).
-- -----------------------------------------------------------------------------

CREATE TABLE account_password_reset_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT,
    account_type ENUM ('FARMER','OPERATOR','PARTNER') NOT NULL,
    account_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    used_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_account_password_reset_tokens_token_hash UNIQUE (token_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_account_password_reset_tokens_account
    ON account_password_reset_tokens (account_type, account_id, used_at);
