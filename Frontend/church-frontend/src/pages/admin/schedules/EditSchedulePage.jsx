import React from 'react';
import PageHeader from '../../../components/ui/PageHeader';
import AppCard from '../../../components/ui/AppCard';
import AppInput from '../../../components/ui/AppInput';
import AppButton from '../../../components/ui/AppButton';

export default function EditSchedulePage() {
  return (
    <div className="stack">
      <PageHeader title="Modifier un horaire" subtitle="Version d'édition du formulaire horaire." />
      <AppCard title="Horaire existant">
        <div className="form-grid">
          <div className="form-field"><label>Libellé</label><AppInput defaultValue="Messe du soir" /></div>
          <div className="form-field"><label>Heure</label><AppInput type="time" defaultValue="18:00" /></div>
        </div>
        <div className="button-row" style={{ marginTop: 18 }}><AppButton>Enregistrer</AppButton></div>
      </AppCard>
    </div>
  );
}
