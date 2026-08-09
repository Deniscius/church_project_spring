import React, { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import AppCard from '../ui/AppCard';
import AppInput from '../ui/AppInput';
import AppTextarea from '../ui/AppTextarea';
import AppAlert from '../ui/AppAlert';
import PhoneField from '../ui/PhoneField';
import { FieldLabel } from '../ui/HelpTip';
import { usePublicDemandeDraft } from '../../contexts/publicDemandeDraft.context';
import { sanitizePersonNameInput, DEFAULT_FIDELE_NAME } from '../../utils/personName';
import {
  INTENTION_MAX_LENGTH,
  INTENTION_SUGGESTIONS,
  intentionError,
  normalizeIntention,
  sanitizeIntentionInput,
} from '../../utils/intentionText';
import { HELP } from '../../constants/helpTips';
import { DEFAULT_PHONE_COUNTRY_ISO } from '../../utils/phone';
import { WEEK_DAY_LABELS } from '../../constants/enums';
import { formatDateShort } from '../../utils/formatDate';

/**
 * Étape 1 — intention d’abord, téléphone ensuite, le reste en option
 * (sauf messe spéciale : téléphone + e-mail obligatoires).
 */
export default function ApplicantForm() {
  const { draft, patch } = usePublicDemandeDraft();
  const speciale = draft.forfaitNature === 'SPECIALE';
  const [touchedIntention, setTouchedIntention] = useState(false);
  const [showOptional, setShowOptional] = useState(() => Boolean(
    speciale
    || draft.prenomFidele
    || draft.nomFidele
    || draft.emailFidele
    || draft.nomCoursier
  ));

  const countryIso = draft.telCountryIso || DEFAULT_PHONE_COUNTRY_ISO;
  const intentionHint = touchedIntention || draft.intention?.trim()
    ? intentionError(draft.intention)
    : null;
  const intentionLen = String(draft.intention || '').length;

  const scheduleHint = draft.prefillFromSchedule
    ? [
      draft.paroisseNom,
      draft.horaireLibelle || draft.horaireHeureCelebration,
      draft.horaireJourSemaine
        ? (WEEK_DAY_LABELS[draft.horaireJourSemaine] || draft.horaireJourSemaine)
        : null,
      formatDateShort(draft.dateDebut),
    ].filter(Boolean).join(' · ')
    : '';

  const optionalSummary = useMemo(() => {
    const bits = [];
    const name = [draft.prenomFidele, draft.nomFidele].filter(Boolean).join(' ');
    if (name) bits.push(name);
    if (draft.emailFidele) bits.push(draft.emailFidele);
    if (draft.nomCoursier) bits.push(`coursier : ${draft.nomCoursier}`);
    return bits.join(' · ');
  }, [draft.prenomFidele, draft.nomFidele, draft.emailFidele, draft.nomCoursier]);

  const applySuggestion = (prefix) => {
    patch({ intention: sanitizeIntentionInput(prefix) });
    setTouchedIntention(true);
    window.requestAnimationFrame(() => {
      document.getElementById('fd-intention')?.focus();
    });
  };

  return (
    <AppCard
      title="Votre intention"
      subtitle={
        speciale
          ? 'Messe spéciale : intention, téléphone et e-mail valides sont obligatoires.'
          : 'Commencez par l’intention (pour qui / pour quoi), puis un numéro joignable.'
      }
    >
      <div className="stack" style={{ gap: 18 }}>
        {scheduleHint ? (
          <div className="demande-schedule-prefill" role="status">
            <strong>Créneau déjà choisi</strong>
            <p>{scheduleHint}</p>
            <small className="muted">
              Indiquez maintenant l’intention de messe — le créneau reste mémorisé pour la suite.
            </small>
          </div>
        ) : null}

        {speciale ? (
          <AppAlert variant="info">
            Pour une demande spéciale, un numéro de téléphone et une adresse e-mail valides
            sont obligatoires afin que la paroisse puisse vous recontacter.
          </AppAlert>
        ) : null}

        <div className="form-field">
          <FieldLabel htmlFor="fd-intention" help={HELP.demande.intention} required>
            Pour qui / pour quelle intention ?
          </FieldLabel>

          <div className="intention-suggestions" role="group" aria-label="Suggestions d’intention">
            {INTENTION_SUGGESTIONS.map((s) => (
              <button
                key={s.id}
                type="button"
                className="intention-chip"
                onClick={() => applySuggestion(s.prefix)}
              >
                {s.label}
              </button>
            ))}
          </div>

          <AppTextarea
            id="fd-intention"
            value={draft.intention}
            onChange={(e) => {
              patch({ intention: sanitizeIntentionInput(e.target.value) });
              if (!touchedIntention) setTouchedIntention(true);
            }}
            onBlur={(e) => {
              setTouchedIntention(true);
              const cleaned = normalizeIntention(e.target.value);
              if (cleaned !== e.target.value) {
                patch({ intention: cleaned });
              }
            }}
            placeholder="Ex. Pour le repos de l’âme de Marie Adjovi…"
            rows={3}
            required
            autoFocus
            maxLength={INTENTION_MAX_LENGTH}
            aria-invalid={intentionHint ? 'true' : undefined}
            aria-describedby="fd-intention-hint"
          />
          <div id="fd-intention-hint" className="intention-field-meta">
            {intentionHint ? (
              <small className="text-red-600">{intentionHint}</small>
            ) : (
              <small className="muted">
                Écrivez une intention claire — pas de « test », liens ou suites de caractères.
              </small>
            )}
            <small className={`intention-counter${intentionLen > INTENTION_MAX_LENGTH - 40 ? ' is-warn' : ''}`}>
              {intentionLen}/{INTENTION_MAX_LENGTH}
            </small>
          </div>
        </div>

        <div className="form-field">
          <FieldLabel htmlFor="fd-phone-national" help={HELP.demande.telephone} required>
            Téléphone
          </FieldLabel>
          <PhoneField
            id="fd-phone"
            required
            countryIso={countryIso}
            national={draft.telNational || ''}
            onChange={({ countryIso: iso, national, e164 }) => {
              patch({
                telCountryIso: iso,
                telNational: national,
                telFidele: e164 || '',
              });
            }}
          />
          <p className="field-hint muted">
            Conservez ce numéro : sur la page{' '}
            <Link to="/suivi">Suivi</Link>, il permet de retrouver vos demandes
            (en plus du code de suivi).
          </p>
        </div>

        {speciale ? (
          <div className="form-field">
            <FieldLabel htmlFor="fd-email" help={HELP.demande.email} required>
              E-mail
            </FieldLabel>
            <AppInput
              id="fd-email"
              type="email"
              autoComplete="email"
              value={draft.emailFidele}
              onChange={(e) => patch({ emailFidele: e.target.value })}
              placeholder="ex. vous@email.com"
              required
            />
          </div>
        ) : null}

        <div className="demande-optional">
          <button
            type="button"
            className="demande-optional-toggle"
            aria-expanded={showOptional}
            onClick={() => setShowOptional((v) => !v)}
          >
            <span>{showOptional ? 'Masquer' : 'Ajouter'} {speciale ? 'nom…' : 'nom, e-mail…'}</span>
            <span className="muted">
              {showOptional
                ? 'optionnel'
                : (optionalSummary || `Par défaut : « ${DEFAULT_FIDELE_NAME} »`)}
            </span>
          </button>

          {!showOptional ? (
            <p className="field-hint muted" style={{ marginTop: 8 }}>
              Si vous ne renseignez ni prénom ni nom, le dossier affichera
              « {DEFAULT_FIDELE_NAME} ».
            </p>
          ) : null}

          {showOptional ? (
            <div className="form-grid demande-optional-fields">
              <div className="form-field">
                <FieldLabel htmlFor="fd-prenom" help={HELP.demande.prenom}>Prénom</FieldLabel>
                <AppInput
                  id="fd-prenom"
                  autoComplete="given-name"
                  value={draft.prenomFidele}
                  onChange={(e) => patch({ prenomFidele: sanitizePersonNameInput(e.target.value) })}
                  placeholder="Optionnel"
                />
              </div>
              <div className="form-field">
                <FieldLabel htmlFor="fd-nom" help={HELP.demande.nom}>Nom</FieldLabel>
                <AppInput
                  id="fd-nom"
                  autoComplete="family-name"
                  value={draft.nomFidele}
                  onChange={(e) => patch({ nomFidele: sanitizePersonNameInput(e.target.value) })}
                  placeholder="Optionnel"
                />
              </div>
              <p className="field-hint muted" style={{ gridColumn: '1 / -1', margin: 0 }}>
                Laissez vide pour apparaître comme « {DEFAULT_FIDELE_NAME} » sur le reçu.
              </p>
              {!speciale ? (
                <div className="form-field">
                  <FieldLabel htmlFor="fd-email" help={HELP.demande.email}>E-mail</FieldLabel>
                  <AppInput
                    id="fd-email"
                    type="email"
                    autoComplete="email"
                    value={draft.emailFidele}
                    onChange={(e) => patch({ emailFidele: e.target.value })}
                    placeholder="Optionnel"
                  />
                </div>
              ) : null}
              <div className="form-field">
                <label htmlFor="fd-coursier">Coursier (si dépôt par un tiers)</label>
                <AppInput
                  id="fd-coursier"
                  value={draft.nomCoursier}
                  onChange={(e) => patch({ nomCoursier: e.target.value })}
                  placeholder="Optionnel"
                />
              </div>
            </div>
          ) : null}
        </div>
      </div>
    </AppCard>
  );
}
