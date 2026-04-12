import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import PageHeader from '../../components/ui/PageHeader';
import AppCard from '../../components/ui/AppCard';
import ApplicantForm from '../../components/public/ApplicantForm';
import RequestSummaryCard from '../../components/public/RequestSummaryCard';
import ParishSelector from '../../components/public/ParishSelector';
import RequestTypeSelector from '../../components/public/RequestTypeSelector';
import PricingSelector from '../../components/public/PricingSelector';
import ScheduleSelector from '../../components/public/ScheduleSelector';
import DatesSelector from '../../components/public/DatesSelector';
import PaymentTypeSelector from '../../components/public/PaymentTypeSelector';
import AppButton from '../../components/ui/AppButton';
import { usePublicDemandeDraft } from '../../contexts/publicDemandeDraft.context';
import { validatePublicDemandeDraft } from '../../utils/publicDemandeValidation';

export default function NewRequestPage() {
  const navigate = useNavigate();
  const { draft, reset } = usePublicDemandeDraft();
  const [errors, setErrors] = useState([]);

  const goRecap = (e) => {
    e.preventDefault();
    const { ok, errors: v } = validatePublicDemandeDraft(draft);
    if (!ok) {
      setErrors(v);
      return;
    }
    setErrors([]);
    navigate('/demande/recapitulatif');
  };

  return (
    <div className="stack">
      <PageHeader
        title="Nouvelle demande"
        subtitle="Formulaire guidé connecté aux référentiels du backend (sans compte)."
      />
      {errors.length > 0 ? (
        <div className="card" style={{ borderColor: 'var(--danger, #b91c1c)' }}>
          <strong>À corriger</strong>
          <ul style={{ margin: '8px 0 0 18px' }}>
            {errors.map((msg) => (
              <li key={msg}>{msg}</li>
            ))}
          </ul>
        </div>
      ) : null}
      <form onSubmit={goRecap}>
        <div className="grid-2">
          <div className="stack">
            <ApplicantForm />
            <ParishSelector />
            <RequestTypeSelector />
            <PricingSelector />
            <ScheduleSelector />
            <DatesSelector />
            <PaymentTypeSelector />
          </div>
          <div className="stack">
            <RequestSummaryCard />
            <AppCard title="Actions" subtitle="Vérification locale puis récapitulatif.">
              <div className="button-row">
                <AppButton type="submit">Continuer vers le récapitulatif</AppButton>
                <AppButton
                  type="button"
                  variant="secondary"
                  onClick={() => {
                    reset();
                    setErrors([]);
                  }}
                >
                  Réinitialiser
                </AppButton>
              </div>
            </AppCard>
          </div>
        </div>
      </form>
    </div>
  );
}
