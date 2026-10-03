import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import PageHeader from '../../../components/ui/PageHeader';
import AppTable from '../../../components/ui/AppTable';
import AppBadge from '../../../components/ui/AppBadge';
import AppDialog from '../../../components/ui/AppDialog';
import { useTenant } from '../../../hooks/useTenant';
import { usePermissions } from '../../../hooks/usePermissions';
import { PERMISSIONS } from '../../../constants/roles';
import { ROUTES, routePath } from '../../../constants/routes';
import { pastoralYearService } from '../../../services/pastoralYear.service';

const columns = [
  { key: 'libelle', label: 'Libellé' },
  { key: 'periode', label: 'Période' },
  { key: 'statut', label: 'État' },
  { key: 'description', label: 'Description' },
  { key: 'actions', label: 'Actions' },
];

const STATUS_LABELS = {
  BROUILLON: 'Brouillon',
  PUBLIEE: 'Publiée',
  CLOTUREE: 'Clôturée',
};

const ACTION_CONTENT = {
  publish: {
    title: 'Publier l’année pastorale',
    label: 'Publier',
    message: 'Après publication, les dates et le contenu ne pourront plus être modifiés.',
  },
  close: {
    title: 'Clôturer l’année pastorale',
    label: 'Clôturer',
    message: 'La clôture conserve l’année dans l’historique et libère la publication de la suivante.',
  },
  delete: {
    title: 'Supprimer le brouillon',
    label: 'Supprimer',
    message: 'Ce brouillon sera supprimé. Cette action ne concerne jamais une année publiée.',
  },
};

function formatDate(value) {
  if (!value) return '—';
  const [year, month, day] = value.split('-');
  return year && month && day ? `${day}/${month}/${year}` : value;
}

export default function PastoralYearsPage() {
  const { activeParish } = useTenant();
  const { has } = usePermissions();
  const [years, setYears] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [pendingAction, setPendingAction] = useState(null);
  const [busy, setBusy] = useState(false);
  const loadSequence = useRef(0);
  const canManage = has(PERMISSIONS.PASTORAL_YEAR_MANAGE);

  const load = useCallback(async () => {
    const sequence = ++loadSequence.current;
    if (!activeParish?.id) {
      setYears([]);
      setLoading(false);
      return;
    }
    try {
      setLoading(true);
      setError(null);
      const data = await pastoralYearService.getByParish(activeParish.id);
      if (sequence !== loadSequence.current) return;
      setYears(Array.isArray(data) ? data : []);
    } catch (e) {
      if (sequence !== loadSequence.current) return;
      setError(e instanceof Error ? e.message : 'Chargement impossible');
    } finally {
      if (sequence === loadSequence.current) setLoading(false);
    }
  }, [activeParish?.id]);

  useEffect(() => {
    load();
    return () => {
      loadSequence.current += 1;
    };
  }, [load]);

  const rows = useMemo(() => years.map((year) => ({
    ...year,
    id: year.publicId,
    periode: `${formatDate(year.dateDebut)} – ${formatDate(year.dateFin)}`,
    description: year.description || '—',
  })), [years]);

  const confirmAction = async () => {
    if (!pendingAction) return;
    const { type, year } = pendingAction;
    try {
      setBusy(true);
      setError(null);
      if (type === 'publish') {
        await pastoralYearService.publish(year.publicId, year.version);
      } else if (type === 'close') {
        await pastoralYearService.close(year.publicId, year.version);
      } else {
        await pastoralYearService.remove(year.publicId, year.version);
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
        title="Années pastorales"
        subtitle="Cadre temporel des activités, célébrations et responsabilités de la paroisse."
        actions={canManage ? (
          <Link className="btn btn-primary" to={ROUTES.PASTORAL_YEAR_CREATE}>
            Nouvelle année
          </Link>
        ) : null}
      />

      {error ? <p className="text-red-600" role="alert">{error}</p> : null}
      {loading ? <p className="muted">Chargement…</p> : null}

      <AppTable
        columns={columns}
        rows={rows}
        renderCell={(row, column) => {
          if (column.key === 'statut') {
            return <AppBadge value={row.statut} label={STATUS_LABELS[row.statut]} />;
          }
          if (column.key === 'actions') {
            if (!canManage) return '—';
            return (
              <div className="button-row">
                {row.statut === 'BROUILLON' ? (
                  <>
                    <Link
                      className="btn btn-secondary"
                      to={routePath(ROUTES.PASTORAL_YEAR_EDIT, { id: row.publicId })}
                    >
                      Modifier
                    </Link>
                    <button
                      type="button"
                      className="btn btn-primary"
                      onClick={() => setPendingAction({ type: 'publish', year: row })}
                    >
                      Publier
                    </button>
                    <button
                      type="button"
                      className="btn btn-danger"
                      onClick={() => setPendingAction({ type: 'delete', year: row })}
                    >
                      Supprimer
                    </button>
                  </>
                ) : null}
                {row.statut === 'PUBLIEE' ? (
                  <button
                    type="button"
                    className="btn btn-secondary"
                    onClick={() => setPendingAction({ type: 'close', year: row })}
                  >
                    Clôturer
                  </button>
                ) : null}
                {row.statut === 'CLOTUREE' ? 'Historique' : null}
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
        danger={pendingAction?.type === 'delete'}
        busy={busy}
        onCancel={() => setPendingAction(null)}
        onConfirm={confirmAction}
      >
        <p style={{ margin: 0 }}>{dialog?.message}</p>
      </AppDialog>
    </div>
  );
}
