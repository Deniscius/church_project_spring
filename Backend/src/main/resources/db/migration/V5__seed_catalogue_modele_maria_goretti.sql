-- Catalogue métier de référence (paroisse Sainte Maria Goretti / Bè-Kpota).
--
-- Le mécanisme TenantCatalogBootstrapService clone ce catalogue vers chaque
-- nouvelle paroisse à l'inscription / activation. Sans ces lignes, le clonage
-- n'avait rien à copier : les tables type_demande / forfait_tarif / horaire
-- étaient vides après la reconstruction Flyway.
--
-- Grille tarifaire type (ajustable ensuite paroisse par paroisse) :
--   Intention : normale 5 000 · dominicale 10 000 · spéciale 20 000
--   Triduum 15 000 · Neuvaine 40 000 · Trentaine 100 000
--
-- Les types de paiement globaux (TMONEY, FLOOZ, ESPECES, CARTE) sont aussi
-- semés ici : sans ESPECES, l'encaissement en caisse locale échoue.

-- ===============================
-- Modes de paiement plateforme
-- ===============================

INSERT INTO type_paiement (public_id, libelle, mode, status_del, created_at, updated_at)
SELECT gen_random_uuid(), src.libelle, src.mode, FALSE, LOCALTIMESTAMP, LOCALTIMESTAMP
FROM (VALUES
    ('TMoney',                      'TMONEY'),
    ('Flooz',                       'FLOOZ'),
    ('Espèces (au secrétariat)',    'ESPECES'),
    ('Carte bancaire',              'CARTE')
) AS src(libelle, mode)
WHERE NOT EXISTS (
    SELECT 1 FROM type_paiement tp
    WHERE tp.mode = src.mode AND tp.status_del = FALSE
);

-- ===============================
-- Horaires de la paroisse modèle
-- ===============================

INSERT INTO horaire (
    public_id, paroisse_id, jour_semaine, heure_celebration, libelle,
    is_active, status_del, created_at, updated_at
)
SELECT gen_random_uuid(), p.id, src.jour, src.heure::time, src.libelle,
       TRUE, FALSE, LOCALTIMESTAMP, LOCALTIMESTAMP
FROM paroisse p
CROSS JOIN (VALUES
    ('LUNDI',     '06:30', 'Messe du matin'),
    ('MARDI',     '06:30', 'Messe du matin'),
    ('MERCREDI',  '06:30', 'Messe du matin'),
    ('JEUDI',     '06:30', 'Messe du matin'),
    ('JEUDI',     '18:00', 'Messe du soir'),
    ('VENDREDI',  '06:30', 'Messe du matin'),
    ('SAMEDI',    '06:30', 'Messe du matin'),
    ('DIMANCHE',  '07:00', 'Messe dominicale'),
    ('DIMANCHE',  '09:00', 'Messe dominicale'),
    ('DIMANCHE',  '18:00', 'Messe du soir')
) AS src(jour, heure, libelle)
WHERE p.nom ILIKE '%Maria Goretti%'
  AND p.status_del = FALSE
  AND NOT EXISTS (
      SELECT 1 FROM horaire h
      WHERE h.paroisse_id = p.id AND h.status_del = FALSE
  );

-- ===============================
-- Types de demande + forfaits
-- ===============================

DO $$
DECLARE
    v_paroisse_id BIGINT;
    v_type_id     BIGINT;
    v_forfait_id  BIGINT;
    v_jour        TEXT;
BEGIN
    SELECT id INTO v_paroisse_id
    FROM paroisse
    WHERE nom ILIKE '%Maria Goretti%'
      AND status_del = FALSE
    ORDER BY id
    LIMIT 1;

    IF v_paroisse_id IS NULL THEN
        RAISE NOTICE 'Paroisse modèle Maria Goretti introuvable — catalogue non semé';
        RETURN;
    END IF;

    IF EXISTS (
        SELECT 1 FROM type_demande
        WHERE paroisse_id = v_paroisse_id AND status_del = FALSE
    ) THEN
        RAISE NOTICE 'Catalogue déjà présent sur la paroisse modèle — seed ignoré';
        RETURN;
    END IF;

    -- 1. Intention de messe (unique) : 3 natures
    INSERT INTO type_demande (
        public_id, paroisse_id, type_principal, libelle, description,
        delai_minimum_heures, is_active, status_del, created_at, updated_at
    ) VALUES (
        gen_random_uuid(), v_paroisse_id, 'EUCHARISTIE',
        'Intention de messe',
        'Messe d''intention pour un vivant ou un défunt',
        24, TRUE, FALSE, LOCALTIMESTAMP, LOCALTIMESTAMP
    )
    RETURNING id INTO v_type_id;

    FOREACH v_jour IN ARRAY ARRAY[
        'LUNDI', 'MARDI', 'MERCREDI', 'JEUDI', 'VENDREDI', 'SAMEDI', 'DIMANCHE'
    ]
    LOOP
        INSERT INTO type_demande_jour_autorise (type_demande_id, jour_semaine)
        VALUES (v_type_id, v_jour);
    END LOOP;

    INSERT INTO forfait_tarif (
        public_id, type_demande_id, code_forfait, nom_forfait, nature_forfait,
        libelle, montant_forfait, nombre_celebration, nombre_jour,
        heure_personnalise, is_active, status_del, created_at, updated_at
    ) VALUES (
        gen_random_uuid(), v_type_id, 'TPL-EUC-NOR-1',
        'Messe normale', 'NORMALE',
        'Célébration en semaine', 5000, 1, 1,
        FALSE, TRUE, FALSE, LOCALTIMESTAMP, LOCALTIMESTAMP
    )
    RETURNING id INTO v_forfait_id;

    FOREACH v_jour IN ARRAY ARRAY[
        'LUNDI', 'MARDI', 'MERCREDI', 'JEUDI', 'VENDREDI', 'SAMEDI'
    ]
    LOOP
        INSERT INTO forfait_tarif_jour_autorise (forfait_tarif_id, jour_semaine)
        VALUES (v_forfait_id, v_jour);
    END LOOP;

    INSERT INTO forfait_tarif (
        public_id, type_demande_id, code_forfait, nom_forfait, nature_forfait,
        libelle, montant_forfait, nombre_celebration, nombre_jour,
        heure_personnalise, is_active, status_del, created_at, updated_at
    ) VALUES (
        gen_random_uuid(), v_type_id, 'TPL-EUC-DOM-1',
        'Messe dominicale', 'DOMINICALE',
        'Célébration du dimanche', 10000, 1, 1,
        FALSE, TRUE, FALSE, LOCALTIMESTAMP, LOCALTIMESTAMP
    )
    RETURNING id INTO v_forfait_id;

    INSERT INTO forfait_tarif_jour_autorise (forfait_tarif_id, jour_semaine)
    VALUES (v_forfait_id, 'DIMANCHE');

    INSERT INTO forfait_tarif (
        public_id, type_demande_id, code_forfait, nom_forfait, nature_forfait,
        libelle, montant_forfait, nombre_celebration, nombre_jour,
        heure_personnalise, is_active, status_del, created_at, updated_at
    ) VALUES (
        gen_random_uuid(), v_type_id, 'TPL-EUC-SPE-1',
        'Messe spéciale', 'SPECIALE',
        'Soumise à validation paroissiale', 20000, 1, 1,
        TRUE, TRUE, FALSE, LOCALTIMESTAMP, LOCALTIMESTAMP
    )
    RETURNING id INTO v_forfait_id;

    FOREACH v_jour IN ARRAY ARRAY[
        'LUNDI', 'MARDI', 'MERCREDI', 'JEUDI', 'VENDREDI', 'SAMEDI', 'DIMANCHE'
    ]
    LOOP
        INSERT INTO forfait_tarif_jour_autorise (forfait_tarif_id, jour_semaine)
        VALUES (v_forfait_id, v_jour);
    END LOOP;

    -- 2. Triduum (3 célébrations)
    INSERT INTO type_demande (
        public_id, paroisse_id, type_principal, libelle, description,
        delai_minimum_heures, is_active, status_del, created_at, updated_at
    ) VALUES (
        gen_random_uuid(), v_paroisse_id, 'EUCHARISTIE',
        'Triduum',
        'Trois messes d''intention sur trois jours',
        48, TRUE, FALSE, LOCALTIMESTAMP, LOCALTIMESTAMP
    )
    RETURNING id INTO v_type_id;

    FOREACH v_jour IN ARRAY ARRAY[
        'LUNDI', 'MARDI', 'MERCREDI', 'JEUDI', 'VENDREDI', 'SAMEDI', 'DIMANCHE'
    ]
    LOOP
        INSERT INTO type_demande_jour_autorise (type_demande_id, jour_semaine)
        VALUES (v_type_id, v_jour);
    END LOOP;

    INSERT INTO forfait_tarif (
        public_id, type_demande_id, code_forfait, nom_forfait, nature_forfait,
        libelle, montant_forfait, nombre_celebration, nombre_jour,
        heure_personnalise, is_active, status_del, created_at, updated_at
    ) VALUES (
        gen_random_uuid(), v_type_id, 'TPL-EUC-NOR-3',
        'Triduum', 'NORMALE',
        'Trois célébrations', 15000, 3, 3,
        FALSE, TRUE, FALSE, LOCALTIMESTAMP, LOCALTIMESTAMP
    );

    -- 3. Neuvaine (9 célébrations)
    INSERT INTO type_demande (
        public_id, paroisse_id, type_principal, libelle, description,
        delai_minimum_heures, is_active, status_del, created_at, updated_at
    ) VALUES (
        gen_random_uuid(), v_paroisse_id, 'EUCHARISTIE',
        'Neuvaine',
        'Neuf messes d''intention',
        72, TRUE, FALSE, LOCALTIMESTAMP, LOCALTIMESTAMP
    )
    RETURNING id INTO v_type_id;

    FOREACH v_jour IN ARRAY ARRAY[
        'LUNDI', 'MARDI', 'MERCREDI', 'JEUDI', 'VENDREDI', 'SAMEDI', 'DIMANCHE'
    ]
    LOOP
        INSERT INTO type_demande_jour_autorise (type_demande_id, jour_semaine)
        VALUES (v_type_id, v_jour);
    END LOOP;

    INSERT INTO forfait_tarif (
        public_id, type_demande_id, code_forfait, nom_forfait, nature_forfait,
        libelle, montant_forfait, nombre_celebration, nombre_jour,
        heure_personnalise, is_active, status_del, created_at, updated_at
    ) VALUES (
        gen_random_uuid(), v_type_id, 'TPL-EUC-NOR-9',
        'Neuvaine', 'NORMALE',
        'Neuf célébrations', 40000, 9, 9,
        FALSE, TRUE, FALSE, LOCALTIMESTAMP, LOCALTIMESTAMP
    );

    -- 4. Trentaine (30 célébrations)
    INSERT INTO type_demande (
        public_id, paroisse_id, type_principal, libelle, description,
        delai_minimum_heures, is_active, status_del, created_at, updated_at
    ) VALUES (
        gen_random_uuid(), v_paroisse_id, 'EUCHARISTIE',
        'Trentaine',
        'Trente messes d''intention',
        72, TRUE, FALSE, LOCALTIMESTAMP, LOCALTIMESTAMP
    )
    RETURNING id INTO v_type_id;

    FOREACH v_jour IN ARRAY ARRAY[
        'LUNDI', 'MARDI', 'MERCREDI', 'JEUDI', 'VENDREDI', 'SAMEDI', 'DIMANCHE'
    ]
    LOOP
        INSERT INTO type_demande_jour_autorise (type_demande_id, jour_semaine)
        VALUES (v_type_id, v_jour);
    END LOOP;

    INSERT INTO forfait_tarif (
        public_id, type_demande_id, code_forfait, nom_forfait, nature_forfait,
        libelle, montant_forfait, nombre_celebration, nombre_jour,
        heure_personnalise, is_active, status_del, created_at, updated_at
    ) VALUES (
        gen_random_uuid(), v_type_id, 'TPL-EUC-NOR-30',
        'Trentaine', 'NORMALE',
        'Trente célébrations', 100000, 30, 30,
        FALSE, TRUE, FALSE, LOCALTIMESTAMP, LOCALTIMESTAMP
    );
END $$;
