import React from 'react';
import AppCard from '../ui/AppCard';
import AppInput from '../ui/AppInput';
import AppTextarea from '../ui/AppTextarea';

export default function ApplicantForm() {
  return (
    <AppCard title="Informations du demandeur" subtitle="Base de formulaire public pour le dépôt d'une demande.">
      <div className="form-grid">
        <div className="form-field">
          <label>Prénom</label>
          <AppInput placeholder="Ex. Kokou" />
        </div>
        <div className="form-field">
          <label>Nom</label>
          <AppInput placeholder="Ex. Adjei" />
        </div>
        <div className="form-field">
          <label>Email</label>
          <AppInput placeholder="email@example.com" />
        </div>
        <div className="form-field">
          <label>Téléphone</label>
          <AppInput placeholder="+228 90 00 00 00" />
        </div>
        <div className="form-field full">
          <label>Intention de messe</label>
          <AppTextarea placeholder="Décrire l'intention ou le contexte de la demande" />
        </div>
      </div>
    </AppCard>
  );
}
