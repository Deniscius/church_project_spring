import React, { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import PageHeader from '../../../components/ui/PageHeader';
import AppCard from '../../../components/ui/AppCard';
import AppBadge from '../../../components/ui/AppBadge';
import { requestService } from '../../../services/request.service';
import { paymentService } from '../../../services/payment.service';
import { formatCurrency } from '../../../utils/formatCurrency';
import { formatDate } from '../../../utils/formatDate';
import { formatCelebrationTime } from '../../../utils/formatTime';
import { paymentStatusLabel } from '../../../utils/statusMapper';
import { formatFideleName } from '../../../utils/personName';
import { usePermissions } from '../../../hooks/usePermissions';
import { PERMISSIONS } from '../../../constants/roles';
import AppButton from '../../../components/ui/AppButton';
import AppDialog from '../../../components/ui/AppDialog';
import { getApiBaseUrl } from '../../../config/apiBaseUrl';

export default function RequestDetailsPage() {
  const { id } = useParams();
  const { has } = usePermissions();
  const [request, setRequest] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [info, setInfo] = useState(null);
  const [validating, setValidating] = useState(false);
  const [editingIntention, setEditingIntention] = useState(false);
  const [intentionDraft, setIntentionDraft] = useState('');
  const [savingIntention, setSavingIntention] = useState(false);
  const [encaissing, setEncaissing] = useState(false);
  const [confirmCaisse, setConfirmCaisse] = useState(false);

  const canEdit = has(PERMISSIONS.DEMAND_EDIT);
  const canValidate = has(PERMISSIONS.DEMAND_VALIDATE);
  const canCash = has(PERMISSIONS.PAYMENT_MANAGE);
  const unpaid = request && request.statutPaiement !== 'PAYE';

  const updateValidation = async (statut) => {
    if (!id) return;
    try {
      setValidating(true);
      setError(null);
      const updated = await requestService.updateValidation(id, statut);
      setRequest(updated);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'La validation a échoué');
    } finally {
      setValidating(false);
    }
  };

  const saveIntention = async () => {
    if (!id) return;
    const trimmed = intentionDraft.trim();
    if (trimmed.length < 2) {
      setError('Indiquez une intention claire.');
      return;
    }
    try {
      setSavingIntention(true);
      setError(null);
      const updated = await requestService.updateIntention(id, trimmed);
      setRequest(updated);
      setEditingIntention(false);
      setInfo('Intention mise à jour.');
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Impossible de modifier l’intention');
    } finally {
      setSavingIntention(false);
    }
  };

  const encaisserCaisse = async () => {
    if (!id || !request) return;
    try {
      setEncaissing(true);
      setError(null);
      setConfirmCaisse(false);
      await paymentService.encaisserCaisse(id);
      const updated = await requestService.getById(id);
      setRequest(updated);
      setInfo('Encaissement espèces enregistré en caisse locale (encaisseur et horodatage tracés).');
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Encaissement impossible');
    } finally {
      setEncaissing(false);
    }
  };

  useEffect(() => {
    if (!id) return;
    let cancelled = false;
    (async () => {
      try {
        setLoading(true);
        setError(null);
        const data = await requestService.getById(id);
        if (!cancelled) setRequest(data);
      } catch (e) {
        if (!cancelled) setError(e instanceof Error ? e.message : 'Erreur');
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [id]);

  const applicant = request
    ? formatFideleName(request.prenomFidele, request.nomFidele)
    : '—';

  return (
    <div className="stack">
      <PageHeader
        title="Détail d'une demande"
        subtitle="Intention, célébration et dépôt sont présentés séparément."
      />
      {loading ? <p className="muted">Chargement…</p> : null}
      {error ? <div className="alert-danger" role="alert">{error}</div> : null}
      {info ? <div className="alert-success" role="status">{info}</div> : null}
      {request?.statusDel ? (
        <div className="alert-danger" role="status">
          Demande archivée (soft delete)
          {request.deletedAt ? ` le ${formatDate(request.deletedAt)}` : ''}
          {request.deletedByNom ? ` par ${request.deletedByNom}` : ''}.
          Elle reste consultable pour traçabilité.
        </div>
      ) : null}
      {request ? (
        <div className="grid-2">
          <AppCard
            title="Intention de messe"
            subtitle="Motif de la célébration — corrigeable par le secrétariat."
          >
            {editingIntention ? (
              <div className="stack">
                <textarea
                  className="textarea"
                  rows={4}
                  maxLength={500}
                  value={intentionDraft}
                  onChange={(e) => setIntentionDraft(e.target.value)}
                  autoFocus
                />
                <div className="button-row">
                  <AppButton onClick={saveIntention} disabled={savingIntention}>
                    {savingIntention ? 'Enregistrement…' : 'Enregistrer'}
                  </AppButton>
                  <AppButton
                    variant="secondary"
                    disabled={savingIntention}
                    onClick={() => {
                      setEditingIntention(false);
                      setIntentionDraft(request.intention || '');
                    }}
                  >
                    Annuler
                  </AppButton>
                </div>
              </div>
            ) : (
              <>
                <p style={{ margin: 0, fontSize: '1.05rem', fontWeight: 600, lineHeight: 1.45 }}>
                  {request.intention || '—'}
                </p>
                {canEdit ? (
                  <div className="button-row" style={{ marginTop: 14 }}>
                    <AppButton
                      variant="secondary"
                      onClick={() => {
                        setIntentionDraft(request.intention || '');
                        setEditingIntention(true);
                        setInfo(null);
                      }}
                    >
                      Corriger l’intention
                    </AppButton>
                  </div>
                ) : null}
              </>
            )}
          </AppCard>

          <AppCard title="Célébration" subtitle="Jour(s) où la messe sera célébrée.">
            <div className="info-list">
              <div className="info-row">
                <span>Date(s) de célébration</span>
                <span>
                  {Array.isArray(request.datesCelebration) && request.datesCelebration.length
                    ? request.datesCelebration.map((d) => formatDate(d)).join(' · ')
                    : '—'}
                </span>
              </div>
              <div className="info-row">
                <span>Heure de célébration</span>
                <span>{formatCelebrationTime(request)}</span>
              </div>
              <div className="info-row">
                <span>Type</span>
                <span>{request.typeDemandeLibelle || '—'}</span>
              </div>
              <div className="info-row">
                <span>Forfait</span>
                <span>{request.forfaitTarifNom || '—'}</span>
              </div>
            </div>
          </AppCard>

          <AppCard title="Demandeur">
            <div className="info-list">
              <div className="info-row">
                <span>Code de suivi</span>
                <strong>{request.codeSuivie}</strong>
              </div>
              <div className="info-row">
                <span>Nom</span>
                <span>{applicant}</span>
              </div>
              <div className="info-row">
                <span>Contact</span>
                <span>
                  {request.telFidele || '—'} {request.emailFidele ? `· ${request.emailFidele}` : ''}
                </span>
              </div>
              <div className="info-row">
                <span>Montant</span>
                <span>{formatCurrency(request.montant != null ? Number(request.montant) : 0)}</span>
              </div>
              <div className="info-row">
                <span>Date de dépôt</span>
                <span>{formatDate(request.createdAt)}</span>
              </div>
            </div>
          </AppCard>

          <AppCard title="Statuts & facturation">
            <div className="info-list">
              <div className="info-row">
                <span>Demande</span>
                <AppBadge value={request.statutDemande} />
              </div>
              <div className="info-row">
                <span>Validation</span>
                <AppBadge value={request.statutValidation} />
              </div>
              {request.validateBy ? (
                <div className="info-row">
                  <span>Traitée par</span>
                  <span>{request.validateBy}</span>
                </div>
              ) : null}
              <div className="info-row">
                <span>Paiement</span>
                <AppBadge
                  value={request.statutPaiement}
                  label={paymentStatusLabel(request.statutPaiement)}
                />
              </div>
              <div className="info-row">
                <span>Mode</span>
                <span>{request.typePaiementLibelle || request.modePaiement || '—'}</span>
              </div>
              <div className="info-row">
                <span>Réf. facture</span>
                <span>
                  {request.refFacture && request.facturePublicId ? (
                    <Link to={`/admin/factures/${request.facturePublicId}`}>{request.refFacture}</Link>
                  ) : (
                    request.refFacture || '—'
                  )}
                </span>
              </div>
              <div className="info-row">
                <span>Transaction</span>
                <span>{request.idTransaction || '—'}</span>
              </div>
            </div>
            <div className="button-row" style={{ marginTop: 18 }}>
              {request.codeSuivie ? (
                <a
                  href={`${getApiBaseUrl()}/demandes/code/${encodeURIComponent(request.codeSuivie)}/recu.pdf`}
                  className="btn btn-secondary"
                  style={{ textDecoration: 'none' }}
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  Imprimer le reçu
                </a>
              ) : null}
              {canValidate ? (
                <>
                  <AppButton
                    disabled={validating || request.statutValidation === 'VALIDEE'}
                    onClick={() => updateValidation('VALIDEE')}
                  >
                    {validating ? 'Traitement…' : 'Valider'}
                  </AppButton>
                  <AppButton
                    variant="secondary"
                    disabled={validating || request.statutValidation === 'REJETEE'}
                    onClick={() => updateValidation('REJETEE')}
                  >
                    Rejeter
                  </AppButton>
                </>
              ) : null}
              {canCash && unpaid ? (
                <AppButton
                  variant="secondary"
                  disabled={encaissing}
                  onClick={() => setConfirmCaisse(true)}
                >
                  {encaissing ? 'Encaissement…' : 'Encaisser en caisse'}
                </AppButton>
              ) : null}
            </div>
            {canCash && unpaid ? (
              <p className="muted" style={{ marginTop: 10, marginBottom: 0 }}>
                L’encaissement espèces reste dans la caisse de la paroisse et n’alimente pas
                le solde à reverser en ligne.
              </p>
            ) : null}
          </AppCard>
        </div>
      ) : null}

      <AppDialog
        open={confirmCaisse}
        title="Encaisser en caisse"
        confirmLabel="Confirmer l’encaissement"
        cancelLabel="Annuler"
        busy={encaissing}
        onCancel={() => setConfirmCaisse(false)}
        onConfirm={encaisserCaisse}
      >
        {request ? (
          <p style={{ margin: 0 }}>
            Encaisser {formatCurrency(Number(request.montant) || 0)} en espèces pour « {request.codeSuivie} » ?
            Votre nom et l’horodatage seront enregistrés pour le contrôle comptable.
          </p>
        ) : null}
      </AppDialog>
    </div>
  );
}
