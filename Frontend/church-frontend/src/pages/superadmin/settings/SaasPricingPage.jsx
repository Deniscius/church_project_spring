import React, { useCallback, useEffect, useMemo, useState } from 'react';
import PageHeader from '../../../components/ui/PageHeader';
import AppAlert from '../../../components/ui/AppAlert';
import AppButton from '../../../components/ui/AppButton';
import AppDialog from '../../../components/ui/AppDialog';
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

const createFormDefaults = (ordreAffichage = 10) => ({
  code: '',
  nom: '',
  description: '',
  montantXof: '',
  dureeMois: '',
  actif: true,
  featured: false,
  ordreAffichage: String(ordreAffichage),
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

function normalizeCodeInput(value) {
  return String(value || '')
    .toUpperCase()
    .replace(/\s+/g, '_')
    .replace(/[^A-Z0-9_-]/g, '')
    .slice(0, 30);
}

function validateCommercialValues(form, { requireCode = false } = {}) {
  const code = normalizeCodeInput(form.code);
  const montantXof = Number(form.montantXof);
  const dureeMois = Number(form.dureeMois);
  const ordreAffichage = Number(form.ordreAffichage);

  if (requireCode && !code) {
    return { error: 'Le code interne du plan est obligatoire.' };
  }
  if (requireCode && !/^[A-Z0-9][A-Z0-9_-]{0,29}$/.test(code)) {
    return { error: 'Le code doit contenir uniquement des lettres, chiffres, tirets ou underscores.' };
  }
  if (!form.nom.trim()) {
    return { error: 'Le nom commercial du plan est obligatoire.' };
  }
  if (!Number.isInteger(montantXof) || montantXof <= 0) {
    return { error: 'Le prix doit être un montant entier supérieur à 0 FCFA.' };
  }
  if (!Number.isInteger(dureeMois) || dureeMois <= 0) {
    return { error: 'La durée doit être exprimée en mois entiers et être supérieure à 0.' };
  }
  if (!Number.isInteger(ordreAffichage) || ordreAffichage < 0) {
    return { error: 'L’ordre d’affichage doit être un entier positif ou nul.' };
  }

  return {
    values: {
      ...(requireCode ? { code } : {}),
      nom: form.nom.trim(),
      description: form.description.trim() || null,
      montantXof,
      dureeMois,
      actif: Boolean(form.actif),
      featured: Boolean(form.actif && form.featured),
      ordreAffichage,
    },
  };
}

function PricingPlanCard({ plan, position, onEdit, onToggleActive, busy }) {
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
          <span>Position d’affichage</span>
          <strong>#{position}</strong>
        </div>
      </div>

      <div className="saas-plan-card-footer">
        <span className="saas-plan-contract-note">
          {plan.actif ? 'Proposé aux nouvelles souscriptions' : 'Masqué aux nouvelles souscriptions'}
        </span>
        <div className="saas-plan-card-actions">
          <AppButton
            variant="secondary"
            size="sm"
            onClick={() => onToggleActive(plan)}
            disabled={busy}
          >
            {plan.actif ? 'Désactiver' : 'Activer'}
          </AppButton>
          <AppButton variant="secondary" size="sm" onClick={() => onEdit(plan)} disabled={busy}>
            Modifier
          </AppButton>
        </div>
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
  const [discardIntent, setDiscardIntent] = useState(null);
  const [createOpen, setCreateOpen] = useState(false);
  const [createBusy, setCreateBusy] = useState(false);
  const [createForm, setCreateForm] = useState(() => createFormDefaults());
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
  const nextDisplayOrder = useMemo(() => {
    if (!plans.length) return 10;
    return Math.max(...plans.map((plan) => Number(plan.ordreAffichage) || 0)) + 10;
  }, [plans]);

  useEffect(() => {
    if (!selectedDirty) return undefined;

    const preventSilentUnload = (event) => {
      event.preventDefault();
      event.returnValue = '';
    };

    window.addEventListener('beforeunload', preventSilentUnload);
    return () => window.removeEventListener('beforeunload', preventSilentUnload);
  }, [selectedDirty]);

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

  const setCreateField = (field, value) => {
    setCreateForm((current) => ({
      ...current,
      [field]: value,
      ...(field === 'actif' && !value ? { featured: false } : {}),
    }));
  };

  const scrollToEditor = () => {
    window.requestAnimationFrame(() => {
      document.getElementById('saas-pricing-editor')?.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    });
  };

  const activateEditor = (plan) => {
    setEditingId(plan.publicId);
    setError(null);
    setInfo(null);
    scrollToEditor();
  };

  const openEditor = (plan) => {
    if (plan.publicId === editingId) {
      scrollToEditor();
      return;
    }

    if (selectedDirty) {
      setDiscardIntent({ type: 'switch-plan', planId: plan.publicId });
      return;
    }

    activateEditor(plan);
  };

  const openCreate = () => {
    setCreateForm(createFormDefaults(nextDisplayOrder));
    setError(null);
    setInfo(null);
    setCreateOpen(true);
  };

  const closeCreate = () => {
    if (createBusy) return;
    setCreateOpen(false);
  };

  const requestRefresh = () => {
    if (selectedDirty) {
      setDiscardIntent({ type: 'refresh' });
      return;
    }
    load();
  };

  const resetSelectedForm = () => {
    if (!selectedPlan) return;
    setForms((current) => ({
      ...current,
      [selectedPlan.publicId]: toForm(selectedPlan),
    }));
  };

  const confirmDiscard = async () => {
    const intent = discardIntent;
    if (!intent) return;

    resetSelectedForm();
    setDiscardIntent(null);
    setError(null);
    setInfo(null);

    if (intent.type === 'refresh') {
      await load();
      return;
    }

    if (intent.type === 'switch-plan') {
      const nextPlan = plans.find((plan) => plan.publicId === intent.planId);
      if (nextPlan) activateEditor(nextPlan);
    }
  };

  const cancelEdit = () => {
    resetSelectedForm();
    setEditingId(null);
    setError(null);
  };

  const createPlan = async () => {
    const validated = validateCommercialValues(createForm, { requireCode: true });
    if (validated.error) {
      setError(validated.error);
      return;
    }

    try {
      setCreateBusy(true);
      setError(null);
      setInfo(null);
      const saved = await planSaasService.create(validated.values);
      setCreateOpen(false);
      await load({ silent: true });
      setInfo(`La formule « ${saved.nom} » a été créée${saved.actif ? ' et est disponible aux paroisses' : ' en mode inactif'}.`);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Création impossible');
    } finally {
      setCreateBusy(false);
    }
  };

  const toggleActive = async (plan) => {
    const nextActive = !plan.actif;
    const form = toForm(plan);
    const validated = validateCommercialValues({
      ...form,
      actif: nextActive,
      featured: nextActive ? form.featured : false,
    });
    if (validated.error) {
      setError(validated.error);
      return;
    }

    try {
      setSavingId(plan.publicId);
      setError(null);
      setInfo(null);
      const saved = await planSaasService.update(plan.publicId, validated.values);
      await load({ silent: true });
      setInfo(
        saved.actif
          ? `La formule « ${saved.nom} » est de nouveau disponible aux nouvelles souscriptions.`
          : `La formule « ${saved.nom} » est désactivée. Les abonnements historiques restent inchangés.`
      );
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Mise à jour impossible');
    } finally {
      setSavingId(null);
    }
  };

  const save = async (plan) => {
    const form = forms[plan.publicId];
    if (!form) return;

    const validated = validateCommercialValues(form);
    if (validated.error) {
      setError(validated.error);
      return;
    }

    try {
      setSavingId(plan.publicId);
      setError(null);
      setInfo(null);
      const saved = await planSaasService.update(plan.publicId, validated.values);

      setPlans((current) => current.map((item) => item.publicId === saved.publicId ? saved : item));
      setForms((current) => ({ ...current, [saved.publicId]: toForm(saved) }));

      await load({ silent: true });

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
        subtitle="Créez, ordonnez et pilotez les formules proposées aux paroisses depuis une source de vérité unique."
        actions={(
          <div className="button-row">
            <AppButton variant="secondary" onClick={requestRefresh} disabled={loading || Boolean(savingId) || createBusy}>
              {loading ? 'Actualisation…' : 'Actualiser'}
            </AppButton>
            <AppButton onClick={openCreate} disabled={loading || createBusy}>
              + Nouvelle formule
            </AppButton>
          </div>
        )}
      />

      <section className="saas-pricing-hero" aria-label="Règle de tarification">
        <div>
          <span className="saas-pricing-kicker">Catalogue commercial dynamique</span>
          <h2>Faites évoluer les offres sans redéployer l’application</h2>
          <p>
            Créez de nouvelles durées, activez ou masquez une formule et choisissez celle à recommander.
            Les montants et durées déjà vendus restent figés dans l’historique des abonnements.
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
          <p>Créez la première formule commerciale à proposer aux paroisses.</p>
          <AppButton onClick={openCreate}>Créer une formule</AppButton>
        </div>
      ) : null}

      {!loading && plans.length > 0 ? (
        <section className="saas-plan-grid" aria-label="Plans disponibles">
          {plans.map((plan, index) => (
            <PricingPlanCard
              key={plan.publicId}
              plan={plan}
              position={index + 1}
              onEdit={openEditor}
              onToggleActive={toggleActive}
              busy={savingId === plan.publicId || createBusy}
            />
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
              <p>Le code interne reste fixe pour protéger l’historique. Les propriétés commerciales restent administrables.</p>
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
                  <span className="muted text-sm">Montant appliqué aux prochaines souscriptions et renouvellements.</span>
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
                  <span className="muted text-sm">Plus la valeur est petite, plus la formule apparaît tôt.</span>
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
              Désactiver une formule la retire des nouvelles souscriptions sans supprimer les contrats historiques.
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

      <AppDialog
        open={createOpen}
        title="Créer une formule SaaS"
        confirmLabel="Créer la formule"
        cancelLabel="Annuler"
        onConfirm={createPlan}
        onCancel={closeCreate}
        busy={createBusy}
        size="lg"
      >
        <div className="saas-pricing-create-intro">
          La nouvelle formule rejoint immédiatement le catalogue si elle est active. Son code interne devient immuable après création.
        </div>
        <div className="form-grid saas-pricing-create-grid">
          <div className="form-field">
            <label htmlFor="new-plan-code">Code interne *</label>
            <AppInput
              id="new-plan-code"
              maxLength={30}
              value={createForm.code}
              disabled={createBusy}
              onChange={(event) => setCreateField('code', normalizeCodeInput(event.target.value))}
              placeholder="Ex. TRIMESTRIEL"
            />
            <span className="muted text-sm">Lettres, chiffres, tirets ou underscores. Non modifiable ensuite.</span>
          </div>

          <div className="form-field">
            <label htmlFor="new-plan-name">Nom commercial *</label>
            <AppInput
              id="new-plan-name"
              maxLength={100}
              value={createForm.nom}
              disabled={createBusy}
              onChange={(event) => setCreateField('nom', event.target.value)}
              placeholder="Ex. Trimestriel"
            />
          </div>

          <div className="form-field">
            <label htmlFor="new-plan-amount">Prix total (FCFA) *</label>
            <AppInput
              id="new-plan-amount"
              type="number"
              min="1"
              step="1"
              inputMode="numeric"
              value={createForm.montantXof}
              disabled={createBusy}
              onChange={(event) => setCreateField('montantXof', event.target.value)}
            />
          </div>

          <div className="form-field">
            <label htmlFor="new-plan-duration">Durée (mois) *</label>
            <AppInput
              id="new-plan-duration"
              type="number"
              min="1"
              step="1"
              inputMode="numeric"
              value={createForm.dureeMois}
              disabled={createBusy}
              onChange={(event) => setCreateField('dureeMois', event.target.value)}
            />
          </div>

          <div className="form-field">
            <label htmlFor="new-plan-order">Ordre d’affichage *</label>
            <AppInput
              id="new-plan-order"
              type="number"
              min="0"
              step="1"
              inputMode="numeric"
              value={createForm.ordreAffichage}
              disabled={createBusy}
              onChange={(event) => setCreateField('ordreAffichage', event.target.value)}
            />
            <span className="muted text-sm">Prérempli après les formules existantes, mais vous pouvez le modifier.</span>
          </div>

          <div className="form-field full">
            <label htmlFor="new-plan-description">Description commerciale</label>
            <textarea
              id="new-plan-description"
              className="textarea"
              maxLength={300}
              rows={3}
              value={createForm.description}
              disabled={createBusy}
              onChange={(event) => setCreateField('description', event.target.value)}
              placeholder="Ex. Une formule souple pour les paroisses qui souhaitent s’engager par trimestre."
            />
            <span className="saas-pricing-char-count">{createForm.description.length}/300</span>
          </div>
        </div>

        <div className="saas-pricing-create-options">
          <label className="saas-pricing-switch-row">
            <span>
              <strong>Activer dès maintenant</strong>
              <small>La formule sera visible sur la page des offres et pendant l’inscription.</small>
            </span>
            <span className="saas-pricing-switch">
              <input
                type="checkbox"
                checked={createForm.actif}
                disabled={createBusy}
                onChange={(event) => setCreateField('actif', event.target.checked)}
              />
              <span aria-hidden="true" />
            </span>
          </label>

          <label className={`saas-pricing-switch-row${!createForm.actif ? ' is-disabled' : ''}`}>
            <span>
              <strong>Définir comme recommandée</strong>
              <small>Remplacera automatiquement la formule actuellement recommandée.</small>
            </span>
            <span className="saas-pricing-switch">
              <input
                type="checkbox"
                checked={createForm.featured}
                disabled={!createForm.actif || createBusy}
                onChange={(event) => setCreateField('featured', event.target.checked)}
              />
              <span aria-hidden="true" />
            </span>
          </label>
        </div>
      </AppDialog>

      <AppDialog
        open={Boolean(discardIntent)}
        title="Abandonner les modifications ?"
        confirmLabel={discardIntent?.type === 'refresh' ? 'Actualiser quand même' : 'Changer de plan'}
        cancelLabel="Continuer l’édition"
        onConfirm={confirmDiscard}
        onCancel={() => setDiscardIntent(null)}
        danger
      >
        Vous avez des modifications non enregistrées sur le plan « {selectedPlan?.nom || 'en cours'} ».
        Elles seront perdues si vous continuez.
      </AppDialog>
    </div>
  );
}
