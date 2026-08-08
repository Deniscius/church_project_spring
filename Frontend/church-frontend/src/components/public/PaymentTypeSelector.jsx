import React, { useContext, useMemo } from 'react';
import AppCard from '../ui/AppCard';
import AppSelect from '../ui/AppSelect';
import { FieldLabel } from '../ui/HelpTip';
import { PublicDemandeDraftContext } from '../../contexts/publicDemandeDraft.context';
import { useTypePaiementsPublicQuery } from '../../hooks/queries/usePublicReferentiel';
import { HELP } from '../../constants/helpTips';

const ONLINE_MODES = new Set(['TMONEY', 'FLOOZ', 'CARTE']);

/**
 * Sélecteur de mode de paiement.
 * - Mode brouillon : lit/écrit le draft public (si provider présent).
 * - Mode contrôlé : value / onChange pour la page paiement.
 * - onlineOnly : exclut le paiement au comptant (parcours suivi / paiement en ligne).
 */
export default function PaymentTypeSelector({
  value,
  onChange,
  parishName,
  onlineOnly = false,
  title = 'Type de paiement',
  subtitle = 'Vous pourrez encore changer de mode plus tard, tant que la demande n’est pas payée.',
}) {
  const draftCtx = useContext(PublicDemandeDraftContext);
  const controlled = typeof onChange === 'function';
  const { data: types = [], isLoading, error } = useTypePaiementsPublicQuery();

  const list = useMemo(() => {
    const raw = Array.isArray(types) ? types : [];
    if (!onlineOnly) return raw;
    return raw.filter((t) => t?.mode && ONLINE_MODES.has(String(t.mode).toUpperCase()));
  }, [types, onlineOnly]);

  const options = useMemo(
    () => list.map((t) => ({
      value: t.publicId,
      label: t.mode ? `${t.libelle} — ${t.mode}` : t.libelle,
    })),
    [list]
  );

  const selectedId = controlled ? (value || '') : (draftCtx?.draft?.typePaiementPublicId || '');
  const paroisseNom = parishName || draftCtx?.draft?.paroisseNom || '';

  const handleChange = (id) => {
    const t = list.find((x) => x.publicId === id) || types.find((x) => x.publicId === id);
    const libelle = t
      ? t.mode === 'ESPECES'
        ? `Au comptant à ${paroisseNom || 'la paroisse'}`
        : `${t.libelle}${t.mode ? ` (${t.mode})` : ''}`
      : '';

    if (controlled) {
      onChange({ publicId: id, libelle, mode: t?.mode || null, raw: t || null });
      return;
    }
    draftCtx?.dispatch?.({
      type: 'SELECT_PAIEMENT',
      payload: { publicId: id, libelle },
    });
  };

  return (
    <AppCard title={title} subtitle={subtitle}>
      {error ? <p className="text-red-600">{error.message}</p> : null}
      {isLoading ? <p className="muted">Chargement…</p> : null}
      {!isLoading && onlineOnly && options.length === 0 ? (
        <p className="text-red-600">
          Aucun mode de paiement en ligne n’est disponible pour le moment.
        </p>
      ) : null}
      <div className="form-field">
        <FieldLabel htmlFor="public-type-paiement" help={HELP.demande.paiement} required>
          Mode
        </FieldLabel>
        <AppSelect
          id="public-type-paiement"
          name="typePaiementPublicId"
          required
          disabled={isLoading}
          placeholder="— Choisir —"
          value={options.some((t) => t.value === selectedId) ? selectedId : ''}
          options={options}
          onChange={handleChange}
        />
        <small className="muted">
          {onlineOnly
            ? 'Sur le suivi en ligne, seuls TMoney, Flooz et carte sont proposés.'
            : 'Le paiement au comptant sera effectué dans la paroisse sélectionnée.'}
        </small>
      </div>
    </AppCard>
  );
}
