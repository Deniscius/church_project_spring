import { getApiBaseUrl } from '../../config/apiBaseUrl';

/**
 * @param {string} path
 * @param {RequestInit} [options]
 * @param {{ auth?: boolean, parse?: 'json' | 'blob' | 'text' | false, signal?: AbortSignal, credentials?: RequestCredentials }} [clientOptions]
 */
export async function apiClient(path, options = {}, clientOptions = {}) {
  const {
    auth = false,
    parse = 'json',
    signal,
    credentials = 'include',
  } = clientOptions;
  const urlPath = path.startsWith('/') ? path : `/${path}`;
  const method = (options.method || 'GET').toUpperCase();
  const isMutation = method !== 'GET' && method !== 'HEAD';

  // Toujours résoudre à l'appel (HMR / env) — jamais figer au chargement du module.
  const apiBaseUrl = getApiBaseUrl();
  let headers = {
    ...(options.headers || {}),
  };

  // Le garde CSRF maison ne concerne que les mutations authentifiées par cookie.
  // Ne pas ajouter ce header sur GET/HEAD évite un préflight CORS inutile en production.
  if (isMutation && !headers['X-Requested-With'] && !headers['x-requested-with']) {
    headers['X-Requested-With'] = 'XMLHttpRequest';
  }

  const isFormData = typeof FormData !== 'undefined' && options.body instanceof FormData;
  if (
    isMutation
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

  // Cookie HttpOnly (MS_AT) par défaut via credentials:include — jamais de Bearer.
  // Les routes publiques peuvent explicitement utiliser credentials:'omit'.
  // `auth` documente les routes protégées (pas d'injection de token).
  void auth;

  let response;
  try {
    response = await fetch(`${apiBaseUrl}${urlPath}`, {
      ...options,
      headers,
      credentials,
      signal: signal || options.signal,
    });
  } catch (err) {
    const raw = err instanceof Error ? err.message : String(err || '');
    if (import.meta.env.DEV) {
      console.error('Échec de la requête API', err);
    }
    throw new Error(
      /failed to fetch|networkerror|load failed/i.test(raw)
        ? 'Le service est momentanément indisponible. Vérifiez votre connexion puis réessayez.'
        : 'La requête n’a pas pu aboutir. Veuillez réessayer.'
    );
  }

  if (!response.ok) {
    if (response.status === 401 && auth && typeof window !== 'undefined') {
      window.dispatchEvent(new CustomEvent('church:session-expired'));
    }
    let message = response.status >= 500
      ? 'Une erreur interne est survenue. Veuillez réessayer plus tard.'
      : `La requête a échoué (HTTP ${response.status}).`;
    let traceId = response.headers.get('x-request-id') || null;
    try {
      const err = await response.json();
      traceId = err?.traceId || traceId;
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
    if (response.status >= 500 && traceId) {
      message = `${message} Référence support : ${traceId}.`;
    }
    const error = new Error(message);
    error.status = response.status;
    error.traceId = traceId;
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
      || (blob.type || '').toLowerCase().includes('html')
    ) {
      const probe = (await blob.slice(0, 64).text()).toLowerCase();
      if (probe.includes('<!doctype') || probe.includes('<html')) {
        throw new Error(
          'Une page HTML a été reçue à la place du fichier demandé. Vérifiez VITE_API_BASE_URL sur Render.'
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
