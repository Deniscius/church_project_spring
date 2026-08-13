import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AppButton from '../../../components/ui/AppButton';
import AppCard from '../../../components/ui/AppCard';
import AppInput from '../../../components/ui/AppInput';
import WeekDaySelector from '../../../components/ui/WeekDaySelector';
import { NATURE_FORFAIT_OPTIONS, WEEK_DAYS, getForfaitDureeLabel } from '../../../constants/enums';
import { useTenant } from '../../../hooks/useTenant';
import { useScrollToError } from '../../../hooks/useScrollToError';
import { pricingService } from '../../../services/pricing.service';
import { requestTypeService } from '../../../services/requestType.service';
import { filterDaysWithinType, formatAllowedDays } from '../../../utils/schedulingUtils';
import FormError from '../../../components/ui/FormError';

const INITIAL_VALUE = {
  codeForfait: '',
  nomForfait: '',
  libelle: '',
  natureForfait: 'NORMALE',
  montantForfait: '',
  nombreJour: '',
  nombreCelebration: '1',
  joursCelebrationAutorises: ['DIMANCHE'],
  heurePersonnalise: false,
  isActive: true,
  typeDemandePublicId: '',
};

const optionalNumber = (value) => (value === '' ? null : Number(value));

const sortDays = (days) => [...days].sort((a, b) => WEEK_DAYS.indexOf(a) - WEEK_DAYS.indexOf(b));

export default function PricingForm({ pricingId = null }) {
  const navigate = useNavigate();
  const { activeParish } = useTenant();
  const [form, setForm] = useState(INITIAL_VALUE);
  const [types, setTypes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const errorRef = useScrollToError(error);

  const selectedType = useMemo(
    () => types.find((type) => type.publicId === form.typeDemandePublicId) || null,
    [types, form.typeDemandePublicId]
  );
  const typeAllowedDays = selectedType?.joursCelebrationAutorises || [];

  useEffect(() => {
    if (!activeParish?.id) return;
    let cancelled = false;
    (async () => {
      try {
        setLoading(true);
        const [availableTypes, existing] = await Promise.all([
          requestTypeService.getByParish(activeParish.id),
          pricingId ? pricingService.getById(pricingId) : Promise.resolve(null),
        ]);
        if (cancelled) return;
        setTypes(availableTypes || []);
        if (existing) {
          const type = (availableTypes || []).find((item) => item.publicId === existing.typeDemandePublicId);
          const allowedByType = type?.joursCelebrationAutorises || [];
          const forfaitDays = existing.joursCelebrationAutorises?.length
            ? filterDaysWithinType(allowedByType, existing.joursCelebrationAutorises)
            : allowedByType.length
              ? allowedByType
              : ['DIMANCHE'];
          setForm({
            codeForfait: existing.codeForfait || '',
            nomForfait: existing.nomForfait || '',
            libelle: existing.libelle || '',
            natureForfait: existing.natureForfait || 'NORMALE',
            montantForfait: existing.montantForfait ?? '',
            nombreJour: existing.nombreJour ?? '',
            nombreCelebration: existing.nombreCelebration ?? '',
            joursCelebrationAutorises: sortDays(forfaitDays),
            heurePersonnalise: Boolean(existing.heurePersonnalise),
            isActive: existing.isActive !== false,
            typeDemandePublicId: existing.typeDemandePublicId || '',
          });
        } else {
          const firstType = availableTypes?.[0];
          setForm((current) => ({
            ...current,
            typeDemandePublicId: firstType?.publicId || '',
            joursCelebrationAutorises: firstType?.joursCelebrationAutorises?.length
              ? sortDays(firstType.joursCelebrationAutorises)
              : ['DIMANCHE'],
          }));
        }
      } catch (e) {
        if (!cancelled) setError(e instanceof Error ? e.message : 'Chargement impossible');
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => { cancelled = true; };
  }, [activeParish?.id, pricingId]);

  const field = (name, value) => setForm((current) => ({ ...current, [name]: value }));

  const handleTypeChange = (typeDemandePublicId) => {
    const type = types.find((item) => item.publicId === typeDemandePublicId);
    const nextTypeDays = type?.joursCelebrationAutorises?.length
      ? type.joursCelebrationAutorises
      : ['DIMANCHE'];
    setForm((current) => ({
      ...current,
      typeDemandePublicId,
      joursCelebrationAutorises: sortDays(
        filterDaysWithinType(nextTypeDays, current.joursCelebrationAutorises).length
          ? filterDaysWithinType(nextTypeDays, current.joursCelebrationAutorises)
          : nextTypeDays
      ),
    }));
  };

  const submit = async (event) => {
    event.preventDefault();
    if (!form.joursCelebrationAutorises.length) {
      return setError('Sélectionnez au moins un jour de célébration pour ce forfait');
    }
    try {
      setLoading(true);
      setError(null);
      const payload = {
        // En modification : renvoyer le code existant. En création : omis → généré côté API.
        ...(pricingId && form.codeForfait ? { codeForfait: form.codeForfait } : {}),
        nomForfait: form.nomForfait,
        libelle: form.libelle,
        natureForfait: form.natureForfait,
        montantForfait: Number(form.montantForfait),
        nombreJour: optionalNumber(form.nombreJour),
        nombreCelebration: optionalNumber(form.nombreCelebration),
        joursCelebrationAutorises: form.joursCelebrationAutorises,
        heurePersonnalise: form.heurePersonnalise,
        isActive: form.isActive,
        typeDemandePublicId: form.typeDemandePublicId,
      };
      if (pricingId) await pricingService.update(pricingId, payload);
      else await pricingService.create(payload);
      navigate('/admin/forfaits');
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Enregistrement impossible');
    } finally {
      setLoading(false);
    }
  };

  return (
    <AppCard title={pricingId ? 'Forfait existant' : 'Nouveau forfait'}>
      <FormError error={error} errorRef={errorRef} />
      {!types.length && !loading ? <p className="muted">Créez d’abord un type de demande actif.</p> : null}
      <form onSubmit={submit}>
        <div className="form-grid">
          {pricingId ? (
            <div className="form-field">
              <label htmlFor="pricing-code">Code forfait</label>
              <AppInput id="pricing-code" value={form.codeForfait} readOnly disabled />
              <small className="muted">Généré automatiquement à la création.</small>
            </div>
          ) : null}
          <div className="form-field"><label htmlFor="pricing-name">Nom *</label>
            <AppInput id="pricing-name" required value={form.nomForfait} onChange={(e) => field('nomForfait', e.target.value)} /></div>
          <div className="form-field"><label htmlFor="pricing-label">Libellé</label>
            <AppInput id="pricing-label" value={form.libelle} onChange={(e) => field('libelle', e.target.value)} /></div>
          <div className="form-field"><label htmlFor="pricing-amount">Montant *</label>
            <AppInput id="pricing-amount" type="number" min="1" step="0.01" required value={form.montantForfait} onChange={(e) => field('montantForfait', e.target.value)} /></div>
          <div className="form-field"><label htmlFor="pricing-type">Type de demande *</label>
            <select id="pricing-type" className="select" required value={form.typeDemandePublicId} onChange={(e) => handleTypeChange(e.target.value)}>
              {types.map((type) => <option key={type.publicId} value={type.publicId}>{type.libelle}</option>)}
            </select></div>
          <div className="form-field"><label htmlFor="pricing-nature">Nature *</label>
            <select
              id="pricing-nature"
              className="select"
              required
              value={form.natureForfait}
              onChange={(e) => {
                const natureForfait = e.target.value;
                setForm((current) => ({
                  ...current,
                  natureForfait,
                  // Spéciale / solennités : souvent tous les jours autorisés du type.
                  joursCelebrationAutorises: natureForfait === 'SPECIALE' && typeAllowedDays.length
                    ? sortDays(typeAllowedDays)
                    : current.joursCelebrationAutorises,
                  heurePersonnalise: natureForfait === 'SPECIALE' ? true : current.heurePersonnalise,
                }));
              }}
            >
              {NATURE_FORFAIT_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>{option.label}</option>
              ))}
            </select>
            <small className="muted">
              Normale / dominicale = selon le jour. Spéciale = solennités et célébrations
              exceptionnelles (montant dédié). Une seule nature par type de demande.
            </small>
          </div>
          <div className="form-field"><label htmlFor="pricing-celebrations">Nombre de célébrations *</label>
            <AppInput
              id="pricing-celebrations"
              type="number"
              min="1"
              required
              value={form.nombreCelebration}
              onChange={(e) => {
                const value = e.target.value;
                setForm((current) => ({
                  ...current,
                  nombreCelebration: value,
                  // Aligne le nombre de jours (triduum=3, neuvaine=9, trentaine=30) si vide ou égal à l’ancienne valeur.
                  nombreJour:
                    current.nombreJour === ''
                    || current.nombreJour === current.nombreCelebration
                      ? value
                      : current.nombreJour,
                }));
              }}
            />
            <small className="muted">
              {getForfaitDureeLabel(Number(form.nombreCelebration) || null)}
              {' '}— le fidèle choisira exactement ce nombre de dates (3 = triduum, 9 = neuvaine, 30 = trentaine).
            </small>
          </div>
          <div className="form-field"><label htmlFor="pricing-days">Nombre de jours (fenêtre)</label>
            <AppInput
              id="pricing-days"
              type="number"
              min="1"
              value={form.nombreJour}
              onChange={(e) => field('nombreJour', e.target.value)}
            />
            <small className="muted">
              Période calendaire maximale entre la 1<sup>re</sup> et la dernière date choisie.
              En général égal au nombre de célébrations.
            </small>
          </div>
          <div className="form-field full">
            <label htmlFor="pricing-celebration-days">Jours de célébration autorisés *</label>
            <WeekDaySelector
              id="pricing-celebration-days"
              value={form.joursCelebrationAutorises}
              availableDays={typeAllowedDays.length ? typeAllowedDays : null}
              onChange={(joursCelebrationAutorises) => field('joursCelebrationAutorises', joursCelebrationAutorises)}
            />
            <small className="muted">
              {typeAllowedDays.length
                ? `Doit être inclus dans les jours du type : ${formatAllowedDays(typeAllowedDays)}.`
                : 'Choisissez les jours applicables à ce forfait.'}
            </small>
          </div>
          <div className="form-field"><label htmlFor="pricing-custom-time">Heure personnalisée *</label>
            <select id="pricing-custom-time" className="select" value={String(form.heurePersonnalise)} onChange={(e) => field('heurePersonnalise', e.target.value === 'true')}>
              <option value="false">Non</option><option value="true">Oui</option>
            </select></div>
          <div className="form-field"><label htmlFor="pricing-active">État *</label>
            <select id="pricing-active" className="select" value={String(form.isActive)} onChange={(e) => field('isActive', e.target.value === 'true')}>
              <option value="true">Actif</option><option value="false">Inactif</option>
            </select></div>
        </div>
        <div className="button-row" style={{ marginTop: 18 }}>
          <AppButton type="submit" disabled={loading || !types.length}>{loading ? 'Enregistrement…' : 'Enregistrer'}</AppButton>
          <AppButton variant="secondary" onClick={() => navigate('/admin/forfaits')}>Annuler</AppButton>
        </div>
      </form>
    </AppCard>
  );
}
