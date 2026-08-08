import React, { useMemo, useState } from 'react';
import AppCard from '../ui/AppCard';
import AppInput from '../ui/AppInput';
import AppTextarea from '../ui/AppTextarea';
import AppAlert from '../ui/AppAlert';
import PhoneField from '../ui/PhoneField';
import { FieldLabel } from '../ui/HelpTip';
import { usePublicDemandeDraft } from '../../contexts/publicDemandeDraft.context';
import { sanitizePersonNameInput } from '../../utils/personName';
import { HELP } from '../../constants/helpTips';
import { DEFAULT_PHONE_COUNTRY_ISO } from '../../utils/phone';

/**
 * Étape 1 — intention d’abord, téléphone ensuite, le reste en option
 * (sauf messe spéciale : téléphone + e-mail obligatoires).
 */
export default function ApplicantForm() {
  const { draft, patch } = usePublicDemandeDraft();
  const speciale = draft.forfaitNature === 'SPECIALE';
  const [showOptional, setShowOptional] = useState(() => Boolean(
    speciale
    || draft.prenomFidele
    || draft.nomFidele
    || draft.emailFidele
    || draft.nomCoursier
  ));

  const countryIso = draft.telCountryIso || DEFAULT_PHONE_COUNTRY_ISO;

  const optionalSummary = useMemo(() => {
    const bits = [];
    const name = [draft.prenomFidele, draft.nomFidele].filter(Boolean).join(' ');
    if (name) bits.push(name);
    if (draft.emailFidele) bits.push(draft.emailFidele);
    if (draft.nomCoursier) bits.push(`coursier : ${draft.nomCoursier}`);
    return bits.join(' · ');
  }, [draft.prenomFidele, draft.nomFidele, draft.emailFidele, draft.nomCoursier]);

  return (
    <AppCard
      title="Votre intention"
      subtitle={
        speciale
          ? 'Messe spéciale : intention, téléphone et e-mail valides sont obligatoires.'
          : 'Deux infos suffisent pour commencer : l’intention et un numéro joignable.'
      }
    >
      <div className="stack" style={{ gap: 18 }}>
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
          <AppTextarea
            id="fd-intention"
            value={draft.intention}
            onChange={(e) => patch({ intention: e.target.value })}
            placeholder="Ex. Pour le repos de l’âme de… / Action de grâce…"
            rows={3}
            required
            autoFocus
          />
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
                : (optionalSummary || 'Sans nom → « Un(e) chrétien(ne) »')}
            </span>
          </button>

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
