import React, { useEffect, useMemo, useState } from 'react';
import { useLocation } from 'react-router-dom';
import PageHeader from '../../components/ui/PageHeader';
import ConfirmationCard from '../../components/public/ConfirmationCard';
import AppCard from '../../components/ui/AppCard';
import AppBadge from '../../components/ui/AppBadge';
import AppButton from '../../components/ui/AppButton';
import { usePublicDemandeDraft } from '../../contexts/publicDemandeDraft.context';
import { useToast } from '../../contexts/toast.context';
import { readDemandeCreationResult } from '../../utils/publicDemandeValidation';
import { formatCurrency } from '../../utils/formatCurrency';
import { copyText } from '../../utils/clipboard';

export default function RequestConfirmationPage() {
  const location = useLocation();
  const toast = useToast();
  const { reset } = usePublicDemandeDraft();
  const [copied, setCopied] = useState(false);

  const result = useMemo(() => {
    return location.state || readDemandeCreationResult();
  }, [location.state]);

  useEffect(() => {
    const r = location.state || readDemandeCreationResult();
    if (r?.codeSuivie) {
      reset();
    }
  }, [reset, location.state]);

  const copyCode = async () => {
    if (!result?.codeSuivie) return;
    const ok = await copyText(result.codeSuivie);
    if (ok) {
      setCopied(true);
      toast.success('Code de suivi copié dans le presse-papiers.');
      window.setTimeout(() => setCopied(false), 2500);
    } else {
      toast.error('Impossible de copier le code. Notez-le manuellement.');
    }
  };

  return (
    <div className="stack public-page">
      <PageHeader
        title="Confirmation"
        subtitle="Votre demande est enregistrée. Copiez et conservez votre numéro de suivi."
      />
      <div className="grid-2">
        <ConfirmationCard result={result} />
        <AppCard title="Détails" subtitle="Statuts initiaux renvoyés par l’API.">
          {result?.codeSuivie ? (
            <div className="info-list">
              <div className="info-row">
                <span>Code de suivi</span>
                <span style={{ display: 'inline-flex', alignItems: 'center', gap: 8 }}>
                  <strong>{result.codeSuivie}</strong>
                  <AppButton type="button" variant="secondary" size="sm" onClick={copyCode}>
                    {copied ? 'Copié' : 'Copier'}
                  </AppButton>
                </span>
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
