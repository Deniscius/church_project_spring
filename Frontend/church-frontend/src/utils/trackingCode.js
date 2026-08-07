/**
 * Normalise un code de suivi saisi (casse, espaces, tirets).
 * Ex. {@code ms sj k7m2xq} → {@code MS-SJ-K7M2XQ}
 * Accepte aussi l’ancien format {@code MS-K7M2XQ}.
 */
export function normalizeTrackingCode(raw) {
  if (raw == null) return '';
  const trimmed = String(raw).trim().toUpperCase();
  const compact = trimmed.replace(/[\s_-]+/g, '');

  // Nouveau : MS + initiales paroisse (2–4) + jeton 6
  if (/^MS[A-Z]{2,4}[A-Z2-9]{6}$/.test(compact)) {
    const body = compact.slice(2);
    const token = body.slice(-6);
    const initials = body.slice(0, -6);
    return `MS-${initials}-${token}`;
  }
  // Ancien format court : MS + jeton 6
  if (/^MS[A-Z2-9]{6}$/.test(compact)) {
    return `MS-${compact.slice(2)}`;
  }
  if (/^[A-Z2-9]{6}$/.test(compact)) {
    return `MS-${compact}`;
  }
  return trimmed.replace(/\s+/g, '');
}

/** Exemple neutre pour tests internes (non affiché aux fidèles). */
export const TRACKING_CODE_EXAMPLE = 'MS-XXXXXX';
