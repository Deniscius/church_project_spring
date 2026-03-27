import React from 'react';
import PageHeader from '../../../components/ui/PageHeader';
import AppCard from '../../../components/ui/AppCard';
import AppInput from '../../../components/ui/AppInput';
import AppButton from '../../../components/ui/AppButton';

export default function EditParishPage() {
  return (
    <div className="stack">
      <PageHeader title="Modifier une paroisse" subtitle="Formulaire d'édition d'une paroisse existante." />
      <AppCard title="Paroisse existante">
        <div className="form-grid">
          <div className="form-field"><label>Nom</label><AppInput defaultValue="Saint Joseph" /></div>
          <div className="form-field"><label>Localité</label><AppInput defaultValue="Lomé Centre" /></div>
        </div>
        <div className="button-row" style={{ marginTop: 18 }}><AppButton>Enregistrer</AppButton></div>
      </AppCard>
    </div>
  );
}
