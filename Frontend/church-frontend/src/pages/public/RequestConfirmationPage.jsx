import React from 'react';
import PageHeader from '../../components/ui/PageHeader';
import ConfirmationCard from '../../components/public/ConfirmationCard';
import AppCard from '../../components/ui/AppCard';

export default function RequestConfirmationPage() {
  return (
    <div className="stack">
      <PageHeader title="Confirmation" subtitle="Page de succès après création de la demande." />
      <div className="grid-2">
        <ConfirmationCard />
        <AppCard title="Informations à mettre en avant" subtitle="Éléments utiles à afficher immédiatement après création.">
          <div className="info-list">
            <div className="info-row"><span>Code de suivi</span><strong>MS-2026-0004</strong></div>
            <div className="info-row"><span>Statut initial</span><span className="badge warning">EN_ATTENTE</span></div>
            <div className="info-row"><span>Étape suivante</span><span>Suivre la demande ou effectuer le paiement</span></div>
          </div>
        </AppCard>
      </div>
    </div>
  );
}
