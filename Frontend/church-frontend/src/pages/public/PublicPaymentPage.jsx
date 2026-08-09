import React, { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import PageHeader from '../../components/ui/PageHeader';
import PublicPaymentCard from '../../components/public/PublicPaymentCard';
import AppCard from '../../components/ui/AppCard';
import AppBadge from '../../components/ui/AppBadge';
import { requestService } from '../../services/request.service';
import { paymentService } from '../../services/payment.service';
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
  const [returnHint] = useState(() => {
    try {
      const q = new URLSearchParams(window.location.search);
      return {
        statut: (q.get('statut') || '').toUpperCase(),
        status: (q.get('status') || '').toLowerCase(),
      };
    } catch {
      return { statut: '', status: '' };
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

  // Après retour FedaPay : resync (webhook peut arriver après le navigateur).
  useEffect(() => {
    if (!codeSuivie || (!returnHint.statut && !returnHint.status)) return undefined;
    if (returnHint.statut === 'PAYE') return undefined;
    let cancelled = false;
    let attempts = 0;
    let nextTimer;
    const tick = async () => {
      attempts += 1;
      try {
        await paymentService.reconcileByTrackingCode(codeSuivie);
        if (!cancelled) await load();
      } catch {
        /* ignore — le load principal suffit */
      }
      if (!cancelled && attempts < 4) {
        nextTimer = window.setTimeout(tick, 2500);
      }
    };
    const t = window.setTimeout(tick, 1500);
    return () => {
      cancelled = true;
      window.clearTimeout(t);
      if (nextTimer) window.clearTimeout(nextTimer);
    };
  }, [codeSuivie, returnHint.statut, returnHint.status, load]);

  const returnBanner = (() => {
    if (returnHint.statut === 'PAYE' || demande?.statutPaiement === 'PAYE') {
      return 'Paiement confirmé. Merci !';
    }
    if (returnHint.statut === 'ECHOUE' || returnHint.status === 'declined'
      || returnHint.status === 'canceled' || returnHint.status === 'cancelled') {
      return 'Le paiement n’a pas abouti. Vous pouvez réessayer ci-dessous.';
    }
    if (returnHint.statut || returnHint.status) {
      return 'Retour de FedaPay reçu. Vérification du statut en cours (webhook / API)…';
    }
    return '';
  })();

  return (
    <div className="stack public-page">
      <PageHeader
        title="Paiement"
        subtitle="Mobile Money ou carte via FedaPay. Le paiement au comptant n’est pas proposé sur le suivi."
      />
      {returnBanner ? (
        <p className="muted">{returnBanner}</p>
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
