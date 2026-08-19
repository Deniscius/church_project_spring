-- Rend le catalogue SaaS extensible sans redéploiement Java.
-- Les anciennes valeurs MENSUEL / SEMESTRIEL / ANNUEL restent inchangées.

UPDATE plan_saas
SET code = UPPER(TRIM(code));

UPDATE paroisse_inscription
SET plan_abonnement = UPPER(TRIM(plan_abonnement));

UPDATE paroisse_abonnement
SET plan = UPPER(TRIM(plan));

ALTER TABLE plan_saas
    DROP CONSTRAINT IF EXISTS chk_plan_saas_code;

ALTER TABLE paroisse_inscription
    DROP CONSTRAINT IF EXISTS ck_paroisse_inscription_plan;

ALTER TABLE paroisse_abonnement
    DROP CONSTRAINT IF EXISTS ck_abonnement_plan;

ALTER TABLE plan_saas
    ADD CONSTRAINT chk_plan_saas_code_format
        CHECK (code ~ '^[A-Z0-9][A-Z0-9_-]{0,29}$');

ALTER TABLE paroisse_inscription
    ADD CONSTRAINT ck_paroisse_inscription_plan_format
        CHECK (plan_abonnement ~ '^[A-Z0-9][A-Z0-9_-]{0,29}$');

ALTER TABLE paroisse_abonnement
    ADD CONSTRAINT ck_abonnement_plan_format
        CHECK (plan ~ '^[A-Z0-9][A-Z0-9_-]{0,29}$');
