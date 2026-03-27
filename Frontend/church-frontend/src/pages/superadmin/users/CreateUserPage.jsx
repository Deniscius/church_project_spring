import React from 'react';
import PageHeader from '../../../components/ui/PageHeader';
import AppCard from '../../../components/ui/AppCard';
import AppInput from '../../../components/ui/AppInput';
import AppButton from '../../../components/ui/AppButton';

export default function CreateUserPage() {
  return (
    <div className="stack">
      <PageHeader title="Créer un utilisateur" subtitle="Formulaire de création de compte pour les paroisses ou les administrateurs globaux." />
      <AppCard title="Nouvel utilisateur">
        <div className="form-grid">
          <div className="form-field"><label>Prénom</label><AppInput placeholder="Jean" /></div>
          <div className="form-field"><label>Nom</label><AppInput placeholder="Kossi" /></div>
          <div className="form-field"><label>Username</label><AppInput placeholder="jkossi" /></div>
          <div className="form-field"><label>Rôle</label><select className="select"><option>SECRETAIRE</option></select></div>
        </div>
        <div className="button-row" style={{ marginTop: 18 }}><AppButton>Créer</AppButton></div>
      </AppCard>
    </div>
  );
}
