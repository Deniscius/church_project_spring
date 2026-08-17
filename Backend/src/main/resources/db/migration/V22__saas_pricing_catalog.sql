-- Catalogue tarifaire SaaS administrable par le SUPER_ADMIN.
-- Les abonnements conservent un snapshot montant + durée afin qu'un changement
-- de catalogue ne modifie jamais rétroactivement un contrat déjà créé.

CREATE TABLE plan_saas (
    id               BIGSERIAL PRIMARY KEY,
    public_id        UUID         NOT NULL UNIQUE,
    code             VARCHAR(30)  NOT NULL,
    nom              VARCHAR(100) NOT NULL,
    description      VARCHAR(300),
    montant_xof      INTEGER      NOT NULL,
    duree_mois       INTEGER      NOT NULL,
    actif             BOOLEAN      NOT NULL DEFAULT TRUE,
    featured         BOOLEAN      NOT NULL DEFAULT FALSE,
    ordre_affichage  INTEGER      NOT NULL DEFAULT 0,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status_del       BOOLEAN      NOT NULL DEFAULT FALSE,

    CONSTRAINT uk_plan_saas_code UNIQUE (code),
    CONSTRAINT chk_plan_saas_code
        CHECK (code IN ('MENSUEL', 'SEMESTRIEL', 'ANNUEL')),
    CONSTRAINT chk_plan_saas_montant
        CHECK (montant_xof > 0),
    CONSTRAINT chk_plan_saas_duree
        CHECK (duree_mois > 0),
    CONSTRAINT chk_plan_saas_ordre
        CHECK (ordre_affichage >= 0)
);

INSERT INTO plan_saas (
    public_id, code, nom, description, montant_xof, duree_mois,
    actif, featured, ordre_affichage
) VALUES
    ('a1000000-0000-0000-0000-000000000001', 'MENSUEL', 'Mensuel',
     'Souplesse, sans engagement long', 5000, 1, TRUE, FALSE, 10),
    ('a1000000-0000-0000-0000-000000000002', 'SEMESTRIEL', '6 mois',
     'Le meilleur équilibre prix / durée', 8000, 6, TRUE, TRUE, 20),
    ('a1000000-0000-0000-0000-000000000003', 'ANNUEL', 'Annuel',
     'Tarif le plus avantageux', 12000, 12, TRUE, FALSE, 30);

ALTER TABLE paroisse_abonnement
    ADD COLUMN IF NOT EXISTS duree_mois INTEGER;

UPDATE paroisse_abonnement
SET duree_mois = CASE plan
    WHEN 'MENSUEL' THEN 1
    WHEN 'SEMESTRIEL' THEN 6
    WHEN 'ANNUEL' THEN 12
    ELSE 1
END
WHERE duree_mois IS NULL;

ALTER TABLE paroisse_abonnement
    ALTER COLUMN duree_mois SET NOT NULL;

ALTER TABLE paroisse_abonnement
    ADD CONSTRAINT chk_paroisse_abonnement_duree_mois CHECK (duree_mois > 0);
