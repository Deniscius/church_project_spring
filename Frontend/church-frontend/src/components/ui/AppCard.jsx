import React from 'react';

export default function AppCard({ title, subtitle, children }) {
  return (
    <div className="card">
      {(title || subtitle) && (
        <div style={{ marginBottom: 16 }}>
          {title ? <h3 style={{ margin: 0 }}>{title}</h3> : null}
          {subtitle ? <p className="page-subtitle">{subtitle}</p> : null}
        </div>
      )}
      {children}
    </div>
  );
}
