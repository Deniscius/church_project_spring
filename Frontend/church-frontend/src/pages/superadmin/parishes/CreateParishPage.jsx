import React from 'react';
import PageHeader from '../../../components/ui/PageHeader';
import AppCard from '../../../components/ui/AppCard';
import AppInput from '../../../components/ui/AppInput';
import AppButton from '../../../components/ui/AppButton';

export default function CreateParishPage() {
  return (
    <div className="stack">
      <PageHeader title="Créer une paroisse" subtitle="Formulaire de création d'une nouvelle paroisse." />
      <AppCard title="Nouvelle paroisse">
        <div className="form-grid">
          <div className="form-field"><label>Nom</label><AppInput placeholder="Paroisse Sainte Rita" /></div>
          <div className="form-field"><label>Localité</label><AppInput placeholder="Agoè" /></div>
          <div className="form-field"><label>Email</label><AppInput placeholder="paroisse@example.com" /></div>
          <div className="form-field"><label>Téléphone</label><AppInput placeholder="+228 90 00 00 00" /></div>
        </div>
        <div className="button-row" style={{ marginTop: 18 }}><AppButton>Créer</AppButton></div>
      </AppCard>
    </div>
  );
}
