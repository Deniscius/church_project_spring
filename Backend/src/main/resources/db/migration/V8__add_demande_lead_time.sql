ALTER TABLE type_demande
    ADD COLUMN IF NOT EXISTS delai_minimum_heures INTEGER NOT NULL DEFAULT 24;

ALTER TABLE type_demande
    DROP CONSTRAINT IF EXISTS ck_type_demande_delai_minimum_heures;

ALTER TABLE type_demande
    ADD CONSTRAINT ck_type_demande_delai_minimum_heures
        CHECK (delai_minimum_heures BETWEEN 0 AND 8760);
