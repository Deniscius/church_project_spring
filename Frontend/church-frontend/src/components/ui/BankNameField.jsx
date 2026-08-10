import React, { useState } from 'react';
import AppSelect from './AppSelect';
import {
  BANQUE_AUTRE_VALUE,
  BANQUE_SELECT_OPTIONS,
  BANQUES_TOGO,
  resolveBanqueSelectValue,
  resolveBanqueStoredName,
} from '../../constants/banquesTogo';

/**
 * Liste déroulante des banques agréées au Togo + saisie libre « Autre ».
 *
 * @param {{
 *   id?: string,
 *   value?: string,
 *   onChange: (nomBanque: string) => void,
 *   disabled?: boolean,
 *   required?: boolean,
 * }} props
 */
export default function BankNameField({
  id = 'bank-name',
  value = '',
  onChange,
  disabled = false,
  required = false,
}) {
  const [selectValue, setSelectValue] = useState(() => resolveBanqueSelectValue(value));
  const [customName, setCustomName] = useState(
    () => (resolveBanqueSelectValue(value) === BANQUE_AUTRE_VALUE ? (value || '') : '')
  );
  const [prevValue, setPrevValue] = useState(value);
  if (value !== prevValue) {
    setPrevValue(value);
    const next = resolveBanqueSelectValue(value);
    setSelectValue(next);
    setCustomName(next === BANQUE_AUTRE_VALUE ? (value || '') : '');
  }

  const isOther = selectValue === BANQUE_AUTRE_VALUE;
  const selectedCode = BANQUES_TOGO.find((b) => b.value === selectValue)?.code;

  return (
    <div className="bank-name-field stack" style={{ gap: 8 }}>
      <AppSelect
        id={id}
        value={selectValue}
        options={BANQUE_SELECT_OPTIONS}
        placeholder="— Choisir une banque —"
        searchable
        searchPlaceholder="Rechercher une banque…"
        disabled={disabled}
        required={required && !isOther}
        aria-label="Banque"
        onChange={(next) => {
          setSelectValue(next);
          if (next === BANQUE_AUTRE_VALUE) {
            onChange?.(customName.trim());
            return;
          }
          setCustomName('');
          onChange?.(resolveBanqueStoredName(next));
        }}
      />
      {isOther ? (
        <div className="form-field" style={{ margin: 0 }}>
          <label htmlFor={`${id}-custom`}>Nom de la banque</label>
          <input
            id={`${id}-custom`}
            className="input"
            maxLength={120}
            required={required}
            disabled={disabled}
            placeholder="Saisir le nom de l’établissement"
            value={customName}
            onChange={(e) => {
              const next = e.target.value;
              setCustomName(next);
              onChange?.(next);
            }}
          />
        </div>
      ) : null}
      <small className="muted">
        Banques agréées au Togo (référence UMOA / BCEAO).
        {selectedCode
          ? ` Code RIB : ${selectedCode}.`
          : isOther
            ? ' Choisissez « Autre » si votre établissement n’apparaît pas.'
            : ' La cohérence RIB ↔ banque sera vérifiée dès que le code est connu.'}
      </small>
    </div>
  );
}
