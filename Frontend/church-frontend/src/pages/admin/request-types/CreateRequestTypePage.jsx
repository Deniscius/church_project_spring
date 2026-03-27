import React from 'react';
import PageHeader from '../../../components/ui/PageHeader';
import AppCard from '../../../components/ui/AppCard';
import AppInput from '../../../components/ui/AppInput';
import AppTextarea from '../../../components/ui/AppTextarea';
import AppButton from '../../../components/ui/AppButton';

export default function CreateRequestTypePage() {
  return (
    <div className="stack">
      <PageHeader title="Créer un type de demande" subtitle="Formulaire de création pour les catégories liturgiques et assimilées." />
      <AppCard title="Nouveau type de demande">
        <div className="form-grid">
          <div className="form-field"><label>Libellé</label><AppInput placeholder="Messe de requiem" /></div>
          <div className="form-field"><label>Catégorie</label><select className="select"><option>EUCHARISTIE</option><option>SACRAMENTAL</option></select></div>
          <div className="form-field full"><label>Description</label><AppTextarea placeholder="Décrire le type de demande" /></div>
        </div>
        <div className="button-row" style={{ marginTop: 18 }}><AppButton>Créer</AppButton></div>
      </AppCard>
    </div>
  );
}
