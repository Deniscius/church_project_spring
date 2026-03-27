import React from 'react';
import PageHeader from '../../components/ui/PageHeader';
import PublicInvoiceCard from '../../components/public/PublicInvoiceCard';
import AppCard from '../../components/ui/AppCard';

export default function PublicInvoicePage() {
  return (
    <div className="stack">
      <PageHeader title="Facture publique" subtitle="Consultation de la facture via le code de suivi ou une route dédiée." />
      <div className="grid-2">
        <PublicInvoiceCard />
        <AppCard title="Données à afficher" subtitle="Structure suggérée pour la facture publique.">
          <div className="info-list">
            <div className="info-row"><span>Référence</span><strong>FAC-2026-0101</strong></div>
            <div className="info-row"><span>Montant</span><span>15 000 XOF</span></div>
            <div className="info-row"><span>Statut</span><span className="badge success">PAYE</span></div>
          </div>
        </AppCard>
      </div>
    </div>
  );
}
