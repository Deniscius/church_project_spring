import { getApiBaseUrl } from '../config/apiBaseUrl';

/** Proxy Vite (/__receipt) disponible en dev et en preview (prod:share / ngrok). */
function useViteReceiptProxy() {
  if (import.meta.env.DEV || import.meta.env.MODE === 'share') return true;
  try {
    return getApiBaseUrl() === '/api';
  } catch {
    return false;
  }
}

function apiReceiptPath(code, { downloadable } = {}) {
  const suffix = downloadable ? 'recu.pdf' : 'recu';
  return `${getApiBaseUrl()}/demandes/code/${encodeURIComponent(code)}/${suffix}`;
}

/**
 * URL d’affichage du reçu dans l’iframe (PDF same-origin, sans fetch JS).
 */
export function getReceiptPreviewUrl(codeSuivie) {
  const code = (codeSuivie || '').trim();
  if (!code) return '';
  if (useViteReceiptProxy()) {
    return `/__receipt/${encodeURIComponent(code)}`;
  }
  return apiReceiptPath(code);
}

/**
 * URL du fichier PDF (téléchargement / nouvel onglet).
 */
export function getReceiptPdfUrl(codeSuivie) {
  const code = (codeSuivie || '').trim();
  if (!code) return '';
  if (useViteReceiptProxy()) {
    return `/__receipt/${encodeURIComponent(code)}`;
  }
  return apiReceiptPath(code, { downloadable: true });
}
