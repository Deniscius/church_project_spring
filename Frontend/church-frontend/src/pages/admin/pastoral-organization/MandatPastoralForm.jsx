import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import AppButton from '../../../components/ui/AppButton';
import AppCard from '../../../components/ui/AppCard';
import AppInput from '../../../components/ui/AppInput';
import AppTextarea from '../../../components/ui/AppTextarea';
import FormError from '../../../components/ui/FormError';
import { useScrollToError } from '../../../hooks/useScrollToError';
import { useTenant } from '../../../hooks/useTenant';
import { pastoralYearService } from '../../../services/pastoralYear.service';
import { pastoralActorService } from '../../../services/pastoralActor.service';
import { pastoralOrganizationService } from '../../../services/pastoralOrganization.service';
import { ROUTES } from '../../../constants/routes';

const INITIAL_VALUE = {
  anneePastoralePublicId: '',
  structurePastoralePublicId: '',
  acteurPastoralPublicId: '',
  fonction: '',
  attributions: '',
  dateDebut: '',
  dateFin: '',
  ordreAffichage: 0,
  version: null,
};

export default function MandatPastoralForm({ mandateId = null }) {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { activeParish } = useTenant();
  const [form, setForm] = useState(INITIAL_VALUE);
  const [years, setYears] = useState([]);
  const [structures, setStructures] = useState([]);
  const [actors, setActors] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const errorRef = useScrollToError(error);

  useEffect(() => {
    if (!activeParish?.id) {
      setLoading(false);
      return undefined;
    }
    let cancelled = false;
    (async () => {
      try {
        setError(null);
        const [yearData, structureData, actorData, current] = await Promise.all([
          pastoralYearService.getByParish(activeParish.id),
          pastoralOrganizationService.getStructures(activeParish.id),
          pastoralActorService.getByParish(activeParish.id, { actif: true }),
          mandateId ? pastoralOrganizationService.getMandate(mandateId) : Promise.resolve(null),
        ]);
        if (cancelled) return;
        const availableYears = Array.isArray(yearData) ? yearData : [];
        const availableStructures = Array.isArray(structureData)
          ? structureData.filter((item) => item.actif)
          : [];
        const availableActors = Array.isArray(actorData) ? actorData : [];
        setYears(availableYears);
        setStructures(availableStructures);
        setActors(availableActors);

        if (current) {
          setForm({
            anneePastoralePublicId: current.anneePastoralePublicId,
            structurePastoralePublicId: current.structurePastoralePublicId,
            acteurPastoralPublicId: current.acteurPastoralPublicId,
            fonction: current.fonction || '',
            attributions: current.attributions || '',
            dateDebut: current.dateDebut || '',
            dateFin: current.dateFin || '',
            ordreAffichage: current.ordreAffichage ?? 0,
            version: current.version,
          });
        } else {
          const requestedYear = searchParams.get('annee');
          const requestedStructure = searchParams.get('structure');
          const preferredYear = availableYears.find((item) => item.publicId === requestedYear)
            || availableYears.find((item) => item.statut === 'PUBLIEE')
            || availableYears[0];
          setForm((value) => ({
            ...value,
            anneePastoralePublicId: preferredYear?.publicId || '',
            structurePastoralePublicId: availableStructures.some(
              (item) => item.publicId === requestedStructure
            ) ? requestedStructure : (availableStructures[0]?.publicId || ''),
          }));
        }
      } catch (e) {
        if (!cancelled) setError(e instanceof Error ? e.message : 'Chargement impossible');
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => { cancelled = true; };
  }, [activeParish?.id, mandateId, searchParams]);

  const selectedYear = useMemo(
    () => years.find((year) => year.publicId === form.anneePastoralePublicId),
    [form.anneePastoralePublicId, years]
  );

  const field = (name, value) => setForm((current) => ({ ...current, [name]: value }));

  const submit = async (event) => {
    event.preventDefault();
    if (!activeParish?.id) {
      setError('Aucune paroisse active');
      return;
    }
    const payload = {
      ...form,
      fonction: form.fonction.trim(),
      attributions: form.attributions.trim() || null,
      dateDebut: form.dateDebut || null,
      dateFin: form.dateFin || null,
      ordreAffichage: Number(form.ordreAffichage) || 0,
      paroissePublicId: activeParish.id,
    };
    try {
      setLoading(true);
      setError(null);
      if (mandateId) {
        await pastoralOrganizationService.updateMandate(mandateId, payload);
      } else {
        await pastoralOrganizationService.createMandate(payload);
      }
      navigate(ROUTES.PASTORAL_ORGANIZATION);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Enregistrement impossible');
    } finally {
      setLoading(false);
    }
  };

  return (
    <AppCard title={mandateId ? 'Responsabilité pastorale' : 'Nouvelle responsabilité pastorale'}>
      <FormError error={error} errorRef={errorRef} />
      <form onSubmit={submit}>
        <div className="form-grid">
          <div className="form-field">
            <label htmlFor="mandate-year">Année pastorale *</label>
            <select
              id="mandate-year"
              className="select"
              required
              value={form.anneePastoralePublicId}
              onChange={(event) => field('anneePastoralePublicId', event.target.value)}
            >
              <option value="">Sélectionner</option>
              {years.map((year) => (
                <option key={year.publicId} value={year.publicId}>
                  {year.libelle} — {year.statut}
                </option>
              ))}
            </select>
          </div>
          <div className="form-field">
            <label htmlFor="mandate-structure">Structure *</label>
            <select
              id="mandate-structure"
              className="select"
              required
              value={form.structurePastoralePublicId}
              onChange={(event) => field('structurePastoralePublicId', event.target.value)}
            >
              <option value="">Sélectionner</option>
              {structures.map((structure) => (
                <option key={structure.publicId} value={structure.publicId}>{structure.nom}</option>
              ))}
            </select>
          </div>
          <div className="form-field">
            <label htmlFor="mandate-actor">Personne *</label>
            <select
              id="mandate-actor"
              className="select"
              required
              value={form.acteurPastoralPublicId}
              onChange={(event) => field('acteurPastoralPublicId', event.target.value)}
            >
              <option value="">Sélectionner</option>
              {actors.map((actor) => (
                <option key={actor.publicId} value={actor.publicId}>
                  {[actor.appellation, actor.prenoms, actor.nom].filter(Boolean).join(' ')}
                </option>
              ))}
            </select>
          </div>
          <div className="form-field">
            <label htmlFor="mandate-role">Rôle ou fonction *</label>
            <AppInput
              id="mandate-role"
              required
              maxLength={120}
              placeholder="Ex. Curé, Vice-président, Responsable"
              value={form.fonction}
              onChange={(event) => field('fonction', event.target.value)}
            />
          </div>
          <div className="form-field">
            <label htmlFor="mandate-start">Début du mandat</label>
            <AppInput
              id="mandate-start"
              type="date"
              min={selectedYear?.dateDebut}
              max={selectedYear?.dateFin}
              value={form.dateDebut}
              onChange={(event) => field('dateDebut', event.target.value)}
            />
          </div>
          <div className="form-field">
            <label htmlFor="mandate-end">Fin du mandat</label>
            <AppInput
              id="mandate-end"
              type="date"
              min={form.dateDebut || selectedYear?.dateDebut}
              max={selectedYear?.dateFin}
              value={form.dateFin}
              onChange={(event) => field('dateFin', event.target.value)}
            />
          </div>
          <div className="form-field">
            <label htmlFor="mandate-order">Ordre d’affichage</label>
            <AppInput
              id="mandate-order"
              type="number"
              min="0"
              value={form.ordreAffichage}
              onChange={(event) => field('ordreAffichage', event.target.value)}
            />
          </div>
          <div className="form-field full">
            <label htmlFor="mandate-duties">Attributions</label>
            <AppTextarea
              id="mandate-duties"
              maxLength={1500}
              rows={4}
              placeholder="Responsabilités confiées à cette personne"
              value={form.attributions}
              onChange={(event) => field('attributions', event.target.value)}
            />
          </div>
        </div>
        <p className="muted">
          Sans dates particulières, le mandat couvrira toute l’année pastorale sélectionnée.
        </p>
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
