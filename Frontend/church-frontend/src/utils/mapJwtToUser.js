/**
 * Adapte la réponse d'authentification legacy vers le modèle utilisateur UI.
 */
export function mapJwtToUser(jwt) {
  const rawRole = jwt.roles?.find((value) => String(value).startsWith('ROLE_')) || jwt.roles?.[0];
  const role = String(rawRole || '').replace(/^ROLE_/, '');

  const full = (jwt.fullName || '').trim();
  const space = full.indexOf(' ');
  const firstName = space === -1 ? full : full.slice(0, space);
  const lastName = space === -1 ? '' : full.slice(space + 1).trim();

  return {
    id: jwt.publicId,
    firstName,
    lastName,
    username: jwt.username,
    role,
    permissions: Array.isArray(jwt.permissions) ? jwt.permissions.filter(Boolean) : [],
    isGlobal: Boolean(jwt.isGlobal),
  };
}
