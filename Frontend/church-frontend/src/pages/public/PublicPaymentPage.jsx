import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import PageHeader from '../../components/ui/PageHeader';
import PublicPaymentCard from '../../components/public/PublicPaymentCard';
import AppCard from '../../components/ui/AppCard';
import AppBadge from '../../components/ui/AppBadge';
import { requestService } from '../../services/request.service';
import { formatCurrency } from '../../utils/formatCurrency';
import { formatDate } from '../../utils/formatDate';

export default function PublicPaymentPage() {
  const { codeSuivie } = useParams();
  const [demande, setDemande] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!codeSuivie) {
      setLoading(false);
      return;
    }
    let cancelled = false;
    (async () => {
      try {
        setLoading(true);
        setError(null);
        const data = await requestService.getByTrackingCode(decodeURIComponent(codeSuivie));
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
  }, [codeSuivie]);

  return (
    <div className="stack">
      <PageHeader title="Paiement public" subtitle="État du paiement lié à la demande (code de suivi)." />
      <div className="grid-2">
        <PublicPaymentCard />
        <AppCard title="Détails">
          {loading ? <p className="muted">Chargement…</p> : null}
          {error ? <p className="text-red-600">{error}</p> : null}
          {demande ? (
            <div className="info-list">
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
