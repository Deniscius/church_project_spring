export function scopeLabel(parish) {
  if (!parish) return 'Aucune paroisse active';
  return `${parish.name} · ${parish.city}`;
}
