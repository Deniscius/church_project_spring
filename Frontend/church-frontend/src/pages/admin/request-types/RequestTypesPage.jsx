import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import PageHeader from '../../../components/ui/PageHeader';
import AppTable from '../../../components/ui/AppTable';
import AppBadge from '../../../components/ui/AppBadge';
import { useTenant } from '../../../hooks/useTenant';
import { requestTypeService } from '../../../services/requestType.service';
import { mapTypeDemandeToRow } from '../../../utils/apiMappers';
import { usePermissions } from '../../../hooks/usePermissions';
import { PERMISSIONS } from '../../../constants/roles';

const columns = [
  { key: 'label', label: 'Libellé' },
  { key: 'category', label: 'Catégorie' },
  { key: 'allowedDays', label: 'Jours autorisés' },
  { key: 'leadTime', label: 'Délai minimum' },
  { key: 'active', label: 'État' },
  { key: 'actions', label: 'Actions' },
];

export default function RequestTypesPage() {
  const { activeParish } = useTenant();
  const { has } = usePermissions();
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const canManage = has(PERMISSIONS.REQUEST_TYPE_MANAGE);

  const remove = async (id) => {
    if (!window.confirm('Supprimer ce type de demande ?')) return;
    try {
      setError(null);
      await requestTypeService.remove(id);
      setRows((current) => current.filter((row) => row.id !== id));
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Suppression impossible');
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
        const data = await requestTypeService.getByParish(activeParish.id);
        if (!cancelled) setRows((data || []).map(mapTypeDemandeToRow));
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
      <PageHeader title="Types de demande" subtitle="Référentiel pour la paroisse active."
        actions={canManage ? <Link className="btn btn-primary" to="/admin/types-demandes/nouveau">Nouveau type</Link> : null} />
      {error ? <p className="text-red-600">{error}</p> : null}
      {loading ? <p className="muted">Chargement…</p> : null}
      <AppTable
        columns={columns}
        rows={rows}
        renderCell={(row, column) => {
          if (column.key === 'active') return <AppBadge value={row.active} />;
          if (column.key === 'actions') return canManage ? (
            <div className="button-row">
              <Link className="btn btn-secondary" to={`/admin/types-demandes/${row.id}/modifier`}>Modifier</Link>
              <button className="btn btn-danger" onClick={() => remove(row.id)}>Supprimer</button>
            </div>
          ) : '—';
          return row[column.key];
        }}
      />
    </div>
  );
}
