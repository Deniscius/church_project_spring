import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import PageHeader from '../../components/ui/PageHeader';
import PublicInvoiceCard from '../../components/public/PublicInvoiceCard';
import AppCard from '../../components/ui/AppCard';
import AppBadge from '../../components/ui/AppBadge';
import { invoiceService } from '../../services/invoice.service';
import { formatCurrency } from '../../utils/formatCurrency';
import { formatDate } from '../../utils/formatDate';

export default function PublicInvoicePage() {
  const { codeSuivie } = useParams();
  const [facture, setFacture] = useState(null);
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
        const data = await invoiceService.getByTrackingCode(decodeURIComponent(codeSuivie));
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
    <div className="stack">
      <PageHeader title="Facture publique" subtitle="Consultation par code de suivi de la demande." />
      <div className="grid-2">
        <PublicInvoiceCard />
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
                <AppBadge value={facture.statutPaiement} />
              </div>
              <div className="info-row">
                <span>Date paiement</span>
                <span>{formatDate(facture.datePaiement)}</span>
              </div>
            </div>
          ) : null}
        </AppCard>
      </div>
    </div>
  );
}
