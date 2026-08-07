import { getApiBaseUrl } from '../../config/apiBaseUrl';
import { getAccessToken } from '../../constants/authStorage';
import { attachToken } from './interceptors';

const API_BASE_URL = getApiBaseUrl();

/**
 * @param {string} path
 * @param {RequestInit} [options]
 * @param {{ auth?: boolean, parse?: 'json' | 'blob' | 'text' | false, signal?: AbortSignal }} [clientOptions]
 */
export async function apiClient(path, options = {}, clientOptions = {}) {
  const { auth = false, parse = 'json', signal } = clientOptions;
  const urlPath = path.startsWith('/') ? path : `/${path}`;
  const method = (options.method || 'GET').toUpperCase();
  let headers = {
    // Compte ngrok gratuit : sans ce header, les fetch() reçoivent souvent un 403 HTML
    'ngrok-skip-browser-warning': '1',
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
  if (auth) {
    headers = attachToken(headers, getAccessToken());
  }
  const response = await fetch(`${API_BASE_URL}${urlPath}`, {
    ...options,
    headers,
    signal: signal || options.signal,
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

  if (response.status === 204 || parse === false) {
    return null;
  }
  if (parse === 'blob') {
    return response.blob();
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
