import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import PageHeader from '../../components/ui/PageHeader';
import RequestSummaryCard from '../../components/public/RequestSummaryCard';
import TrackingSuccessDialog from '../../components/public/TrackingSuccessDialog';
import AppCard from '../../components/ui/AppCard';
import FormError from '../../components/ui/FormError';
import AppButton from '../../components/ui/AppButton';
import { usePublicDemandeDraft } from '../../contexts/publicDemandeDraft.context';
import { useToast } from '../../contexts/toast.context';
import { useScrollToError } from '../../hooks/useScrollToError';
import { requestService } from '../../services/request.service';
import {
  buildDemandeRequestBody,
  persistDemandeCreationResult,
  validatePublicDemandeDraft,
} from '../../utils/publicDemandeValidation';
import { formatCurrency } from '../../utils/formatCurrency';
import { useMutation } from '@tanstack/react-query';

export default function RequestRecapPage() {
  const navigate = useNavigate();
  const toast = useToast();
  const { draft, reset } = usePublicDemandeDraft();
  const [errors, setErrors] = useState([]);
  const [successResult, setSuccessResult] = useState(null);
  const errorRef = useScrollToError(errors.length ? errors.join('|') : null);

  useEffect(() => {
    const { ok } = validatePublicDemandeDraft(draft);
    if (!ok && !successResult) {
      navigate('/demande', { replace: true });
    }
  }, [draft, navigate, successResult]);

  const goToConfirmation = (result) => {
    reset();
    navigate('/demande/confirmation', { replace: true, state: result });
  };

  const mutation = useMutation({
    mutationFn: (body) => requestService.create(body),
    onSuccess: (data) => {
      const result = {
        codeSuivie: data.codeSuivie,
        statutDemande: data.statutDemande,
        statutPaiement: data.statutPaiement,
        statutValidation: data.statutValidation,
        refFacture: data.refFacture,
        montant: data.montant,
      };
      persistDemandeCreationResult(result);
      setSuccessResult(result);
      toast.success('Demande enregistrée. Conservez votre code de suivi.');
    },
    onError: (err) => {
      const message = err instanceof Error ? err.message : 'Échec de la création';
      setErrors([message]);
      toast.error(message);
    },
  });

  const submit = () => {
    const { ok, errors: v } = validatePublicDemandeDraft(draft);
    if (!ok) {
      setErrors(v);
      toast.error(v.length === 1 ? v[0] : `${v.length} points à corriger avant envoi.`);
      return;
    }
    setErrors([]);
    mutation.mutate(buildDemandeRequestBody(draft));
  };

  return (
    <div className="stack public-page">
      <PageHeader
        title="Récapitulatif de la demande"
        subtitle="Relisez les informations avant envoi définitif."
      />
      <FormError error={errors} errorRef={errorRef} />
      <div className="grid-2">
        <RequestSummaryCard mode="full" />
        <AppCard title="Validation" subtitle="Dernière étape avant enregistrement.">
          <div className="info-list">
            <div className="info-row">
              <span>Paroisse</span>
              <span>{draft.paroisseNom || '—'}</span>
            </div>
            <div className="info-row">
              <span>Type</span>
              <span>{draft.typeDemandeLibelle || '—'}</span>
            </div>
            <div className="info-row">
              <span>Montant</span>
              <span>
                {draft.forfaitMontant != null
                  ? formatCurrency(Number(draft.forfaitMontant))
                  : '—'}
              </span>
            </div>
          </div>
          <div className="button-row" style={{ marginTop: 20 }}>
            <AppButton type="button" onClick={submit} loading={mutation.isPending}>
              {mutation.isPending ? 'Envoi…' : 'Confirmer la demande'}
            </AppButton>
            <AppButton
              type="button"
              variant="secondary"
              disabled={mutation.isPending || Boolean(successResult)}
              onClick={() => {
                toast.info('Vous pouvez corriger le formulaire, vos saisies sont conservées.');
                navigate('/demande');
              }}
            >
              Corriger le formulaire
            </AppButton>
          </div>
        </AppCard>
      </div>

      <TrackingSuccessDialog
        open={Boolean(successResult?.codeSuivie)}
        codeSuivie={successResult?.codeSuivie}
        onClose={() => successResult && goToConfirmation(successResult)}
        onContinue={() => successResult && goToConfirmation(successResult)}
        onLeave={() => reset()}
      />
    </div>
  );
}
