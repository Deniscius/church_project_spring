-- One latest change per demand: bounded by business data, not by number of edits.
-- The parish counter is locked until commit. A cursor never skips a concurrent commit.
CREATE TABLE desktop_sync_clock (
 parish_id bigint PRIMARY KEY REFERENCES paroisse(id) ON DELETE CASCADE,
 revision bigint NOT NULL DEFAULT 0,
 catalogue_revision bigint NOT NULL DEFAULT 0
);
CREATE TABLE desktop_demand_change (
 parish_id bigint NOT NULL REFERENCES paroisse(id) ON DELETE CASCADE,
 demand_public_id uuid NOT NULL,
 revision bigint NOT NULL,
 PRIMARY KEY(parish_id,demand_public_id)
);
CREATE UNIQUE INDEX desktop_demand_change_cursor ON desktop_demand_change(parish_id,revision);
CREATE TABLE desktop_global_clock(id integer PRIMARY KEY CHECK(id=1),revision bigint NOT NULL DEFAULT 0);
INSERT INTO desktop_global_clock(id) VALUES(1);
INSERT INTO desktop_sync_clock(parish_id) SELECT id FROM paroisse;

CREATE FUNCTION desktop_record_demand_change(p_parish bigint,p_demand uuid) RETURNS void LANGUAGE plpgsql AS $$
DECLARE version bigint;
BEGIN
 IF p_parish IS NULL OR p_demand IS NULL THEN RETURN; END IF;
 INSERT INTO desktop_sync_clock(parish_id,revision) VALUES(p_parish,1)
 ON CONFLICT(parish_id) DO UPDATE SET revision=desktop_sync_clock.revision+1 RETURNING revision INTO version;
 INSERT INTO desktop_demand_change(parish_id,demand_public_id,revision) VALUES(p_parish,p_demand,version)
 ON CONFLICT(parish_id,demand_public_id) DO UPDATE SET revision=EXCLUDED.revision;
END $$;

CREATE FUNCTION desktop_track_demand_change() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE row_data jsonb; demand_key bigint; parish_key bigint; public_key uuid;
BEGIN
 IF TG_OP='UPDATE' AND NEW IS NOT DISTINCT FROM OLD THEN RETURN NEW; END IF;
 IF TG_OP='DELETE' THEN row_data=to_jsonb(OLD); ELSE row_data=to_jsonb(NEW); END IF;
 IF TG_TABLE_NAME='demande' THEN
   parish_key=(row_data->>'paroisse_id')::bigint;
   public_key=(row_data->>'public_id')::uuid;
   IF TG_OP='UPDATE' AND OLD.paroisse_id IS DISTINCT FROM NEW.paroisse_id THEN
     -- Lock both clocks in a deterministic order for moves between parishes.
     INSERT INTO desktop_sync_clock(parish_id) SELECT p FROM unnest(ARRAY[OLD.paroisse_id,NEW.paroisse_id]) p WHERE p IS NOT NULL ORDER BY p ON CONFLICT DO NOTHING;
     PERFORM 1 FROM desktop_sync_clock WHERE parish_id IN (OLD.paroisse_id,NEW.paroisse_id) ORDER BY parish_id FOR UPDATE;
     PERFORM desktop_record_demand_change(OLD.paroisse_id,OLD.public_id);
   END IF;
 ELSE
   IF TG_TABLE_NAME='details_paiement' THEN
     SELECT demande_id INTO demand_key FROM facture WHERE id=(row_data->>'facture_id')::bigint;
   ELSE demand_key=(row_data->>'demande_id')::bigint; END IF;
   SELECT paroisse_id,public_id INTO parish_key,public_key FROM demande WHERE id=demand_key;
 END IF;
 PERFORM desktop_record_demand_change(parish_key,public_key);
 IF TG_OP='DELETE' THEN RETURN OLD; ELSE RETURN NEW; END IF;
END $$;
CREATE TRIGGER desktop_demande_change AFTER INSERT OR UPDATE OR DELETE ON demande FOR EACH ROW EXECUTE FUNCTION desktop_track_demand_change();
CREATE TRIGGER desktop_date_change AFTER INSERT OR UPDATE OR DELETE ON demande_date FOR EACH ROW EXECUTE FUNCTION desktop_track_demand_change();
CREATE TRIGGER desktop_invoice_change AFTER INSERT OR UPDATE OR DELETE ON facture FOR EACH ROW EXECUTE FUNCTION desktop_track_demand_change();
CREATE TRIGGER desktop_payment_change AFTER INSERT OR UPDATE OR DELETE ON details_paiement FOR EACH ROW EXECUTE FUNCTION desktop_track_demand_change();

CREATE FUNCTION desktop_track_catalogue_change() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE row_data jsonb; parish_key bigint; category bigint;
BEGIN
 IF TG_OP='UPDATE' AND NEW IS NOT DISTINCT FROM OLD THEN RETURN NEW; END IF;
 IF TG_OP='DELETE' THEN row_data=to_jsonb(OLD); ELSE row_data=to_jsonb(NEW); END IF;
 IF TG_ARGV[0]='global' THEN
   UPDATE desktop_global_clock SET revision=revision+1 WHERE id=1;
 ELSE
   CASE TG_ARGV[0]
   WHEN 'self' THEN
     IF TG_OP='DELETE' THEN RETURN OLD; END IF;
     parish_key=(row_data->>'id')::bigint;
   WHEN 'type' THEN SELECT paroisse_id INTO parish_key FROM type_demande WHERE id=(row_data->>'type_demande_id')::bigint;
   WHEN 'price' THEN SELECT d.paroisse_id INTO parish_key FROM forfait_tarif f JOIN type_demande d ON d.id=f.type_demande_id WHERE f.id=(row_data->>'forfait_tarif_id')::bigint;
   ELSE parish_key=(row_data->>'paroisse_id')::bigint;
   END CASE;
   category=CASE WHEN TG_ARGV[1]='other' THEN 0 ELSE 1 END;
   IF parish_key IS NOT NULL AND EXISTS(SELECT 1 FROM paroisse WHERE id=parish_key) THEN
     INSERT INTO desktop_sync_clock(parish_id,revision,catalogue_revision) VALUES(parish_key,1,category)
     ON CONFLICT(parish_id) DO UPDATE SET revision=desktop_sync_clock.revision+1,catalogue_revision=desktop_sync_clock.catalogue_revision+category;
   END IF;
 END IF;
 IF TG_OP='DELETE' THEN RETURN OLD; ELSE RETURN NEW; END IF;
END $$;
CREATE TRIGGER desktop_schedule_change AFTER INSERT OR UPDATE OR DELETE ON horaire FOR EACH ROW EXECUTE FUNCTION desktop_track_catalogue_change('direct');
CREATE TRIGGER desktop_type_change AFTER INSERT OR UPDATE OR DELETE ON type_demande FOR EACH ROW EXECUTE FUNCTION desktop_track_catalogue_change('direct');
CREATE TRIGGER desktop_price_change AFTER INSERT OR UPDATE OR DELETE ON forfait_tarif FOR EACH ROW EXECUTE FUNCTION desktop_track_catalogue_change('type');
CREATE TRIGGER desktop_type_day_change AFTER INSERT OR UPDATE OR DELETE ON type_demande_jour_autorise FOR EACH ROW EXECUTE FUNCTION desktop_track_catalogue_change('type');
CREATE TRIGGER desktop_price_day_change AFTER INSERT OR UPDATE OR DELETE ON forfait_tarif_jour_autorise FOR EACH ROW EXECUTE FUNCTION desktop_track_catalogue_change('price');
CREATE TRIGGER desktop_parish_change AFTER INSERT OR UPDATE OR DELETE ON paroisse FOR EACH ROW EXECUTE FUNCTION desktop_track_catalogue_change('self');
CREATE TRIGGER desktop_access_change AFTER INSERT OR UPDATE OR DELETE ON paroisse_access FOR EACH ROW EXECUTE FUNCTION desktop_track_catalogue_change('direct');
CREATE TRIGGER desktop_choir_change AFTER INSERT OR UPDATE OR DELETE ON chorale FOR EACH ROW EXECUTE FUNCTION desktop_track_catalogue_change('direct');
CREATE TRIGGER desktop_account_change AFTER INSERT OR UPDATE OR DELETE ON compte_paroisse FOR EACH ROW EXECUTE FUNCTION desktop_track_catalogue_change('direct','other');
CREATE TRIGGER desktop_transfer_change AFTER INSERT OR UPDATE OR DELETE ON demande_reversement FOR EACH ROW EXECUTE FUNCTION desktop_track_catalogue_change('direct','other');
CREATE TRIGGER desktop_user_change AFTER INSERT OR UPDATE OR DELETE ON users FOR EACH ROW EXECUTE FUNCTION desktop_track_catalogue_change('global');
CREATE TRIGGER desktop_payment_type_change AFTER INSERT OR UPDATE OR DELETE ON type_paiement FOR EACH ROW EXECUTE FUNCTION desktop_track_catalogue_change('global');
