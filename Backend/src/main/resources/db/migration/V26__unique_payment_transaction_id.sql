-- Une transaction fournisseur ne doit correspondre qu'à un seul paiement,
-- même si la ligne métier est ensuite soft-supprimée.

-- Les anciennes chaînes vides ne représentent aucune transaction réelle.
UPDATE details_paiement
SET id_transaction = NULL
WHERE id_transaction IS NOT NULL
  AND btrim(id_transaction) = '';

-- Un doublon financier ne peut pas être corrigé automatiquement sans risquer
-- d'associer un paiement au mauvais dossier. La migration s'arrête donc avec
-- un diagnostic explicite avant la création de l'index.
DO $$
DECLARE
    duplicated_ids TEXT;
BEGIN
    SELECT string_agg(quote_literal(id_transaction), ', ')
    INTO duplicated_ids
    FROM (
        SELECT id_transaction
        FROM details_paiement
        WHERE id_transaction IS NOT NULL
        GROUP BY id_transaction
        HAVING count(*) > 1
        ORDER BY id_transaction
        LIMIT 10
    ) duplicates;

    IF duplicated_ids IS NOT NULL THEN
        RAISE EXCEPTION
            'Transactions fournisseur dupliquées dans details_paiement : %. Corrigez ces données avant de relancer Flyway.',
            duplicated_ids;
    END IF;
END
$$;

CREATE UNIQUE INDEX IF NOT EXISTS uq_details_paiement_id_transaction
    ON details_paiement (id_transaction)
    WHERE id_transaction IS NOT NULL;
