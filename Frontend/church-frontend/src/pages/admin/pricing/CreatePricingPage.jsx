import React from 'react';
import PageHeader from '../../../components/ui/PageHeader';
import AppCard from '../../../components/ui/AppCard';
import AppInput from '../../../components/ui/AppInput';
import AppButton from '../../../components/ui/AppButton';

export default function CreatePricingPage() {
  return (
    <div className="stack">
      <PageHeader title="Créer un forfait" subtitle="Formulaire de création des tarifs associés aux types de demande." />
      <AppCard title="Nouveau forfait">
        <div className="form-grid">
          <div className="form-field"><label>Libellé</label><AppInput placeholder="Forfait simple" /></div>
          <div className="form-field"><label>Montant</label><AppInput type="number" placeholder="5000" /></div>
          <div className="form-field"><label>Nombre de célébrations</label><AppInput type="number" placeholder="1" /></div>
          <div className="form-field"><label>Heure personnalisée</label><select className="select"><option>Non</option><option>Oui</option></select></div>
        </div>
        <div className="button-row" style={{ marginTop: 18 }}><AppButton>Créer</AppButton></div>
      </AppCard>
    </div>
  );
}
