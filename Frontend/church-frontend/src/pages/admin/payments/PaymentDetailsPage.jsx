import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import PageHeader from '../../../components/ui/PageHeader';
import AppCard from '../../../components/ui/AppCard';
import AppBadge from '../../../components/ui/AppBadge';
import { requestService } from '../../../services/request.service';
import { formatCurrency } from '../../../utils/formatCurrency';
import { formatDate } from '../../../utils/formatDate';

export default function PaymentDetailsPage() {
  const { id } = useParams();
  const [demande, setDemande] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!id) return;
    let cancelled = false;
    (async () => {
      try {
        setLoading(true);
        setError(null);
        const data = await requestService.getById(id);
        if (!cancelled) setDemande(data);
      } catch (e) {
        if (!cancelled) setError(e instanceof Error ? e.message : 'Erreur');
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [id]);

  return (
    <div className="stack">
      <PageHeader title="Détail paiement" subtitle="Informations de paiement portées par la demande." />
      {loading ? <p className="muted">Chargement…</p> : null}
      {error ? <p className="text-red-600">{error}</p> : null}
      {demande ? (
        <AppCard title="Transaction">
          <div className="info-list">
            <div className="info-row">
              <span>Code de suivi</span>
              <strong>{demande.codeSuivie}</strong>
            </div>
            <div className="info-row">
              <span>Référence / ID transaction</span>
              <span>{demande.idTransaction || '—'}</span>
            </div>
            <div className="info-row">
              <span>Numéro</span>
              <span>{demande.numero || '—'}</span>
            </div>
            <div className="info-row">
              <span>Montant</span>
              <span>{formatCurrency(demande.montant != null ? Number(demande.montant) : 0)}</span>
            </div>
            <div className="info-row">
              <span>Type de paiement</span>
              <span>{demande.typePaiementLibelle || demande.modePaiement || '—'}</span>
            </div>
            <div className="info-row">
              <span>Date détail paiement</span>
              <span>{formatDate(demande.dateDetailsPaiement)}</span>
            </div>
            <div className="info-row">
              <span>Statut</span>
              <AppBadge value={demande.statutPaiement} />
            </div>
          </div>
        </AppCard>
      ) : null}
    </div>
  );
}
