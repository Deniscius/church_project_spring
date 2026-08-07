import React from 'react';

export default function AppTable({
  columns = [],
  rows = [],
  renderCell,
  emptyMessage = 'Aucune donnée disponible.',
  ariaLabel = 'Tableau de données',
}) {
  if (!rows.length) {
    return (
      <div className="empty-state" role="status">
        <h3>Aucun résultat</h3>
        <p>{emptyMessage}</p>
      </div>
    );
  }

  return (
    <div className="card table-card" role="region" aria-label={ariaLabel} tabIndex="0">
      <table className="app-table">
        <thead>
          <tr>
            {columns.map((column) => (
              <th key={column.key}>{column.label}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={row.id || row.key}>
              {columns.map((column) => (
                <td key={column.key} data-label={column.label}>
                  {renderCell ? renderCell(row, column) : row[column.key]}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
