/**
 * Adapte la réponse JWT du backend vers le modèle utilisateur attendu par l’UI.
 */
export function mapJwtToUser(jwt) {
  const rawRole = jwt.roles?.[0] ?? 'ROLE_USER';
  const role = String(rawRole).replace(/^ROLE_/, '') || 'USER';

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
  };
}
