/**
 * Normalise les erreurs formulaire / API en liste de messages affichables.
 */

export function splitErrorMessages(raw) {
  if (!raw) return [];
  if (Array.isArray(raw)) {
    return raw.flatMap((item) => splitErrorMessages(item)).filter(Boolean);
  }
  if (typeof raw === 'object' && Array.isArray(raw.messages)) {
    return raw.messages.filter(Boolean);
  }
  const text = raw instanceof Error
    ? raw.message
    : (typeof raw === 'string' ? raw : String(raw?.message || raw));
  if (!text) return [];
  return text
    .split(/\s*;\s*|\n+/)
    .map((part) => part.trim())
    .filter(Boolean);
}

/** Pour FormError + toast. */
export function normalizeFormErrors(err) {
  const list = splitErrorMessages(err);
  return list.length ? list : ['Une erreur est survenue.'];
}

/** Identifiant / e-mail : pas d’espaces en tête/queue. */
export function sanitizeAuthUsernameInput(value) {
  return String(value ?? '').replace(/^\s+|\s+$/g, '');
}

/**
 * Mot de passe : retire uniquement les espaces de début/fin
 * (les espaces au milieu restent possibles).
 */
export function sanitizeAuthPasswordEdges(value) {
  return String(value ?? '').replace(/^\s+|\s+$/g, '');
}
