import React from 'react';
import AppCard from '../ui/AppCard';
import { usePublicDemandeDraft } from '../../contexts/publicDemandeDraft.context';
import { formatCurrency } from '../../utils/formatCurrency';

export default function RequestSummaryCard() {
  const { draft } = usePublicDemandeDraft();

  return (
    <AppCard title="Résumé" subtitle="Aperçu du dossier avant envoi.">
      <div className="info-list">
        <div className="info-row">
          <span>Demandeur</span>
          <span>
            {[draft.prenomFidele, draft.nomFidele].filter(Boolean).join(' ') || '—'}
          </span>
        </div>
        <div className="info-row">
          <span>Paroisse</span>
          <span>{draft.paroisseNom || '—'}</span>
        </div>
        <div className="info-row">
          <span>Type</span>
          <span>{draft.typeDemandeLibelle || '—'}</span>
        </div>
        <div className="info-row">
          <span>Forfait</span>
          <span>{draft.forfaitLabel || '—'}</span>
        </div>
        <div className="info-row">
          <span>Montant</span>
          <span>
            {draft.forfaitMontant != null
              ? formatCurrency(Number(draft.forfaitMontant))
              : '—'}
          </span>
        </div>
        <div className="info-row">
          <span>Date début</span>
          <span>{draft.dateDebut || '—'}</span>
        </div>
        <div className="info-row">
          <span>Horaire</span>
          <span>
            {draft.horaireLibelle || '—'}
            {draft.forfaitHeurePersonnalise && draft.heurePersonnalisee
              ? ` · perso ${draft.heurePersonnalisee}`
              : ''}
          </span>
        </div>
        <div className="info-row">
          <span>Paiement</span>
          <span>{draft.typePaiementLibelle || '—'}</span>
        </div>
      </div>
    </AppCard>
  );
}
