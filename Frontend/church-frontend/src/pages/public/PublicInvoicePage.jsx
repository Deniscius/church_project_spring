import React, { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import PageHeader from '../../components/ui/PageHeader';
import PublicInvoiceCard from '../../components/public/PublicInvoiceCard';
import AppCard from '../../components/ui/AppCard';
import AppBadge from '../../components/ui/AppBadge';
import { invoiceService } from '../../services/invoice.service';
import { formatCurrency } from '../../utils/formatCurrency';
import { formatDate } from '../../utils/formatDate';
import { paymentStatusLabel } from '../../utils/statusMapper';
import { getInvoiceCode, setInvoiceCode } from '../../utils/sensitiveNav';

export default function PublicInvoicePage() {
  const navigate = useNavigate();
  const [codeSuivie, setCodeSuivie] = useState(() => getInvoiceCode());
  const [facture, setFacture] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const parts = window.location.pathname.split('/').filter(Boolean);
    if (parts[0] === 'facture' && parts[1]) {
      setInvoiceCode(decodeURIComponent(parts[1]));
      setCodeSuivie(getInvoiceCode());
      navigate('/facture', { replace: true });
    }
  }, [navigate]);

  useEffect(() => {
    const code = codeSuivie || getInvoiceCode();
    if (!code) {
      setLoading(false);
      return;
    }
    let cancelled = false;
    (async () => {
      try {
        setLoading(true);
        setError(null);
        const data = await invoiceService.getByTrackingCode(code);
        if (!cancelled) setFacture(data);
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
    <div className="stack public-page">
      <PageHeader title="Facture publique" subtitle="Consultation par code de suivi de la demande." />
      {!codeSuivie && !loading ? (
        <p className="muted">
          Aucune facture en session. <Link to="/suivi">Passer par le suivi</Link>.
        </p>
      ) : null}
      <div className="grid-2">
        <PublicInvoiceCard codeSuivie={facture?.codeSuivieDemande || codeSuivie} />
        <AppCard title="Données facture">
          {loading ? <p className="muted">Chargement…</p> : null}
          {error ? <p className="text-red-600">{error}</p> : null}
          {facture ? (
            <div className="info-list">
              <div className="info-row">
                <span>Référence</span>
                <strong>{facture.refFacture}</strong>
              </div>
              <div className="info-row">
                <span>Code suivi</span>
                <span>{facture.codeSuivieDemande}</span>
              </div>
              <div className="info-row">
                <span>Montant</span>
                <span>{formatCurrency(facture.montant != null ? Number(facture.montant) : 0)}</span>
              </div>
              <div className="info-row">
                <span>Statut</span>
                <AppBadge value={paymentStatusLabel(facture.statutPaiement) || facture.statutPaiement} />
              </div>
              <div className="info-row">
                <span>Émise le</span>
                <span>{formatDate(facture.dateEmission)}</span>
              </div>
            </div>
          ) : null}
        </AppCard>
      </div>
    </div>
  );
}
