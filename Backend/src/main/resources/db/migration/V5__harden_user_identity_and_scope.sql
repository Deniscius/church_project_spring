-- Un nom d'utilisateur supprimé logiquement doit pouvoir être réutilisé.
-- La comparaison est insensible à la casse, comme lors de l'authentification.
DO $$
DECLARE
    constraint_name text;
BEGIN
    FOR constraint_name IN
        SELECT constraint_info.conname
        FROM (
            SELECT con.conname,
                   (
                       SELECT string_agg(att.attname, ',' ORDER BY att.attname)
                       FROM unnest(con.conkey) AS key_column(attnum)
                       JOIN pg_attribute att
                         ON att.attrelid = con.conrelid
                        AND att.attnum = key_column.attnum
                   ) AS column_names
            FROM pg_constraint con
            JOIN pg_class relation ON relation.oid = con.conrelid
            JOIN pg_namespace namespace ON namespace.oid = relation.relnamespace
            WHERE relation.relname = 'users'
              AND namespace.nspname = current_schema()
              AND con.contype = 'u'
        ) AS constraint_info
        WHERE constraint_info.column_names = 'username'
    LOOP
        EXECUTE format('ALTER TABLE users DROP CONSTRAINT %I', constraint_name);
    END LOOP;
END
$$;

UPDATE users
SET username = lower(btrim(username))
WHERE username IS DISTINCT FROM lower(btrim(username));

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM users
        WHERE status_del = false
        GROUP BY lower(btrim(username))
        HAVING count(*) > 1
    ) THEN
        RAISE EXCEPTION
            'Des utilisateurs actifs possèdent le même username sans tenir compte de la casse';
    END IF;
END
$$;

ALTER TABLE users
    ALTER COLUMN is_global SET DEFAULT false,
    ALTER COLUMN is_active SET DEFAULT true,
    ALTER COLUMN status_del SET DEFAULT false,
    ALTER COLUMN nom SET NOT NULL,
    ALTER COLUMN prenom SET NOT NULL,
    ALTER COLUMN username SET NOT NULL,
    ALTER COLUMN password SET NOT NULL,
    ALTER COLUMN is_global SET NOT NULL,
    ALTER COLUMN is_active SET NOT NULL,
    ALTER COLUMN role SET NOT NULL,
    ALTER COLUMN status_del SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_users_active_username_ci
    ON users (lower(btrim(username)))
    WHERE status_del = false;

-- Le périmètre existant est conservé : un compte déjà global reçoit le rôle
-- global canonique, tandis qu'un SUPER_ADMIN local est ramené au rôle ADMIN.
UPDATE users
SET role = 'SUPER_ADMIN'
WHERE is_global = true
  AND role <> 'SUPER_ADMIN';

UPDATE users
SET role = 'ADMIN'
WHERE is_global = false
  AND role = 'SUPER_ADMIN';

ALTER TABLE users
    ADD CONSTRAINT ck_users_global_super_admin
    CHECK ((role = 'SUPER_ADMIN') = is_global);
