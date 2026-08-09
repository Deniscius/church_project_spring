export const THEME_STORAGE_KEY = 'missanye_theme';

/** @typedef {'system' | 'light' | 'dark'} ThemePreference */
/** @typedef {'light' | 'dark'} ResolvedTheme */

export const THEME_PREFERENCES = /** @type {const} */ (['system', 'light', 'dark']);

/**
 * @param {string | null | undefined} raw
 * @returns {ThemePreference}
 */
export function normalizeThemePreference(raw) {
  if (raw === 'light' || raw === 'dark' || raw === 'system') return raw;
  return 'system';
}

/**
 * @param {ThemePreference} preference
 * @param {boolean} [systemDark]
 * @returns {ResolvedTheme}
 */
export function resolveTheme(preference, systemDark) {
  if (preference === 'light') return 'light';
  if (preference === 'dark') return 'dark';
  if (typeof systemDark === 'boolean') return systemDark ? 'dark' : 'light';
  if (typeof window !== 'undefined' && window.matchMedia) {
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
  }
  return 'light';
}

/**
 * @param {ResolvedTheme} resolved
 * @param {ThemePreference} [preference]
 */
export function applyThemeToDocument(resolved, preference = 'system') {
  const root = document.documentElement;
  root.setAttribute('data-theme', resolved);
  root.setAttribute('data-theme-pref', preference);
  root.style.colorScheme = resolved;

  const colorSchemeMeta = document.querySelector('meta[name="color-scheme"]');
  if (colorSchemeMeta) {
    colorSchemeMeta.setAttribute('content', preference === 'system' ? 'light dark' : resolved);
  }

  const themeColorMeta = document.querySelector('meta[name="theme-color"]');
  if (themeColorMeta) {
    themeColorMeta.setAttribute('content', resolved === 'dark' ? '#0e1520' : '#1a3a6b');
  }
}

export function readStoredThemePreference() {
  try {
    return normalizeThemePreference(localStorage.getItem(THEME_STORAGE_KEY));
  } catch {
    return 'system';
  }
}

/**
 * @param {ThemePreference} preference
 */
export function storeThemePreference(preference) {
  try {
    localStorage.setItem(THEME_STORAGE_KEY, preference);
  } catch {
    /* private mode */
  }
}

export function themePreferenceLabel(preference) {
  if (preference === 'light') return 'Clair';
  if (preference === 'dark') return 'Sombre';
  return 'Système';
}
