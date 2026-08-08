-- Soft delete demandes : conserver une trace (qui / quand) pour l'audit paroissial.
ALTER TABLE demande
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS deleted_by_nom VARCHAR(150);

COMMENT ON COLUMN demande.deleted_at IS 'Horodatage du soft delete (status_del = true)';
COMMENT ON COLUMN demande.deleted_by_nom IS 'Nom de l''utilisateur ayant soft-supprimé la demande';
