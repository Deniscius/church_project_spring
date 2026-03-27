import React from 'react';
import PageHeader from '../../components/ui/PageHeader';
import PublicPaymentCard from '../../components/public/PublicPaymentCard';
import AppCard from '../../components/ui/AppCard';

export default function PublicPaymentPage() {
  return (
    <div className="stack">
      <PageHeader title="Paiement public" subtitle="Vue publique de l'état du paiement rattaché à une demande." />
      <div className="grid-2">
        <PublicPaymentCard />
        <AppCard title="Données à afficher" subtitle="Éléments clés d'un paiement consulté publiquement.">
          <div className="info-list">
            <div className="info-row"><span>Référence transaction</span><strong>TX-92011</strong></div>
            <div className="info-row"><span>Moyen</span><span>TMONEY</span></div>
            <div className="info-row"><span>Statut</span><span className="badge success">PAYE</span></div>
          </div>
        </AppCard>
      </div>
    </div>
  );
}
