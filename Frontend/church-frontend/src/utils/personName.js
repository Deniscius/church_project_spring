/** Normalise les apostrophes typographiques vers ASCII (stockage / API). */
export function normalizeApostrophes(value) {
  return String(value ?? '').replace(/[\u2018\u2019\u02BC\u0060\u00B4]/g, "'");
}

/** Valeur stockée lorsque le fidèle ne renseigne pas son identité. */
export const DEFAULT_FIDELE_NAME = 'Un(e) chrétien(ne)';

/** Lettres (accents inclus), espaces, tirets et apostrophes. */
const PERSON_NAME_PATTERN = /^[\p{L}]+(?:[\s'-]+[\p{L}]+)*$/u;

/**
 * Filtre la saisie en temps réel (chiffres exclus ; apostrophes conservées).
 */
export function sanitizePersonNameInput(value) {
  return normalizeApostrophes(value)
    .replace(/[^\p{L}\s'-]/gu, '')
    .replace(/\s+/g, ' ');
}

export function isValidPersonName(value) {
  const trimmed = normalizeApostrophes(value).trim();
  if (!trimmed) return true;
  return PERSON_NAME_PATTERN.test(trimmed);
}

/**
 * Normalise pour envoi API : trim, ou null si vide (le backend applique le défaut).
 */
export function toOptionalPersonName(value) {
  const trimmed = normalizeApostrophes(value).trim().replace(/\s+/g, ' ');
  return trimmed || null;
}

/**
 * Affichage : prénom seul, nom seul, les deux, ou défaut anonyme.
 */
export function formatFideleName(prenom, nom) {
  const p = String(prenom ?? '').trim();
  const n = String(nom ?? '').trim();
  if (!p && !n) return DEFAULT_FIDELE_NAME;
  if (!p) return n;
  if (!n) return p;
  return `${p} ${n}`;
}

export function personNameError(value, label = 'Ce champ') {
  const trimmed = normalizeApostrophes(value).trim();
  if (!trimmed) return null;
  if (!PERSON_NAME_PATTERN.test(trimmed)) {
    return `${label} ne doit contenir que des lettres (espaces, tirets et apostrophes autorisés).`;
  }
  return null;
}
