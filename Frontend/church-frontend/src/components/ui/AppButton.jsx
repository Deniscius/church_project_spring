import React from 'react';

export default function AppButton({ children, variant = 'primary', type = 'button' }) {
  const className = variant === 'secondary' ? 'btn btn-secondary' : 'btn btn-primary';
  return (
    <button type={type} className={className}>
      {children}
    </button>
  );
}
