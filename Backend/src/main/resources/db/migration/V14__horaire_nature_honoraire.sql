-- Honoraire applicable aux célébrations à date précise (événements solennels).
-- null = créneau hebdomadaire, ou date précise sans forçage (dérivé côté métier).
ALTER TABLE horaire
    ADD COLUMN IF NOT EXISTS nature_honoraire VARCHAR(20);

COMMENT ON COLUMN horaire.nature_honoraire IS
    'NORMALE | DOMINICALE | SPECIALE — honoraire imposé pour une date précise (solennité).';
