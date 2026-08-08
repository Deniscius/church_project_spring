import React, { useEffect, useMemo, useState } from 'react';
import PageHeader from '../../../components/ui/PageHeader';
import AppBadge from '../../../components/ui/AppBadge';
import AppDialog from '../../../components/ui/AppDialog';
import AppTable from '../../../components/ui/AppTable';
import StepHelpBanner from '../../../components/ui/StepHelpBanner';
import { useTenant } from '../../../hooks/useTenant';
import { comptabiliteService } from '../../../services/inscription.service';
import { parishService } from '../../../services/parish.service';
import { paymentService } from '../../../services/payment.service';
import { formatCurrency } from '../../../utils/formatCurrency';
import { usePermissions } from '../../../hooks/usePermissions';
import { useAuth } from '../../../hooks/useAuth';
import { PERMISSIONS } from '../../../constants/roles';
import { HELP } from '../../../constants/helpTips';

const EMPTY_BANK = { nomBanque: '', titulaireCompte: '', ibanOrRib: '', email: '', telephone: '' };

const STATUS_FILTERS = [
  { id: 'ALL', label: 'Tous' },
  { id: 'EN_ATTENTE', label: 'En attente' },
  { id: 'PAYE', label: 'Payés' },
  { id: 'REJETE', label: 'Rejetés' },
];

const CAISSE_PAGE_SIZE = 15;
const REVERSEMENT_PAGE_SIZE = 12;

const CAISSE_COLUMNS = [
  { key: 'dateEncaissement', label: 'Date / heure', sortable: true },
  { key: 'encaisseurNom', label: 'Encaisseur', sortable: true },
  { key: 'codeSuivie', label: 'Code', sortable: true },
  { key: 'fidele', label: 'Fidèle', sortable: true },
  { key: 'intention', label: 'Intention', sortable: true },
  { key: 'montant', label: 'Montant', sortable: true, sortValue: (row) => Number(row.montant) || 0 },
];

const REVERSEMENT_COLUMNS = [
  { key: 'createdAt', label: 'Date', sortable: true },
  { key: 'montant', label: 'Montant', sortable: true, sortValue: (row) => Number(row.montant) || 0 },
  { key: 'statut', label: 'Statut', sortable: true },
  { key: 'motif', label: 'Motif', sortable: true },
  { key: 'referenceVirement', label: 'Référence', sortable: true },
];

function formatDate(value) {
  if (!value) return '—';
  try {
    return new Date(value).toLocaleString('fr-FR', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  } catch {
    return String(value);
  }
}

export default function ParishTreasuryPage() {
  const { activeParish } = useTenant();
  const { user } = useAuth();
  const { has } = usePermissions();
  const paroisseId = activeParish?.publicId || activeParish?.id;
  const canManageCashOps = has(PERMISSIONS.PAYMENT_MANAGE);
  const isLocalAccountant = user?.role === 'COMPTABLE_LOCAL';
  const canEditBank = has(PERMISSIONS.USER_MANAGE) || user?.role === 'ADMIN';
  const canRequestReversement = user?.role === 'ADMIN';
  const [compte, setCompte] = useState(null);
  const [rows, setRows] = useState([]);
  const [filter, setFilter] = useState('ALL');
  const [montant, setMontant] = useState('');
  const [motif, setMotif] = useState('');
  const [error, setError] = useState(null);
  const [info, setInfo] = useState(null);
  const [busy, setBusy] = useState(false);
  const [loading, setLoading] = useState(true);
  const [bank, setBank] = useState(EMPTY_BANK);
  const [bankDraft, setBankDraft] = useState(EMPTY_BANK);
  const [savingBank, setSavingBank] = useState(false);
  const [bankModalOpen, setBankModalOpen] = useState(false);
  const [reversementModalOpen, setReversementModalOpen] = useState(false);
  const [caisse, setCaisse] = useState(null);
  const [caissePage, setCaissePage] = useState(0);
  const [caisseSearch, setCaisseSearch] = useState('');
  const [reversementPage, setReversementPage] = useState(0);
  const [reversementSearch, setReversementSearch] = useState('');

  async function load() {
    if (!paroisseId) return;
    try {
      setError(null);
      setLoading(true);
      const [c, r, p, cash] = await Promise.all([
        comptabiliteService.getCompte(paroisseId),
        comptabiliteService.listReversementsParoisse(paroisseId),
        parishService.getById(paroisseId),
        paymentService.resumeCaisse(paroisseId),
      ]);
      setCompte(c);
      setRows(Array.isArray(r) ? r : []);
      setCaisse(cash || null);
      const nextBank = {
        nomBanque: p?.nomBanque || '',
        titulaireCompte: p?.titulaireCompte || '',
        ibanOrRib: p?.ibanOrRib || '',
        email: p?.email || '',
        telephone: p?.telephone || '',
      };
      setBank(nextBank);
      setBankDraft(nextBank);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Erreur');
    } finally {
      setLoading(false);
    }
  }

  async function saveBank() {
    if (!paroisseId) return;
    setSavingBank(true);
    setError(null);
    setInfo(null);
    try {
      await parishService.updateCoordonnees(paroisseId, bankDraft);
      setBank(bankDraft);
      setBankModalOpen(false);
      setInfo('Coordonnées bancaires enregistrées.');
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Enregistrement impossible');
    } finally {
      setSavingBank(false);
    }
  }

  useEffect(() => {
    load();
  }, [paroisseId]);

  useEffect(() => {
    setCaissePage(0);
  }, [caisse?.encaissements?.length, paroisseId, caisseSearch]);

  useEffect(() => {
    setReversementPage(0);
  }, [filter, reversementSearch, paroisseId]);

  const available = compte?.soldeDisponible ?? 0;
  const pending = compte?.soldeEnAttente ?? 0;
  const totalBooked = available + pending;
  const hasRib = Boolean(bank.ibanOrRib);
  const canRequest = available > 0 && hasRib;

  const cashTotal = caisse?.total || 0;
  const cashToday = caisse?.totalDuJour || 0;
  const cashMonth = caisse?.totalDuMois || 0;
  const cashCount = caisse?.nombreEncaissements || 0;
  const cashLines = caisse?.encaissements || [];

  const filteredCash = useMemo(() => {
    const needle = caisseSearch.trim().toLowerCase();
    if (!needle) return cashLines;
    return cashLines.filter((line) => (
      [line.encaisseurNom, line.codeSuivie, line.fidele, line.intention, String(line.montant || '')]
        .filter(Boolean)
        .some((field) => String(field).toLowerCase().includes(needle))
    ));
  }, [cashLines, caisseSearch]);

  const cashPageCount = Math.max(1, Math.ceil(filteredCash.length / CAISSE_PAGE_SIZE));
  const safeCashPage = Math.min(caissePage, cashPageCount - 1);
  const pagedCash = filteredCash.slice(
    safeCashPage * CAISSE_PAGE_SIZE,
    (safeCashPage + 1) * CAISSE_PAGE_SIZE
  ).map((line) => ({ ...line, id: line.publicId }));

  const visible = useMemo(() => {
    const byStatus = filter === 'ALL' ? rows : rows.filter((r) => r.statut === filter);
    const needle = reversementSearch.trim().toLowerCase();
    if (!needle) return byStatus;
    return byStatus.filter((r) => (
      [r.motif, r.referenceVirement, r.statut, String(r.montant || '')]
        .filter(Boolean)
        .some((field) => String(field).toLowerCase().includes(needle))
    ));
  }, [rows, filter, reversementSearch]);

  const reversementPageCount = Math.max(1, Math.ceil(visible.length / REVERSEMENT_PAGE_SIZE));
  const safeReversementPage = Math.min(reversementPage, reversementPageCount - 1);
  const pagedReversements = visible.slice(
    safeReversementPage * REVERSEMENT_PAGE_SIZE,
    (safeReversementPage + 1) * REVERSEMENT_PAGE_SIZE
  ).map((r) => ({ ...r, id: r.publicId }));

  const counts = useMemo(() => ({
    ALL: rows.length,
    EN_ATTENTE: rows.filter((r) => r.statut === 'EN_ATTENTE').length,
    PAYE: rows.filter((r) => r.statut === 'PAYE').length,
    REJETE: rows.filter((r) => r.statut === 'REJETE').length,
  }), [rows]);

  async function onDemande() {
    if (!paroisseId) return;
    const amount = Number(montant);
    if (!Number.isFinite(amount) || amount < 100) {
      setError('Montant minimum : 100 FCFA');
      return;
    }
    if (amount > available) {
      setError('Le montant dépasse le solde disponible.');
      return;
    }

    setBusy(true);
    setError(null);
    setInfo(null);
    try {
      await comptabiliteService.demanderReversement({
        paroissePublicId: paroisseId,
        montant: amount,
        motif: motif.trim(),
      });
      setMontant('');
      setMotif('');
      setReversementModalOpen(false);
      setInfo('Demande de reversement enregistrée. L’équipe plateforme traitera le virement.');
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Échec demande');
    } finally {
      setBusy(false);
    }
  }

  function useMaxAvailable() {
    if (available > 0) setMontant(String(available));
  }

  return (
    <div className="stack treasury-page">
      <PageHeader
        title="Trésorerie"
        subtitle={
          activeParish?.name
            ? `Caisse espèces et solde en ligne — ${activeParish.name}`
            : 'Caisse espèces locale et solde en ligne à reverser.'
        }
      />

      <StepHelpBanner title="Deux caisses distinctes" text={HELP.admin.tresorerie} />

      {error ? <p className="alert-error">{error}</p> : null}
      {info ? <p className="alert-success">{info}</p> : null}
      {isLocalAccountant ? (
        <div className="alert-info" role="status">
          Contrôle comptable : consultation de la caisse et du solde en ligne.
        </div>
      ) : null}
      {canManageCashOps && !canRequestReversement ? (
        <div className="alert-info" role="status">
          Caisse locale : encaissement depuis les demandes ; journal ci-dessous pour le suivi.
        </div>
      ) : null}

      {!paroisseId ? (
        <p className="muted">Sélectionnez une paroisse active pour consulter la trésorerie.</p>
      ) : null}

      <section className="panel treasury-cash-panel">
        <div className="treasury-cash-hero">
          <div>
            <p className="treasury-kicker">Caisse locale · espèces</p>
            <p className="treasury-balance treasury-balance--cash">
              {loading ? '…' : formatCurrency(cashTotal)}
            </p>
            <p className="muted" style={{ margin: 0 }}>
              Total encaissé au secrétariat (hors reversement plateforme).
            </p>
          </div>
          <div className="treasury-cash-kpis">
            <div className="treasury-kpi">
              <span>Aujourd’hui</span>
              <strong>{loading ? '…' : formatCurrency(cashToday)}</strong>
            </div>
            <div className="treasury-kpi">
              <span>Ce mois</span>
              <strong>{loading ? '…' : formatCurrency(cashMonth)}</strong>
            </div>
            <div className="treasury-kpi">
              <span>Encaissements</span>
              <strong>{loading ? '…' : cashCount}</strong>
            </div>
          </div>
        </div>

        {!loading && cashLines.length > 0 ? (
          <div className="toolbar treasury-grid-toolbar">
            <input
              type="search"
              className="input"
              value={caisseSearch}
              onChange={(e) => setCaisseSearch(e.target.value)}
              placeholder="Rechercher code, fidèle, intention…"
              aria-label="Rechercher dans la caisse"
            />
            <span className="muted text-sm">
              {filteredCash.length} ligne{filteredCash.length > 1 ? 's' : ''}
            </span>
          </div>
        ) : null}

        {!loading ? (
          <AppTable
            columns={CAISSE_COLUMNS}
            rows={pagedCash}
            ariaLabel="Journal de caisse espèces"
            emptyMessage={
              cashLines.length
                ? 'Aucun encaissement ne correspond à cette recherche.'
                : 'Aucun encaissement espèces pour le moment.'
            }
            defaultSortKey="dateEncaissement"
            defaultSortDir="desc"
            renderCell={(row, column) => {
              switch (column.key) {
                case 'dateEncaissement':
                  return formatDate(row.dateEncaissement);
                case 'encaisseurNom':
                  return row.encaisseurNom || '—';
                case 'codeSuivie':
                  return <strong>{row.codeSuivie || '—'}</strong>;
                case 'fidele':
                  return row.fidele || '—';
                case 'intention':
                  return (
                    <span className="cell-truncate" title={row.intention || ''}>
                      {row.intention || '—'}
                    </span>
                  );
                case 'montant':
                  return <strong>{formatCurrency(row.montant || 0)}</strong>;
                default:
                  return row[column.key];
              }
            }}
          />
        ) : null}

        {!loading && filteredCash.length > CAISSE_PAGE_SIZE ? (
          <div className="button-row" style={{ justifyContent: 'space-between', marginTop: 12 }}>
            <span className="muted text-sm">
              {safeCashPage * CAISSE_PAGE_SIZE + 1}–{Math.min((safeCashPage + 1) * CAISSE_PAGE_SIZE, filteredCash.length)}
              {' '}sur {filteredCash.length}
            </span>
            <div className="button-row">
              <button
                type="button"
                className="btn btn-secondary btn-sm"
                disabled={safeCashPage <= 0}
                onClick={() => setCaissePage((p) => Math.max(0, p - 1))}
              >
                Précédent
              </button>
              <button
                type="button"
                className="btn btn-secondary btn-sm"
                disabled={safeCashPage >= cashPageCount - 1}
                onClick={() => setCaissePage((p) => Math.min(cashPageCount - 1, p + 1))}
              >
                Suivant
              </button>
            </div>
          </div>
        ) : null}
      </section>

      <section className="treasury-hero panel">
        <div className="treasury-hero-main">
          <p className="treasury-kicker">Solde en ligne à reverser</p>
          <p className="treasury-balance">
            {loading ? '…' : formatCurrency(available)}
          </p>
          <p className="muted" style={{ margin: 0 }}>
            Paiements mobiles / carte uniquement — distinct de la caisse espèces.
          </p>
          {canRequestReversement ? (
            <div className="button-row" style={{ marginTop: 14 }}>
              <button
                type="button"
                className="btn btn-primary"
                disabled={loading || !canRequest}
                onClick={() => {
                  setError(null);
                  setReversementModalOpen(true);
                }}
              >
                Demander un reversement
              </button>
            </div>
          ) : null}
        </div>
        <div className="treasury-hero-stats">
          <div className="treasury-stat">
            <span>En attente de virement</span>
            <strong>{loading ? '…' : formatCurrency(pending)}</strong>
          </div>
          <div className="treasury-stat">
            <span>Total en ligne comptabilisé</span>
            <strong>{loading ? '…' : formatCurrency(totalBooked)}</strong>
          </div>
        </div>
      </section>

      <section className="panel">
        <div className="treasury-history-head">
          <h2 className="section-title" style={{ margin: 0 }}>Compte à créditer</h2>
          {canEditBank ? (
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => {
                setBankDraft(bank);
                setBankModalOpen(true);
              }}
            >
              {hasRib ? 'Modifier' : 'Renseigner le RIB'}
            </button>
          ) : null}
        </div>
        <div className="info-list">
          <div className="info-row"><span>Titulaire</span><span>{bank.titulaireCompte || '—'}</span></div>
          <div className="info-row"><span>RIB / IBAN</span><span>{bank.ibanOrRib || 'Non renseigné'}</span></div>
          <div className="info-row"><span>Banque</span><span>{bank.nomBanque || '—'}</span></div>
          <div className="info-row"><span>Contact paroisse</span><span>{[bank.email, bank.telephone].filter(Boolean).join(' · ') || '—'}</span></div>
        </div>
      </section>

      <section className="panel treasury-history">
        <div className="treasury-history-head">
          <h2 className="section-title" style={{ margin: 0 }}>Historique des reversements</h2>
          <div className="filter-chips">
            {STATUS_FILTERS.map((f) => (
              <button
                key={f.id}
                type="button"
                className={`chip${filter === f.id ? ' is-active' : ''}`}
                onClick={() => setFilter(f.id)}
              >
                {f.label}
                {counts[f.id] != null ? ` (${counts[f.id]})` : ''}
              </button>
            ))}
          </div>
        </div>

        {loading ? <p className="muted">Chargement…</p> : null}

        {!loading && rows.length > 0 ? (
          <div className="toolbar treasury-grid-toolbar">
            <input
              type="search"
              className="input"
              value={reversementSearch}
              onChange={(e) => setReversementSearch(e.target.value)}
              placeholder="Rechercher motif, référence…"
              aria-label="Rechercher un reversement"
            />
          </div>
        ) : null}

        {!loading ? (
          <AppTable
            columns={REVERSEMENT_COLUMNS}
            rows={pagedReversements}
            ariaLabel="Historique des reversements"
            emptyMessage={
              rows.length
                ? 'Aucune demande ne correspond à ce filtre.'
                : 'Aucune demande de reversement.'
            }
            defaultSortKey="createdAt"
            defaultSortDir="desc"
            renderCell={(row, column) => {
              switch (column.key) {
                case 'createdAt':
                  return formatDate(row.createdAt);
                case 'montant':
                  return <strong>{formatCurrency(row.montant)}</strong>;
                case 'statut':
                  return <AppBadge value={row.statut} />;
                case 'motif':
                  return row.motif || '—';
                case 'referenceVirement':
                  return row.referenceVirement || '—';
                default:
                  return row[column.key];
              }
            }}
          />
        ) : null}

        {!loading && visible.length > REVERSEMENT_PAGE_SIZE ? (
          <div className="button-row" style={{ justifyContent: 'space-between', marginTop: 12 }}>
            <span className="muted text-sm">
              {safeReversementPage * REVERSEMENT_PAGE_SIZE + 1}–
              {Math.min((safeReversementPage + 1) * REVERSEMENT_PAGE_SIZE, visible.length)}
              {' '}sur {visible.length}
            </span>
            <div className="button-row">
              <button
                type="button"
                className="btn btn-secondary btn-sm"
                disabled={safeReversementPage <= 0}
                onClick={() => setReversementPage((p) => Math.max(0, p - 1))}
              >
                Précédent
              </button>
              <button
                type="button"
                className="btn btn-secondary btn-sm"
                disabled={safeReversementPage >= reversementPageCount - 1}
                onClick={() => setReversementPage((p) => Math.min(reversementPageCount - 1, p + 1))}
              >
                Suivant
              </button>
            </div>
          </div>
        ) : null}
      </section>

      <AppDialog
        open={bankModalOpen}
        title="Coordonnées bancaires"
        confirmLabel={savingBank ? 'Enregistrement…' : 'Enregistrer'}
        cancelLabel="Annuler"
        busy={savingBank}
        size="lg"
        onCancel={() => {
          if (!savingBank) setBankModalOpen(false);
        }}
        onConfirm={saveBank}
      >
        <div className="form-grid">
          <div className="form-field">
            <label htmlFor="bank-holder">Titulaire du compte</label>
            <input
              id="bank-holder"
              maxLength={150}
              value={bankDraft.titulaireCompte}
              onChange={(e) => setBankDraft({ ...bankDraft, titulaireCompte: e.target.value })}
            />
          </div>
          <div className="form-field">
            <label htmlFor="bank-name">Banque</label>
            <input
              id="bank-name"
              maxLength={120}
              value={bankDraft.nomBanque}
              onChange={(e) => setBankDraft({ ...bankDraft, nomBanque: e.target.value })}
            />
          </div>
          <div className="form-field full">
            <label htmlFor="bank-rib">RIB / IBAN</label>
            <input
              id="bank-rib"
              maxLength={80}
              value={bankDraft.ibanOrRib}
              onChange={(e) => setBankDraft({ ...bankDraft, ibanOrRib: e.target.value })}
            />
          </div>
          <div className="form-field">
            <label htmlFor="bank-email">E-mail de la paroisse</label>
            <input
              id="bank-email"
              type="email"
              maxLength={150}
              value={bankDraft.email}
              onChange={(e) => setBankDraft({ ...bankDraft, email: e.target.value })}
            />
          </div>
          <div className="form-field">
            <label htmlFor="bank-phone">Téléphone</label>
            <input
              id="bank-phone"
              type="tel"
              maxLength={50}
              value={bankDraft.telephone}
              onChange={(e) => setBankDraft({ ...bankDraft, telephone: e.target.value })}
            />
          </div>
        </div>
      </AppDialog>

      <AppDialog
        open={reversementModalOpen}
        title="Demander un reversement"
        confirmLabel={busy ? 'Envoi…' : 'Demander le virement'}
        cancelLabel="Annuler"
        busy={busy}
        onCancel={() => {
          if (!busy) setReversementModalOpen(false);
        }}
        onConfirm={onDemande}
      >
        <p className="muted" style={{ marginTop: 0 }}>
          Solde disponible : <strong>{formatCurrency(available)}</strong>
          {!hasRib ? ' — renseignez d’abord le RIB.' : ''}
        </p>
        <div className="stack" style={{ gap: 12 }}>
          <div className="form-field">
            <label htmlFor="treasury-amount">Montant (FCFA)</label>
            <div className="treasury-amount-row">
              <input
                id="treasury-amount"
                required
                type="number"
                min={100}
                max={available > 0 ? available : undefined}
                step={1}
                value={montant}
                disabled={busy || !canRequest}
                onChange={(e) => setMontant(e.target.value)}
                placeholder="0"
              />
              <button
                type="button"
                className="btn btn-secondary"
                disabled={busy || !canRequest}
                onClick={useMaxAvailable}
              >
                Tout
              </button>
            </div>
          </div>
          <div className="form-field">
            <label htmlFor="treasury-motif">Motif (optionnel)</label>
            <input
              id="treasury-motif"
              value={motif}
              disabled={busy || !canRequest}
              onChange={(e) => setMotif(e.target.value)}
              placeholder="Ex. Reversement mensuel"
              maxLength={200}
            />
          </div>
        </div>
      </AppDialog>
    </div>
  );
}
