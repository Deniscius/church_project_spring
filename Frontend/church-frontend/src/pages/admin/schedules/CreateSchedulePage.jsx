import React from 'react';
import PageHeader from '../../../components/ui/PageHeader';
import AppCard from '../../../components/ui/AppCard';
import AppInput from '../../../components/ui/AppInput';
import AppButton from '../../../components/ui/AppButton';

export default function CreateSchedulePage() {
  return (
    <div className="stack">
      <PageHeader title="Créer un horaire" subtitle="Formulaire de création d'un horaire rattaché à la paroisse active." />
      <AppCard title="Nouvel horaire">
        <div className="form-grid">
          <div className="form-field"><label>Libellé</label><AppInput placeholder="Messe du matin" /></div>
          <div className="form-field"><label>Heure</label><AppInput type="time" /></div>
          <div className="form-field"><label>Jour</label><select className="select"><option>LUNDI</option></select></div>
          <div className="form-field"><label>Actif</label><select className="select"><option>ACTIVE</option><option>INACTIVE</option></select></div>
        </div>
        <div className="button-row" style={{ marginTop: 18 }}><AppButton>Créer</AppButton></div>
      </AppCard>
    </div>
  );
}
