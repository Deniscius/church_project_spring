import { apiClient } from './http/apiClient';

const API_BASE = '/admin/users';

/**
 * Service CRUD pour la gestion des utilisateurs (Admin)
 * Endpoints conventionnels RESTful
 */
export const userService = {
  /**
   * Crée un nouvel utilisateur
   * POST /admin/users
   */
  create: (userData) =>
    apiClient(API_BASE, 
      { method: 'POST', body: JSON.stringify(userData) }, 
      { auth: true }
    ),

  /**
   * Récupère tous les utilisateurs actifs
   * GET /admin/users
   */
  getAll: () =>
    apiClient(API_BASE, {}, { auth: true }),

  /**
   * Récupère un utilisateur spécifique par son ID
   * GET /admin/users/{userId}
   */
  getById: (userId) =>
    apiClient(`${API_BASE}/${userId}`, {}, { auth: true }),

  /**
   * Met à jour un utilisateur
   * PUT /admin/users/{userId}
   */
  update: (userId, userData) =>
    apiClient(`${API_BASE}/${userId}`, 
      { method: 'PUT', body: JSON.stringify(userData) }, 
      { auth: true }
    ),

  /**
   * Supprime (soft delete) un utilisateur
   * DELETE /admin/users/{userId}
   */
  delete: (userId) =>
    apiClient(`${API_BASE}/${userId}`, 
      { method: 'DELETE' }, 
      { auth: true }
    ),

  /**
   * Assigne une paroisse à un utilisateur
   * POST /admin/users/{userId}/paroisses
   */
  assignParoisse: (userId, paroisseData) =>
    apiClient(`${API_BASE}/${userId}/paroisses`, 
      { method: 'POST', body: JSON.stringify(paroisseData) }, 
      { auth: true }
    ),

  /**
   * Révoque l'accès à une paroisse
   * DELETE /admin/users/{userId}/paroisses/{paroisseId}
   */
  revokeParoisseAccess: (userId, paroisseId) =>
    apiClient(`${API_BASE}/${userId}/paroisses/${paroisseId}`, 
      { method: 'DELETE' }, 
      { auth: true }
    ),

  /**
   * Récupère les paroisses d'un utilisateur
   * GET /admin/users/{userId}/paroisses
   */
  getUserParoisses: (userId) =>
    apiClient(`${API_BASE}/${userId}/paroisses`, {}, { auth: true }),

  /**
   * Récupère les utilisateurs d'une paroisse
   * GET /admin/users/paroisse/{paroisseId}
   */
  getUsersByParoisse: (paroisseId) =>
    apiClient(`${API_BASE}/paroisse/${paroisseId}`, {}, { auth: true }),
};
