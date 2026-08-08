import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import PageHeader from '../../components/ui/PageHeader';
import TrackingCodeForm from '../../components/public/TrackingCodeForm';
import AppCard from '../../components/ui/AppCard';
import AppInput from '../../components/ui/AppInput';
import AppButton from '../../components/ui/AppButton';
import PhoneField from '../../components/ui/PhoneField';
import { normalizeTrackingCode } from '../../utils/trackingCode';
import { goToTrackingResult } from '../../utils/sensitiveNav';
import { FieldLabel } from '../../components/ui/HelpTip';
import StepHelpBanner from '../../components/ui/StepHelpBanner';
import { HELP } from '../../constants/helpTips';
import { DEFAULT_PHONE_COUNTRY_ISO, toE164 } from '../../utils/phone';
import { requestService } from '../../services/request.service';

export default function TrackingPage() {
  const navigate = useNavigate();
  const [code, setCode] = useState('');
  const [telCountryIso, setTelCountryIso] = useState(DEFAULT_PHONE_COUNTRY_ISO);
  const [telNational, setTelNational] = useState('');
  const [phoneBusy, setPhoneBusy] = useState(false);
  const [phoneError, setPhoneError] = useState('');
  const [phoneCodes, setPhoneCodes] = useState([]);

  const submit = (e) => {
    e.preventDefault();
    const c = normalizeTrackingCode(code);
    if (!c) return;
    goToTrackingResult(navigate, c);
  };

  const lookupPhone = async (e) => {
    e.preventDefault();
    setPhoneError('');
    setPhoneCodes([]);
    const e164 = toE164(telCountryIso, telNational);
    if (!e164) {
      setPhoneError('Indiquez un numéro de téléphone valide.');
      return;
    }
    setPhoneBusy(true);
    try {
      const res = await requestService.lookupByPhone(e164);
      const codes = Array.isArray(res?.codes) ? res.codes : [];
      setPhoneCodes(codes);
      if (codes.length === 1) {
        goToTrackingResult(navigate, codes[0]);
        return;
      }
      if (codes.length === 0) {
        setPhoneError('Aucune demande trouvée pour ce numéro (ou numéro inconnu).');
      }
    } catch (err) {
      setPhoneError(err instanceof Error ? err.message : 'Recherche impossible');
    } finally {
      setPhoneBusy(false);
    }
  };

  return (
    <div className="stack public-page">
      <PageHeader
        title="Suivre une demande"
        subtitle="Code de suivi ou numéro de téléphone utilisé lors du dépôt."
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
              <small className="muted">Majuscules ou minuscules acceptées.</small>
            </div>
            <div className="button-row" style={{ marginTop: 16 }}>
              <AppButton type="submit">Consulter</AppButton>
            </div>
          </form>
        </AppCard>

        <AppCard
          title="Par téléphone"
          subtitle="Retrouvez le(s) code(s) liés au numéro saisi lors de la demande."
        >
          <form onSubmit={lookupPhone}>
            <div className="form-field">
              <FieldLabel htmlFor="track-phone-national" help={HELP.demande.telephone} required>
                Téléphone
              </FieldLabel>
              <PhoneField
                id="track-phone"
                required
                countryIso={telCountryIso}
                national={telNational}
                onChange={({ countryIso: iso, national }) => {
                  setTelCountryIso(iso);
                  setTelNational(national);
                }}
              />
            </div>
            {phoneError ? <p className="text-red-600" role="alert">{phoneError}</p> : null}
            {phoneCodes.length > 1 ? (
              <div className="stack" style={{ gap: 8, marginTop: 12 }}>
                <p className="muted" style={{ margin: 0 }}>
                  Plusieurs demandes trouvées — choisissez un code :
                </p>
                <ul className="tracking-phone-codes">
                  {phoneCodes.map((c) => (
                    <li key={c}>
                      <AppButton
                        type="button"
                        variant="secondary"
                        size="sm"
                        onClick={() => goToTrackingResult(navigate, c)}
                      >
                        {c}
                      </AppButton>
                    </li>
                  ))}
                </ul>
              </div>
            ) : null}
            <div className="button-row" style={{ marginTop: 16 }}>
              <AppButton type="submit" loading={phoneBusy} disabled={phoneBusy}>
                {phoneBusy ? 'Recherche…' : 'Rechercher'}
              </AppButton>
            </div>
          </form>
        </AppCard>

        <TrackingCodeForm />
      </div>
    </div>
  );
}
