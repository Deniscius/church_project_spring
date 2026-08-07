/**
 * Base URL de l'API Spring.
 * En dev / ngrok : `/api` (proxy Vite → localhost:8081), même origine → pas de CORS.
 * En prod : VITE_API_BASE_URL est obligatoire (URL absolue du backend).
 */
export function getApiBaseUrl() {
  const raw = import.meta.env.VITE_API_BASE_URL;
  if (raw === undefined || raw === null || String(raw).trim() === '') {
    if (import.meta.env.DEV) {
      return '/api';
    }
    throw new Error(
      'VITE_API_BASE_URL est obligatoire en production. '
      + 'Définissez l’URL absolue de l’API (ex. https://api.exemple.tg).'
    );
  }
  return String(raw).replace(/\/$/, '');
}
