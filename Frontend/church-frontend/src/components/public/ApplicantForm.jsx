import React from 'react';
import AppCard from '../ui/AppCard';
import AppInput from '../ui/AppInput';
import AppTextarea from '../ui/AppTextarea';
import { usePublicDemandeDraft } from '../../contexts/publicDemandeDraft.context';

export default function ApplicantForm() {
  const { draft, patch } = usePublicDemandeDraft();

  return (
    <AppCard title="Informations du demandeur" subtitle="Champs requis par l’API de création de demande.">
      <div className="form-grid">
        <div className="form-field">
          <label htmlFor="fd-prenom">Prénom *</label>
          <AppInput
            id="fd-prenom"
            autoComplete="given-name"
            value={draft.prenomFidele}
            onChange={(e) => patch({ prenomFidele: e.target.value })}
            placeholder="Ex. Kokou"
            required
          />
        </div>
        <div className="form-field">
          <label htmlFor="fd-nom">Nom *</label>
          <AppInput
            id="fd-nom"
            autoComplete="family-name"
            value={draft.nomFidele}
            onChange={(e) => patch({ nomFidele: e.target.value })}
            placeholder="Ex. Adjei"
            required
          />
        </div>
        <div className="form-field">
          <label htmlFor="fd-email">Email</label>
          <AppInput
            id="fd-email"
            type="email"
            autoComplete="email"
            value={draft.emailFidele}
            onChange={(e) => patch({ emailFidele: e.target.value })}
            placeholder="email@example.com"
          />
        </div>
        <div className="form-field">
          <label htmlFor="fd-tel">Téléphone *</label>
          <AppInput
            id="fd-tel"
            type="tel"
            autoComplete="tel"
            value={draft.telFidele}
            onChange={(e) => patch({ telFidele: e.target.value })}
            placeholder="+228 90 00 00 00"
            required
          />
        </div>
        <div className="form-field">
          <label htmlFor="fd-coursier">Nom du coursier (optionnel)</label>
          <AppInput
            id="fd-coursier"
            value={draft.nomCoursier}
            onChange={(e) => patch({ nomCoursier: e.target.value })}
            placeholder="Si dépôt par un tiers"
          />
        </div>
        <div className="form-field full">
          <label htmlFor="fd-intention">Intention de messe *</label>
          <AppTextarea
            id="fd-intention"
            value={draft.intention}
            onChange={(e) => patch({ intention: e.target.value })}
            placeholder="Décrire l’intention ou le contexte de la demande"
            rows={4}
            required
          />
        </div>
      </div>
    </AppCard>
  );
}
