-- Horaire / heure par date de célébration (triduum, neuvaine, trentaine).
ALTER TABLE demande_date
    ADD COLUMN IF NOT EXISTS horaire_id BIGINT,
    ADD COLUMN IF NOT EXISTS heure_personnalisee TIME;

ALTER TABLE demande_date
    DROP CONSTRAINT IF EXISTS fk_demande_date_horaire;

ALTER TABLE demande_date
    ADD CONSTRAINT fk_demande_date_horaire
        FOREIGN KEY (horaire_id) REFERENCES horaire (id);

CREATE INDEX IF NOT EXISTS idx_demande_date_horaire
    ON demande_date (horaire_id)
    WHERE status_del = FALSE AND horaire_id IS NOT NULL;
