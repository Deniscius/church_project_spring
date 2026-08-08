import React, { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import PageHeader from '../../../components/ui/PageHeader';
import AppCard from '../../../components/ui/AppCard';
import { comptabiliteService } from '../../../services/inscription.service';
import { useTenant } from '../../../hooks/useTenant';
import { mapParoisseToTenant } from '../../../utils/apiMappers';

/**
 * Point d'entrée pour le catalogue plateforme par défaut.
 * Épingle le support technique système comme contexte de travail,
 * puis renvoie vers les écrans types / forfaits / horaires.
 */
export default function CatalogueModelePage() {
  const navigate = useNavigate();
  const { setActiveParish, activeParish } = useTenant();
  const [template, setTemplate] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [ready, setReady] = useState(false);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        setLoading(true);
        setError(null);
        const data = await comptabiliteService.getCatalogueModele();
        if (cancelled) return;
        setTemplate(data);
        const tenant = mapParoisseToTenant({
          ...data,
          nom: 'Catalogue plateforme',
        });
        setActiveParish({
          ...tenant,
          name: 'Catalogue plateforme',
          raw: { ...data, nom: 'Catalogue plateforme', isActive: true, active: true, isSystem: true },
        });
        setReady(true);
      } catch (e) {
        if (!cancelled) setError(e instanceof Error ? e.message : 'Catalogue plateforme introuvable');
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => { cancelled = true; };
    // Chargement unique au montage.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const openSection = (path) => {
    if (!ready) return;
    navigate(path);
  };

  return (
    <div className="stack">
      <PageHeader
        title="Catalogue plateforme"
        subtitle="Grille par défaut du système, clonée à chaque activation de paroisse. Chaque paroisse peut ensuite l’enrichir localement."
        backTo="/admin/abonnements"
        backLabel="Retour"
      />

      {loading ? <p className="muted">Chargement du catalogue…</p> : null}
      {error ? <div className="alert-danger" role="alert">{error}</div> : null}

      {template ? (
        <>
          <AppCard title="Configuration système">
            <div className="info-list">
              <div className="info-row">
                <span>Périmètre</span>
                <strong>Toute la plateforme</strong>
              </div>
              <div className="info-row">
                <span>Contexte de travail</span>
                <span>{activeParish?.name || 'Catalogue plateforme'}</span>
              </div>
            </div>
            <p className="muted" style={{ marginBottom: 0, marginTop: 12 }}>
              Les modifications s’appliquent aux prochaines activations.
              Les paroisses déjà configurées conservent leur propre grille et peuvent y ajouter des éléments.
            </p>
          </AppCard>

          <div className="card-grid">
            <AppCard title="Types de demande">
              <p className="muted">Intention, triduum, neuvaine, trentaine…</p>
              <button type="button" className="btn btn-primary" disabled={!ready} onClick={() => openSection('/admin/types-demandes')}>
                Modifier les types
              </button>
            </AppCard>
            <AppCard title="Forfaits / tarifs">
              <p className="muted">Montants normale, dominicale, spéciale…</p>
              <button type="button" className="btn btn-primary" disabled={!ready} onClick={() => openSection('/admin/forfaits')}>
                Modifier les forfaits
              </button>
            </AppCard>
            <AppCard title="Horaires">
              <p className="muted">Messes du matin, dimanche, soir…</p>
              <button type="button" className="btn btn-primary" disabled={!ready} onClick={() => openSection('/admin/horaires')}>
                Modifier les horaires
              </button>
            </AppCard>
          </div>

          <p className="muted">
            Retour :{' '}
            <Link to="/admin/abonnements">Abonnements</Link>
          </p>
        </>
      ) : null}
    </div>
  );
}
