-- Vérification des numéros de téléphone par OTP SMS.
-- Aucun OTP ni jeton de preuve n'est stocké en clair : hash SHA-256 uniquement.
CREATE TABLE IF NOT EXISTS phone_verification (
    id                              BIGSERIAL PRIMARY KEY,
    public_id                       UUID        NOT NULL UNIQUE,
    telephone_e164                  VARCHAR(20) NOT NULL,
    purpose                         VARCHAR(30) NOT NULL,
    otp_hash                        VARCHAR(64) NOT NULL,
    otp_expires_at                  TIMESTAMP   NOT NULL,
    attempts                        INTEGER     NOT NULL DEFAULT 0,
    last_sent_at                    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    verified_at                     TIMESTAMP,
    verification_token_hash         VARCHAR(64) UNIQUE,
    verification_token_expires_at   TIMESTAMP,
    consumed_at                     TIMESTAMP,
    request_ip                      VARCHAR(64),
    created_at                      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_phone_verification_purpose
        CHECK (purpose IN ('DEMANDE_CREATION', 'SUIVI_DEMANDE')),
    CONSTRAINT chk_phone_verification_attempts
        CHECK (attempts >= 0),
    CONSTRAINT chk_phone_verification_e164
        CHECK (telephone_e164 ~ '^\\+[1-9][0-9]{7,14}$')
);

CREATE INDEX IF NOT EXISTS idx_phone_verification_phone_purpose_created
    ON phone_verification (telephone_e164, purpose, created_at);

CREATE INDEX IF NOT EXISTS idx_phone_verification_otp_expires
    ON phone_verification (otp_expires_at);

CREATE INDEX IF NOT EXISTS idx_phone_verification_token_expires
    ON phone_verification (verification_token_expires_at)
    WHERE verification_token_hash IS NOT NULL;
