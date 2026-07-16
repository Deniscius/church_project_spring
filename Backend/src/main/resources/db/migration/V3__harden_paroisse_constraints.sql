-- Aligne le schéma historique sur le contrat de l'entité Paroisse.
ALTER TABLE paroisse ALTER COLUMN nom SET NOT NULL;
ALTER TABLE paroisse ALTER COLUMN adresse SET NOT NULL;
ALTER TABLE paroisse ALTER COLUMN localite_id SET NOT NULL;

UPDATE paroisse SET is_active = true WHERE is_active IS NULL;
ALTER TABLE paroisse ALTER COLUMN is_active SET DEFAULT true;
ALTER TABLE paroisse ALTER COLUMN is_active SET NOT NULL;

-- Deux paroisses actives peuvent porter le même nom seulement si elles se
-- trouvent dans des localités différentes.
CREATE UNIQUE INDEX IF NOT EXISTS uk_paroisse_active_nom_localite
    ON paroisse (lower(btrim(nom)), localite_id)
    WHERE status_del = false;
