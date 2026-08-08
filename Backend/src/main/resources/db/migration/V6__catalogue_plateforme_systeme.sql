-- Catalogue plateforme : détaché de toute paroisse réelle (ex. Maria Goretti).
--
-- Les tables métier exigent encore un paroisse_id NOT NULL : on utilise une
-- paroisse technique is_system = TRUE, invisible de l'annuaire et des listes
-- clients. Le comptable édite ce catalogue ; il est cloné à chaque activation.

ALTER TABLE paroisse
    ADD COLUMN IF NOT EXISTS is_system BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN paroisse.is_system IS
    'TRUE = support technique du catalogue plateforme (pas une paroisse cliente)';

-- ===============================
-- Paroisse technique plateforme
-- ===============================

INSERT INTO paroisse (
    public_id, nom, adresse, email, telephone,
    is_active, statut_tenant, is_system, doyenne_id,
    status_del, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    'Catalogue plateforme',
    'Configuration système — non visible',
    NULL,
    NULL,
    FALSE,
    'PROSPECT',
    TRUE,
    d.id,
    FALSE,
    LOCALTIMESTAMP,
    LOCALTIMESTAMP
FROM doyenne d
WHERE d.status_del = FALSE
  AND NOT EXISTS (
      SELECT 1 FROM paroisse p WHERE p.is_system = TRUE AND p.status_del = FALSE
  )
ORDER BY d.rang NULLS LAST, d.nom
LIMIT 1;

-- ===============================
-- Initialiser le catalogue système
-- ===============================

DO $$
DECLARE
    v_system_id      BIGINT;
    v_source_id      BIGINT;
    v_src_type       RECORD;
    v_new_type_id    BIGINT;
    v_src_forfait    RECORD;
    v_new_forfait_id BIGINT;
    v_code           TEXT;
BEGIN
    SELECT id INTO v_system_id
    FROM paroisse
    WHERE is_system = TRUE AND status_del = FALSE
    ORDER BY id
    LIMIT 1;

    IF v_system_id IS NULL THEN
        RAISE NOTICE 'Paroisse système introuvable — catalogue plateforme non initialisé';
        RETURN;
    END IF;

    IF EXISTS (
        SELECT 1 FROM type_demande
        WHERE paroisse_id = v_system_id AND status_del = FALSE
    ) THEN
        RAISE NOTICE 'Catalogue plateforme déjà présent — initialisation ignorée';
        RETURN;
    END IF;

    -- Source historique éventuelle (exemple Maria Goretti), sinon première paroisse avec types.
    SELECT id INTO v_source_id
    FROM paroisse
    WHERE status_del = FALSE
      AND is_system = FALSE
      AND nom ILIKE '%Maria Goretti%'
    ORDER BY id
    LIMIT 1;

    IF v_source_id IS NULL THEN
        SELECT p.id INTO v_source_id
        FROM paroisse p
        WHERE p.status_del = FALSE
          AND p.is_system = FALSE
          AND EXISTS (
              SELECT 1 FROM type_demande td
              WHERE td.paroisse_id = p.id AND td.status_del = FALSE
          )
        ORDER BY p.id
        LIMIT 1;
    END IF;

    IF v_source_id IS NOT NULL THEN
        INSERT INTO horaire (
            public_id, paroisse_id, jour_semaine, heure_celebration, libelle,
            is_active, status_del, created_at, updated_at
        )
        SELECT gen_random_uuid(), v_system_id, h.jour_semaine, h.heure_celebration, h.libelle,
               COALESCE(h.is_active, TRUE), FALSE, LOCALTIMESTAMP, LOCALTIMESTAMP
        FROM horaire h
        WHERE h.paroisse_id = v_source_id
          AND h.status_del = FALSE;

        FOR v_src_type IN
            SELECT * FROM type_demande
            WHERE paroisse_id = v_source_id AND status_del = FALSE
            ORDER BY id
        LOOP
            INSERT INTO type_demande (
                public_id, paroisse_id, type_principal, libelle, description,
                delai_minimum_heures, is_active, status_del, created_at, updated_at
            ) VALUES (
                gen_random_uuid(), v_system_id, v_src_type.type_principal, v_src_type.libelle,
                v_src_type.description, v_src_type.delai_minimum_heures,
                COALESCE(v_src_type.is_active, TRUE), FALSE, LOCALTIMESTAMP, LOCALTIMESTAMP
            )
            RETURNING id INTO v_new_type_id;

            INSERT INTO type_demande_jour_autorise (type_demande_id, jour_semaine)
            SELECT v_new_type_id, j.jour_semaine
            FROM type_demande_jour_autorise j
            WHERE j.type_demande_id = v_src_type.id;

            FOR v_src_forfait IN
                SELECT * FROM forfait_tarif
                WHERE type_demande_id = v_src_type.id AND status_del = FALSE
                ORDER BY id
            LOOP
                v_code := 'PLT-' || v_src_forfait.id::text || '-' || substr(md5(random()::text), 1, 8);
                INSERT INTO forfait_tarif (
                    public_id, type_demande_id, code_forfait, nom_forfait, nature_forfait,
                    libelle, montant_forfait, nombre_celebration, nombre_jour,
                    heure_personnalise, is_active, status_del, created_at, updated_at
                ) VALUES (
                    gen_random_uuid(),
                    v_new_type_id,
                    v_code,
                    v_src_forfait.nom_forfait,
                    v_src_forfait.nature_forfait,
                    v_src_forfait.libelle,
                    v_src_forfait.montant_forfait,
                    v_src_forfait.nombre_celebration,
                    v_src_forfait.nombre_jour,
                    COALESCE(v_src_forfait.heure_personnalise, FALSE),
                    COALESCE(v_src_forfait.is_active, TRUE),
                    FALSE,
                    LOCALTIMESTAMP,
                    LOCALTIMESTAMP
                )
                RETURNING id INTO v_new_forfait_id;

                INSERT INTO forfait_tarif_jour_autorise (forfait_tarif_id, jour_semaine)
                SELECT v_new_forfait_id, j.jour_semaine
                FROM forfait_tarif_jour_autorise j
                WHERE j.forfait_tarif_id = v_src_forfait.id;
            END LOOP;
        END LOOP;

        RAISE NOTICE 'Catalogue plateforme initialisé depuis paroisse id=%', v_source_id;
        RETURN;
    END IF;

    -- Aucune source : grille par défaut minimale sur le support système.
    INSERT INTO horaire (
        public_id, paroisse_id, jour_semaine, heure_celebration, libelle,
        is_active, status_del, created_at, updated_at
    )
    SELECT gen_random_uuid(), v_system_id, src.jour, src.heure::time, src.libelle,
           TRUE, FALSE, LOCALTIMESTAMP, LOCALTIMESTAMP
    FROM (VALUES
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
    ) AS src(jour, heure, libelle);

    INSERT INTO type_demande (
        public_id, paroisse_id, type_principal, libelle, description,
        delai_minimum_heures, is_active, status_del, created_at, updated_at
    ) VALUES (
        gen_random_uuid(), v_system_id, 'EUCHARISTIE',
        'Intention de messe',
        'Messe d''intention pour un vivant ou un défunt',
        24, TRUE, FALSE, LOCALTIMESTAMP, LOCALTIMESTAMP
    )
    RETURNING id INTO v_new_type_id;

    INSERT INTO type_demande_jour_autorise (type_demande_id, jour_semaine)
    SELECT v_new_type_id, j FROM (VALUES
        ('LUNDI'), ('MARDI'), ('MERCREDI'), ('JEUDI'), ('VENDREDI'), ('SAMEDI'), ('DIMANCHE')
    ) AS d(j);

    INSERT INTO forfait_tarif (
        public_id, type_demande_id, code_forfait, nom_forfait, nature_forfait,
        libelle, montant_forfait, nombre_celebration, nombre_jour,
        heure_personnalise, is_active, status_del, created_at, updated_at
    ) VALUES
        (gen_random_uuid(), v_new_type_id, 'PLT-EUC-NOR-1', 'Messe normale', 'NORMALE',
         'Célébration en semaine', 5000, 1, 1, FALSE, TRUE, FALSE, LOCALTIMESTAMP, LOCALTIMESTAMP),
        (gen_random_uuid(), v_new_type_id, 'PLT-EUC-DOM-1', 'Messe dominicale', 'DOMINICALE',
         'Célébration du dimanche', 10000, 1, 1, FALSE, TRUE, FALSE, LOCALTIMESTAMP, LOCALTIMESTAMP),
        (gen_random_uuid(), v_new_type_id, 'PLT-EUC-SPE-1', 'Messe spéciale', 'SPECIALE',
         'Soumise à validation paroissiale', 20000, 1, 1, TRUE, TRUE, FALSE, LOCALTIMESTAMP, LOCALTIMESTAMP);

    INSERT INTO type_demande (
        public_id, paroisse_id, type_principal, libelle, description,
        delai_minimum_heures, is_active, status_del, created_at, updated_at
    ) VALUES
        (gen_random_uuid(), v_system_id, 'EUCHARISTIE', 'Triduum',
         'Trois messes d''intention sur trois jours', 48, TRUE, FALSE, LOCALTIMESTAMP, LOCALTIMESTAMP),
        (gen_random_uuid(), v_system_id, 'EUCHARISTIE', 'Neuvaine',
         'Neuf messes d''intention', 72, TRUE, FALSE, LOCALTIMESTAMP, LOCALTIMESTAMP),
        (gen_random_uuid(), v_system_id, 'EUCHARISTIE', 'Trentaine',
         'Trente messes d''intention', 72, TRUE, FALSE, LOCALTIMESTAMP, LOCALTIMESTAMP);

    INSERT INTO forfait_tarif (
        public_id, type_demande_id, code_forfait, nom_forfait, nature_forfait,
        libelle, montant_forfait, nombre_celebration, nombre_jour,
        heure_personnalise, is_active, status_del, created_at, updated_at
    )
    SELECT gen_random_uuid(), td.id,
           CASE td.libelle
               WHEN 'Triduum' THEN 'PLT-EUC-NOR-3'
               WHEN 'Neuvaine' THEN 'PLT-EUC-NOR-9'
               ELSE 'PLT-EUC-NOR-30'
           END,
           td.libelle, 'NORMALE',
           CASE td.libelle
               WHEN 'Triduum' THEN 'Trois célébrations'
               WHEN 'Neuvaine' THEN 'Neuf célébrations'
               ELSE 'Trente célébrations'
           END,
           CASE td.libelle
               WHEN 'Triduum' THEN 15000
               WHEN 'Neuvaine' THEN 40000
               ELSE 100000
           END,
           CASE td.libelle
               WHEN 'Triduum' THEN 3
               WHEN 'Neuvaine' THEN 9
               ELSE 30
           END,
           CASE td.libelle
               WHEN 'Triduum' THEN 3
               WHEN 'Neuvaine' THEN 9
               ELSE 30
           END,
           FALSE, TRUE, FALSE, LOCALTIMESTAMP, LOCALTIMESTAMP
    FROM type_demande td
    WHERE td.paroisse_id = v_system_id
      AND td.libelle IN ('Triduum', 'Neuvaine', 'Trentaine')
      AND td.status_del = FALSE;

    RAISE NOTICE 'Catalogue plateforme initialisé avec la grille par défaut';
END $$;
