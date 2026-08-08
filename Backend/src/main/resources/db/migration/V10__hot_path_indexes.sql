-- Index ciblés pour les chemins chauds (accueil, feuille, impayés, abonnements).

-- Balayage des abonnements ACTIF arrivés à échéance
CREATE INDEX IF NOT EXISTS idx_abonnement_statut_fin
    ON paroisse_abonnement (statut, fin_at)
    WHERE status_del = false;

-- Suspension après période de grâce
CREATE INDEX IF NOT EXISTS idx_paroisse_active_subscription_expires
    ON paroisse (subscription_expires_at)
    WHERE status_del = false AND is_active = true;

-- Listes paroisse filtrées par statut de paiement
CREATE INDEX IF NOT EXISTS idx_demande_paroisse_paiement_statusdel
    ON demande (paroisse_id, statut_paiement, status_del);

-- Feuille de célébration / annulation impayés (1ère date)
CREATE INDEX IF NOT EXISTS idx_demande_date_celebration_ordre_statusdel
    ON demande_date (date_celebration, ordre, status_del)
    WHERE status_del = false;

-- Horaires publics / admin par paroisse active
CREATE INDEX IF NOT EXISTS idx_horaire_paroisse_active_statusdel
    ON horaire (paroisse_id, is_active, status_del);

-- Journal de caisse (filtre statut paiement)
CREATE INDEX IF NOT EXISTS idx_details_paiement_statut_statusdel
    ON details_paiement (statut_paiement, status_del)
    WHERE status_del = false;
