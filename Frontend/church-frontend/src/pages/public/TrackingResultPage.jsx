import React, { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import PageHeader from '../../components/ui/PageHeader';
import AppCard from '../../components/ui/AppCard';
import AppBadge from '../../components/ui/AppBadge';
import { requestService } from '../../services/request.service';

export default function TrackingResultPage() {
  const [params] = useSearchParams();
  const code = params.get('code')?.trim() || '';
  const [demande, setDemande] = useState(null);
  const [loading, setLoading] = useState(Boolean(code));
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!code) {
      setDemande(null);
      setLoading(false);
      setError(null);
      return;
    }
    let cancelled = false;
    (async () => {
      try {
        setLoading(true);
        setError(null);
        const data = await requestService.getByTrackingCode(code);
        if (!cancelled) setDemande(data);
      } catch (e) {
        if (!cancelled) setError(e instanceof Error ? e.message : 'Introuvable');
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [code]);

  return (
    <div className="stack">
      <PageHeader title="Résultat du suivi" subtitle="Statuts issus du backend pour ce code." />
      {!code ? (
        <p className="muted">Ajoutez un code dans l’URL (?code=…) ou depuis la page Suivi.</p>
      ) : null}
      {loading ? <p className="muted">Chargement…</p> : null}
      {error ? <p className="text-red-600">{error}</p> : null}
      {demande ? (
        <div className="grid-2">
          <AppCard title="Statuts de la demande">
            <div className="info-list">
              <div className="info-row">
                <span>Code</span>
                <strong>{demande.codeSuivie}</strong>
              </div>
              <div className="info-row">
                <span>Statut demande</span>
                <AppBadge value={demande.statutDemande} />
              </div>
              <div className="info-row">
                <span>Statut validation</span>
                <AppBadge value={demande.statutValidation} />
              </div>
              <div className="info-row">
                <span>Statut paiement</span>
                <AppBadge value={demande.statutPaiement} />
              </div>
            </div>
          </AppCard>
          <AppCard title="Liens utiles">
            <ul style={{ margin: 0, paddingLeft: 18 }}>
              <li>
                <Link to={`/facture/${encodeURIComponent(demande.codeSuivie)}`}>Voir la facture</Link>
              </li>
              <li>
                <Link to={`/paiement/${encodeURIComponent(demande.codeSuivie)}`}>État du paiement</Link>
              </li>
            </ul>
          </AppCard>
        </div>
      ) : null}
    </div>
  );
}
