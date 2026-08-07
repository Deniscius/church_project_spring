import React, { useEffect, useMemo, useState } from 'react';
import PageHeader from '../../../components/ui/PageHeader';
import AppBadge from '../../../components/ui/AppBadge';
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
  const [savingBank, setSavingBank] = useState(false);
  const [editingBank, setEditingBank] = useState(false);
  const [caisse, setCaisse] = useState(null);

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
      setBank({
        nomBanque: p?.nomBanque || '',
        titulaireCompte: p?.titulaireCompte || '',
        ibanOrRib: p?.ibanOrRib || '',
        email: p?.email || '',
        telephone: p?.telephone || '',
      });
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Erreur');
    } finally {
      setLoading(false);
    }
  }

  async function saveBank(e) {
    e.preventDefault();
    if (!paroisseId) return;
    setSavingBank(true);
    setError(null);
    setInfo(null);
    try {
      await parishService.updateCoordonnees(paroisseId, bank);
      setEditingBank(false);
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

  const available = compte?.soldeDisponible ?? 0;
  const pending = compte?.soldeEnAttente ?? 0;
  const totalBooked = available + pending;
  const hasRib = Boolean(bank.ibanOrRib);
  const canRequest = available > 0 && hasRib;

  const visible = useMemo(
    () => (filter === 'ALL' ? rows : rows.filter((r) => r.statut === filter)),
    [rows, filter]
  );

  const counts = useMemo(() => ({
    ALL: rows.length,
    EN_ATTENTE: rows.filter((r) => r.statut === 'EN_ATTENTE').length,
    PAYE: rows.filter((r) => r.statut === 'PAYE').length,
    REJETE: rows.filter((r) => r.statut === 'REJETE').length,
  }), [rows]);

  async function onDemande(e) {
    e.preventDefault();
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
            ? `Paiements en ligne à reverser et caisse espèces locale — ${activeParish.name}`
            : 'Solde en ligne à reverser, distinct de la caisse espèces gérée par la paroisse.'
        }
      />

      <StepHelpBanner title="Solde en ligne" text={HELP.admin.tresorerie} />

      {error ? <p className="alert-error">{error}</p> : null}
      {info ? <p className="alert-success">{info}</p> : null}
      {isLocalAccountant ? (
        <div className="alert-info" role="status">
          Contrôle comptable : consultation de la caisse et du solde en ligne. L’encaissement
          relève de la secrétaire ; la demande de reversement relève de l’administrateur.
        </div>
      ) : null}
      {canManageCashOps && !canRequestReversement ? (
        <div className="alert-info" role="status">
          Caisse locale : vous pouvez encaisser les espèces depuis les demandes. Le journal
          ci-dessous sert au suivi quotidien.
        </div>
      ) : null}

      {!paroisseId ? (
        <p className="muted">Sélectionnez une paroisse active pour consulter la trésorerie.</p>
      ) : null}

      <section className="treasury-hero panel">
        <div className="treasury-hero-main">
          <p className="treasury-kicker">Solde en ligne à reverser</p>
          <p className="treasury-balance">
            {loading ? '…' : formatCurrency(available)}
          </p>
          <p className="muted" style={{ margin: 0 }}>
            Uniquement les paiements mobiles / carte. Les espèces restent en caisse locale.
          </p>
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
        <h2 className="section-title">Caisse locale (espèces)</h2>
        <p className="muted" style={{ marginTop: 0 }}>
          Encaissements au secrétariat. Gérés par la paroisse — jamais inclus dans un reversement plateforme.
        </p>
        <div className="card-grid" style={{ marginBottom: 16 }}>
          <div className="treasury-stat">
            <span>Aujourd’hui</span>
            <strong>{loading ? '…' : formatCurrency(caisse?.totalDuJour || 0)}</strong>
          </div>
          <div className="treasury-stat">
            <span>Ce mois</span>
            <strong>{loading ? '…' : formatCurrency(caisse?.totalDuMois || 0)}</strong>
          </div>
          <div className="treasury-stat">
            <span>Total encaissé</span>
            <strong>{loading ? '…' : formatCurrency(caisse?.total || 0)}</strong>
          </div>
          <div className="treasury-stat">
            <span>Nombre d’encaissements</span>
            <strong>{loading ? '…' : (caisse?.nombreEncaissements || 0)}</strong>
          </div>
        </div>
        {!loading && (!caisse?.encaissements || caisse.encaissements.length === 0) ? (
          <p className="muted">Aucun encaissement espèces pour le moment.</p>
        ) : null}
        {!loading && caisse?.encaissements?.length > 0 ? (
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Date / heure</th>
                  <th>Encaisseur</th>
                  <th>Code</th>
                  <th>Fidèle</th>
                  <th>Intention</th>
                  <th>Montant</th>
                </tr>
              </thead>
              <tbody>
                {caisse.encaissements.map((line) => (
                  <tr key={line.publicId}>
                    <td>{formatDate(line.dateEncaissement)}</td>
                    <td>{line.encaisseurNom || '—'}</td>
                    <td><strong>{line.codeSuivie || '—'}</strong></td>
                    <td>{line.fidele || '—'}</td>
                    <td>{line.intention || '—'}</td>
                    <td><strong>{formatCurrency(line.montant || 0)}</strong></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : null}
      </section>

      <section className="panel">
        <div className="treasury-history-head">
          <h2 className="section-title" style={{ margin: 0 }}>Compte à créditer</h2>
          {!editingBank && canEditBank ? (
            <button type="button" className="btn btn-secondary" onClick={() => setEditingBank(true)}>
              {hasRib ? 'Modifier' : 'Renseigner le RIB'}
            </button>
          ) : null}
        </div>

        {!editingBank ? (
          <div className="info-list">
            <div className="info-row"><span>Titulaire</span><span>{bank.titulaireCompte || '—'}</span></div>
            <div className="info-row"><span>RIB / IBAN</span><span>{bank.ibanOrRib || 'Non renseigné'}</span></div>
            <div className="info-row"><span>Banque</span><span>{bank.nomBanque || '—'}</span></div>
            <div className="info-row"><span>Contact paroisse</span><span>{[bank.email, bank.telephone].filter(Boolean).join(' · ') || '—'}</span></div>
          </div>
        ) : (
          <form className="form-grid" onSubmit={saveBank}>
            <div className="form-field">
              <label htmlFor="bank-holder">Titulaire du compte</label>
              <input id="bank-holder" maxLength={150} value={bank.titulaireCompte}
                onChange={(e) => setBank({ ...bank, titulaireCompte: e.target.value })} />
            </div>
            <div className="form-field">
              <label htmlFor="bank-name">Banque</label>
              <input id="bank-name" maxLength={120} value={bank.nomBanque}
                onChange={(e) => setBank({ ...bank, nomBanque: e.target.value })} />
            </div>
            <div className="form-field full">
              <label htmlFor="bank-rib">RIB / IBAN</label>
              <input id="bank-rib" maxLength={80} value={bank.ibanOrRib}
                onChange={(e) => setBank({ ...bank, ibanOrRib: e.target.value })} />
            </div>
            <div className="form-field">
              <label htmlFor="bank-email">E-mail de la paroisse</label>
              <input id="bank-email" type="email" maxLength={150} value={bank.email}
                onChange={(e) => setBank({ ...bank, email: e.target.value })} />
            </div>
            <div className="form-field">
              <label htmlFor="bank-phone">Téléphone</label>
              <input id="bank-phone" type="tel" maxLength={50} value={bank.telephone}
                onChange={(e) => setBank({ ...bank, telephone: e.target.value })} />
            </div>
            <div className="form-field full button-row">
              <button type="submit" className="btn btn-primary" disabled={savingBank}>
                {savingBank ? 'Enregistrement…' : 'Enregistrer'}
              </button>
              <button type="button" className="btn btn-secondary" disabled={savingBank}
                onClick={() => { setEditingBank(false); load(); }}>
                Annuler
              </button>
            </div>
          </form>
        )}
      </section>

      <div className="treasury-layout">
        {canRequestReversement ? (
        <section className="panel treasury-request">
          <h2 className="section-title">Demander un reversement</h2>
          <p className="muted" style={{ marginTop: 0 }}>
            {hasRib
              ? 'Le comptable de la plateforme exécute le virement puis confirme sa référence ici.'
              : 'Renseignez d’abord le RIB ci-dessus : sans lui, aucune demande ne peut être transmise.'}
          </p>
          <form className="stack" onSubmit={onDemande}>
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
                  disabled={busy || loading || !canRequest}
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
            <button
              type="submit"
              className="btn btn-primary"
              disabled={busy || !paroisseId || !canRequest}
            >
              {busy ? 'Envoi…' : 'Demander le virement'}
            </button>
            {available <= 0 && !loading ? (
              <p className="muted" style={{ margin: 0 }}>
                Aucun solde disponible pour le moment.
              </p>
            ) : null}
          </form>
        </section>
        ) : (
          <section className="panel treasury-request">
            <h2 className="section-title">Contrôle trésorerie</h2>
            <p className="muted" style={{ marginTop: 0 }}>
              {isLocalAccountant
                ? 'Vous consultez les soldes et la caisse pour contrôle. La demande de reversement est réservée à l’administrateur de paroisse.'
                : 'La demande de reversement en ligne est réservée à l’administrateur de paroisse.'}
            </p>
          </section>
        )}

        <section className="panel treasury-history">
          <div className="treasury-history-head">
            <h2 className="section-title" style={{ margin: 0 }}>Historique</h2>
            <div className="filter-chips">
              {STATUS_FILTERS.map((f) => (
                <button
                  key={f.id}
                  type="button"
                  className={`onboard-mini${filter === f.id ? ' active' : ''}`}
                  onClick={() => setFilter(f.id)}
                >
                  {f.label}
                  {counts[f.id] != null ? ` (${counts[f.id]})` : ''}
                </button>
              ))}
            </div>
          </div>

          {loading ? <p className="muted">Chargement…</p> : null}

          {!loading && visible.length === 0 ? (
            <p className="muted">Aucune demande de reversement.</p>
          ) : null}

          {!loading && visible.length > 0 ? (
            <div className="table-wrap">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Date</th>
                    <th>Montant</th>
                    <th>Statut</th>
                    <th>Motif</th>
                    <th>Référence</th>
                  </tr>
                </thead>
                <tbody>
                  {visible.map((r) => (
                    <tr key={r.publicId}>
                      <td>{formatDate(r.createdAt)}</td>
                      <td><strong>{formatCurrency(r.montant)}</strong></td>
                      <td><AppBadge value={r.statut} /></td>
                      <td>{r.motif || '—'}</td>
                      <td>{r.referenceVirement || '—'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : null}
        </section>
      </div>
    </div>
  );
}
