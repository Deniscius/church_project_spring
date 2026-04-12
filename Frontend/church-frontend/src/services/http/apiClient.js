import { getAccessToken } from '../../constants/authStorage';
import { attachToken } from './interceptors';

/** Aligné sur le backend Spring (port par défaut 8081, pas de context-path /api). */
const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081').replace(/\/$/, '');

/**
 * @param {string} path
 * @param {RequestInit} [options]
 * @param {{ auth?: boolean }} [clientOptions] — auth: envoyer le JWT (session)
 */
export async function apiClient(path, options = {}, clientOptions = {}) {
  const { auth = false } = clientOptions;
  const urlPath = path.startsWith('/') ? path : `/${path}`;
  let headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {}),
  };
  if (auth) {
    headers = attachToken(headers, getAccessToken());
  }
  const response = await fetch(`${API_BASE_URL}${urlPath}`, {
    ...options,
    headers,
  });

  if (!response.ok) {
    let message = `Erreur HTTP ${response.status}`;
    try {
      const err = await response.json();
      if (typeof err === 'string') {
        message = err;
      } else if (err?.message) {
        message = err.message;
      } else if (Array.isArray(err?.errors) && err.errors[0]?.defaultMessage) {
        message = err.errors[0].defaultMessage;
      }
    } catch {
      /* corps non JSON */
    }
    throw new Error(message);
  }

  if (response.status === 204) {
    return null;
  }

  return response.json();
}
