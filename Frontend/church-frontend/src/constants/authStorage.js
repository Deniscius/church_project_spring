export const AUTH_TOKEN_KEY = 'church_auth_token';
export const AUTH_USER_KEY = 'church_auth_user';
export const AUTH_PAROISSES_KEY = 'paroisses';
export const AUTH_SELECTED_PAROISSE_KEY = 'selectedParoisse';
export const ACTIVE_PARISH_KEY = 'church_active_parish_id';

const AUTH_KEYS = [
  AUTH_TOKEN_KEY,
  AUTH_USER_KEY,
  AUTH_PAROISSES_KEY,
  AUTH_SELECTED_PAROISSE_KEY,
  ACTIVE_PARISH_KEY,
];

/** Persist auth across reloads and browser restarts (cleared on logout). */
export const authStorage = localStorage;

/** Migrate legacy sessionStorage auth → localStorage once. */
export function migrateAuthStorage() {
  try {
    AUTH_KEYS.forEach((key) => {
      if (authStorage.getItem(key) != null) return;
      const legacy = sessionStorage.getItem(key);
      if (legacy != null) {
        authStorage.setItem(key, legacy);
        sessionStorage.removeItem(key);
      }
    });
  } catch {
    // ignore storage access errors
  }
}

export function clearAuthStorage() {
  AUTH_KEYS.forEach((key) => {
    authStorage.removeItem(key);
    sessionStorage.removeItem(key);
  });
}

export function getAccessToken() {
  migrateAuthStorage();
  return authStorage.getItem(AUTH_TOKEN_KEY);
}

export function getActiveParishId() {
  migrateAuthStorage();
  return authStorage.getItem(ACTIVE_PARISH_KEY);
}

export function setActiveParishId(publicId) {
  if (publicId) authStorage.setItem(ACTIVE_PARISH_KEY, publicId);
  else authStorage.removeItem(ACTIVE_PARISH_KEY);
}
