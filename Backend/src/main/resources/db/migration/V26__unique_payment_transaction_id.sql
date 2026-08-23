-- Une transaction fournisseur ne doit correspondre qu'à un seul paiement,
-- même si la ligne métier est ensuite soft-supprimée.
CREATE UNIQUE INDEX IF NOT EXISTS uq_details_paiement_id_transaction
    ON details_paiement (id_transaction)
    WHERE id_transaction IS NOT NULL;
