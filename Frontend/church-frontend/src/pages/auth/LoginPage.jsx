import React from 'react';
import PageHeader from '../../components/ui/PageHeader';
import AppInput from '../../components/ui/AppInput';
import AppButton from '../../components/ui/AppButton';

export default function LoginPage() {
  return (
    <div className="stack">
      <PageHeader title="Connexion paroisse" subtitle="Écran d'authentification pour accéder au dashboard privé de chaque paroisse." />
      <div className="form-field">
        <label>Nom d'utilisateur</label>
        <AppInput placeholder="jkossi" />
      </div>
      <div className="form-field">
        <label>Mot de passe</label>
        <AppInput type="password" placeholder="••••••••" />
      </div>
      <div className="button-row">
        <AppButton>Se connecter</AppButton>
        <AppButton variant="secondary">Mot de passe oublié</AppButton>
      </div>
    </div>
  );
}
