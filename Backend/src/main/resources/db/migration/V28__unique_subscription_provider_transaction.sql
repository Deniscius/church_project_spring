-- Garantit l'idempotence des notifications de paiement d'abonnement.
-- Le diagnostic explicite évite qu'un historique incohérent soit masqué lors
-- du déploiement de la contrainte.

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM paroisse_abonnement
        WHERE status_del = false
          AND id_transaction IS NOT NULL
        GROUP BY btrim(id_transaction)
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION
            'Doublons id_transaction détectés dans paroisse_abonnement : corrigez les données avant V28';
    END IF;
END
$$;

CREATE UNIQUE INDEX uk_paroisse_abonnement_active_transaction
    ON paroisse_abonnement (btrim(id_transaction))
    WHERE status_del = false
      AND id_transaction IS NOT NULL;
