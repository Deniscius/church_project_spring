import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AppButton from '../../../components/ui/AppButton';
import AppCard from '../../../components/ui/AppCard';
import AppInput from '../../../components/ui/AppInput';
import AppTextarea from '../../../components/ui/AppTextarea';
import FormError from '../../../components/ui/FormError';
import { useScrollToError } from '../../../hooks/useScrollToError';
import { useTenant } from '../../../hooks/useTenant';
import { pastoralOrganizationService } from '../../../services/pastoralOrganization.service';
import { ROUTES } from '../../../constants/routes';
import { STRUCTURE_TYPES } from './pastoralOrganization.constants';

const INITIAL_VALUE = {
  type: 'CPP',
  nom: '',
  attributions: '',
  ordreAffichage: 0,
  version: null,
};

export default function StructurePastoraleForm({ structureId = null }) {
  const navigate = useNavigate();
  const { activeParish } = useTenant();
  const [form, setForm] = useState(INITIAL_VALUE);
  const [loading, setLoading] = useState(Boolean(structureId));
  const [error, setError] = useState(null);
  const errorRef = useScrollToError(error);

  useEffect(() => {
    if (!structureId) return undefined;
    let cancelled = false;
    (async () => {
      try {
        const data = await pastoralOrganizationService.getStructure(structureId);
        if (!cancelled) {
          setForm({
            type: data.type || 'CPP',
            nom: data.nom || '',
            attributions: data.attributions || '',
            ordreAffichage: data.ordreAffichage ?? 0,
            version: data.version,
          });
        }
      } catch (e) {
        if (!cancelled) setError(e instanceof Error ? e.message : 'Structure introuvable');
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => { cancelled = true; };
  }, [structureId]);

  const field = (name, value) => setForm((current) => ({ ...current, [name]: value }));

  const submit = async (event) => {
    event.preventDefault();
    if (!activeParish?.id) {
      setError('Aucune paroisse active');
      return;
    }
    const payload = {
      ...form,
      nom: form.nom.trim(),
      attributions: form.attributions.trim() || null,
      ordreAffichage: Number(form.ordreAffichage) || 0,
      paroissePublicId: activeParish.id,
    };
    try {
      setLoading(true);
      setError(null);
      if (structureId) {
        await pastoralOrganizationService.updateStructure(structureId, payload);
      } else {
        await pastoralOrganizationService.createStructure(payload);
      }
      navigate(ROUTES.PASTORAL_ORGANIZATION);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Enregistrement impossible');
    } finally {
      setLoading(false);
    }
  };

  return (
    <AppCard title={structureId ? 'Structure pastorale' : 'Nouvelle structure pastorale'}>
      <FormError error={error} errorRef={errorRef} />
      <form onSubmit={submit}>
        <div className="form-grid">
          <div className="form-field">
            <label htmlFor="pastoral-structure-type">Type *</label>
            <select
              id="pastoral-structure-type"
              className="select"
              required
              value={form.type}
              onChange={(event) => field('type', event.target.value)}
            >
              {STRUCTURE_TYPES.map((type) => (
                <option key={type.value} value={type.value}>{type.label}</option>
              ))}
            </select>
          </div>
          <div className="form-field">
            <label htmlFor="pastoral-structure-order">Ordre d’affichage</label>
            <AppInput
              id="pastoral-structure-order"
              type="number"
              min="0"
              value={form.ordreAffichage}
              onChange={(event) => field('ordreAffichage', event.target.value)}
            />
          </div>
          <div className="form-field full">
            <label htmlFor="pastoral-structure-name">Dénomination *</label>
            <AppInput
              id="pastoral-structure-name"
              required
              maxLength={150}
              placeholder="Ex. Bureau exécutif du CPP, Commission Liturgie"
              value={form.nom}
              onChange={(event) => field('nom', event.target.value)}
            />
          </div>
          <div className="form-field full">
            <label htmlFor="pastoral-structure-duties">Attributions</label>
            <AppTextarea
              id="pastoral-structure-duties"
              maxLength={1500}
              rows={5}
              placeholder="Mission et responsabilités de cette instance"
              value={form.attributions}
              onChange={(event) => field('attributions', event.target.value)}
            />
          </div>
        </div>
        <div className="button-row" style={{ marginTop: 18 }}>
          <AppButton type="submit" disabled={loading}>
            {loading ? 'Enregistrement…' : 'Enregistrer'}
          </AppButton>
          <AppButton type="button" variant="secondary" onClick={() => navigate(ROUTES.PASTORAL_ORGANIZATION)}>
            Annuler
          </AppButton>
        </div>
      </form>
    </AppCard>
  );
}
