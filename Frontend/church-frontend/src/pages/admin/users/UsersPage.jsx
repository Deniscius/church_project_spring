import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import PageHeader from '../../../components/ui/PageHeader';
import AppTable from '../../../components/ui/AppTable';
import AppBadge from '../../../components/ui/AppBadge';
import AppDialog from '../../../components/ui/AppDialog';
import { useTenant } from '../../../hooks/useTenant';
import { useAuth } from '../../../hooks/useAuth';
import { usePermissions } from '../../../hooks/usePermissions';
import { PERMISSIONS, ROLE_LABELS } from '../../../constants/roles';
import { userService } from '../../../services/user.service';
import { mapUserToRow } from '../../../utils/apiMappers';
import { formatRole } from '../../../utils/roleMapper';

const columns = [
  { key: 'fullName', label: 'Nom' },
  { key: 'username', label: 'Identifiant' },
  { key: 'email', label: 'E-mail pro' },
  { key: 'roleLabel', label: 'Rôle' },
  { key: 'active', label: 'État' },
  { key: 'actions', label: 'Actions' },
];

export default function UsersPage() {
  const { activeParish } = useTenant();
  const { user: currentUser } = useAuth();
  const { has } = usePermissions();
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [pendingDelete, setPendingDelete] = useState(null);
  const [deleting, setDeleting] = useState(false);
  const canManage = has(PERMISSIONS.USER_MANAGE);
  const currentUserId = currentUser?.id || currentUser?.publicId;

  const confirmRemove = async () => {
    if (!pendingDelete) return;
    if (pendingDelete.id === currentUserId) {
      setError('Vous ne pouvez pas désactiver votre propre compte');
      setPendingDelete(null);
      return;
    }
    try {
      setDeleting(true);
      setError(null);
      await userService.delete(pendingDelete.id);
      setRows((current) => current.filter((row) => row.id !== pendingDelete.id));
      setPendingDelete(null);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Suppression impossible');
    } finally {
      setDeleting(false);
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
        const data = await userService.getUsersByParoisse(activeParish.id);
        if (!cancelled) {
          setRows(
            (data || []).map((user) => {
              const row = mapUserToRow(user);
              return {
                ...row,
                fullName: [row.firstName, row.lastName].filter(Boolean).join(' ') || '—',
                email: user.email || row.email || '—',
                roleLabel: formatRole(row.role),
                isSelf: row.id === currentUserId,
              };
            })
          );
        }
      } catch (e) {
        if (!cancelled) setError(e instanceof Error ? e.message : 'Erreur de chargement');
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [activeParish?.id, currentUserId]);

  return (
    <div className="stack">
      <PageHeader
        title="Équipe"
        subtitle={
          activeParish?.name
            ? `Membres rattachés à ${activeParish.name} (e-mails professionnels générés par paroisse).`
            : 'Sélectionnez une paroisse active pour gérer l’équipe.'
        }
        actions={
          canManage && activeParish?.id ? (
            <Link className="btn btn-primary" to="/admin/equipe/nouveau">
              Nouveau membre
            </Link>
          ) : null
        }
      />

      {!activeParish?.id ? (
        <p className="muted">Aucune paroisse active dans votre session.</p>
      ) : null}
      {error ? <p className="text-red-600">{error}</p> : null}
      {loading ? <p className="muted">Chargement…</p> : null}

      {!loading && activeParish?.id && rows.length === 0 ? (
        <p className="muted">Aucun membre dans l’équipe de cette paroisse.</p>
      ) : null}

      {!loading && rows.length > 0 ? (
        <AppTable
          columns={columns}
          rows={rows}
          renderCell={(row, column) => {
            if (column.key === 'active') return <AppBadge value={row.active} />;
            if (column.key === 'roleLabel') {
              return ROLE_LABELS[row.role] || row.roleLabel || row.role || '—';
            }
            if (column.key === 'actions') {
              return canManage ? (
                <div className="button-row">
                  <Link className="btn btn-secondary" to={`/admin/equipe/${row.id}/modifier`}>
                    Modifier
                  </Link>
                  {row.isSelf ? (
                    <span className="muted text-sm">Vous</span>
                  ) : (
                    <button type="button" className="btn btn-danger" onClick={() => setPendingDelete(row)}>
                      Désactiver
                    </button>
                  )}
                </div>
              ) : (
                '—'
              );
            }
            return row[column.key];
          }}
        />
      ) : null}

      <AppDialog
        open={Boolean(pendingDelete)}
        title="Désactiver le membre"
        confirmLabel="Désactiver"
        cancelLabel="Annuler"
        danger
        busy={deleting}
        onCancel={() => setPendingDelete(null)}
        onConfirm={confirmRemove}
      >
        {pendingDelete ? (
          <p style={{ margin: 0 }}>
            Désactiver « {pendingDelete.fullName || pendingDelete.username} » ?
          </p>
        ) : null}
      </AppDialog>
    </div>
  );
}
