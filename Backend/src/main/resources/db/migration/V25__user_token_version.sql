-- Révocation ciblée des JWT après modification d'un secret utilisateur.
-- La valeur 0 rend la migration rétrocompatible avec tous les comptes existants.
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS token_version BIGINT;

-- Répare également une éventuelle colonne créée manuellement avant Flyway.
UPDATE users
SET token_version = 0
WHERE token_version IS NULL;

ALTER TABLE users
    ALTER COLUMN token_version SET DEFAULT 0,
    ALTER COLUMN token_version SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ck_users_token_version_non_negative'
          AND conrelid = 'users'::regclass
    ) THEN
        ALTER TABLE users
            ADD CONSTRAINT ck_users_token_version_non_negative
                CHECK (token_version >= 0);
    END IF;
END
$$;
