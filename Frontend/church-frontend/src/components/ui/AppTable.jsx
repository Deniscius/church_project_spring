import React from 'react';

export default function AppTable({ columns = [], rows = [], renderCell }) {
  return (
    <div className="card">
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
                <td key={column.key}>
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
