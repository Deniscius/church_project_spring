import { normalizeTrackingCode } from './trackingCode';

/** Clés session — jamais dans la barre d’adresse. */
export const SENSITIVE_KEYS = {
  TRACKING_CODE: 'ms_nav_tracking_code',
  PAYMENT_CODE: 'ms_nav_payment_code',
  INVOICE_CODE: 'ms_nav_invoice_code',
  RESET_TOKEN: 'ms_nav_reset_token',
  DOYENNE_FILTER: 'ms_nav_doyenne_filter',
};

function read(key) {
  try {
    return sessionStorage.getItem(key) || '';
  } catch {
    return '';
  }
}

function write(key, value) {
  try {
    if (value) sessionStorage.setItem(key, value);
    else sessionStorage.removeItem(key);
  } catch {
    /* private mode */
  }
}

export function setTrackingCode(code) {
  const c = normalizeTrackingCode(code);
  write(SENSITIVE_KEYS.TRACKING_CODE, c);
  return c;
}

export function getTrackingCode() {
  return normalizeTrackingCode(read(SENSITIVE_KEYS.TRACKING_CODE));
}

export function setPaymentCode(code) {
  const c = normalizeTrackingCode(code);
  write(SENSITIVE_KEYS.PAYMENT_CODE, c);
  return c;
}

export function getPaymentCode() {
  return normalizeTrackingCode(read(SENSITIVE_KEYS.PAYMENT_CODE));
}

export function setInvoiceCode(code) {
  const c = normalizeTrackingCode(code);
  write(SENSITIVE_KEYS.INVOICE_CODE, c);
  return c;
}

export function getInvoiceCode() {
  return normalizeTrackingCode(read(SENSITIVE_KEYS.INVOICE_CODE));
}

export function setResetToken(token) {
  const t = (token || '').trim();
  write(SENSITIVE_KEYS.RESET_TOKEN, t);
  return t;
}

export function getResetToken() {
  return read(SENSITIVE_KEYS.RESET_TOKEN).trim();
}

export function clearResetToken() {
  write(SENSITIVE_KEYS.RESET_TOKEN, '');
}

export function setDoyenneFilter(id) {
  write(SENSITIVE_KEYS.DOYENNE_FILTER, id || '');
}

export function getDoyenneFilter() {
  return read(SENSITIVE_KEYS.DOYENNE_FILTER);
}

/** Navigation interne vers le résultat de suivi (sans ?code=). */
export function goToTrackingResult(navigate, code) {
  const c = setTrackingCode(code);
  if (!c) return;
  navigate('/suivi/resultat', { replace: false });
}

/** Navigation interne vers paiement (sans code dans l’URL). */
export function goToPayment(navigate, code, options = {}) {
  const c = setPaymentCode(code);
  if (!c) return;
  navigate('/paiement', { replace: Boolean(options.replace) });
}

/** Navigation interne vers facture publique. */
export function goToInvoice(navigate, code) {
  const c = setInvoiceCode(code);
  if (!c) return;
  navigate('/facture');
}
