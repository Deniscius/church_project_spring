import React, { useMemo } from 'react';
import AppSelect from './AppSelect';
import {
  DEFAULT_PHONE_COUNTRY_ISO,
  getPhoneCountry,
  PHONE_COUNTRIES,
  toE164,
  validatePhoneForCountry,
  digitsOnly,
} from '../../utils/phone';

/**
 * Sélecteur pays + numéro national (stockage E.164 via onChange).
 * @param {boolean} required — si false, vide = OK ; sinon validation stricte.
 */
export default function PhoneField({
  id = 'phone',
  countryIso = DEFAULT_PHONE_COUNTRY_ISO,
  national = '',
  onChange,
  required = false,
  disabled = false,
  helpHint,
}) {
  const iso = countryIso || DEFAULT_PHONE_COUNTRY_ISO;
  const country = getPhoneCountry(iso);
  const hasDigits = Boolean(digitsOnly(national));
  const check = hasDigits || required
    ? validatePhoneForCountry(iso, national)
    : { ok: true, message: null, e164: '' };

  const countryOptions = useMemo(
    () => PHONE_COUNTRIES.map((c) => ({
      value: c.iso,
      label: `${c.flag} ${c.name} (+${c.dial})`,
    })),
    []
  );

  function apply(nextIso, nextNational) {
    const e164 = toE164(nextIso, nextNational) || '';
    onChange?.({
      countryIso: nextIso,
      national: nextNational,
      e164,
    });
  }

  return (
    <div className="stack" style={{ gap: 6 }}>
      <div className="phone-row">
        <AppSelect
          id={`${id}-country`}
          className="phone-country"
          searchable
          searchPlaceholder="Pays…"
          value={iso}
          options={countryOptions}
          disabled={disabled}
          onChange={(nextIso) => apply(nextIso, national || '')}
        />
        <input
          id={`${id}-national`}
          type="tel"
          className="input"
          autoComplete="tel-national"
          inputMode="tel"
          value={national || ''}
          disabled={disabled}
          onChange={(e) => apply(iso, e.target.value)}
          aria-invalid={hasDigits && !check.ok}
          aria-describedby={`${id}-hint`}
          placeholder="Numéro sans indicatif"
        />
      </div>
      <small
        id={`${id}-hint`}
        className={hasDigits && !check.ok ? 'text-red-600' : 'muted'}
      >
        {hasDigits && !check.ok
          ? check.message
          : (helpHint || `${country?.flag || ''} +${country?.dial || ''} — numéro national sans l’indicatif.`)}
      </small>
    </div>
  );
}
