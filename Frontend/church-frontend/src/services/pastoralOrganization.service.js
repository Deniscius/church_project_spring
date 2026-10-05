import { apiClient } from './http/apiClient';

export const pastoralOrganizationService = {
  getStructures: (paroissePublicId) =>
    apiClient(`/structures-pastorales/paroisse/${paroissePublicId}`, {}, { auth: true }),

  getStructure: (publicId) =>
    apiClient(`/structures-pastorales/${publicId}`, {}, { auth: true }),

  createStructure: (payload) =>
    apiClient(
      '/structures-pastorales',
      { method: 'POST', body: JSON.stringify(payload) },
      { auth: true }
    ),

  updateStructure: (publicId, payload) =>
    apiClient(
      `/structures-pastorales/${publicId}`,
      { method: 'PUT', body: JSON.stringify(payload) },
      { auth: true }
    ),

  setStructureStatus: (publicId, actif, version) =>
    apiClient(
      `/structures-pastorales/${publicId}/statut`,
      { method: 'POST', body: JSON.stringify({ actif, version }) },
      { auth: true }
    ),

  removeStructure: (publicId, version) =>
    apiClient(
      `/structures-pastorales/${publicId}?version=${encodeURIComponent(version)}`,
      { method: 'DELETE' },
      { auth: true }
    ),

  getMandates: (paroissePublicId, anneePastoralePublicId) =>
    apiClient(
      `/mandats-pastoraux/paroisse/${paroissePublicId}/annee/${anneePastoralePublicId}`,
      {},
      { auth: true }
    ),

  getMandate: (publicId) =>
    apiClient(`/mandats-pastoraux/${publicId}`, {}, { auth: true }),

  createMandate: (payload) =>
    apiClient(
      '/mandats-pastoraux',
      { method: 'POST', body: JSON.stringify(payload) },
      { auth: true }
    ),

  updateMandate: (publicId, payload) =>
    apiClient(
      `/mandats-pastoraux/${publicId}`,
      { method: 'PUT', body: JSON.stringify(payload) },
      { auth: true }
    ),

  removeMandate: (publicId, version) =>
    apiClient(
      `/mandats-pastoraux/${publicId}?version=${encodeURIComponent(version)}`,
      { method: 'DELETE' },
      { auth: true }
    ),
};
