import React, { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import PageHeader from '../../components/ui/PageHeader';
import PublicPaymentCard from '../../components/public/PublicPaymentCard';
import AppCard from '../../components/ui/AppCard';
import AppBadge from '../../components/ui/AppBadge';
import { requestService } from '../../services/request.service';
import { formatCurrency } from '../../utils/formatCurrency';
import { formatDate } from '../../utils/formatDate';
import { getPaymentCode, setPaymentCode } from '../../utils/sensitiveNav';

export default function PublicPaymentPage() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const [codeSuivie, setCodeSuivie] = useState(() => getPaymentCode());
  const [demande, setDemande] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [fedapayStatus] = useState(() => {
    try {
      return new URLSearchParams(window.location.search).get('status') || '';
    } catch {
      return '';
    }
  });

  // Compat anciennes URL /paiement/:code — capture puis URL propre.
  useEffect(() => {
    const parts = window.location.pathname.split('/').filter(Boolean);
    if (parts[0] === 'paiement' && parts[1] && parts[1] !== 'retour') {
      const legacy = decodeURIComponent(parts[1]);
      setPaymentCode(legacy);
      setCodeSuivie(getPaymentCode());
      navigate('/paiement', { replace: true });
    }
  }, [navigate]);

  // Nettoie la query après lecture.
  useEffect(() => {
    if ([...searchParams.keys()].length === 0) return;
    setSearchParams({}, { replace: true });
  }, [searchParams, setSearchParams]);

  const load = useCallback(async () => {
    const code = codeSuivie || getPaymentCode();
    if (!code) {
      setLoading(false);
      return;
    }
    try {
      setLoading(true);
      setError(null);
      const data = await requestService.getByTrackingCode(code);
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
      {!codeSuivie && !loading ? (
        <p className="muted">
          Aucune demande en session.{' '}
          <Link to="/suivi">Passer par le suivi</Link>
          {' '}avec votre code.
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
                <span>{formatDate(demande.dateDetailsPaiement || demande.createdAt)}</span>
              </div>
            </div>
          ) : null}
        </AppCard>
      </div>
    </div>
  );
}
