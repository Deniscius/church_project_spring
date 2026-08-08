-- Jetons de réinitialisation de mot de passe (hash SHA-256 uniquement, jamais le token brut).
CREATE TABLE IF NOT EXISTS password_reset_token (
    id              BIGSERIAL PRIMARY KEY,
    public_id       UUID         NOT NULL UNIQUE,
    user_id         BIGINT       NOT NULL REFERENCES users (id),
    token_hash      VARCHAR(64)  NOT NULL UNIQUE,
    expires_at      TIMESTAMP    NOT NULL,
    used_at         TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    request_ip      VARCHAR(64)
);

CREATE INDEX IF NOT EXISTS idx_prt_user_id ON password_reset_token (user_id);
CREATE INDEX IF NOT EXISTS idx_prt_expires ON password_reset_token (expires_at);
