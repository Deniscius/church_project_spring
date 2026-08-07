import React, { useEffect, useId, useRef, useState } from 'react';

/**
 * Bulle d’aide : bouton « ? » qui ouvre un court texte guidant l’utilisateur.
 * Ouverture au clic / Entrée ; fermeture Esc, clic extérieur, blur.
 */
export default function HelpTip({
  text,
  label = 'Aide',
  placement = 'top',
  className = '',
}) {
  const tipId = useId();
  const rootRef = useRef(null);
  const [open, setOpen] = useState(false);

  useEffect(() => {
    if (!open) return undefined;
    const onDoc = (event) => {
      if (!rootRef.current?.contains(event.target)) {
        setOpen(false);
      }
    };
    const onKey = (event) => {
      if (event.key === 'Escape') setOpen(false);
    };
    document.addEventListener('mousedown', onDoc);
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('mousedown', onDoc);
      document.removeEventListener('keydown', onKey);
    };
  }, [open]);

  if (!text) return null;

  return (
    <span
      ref={rootRef}
      className={`help-tip help-tip--${placement} ${open ? 'is-open' : ''} ${className}`.trim()}
    >
      <button
        type="button"
        className="help-tip-trigger"
        aria-label={label}
        aria-expanded={open}
        aria-controls={tipId}
        onClick={(e) => {
          e.preventDefault();
          e.stopPropagation();
          setOpen((v) => !v);
        }}
      >
        ?
      </button>
      <span
        id={tipId}
        role="tooltip"
        className="help-tip-bubble"
        hidden={!open}
      >
        {text}
      </span>
    </span>
  );
}

/** Libellé de champ + bulle d’aide alignés. */
export function FieldLabel({ htmlFor, children, help, required = false }) {
  return (
    <label htmlFor={htmlFor} className="field-label-with-help">
      <span>
        {children}
        {required ? ' *' : ''}
      </span>
      {help ? <HelpTip text={help} label={`Aide : ${typeof children === 'string' ? children : 'champ'}`} /> : null}
    </label>
  );
}
