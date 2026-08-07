import { getApiBaseUrl } from '../config/apiBaseUrl';
import { getAccessToken } from '../constants/authStorage';
import { apiClient } from './http/apiClient';
import { attachToken } from './http/interceptors';

const API_BASE_URL = getApiBaseUrl();

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
    const headers = attachToken(
      {
        Accept: 'application/pdf',
        'ngrok-skip-browser-warning': '1',
      },
      getAccessToken()
    );
    const response = await fetch(
      `${API_BASE_URL}/celebrations/paroisse/${paroissePublicId}/feuille.pdf?${query}`,
      { headers }
    );
    if (!response.ok) {
      let message = `Erreur HTTP ${response.status}`;
      try {
        const err = await response.json();
        if (err?.message) message = err.message;
      } catch {
        /* ignore */
      }
      throw new Error(message);
    }
    return response.blob();
  },
};
