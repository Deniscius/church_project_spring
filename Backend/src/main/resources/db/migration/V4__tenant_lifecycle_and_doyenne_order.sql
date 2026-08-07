-- Deux évolutions liées à l'arrivée de l'annuaire diocésain complet.
--
-- 1. Les doyennés ont un ordre officiel (I à XIII) qui n'est pas l'ordre
--    alphabétique. Cet ordre doit être porté par une colonne, pas déduit d'un
--    libellé, pour que l'API et l'interface le respectent partout.
--
-- 2. Le booléen is_active ne suffit plus à décrire une paroisse. Il confondait
--    « pas encore cliente » et « suspendue pour impayé », or l'annuaire ajoute
--    134 paroisses qui ne sont ni l'un ni l'autre : elles existent, mais n'ont
--    jamais été démarchées. Le cycle de vie devient explicite.

-- ===============================
-- Ordre officiel des doyennés
-- ===============================

ALTER TABLE doyenne
    ADD COLUMN rang INTEGER;

-- Le doyenné saisi à la main avant l'import est aligné sur la nomenclature
-- officielle plutôt que dupliqué.
UPDATE doyenne
SET nom = 'Lomé-Centre'
WHERE lower(btrim(nom)) = lower(btrim('Lomé-centre'));

UPDATE doyenne
SET rang = src.rang,
    description = NULL
FROM (VALUES
    ('Lomé-Centre',                1),
    ('Lomé-Nord 1 (Hédzranawoé)',  2),
    ('Lomé-Nord 2 (Tokoin)',       3),
    ('Lomé-Est 1 (Bè)',            4),
    ('Lomé-Est 2 (Baguida)',       5),
    ('Lomé-Ouest 1 (Klikamé)',     6),
    ('Lomé-Ouest 2 (Agoè-Nyivé)',  7),
    ('Lomé-Ouest 3 (Adidogomé)',   8),
    ('Lomé-Ouest 4 (Sanguéra)',    9),
    ('Nord-Est 1 (Tsévié)',       10),
    ('Nord-Est 2 (Agbélouvé)',    11),
    ('Nord-Est 3 (Abobo)',        12),
    ('Nord-Ouest (Assahoun)',     13)
) AS src(nom, rang)
WHERE lower(btrim(doyenne.nom)) = lower(btrim(src.nom));

-- Un doyenné hors nomenclature diocésaine se range après les treize officiels.
UPDATE doyenne
SET rang = 99
WHERE rang IS NULL;

ALTER TABLE doyenne
    ALTER COLUMN rang SET NOT NULL;

ALTER TABLE doyenne
    ADD CONSTRAINT ck_doyenne_rang_positif CHECK (rang >= 1);

-- ===============================
-- Cycle de vie du tenant paroisse
-- ===============================

ALTER TABLE paroisse
    ADD COLUMN statut_tenant VARCHAR(30);

-- Reconstitution de l'état courant à partir des deux seuls signaux existants :
-- l'interrupteur d'accès et l'échéance d'abonnement. Une paroisse inactive sans
-- échéance et sans compte rattaché n'a jamais été démarchée : c'est un prospect.
UPDATE paroisse
SET statut_tenant = CASE
    WHEN is_active AND (subscription_expires_at IS NULL OR subscription_expires_at >= LOCALTIMESTAMP)
        THEN 'ACTIVE'
    WHEN is_active
        THEN 'EN_TOLERANCE'
    WHEN subscription_expires_at IS NOT NULL
        THEN 'SUSPENDUE'
    WHEN EXISTS (
            SELECT 1 FROM paroisse_access pa
            WHERE pa.paroisse_id = paroisse.id AND pa.status_del = FALSE
         )
        THEN 'EN_ATTENTE_PAIEMENT'
    ELSE 'PROSPECT'
END;

ALTER TABLE paroisse
    ALTER COLUMN statut_tenant SET NOT NULL;

ALTER TABLE paroisse
    ADD CONSTRAINT ck_paroisse_statut_tenant
        CHECK (statut_tenant IN (
            'PROSPECT',
            'EN_ATTENTE_PAIEMENT',
            'ACTIVE',
            'EN_TOLERANCE',
            'SUSPENDUE',
            'RESILIEE'
        ));

-- is_active reste l'interrupteur lu par l'authentification et par le dépôt de
-- demandes ; il devient une projection du cycle de vie, et ne peut plus diverger.
ALTER TABLE paroisse
    ADD CONSTRAINT ck_paroisse_actif_suit_statut
        CHECK (is_active = (statut_tenant IN ('ACTIVE', 'EN_TOLERANCE')));

CREATE INDEX idx_paroisse_statut_tenant
    ON paroisse (statut_tenant)
    WHERE status_del = FALSE;
