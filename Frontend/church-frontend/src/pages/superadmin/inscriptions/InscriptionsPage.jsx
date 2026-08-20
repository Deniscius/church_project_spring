import React, { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { useQueryClient } from '@tanstack/react-query';
import PageHeader from '../../../components/ui/PageHeader';
import AppBadge from '../../../components/ui/AppBadge';
import AppDialog from '../../../components/ui/AppDialog';
import { inscriptionService, comptabiliteService } from '../../../services/inscription.service';
import { formatCurrency } from '../../../utils/formatCurrency';
import { usePermissions } from '../../../hooks/usePermissions';
import { PERMISSIONS } from '../../../constants/roles';

const FILTERS = [
  { id: 'SOUMISE', label: 'À approuver', match: (r) => r.statut === 'SOUMISE' },
  { id: 'A_ACTIVER', label: 'À activer', match: (r) => r.statut === 'APPROUVEE' && !r.paroisseActive },
  { id: 'ACTIVE', label: 'En service', match: (r) => r.statut === 'APPROUVEE' && r.paroisseActive === true },
  { id: 'REJETEE', label: 'Rejetées', match: (r) => r.statut === 'REJETEE' },
  { id: 'ALL', label: 'Tous', match: () => true },
];

function contactLine(email, telephone) {
  return [email, telephone].filter(Boolean).join(' · ') || '—';
}

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

function stepState(row, step) {
  if (row.statut === 'REJETEE') return step === 1 ? 'done' : '';
  const approuvee = row.statut === 'APPROUVEE';
  const active = approuvee && row.paroisseActive === true;

  switch (step) {
    case 1:
      return 'done';
    case 2:
      return approuvee ? 'done' : 'active';
    case 3:
      if (active) return 'done';
      return approuvee ? 'active' : '';
    case 4:
      return active ? 'done' : '';
    default:
      return '';
  }
}

export default function InscriptionsPage() {
  const queryClient = useQueryClient();
  const { has } = usePermissions();
  const canManageRegistration = has(PERMISSIONS.PARISH_REGISTRATION_MANAGE);
  const canCheckout = has(PERMISSIONS.SUBSCRIPTION_CHECKOUT);
  const canActivate = has(PERMISSIONS.SUBSCRIPTION_ACTIVATE);
  const canViewParishes = has(PERMISSIONS.PARISH_READ);
  const [rows, setRows] = useState([]);
  const [filter, setFilter] = useState('SOUMISE');
  const [selectedId, setSelectedId] = useState(null);
  const [error, setError] = useState(null);
  const [info, setInfo] = useState(null);
  const [busy, setBusy] = useState(false);
  const [paymentUrl, setPaymentUrl] = useState('');
  const [pendingReject, setPendingReject] = useState(null);
  const [rejectMotif, setRejectMotif] = useState('');
  const [pendingActivate, setPendingActivate] = useState(null);

  async function load() {
    try {
      setError(null);
      const data = await inscriptionService.list();
      setRows(Array.isArray(data) ? data : []);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Erreur de chargement');
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

  const visible = useMemo(() => {
    const active = FILTERS.find((f) => f.id === filter) || FILTERS[FILTERS.length - 1];
    return rows.filter(active.match);
  }, [rows, filter]);

  const selected = useMemo(
    () => rows.find((r) => r.publicId === selectedId) || visible[0] || null,
    [rows, selectedId, visible]
  );

  useEffect(() => {
    if (selected && !visible.some((r) => r.publicId === selected.publicId)) {
      setSelectedId(visible[0]?.publicId || null);
    }
  }, [visible, selected]);

  async function approuver(row) {
    if (!canManageRegistration) return;
    setBusy(true);
    setInfo(null);
    setPaymentUrl('');
    try {
      const res = await inscriptionService.approuver(row.publicId);
      const url = res?.abonnementCheckout?.paymentUrl || '';
      setPaymentUrl(url);
      const emailsHint = [res?.emailParoisse, res?.emailAdmin].filter(Boolean).length
        ? ` E-mails pro : paroisse ${res.emailParoisse || '—'} · admin ${res.emailAdmin || '—'}.`
        : '';
      if (canCheckout || canActivate) {
        setInfo(
          (url && canCheckout
            ? 'Dossier approuvé. Le lien de paiement est disponible.'
            : canActivate
              ? 'Dossier approuvé. Vous pouvez activer l’accès si le paiement a déjà été reçu.'
              : 'Dossier approuvé.')
          + emailsHint
        );
      } else {
        setInfo(`Dossier approuvé. Le traitement de l’abonnement relève d’un compte autorisé.${emailsHint}`);
      }
      await queryClient.invalidateQueries({ queryKey: ['paroisses'] });
      await load();
      setSelectedId(row.publicId);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Échec approbation');
    } finally {
      setBusy(false);
    }
  }

  async function rejeter() {
    if (!canManageRegistration || !pendingReject) return;
    const motif = rejectMotif.trim();
    if (motif.length < 8) {
      setError('Indiquez un motif de rejet (au moins 8 caractères).');
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await inscriptionService.rejeter(pendingReject.publicId, motif);
      setInfo('Inscription rejetée. Un e-mail a été envoyé au contact admin.');
      setPendingReject(null);
      setRejectMotif('');
      setFilter('REJETEE');
      await load();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Échec rejet');
    } finally {
      setBusy(false);
    }
  }

  async function genererLienPaiement(row) {
    if (!canCheckout) return;
    if (!row.paroissePublicId) {
      setError('Paroisse pas encore créée — approuvez d’abord le dossier.');
      return;
    }
    setBusy(true);
    setError(null);
    try {
      const res = await comptabiliteService.checkoutAbonnement(row.paroissePublicId, row.planAbonnement);
      const url = res?.paymentUrl || '';
      setPaymentUrl(url);
      if (url) {
        setInfo('Lien de paiement généré. Copiez-le ou ouvrez FedaPay.');
      } else {
        setInfo(res?.message || 'Pas d’URL FedaPay disponible.');
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Échec génération lien');
    } finally {
      setBusy(false);
    }
  }

  async function activerAcces() {
    if (!canActivate) return;
    if (!pendingActivate?.paroissePublicId) {
      setError('Paroisse introuvable sur ce dossier.');
      return;
    }
    setBusy(true);
    setError(null);
    try {
      const res = await comptabiliteService.activerAbonnement(
        pendingActivate.paroissePublicId,
        pendingActivate.planAbonnement
      );
      setInfo(res?.message || 'Paroisse activée. L’administrateur peut se connecter.');
      setPendingActivate(null);
      await queryClient.invalidateQueries({ queryKey: ['paroisses'] });
      await load();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Échec activation');
    } finally {
      setBusy(false);
    }
  }

  async function copyPaymentUrl() {
    if (!paymentUrl) return;
    try {
      await navigator.clipboard.writeText(paymentUrl);
      setInfo('Lien copié dans le presse-papiers.');
    } catch {
      setInfo(paymentUrl);
    }
  }

  async function openDocument(publicId, type) {
    setBusy(true);
    setError(null);
    try {
      const blob = await inscriptionService.downloadDocument(publicId, type);
      const url = URL.createObjectURL(blob);
      window.open(url, '_blank', 'noopener,noreferrer');
      window.setTimeout(() => URL.revokeObjectURL(url), 60_000);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Document inaccessible');
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="stack">
      <PageHeader
        title="Inscriptions"
        subtitle="Pipeline SaaS : validation → paiement abonnement → activation → connexion admin paroisse."
        actions={
          canViewParishes ? (
            <Link className="btn btn-secondary" to="/admin/paroisses" style={{ textDecoration: 'none' }}>
              Voir les paroisses
            </Link>
          ) : null
        }
      />

      <div className="card-grid inscription-kpi-grid">
        {[
          { id: 'SOUMISE', label: 'À approuver', tone: 'warn' },
          { id: 'A_ACTIVER', label: 'À activer', tone: 'info' },
          { id: 'ACTIVE', label: 'En service', tone: 'ok' },
          { id: 'REJETEE', label: 'Rejetées', tone: 'danger' },
        ].map((kpi) => (
          <button
            key={kpi.id}
            type="button"
            className={`card inscription-kpi inscription-kpi--${kpi.tone}${filter === kpi.id ? ' is-active' : ''}`}
            onClick={() => setFilter(kpi.id)}
          >
            <div className="muted">{kpi.label}</div>
            <strong style={{ fontSize: '1.45rem' }}>{counts[kpi.id] || 0}</strong>
          </button>
        ))}
      </div>

      <div className="filter-chips" role="group" aria-label="Étape du dossier">
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

      {error ? <div className="alert-danger" role="alert">{error}</div> : null}
      {info ? <div className="alert-success" role="status">{info}</div> : null}

      <div className="inbox-layout">
        <aside className="inbox-list card">
          {visible.length === 0 ? (
            <p className="muted" style={{ margin: 0 }}>Aucun dossier dans ce filtre.</p>
          ) : (
            visible.map((row) => {
              const stage = row.statut === 'REJETEE'
                ? 'Rejetée'
                : row.statut === 'SOUMISE'
                  ? 'À approuver'
                  : row.paroisseActive
                    ? 'En service'
                    : 'À activer';
              const docs = [
                row.mandatCurePresent ? 'Mandat' : null,
                row.adminCniPresent ? 'CNI' : null,
              ].filter(Boolean).join(' · ') || 'Docs manquants';
              return (
                <button
                  key={row.publicId}
                  type="button"
                  className={`inbox-item inscription-card${selected?.publicId === row.publicId ? ' active' : ''}`}
                  onClick={() => {
                    setSelectedId(row.publicId);
                    setPaymentUrl('');
                  }}
                >
                  <div className="inbox-item-top">
                    <strong>{row.nomParoisse}</strong>
                    <span className={`inscription-stage-chip statut-${row.statut?.toLowerCase() || 'x'}`}>
                      {stage}
                    </span>
                  </div>
                  <div className="inscription-card-meta">
                    <span>{row.planAbonnement} · {formatCurrency(row.montantAbonnement)}</span>
                    <span>{formatDateTime(row.createdAt)}</span>
                  </div>
                  <div className="muted text-sm">
                    {row.adminPrenom} {row.adminNom}
                    {row.doyenneNom ? ` · ${row.doyenneNom}` : ''}
                  </div>
                  <div className="inscription-card-docs muted text-sm">{docs}</div>
                </button>
              );
            })
          )}
        </aside>

        <section className="inbox-detail card">
          {!selected ? (
            <p className="muted">Sélectionnez un dossier pour le traiter.</p>
          ) : (
            <div className="stack">
              <div className="inbox-detail-head">
                <div>
                  <h2 style={{ margin: '0 0 6px' }}>{selected.nomParoisse}</h2>
                  <p className="muted" style={{ margin: 0 }}>{selected.adresse}</p>
                </div>
                <AppBadge value={selected.statut} />
              </div>

              <div className="onboard-steps" aria-label="Étapes d’activation">
                <div className={`onboard-mini ${stepState(selected, 1)}`}>1. Dossier</div>
                <div className={`onboard-mini ${stepState(selected, 2)}`}>2. Approbation</div>
                <div className={`onboard-mini ${stepState(selected, 3)}`}>3. Paiement / activation</div>
                <div className={`onboard-mini ${stepState(selected, 4)}`}>4. Connexion admin</div>
              </div>

              <div className="info-list">
                <div className="info-row"><span>Doyenné</span><span>{selected.doyenneNom || '—'}</span></div>
                <div className="info-row"><span>Contact paroisse</span><span>{contactLine(selected.email, selected.telephone)}</span></div>
                <div className="info-row"><span>Plan</span><strong>{selected.planAbonnement} — {formatCurrency(selected.montantAbonnement)}</strong></div>
                <div className="info-row"><span>Déposé le</span><span>{formatDateTime(selected.createdAt)}</span></div>
                <div className="info-row"><span>Admin</span><span>{selected.adminPrenom} {selected.adminNom}</span></div>
                <div className="info-row"><span>Identifiant</span><strong>{selected.adminUsername}</strong></div>
                <div className="info-row"><span>Contact admin</span><span>{contactLine(selected.adminEmail, selected.adminTelephone)}</span></div>
                {selected.paroissePublicId ? (
                  <div className="info-row">
                    <span>Accès paroisse</span>
                    <span>
                      {selected.paroisseActive
                        ? `Actif${selected.abonnementFinAt ? ` jusqu’au ${formatDateTime(selected.abonnementFinAt)}` : ''}`
                        : 'En attente d’activation'}
                    </span>
                  </div>
                ) : (
                  <div className="info-row">
                    <span>Accès paroisse</span>
                    <span className="muted">La paroisse sera créée à l’approbation</span>
                  </div>
                )}
                {selected.message && selected.statut !== 'REJETEE' ? (
                  <div className="info-row"><span>Message</span><span>{selected.message}</span></div>
                ) : null}
                <div className="info-row">
                  <span>Documents</span>
                  <span>
                    {[
                      selected.mandatCurePresent ? 'Mandat curé' : null,
                      selected.adminCniPresent ? 'CNI admin' : null,
                    ].filter(Boolean).join(' · ') || 'Manquants'}
                  </span>
                </div>
                {selected.membres?.length ? (
                  <div className="info-row">
                    <span>Équipe</span>
                    <span>
                      {selected.membres
                        .map((m) => `${m.prenom} ${m.nom}${m.roleParoisse ? ` (${m.roleParoisse})` : ''}`)
                        .join(' · ')}
                    </span>
                  </div>
                ) : null}
              </div>

              {selected.membres?.length ? (
                <details className="members-details">
                  <summary>Coordonnées de l’équipe ({selected.membres.length})</summary>
                  <div className="info-list">
                    {selected.membres.map((m) => (
                      <div className="info-row" key={`${m.username || m.email || m.nom}-${m.prenom}`}>
                        <span>{m.prenom} {m.nom}</span>
                        <span>{contactLine(m.email, m.telephone)}</span>
                      </div>
                    ))}
                  </div>
                </details>
              ) : null}

              {(selected.mandatCurePresent || selected.adminCniPresent) ? (
                <div className="button-row">
                  {selected.mandatCurePresent ? (
                    <button
                      type="button"
                      className="btn btn-secondary"
                      disabled={busy}
                      onClick={() => openDocument(selected.publicId, 'mandat')}
                    >
                      Voir le mandat du curé
                    </button>
                  ) : null}
                  {selected.adminCniPresent ? (
                    <button
                      type="button"
                      className="btn btn-secondary"
                      disabled={busy}
                      onClick={() => openDocument(selected.publicId, 'cni')}
                    >
                      Voir la pièce d’identité
                    </button>
                  ) : null}
                </div>
              ) : null}

              {selected.statut === 'REJETEE' ? (
                <div className="alert-danger" role="status">
                  <strong>Dossier rejeté.</strong>
                  {selected.message ? (
                    <p style={{ margin: '8px 0 0' }}>Motif : {selected.message}</p>
                  ) : (
                    <p style={{ margin: '8px 0 0' }} className="muted">Aucun motif renseigné.</p>
                  )}
                </div>
              ) : null}

              {canManageRegistration && selected.statut === 'SOUMISE' ? (
                <div className="button-row">
                  <button type="button" className="btn btn-primary" disabled={busy} onClick={() => approuver(selected)}>
                    Approuver le dossier
                  </button>
                  <button
                    type="button"
                    className="btn btn-danger"
                    disabled={busy}
                    onClick={() => {
                      setRejectMotif('');
                      setPendingReject(selected);
                    }}
                  >
                    Rejeter
                  </button>
                </div>
              ) : null}

              {selected.statut === 'APPROUVEE' && selected.paroisseActive ? (
                <div className="alert-success" role="status">
                  Paroisse en service. L’administrateur <strong>{selected.adminUsername}</strong> se
                  connecte sur <code>/admin/login</code>
                  {selected.abonnementFinAt
                    ? `. Abonnement valable jusqu’au ${formatDateTime(selected.abonnementFinAt)}.`
                    : '.'}
                </div>
              ) : null}

              {selected.statut === 'APPROUVEE' && !selected.paroisseActive ? (
                <div className="stack">
                  <div className="activation-callout">
                    <strong>Comment l’admin se connecte ?</strong>
                    <ol>
                      <li>Activez l’accès après paiement ou générez le lien FedaPay.</li>
                      <li>L’admin ouvre <code>/admin/login</code>.</li>
                      <li>Identifiant : <strong>{selected.adminUsername}</strong> + mot de passe choisi à l’inscription.</li>
                    </ol>
                  </div>
                  {canActivate || canCheckout ? (
                    <div className="button-row">
                      {canActivate ? (
                        <button
                          type="button"
                          className="btn btn-primary"
                          disabled={busy || !selected.paroissePublicId}
                          onClick={() => setPendingActivate(selected)}
                        >
                          Activer l’accès maintenant
                        </button>
                      ) : null}
                      {canCheckout ? (
                        <button
                          type="button"
                          className="btn btn-secondary"
                          disabled={busy || !selected.paroissePublicId}
                          onClick={() => genererLienPaiement(selected)}
                        >
                          Générer lien paiement
                        </button>
                      ) : null}
                    </div>
                  ) : (
                    <div className="alert-info" role="status">
                      Dossier approuvé. Votre compte peut consulter ce dossier mais ne peut pas traiter l’abonnement.
                    </div>
                  )}
                  {paymentUrl ? (
                    <div className="payment-link-box">
                      <div className="muted text-sm">Lien FedaPay</div>
                      <code className="payment-link">{paymentUrl}</code>
                      <div className="button-row" style={{ marginTop: 10 }}>
                        <button type="button" className="btn btn-secondary" onClick={copyPaymentUrl}>Copier</button>
                        <a className="btn btn-primary" href={paymentUrl} target="_blank" rel="noreferrer" style={{ textDecoration: 'none' }}>
                          Ouvrir FedaPay
                        </a>
                      </div>
                    </div>
                  ) : null}
                </div>
              ) : null}
            </div>
          )}
        </section>
      </div>

      <AppDialog
        open={Boolean(pendingReject) && canManageRegistration}
        title="Rejeter l'inscription"
        confirmLabel="Rejeter"
        cancelLabel="Annuler"
        danger
        busy={busy}
        promptLabel="Motif du rejet (obligatoire, min. 8 caractères)"
        promptValue={rejectMotif}
        onPromptChange={setRejectMotif}
        onCancel={() => {
          setPendingReject(null);
          setRejectMotif('');
        }}
        onConfirm={rejeter}
      >
        {pendingReject ? (
          <p style={{ margin: 0 }}>
            Rejeter le dossier « {pendingReject.nomParoisse} » ?
            Un e-mail avec le motif sera envoyé au contact admin.
          </p>
        ) : null}
      </AppDialog>

      <AppDialog
        open={Boolean(pendingActivate) && canActivate}
        title="Activer l'accès"
        confirmLabel="Activer"
        cancelLabel="Annuler"
        busy={busy}
        onCancel={() => setPendingActivate(null)}
        onConfirm={activerAcces}
      >
        {pendingActivate ? (
          <p style={{ margin: 0 }}>
            Activer « {pendingActivate.nomParoisse} » ?
            L’admin <strong>{pendingActivate.adminUsername}</strong> pourra se connecter immédiatement.
          </p>
        ) : null}
      </AppDialog>
    </div>
  );
}
