/**
 * Indicatifs mondiaux + validation via libphonenumber-js.
 * Format stocké : E.164 (+indicatif + national, sans espaces).
 */
import {
  getCountries,
  getCountryCallingCode,
  isSupportedCountry,
  parsePhoneNumberFromString,
} from 'libphonenumber-js';

const regionNames = typeof Intl !== 'undefined'
  ? new Intl.DisplayNames(['fr'], { type: 'region' })
  : null;

/** Drapeau emoji à partir du code ISO (ex. TG → 🇹🇬). */
export function countryFlag(iso) {
  const code = String(iso || '').toUpperCase();
  if (!/^[A-Z]{2}$/.test(code)) return '🌐';
  return String.fromCodePoint(
    ...[...code].map((char) => 127397 + char.charCodeAt(0))
  );
}

function countryName(iso) {
  try {
    return regionNames?.of(iso) || iso;
  } catch {
    return iso;
  }
}

/**
 * Pays prioritaires (Afrique de l’Ouest + diaspora fréquente) en tête de liste.
 */
const PRIORITY_ISO = [
  'TG', 'BJ', 'GH', 'NG', 'CI', 'BF', 'NE', 'ML', 'SN', 'CM', 'GA', 'CG', 'CD',
  'FR', 'BE', 'CA', 'US', 'GB', 'DE', 'IT', 'ES', 'PT', 'CH',
];

function buildPhoneCountries() {
  const all = getCountries()
    .filter((iso) => isSupportedCountry(iso))
    .map((iso) => ({
      iso,
      name: countryName(iso),
      dial: getCountryCallingCode(iso),
      flag: countryFlag(iso),
    }));

  const byIso = Object.fromEntries(all.map((c) => [c.iso, c]));
  const priority = PRIORITY_ISO.map((iso) => byIso[iso]).filter(Boolean);
  const prioritySet = new Set(priority.map((c) => c.iso));
  const rest = all
    .filter((c) => !prioritySet.has(c.iso))
    .sort((a, b) => a.name.localeCompare(b.name, 'fr'));

  return [...priority, ...rest];
}

export const PHONE_COUNTRIES = buildPhoneCountries();

export const DEFAULT_PHONE_COUNTRY_ISO = 'TG';
/** @deprecated alias — préférer DEFAULT_PHONE_COUNTRY_ISO */
export const DEFAULT_PHONE_COUNTRY = DEFAULT_PHONE_COUNTRY_ISO;

export function getPhoneCountry(iso) {
  const code = String(iso || DEFAULT_PHONE_COUNTRY_ISO).toUpperCase();
  return PHONE_COUNTRIES.find((c) => c.iso === code)
    || PHONE_COUNTRIES.find((c) => c.iso === DEFAULT_PHONE_COUNTRY_ISO)
    || PHONE_COUNTRIES[0];
}

/** Garde uniquement les chiffres du numéro national. */
export function digitsOnly(value) {
  return String(value ?? '').replace(/\D/g, '');
}

/**
 * Compose le numéro E.164 à partir du pays et du national.
 * @returns {string} ex. +22890123456
 */
export function toE164(iso, national) {
  const country = getPhoneCountry(iso);
  const nationalDigits = digitsOnly(national);
  if (!nationalDigits) return '';

  const parsed = parsePhoneNumberFromString(nationalDigits, country.iso);
  if (parsed?.isValid()) return parsed.format('E.164');

  // Fallback : composition brute (utile pendant la saisie partielle).
  return `+${country.dial}${nationalDigits}`;
}

/**
 * Tente de découper un E.164 / saisie libre en { iso, national }.
 */
export function parseStoredPhone(raw, fallbackIso = DEFAULT_PHONE_COUNTRY_ISO) {
  const trimmed = String(raw ?? '').trim();
  if (!trimmed) {
    return { iso: fallbackIso, national: '' };
  }

  const parsed = parsePhoneNumberFromString(trimmed);
  if (parsed?.country) {
    return {
      iso: parsed.country,
      national: parsed.nationalNumber || digitsOnly(trimmed),
    };
  }

  const compact = trimmed.replace(/[\s().-]/g, '');
  if (compact.startsWith('+')) {
    const rest = compact.slice(1);
    const sorted = [...PHONE_COUNTRIES].sort((a, b) => b.dial.length - a.dial.length);
    for (const c of sorted) {
      if (rest.startsWith(c.dial)) {
        return { iso: c.iso, national: rest.slice(c.dial.length) };
      }
    }
  }

  return { iso: fallbackIso, national: digitsOnly(compact) };
}

/**
 * @returns {{ ok: boolean, message: string|null, e164: string }}
 */
export function validatePhoneForCountry(iso, national) {
  const country = getPhoneCountry(iso);
  const nationalDigits = digitsOnly(national);

  if (!nationalDigits) {
    return { ok: false, message: 'Le téléphone est obligatoire.', e164: '' };
  }

  const parsed = parsePhoneNumberFromString(nationalDigits, country.iso);
  if (!parsed || !parsed.isValid()) {
    return {
      ok: false,
      message: `Le numéro n’est pas valide pour ${country.flag} ${country.name} (+${country.dial}).`,
      e164: '',
    };
  }

  if (parsed.country && parsed.country !== country.iso) {
    return {
      ok: false,
      message: `Ce numéro correspond à un autre pays. Sélectionnez ${countryFlag(parsed.country)} ${countryName(parsed.country)}.`,
      e164: '',
    };
  }

  return {
    ok: true,
    message: null,
    e164: parsed.format('E.164'),
  };
}
