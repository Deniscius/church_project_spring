import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import PageHeader from '../../../components/ui/PageHeader';
import AppCard from '../../../components/ui/AppCard';
import AppBadge from '../../../components/ui/AppBadge';
import { invoiceService } from '../../../services/invoice.service';
import { formatCurrency } from '../../../utils/formatCurrency';
import { formatDate } from '../../../utils/formatDate';

export default function InvoiceDetailsPage() {
  const { id } = useParams();
  const [invoice, setInvoice] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!id) return;
    let cancelled = false;
    (async () => {
      try {
        setLoading(true);
        setError(null);
        const data = await invoiceService.getById(id);
        if (!cancelled) setInvoice(data);
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
      <PageHeader title="Détail facture" subtitle="Facture (FactureResponse)." />
      {loading ? <p className="muted">Chargement…</p> : null}
      {error ? <p className="text-red-600">{error}</p> : null}
      {invoice ? (
        <AppCard title="Facture">
          <div className="info-list">
            <div className="info-row">
              <span>Numéro</span>
              <strong>{invoice.refFacture}</strong>
            </div>
            <div className="info-row">
              <span>Code de suivi</span>
              <span>{invoice.codeSuivieDemande || '—'}</span>
            </div>
            <div className="info-row">
              <span>Demandeur</span>
              <span>
                {[invoice.prenomFidele, invoice.nomFidele].filter(Boolean).join(' ') || '—'}
              </span>
            </div>
            <div className="info-row">
              <span>Montant</span>
              <span>{formatCurrency(invoice.montant != null ? Number(invoice.montant) : 0)}</span>
            </div>
            <div className="info-row">
              <span>Statut</span>
              <AppBadge value={invoice.statutPaiement} />
            </div>
            <div className="info-row">
              <span>Date paiement</span>
              <span>{formatDate(invoice.datePaiement)}</span>
            </div>
            <div className="info-row">
              <span>Mode / type</span>
              <span>
                {invoice.typePaiementLibelle || '—'} {invoice.modePaiement ? `(${invoice.modePaiement})` : ''}
              </span>
            </div>
          </div>
        </AppCard>
      ) : null}
    </div>
  );
}
