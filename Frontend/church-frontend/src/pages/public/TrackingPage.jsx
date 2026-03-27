import React from 'react';
import PageHeader from '../../components/ui/PageHeader';
import TrackingCodeForm from '../../components/public/TrackingCodeForm';
import AppCard from '../../components/ui/AppCard';
import AppInput from '../../components/ui/AppInput';
import AppButton from '../../components/ui/AppButton';

export default function TrackingPage() {
  return (
    <div className="stack">
      <PageHeader title="Suivi de demande" subtitle="Recherche publique par code de suivi." />
      <div className="grid-2">
        <AppCard title="Entrer un code de suivi" subtitle="Le backend a déjà prévu les endpoints de consultation par code.">
          <div className="form-field">
            <label>Code de suivi</label>
            <AppInput placeholder="Ex. MS-2026-0001" />
          </div>
          <div className="button-row" style={{ marginTop: 16 }}>
            <AppButton>Consulter</AppButton>
          </div>
        </AppCard>
        <TrackingCodeForm />
      </div>
    </div>
  );
}
