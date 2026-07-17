-- Hibernate avait créé une contrainte UNIQUE physique sur (user_id,
-- paroisse_id). Elle empêchait de réaffecter un utilisateur après une
-- suppression logique de son ancien accès.
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
            WHERE relation.relname = 'paroisse_access'
              AND namespace.nspname = current_schema()
              AND con.contype = 'u'
        ) AS constraint_info
        WHERE constraint_info.column_names = 'paroisse_id,user_id'
    LOOP
        EXECUTE format(
                'ALTER TABLE paroisse_access DROP CONSTRAINT %I',
                constraint_name
        );
    END LOOP;
END
$$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_paroisse_access_active_user_paroisse
    ON paroisse_access (user_id, paroisse_id)
    WHERE status_del = false;
