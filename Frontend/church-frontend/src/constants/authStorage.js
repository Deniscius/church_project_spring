export const AUTH_TOKEN_KEY = 'church_auth_token';
export const AUTH_USER_KEY = 'church_auth_user';
export const AUTH_PAROISSES_KEY = 'paroisses';
export const AUTH_SELECTED_PAROISSE_KEY = 'selectedParoisse';
export const ACTIVE_PARISH_KEY = 'church_active_parish_id';
/** Marqueur local : une session cookie est attendue (sans stocker le JWT). */
export const AUTH_SESSION_FLAG_KEY = 'church_auth_session';

const AUTH_KEYS = [
  AUTH_TOKEN_KEY,
  AUTH_USER_KEY,
  AUTH_PAROISSES_KEY,
  AUTH_SELECTED_PAROISSE_KEY,
  ACTIVE_PARISH_KEY,
  AUTH_SESSION_FLAG_KEY,
];

/** Profil utilisateur / paroisses uniquement — jamais le JWT. */
export const authStorage = localStorage;

/** Migrate legacy sessionStorage auth → localStorage once. */
export function migrateAuthStorage() {
  try {
    AUTH_KEYS.forEach((key) => {
      if (authStorage.getItem(key) != null) return;
      const legacy = sessionStorage.getItem(key);
      if (legacy != null) {
        // Ancien JWT en clair : on ne le migre pas.
        if (key === AUTH_TOKEN_KEY) {
          sessionStorage.removeItem(key);
          return;
        }
        authStorage.setItem(key, legacy);
        sessionStorage.removeItem(key);
      }
    });
    // Purge systématique de tout JWT restant côté client.
    authStorage.removeItem(AUTH_TOKEN_KEY);
    sessionStorage.removeItem(AUTH_TOKEN_KEY);
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

/**
 * @deprecated Le JWT n'est plus stocké côté client (cookie HttpOnly).
 * Conservé pour compat : renvoie toujours null.
 */
export function getAccessToken() {
  migrateAuthStorage();
  return null;
}

export function hasSessionFlag() {
  migrateAuthStorage();
  return authStorage.getItem(AUTH_SESSION_FLAG_KEY) === '1';
}

export function setSessionFlag(on) {
  if (on) authStorage.setItem(AUTH_SESSION_FLAG_KEY, '1');
  else authStorage.removeItem(AUTH_SESSION_FLAG_KEY);
}

export function getActiveParishId() {
  migrateAuthStorage();
  return authStorage.getItem(ACTIVE_PARISH_KEY);
}

export function setActiveParishId(publicId) {
  if (publicId) authStorage.setItem(ACTIVE_PARISH_KEY, publicId);
  else authStorage.removeItem(ACTIVE_PARISH_KEY);
}
