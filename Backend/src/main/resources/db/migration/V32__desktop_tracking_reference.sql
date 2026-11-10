-- References handed out on a disconnected workstation remain valid after upload.
CREATE TABLE desktop_tracking_reference (
    reference VARCHAR(80) PRIMARY KEY,
    demande_public_id UUID NOT NULL REFERENCES demande(public_id),
    owner_id UUID NOT NULL,
    operation_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_desktop_tracking_demande ON desktop_tracking_reference(demande_public_id);
