import { apiClient } from './http/apiClient';

function buildQuery({ date, inclureNonPayees, heures }) {
  const params = new URLSearchParams();
  params.set('date', date);
  if (inclureNonPayees) params.set('inclureNonPayees', 'true');
  // Une liste vide vaut « toutes les messes du jour » côté backend.
  (heures || []).forEach((heure) => params.append('heures', heure));
  return params.toString();
}

export const celebrationService = {
  listByParishAndDate: (paroissePublicId, { date, inclureNonPayees = false, heures = [] }) =>
    apiClient(
      `/celebrations/paroisse/${paroissePublicId}?${buildQuery({ date, inclureNonPayees, heures })}`,
      {},
      { auth: true }
    ),

  downloadFeuillePdf: async (paroissePublicId, { date, inclureNonPayees = false, heures = [] }) => {
    const query = buildQuery({ date, inclureNonPayees, heures });
    const headers = { Accept: 'application/pdf' };
    try {
      return await apiClient(
        `/celebrations/paroisse/${paroissePublicId}/feuille?${query}`,
        { headers },
        { auth: true, parse: 'blob' }
      );
    } catch (err) {
      if (err?.status === 404) {
        return apiClient(
          `/celebrations/paroisse/${paroissePublicId}/feuille.pdf?${query}`,
          { headers },
          { auth: true, parse: 'blob' }
        );
      }
      throw err;
    }
  },
};
