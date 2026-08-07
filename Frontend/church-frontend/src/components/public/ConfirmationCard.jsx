import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { getApiBaseUrl } from '../../config/apiBaseUrl';
import AppCard from '../ui/AppCard';
import AppButton from '../ui/AppButton';
import { useToast } from '../../contexts/toast.context';
import { copyText } from '../../utils/clipboard';

export default function ConfirmationCard({ result }) {
  const toast = useToast();
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

  const apiBaseUrl = getApiBaseUrl();
  const receiptUrl = `${apiBaseUrl}/demandes/code/${encodeURIComponent(result.codeSuivie)}/recu.pdf`;

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
      subtitle="Conservez le code de suivi : il sert pour le suivi, la facture et le paiement."
    >
      <div className="tracking-code-box">
        <span className="tracking-code-label">Code de suivi</span>
        <strong className="tracking-code-value">{result.codeSuivie}</strong>
        <AppButton type="button" variant="secondary" size="sm" onClick={copyCode}>
          {copied ? 'Code copié' : 'Copier le code'}
        </AppButton>
      </div>
      <p className="muted" style={{ margin: '12px 0 0' }}>
        Conservez le reçu PDF (une page A4) : il contient le code de suivi et le détail du dépôt.
        Les statuts (demande, validation, paiement) y figurent.
      </p>
      <div className="button-row" style={{ marginTop: 16 }}>
        <Link
          to={`/suivi/resultat?code=${encodeURIComponent(result.codeSuivie)}`}
          className="btn btn-primary"
          style={{ textDecoration: 'none' }}
        >
          Suivre la demande
        </Link>
        <Link
          to={`/facture/${encodeURIComponent(result.codeSuivie)}`}
          className="btn btn-secondary"
          style={{ textDecoration: 'none' }}
        >
          Voir la facture
        </Link>
        <Link
          to={`/paiement/${encodeURIComponent(result.codeSuivie)}`}
          className="btn btn-primary"
          style={{ textDecoration: 'none' }}
        >
          Payer maintenant
        </Link>
        <a
          href={receiptUrl}
          className="btn btn-secondary"
          style={{ textDecoration: 'none' }}
        >
          Télécharger le reçu
        </a>
      </div>
    </AppCard>
  );
}
