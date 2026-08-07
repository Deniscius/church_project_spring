/**
 * Valide le brouillon avant envoi (règles alignées sur DemandeServiceImpl).
 * @returns {{ ok: boolean, errors: string[] }}
 */
import { getForfaitDureeLabel, isMultiCelebrationForfait } from '../constants/enums';
import { personNameError } from './personName';
import {
  computeCelebrationDates,
  formatAllowedDays,
  getEffectiveAllowedDays,
  isDateAllowedForDays,
} from './schedulingUtils';
import {
  DEFAULT_PHONE_COUNTRY_ISO,
  validatePhoneForCountry,
} from './phone';

/** Aligné sur une validation e-mail pragmatique (HTML5 / Jakarta @Email). */
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/i;

export function isValidEmail(value) {
  const trimmed = String(value ?? '').trim();
  if (!trimmed) return true;
  return EMAIL_PATTERN.test(trimmed) && trimmed.length <= 150;
}

export function emailError(value) {
  const trimmed = String(value ?? '').trim();
  if (!trimmed) return null;
  if (!isValidEmail(trimmed)) {
    return 'L’adresse e-mail n’est pas valide.';
  }
  return null;
}

function collect(fn) {
  const errors = [];
  const req = (cond, msg) => {
    if (!cond) errors.push(msg);
  };
  fn(req, errors);
  return { ok: errors.length === 0, errors };
}

/** Étape 1 — identité & intention */
export function validatePublicDemandeStep1(draft) {
  return collect((req, errors) => {
    req(draft.intention?.trim(), 'L’intention est obligatoire.');
    const prenomErr = personNameError(draft.prenomFidele, 'Le prénom');
    const nomErr = personNameError(draft.nomFidele, 'Le nom');
    const mailErr = emailError(draft.emailFidele);
    if (prenomErr) errors.push(prenomErr);
    if (nomErr) errors.push(nomErr);
    if (mailErr) errors.push(mailErr);

    const phone = validatePhoneForCountry(
      draft.telCountryIso || DEFAULT_PHONE_COUNTRY_ISO,
      draft.telNational || ''
    );
    if (!phone.ok) {
      errors.push(phone.message);
    } else if (!draft.telFidele?.trim()) {
      errors.push('Le téléphone est obligatoire.');
    }
  });
}

/** Étape 2 — paroisse, type, nature */
export function validatePublicDemandeStep2(draft) {
  return collect((req) => {
    req(draft.paroissePublicId, 'Choisissez une paroisse.');
    req(draft.typeDemandePublicId, 'Choisissez un type de demande.');
    req(draft.forfaitTarifPublicId, 'Choisissez la nature de la messe (normale, dominicale ou spéciale).');
    req(
      draft.forfaitNombreCelebration != null && draft.forfaitNombreCelebration > 0,
      'Le forfait doit prévoir au moins une célébration.'
    );
  });
}

/** Étape 3 — horaire & dates */
export function validatePublicDemandeStep3(draft) {
  return collect((req) => {
    const n = draft.forfaitNombreCelebration;
    const multi = isMultiCelebrationForfait(n);
    const dureeLabel = getForfaitDureeLabel(n);
    const windowDays = draft.forfaitNombreJour > 0 ? Number(draft.forfaitNombreJour) : Number(n);

    const allowedDays = getEffectiveAllowedDays(
      draft.typeDemandeJoursCelebrationAutorises,
      draft.forfaitJoursCelebrationAutorises,
      null
    );

    const hp = Boolean(draft.forfaitHeurePersonnalise);
    const startDate = draft.dateDebut?.trim() || '';
    req(startDate, multi
      ? `La date de début du ${dureeLabel.toLowerCase()} est obligatoire.`
      : 'La date de célébration est obligatoire.');

    const celebrationDates = multi
      ? (startDate ? computeCelebrationDates(startDate, allowedDays, Number(n)) : [])
      : (startDate ? [startDate] : []);

    if (multi && startDate) {
      req(
        celebrationDates.length === Number(n),
        `Impossible de planifier ${n} célébration(s) pour ce ${dureeLabel.toLowerCase()} à partir de la date choisie.`
      );
    }

    if (!multi) {
      const hasHoraire = Boolean(draft.horairePublicId);
      const hasHeurePerso = Boolean(draft.heurePersonnalisee?.trim());
      if (hp) {
        req(
          hasHoraire || hasHeurePerso,
          'Indiquez un horaire de paroisse ou une heure personnalisée.'
        );
      } else {
        req(hasHoraire, 'Un horaire fixe est obligatoire pour ce forfait.');
        req(!hasHeurePerso, 'L’heure personnalisée n’est pas autorisée pour ce forfait.');
      }
    }

    if (celebrationDates.length) {
      const sorted = [...celebrationDates].sort();
      const first = sorted[0];
      const last = sorted[sorted.length - 1];

      if (multi && windowDays > 0) {
        const end = new Date(`${first}T12:00:00`);
        end.setDate(end.getDate() + windowDays - 1);
        const endIso = end.toLocaleDateString('en-CA', { timeZone: 'Africa/Lome' });
        req(
          last <= endIso,
          `Les dates du ${dureeLabel.toLowerCase()} doivent s’inscrire dans ${windowDays} jour(s) (${first} → ${endIso}).`
        );
      }

      const schedules = draft.dateSchedules || {};

      celebrationDates.forEach((dateStr, index) => {
        // Multi : chaque date doit respecter les jours du forfait.
        // Unique : le fidèle choisit librement (y compris une date précise hors grille) ;
        // si messe unique ce jour-là, seule l’heure est imposée.
        if (multi) {
          req(
            isDateAllowedForDays(dateStr, allowedDays),
            `La date ${index + 1} n’est pas autorisée pour cette nature (${formatAllowedDays(allowedDays)}).`
          );
        }

        let celebrationTime;
        if (multi) {
          const slot = schedules[dateStr] || {};
          const hasHoraire = Boolean(slot.horairePublicId);
          const hasHeurePerso = Boolean(slot.heurePersonnalisee?.trim());
          const uniqueDay = Boolean(slot.uniqueSurParoisse);
          if (uniqueDay) {
            req(hasHoraire, `Célébration ${index + 1} (${dateStr}) : heure de la messe unique obligatoire.`);
            req(!hasHeurePerso, `Célébration ${index + 1} (${dateStr}) : messe unique — heure personnalisée interdite.`);
          } else if (hp) {
            req(
              hasHoraire || hasHeurePerso,
              `Célébration ${index + 1} (${dateStr}) : indiquez un horaire ou une heure personnalisée.`
            );
          } else {
            req(
              hasHoraire,
              `Célébration ${index + 1} (${dateStr}) : un horaire paroissial est obligatoire pour ce jour.`
            );
          }
          celebrationTime = hasHeurePerso && !uniqueDay
            ? slot.heurePersonnalisee.trim()
            : slot.heureCelebration?.slice(0, 5);
        } else {
          celebrationTime = draft.heurePersonnalisee?.trim()
            || draft.horaireHeureCelebration?.slice(0, 5);
        }

        if (celebrationTime) {
          const requestedAt = new Date(`${dateStr}T${celebrationTime}:00`);
          const minimumAt = new Date(Date.now() + (draft.typeDemandeDelaiMinimumHeures ?? 24) * 3_600_000);
          req(
            !Number.isNaN(requestedAt.getTime()) && requestedAt >= minimumAt,
            `La célébration du ${dateStr} doit être au moins ${draft.typeDemandeDelaiMinimumHeures ?? 24} heure(s) après maintenant.`
          );
        }
      });
    }
  });
}

/** Étape 4 — paiement */
export function validatePublicDemandeStep4(draft) {
  return collect((req) => {
    req(draft.typePaiementPublicId, 'Choisissez un mode de paiement.');
  });
}

export function validatePublicDemandeStep(step, draft) {
  switch (step) {
    case 1:
      return validatePublicDemandeStep1(draft);
    case 2:
      return validatePublicDemandeStep2(draft);
    case 3:
      return validatePublicDemandeStep3(draft);
    case 4:
      return validatePublicDemandeStep4(draft);
    default:
      return validatePublicDemandeDraft(draft);
  }
}

export function validatePublicDemandeDraft(draft) {
  const all = [
    ...validatePublicDemandeStep1(draft).errors,
    ...validatePublicDemandeStep2(draft).errors,
    ...validatePublicDemandeStep3(draft).errors,
    ...validatePublicDemandeStep4(draft).errors,
  ];
  return { ok: all.length === 0, errors: all };
}

/**
 * Corps JSON pour POST /demandes (champs attendus par le backend).
 */
export function buildDemandeRequestBody(draft) {
  const formatTime = (value) => {
    const heure = value?.trim();
    if (!heure) return null;
    const [h, m] = heure.split(':');
    if (h == null || m == null) return null;
    return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:00`;
  };

  const email = draft.emailFidele?.trim();
  const coursier = draft.nomCoursier?.trim();
  const phone = validatePhoneForCountry(
    draft.telCountryIso || DEFAULT_PHONE_COUNTRY_ISO,
    draft.telNational || ''
  );
  const telE164 = phone.ok ? phone.e164 : (draft.telFidele?.trim() || '');
  const multi = isMultiCelebrationForfait(draft.forfaitNombreCelebration);

  const allowedDaysForBody = getEffectiveAllowedDays(
    draft.typeDemandeJoursCelebrationAutorises,
    draft.forfaitJoursCelebrationAutorises,
    multi ? null : draft.horaireJourSemaine
  );
  const start = draft.dateDebut?.trim() || null;
  const datesCelebration = multi
    ? (start
      ? computeCelebrationDates(start, allowedDaysForBody, Number(draft.forfaitNombreCelebration))
      : [])
    : (start ? [start] : []);

  const dateDebut = datesCelebration[0] || start;
  const schedules = draft.dateSchedules || {};

  const celebrationSlots = multi
    ? datesCelebration.map((date) => {
      const slot = schedules[date] || {};
      return {
        date,
        horairePublicId: slot.horairePublicId || null,
        heurePersonnalisee: formatTime(slot.heurePersonnalisee),
      };
    })
    : null;

  return {
    intention: draft.intention.trim(),
    // Vide → null : le backend applique « Un(e) chrétien(ne) » si les deux sont vides
    nomFidele: draft.nomFidele?.trim() || null,
    prenomFidele: draft.prenomFidele?.trim() || null,
    telFidele: telE164,
    emailFidele: email || null,
    nomCoursier: coursier || null,
    heurePersonnalisee: multi ? null : formatTime(draft.heurePersonnalisee),
    dateDebut,
    datesCelebration: multi ? datesCelebration : null,
    celebrationSlots,
    paroissePublicId: draft.paroissePublicId,
    typeDemandePublicId: draft.typeDemandePublicId,
    forfaitTarifPublicId: draft.forfaitTarifPublicId,
    horairePublicId: multi
      ? (celebrationSlots?.[0]?.horairePublicId || null)
      : (draft.horairePublicId || null),
    userPublicId: null,
    typePaiementPublicId: draft.typePaiementPublicId,
  };
}

export const PUBLIC_DEMANDE_RESULT_KEY = 'public_demande_dernier_resultat';

export function persistDemandeCreationResult(result) {
  try {
    sessionStorage.setItem(PUBLIC_DEMANDE_RESULT_KEY, JSON.stringify(result));
  } catch {
    /* ignore */
  }
}

export function readDemandeCreationResult() {
  try {
    const raw = sessionStorage.getItem(PUBLIC_DEMANDE_RESULT_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

export function clearDemandeCreationResult() {
  sessionStorage.removeItem(PUBLIC_DEMANDE_RESULT_KEY);
}
