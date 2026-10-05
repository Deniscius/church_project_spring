import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import PageHeader from '../../../components/ui/PageHeader';
import AppTable from '../../../components/ui/AppTable';
import AppBadge from '../../../components/ui/AppBadge';
import AppDialog from '../../../components/ui/AppDialog';
import AppInput from '../../../components/ui/AppInput';
import { useTenant } from '../../../hooks/useTenant';
import { usePermissions } from '../../../hooks/usePermissions';
import { PERMISSIONS } from '../../../constants/roles';
import { ROUTES, routePath } from '../../../constants/routes';
import { pastoralActorService } from '../../../services/pastoralActor.service';

const columns = [
  { key: 'nomComplet', label: 'Nom complet' },
  { key: 'categorieLabel', label: 'Catégorie' },
  { key: 'contact', label: 'Contact' },
  { key: 'statut', label: 'Statut' },
  { key: 'actions', label: 'Actions' },
];

const CATEGORY_LABELS = {
  CLERGE: 'Clergé',
  VIE_CONSACREE: 'Vie consacrée',
  LAIC: 'Laïc',
  AUTRE: 'Autre',
};

const ACTION_CONTENT = {
  deactivate: {
    title: 'Désactiver l’acteur pastoral',
    label: 'Désactiver',
    message: 'La personne restera dans l’historique mais ne sera plus proposée pour de nouvelles affectations.',
  },
  activate: {
    title: 'Réactiver l’acteur pastoral',
    label: 'Réactiver',
    message: 'La personne pourra de nouveau recevoir des mandats et des affectations.',
  },
  archive: {
    title: 'Archiver l’acteur pastoral',
    label: 'Archiver',
    message: 'La fiche sera retirée du répertoire. Seuls les acteurs déjà désactivés peuvent être archivés.',
  },
};

export default function PastoralActorsPage() {
  const { activeParish } = useTenant();
  const { has } = usePermissions();
  const [actors, setActors] = useState([]);
  const [statusFilter, setStatusFilter] = useState('ACTIVE');
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [pendingAction, setPendingAction] = useState(null);
  const [busy, setBusy] = useState(false);
  const loadSequence = useRef(0);
  const canManage = has(PERMISSIONS.PASTORAL_ACTOR_MANAGE);

  const load = useCallback(async () => {
    const sequence = ++loadSequence.current;
    if (!activeParish?.id) {
      setActors([]);
      setLoading(false);
      return;
    }
    try {
      setLoading(true);
      setError(null);
      const actif = statusFilter === 'ALL' ? undefined : statusFilter === 'ACTIVE';
      const data = await pastoralActorService.getByParish(activeParish.id, {
        actif,
        q: search,
      });
      if (sequence !== loadSequence.current) return;
      setActors(Array.isArray(data) ? data : []);
    } catch (e) {
      if (sequence !== loadSequence.current) return;
      setError(e instanceof Error ? e.message : 'Chargement impossible');
    } finally {
      if (sequence === loadSequence.current) setLoading(false);
    }
  }, [activeParish?.id, search, statusFilter]);

  useEffect(() => {
    const timer = window.setTimeout(load, 250);
    return () => {
      window.clearTimeout(timer);
      loadSequence.current += 1;
    };
  }, [load]);

  const rows = useMemo(() => actors.map((actor) => {
    const title = actor.appellation ? `${actor.appellation} ` : '';
    const contacts = [actor.telephone, actor.email].filter(Boolean);
    return {
      ...actor,
      id: actor.publicId,
      nomComplet: `${title}${actor.prenoms} ${actor.nom}`.trim(),
      categorieLabel: CATEGORY_LABELS[actor.categorie] || actor.categorie,
      contact: contacts.length > 0 ? contacts.join(' · ') : '—',
      statut: actor.actif ? 'ACTIF' : 'INACTIF',
    };
  }), [actors]);

  const confirmAction = async () => {
    if (!pendingAction) return;
    const { type, actor } = pendingAction;
    try {
      setBusy(true);
      setError(null);
      if (type === 'activate') {
        await pastoralActorService.setStatus(actor.publicId, true, actor.version);
      } else if (type === 'deactivate') {
        await pastoralActorService.setStatus(actor.publicId, false, actor.version);
      } else {
        await pastoralActorService.remove(actor.publicId, actor.version);
      }
      setPendingAction(null);
      await load();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Opération impossible');
      setPendingAction(null);
    } finally {
      setBusy(false);
    }
  };

  const dialog = pendingAction ? ACTION_CONTENT[pendingAction.type] : null;

  return (
    <div className="stack">
      <PageHeader
        title="Acteurs pastoraux"
        subtitle="Répertoire des personnes participant à la vie pastorale de la paroisse."
        actions={canManage ? (
          <Link className="btn btn-primary" to={ROUTES.PASTORAL_ACTOR_CREATE}>
            Ajouter une personne
          </Link>
        ) : null}
      />

      <div className="card filters">
        <AppInput
          type="search"
          placeholder="Rechercher par nom, téléphone ou email"
          aria-label="Rechercher un acteur pastoral"
          value={search}
          onChange={(event) => setSearch(event.target.value)}
        />
        <select
          className="select"
          value={statusFilter}
          onChange={(event) => setStatusFilter(event.target.value)}
          aria-label="Filtrer par statut"
        >
          <option value="ACTIVE">Actifs</option>
          <option value="INACTIVE">Inactifs</option>
          <option value="ALL">Tous</option>
        </select>
      </div>

      {error ? <p className="text-red-600" role="alert">{error}</p> : null}
      {loading ? <p className="muted">Chargement…</p> : null}

      <AppTable
        columns={columns}
        rows={rows}
        renderCell={(row, column) => {
          if (column.key === 'statut') {
            return (
              <AppBadge
                value={row.statut}
                label={row.actif ? 'Actif' : 'Inactif'}
              />
            );
          }
          if (column.key === 'actions') {
            if (!canManage) return '—';
            return (
              <div className="button-row">
                <Link
                  className="btn btn-secondary"
                  to={routePath(ROUTES.PASTORAL_ACTOR_EDIT, { id: row.publicId })}
                >
                  Modifier
                </Link>
                <button
                  type="button"
                  className={row.actif ? 'btn btn-secondary' : 'btn btn-primary'}
                  onClick={() => setPendingAction({
                    type: row.actif ? 'deactivate' : 'activate',
                    actor: row,
                  })}
                >
                  {row.actif ? 'Désactiver' : 'Réactiver'}
                </button>
                {!row.actif ? (
                  <button
                    type="button"
                    className="btn btn-danger"
                    onClick={() => setPendingAction({ type: 'archive', actor: row })}
                  >
                    Archiver
                  </button>
                ) : null}
              </div>
            );
          }
          return row[column.key];
        }}
      />

      <AppDialog
        open={Boolean(pendingAction)}
        title={dialog?.title || ''}
        confirmLabel={dialog?.label || 'Confirmer'}
        cancelLabel="Annuler"
        danger={pendingAction?.type === 'archive'}
        busy={busy}
        onCancel={() => setPendingAction(null)}
        onConfirm={confirmAction}
      >
        <p style={{ margin: 0 }}>{dialog?.message}</p>
      </AppDialog>
    </div>
  );
}
