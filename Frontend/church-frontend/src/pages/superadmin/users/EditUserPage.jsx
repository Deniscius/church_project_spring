import React from 'react';
import PageHeader from '../../../components/ui/PageHeader';
import AppCard from '../../../components/ui/AppCard';
import AppInput from '../../../components/ui/AppInput';
import AppButton from '../../../components/ui/AppButton';

export default function EditUserPage() {
  return (
    <div className="stack">
      <PageHeader title="Modifier un utilisateur" subtitle="Formulaire d'édition d'un compte existant." />
      <AppCard title="Utilisateur existant">
        <div className="form-grid">
          <div className="form-field"><label>Prénom</label><AppInput defaultValue="Mawuli" /></div>
          <div className="form-field"><label>Nom</label><AppInput defaultValue="Seddoh" /></div>
        </div>
        <div className="button-row" style={{ marginTop: 18 }}><AppButton>Enregistrer</AppButton></div>
      </AppCard>
    </div>
  );
}
