import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import PageHeader from '../../components/ui/PageHeader';
import TrackingCodeForm from '../../components/public/TrackingCodeForm';
import AppCard from '../../components/ui/AppCard';
import AppInput from '../../components/ui/AppInput';
import AppButton from '../../components/ui/AppButton';

export default function TrackingPage() {
  const navigate = useNavigate();
  const [code, setCode] = useState('');

  const submit = (e) => {
    e.preventDefault();
    const c = code.trim();
    if (!c) return;
    navigate(`/suivi/resultat?code=${encodeURIComponent(c)}`);
  };

  return (
    <div className="stack">
      <PageHeader title="Suivi de demande" subtitle="Recherche publique par code de suivi." />
      <div className="grid-2">
        <AppCard title="Entrer un code de suivi" subtitle="Consultation sans compte (API /demandes/code/…).">
          <form onSubmit={submit}>
            <div className="form-field">
              <label htmlFor="track-code">Code de suivi</label>
              <AppInput
                id="track-code"
                value={code}
                onChange={(e) => setCode(e.target.value)}
                placeholder="Ex. DEM-XXXXXXXX"
              />
            </div>
            <div className="button-row" style={{ marginTop: 16 }}>
              <AppButton type="submit">Consulter</AppButton>
            </div>
          </form>
        </AppCard>
        <TrackingCodeForm />
      </div>
    </div>
  );
}
