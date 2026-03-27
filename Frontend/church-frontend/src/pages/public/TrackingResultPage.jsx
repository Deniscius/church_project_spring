import React from 'react';
import PageHeader from '../../components/ui/PageHeader';
import AppCard from '../../components/ui/AppCard';
import AppBadge from '../../components/ui/AppBadge';

export default function TrackingResultPage() {
  return (
    <div className="stack">
      <PageHeader title="Résultat du suivi" subtitle="Vue publique des statuts de la demande, de la validation et du paiement." />
      <div className="grid-2">
        <AppCard title="Statuts de la demande" subtitle="Structure idéale pour l'écran de résultat.">
          <div className="info-list">
            <div className="info-row"><span>Code</span><strong>MS-2026-0002</strong></div>
            <div className="info-row"><span>Statut demande</span><AppBadge value="VALIDEE" /></div>
            <div className="info-row"><span>Statut validation</span><AppBadge value="VALIDEE" /></div>
            <div className="info-row"><span>Statut paiement</span><AppBadge value="PAYE" /></div>
          </div>
        </AppCard>
        <AppCard title="Actions utiles" subtitle="Boutons ou liens rapides à garder sous la main.">
          <ul style={{ margin: 0, paddingLeft: 18 }}>
            <li>Voir la facture liée</li>
            <li>Voir le détail de paiement</li>
            <li>Relancer la paroisse si besoin</li>
          </ul>
        </AppCard>
      </div>
    </div>
  );
}
