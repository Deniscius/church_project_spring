import { useEffect, useRef } from 'react';
import { scrollElementIntoView } from '../utils/scrollToError';

/**
 * Quand `error` devient truthy, scrolle vers le nœud attaché à la ref.
 * @param {unknown} error
 * @returns {import('react').RefObject<HTMLElement|null>}
 */
export function useScrollToError(error) {
  const ref = useRef(null);

  useEffect(() => {
    if (!error) return;
    // Laisse React peindre le bandeau avant le scroll.
    const id = window.requestAnimationFrame(() => {
      scrollElementIntoView(ref.current);
    });
    return () => window.cancelAnimationFrame(id);
  }, [error]);

  return ref;
}
