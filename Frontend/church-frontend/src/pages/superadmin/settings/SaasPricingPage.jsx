import React, { useCallback, useEffect, useMemo, useState } from 'react';
import PageHeader from '../../../components/ui/PageHeader';
import AppAlert from '../../../components/ui/AppAlert';
import AppButton from '../../../components/ui/AppButton';
import AppInput from '../../../components/ui/AppInput';
import { planSaasService } from '../../../services/planSaas.service';
import { formatCurrency } from '../../../utils/formatCurrency';
import './SaasPricingPage.css';

const FORM_FIELDS = [
  'nom',
  'description',
  'montantXof',
  'dureeMois',
  'actif',
  'featured',
  'ordreAffichage',
];

const toForm = (plan) => ({
  nom: plan.nom || '',
  description: plan.description || '',
  montantXof: String(plan.montantXof ?? ''),
  dureeMois: String(plan.dureeMois ?? ''),
  actif: Boolean(plan.actif),
  featured: Boolean(plan.featured),
  ordreAffichage: String(plan.ordreAffichage ?? 0),
});

function hasUnsavedChanges(plan, form) {
  if (!plan || !form) return false;
  const initial = toForm(plan);
  return FORM_FIELDS.some((field) => initial[field] !== form[field]);
}

function monthlyEquivalent(plan) {
  const amount = Number(plan?.montantXof);
  const months = Number(plan?.dureeMois);
  if (!Number.isFinite(amount) || !Number.isFinite(months) || months <= 0) return null;
  return Math.round(amount / months);
}

function durationLabel(months) {
  const value = Number(months);
  if (value === 1) return '1 mois';
  return `${value || 0} mois`;
}

function PricingPlanCard({ plan, onEdit }) {
  const monthly = monthlyEquivalent(plan);

  return (
    <article
      className={`saas-plan-card${plan.featured ? ' is-featured' : ''}${!plan.actif ? ' is-inactive' : ''}`}
    >
      <div className="saas-plan-card-topline">
        <span className="saas-plan-code">{plan.code}</span>
        <span className={`saas-plan-status${plan.actif ? ' is-active' : ''}`}>
          <span className="saas-plan-status-dot" aria-hidden="true" />
          {plan.actif ? 'Actif' : 'Inactif'}
        </span>
      </div>

      {plan.featured ? <span className="saas-plan-featured">Recommandé</span> : null}

      <div className="saas-plan-heading">
        <h2>{plan.nom}</h2>
        <p>{plan.description || 'Aucune description commerciale renseignée.'}</p>
      </div>

      <div className="saas-plan-price-block">
        <strong>{formatCurrency(plan.montantXof)}</strong>
        <span>pour {durationLabel(plan.dureeMois)}</span>
      </div>

      <div className="saas-plan-meta">
        <div>
          <span>Coût moyen / mois</span>
          <strong>{monthly != null ? formatCurrency(monthly) : '—'}</strong>
        </div>
        <div>
          <span>Position</span>
          <strong>#{Number(plan.ordreAffichage ?? 0) + 1}</strong>
        </div>
      </div>

      <div className="saas-plan-card-footer">
        <span className="saas-plan-contract-note">Nouveaux abonnements uniquement</span>
        <AppButton variant="secondary" size="sm" onClick={() => onEdit(plan)}>
          Modifier le plan
        </AppButton>
      </div>
    </article>
  );
}

export default function SaasPricingPage() {
  const [plans, setPlans] = useState([]);
  const [forms, setForms] = useState({});
  const [loading, setLoading] = useState(true);
  const [savingId, setSavingId] = useState(null);
  const [editingId, setEditingId] = useState(null);
  const [error, setError] = useState(null);
  const [info, setInfo] = useState(null);

  const load = useCallback(async ({ silent = false } = {}) => {
    try {
      if (!silent) setLoading(true);
      setError(null);
      const data = await planSaasService.listAll();
      const list = Array.isArray(data) ? data : [];
      setPlans(list);
      setForms(Object.fromEntries(list.map((plan) => [plan.publicId, toForm(plan)])));
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Chargement impossible');
    } finally {
      if (!silent) setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const activePlans = useMemo(() => plans.filter((plan) => plan.actif), [plans]);
  const featuredPlan = useMemo(() => plans.find((plan) => plan.featured) || null, [plans]);
  const selectedPlan = useMemo(
    () => plans.find((plan) => plan.publicId === editingId) || null,
    [editingId, plans]
  );
  const selectedForm = selectedPlan ? forms[selectedPlan.publicId] || toForm(selectedPlan) : null;
  const selectedDirty = hasUnsavedChanges(selectedPlan, selectedForm);

  const setField = (id, field, value) => {
    setForms((current) => ({
      ...current,
      [id]: { ...current[id], [field]: value },
    }));
  };

  const setActive = (id, checked) => {
    setForms((current) => ({
      ...current,
      [id]: {
        ...current[id],
        actif: checked,
        featured: checked ? current[id]?.featured : false,
      },
    }));
  };

  const openEditor = (plan) => {
    setEditingId(plan.publicId);
    setError(null);
    setInfo(null);
    window.requestAnimationFrame(() => {
      document.getElementById('saas-pricing-editor')?.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    });
  };

  const cancelEdit = () => {
    if (selectedPlan) {
      setForms((current) => ({
        ...current,
        [selectedPlan.publicId]: toForm(selectedPlan),
      }));
    }
    setEditingId(null);
    setError(null);
  };

  const save = async (plan) => {
    const form = forms[plan.publicId];
    if (!form) return;

    const montantXof = Number(form.montantXof);
    const dureeMois = Number(form.dureeMois);
    const ordreAffichage = Number(form.ordreAffichage);

    if (!form.nom.trim()) {
      setError('Le nom commercial du plan est obligatoire.');
      return;
    }
    if (!Number.isInteger(montantXof) || montantXof <= 0) {
      setError('Le prix doit être un montant entier supérieur à 0 FCFA.');
      return;
    }
    if (!Number.isInteger(dureeMois) || dureeMois <= 0) {
      setError('La durée doit être exprimée en mois entiers et être supérieure à 0.');
      return;
    }
    if (!Number.isInteger(ordreAffichage) || ordreAffichage < 0) {
      setError('L’ordre d’affichage doit être un entier positif ou nul.');
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
        featured: Boolean(form.actif && form.featured),
        ordreAffichage,
      });

      setPlans((current) => current.map((item) => item.publicId === saved.publicId ? saved : item));
      setForms((current) => ({ ...current, [saved.publicId]: toForm(saved) }));

      if (saved.featured) {
        await load({ silent: true });
      }

      setEditingId(null);
      setInfo(`Le plan « ${saved.nom} » a été mis à jour. Les abonnements déjà facturés restent inchangés.`);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Enregistrement impossible');
    } finally {
      setSavingId(null);
    }
  };

  return (
    <div className="stack saas-pricing-page">
      <PageHeader
        title="Tarification SaaS"
        subtitle="Pilotez les formules proposées aux paroisses depuis une source de vérité unique."
        actions={(
          <AppButton variant="secondary" onClick={() => load()} disabled={loading || Boolean(savingId)}>
            {loading ? 'Actualisation…' : 'Actualiser'}
          </AppButton>
        )}
      />

      <section className="saas-pricing-hero" aria-label="Règle de tarification">
        <div>
          <span className="saas-pricing-kicker">Catalogue commercial</span>
          <h2>Des prix simples à administrer, sans casser l’historique</h2>
          <p>
            Une modification ici s’applique aux prochaines souscriptions et renouvellements.
            Les montants et durées déjà vendus restent figés dans leurs abonnements.
          </p>
        </div>
        <div className="saas-pricing-hero-chip">Source de vérité backend</div>
      </section>

      {error ? <AppAlert variant="danger">{error}</AppAlert> : null}
      {info ? <AppAlert variant="success">{info}</AppAlert> : null}

      <section className="saas-pricing-summary" aria-label="Résumé des plans SaaS">
        <div className="saas-pricing-summary-item">
          <span>Plans configurés</span>
          <strong>{plans.length}</strong>
        </div>
        <div className="saas-pricing-summary-item">
          <span>Plans actifs</span>
          <strong>{activePlans.length}</strong>
        </div>
        <div className="saas-pricing-summary-item is-featured">
          <span>Plan recommandé</span>
          <strong>{featuredPlan?.nom || 'Aucun'}</strong>
        </div>
      </section>

      {loading ? (
        <div className="saas-pricing-loading" role="status" aria-live="polite">
          <span className="spinner" aria-hidden="true" />
          Chargement du catalogue…
        </div>
      ) : null}

      {!loading && plans.length === 0 ? (
        <div className="card empty-state" role="status">
          <h3>Aucun plan SaaS configuré</h3>
          <p>Le catalogue tarifaire ne contient actuellement aucune formule.</p>
        </div>
      ) : null}

      {!loading && plans.length > 0 ? (
        <section className="saas-plan-grid" aria-label="Plans disponibles">
          {plans.map((plan) => (
            <PricingPlanCard key={plan.publicId} plan={plan} onEdit={openEditor} />
          ))}
        </section>
      ) : null}

      {selectedPlan && selectedForm ? (
        <form
          id="saas-pricing-editor"
          className="saas-pricing-editor"
          onSubmit={(event) => {
            event.preventDefault();
            save(selectedPlan);
          }}
        >
          <div className="saas-pricing-editor-head">
            <div>
              <span className="saas-pricing-kicker">Édition · {selectedPlan.code}</span>
              <h2>Modifier {selectedPlan.nom}</h2>
              <p>Le code métier reste fixe. Seules les propriétés commerciales sont modifiables.</p>
            </div>
            {selectedDirty ? <span className="saas-pricing-unsaved">Modifications non enregistrées</span> : null}
          </div>

          <div className="saas-pricing-editor-body">
            <div className="saas-pricing-editor-fields">
              <div className="form-grid">
                <div className="form-field">
                  <label htmlFor={`name-${selectedPlan.publicId}`}>Nom commercial *</label>
                  <AppInput
                    id={`name-${selectedPlan.publicId}`}
                    maxLength={100}
                    value={selectedForm.nom}
                    onChange={(event) => setField(selectedPlan.publicId, 'nom', event.target.value)}
                  />
                  <span className="muted text-sm">Nom affiché lors de l’inscription et du renouvellement.</span>
                </div>

                <div className="form-field">
                  <label htmlFor={`amount-${selectedPlan.publicId}`}>Prix total (FCFA) *</label>
                  <AppInput
                    id={`amount-${selectedPlan.publicId}`}
                    type="number"
                    min="1"
                    step="1"
                    inputMode="numeric"
                    value={selectedForm.montantXof}
                    onChange={(event) => setField(selectedPlan.publicId, 'montantXof', event.target.value)}
                  />
                  <span className="muted text-sm">Montant envoyé au checkout pour les nouveaux abonnements.</span>
                </div>

                <div className="form-field">
                  <label htmlFor={`duration-${selectedPlan.publicId}`}>Durée contractuelle (mois) *</label>
                  <AppInput
                    id={`duration-${selectedPlan.publicId}`}
                    type="number"
                    min="1"
                    step="1"
                    inputMode="numeric"
                    value={selectedForm.dureeMois}
                    onChange={(event) => setField(selectedPlan.publicId, 'dureeMois', event.target.value)}
                  />
                </div>

                <div className="form-field">
                  <label htmlFor={`order-${selectedPlan.publicId}`}>Ordre d’affichage *</label>
                  <AppInput
                    id={`order-${selectedPlan.publicId}`}
                    type="number"
                    min="0"
                    step="1"
                    inputMode="numeric"
                    value={selectedForm.ordreAffichage}
                    onChange={(event) => setField(selectedPlan.publicId, 'ordreAffichage', event.target.value)}
                  />
                  <span className="muted text-sm">0 s’affiche avant 1, puis 2, etc.</span>
                </div>

                <div className="form-field full">
                  <label htmlFor={`description-${selectedPlan.publicId}`}>Description commerciale</label>
                  <textarea
                    id={`description-${selectedPlan.publicId}`}
                    className="textarea"
                    maxLength={300}
                    rows={4}
                    value={selectedForm.description}
                    onChange={(event) => setField(selectedPlan.publicId, 'description', event.target.value)}
                    placeholder="Expliquez en une phrase à qui s’adresse cette formule."
                  />
                  <span className="saas-pricing-char-count">{selectedForm.description.length}/300</span>
                </div>
              </div>
            </div>

            <aside className="saas-pricing-options" aria-label="Options du plan">
              <h3>Disponibilité</h3>

              <label className="saas-pricing-switch-row">
                <span>
                  <strong>Plan actif</strong>
                  <small>Visible et souscriptible par les nouvelles paroisses.</small>
                </span>
                <span className="saas-pricing-switch">
                  <input
                    type="checkbox"
                    checked={selectedForm.actif}
                    onChange={(event) => setActive(selectedPlan.publicId, event.target.checked)}
                  />
                  <span aria-hidden="true" />
                </span>
              </label>

              <label className={`saas-pricing-switch-row${!selectedForm.actif ? ' is-disabled' : ''}`}>
                <span>
                  <strong>Plan recommandé</strong>
                  <small>Met cette formule visuellement en avant. Un seul plan à la fois.</small>
                </span>
                <span className="saas-pricing-switch">
                  <input
                    type="checkbox"
                    checked={selectedForm.featured}
                    disabled={!selectedForm.actif}
                    onChange={(event) => setField(selectedPlan.publicId, 'featured', event.target.checked)}
                  />
                  <span aria-hidden="true" />
                </span>
              </label>

              <div className="saas-pricing-preview">
                <span>Aperçu financier</span>
                <strong>{formatCurrency(Number(selectedForm.montantXof) || 0)}</strong>
                <small>
                  {durationLabel(selectedForm.dureeMois)}
                  {Number(selectedForm.dureeMois) > 0 && Number(selectedForm.montantXof) > 0
                    ? ` · env. ${formatCurrency(Math.round(Number(selectedForm.montantXof) / Number(selectedForm.dureeMois)))}/mois`
                    : ''}
                </small>
              </div>
            </aside>
          </div>

          <div className="saas-pricing-editor-actions">
            <span className="muted text-sm">
              Les abonnements déjà créés conservent leur montant et leur durée d’origine.
            </span>
            <div className="button-row">
              <AppButton variant="secondary" onClick={cancelEdit} disabled={savingId === selectedPlan.publicId}>
                Annuler
              </AppButton>
              <AppButton
                type="submit"
                loading={savingId === selectedPlan.publicId}
                disabled={!selectedDirty}
              >
                Enregistrer les modifications
              </AppButton>
            </div>
          </div>
        </form>
      ) : null}
    </div>
  );
}
