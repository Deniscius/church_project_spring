import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AppDialog from '../ui/AppDialog';
import AppButton from '../ui/AppButton';
import { useToast } from '../../contexts/toast.context';
import { copyText } from '../../utils/clipboard';
import { goToPayment, goToTrackingResult } from '../../utils/sensitiveNav';

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
  const navigate = useNavigate();
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
        Conservez précieusement ce numéro de suivi. Vous pourrez aussi retrouver
        votre demande avec le <strong>téléphone</strong> utilisé lors du dépôt
        (page Suivi).
      </p>
      <div className="tracking-code-box" role="status">
        <span className="tracking-code-label">Numéro de suivi</span>
        <strong className="tracking-code-value">{codeSuivie}</strong>
        <AppButton type="button" variant="secondary" onClick={copyCode}>
          {copied ? 'Code copié' : 'Copier le code de suivi'}
        </AppButton>
      </div>
      <p className="muted" style={{ marginTop: 12 }}>
        Astuce : notez le code <em>et</em> gardez le même numéro de téléphone pour
        retrouver facilement vos intentions plus tard.
      </p>
      <div className="button-row tracking-code-links">
        <AppButton
          type="button"
          variant="secondary"
          onClick={() => {
            onLeave?.();
            goToTrackingResult(navigate, codeSuivie);
          }}
        >
          Suivre la demande
        </AppButton>
        <AppButton
          type="button"
          onClick={() => {
            onLeave?.();
            goToPayment(navigate, codeSuivie);
          }}
        >
          Payer maintenant
        </AppButton>
      </div>
    </AppDialog>
  );
}
