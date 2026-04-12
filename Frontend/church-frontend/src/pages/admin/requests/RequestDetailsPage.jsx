import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import PageHeader from '../../../components/ui/PageHeader';
import AppCard from '../../../components/ui/AppCard';
import AppBadge from '../../../components/ui/AppBadge';
import { requestService } from '../../../services/request.service';
import { formatCurrency } from '../../../utils/formatCurrency';
import { formatDate } from '../../../utils/formatDate';

export default function RequestDetailsPage() {
  const { id } = useParams();
  const [request, setRequest] = useState(null);
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
        if (!cancelled) setRequest(data);
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

  const applicant = request
    ? [request.prenomFidele, request.nomFidele].filter(Boolean).join(' ')
    : '—';

  return (
    <div className="stack">
      <PageHeader
        title="Détail d'une demande"
        subtitle="Données issues du backend (DemandeResponse)."
      />
      {loading ? <p className="muted">Chargement…</p> : null}
      {error ? <p className="text-red-600">{error}</p> : null}
      {request ? (
        <div className="grid-2">
          <AppCard title="Informations principales">
            <div className="info-list">
              <div className="info-row">
                <span>Code de suivi</span>
                <strong>{request.codeSuivie}</strong>
              </div>
              <div className="info-row">
                <span>Demandeur</span>
                <span>{applicant}</span>
              </div>
              <div className="info-row">
                <span>Contact</span>
                <span>
                  {request.telFidele || '—'} {request.emailFidele ? `· ${request.emailFidele}` : ''}
                </span>
              </div>
              <div className="info-row">
                <span>Type</span>
                <span>{request.typeDemandeLibelle || '—'}</span>
              </div>
              <div className="info-row">
                <span>Intention</span>
                <span>{request.intention || '—'}</span>
              </div>
              <div className="info-row">
                <span>Montant</span>
                <span>{formatCurrency(request.montant != null ? Number(request.montant) : 0)}</span>
              </div>
              <div className="info-row">
                <span>Créée le</span>
                <span>{formatDate(request.createdAt)}</span>
              </div>
            </div>
          </AppCard>
          <AppCard title="Statuts & facturation">
            <div className="info-list">
              <div className="info-row">
                <span>Demande</span>
                <AppBadge value={request.statutDemande} />
              </div>
              <div className="info-row">
                <span>Validation</span>
                <AppBadge value={request.statutValidation} />
              </div>
              <div className="info-row">
                <span>Paiement</span>
                <AppBadge value={request.statutPaiement} />
              </div>
              <div className="info-row">
                <span>Réf. facture</span>
                <span>{request.refFacture || '—'}</span>
              </div>
              <div className="info-row">
                <span>Transaction</span>
                <span>{request.idTransaction || '—'}</span>
              </div>
            </div>
          </AppCard>
        </div>
      ) : null}
    </div>
  );
}
