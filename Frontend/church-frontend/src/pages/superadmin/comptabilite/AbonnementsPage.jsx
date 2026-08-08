import React, { useEffect, useMemo, useState } from 'react';
import PageHeader from '../../../components/ui/PageHeader';
import AppCard from '../../../components/ui/AppCard';
import AppBadge from '../../../components/ui/AppBadge';
import AppDialog from '../../../components/ui/AppDialog';
import { comptabiliteService } from '../../../services/inscription.service';
import { formatCurrency } from '../../../utils/formatCurrency';
import { usePermissions } from '../../../hooks/usePermissions';
import { PERMISSIONS } from '../../../constants/roles';

const STATUS_LABELS = {
  EN_ATTENTE: 'Paiement attendu',
  ACTIF: 'Actif',
  EXPIRE: 'Échu',
  ANNULE: 'Annulé',
};

const PLAN_LABELS = {
  MENSUEL: 'Mensuel',
  SEMESTRIEL: 'Semestriel',
  ANNUEL: 'Annuel',
};

const PLAN_OPTIONS = [
  { value: 'MENSUEL', label: 'Mensuel — 5 000 FCFA', price: 5000 },
  { value: 'SEMESTRIEL', label: 'Semestriel — 8 000 FCFA', price: 8000 },
  { value: 'ANNUEL', label: 'Annuel — 12 000 FCFA', price: 12000 },
];

const PROLONG_OPTIONS = [
  { value: 7, label: '7 jours' },
  { value: 15, label: '15 jours' },
  { value: 30, label: '30 jours' },
  { value: 90, label: '90 jours' },
];

const SOURCE_LABELS = {
  FEDAPAY: 'Agrégateur FedaPay',
  MANUEL: 'Activation manuelle',
  PROLONGATION: 'Prolongation gracieuse',
};

const FILTERS = [
  { id: 'A_RELANCER', label: 'À relancer', match: (r) => r.statut === 'EXPIRE' || r.enTolerance },
  { id: 'PROCHE', label: 'Échéance proche', match: (r) => r.echeanceProche && r.statut === 'ACTIF' },
  { id: 'EN_ATTENTE', label: 'Paiement attendu', match: (r) => r.statut === 'EN_ATTENTE' },
  { id: 'ACTIF', label: 'À jour', match: (r) => r.statut === 'ACTIF' && !r.echeanceProche },
  { id: 'ALL', label: 'Tous', match: () => true },
];

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

function formatDate(value) {
  if (!value) return '—';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  return date.toLocaleDateString('fr-FR', { day: '2-digit', month: 'short', year: 'numeric' });
}

function echeanceLabel(row) {
  if (row.finAt == null || row.joursRestants == null) return 'Non démarré';
  if (row.joursRestants < 0) {
    const retard = Math.abs(row.joursRestants);
    return `Échu depuis ${retard} jour${retard > 1 ? 's' : ''}`;
  }
  if (row.joursRestants === 0) return 'Échoit aujourd’hui';
  return `Dans ${row.joursRestants} jour${row.joursRestants > 1 ? 's' : ''}`;
}

export default function AbonnementsPage() {
  const { has } = usePermissions();
  const canExecute = has(PERMISSIONS.FINANCE_MANAGE);
  const [rows, setRows] = useState([]);
  const [filter, setFilter] = useState('A_RELANCER');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [info, setInfo] = useState(null);
  const [busyId, setBusyId] = useState(null);
  const [plansChoisis, setPlansChoisis] = useState({});
  const [prolongJours, setProlongJours] = useState({});
  const [confirmRow, setConfirmRow] = useState(null);
  const [confirmMode, setConfirmMode] = useState(null); // checkout | manuel | prolonger | annuler | resilier

  async function load() {
    try {
      setError(null);
      setLoading(true);
      const data = await comptabiliteService.listAbonnements();
      const list = Array.isArray(data) ? data : [];
      setRows(list);
      setPlansChoisis((prev) => {
        const next = { ...prev };
        list.forEach((r) => {
          if (!next[r.publicId]) next[r.publicId] = r.plan || 'MENSUEL';
        });
        return next;
      });
      setProlongJours((prev) => {
        const next = { ...prev };
        list.forEach((r) => {
          if (!next[r.publicId]) next[r.publicId] = 30;
        });
        return next;
      });
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Erreur de chargement');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, []);

  const counts = useMemo(() => {
    const base = {};
    FILTERS.forEach((f) => {
      base[f.id] = rows.filter(f.match).length;
    });
    return base;
  }, [rows]);

  const recurrent = useMemo(
    () => rows
      .filter((r) => r.statut === 'ACTIF')
      .reduce((total, r) => total + (r.montant || 0), 0),
    [rows]
  );

  const visible = useMemo(() => {
    const active = FILTERS.find((f) => f.id === filter) || FILTERS[FILTERS.length - 1];
    return [...rows].filter(active.match).sort((a, b) => {
      if (a.joursRestants == null) return 1;
      if (b.joursRestants == null) return -1;
      return a.joursRestants - b.joursRestants;
    });
  }, [rows, filter]);

  const planFor = (row) => plansChoisis[row.publicId] || row.plan || 'MENSUEL';
  const joursFor = (row) => prolongJours[row.publicId] || 30;

  async function runAction(row, action) {
    setBusyId(row.publicId);
    setError(null);
    setInfo(null);
    setConfirmRow(null);
    setConfirmMode(null);
    try {
      let res;
      if (action === 'checkout') {
        res = await comptabiliteService.checkoutAbonnement(row.paroissePublicId, planFor(row));
        if (res?.paymentUrl) {
          window.open(res.paymentUrl, '_blank', 'noopener,noreferrer');
          setInfo(`Lien de paiement FedaPay ouvert pour « ${row.paroisseNom} » (${PLAN_LABELS[planFor(row)] || planFor(row)}).`);
        } else {
          setInfo(res?.message || 'Agrégateur indisponible — utilisez l’activation manuelle si le paiement est déjà reçu.');
        }
      } else if (action === 'manuel') {
        res = await comptabiliteService.activerAbonnement(row.paroissePublicId, planFor(row));
        setInfo(res?.message || 'Abonnement activé.');
      } else if (action === 'prolonger') {
        res = await comptabiliteService.prolongerAbonnement(row.paroissePublicId, joursFor(row));
        setInfo(res?.message || 'Abonnement prolongé.');
      } else if (action === 'annuler') {
        res = await comptabiliteService.annulerAbonnementPending(row.publicId);
        setInfo(res?.message || 'Paiement annulé.');
      } else if (action === 'resilier') {
        res = await comptabiliteService.resilierAbonnement(row.paroissePublicId);
        setInfo(res?.message || 'Paroisse résiliée.');
      }
      await load();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Échec de l’opération');
    } finally {
      setBusyId(null);
    }
  }

  const confirmPlan = confirmRow ? planFor(confirmRow) : null;
  const confirmOption = PLAN_OPTIONS.find((p) => p.value === confirmPlan);

  const dialogTitle = {
    checkout: 'Paiement via agrégateur',
    manuel: 'Activation manuelle',
    prolonger: 'Prolongation gracieuse',
    annuler: 'Annuler le paiement attendu',
    resilier: 'Résilier la paroisse',
  }[confirmMode] || 'Confirmation';

  const dialogConfirm = {
    checkout: 'Ouvrir FedaPay',
    manuel: 'Confirmer l’activation',
    prolonger: 'Prolonger',
    annuler: 'Annuler le paiement',
    resilier: 'Confirmer la résiliation',
  }[confirmMode] || 'Confirmer';

  return (
    <div className="stack">
      <PageHeader
        title="Abonnements SaaS"
        subtitle="Une ligne par paroisse : payer, activer, prolonger, annuler un lien abandonné ou résilier."
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
          Consultation seule : le paiement et l’activation relèvent du comptable plateforme.
        </div>
      ) : (
        <div className="alert-info" role="status">
          Circuit normal : lien FedaPay. Prolongation = geste commercial sans facture.
          Résiliation coupe l’accès sans effacer l’historique.
        </div>
      )}

      <div className="card-grid">
        <AppCard title="À relancer">
          <div className="kpi">
            <strong>{counts.A_RELANCER}</strong>
            <span className="page-subtitle">Échéance dépassée</span>
          </div>
        </AppCard>
        <AppCard title="Échéance proche">
          <div className="kpi">
            <strong>{counts.PROCHE}</strong>
            <span className="page-subtitle">À renouveler prochainement</span>
          </div>
        </AppCard>
        <AppCard title="Paiement attendu">
          <div className="kpi">
            <strong>{counts.EN_ATTENTE}</strong>
            <span className="page-subtitle">Jamais activés</span>
          </div>
        </AppCard>
        <AppCard title="Revenu par période">
          <div className="kpi kpi-money">
            <strong>{formatCurrency(recurrent)}</strong>
            <span className="page-subtitle">Somme des abonnements actifs</span>
          </div>
        </AppCard>
      </div>

      <div className="filter-chips" role="group" aria-label="État des abonnements">
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
          <h3>Aucun abonnement</h3>
          <p>Les abonnements apparaissent ici dès qu’une inscription de paroisse est approuvée.</p>
        </div>
      ) : null}

      {!loading && rows.length > 0 && visible.length === 0 ? (
        <div className="empty-state" role="status">
          <h3>Rien dans ce filtre</h3>
          <p>Aucune paroisse avec l’état « {FILTERS.find((f) => f.id === filter)?.label} ».</p>
        </div>
      ) : null}

      {visible.length > 0 ? (
        <div className="card table-card" role="region" aria-label="Abonnements des paroisses" tabIndex="0">
          <table className="app-table">
            <thead>
              <tr>
                <th>Paroisse</th>
                <th>Plan</th>
                <th>Activation</th>
                <th>Période</th>
                <th>Échéance</th>
                <th>État</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {visible.map((r) => (
                <tr key={r.publicId}>
                  <td data-label="Paroisse">
                    <div className="cell-stack">
                      <strong>{r.paroisseNom}</strong>
                      <span className="muted">{r.doyenneNom || '—'}</span>
                      <span className="muted">
                        {[r.paroisseEmail, r.paroisseTelephone].filter(Boolean).join(' · ') || 'Pas de contact'}
                      </span>
                    </div>
                  </td>
                  <td data-label="Plan">
                    <div className="cell-stack">
                      <span>{PLAN_LABELS[r.plan] || r.plan}</span>
                      <span className="muted">{formatCurrency(r.montant || 0)}</span>
                      {canExecute && r.statut !== 'ANNULE' ? (
                        <label className="muted" style={{ display: 'grid', gap: 4, marginTop: 6 }}>
                          <span>Formule</span>
                          <select
                            className="select"
                            value={planFor(r)}
                            disabled={busyId === r.publicId}
                            onChange={(e) => setPlansChoisis((prev) => ({
                              ...prev,
                              [r.publicId]: e.target.value,
                            }))}
                          >
                            {PLAN_OPTIONS.map((p) => (
                              <option key={p.value} value={p.value}>{p.label}</option>
                            ))}
                          </select>
                        </label>
                      ) : null}
                      {canExecute && (r.statut === 'ACTIF' || r.statut === 'EXPIRE' || r.enTolerance) ? (
                        <label className="muted" style={{ display: 'grid', gap: 4, marginTop: 6 }}>
                          <span>Prolonger</span>
                          <select
                            className="select"
                            value={joursFor(r)}
                            disabled={busyId === r.publicId}
                            onChange={(e) => setProlongJours((prev) => ({
                              ...prev,
                              [r.publicId]: Number(e.target.value),
                            }))}
                          >
                            {PROLONG_OPTIONS.map((p) => (
                              <option key={p.value} value={p.value}>{p.label}</option>
                            ))}
                          </select>
                        </label>
                      ) : null}
                    </div>
                  </td>
                  <td data-label="Activation">
                    <div className="cell-stack">
                      <span>{formatDateTime(r.activatedAt || (r.statut === 'ACTIF' ? r.debutAt : null))}</span>
                      <span className="muted">
                        {SOURCE_LABELS[r.activationSource] || (r.statut === 'EN_ATTENTE' ? 'En attente' : '—')}
                      </span>
                      {r.activatedByNom ? (
                        <span className="muted">Par {r.activatedByNom}</span>
                      ) : null}
                    </div>
                  </td>
                  <td data-label="Période">
                    <div className="cell-stack">
                      <span>{formatDate(r.debutAt)}</span>
                      <span className="muted">au {formatDate(r.finAt)}</span>
                    </div>
                  </td>
                  <td data-label="Échéance">
                    <div className="cell-stack">
                      <span>{echeanceLabel(r)}</span>
                      {r.enTolerance ? (
                        <span className="muted">Tolérance en cours — accès encore ouvert</span>
                      ) : null}
                      {!r.paroisseActive && r.statut !== 'EN_ATTENTE' ? (
                        <span className="muted">Accès suspendu</span>
                      ) : null}
                    </div>
                  </td>
                  <td data-label="État">
                    <AppBadge value={r.statut} label={STATUS_LABELS[r.statut] || r.statut} />
                  </td>
                  <td data-label="Actions" className="button-row">
                    {canExecute && r.statut !== 'ANNULE' ? (
                      <>
                        <button
                          type="button"
                          className="btn btn-primary"
                          disabled={busyId === r.publicId || !r.paroissePublicId}
                          onClick={() => { setConfirmMode('checkout'); setConfirmRow(r); }}
                        >
                          {busyId === r.publicId ? 'Traitement…' : 'Payer (FedaPay)'}
                        </button>
                        <button
                          type="button"
                          className="btn btn-secondary"
                          disabled={busyId === r.publicId || !r.paroissePublicId}
                          onClick={() => { setConfirmMode('manuel'); setConfirmRow(r); }}
                        >
                          Activer
                        </button>
                        {(r.statut === 'ACTIF' || r.statut === 'EXPIRE' || r.enTolerance) ? (
                          <button
                            type="button"
                            className="btn btn-secondary"
                            disabled={busyId === r.publicId || !r.paroissePublicId}
                            onClick={() => { setConfirmMode('prolonger'); setConfirmRow(r); }}
                          >
                            Prolonger
                          </button>
                        ) : null}
                        {r.statut === 'EN_ATTENTE' ? (
                          <button
                            type="button"
                            className="btn btn-secondary"
                            disabled={busyId === r.publicId}
                            onClick={() => { setConfirmMode('annuler'); setConfirmRow(r); }}
                          >
                            Annuler lien
                          </button>
                        ) : null}
                        {r.statut !== 'EN_ATTENTE' ? (
                          <button
                            type="button"
                            className="btn btn-secondary"
                            disabled={busyId === r.publicId || !r.paroissePublicId}
                            onClick={() => { setConfirmMode('resilier'); setConfirmRow(r); }}
                          >
                            Résilier
                          </button>
                        ) : null}
                      </>
                    ) : null}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}

      <AppDialog
        open={Boolean(confirmRow)}
        title={dialogTitle}
        confirmLabel={dialogConfirm}
        cancelLabel="Retour"
        busy={Boolean(busyId)}
        onCancel={() => { setConfirmRow(null); setConfirmMode(null); }}
        onConfirm={() => {
          if (!confirmRow || !confirmMode) return;
          runAction(confirmRow, confirmMode);
        }}
      >
        {confirmRow ? (
          <p style={{ margin: 0 }}>
            {confirmMode === 'checkout' ? (
              <>Générer le lien FedaPay pour « {confirmRow.paroisseNom} » — {PLAN_LABELS[confirmPlan] || confirmPlan}
                {confirmOption ? ` (${formatCurrency(confirmOption.price)}).` : '.'}
              </>
            ) : null}
            {confirmMode === 'manuel' ? (
              <>
                Confirmer l’activation manuelle de « {confirmRow.paroisseNom} »
                {confirmOption ? ` (${formatCurrency(confirmOption.price)}).` : '.'}
                {' '}Réservé au cas où le paiement a déjà été reçu hors agrégateur.
              </>
            ) : null}
            {confirmMode === 'prolonger' ? (
              <>
                Prolonger « {confirmRow.paroisseNom} » de {joursFor(confirmRow)} jour(s)
                sans nouveau paiement. L’échéance part de la date actuelle ou des jours restants.
              </>
            ) : null}
            {confirmMode === 'annuler' ? (
              <>Annuler le paiement attendu pour « {confirmRow.paroisseNom} » (lien FedaPay abandonné).</>
            ) : null}
            {confirmMode === 'resilier' ? (
              <>
                Résilier « {confirmRow.paroisseNom} » : l’accès SaaS sera coupé.
                L’historique (demandes, comptes) est conservé.
              </>
            ) : null}
          </p>
        ) : null}
      </AppDialog>
    </div>
  );
}
