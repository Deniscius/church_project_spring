-- Transforme le référentiel géographique historique en structure ecclésiastique.
-- Les données existantes sont conservées : "Ville - Quartier" devient le nom
-- initial du doyenné et l'ancien quartier est conservé comme description.
DROP INDEX IF EXISTS uk_paroisse_active_nom_localite;
DROP INDEX IF EXISTS uk_localite_active_ville_quartier;

ALTER TABLE localite RENAME TO doyenne;
ALTER TABLE doyenne RENAME COLUMN ville TO nom;
ALTER TABLE doyenne RENAME COLUMN quartier TO description;
ALTER TABLE paroisse RENAME COLUMN localite_id TO doyenne_id;

UPDATE doyenne
SET nom = concat_ws(' - ', nullif(btrim(nom), ''), nullif(btrim(description), '')),
    description = nullif(btrim(description), '');

ALTER TABLE doyenne ALTER COLUMN nom TYPE varchar(400);
ALTER TABLE doyenne ALTER COLUMN nom SET NOT NULL;
ALTER TABLE doyenne ALTER COLUMN description TYPE varchar(500);
ALTER TABLE doyenne ALTER COLUMN description DROP NOT NULL;
ALTER TABLE paroisse ALTER COLUMN doyenne_id SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_doyenne_active_nom
    ON doyenne (lower(btrim(nom)))
    WHERE status_del = false;

CREATE UNIQUE INDEX IF NOT EXISTS uk_paroisse_active_nom_doyenne
    ON paroisse (lower(btrim(nom)), doyenne_id)
    WHERE status_del = false;
