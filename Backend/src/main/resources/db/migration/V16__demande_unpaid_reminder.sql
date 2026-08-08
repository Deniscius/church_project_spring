-- Horodatage du dernier rappel impayé (e-mail D-3, toutes les 6 h).
ALTER TABLE demande
    ADD COLUMN IF NOT EXISTS last_unpaid_reminder_at TIMESTAMP;
