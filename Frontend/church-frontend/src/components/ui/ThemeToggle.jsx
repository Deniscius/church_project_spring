import React, { useEffect, useId, useRef, useState } from 'react';
import AppIcon from './AppIcon';
import { useTheme } from '../../store/theme.context';
import { themePreferenceLabel } from '../../constants/theme';

const OPTIONS = [
  { value: 'system', label: 'Système', icon: 'system', hint: 'Suit les préférences de l’appareil' },
  { value: 'light', label: 'Clair', icon: 'sun', hint: 'Toujours le thème clair' },
  { value: 'dark', label: 'Sombre', icon: 'moon', hint: 'Toujours le thème sombre' },
];

/**
 * @param {{ compact?: boolean, className?: string }} props
 */
export default function ThemeToggle({ compact = false, className = '' }) {
  const { preference, resolved, setPreference } = useTheme();
  const [open, setOpen] = useState(false);
  const rootRef = useRef(null);
  const menuId = useId();

  useEffect(() => {
    if (!open) return undefined;
    const onPointer = (e) => {
      if (rootRef.current && !rootRef.current.contains(e.target)) setOpen(false);
    };
    const onKey = (e) => {
      if (e.key === 'Escape') setOpen(false);
    };
    document.addEventListener('pointerdown', onPointer);
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('pointerdown', onPointer);
      document.removeEventListener('keydown', onKey);
    };
  }, [open]);

  const activeIcon = preference === 'system' ? 'system' : preference === 'dark' ? 'moon' : 'sun';
  const label = themePreferenceLabel(preference);
  const resolvedHint = resolved === 'dark' ? 'sombre' : 'clair';

  return (
    <div
      ref={rootRef}
      className={`theme-toggle${compact ? ' theme-toggle--compact' : ''}${open ? ' is-open' : ''} ${className}`.trim()}
    >
      <button
        type="button"
        className="theme-toggle-trigger"
        aria-haspopup="menu"
        aria-expanded={open}
        aria-controls={menuId}
        title={`Thème : ${label}${preference === 'system' ? ` (${resolvedHint})` : ''}`}
        aria-label={`Thème d’affichage : ${label}. Ouvrir les options.`}
        onClick={() => setOpen((v) => !v)}
      >
        <AppIcon name={activeIcon} size={compact ? 16 : 18} />
        {compact ? null : <span className="theme-toggle-label">{label}</span>}
      </button>

      <div
        id={menuId}
        className="theme-toggle-menu"
        role="menu"
        hidden={!open}
        aria-label="Choisir le thème"
      >
        {OPTIONS.map((opt) => (
          <button
            key={opt.value}
            type="button"
            role="menuitemradio"
            aria-checked={preference === opt.value}
            className={`theme-toggle-option${preference === opt.value ? ' is-active' : ''}`}
            title={opt.hint}
            onClick={() => {
              setPreference(opt.value);
              setOpen(false);
            }}
          >
            <AppIcon name={opt.icon} size={16} />
            <span>{opt.label}</span>
          </button>
        ))}
      </div>
    </div>
  );
}
