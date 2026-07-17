CREATE TABLE IF NOT EXISTS type_demande_jour_autorise (
    type_demande_id BIGINT NOT NULL REFERENCES type_demande (id) ON DELETE CASCADE,
    jour_semaine      VARCHAR(20) NOT NULL,
    PRIMARY KEY (type_demande_id, jour_semaine)
);

CREATE INDEX IF NOT EXISTS idx_type_demande_jour_autorise_type
    ON type_demande_jour_autorise (type_demande_id);
