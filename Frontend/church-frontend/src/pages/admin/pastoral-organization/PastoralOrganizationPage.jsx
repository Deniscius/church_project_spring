import React, { useCallback, useEffect, useMemo, useState } from 'react';
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
import { pastoralOrganizationService } from '../../../services/pastoralOrganization.service';
import { STRUCTURE_TYPES } from './pastoralOrganization.constants';

const mandateColumns = [
  { key: 'fonction', label: 'Rôle / fonction' },
  { key: 'personne', label: 'Nom et prénoms' },
  { key: 'contact', label: 'Contact' },
  { key: 'attributions', label: 'Attributions' },
  { key: 'periode', label: 'Période' },
  { key: 'actions', label: 'Actions' },
];

function formatDate(value) {
  if (!value) return '—';
  const [year, month, day] = value.split('-');
  return year && month && day ? `${day}/${month}/${year}` : value;
}

export default function PastoralOrganizationPage() {
  const { activeParish } = useTenant();
  const { has } = usePermissions();
  const canManage = has(PERMISSIONS.PASTORAL_ORGANIZATION_MANAGE);
  const [years, setYears] = useState([]);
  const [selectedYearId, setSelectedYearId] = useState('');
  const [structures, setStructures] = useState([]);
  const [mandates, setMandates] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [pendingAction, setPendingAction] = useState(null);
  const [busy, setBusy] = useState(false);

  const selectedYear = useMemo(
    () => years.find((year) => year.publicId === selectedYearId),
    [selectedYearId, years]
  );
  const readOnly = selectedYear?.statut === 'CLOTUREE';

  const loadFoundation = useCallback(async () => {
    if (!activeParish?.id) {
      setYears([]);
      setStructures([]);
      setLoading(false);
      return;
    }
    try {
      setLoading(true);
      setError(null);
      const [yearData, structureData] = await Promise.all([
        pastoralYearService.getByParish(activeParish.id),
        pastoralOrganizationService.getStructures(activeParish.id),
      ]);
      const yearList = Array.isArray(yearData) ? yearData : [];
      setYears(yearList);
      setStructures(Array.isArray(structureData) ? structureData : []);
      setSelectedYearId((current) => {
        if (yearList.some((year) => year.publicId === current)) return current;
        return yearList.find((year) => year.statut === 'PUBLIEE')?.publicId
          || yearList[0]?.publicId
          || '';
      });
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Chargement impossible');
    } finally {
      setLoading(false);
    }
  }, [activeParish?.id]);

  const loadMandates = useCallback(async () => {
    if (!activeParish?.id || !selectedYearId) {
      setMandates([]);
      return;
    }
    try {
      setError(null);
      const data = await pastoralOrganizationService.getMandates(
        activeParish.id,
        selectedYearId
      );
      setMandates(Array.isArray(data) ? data : []);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Chargement des responsabilités impossible');
    }
  }, [activeParish?.id, selectedYearId]);

  useEffect(() => { loadFoundation(); }, [loadFoundation]);
  useEffect(() => { loadMandates(); }, [loadMandates]);

  const mandatesByStructure = useMemo(() => {
    const grouped = new Map();
    mandates.forEach((mandate) => {
      const list = grouped.get(mandate.structurePastoralePublicId) || [];
      list.push({
        ...mandate,
        id: mandate.publicId,
        personne: [
          mandate.acteurAppellation,
          mandate.acteurPrenoms,
          mandate.acteurNom,
        ].filter(Boolean).join(' '),
        contact: [mandate.acteurTelephone, mandate.acteurEmail].filter(Boolean).join(' · ') || '—',
        attributions: mandate.attributions || '—',
        periode: `${formatDate(mandate.dateDebut)} – ${formatDate(mandate.dateFin)}`,
      });
      grouped.set(mandate.structurePastoralePublicId, list);
    });
    return grouped;
  }, [mandates]);

  const confirmAction = async () => {
    if (!pendingAction) return;
    try {
      setBusy(true);
      setError(null);
      if (pendingAction.kind === 'mandate') {
        await pastoralOrganizationService.removeMandate(
          pendingAction.item.publicId,
          pendingAction.item.version
        );
        await loadMandates();
      } else if (pendingAction.kind === 'structure-status') {
        await pastoralOrganizationService.setStructureStatus(
          pendingAction.item.publicId,
          !pendingAction.item.actif,
          pendingAction.item.version
        );
        await loadFoundation();
      } else {
        await pastoralOrganizationService.removeStructure(
          pendingAction.item.publicId,
          pendingAction.item.version
        );
        await loadFoundation();
      }
      setPendingAction(null);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Opération impossible');
      setPendingAction(null);
    } finally {
      setBusy(false);
    }
  };

  const dialogTitle = pendingAction?.kind === 'mandate'
    ? 'Retirer cette responsabilité'
    : pendingAction?.kind === 'structure-archive'
      ? 'Archiver cette structure'
      : pendingAction?.item?.actif
        ? 'Désactiver cette structure'
        : 'Réactiver cette structure';

  return (
    <div className="stack">
      <PageHeader
        title="Organisation pastorale"
        subtitle="Communauté sacerdotale, conseils, commissions, groupes et responsables par année."
        actions={canManage ? (
          <div className="button-row">
            <Link className="btn btn-secondary" to={ROUTES.PASTORAL_STRUCTURE_CREATE}>
              Nouvelle structure
            </Link>
            <Link className="btn btn-primary" to={ROUTES.PASTORAL_MANDATE_CREATE}>
              Ajouter une responsabilité
            </Link>
          </div>
        ) : null}
      />

      <div className="card filters">
        <label className="form-field" htmlFor="organization-year">
          <span>Année pastorale</span>
          <select
            id="organization-year"
            className="select"
            value={selectedYearId}
            onChange={(event) => setSelectedYearId(event.target.value)}
          >
            <option value="">Sélectionner une année</option>
            {years.map((year) => (
              <option key={year.publicId} value={year.publicId}>
                {year.libelle} — {year.statut}
              </option>
            ))}
          </select>
        </label>
        {selectedYear ? <AppBadge value={selectedYear.statut} label={selectedYear.statut} /> : null}
      </div>

      {readOnly ? (
        <p className="muted">
          Cette année est clôturée : son organisation est conservée en lecture seule.
        </p>
      ) : null}
      {error ? <p className="text-red-600" role="alert">{error}</p> : null}
      {loading ? <p className="muted">Chargement…</p> : null}
      {!loading && years.length === 0 ? (
        <div className="card">
          <p>Aucune année pastorale n’est encore disponible.</p>
          <Link className="btn btn-primary" to={ROUTES.PASTORAL_YEAR_CREATE}>
            Créer l’année pastorale
          </Link>
        </div>
      ) : null}

      {selectedYearId ? STRUCTURE_TYPES.map((section) => {
        const sectionStructures = structures.filter((item) => item.type === section.value);
        return (
          <section className="stack" key={section.value}>
            <div>
              <h2 style={{ marginBottom: 4 }}>{section.label}</h2>
            </div>
            {sectionStructures.length === 0 ? (
              <div className="card">
                <p className="muted" style={{ margin: 0 }}>Aucune structure renseignée.</p>
              </div>
            ) : sectionStructures.map((structure) => {
              const rows = mandatesByStructure.get(structure.publicId) || [];
              return (
                <div className="card stack" key={structure.publicId}>
                  <div className="button-row" style={{ justifyContent: 'space-between' }}>
                    <div>
                      <h3 style={{ margin: 0 }}>{structure.nom}</h3>
                      {structure.attributions ? (
                        <p className="muted" style={{ marginBottom: 0 }}>{structure.attributions}</p>
                      ) : null}
                    </div>
                    <div className="button-row">
                      <AppBadge
                        value={structure.actif ? 'ACTIF' : 'INACTIF'}
                        label={structure.actif ? 'Active' : 'Inactive'}
                      />
                      {canManage && !readOnly ? (
                        <>
                          <Link
                            className="btn btn-secondary"
                            to={routePath(ROUTES.PASTORAL_STRUCTURE_EDIT, { id: structure.publicId })}
                          >
                            Modifier
                          </Link>
                          <Link
                            className="btn btn-primary"
                            to={`${ROUTES.PASTORAL_MANDATE_CREATE}?annee=${selectedYearId}&structure=${structure.publicId}`}
                          >
                            Ajouter un membre
                          </Link>
                          <button
                            type="button"
                            className="btn btn-secondary"
                            onClick={() => setPendingAction({
                              kind: 'structure-status',
                              item: structure,
                            })}
                          >
                            {structure.actif ? 'Désactiver' : 'Réactiver'}
                          </button>
                          {!structure.actif ? (
                            <button
                              type="button"
                              className="btn btn-danger"
                              onClick={() => setPendingAction({
                                kind: 'structure-archive',
                                item: structure,
                              })}
                            >
                              Archiver
                            </button>
                          ) : null}
                        </>
                      ) : null}
                    </div>
                  </div>

                  <AppTable
                    columns={mandateColumns}
                    rows={rows}
                    renderCell={(row, column) => {
                      if (column.key === 'actions') {
                        if (!canManage || readOnly) return '—';
                        return (
                          <div className="button-row">
                            <Link
                              className="btn btn-secondary"
                              to={routePath(ROUTES.PASTORAL_MANDATE_EDIT, { id: row.publicId })}
                            >
                              Modifier
                            </Link>
                            <button
                              type="button"
                              className="btn btn-danger"
                              onClick={() => setPendingAction({ kind: 'mandate', item: row })}
                            >
                              Retirer
                            </button>
                          </div>
                        );
                      }
                      return row[column.key];
                    }}
                  />
                </div>
              );
            })}
          </section>
        );
      }) : null}

      <AppDialog
        open={Boolean(pendingAction)}
        title={dialogTitle}
        confirmLabel="Confirmer"
        cancelLabel="Annuler"
        danger={pendingAction?.kind !== 'structure-status'}
        busy={busy}
        onCancel={() => setPendingAction(null)}
        onConfirm={confirmAction}
      >
        <p style={{ margin: 0 }}>
          L’historique d’une année clôturée reste toujours conservé.
        </p>
      </AppDialog>
    </div>
  );
}
