import React from 'react';
import { useParams } from 'react-router-dom';
import PageHeader from '../../../components/ui/PageHeader';
import AppCard from '../../../components/ui/AppCard';
import AppBadge from '../../../components/ui/AppBadge';
import { mockInvoices } from '../../../data/mockData';
import { formatCurrency } from '../../../utils/formatCurrency';

export default function InvoiceDetailsPage() {
  const { id } = useParams();
  const invoice = mockInvoices.find((item) => item.id === Number(id)) || mockInvoices[0];

  return (
    <div className="stack">
      <PageHeader title="Détail facture" subtitle="Lecture complète d'une facture générée pour une demande." />
      <AppCard title="Facture">
        <div className="info-list">
          <div className="info-row"><span>Numéro</span><strong>{invoice.number}</strong></div>
          <div className="info-row"><span>Code de suivi</span><span>{invoice.trackingCode}</span></div>
          <div className="info-row"><span>Montant</span><span>{formatCurrency(invoice.amount)}</span></div>
          <div className="info-row"><span>Statut</span><AppBadge value={invoice.status} /></div>
        </div>
      </AppCard>
    </div>
  );
}
