-- Horaires ponctuels (date précise) et option « messe unique sur la paroisse ».
-- Les créneaux hebdomadaires restent date_specifique IS NULL.

ALTER TABLE horaire
    ADD COLUMN IF NOT EXISTS date_specifique DATE,
    ADD COLUMN IF NOT EXISTS unique_sur_paroisse BOOLEAN NOT NULL DEFAULT FALSE;

-- Une messe unique au plus par paroisse et par date (parmi les lignes actives).
CREATE UNIQUE INDEX IF NOT EXISTS uk_horaire_unique_sur_paroisse_date
    ON horaire (paroisse_id, date_specifique)
    WHERE status_del = FALSE
      AND unique_sur_paroisse = TRUE
      AND date_specifique IS NOT NULL;

-- Pas de doublon d'heure pour une même date ponctuelle.
CREATE UNIQUE INDEX IF NOT EXISTS uk_horaire_paroisse_date_heure
    ON horaire (paroisse_id, date_specifique, heure_celebration)
    WHERE status_del = FALSE
      AND date_specifique IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_horaire_paroisse_date_specifique
    ON horaire (paroisse_id, date_specifique)
    WHERE status_del = FALSE AND date_specifique IS NOT NULL;
