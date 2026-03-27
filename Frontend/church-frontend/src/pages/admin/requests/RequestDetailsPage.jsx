import React from 'react';
import { useParams } from 'react-router-dom';
import PageHeader from '../../../components/ui/PageHeader';
import AppCard from '../../../components/ui/AppCard';
import AppBadge from '../../../components/ui/AppBadge';
import { mockRequests } from '../../../data/mockData';
import { formatCurrency } from '../../../utils/formatCurrency';

export default function RequestDetailsPage() {
  const { id } = useParams();
  const request = mockRequests.find((item) => item.id === Number(id)) || mockRequests[0];

  return (
    <div className="stack">
      <PageHeader title="Détail d'une demande" subtitle="Vue complète pour la secrétaire, le gestionnaire ou l'administrateur de paroisse." />
      <div className="grid-2">
        <AppCard title="Informations principales">
          <div className="info-list">
            <div className="info-row"><span>Code de suivi</span><strong>{request.trackingCode}</strong></div>
            <div className="info-row"><span>Demandeur</span><span>{request.applicant}</span></div>
            <div className="info-row"><span>Type</span><span>{request.requestType}</span></div>
            <div className="info-row"><span>Montant</span><span>{formatCurrency(request.amount)}</span></div>
          </div>
        </AppCard>
        <AppCard title="Statuts">
          <div className="info-list">
            <div className="info-row"><span>Demande</span><AppBadge value={request.requestStatus} /></div>
            <div className="info-row"><span>Validation</span><AppBadge value={request.validationStatus} /></div>
            <div className="info-row"><span>Paiement</span><AppBadge value={request.paymentStatus} /></div>
          </div>
        </AppCard>
      </div>
    </div>
  );
}
