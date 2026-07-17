-- Les utilisateurs globaux ou supprimés ne doivent conserver aucune
-- affectation paroissiale exploitable.
UPDATE paroisse_access access
SET active = false,
    status_del = true
FROM users app_user
WHERE access.user_id = app_user.id
  AND access.status_del = false
  AND (app_user.is_global = true OR app_user.status_del = true);

-- Le choix automatique d'une paroisse parmi plusieurs serait arbitraire :
-- la migration s'arrête avec un diagnostic clair pour permettre une correction.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM paroisse_access
        WHERE active = true
          AND status_del = false
        GROUP BY user_id
        HAVING count(*) > 1
    ) THEN
        RAISE EXCEPTION
            'Un utilisateur local possède plusieurs accès paroissiaux actifs';
    END IF;
END
$$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_paroisse_access_one_active_per_user
    ON paroisse_access (user_id)
    WHERE active = true
      AND status_del = false;
