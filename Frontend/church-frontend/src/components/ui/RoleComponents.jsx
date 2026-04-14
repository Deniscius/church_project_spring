import React from 'react';
import { ROLES, ROLE_LABELS, ROLE_DESCRIPTIONS } from '../../constants/roles';

/**
 * Composant pour sélectionner un rôle dans l'interface admin.
 * Affiche les rôles avec leurs descriptions.
 */
export function RoleSelector({ selectedRole, onChange, disabled = false }) {
  const roleEntries = Object.entries(ROLES).map(([key, value]) => ({
    key,
    value,
    label: ROLE_LABELS[value],
    description: ROLE_DESCRIPTIONS[value],
  }));

  return (
    <div className="role-selector">
      <label htmlFor="role-select" className="block mb-2 font-semibold">
        Rôle
      </label>

      <select
        id="role-select"
        value={selectedRole}
        onChange={(e) => onChange(e.target.value)}
        disabled={disabled}
        className="w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
      >
        <option value="">-- Sélectionnez un rôle --</option>
        {roleEntries.map(({ value, label }) => (
          <option key={value} value={value}>
            {label}
          </option>
        ))}
      </select>

      {selectedRole && (
        <div className="mt-3 p-3 bg-blue-50 border border-blue-200 rounded-md">
          <p className="text-sm font-semibold text-blue-900">
            {ROLE_LABELS[selectedRole]}
          </p>
          <p className="text-xs text-blue-700 mt-1">
            {ROLE_DESCRIPTIONS[selectedRole]}
          </p>
        </div>
      )}
    </div>
  );
}

/**
 * Composant pour afficher un badge de rôle.
 */
export function RoleBadge({ role, className = '' }) {
  const colorMap = {
    SUPER_ADMIN: 'bg-red-100 text-red-800',
    ADMIN: 'bg-orange-100 text-orange-800',
    SECRETAIRE: 'bg-blue-100 text-blue-800',
    CURE: 'bg-green-100 text-green-800',
    USER: 'bg-gray-100 text-gray-800',
  };

  return (
    <span className={`px-3 py-1 rounded-full text-sm font-semibold ${colorMap[role] || ''} ${className}`}>
      {ROLE_LABELS[role] || role}
    </span>
  );
}

/**
 * Composant pour afficher la liste des permissions d'un rôle.
 */
export function PermissionsList({ role, className = '' }) {
  const { ROLE_PERMISSIONS } = require('../../constants/roles');
  const permissions = ROLE_PERMISSIONS[role] || [];

  const permissionLabels = {
    read: 'Lire',
    create: 'Créer',
    edit: 'Éditer',
    delete: 'Supprimer',
    validate: 'Valider',
    admin: 'Administrer',
    manage_users: 'Gérer les utilisateurs',
    manage_system: 'Gérer le système',
  };

  return (
    <div className={`permissions-list ${className}`}>
      <h4 className="font-semibold mb-2">Permissions</h4>
      <div className="flex flex-wrap gap-2">
        {permissions.map((perm) => (
          <span
            key={perm}
            className="px-2 py-1 bg-green-100 text-green-800 text-xs rounded-md"
          >
            ✓ {permissionLabels[perm] || perm}
          </span>
        ))}
      </div>
    </div>
  );
}
