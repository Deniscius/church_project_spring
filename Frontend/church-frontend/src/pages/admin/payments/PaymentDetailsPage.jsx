import React from 'react';
import { useParams } from 'react-router-dom';
import PageHeader from '../../../components/ui/PageHeader';
import AppCard from '../../../components/ui/AppCard';
import AppBadge from '../../../components/ui/AppBadge';
import { mockPayments } from '../../../data/mockData';
import { formatCurrency } from '../../../utils/formatCurrency';

export default function PaymentDetailsPage() {
  const { id } = useParams();
  const payment = mockPayments.find((item) => item.id === Number(id)) || mockPayments[0];

  return (
    <div className="stack">
      <PageHeader title="Détail paiement" subtitle="Écran de consultation d'une transaction précise." />
      <AppCard title="Transaction">
        <div className="info-list">
          <div className="info-row"><span>Code de suivi</span><strong>{payment.trackingCode}</strong></div>
          <div className="info-row"><span>Référence</span><span>{payment.transactionId}</span></div>
          <div className="info-row"><span>Montant</span><span>{formatCurrency(payment.amount)}</span></div>
          <div className="info-row"><span>Statut</span><AppBadge value={payment.status} /></div>
        </div>
      </AppCard>
    </div>
  );
}
