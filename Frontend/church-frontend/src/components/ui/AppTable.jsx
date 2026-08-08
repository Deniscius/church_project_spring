import React, { useMemo, useState } from 'react';

function compareValues(a, b) {
  if (a == null && b == null) return 0;
  if (a == null) return 1;
  if (b == null) return -1;
  if (typeof a === 'number' && typeof b === 'number') return a - b;
  return String(a).localeCompare(String(b), 'fr', { numeric: true, sensitivity: 'base' });
}

/**
 * Tableau fonctionnel : tri optionnel par colonne (sortable: true).
 */
export default function AppTable({
  columns = [],
  rows = [],
  renderCell,
  emptyMessage = 'Aucune donnée disponible.',
  ariaLabel = 'Tableau de données',
  onRowClick,
  rowClassName,
  defaultSortKey = null,
  defaultSortDir = 'asc',
}) {
  const [sortKey, setSortKey] = useState(defaultSortKey);
  const [sortDir, setSortDir] = useState(defaultSortDir);

  const sortableColumns = useMemo(
    () => new Set(columns.filter((c) => c.sortable).map((c) => c.key)),
    [columns]
  );

  const sortedRows = useMemo(() => {
    if (!sortKey || !sortableColumns.has(sortKey)) return rows;
    const sorted = [...rows].sort((left, right) => {
      const column = columns.find((c) => c.key === sortKey);
      const getter = column?.sortValue;
      const a = typeof getter === 'function' ? getter(left) : left[sortKey];
      const b = typeof getter === 'function' ? getter(right) : right[sortKey];
      return compareValues(a, b);
    });
    return sortDir === 'desc' ? sorted.reverse() : sorted;
  }, [rows, sortKey, sortDir, sortableColumns, columns]);

  function toggleSort(key) {
    if (!sortableColumns.has(key)) return;
    if (sortKey === key) {
      setSortDir((d) => (d === 'asc' ? 'desc' : 'asc'));
      return;
    }
    setSortKey(key);
    setSortDir('asc');
  }

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
            {columns.map((column) => {
              const canSort = sortableColumns.has(column.key);
              const active = sortKey === column.key;
              return (
                <th
                  key={column.key}
                  aria-sort={active ? (sortDir === 'asc' ? 'ascending' : 'descending') : undefined}
                >
                  {canSort ? (
                    <button
                      type="button"
                      className={`app-table-sort${active ? ' is-active' : ''}`}
                      onClick={() => toggleSort(column.key)}
                    >
                      <span>{column.label}</span>
                      <span className="app-table-sort-icon" aria-hidden="true">
                        {active ? (sortDir === 'asc' ? '↑' : '↓') : '↕'}
                      </span>
                    </button>
                  ) : (
                    column.label
                  )}
                </th>
              );
            })}
          </tr>
        </thead>
        <tbody>
          {sortedRows.map((row) => {
            const clickable = typeof onRowClick === 'function';
            const extraClass = typeof rowClassName === 'function' ? rowClassName(row) : rowClassName;
            return (
              <tr
                key={row.id || row.key || row.publicId}
                className={[clickable ? 'is-clickable' : '', extraClass].filter(Boolean).join(' ') || undefined}
                onClick={clickable ? () => onRowClick(row) : undefined}
                onKeyDown={clickable ? (e) => {
                  if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    onRowClick(row);
                  }
                } : undefined}
                tabIndex={clickable ? 0 : undefined}
                role={clickable ? 'button' : undefined}
              >
                {columns.map((column) => (
                  <td key={column.key} data-label={column.label}>
                    {renderCell ? renderCell(row, column) : row[column.key]}
                  </td>
                ))}
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
