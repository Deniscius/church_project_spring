import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AppButton from '../../../components/ui/AppButton';
import AppCard from '../../../components/ui/AppCard';
import AppInput from '../../../components/ui/AppInput';
import AppTextarea from '../../../components/ui/AppTextarea';
import FormError from '../../../components/ui/FormError';
import { useScrollToError } from '../../../hooks/useScrollToError';
import { useTenant } from '../../../hooks/useTenant';
import { pastoralActorService } from '../../../services/pastoralActor.service';
import { ROUTES } from '../../../constants/routes';

const INITIAL_VALUE = {
  nom: '',
  prenoms: '',
  appellation: '',
  categorie: 'LAIC',
  telephone: '',
  email: '',
  notes: '',
  version: null,
};

const CATEGORIES = [
  { value: 'CLERGE', label: 'Clergé' },
  { value: 'VIE_CONSACREE', label: 'Vie consacrée' },
  { value: 'LAIC', label: 'Laïc' },
  { value: 'AUTRE', label: 'Autre' },
];

export default function PastoralActorForm({ pastoralActorId = null }) {
  const navigate = useNavigate();
  const { activeParish } = useTenant();
  const [form, setForm] = useState(INITIAL_VALUE);
  const [loading, setLoading] = useState(Boolean(pastoralActorId));
  const [error, setError] = useState(null);
  const errorRef = useScrollToError(error);

  useEffect(() => {
    if (!pastoralActorId) return undefined;
    let cancelled = false;
    (async () => {
      try {
        setError(null);
        const data = await pastoralActorService.getById(pastoralActorId);
        if (!cancelled) {
          setForm({
            nom: data.nom || '',
            prenoms: data.prenoms || '',
            appellation: data.appellation || '',
            categorie: data.categorie || 'LAIC',
            telephone: data.telephone || '',
            email: data.email || '',
            notes: data.notes || '',
            version: data.version,
          });
        }
      } catch (e) {
        if (!cancelled) {
          setError(e instanceof Error ? e.message : 'Acteur pastoral introuvable');
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => { cancelled = true; };
  }, [pastoralActorId]);

  const updateField = (name, value) => {
    setForm((current) => ({ ...current, [name]: value }));
  };

  const submit = async (event) => {
    event.preventDefault();
    if (!activeParish?.id) {
      setError('Aucune paroisse active');
      return;
    }

    const payload = {
      ...form,
      nom: form.nom.trim(),
      prenoms: form.prenoms.trim(),
      appellation: form.appellation.trim() || null,
      telephone: form.telephone.trim() || null,
      email: form.email.trim() || null,
      notes: form.notes.trim() || null,
      paroissePublicId: activeParish.id,
    };

    try {
      setLoading(true);
      setError(null);
      if (pastoralActorId) {
        await pastoralActorService.update(pastoralActorId, payload);
      } else {
        await pastoralActorService.create(payload);
      }
      navigate(ROUTES.PASTORAL_ACTORS);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Enregistrement impossible');
    } finally {
      setLoading(false);
    }
  };

  return (
    <AppCard title={pastoralActorId ? 'Fiche de l’acteur pastoral' : 'Nouvel acteur pastoral'}>
      <FormError error={error} errorRef={errorRef} />
      <form onSubmit={submit}>
        <div className="form-grid">
          <div className="form-field">
            <label htmlFor="pastoral-actor-title">Appellation</label>
            <AppInput
              id="pastoral-actor-title"
              maxLength={50}
              placeholder="Ex. Père, Abbé, Sœur, Mme"
              value={form.appellation}
              onChange={(event) => updateField('appellation', event.target.value)}
            />
          </div>
          <div className="form-field">
            <label htmlFor="pastoral-actor-category">Catégorie *</label>
            <select
              id="pastoral-actor-category"
              className="select"
              required
              value={form.categorie}
              onChange={(event) => updateField('categorie', event.target.value)}
            >
              {CATEGORIES.map((category) => (
                <option key={category.value} value={category.value}>{category.label}</option>
              ))}
            </select>
          </div>
          <div className="form-field">
            <label htmlFor="pastoral-actor-last-name">Nom *</label>
            <AppInput
              id="pastoral-actor-last-name"
              required
              maxLength={100}
              autoComplete="family-name"
              value={form.nom}
              onChange={(event) => updateField('nom', event.target.value)}
            />
          </div>
          <div className="form-field">
            <label htmlFor="pastoral-actor-first-names">Prénoms *</label>
            <AppInput
              id="pastoral-actor-first-names"
              required
              maxLength={150}
              autoComplete="given-name"
              value={form.prenoms}
              onChange={(event) => updateField('prenoms', event.target.value)}
            />
          </div>
          <div className="form-field">
            <label htmlFor="pastoral-actor-phone">Téléphone</label>
            <AppInput
              id="pastoral-actor-phone"
              type="tel"
              maxLength={50}
              autoComplete="tel"
              value={form.telephone}
              onChange={(event) => updateField('telephone', event.target.value)}
            />
          </div>
          <div className="form-field">
            <label htmlFor="pastoral-actor-email">Email</label>
            <AppInput
              id="pastoral-actor-email"
              type="email"
              maxLength={150}
              autoComplete="email"
              value={form.email}
              onChange={(event) => updateField('email', event.target.value)}
            />
          </div>
          <div className="form-field full">
            <label htmlFor="pastoral-actor-notes">Notes internes</label>
            <AppTextarea
              id="pastoral-actor-notes"
              maxLength={1000}
              rows={4}
              placeholder="Informations utiles pour l’organisation pastorale"
              value={form.notes}
              onChange={(event) => updateField('notes', event.target.value)}
            />
          </div>
        </div>
        <p className="muted">
          Les fonctions et responsabilités seront attribuées séparément afin de conserver leur historique.
        </p>
        <div className="button-row" style={{ marginTop: 18 }}>
          <AppButton type="submit" disabled={loading}>
            {loading ? 'Enregistrement…' : 'Enregistrer'}
          </AppButton>
          <AppButton
            type="button"
            variant="secondary"
            onClick={() => navigate(ROUTES.PASTORAL_ACTORS)}
          >
            Annuler
          </AppButton>
        </div>
      </form>
    </AppCard>
  );
}
