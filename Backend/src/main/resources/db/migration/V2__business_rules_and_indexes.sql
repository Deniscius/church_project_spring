-- Règles métier que le modèle JPA ne sait pas exprimer, et index des chemins chauds.
--
-- L'unicité est presque toujours partielle : un enregistrement supprimé
-- logiquement (status_del = true) ne doit plus bloquer la recréation de son
-- équivalent actif.

-- ===============================
-- Unicité métier sur les enregistrements actifs
-- ===============================

-- L'authentification compare le username sans tenir compte de la casse.
CREATE UNIQUE INDEX uk_users_active_username_ci
    ON users (lower(btrim(username)))
    WHERE status_del = false;

CREATE UNIQUE INDEX uk_doyenne_active_nom
    ON doyenne (lower(btrim(nom)))
    WHERE status_del = false;

-- Deux paroisses actives peuvent porter le même nom dans des doyennés différents.
CREATE UNIQUE INDEX uk_paroisse_active_nom_doyenne
    ON paroisse (lower(btrim(nom)), doyenne_id)
    WHERE status_del = false;

CREATE UNIQUE INDEX uk_paroisse_access_active_user_paroisse
    ON paroisse_access (user_id, paroisse_id)
    WHERE status_del = false;

-- Un utilisateur local ne travaille que dans une seule paroisse à la fois.
CREATE UNIQUE INDEX uk_paroisse_access_one_active_per_user
    ON paroisse_access (user_id)
    WHERE active = true
      AND status_del = false;

-- Une seule grille tarifaire active par nature de célébration et type de demande.
CREATE UNIQUE INDEX uk_forfait_tarif_type_nature_active
    ON forfait_tarif (type_demande_id, nature_forfait)
    WHERE status_del = false;

-- ===============================
-- Invariants de cohérence
-- ===============================

-- Le périmètre global est réservé aux comptes de la plateforme : SUPER_ADMIN et
-- COMPTABLE n'ont aucun rattachement paroissial, les autres rôles en ont un.
ALTER TABLE users
    ADD CONSTRAINT ck_users_global_platform_role
        CHECK (is_global = (role IN ('SUPER_ADMIN', 'COMPTABLE')));

-- Délai de dépôt minimum exprimé en heures, plafonné à un an.
ALTER TABLE type_demande
    ADD CONSTRAINT ck_type_demande_delai_minimum_heures
        CHECK (delai_minimum_heures BETWEEN 0 AND 8760);

-- ===============================
-- Index des chemins chauds
-- ===============================

CREATE INDEX idx_demande_paroisse_statusdel
    ON demande (paroisse_id, status_del);

CREATE INDEX idx_demande_paroisse_statut_statusdel
    ON demande (paroisse_id, statut_demande, status_del);

CREATE INDEX idx_demande_date_celebration_statusdel
    ON demande_date (date_celebration, status_del);

CREATE INDEX idx_demande_date_demande_statusdel
    ON demande_date (demande_id, status_del);

CREATE INDEX idx_facture_demande_statusdel
    ON facture (demande_id, status_del);

CREATE INDEX idx_details_paiement_facture_statusdel
    ON details_paiement (facture_id, status_del);

CREATE INDEX idx_horaire_paroisse_statusdel
    ON horaire (paroisse_id, status_del);

CREATE INDEX idx_type_demande_paroisse_statusdel
    ON type_demande (paroisse_id, status_del);

CREATE INDEX idx_type_demande_jour_autorise_type
    ON type_demande_jour_autorise (type_demande_id);

CREATE INDEX idx_forfait_tarif_jour_autorise_forfait
    ON forfait_tarif_jour_autorise (forfait_tarif_id);

-- Rapprochement des notifications de l'agrégateur de paiement.
CREATE INDEX idx_details_paiement_id_transaction
    ON details_paiement (id_transaction)
    WHERE status_del = false
      AND id_transaction IS NOT NULL;

CREATE INDEX idx_users_email
    ON users (lower(email))
    WHERE email IS NOT NULL
      AND status_del = false;

CREATE INDEX idx_abonnement_paroisse
    ON paroisse_abonnement (paroisse_id)
    WHERE status_del = false;

CREATE INDEX idx_ecriture_paroisse
    ON ecriture_comptable (paroisse_id, created_at DESC)
    WHERE status_del = false;

CREATE INDEX idx_reversement_statut
    ON demande_reversement (statut)
    WHERE status_del = false;
