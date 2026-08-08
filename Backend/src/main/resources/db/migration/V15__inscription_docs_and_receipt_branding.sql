-- Documents officiels exigés à l'inscription paroisse (scans).
ALTER TABLE paroisse_inscription
    ADD COLUMN IF NOT EXISTS mandat_cure_path VARCHAR(500),
    ADD COLUMN IF NOT EXISTS admin_cni_path VARCHAR(500);

-- Logo paroissial pour personnaliser les reçus PDF (après activation).
ALTER TABLE paroisse
    ADD COLUMN IF NOT EXISTS logo_path VARCHAR(500);
