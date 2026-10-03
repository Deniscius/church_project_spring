import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AppButton from '../../../components/ui/AppButton';
import AppCard from '../../../components/ui/AppCard';
import AppInput from '../../../components/ui/AppInput';
import AppTextarea from '../../../components/ui/AppTextarea';
import FormError from '../../../components/ui/FormError';
import { useScrollToError } from '../../../hooks/useScrollToError';
import { useTenant } from '../../../hooks/useTenant';
import { pastoralYearService } from '../../../services/pastoralYear.service';
import { ROUTES } from '../../../constants/routes';

const INITIAL_VALUE = {
  libelle: '',
  dateDebut: '',
  dateFin: '',
  description: '',
  version: null,
};

export default function PastoralYearForm({ pastoralYearId = null }) {
  const navigate = useNavigate();
  const { activeParish } = useTenant();
  const [form, setForm] = useState(INITIAL_VALUE);
  const [loading, setLoading] = useState(Boolean(pastoralYearId));
  const [error, setError] = useState(null);
  const errorRef = useScrollToError(error);

  useEffect(() => {
    if (!pastoralYearId) return undefined;
    let cancelled = false;
    (async () => {
      try {
        setError(null);
        const data = await pastoralYearService.getById(pastoralYearId);
        if (!cancelled) {
          setForm({
            libelle: data.libelle || '',
            dateDebut: data.dateDebut || '',
            dateFin: data.dateFin || '',
            description: data.description || '',
            version: data.version,
          });
        }
      } catch (e) {
        if (!cancelled) {
          setError(e instanceof Error ? e.message : 'Année pastorale introuvable');
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => { cancelled = true; };
  }, [pastoralYearId]);

  const submit = async (event) => {
    event.preventDefault();
    if (!activeParish?.id) {
      setError('Aucune paroisse active');
      return;
    }
    if (form.dateDebut && form.dateFin && form.dateFin <= form.dateDebut) {
      setError('La date de fin doit être postérieure à la date de début');
      return;
    }

    try {
      setLoading(true);
      setError(null);
      const payload = {
        ...form,
        paroissePublicId: activeParish.id,
        description: form.description.trim() || null,
      };
      if (pastoralYearId) {
        await pastoralYearService.update(pastoralYearId, payload);
      } else {
        await pastoralYearService.create(payload);
      }
      navigate(ROUTES.PASTORAL_YEARS);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Enregistrement impossible');
    } finally {
      setLoading(false);
    }
  };

  return (
    <AppCard title={pastoralYearId ? 'Année pastorale existante' : 'Nouvelle année pastorale'}>
      <FormError error={error} errorRef={errorRef} />
      <form onSubmit={submit}>
        <div className="form-grid">
          <div className="form-field full">
            <label htmlFor="pastoral-year-label">Libellé *</label>
            <AppInput
              id="pastoral-year-label"
              required
              maxLength={100}
              placeholder="Ex. 2026-2027"
              value={form.libelle}
              onChange={(event) => setForm({ ...form, libelle: event.target.value })}
            />
          </div>
          <div className="form-field">
            <label htmlFor="pastoral-year-start">Date de début *</label>
            <AppInput
              id="pastoral-year-start"
              type="date"
              required
              value={form.dateDebut}
              onChange={(event) => setForm({ ...form, dateDebut: event.target.value })}
            />
          </div>
          <div className="form-field">
            <label htmlFor="pastoral-year-end">Date de fin *</label>
            <AppInput
              id="pastoral-year-end"
              type="date"
              required
              min={form.dateDebut || undefined}
              value={form.dateFin}
              onChange={(event) => setForm({ ...form, dateFin: event.target.value })}
            />
          </div>
          <div className="form-field full">
            <label htmlFor="pastoral-year-description">Description</label>
            <AppTextarea
              id="pastoral-year-description"
              maxLength={1000}
              rows={4}
              placeholder="Orientations ou thème pastoral de l’année"
              value={form.description}
              onChange={(event) => setForm({ ...form, description: event.target.value })}
            />
          </div>
        </div>
        <p className="muted">
          L’année sera créée en brouillon. Ses dates deviennent immuables après publication.
        </p>
        <div className="button-row" style={{ marginTop: 18 }}>
          <AppButton type="submit" disabled={loading}>
            {loading ? 'Enregistrement…' : 'Enregistrer'}
          </AppButton>
          <AppButton
            type="button"
            variant="secondary"
            onClick={() => navigate(ROUTES.PASTORAL_YEARS)}
          >
            Annuler
          </AppButton>
        </div>
      </form>
    </AppCard>
  );
}
