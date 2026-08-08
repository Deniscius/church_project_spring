-- E-mail admin d'inscription : contact optionnel (l'e-mail pro est généré à l'approbation).
ALTER TABLE paroisse_inscription
    ALTER COLUMN admin_email DROP NOT NULL;
