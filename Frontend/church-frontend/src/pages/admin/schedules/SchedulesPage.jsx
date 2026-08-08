import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import PageHeader from '../../../components/ui/PageHeader';
import AppTable from '../../../components/ui/AppTable';
import AppBadge from '../../../components/ui/AppBadge';
import AppDialog from '../../../components/ui/AppDialog';
import { useTenant } from '../../../hooks/useTenant';
import { scheduleService } from '../../../services/schedule.service';
import { mapHoraireToRow } from '../../../utils/apiMappers';
import { usePermissions } from '../../../hooks/usePermissions';
import { PERMISSIONS } from '../../../constants/roles';

const columns = [
  { key: 'label', label: 'Libellé' },
  { key: 'day', label: 'Jour / date' },
  { key: 'hour', label: 'Heure' },
  { key: 'flags', label: 'Type' },
  { key: 'active', label: 'État' },
  { key: 'actions', label: 'Actions' },
];

export default function SchedulesPage() {
  const { activeParish } = useTenant();
  const { has } = usePermissions();
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [pendingDelete, setPendingDelete] = useState(null);
  const [deletingId, setDeletingId] = useState(null);
  const canManage = has(PERMISSIONS.SCHEDULE_MANAGE);

  const remove = async () => {
    if (!pendingDelete) return;
    try {
      setDeletingId(pendingDelete);
      setError(null);
      await scheduleService.remove(pendingDelete);
      setRows((current) => current.filter((row) => row.id !== pendingDelete));
      setPendingDelete(null);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Suppression impossible');
    } finally {
      setDeletingId(null);
    }
  };

  useEffect(() => {
    if (!activeParish?.id) {
      setRows([]);
      setLoading(false);
      return;
    }
    let cancelled = false;
    (async () => {
      try {
        setLoading(true);
        setError(null);
        const data = await scheduleService.getByParish(activeParish.id);
        if (!cancelled) setRows((data || []).map(mapHoraireToRow));
      } catch (e) {
        if (!cancelled) setError(e instanceof Error ? e.message : 'Erreur');
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [activeParish?.id]);

  return (
    <div className="stack">
      <PageHeader title="Horaires" subtitle="Créneaux hebdomadaires et messes à date précise de la paroisse active."
        actions={canManage ? <Link className="btn btn-primary" to="/admin/horaires/nouveau">Nouvel horaire</Link> : null} />
      {error ? <p className="text-red-600">{error}</p> : null}
      {loading ? <p className="muted">Chargement…</p> : null}
      <AppTable
        columns={columns}
        rows={rows}
        renderCell={(row, column) => {
          if (column.key === 'active') return <AppBadge value={row.active} />;
          if (column.key === 'actions') return canManage ? (
            <div className="button-row">
              <Link className="btn btn-secondary" to={`/admin/horaires/${row.id}/modifier`}>Modifier</Link>
              <button
                className="btn btn-danger"
                disabled={deletingId === row.id}
                onClick={() => setPendingDelete(row.id)}
              >
                {deletingId === row.id ? 'Suppression…' : 'Supprimer'}
              </button>
            </div>
          ) : '—';
          return row[column.key];
        }}
      />

      <AppDialog
        open={Boolean(pendingDelete)}
        title="Supprimer l'horaire"
        confirmLabel="Supprimer"
        cancelLabel="Annuler"
        danger
        busy={Boolean(deletingId)}
        onCancel={() => setPendingDelete(null)}
        onConfirm={remove}
      >
        <p style={{ margin: 0 }}>Supprimer cet horaire ?</p>
      </AppDialog>
    </div>
  );
}
