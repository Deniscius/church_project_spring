import React from 'react';

export default function AppBreadcrumb({ items = [] }) {
  return (
    <div style={{ marginBottom: 16, color: 'var(--muted)' }}>
      {items.map((item, index) => (
        <span key={item.label}>
          {index > 0 ? ' / ' : ''}
          {item.label}
        </span>
      ))}
    </div>
  );
}
