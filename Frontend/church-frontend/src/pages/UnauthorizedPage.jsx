import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';

/**
 * Page d'accès refusé (403 Unauthorized).
 */
export function UnauthorizedPage() {
  const navigate = useNavigate();
  const { user } = useAuth();

  const handleGoBack = () => {
    navigate(-1);
  };

  const handleGoHome = () => {
    if (user?.role === 'ADMIN' || user?.role === 'SUPER_ADMIN') {
      navigate('/admin/dashboard');
    } else if (user?.role === 'SECRETAIRE' || user?.role === 'CURE') {
      navigate('/admin/demandes');
    } else {
      navigate('/public/demandes');
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-red-50 to-orange-50 flex items-center justify-center px-4">
      <div className="max-w-md w-full bg-white rounded-lg shadow-lg p-8">
        {/* Icon */}
        <div className="text-center mb-6">
          <div className="inline-flex items-center justify-center w-16 h-16 bg-red-100 rounded-full mb-4">
            <svg
              className="w-8 h-8 text-red-600"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M12 8v4m0 4v.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
              />
            </svg>
          </div>
        </div>

        {/* Content */}
        <h1 className="text-2xl font-bold text-center text-gray-900 mb-2">
          Accès refusé
        </h1>

        <p className="text-center text-gray-600 mb-2">
          Vous n'avez pas les permissions nécessaires pour accéder à cette page.
        </p>

        {user && (
          <p className="text-center text-sm text-gray-500 mb-6">
            Votre rôle actuel: <span className="font-semibold">{user.role}</span>
          </p>
        )}

        {/* Error Description */}
        <div className="bg-red-50 border border-red-200 rounded-lg p-4 mb-6">
          <p className="text-sm text-red-800">
            <strong>Code d'erreur:</strong> 403 Forbidden
          </p>
          <p className="text-sm text-red-700 mt-2">
            Veuillez vous assurer que vous disposez des droits d'accès appropriés ou contactez un administrateur.
          </p>
        </div>

        {/* Actions */}
        <div className="flex gap-3">
          <button
            onClick={handleGoBack}
            className="flex-1 px-4 py-2 bg-gray-200 text-gray-800 rounded-lg font-semibold hover:bg-gray-300 transition"
          >
            Retour
          </button>
          <button
            onClick={handleGoHome}
            className="flex-1 px-4 py-2 bg-blue-600 text-white rounded-lg font-semibold hover:bg-blue-700 transition"
          >
            Accueil
          </button>
        </div>

        {/* Help Link */}
        <p className="text-center text-sm text-gray-500 mt-6">
          Besoin d'aide?{' '}
          <a href="/contact" className="text-blue-600 hover:underline">
            Contactez le support
          </a>
        </p>
      </div>
    </div>
  );
}

export default UnauthorizedPage;
