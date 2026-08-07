import React from 'react';
import { useNavigate } from 'react-router-dom';
import AppIcon from './AppIcon';

export default function BackButton({ to, label = 'Retour' }) {
  const navigate = useNavigate();

  const goBack = () => {
    if (to) {
      navigate(to);
      return;
    }
    navigate(-1);
  };

  return (
    <button type="button" className="back-button" onClick={goBack} aria-label={label}>
      <AppIcon name="back" size={18} />
      <span>{label}</span>
    </button>
  );
}
