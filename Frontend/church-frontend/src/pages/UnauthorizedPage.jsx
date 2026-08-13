import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

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
    if (user?.role === 'SUPER_ADMIN' && user?.isGlobal === true) {
      navigate('/admin/paroisses');
    } else if (user?.role === 'COMPTABLE' && user?.isGlobal === true) {
      navigate('/admin/demandes-plateforme');
    } else if (user?.role === 'ADMIN') {
      navigate('/admin/dashboard');
    } else if (user?.role === 'SECRETAIRE' || user?.role === 'CURE' || user?.role === 'COMPTABLE_LOCAL') {
      navigate('/admin/demandes');
    } else {
      navigate('/');
    }
  };

  return (
    <main className="page-section">
      <div className="container" style={{ maxWidth: 620, paddingTop: 64 }}>
        <div className="card stack">
          <span className="badge danger" style={{ alignSelf: 'flex-start' }}>403 · Accès refusé</span>
          <h1 className="page-title">Vous n’avez pas accès à cette page</h1>
          <p className="page-subtitle">
            Cette action n’est pas autorisée pour votre rôle{user?.role ? ` (${user.role})` : ''}.
            Si vous pensez qu’il s’agit d’une erreur, contactez un administrateur.
          </p>
          <div className="button-row">
            <button onClick={handleGoBack} className="btn btn-secondary">Retour</button>
            <button onClick={handleGoHome} className="btn btn-primary">Aller à mon accueil</button>
          </div>
        </div>
      </div>
    </main>
  );
}

export default UnauthorizedPage;
