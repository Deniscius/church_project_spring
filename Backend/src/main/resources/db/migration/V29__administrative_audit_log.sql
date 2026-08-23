-- Journal append-only des décisions administratives sensibles.
CREATE TABLE administrative_audit_event (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL UNIQUE,
    action VARCHAR(80) NOT NULL,
    target_type VARCHAR(60) NOT NULL,
    target_public_id UUID NOT NULL,
    paroisse_public_id UUID,
    actor_public_id UUID,
    actor_username VARCHAR(100) NOT NULL,
    actor_name VARCHAR(160) NOT NULL,
    details VARCHAR(500),
    occurred_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_admin_audit_occurred_at
    ON administrative_audit_event (occurred_at DESC);

CREATE INDEX idx_admin_audit_target
    ON administrative_audit_event (target_type, target_public_id, occurred_at DESC);

CREATE INDEX idx_admin_audit_paroisse
    ON administrative_audit_event (paroisse_public_id, occurred_at DESC)
    WHERE paroisse_public_id IS NOT NULL;

COMMENT ON TABLE administrative_audit_event IS
    'Journal append-only des décisions administratives ; ne doit contenir aucun secret.';
