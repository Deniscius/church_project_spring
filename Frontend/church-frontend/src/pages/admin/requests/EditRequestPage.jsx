import React from 'react';
import { Navigate, useParams } from 'react-router-dom';

/**
 * L'édition complète de demande n'est pas encore exposée.
 * On redirige vers le détail (validation / consultation).
 */
export default function EditRequestPage() {
  const { id } = useParams();
  return <Navigate to={id ? `/admin/demandes/${id}` : '/admin/demandes'} replace />;
}
