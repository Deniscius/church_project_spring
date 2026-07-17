CREATE TABLE IF NOT EXISTS forfait_tarif_jour_autorise (
    forfait_tarif_id BIGINT NOT NULL REFERENCES forfait_tarif (id) ON DELETE CASCADE,
    jour_semaine       VARCHAR(20) NOT NULL,
    PRIMARY KEY (forfait_tarif_id, jour_semaine)
);

CREATE INDEX IF NOT EXISTS idx_forfait_tarif_jour_autorise_forfait
    ON forfait_tarif_jour_autorise (forfait_tarif_id);

ALTER TABLE forfait_tarif
    DROP COLUMN IF EXISTS jours_autorise;
