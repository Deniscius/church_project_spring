-- Same transaction as the business mutation; retained so retries never duplicate it.
CREATE TABLE desktop_operation (
    owner_id UUID NOT NULL,
    operation_id UUID NOT NULL,
    kind VARCHAR(32) NOT NULL,
    payload_hash VARCHAR(64) NOT NULL,
    parish_id UUID NOT NULL,
    response TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (owner_id, operation_id)
);
