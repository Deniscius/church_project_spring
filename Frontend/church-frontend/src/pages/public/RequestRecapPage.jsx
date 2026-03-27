import React from 'react';
import PageHeader from '../../components/ui/PageHeader';
import RequestSummaryCard from '../../components/public/RequestSummaryCard';
import AppCard from '../../components/ui/AppCard';
import AppButton from '../../components/ui/AppButton';

export default function RequestRecapPage() {
  return (
    <div className="stack">
      <PageHeader title="Récapitulatif de la demande" subtitle="Étape finale avant l'enregistrement ou l'envoi vers le tunnel de paiement." />
      <div className="grid-2">
        <RequestSummaryCard />
        <AppCard title="Décisions" subtitle="Zone pour la confirmation finale du client.">
          <div className="info-list">
            <div className="info-row"><span>Paroisse</span><span>Saint Joseph</span></div>
            <div className="info-row"><span>Type</span><span>Messe d’action de grâce</span></div>
            <div className="info-row"><span>Montant</span><span>5 000 XOF</span></div>
          </div>
          <div className="button-row" style={{ marginTop: 20 }}>
            <AppButton>Confirmer la demande</AppButton>
            <AppButton variant="secondary">Modifier</AppButton>
          </div>
        </AppCard>
      </div>
    </div>
  );
}
