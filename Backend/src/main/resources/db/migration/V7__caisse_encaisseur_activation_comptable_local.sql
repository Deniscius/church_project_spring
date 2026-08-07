-- Encaisseur caisse, audit d'activation SaaS, rôle comptable local.

-- ===============================
-- Paiements espèces : qui a encaissé
-- ===============================

ALTER TABLE details_paiement
    ADD COLUMN IF NOT EXISTS encaisseur_nom VARCHAR(150);

ALTER TABLE details_paiement
    ADD COLUMN IF NOT EXISTS encaisseur_user_public_id UUID;

COMMENT ON COLUMN details_paiement.encaisseur_nom IS
    'Nom de l''agent ayant encaissé en caisse (espèces)';

-- ===============================
-- Abonnements : traçabilité d'activation
-- ===============================

ALTER TABLE paroisse_abonnement
    ADD COLUMN IF NOT EXISTS activated_at TIMESTAMP(6);

ALTER TABLE paroisse_abonnement
    ADD COLUMN IF NOT EXISTS activation_source VARCHAR(30);

ALTER TABLE paroisse_abonnement
    ADD COLUMN IF NOT EXISTS activated_by_nom VARCHAR(150);

COMMENT ON COLUMN paroisse_abonnement.activation_source IS
    'FEDAPAY | MANUEL';

UPDATE paroisse_abonnement
SET activated_at = COALESCE(debut_at, created_at),
    activation_source = CASE
        WHEN id_transaction IS NOT NULL AND id_transaction <> '' THEN 'FEDAPAY'
        WHEN statut = 'ACTIF' THEN 'MANUEL'
        ELSE activation_source
    END
WHERE statut = 'ACTIF'
  AND activated_at IS NULL;

-- ===============================
-- Rôle COMPTABLE_LOCAL (tentative initiale — noms de contraintes corrigés en V8)
-- ===============================

ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;
ALTER TABLE users
    ADD CONSTRAINT users_role_check
        CHECK (role IN (
            'SECRETAIRE', 'CURE', 'ADMIN', 'COMPTABLE_LOCAL', 'COMPTABLE', 'SUPER_ADMIN'
        ));

ALTER TABLE users DROP CONSTRAINT IF EXISTS ck_users_global_role;
ALTER TABLE users
    ADD CONSTRAINT ck_users_global_role
        CHECK (is_global = (role IN ('SUPER_ADMIN', 'COMPTABLE')));
