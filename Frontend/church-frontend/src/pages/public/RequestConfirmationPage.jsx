import React, { useEffect, useMemo } from 'react';
import { useLocation } from 'react-router-dom';
import PageHeader from '../../components/ui/PageHeader';
import ConfirmationCard from '../../components/public/ConfirmationCard';
import AppCard from '../../components/ui/AppCard';
import AppBadge from '../../components/ui/AppBadge';
import { usePublicDemandeDraft } from '../../contexts/publicDemandeDraft.context';
import { readDemandeCreationResult } from '../../utils/publicDemandeValidation';
import { formatCurrency } from '../../utils/formatCurrency';

export default function RequestConfirmationPage() {
  const location = useLocation();
  const { reset } = usePublicDemandeDraft();

  const result = useMemo(() => {
    return location.state || readDemandeCreationResult();
  }, [location.state]);

  useEffect(() => {
    const r = location.state || readDemandeCreationResult();
    if (r?.codeSuivie) {
      reset();
    }
  }, [reset, location.state]);

  return (
    <div className="stack">
      <PageHeader title="Confirmation" subtitle="La demande a été créée côté serveur." />
      <div className="grid-2">
        <ConfirmationCard result={result} />
        <AppCard title="Détails" subtitle="Statuts initiaux renvoyés par l’API.">
          {result?.codeSuivie ? (
            <div className="info-list">
              <div className="info-row">
                <span>Code de suivi</span>
                <strong>{result.codeSuivie}</strong>
              </div>
              <div className="info-row">
                <span>Statut demande</span>
                <AppBadge value={result.statutDemande} />
              </div>
              <div className="info-row">
                <span>Validation</span>
                <AppBadge value={result.statutValidation} />
              </div>
              <div className="info-row">
                <span>Paiement</span>
                <AppBadge value={result.statutPaiement} />
              </div>
              {result.refFacture ? (
                <div className="info-row">
                  <span>Facture</span>
                  <span>{result.refFacture}</span>
                </div>
              ) : null}
              {result.montant != null ? (
                <div className="info-row">
                  <span>Montant</span>
                  <span>{formatCurrency(Number(result.montant))}</span>
                </div>
              ) : null}
            </div>
          ) : (
            <p className="muted">Ouvrez cette page après une création réussie, ou utilisez le lien ci-contre.</p>
          )}
        </AppCard>
      </div>
    </div>
  );
}
