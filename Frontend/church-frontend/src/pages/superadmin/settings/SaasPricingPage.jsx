import React, { useEffect, useState } from 'react';
import PageHeader from '../../../components/ui/PageHeader';
import AppCard from '../../../components/ui/AppCard';
import AppInput from '../../../components/ui/AppInput';
import AppButton from '../../../components/ui/AppButton';
import { planSaasService } from '../../../services/planSaas.service';
import { formatCurrency } from '../../../utils/formatCurrency';

const toForm = (plan) => ({
  nom: plan.nom || '',
  description: plan.description || '',
  montantXof: String(plan.montantXof ?? ''),
  dureeMois: String(plan.dureeMois ?? ''),
  actif: Boolean(plan.actif),
  featured: Boolean(plan.featured),
  ordreAffichage: String(plan.ordreAffichage ?? 0),
});

export default function SaasPricingPage() {
  const [plans, setPlans] = useState([]);
  const [forms, setForms] = useState({});
  const [loading, setLoading] = useState(true);
  const [savingId, setSavingId] = useState(null);
  const [error, setError] = useState(null);
  const [info, setInfo] = useState(null);

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await planSaasService.listAll();
      const list = Array.isArray(data) ? data : [];
      setPlans(list);
      setForms(Object.fromEntries(list.map((plan) => [plan.publicId, toForm(plan)])));
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Chargement impossible');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const setField = (id, field, value) => {
    setForms((current) => ({
      ...current,
      [id]: { ...current[id], [field]: value },
    }));
  };

  const save = async (plan) => {
    const form = forms[plan.publicId];
    if (!form) return;

    const montantXof = Number(form.montantXof);
    const dureeMois = Number(form.dureeMois);
    const ordreAffichage = Number(form.ordreAffichage);
    if (!form.nom.trim() || !Number.isInteger(montantXof) || montantXof <= 0
      || !Number.isInteger(dureeMois) || dureeMois <= 0
      || !Number.isInteger(ordreAffichage) || ordreAffichage < 0) {
      setError('Vérifiez le nom, le montant, la durée et l’ordre d’affichage.');
      return;
    }

    try {
      setSavingId(plan.publicId);
      setError(null);
      setInfo(null);
      const saved = await planSaasService.update(plan.publicId, {
        nom: form.nom.trim(),
        description: form.description.trim() || null,
        montantXof,
        dureeMois,
        actif: Boolean(form.actif),
        featured: Boolean(form.featured),
        ordreAffichage,
      });
      setPlans((current) => current.map((item) => item.publicId === saved.publicId ? saved : item));
      setForms((current) => ({ ...current, [saved.publicId]: toForm(saved) }));
      setInfo(`Plan « ${saved.nom} » mis à jour.`);
      // featured est exclusif côté backend : recharge pour refléter les autres plans.
      if (saved.featured) await load();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Enregistrement impossible');
    } finally {
      setSavingId(null);
    }
  };

  return (
    <div className="stack">
      <PageHeader
        title="Tarification SaaS"
        subtitle="Catalogue global des abonnements paroisses. Les changements s’appliquent aux nouveaux abonnements, sans modifier l’historique déjà facturé."
        actions={<AppButton variant="secondary" onClick={load} disabled={loading}>Actualiser</AppButton>}
      />

      {error ? <div className="alert-danger" role="alert">{error}</div> : null}
      {info ? <div className="alert-success" role="status">{info}</div> : null}
      {loading ? <p className="muted">Chargement…</p> : null}

      {!loading && plans.map((plan) => {
        const form = forms[plan.publicId] || toForm(plan);
        return (
          <AppCard
            key={plan.publicId}
            title={`${plan.nom} · ${plan.code}`}
            subtitle={`${formatCurrency(plan.montantXof)} · ${plan.dureeMois} mois${plan.featured ? ' · Recommandé' : ''}`}
          >
            <div className="form-grid">
              <div className="form-field">
                <label htmlFor={`name-${plan.publicId}`}>Nom commercial</label>
                <AppInput id={`name-${plan.publicId}`} maxLength={100} value={form.nom}
                  onChange={(e) => setField(plan.publicId, 'nom', e.target.value)} />
              </div>
              <div className="form-field">
                <label htmlFor={`amount-${plan.publicId}`}>Prix (FCFA)</label>
                <AppInput id={`amount-${plan.publicId}`} type="number" min="1" step="1" value={form.montantXof}
                  onChange={(e) => setField(plan.publicId, 'montantXof', e.target.value)} />
              </div>
              <div className="form-field">
                <label htmlFor={`duration-${plan.publicId}`}>Durée (mois)</label>
                <AppInput id={`duration-${plan.publicId}`} type="number" min="1" step="1" value={form.dureeMois}
                  onChange={(e) => setField(plan.publicId, 'dureeMois', e.target.value)} />
              </div>
              <div className="form-field">
                <label htmlFor={`order-${plan.publicId}`}>Ordre d’affichage</label>
                <AppInput id={`order-${plan.publicId}`} type="number" min="0" step="1" value={form.ordreAffichage}
                  onChange={(e) => setField(plan.publicId, 'ordreAffichage', e.target.value)} />
              </div>
              <div className="form-field full">
                <label htmlFor={`description-${plan.publicId}`}>Description</label>
                <textarea id={`description-${plan.publicId}`} className="input" maxLength={300} rows={3}
                  value={form.description}
                  onChange={(e) => setField(plan.publicId, 'description', e.target.value)} />
              </div>
            </div>

            <div className="button-row" style={{ marginTop: 16, alignItems: 'center' }}>
              <label style={{ display: 'inline-flex', alignItems: 'center', gap: 8 }}>
                <input type="checkbox" checked={form.actif}
                  onChange={(e) => setField(plan.publicId, 'actif', e.target.checked)} />
                Plan actif
              </label>
              <label style={{ display: 'inline-flex', alignItems: 'center', gap: 8 }}>
                <input type="checkbox" checked={form.featured} disabled={!form.actif}
                  onChange={(e) => setField(plan.publicId, 'featured', e.target.checked)} />
                Plan recommandé
              </label>
              <AppButton onClick={() => save(plan)} disabled={savingId === plan.publicId}>
                {savingId === plan.publicId ? 'Enregistrement…' : 'Enregistrer'}
              </AppButton>
            </div>
          </AppCard>
        );
      })}
    </div>
  );
}
