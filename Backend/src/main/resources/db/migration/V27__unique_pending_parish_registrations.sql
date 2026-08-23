-- Prevent concurrent public submissions from creating duplicate pending dossiers.
DO $$
DECLARE
    duplicates text;
BEGIN
    SELECT string_agg(
               format('%s / %s (%s dossiers)', normalized_name, doyenne_public_id, total),
               ', '
           )
    INTO duplicates
    FROM (
        SELECT lower(btrim(nom_paroisse)) AS normalized_name,
               doyenne_public_id,
               count(*) AS total
        FROM paroisse_inscription
        WHERE status_del = false
          AND statut = 'SOUMISE'
        GROUP BY lower(btrim(nom_paroisse)), doyenne_public_id
        HAVING count(*) > 1
    ) duplicate_rows;

    IF duplicates IS NOT NULL THEN
        RAISE EXCEPTION
            'V27 blocked: duplicate pending parish registrations must be resolved first: %',
            duplicates;
    END IF;
END
$$;

DO $$
DECLARE
    duplicates text;
BEGIN
    SELECT string_agg(
               format('%s (%s dossiers)', normalized_username, total),
               ', '
           )
    INTO duplicates
    FROM (
        SELECT lower(btrim(admin_username)) AS normalized_username,
               count(*) AS total
        FROM paroisse_inscription
        WHERE status_del = false
          AND statut = 'SOUMISE'
        GROUP BY lower(btrim(admin_username))
        HAVING count(*) > 1
    ) duplicate_rows;

    IF duplicates IS NOT NULL THEN
        RAISE EXCEPTION
            'V27 blocked: duplicate pending registration usernames must be resolved first: %',
            duplicates;
    END IF;
END
$$;

CREATE UNIQUE INDEX uq_paroisse_inscription_pending_parish
    ON paroisse_inscription (lower(btrim(nom_paroisse)), doyenne_public_id)
    WHERE status_del = false AND statut = 'SOUMISE';

CREATE UNIQUE INDEX uq_paroisse_inscription_pending_username
    ON paroisse_inscription (lower(btrim(admin_username)))
    WHERE status_del = false AND statut = 'SOUMISE';
