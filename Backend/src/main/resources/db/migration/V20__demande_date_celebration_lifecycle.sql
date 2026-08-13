-- Cycle de vie célébration : confirmation manuelle / auto après l'heure + rappels J-1 / H-2.
ALTER TABLE demande_date
    ADD COLUMN IF NOT EXISTS celebre BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS celebre_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS last_reminder_j1_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS last_reminder_h2_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_demande_date_celebre_pending
    ON demande_date (date_celebration, celebre)
    WHERE status_del = FALSE AND celebre = FALSE;
