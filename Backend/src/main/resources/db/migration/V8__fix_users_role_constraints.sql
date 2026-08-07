-- Corrige les contraintes users : V7 utilisait de mauvais noms
-- (users_role_check / ck_users_global_role) alors que le schéma porte
-- ck_users_role / ck_users_global_platform_role. Sans cela, COMPTABLE_LOCAL
-- reste rejeté par l'ancienne contrainte.

ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;
ALTER TABLE users DROP CONSTRAINT IF EXISTS ck_users_global_role;

ALTER TABLE users DROP CONSTRAINT IF EXISTS ck_users_role;
ALTER TABLE users
    ADD CONSTRAINT ck_users_role
        CHECK (role IN (
            'SECRETAIRE', 'CURE', 'ADMIN', 'COMPTABLE_LOCAL', 'COMPTABLE', 'SUPER_ADMIN'
        ));

ALTER TABLE users DROP CONSTRAINT IF EXISTS ck_users_global_platform_role;
ALTER TABLE users
    ADD CONSTRAINT ck_users_global_platform_role
        CHECK (is_global = (role IN ('SUPER_ADMIN', 'COMPTABLE')));
