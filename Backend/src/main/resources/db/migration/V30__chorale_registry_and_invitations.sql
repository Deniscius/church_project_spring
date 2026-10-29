CREATE TABLE chorale (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL UNIQUE,
    nom VARCHAR(150) NOT NULL,
    description VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    paroisse_id BIGINT NOT NULL REFERENCES paroisse(id),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status_del BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE UNIQUE INDEX uq_chorale_nom_paroisse_active
    ON chorale (paroisse_id, lower(nom)) WHERE status_del = FALSE;
CREATE INDEX idx_chorale_paroisse ON chorale (paroisse_id, is_active) WHERE status_del = FALSE;

CREATE TABLE chorale_invitation (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL UNIQUE,
    chorale_id BIGINT NOT NULL REFERENCES chorale(id),
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    used_at TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status_del BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_chorale_invitation_lookup
    ON chorale_invitation (token_hash, expires_at) WHERE status_del = FALSE AND used_at IS NULL;

COMMENT ON COLUMN chorale_invitation.token_hash IS
    'Empreinte SHA-256 du secret remis une seule fois ; le secret brut ne doit jamais être stocké.';
