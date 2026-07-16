-- La paroisse est le tenant fonctionnel. Son appartenance est portée par les
-- relations paroisse_id ou par une relation métier vers demande/type_demande.
-- Les anciennes colonnes tenant_id provenaient de BaseEntity, n'étaient pas
-- alimentées de façon fiable et empêchaient notamment la création du SUPER_ADMIN.

ALTER TABLE IF EXISTS demande DROP COLUMN IF EXISTS tenant_id;
ALTER TABLE IF EXISTS demande_date DROP COLUMN IF EXISTS tenant_id;
ALTER TABLE IF EXISTS details_paiement DROP COLUMN IF EXISTS tenant_id;
ALTER TABLE IF EXISTS facture DROP COLUMN IF EXISTS tenant_id;
ALTER TABLE IF EXISTS forfait_tarif DROP COLUMN IF EXISTS tenant_id;
ALTER TABLE IF EXISTS horaire DROP COLUMN IF EXISTS tenant_id;
ALTER TABLE IF EXISTS localite DROP COLUMN IF EXISTS tenant_id;
ALTER TABLE IF EXISTS paroisse DROP COLUMN IF EXISTS tenant_id;
ALTER TABLE IF EXISTS paroisse_access DROP COLUMN IF EXISTS tenant_id;
ALTER TABLE IF EXISTS type_demande DROP COLUMN IF EXISTS tenant_id;
ALTER TABLE IF EXISTS type_paiement DROP COLUMN IF EXISTS tenant_id;
ALTER TABLE IF EXISTS users DROP COLUMN IF EXISTS tenant_id;
