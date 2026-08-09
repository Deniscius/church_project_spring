import React, { useEffect, useState } from 'react';
import AppCard from '../ui/AppCard';
import AppButton from '../ui/AppButton';
import FormError from '../ui/FormError';
import PaymentTypeSelector from './PaymentTypeSelector';
import { paymentService } from '../../services/payment.service';
import { requestService } from '../../services/request.service';
import { formatCurrency } from '../../utils/formatCurrency';
import { useScrollToError } from '../../hooks/useScrollToError';
import ReceiptPreviewButton from '../ui/ReceiptPreviewButton';

const ONLINE_MODES = new Set(['TMONEY', 'FLOOZ', 'CARTE']);

function FeeBreakdown({ quote, loading }) {
  if (loading && !quote) {
    return <p className="muted" style={{ margin: 0 }}>Calcul du total…</p>;
  }
  if (!quote) return null;

  return (
    <div className="payment-fee-breakdown">
      <div className="info-list">
        <div className="info-row">
          <span>Prix de l’intention</span>
          <span>{formatCurrency(quote.montantFacture)}</span>
        </div>
        <div className="info-row">
          <span>
            Frais de service
            {quote.feePercentPlateforme != null ? ` (${quote.feePercentPlateforme}%)` : ''}
          </span>
          <span>{formatCurrency(quote.montantFraisPlateforme ?? 0)}</span>
        </div>
        <div className="info-row">
          <span>
            Frais de paiement
            {quote.feePercentAgregeateur != null ? ` (${quote.feePercentAgregeateur}%)` : ''}
          </span>
          <span>{formatCurrency(quote.montantFraisAgregeateur ?? quote.montantFrais ?? 0)}</span>
        </div>
        <div className="info-row info-row-total payment-fee-total">
          <span>Total à payer</span>
          <strong>{formatCurrency(quote.montantCharge)}</strong>
        </div>
      </div>
      <p className="muted payment-fee-hint">
        Ce total inclut les frais. C’est le montant qui sera débité.
      </p>
    </div>
  );
}

export default function PublicPaymentCard({ demande, onStatusMaybeChanged }) {
  const [quote, setQuote] = useState(null);
  const [quoteLoading, setQuoteLoading] = useState(false);
  const [busy, setBusy] = useState(false);
  const [changingMode, setChangingMode] = useState(false);
  const [error, setError] = useState(null);
  const [info, setInfo] = useState(null);
  const [showModeEditor, setShowModeEditor] = useState(false);
  const [pendingModeId, setPendingModeId] = useState('');
  const errorRef = useScrollToError(error);

  const mode = demande?.modePaiement;
  const status = demande?.statutPaiement;
  const isCash = String(mode || '').toUpperCase() === 'ESPECES';
  const isOnline = ONLINE_MODES.has(String(mode || '').toUpperCase());
  const canPayOnline =
    Boolean(demande?.codeSuivie) &&
    isOnline &&
    status !== 'PAYE';
  const canChangeMode =
    Boolean(demande?.codeSuivie) &&
    status !== 'PAYE' &&
    demande?.statutDemande !== 'ANNULEE' &&
    demande?.statutDemande !== 'REJETEE';

  useEffect(() => {
    if (!demande || status === 'PAYE') return;
    if (isCash) {
      setShowModeEditor(true);
      setPendingModeId('');
      setInfo('Le paiement au comptant n’est pas proposé en ligne. Choisissez TMoney, Flooz ou carte.');
    }
  }, [demande?.codeSuivie, isCash, status]);

  // Affiche le total TTC dès l’arrivée (mode en ligne).
  useEffect(() => {
    if (!canPayOnline || !demande?.codeSuivie) {
      setQuote(null);
      return undefined;
    }
    let cancelled = false;
    (async () => {
      setQuoteLoading(true);
      setError(null);
      try {
        const data = await paymentService.quoteByTrackingCode(demande.codeSuivie);
        if (!cancelled) setQuote(data);
      } catch (e) {
        if (!cancelled) {
          setQuote(null);
          setError(e instanceof Error ? e.message : 'Impossible de calculer le total');
        }
      } finally {
        if (!cancelled) setQuoteLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [canPayOnline, demande?.codeSuivie, demande?.modePaiement, demande?.typePaiementPublicId]);

  async function startCheckout() {
    if (!demande?.codeSuivie) return;
    if (!isOnline) {
      setError('Choisissez d’abord un mode de paiement en ligne (TMoney, Flooz ou carte).');
      setShowModeEditor(true);
      return;
    }
    setBusy(true);
    setError(null);
    setInfo(null);
    try {
      const data = await paymentService.checkoutByTrackingCode(demande.codeSuivie);
      setQuote({
        montantFacture: data.montantFacture,
        montantFrais: data.montantFrais,
        montantFraisAgregeateur: data.montantFraisAgregeateur,
        montantFraisPlateforme: data.montantFraisPlateforme,
        montantCharge: data.montantCharge,
        feePercentAgregeateur: data.feePercentAgregeateur,
        feePercentPlateforme: data.feePercentPlateforme,
        feePayer: data.feePayer,
        mode: data.modePaiement,
      });
      if (data.paymentUrl) {
        window.location.assign(data.paymentUrl);
        return;
      }
      setInfo(data.message || 'Paiement en ligne non disponible pour ce mode.');
      onStatusMaybeChanged?.();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Échec du démarrage du paiement');
    } finally {
      setBusy(false);
    }
  }

  async function saveMode() {
    if (!demande?.codeSuivie || !pendingModeId) return;
    setChangingMode(true);
    setError(null);
    setInfo(null);
    try {
      await requestService.updateTypePaiementByTrackingCode(demande.codeSuivie, pendingModeId);
      setQuote(null);
      setShowModeEditor(false);
      setInfo('Mode de paiement mis à jour. Le total ci-dessous est recalculé.');
      onStatusMaybeChanged?.();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Impossible de changer le mode de paiement');
    } finally {
      setChangingMode(false);
    }
  }

  const payLabel = quote?.montantCharge != null
    ? `Payer ${formatCurrency(quote.montantCharge)}`
    : 'Payer avec FedaPay';

  return (
    <AppCard
      title="Payer en ligne"
      subtitle="Mobile Money ou carte via FedaPay. Le paiement au comptant n’est pas proposé ici."
    >
      {!demande ? (
        <p className="muted" style={{ margin: 0 }}>
          Chargez une demande pour afficher le paiement.
        </p>
      ) : (
        <div className="stack" style={{ gap: 12 }}>
          {isCash && status !== 'PAYE' ? (
            <p style={{ margin: 0 }}>
              Cette demande était enregistrée « au comptant ». Pour payer depuis le suivi,
              choisissez un mode en ligne ci-dessous.
            </p>
          ) : null}

          {status === 'PAYE' ? (
            <p style={{ margin: 0 }}>
              Cette demande est déjà payée. Vous pouvez télécharger le reçu PDF
              (fidèle + archive secrétariat) avec les statuts à jour.
            </p>
          ) : null}

          {canPayOnline || (quote && status !== 'PAYE') ? (
            <FeeBreakdown quote={quote} loading={quoteLoading} />
          ) : null}

          {error ? <FormError error={error} errorRef={errorRef} /> : null}
          {info ? <p className="muted" style={{ margin: 0 }}>{info}</p> : null}

          <div className="button-row">
            {canPayOnline ? (
              <AppButton type="button" onClick={startCheckout} loading={busy} disabled={quoteLoading}>
                {busy ? 'Redirection…' : payLabel}
              </AppButton>
            ) : null}
            {demande?.codeSuivie ? (
              <ReceiptPreviewButton codeSuivie={demande.codeSuivie} />
            ) : null}
            {canChangeMode ? (
              <AppButton
                type="button"
                variant="ghost"
                disabled={busy || changingMode}
                onClick={() => {
                  setShowModeEditor((v) => !v);
                  setPendingModeId(isOnline ? (demande.typePaiementPublicId || '') : '');
                  setError(null);
                }}
              >
                {showModeEditor ? 'Fermer' : (isCash ? 'Choisir un mode en ligne' : 'Changer de mode')}
              </AppButton>
            ) : null}
          </div>

          {showModeEditor && canChangeMode ? (
            <div className="stack" style={{ gap: 12 }}>
              <PaymentTypeSelector
                onlineOnly
                value={pendingModeId}
                parishName={demande.paroisseNom}
                title="Mode de paiement en ligne"
                subtitle="TMoney, Flooz ou carte uniquement — le comptant n’est pas proposé sur le suivi."
                onChange={({ publicId }) => setPendingModeId(publicId)}
              />
              <div className="button-row">
                <AppButton
                  type="button"
                  onClick={saveMode}
                  loading={changingMode}
                  disabled={!pendingModeId || pendingModeId === demande.typePaiementPublicId}
                >
                  Enregistrer le mode
                </AppButton>
              </div>
            </div>
          ) : null}
        </div>
      )}
    </AppCard>
  );
}
