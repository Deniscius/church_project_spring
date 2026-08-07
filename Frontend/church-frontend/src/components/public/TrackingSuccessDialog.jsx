import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import AppDialog from '../ui/AppDialog';
import AppButton from '../ui/AppButton';
import { useToast } from '../../contexts/toast.context';
import { copyText } from '../../utils/clipboard';

/**
 * Modal post-création : met en avant le numéro de suivi du fidèle.
 */
export default function TrackingSuccessDialog({
  open,
  codeSuivie,
  onClose,
  onContinue,
  onLeave,
}) {
  const toast = useToast();
  const [copied, setCopied] = useState(false);

  const copyCode = async () => {
    if (!codeSuivie) return;
    const ok = await copyText(codeSuivie);
    if (ok) {
      setCopied(true);
      toast.success('Code de suivi copié dans le presse-papiers.');
      window.setTimeout(() => setCopied(false), 2500);
    } else {
      toast.error('Impossible de copier le code. Notez-le manuellement.');
    }
  };

  return (
    <AppDialog
      open={open}
      title="Demande enregistrée"
      variant="success"
      size="lg"
      hideCancel
      confirmLabel="Voir la confirmation"
      onConfirm={onContinue}
      onCancel={onClose}
    >
      <p className="dialog-lead">
        Conservez précieusement ce numéro de suivi. Utilisez « Copier le code de suivi »
        pour ne pas le perdre.
      </p>
      <div className="tracking-code-box" role="status">
        <span className="tracking-code-label">Numéro de suivi</span>
        <strong className="tracking-code-value">{codeSuivie}</strong>
        <AppButton type="button" variant="secondary" onClick={copyCode}>
          {copied ? 'Code copié' : 'Copier le code de suivi'}
        </AppButton>
      </div>
      <div className="button-row tracking-code-links">
        <Link
          to={`/suivi/resultat?code=${encodeURIComponent(codeSuivie || '')}`}
          className="btn btn-secondary"
          style={{ textDecoration: 'none' }}
          onClick={() => onLeave?.()}
        >
          Suivre la demande
        </Link>
        <Link
          to={`/paiement/${encodeURIComponent(codeSuivie || '')}`}
          className="btn btn-primary"
          style={{ textDecoration: 'none' }}
          onClick={() => onLeave?.()}
        >
          Payer maintenant
        </Link>
      </div>
    </AppDialog>
  );
}
