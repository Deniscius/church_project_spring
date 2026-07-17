import React from 'react';

const VARIANTS = {
  danger: 'alert-danger',
  success: 'alert-success',
  warning: 'alert-warning',
  info: 'alert-info',
};

export default function AppAlert({ children, variant = 'danger', className = '' }) {
  const cls = VARIANTS[variant] || VARIANTS.danger;
  return (
    <div className={`alert ${cls} ${className}`.trim()} role="alert">
      {children}
    </div>
  );
}
