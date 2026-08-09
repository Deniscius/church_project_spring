/**
 * Intention de messe — normalisation + règles anti-saisie fantaisiste.
 * Aligné sur IntentionTextUtils (backend).
 */

export const INTENTION_MAX_LENGTH = 500;
export const INTENTION_MIN_LENGTH = 10;

/** Suggestions cliquables (préfixe à compléter). */
export const INTENTION_SUGGESTIONS = [
  { id: 'repos', label: 'Repos de l’âme', prefix: 'Pour le repos de l’âme de ' },
  { id: 'grace', label: 'Action de grâce', prefix: 'Action de grâce pour ' },
  { id: 'guerison', label: 'Guérison', prefix: 'Pour la guérison de ' },
  { id: 'protection', label: 'Protection', prefix: 'Pour la protection de ' },
  { id: 'anniversaire', label: 'Anniversaire', prefix: 'Action de grâce pour l’anniversaire de ' },
];

const JUNK = new Set([
  'test',
  'testing',
  'teste',
  'essai',
  'xxx',
  'xxxx',
  'aaaa',
  'bbbb',
  'asdf',
  'asdfgh',
  'qwerty',
  'azerty',
  'lorem',
  'ipsum',
  'nimp',
  'nimporte',
  'n importe',
  'n\'importe',
  'n’importe',
  'blah',
  'bla',
  'blabla',
  'rien',
  'aucun',
  'aucune',
  'oui',
  'non',
  'ok',
  'hello',
  'salut',
  '...',
  '….…',
]);

export function normalizeIntention(value) {
  return String(value ?? '')
    .replace(/[\u2018\u2019\u02BC\u0060\u00B4]/g, "'")
    .replace(/\s+/g, ' ')
    .trim();
}

/** Filtre doux à la saisie : pas de contrôle caractères, longueur max. */
export function sanitizeIntentionInput(value) {
  return String(value ?? '')
    .replace(/[\u0000-\u0008\u000B\u000C\u000E-\u001F]/g, '')
    .slice(0, INTENTION_MAX_LENGTH);
}

function letterCount(text) {
  const m = text.match(/\p{L}/gu);
  return m ? m.length : 0;
}

function isJunkPhrase(normalized) {
  const compact = normalized
    .toLowerCase()
    .normalize('NFD')
    .replace(/\p{M}/gu, '')
    .replace(/[^\p{L}\s']/gu, ' ')
    .replace(/\s+/g, ' ')
    .trim();
  if (!compact) return true;
  if (JUNK.has(compact)) return true;
  // « test test », « bla bla »
  const parts = compact.split(' ').filter(Boolean);
  if (parts.length && parts.every((p) => JUNK.has(p))) return true;
  return false;
}

/**
 * @returns {string|null} message d'erreur ou null si OK
 */
export function intentionError(value) {
  const text = normalizeIntention(value);
  if (!text) {
    return 'L’intention est obligatoire — indiquez pour qui ou pour quelle intention.';
  }
  if (text.length < INTENTION_MIN_LENGTH) {
    return `Précisez un peu plus (au moins ${INTENTION_MIN_LENGTH} caractères). Ex. « Pour le repos de l’âme de… ».`;
  }
  if (text.length > INTENTION_MAX_LENGTH) {
    return `L’intention ne peut pas dépasser ${INTENTION_MAX_LENGTH} caractères.`;
  }

  const letters = letterCount(text);
  if (letters < 5) {
    return 'L’intention doit contenir des mots (pas seulement des chiffres ou signes).';
  }

  const nonSpace = text.replace(/\s/g, '');
  if (nonSpace.length > 0 && letters / nonSpace.length < 0.45) {
    return 'Formulez une intention lisible (évitez les suites de chiffres ou de symboles).';
  }

  if (/(.)\1{4,}/u.test(text.replace(/\s/g, ''))) {
    return 'Cette intention ne semble pas valide (caractères répétés).';
  }

  if (/https?:\/\/|www\./i.test(text)) {
    return 'Une intention de messe ne doit pas contenir de lien internet.';
  }

  if (isJunkPhrase(text)) {
    return 'Indiquez une intention réelle (ex. « Pour le repos de l’âme de… » ou « Action de grâce »).';
  }

  return null;
}

export function isValidIntention(value) {
  return intentionError(value) == null;
}
