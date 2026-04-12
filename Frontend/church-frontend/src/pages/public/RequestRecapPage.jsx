import React, { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useMutation } from '@tanstack/react-query';
import PageHeader from '../../components/ui/PageHeader';
import RequestSummaryCard from '../../components/public/RequestSummaryCard';
import AppCard from '../../components/ui/AppCard';
import AppButton from '../../components/ui/AppButton';
import { usePublicDemandeDraft } from '../../contexts/publicDemandeDraft.context';
import { requestService } from '../../services/request.service';
import {
  buildDemandeRequestBody,
  persistDemandeCreationResult,
  validatePublicDemandeDraft,
} from '../../utils/publicDemandeValidation';
import { formatCurrency } from '../../utils/formatCurrency';

export default function RequestRecapPage() {
  const navigate = useNavigate();
  const { draft } = usePublicDemandeDraft();
  const [errors, setErrors] = useState([]);

  useEffect(() => {
    const { ok } = validatePublicDemandeDraft(draft);
    if (!ok) {
      navigate('/demande', { replace: true });
    }
  }, [draft, navigate]);

  const mutation = useMutation({
    mutationFn: (body) => requestService.create(body),
    onSuccess: (data) => {
      const result = {
        codeSuivie: data.codeSuivie,
        statutDemande: data.statutDemande,
        statutPaiement: data.statutPaiement,
        statutValidation: data.statutValidation,
        refFacture: data.refFacture,
        montant: data.montant,
      };
      persistDemandeCreationResult(result);
      navigate('/demande/confirmation', { replace: true, state: result });
    },
    onError: (err) => {
      setErrors([err instanceof Error ? err.message : 'Échec de la création']);
    },
  });

  const submit = () => {
    const { ok, errors: v } = validatePublicDemandeDraft(draft);
    if (!ok) {
      setErrors(v);
      return;
    }
    setErrors([]);
    mutation.mutate(buildDemandeRequestBody(draft));
  };

  return (
    <div className="stack">
      <PageHeader
        title="Récapitulatif de la demande"
        subtitle="Relisez les informations avant envoi définitif au serveur."
      />
      {errors.length > 0 ? (
        <div className="card" style={{ borderColor: 'var(--danger, #b91c1c)' }}>
          <ul style={{ margin: '0 0 0 18px' }}>
            {errors.map((msg) => (
              <li key={msg}>{msg}</li>
            ))}
          </ul>
        </div>
      ) : null}
      <div className="grid-2">
        <RequestSummaryCard />
        <AppCard title="Validation" subtitle="Création via POST /demandes (sans authentification).">
          <div className="info-list">
            <div className="info-row">
              <span>Paroisse</span>
              <span>{draft.paroisseNom || '—'}</span>
            </div>
            <div className="info-row">
              <span>Type</span>
              <span>{draft.typeDemandeLibelle || '—'}</span>
            </div>
            <div className="info-row">
              <span>Montant</span>
              <span>
                {draft.forfaitMontant != null
                  ? formatCurrency(Number(draft.forfaitMontant))
                  : '—'}
              </span>
            </div>
          </div>
          <div className="button-row" style={{ marginTop: 20 }}>
            <AppButton type="button" onClick={submit} disabled={mutation.isPending}>
              {mutation.isPending ? 'Envoi…' : 'Confirmer la demande'}
            </AppButton>
            <Link to="/demande" className="btn btn-secondary" style={{ textDecoration: 'none' }}>
              Modifier
            </Link>
          </div>
        </AppCard>
      </div>
    </div>
  );
}
