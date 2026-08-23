import React, { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import PageHeader from '../../components/ui/PageHeader';
import AppCard from '../../components/ui/AppCard';
import AppBadge from '../../components/ui/AppBadge';
import AppButton from '../../components/ui/AppButton';
import { useToast } from '../../contexts/toast.context';
import { requestService } from '../../services/request.service';
import { paymentService } from '../../services/payment.service';
import { formatCurrency } from '../../utils/formatCurrency';
import { formatDate } from '../../utils/formatDate';
import { copyText } from '../../utils/clipboard';
import {
  getTrackingCode,
  goToInvoice,
  goToPayment,
  setTrackingCode,
} from '../../utils/sensitiveNav';
import ReceiptPreviewButton from '../../components/ui/ReceiptPreviewButton';

function canOfferPayment(demande) {
  if (!demande?.codeSuivie) return false;
  if (demande.paiementDisponible === false) return false;
  const statutDemande = String(demande.statutDemande || '').toUpperCase();
  if (statutDemande === 'ANNULEE' || statutDemande === 'REJETEE') return false;
  const statutPaiement = String(demande.statutPaiement || '').toUpperCase();
  return statutPaiement !== 'PAYE';
}

export default function TrackingResultPage() {
  const toast = useToast();
  const navigate = useNavigate();
  const [code, setCode] = useState(() => getTrackingCode());
  const [demande, setDemande] = useState(null);
  const [loading, setLoading] = useState(Boolean(getTrackingCode()));
  const [error, setError] = useState(null);
  const [copied, setCopied] = useState(false);
  const [quote, setQuote] = useState(null);
  const [quoteLoading, setQuoteLoading] = useState(false);

  // Compat ancienne URL ?code=… : récupère puis nettoie immédiatement la barre d’adresse.
  useEffect(() => {
    const qs = new URLSearchParams(window.location.search);
    const legacy = qs.get('code');
    if (legacy) {
      const stored = setTrackingCode(legacy);
      setCode(stored);
      qs.delete('code');
      const clean = `${window.location.pathname}${qs.toString() ? `?${qs}` : ''}`;
      window.history.replaceState({}, '', clean);
    }
  }, []);

  useEffect(() => {
    if (!code) {
      setDemande(null);
      setLoading(false);
      setError(null);
      return;
    }
    const controller = new AbortController();
    (async () => {
      try {
        setLoading(true);
        setError(null);
        setDemande(null);
        const data = await requestService.getByTrackingCode(
          code,
          { signal: controller.signal }
        );
        if (!controller.signal.aborted) setDemande(data);
      } catch (e) {
        if (!controller.signal.aborted) {
          setError(e instanceof Error ? e.message : 'Introuvable');
        }
      } finally {
        if (!controller.signal.aborted) setLoading(false);
      }
    })();
    return () => controller.abort();
  }, [code]);

  const offerPayment = canOfferPayment(demande);
  const paymentUnavailableReason = demande?.paiementIndisponibleMotif || null;
  const validationRequired = demande?.validationRequise
    ?? demande?.natureForfait === 'SPECIALE';
  const alreadyPaid = String(demande?.statutPaiement || '').toUpperCase() === 'PAYE';
  const mode = String(demande?.modePaiement || '').toUpperCase();
  const isOnline = mode === 'TMONEY' || mode === 'FLOOZ' || mode === 'CARTE';

  useEffect(() => {
    if (!offerPayment || !demande?.codeSuivie || !isOnline) {
      setQuote(null);
      return undefined;
    }
    let cancelled = false;
    (async () => {
      setQuoteLoading(true);
      try {
        const data = await paymentService.quoteByTrackingCode(demande.codeSuivie);
        if (!cancelled) setQuote(data);
      } catch {
        if (!cancelled) setQuote(null);
      } finally {
        if (!cancelled) setQuoteLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [offerPayment, demande?.codeSuivie, isOnline, demande?.typePaiementPublicId]);

  const baseAmount = formatCurrency(demande?.montant != null ? Number(demande.montant) : 0);
  const totalAmount = quote?.montantCharge != null
    ? formatCurrency(quote.montantCharge)
    : null;

  return (
    <div className="stack public-page">
      <PageHeader
        title="Votre demande"
        subtitle="Statut de validation, paiement et prochaines étapes."
      />
      {!code ? (
        <p className="muted">
          Aucun code en session.{' '}
          <Link to="/suivi">Retourner à la page Suivi</Link>
          {' '}pour consulter une demande.
        </p>
      ) : null}
      {loading ? (
        <p className="muted" role="status" aria-live="polite">Chargement…</p>
      ) : null}
      {error ? <p className="text-red-600" role="alert">{error}</p> : null}
      {demande ? (
        <div className="grid-2">
          <AppCard title="Statuts de la demande">
            <div className="info-list">
              <div className="info-row tracking-result-code-row">
                <span>Code</span>
                <div className="tracking-code-box" style={{ margin: 0, width: '100%' }}>
                  <strong className="tracking-code-value">{demande.codeSuivie}</strong>
                  <AppButton
                    type="button"
                    variant="secondary"
                    onClick={async () => {
                      const ok = await copyText(demande.codeSuivie);
                      if (ok) {
                        setCopied(true);
                        toast.success('Code de suivi copié.');
                        window.setTimeout(() => setCopied(false), 2500);
                      } else {
                        toast.error('Impossible de copier le code.');
                      }
                    }}
                  >
                    {copied ? 'Copié' : 'Copier le code'}
                  </AppButton>
                </div>
              </div>
              <div className="info-row">
                <span>Intention de messe</span>
                <span>{demande.intention || '—'}</span>
              </div>
              <div className="info-row">
                <span>Montant</span>
                <strong>{baseAmount}</strong>
              </div>
              <div className="info-row">
                <span>Statut demande</span>
                <AppBadge value={demande.statutDemande} />
              </div>
              <div className="info-row">
                <span>Validation</span>
                {validationRequired ? (
                  <AppBadge value={demande.statutValidation} />
                ) : (
                  <span className="badge badge-success">Non requise</span>
                )}
              </div>
              <div className="info-row">
                <span>Statut paiement</span>
                <AppBadge value={demande.statutPaiement} />
              </div>
              <div className="info-row">
                <span>Date(s) de célébration</span>
                <span>
                  {Array.isArray(demande.celebrationSlots) && demande.celebrationSlots.length
                    ? demande.celebrationSlots.map((s) => {
                      const d = formatDate(s.date);
                      const h = s.heure ? String(s.heure).slice(0, 5) : null;
                      const label = s.horaireLibelle ? ` · ${s.horaireLibelle}` : '';
                      return [d, h].filter(Boolean).join(' ') + label;
                    }).join(' · ')
                    : Array.isArray(demande.datesCelebration) && demande.datesCelebration.length
                      ? demande.datesCelebration.map((d) => formatDate(d)).join(' · ')
                      : '—'}
                </span>
              </div>
              <div className="info-row">
                <span>Date de dépôt</span>
                <span>{formatDate(demande.createdAt)}</span>
              </div>
            </div>
          </AppCard>

          <AppCard
            title={offerPayment ? 'Payer votre intention' : 'Actions'}
            subtitle={
              offerPayment
                ? 'Réglez en ligne (Mobile Money / carte). Le paiement au comptant n’est pas proposé ici.'
                : alreadyPaid
                  ? 'Cette demande est déjà payée.'
                  : paymentUnavailableReason || 'Consultation et documents liés à votre demande.'
            }
          >
            {offerPayment ? (
              <div className="stack" style={{ gap: 12 }}>
                {quoteLoading && !quote ? (
                  <p className="muted" style={{ margin: 0 }} role="status" aria-live="polite">
                    Calcul du total…
                  </p>
                ) : null}
                {quote ? (
                  <div className="payment-fee-breakdown">
                    <div className="info-list">
                      <div className="info-row">
                        <span>Prix de l’intention</span>
                        <span>{formatCurrency(quote.montantFacture)}</span>
                      </div>
                      <div className="info-row">
                        <span>Frais de service</span>
                        <span>{formatCurrency(quote.montantFraisPlateforme ?? 0)}</span>
                      </div>
                      <div className="info-row">
                        <span>Frais de paiement</span>
                        <span>{formatCurrency(quote.montantFraisAgregeateur ?? 0)}</span>
                      </div>
                      <div className="info-row info-row-total payment-fee-total">
                        <span>Total à payer</span>
                        <strong>{totalAmount}</strong>
                      </div>
                    </div>
                  </div>
                ) : (
                  <p style={{ margin: 0 }}>
                    Montant intention : <strong>{baseAmount}</strong>
                    {!isOnline ? (
                      <span className="muted"> — choisissez un mode en ligne pour voir le total avec frais.</span>
                    ) : null}
                  </p>
                )}
                <div className="button-row">
                  <AppButton type="button" onClick={() => goToPayment(navigate, demande.codeSuivie)}>
                    {totalAmount ? `Payer ${totalAmount}` : 'Payer maintenant'}
                  </AppButton>
                  <ReceiptPreviewButton codeSuivie={demande.codeSuivie} />
                  <AppButton
                    type="button"
                    variant="secondary"
                    onClick={() => goToInvoice(navigate, demande.codeSuivie)}
                  >
                    Voir la facture
                  </AppButton>
                </div>
                <p className="muted" style={{ margin: 0, fontSize: '0.9rem' }}>
                  Le total inclut les frais. Paiement en ligne uniquement depuis le suivi.
                </p>
              </div>
            ) : (
              <div className="stack" style={{ gap: 12 }}>
                {!alreadyPaid && paymentUnavailableReason ? (
                  <div className="alert-danger" role="status">
                    {paymentUnavailableReason}
                  </div>
                ) : null}
                <div className="button-row">
                {alreadyPaid ? (
                  <AppButton
                    type="button"
                    variant="secondary"
                    onClick={() => goToPayment(navigate, demande.codeSuivie)}
                  >
                    Voir le détail du paiement
                  </AppButton>
                ) : null}
                <ReceiptPreviewButton codeSuivie={demande.codeSuivie} variant="primary" />
                <AppButton
                  type="button"
                  variant="secondary"
                  onClick={() => goToInvoice(navigate, demande.codeSuivie)}
                >
                  Voir la facture
                </AppButton>
                <Link to="/suivi" className="btn btn-secondary" style={{ textDecoration: 'none' }}>
                  Nouvelle consultation
                </Link>
                </div>
              </div>
            )}
          </AppCard>
        </div>
      ) : null}
    </div>
  );
}
