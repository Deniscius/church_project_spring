import React, { useEffect, useMemo, useState } from 'react';
import PageHeader from '../../../components/ui/PageHeader';
import AppCard from '../../../components/ui/AppCard';
import AppBadge from '../../../components/ui/AppBadge';
import { comptabiliteService } from '../../../services/inscription.service';
import { formatCurrency } from '../../../utils/formatCurrency';
import { usePermissions } from '../../../hooks/usePermissions';
import { PERMISSIONS } from '../../../constants/roles';

const FILTERS = [
  { id: 'EN_ATTENTE', label: 'À virer' },
  { id: 'PAYE', label: 'Virements exécutés' },
  { id: 'REJETE', label: 'Rejetés' },
  { id: 'ALL', label: 'Tous' },
];

const STATUS_LABELS = {
  EN_ATTENTE: 'En attente de virement',
  PAYE: 'Virement exécuté',
  REJETE: 'Rejeté',
};

function formatDateTime(value) {
  if (!value) return '—';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  return date.toLocaleString('fr-FR', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export default function ReversementsPage() {
  const { has } = usePermissions();
  const canExecute = has(PERMISSIONS.FINANCE_MANAGE);
  const [rows, setRows] = useState([]);
  const [filter, setFilter] = useState('EN_ATTENTE');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [info, setInfo] = useState(null);
  const [busyId, setBusyId] = useState(null);
  // Décision en cours de saisie : { id, action: 'PAYER' | 'REJETER', value }
  const [decision, setDecision] = useState(null);

  async function load() {
    try {
      setError(null);
      setLoading(true);
      const data = await comptabiliteService.listReversements();
      setRows(Array.isArray(data) ? data : []);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Erreur de chargement');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, []);

  const counts = useMemo(() => ({
    ALL: rows.length,
    EN_ATTENTE: rows.filter((r) => r.statut === 'EN_ATTENTE').length,
    PAYE: rows.filter((r) => r.statut === 'PAYE').length,
    REJETE: rows.filter((r) => r.statut === 'REJETE').length,
  }), [rows]);

  const totals = useMemo(() => {
    const sum = (statut) => rows
      .filter((r) => r.statut === statut)
      .reduce((total, r) => total + (r.montant || 0), 0);
    return { pending: sum('EN_ATTENTE'), paid: sum('PAYE') };
  }, [rows]);

  const visible = useMemo(
    () => (filter === 'ALL' ? rows : rows.filter((r) => r.statut === filter)),
    [rows, filter]
  );

  function startDecision(row, action) {
    setInfo(null);
    setError(null);
    setDecision({ id: row.publicId, action, value: '' });
  }

  async function confirmDecision(row) {
    if (!decision) return;
    const { action, value } = decision;
    if (action === 'PAYER' && !value.trim()) {
      setError('La référence du virement est obligatoire pour justifier le décaissement.');
      return;
    }

    setBusyId(row.publicId);
    setError(null);
    try {
      if (action === 'PAYER') {
        await comptabiliteService.payerReversement(row.publicId, {
          referenceVirement: value.trim(),
        });
        setInfo(`Virement de ${formatCurrency(row.montant)} enregistré pour ${row.paroisseNom}.`);
      } else {
        await comptabiliteService.rejeterReversement(row.publicId, { motif: value.trim() });
        setInfo(`Demande rejetée : le solde de ${row.paroisseNom} a été rétabli.`);
      }
      setDecision(null);
      await load();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Échec de l’opération');
    } finally {
      setBusyId(null);
    }
  }

  return (
    <div className="stack">
      <PageHeader
        title="Reversements"
        subtitle="Les intentions payées créditent le solde de chaque paroisse. La paroisse demande son virement, le comptable l’exécute en banque puis le confirme ici."
        actions={(
          <button type="button" className="btn btn-secondary" onClick={load} disabled={loading}>
            {loading ? 'Actualisation…' : 'Actualiser'}
          </button>
        )}
      />

      {error ? <div className="alert-error" role="alert">{error}</div> : null}
      {info ? <div className="alert-success" role="status">{info}</div> : null}
      {!canExecute ? (
        <div className="alert-info" role="status">
          Consultation seule : l’exécution des virements relève du comptable plateforme.
        </div>
      ) : null}

      <div className="card-grid">
        <AppCard title="À virer">
          <div className="kpi kpi-money">
            <strong>{formatCurrency(totals.pending)}</strong>
            <span className="page-subtitle">
              {counts.EN_ATTENTE} demande{counts.EN_ATTENTE > 1 ? 's' : ''} en attente
            </span>
          </div>
        </AppCard>
        <AppCard title="Déjà reversé">
          <div className="kpi kpi-money">
            <strong>{formatCurrency(totals.paid)}</strong>
            <span className="page-subtitle">
              {counts.PAYE} virement{counts.PAYE > 1 ? 's' : ''} exécuté{counts.PAYE > 1 ? 's' : ''}
            </span>
          </div>
        </AppCard>
        <AppCard title="Rejetés">
          <div className="kpi kpi-money">
            <strong>{counts.REJETE}</strong>
            <span className="page-subtitle">Solde rendu à la paroisse</span>
          </div>
        </AppCard>
      </div>

      <div className="filter-chips" role="group" aria-label="Statut des reversements">
        {FILTERS.map((f) => (
          <button
            key={f.id}
            type="button"
            className={`chip${filter === f.id ? ' is-active' : ''}`}
            aria-pressed={filter === f.id}
            onClick={() => setFilter(f.id)}
          >
            {f.label} ({counts[f.id]})
          </button>
        ))}
      </div>

      {loading ? <p className="muted">Chargement…</p> : null}

      {!loading && rows.length === 0 ? (
        <div className="empty-state" role="status">
          <h3>Aucune demande de reversement</h3>
          <p>
            Rien à traiter pour le moment. Une demande apparaît ici dès qu’une paroisse
            réclame le virement de son solde, depuis son écran Trésorerie. Elle doit au
            préalable avoir renseigné son RIB sur sa fiche paroisse.
          </p>
        </div>
      ) : null}

      {!loading && rows.length > 0 && visible.length === 0 ? (
        <div className="empty-state" role="status">
          <h3>Rien dans ce filtre</h3>
          <p>Aucune demande avec le statut « {FILTERS.find((f) => f.id === filter)?.label} ».</p>
        </div>
      ) : null}

      {visible.length > 0 ? (
        <div className="card table-card" role="region" aria-label="Demandes de reversement" tabIndex="0">
          <table className="app-table">
            <thead>
              <tr>
                <th>Paroisse</th>
                <th>Demandé le</th>
                <th>Montant</th>
                <th>Compte à créditer</th>
                <th>Statut</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {visible.map((r) => {
                const editing = decision?.id === r.publicId;
                return (
                  <React.Fragment key={r.publicId}>
                    <tr>
                      <td data-label="Paroisse">
                        <div className="cell-stack">
                          <strong>{r.paroisseNom}</strong>
                          {r.motif ? <span className="muted">{r.motif}</span> : null}
                        </div>
                      </td>
                      <td data-label="Demandé le">{formatDateTime(r.createdAt)}</td>
                      <td data-label="Montant"><strong>{formatCurrency(r.montant)}</strong></td>
                      <td data-label="Compte à créditer">
                        <div className="cell-stack">
                          <span>{r.titulaireCompte || '—'}</span>
                          <span className="muted">{r.ibanOrRib || 'RIB manquant'}</span>
                          {r.nomBanque ? <span className="muted">{r.nomBanque}</span> : null}
                        </div>
                      </td>
                      <td data-label="Statut">
                        <div className="cell-stack">
                          <AppBadge value={r.statut} label={STATUS_LABELS[r.statut] || r.statut} />
                          {r.referenceVirement ? (
                            <span className="muted">Réf. {r.referenceVirement}</span>
                          ) : null}
                          {r.traiteAt ? (
                            <span className="muted">Traité le {formatDateTime(r.traiteAt)}</span>
                          ) : null}
                        </div>
                      </td>
                      <td data-label="Actions" className="button-row">
                        {canExecute && r.statut === 'EN_ATTENTE' && !editing ? (
                          <>
                            <button
                              type="button"
                              className="btn btn-primary"
                              disabled={busyId === r.publicId}
                              onClick={() => startDecision(r, 'PAYER')}
                            >
                              Confirmer le virement
                            </button>
                            <button
                              type="button"
                              className="btn btn-secondary"
                              disabled={busyId === r.publicId}
                              onClick={() => startDecision(r, 'REJETER')}
                            >
                              Rejeter
                            </button>
                          </>
                        ) : null}
                      </td>
                    </tr>

                    {editing ? (
                      <tr className="row-form">
                        <td colSpan={6}>
                          <div className="decision-form">
                            <label htmlFor={`decision-${r.publicId}`}>
                              {decision.action === 'PAYER'
                                ? `Référence du virement bancaire de ${formatCurrency(r.montant)} vers ${r.titulaireCompte || r.paroisseNom}`
                                : 'Motif du rejet (le solde sera rendu à la paroisse)'}
                            </label>
                            <div className="decision-form-row">
                              <input
                                id={`decision-${r.publicId}`}
                                className="input"
                                autoFocus
                                value={decision.value}
                                maxLength={200}
                                placeholder={
                                  decision.action === 'PAYER'
                                    ? 'Ex. VIR-2026-0148'
                                    : 'Ex. RIB erroné'
                                }
                                onChange={(e) => setDecision({ ...decision, value: e.target.value })}
                              />
                              <button
                                type="button"
                                className="btn btn-primary"
                                disabled={busyId === r.publicId}
                                onClick={() => confirmDecision(r)}
                              >
                                {busyId === r.publicId ? 'Enregistrement…' : 'Valider'}
                              </button>
                              <button
                                type="button"
                                className="btn btn-secondary"
                                disabled={busyId === r.publicId}
                                onClick={() => setDecision(null)}
                              >
                                Annuler
                              </button>
                            </div>
                          </div>
                        </td>
                      </tr>
                    ) : null}
                  </React.Fragment>
                );
              })}
            </tbody>
          </table>
        </div>
      ) : null}
    </div>
  );
}
