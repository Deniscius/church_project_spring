/**
 * Base URL de l'API Spring.
 * - Dev / partage local : `/api` (proxy Vite → :8081), même origine.
 * - Prod déployée : VITE_API_BASE_URL = URL absolue du backend.
 */
function looksLikeLocalOrTunnelHost(hostname) {
  if (!hostname) return false;
  const h = String(hostname).toLowerCase();
  return (
    h === 'localhost'
    || h === '127.0.0.1'
    || h === '[::1]'
    || h.endsWith('.loca.lt')
    || h.endsWith('.localtunnel.me')
    || h.endsWith('.ngrok-free.app')
    || h.endsWith('.ngrok-free.dev')
    || h.endsWith('.ngrok.app')
    || h.endsWith('.ngrok.io')
  );
}

export function getApiBaseUrl() {
  const raw = import.meta.env.VITE_API_BASE_URL;
  if (raw !== undefined && raw !== null && String(raw).trim() !== '') {
    return String(raw).replace(/\/$/, '');
  }

  // Modes locaux (dev Vite, npm run prod:share)
  if (import.meta.env.DEV || import.meta.env.MODE === 'share') {
    return '/api';
  }

  // Filet de sécurité : preview / tunnel sans .env.production
  if (typeof window !== 'undefined' && looksLikeLocalOrTunnelHost(window.location.hostname)) {
    return '/api';
  }

  throw new Error(
    'VITE_API_BASE_URL est obligatoire en production. '
    + 'En local : `npm run dev` ou `npm run prod:share`. '
    + 'En déploiement : URL absolue de l’API (ex. https://api.exemple.tg).'
  );
}
