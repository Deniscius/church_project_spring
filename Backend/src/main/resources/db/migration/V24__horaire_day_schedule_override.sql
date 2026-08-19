-- Permet de remplacer la grille hebdomadaire pour une date précise avec
-- plusieurs créneaux personnalisés, sans détourner l'option « messe unique ».

ALTER TABLE horaire
    ADD COLUMN IF NOT EXISTS programme_jour_override BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_horaire_paroisse_date_override
    ON horaire (paroisse_id, date_specifique, programme_jour_override)
    WHERE status_del = FALSE
      AND is_active = TRUE
      AND date_specifique IS NOT NULL;
