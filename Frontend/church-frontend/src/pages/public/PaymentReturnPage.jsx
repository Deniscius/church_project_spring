import React, { useEffect, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import PageHeader from '../../components/ui/PageHeader';
import AppLoading from '../../components/ui/AppLoading';
import { paymentService } from '../../services/payment.service';
import { setPaymentCode } from '../../utils/sensitiveNav';

/**
 * Retour FedaPay : /paiement/retour?r=<jeton>&id=<txId>&status=<hint>
 * Le query `status` n'est qu'un hint UI ; la vérité passe par réconciliation API / webhook.
 */
export default function PaymentReturnPage() {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const [error, setError] = useState('');

  useEffect(() => {
    let cancelled = false;
    (async () => {
      const token = (params.get('r') || '').trim();
      const providerTransactionId = (params.get('id') || params.get('transaction_id') || '').trim();
      const statusHint = (params.get('status') || '').trim();
      // Compat : ancien retour /paiement/retour/:code
      const pathParts = window.location.pathname.split('/').filter(Boolean);
      const legacyCode = pathParts[2] && pathParts[2] !== 'retour' ? pathParts[2] : '';

      try {
        let code = '';
        let statutPaiement = '';
        if (token) {
          const res = await paymentService.resolveReturn({
            token,
            providerTransactionId: providerTransactionId || undefined,
          });
          code = res?.codeSuivie || '';
          statutPaiement = res?.statutPaiement || '';
        } else if (legacyCode) {
          code = decodeURIComponent(legacyCode);
          if (providerTransactionId || code) {
            const res = await paymentService.reconcileByTrackingCode(code);
            statutPaiement = res?.statutPaiement || '';
          }
        }
        if (!code) {
          throw new Error('Retour de paiement invalide ou expiré.');
        }
        if (cancelled) return;
        setPaymentCode(code);
        const qs = new URLSearchParams();
        if (statutPaiement) qs.set('statut', statutPaiement);
        else if (statusHint) qs.set('status', statusHint);
        navigate(qs.toString() ? `/paiement?${qs}` : '/paiement', { replace: true });
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
        <p className="text-red-600" role="alert">{error}</p>
        <Link to="/suivi" className="btn btn-primary" style={{ textDecoration: 'none', width: 'fit-content' }}>
          Aller au suivi
        </Link>
      </div>
    );
  }

  return (
    <div className="stack public-page">
      <PageHeader title="Retour paiement" subtitle="Finalisation sécurisée…" />
      <AppLoading message="Vérification du paiement auprès de FedaPay…" />
    </div>
  );
}
