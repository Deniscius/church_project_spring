-- Les champs sont obligatoires dans le contrat API et dans l'entité.
-- La migration échoue volontairement si des lignes historiques invalides
-- existent afin qu'elles soient corrigées sans perte silencieuse de données.
ALTER TABLE localite ALTER COLUMN ville SET NOT NULL;
ALTER TABLE localite ALTER COLUMN quartier SET NOT NULL;

-- Unicité métier insensible à la casse pour les localités actives.
-- Une localité supprimée logiquement peut être recréée ultérieurement.
CREATE UNIQUE INDEX IF NOT EXISTS uk_localite_active_ville_quartier
    ON localite (lower(btrim(ville)), lower(btrim(quartier)))
    WHERE status_del = false;
