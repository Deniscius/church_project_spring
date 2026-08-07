import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import PageHeader from '../../components/ui/PageHeader';
import FormStepper from '../../components/ui/FormStepper';
import FormError from '../../components/ui/FormError';
import ApplicantForm from '../../components/public/ApplicantForm';
import CelebrationChoiceForm from '../../components/public/CelebrationChoiceForm';
import RequestSummaryCard from '../../components/public/RequestSummaryCard';
import ScheduleSelector from '../../components/public/ScheduleSelector';
import DatesSelector from '../../components/public/DatesSelector';
import PaymentTypeSelector from '../../components/public/PaymentTypeSelector';
import AppButton from '../../components/ui/AppButton';
import { usePublicDemandeDraft } from '../../contexts/publicDemandeDraft.context';
import { useToast } from '../../contexts/toast.context';
import { useScrollToError } from '../../hooks/useScrollToError';
import { validatePublicDemandeStep } from '../../utils/publicDemandeValidation';
import { isMultiCelebrationForfait } from '../../constants/enums';
import { formatCurrency } from '../../utils/formatCurrency';

const STEPS = [
  { id: 'identity', label: 'Intention' },
  { id: 'parish', label: 'Paroisse' },
  { id: 'schedule', label: 'Date' },
  { id: 'payment', label: 'Paiement' },
];

export default function NewRequestPage() {
  const navigate = useNavigate();
  const toast = useToast();
  const { draft, reset } = usePublicDemandeDraft();
  const multi = isMultiCelebrationForfait(draft.forfaitNombreCelebration);
  const [step, setStep] = useState(1);
  const [errors, setErrors] = useState([]);
  const errorRef = useScrollToError(errors.length ? errors.join('|') : null);

  const goNext = () => {
    const { ok, errors: v } = validatePublicDemandeStep(step, draft);
    if (!ok) {
      setErrors(v);
      toast.error(v.length === 1 ? v[0] : `${v.length} points à corriger.`);
      return;
    }
    setErrors([]);
    if (step < STEPS.length) {
      setStep((s) => s + 1);
      window.scrollTo({ top: 0, behavior: 'smooth' });
      return;
    }
    navigate('/demande/recapitulatif');
  };

  const goPrev = () => {
    setErrors([]);
    setStep((s) => Math.max(1, s - 1));
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const resetForm = () => {
    reset();
    setStep(1);
    setErrors([]);
    toast.info('Formulaire réinitialisé.');
  };

  const amountLabel = draft.forfaitMontant != null
    ? formatCurrency(Number(draft.forfaitMontant))
    : null;

  return (
    <div className="stack public-page demande-page">
      <PageHeader
        title="Déposer une intention de messe"
        subtitle="Simple, sans compte — 4 étapes courtes."
      />

      <FormStepper
        steps={STEPS}
        currentStep={step}
        onStepClick={(n) => {
          if (n <= step) {
            setErrors([]);
            setStep(n);
          }
        }}
      />

      <FormError error={errors} errorRef={errorRef} />

      <div className="grid-2 public-demande-layout">
        <div className="stack public-demande-main">
          {step === 1 ? <ApplicantForm /> : null}
          {step === 2 ? <CelebrationChoiceForm /> : null}
          {step === 3 ? (
            multi ? (
              <DatesSelector />
            ) : (
              <>
                <DatesSelector />
                <ScheduleSelector />
              </>
            )
          ) : null}
          {step === 4 ? <PaymentTypeSelector /> : null}
        </div>

        <aside className="stack public-demande-aside">
          <div className="demande-summary-desktop">
            <RequestSummaryCard step={step} />
          </div>

          <div className="demande-step-actions card">
            <div className="demande-step-actions-head">
              <strong>
                Étape {step}/{STEPS.length}
              </strong>
              <span className="muted">{STEPS[step - 1]?.label}</span>
              {amountLabel && step >= 2 ? (
                <span className="demande-step-amount">{amountLabel}</span>
              ) : null}
            </div>
            <div className="button-row public-step-actions">
              {step > 1 ? (
                <AppButton type="button" variant="secondary" onClick={goPrev}>
                  Retour
                </AppButton>
              ) : null}
              <AppButton type="button" onClick={goNext}>
                {step < STEPS.length ? 'Continuer' : 'Voir le récapitulatif'}
              </AppButton>
            </div>
            <button type="button" className="demande-reset-link" onClick={resetForm}>
              Recommencer
            </button>
          </div>
        </aside>
      </div>
    </div>
  );
}
