export const AUTH_TOKEN_KEY = 'church_auth_token';
export const AUTH_USER_KEY = 'church_auth_user';
export const ACTIVE_PARISH_KEY = 'church_active_parish_id';

export function getAccessToken() {
  return sessionStorage.getItem(AUTH_TOKEN_KEY);
}

export function getActiveParishId() {
  return sessionStorage.getItem(ACTIVE_PARISH_KEY);
}

export function setActiveParishId(publicId) {
  if (publicId) sessionStorage.setItem(ACTIVE_PARISH_KEY, publicId);
  else sessionStorage.removeItem(ACTIVE_PARISH_KEY);
}
