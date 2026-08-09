import { getApiBaseUrl } from '../../config/apiBaseUrl';

/**
 * @param {string} path
 * @param {RequestInit} [options]
 * @param {{ auth?: boolean, parse?: 'json' | 'blob' | 'text' | false, signal?: AbortSignal }} [clientOptions]
 */
export async function apiClient(path, options = {}, clientOptions = {}) {
  const { auth = false, parse = 'json', signal } = clientOptions;
  const urlPath = path.startsWith('/') ? path : `/${path}`;
  const method = (options.method || 'GET').toUpperCase();
  // Toujours résoudre à l'appel (HMR / env) — jamais figer au chargement du module.
  const apiBaseUrl = getApiBaseUrl();
  let headers = {
    // Compte ngrok gratuit : sans ce header, les fetch() reçoivent l'interstitiel
    'ngrok-skip-browser-warning': 'true',
    // Force un préflight CORS + garde CSRF cookie (CookieAuthMutationGuardFilter)
    'X-Requested-With': 'XMLHttpRequest',
    ...(options.headers || {}),
  };
  const isFormData = typeof FormData !== 'undefined' && options.body instanceof FormData;
  if (
    method !== 'GET'
    && method !== 'HEAD'
    && !isFormData
    && !headers['Content-Type']
    && !headers['content-type']
  ) {
    headers['Content-Type'] = 'application/json';
  }
  if (isFormData) {
    // Laisser le navigateur poser le boundary multipart.
    delete headers['Content-Type'];
    delete headers['content-type'];
  }
  // Cookie HttpOnly (MS_AT) via credentials:include — jamais de Bearer.
  // `auth` documente les routes protégées (pas d'injection de token).
  void auth;

  let response;
  try {
    response = await fetch(`${apiBaseUrl}${urlPath}`, {
      ...options,
      headers,
      credentials: 'include',
      signal: signal || options.signal,
    });
  } catch (err) {
    const raw = err instanceof Error ? err.message : String(err || '');
    throw new Error(
      /failed to fetch|networkerror|load failed/i.test(raw)
        ? 'Impossible de joindre l’API (réseau, proxy /api ou tunnel ngrok).'
        : (raw || 'Erreur réseau')
    );
  }

  if (!response.ok) {
    let message = `Erreur HTTP ${response.status}`;
    try {
      const err = await response.json();
      if (typeof err === 'string') {
        message = err;
      } else if (err?.message) {
        message = err.message;
      } else if (Array.isArray(err?.errors) && err.errors.length) {
        message = err.errors
          .map((e) => e.defaultMessage || e.message || e)
          .filter(Boolean)
          .join(' ; ');
      }
    } catch {
      /* corps non JSON */
    }
    const error = new Error(message);
    error.status = response.status;
    error.messages = String(message)
      .split(/\s*;\s*/)
      .map((part) => part.trim())
      .filter(Boolean);
    throw error;
  }

  if (response.status === 204 || parse === false) {
    return null;
  }
  if (parse === 'blob') {
    const contentType = (response.headers.get('content-type') || '').toLowerCase();
    const blob = await response.blob();
    if (
      contentType.includes('text/html')
      || contentType.includes('text/plain')
      || (blob.type || '').includes('html')
      || (blob.type || '').includes('text/plain')
    ) {
      // Ngrok free renvoie parfois l'interstitiel en text/plain (200).
      const probe = (await blob.slice(0, 32).text()).toLowerCase();
      if (probe.includes('ngrok') || probe.includes('<!doctype') || probe.includes('<html')) {
        throw new Error(
          'Interstitiel ngrok reçu à la place du fichier. '
          + 'Ouvrez le lien dans un onglet, cliquez « Visit Site », puis réessayez.'
        );
      }
    }
    return blob;
  }
  if (parse === 'text') {
    return response.text();
  }

  const text = await response.text();
  if (!text) {
    return null;
  }
  return JSON.parse(text);
}
