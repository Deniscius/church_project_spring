import React from 'react';
import PageHeader from '../../../components/ui/PageHeader';
import AppCard from '../../../components/ui/AppCard';
import AppInput from '../../../components/ui/AppInput';
import AppTextarea from '../../../components/ui/AppTextarea';
import AppButton from '../../../components/ui/AppButton';

export default function EditRequestPage() {
  return (
    <div className="stack">
      <PageHeader title="Modifier une demande" subtitle="Formulaire d'édition branché plus tard sur le détail complet de la demande." />
      <AppCard title="Édition métier">
        <div className="form-grid">
          <div className="form-field"><label>Demandeur</label><AppInput defaultValue="Kokou Adjei" /></div>
          <div className="form-field"><label>Statut</label><select className="select"><option>EN_ATTENTE</option><option>VALIDEE</option></select></div>
          <div className="form-field full"><label>Note interne</label><AppTextarea defaultValue="Exemple de commentaire interne pour la paroisse." /></div>
        </div>
        <div className="button-row" style={{ marginTop: 18 }}>
          <AppButton>Enregistrer</AppButton>
          <AppButton variant="secondary">Annuler</AppButton>
        </div>
      </AppCard>
    </div>
  );
}
