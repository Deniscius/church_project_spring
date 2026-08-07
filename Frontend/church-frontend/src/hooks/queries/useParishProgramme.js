import { useQuery } from '@tanstack/react-query';
import { scheduleService } from '../../services/schedule.service';

export const parishProgrammeKeys = {
  all: (parishId) => ['horaires', 'programme', parishId],
  range: (parishId, debut, fin) => [...parishProgrammeKeys.all(parishId), debut, fin],
};

function defaultRange() {
  const debut = new Date();
  const fin = new Date();
  fin.setDate(fin.getDate() + 13);
  const toIso = (d) => d.toLocaleDateString('en-CA', { timeZone: 'Africa/Lome' });
  return { debut: toIso(debut), fin: toIso(fin) };
}

/** Programme résolu de la paroisse (messe unique respectée). */
export function useParishProgrammeQuery(parishId, options = {}) {
  const range = options.debut && options.fin
    ? { debut: options.debut, fin: options.fin }
    : defaultRange();

  return useQuery({
    queryKey: parishProgrammeKeys.range(parishId, range.debut, range.fin),
    queryFn: ({ signal }) =>
      scheduleService.getProgramme(parishId, { ...range, signal }),
    enabled: Boolean(parishId),
    staleTime: 2 * 60_000,
  });
}
