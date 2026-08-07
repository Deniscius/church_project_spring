export const PRIMARY_REQUEST_TYPES = ['EUCHARISTIE', 'SACRAMENTAL', 'SACREMENT'];
export const PAYMENT_TYPES = ['TMONEY', 'FLOOZ', 'ESPECES', 'CARTE'];
export const WEEK_DAYS = ['DIMANCHE', 'LUNDI', 'MARDI', 'MERCREDI', 'JEUDI', 'VENDREDI', 'SAMEDI'];

export const WEEK_DAY_LABELS = {
  DIMANCHE: 'Dimanche',
  LUNDI: 'Lundi',
  MARDI: 'Mardi',
  MERCREDI: 'Mercredi',
  JEUDI: 'Jeudi',
  VENDREDI: 'Vendredi',
  SAMEDI: 'Samedi',
};

export const NATURE_FORFAIT_OPTIONS = [
  {
    value: 'NORMALE',
    label: 'Messe normale',
    hint: 'Célébration standard, validation automatique après paiement.',
  },
  {
    value: 'DOMINICALE',
    label: 'Messe dominicale',
    hint: 'Célébration du dimanche selon le calendrier paroissial.',
  },
  {
    value: 'SPECIALE',
    label: 'Messe spéciale',
    hint: 'Solennité ou célébration exceptionnelle — validation paroissiale et honoraire dédié.',
  },
];

export const NATURE_FORFAIT_LABELS = Object.fromEntries(
  NATURE_FORFAIT_OPTIONS.map(({ value, label }) => [value, label])
);

/** Libellés métier selon le nombre de célébrations (triduum / neuvaine / trentaine). */
export const FORFAIT_DUREE_LABELS = {
  1: 'Célébration unique',
  3: 'Triduum',
  9: 'Neuvaine',
  30: 'Trentaine',
};

export function getForfaitDureeLabel(nombreCelebration) {
  if (nombreCelebration == null) return 'Forfait';
  return FORFAIT_DUREE_LABELS[nombreCelebration]
    || `${nombreCelebration} célébrations`;
}

export function isMultiCelebrationForfait(nombreCelebration) {
  return nombreCelebration != null && Number(nombreCelebration) > 1;
}
