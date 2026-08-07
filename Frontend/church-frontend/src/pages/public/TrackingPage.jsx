import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import PageHeader from '../../components/ui/PageHeader';
import TrackingCodeForm from '../../components/public/TrackingCodeForm';
import AppCard from '../../components/ui/AppCard';
import AppInput from '../../components/ui/AppInput';
import AppButton from '../../components/ui/AppButton';
import { normalizeTrackingCode } from '../../utils/trackingCode';
import { FieldLabel } from '../../components/ui/HelpTip';
import StepHelpBanner from '../../components/ui/StepHelpBanner';
import { HELP } from '../../constants/helpTips';

export default function TrackingPage() {
  const navigate = useNavigate();
  const [code, setCode] = useState('');

  const submit = (e) => {
    e.preventDefault();
    const c = normalizeTrackingCode(code);
    if (!c) return;
    navigate(`/suivi/resultat?code=${encodeURIComponent(c)}`);
  };

  return (
    <div className="stack public-page">
      <PageHeader
        title="Suivre une demande"
        subtitle="Saisissez le code reçu lors du dépôt — sans créer de compte."
      />
      <StepHelpBanner title="Code de suivi" text={HELP.demande.codeSuivi} />
      <div className="grid-2 tracking-page-layout">
        <AppCard title="Code de suivi" subtitle="Celui figurant sur votre confirmation ou reçu.">
          <form onSubmit={submit}>
            <div className="form-field">
              <FieldLabel htmlFor="track-code" help={HELP.demande.codeSuivi}>
                Code de suivi
              </FieldLabel>
              <AppInput
                id="track-code"
                value={code}
                autoComplete="off"
                autoCapitalize="characters"
                spellCheck={false}
                inputMode="text"
                onChange={(e) => setCode(e.target.value.toUpperCase())}
                placeholder="Votre code de suivi"
                className="tracking-code-input"
              />
              <small className="muted">
                Majuscules ou minuscules acceptées.
              </small>
            </div>
            <div className="button-row" style={{ marginTop: 16 }}>
              <AppButton type="submit">Consulter</AppButton>
            </div>
          </form>
        </AppCard>
        <TrackingCodeForm />
      </div>
    </div>
  );
}
