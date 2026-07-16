import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AppButton from '../../../components/ui/AppButton';
import AppCard from '../../../components/ui/AppCard';
import AppInput from '../../../components/ui/AppInput';
import { useTenant } from '../../../hooks/useTenant';
import { pricingService } from '../../../services/pricing.service';
import { requestTypeService } from '../../../services/requestType.service';

const INITIAL_VALUE = {
  codeForfait: '', nomForfait: '', libelle: '', montantForfait: '',
  nombreJour: '', nombreCelebration: '1', joursAutorise: '',
  heurePersonnalise: false, isActive: true, typeDemandePublicId: '',
};

const optionalNumber = (value) => value === '' ? null : Number(value);

export default function PricingForm({ pricingId = null }) {
  const navigate = useNavigate();
  const { activeParish } = useTenant();
  const [form, setForm] = useState(INITIAL_VALUE);
  const [types, setTypes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

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
          setForm({
            codeForfait: existing.codeForfait || '',
            nomForfait: existing.nomForfait || '',
            libelle: existing.libelle || '',
            montantForfait: existing.montantForfait ?? '',
            nombreJour: existing.nombreJour ?? '',
            nombreCelebration: existing.nombreCelebration ?? '',
            joursAutorise: existing.joursAutorise ?? '',
            heurePersonnalise: Boolean(existing.heurePersonnalise),
            isActive: existing.isActive !== false,
            typeDemandePublicId: existing.typeDemandePublicId || '',
          });
        } else {
          setForm((current) => ({ ...current, typeDemandePublicId: availableTypes?.[0]?.publicId || '' }));
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

  const submit = async (event) => {
    event.preventDefault();
    try {
      setLoading(true);
      setError(null);
      const payload = {
        ...form,
        montantForfait: Number(form.montantForfait),
        nombreJour: optionalNumber(form.nombreJour),
        nombreCelebration: optionalNumber(form.nombreCelebration),
        joursAutorise: optionalNumber(form.joursAutorise),
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
      {error ? <p className="text-red-600">{error}</p> : null}
      {!types.length && !loading ? <p className="muted">Créez d’abord un type de demande actif.</p> : null}
      <form onSubmit={submit}>
        <div className="form-grid">
          <div className="form-field"><label htmlFor="pricing-code">Code *</label>
            <AppInput id="pricing-code" required value={form.codeForfait} onChange={(e) => field('codeForfait', e.target.value)} /></div>
          <div className="form-field"><label htmlFor="pricing-name">Nom *</label>
            <AppInput id="pricing-name" required value={form.nomForfait} onChange={(e) => field('nomForfait', e.target.value)} /></div>
          <div className="form-field"><label htmlFor="pricing-label">Libellé</label>
            <AppInput id="pricing-label" value={form.libelle} onChange={(e) => field('libelle', e.target.value)} /></div>
          <div className="form-field"><label htmlFor="pricing-amount">Montant *</label>
            <AppInput id="pricing-amount" type="number" min="1" step="0.01" required value={form.montantForfait} onChange={(e) => field('montantForfait', e.target.value)} /></div>
          <div className="form-field"><label htmlFor="pricing-type">Type de demande *</label>
            <select id="pricing-type" className="select" required value={form.typeDemandePublicId} onChange={(e) => field('typeDemandePublicId', e.target.value)}>
              {types.map((type) => <option key={type.publicId} value={type.publicId}>{type.libelle}</option>)}
            </select></div>
          <div className="form-field"><label htmlFor="pricing-celebrations">Nombre de célébrations</label>
            <AppInput id="pricing-celebrations" type="number" min="0" value={form.nombreCelebration} onChange={(e) => field('nombreCelebration', e.target.value)} /></div>
          <div className="form-field"><label htmlFor="pricing-days">Nombre de jours</label>
            <AppInput id="pricing-days" type="number" min="0" value={form.nombreJour} onChange={(e) => field('nombreJour', e.target.value)} /></div>
          <div className="form-field"><label htmlFor="pricing-allowed-days">Jours autorisés</label>
            <AppInput id="pricing-allowed-days" type="number" min="0" value={form.joursAutorise} onChange={(e) => field('joursAutorise', e.target.value)} /></div>
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
