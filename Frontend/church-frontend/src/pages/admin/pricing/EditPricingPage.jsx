import React from 'react';
import PageHeader from '../../../components/ui/PageHeader';
import AppCard from '../../../components/ui/AppCard';
import AppInput from '../../../components/ui/AppInput';
import AppButton from '../../../components/ui/AppButton';

export default function EditPricingPage() {
  return (
    <div className="stack">
      <PageHeader title="Modifier un forfait" subtitle="Formulaire d'édition d'un tarif existant." />
      <AppCard title="Forfait existant">
        <div className="form-grid">
          <div className="form-field"><label>Libellé</label><AppInput defaultValue="Forfait famille" /></div>
          <div className="form-field"><label>Montant</label><AppInput type="number" defaultValue="15000" /></div>
        </div>
        <div className="button-row" style={{ marginTop: 18 }}><AppButton>Enregistrer</AppButton></div>
      </AppCard>
    </div>
  );
}
