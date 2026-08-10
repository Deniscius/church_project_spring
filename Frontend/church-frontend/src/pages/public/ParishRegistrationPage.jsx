import React, { useMemo, useState, useTransition } from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import PageHeader from '../../components/ui/PageHeader';
import AppAlert from '../../components/ui/AppAlert';
import AppButton from '../../components/ui/AppButton';
import AppInput from '../../components/ui/AppInput';
import AppSelect from '../../components/ui/AppSelect';
import PhoneField from '../../components/ui/PhoneField';
import { validateOptionalPhone } from '../../utils/phone';
import { FieldLabel } from '../../components/ui/HelpTip';
import StepHelpBanner from '../../components/ui/StepHelpBanner';
import { inscriptionService } from '../../services/inscription.service';
import { apiClient } from '../../services/http/apiClient';
import { formatCurrency } from '../../utils/formatCurrency';
import { HELP } from '../../constants/helpTips';
import { sanitizePersonNameInput, personNameError } from '../../utils/personName';
import { DEFAULT_PHONE_COUNTRY_ISO } from '../../utils/phone';

const PLANS = [
  {
    value: 'MENSUEL',
    name: 'Mensuel',
    price: 5000,
    period: '/ mois',
    hint: 'Souplesse, sans engagement long',
  },
  {
    value: 'SEMESTRIEL',
    name: '6 mois',
    price: 8000,
    period: '/ 6 mois',
    hint: 'Le meilleur équilibre prix / durée',
    featured: true,
  },
  {
    value: 'ANNUEL',
    name: 'Annuel',
    price: 12000,
    period: '/ an',
    hint: 'Tarif le plus avantageux',
  },
];

const STEPS = [
  { id: 1, label: 'Paroisse', short: 'Paroisse' },
  { id: 2, label: 'Formule', short: 'Formule' },
  { id: 3, label: 'Administrateur', short: 'Admin' },
  { id: 4, label: 'Documents', short: 'Docs' },
  { id: 5, label: 'Envoi', short: 'Envoi' },
];

const ACCEPT_DOCS = 'application/pdf,image/jpeg,image/png,.pdf,.jpg,.jpeg,.png';
const MAX_DOC_BYTES = 5 * 1024 * 1024;

const emptyForm = {
  nomParoisse: '',
  adresse: '',
  email: '',
  telephone: '',
  telephoneCountryIso: DEFAULT_PHONE_COUNTRY_ISO,
  telephoneNational: '',
  doyennePublicId: '',
  planAbonnement: 'SEMESTRIEL',
  adminNom: '',
  adminPrenom: '',
  adminEmail: '',
  adminTelephone: '',
  adminTelephoneCountryIso: DEFAULT_PHONE_COUNTRY_ISO,
  adminTelephoneNational: '',
  adminUsername: '',
  otpProof: '',
  message: '',
};

function validateFile(file, label) {
  if (!file) return `${label} requis (scan PDF, JPEG ou PNG).`;
  if (file.size > MAX_DOC_BYTES) return `${label} trop volumineux (max 5 Mo).`;
  const type = (file.type || '').toLowerCase();
  const ok = type.includes('pdf') || type.includes('jpeg') || type.includes('jpg') || type.includes('png');
  if (!ok) return `${label} : formats acceptés PDF, JPEG, PNG.`;
  return null;
}

function formatFileSize(bytes) {
  if (!bytes && bytes !== 0) return '';
  if (bytes < 1024) return `${bytes} o`;
  if (bytes < 1024 * 1024) return `${Math.round(bytes / 1024)} Ko`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} Mo`;
}

function FileDropField({ id, label, help, file, onChange, required }) {
  return (
    <div className="form-field full">
      <FieldLabel htmlFor={id} help={help} required={required}>
        {label}
      </FieldLabel>
      <label className={`onboard-file${file ? ' has-file' : ''}`} htmlFor={id}>
        <input
          id={id}
          type="file"
          accept={ACCEPT_DOCS}
          capture="environment"
          onChange={(e) => onChange(e.target.files?.[0] || null)}
        />
        {file ? (
          <span className="onboard-file-meta">
            <strong>{file.name}</strong>
            <span className="muted text-sm">{formatFileSize(file.size)}</span>
          </span>
        ) : (
          <span className="onboard-file-meta">
            <strong>Choisir un fichier</strong>
            <span className="muted text-sm">PDF, JPEG ou PNG — 5 Mo max</span>
          </span>
        )}
      </label>
    </div>
  );
}

export default function ParishRegistrationPage() {
  const [step, setStep] = useState(1);
  const [form, setForm] = useState(emptyForm);
  const [otpCode, setOtpCode] = useState('');
  const [otpSent, setOtpSent] = useState(false);
  const [otpVerified, setOtpVerified] = useState(false);
  const [mandatCure, setMandatCure] = useState(null);
  const [adminCni, setAdminCni] = useState(null);
  const [busy, setBusy] = useState(false);
  const [otpBusy, setOtpBusy] = useState(false);
  const [error, setError] = useState(null);
  const [done, setDone] = useState(null);
  const [horsAnnuaire, setHorsAnnuaire] = useState(false);
  const [annuairePublicId, setAnnuairePublicId] = useState('');

  const [, startTransition] = useTransition();

  const { data: doyennes = [], isLoading: loadingDoyennes } = useQuery({
    queryKey: ['doyennes', 'public'],
    queryFn: () => apiClient('/doyennes'),
    staleTime: 10 * 60_000,
    select: (data) => (Array.isArray(data) ? data : []),
  });

  const { data: annuaire = [], isLoading: loadingAnnuaire } = useQuery({
    queryKey: ['annuaire', form.doyennePublicId],
    queryFn: () => apiClient(`/paroisses/annuaire/${form.doyennePublicId}`),
    enabled: Boolean(form.doyennePublicId),
    staleTime: 10 * 60_000,
    select: (data) => (Array.isArray(data) ? data : []),
  });

  const doyenneOptions = useMemo(
    () => doyennes.map((d) => ({ value: d.publicId, label: d.nom })),
    [doyennes]
  );

  const annuaireOptions = useMemo(
    () => annuaire.map((p) => ({
      value: p.publicId || p.nom,
      label: p.nom,
    })),
    [annuaire]
  );

  const selectedPlan = useMemo(
    () => PLANS.find((p) => p.value === form.planAbonnement) || PLANS[1],
    [form.planAbonnement]
  );

  const doyenneNom = useMemo(
    () => doyennes.find((d) => d.publicId === form.doyennePublicId)?.nom || '—',
    [doyennes, form.doyennePublicId]
  );

  const progressPct = Math.round((step / STEPS.length) * 100);

  function resetOtpState() {
    setOtpVerified(false);
    setOtpSent(false);
    setOtpCode('');
  }

  function setField(key, value) {
    const resetsOtp = key === 'adminEmail' || key === 'adminNom' || key === 'adminPrenom';
    setForm((prev) => {
      const next = { ...prev, [key]: value };
      if (resetsOtp) {
        next.otpProof = '';
        next.adminUsername = '';
      }
      return next;
    });
    if (resetsOtp) {
      resetOtpState();
    }
  }

  function selectDoyenne(doyennePublicId) {
    setHorsAnnuaire(false);
    setAnnuairePublicId('');
    setForm((prev) => ({ ...prev, doyennePublicId, nomParoisse: '', adresse: '' }));
  }

  function selectParoisseAnnuaire(value) {
    const entree = annuaire.find(
      (p) => p.publicId === value || p.nom === value
    );
    setAnnuairePublicId(entree?.publicId || value || '');
    setForm((prev) => ({
      ...prev,
      nomParoisse: entree?.nom || value || '',
      adresse: (entree?.adresse || '').trim(),
    }));
  }

  function goToStep(target) {
    setError(null);
    startTransition(() => setStep(target));
  }

  function validateStep(current) {
    if (current === 1) {
      if (!form.doyennePublicId) return 'Sélectionnez un doyenné.';
      if (!form.nomParoisse.trim() || form.nomParoisse.trim().length < 2) {
        return 'Indiquez le nom de la paroisse.';
      }
      if (!form.adresse.trim() || form.adresse.trim().length < 5) {
        return 'Indiquez une adresse complète (quartier, rue, ville).';
      }
      if (form.email.trim()) {
        const okEmail = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim());
        if (!okEmail) return 'E-mail de la paroisse invalide.';
      }
      const phone = validateOptionalPhone(form.telephoneCountryIso, form.telephoneNational);
      if (!phone.ok) return phone.message;
    }
    if (current === 2 && !form.planAbonnement) return 'Choisissez une formule.';
    if (current === 3) {
      if (!form.adminNom.trim() || !form.adminPrenom.trim()) {
        return 'Nom et prénom de l’administrateur requis.';
      }
      const nomErr = personNameError(form.adminNom, 'Le nom');
      if (nomErr) return nomErr;
      const prenomErr = personNameError(form.adminPrenom, 'Le prénom');
      if (prenomErr) return prenomErr;
      if (!form.adminEmail.trim()) return 'E-mail personnel requis pour la vérification.';
      const adminPhone = validateOptionalPhone(form.adminTelephoneCountryIso, form.adminTelephoneNational);
      if (!adminPhone.ok) return adminPhone.message;
      if (!otpVerified || !form.otpProof || !form.adminUsername) {
        return 'Validez d’abord votre e-mail avec le code reçu.';
      }
    }
    if (current === 4) {
      return validateFile(mandatCure, 'Mandat du curé')
        || validateFile(adminCni, 'Pièce d’identité de l’administrateur');
    }
    return null;
  }

  async function sendOtp() {
    if (!form.adminEmail.trim()) {
      setError('Indiquez votre e-mail personnel.');
      return;
    }
    if (!form.adminNom.trim() || !form.adminPrenom.trim()) {
      setError('Renseignez d’abord le nom et le prénom.');
      return;
    }
    setOtpBusy(true);
    setError(null);
    try {
      await inscriptionService.sendOtp(form.adminEmail.trim());
      setOtpSent(true);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Envoi du code impossible');
    } finally {
      setOtpBusy(false);
    }
  }

  async function verifyOtp() {
    if (!otpCode.trim()) {
      setError('Saisissez le code reçu par e-mail.');
      return;
    }
    if (!form.nomParoisse.trim()) {
      setError('Indiquez d’abord la paroisse (étape 1) pour générer un identifiant cohérent.');
      return;
    }
    setOtpBusy(true);
    setError(null);
    try {
      const res = await inscriptionService.verifyOtp({
        email: form.adminEmail.trim(),
        code: otpCode.trim(),
        adminPrenom: form.adminPrenom.trim(),
        adminNom: form.adminNom.trim(),
        nomParoisse: form.nomParoisse.trim(),
      });
      setForm((prev) => ({
        ...prev,
        otpProof: res.otpProof,
        adminUsername: res.adminUsername,
        adminEmail: res.email || prev.adminEmail,
      }));
      setOtpVerified(true);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Code incorrect');
    } finally {
      setOtpBusy(false);
    }
  }

  function next() {
    const msg = validateStep(step);
    if (msg) {
      setError(msg);
      return;
    }
    setError(null);
    startTransition(() => setStep((s) => Math.min(5, s + 1)));
  }

  function back() {
    setError(null);
    startTransition(() => setStep((s) => Math.max(1, s - 1)));
  }

  async function onSubmit(e) {
    e.preventDefault();
    const adminMsg = validateStep(3);
    const docsMsg = validateStep(4);
    if (adminMsg) {
      setError(adminMsg);
      setStep(3);
      return;
    }
    if (docsMsg) {
      setError(docsMsg);
      setStep(4);
      return;
    }
    setBusy(true);
    setError(null);
    try {
      const payload = {
        nomParoisse: form.nomParoisse,
        adresse: form.adresse,
        email: form.email || '',
        telephone: form.telephone || '',
        doyennePublicId: form.doyennePublicId,
        planAbonnement: form.planAbonnement,
        adminNom: form.adminNom,
        adminPrenom: form.adminPrenom,
        adminEmail: form.adminEmail,
        adminTelephone: form.adminTelephone || '',
        adminUsername: form.adminUsername,
        otpProof: form.otpProof,
        message: form.message || '',
        membres: [],
      };
      const res = await inscriptionService.soumettre(payload, mandatCure, adminCni);
      setDone(res);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Échec de l’inscription');
    } finally {
      setBusy(false);
    }
  }

  if (done) {
    return (
      <div className="onboard-shell public-page">
        <div className="onboard-success card">
          <p className="onboard-kicker">Dossier reçu</p>
          <h1>Merci, {done.nomParoisse}</h1>
          <p className="muted">
            Votre inscription et vos documents sont en cours de validation par Missanye.
            Conservez les identifiants reçus par e-mail. Après approbation et paiement,
            vous pourrez accéder à l’espace paroisse.
          </p>
          <div className="button-row" style={{ marginTop: 20 }}>
            <Link className="btn btn-primary" to="/">Retour à l’accueil</Link>
            <Link className="btn btn-secondary" to="/admin/login">Se connecter plus tard</Link>
          </div>
        </div>
      </div>
    );
  }

  const currentStep = STEPS[step - 1];

  return (
    <div className="onboard-shell public-page">
      <header className="onboard-hero">
        <p className="onboard-kicker">Inscription paroisse · Missanye</p>
        <h1>Ouvrir l’espace de votre paroisse</h1>
        <p className="muted">
          Cinq étapes courtes. Aucun paiement maintenant — validation puis activation.
        </p>
      </header>

      <div className="onboard-progress" aria-hidden="true">
        <div className="onboard-progress-bar" style={{ width: `${progressPct}%` }} />
      </div>

      <div className="onboard-progress-meta">
        <p className="onboard-progress-label">
          Étape {step}/{STEPS.length}
          <span aria-hidden="true"> — </span>
          <strong>{currentStep?.label}</strong>
        </p>
        <ol className="onboard-steps" aria-label="Étapes d’inscription">
          {STEPS.map((s) => {
            const state = step === s.id ? 'active' : step > s.id ? 'done' : '';
            return (
              <li key={s.id} className={state}>
                <button
                  type="button"
                  className="onboard-step-btn"
                  disabled={s.id > step}
                  onClick={() => s.id < step && goToStep(s.id)}
                  aria-current={step === s.id ? 'step' : undefined}
                >
                  <span className="onboard-step-index">{step > s.id ? '✓' : s.id}</span>
                  <span className="onboard-step-label">{s.short}</span>
                </button>
              </li>
            );
          })}
        </ol>
      </div>

      <form
        className="onboard-panel card"
        onSubmit={step === 5 ? onSubmit : (e) => { e.preventDefault(); next(); }}
        noValidate
      >
        {step === 1 ? (
          <section className="stack">
            <PageHeader
              title="Votre paroisse"
              subtitle="Identifiez la paroisse et son adresse de rattachement."
            />
            <div className="form-grid">
              <div className="form-field">
                <FieldLabel htmlFor="reg-doyenne" help={HELP.inscription.doyenne} required>
                  Doyenné
                </FieldLabel>
                <AppSelect
                  id="reg-doyenne"
                  required
                  searchable={doyenneOptions.length > 6}
                  searchPlaceholder="Rechercher un doyenné…"
                  placeholder={loadingDoyennes ? 'Chargement…' : 'Choisir un doyenné'}
                  value={form.doyennePublicId}
                  options={doyenneOptions}
                  disabled={loadingDoyennes}
                  onChange={(id) => selectDoyenne(id)}
                />
              </div>

              <div className="form-field">
                <FieldLabel htmlFor="reg-paroisse" help={HELP.inscription.paroisse} required>
                  Paroisse
                </FieldLabel>
                {horsAnnuaire ? (
                  <AppInput
                    id="reg-paroisse"
                    required
                    value={form.nomParoisse}
                    onChange={(e) => setField('nomParoisse', e.target.value)}
                    placeholder="Nom complet de la paroisse"
                  />
                ) : (
                  <AppSelect
                    id="reg-paroisse"
                    required
                    searchable
                    searchPlaceholder="Tapez le nom de la paroisse…"
                    placeholder={
                      !form.doyennePublicId
                        ? 'Choisissez d’abord un doyenné'
                        : loadingAnnuaire
                          ? 'Chargement…'
                          : 'Saisir ou choisir votre paroisse'
                    }
                    value={annuairePublicId}
                    options={annuaireOptions}
                    disabled={!form.doyennePublicId || loadingAnnuaire}
                    onChange={(id) => selectParoisseAnnuaire(id)}
                  />
                )}
              </div>

              {form.doyennePublicId ? (
                <p className="muted onboard-inline-hint">
                  {horsAnnuaire ? (
                    <>
                      Saisie libre.{' '}
                      <button
                        type="button"
                        className="link-button"
                        onClick={() => {
                          setHorsAnnuaire(false);
                          setAnnuairePublicId('');
                          setForm((prev) => ({ ...prev, nomParoisse: '', adresse: '' }));
                        }}
                      >
                        Revenir à l’annuaire
                      </button>
                    </>
                  ) : (
                    <>
                      Votre paroisse n’apparaît pas ?{' '}
                      <button
                        type="button"
                        className="link-button"
                        onClick={() => {
                          setHorsAnnuaire(true);
                          setAnnuairePublicId('');
                          setForm((prev) => ({ ...prev, nomParoisse: '', adresse: '' }));
                        }}
                      >
                        Saisir son nom manuellement
                      </button>
                    </>
                  )}
                </p>
              ) : null}

              <div className="form-field full">
                <FieldLabel htmlFor="reg-adresse" help={HELP.inscription.adresse} required>
                  Adresse
                </FieldLabel>
                <textarea
                  id="reg-adresse"
                  className="textarea"
                  required
                  rows={3}
                  maxLength={200}
                  autoComplete="street-address"
                  value={form.adresse}
                  onChange={(e) => setField('adresse', e.target.value)}
                  placeholder={
                    !horsAnnuaire && !annuairePublicId
                      ? 'Sélectionnez une paroisse pour préremplir l’adresse'
                      : 'Quartier, rue, ville…'
                  }
                />
                {!horsAnnuaire && annuairePublicId ? (
                  <span className="muted text-sm">
                    Adresse reprise de l’annuaire — vous pouvez la corriger si besoin.
                  </span>
                ) : null}
              </div>

              <div className="form-field">
                <FieldLabel htmlFor="reg-email" help={HELP.inscription.emailParoisse}>
                  E-mail de contact paroisse
                </FieldLabel>
                <AppInput
                  id="reg-email"
                  type="email"
                  autoComplete="email"
                  maxLength={150}
                  value={form.email}
                  onChange={(e) => setField('email', e.target.value)}
                  placeholder="Optionnel — ex. secretariat@paroisse.tg"
                />
                <span className="muted text-sm">
                  Ce n’est pas l’adresse de vérification. Le code OTP part à l’étape suivante
                  vers l’e-mail personnel de l’administrateur.
                </span>
              </div>

              <div className="form-field">
                <FieldLabel htmlFor="reg-tel-national" help={HELP.inscription.telephone}>
                  Téléphone
                </FieldLabel>
                <PhoneField
                  id="reg-tel"
                  countryIso={form.telephoneCountryIso}
                  national={form.telephoneNational}
                  onChange={({ countryIso, national, e164 }) => {
                    setForm((prev) => ({
                      ...prev,
                      telephoneCountryIso: countryIso,
                      telephoneNational: national,
                      telephone: e164,
                    }));
                  }}
                />
              </div>
            </div>
          </section>
        ) : null}

        {step === 2 ? (
          <section className="stack">
            <PageHeader
              title="Formule d’abonnement"
              subtitle="Choisissez la durée. Vous pourrez changer au renouvellement."
            />
            <div className="plan-grid" role="radiogroup" aria-label="Formules">
              {PLANS.map((plan) => {
                const selected = form.planAbonnement === plan.value;
                return (
                  <button
                    key={plan.value}
                    type="button"
                    role="radio"
                    aria-checked={selected}
                    className={`plan-card${selected ? ' selected' : ''}${plan.featured ? ' featured' : ''}`}
                    onClick={() => setField('planAbonnement', plan.value)}
                  >
                    {plan.featured ? <span className="plan-badge">Recommandé</span> : null}
                    <strong className="plan-name">{plan.name}</strong>
                    <div className="plan-price">
                      {formatCurrency(plan.price)}
                      <span>{plan.period}</span>
                    </div>
                    <p className="muted plan-hint">{plan.hint}</p>
                  </button>
                );
              })}
            </div>
          </section>
        ) : null}

        {step === 3 ? (
          <section className="stack">
            <PageHeader
              title="Administrateur"
              subtitle="Compte du premier responsable. L’e-mail personnel est obligatoire : c’est là que part le code de vérification."
            />
            <StepHelpBanner title="À qui part le mail ?" text={HELP.inscription.otp} />

            <div className="onboard-block">
              <h3 className="onboard-block-title">Identité</h3>
              <div className="form-grid">
                <div className="form-field">
                  <FieldLabel htmlFor="reg-admin-nom" required>Nom</FieldLabel>
                  <AppInput
                    id="reg-admin-nom"
                    required
                    autoComplete="family-name"
                    value={form.adminNom}
                    onChange={(e) => setField('adminNom', sanitizePersonNameInput(e.target.value))}
                    disabled={otpVerified}
                  />
                </div>
                <div className="form-field">
                  <FieldLabel htmlFor="reg-admin-prenom" required>Prénom</FieldLabel>
                  <AppInput
                    id="reg-admin-prenom"
                    required
                    autoComplete="given-name"
                    value={form.adminPrenom}
                    onChange={(e) => setField('adminPrenom', sanitizePersonNameInput(e.target.value))}
                    disabled={otpVerified}
                  />
                </div>
                <div className="form-field full">
                  <FieldLabel htmlFor="reg-admin-email" help={HELP.inscription.adminEmail} required>
                    E-mail personnel (recevra le code)
                  </FieldLabel>
                  <AppInput
                    id="reg-admin-email"
                    required
                    type="email"
                    autoComplete="email"
                    value={form.adminEmail}
                    onChange={(e) => setField('adminEmail', e.target.value)}
                    disabled={otpVerified}
                    placeholder="ex. jean.dupont@gmail.com"
                  />
                </div>
                <div className="form-field">
                  <FieldLabel htmlFor="reg-admin-tel-national" help={HELP.inscription.telephone}>
                    Téléphone
                  </FieldLabel>
                  <PhoneField
                    id="reg-admin-tel"
                    countryIso={form.adminTelephoneCountryIso}
                    national={form.adminTelephoneNational}
                    onChange={({ countryIso, national, e164 }) => {
                      setForm((prev) => ({
                        ...prev,
                        adminTelephoneCountryIso: countryIso,
                        adminTelephoneNational: national,
                        adminTelephone: e164,
                      }));
                    }}
                  />
                </div>
              </div>
            </div>

            {!otpVerified ? (
              <div className="onboard-block">
                <h3 className="onboard-block-title">Vérifier l’e-mail</h3>
                <p className="muted text-sm" style={{ marginTop: 0 }}>
                  {form.adminEmail.trim()
                    ? <>Le code sera envoyé à <strong>{form.adminEmail.trim()}</strong>.</>
                    : 'Saisissez d’abord votre e-mail personnel ci-dessus.'}
                </p>
                <div className="button-row">
                  <AppButton
                    type="button"
                    variant="secondary"
                    loading={otpBusy}
                    onClick={sendOtp}
                    disabled={!form.adminEmail.trim()}
                  >
                    {otpSent ? 'Renvoyer le code' : 'Envoyer le code'}
                  </AppButton>
                </div>
                {otpSent ? (
                  <div className="form-grid" style={{ marginTop: 12 }}>
                    <div className="form-field">
                      <FieldLabel htmlFor="reg-otp" required>Code reçu</FieldLabel>
                      <AppInput
                        id="reg-otp"
                        value={otpCode}
                        onChange={(e) => setOtpCode(e.target.value.replace(/\D/g, '').slice(0, 8))}
                        inputMode="numeric"
                        autoComplete="one-time-code"
                        placeholder="6 chiffres"
                        maxLength={8}
                      />
                    </div>
                    <div className="form-field" style={{ display: 'flex', alignItems: 'flex-end' }}>
                      <AppButton type="button" loading={otpBusy} onClick={verifyOtp}>
                        Valider le code
                      </AppButton>
                    </div>
                  </div>
                ) : null}
              </div>
            ) : null}

            {otpVerified ? (
              <AppAlert variant="success">
                <strong>E-mail vérifié</strong>
                <p className="muted text-sm" style={{ marginTop: 8, marginBottom: 0 }}>
                  Identifiant réservé : <strong>{form.adminUsername}</strong>.
                  Le mot de passe temporaire a été envoyé uniquement à{' '}
                  <strong>{form.adminEmail}</strong> — il n’apparaît jamais dans cette page.
                </p>
              </AppAlert>
            ) : null}

            <div className="form-field full">
              <FieldLabel htmlFor="reg-message">Message (optionnel)</FieldLabel>
              <textarea
                id="reg-message"
                className="textarea"
                rows={3}
                value={form.message}
                onChange={(e) => setField('message', e.target.value)}
                placeholder="Contexte, contact curé, besoins…"
              />
            </div>
          </section>
        ) : null}

        {step === 4 ? (
          <section className="stack">
            <PageHeader
              title="Documents officiels"
              subtitle="Scannez ou photographiez les pièces (PDF, JPEG ou PNG, max 5 Mo)."
            />
            <StepHelpBanner title="Documents à fournir" text={HELP.inscription.documents} />
            <div className="form-grid">
              <FileDropField
                id="reg-mandat"
                label="Mandat du curé"
                help={HELP.inscription.documents}
                file={mandatCure}
                required
                onChange={setMandatCure}
              />
              <FileDropField
                id="reg-cni"
                label="Pièce d’identité de l’administrateur"
                file={adminCni}
                required
                onChange={setAdminCni}
              />
            </div>
          </section>
        ) : null}

        {step === 5 ? (
          <section className="stack">
            <PageHeader
              title="Vérification avant envoi"
              subtitle="Contrôlez les informations. Vous pouvez revenir à une étape pour corriger."
            />

            <div className="onboard-recap">
              <div className="onboard-recap-card">
                <div className="onboard-recap-head">
                  <h3>Paroisse</h3>
                  <button type="button" className="link-button" onClick={() => goToStep(1)}>Modifier</button>
                </div>
                <div className="info-list">
                  <div className="info-row"><span>Nom</span><strong>{form.nomParoisse}</strong></div>
                  <div className="info-row"><span>Doyenné</span><span>{doyenneNom}</span></div>
                  <div className="info-row"><span>Adresse</span><span>{form.adresse}</span></div>
                  {form.telephone ? (
                    <div className="info-row"><span>Téléphone</span><span>{form.telephone}</span></div>
                  ) : null}
                </div>
              </div>

              <div className="onboard-recap-card">
                <div className="onboard-recap-head">
                  <h3>Formule</h3>
                  <button type="button" className="link-button" onClick={() => goToStep(2)}>Modifier</button>
                </div>
                <div className="info-list">
                  <div className="info-row">
                    <span>Abonnement</span>
                    <strong>{selectedPlan.name} — {formatCurrency(selectedPlan.price)}</strong>
                  </div>
                </div>
              </div>

              <div className="onboard-recap-card">
                <div className="onboard-recap-head">
                  <h3>Administrateur</h3>
                  <button type="button" className="link-button" onClick={() => goToStep(3)}>Modifier</button>
                </div>
                <div className="info-list">
                  <div className="info-row">
                    <span>Nom</span>
                    <span>{form.adminPrenom} {form.adminNom}</span>
                  </div>
                  <div className="info-row"><span>E-mail</span><span>{form.adminEmail}</span></div>
                  <div className="info-row"><span>Identifiant</span><strong>{form.adminUsername}</strong></div>
                </div>
              </div>

              <div className="onboard-recap-card">
                <div className="onboard-recap-head">
                  <h3>Documents</h3>
                  <button type="button" className="link-button" onClick={() => goToStep(4)}>Modifier</button>
                </div>
                <div className="info-list">
                  <div className="info-row"><span>Mandat</span><span>{mandatCure?.name || '—'}</span></div>
                  <div className="info-row"><span>Pièce d’identité</span><span>{adminCni?.name || '—'}</span></div>
                </div>
              </div>
            </div>

            <p className="muted text-sm">
              Aucun paiement maintenant. Après validation, un e-mail professionnel Missanye
              sera associé à la paroisse et vous pourrez vous connecter.
            </p>
          </section>
        ) : null}

        {error ? <AppAlert variant="danger">{error}</AppAlert> : null}

        <div className="button-row onboard-actions">
          {step > 1 ? (
            <AppButton type="button" variant="secondary" onClick={back} disabled={busy}>
              Retour
            </AppButton>
          ) : (
            <Link className="btn btn-secondary" to="/">Annuler</Link>
          )}
          {step < 5 ? (
            <AppButton type="submit">Continuer</AppButton>
          ) : (
            <AppButton type="submit" loading={busy}>
              Soumettre l’inscription
            </AppButton>
          )}
        </div>
      </form>
    </div>
  );
}
