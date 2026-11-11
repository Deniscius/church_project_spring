-- Directory for a future tenant move. This release supports the shared database only.
-- Deliberately no hash/modulo assignment: adding a database must never move a tenant implicitly.
CREATE TABLE parish_storage_placement (
    parish_id UUID PRIMARY KEY REFERENCES paroisse(public_id),
    shard_key VARCHAR(64) NOT NULL DEFAULT 'primary',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT parish_storage_shared_only CHECK (shard_key = 'primary')
);
INSERT INTO parish_storage_placement(parish_id) SELECT public_id FROM paroisse;

-- Keep registration atomic with parish creation, including imports and catalog provisioning.
CREATE FUNCTION register_parish_storage() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    INSERT INTO parish_storage_placement(parish_id) VALUES (NEW.public_id);
    RETURN NEW;
END;
$$;
CREATE TRIGGER parish_storage_registration AFTER INSERT ON paroisse
    FOR EACH ROW EXECUTE FUNCTION register_parish_storage();

CREATE INDEX idx_desktop_operation_parish_created ON desktop_operation(parish_id, created_at);
