import React, { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

/**
 * Ancien récapitulatif (4e écran). Le dépôt se fait à l’étape 3 de /demande.
 * Conservé pour les liens / favoris existants.
 */
export default function RequestRecapPage() {
  const navigate = useNavigate();
  useEffect(() => {
    navigate('/demande', { replace: true });
  }, [navigate]);
  return null;
}
