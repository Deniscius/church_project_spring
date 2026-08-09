-- Autorise la purge du hash admin après approbation / rejet (secret déjà sur users).
ALTER TABLE paroisse_inscription
    ALTER COLUMN admin_password_hash DROP NOT NULL;
