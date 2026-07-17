import React from 'react';

const VARIANTS = {
  primary: 'btn btn-primary',
  secondary: 'btn btn-secondary',
  danger: 'btn btn-danger',
  ghost: 'btn btn-ghost',
};

export default function AppButton({
  children,
  variant = 'primary',
  type = 'button',
  loading = false,
  size,
  className = '',
  disabled,
  ...rest
}) {
  const base = VARIANTS[variant] || VARIANTS.primary;
  const sizeClass = size === 'sm' ? ' btn-sm' : '';
  const isDisabled = disabled || loading;

  return (
    <button
      type={type}
      className={`${base}${sizeClass} ${className}`.trim()}
      disabled={isDisabled}
      aria-busy={loading || undefined}
      {...rest}
    >
      {loading ? (
        <>
          <span className="spinner" style={{ width: 14, height: 14, borderWidth: 2 }} aria-hidden="true" />
          {children}
        </>
      ) : (
        children
      )}
    </button>
  );
}
