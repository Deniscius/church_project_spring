-- Index montée en charge : listes admin / tableaux de bord par paroisse + date.

CREATE INDEX IF NOT EXISTS idx_demande_paroisse_created_at
    ON demande (paroisse_id, created_at DESC)
    WHERE status_del = false;

CREATE INDEX IF NOT EXISTS idx_demande_paroisse_statut_created
    ON demande (paroisse_id, statut_demande, created_at DESC)
    WHERE status_del = false;
