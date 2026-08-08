import React, { useEffect, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import PageHeader from '../../components/ui/PageHeader';
import AppLoading from '../../components/ui/AppLoading';
import { apiClient } from '../../services/http/apiClient';
import { setPaymentCode } from '../../utils/sensitiveNav';

/**
 * Retour FedaPay : /paiement/retour?r=<jeton opaque>
 * Résout le jeton, stocke le code en session, redirige vers /paiement (URL propre).
 */
export default function PaymentReturnPage() {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const [error, setError] = useState('');

  useEffect(() => {
    let cancelled = false;
    (async () => {
      const token = (params.get('r') || '').trim();
      // Compat : ancien retour /paiement/retour/:code
      const pathParts = window.location.pathname.split('/').filter(Boolean);
      const legacyCode = pathParts[2] && pathParts[2] !== 'retour' ? pathParts[2] : '';

      try {
        let code = '';
        if (token) {
          const res = await apiClient('/paiements/retour/resoudre', {
            method: 'POST',
            body: JSON.stringify({ token }),
          });
          code = res?.codeSuivie || '';
        } else if (legacyCode) {
          code = decodeURIComponent(legacyCode);
        }
        if (!code) {
          throw new Error('Retour de paiement invalide ou expiré.');
        }
        if (cancelled) return;
        setPaymentCode(code);
        const status = params.get('status');
        navigate(status ? `/paiement?status=${encodeURIComponent(status)}` : '/paiement', {
          replace: true,
        });
      } catch (e) {
        if (!cancelled) {
          setError(e instanceof Error ? e.message : 'Retour de paiement impossible');
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [navigate, params]);

  if (error) {
    return (
      <div className="stack public-page">
        <PageHeader title="Retour paiement" subtitle="Impossible de reprendre la session." />
        <p className="text-red-600">{error}</p>
        <Link to="/suivi" className="btn btn-primary" style={{ textDecoration: 'none', width: 'fit-content' }}>
          Aller au suivi
        </Link>
      </div>
    );
  }

  return (
    <div className="stack public-page">
      <PageHeader title="Retour paiement" subtitle="Finalisation sécurisée…" />
      <AppLoading message="Reprise de votre paiement…" />
    </div>
  );
}
