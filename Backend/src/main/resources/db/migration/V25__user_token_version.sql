-- Révocation ciblée des JWT après modification d'un secret utilisateur.
-- La valeur 0 rend la migration rétrocompatible avec tous les comptes existants.
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS token_version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE users
    ADD CONSTRAINT ck_users_token_version_non_negative
        CHECK (token_version >= 0);
