import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import PageHeader from '../../components/ui/PageHeader';
import AppCard from '../../components/ui/AppCard';
import AppInput from '../../components/ui/AppInput';
import AppButton from '../../components/ui/AppButton';
import AppBadge from '../../components/ui/AppBadge';
import PhoneField from '../../components/ui/PhoneField';
import { normalizeTrackingCode } from '../../utils/trackingCode';
import { goToTrackingResult } from '../../utils/sensitiveNav';
import { FieldLabel } from '../../components/ui/HelpTip';
import StepHelpBanner from '../../components/ui/StepHelpBanner';
import { HELP } from '../../constants/helpTips';
import { DEFAULT_PHONE_COUNTRY_ISO, toE164 } from '../../utils/phone';
import { requestService } from '../../services/request.service';
import { formatDate, formatDateShort } from '../../utils/formatDate';

function normalizePhoneResults(response) {
  const demandes = Array.isArray(response?.demandes)
    ? response.demandes.filter((item) => item?.codeSuivie)
    : [];

  if (demandes.length) {
    return [...demandes].sort((a, b) => {
      const left = String(a.createdAt || '');
      const right = String(b.createdAt || '');
      return left.localeCompare(right);
    });
  }

  // Compatibilité avec une ancienne réponse backend ne contenant que les codes.
  const codes = Array.isArray(response?.codes)
    ? response.codes.filter(Boolean)
    : [];
  return codes.map((codeSuivie) => ({ codeSuivie }));
}

function celebrationLabel(dates) {
  if (!Array.isArray(dates) || !dates.length) return '';
  const formatted = dates.map(formatDateShort).filter(Boolean);
  if (!formatted.length) return '';
  if (formatted.length <= 3) return formatted.join(' · ');
  return `${formatted.slice(0, 3).join(' · ')} · +${formatted.length - 3}`;
}

export default function TrackingPage() {
  const navigate = useNavigate();
  const [code, setCode] = useState('');
  const [telCountryIso, setTelCountryIso] = useState(DEFAULT_PHONE_COUNTRY_ISO);
  const [telNational, setTelNational] = useState('');
  const [phoneBusy, setPhoneBusy] = useState(false);
  const [phoneError, setPhoneError] = useState('');
  const [phoneRequests, setPhoneRequests] = useState([]);
  const [otpSent, setOtpSent] = useState(false);
  const [otpCode, setOtpCode] = useState('');
  const [emailMasked, setEmailMasked] = useState('');
  const [pendingPhone, setPendingPhone] = useState('');

  const submit = (e) => {
    e.preventDefault();
    const c = normalizeTrackingCode(code);
    if (!c) return;
    goToTrackingResult(navigate, c);
  };

  const showPhoneResults = (response) => {
    const requests = normalizePhoneResults(response);
    setPhoneRequests(requests);
    setOtpSent(false);
    setOtpCode('');
    if (!requests.length) {
      setPhoneError('Aucune demande trouvée pour ce numéro.');
    }
  };

  const requestPhoneOtp = async (e) => {
    e.preventDefault();
    setPhoneError('');
    setPhoneRequests([]);
    setOtpSent(false);
    setOtpCode('');
    const e164 = toE164(telCountryIso, telNational);
    if (!e164) {
      setPhoneError('Indiquez un numéro de téléphone valide.');
      return;
    }
    setPhoneBusy(true);
    try {
      const res = await requestService.lookupByPhone(e164);
      const directResults = normalizePhoneResults(res);
      setPendingPhone(e164);

      // Sans e-mail au dépôt : afficher immédiatement toutes les demandes.
      if (directResults.length > 0 && !res?.emailMasked) {
        setEmailMasked('');
        showPhoneResults(res);
        return;
      }

      setEmailMasked(res?.emailMasked || '');
      setOtpSent(true);
    } catch (err) {
      setPhoneError(err instanceof Error ? err.message : 'Recherche impossible');
    } finally {
      setPhoneBusy(false);
    }
  };

  const verifyPhoneOtp = async (e) => {
    e.preventDefault();
    setPhoneError('');
    if (!pendingPhone || !otpCode.trim()) {
      setPhoneError('Saisissez le code reçu par e-mail.');
      return;
    }
    setPhoneBusy(true);
    try {
      const res = await requestService.verifyPhoneLookup(pendingPhone, otpCode.trim());
      showPhoneResults(res);
    } catch (err) {
      setPhoneError(err instanceof Error ? err.message : 'Code incorrect');
    } finally {
      setPhoneBusy(false);
    }
  };

  return (
    <div className="stack public-page">
      <PageHeader
        title="Retrouver mes demandes"
        subtitle="Utilisez votre code de suivi ou le téléphone saisi lors du dépôt."
      />
      <StepHelpBanner
        title="Deux façons de retrouver une demande"
        text="Avec un code, vous ouvrez directement la demande. Avec votre téléphone, Missanye affiche toutes les demandes associées à ce numéro, dans l’ordre chronologique."
      />
      <div className="grid-2 tracking-page-layout">
        <AppCard title="Avec mon code de suivi" subtitle="Le code figurant sur votre confirmation ou votre reçu.">
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
                placeholder="Ex. DEM-XXXX"
                className="tracking-code-input"
              />
              <small className="muted">Majuscules ou minuscules acceptées.</small>
            </div>
            <div className="button-row" style={{ marginTop: 16 }}>
              <AppButton type="submit">Voir ma demande</AppButton>
            </div>
          </form>
        </AppCard>

        <AppCard
          title="Avec mon numéro de téléphone"
          subtitle="Toutes les demandes faites avec ce numéro seront regroupées ici."
        >
          <form onSubmit={otpSent ? verifyPhoneOtp : requestPhoneOtp}>
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
                  setOtpSent(false);
                  setOtpCode('');
                  setPhoneRequests([]);
                  setPhoneError('');
                }}
              />
            </div>
            {otpSent ? (
              <div className="form-field" style={{ marginTop: 12 }}>
                <FieldLabel htmlFor="track-phone-otp" required>
                  Code reçu par e-mail
                </FieldLabel>
                <p className="muted text-sm" style={{ marginTop: 0 }}>
                  Envoyé à {emailMasked || 'votre adresse e-mail'}.
                </p>
                <AppInput
                  id="track-phone-otp"
                  value={otpCode}
                  onChange={(e) => setOtpCode(e.target.value.replace(/\D/g, '').slice(0, 8))}
                  inputMode="numeric"
                  autoComplete="one-time-code"
                  placeholder="6 chiffres"
                  maxLength={8}
                />
              </div>
            ) : null}
            {phoneError ? <p className="text-red-600" role="alert">{phoneError}</p> : null}
            <div className="button-row" style={{ marginTop: 16 }}>
              <AppButton type="submit" loading={phoneBusy} disabled={phoneBusy}>
                {otpSent ? 'Afficher mes demandes' : 'Rechercher mes demandes'}
              </AppButton>
              {otpSent ? (
                <AppButton
                  type="button"
                  variant="secondary"
                  disabled={phoneBusy}
                  onClick={requestPhoneOtp}
                >
                  Renvoyer le code
                </AppButton>
              ) : null}
            </div>
          </form>

          {phoneRequests.length ? (
            <div className="stack" style={{ gap: 10, marginTop: 20 }} role="region" aria-live="polite">
              <div>
                <strong>
                  {phoneRequests.length} demande{phoneRequests.length > 1 ? 's' : ''} trouvée{phoneRequests.length > 1 ? 's' : ''}
                </strong>
                <p className="muted" style={{ margin: '4px 0 0' }}>
                  De la plus ancienne à la plus récente.
                </p>
              </div>

              <ol className="stack" style={{ gap: 10, margin: 0, padding: 0, listStyle: 'none' }}>
                {phoneRequests.map((item, index) => {
                  const dates = celebrationLabel(item.datesCelebration);
                  return (
                    <li key={item.codeSuivie} className="formule-summary-box">
                      <div className="stack" style={{ gap: 6 }}>
                        <div className="button-row" style={{ justifyContent: 'space-between', alignItems: 'center' }}>
                          <strong>{index + 1}. {item.codeSuivie}</strong>
                          {item.createdAt ? <span className="muted">{formatDate(item.createdAt)}</span> : null}
                        </div>
                        {item.paroisseNom || item.typeDemandeLibelle ? (
                          <p style={{ margin: 0 }}>
                            {[item.paroisseNom, item.typeDemandeLibelle].filter(Boolean).join(' · ')}
                          </p>
                        ) : null}
                        {dates ? (
                          <small className="muted">Célébration : {dates}</small>
                        ) : null}
                        {item.statutDemande || item.statutPaiement ? (
                          <div className="button-row" style={{ gap: 6 }}>
                            {item.statutDemande ? <AppBadge value={item.statutDemande} /> : null}
                            {item.statutPaiement ? <AppBadge value={item.statutPaiement} /> : null}
                          </div>
                        ) : null}
                        <div className="button-row">
                          <AppButton
                            type="button"
                            variant="secondary"
                            size="sm"
                            onClick={() => goToTrackingResult(navigate, item.codeSuivie)}
                          >
                            Voir cette demande
                          </AppButton>
                        </div>
                      </div>
                    </li>
                  );
                })}
              </ol>
            </div>
          ) : null}
        </AppCard>
      </div>
    </div>
  );
}
