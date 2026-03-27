import React from 'react';
import PageHeader from '../../../components/ui/PageHeader';
import AppCard from '../../../components/ui/AppCard';
import AppInput from '../../../components/ui/AppInput';
import AppButton from '../../../components/ui/AppButton';

export default function EditRequestTypePage() {
  return (
    <div className="stack">
      <PageHeader title="Modifier un type de demande" subtitle="Version d'édition du référentiel type de demande." />
      <AppCard title="Type de demande">
        <div className="form-grid">
          <div className="form-field"><label>Libellé</label><AppInput defaultValue="Messe d’action de grâce" /></div>
          <div className="form-field"><label>Actif</label><select className="select"><option>ACTIVE</option></select></div>
        </div>
        <div className="button-row" style={{ marginTop: 18 }}><AppButton>Enregistrer</AppButton></div>
      </AppCard>
    </div>
  );
}
