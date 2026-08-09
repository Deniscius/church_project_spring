/**
 * Banques agréées au Togo (Commission Bancaire de l’UMOA / BCEAO).
 * `code` = code banque RIB/IBAN (5 chiffres) lorsqu’il est connu.
 * Source indicative : liste CB-UMOA Togo + codes IBAN courants.
 */
export const BANQUE_AUTRE_VALUE = '__autre__';

/** @type {{ value: string, label: string, code?: string }[]} */
export const BANQUES_TOGO = [
  { value: 'Ecobank Togo', label: 'Ecobank Togo', code: '00906' },
  { value: 'Orabank Togo', label: 'Orabank Togo', code: '00904' },
  { value: 'Banque Atlantique Togo', label: 'Banque Atlantique Togo', code: '00801' },
  { value: 'Union Togolaise de Banque (UTB)', label: 'Union Togolaise de Banque (UTB)', code: '00301' },
  { value: 'Bank of Africa — Togo (BOA)', label: 'Bank of Africa — Togo (BOA)' },
  { value: 'Banque Internationale pour l’Afrique au Togo (BIA)', label: 'Banque Internationale pour l’Afrique au Togo (BIA)' },
  { value: 'BSIC — Togo', label: 'BSIC — Togo' },
  { value: 'Coris Bank International — Togo', label: 'Coris Bank International — Togo' },
  { value: 'International Business Bank Togo (IB Bank)', label: 'International Business Bank Togo (IB Bank)' },
  { value: 'Société Interafricaine de Banque (SIAB)', label: 'Société Interafricaine de Banque (SIAB)' },
  { value: 'SUNU Bank Togo', label: 'SUNU Bank Togo' },
  { value: 'NSIA Banque (succursale du Togo)', label: 'NSIA Banque (succursale du Togo)' },
  { value: 'Société Générale (succursale du Togo)', label: 'Société Générale (succursale du Togo)' },
  { value: 'Banque de Développement du Mali (succ. Togo)', label: 'Banque de Développement du Mali (succ. Togo)' },
];

export const BANQUE_SELECT_OPTIONS = [
  ...BANQUES_TOGO,
  { value: BANQUE_AUTRE_VALUE, label: 'Autre banque…' },
];

/** @deprecated alias — utiliser BANQUES_TOGO */
export const BANQUES_BENIN = BANQUES_TOGO;

/**
 * @param {string | null | undefined} nomBanque
 * @returns {string}
 */
export function resolveBanqueSelectValue(nomBanque) {
  const raw = (nomBanque || '').trim();
  if (!raw) return '';
  const exact = BANQUES_TOGO.find(
    (b) => b.value.toLowerCase() === raw.toLowerCase() || b.label.toLowerCase() === raw.toLowerCase()
  );
  if (exact) return exact.value;
  return BANQUE_AUTRE_VALUE;
}

/**
 * @param {string} selectValue
 * @param {string} customName
 * @returns {string}
 */
export function resolveBanqueStoredName(selectValue, customName = '') {
  if (!selectValue) return '';
  if (selectValue === BANQUE_AUTRE_VALUE) return (customName || '').trim();
  const found = BANQUES_TOGO.find((b) => b.value === selectValue);
  return found ? found.value : (customName || selectValue).trim();
}

/**
 * @param {string | null | undefined} nomBanque
 * @returns {string | null}
 */
export function bankCodeForNomBanque(nomBanque) {
  const select = resolveBanqueSelectValue(nomBanque);
  if (!select || select === BANQUE_AUTRE_VALUE) return null;
  return BANQUES_TOGO.find((b) => b.value === select)?.code || null;
}
