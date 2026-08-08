/**
 * Fait défiler (et focus) vers un élément d’erreur pour qu’il soit visible,
 * y compris sous un header sticky / sur mobile.
 */
export function scrollElementIntoView(el, { behavior = 'smooth', block = 'center' } = {}) {
  if (!el || typeof el.scrollIntoView !== 'function') return;
  const reduceMotion = typeof window !== 'undefined'
    && window.matchMedia?.('(prefers-reduced-motion: reduce)')?.matches;
  el.scrollIntoView({
    behavior: reduceMotion ? 'auto' : behavior,
    block,
  });
  if (typeof el.focus === 'function') {
    try {
      el.focus({ preventScroll: true });
    } catch {
      el.focus();
    }
  }
}

/**
 * Cherche le premier message d’erreur visible dans le document et y scrolle.
 */
export function scrollToFirstError(root = document) {
  const el = root.querySelector?.(
    '.form-error-banner, [data-form-error], [role="alert"].app-alert-danger, .text-red-600'
  );
  if (el) scrollElementIntoView(el);
}
