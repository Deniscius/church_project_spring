import { BANQUES_TOGO, BANQUE_AUTRE_VALUE, resolveBanqueSelectValue } from '../constants/banquesTogo';

/**
 * Normalise un RIB / IBAN (espaces, tirets, casse).
 * @param {string | null | undefined} raw
 */
export function normalizeRib(raw) {
  return String(raw || '')
    .toUpperCase()
    .replace(/[\s\-._]/g, '');
}

/**
 * MOD-97 ISO 13616 (reste doit être 1).
 * @param {string} iban compact uppercase
 */
export function ibanMod97Valid(iban) {
  if (!iban || iban.length < 5) return false;
  const rearranged = iban.slice(4) + iban.slice(0, 4);
  let numeric = '';
  for (const ch of rearranged) {
    if (ch >= 'A' && ch <= 'Z') numeric += String(ch.charCodeAt(0) - 55);
    else if (ch >= '0' && ch <= '9') numeric += ch;
    else return false;
  }
  let remainder = 0;
  for (let i = 0; i < numeric.length; i += 1) {
    remainder = (remainder * 10 + (numeric.charCodeAt(i) - 48)) % 97;
  }
  return remainder === 1;
}

/**
 * Clé RIB française (23 caractères : 5+5+11+2).
 * @param {string} rib compact
 */
export function frenchRibKeyValid(rib) {
  if (!/^[A-Z0-9]{23}$/.test(rib)) return false;
  const bank = rib.slice(0, 5);
  const branch = rib.slice(5, 10);
  const account = rib.slice(10, 21);
  const key = Number(rib.slice(21, 23));
  if (!Number.isInteger(key) || key < 0 || key > 97) return false;

  const toNumber = (value) => {
    let out = '';
    for (const ch of value) {
      if (ch >= '0' && ch <= '9') out += ch;
      else if (ch >= 'A' && ch <= 'I') out += String(ch.charCodeAt(0) - 64);
      else if (ch >= 'J' && ch <= 'R') out += String(ch.charCodeAt(0) - 73);
      else if (ch >= 'S' && ch <= 'Z') out += String(ch.charCodeAt(0) - 81);
      else return null;
    }
    return out;
  };

  const b = toNumber(bank);
  const g = toNumber(branch);
  const c = toNumber(account);
  if (b == null || g == null || c == null) return false;

  const mod = (str, m) => {
    let r = 0;
    for (let i = 0; i < str.length; i += 1) {
      r = (r * 10 + (str.charCodeAt(i) - 48)) % m;
    }
    return r;
  };

  const expected = 97 - ((89 * mod(b, 97) + 15 * mod(g, 97) + 3 * mod(c, 97)) % 97);
  const normalized = expected === 0 ? 97 : expected;
  return normalized === key;
}

/**
 * Extrait le code banque (5 chiffres) depuis un RIB/IBAN togolais.
 * BBAN : TG + codeBanque(5) + …
 * @param {string} compact
 * @returns {string | null}
 */
export function extractTogoBankCode(compact) {
  if (!compact) return null;
  if (compact.startsWith('TG') && compact.length === 28 && compact.slice(4, 6) === 'TG') {
    const code = compact.slice(6, 11);
    return /^\d{5}$/.test(code) ? code : null;
  }
  if (/^TG\d{22}$/.test(compact) && compact.length === 24) {
    const code = compact.slice(2, 7);
    return /^\d{5}$/.test(code) ? code : null;
  }
  return null;
}

/**
 * @param {string | null | undefined} nomBanque
 * @returns {string | null}
 */
export function expectedBankCodeForNomBanque(nomBanque) {
  const select = resolveBanqueSelectValue(nomBanque);
  if (!select || select === BANQUE_AUTRE_VALUE) return null;
  const bank = BANQUES_TOGO.find((b) => b.value === select);
  return bank?.code || null;
}

/**
 * Valide un RIB / IBAN togolais et sa cohérence avec la banque choisie.
 *
 * @param {string | null | undefined} rawRib
 * @param {string | null | undefined} nomBanque
 * @returns {{ valid: boolean, message?: string, normalized?: string, bankCode?: string | null }}
 */
export function validateRibTogo(rawRib, nomBanque) {
  const compact = normalizeRib(rawRib);
  if (!compact) {
    return { valid: false, message: 'Le RIB / IBAN est obligatoire.' };
  }

  let bankCode = null;
  let kind = null;

  if (compact.startsWith('TG') && compact.length === 28) {
    if (!/^TG\d{2}TG\d{22}$/.test(compact)) {
      return {
        valid: false,
        message:
          'IBAN Togo invalide : format attendu TG + clé + TG + code banque (5) + guichet + compte (28 caractères).',
      };
    }
    if (!ibanMod97Valid(compact)) {
      return {
        valid: false,
        message: 'IBAN invalide : la clé de contrôle est incorrecte (erreur de saisie probable).',
      };
    }
    bankCode = extractTogoBankCode(compact);
    kind = 'iban';
  } else if (/^TG\d{22}$/.test(compact) && compact.length === 24) {
    bankCode = extractTogoBankCode(compact);
    kind = 'bban';
  } else if (/^[A-Z0-9]{23}$/.test(compact) && compact.length === 23) {
    if (!frenchRibKeyValid(compact)) {
      return {
        valid: false,
        message: 'RIB invalide : la clé RIB (2 derniers chiffres) ne correspond pas au compte.',
      };
    }
    kind = 'rib-fr';
  } else {
    return {
      valid: false,
      message:
        'Format non reconnu. Utilisez un IBAN Togo (28 car., ex. TG53 TG00 9060 …) ou le RIB domestique (24 car., ex. TG00906…).',
    };
  }

  const expected = expectedBankCodeForNomBanque(nomBanque);
  if (expected && bankCode && bankCode !== expected) {
    const bank = BANQUES_TOGO.find((b) => b.code === expected);
    return {
      valid: false,
      message: `Ce RIB appartient à la banque ${bankCode}, pas à « ${bank?.label || nomBanque} » (code attendu ${expected}).`,
      bankCode,
      normalized: compact,
    };
  }

  if (expected && kind === 'rib-fr') {
    return {
      valid: false,
      message:
        'Pour vérifier la banque, saisissez le RIB / IBAN au format Togo (commençant par TG…).',
    };
  }

  return {
    valid: true,
    normalized: compact,
    bankCode,
    message:
      kind === 'iban'
        ? 'IBAN Togo valide'
          + (expected ? ' et cohérent avec la banque.' : '.')
        : kind === 'bban'
          ? 'RIB domestique Togo valide'
            + (expected ? ' et cohérent avec la banque.' : '.')
          : 'RIB valide (clé correcte).',
  };
}

/** @deprecated alias — utiliser validateRibTogo */
export const validateRibBenin = validateRibTogo;
