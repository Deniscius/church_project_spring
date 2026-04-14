import React from 'react';
import { useAuth } from '../../hooks/useAuth';
import { usePermissions } from '../../hooks/usePermissions';
import { RoleBadge, PermissionsList } from './RoleComponents';

/**
 * Composant pour afficher le profil utilisateur courant et ses permissions.
 */
export function UserProfileCard() {
  const { user } = useAuth();
  const { allPermissions } = usePermissions();

  if (!user) {
    return null;
  }

  return (
    <div className="bg-white rounded-lg shadow-md p-6 max-w-md">
      {/* Header */}
      <div className="flex items-center gap-4 mb-6">
        <div className="w-12 h-12 bg-blue-500 rounded-full flex items-center justify-center text-white font-semibold text-lg">
          {user.firstName?.charAt(0).toUpperCase()}{user.lastName?.charAt(0).toUpperCase()}
        </div>
        <div>
          <h3 className="font-semibold text-lg text-gray-900">
            {user.firstName} {user.lastName}
          </h3>
          <p className="text-sm text-gray-500">@{user.username}</p>
        </div>
      </div>

      {/* Role */}
      <div className="mb-6">
        <h4 className="text-sm font-semibold text-gray-700 mb-2">Rôle</h4>
        <RoleBadge role={user.role} />
      </div>

      {/* ID */}
      <div className="mb-6">
        <h4 className="text-sm font-semibold text-gray-700 mb-2">ID Utilisateur</h4>
        <p className="text-xs text-gray-600 font-mono bg-gray-50 p-2 rounded break-all">
          {user.id}
        </p>
      </div>

      {/* Permissions */}
      <div>
        <h4 className="text-sm font-semibold text-gray-700 mb-2">Permissions</h4>
        <div className="flex flex-wrap gap-2">
          {allPermissions.map((perm) => (
            <span
              key={perm}
              className="px-2 py-1 bg-green-100 text-green-800 text-xs rounded-md"
            >
              ✓ {perm}
            </span>
          ))}
        </div>
      </div>
    </div>
  );
}

/**
 * Composant pour afficher les permissions de l'utilisateur courant.
 */
export function UserPermissionsInfo() {
  const { can, allPermissions } = usePermissions();

  return (
    <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
      <h4 className="font-semibold text-blue-900 mb-3">Vos permissions</h4>

      <div className="space-y-2 text-sm">
        <div className="flex items-center gap-2">
          <span className={`w-5 h-5 rounded border flex items-center justify-center ${
            can.read ? 'bg-green-100 border-green-500' : 'bg-gray-100 border-gray-300'
          }`}>
            {can.read && <span className="text-green-600">✓</span>}
          </span>
          <span>Lecture</span>
        </div>

        <div className="flex items-center gap-2">
          <span className={`w-5 h-5 rounded border flex items-center justify-center ${
            can.create ? 'bg-green-100 border-green-500' : 'bg-gray-100 border-gray-300'
          }`}>
            {can.create && <span className="text-green-600">✓</span>}
          </span>
          <span>Créer</span>
        </div>

        <div className="flex items-center gap-2">
          <span className={`w-5 h-5 rounded border flex items-center justify-center ${
            can.edit ? 'bg-green-100 border-green-500' : 'bg-gray-100 border-gray-300'
          }`}>
            {can.edit && <span className="text-green-600">✓</span>}
          </span>
          <span>Éditer</span>
        </div>

        <div className="flex items-center gap-2">
          <span className={`w-5 h-5 rounded border flex items-center justify-center ${
            can.delete ? 'bg-green-100 border-green-500' : 'bg-gray-100 border-gray-300'
          }`}>
            {can.delete && <span className="text-green-600">✓</span>}
          </span>
          <span>Supprimer</span>
        </div>

        <div className="flex items-center gap-2">
          <span className={`w-5 h-5 rounded border flex items-center justify-center ${
            can.validate ? 'bg-green-100 border-green-500' : 'bg-gray-100 border-gray-300'
          }`}>
            {can.validate && <span className="text-green-600">✓</span>}
          </span>
          <span>Valider</span>
        </div>
      </div>
    </div>
  );
}
