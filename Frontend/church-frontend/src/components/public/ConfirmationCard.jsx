import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import AppCard from '../ui/AppCard';
import AppButton from '../ui/AppButton';
import ReceiptPreviewButton from '../ui/ReceiptPreviewButton';
import { useToast } from '../../contexts/toast.context';
import { copyText } from '../../utils/clipboard';
import { goToInvoice, goToPayment, goToTrackingResult } from '../../utils/sensitiveNav';

export default function ConfirmationCard({ result }) {
  const toast = useToast();
  const navigate = useNavigate();
  const [copied, setCopied] = useState(false);

  if (!result?.codeSuivie) {
    return (
      <AppCard title="Aucune donnée" subtitle="Revenez au formulaire pour créer une demande.">
        <Link to="/demande" className="btn btn-primary" style={{ textDecoration: 'none' }}>
          Nouvelle demande
        </Link>
      </AppCard>
    );
  }

  const copyCode = async () => {
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
    <AppCard
      title="Demande enregistrée"
      subtitle="Conservez le code de suivi — et le téléphone utilisé au dépôt."
    >
      <div className="tracking-code-box">
        <span className="tracking-code-label">Code de suivi</span>
        <strong className="tracking-code-value">{result.codeSuivie}</strong>
        <AppButton type="button" variant="secondary" size="sm" onClick={copyCode}>
          {copied ? 'Code copié' : 'Copier le code'}
        </AppButton>
      </div>

      <div className="demande-confirm-tips" role="note">
        <p>
          <strong>Deux façons de retrouver votre demande</strong>
        </p>
        <ul>
          <li>avec ce <strong>code de suivi</strong> ;</li>
          <li>
            avec le <strong>numéro de téléphone</strong> saisi lors du dépôt, sur la page{' '}
            <Link to="/suivi">Suivi</Link>.
          </li>
        </ul>
      </div>

      <p className="muted" style={{ margin: '12px 0 0' }}>
        Consultez d’abord l’aperçu du reçu PDF (une page A4), puis téléchargez-le.
        Les statuts (demande, validation, paiement) y figurent.
      </p>
      <div className="button-row" style={{ marginTop: 16 }}>
        <AppButton type="button" onClick={() => goToTrackingResult(navigate, result.codeSuivie)}>
          Suivre la demande
        </AppButton>
        <AppButton
          type="button"
          variant="secondary"
          onClick={() => goToInvoice(navigate, result.codeSuivie)}
        >
          Voir la facture
        </AppButton>
        <AppButton type="button" onClick={() => goToPayment(navigate, result.codeSuivie)}>
          Payer maintenant
        </AppButton>
        <ReceiptPreviewButton codeSuivie={result.codeSuivie} />
      </div>
    </AppCard>
  );
}
