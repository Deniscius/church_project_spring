/**
 * Valide le brouillon avant envoi (règles alignées sur DemandeServiceImpl).
 * @returns {{ ok: boolean, errors: string[] }}
 */
export function validatePublicDemandeDraft(draft) {
  const errors = [];

  const req = (cond, msg) => {
    if (!cond) errors.push(msg);
  };

  req(draft.intention?.trim(), 'L’intention est obligatoire.');
  req(draft.nomFidele?.trim(), 'Le nom du fidèle est obligatoire.');
  req(draft.prenomFidele?.trim(), 'Le prénom du fidèle est obligatoire.');
  req(draft.telFidele?.trim(), 'Le téléphone est obligatoire.');
  req(draft.paroissePublicId, 'Choisissez une paroisse.');
  req(draft.typeDemandePublicId, 'Choisissez un type de demande.');
  req(draft.forfaitTarifPublicId, 'Choisissez un forfait.');
  req(draft.typePaiementPublicId, 'Choisissez un mode de paiement.');
  req(draft.dateDebut?.trim(), 'La date de début est obligatoire.');

  const n = draft.forfaitNombreCelebration;
  req(n != null && n > 0, 'Le forfait doit prévoir au moins une célébration.');

  const hp = Boolean(draft.forfaitHeurePersonnalise);
  const hasHoraire = Boolean(draft.horairePublicId);
  const hasHeurePerso = Boolean(draft.heurePersonnalisee?.trim());

  if (hp) {
    req(
      hasHoraire || hasHeurePerso,
      'Indiquez un horaire de paroisse ou une heure personnalisée.'
    );
  } else {
    req(hasHoraire, 'Un horaire fixe est obligatoire pour ce forfait.');
    req(!hasHeurePerso, 'L’heure personnalisée n’est pas autorisée pour ce forfait.');
  }

  return { ok: errors.length === 0, errors };
}

/**
 * Corps JSON pour POST /demandes (champs attendus par le backend).
 */
export function buildDemandeRequestBody(draft) {
  const heure = draft.heurePersonnalisee?.trim();
  let heurePersonnalisee = null;
  if (heure) {
    const [h, m] = heure.split(':');
    if (h != null && m != null) {
      heurePersonnalisee = `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:00`;
    }
  }

  const email = draft.emailFidele?.trim();
  const coursier = draft.nomCoursier?.trim();

  return {
    intention: draft.intention.trim(),
    nomFidele: draft.nomFidele.trim(),
    prenomFidele: draft.prenomFidele.trim(),
    telFidele: draft.telFidele.trim(),
    emailFidele: email || null,
    nomCoursier: coursier || null,
    heurePersonnalisee,
    dateDebut: draft.dateDebut.trim(),
    paroissePublicId: draft.paroissePublicId,
    typeDemandePublicId: draft.typeDemandePublicId,
    forfaitTarifPublicId: draft.forfaitTarifPublicId,
    horairePublicId: draft.horairePublicId || null,
    userPublicId: null,
    typePaiementPublicId: draft.typePaiementPublicId,
  };
}

export const PUBLIC_DEMANDE_RESULT_KEY = 'public_demande_dernier_resultat';

export function persistDemandeCreationResult(result) {
  try {
    sessionStorage.setItem(PUBLIC_DEMANDE_RESULT_KEY, JSON.stringify(result));
  } catch {
    /* ignore */
  }
}

export function readDemandeCreationResult() {
  try {
    const raw = sessionStorage.getItem(PUBLIC_DEMANDE_RESULT_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

export function clearDemandeCreationResult() {
  sessionStorage.removeItem(PUBLIC_DEMANDE_RESULT_KEY);
}
