-- A parish journal may be transmitted by different authorized users after a lost response.
-- Do not delete/merge conflicting historical records silently: investigate duplicates before migration.
ALTER TABLE desktop_operation ADD CONSTRAINT desktop_operation_parish_operation_unique
    UNIQUE (parish_id, operation_id);
