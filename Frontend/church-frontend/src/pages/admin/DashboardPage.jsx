import React from 'react';
import PageHeader from '../../components/ui/PageHeader';
import AppCard from '../../components/ui/AppCard';
import { mockStats, mockRequests } from '../../data/mockData';
import { formatCurrency } from '../../utils/formatCurrency';

export default function DashboardPage() {
  return (
    <div className="stack">
      <PageHeader title="Dashboard paroisse" subtitle="Vue d'ensemble des demandes, paiements, factures et points d'attention de la paroisse connectée." />
      <div className="card-grid">
        <AppCard title="Demandes totales"><div className="kpi"><strong>{mockStats.requests}</strong><span className="page-subtitle">Toutes les demandes de la paroisse</span></div></AppCard>
        <AppCard title="En attente"><div className="kpi"><strong>{mockStats.pendingRequests}</strong><span className="page-subtitle">Demandes à traiter</span></div></AppCard>
        <AppCard title="Validées"><div className="kpi"><strong>{mockStats.validatedRequests}</strong><span className="page-subtitle">Demandes approuvées</span></div></AppCard>
        <AppCard title="Montant exemple"><div className="kpi"><strong>{formatCurrency(45000)}</strong><span className="page-subtitle">Encaissements ou volume ciblé</span></div></AppCard>
      </div>
      <AppCard title="Demandes récentes" subtitle="Tableau condensé à afficher sur le dashboard.">
        <table className="app-table">
          <thead>
            <tr>
              <th>Code</th>
              <th>Demandeur</th>
              <th>Type</th>
              <th>Statut</th>
            </tr>
          </thead>
          <tbody>
            {mockRequests.map((item) => (
              <tr key={item.id}>
                <td>{item.trackingCode}</td>
                <td>{item.applicant}</td>
                <td>{item.requestType}</td>
                <td>{item.requestStatus}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </AppCard>
    </div>
  );
}
