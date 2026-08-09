/* eslint-disable react-refresh/only-export-components */
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  useSyncExternalStore,
} from 'react';
import {
  applyThemeToDocument,
  normalizeThemePreference,
  readStoredThemePreference,
  resolveTheme,
  storeThemePreference,
  THEME_PREFERENCES,
} from '../constants/theme';

const ThemeContext = createContext(null);

function subscribeSystemDark(callback) {
  const mq = window.matchMedia('(prefers-color-scheme: dark)');
  const handler = () => callback();
  mq.addEventListener('change', handler);
  return () => mq.removeEventListener('change', handler);
}

function getSystemDarkSnapshot() {
  return window.matchMedia('(prefers-color-scheme: dark)').matches;
}

function getServerSystemDarkSnapshot() {
  return false;
}

export function ThemeProvider({ children }) {
  const [preference, setPreferenceState] = useState(() => readStoredThemePreference());
  const systemDark = useSyncExternalStore(
    subscribeSystemDark,
    getSystemDarkSnapshot,
    getServerSystemDarkSnapshot
  );

  const resolved = resolveTheme(preference, systemDark);

  useEffect(() => {
    applyThemeToDocument(resolved, preference);
  }, [resolved, preference]);

  const setPreference = useCallback((next) => {
    const normalized = normalizeThemePreference(next);
    storeThemePreference(normalized);
    setPreferenceState(normalized);
  }, []);

  const cyclePreference = useCallback(() => {
    const idx = THEME_PREFERENCES.indexOf(preference);
    const next = THEME_PREFERENCES[(idx + 1) % THEME_PREFERENCES.length];
    setPreference(next);
  }, [preference, setPreference]);

  const value = useMemo(
    () => ({
      preference,
      resolved,
      systemDark,
      setPreference,
      cyclePreference,
    }),
    [preference, resolved, systemDark, setPreference, cyclePreference]
  );

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
}

export function useTheme() {
  const context = useContext(ThemeContext);
  if (!context) {
    throw new Error('useTheme must be used inside ThemeProvider');
  }
  return context;
}
