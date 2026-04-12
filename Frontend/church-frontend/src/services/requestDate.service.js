import { apiClient } from './http/apiClient';

export const requestDateService = {
  getByDemande: (demandePublicId) =>
    apiClient(`/demande-dates/demande/${demandePublicId}`, {}, { auth: false }),
};
