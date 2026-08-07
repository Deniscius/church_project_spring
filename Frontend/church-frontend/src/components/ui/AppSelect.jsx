import React, {
  useCallback,
  useEffect,
  useId,
  useMemo,
  useRef,
  useState,
} from 'react';

/**
 * Liste déroulante contrôlée : le contenu des options n’est rendu
 * dans le DOM qu’après ouverture (pas d’exposition HTML tant que fermé).
 *
 * @param {{ value: string, label: string, disabled?: boolean }[]} options
 */
export default function AppSelect({
  id,
  name,
  value = '',
  onChange,
  options = [],
  placeholder = '— Choisir —',
  disabled = false,
  required = false,
  searchable = false,
  searchPlaceholder = 'Rechercher…',
  className = '',
  'aria-label': ariaLabel,
  'aria-describedby': ariaDescribedBy,
  'aria-invalid': ariaInvalid,
}) {
  const reactId = useId();
  const listboxId = `${id || reactId}-listbox`;
  const triggerRef = useRef(null);
  const panelRef = useRef(null);
  const searchRef = useRef(null);
  const [open, setOpen] = useState(false);
  /** Ne monte le panneau qu’après la 1ʳᵉ ouverture. */
  const [hasOpened, setHasOpened] = useState(false);
  const [query, setQuery] = useState('');
  const [highlight, setHighlight] = useState(-1);

  const selected = useMemo(
    () => options.find((o) => String(o.value) === String(value)) || null,
    [options, value]
  );

  const filtered = useMemo(() => {
    if (!searchable || !query.trim()) return options;
    const q = query.trim().toLowerCase();
    return options.filter((o) => String(o.label).toLowerCase().includes(q));
  }, [options, searchable, query]);

  const close = useCallback(() => {
    setOpen(false);
    setQuery('');
    setHighlight(-1);
  }, []);

  const openPanel = useCallback(() => {
    if (disabled) return;
    setHasOpened(true);
    setOpen(true);
  }, [disabled]);

  const toggle = () => {
    if (open) close();
    else openPanel();
  };

  const pick = (option) => {
    if (!option || option.disabled) return;
    onChange?.(option.value, option);
    close();
    triggerRef.current?.focus();
  };

  useEffect(() => {
    if (!open) return undefined;
    const onDoc = (event) => {
      const t = event.target;
      if (triggerRef.current?.contains(t) || panelRef.current?.contains(t)) return;
      close();
    };
    const onKey = (event) => {
      if (event.key === 'Escape') {
        event.preventDefault();
        close();
        triggerRef.current?.focus();
      }
    };
    document.addEventListener('mousedown', onDoc);
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('mousedown', onDoc);
      document.removeEventListener('keydown', onKey);
    };
  }, [open, close]);

  useEffect(() => {
    if (!open) return undefined;
    setHighlight(filtered.findIndex((o) => String(o.value) === String(value)));
    if (searchable) {
      const t = window.setTimeout(() => searchRef.current?.focus(), 0);
      return () => window.clearTimeout(t);
    }
    return undefined;
  }, [open, filtered, value, searchable]);

  const onTriggerKeyDown = (event) => {
    if (disabled) return;
    if (searchable && event.key.length === 1 && !event.ctrlKey && !event.metaKey && !event.altKey) {
      event.preventDefault();
      openPanel();
      setQuery(event.key);
      setHighlight(0);
      return;
    }
    if (event.key === 'ArrowDown' || event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      if (!open) openPanel();
      else if (event.key === 'ArrowDown') {
        setHighlight((h) => Math.min(filtered.length - 1, Math.max(0, h + 1)));
      }
    }
  };

  const onPanelKeyDown = (event) => {
    if (event.key === 'ArrowDown') {
      event.preventDefault();
      setHighlight((h) => Math.min(filtered.length - 1, h + 1));
    } else if (event.key === 'ArrowUp') {
      event.preventDefault();
      setHighlight((h) => Math.max(0, h - 1));
    } else if (event.key === 'Enter') {
      event.preventDefault();
      const opt = filtered[highlight];
      if (opt) pick(opt);
    } else if (event.key === 'Home') {
      event.preventDefault();
      setHighlight(0);
    } else if (event.key === 'End') {
      event.preventDefault();
      setHighlight(filtered.length - 1);
    }
  };

  return (
    <div className={`app-select ${open ? 'is-open' : ''} ${className}`.trim()}>
      <input
        type="hidden"
        name={name}
        value={value || ''}
        required={required}
        disabled={disabled}
        readOnly
      />
      <button
        type="button"
        id={id}
        ref={triggerRef}
        className="app-select-trigger select"
        disabled={disabled}
        aria-haspopup="listbox"
        aria-expanded={open}
        aria-controls={hasOpened ? listboxId : undefined}
        aria-label={ariaLabel}
        aria-describedby={ariaDescribedBy}
        aria-invalid={ariaInvalid}
        aria-required={required || undefined}
        onClick={toggle}
        onKeyDown={onTriggerKeyDown}
      >
        <span className={`app-select-value${selected ? '' : ' is-placeholder'}`}>
          {selected ? selected.label : placeholder}
        </span>
        <span className="app-select-chevron" aria-hidden="true" />
      </button>

      {hasOpened ? (
        <div
          ref={panelRef}
          className={`app-select-panel${open ? ' is-visible' : ''}`}
          hidden={!open}
          onKeyDown={onPanelKeyDown}
        >
          {searchable ? (
            <div className="app-select-search">
              <input
                ref={searchRef}
                type="search"
                className="input"
                value={query}
                autoComplete="off"
                autoCorrect="off"
                spellCheck={false}
                placeholder={searchPlaceholder}
                aria-label={searchPlaceholder}
                onChange={(e) => {
                  setQuery(e.target.value);
                  setHighlight(0);
                }}
              />
            </div>
          ) : null}
          <ul
            id={listboxId}
            className="app-select-options"
            role="listbox"
            aria-label={ariaLabel || placeholder}
          >
            {!required ? (
              <li role="presentation">
                <button
                  type="button"
                  role="option"
                  className={`app-select-option${value === '' ? ' is-selected' : ''}`}
                  aria-selected={value === ''}
                  onClick={() => pick({ value: '', label: placeholder })}
                >
                  {placeholder}
                </button>
              </li>
            ) : null}
            {filtered.length === 0 ? (
              <li className="app-select-empty muted" role="presentation">
                Aucun résultat
              </li>
            ) : (
              filtered.map((option, index) => {
                const selectedOpt = String(option.value) === String(value);
                const active = index === highlight;
                return (
                  <li key={String(option.value)} role="presentation">
                    <button
                      type="button"
                      role="option"
                      className={`app-select-option${selectedOpt ? ' is-selected' : ''}${active ? ' is-active' : ''}`}
                      aria-selected={selectedOpt}
                      disabled={option.disabled}
                      onMouseEnter={() => setHighlight(index)}
                      onClick={() => pick(option)}
                    >
                      {option.label}
                    </button>
                  </li>
                );
              })
            )}
          </ul>
        </div>
      ) : null}
    </div>
  );
}
