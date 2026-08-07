import React, { useCallback, useEffect, useState } from 'react';
import { useParams, useSearchParams } from 'react-router-dom';
import PageHeader from '../../components/ui/PageHeader';
import PublicPaymentCard from '../../components/public/PublicPaymentCard';
import AppCard from '../../components/ui/AppCard';
import AppBadge from '../../components/ui/AppBadge';
import { requestService } from '../../services/request.service';
import { formatCurrency } from '../../utils/formatCurrency';
import { formatDate } from '../../utils/formatDate';

export default function PublicPaymentPage() {
  const { codeSuivie } = useParams();
  const [searchParams] = useSearchParams();
  const [demande, setDemande] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const load = useCallback(async () => {
    if (!codeSuivie) {
      setLoading(false);
      return;
    }
    try {
      setLoading(true);
      setError(null);
      const data = await requestService.getByTrackingCode(decodeURIComponent(codeSuivie));
      setDemande(data);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Erreur');
    } finally {
      setLoading(false);
    }
  }, [codeSuivie]);

  useEffect(() => {
    load();
  }, [load]);

  const fedapayStatus = searchParams.get('status');

  return (
    <div className="stack public-page">
      <PageHeader
        title="Paiement"
        subtitle="Mobile Money ou carte via FedaPay. Le paiement au comptant n’est pas proposé sur le suivi."
      />
      {fedapayStatus ? (
        <p className="muted">
          Retour FedaPay — statut transaction : <strong>{fedapayStatus}</strong>.
          La confirmation définitive arrive via webhook (quelques secondes).
        </p>
      ) : null}
      <div className="grid-2">
        <PublicPaymentCard demande={demande} onStatusMaybeChanged={load} />
        <AppCard title="Détails">
          {loading ? <p className="muted">Chargement…</p> : null}
          {error ? <p className="text-red-600">{error}</p> : null}
          {demande ? (
            <div className="info-list">
              <div className="info-row">
                <span>Code de suivi</span>
                <strong>{demande.codeSuivie}</strong>
              </div>
              <div className="info-row">
                <span>Référence transaction</span>
                <strong>{demande.idTransaction || '—'}</strong>
              </div>
              <div className="info-row">
                <span>Moyen</span>
                <span>{demande.typePaiementLibelle || demande.modePaiement || '—'}</span>
              </div>
              <div className="info-row">
                <span>Montant</span>
                <span>{formatCurrency(demande.montant != null ? Number(demande.montant) : 0)}</span>
              </div>
              <div className="info-row">
                <span>Statut</span>
                <AppBadge value={demande.statutPaiement} />
              </div>
              <div className="info-row">
                <span>Date</span>
                <span>{formatDate(demande.dateDetailsPaiement)}</span>
              </div>
            </div>
          ) : null}
        </AppCard>
      </div>
    </div>
  );
}
